param(
    [Parameter(Mandatory=$true)][ValidateSet('1.16.5','1.18.2','1.19.2','1.20.1','1.21.1')][string]$Minecraft,
    [string]$Profile = 'all',
    [Parameter(Mandatory=$true)][string]$LogPrefix,
    [string]$ManifestPath = ''
)
$ErrorActionPreference = 'Stop'
if (!$ManifestPath) { $ManifestPath = Join-Path $PSScriptRoot 'manifest.json' }
$manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
$selection = @($manifest.profiles | Where-Object { $_.minecraft -eq $Minecraft -and $_.profile -eq $Profile })[0]
if (!$selection) { throw 'No such pinned compatibility profile.' }
$mods = @($selection.mods | ForEach-Object {
    $versionId = $_
    @($manifest.mods | Where-Object { $_.versionId -eq $versionId -and $_.minecraft -eq $Minecraft })[0]
})
$LogPrefix = [IO.Path]::GetFullPath($LogPrefix)
$exitSuffix = if ($selection.loader -eq 'neoforge') { '.gradle.exitcode' } else { '.exitcode' }
$nativeExit = [int](Get-Content -LiteralPath ($LogPrefix + $exitSuffix) -Raw).Trim()
if ($nativeExit -ne 0) { throw "Native execution failed: $nativeExit" }
$buildExit = [int](Get-Content -LiteralPath ($LogPrefix + '.gradle.exitcode') -Raw).Trim()
if ($buildExit -ne 0) { throw "Gradle preparation failed: $buildExit" }
$logSuffix = if ($selection.loader -eq 'neoforge') { '.gradle.log' } else { '.stdout.log' }
$log = Get-Content -LiteralPath ($LogPrefix + $logSuffix) -Raw
foreach ($mod in $mods) {
    $marker = 'DRAGONLOOT_COMPAT_LOADED ' + $mod.modId + '=' + $mod.modVersion
    $pattern = 'DRAGONLOOT_COMPAT_LOADED:?\s+' + [regex]::Escape($mod.modId + '=' + $mod.modVersion) + '(?:\s|$)'
    if (![regex]::IsMatch($log, $pattern)) { throw ('Missing actual runtime version assertion: ' + $marker) }
}
$externalProviders = @()
if ($Minecraft -eq '1.16.5') {
    $reportFile = $LogPrefix + '.smoke-report.json'
    $report = Get-Content -LiteralPath $reportFile -Raw | ConvertFrom-Json
    if ($report.passed -ne $true -or $report.failures -ne 0 -or @($report.tests | Where-Object { $_.passed -ne $true }).Count) {
        throw 'The smoke report contains failed assertions despite normal Java shutdown.'
    }
    if ($report.compatibilityProfile -ne $Profile -or $report.tests.Count -ne 7) {
        throw 'The smoke report does not contain the expected seven applicable profile cases.'
    }
    $caseCount = $report.tests.Count
    $reportGuard = $true
} else {
    $baselineCases = if ($Minecraft -eq '1.21.1') { 24 } else { 23 }
    $expectedCases = $baselineCases + 1
    if ($mods.modId -contains 'bettercombat') {
        $expectedCases++
        foreach ($weapon in @('sword','axe','trident')) {
            if (!$log.Contains('DRAGONLOOT_BETTER_COMBAT ' + $weapon + ' category=' + $weapon)) {
                throw ('Missing real Better Combat weapon-registry assertion: ' + $weapon)
            }
        }
    }
    foreach ($provider in @('enigmaticlegacy','icarus')) {
        if ($mods.modId -contains $provider) {
            $expectedCases++
            $item = if ($provider -eq 'icarus') { 'icarus:white_feathered_wings' } else { 'enigmaticlegacy:enigmatic_elytra' }
            foreach ($control in @('empty','vanilla','dragon')) {
                foreach ($event in @('DRAGONLOOT_EXTERNAL_FLIGHT','DRAGONLOOT_EXTERNAL_FLIGHT_REMOVED')) {
                    if (!$log.Contains($event + ' item=' + $item + ' chest=' + $control + ' ')) {
                        throw ('Missing actual external flight/removal assertion: ' + $event + ' ' + $item + ' ' + $control)
                    }
                }
            }
            $externalProviders += $provider
        }
    }
    $matches = [regex]::Matches($log, 'All ([0-9]+) required tests passed')
    if (!$matches.Count) { throw 'No successful required-GameTest summary was captured.' }
    $caseCount = [int]$matches[$matches.Count - 1].Groups[1].Value
    if ($caseCount -ne $expectedCases) {
        throw "Expected $expectedCases required cases, captured $caseCount. Review suite changes before changing this guard."
    }
    if ($log -match 'LogTestReporter[^\r\n]* failed!|[1-9][0-9]* required tests failed') {
        throw 'The successful capture also contains a failed required-test marker.'
    }
    $reportGuard = $false
}
[pscustomobject]@{
    minecraft = $Minecraft
    loader = $selection.loader
    loaderVersion = $selection.loaderVersion
    profile = $Profile
    status = 'PASS'
    requiredPassed = $caseCount
    requiredTotal = $caseCount
    nativeExitCode = $nativeExit
    gradleExitCode = $buildExit
    smokeJsonReportVerified = $reportGuard
    logPrefix = $LogPrefix
    loadedMods = @($mods | ForEach-Object { [pscustomobject]@{ id=$_.modId; version=$_.modVersion; sha256=$_.sha256 } })
    externalCuriosProviders = @($externalProviders)
    externalChestControls = if ($externalProviders.Count) { @('empty','vanilla','dragon') } else { @() }
}
