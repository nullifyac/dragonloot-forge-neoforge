param(
    [Parameter(Mandatory=$true)][string]$LaunchFile,
    [Parameter(Mandatory=$true)][string]$Java8Home,
    [Parameter(Mandatory=$true)][string]$LogPrefix
)
$ErrorActionPreference = 'Stop'
$availableRam = (Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1MB
if ($availableRam -lt 4) { throw 'Defer launch: less than4GiB RAM free.' }
$launch = Get-Content -LiteralPath $LaunchFile -Raw | ConvertFrom-Json
$runtimeArguments = @($launch.jvmArgs) + @('-classpath', $launch.classpath, $launch.main) + @($launch.args)
if (!($runtimeArguments -contains '-Xmx2G')) { throw 'The smoke JVM must retain its2GiB heap cap.' }
$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = Join-Path $Java8Home 'bin/java.exe'
$startInfo.Arguments = ($runtimeArguments | ForEach-Object {
    $quoted = [regex]::Replace($_, '(\\*)"', '$1$1\"')
    $quoted = [regex]::Replace($quoted, '(\\+)$', '$1$1')
    '"' + $quoted + '"'
}) -join ' '
$startInfo.WorkingDirectory = $launch.workingDirectory
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
foreach ($entry in $launch.environment.PSObject.Properties) {
    $startInfo.EnvironmentVariables[$entry.Name] = [string]$entry.Value
}
$process = New-Object System.Diagnostics.Process
$process.StartInfo = $startInfo
$process.Start() | Out-Null
$process.Id | Set-Content -LiteralPath "$LogPrefix.pid"
Write-Output "Started dedicated smoke JVM PID $($process.Id)."
$standardOutput = $process.StandardOutput.ReadToEndAsync()
$standardError = $process.StandardError.ReadToEndAsync()
if (!$process.WaitForExit(480000)) {
    $process.Kill()
    $process.WaitForExit()
    throw "Only this child smoke JVM was stopped after8minute timeout: $LogPrefix"
}
[IO.File]::WriteAllText("$LogPrefix.stdout.log", $standardOutput.Result)
[IO.File]::WriteAllText("$LogPrefix.stderr.log", $standardError.Result)
$runtimeExit = $process.ExitCode
$process.Dispose()
Set-Content -LiteralPath "$LogPrefix.exitcode" -Value $runtimeExit
Write-Output "Dedicated smoke JVM exited $runtimeExit."
exit $runtimeExit
