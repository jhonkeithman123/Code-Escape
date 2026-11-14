# Desktop runtime verification (read-only), 2026-11-13

Repo: `C:\Users\131fgh\Documents\NetBeansProjects\S_fixed` @ 70c56f4
No source/docs/commits were modified. Working-tree modifications present before this run were left untouched.

## Build (JDK 17)
- Command: `.\gradlew.bat :desktop:installDist --console=plain` (JAVA_HOME = `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot`)
- Result: BUILD SUCCESSFUL in 34s (7 tasks; `:desktop:jar`, `:desktop:startScripts`, `:desktop:installDist` executed)
- Log: `.tmp/desktop-verify/build-installDist.log`
- Launch scripts generated (listed, not assumed):
  - `desktop\build\install\desktop\bin\desktop.bat`
  - `desktop\build\install\desktop\bin\desktop` (sh)
- Distribution libs: `CodeEscape-0.1.0.jar`, `core-0.1.0.jar`
- Bytecode: `major version: 52` (Java 8) for both `codeescape.desktop.DesktopApp` and
  `codeescape.core.engine.LessonEngine` (checked with JDK 8 `javap`).
- Manifest `Main-Class: codeescape.desktop.DesktopApp`.

## JDKs present
- JDK 17: `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot` (17.0.20.1) - used for build.
- JDK 8: **the path given in the task (`...\temurin-8-amd64-windows`) has no `bin\java.exe` at its root.**
  The real full JDK (java + javac) is nested at
  `C:\Users\131fgh\.gradle\jdks\temurin-8-amd64-windows\jdk8u512-b09` (Temurin 1.8.0_512-b09, JDK not JRE).
- JDK 21: **not installed anywhere on the machine** (enumerated all `java.exe` under C:; Gradle
  `javaToolchains` lists only 8 and 17; auto-detect is disabled and foojay only downloaded 8/17).
- Extra (out of required matrix): `C:\Program Files\Apache NetBeans\jdk` = OpenJDK 26.0.2.
- Note: `C:\Program Files\Java\jre1.8.0_511` is a **JRE only** (no javac).

## Exact launch commands
Real launcher used in every run (via a wrapper that sets JAVA_HOME inside cmd):

```
set "JAVA_HOME=<jdk home>"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "C:\Users\131fgh\Documents\NetBeansProjects\S_fixed\desktop\build\install\desktop\bin\desktop.bat"
```

Effective JVM command line observed (JDK 17):
```
"C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot/bin/java.exe" -classpath
"...\desktop\build\install\desktop\lib\CodeEscape-0.1.0.jar;...\lib\core-0.1.0.jar"
codeescape.desktop.DesktopApp
```

## Results per JDK (clean runs)
| JDK | PID detected | Alive at 10s | Screenshot | Log exceptions | Exit |
|-----|--------------|--------------|------------|----------------|------|
| 17.0.20.1 | 3.32s | yes | `startup-jdk17-clean.png` | none (stdout 0 B, stderr 0 B) | 0 (graceful WM_CLOSE) |
| 1.8.0_512 | 5.43s | yes | `startup-jdk8-clean.png` | none (stdout 0 B, stderr 0 B) | 0 (graceful WM_CLOSE) |
| 26.0.2 (extra) | 3.35s | yes | `startup-jdk26-clean.png` | none (stdout 0 B, stderr 0 B) | 0 (graceful WM_CLOSE) |

- All three clean captures show the **LOGIN** screen (Username/Email, Password, LOGIN, SIGN UP / REGISTER, BACK),
  window title `Code Escape - Gamified Programming Learning System`, window rect 1502x974.
- PNG content check: `startup-jdk17-clean.png` 16,526 distinct sampled colors; `startup-jdk8-clean.png` 17,776;
  both visually inspected. JDK 17 and 26 LOGIN captures are byte-identical (SHA256
  `20635CEB...D120B9`), i.e. deterministic rendering; JDK 8 differs slightly (font/DPI rendering).
- Exit codes were captured through the wrapper (`> file echo %ERRORLEVEL%`); 17/8/26 all wrote `0`.
  Force-killed exploratory runs reported `-1` (expected).

## Shared-GUI caveat (important)
The Windows session is shared with other agents. An earlier, full-screen capture round showed the app on
story/character/question screens (eventually Level 3, XP 200, "TIME'S UP! -1 HEART") because **another agent's
scripted input reached the foreground Code Escape window**; the app has no auto-login/auto-play path
(`STARTUP_COMPLETE -> LOGIN`, verified in `FlowTransitions`). Those images are kept under
`observed-interactive/` and are **not** used as startup evidence.

Mitigation used for the clean runs: launch detached via WMI, immediately push our window to
`HWND_BOTTOM`, verify `foregroundIsOurs == false`, and capture the specific PID's window with
`PrintWindow(PW_RENDERFULLCONTENT)` instead of a full-screen grab. The clean runs therefore reflect
only app startup, not any injected input.

Incidental observations (not controlled tests): with external input the app rendered story, character,
question (Level 1 and Level 3), HUD timer/hearts/XP, and the timeout modal with no exceptions logged.
The app persists `scores.properties` / `users.properties` relative to its working directory (created under
`.tmp/desktop-verify/` during these runs).

## Automated checks
- `.\gradlew.bat :core:test :desktop:test --console=plain` -> BUILD SUCCESSFUL, but all tasks UP-TO-DATE
  (`:desktop:test` NO-SOURCE), i.e. nothing executed.
- Re-ran forced: `.\gradlew.bat :core:test :desktop:test --console=plain --rerun-tasks` ->
  **BUILD SUCCESSFUL in 8s**, exit 0. Logs: `test-run.log`, `test-run-rerun.log`.
- Test counts from `core/build/test-results/test/*.xml`: **32 tests, 0 failures, 0 errors, 0 skipped**
  - AnimationTimingTest 2, ContentAndConfigTest 4, LessonEngineTest 10,
    FlowTransitionsTest 10, GameSessionTest 3, ModelBasicsTest 3.
- `:desktop:test`: **NO-SOURCE** (module has no test sources); no desktop test pass/fail counts exist.

## Not verified
- Manual GUI/UX parity across all screens and flows (only the LOGIN screen was captured; no controlled
  click-through was attempted because the shared desktop is being driven by other agents).
- Gameplay correctness, scoring/XP commit, save/resume, character/profile editing, retry/game-over/ending flows.
- TTS/narration, background music/SFX (audio device behavior), and audio-driven story highlighting.
- JDK 21 runtime (not installed); JDK 26 was only a startup smoke test, not part of the required matrix.
- Reproducibility of `:desktop:test` beyond NO-SOURCE (no desktop tests exist to run).
- CI/GitHub Actions, cross-OS matrix, signing/packaging, Android runtime/device items from docs/STATUS.md.
