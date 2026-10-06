[CmdletBinding()]
param(
    [ValidateSet('1.18.2', '1.19.2', '1.20.1', '1.21.1')][string[]]$Versions = @('1.18.2', '1.19.2', '1.20.1', '1.21.1'),
    [string]$Java17Home = 'C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot',
    [string]$Java21Home = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot',
    [string]$ResultsDirectory = ''
)
$ErrorActionPreference = 'Stop'
$repositoryDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (!$ResultsDirectory) { $ResultsDirectory = Join-Path $repositoryDirectory 'release/test-results/2026-10-06/config-profiles' }
$ResultsDirectory = [IO.Path]::GetFullPath($ResultsDirectory)
New-Item -ItemType Directory -Path $ResultsDirectory -Force | Out-Null
$runStamp = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')
$modules = @{
    '1.18.2' = 'DragonLoot-1.18.2-forge/DragonLoot-1.18'
    '1.19.2' = 'DragonLoot-1.19.2-forge/DragonLoot-1.19'
    '1.20.1' = 'DragonLoot-1.20.1-forge/DragonLoot-1.20'
    '1.21.1' = 'DragonLoot-1.21.1-neoforge/DragonLoot-1.21'
}
$manifest = New-Object System.Collections.Generic.List[object]
$firstConfig = @'
[dragonloot]
scale_minimum_drop_amount = 5
[dragonloot.armor]
dragon_armor_protection_chest = 17
dragon_armor_protection_horse = 23
dragon_armor_toughness = 6.0
dragon_armor_durability_multiplier = 50
[dragonloot.tools]
dragon_item_durability_multiplier = 60
dragon_item_base_damage = 25.0
dragon_tool_mining_speed = 41.0
dragon_tool_enchantability = 70
'@
$restartConfig = @'
[dragonloot]
scale_minimum_drop_amount = 6
[dragonloot.armor]
dragon_armor_protection_chest = 19
dragon_armor_protection_horse = 25
dragon_armor_toughness = 7.0
dragon_armor_durability_multiplier = 51
[dragonloot.tools]
dragon_item_durability_multiplier = 61
dragon_item_base_damage = 29.0
dragon_tool_mining_speed = 43.0
dragon_tool_enchantability = 80
'@

function Quote-NativeArgument([string]$argument) {
    $quoted = [regex]::Replace($argument, '(\\*)"', '$1$1\"')
    $quoted = [regex]::Replace($quoted, '(\\+)$', '$1$1')
    return '"' + $quoted + '"'
}

