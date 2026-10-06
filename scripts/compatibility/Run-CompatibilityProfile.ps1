param(
    [Parameter(Mandatory=$true)][ValidateSet('1.16.5','1.18.2','1.19.2','1.20.1','1.21.1')][string]$Minecraft,
    [ValidateSet('caelus','better-combat','advanced-netherite','icarus','enigmatic-legacy','all')][string]$Profile = 'all',
    [string]$JavaHome = '',
    [string]$ModVersion = '1.1.16-dev',
    [string]$AssetRoot = ''
)
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if (!$AssetRoot) { $AssetRoot = Join-Path $repoRoot 'release/test-results/2026-10-06/compatibility' }
$AssetRoot = [IO.Path]::GetFullPath($AssetRoot)
$manifest = Get-Content -LiteralPath (Join-Path $AssetRoot 'manifest.json') -Raw | ConvertFrom-Json
$target = @($manifest.targets | Where-Object { $_.minecraft -eq $Minecraft })[0]
$profileEntry = @($manifest.profiles | Where-Object { $_.minecraft -eq $Minecraft -and $_.profile -eq $Profile })[0]
if (!$target -or !$profileEntry) { throw 'Prepare an available official compatibility profile first.' }
$moduleNames = @{
    '1.16.5'='DragonLoot-1.16.5-forge/DragonLoot-1.16'
    '1.18.2'='DragonLoot-1.18.2-forge/DragonLoot-1.18'
    '1.19.2'='DragonLoot-1.19.2-forge/DragonLoot-1.19'
    '1.20.1'='DragonLoot-1.20.1-forge/DragonLoot-1.20'
    '1.21.1'='DragonLoot-1.21.1-neoforge/DragonLoot-1.21'
}
if (!$JavaHome) {
    $javaDirectory = if ($Minecraft -eq '1.16.5') { 'jdk-8.0.504.1-hotspot' }
        elseif ($Minecraft -eq '1.21.1') { 'jdk-21.0.12.101-hotspot' }
        else { 'jdk-17.0.20.101-hotspot' }
    $JavaHome = Join-Path 'C:/Program Files/Eclipse Adoptium' $javaDirectory
}
if (!(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) { throw 'Supply the matching Java 8/17/21 JDK path.' }
$availableRam = (Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1MB
if ($availableRam -lt 4) { throw 'Defer build/server launch: less than 4 GiB RAM free.' }
$stamp = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')
$logPrefix = Join-Path $AssetRoot ($Minecraft + '-' + $Profile + '-' + $stamp)
$moduleDirectory = Join-Path $repoRoot $moduleNames[$Minecraft]
$lockDirectory = Join-Path $AssetRoot 'locks'
New-Item -ItemType Directory -Path $lockDirectory -Force | Out-Null
try {
    $moduleLock = [IO.File]::Open((Join-Path $lockDirectory ($Minecraft + '.lock')),
        [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
} catch { throw ('Another compatibility runner owns Minecraft ' + $Minecraft + '. Wait for it to finish.') }
try {
$launchFile = $logPrefix + '.launch.json'
$arguments = @('--no-daemon','--max-workers=1','-Dorg.gradle.jvmargs=-Xmx1G',
    "-Pmod_version=$ModVersion", '-I', (Join-Path $PSScriptRoot 'compatibility.init.gradle'),
    "-PdragonlootCompatProfile=$Profile", "-PdragonlootCompatRoot=$AssetRoot", "-PdragonlootCompatRunId=$stamp")
if ($Minecraft -eq '1.16.5') {
    $arguments += @('-PsmokeCase=compatibility', 'writeSmokeTestLaunch')
    $workingDirectory = Join-Path $AssetRoot ("runs/$Minecraft-forge/$Profile/$stamp/smokeTest")
    New-Item -ItemType Directory -Path $workingDirectory -Force | Out-Null
    [IO.File]::WriteAllText((Join-Path $workingDirectory 'eula.txt'), "eula=true`n")
    [IO.File]::WriteAllText((Join-Path $workingDirectory 'server.properties'), @'
online-mode=false
server-ip=127.0.0.1
server-port=0
level-type=flat
generator-settings={"structures":{"structures":{}},"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
generate-structures=false
spawn-animals=false
spawn-monsters=false
view-distance=2
max-tick-time=120000
'@)
    [IO.File]::WriteAllText((Join-Path $workingDirectory 'smoke-expectations.properties'), @'
speed=12
swordModifier=8
toolDurability=2479
chestDurability=1295
chestArmor=10
toughness=3
horseArmor=18
toolEnchantability=20
scales=3
'@)
} elseif ($target.loader -eq 'forge') {
    $arguments += @('-I', (Join-Path $PSScriptRoot 'export-gametest-launch.gradle'),
        "-PlaunchOutput=$launchFile", 'writeGameTestLaunch')
} else {
    $arguments += @('-I', (Join-Path $PSScriptRoot 'headless-server.init.gradle'),
        '-PdragonlootHeadlessServer=true','runGameTestServer')
}
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome/bin;$env:PATH"
Push-Location $moduleDirectory
try {
    # Windows PowerShell represents native stderr warnings as ErrorRecords.
    # The native process exit code, rather than harmless compiler warnings, decides success.
    $nativePolicy = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & .\gradlew.bat @arguments *> ($logPrefix + '.gradle.log')
        $buildExit = $LASTEXITCODE
    } finally { $ErrorActionPreference = $nativePolicy }
    Set-Content -LiteralPath ($logPrefix + '.gradle.exitcode') -Value $buildExit
    if ($buildExit -ne 0) { throw ('Gradle preparation/server failed; inspect ' + $logPrefix + '.gradle.log') }
    if ($Minecraft -eq '1.16.5') {
        Copy-Item -LiteralPath 'build/smoke-test-launch.json' -Destination $launchFile
        & (Join-Path $PSScriptRoot 'Start-DirectSmoke.ps1') -LaunchFile $launchFile -Java8Home $JavaHome -LogPrefix $logPrefix
        if ($LASTEXITCODE -ne 0) { throw 'The standalone smoke JVM failed.' }
        $smokeReport = Get-Content -LiteralPath (Join-Path $workingDirectory 'smoke-report.json') -Raw | ConvertFrom-Json
        Copy-Item -LiteralPath (Join-Path $workingDirectory 'smoke-report.json') -Destination ($logPrefix + '.smoke-report.json')
        if ($smokeReport.passed -ne $true -or $smokeReport.failures -ne 0) {
            throw ('Smoke assertions failed despite normal server shutdown; inspect ' + $logPrefix + '.smoke-report.json')
        }
    } elseif ($target.loader -eq 'forge') {
        & (Join-Path $PSScriptRoot 'Start-DirectGameTest.ps1') -LaunchFile $launchFile -LogPrefix $logPrefix
        if ($LASTEXITCODE -ne 0) { throw 'The standalone GameTest JVM failed.' }
    }
} finally { Pop-Location }
$verifiedResult = & (Join-Path $PSScriptRoot 'Get-CompatibilityResult.ps1') -Minecraft $Minecraft -Profile $Profile `
    -LogPrefix $logPrefix -ManifestPath (Join-Path $AssetRoot 'manifest.json')
$verifiedResult | ConvertTo-Json -Depth 7 | Set-Content -LiteralPath ($logPrefix + '.verified-result.json') -Encoding UTF8
Write-Output ('Completed profile. Verify required-test counts and loaded pins in ' + $logPrefix)
} finally { $moduleLock.Dispose() }
