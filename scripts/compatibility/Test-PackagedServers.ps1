[CmdletBinding()]
param(
    [ValidateSet('1.16.5','1.18.2','1.19.2','1.20.1','1.21.1')][string[]]$Versions = @('1.16.5','1.18.2','1.19.2','1.20.1','1.21.1'),
    [ValidateSet('all','dragonloot')][string]$Profile = 'all',
    [ValidateSet('Prepare','Run','Both')][string]$Phase = 'Both',
    [string]$ResultsDirectory = '',
    [string]$AssetRoot = '',
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9._+-]*$')][string]$ModVersion = '1.1.16-dev',
    [string]$ProductionDirectory = '',
    [string]$Java8Home = 'C:/Program Files/Eclipse Adoptium/jdk-8.0.504.1-hotspot',
    [string]$Java17Home = 'C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot',
    [string]$Java21Home = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot',
    [switch]$HoldReady,
    [switch]$AllowFlight,
    [ValidateRange(0,65535)][int]$ServerPort = 0,
    [ValidatePattern('^$|^[A-Za-z0-9_]{1,16}$')][string]$OperatorName = '',
    [ValidateRange(30,86400)][int]$TimeoutSeconds = 600
)
$ErrorActionPreference = 'Stop'
$repositoryDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if (!$ProductionDirectory) { $ProductionDirectory = Join-Path $repositoryDirectory 'release' }
$ProductionDirectory = [IO.Path]::GetFullPath($ProductionDirectory)
if (!$AssetRoot) { $AssetRoot = Join-Path $repositoryDirectory 'release/test-results/2026-10-06/compatibility' }
$AssetRoot = [IO.Path]::GetFullPath($AssetRoot)
if (!$ResultsDirectory) {
    if ($Phase -eq 'Run') { throw 'Run requires the exact ResultsDirectory used for Prepare.' }
    $ResultsDirectory = Join-Path $repositoryDirectory ('release/test-results/2026-10-06/production-servers/' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss'))
}
$ResultsDirectory = [IO.Path]::GetFullPath($ResultsDirectory)
New-Item -ItemType Directory -Path $ResultsDirectory -Force | Out-Null
$manifest = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'manifest.json') -Raw | ConvertFrom-Json
$installers = Get-Content -LiteralPath (Join-Path $AssetRoot 'installers.json') -Raw | ConvertFrom-Json
$utf8 = New-Object Text.UTF8Encoding($false)
if ($HoldReady -and $Versions.Count -ne 1) { throw 'HoldReady requires exactly one selected version.' }

function Write-JsonFile([string]$path, $value) {
    [IO.File]::WriteAllText($path, ($value | ConvertTo-Json -Depth 14), $utf8)
}

function Quote-NativeArgument([string]$argument) {
    $quoted = [regex]::Replace($argument, '(\\*)"', '$1$1\"')
    $quoted = [regex]::Replace($quoted, '(\\+)$', '$1$1')
    return '"' + $quoted + '"'
}

