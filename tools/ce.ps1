param(
  [Parameter(Position=0)] [string]$Command = "help",
  [Parameter(Position=1, ValueFromRemainingArguments=$true)] [string[]]$ArgsRest
)
$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$Version = "0.0.0"
$vp = Join-Path $Root "version.properties"
if (Test-Path $vp) {
  foreach ($line in Get-Content $vp) { if ($line -match '^version=(.+)$') { $Version = $Matches[1].Trim() } }
}

function Show-Usage {
@"
Code Escape developer CLI (ce)

Usage:
  ce <command> [options]
  ce -h | --help

Commands:
  help                 Show this help
  version              Show project version and environment info
  build [target]       Build targets: core, desktop, android, all (default: all)
  run [target]         Run app: desktop (default), android
  test [target]        Run tests: core, desktop, all (default: all)
  clean                Clean Gradle build outputs
  release [target]     Package versioned artifacts into output/v$Version/ (core|desktop|android|all|-all)
  android [action]     Android helpers: devices, install, launch, run, release (default: devices)
  doctor               Check common prerequisites

Android run/install selects the most active adb device/emulator automatically.
"@
}

function Get-GradleCmd {
  $gw = Join-Path $Root "gradlew.bat"
  if (Test-Path $gw) { return $gw }
  $gw2 = Join-Path $Root "gradlew"
  if (Test-Path $gw2) { return $gw2 }
  $g = Get-Command gradle -ErrorAction SilentlyContinue
  if ($g) { return $g.Source }
  return $null
}

function Need-Gradle {
  $g = Get-GradleCmd
  if (-not $g) { throw "Gradle not found. Install Gradle or generate the Gradle wrapper (gradle wrapper)." }
  return $g
}

function Invoke-Gradle([string[]]$GradleArgs) {
  $g = Need-Gradle
  Push-Location $Root
  try {
    & $g @GradleArgs
    if ($LASTEXITCODE -ne 0) { throw "Gradle exited with code $LASTEXITCODE (args: $($GradleArgs -join ' '))" }
  } finally { Pop-Location }
}

function Get-AdbCmd {
  $a = Get-Command adb -ErrorAction SilentlyContinue
  if ($a) { return $a.Source }
  $sdk = $env:ANDROID_HOME; if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
  if ($sdk) {
    $cand = Join-Path $sdk "platform-tools/adb.exe"
    if (Test-Path $cand) { return $cand }
    $cand2 = Join-Path $sdk "platform-tools/adb"
    if (Test-Path $cand2) { return $cand2 }
  }
  return $null
}

function Get-EmulatorCmd {
  $e = Get-Command emulator -ErrorAction SilentlyContinue
  if ($e) { return $e.Source }
  $sdk = $env:ANDROID_HOME; if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
  if ($sdk) {
    $cand = Join-Path $sdk "emulator/emulator.exe"
    if (Test-Path $cand) { return $cand }
    $cand2 = Join-Path $sdk "emulator/emulator"
    if (Test-Path $cand2) { return $cand2 }
  }
  return $null
}

function Get-AdbDevices {
  $adb = Get-AdbCmd
  if (-not $adb) { return @() }
  $lines = & $adb devices -l 2>$null
  $devs = @()
  foreach ($line in $lines) {
    if ($line -match '^List of devices' -or [string]::IsNullOrWhiteSpace($line)) { continue }
    # serial<TAB/space>state ...
    if ($line -match '^(\S+)\s+(\S+)(.*)$') {
      $serial = $Matches[1]; $state = $Matches[2]; $rest = $Matches[3]
      $isEmu = ($serial -match '^emulator-' -or $rest -match 'emulator|virtual' -or $serial -match '^127\.0\.0\.1:')
      $devs += [pscustomobject]@{ serial=$serial; state=$state; info=$rest.Trim(); isEmulator=$isEmu; raw=$line }
    }
  }
  return $devs
}

function Select-ActiveAdbDevice {
  $devs = @(Get-AdbDevices | Where-Object { $_.state -eq 'device' })
  if ($devs.Count -eq 0) { return $null }
  # Prefer physical devices over emulators; then first in adb list order
  $physical = @($devs | Where-Object { -not $_.isEmulator })
  if ($physical.Count -gt 0) { return $physical[0] }
  return $devs[0]
}

function Show-AndroidDevices {
  $adb = Get-AdbCmd
  if (-not $adb) { Write-Output "adb: NOT FOUND"; return }
  Write-Output "adb: $adb"
  $devs = @(Get-AdbDevices)
  if ($devs.Count -eq 0) { Write-Output "devices: (none)"; }
  else {
    foreach ($d in $devs) { Write-Output ("  - {0}  {1}  {2}  {3}" -f $d.serial, $d.state, ($(if($d.isEmulator){'emulator'}else{'physical'})), $d.info) }
  }
  $sel = Select-ActiveAdbDevice
  if ($sel) { Write-Output "selected (most active): $($sel.serial)" } else { Write-Output "selected (most active): (none)" }
  $emu = Get-EmulatorCmd
  if ($emu) {
    Write-Output "emulator: $emu"
    try { $avds = & $emu -list-avds 2>$null; if ($avds) { Write-Output "avds:"; $avds | ForEach-Object { Write-Output "  - $_" } } else { Write-Output "avds: (none)" } } catch { Write-Output "avds: (unavailable)" }
  } else { Write-Output "emulator: NOT FOUND" }
}

