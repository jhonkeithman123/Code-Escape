# Code Escape Migration Task List (Phases 0–5)

Track progress here. Update checkboxes/status as work is completed.

**Current focus:** complete migration of the original `src/` monolith into the new project structure (`:core`, `:desktop`, `:android`) with parity, before adding new features.

**Scope:** 1=Gradle multi-module, 2=Swing desktop, 3=Compose+Canvas Android, 4=JDK 8/17/21-compatible core+desktop, 5=best-effort TTS, 6=fresh Android storage, 7=0–5.  
**Goals:** shared core, separate Windows/Android frontends, animation parity (same timing/easing, scaled to screen), `ce` CLI, versioned release output.

## Phase 0 — Scaffold & Tooling
- [x] Gradle multi-module scaffold (`:core`, `:desktop`, `:android`) with root/module builds and `settings.gradle`
- [x] Java compatibility strategy in build (core/desktop Java 8 bytecode; buildable on JDK 8/17/21 where configured)
- [x] `version.properties` as version source of truth
- [x] `.gitignore` updated (Gradle/IDE/Android/output/runtime/graphify)
- [x] `ce` CLI (`ce`, `ce.cmd`, `tools/ce.sh`, `tools/ce.ps1`) — help, version, doctor, build, run, test, clean, release
- [x] Release helper: versioned output `output/v<version>/{desktop,android}` with `manifest.json` and `checksums.sha256`
- [x] Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`) — Gradle 8.7 wrapper scripts/properties and `gradle-wrapper.jar` present (runtime not verified in this environment)
- [~] CI-friendly checks across JDK 8/17/21 (+ Android SDK where applicable) — `.github/workflows/ci.yml` added/reviewed (matrix core/desktop + Android SDK job), not executed/verified
- [~] Android release signing hooks/configuration — conditional release signing in `android/build.gradle`, `keystore.properties.example`, signing ignores added; runtime/release verification pending (no secrets committed)

## Phase 1 — Core Domain & Content
- [x] Extract `Question` model to `:core`
- [x] Extract `QuestionBank` and `GameConfig` to `:core`
- [x] Extract `PlayerProfile`, `GameSession` models
- [x] Extract shared animation timing/state (`AnimationTiming`, `RunnerAnimationState`)
- [x] Move story/level metadata and core content/constants into `:core` (`StoryContent`, `LevelMetadata`, `AudioCatalog`, `GameConfig` delays); wired in desktop adapter/app story/loading paths
- [x] Unit tests for content/models/timing helpers in `:core` (`core/src/test/java/...`: config/content/models/timing/session; not executed in this environment)

## Phase 2 — Core Engine & Flow
- [x] `LessonEngine` (shuffle, question timing, submit MCQ/syntax, timeout, progression, game complete/over)
- [x] Wire legacy desktop progression to `LessonEngine` (start/load/answer/timeout/advance/commit hooks)
- [x] `FlowState` enum (seed)
- [~] Formalize core flow contract foundations (`Navigator`, `FlowEvent`, `FlowTransitions`) and integrate transition decisions into desktop controller (`CodeEscapeApp`) via `FlowTransitions`/`Navigator` where feasible (additive `FlowEvent.LOGOUT_CONFIRMED`/`targetFor(...)`/`dispatch(...)` usage and selected app-level routing added; broader/cross-frontend-consistent event dispatch still incremental)
- [x] Remove duplicated progression/state ownership from monolith (adopt `GameSession`/make `LessonEngine`+session the single source of truth for attempt state; legacy controller reads/writes session state)
- [x] Unit tests for progression/XP/heart/commit-cancel rules and flow transitions in `:core` (added and **23/23 passed** in latest environment run via minimal runner; Gradle/JUnit Platform execution still pending)

## Phase 3 — Ports & Adapters
- [x] Core ports: `ProfileRepository`, `AudioPort`, `NarrationPort` (additive `core.port.NarrationCallback` and default `NarrationPort.speakStory(..., callback)` added; desktop narration adapter aligned)
- [x] Desktop `DesktopAudioAdapter` (music/volume/feedback) and wiring in app flow
- [x] Desktop `WindowsNarrationAdapter` + callback wiring (TTS/karaoke, music pause/resume)
- [x] Desktop `FileProfileRepository` (compatibility-oriented seed for `users.properties`/`scores.properties`)
- [x] Wire monolith persistence end-to-end to `ProfileRepository` (replace direct account/score/profile maps and save/load/commit paths in `src/codeescape/CodeEscapeApp.java`; delegates load/save/auth/register/character-save/commit to `FileProfileRepository` with cache sync) while preserving `users.properties`/`scores.properties` compatibility and commit/cancel/saved-lifetime XP behavior
- [~] Desktop scheduler/timer abstraction and adapter where needed (`desktop.scheduler.SwingScheduler` adopted for loading delays, question countdown paths, and story fade in parts; many UI timers/ownership still direct Swing/monolith-managed for parity)
- [~] Android adapters: storage, audio, TTS (best-effort), scheduler/lifecycle scaffolding aligned with core ports (`android.adapter.*`) — integrated into runtime flows with lifecycle/media/state improvements and snapshot/TTS/audio fixes; runtime/device parity verification pending

## Phase 4 — Desktop Frontend (Swing, parity)
- [x] `DesktopApp` bridge entrypoint
- [x] Extract and wire screens/builders: `LoadingScreen`, `WelcomeScreen`, `DashboardScreen`, `LoginScreen`, `RegisterScreen`, `StoryScreenBuilder` (all story screens), `MessageScreen` (`Complete`/`GameOver`/`Logout`), `ProfileScreen`, `RunningTransitionScreen` (panel/progress shell), `EndingScreen` (shell), `CharacterScreen`
- [x] Extract Question screen + HUD UI composition to `desktop.ui.screens.QuestionScreen` (refs wired; question/timer/engine logic remains in controller)
- [x] Move reusable UI kit components (`BackgroundPanel` adapter, `GlassPanel`, `RoundedBorder`, `ModernButton`, `ModernCardPanel`, `PlaceholderTextField`, `PlaceholderPasswordField`) to `desktop.ui.components` and wire monolith usage
- [x] Extract sprite frame helpers to `desktop.ui.animation.SpriteFrames`; move animation views `MiniRunner`/`MovingRunner`/`EndingScene` to `desktop.ui.animation.*`
- [x] Extract key dialogs to `desktop.ui.dialogs.GameDialogs` (game exit, treasure chest, ending XP)
- [~] (In progress/incremental) Question HUD/UI helper/controller decoupling: `QuestionHudController` wired/synchronized with central timer/input helpers, controller-preferred countdown/sync paths, HUD/helper updates, and `QuestionScreen` render/input delegation in touched flows; monolith still owns much orchestration/label composition and many field mutations
- [ ] Move remaining UI kit/HUD helper pieces and further decouple question logic from monolith fields where safe (migrate timers/answer-timeout/loadQuestion/updateHud/exit-restart flows onto extracted helpers/controllers with parity)
- [~] Drive desktop UI from core flow/state/events with minimal direct field mutation (additive core flow targets + selective `dispatch`/navigator routing; broader dispatch/ownership pending)
- [ ] Full manual parity verification (auth, selection, stories/skip/TTS, questions/timers, transitions, XP commit/cancel, persistence reload, ending) — not runnable as full GUI parity in this environment; `javac` compile checks passed (latest 51-source core+desktop+monolith), runtime/manual checks still required

## Phase 5 — Android Frontend (Compose + Canvas, parity scaled)
- [x] Android module scaffold (manifest/resources/theme/proguard/Compose setup)
- [x] Asset loading & frame isolation (`SpriteAssets`, alpha-bounds, shared-canvas parity with desktop)
- [x] Asset-backed, screen-scaled runner renderer (`AssetRunningTransition`) using shared timing/easing/frames
- [x] Asset-backed, screen-scaled ending scene foundation (`AssetEndingScene`) with key/chest/victory sequence hooks
- [x] `MainActivity` wired to full `GameApp` (Compose) shell
- [x] Implement core game screens/flows (welcome/loading/auth/character/story/dashboard/question/running/profile/results/ending) with navigation and state wiring to core (`LessonEngine`/`GameSession`, `FlowTransitions`, core content/config)
- [~] Wire real sprite/background assets for gameplay visuals and advance ending choreography parity (assets/renderers wired and screen-scaled; level-complete story→running→question and ending key/chest/treasure/XP dialog polish applied, but full desktop dialog/timing choreography parity not fully verified on device)
- [~] Android navigation (Navigation Compose), input/touch, back handling, and lifecycle awareness (pause/resume observers, audio/TTS pause/stop hooks); best-effort run snapshot restoration with safer normalization/fallbacks and lifecycle-aware question timing/feedback-state handling added, but full process/session/animation/dialog continuity still pending device verification
- [x] Persist profiles/scores in Android app storage via core-compatible schema (`AndroidProfileRepository` JSON, `ProfileRepository` integration for auth/profile/lifetime XP commit/cancel)
- [~] Android audio and best-effort TTS integration (`AndroidAudioAdapter`/`AndroidNarrationAdapter` improved for lifecycle pause/resume/recovery, same-track/active guards, broader music/SFX resolution, and utterance-id/range hooks with UI highlight status; engine/device-dependent highlighting, SFX parity, and runtime verification pending)

## Focus Areas (requested sequence)
- [x] **(a)** Wire ports/adapters into app flow (audio/narration adapters + callback wiring, persistence adapter seed, engine-driven progression) — *initial pass*
- [x] **(b)** Extract concrete Swing screens/shells (`LoadingScreen`, `WelcomeScreen`, `DashboardScreen`, `LoginScreen`, `RegisterScreen`, `StoryScreenBuilder`, `MessageScreen`, `ProfileScreen`, `RunningTransitionScreen`, `EndingScreen`, `CharacterScreen`), sprite helpers (`SpriteFrames`), animation views (`MiniRunner`/`MovingRunner`/`EndingScene`), and key dialogs (`GameDialogs`)
- [x] **(c)** Build Android asset-backed runner/ending renderers (shared timing, scaled to screen)
- [~] **(a)** Complete: remaining port/state ownership formalization (additive narration callback/flow targets + selected `dispatch` routing, partial `SwingScheduler` adoption, ownership/dead-code cleanup); remaining caches/timers/state duplication and cross-frontend flow alignment pending
- [~] **(b)** Complete: finish decoupling Question/HUD controller logic and remaining helpers from monolith; drive UI from core flow/state/events with parity (incremental progress after Phase 3/4 reconciliation, controller/helpers not yet authoritative)
- [~] **(c)** Complete: full Android app flows/visual parity and wire adapters into screens/navigation/lifecycle (additional source-level flow/choreography/TTS/audio/restoration polish landed; build/emulator/device parity verification pending)

## Validation / Maintenance
- [x] `javac -encoding UTF-8` compile checks for core, desktop Java, and legacy integrated app (latest reconciliation: **PASS**, 51 sources)
- [x] Core tests compiled and executed in environment (**23/23 passed**, Phase 3 minimal-runner pass); Gradle/JUnit Platform run pending
- [x] `graphify update .` run after structural changes (graph rebuilt; warnings/LLM label refresh pending)
- [ ] `ce build/test` and `ce release` verified with Gradle/SDK installed (wrapper/CI/signing runtime unverified)
- [ ] Run desktop parity checklist on JDK 8/17/21
- [ ] Android build/run verification on emulator/device

---
*Last updated: 2026-11-12 — Delegated remaining Phase 0/3/4/5 (GLM 5.3 flash): wrapper jar+CI/signing configs in place (runtime unverified), core narration/flow contract + desktop dispatch/scheduler/ownership cleanup with `javac` (51 sources) and core tests 23/23 passing, desktop question/HUD decoupling reconciled, and Android source-level flow/dialog/TTS/audio/restoration parity polish (no SDK build/run). `graphify update .` run; full manual/Gradle/SDK/device parity verification still required.*