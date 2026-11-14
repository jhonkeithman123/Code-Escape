param(
    [Parameter(Mandatory = $true)][string]$OutPath
)
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Windows.Forms, System.Drawing
$bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
$bmp = New-Object System.Drawing.Bitmap($bounds.Width, $bounds.Height)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
$g.Dispose()
$full = [System.IO.Path]::GetFullPath($OutPath)
$bmp.Save($full, [System.Drawing.Imaging.ImageFormat]::Png)
# Blank/quality heuristic: sample every 8th pixel, count distinct colors + luminance spread
$colors = New-Object 'System.Collections.Generic.HashSet[int]'
$minL = 255.0; $maxL = 0.0; $sum = 0.0; $n = 0
for ($y = 0; $y -lt $bounds.Height; $y += 8) {
    for ($x = 0; $x -lt $bounds.Width; $x += 8) {
        $c = $bmp.GetPixel($x, $y)
        [void]$colors.Add($c.ToArgb())
        $l = 0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B
        if ($l -lt $minL) { $minL = $l }
        if ($l -gt $maxL) { $maxL = $l }
        $sum += $l; $n++
    }
}
$bmp.Dispose()
$info = [ordered]@{
    path          = $full
    bytes         = (Get-Item $full).Length
    width         = $bounds.Width
    height        = $bounds.Height
    sampledPixels = $n
    distinctColors = $colors.Count
    minLuma       = [math]::Round($minL, 2)
    maxLuma       = [math]::Round($maxL, 2)
    meanLuma      = [math]::Round(($sum / $n), 2)
}
$info | ConvertTo-Json -Compress
