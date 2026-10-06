param([string]$AssetRoot = '')
$ErrorActionPreference = 'Stop'
if (!$AssetRoot) { $AssetRoot = Join-Path $PSScriptRoot '../../release/test-results/2026-10-06/compatibility' }
$AssetRoot = [IO.Path]::GetFullPath($AssetRoot)
$manifest = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'manifest.json') -Raw | ConvertFrom-Json
$installerDir = Join-Path $AssetRoot 'installers'
New-Item -ItemType Directory -Path $installerDir -Force | Out-Null
$results = @(foreach ($target in $manifest.targets) {
    if ($target.loader -eq 'forge') {
        $fullVersion = $target.minecraft + '-' + $target.loaderVersion
        $filename = 'forge-' + $fullVersion + '-installer.jar'
        $url = 'https://maven.minecraftforge.net/net/minecraftforge/forge/' + $fullVersion + '/' + $filename
    } else {
        $filename = 'neoforge-' + $target.loaderVersion + '-installer.jar'
        $url = 'https://maven.neoforged.net/releases/net/neoforged/neoforge/' + $target.loaderVersion + '/' + $filename
    }
    $checksumContent = (Invoke-WebRequest -UseBasicParsing -Uri ($url + '.sha1') -TimeoutSec 60).Content
    $checksum = if ($checksumContent -is [byte[]]) { [Text.Encoding]::UTF8.GetString($checksumContent).Trim() } else { [string]$checksumContent.Trim() }
    if ($checksum -notmatch '^[0-9a-fA-F]{40}$') { throw ('Invalid official installer checksum: ' + $filename) }
    $destination = Join-Path $installerDir $filename
    if (!(Test-Path -LiteralPath $destination)) {
        Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 90 -OutFile ($destination + '.partial')
        if ((Get-FileHash -LiteralPath ($destination + '.partial') -Algorithm SHA1).Hash -ne $checksum) {
            throw ('Installer download checksum mismatch: ' + $filename)
        }
        Move-Item -LiteralPath ($destination + '.partial') -Destination $destination
    }
    if ((Get-FileHash -LiteralPath $destination -Algorithm SHA1).Hash -ne $checksum) {
        throw ('Cached installer checksum mismatch: ' + $filename)
    }
    Write-Output ('Verified official installer ' + $filename)
    [ordered]@{minecraft=$target.minecraft;loader=$target.loader;loaderVersion=$target.loaderVersion;
        filename=$filename;downloadUrl=$url;sha1=$checksum.ToLowerInvariant();
        sha256=(Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()}
})
# Keep only records in JSON; status strings are for the console.
$records = @($results | Where-Object { $_ -is [Collections.IDictionary] })
$records | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $AssetRoot 'installers.json') -Encoding UTF8
Write-Output ('Prepared ' + $records.Count + ' verified official server installers.')
