param(
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [Parameter(Mandatory = $true)][string]$Label
)
$ErrorActionPreference = "Stop"
$root = "C:\Users\131fgh\Documents\NetBeansProjects\S_fixed"
$outDir = Join-Path $root ".tmp\desktop-verify"
$installScript = Join-Path $root "desktop\build\install\desktop\bin\desktop.bat"
$wrapper = Join-Path $outDir "run-$Label.bat"
$log = Join-Path $outDir "desktop-$Label.log"
$errLog = Join-Path $outDir "desktop-$Label.err.log"
$exitFile = Join-Path $outDir "desktop-$Label.exitcode.txt"

foreach ($f in @($log, $errLog, $exitFile)) { if (Test-Path $f) { Remove-Item $f -Force } }

# Wrapper sets JAVA_HOME inside its own cmd environment (WMI-created processes do not
# inherit this PowerShell session's env), then runs the real generated launch script.
$wrapperText = @"
@echo off
set "JAVA_HOME=$JavaHome"
set "PATH=$JavaHome\bin;%PATH%"
call "$installScript"
> "$exitFile" echo %ERRORLEVEL%
"@
Set-Content -Path $wrapper -Value $wrapperText -Encoding ASCII

# Win32_Process.Create launches fully detached, so no stdio handles leak into the harness.
$cmdLine = 'cmd.exe /c ""' + $wrapper + '" > "' + $log + '" 2> "' + $errLog + '""'
$res = ([wmiclass]"Win32_Process").Create($cmdLine, $outDir, $null)
$launcherPid = $res.ProcessId

Start-Sleep -Seconds 12

$javaProcs = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -like "*build\install\desktop*" }

[ordered]@{
    label            = $Label
    javaHome         = $JavaHome
    launcherPid      = $launcherPid
    wmiReturnValue   = $res.ReturnValue
    secondsWaited    = 12
    javaProcessCount = @($javaProcs).Count
    javaPids         = @($javaProcs | ForEach-Object { $_.ProcessId })
    javaCommandLine  = @($javaProcs | ForEach-Object { $_.CommandLine })
    logPath          = $log
    errLogPath       = $errLog
    logBytes         = if (Test-Path $log) { (Get-Item $log).Length } else { -1 }
    errLogBytes      = if (Test-Path $errLog) { (Get-Item $errLog).Length } else { -1 }
} | ConvertTo-Json -Depth 4
