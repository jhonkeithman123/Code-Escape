param(
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [Parameter(Mandatory = $true)][string]$Label,
    [int]$WaitSeconds = 10
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

$wrapperText = @"
@echo off
set "JAVA_HOME=$JavaHome"
set "PATH=$JavaHome\bin;%PATH%"
call "$installScript"
> "$exitFile" echo %ERRORLEVEL%
"@
Set-Content -Path $wrapper -Value $wrapperText -Encoding ASCII

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win32Lv {
    [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr hWnd, IntPtr after, int x, int y, int cx, int cy, uint flags);
}
"@

$cmdLine = 'cmd.exe /c ""' + $wrapper + '" > "' + $log + '" 2> "' + $errLog + '""'
$res = ([wmiclass]"Win32_Process").Create($cmdLine, $outDir, $null)

# Poll for the java process and push its window to the bottom as soon as it exists so
# concurrent agents' Robot input (this is a shared GUI session) cannot land on it.
$sw = [System.Diagnostics.Stopwatch]::StartNew()
$javaProc = $null
while ($sw.Elapsed.TotalSeconds -lt 20) {
    $javaProc = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
        Where-Object { $_.CommandLine -like "*build\install\desktop*" } | Select-Object -First 1
    if ($javaProc) {
        $p = Get-Process -Id $javaProc.ProcessId -ErrorAction SilentlyContinue
        if ($p -and $p.MainWindowHandle -ne [IntPtr]::Zero) {
            [void][Win32Lv]::SetWindowPos($p.MainWindowHandle, [IntPtr]1, 0, 0, 0, 0, 0x0013)
            break
        }
    }
    Start-Sleep -Milliseconds 250
}
$detectedAfter = [math]::Round($sw.Elapsed.TotalSeconds, 2)

if (-not $javaProc) { throw "App java process not detected within 20s" }
$pidVal = $javaProc.ProcessId
$deadline = $detectedAfter + $WaitSeconds
while ($sw.Elapsed.TotalSeconds -lt $deadline) { Start-Sleep -Milliseconds 250 }
# Re-assert bottom after the wait (another app may have raised it).
$p = Get-Process -Id $pidVal -ErrorAction SilentlyContinue
if ($p -and $p.MainWindowHandle -ne [IntPtr]::Zero) {
    [void][Win32Lv]::SetWindowPos($p.MainWindowHandle, [IntPtr]1, 0, 0, 0, 0, 0x0013)
    Start-Sleep -Milliseconds 500
}

$fgPid = 0
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win32Fg {
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint pid);
}
"@
[void][Win32Fg]::GetWindowThreadProcessId([Win32Fg]::GetForegroundWindow(), [ref]$fgPid)

$alive = [bool](Get-Process -Id $pidVal -ErrorAction SilentlyContinue)
[ordered]@{
    label               = $Label
    javaHome            = $JavaHome
    launcherPid         = $res.ProcessId
    wmiReturnValue      = $res.ReturnValue
    javaPid             = $pidVal
    detectedAfterSec    = $detectedAfter
    aliveAfterWait      = $alive
    waitSeconds         = $WaitSeconds
    foregroundPid       = $fgPid
    foregroundIsOurs    = ($fgPid -eq $pidVal)
    logPath             = $log
    errLogPath          = $errLog
    logBytes            = if (Test-Path $log) { (Get-Item $log).Length } else { -1 }
    errLogBytes         = if (Test-Path $errLog) { (Get-Item $errLog).Length } else { -1 }
} | ConvertTo-Json -Depth 4
