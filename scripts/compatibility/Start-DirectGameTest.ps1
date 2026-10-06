param(
    [Parameter(Mandatory=$true)][string]$LaunchFile,
    [Parameter(Mandatory=$true)][string]$LogPrefix
)
$ErrorActionPreference = 'Stop'
$availableRam = (Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1MB
if ($availableRam -lt 4) { throw 'Defer server launch: less than 4 GiB RAM free.' }
$launchDetails = Get-Content -LiteralPath $LaunchFile -Raw | ConvertFrom-Json
$javaExecutable = $launchDetails.executable
if (-not $javaExecutable) {
    $javaExecutable = 'C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot/bin/java.exe'
}
$arguments = @($launchDetails.jvmArgs) + @('-classpath', $launchDetails.classpath,
    $launchDetails.mainClass) + @($launchDetails.args)
$heapArguments = @($arguments | Where-Object { $_ -match '^-Xmx' })
if (!$heapArguments.Count -or $heapArguments[-1] -ne '-Xmx2G') {
    throw 'Direct GameTest launch must retain the 2 GB heap cap.'
}
$argumentFile = [IO.Path]::GetFullPath("$LogPrefix.args")
$encodedArguments = foreach ($argument in $arguments) {
    '"' + $argument.Replace('\', '\\').Replace('"', '\"') + '"'
}
[IO.File]::WriteAllLines($argumentFile, [string[]]$encodedArguments,
    (New-Object Text.UTF8Encoding($false)))
foreach ($entry in $launchDetails.environment.PSObject.Properties) {
    Set-Item -LiteralPath "Env:$($entry.Name)" -Value $entry.Value
}
New-Item -ItemType Directory -Path $launchDetails.workingDirectory -Force | Out-Null
$process = Start-Process -FilePath $javaExecutable -ArgumentList "@$argumentFile" `
    -WorkingDirectory $launchDetails.workingDirectory -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput "$LogPrefix.stdout.log" -RedirectStandardError "$LogPrefix.stderr.log"
# Windows PowerShell must retain the handle before waiting to expose ExitCode.
$processHandle = $process.Handle
$process.Id | Set-Content -LiteralPath "$LogPrefix.pid"
Write-Output "Started direct GameTest JVM PID $($process.Id)."
$process.WaitForExit()
$process.Refresh()
if ($null -eq $process.ExitCode) {
    throw 'Direct GameTest exit code was unavailable; do not treat this run as successful.'
}
$process.ExitCode | Set-Content -LiteralPath "$LogPrefix.exitcode"
Write-Output "Direct GameTest JVM exited $($process.ExitCode)."
exit $process.ExitCode
