[CmdletBinding()]
param(
    [string]$Java8Home = $env:JAVA_HOME,
    [string]$ResultsDirectory = ''
)
$ErrorActionPreference = 'Stop'
$moduleDirectory = $PSScriptRoot
if (!$ResultsDirectory) { $ResultsDirectory = Join-Path $moduleDirectory '../../release/test-results/2026-10-06/1.16.5' }
$javaExecutable = Join-Path $Java8Home 'bin/java.exe'
if (!(Test-Path -LiteralPath $javaExecutable)) { throw 'Set -Java8Home to a Java 8 JDK.' }
$ResultsDirectory = [System.IO.Path]::GetFullPath($ResultsDirectory)
New-Item -ItemType Directory -Path $ResultsDirectory -Force | Out-Null
$runStamp = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')

function Quote-NativeArgument([string]$argument) {
    # Windows CreateProcess quoting, including embedded quotes and trailing backslashes.
    $quoted = [regex]::Replace($argument, '(\\*)"', '$1$1\"')
    $quoted = [regex]::Replace($quoted, '(\\+)$', '$1$1')
    return '"' + $quoted + '"'
}

function Invoke-IsolatedJava([string[]]$arguments, [string]$workingDirectory, [string]$logPrefix, [hashtable]$environment) {
    if ([string]::IsNullOrWhiteSpace($logPrefix)) { throw 'An absolute, named log prefix is required before launching Java.' }
    $logRoot = [IO.Path]::GetPathRoot($logPrefix)
    if ($logRoot.Length -le 1 -or $logRoot.EndsWith(':') -or [string]::IsNullOrWhiteSpace([IO.Path]::GetFileName($logPrefix))) {
        throw 'An absolute, named log prefix is required before launching Java.'
    }
    if (![IO.Directory]::Exists($workingDirectory) -or !$arguments.Count) { throw 'An existing working directory and Java arguments are required.' }
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $javaExecutable
    $startInfo.Arguments = ($arguments | ForEach-Object { Quote-NativeArgument $_ }) -join ' '
    $startInfo.WorkingDirectory = $workingDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    foreach ($key in $environment.Keys) { $startInfo.EnvironmentVariables[$key] = [string]$environment[$key] }
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    $process.Start() | Out-Null
    $standardOutput = $process.StandardOutput.ReadToEndAsync()
    $standardError = $process.StandardError.ReadToEndAsync()
    $timedOut = !$process.WaitForExit(480000)
    if ($timedOut) {
        # Only this isolated child process is terminated on timeout.
        $process.Kill()
        $process.WaitForExit()
    }
    [System.IO.File]::WriteAllText("$logPrefix.stdout.log", $standardOutput.Result)
    [System.IO.File]::WriteAllText("$logPrefix.stderr.log", $standardError.Result)
    $exitCode = $process.ExitCode
    [System.IO.File]::WriteAllText("$logPrefix.exit-code.txt", [string]$exitCode)
    $process.Dispose()
    if ($timedOut) { throw "Isolated smoke-test JVM timed out: $logPrefix" }
    return $exitCode
}

function Write-TestServer([string]$directory, [string]$expectations) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    [System.IO.File]::WriteAllText((Join-Path $directory 'eula.txt'), "eula=true`n")
    $properties = @'
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
'@
    [System.IO.File]::WriteAllText((Join-Path $directory 'server.properties'), $properties)
    [System.IO.File]::WriteAllText((Join-Path $directory 'smoke-expectations.properties'), $expectations)
}

