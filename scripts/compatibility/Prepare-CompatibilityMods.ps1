param(
    [string]$AssetRoot = '',
    [string]$ManifestPath = ''
)
$ErrorActionPreference = 'Stop'
if (!$AssetRoot) { $AssetRoot = Join-Path $PSScriptRoot '../../release/test-results/2026-10-06/compatibility' }
if (!$ManifestPath) { $ManifestPath = Join-Path $PSScriptRoot 'manifest.json' }
$AssetRoot = [IO.Path]::GetFullPath($AssetRoot)
$manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
if ($manifest.schemaVersion -ne 1) { throw 'Unsupported compatibility manifest schema.' }

function Asset-Path([string]$relative) {
    $resolved = [IO.Path]::GetFullPath((Join-Path $AssetRoot $relative))
    if (!$resolved.StartsWith($AssetRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar,
            [StringComparison]::OrdinalIgnoreCase)) { throw 'Asset path escapes the isolated directory.' }
    return $resolved
}

foreach ($profile in $manifest.profiles) {
    $mods = @($manifest.mods | Where-Object {
        $_.minecraft -eq $profile.minecraft -and $_.loader -eq $profile.loader -and $_.versionId -in $profile.mods
    })
    if ($mods.Count -ne $profile.mods.Count) { throw ('Missing profile selection: ' + $profile.profile) }
    if ($profile.profile -ne 'all' -and $profile.profile -notin $mods.project) {
        throw ('Profile does not include its requested mod: ' + $profile.profile)
    }
    foreach ($mod in $mods) {
        foreach ($dependency in $mod.requiredVersionIds) {
            if ($dependency -notin $profile.mods) { throw ('Missing required dependency for ' + $mod.project) }
        }
    }
}

foreach ($mod in $manifest.mods) {
    if ([IO.Path]::GetFileName($mod.filename) -ne $mod.filename) { throw 'Invalid pinned filename.' }
    $url = [Uri]$mod.downloadUrl
    if ($url.Scheme -ne 'https' -or $url.Host -ne 'cdn.modrinth.com') { throw 'Expected an official Modrinth CDN URL.' }
    $destination = Asset-Path $mod.relativePath
    New-Item -ItemType Directory -Path (Split-Path $destination) -Force | Out-Null
    if (!(Test-Path -LiteralPath $destination)) {
        $partial = $destination + '.partial'
        Invoke-WebRequest -UseBasicParsing -Uri $url -Headers @{
            'User-Agent' = 'DragonLoot-local-compatibility-tests/1.0'
        } -OutFile $partial
        foreach ($algorithm in @('SHA1', 'SHA512', 'SHA256')) {
            $expected = $mod.($algorithm.ToLowerInvariant())
            if ((Get-FileHash -LiteralPath $partial -Algorithm $algorithm).Hash.ToLowerInvariant() -ne $expected) {
                throw ('Official checksum mismatch: ' + $mod.filename + ' ' + $algorithm)
            }
        }
        Move-Item -LiteralPath $partial -Destination $destination
    }
    foreach ($algorithm in @('SHA1', 'SHA512', 'SHA256')) {
        $expected = $mod.($algorithm.ToLowerInvariant())
        if ((Get-FileHash -LiteralPath $destination -Algorithm $algorithm).Hash.ToLowerInvariant() -ne $expected) {
            throw ('Cached checksum mismatch: ' + $mod.filename + ' ' + $algorithm)
        }
    }
    $alias = Asset-Path ('maven-artifacts/' + $mod.projectId + '-' + $mod.versionId + '.jar')
    New-Item -ItemType Directory -Path (Split-Path $alias) -Force | Out-Null
    Copy-Item -LiteralPath $destination -Destination $alias -Force
    Write-Output ('Verified ' + $mod.minecraft + ' ' + $mod.project + ' ' + $mod.modVersion)
}
Copy-Item -LiteralPath $ManifestPath -Destination (Join-Path $AssetRoot 'manifest.json') -Force
Write-Output ('Prepared ' + $manifest.mods.Count + ' pinned artifacts in ' + $AssetRoot)
