param(
    [Parameter(Mandatory = $true)][int]$ProcessId,
    [Parameter(Mandatory = $true)][string]$OutPath,
    [switch]$SendToBack
)
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Windows.Forms, System.Drawing

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win32Pw {
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr hWnd, IntPtr hdc, uint flags);
    [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr hWnd, IntPtr after, int x, int y, int cx, int cy, uint flags);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint pid);
    [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr hWnd, System.Text.StringBuilder text, int count);
}
"@

$proc = Get-Process -Id $ProcessId -ErrorAction Stop
$hwnd = $proc.MainWindowHandle
if ($hwnd -eq [IntPtr]::Zero) { throw "PID $ProcessId has no main window handle" }

if ($SendToBack) {
    # HWND_BOTTOM=1, SWP_NOMOVE|SWP_NOSIZE|SWP_NOACTIVATE = 0x0001|0x0002|0x0010
    [void][Win32Pw]::SetWindowPos($hwnd, [IntPtr]1, 0, 0, 0, 0, 0x0013)
    Start-Sleep -Milliseconds 700
}

$fg = [Win32Pw]::GetForegroundWindow()
$fgPid = 0
[void][Win32Pw]::GetWindowThreadProcessId($fg, [ref]$fgPid)
$iconic = [Win32Pw]::IsIconic($hwnd)

$rect = New-Object Win32Pw+RECT
[void][Win32Pw]::GetWindowRect($hwnd, [ref]$rect)
$w = $rect.Right - $rect.Left
$h = $rect.Bottom - $rect.Top

$bmp = New-Object System.Drawing.Bitmap($w, $h)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$hdc = $g.GetHdc()
# 2 = PW_RENDERFULLCONTENT (needed for DirectComposition/occluded windows)
$ok = [Win32Pw]::PrintWindow($hwnd, $hdc, 2)
$g.ReleaseHdc($hdc)
$g.Dispose()
$full = [System.IO.Path]::GetFullPath($OutPath)
$bmp.Save($full, [System.Drawing.Imaging.ImageFormat]::Png)

$sb = New-Object System.Text.StringBuilder 512
[void][Win32Pw]::GetWindowText($hwnd, $sb, 512)

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
    printWindowOk    = $ok
    minimized        = $iconic
    foregroundPid    = $fgPid
    foregroundIsOurs = ($fgPid -eq $ProcessId)
    path             = $full
    bytes            = (Get-Item $full).Length
    distinctColors   = $colors.Count
    minLuma          = [math]::Round($minL, 2)
    maxLuma          = [math]::Round($maxL, 2)
    meanLuma         = [math]::Round(($sum / $n), 2)
} | ConvertTo-Json