function Get-AndroidDebugApk {
  $dir = Join-Path $Root "android/build/outputs/apk/debug"
  if (-not (Test-Path $dir)) { return $null }
  $apk = Get-ChildItem -Path $dir -Filter "*.apk" -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
  if ($apk) { return $apk.FullName }
  return $null
}

function Install-AndroidApk([string]$Serial) {
  $adb = Get-AdbCmd
  if (-not $adb) { throw "adb not found. Install Android platform-tools or set ANDROID_HOME/ANDROID_SDK_ROOT." }
  $apk = Get-AndroidDebugApk
  if (-not $apk) { throw "Debug APK not found in android/build/outputs/apk/debug. Run 'ce build android' first." }
  if ($Serial) { & $adb -s $Serial install -r "$apk" } else { & $adb install -r "$apk" }
  if ($LASTEXITCODE -ne 0) { throw "adb install failed (exit $LASTEXITCODE)" }
  Write-Output "Installed: $apk"
}

function Launch-AndroidApp([string]$Serial) {
  $adb = Get-AdbCmd
  if (-not $adb) { throw "adb not found." }
  $component = "codeescape.android/.MainActivity"
  if ($Serial) { & $adb -s $Serial shell am start -n $component } else { & $adb shell am start -n $component }
  if ($LASTEXITCODE -ne 0) { throw "adb launch failed (exit $LASTEXITCODE)" }
  Write-Output "Launched: $component"
}

function Run-AndroidApp {
  Invoke-Gradle @(':android:assembleDebug')
  $sel = Select-ActiveAdbDevice
  $adb = Get-AdbCmd
  if (-not $sel) {
    $emu = Get-EmulatorCmd
    $avds = @()
    if ($emu) { try { $avds = @(& $emu -list-avds 2>$null) } catch {} }
    if ($avds.Count -gt 0) {
      Write-Output "No active adb device found. Available AVDs: $($avds -join ', ')"
      Write-Output "Start an emulator or connect a device, then rerun 'ce run android' (or 'ce android install' / 'ce android launch')."
    } else {
      Write-Output "No active adb device/emulator found."
    }
    throw "No adb device in 'device' state available for install/launch."
  }
  Write-Output "Using most active device: $($sel.serial) ($(if($sel.isEmulator){'emulator'}else{'physical'}))"
  Install-AndroidApk $sel.serial
  Launch-AndroidApp $sel.serial
}