function Invoke-SmokeProfile([string]$caseName, [string]$profileName) {
    $profileResults = Join-Path $ResultsDirectory $profileName
    if (Test-Path -LiteralPath $profileResults) {
        $archiveDirectory = Join-Path $ResultsDirectory "history/$runStamp/$profileName"
        New-Item -ItemType Directory -Path $archiveDirectory -Force | Out-Null
        Get-ChildItem -LiteralPath $profileResults | Copy-Item -Destination $archiveDirectory -Recurse
    }
    New-Item -ItemType Directory -Path $profileResults -Force | Out-Null
    if (Test-Path -LiteralPath (Join-Path $profileResults 'smoke-report.json')) {
        Move-Item -LiteralPath (Join-Path $profileResults 'smoke-report.json') -Destination (Join-Path $profileResults 'previous-smoke-report.json') -Force
    }
    [ordered]@{ minecraft = '1.16.5'; profile = $profileName; passed = $false; stage = 'preparing' } |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $profileResults 'runner-report.json') -Encoding UTF8
    $launchFile = Join-Path $moduleDirectory 'build/smoke-test-launch.json'
    if (Test-Path -LiteralPath $launchFile) {
        Move-Item -LiteralPath $launchFile -Destination (Join-Path $profileResults 'previous-launch.json') -Force
    }
    Write-Host "Preparing Forge 1.16.5 smoke profile: $profileName"
    $prepareArguments = @('-Xmx64m', '-Dorg.gradle.appname=gradlew', '-classpath',
        (Join-Path $moduleDirectory 'gradle/wrapper/gradle-wrapper.jar'),
        'org.gradle.wrapper.GradleWrapperMain', '--no-daemon', '--max-workers=1',
        '-Dorg.gradle.jvmargs=-Xmx512M', "-PsmokeCase=$caseName", 'writeSmokeTestLaunch')
    $prepareExit = Invoke-IsolatedJava $prepareArguments $moduleDirectory (Join-Path $profileResults 'prepare') @{ JAVA_HOME = $Java8Home }
    if ($prepareExit -ne 0) { throw "Preparing profile $profileName failed with exit $prepareExit." }
    $launch = Get-Content -LiteralPath $launchFile -Raw | ConvertFrom-Json
    $expectedWorkingDirectory = Join-Path $moduleDirectory "run/smokeTest/$caseName"
    if ([IO.Path]::GetFullPath($launch.workingDirectory) -ine [IO.Path]::GetFullPath($expectedWorkingDirectory)) {
        throw "Exporter selected an unexpected configuration/world directory: $($launch.workingDirectory)"
    }
    foreach ($relativeFile in @('smoke-report.json', 'logs/latest.log')) {
        $previousArtifact = Join-Path $launch.workingDirectory $relativeFile
        if (Test-Path -LiteralPath $previousArtifact) {
            Move-Item -LiteralPath $previousArtifact -Destination (Join-Path $profileResults ('previous-run-' + [IO.Path]::GetFileName($relativeFile))) -Force
        }
    }
    [ordered]@{ minecraft = '1.16.5'; profile = $profileName; passed = $false; stage = 'running' } |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $profileResults 'runner-report.json') -Encoding UTF8
    $launchEnvironment = @{}
    foreach ($entry in $launch.environment.PSObject.Properties) { $launchEnvironment[$entry.Name] = $entry.Value }
    $runtimeArguments = @($launch.jvmArgs) + @('-classpath', $launch.classpath, $launch.main) + @($launch.args)
    Write-Host "Starting real dedicated server: $profileName"
    $runtimeExit = Invoke-IsolatedJava $runtimeArguments $launch.workingDirectory (Join-Path $profileResults 'server') $launchEnvironment
    [System.IO.File]::WriteAllText((Join-Path $profileResults 'exit-code.txt'), [string]$runtimeExit)
    foreach ($relativeFile in @('smoke-report.json', 'smoke-expectations.properties', 'server.properties')) {
        Copy-Item -LiteralPath (Join-Path $launch.workingDirectory $relativeFile) -Destination $profileResults
    }
    Copy-Item -LiteralPath (Join-Path $launch.workingDirectory 'logs/latest.log') -Destination $profileResults
    Copy-Item -LiteralPath (Join-Path $launch.workingDirectory 'config/dragonloot-common.toml') -Destination $profileResults
    $report = Get-Content -LiteralPath (Join-Path $profileResults 'smoke-report.json') -Raw | ConvertFrom-Json
    $requiredCases = @('startup-config-and-registered-items', 'trident-constructors',
        'throw-collision-save-loyalty-owner-pickup', 'almost-broken-release-guard',
        'native-winged-flight-gates-and-wear', 'reload-runtime-values-with-frozen-gear')
    $missingCases = @($requiredCases | Where-Object { $_ -notin @($report.tests | Where-Object passed | Select-Object -ExpandProperty name) })
    $defaultPreserved = $profileName -eq 'baseline' -or
        [IO.File]::ReadAllText((Join-Path $launch.workingDirectory 'defaultconfigs/dragonloot-common.toml')) -ceq $customConfig
    $passed = $runtimeExit -eq 0 -and $report.passed -and $report.case -eq $caseName -and
        $missingCases.Count -eq 0 -and $defaultPreserved
    [ordered]@{ minecraft = '1.16.5'; profile = $profileName; passed = $passed; stage = 'finished';
        jvmExitCode = $runtimeExit; missingCases = $missingCases; originalDefaultConfigPreserved = $defaultPreserved } |
        ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $profileResults 'runner-report.json') -Encoding UTF8
    if (!$passed) { throw "Profile $profileName failed: JVM exit $runtimeExit, $($report.failures) assertion failures, missing cases $($missingCases.Count), original default preserved $defaultPreserved." }
    Write-Host "PASS: $profileName ($($report.tests.Count) real-server cases, JVM exit $runtimeExit)"
}

$baselineCase = "baseline-$runStamp"
$baselineDirectory = Join-Path $moduleDirectory "run/smokeTest/$baselineCase"
$baselineExpectations = @'
speed=12
swordModifier=8
toolDurability=2479
chestDurability=1295
chestArmor=10
toughness=3
horseArmor=18
toolEnchantability=20
scales=3
'@
Write-TestServer $baselineDirectory $baselineExpectations
Invoke-SmokeProfile $baselineCase 'baseline'

$customCase = "custom-$runStamp"
$customDirectory = Join-Path $moduleDirectory "run/smokeTest/$customCase"
$customExpectations = @'
speed=41
swordModifier=28
toolDurability=4020
chestDurability=1750
chestArmor=17
toughness=6
horseArmor=23
toolEnchantability=70
scales=5
'@
$customConfig = @'
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
Write-TestServer $customDirectory $customExpectations
$defaultConfigDirectory = Join-Path $customDirectory 'defaultconfigs'
New-Item -ItemType Directory -Path $defaultConfigDirectory -Force | Out-Null
[System.IO.File]::WriteAllText((Join-Path $defaultConfigDirectory 'dragonloot-common.toml'), $customConfig)
if (Test-Path -LiteralPath (Join-Path $customDirectory 'config/dragonloot-common.toml')) {
    throw 'First-run config test requires an absent destination config.'
}
Invoke-SmokeProfile $customCase 'defaultconfigs-first-run'

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
# Keep the original defaultconfigs file. An existing edited config must win after restart.
[System.IO.File]::WriteAllText((Join-Path $customDirectory 'config/dragonloot-common.toml'), $restartConfig)
$restartExpectations = @'
speed=43
swordModifier=32
toolDurability=4087
chestDurability=1785
chestArmor=19
toughness=7
horseArmor=25
toolEnchantability=80
scales=6
'@
[System.IO.File]::WriteAllText((Join-Path $customDirectory 'smoke-expectations.properties'), $restartExpectations)
Invoke-SmokeProfile $customCase 'full-restart'

Write-Host "All three profiles passed. Reports: $ResultsDirectory"
