param(
    [Parameter(Mandatory = $true)][int]$ProcessId,
    [Parameter(Mandatory = $true)][string]$OutPath
)
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Windows.Forms, System.Drawing

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win32Cap {
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr hWnd, System.Text.StringBuilder text, int count);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint pid);
}
"@

$proc = Get-Process -Id $ProcessId -ErrorAction Stop
$hwnd = $proc.MainWindowHandle
if ($hwnd -eq [IntPtr]::Zero) { throw "PID $ProcessId has no main window handle" }

[void][Win32Cap]::ShowWindow($hwnd, 3)   # SW_MAXIMIZE
[void][Win32Cap]::SetForegroundWindow($hwnd)
Start-Sleep -Milliseconds 900
# verify foreground belongs to our pid
$fg = [Win32Cap]::GetForegroundWindow()
$fgPid = 0
[void][Win32Cap]::GetWindowThreadProcessId($fg, [ref]$fgPid)

$rect = New-Object Win32Cap+RECT
[void][Win32Cap]::GetWindowRect($hwnd, [ref]$rect)
$w = $rect.Right - $rect.Left
$h = $rect.Bottom - $rect.Top
$bmp = New-Object System.Drawing.Bitmap($w, $h)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($rect.Left, $rect.Top, 0, 0, (New-Object System.Drawing.Size($w, $h)))
$g.Dispose()
$full = [System.IO.Path]::GetFullPath($OutPath)
$bmp.Save($full, [System.Drawing.Imaging.ImageFormat]::Png)

$sb = New-Object System.Text.StringBuilder 512
[void][Win32Cap]::GetWindowText($hwnd, $sb, 512)

$colors = New-Object 'System.Collections.Generic.HashSet[int]'
$minL = 255.0; $maxL = 0.0; $sum = 0.0; $n = 0
for ($y = 0; $y -lt $h; $y += 8) {
    for ($x = 0; $x -lt $w; $x += 8) {
        $c = $bmp.GetPixel($x, $y)
        [void]$colors.Add($c.ToArgb())
        $l = 0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B
        if ($l -lt $minL) { $minL = $l }
        if ($l -gt $maxL) { $maxL = $l }
        $sum += $l; $n++
    }
}
$bmp.Dispose()
[ordered]@{
    pid              = $ProcessId
    hwnd             = $hwnd.ToInt64()
    windowTitle      = $sb.ToString()
    windowRect       = "$($rect.Left),$($rect.Top),$w,$h"
    foregroundPid    = $fgPid
    foregroundIsOurs = ($fgPid -eq $ProcessId)
    path             = $full
    bytes            = (Get-Item $full).Length
    distinctColors   = $colors.Count
    minLuma          = [math]::Round($minL, 2)
    maxLuma          = [math]::Round($maxL, 2)
    meanLuma         = [math]::Round(($sum / $n), 2)
} | ConvertTo-Json
