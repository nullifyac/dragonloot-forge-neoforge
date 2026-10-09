param(
    [ValidateSet('1.16.5', '1.18.2', '1.19.2', '1.20.1', '1.21.1', '26.1.2')]
    [string]$MinecraftVersion = '1.21.1',
    [string]$JavaHome,
    [string]$ModVersion,
    [string[]]$Tasks = @('assemble'),
    [switch]$Offline,
    [switch]$AllVersions
)

$ErrorActionPreference = 'Stop'
$projects = @{
    '1.16.5' = @('DragonLoot-1.16.5-forge/DragonLoot-1.16', 8)
    '1.18.2' = @('DragonLoot-1.18.2-forge/DragonLoot-1.18', 17)
    '1.19.2' = @('DragonLoot-1.19.2-forge/DragonLoot-1.19', 17)
    '1.20.1' = @('DragonLoot-1.20.1-forge/DragonLoot-1.20', 17)
    '1.21.1' = @('DragonLoot-1.21.1-neoforge/DragonLoot-1.21', 21)
    '26.1.2' = @('DragonLoot-26.1.2-neoforge/DragonLoot-26.1.2', 25)
}
if ($AllVersions) {
    if ($JavaHome) { throw '-AllVersions selects the appropriate JDK for each version; omit -JavaHome.' }
    $failed = @()
    foreach ($targetVersion in @('1.16.5', '1.18.2', '1.19.2', '1.20.1', '1.21.1', '26.1.2')) {
        $parameters = @{ MinecraftVersion = $targetVersion; Tasks = $Tasks }
        if ($ModVersion) { $parameters.ModVersion = $ModVersion }
        if ($Offline) { $parameters.Offline = $true }
        try { & $PSCommandPath @parameters } catch {
            $failed += $targetVersion
            Write-Warning "$targetVersion failed: $_"
        }
    }
    if ($failed.Count) { throw "Builds failed for: $($failed -join ', ')." }
    return
}
$project = $projects[$MinecraftVersion]
$repoRoot = Split-Path -Parent $PSScriptRoot

function Test-JavaVersion([string]$Candidate, [int]$Major) {
    if (-not $Candidate) { return $false }
    $javaExecutable = Join-Path $Candidate 'bin/java.exe'
    if (-not (Test-Path -LiteralPath $javaExecutable)) { return $false }
    $releaseFile = Join-Path $Candidate 'release'
    if (-not (Test-Path -LiteralPath $releaseFile)) { return $false }
    $version = Get-Content -LiteralPath $releaseFile -Raw
    $pattern = if ($Major -eq 8) { 'JAVA_VERSION="1\.8\.' } else { 'JAVA_VERSION="' + $Major + '\.' }
    return $version -match $pattern
}

if ($JavaHome) {
    if (-not (Test-JavaVersion $JavaHome $project[1])) {
        throw "Minecraft $MinecraftVersion requires JDK $($project[1]); check -JavaHome."
    }
} else {
    $candidates = @($env:JAVA_HOME)
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) { $candidates += Split-Path -Parent (Split-Path -Parent $javaCommand.Source) }
    $adoptium = Join-Path $env:ProgramFiles 'Eclipse Adoptium'
    if (Test-Path -LiteralPath $adoptium) {
        $candidates += Get-ChildItem -LiteralPath $adoptium -Directory | Select-Object -ExpandProperty FullName
    }
    $userJdks = Join-Path $env:USERPROFILE '.jdks'
    if (Test-Path -LiteralPath $userJdks) {
        $candidates += Get-ChildItem -LiteralPath $userJdks -Directory | Select-Object -ExpandProperty FullName
    }
    $JavaHome = $candidates | Where-Object { Test-JavaVersion $_ $project[1] } | Select-Object -First 1
    if (-not $JavaHome) { throw "Pass -JavaHome with an installed JDK $($project[1])." }
}
if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/javac.exe'))) {
    throw "A full JDK is required: javac.exe is missing from $JavaHome."
}

$originalJavaHome = $env:JAVA_HOME
Push-Location (Join-Path $repoRoot $project[0])
try {
    $env:JAVA_HOME = $JavaHome
    Write-Host "Building Minecraft $MinecraftVersion with JDK $($project[1]), one worker and a 1 GB Gradle heap."
    $gradleArgs = @($Tasks) + @('--no-daemon', '--max-workers=1', '-Dorg.gradle.jvmargs=-Xmx1G -XX:MaxMetaspaceSize=384m', '--console=plain')
    if ($ModVersion) { $gradleArgs += "-Pmod_version=$ModVersion" }
    if ($Offline) { $gradleArgs += '--offline' }
    & .\gradlew.bat @gradleArgs
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE." }
} finally {
    $env:JAVA_HOME = $originalJavaHome
    Pop-Location
}
