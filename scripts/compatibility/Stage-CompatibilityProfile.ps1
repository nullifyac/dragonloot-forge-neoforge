param(
    [Parameter(Mandatory=$true)][ValidateSet('1.16.5','1.18.2','1.19.2','1.20.1','1.21.1')][string]$Minecraft,
    [ValidateSet('caelus','better-combat','advanced-netherite','icarus','enigmatic-legacy','all')][string]$Profile = 'all',
    [string]$AssetRoot = ''
)
$ErrorActionPreference = 'Stop'
if (!$AssetRoot) { $AssetRoot = Join-Path $PSScriptRoot '../../release/test-results/2026-10-06/compatibility' }
$AssetRoot = [IO.Path]::GetFullPath($AssetRoot)
$manifest = Get-Content -LiteralPath (Join-Path $AssetRoot 'manifest.json') -Raw | ConvertFrom-Json
$entry = @($manifest.profiles | Where-Object { $_.minecraft -eq $Minecraft -and $_.profile -eq $Profile })[0]
if (!$entry) { throw ('No matching official profile: ' + $Minecraft + '/' + $Profile) }
$mods = @($manifest.mods | Where-Object {
    $_.minecraft -eq $Minecraft -and $_.loader -eq $entry.loader -and $_.versionId -in $entry.mods
})
if ($mods.Count -ne $entry.mods.Count -or ($Profile -ne 'all' -and $Profile -notin $mods.project)) {
    throw 'Profile selection is incomplete.'
}
$destination = Join-Path $AssetRoot ('staged/' + $Minecraft + '-' + $entry.loader + '/' + $Profile + '/mods')
New-Item -ItemType Directory -Path $destination -Force | Out-Null
$unexpected = @(Get-ChildItem -LiteralPath $destination -File -Filter '*.jar' | Where-Object { $_.Name -notin $mods.filename })
if ($unexpected.Count) { throw ('Unselected artifacts in staging directory: ' + ($unexpected.Name -join ', ')) }
foreach ($mod in $mods) {
    $source = Join-Path $AssetRoot $mod.relativePath
    if ((Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant() -ne $mod.sha256) {
        throw ('Pinned checksum mismatch: ' + $mod.filename)
    }
    Copy-Item -LiteralPath $source -Destination (Join-Path $destination $mod.filename) -Force
}
$entry | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path (Split-Path $destination) 'profile.json') -Encoding UTF8
Write-Output ('Staged ' + $mods.Count + ' verified artifacts in ' + $destination)