function Invoke-IsolatedJava([string]$java, [string[]]$arguments, [string]$directory, [string]$logPrefix, [hashtable]$environment) {
    if ([string]::IsNullOrWhiteSpace($logPrefix)) { throw 'An absolute, named log prefix is required before launching Java.' }
    $logRoot = [IO.Path]::GetPathRoot($logPrefix)
    if ($logRoot.Length -le 1 -or $logRoot.EndsWith(':') -or [string]::IsNullOrWhiteSpace([IO.Path]::GetFileName($logPrefix))) {
        throw 'An absolute, named log prefix is required before launching Java.'
    }
    if (![IO.Directory]::Exists($directory) -or !$arguments.Count) { throw 'An existing working directory and Java arguments are required.' }
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $java
    # Both supported JDKs accept argument files, avoiding Windows command-line limits.
    $argumentFile = [IO.Path]::GetFullPath("$logPrefix.arguments")
    $encoded = $arguments | ForEach-Object { '"' + $_.Replace('\', '\\').Replace('"', '\"') + '"' }
    [IO.File]::WriteAllLines($argumentFile, [string[]]$encoded, (New-Object Text.UTF8Encoding($false)))
    $startInfo.Arguments = Quote-NativeArgument "@$argumentFile"
    $startInfo.WorkingDirectory = $directory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    foreach ($key in $environment.Keys) { $startInfo.EnvironmentVariables[$key] = [string]$environment[$key] }
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    $process.Start() | Out-Null
    $output = $process.StandardOutput.ReadToEndAsync()
    $errorOutput = $process.StandardError.ReadToEndAsync()
    $timedOut = !$process.WaitForExit(600000)
    if ($timedOut) {
        $process.Kill()
        $process.WaitForExit()
    }
    [IO.File]::WriteAllText("$logPrefix.stdout.log", $output.Result)
    [IO.File]::WriteAllText("$logPrefix.stderr.log", $errorOutput.Result)
    $code = $process.ExitCode
    [IO.File]::WriteAllText("$logPrefix.exit-code.txt", [string]$code)
    $process.Dispose()
    if ($timedOut) { throw "This isolated JVM exceeded its ten-minute timeout: $logPrefix" }
    return $code
}

function Write-Expectations([string]$directory, [string]$version, [bool]$restarted) {
    $armorBase = if ($version -eq '1.21.1') { 16 } else { 35 }
    $expected = if ($restarted) {
        "speed=43`nswordModifier=32`ntoolDurability=4087`nchestDurability=$($armorBase * 51)`nchestArmor=19`ntoughness=7`nhorseArmor=25`ntoolEnchantability=80`nscales=6`n"
    } else {
        "speed=41`nswordModifier=28`ntoolDurability=4020`nchestDurability=$($armorBase * 50)`nchestArmor=17`ntoughness=6`nhorseArmor=23`ntoolEnchantability=70`nscales=5`n"
    }
    [IO.File]::WriteAllText((Join-Path $directory 'gear-expectations.properties'), $expected)
}

foreach ($version in $Versions) {
    $moduleDirectory = Join-Path $repositoryDirectory $modules[$version]
    $javaHome = if ($version -eq '1.21.1') { $Java21Home } else { $Java17Home }
    $java = Join-Path $javaHome 'bin/java.exe'
    if (!(Test-Path -LiteralPath $java)) { throw "JDK not found: $java" }
    $runDirectory = Join-Path $moduleDirectory "run/gear-config-profiles/$runStamp"
    $defaultDirectory = Join-Path $runDirectory 'defaultconfigs'
    New-Item -ItemType Directory -Path $defaultDirectory -Force | Out-Null
    [IO.File]::WriteAllText((Join-Path $runDirectory 'eula.txt'), "eula=true`n")
    [IO.File]::WriteAllText((Join-Path $runDirectory 'server.properties'), "server-ip=127.0.0.1`nserver-port=0`nview-distance=2`n")
    [IO.File]::WriteAllText((Join-Path $defaultDirectory 'dragonloot-common.toml'), $firstConfig)
    if (Test-Path -LiteralPath (Join-Path $runDirectory 'config/dragonloot-common.toml')) {
        throw "Fresh first-run profile already has a config: $runDirectory"
    }
    foreach ($profile in @('defaultconfigs-first-run', 'full-restart')) {
        $restarted = $profile -eq 'full-restart'
        # The config directory persists across launches; each GameTest world is fresh
        # so saved test structures cannot obstruct a later sky-access fixture.
        [IO.File]::WriteAllText((Join-Path $runDirectory 'server.properties'), "server-ip=127.0.0.1`nserver-port=0`nview-distance=2`nlevel-name=world-$profile`n")
        if ($restarted) {
            if (!(Test-Path -LiteralPath (Join-Path $runDirectory 'config/dragonloot-common.toml'))) {
                throw 'First run did not create the managed configuration.'
            }
            [IO.File]::WriteAllText((Join-Path $runDirectory 'config/dragonloot-common.toml'), $restartConfig)
        }
        Write-Expectations $runDirectory $version $restarted
        $profileDirectory = Join-Path $ResultsDirectory "$version/$profile"
        if (Test-Path -LiteralPath $profileDirectory) {
            $archiveDirectory = Join-Path $ResultsDirectory "history/$runStamp/$version/$profile"
            New-Item -ItemType Directory -Path $archiveDirectory -Force | Out-Null
            Get-ChildItem -LiteralPath $profileDirectory | Copy-Item -Destination $archiveDirectory -Recurse
        }
        New-Item -ItemType Directory -Path $profileDirectory -Force | Out-Null
        # Mark this attempt unsuccessful until its own server output proves otherwise.
        [ordered]@{ minecraft = $version; profile = $profile; passed = $false; stage = 'preparing' } |
            ConvertTo-Json | Set-Content -LiteralPath (Join-Path $profileDirectory 'report.json') -Encoding UTF8
        $launchFile = Join-Path $profileDirectory 'launch.json'
        if (Test-Path -LiteralPath $launchFile) {
            Move-Item -LiteralPath $launchFile -Destination (Join-Path $profileDirectory 'previous-launch.json') -Force
        }
        Write-Host "Preparing $version / $profile"
        $prepareArguments = @('-Xmx64m', '-Dorg.gradle.appname=gradlew', '-classpath',
            (Join-Path $moduleDirectory 'gradle/wrapper/gradle-wrapper.jar'),
            'org.gradle.wrapper.GradleWrapperMain', '--no-daemon', '--max-workers=1',
            '-Dorg.gradle.jvmargs=-Xmx768M', '-I', (Join-Path $PSScriptRoot 'gear-config-profiles.init.gradle'),
            '-PdragonlootGearProfiles=true', "-PgearRunDirectory=$runDirectory",
            "-PgearLaunchOutput=$launchFile", 'writeGearProfileLaunch')
        $prepareExit = Invoke-IsolatedJava $java $prepareArguments $moduleDirectory (Join-Path $profileDirectory 'prepare') @{ JAVA_HOME = $javaHome }
        if ($prepareExit -ne 0) { throw "Preparing $version / $profile failed (exit $prepareExit)." }
        $launch = Get-Content -LiteralPath $launchFile -Raw | ConvertFrom-Json
        if ([IO.Path]::GetFullPath($launch.workingDirectory) -ine [IO.Path]::GetFullPath($runDirectory)) {
            throw "Exporter selected an unexpected configuration/world directory: $($launch.workingDirectory)"
        }
        $previousLog = Join-Path $runDirectory 'logs/latest.log'
        if (Test-Path -LiteralPath $previousLog) {
            Move-Item -LiteralPath $previousLog -Destination (Join-Path $profileDirectory 'previous-run-latest.log') -Force
        }
        [ordered]@{ minecraft = $version; profile = $profile; passed = $false; stage = 'running' } |
            ConvertTo-Json | Set-Content -LiteralPath (Join-Path $profileDirectory 'report.json') -Encoding UTF8
        $environment = @{}
        foreach ($entry in $launch.environment.PSObject.Properties) { $environment[$entry.Name] = $entry.Value }
        $arguments = @($launch.jvmArgs) + @('-classpath', $launch.classpath, $launch.mainClass) + @($launch.args)
        Write-Host "Running real GameTest server: $version / $profile"
        $exitCode = Invoke-IsolatedJava $java $arguments $launch.workingDirectory (Join-Path $profileDirectory 'server') $environment
        [IO.File]::WriteAllText((Join-Path $profileDirectory 'exit-code.txt'), [string]$exitCode)
        $missingArtifacts = New-Object System.Collections.Generic.List[string]
        foreach ($relativeFile in @('gear-expectations.properties', 'config/dragonloot-common.toml', 'server.properties', 'logs/latest.log')) {
            $artifact = Join-Path $runDirectory $relativeFile
            if (Test-Path -LiteralPath $artifact) {
                Copy-Item -LiteralPath $artifact -Destination $profileDirectory
            } else {
                $missingArtifacts.Add($relativeFile)
            }
        }
        $log = if (Test-Path -LiteralPath (Join-Path $runDirectory 'logs/latest.log')) {
            [IO.File]::ReadAllText((Join-Path $runDirectory 'logs/latest.log'))
        } else { '' }
        $summary = [regex]::Match($log, 'All (\d+) required tests passed')
        $defaultPreserved = [IO.File]::ReadAllText((Join-Path $defaultDirectory 'dragonloot-common.toml')) -ceq $firstConfig
        $passed = $exitCode -eq 0 -and $summary.Success -and [int]$summary.Groups[1].Value -ge 4 -and
            $missingArtifacts.Count -eq 0 -and $defaultPreserved
        $result = [ordered]@{
            minecraft = $version
            profile = $profile
            passed = $passed
            jvmExitCode = $exitCode
            requiredTestsPassed = if ($summary.Success) { [int]$summary.Groups[1].Value } else { 0 }
            workingDirectory = $runDirectory
            testWorld = "world-$profile"
            missingArtifacts = $missingArtifacts.ToArray()
            expectations = [IO.File]::ReadAllText((Join-Path $runDirectory 'gear-expectations.properties'))
            originalDefaultConfigPreserved = $defaultPreserved
            sourceSha256 = (Get-FileHash -LiteralPath (Join-Path $moduleDirectory 'src/gametest/java/net/dragonloot/item/DragonGearGameTests.java') -Algorithm SHA256).Hash
        }
        $manifest.Add($result)
        $result | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $profileDirectory 'report.json') -Encoding UTF8
        $manifest.ToArray() | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $ResultsDirectory "manifest-$runStamp.json") -Encoding UTF8
        if (!$passed) { throw "Runtime $version / $profile failed: JVM exit $exitCode; inspect $profileDirectory." }
        Write-Host "PASS: $version / $profile ($($result.requiredTestsPassed) required tests, JVM exit0)"
    }
}
Write-Host "Requested configuration profiles passed. Reports: $ResultsDirectory"
