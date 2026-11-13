#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
VERSION="$(grep '^version=' "$ROOT/version.properties" 2>/dev/null | head -n1 | cut -d= -f2)"
[ -n "$VERSION" ] || VERSION="0.0.0"

gradle_cmd() {
  if [ -x "$ROOT/gradlew" ]; then echo "$ROOT/gradlew"; return; fi
  if command -v gradle >/dev/null 2>&1; then echo "gradle"; return; fi
  echo ""; return
}

usage() {
  cat <<EOF
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
  release [target]     Package versioned artifacts into output/v$VERSION/ (core|desktop|android|all)
  android [action]     Android helpers: devices, install, launch, run (default: devices)
  doctor               Check common prerequisites

Android run/install selects the most active adb device/emulator automatically.
EOF
}

adb_cmd() {
  if command -v adb >/dev/null 2>&1; then command -v adb; return; fi
  SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [ -n "$SDK" ] && [ -x "$SDK/platform-tools/adb" ]; then echo "$SDK/platform-tools/adb"; return; fi
  if [ -n "$SDK" ] && [ -x "$SDK/platform-tools/adb.exe" ]; then echo "$SDK/platform-tools/adb.exe"; return; fi
  echo ""
}

emulator_cmd() {
  if command -v emulator >/dev/null 2>&1; then command -v emulator; return; fi
  SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [ -n "$SDK" ] && [ -x "$SDK/emulator/emulator" ]; then echo "$SDK/emulator/emulator"; return; fi
  if [ -n "$SDK" ] && [ -x "$SDK/emulator/emulator.exe" ]; then echo "$SDK/emulator/emulator.exe"; return; fi
  echo ""
}

# Outputs lines: "serial<TAB>state<TAB>kind<TAB>info" for devices in 'device' state, ranked (physical first)
active_adb_devices_ranked() {
  ADB="$(adb_cmd)"; [ -n "$ADB" ] || return 0
  "$ADB" devices -l 2>/dev/null | awk '
    NR==1 {next}
    NF < 2 {next}
    {
      serial=$1; state=$2; info="";
      for(i=3;i<=NF;i++){info = info (i==3?"":" ") $i}
      if(state!="device") next;
      kind="physical";
      if(serial ~ /^emulator-/ || serial ~ /^127\.0\.0\.1:/ || info ~ /emulator|virtual/) kind="emulator";
      print serial "\t" state "\t" kind "\t" info;
    }
  ' | awk -F'\t' '$3=="physical"{print} $3=="emulator"{print > "/tmp/ce_emu_devs.$$"} END{if((getline line < "/tmp/ce_emu_devs.$$")>=0){close("/tmp/ce_emu_devs.$$"); system("cat /tmp/ce_emu_devs.$$ 2>/dev/null; rm -f /tmp/ce_emu_devs.$$")} else {system("rm -f /tmp/ce_emu_devs.$$ 2>/dev/null")}}'
}

select_active_adb_device() {
  active_adb_devices_ranked | head -n 1 | cut -f1
}

android_devices() {
  ADB="$(adb_cmd)"; EMU="$(emulator_cmd)"
  if [ -n "$ADB" ]; then echo "adb: $ADB"; "$ADB" devices -l 2>/dev/null || true; else echo "adb: NOT FOUND"; fi
  SEL="$(select_active_adb_device 2>/dev/null || true)"
  if [ -n "$SEL" ]; then echo "selected (most active): $SEL"; else echo "selected (most active): (none)"; fi
  if [ -n "$EMU" ]; then echo "emulator: $EMU"; AVDS="$("$EMU" -list-avds 2>/dev/null || true)"; if [ -n "$AVDS" ]; then echo "avds:"; echo "$AVDS" | sed 's/^/  - /'; else echo "avds: (none)"; fi; else echo "emulator: NOT FOUND"; fi
}

