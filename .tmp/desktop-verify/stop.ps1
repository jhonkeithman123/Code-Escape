param(
    [Parameter(Mandatory = $true)][string]$Label,
    [switch]$SkipWait
)
$ErrorActionPreference = "Stop"
$root = "C:\Users\131fgh\Documents\NetBeansProjects\S_fixed"
$outDir = Join-Path $root ".tmp\desktop-verify"
$exitFile = Join-Path $outDir "desktop-$Label.exitcode.txt"

$javaProcs = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -like "*build\install\desktop*" }
$killed = @()
foreach ($jp in $javaProcs) {
    $killed += $jp.ProcessId
    Stop-Process -Id $jp.ProcessId -Force -ErrorAction SilentlyContinue
}
if (-not $SkipWait) { Start-Sleep -Seconds 3 }

$remaining = @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -like "*build\install\desktop*" }).Count
$exitCode = if (Test-Path $exitFile) { (Get-Content $exitFile -Raw).Trim() } else { $null }

[ordered]@{
    label              = $Label
    killedPids         = $killed
    remainingAppProcs  = $remaining
    wrapperExitCode    = $exitCode
} | ConvertTo-Json
