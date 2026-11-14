param(
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [Parameter(Mandatory = $true)][string]$Label
)
$ErrorActionPreference = "Stop"
$root = "C:\Users\131fgh\Documents\NetBeansProjects\S_fixed"
$outDir = Join-Path $root ".tmp\desktop-verify"
$script = Join-Path $root "desktop\build\install\desktop\bin\desktop.bat"
$log = Join-Path $outDir "desktop-$Label.log"
$errLog = Join-Path $outDir "desktop-$Label.err.log"

foreach ($f in @($log, $errLog)) { if (Test-Path $f) { Remove-Item $f -Force } }

$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;" + $env:PATH
$started = Get-Date
$p = Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c", "`"$script`"" `
    -WorkingDirectory $outDir `
    -RedirectStandardOutput $log `
    -RedirectStandardError $errLog `
    -WindowStyle Hidden -PassThru

Start-Sleep -Seconds 12

# Find the java process(es) belonging to this installation
$javaProcs = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -like "*desktop\build\install\desktop*" }

$alive = @($javaProcs).Count
$result = [ordered]@{
    label             = $Label
    javaHome          = $JavaHome
    launcherPid       = $p.Id
    launcherExited    = $p.HasExited
    launcherExitCode  = if ($p.HasExited) { $p.ExitCode } else { $null }
    secondsWaited     = 12
    javaProcessCount  = $alive
    javaPids          = @($javaProcs | ForEach-Object { $_.ProcessId })
    javaCommandLine   = @($javaProcs | ForEach-Object { $_.CommandLine })
    logPath           = $log
    errLogPath        = $errLog
    logBytes          = if (Test-Path $log) { (Get-Item $log).Length } else { -1 }
    errLogBytes       = if (Test-Path $errLog) { (Get-Item $errLog).Length } else { -1 }
}
Write-Output ($result | ConvertTo-Json -Depth 4)