android_install() {
  ADB="$(adb_cmd)"; [ -n "$ADB" ] || { echo "adb not found. Install Android platform-tools or set ANDROID_HOME/ANDROID_SDK_ROOT." >&2; exit 127; }
  APK="$(ls -1t "$ROOT"/android/build/outputs/apk/debug/*.apk 2>/dev/null | head -n 1 || true)"
  [ -n "$APK" ] || { echo "Debug APK not found in android/build/outputs/apk/debug. Run 'ce build android' first." >&2; exit 1; }
  SEL="$(select_active_adb_device)"; [ -n "$SEL" ] || { echo "No active adb device/emulator found. Connect a device or start an emulator." >&2; exit 1; }
  echo "Using most active device: $SEL"
  "$ADB" -s "$SEL" install -r "$APK"
  echo "Installed: $APK"
}

android_launch() {
  ADB="$(adb_cmd)"; [ -n "$ADB" ] || { echo "adb not found." >&2; exit 127; }
  SEL="$(select_active_adb_device)"; [ -n "$SEL" ] || { echo "No active adb device/emulator found." >&2; exit 1; }
  echo "Using most active device: $SEL"
  "$ADB" -s "$SEL" shell am start -n codeescape.android/.MainActivity
  echo "Launched: codeescape.android/.MainActivity"
}

android_run() {
  G="$(need_gradle)"; cd "$ROOT"; "$G" :android:assembleDebug
  SEL="$(select_active_adb_device)"
  if [ -z "$SEL" ]; then
    EMU="$(emulator_cmd)"; AVDS=""
    if [ -n "$EMU" ]; then AVDS="$("$EMU" -list-avds 2>/dev/null || true)"; fi
    if [ -n "$AVDS" ]; then echo "No active adb device found. Available AVDs:"; echo "$AVDS" | sed 's/^/  - /'; echo "Start an emulator or connect a device, then rerun 'ce run android'."; else echo "No active adb device/emulator found."; fi
    exit 1
  fi
  echo "Using most active device: $SEL"
  android_install
  android_launch
}

doctor() {
  echo "Project: $ROOT"
  echo "Version: $VERSION"
  if command -v java >/dev/null 2>&1; then
    echo "java: $(command -v java)"
    java -version 2>&1 | head -n 2 | sed 's/^/  /'
  else
    echo "java: NOT FOUND"
  fi
  G="$(gradle_cmd)"; if [ -n "$G" ]; then echo "gradle: $G"; else echo "gradle: NOT FOUND (install Gradle or add wrapper)"; fi
  if [ -n "${ANDROID_HOME:-}" ] || [ -n "${ANDROID_SDK_ROOT:-}" ]; then echo "android sdk: configured"; else echo "android sdk: not detected (ANDROID_HOME/ANDROID_SDK_ROOT)"; fi
  ADB="$(adb_cmd)"; if [ -n "$ADB" ]; then echo "adb: $ADB"; else echo "adb: NOT FOUND"; fi
  EMU="$(emulator_cmd)"; if [ -n "$EMU" ]; then echo "emulator: $EMU"; else echo "emulator: NOT FOUND"; fi
  SEL="$(select_active_adb_device 2>/dev/null || true)"; if [ -n "$SEL" ]; then echo "active adb device: $SEL"; else echo "active adb device: (none)"; fi
}

need_gradle() {
  G="$(gradle_cmd)"
  if [ -z "$G" ]; then echo "Gradle not found. Install Gradle or generate the Gradle wrapper (gradle wrapper)." >&2; exit 127; fi
  echo "$G"
}