function Read-LiveLog([string]$path) {
    $stream = [IO.FileStream]::new($path, [IO.FileMode]::Open, [IO.FileAccess]::Read,
        [IO.FileShare]::ReadWrite)
    $reader = [IO.StreamReader]::new($stream)
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

function Get-LoadedModEvidence([string]$directory, $prepared) {
    $debugPath = Join-Path (Join-Path $directory 'logs') 'debug.log'
    if (!(Test-Path -LiteralPath $debugPath)) {
        return [ordered]@{verified=$false;mods=@();error='Native debug log is unavailable; loaded versions are unverified.'}
    }
    $debug = [IO.File]::ReadAllText($debugPath)
    $observed = @{}
    foreach ($match in [regex]::Matches($debug, 'Found valid mod file [^\r\n]+ with \{([^}]+)\} mods - versions \{([^}]+)\}')) {
        $ids = $match.Groups[1].Value.Split(',')
        $versions = $match.Groups[2].Value.Split(',')
        if ($ids.Count -eq $versions.Count) {
            for ($index = 0; $index -lt $ids.Count; $index++) { $observed[$ids[$index].Trim()] = $versions[$index].Trim() }
        }
    }
    $expected = @($prepared.mods) + @(
        [pscustomobject]@{id=$prepared.loader;version=$prepared.loaderVersion},
        [pscustomobject]@{id='minecraft';version=$prepared.minecraft})
    $records = @()
    foreach ($mod in $expected) {
        $actual = $observed[$mod.id]
        $records += [ordered]@{id=$mod.id;expected=$mod.version;observed=$actual;matched=$actual -eq $mod.version}
    }
    return [ordered]@{verified=@($records | Where-Object { !$_.matched }).Count -eq 0;mods=$records}
}

function Stop-OwnedProcessTree([Diagnostics.Process]$process) {
    if ($process.HasExited) { return }
    $snapshot = @(Get-CimInstance Win32_Process | Select-Object ProcessId,ParentProcessId)
    $owned = New-Object System.Collections.Generic.List[int]
    $owned.Add($process.Id)
    for ($index = 0; $index -lt $owned.Count; $index++) {
        foreach ($child in $snapshot | Where-Object { $_.ParentProcessId -eq $owned[$index] }) {
            if (!$owned.Contains([int]$child.ProcessId)) { $owned.Add([int]$child.ProcessId) }
        }
    }
    for ($index = $owned.Count - 1; $index -ge 0; $index--) {
        Stop-Process -Id $owned[$index] -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-RecordedJava([string]$java, [string[]]$arguments, [string]$directory,
    [string]$prefix, [string]$heap, [bool]$serverRun) {
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = $java
    $start.Arguments = ($arguments | ForEach-Object { Quote-NativeArgument $_ }) -join ' '
    $start.WorkingDirectory = $directory
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.RedirectStandardInput = $serverRun
    # The installer launches processors as children; constrain those JVMs as well.
    $start.EnvironmentVariables['_JAVA_OPTIONS'] = "-Xmx$heap -XX:ActiveProcessorCount=2"
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $start
    $stdoutPath = "$prefix.stdout.log"
    $stderrPath = "$prefix.stderr.log"
    $stdoutFile = [IO.FileStream]::new($stdoutPath, [IO.FileMode]::Create, [IO.FileAccess]::Write,
        [IO.FileShare]::ReadWrite, 1, [IO.FileOptions]::Asynchronous)
    $stderrFile = [IO.FileStream]::new($stderrPath, [IO.FileMode]::Create, [IO.FileAccess]::Write,
        [IO.FileShare]::ReadWrite, 1, [IO.FileOptions]::Asynchronous)
    $done = $false
    $stopSent = $false
    $timedOut = $false
    $watch = [Diagnostics.Stopwatch]::StartNew()
    $lastProgress = 0
    $commandIndex = 0
    $operatorGranted = $false
    $commandsFile = Join-Path $directory 'server-commands.txt'
    try {
        $process.Start() | Out-Null
        $processHandle = $process.Handle
        $process.Id | Set-Content -LiteralPath "$prefix.pid"
        $stdoutCopy = $process.StandardOutput.BaseStream.CopyToAsync($stdoutFile)
        $stderrCopy = $process.StandardError.BaseStream.CopyToAsync($stderrFile)
        while (!$process.HasExited) {
            if ($watch.Elapsed.TotalSeconds -ge $TimeoutSeconds) {
                $timedOut = $true
                Stop-OwnedProcessTree $process
                break
            }
            if ($serverRun -and !$done) {
                $console = (Read-LiveLog $stdoutPath) + (Read-LiveLog $stderrPath)
                if ($console -match 'Done \([^\r\n]+\)! For help, type') {
                    $done = $true
                    # Keep natural flat-world mobs from interrupting supervised
                    # controls while preserving explicitly summoned test targets.
                    $process.StandardInput.WriteLine('gamerule doMobSpawning false')
                    $process.StandardInput.WriteLine('datapack list enabled')
                    $process.StandardInput.WriteLine('list')
                    if (!$HoldReady) {
                        $process.StandardInput.WriteLine('stop')
                        $stopSent = $true
                    }
                    $process.StandardInput.Flush()
                    if ($HoldReady) { Write-Host "Server READY; sent gamerule doMobSpawning false; append console commands to $commandsFile (stop ends the run)." }
                    else { Write-Host "Server reached Done; sent datapack/list/stop: $directory" }
                }
            }
            if ($serverRun -and $done -and $HoldReady -and !$stopSent) {
                if ($OperatorName -and !$operatorGranted) {
                    $console = (Read-LiveLog $stdoutPath) + (Read-LiveLog $stderrPath)
                    if ($console -match (': ' + [regex]::Escape($OperatorName) + ' joined the game(?:\r?\n|$)')) {
                        # Resolve the connected profile. A pre-join cache lookup can
                        # return an online UUID while this offline server uses another.
                        $process.StandardInput.WriteLine("op $OperatorName")
                        $process.StandardInput.Flush()
                        $operatorGranted = $true
                        Write-Host "Requested operator permission for connected profile $OperatorName."
                    }
                }
                $commands = @((Read-LiveLog $commandsFile) -split '\r?\n' | Where-Object { $_.Trim() })
                while ($commandIndex -lt $commands.Count) {
                    $command = $commands[$commandIndex].Trim()
                    $process.StandardInput.WriteLine($command)
                    $process.StandardInput.Flush()
                    $commandIndex++
                    if ($command -eq 'stop') { $stopSent = $true; break }
                }
            }
            if ($watch.Elapsed.TotalSeconds - $lastProgress -ge 30) {
                $lastProgress = $watch.Elapsed.TotalSeconds
                Write-Host ("Waiting for owned JVM {0}: {1}s" -f $process.Id, [int]$lastProgress)
            }
            Start-Sleep -Milliseconds 250
        }
        $process.WaitForExit()
        $process.Refresh()
        $null = $stdoutCopy.GetAwaiter().GetResult()
        $null = $stderrCopy.GetAwaiter().GetResult()
        $stdoutFile.Flush()
        $stderrFile.Flush()
        $code = $process.ExitCode
        if ($null -eq $code) { throw 'Owned Java process exit code was unavailable.' }
        $code | Set-Content -LiteralPath "$prefix.exitcode"
        return [ordered]@{exitCode=$code;done=$done;stopSent=$stopSent;timedOut=$timedOut;
            durationSeconds=[Math]::Round($watch.Elapsed.TotalSeconds,2)}
    } finally {
        if (!$process.HasExited) { Stop-OwnedProcessTree $process }
        $stdoutFile.Dispose()
        $stderrFile.Dispose()
        $process.Dispose()
    }
}

$failures = 0
foreach ($version in $Versions) {
    $target = $manifest.targets | Where-Object { $_.minecraft -eq $version } | Select-Object -First 1
    if (!$target) { throw "No pinned loader target for $version" }
    $installer = $installers | Where-Object { $_.minecraft -eq $version -and $_.loader -eq $target.loader } | Select-Object -First 1
    if (!$installer -or $installer.loaderVersion -ne $target.loaderVersion) { throw "Installer pin differs for $version" }
    $javaHome = if ($version -eq '1.16.5') { $Java8Home } elseif ($version -eq '1.21.1') { $Java21Home } else { $Java17Home }
    $java = Join-Path $javaHome 'bin/java.exe'
    if (!(Test-Path -LiteralPath $java)) { throw "Matching JDK executable missing: $java" }
    $directory = Join-Path $ResultsDirectory ($version + '-' + $target.loader + '-' + $Profile)
    $preparedFile = Join-Path $directory 'prepared.json'
    $productionJar = Join-Path $ProductionDirectory "dragonloot-$ModVersion-$version-$($target.loader).jar"
    if ($Phase -in @('Prepare','Both')) {
        $setupFile = Join-Path $directory 'setup.json'
        if ((Test-Path -LiteralPath $directory) -and @(Get-ChildItem -LiteralPath $directory -Force).Count -gt 0 -and !(Test-Path -LiteralPath $setupFile)) {
            throw "Refusing to prepare an existing directory without this harness's marker: $directory"
        }
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
        if (Test-Path -LiteralPath $setupFile) {
            $existing = Get-Content -LiteralPath $setupFile -Raw | ConvertFrom-Json
            if ($existing.minecraft -ne $version -or $existing.loaderVersion -ne $target.loaderVersion -or $existing.profile -ne $Profile) {
                throw 'Existing setup marker does not match the requested isolated server.'
            }
        } else {
            Write-JsonFile $setupFile ([ordered]@{minecraft=$version;loader=$target.loader;loaderVersion=$target.loaderVersion;profile=$Profile})
        }
        $installerPath = Join-Path (Join-Path $AssetRoot 'installers') $installer.filename
        if ((Get-FileHash -LiteralPath $installerPath -Algorithm SHA1).Hash.ToLowerInvariant() -ne $installer.sha1) {
            throw "Official installer SHA-1 mismatch: $installerPath"
        }
        Write-Output "Preparing pinned $version $($target.loader) $($target.loaderVersion) in $directory"
        & py -3 (Join-Path $PSScriptRoot 'seed-server-libraries.py') --installer $installerPath `
            --installer-sha1 $installer.sha1 --server-dir $directory
        if ($LASTEXITCODE -ne 0) { throw "Server library verification failed for $version" }
        $installation = Invoke-RecordedJava $java @('-Xmx1G','-Djava.awt.headless=true','-jar',$installerPath,'--installServer',$directory) `
            $directory (Join-Path $directory 'installer') '1G' $false
        Write-JsonFile (Join-Path $directory 'installer-result.json') $installation
        if ($installation.exitCode -ne 0 -or $installation.timedOut) { throw "Pinned installer failed for $version" }
        $launchArgs = @('-Xmx2G','-Xms512M','-Djava.net.preferIPv4Stack=true','-Dforge.logging.console.level=info')
        if ($version -eq '1.16.5') {
            $launcher = Join-Path $directory "forge-$version-$($target.loaderVersion).jar"
            if (!(Test-Path -LiteralPath $launcher)) { throw 'Forge server launcher was not generated.' }
            $launchArgs += @('-jar',$launcher,'nogui')
        } else {
            $argumentFile = if ($target.loader -eq 'neoforge') {
                "libraries/net/neoforged/neoforge/$($target.loaderVersion)/win_args.txt"
            } else {
                "libraries/net/minecraftforge/forge/$version-$($target.loaderVersion)/win_args.txt"
            }
            if (!(Test-Path -LiteralPath (Join-Path $directory $argumentFile))) { throw 'Installer server arguments were not generated.' }
            $launchArgs += @("@$argumentFile",'nogui')
        }
        $modsDirectory = Join-Path $directory 'mods'
        New-Item -ItemType Directory -Path $modsDirectory -Force | Out-Null
        if (!(Test-Path -LiteralPath $productionJar)) { throw "Packaged DragonLoot JAR is missing: $productionJar" }
        $dragonHash = (Get-FileHash -LiteralPath $productionJar -Algorithm SHA256).Hash.ToLowerInvariant()
        Copy-Item -LiteralPath $productionJar -Destination (Join-Path $modsDirectory ([IO.Path]::GetFileName($productionJar)))
        $mods = @([ordered]@{id='dragonloot';version=$ModVersion;filename=[IO.Path]::GetFileName($productionJar);sha256=$dragonHash})
        if ($Profile -eq 'all') {
            $selectedProfile = $manifest.profiles | Where-Object { $_.minecraft -eq $version -and $_.loader -eq $target.loader -and $_.profile -eq 'all' } | Select-Object -First 1
            if (!$selectedProfile) { throw 'Pinned all-mods profile is missing.' }
            foreach ($id in $selectedProfile.mods) {
                $mod = $manifest.mods | Where-Object { $_.minecraft -eq $version -and $_.loader -eq $target.loader -and $_.versionId -eq $id } | Select-Object -First 1
                if (!$mod) { throw "Pinned mod identity is missing: $id" }
                $source = Join-Path $AssetRoot $mod.relativePath
                if ((Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant() -ne $mod.sha256) {
                    throw "Pinned production mod hash differs: $($mod.filename)"
                }
                Copy-Item -LiteralPath $source -Destination (Join-Path $modsDirectory $mod.filename)
                $mods += [ordered]@{id=$mod.modId;version=$mod.modVersion;filename=$mod.filename;sha256=$mod.sha256}
            }
        }
        $prepared = [ordered]@{minecraft=$version;loader=$target.loader;loaderVersion=$target.loaderVersion;
            profile=$Profile;java=$java;directory=$directory;arguments=$launchArgs;installer=$installer;
            sourceDragonLoot=$productionJar;dragonLootSha256=$dragonHash;mods=$mods}
        Write-JsonFile $preparedFile $prepared
        Write-Output "Prepared production server: $version ($($mods.Count) packaged mods)."
    }
    if ($Phase -in @('Run','Both')) {
        $availableRam = (Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1MB
        if ($availableRam -lt 4) { throw 'Defer server launch: less than 4 GiB RAM free.' }
        if (!(Test-Path -LiteralPath $preparedFile)) { throw "Prepare this isolated server first: $directory" }
        $prepared = Get-Content -LiteralPath $preparedFile -Raw | ConvertFrom-Json
        if ($prepared.minecraft -ne $version -or $prepared.loaderVersion -ne $target.loaderVersion -or $prepared.profile -ne $Profile) {
            throw 'Prepared snapshot differs from the requested pinned server.'
        }
        $preparedDragonLoot = @($prepared.mods | Where-Object { $_.id -eq 'dragonloot' })
        if ($preparedDragonLoot.Count -ne 1 -or $preparedDragonLoot[0].version -ne $ModVersion -or
            [IO.Path]::GetFullPath($prepared.sourceDragonLoot) -ne $productionJar) {
            throw 'Prepared DragonLoot version/path differs; use the same ModVersion and ProductionDirectory as Prepare.'
        }
        if ((Get-FileHash -LiteralPath $prepared.sourceDragonLoot -Algorithm SHA256).Hash.ToLowerInvariant() -ne $prepared.dragonLootSha256) {
            throw 'The release JAR changed after preparation; prepare a fresh snapshot before running.'
        }
        foreach ($mod in $prepared.mods) {
            if ((Get-FileHash -LiteralPath (Join-Path (Join-Path $directory 'mods') $mod.filename) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $mod.sha256) {
                throw "Prepared production mod hash changed: $($mod.filename)"
            }
        }
        $worldName = 'world-' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')
        $levelType = if ($version -eq '1.16.5') { 'flat' } else { 'minecraft:flat' }
        $allowFlightValue = ([bool]$AllowFlight).ToString().ToLowerInvariant()
        $properties = @"
online-mode=false
allow-flight=$allowFlightValue
server-ip=127.0.0.1
server-port=$ServerPort
enable-query=false
enable-rcon=false
enforce-secure-profile=false
view-distance=2
simulation-distance=2
max-tick-time=-1
spawn-protection=0
generate-structures=false
level-name=$worldName
level-type=$levelType
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains","structures":{"structures":{}},"structure_overrides":[]}
"@
        [IO.File]::WriteAllText((Join-Path $directory 'server.properties'), $properties, $utf8)
        [IO.File]::WriteAllText((Join-Path $directory 'eula.txt'), "eula=true`r`n", $utf8)
        if ($HoldReady) { [IO.File]::WriteAllText((Join-Path $directory 'server-commands.txt'), '', $utf8) }
        Write-Output "Starting packaged $version server on localhost port $ServerPort (0 selects an ephemeral port)."
        $runtime = Invoke-RecordedJava $prepared.java ([string[]]$prepared.arguments) $directory `
            (Join-Path $directory 'server') '2G' $true
        $console = [IO.File]::ReadAllText((Join-Path $directory 'server.stdout.log')) + [IO.File]::ReadAllText((Join-Path $directory 'server.stderr.log'))
        $shutdown = $console -match 'Stopping server' -and $console -match '(All dimensions are saved|All chunks are saved|Saving chunks for level)'
        $runtime['minecraft'] = $version
        $runtime['loader'] = $target.loader
        $runtime['loaderVersion'] = $target.loaderVersion
        $runtime['profile'] = $Profile
        $runtime['dragonLootSha256'] = $prepared.dragonLootSha256
        $runtime['mods'] = $prepared.mods
        $runtime['world'] = $worldName
        $runtime['serverPort'] = $ServerPort
        $runtime['heldForClient'] = [bool]$HoldReady
        $runtime['operatorName'] = $OperatorName
        $runtime['allowFlight'] = [bool]$AllowFlight
        $runtime['savedShutdown'] = $shutdown
        $runtime['scope'] = 'Packaged server startup, loaded versions and saved shutdown; client gameplay assertions are recorded separately.'
        $runtime['playerJoinObserved'] = $console -match ': [A-Za-z0-9_]+ joined the game'
        $runtime['floatingKickObserved'] = $console -match 'was kicked for floating too long!'
        $runtime['loadedVersions'] = Get-LoadedModEvidence $directory $prepared
        $runtime['passed'] = $runtime.exitCode -eq 0 -and $runtime.done -and $runtime.stopSent -and $shutdown -and !$runtime.timedOut -and $runtime.loadedVersions.verified
        Write-JsonFile (Join-Path $directory 'runtime-result.json') $runtime
        if (!$runtime.passed) { $failures++; Write-Output "Packaged server FAILED: $version (exit $($runtime.exitCode), Done=$($runtime.done))." }
        else { Write-Output "Packaged server PASSED: $version (Done, datapack/list/stop, saved shutdown, exit 0)." }
    }
}
Write-Output "Recorded isolated production server evidence in $ResultsDirectory"
if ($failures -gt 0) { exit 1 }
