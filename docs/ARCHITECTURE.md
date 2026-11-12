# Code Escape Architecture (Shared Core + Separate Frontends)

## Targets
- **:core**: platform-agnostic Java library (models, content, rules/engine, flow state, ports, animation timing/state). Compatible with JDK 8/17/21 as Java 8 bytecode.
- **:desktop**: Windows/desktop frontend (Swing parity path), depends on `:core`.
- **:android**: Android frontend (Compose + Canvas for animation parity), depends on `:core`.

## Shared core (current)
- `codeescape.core.model`: `Question`, `PlayerProfile`, `GameSession`
- `codeescape.core.content`: `GameConfig`, `QuestionBank`
- `codeescape.core.animation`: `AnimationTiming`, `RunnerAnimationState` (shared durations/easing/frame sequencing; frontends scale to viewport)
- `codeescape.core.engine`: `LessonEngine` (lesson/question progression & scoring logic)
- `codeescape.core.port`: `ProfileRepository`, `AudioPort`, `NarrationPort`
- `codeescape.core.flow`: `FlowState`

## Frontends
- **Desktop**: `codeescape.desktop.DesktopApp` bridges to legacy `codeescape.CodeEscapeApp` while modularization continues. Legacy app now consumes core `Question`, `QuestionBank`, and `AnimationTiming` for parity-critical animation behavior.
- **Android**: `codeescape.android.MainActivity` shell + `ui.RunningTransitionCanvas` demonstrating time-based, screen-scaled transition animation using shared `RunnerAnimationState`.

## Animation parity rule
Timing, easing (`smoothStep`), frame index sequencing, and run duration are defined in `:core`. Each frontend renders at its own resolution/density and scales sprite/scene geometry to the available screen while preserving progress-over-time behavior.

## Build & release
- Gradle multi-module with `version.properties` as version source.
- `ce` CLI: `help|version|doctor|build|run|test|clean|release`.
- Release artifacts collected under `output/v<version>/{desktop,android}`.