cmd="${1:-help}"
shift || true
case "$cmd" in
  -h|--help|help) usage ;;
  version)
    echo "CodeEscape $VERSION"
    if command -v java >/dev/null 2>&1; then java -version 2>&1 | head -n 1; fi
    ;;
  doctor) doctor ;;
  clean) G="$(need_gradle)"; cd "$ROOT"; "$G" clean ;;
  build)
    target="${1:-all}"; G="$(need_gradle)"; cd "$ROOT"
    case "$target" in
      core) "$G" :core:build ;;
      desktop) "$G" :desktop:build ;;
      android) "$G" :android:assembleDebug ;;
      all) "$G" build :android:assembleDebug || "$G" :core:build :desktop:build ;;
      *) echo "Unknown build target: $target" >&2; exit 2 ;;
    esac
    ;;
  run)
    target="${1:-desktop}"
    case "$target" in
      desktop) G="$(need_gradle)"; cd "$ROOT"; "$G" :desktop:run ;;
      android) android_run ;;
      *) echo "Unknown run target: $target (expected: desktop, android)" >&2; exit 2 ;;
    esac
    ;;
  android)
    action="${1:-devices}"
    case "$action" in
      devices) android_devices ;;
      install) G="$(need_gradle)"; cd "$ROOT"; "$G" :android:assembleDebug; android_install ;;
      launch) android_launch ;;
      run) android_run ;;
      *) echo "Unknown android action: $action (expected: devices, install, launch, run)" >&2; exit 2 ;;
    esac
    ;;
  test)
    target="${1:-all}"; G="$(need_gradle)"; cd "$ROOT"
    case "$target" in
      core) "$G" :core:test ;;
      desktop) "$G" :desktop:test ;;
      all) "$G" :core:test :desktop:test ;;
      *) echo "Unknown test target: $target" >&2; exit 2 ;;
    esac
    ;;
  release)
    target="${1:-all}"; G="$(need_gradle)"; cd "$ROOT"
    OUT="$ROOT/output/v$VERSION"
    mkdir -p "$OUT"
    case "$target" in
      core) "$G" :core:build ;;
      desktop) "$G" :desktop:packageRelease ;;
      android) "$G" :android:packageReleaseArtifacts ;;
      all) "$G" :core:build :desktop:packageRelease :android:packageReleaseArtifacts || "$G" :core:build :desktop:packageRelease ;;
      *) echo "Unknown release target: $target" >&2; exit 2 ;;
    esac
    # manifest.json and checksums for versioned output
    BUILT_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date)"
    ARTIFACTS=""
    if [ -d "$OUT" ]; then
      if command -v sha256sum >/dev/null 2>&1 || command -v shasum >/dev/null 2>&1; then
        ARTIFACTS_TMP="$(mktemp 2>/dev/null || echo "$OUT/.artifacts.tmp")"
        : > "$ARTIFACTS_TMP"
        (cd "$OUT" && find . -type f ! -name checksums.sha256 ! -name manifest.json -print | sed 's#^\./##' | sort) | while IFS= read -r rel; do
          [ -n "$rel" ] || continue
          size="$(wc -c < "$OUT/$rel" | tr -d ' ')"
          esc_rel="$(printf '%s' "$rel" | sed 's/\\/\\\\/g; s/"/\\"/g')"
          printf '    {"path": "%s", "size": %s},\n' "$esc_rel" "$size" >> "$ARTIFACTS_TMP"
        done
        if [ -s "$ARTIFACTS_TMP" ]; then
          ARTIFACTS="$(sed '$ s/,$//' "$ARTIFACTS_TMP")"
        fi
        rm -f "$ARTIFACTS_TMP"
      fi
    fi
    {
      echo '{'
      echo "  \"name\": \"CodeEscape\","
      echo "  \"version\": \"$VERSION\","
      echo "  \"builtAt\": \"$BUILT_AT\","
      echo "  \"target\": \"$target\","
      echo '  "artifacts": ['
      if [ -n "$ARTIFACTS" ]; then printf '%s\n' "$ARTIFACTS"; fi
      echo '  ]'
      echo '}'
    } > "$OUT/manifest.json"
    if command -v sha256sum >/dev/null 2>&1; then
      (cd "$OUT" && find . -type f ! -name checksums.sha256 ! -name manifest.json -print0 | sort -z | xargs -0 sha256sum) > "$OUT/checksums.sha256" 2>/dev/null || true
    elif command -v shasum >/dev/null 2>&1; then
      (cd "$OUT" && find . -type f ! -name checksums.sha256 ! -name manifest.json -print0 | sort -z | xargs -0 shasum -a 256) > "$OUT/checksums.sha256" 2>/dev/null || true
    fi
    echo "Release output: $OUT"
    ;;
  *) echo "Unknown command: $cmd" >&2; usage; exit 2 ;;
esac