switch -Regex ($Command) {
  '^(?i)(-h|--help|help)$' { Show-Usage; break }
  '^(?i)version$' {
    Write-Output "CodeEscape $Version"
    try { & java -version 2>&1 | Select-Object -First 1 | ForEach-Object { Write-Output $_ } } catch {}
    break
  }
  '^(?i)doctor$' {
    Write-Output "Project: $Root"
    Write-Output "Version: $Version"
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
      Write-Output "java: $($javaCmd.Source)"
      try { & java -version 2>&1 | Select-Object -First 2 | ForEach-Object { Write-Output "  $_" } } catch {}
    } else { Write-Output "java: NOT FOUND" }
    $g = Get-GradleCmd; if ($g) { Write-Output "gradle: $g" } else { Write-Output "gradle: NOT FOUND (install Gradle or add wrapper)" }
    if ($env:ANDROID_HOME -or $env:ANDROID_SDK_ROOT) { Write-Output "android sdk: configured" } else { Write-Output "android sdk: not detected (ANDROID_HOME/ANDROID_SDK_ROOT)" }
    $adb = Get-AdbCmd; if ($adb) { Write-Output "adb: $adb" } else { Write-Output "adb: NOT FOUND" }
    $emu = Get-EmulatorCmd; if ($emu) { Write-Output "emulator: $emu" } else { Write-Output "emulator: NOT FOUND" }
    $sel = Select-ActiveAdbDevice
    if ($sel) { Write-Output "active adb device: $($sel.serial) ($(if($sel.isEmulator){'emulator'}else{'physical'}))" } else { Write-Output "active adb device: (none)" }
    break
  }
  '^(?i)clean$' { Invoke-Gradle @('clean'); break }
  '^(?i)build$' {
    $target = if ($ArgsRest.Count -gt 0) { $ArgsRest[0] } else { 'all' }
    switch ($target) {
      'core' { Invoke-Gradle @(':core:build') }
      'desktop' { Invoke-Gradle @(':desktop:build') }
      'android' { Invoke-Gradle @(':android:assembleDebug') }
      'all' { try { Invoke-Gradle @('build', ':android:assembleDebug') } catch { Invoke-Gradle @(':core:build', ':desktop:build') } }
      default { throw "Unknown build target: $target" }
    }
    break
  }
  '^(?i)run$' {
    $target = if ($ArgsRest.Count -gt 0) { $ArgsRest[0] } else { 'desktop' }
    switch ($target) {
      'desktop' { Invoke-Gradle @(':desktop:run') }
      'android' { Run-AndroidApp }
      default { throw "Unknown run target: $target (expected: desktop, android)" }
    }
    break
  }
  '^(?i)android$' {
    $action = if ($ArgsRest.Count -gt 0) { $ArgsRest[0] } else { 'devices' }
    switch ($action) {
      'devices' { Show-AndroidDevices }
      'install' {
        Invoke-Gradle @(':android:assembleDebug')
        $sel = Select-ActiveAdbDevice
        if (-not $sel) { throw "No active adb device/emulator found. Connect a device or start an emulator." }
        Write-Output "Using most active device: $($sel.serial) ($(if($sel.isEmulator){'emulator'}else{'physical'}))"
        Install-AndroidApk $sel.serial
      }
      'launch' {
        $sel = Select-ActiveAdbDevice
        if (-not $sel) { throw "No active adb device/emulator found." }
        Write-Output "Using most active device: $($sel.serial) ($(if($sel.isEmulator){'emulator'}else{'physical'}))"
        Launch-AndroidApp $sel.serial
      }
      'run' { Run-AndroidApp }
      'release' {
        Invoke-Gradle @(':android:packageReleaseArtifacts')
        Write-Output "Android release packaged to output/v$Version/android/CodeEscape-$Version.apk and output/v$Version/changelog.md updated."
      }
      default { throw "Unknown android action: $action (expected: devices, install, launch, run, release)" }
    }
    break
  }
  '^(?i)test$' {
    $target = if ($ArgsRest.Count -gt 0) { $ArgsRest[0] } else { 'all' }
    switch ($target) {
      'core' { Invoke-Gradle @(':core:test') }
      'desktop' { Invoke-Gradle @(':desktop:test') }
      'all' { Invoke-Gradle @(':core:test', ':desktop:test') }
      default { throw "Unknown test target: $target" }
    }
    break
  }
  '^(?i)release$' {
    $target = if ($ArgsRest.Count -gt 0) { $ArgsRest[0] } else { 'all' }
    if ($target -in @('-all','--all','all')) { $target = 'all' }
    $outVer = Join-Path $Root "output\v$Version"
    New-Item -ItemType Directory -Force -Path $outVer | Out-Null
    switch ($target) {
      'core' { Invoke-Gradle @(':core:build') }
      'desktop' { Invoke-Gradle @(':desktop:packageRelease') }
      'android' { Invoke-Gradle @(':android:packageReleaseArtifacts') }
      'all' {
        try { Invoke-Gradle @(':core:build', ':desktop:packageRelease', ':android:packageReleaseArtifacts') }
        catch { Invoke-Gradle @(':core:build', ':desktop:packageRelease') }
      }
      default { throw "Unknown release target: $target (use core|desktop|android|all or -all)" }
    }
    # Manifest and checksums for versioned output
    $artifacts = @()
    if (Test-Path $outVer) {
      Get-ChildItem -Path $outVer -Recurse -File | Where-Object { $_.Name -ne 'manifest.json' -and $_.Name -ne 'checksums.sha256' } | ForEach-Object {
        $rel = $_.FullName.Substring($outVer.Length).TrimStart('\','/')
        $artifacts += [ordered]@{ path = $rel; size = $_.Length }
      }
    }
    $manifest = [ordered]@{
      name = 'CodeEscape'
      version = $Version
      builtAt = (Get-Date).ToString('o')
      target = $target
      artifacts = $artifacts
    }
    $manifest | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 (Join-Path $outVer 'manifest.json')
    $checksumPath = Join-Path $outVer 'checksums.sha256'
    if (Test-Path $checksumPath) { Remove-Item $checksumPath -Force }
    if (Test-Path $outVer) {
      Get-ChildItem -Path $outVer -Recurse -File | Where-Object { $_.Name -ne 'checksums.sha256' -and $_.Name -ne 'manifest.json' } | Sort-Object FullName | ForEach-Object {
        $hash = (Get-FileHash -Algorithm SHA256 $_.FullName).Hash.ToLower()
        $rel = $_.FullName.Substring($outVer.Length).TrimStart('\','/')
        Add-Content -Encoding UTF8 -Path $checksumPath -Value ("$hash  $rel")
      }
    }
    Write-Output "Release output: $outVer"
    break
  }
  default { Write-Error "Unknown command: $Command"; Show-Usage; exit 2 }
}
