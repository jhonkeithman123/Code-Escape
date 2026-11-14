package codeescape.android.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import codeescape.android.adapter.AndroidAudioAdapter
import codeescape.android.adapter.AndroidLifecycleScheduler
import codeescape.android.adapter.AndroidNarrationAdapter
import codeescape.android.adapter.AndroidProfileRepository
import codeescape.android.adapter.AndroidSettingsStore
import codeescape.core.content.AudioCatalog
import codeescape.core.content.GameConfig
import codeescape.core.content.LevelMetadata
import codeescape.core.engine.LessonEngine
import codeescape.core.flow.FlowState
import codeescape.core.flow.FlowTransitions
import codeescape.core.model.GameSettings
import codeescape.core.model.PlayerProfile
import codeescape.core.model.Question
import java.lang.Runnable

class GameAppState(private val context: Context) {
    private val prefs = context.getSharedPreferences("codeescape_app", Context.MODE_PRIVATE)
    val profileRepo = AndroidProfileRepository(context)
    val audio = AndroidAudioAdapter(context)
    val narration = AndroidNarrationAdapter(context)
    val scheduler = AndroidLifecycleScheduler()
    val settingsStore = AndroidSettingsStore(context)

    // Shared audio/narration settings (persisted in SharedPreferences).
    var musicEnabled by mutableStateOf(true)
    var muted by mutableStateOf(false)
    var volume by mutableStateOf(GameSettings.DEFAULT_VOLUME)
    var ttsEnabled by mutableStateOf(true)
    var showSettings by mutableStateOf(false)

    val engine = LessonEngine()

    var currentUsername by mutableStateOf<String?>(null)
    var currentProfile by mutableStateOf<PlayerProfile?>(null)

    // navigation / flow
    var screen by mutableStateOf(FlowState.WELCOME)
    var pendingStoryLevel by mutableIntStateOf(1) // 1 = intro/start level1 context
    var lastAnswerCorrect by mutableStateOf<Boolean?>(null)
    var resultMessage by mutableStateOf("")

    // Where the shared loading screen advances to (desktop parity: startup -> LOGIN, post-auth -> STORY).
    enum class LoadingTarget { LOGIN, STORY }
    var loadingTarget by mutableStateOf(LoadingTarget.LOGIN)

    // question runtime
    var questionTimeLeft by mutableIntStateOf(GameConfig.MCQ_SECONDS)
    var syntaxInput by mutableStateOf("")
    var answered by mutableStateOf(false)
    var awaitingAdvance by mutableStateOf(false)
    private var pendingAdvanceAction: Runnable? = null

    // Modal feedback popup parity with desktop showGamePopup (heading / big / detail + CONTINUE).
    var feedbackHeading by mutableStateOf("")
    var feedbackBig by mutableStateOf("")
    var feedbackDetail by mutableStateOf("")

    // Exit-run confirm parity with desktop showGameExitDialog.
    var exitDialogShown by mutableStateOf(false)
    // Logout confirm parity with desktop LOGOUT message screen (YES/NO).
    var logoutDialogShown by mutableStateOf(false)

    // running/ending choreography context
    enum class RunningReason { LEVEL_COMPLETE, GAME_COMPLETE }
    var runningReason by mutableStateOf<RunningReason?>(null)
    var endingChestOpened by mutableStateOf(false)
    var endingTreasureDialogShown by mutableStateOf(false)
    var endingXpDialogShown by mutableStateOf(false)
    var endingFinalXp by mutableIntStateOf(0)
    var storyHighlightRange by mutableStateOf<Pair<Int, Int>?>(null)

    // lifecycle flags
    var isResumed by mutableStateOf(true)

    /** Desktop parity: answer popup is user-confirmed (CONTINUE); this fallback auto-advances. */
    private val answerFeedbackDelayMs = 1200L

    init {
        narration.onRange = { s, e -> storyHighlightRange = s to e }
        narration.onDone = { storyHighlightRange = null }
        val loaded = settingsStore.load()
        musicEnabled = loaded.isMusicEnabled
        muted = loaded.isMuted
        volume = loaded.volume
        ttsEnabled = loaded.isTtsEnabled
        applySettings()
        restoreSessionHint()
    }

    private fun applySettings() {
        audio.isEnabled = musicEnabled
        audio.isMuted = muted
        audio.volume = volume
        narration.isEnabled = ttsEnabled
    }

    private fun persistSettings() {
        settingsStore.save(GameSettings(musicEnabled, muted, volume, ttsEnabled))
    }

    fun openSettings() { showSettings = true }
    fun closeSettings() { showSettings = false }

    fun updateMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        applySettings()
        persistSettings()
    }

    fun updateMuted(muted: Boolean) {
        this.muted = muted
        applySettings()
        persistSettings()
    }

    fun updateVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        applySettings()
        persistSettings()
    }

    fun updateTtsEnabled(enabled: Boolean) {
        ttsEnabled = enabled
        applySettings()
        if (!enabled) narration.stop()
        persistSettings()
    }

    private fun restoreSessionHint() {
        val savedUser = prefs.getString("last_user", null)
        if (!savedUser.isNullOrBlank()) {
            // lightweight restoration: return to dashboard for saved user if profile exists
            val p = profileRepo.findByUsername(savedUser).orElse(null)
            if (p != null) {
                currentUsername = savedUser
                currentProfile = p
                val snap = profileRepo.loadRunSnapshot(savedUser)
                if (snap != null && restoreRunSnapshot(snap)) {
                    return
                }
                screen = if (p.character.isNullOrBlank() || p.character == "Unspecified") FlowState.CHARACTER else FlowState.DASHBOARD
            }
        }
    }

    private fun restoreRunSnapshot(snap: AndroidProfileRepository.RunSnapshot): Boolean {
        return try {
            val s = try { FlowState.valueOf(snap.screen) } catch (_: Exception) { FlowState.DASHBOARD }
            engine.startNewLesson(snap.savedXpBeforeRun)
            val sess = engine.getSession()
            sess.username = currentUsername
            sess.level = snap.level.coerceIn(1, GameConfig.LEVEL_COUNT)
            sess.questionIndex = snap.questionIndex.coerceAtLeast(0).coerceAtMost(GameConfig.QUESTIONS_PER_LEVEL - 1)
            sess.hearts = snap.hearts.coerceIn(0, GameConfig.STARTING_HEARTS)
            sess.xp = snap.xp.coerceAtLeast(0)
            sess.correctAnswers = snap.correctAnswers.coerceAtLeast(0)
            sess.savedXpBeforeRun = snap.savedXpBeforeRun.coerceAtLeast(0)
            sess.showZeroXpAfterCancelledRun = snap.showZeroXpAfterCancelledRun
            sess.sessionCommitted = snap.sessionCommitted
            pendingStoryLevel = snap.pendingStoryLevel.coerceIn(1, GameConfig.LEVEL_COUNT)
            syntaxInput = snap.syntaxInput
            resultMessage = snap.resultMessage
            feedbackHeading = snap.feedbackHeading
            feedbackBig = snap.feedbackBig
            feedbackDetail = snap.feedbackDetail
            lastAnswerCorrect = snap.lastAnswerCorrect
            endingChestOpened = snap.endingChestOpened
            endingTreasureDialogShown = snap.endingTreasureDialogShown
            endingXpDialogShown = snap.endingXpDialogShown
            endingFinalXp = snap.endingFinalXp.coerceAtLeast(0)
            runningReason = try { if (snap.runningReason.isBlank()) null else RunningReason.valueOf(snap.runningReason) } catch (_: Exception) { null }
            loadingTarget = try { LoadingTarget.valueOf(snap.loadingTarget) } catch (_: Exception) { LoadingTarget.LOGIN }
            // never restore pending delayed-advance callbacks; normalize to a safe resumable state
            answered = false
            awaitingAdvance = false
            cancelPendingAdvance()
            questionTimeLeft = snap.questionTimeLeft.coerceAtLeast(0)
            exitDialogShown = false; logoutDialogShown = false
            storyHighlightRange = null
            // restore into safe, continuity-preserving states (process-kill best-effort)
            screen = when (s) {
                FlowState.RUNNING_TRANSITION -> {
                    // animation cannot truly resume mid-frame after process death; restart transition safely
                    if (runningReason == null) runningReason = RunningReason.LEVEL_COMPLETE
                    FlowState.RUNNING_TRANSITION
                }
                FlowState.QUESTION -> {
                    if (sess.sessionCommitted || sess.hearts <= 0) {
                        normalizeToDashboardBaseline()
                        FlowState.DASHBOARD
                    } else {
                        if (questionTimeLeft <= 0) prepareQuestionForCurrentLevel() else {
                            // keep restored question/time/input; ensure transient answer/feedback UI is resumable
                            answered = false
                            awaitingAdvance = false
                            feedbackHeading = ""; feedbackBig = ""; feedbackDetail = ""
                            lastAnswerCorrect = null
                        }
                        FlowState.QUESTION
                    }
                }
                FlowState.STORY_INTRO, FlowState.STORY_LEVEL2, FlowState.STORY_LEVEL3, FlowState.STORY_LEVEL4, FlowState.STORY_LEVEL5 -> s
                // loading is only used post-auth on Android; resume into the story intro
                FlowState.LOADING -> FlowState.STORY_INTRO
                FlowState.ENDING -> {
                    // preserve ending/choreography/dialog continuity as best-effort
                    if (endingFinalXp <= 0) endingFinalXp = sess.xp.coerceAtLeast(0)
                    if (endingXpDialogShown) endingTreasureDialogShown = false
                    if (endingChestOpened && !endingTreasureDialogShown && !endingXpDialogShown) {
                        // continuity fallback: chest already opened but dialog state missing -> resume at XP summary
                        endingXpDialogShown = true
                    }
                    FlowState.ENDING
                }
                FlowState.GAMEOVER, FlowState.COMPLETE, FlowState.LOGOUT -> {
                    if (resultMessage.isBlank() && s == FlowState.GAMEOVER) resultMessage = "You ran out of hearts."
                    s
                }
                FlowState.DASHBOARD, FlowState.PROFILE, FlowState.CHARACTER, FlowState.WELCOME, FlowState.LOGIN, FlowState.REGISTER -> {
                    // clean run/ending state for non-run screens so stale attempt state isn't surfaced
                    if (s == FlowState.DASHBOARD || s == FlowState.PROFILE || s == FlowState.CHARACTER) {
                        // keep session baseline coherent; don't surface stale dialogs/feedback
                        resetRunUiState()
                        runningReason = null
                    } else {
                        normalizeToDashboardBaseline(keepNonDashboardScreen = true)
                    }
                    s
                }
                else -> s
            }
            playMusicForCurrentScreen()
            true
        } catch (_: Exception) { false }
    }

    private fun normalizeToDashboardBaseline(keepNonDashboardScreen: Boolean = false) {
        val u = currentUsername
        val saved = if (u != null) try { profileRepo.getLifetimeXp(u) } catch (_: Exception) { engine.getSession().savedXpBeforeRun } else 0
        try { engine.cancelRun(saved) } catch (_: Exception) {}
        resetRunUiState()
        resultMessage = ""
        pendingStoryLevel = 1
        if (!keepNonDashboardScreen) {
            screen = FlowState.DASHBOARD
        }
    }

    private fun cancelPendingAdvance() {
        try { scheduler.cancel(pendingAdvanceAction) } catch (_: Exception) {}
        pendingAdvanceAction = null
        awaitingAdvance = false
    }

    fun persistState() {
        try {
            prefs.edit().putString("last_user", currentUsername).apply()
            val u = currentUsername ?: run { profileRepo.clearRunSnapshot(); return }
            val sess = engine.getSession()
            val snap = AndroidProfileRepository.RunSnapshot(
                screen = screen.name,
                level = sess.level,
                questionIndex = sess.questionIndex,
                hearts = sess.hearts,
                xp = sess.xp,
                correctAnswers = sess.correctAnswers,
                savedXpBeforeRun = sess.savedXpBeforeRun,
                showZeroXpAfterCancelledRun = sess.showZeroXpAfterCancelledRun,
                sessionCommitted = sess.sessionCommitted,
                pendingStoryLevel = pendingStoryLevel,
                questionTimeLeft = questionTimeLeft,
                syntaxInput = syntaxInput,
                answered = false, // pending callbacks/answer commits are never persisted; restore normalizes safely
                runningReason = runningReason?.name ?: "",
                endingChestOpened = endingChestOpened,
                endingTreasureDialogShown = endingTreasureDialogShown,
                endingXpDialogShown = endingXpDialogShown,
                endingFinalXp = endingFinalXp,
                resultMessage = resultMessage,
                feedbackHeading = "",
                feedbackBig = "",
                feedbackDetail = "",
                lastAnswerCorrect = null,
                exitDialogShown = false,
                loadingTarget = loadingTarget.name
            )
            profileRepo.saveRunSnapshot(u, snap)
        } catch (_: Exception) {}
    }

    fun onResume() {
        isResumed = true
        // defensive: no pending result callbacks should survive pause; keep question resumable
        if (screen == FlowState.QUESTION && pendingAdvanceAction == null) {
            awaitingAdvance = false
        }
        try { audio.resumeMusic() } catch (_: Exception) {}
        // best-effort: ensure intended track is playing after process/config recreation,
        // without restarting a track that is already playing (desktop keeps music running)
        try { if (!audio.isMusicActive()) playMusicForCurrentScreen() } catch (_: Exception) {}
        // if narration was stopped by lifecycle pause, keep highlight cleared (story can be re-read manually)
        if (screen !in setOf(FlowState.STORY_INTRO, FlowState.STORY_LEVEL2, FlowState.STORY_LEVEL3, FlowState.STORY_LEVEL4, FlowState.STORY_LEVEL5)) {
            storyHighlightRange = null
        }
    }

    fun onPause() {
        isResumed = false
        // avoid late answer/result commits firing while backgrounded or after recreation.
        // engine mutation happens inside delayed callbacks, so cancelling keeps engine state consistent;
        // clear transient question UI flags so the user can continue safely on resume.
        if (screen == FlowState.QUESTION && (awaitingAdvance || answered)) {
            cancelPendingAdvance()
            answered = false
            lastAnswerCorrect = null
            feedbackHeading = ""; feedbackBig = ""; feedbackDetail = ""
        } else {
            cancelPendingAdvance()
        }
        // exit/logout confirms and narration highlight are transient UI/state
        exitDialogShown = false
        logoutDialogShown = false
        storyHighlightRange = null
        persistState()
        try { narration.pauseForLifecycle() } catch (_: Exception) { narration.stop() }
        try { audio.pauseMusic() } catch (_: Exception) {}
    }

    fun onDestroy() {
        persistState()
        scheduler.cancelAll()
        narration.shutdown()
        audio.release()
    }

    fun refreshProfile() {
        val u = currentUsername ?: return
        currentProfile = profileRepo.findByUsername(u).orElse(null)
    }

    fun login(username: String, password: String): String? {
        val u = username.trim()
        if (u.isEmpty() || password.isEmpty()) return "Enter username and password."
        if (!profileRepo.authenticate(u, password)) return "Invalid username or password."
        currentUsername = u
        prefs.edit().putString("last_user", u).apply()
        refreshProfile()
        // Desktop parity: fresh run baseline for the logged-in user, then LOADING -> STORY_INTRO.
        val savedXp = if (u.isNotEmpty()) profileRepo.getLifetimeXp(u) else 0
        engine.startNewLesson(savedXp)
        engine.getSession().username = u
        pendingStoryLevel = 1
        resetRunUiState()
        loadingTarget = LoadingTarget.STORY
        screen = FlowState.LOADING
        playMusicForCurrentScreen()
        persistState()
        return null
    }

    fun register(username: String, email: String, password: String): String? {
        val u = username.trim()
        val e = email.trim()
        if (u.isEmpty() || e.isEmpty() || password.isEmpty()) return "Fill in all fields."
        if (profileRepo.findByUsername(u).isPresent) return "Username already exists."
        val p = PlayerProfile(u, e)
        profileRepo.saveAccount(u, e, password, p)
        currentUsername = u
        prefs.edit().putString("last_user", u).apply()
        currentProfile = p
        // Desktop parity: new lesson baseline, then LOADING -> STORY_INTRO.
        engine.startNewLesson(0)
        engine.getSession().username = u
        pendingStoryLevel = 1
        resetRunUiState()
        loadingTarget = LoadingTarget.STORY
        screen = FlowState.LOADING
        playMusicForCurrentScreen()
        persistState()
        return null
    }

    /** Called by the loading screen when its parity 1800ms progress completes. */
    fun onLoadingFinished() {
        when (loadingTarget) {
            LoadingTarget.STORY -> goToStoryFor(1)
            LoadingTarget.LOGIN -> screen = FlowState.LOGIN
        }
        playMusicForCurrentScreen()
    }

    fun logout() {
        narration.stop()
        audio.stopMusic()
        currentUsername = null
        currentProfile = null
        prefs.edit().remove("last_user").apply()
        profileRepo.clearRunSnapshot()
        engine.getSession().resetAfterCancel(0)
        screen = FlowState.WELCOME
        playMusicForCurrentScreen()
    }

    fun saveCharacter(character: String, variant: Int) {
        val u = currentUsername ?: return
        val p = profileRepo.findByUsername(u).orElse(PlayerProfile(u, ""))
        p.character = character
        p.outfitVariant = variant.coerceIn(0, 3)
        profileRepo.saveProfile(p)
        currentProfile = p
        // Desktop parity: character confirm lands on the dashboard.
        screen = FlowState.DASHBOARD
        playMusicForCurrentScreen()
        persistState()
    }

    /** Desktop parity: dashboard PLAY starts a fresh run and goes straight to level 1 questions. */
    fun startLessonFromBeginning() {
        val u = currentUsername ?: return
        val saved = profileRepo.getLifetimeXp(u)
        engine.startNewLesson(saved)
        engine.getSession().username = u
        pendingStoryLevel = 1
        resetRunUiState()
        prepareQuestionForCurrentLevel()
        screen = FlowState.QUESTION
        playLevelMusic(1)
        persistState()
    }

    fun startLessonContinue() {
        startLessonFromBeginning()
    }

    private fun resetRunUiState() {
        answered = false
        awaitingAdvance = false
        lastAnswerCorrect = null
        syntaxInput = ""
        feedbackHeading = ""; feedbackBig = ""; feedbackDetail = ""
        exitDialogShown = false
        cancelPendingAdvance()
        endingChestOpened = false; endingTreasureDialogShown = false; endingXpDialogShown = false
        runningReason = null
    }

    fun goToStoryFor(level: Int) {
        pendingStoryLevel = level.coerceIn(1, GameConfig.LEVEL_COUNT)
        screen = FlowTransitions.storyStateForLevel(pendingStoryLevel)
        playMusicForCurrentScreen()
    }

    /**
     * Desktop parity: story intro button goes to character selection; level story buttons
     * (levels 2..5) launch the runner transition, whose finish starts the level questions.
     */
    fun continueFromStory() {
        narration.stop()
        val next = FlowTransitions.nextAfterStoryContinue(pendingStoryLevel)
        if (next == FlowState.CHARACTER) {
            screen = FlowState.CHARACTER
            playMusicForCurrentScreen()
            persistState()
            return
        }
        val targetLevel = pendingStoryLevel.coerceIn(2, GameConfig.LEVEL_COUNT)
        engine.getSession().level = targetLevel
        engine.getSession().questionIndex = 0
        runningReason = RunningReason.LEVEL_COMPLETE
        resetAnswerUiForRun()
        screen = FlowState.RUNNING_TRANSITION
        playLevelMusic(targetLevel)
        persistState()
    }

    private fun resetAnswerUiForRun() {
        answered = false
        awaitingAdvance = false
        lastAnswerCorrect = null
        syntaxInput = ""
        feedbackHeading = ""; feedbackBig = ""; feedbackDetail = ""
        cancelPendingAdvance()
    }

    private fun prepareQuestionForCurrentLevel() {
        answered = false
        awaitingAdvance = false
        lastAnswerCorrect = null
        syntaxInput = ""
        val q = engine.currentQuestion()
        questionTimeLeft = questionTimeLimitSeconds(q)
    }

    fun prepareQuestion() { prepareQuestionForCurrentLevel() }

    fun currentQuestion(): Question = engine.currentQuestion()

    /**
     * Android-only: typable/syntax questions get extra time (1-4s) based on answer complexity,
     * while multiple-choice questions keep the original timer.
     */
    fun questionTimeLimitSeconds(q: Question = currentQuestion()): Int {
        if (!q.syntax) return GameConfig.MCQ_SECONDS
        val expected = (q.expected ?: "").trim()
        var extra = 1
        val len = expected.length
        if (len >= 12) extra++
        if (len >= 24) extra++
        val complexity = expected.count { it in "<>{}()[]\"';:.,!&|+-*/_#=/" }
        if (complexity >= 8) extra++
        return GameConfig.SYNTAX_SECONDS + extra.coerceIn(1, 4)
    }

    /** Desktop showGamePopup strings, kept verbatim for parity. */
    private fun setFeedbackStrings(correct: Boolean, syntax: Boolean, timedOut: Boolean) {
        when {
            timedOut -> {
                feedbackHeading = "TIME'S UP!"
                feedbackBig = "-1 HEART"
                feedbackDetail = "Time Ran Out. Try the same question again."
            }
            correct && syntax -> {
                feedbackHeading = "CORRECT!"
                feedbackBig = "+10 XP"
                feedbackDetail = "Great Job! You completed the syntax!"
            }
            correct -> {
                feedbackHeading = "CORRECT!"
                feedbackBig = "+10 XP"
                feedbackDetail = "Great Job! You got the Correct Answer"
            }
            syntax -> {
                feedbackHeading = "NOT QUITE!"
                feedbackBig = "-1 HEART"
                feedbackDetail = "Check the syntax and try again."
            }
            else -> {
                feedbackHeading = "NOT QUITE!"
                feedbackBig = "-1 HEART"
                feedbackDetail = "Try again! You must perfect this question before moving to the next one."
            }
        }
    }

    fun submitChoice(choice: Int) {
        if (answered || awaitingAdvance || screen != FlowState.QUESTION) return
        val q = currentQuestion()
        val correct = (choice == q.correct)
        lastAnswerCorrect = correct
        setFeedbackStrings(correct, syntax = false, timedOut = false)
        feedback(correct)
        answered = true
        awaitingAdvance = true
        pendingAdvanceAction = scheduler.post(answerFeedbackDelayMs) {
            pendingAdvanceAction = null
            if (screen == FlowState.QUESTION && isResumed) applyResult(engine.submitChoice(choice))
            else awaitingAdvance = false
        }
    }

    fun submitSyntax() {
        if (answered || awaitingAdvance || screen != FlowState.QUESTION) return
        val expected = (currentQuestion().expected ?: "").trim().replace("\\s+".toRegex(), " ")
        val actual = syntaxInput.trim().replace("\\s+".toRegex(), " ")
        val ok = actual.equals(expected, ignoreCase = true)
        lastAnswerCorrect = ok
        setFeedbackStrings(ok, syntax = true, timedOut = false)
        feedback(ok)
        answered = true
        awaitingAdvance = true
        pendingAdvanceAction = scheduler.post(answerFeedbackDelayMs) {
            pendingAdvanceAction = null
            if (screen == FlowState.QUESTION && isResumed) applyResult(engine.submitSyntax(syntaxInput))
            else awaitingAdvance = false
        }
    }

    fun timeoutQuestion() {
        if (answered || awaitingAdvance || screen != FlowState.QUESTION) return
        answered = true
        awaitingAdvance = true
        lastAnswerCorrect = false
        setFeedbackStrings(correct = false, syntax = false, timedOut = true)
        feedback(false)
        pendingAdvanceAction = scheduler.post(answerFeedbackDelayMs) {
            pendingAdvanceAction = null
            if (screen == FlowState.QUESTION && isResumed) applyResult(engine.timeout())
            else awaitingAdvance = false
        }
    }

    /** User-confirmed advance (CONTINUE on the feedback popup), desktop showGamePopup parity. */
    fun advanceFeedbackNow() {
        if (!awaitingAdvance) return
        val action = pendingAdvanceAction ?: run {
            awaitingAdvance = false
            return
        }
        cancelPendingAdvance()
        answered = false
        action.run()
    }

    fun isDemoAccount(): Boolean = (currentUsername ?: "").trim().equals("demo", ignoreCase = true)

    /**
     * Demo-only testing cheat: complete the current stage perfectly and jump straight to narration/ending flow.
     * Triggered via a special Android gesture (long-press question HUD) for testing animations.
     */
    fun cheatCompleteCurrentStageForTesting() {
        if (!isDemoAccount()) return
        if (screen != FlowState.QUESTION) return
        if (engine.getSession().sessionCommitted) return

        cancelPendingAdvance()
        awaitingAdvance = false
        answered = false
        lastAnswerCorrect = true
        feedbackHeading = ""; feedbackBig = ""; feedbackDetail = ""
        exitDialogShown = false
        syntaxInput = ""

        val result = engine.cheatCompleteCurrentStagePerfectly()
        applyResult(result)
    }

    private fun applyResult(result: LessonEngine.AnswerResult) {
        pendingAdvanceAction = null
        feedbackHeading = ""; feedbackBig = ""; feedbackDetail = ""
        val u = currentUsername
        if (FlowTransitions.shouldCommitSession(result) && u != null) {
            if (engine.getSession().canCommit()) {
                val runXp = engine.getXp().coerceAtLeast(0)
                endingFinalXp = runXp
                try { profileRepo.commitAttemptXp(u, runXp) } catch (_: Exception) {}
                try { engine.markSessionCommitted() } catch (_: Exception) {}
                try {
                    // Keep session/dashboard baseline in sync with newly committed lifetime XP.
                    engine.getSession().savedXpBeforeRun = profileRepo.getLifetimeXp(u).coerceAtLeast(0)
                    engine.getSession().showZeroXpAfterCancelledRun = false
                } catch (_: Exception) {}
                refreshProfile()
            } else if (endingFinalXp <= 0) {
                endingFinalXp = engine.getXp().coerceAtLeast(0)
            }
        }
        val levelAfter = engine.getLevel()
        val next = FlowTransitions.nextAfterAnswer(result, levelAfter)
        awaitingAdvance = false
        answered = false
        lastAnswerCorrect = null
        when (result) {
            LessonEngine.AnswerResult.CORRECT -> {
                screen = FlowState.QUESTION
                prepareQuestionForCurrentLevel()
            }
            LessonEngine.AnswerResult.WRONG, LessonEngine.AnswerResult.TIMEOUT_WRONG -> {
                if (next == FlowState.GAMEOVER) {
                    resultMessage = "You ran out of hearts."
                    screen = FlowState.GAMEOVER
                    playMusicForCurrentScreen()
                } else {
                    screen = FlowState.QUESTION
                    prepareQuestionForCurrentLevel()
                }
            }
            LessonEngine.AnswerResult.LEVEL_COMPLETE -> {
                // Desktop parity: level completion shows the level story first, then the runner.
                resultMessage = ""
                pendingStoryLevel = levelAfter.coerceIn(2, GameConfig.LEVEL_COUNT)
                screen = next
                playMusicForCurrentScreen()
            }
            LessonEngine.AnswerResult.GAME_COMPLETE -> {
                resultMessage = ""
                endingChestOpened = false; endingTreasureDialogShown = false; endingXpDialogShown = false
                screen = FlowState.ENDING
                playMusicForCurrentScreen()
            }
            LessonEngine.AnswerResult.GAME_OVER -> {
                resultMessage = "You ran out of hearts."
                screen = FlowState.GAMEOVER
                playMusicForCurrentScreen()
            }
        }
        persistState()
    }

    /** Runner finished (desktop MovingRunner.onFinished -> startQuestion). */
    fun onRunningFinished() {
        if (screen != FlowState.RUNNING_TRANSITION) return
        val sess = engine.getSession()
        runningReason = null
        if (sess.hearts <= 0) {
            resultMessage = "You ran out of hearts."
            screen = FlowState.GAMEOVER
            playMusicForCurrentScreen()
            persistState()
            return
        }
        if (sess.sessionCommitted && engine.getLevel() >= GameConfig.LEVEL_COUNT && engine.getQuestionIndex() == 0) {
            endingChestOpened = false; endingTreasureDialogShown = false; endingXpDialogShown = false
            if (endingFinalXp <= 0) endingFinalXp = sess.xp.coerceAtLeast(0)
            screen = FlowState.ENDING
            playMusicForCurrentScreen()
            persistState()
            return
        }
        prepareQuestionForCurrentLevel()
        screen = FlowState.QUESTION
        playLevelMusic(engine.getLevel())
        persistState()
    }

    fun onEndingChestOpened() {
        if (screen != FlowState.ENDING) return
        if (endingChestOpened) return
        endingChestOpened = true
        playVictoryMusic()
        // dialogs are driven by UI state for parity (treasure note -> XP summary)
        endingTreasureDialogShown = true
        endingXpDialogShown = false
        persistState()
    }

    fun onTreasureDialogContinue() {
        if (screen != FlowState.ENDING) return
        endingTreasureDialogShown = false
        endingXpDialogShown = true
        // keep endingFinalXp as the committed run XP snapshot for dialog parity where possible
        if (endingFinalXp <= 0) endingFinalXp = (currentProfile?.lifetimeXp ?: engine.getXp()).coerceAtLeast(0)
        persistState()
    }

    fun onEndingXpMenu() {
        if (screen != FlowState.ENDING) return
        endingXpDialogShown = false
        endingTreasureDialogShown = false
        returnToDashboardFromResult()
    }

    fun onEndingXpRetry() {
        if (screen != FlowState.ENDING) return
        endingXpDialogShown = false
        endingTreasureDialogShown = false
        endingChestOpened = false
        startLessonFromBeginning()
    }

    fun returnToDashboardFromResult() {
        // Desktop parity: HOME from result screens cancels the unfinished/finished run state.
        cancelPendingAdvance()
        val u = currentUsername
        val saved = if (u != null) profileRepo.getLifetimeXp(u) else 0
        engine.cancelRun(saved)
        resetRunUiState()
        screen = FlowState.DASHBOARD
        playMusicForCurrentScreen()
        persistState()
    }

    fun openProfile() { screen = FlowState.PROFILE; playMusicForCurrentScreen() }
    fun backToDashboard() { refreshDashboardXpBaseline(); screen = FlowState.DASHBOARD; playMusicForCurrentScreen(); persistState() }

    private fun refreshDashboardXpBaseline() {
        val u = currentUsername ?: return
        val saved = try { profileRepo.getLifetimeXp(u).coerceAtLeast(0) } catch (_: Exception) { engine.getSession().savedXpBeforeRun.coerceAtLeast(0) }
        val sess = engine.getSession()
        sess.savedXpBeforeRun = saved
        sess.showZeroXpAfterCancelledRun = false
        currentProfile = try { profileRepo.findByUsername(u).orElse(currentProfile) } catch (_: Exception) { currentProfile }
    }

    // --- desktop showGameExitDialog parity (QUESTION screen) ---
    fun openExitDialog() {
        if (screen != FlowState.QUESTION || exitDialogShown) return
        exitDialogShown = true
    }

    fun dismissExitDialog() {
        exitDialogShown = false
    }

    // --- desktop LOGOUT message screen parity (dashboard EXIT) ---
    fun openLogoutDialog() {
        if (logoutDialogShown) return
        logoutDialogShown = true
    }

    fun dismissLogoutDialog() {
        logoutDialogShown = false
    }

    fun confirmLogout() {
        logoutDialogShown = false
        logout()
    }

    fun cancelRunAndDashboard() {
        narration.stop()
        cancelPendingAdvance()
        scheduler.cancelAll()
        exitDialogShown = false
        val u = currentUsername
        val saved = if (u != null) profileRepo.getLifetimeXp(u) else 0
        engine.cancelRun(saved)
        resetRunUiState()
        refreshProfile()
        screen = FlowState.DASHBOARD
        playMusicForCurrentScreen()
        persistState()
    }

    fun playMusicForCurrentScreen() {
        if (!audio.isEnabled) return
        // Desktop showScreen() music mapping, kept 1:1 where a track is determinable.
        when (screen) {
            FlowState.WELCOME, FlowState.REGISTER, FlowState.CHARACTER, FlowState.DASHBOARD -> audio.playMusic(AudioCatalog.TRACK_MAIN_MENU, true)
            FlowState.LOADING -> { /* desktop keeps the previous (menu) track during loading */ }
            FlowState.LOGIN, FlowState.PROFILE -> playLevelMusic(5)
            FlowState.STORY_INTRO -> audio.playMusic(AudioCatalog.TRACK_DUNGEON, true)
            FlowState.STORY_LEVEL2 -> playLevelMusic(2)
            FlowState.STORY_LEVEL3 -> playLevelMusic(3)
            FlowState.STORY_LEVEL4 -> playLevelMusic(4)
            FlowState.STORY_LEVEL5 -> playLevelMusic(5)
            FlowState.QUESTION, FlowState.RUNNING_TRANSITION -> playLevelMusic(engine.getLevel())
            FlowState.ENDING -> playLevelMusic(5)
            FlowState.COMPLETE -> audio.playMusic(AudioCatalog.TRACK_VICTORY, true)
            FlowState.GAMEOVER -> audio.playMusic(AudioCatalog.TRACK_DUNGEON, true)
            FlowState.LOGOUT -> audio.stopMusic()
            else -> {}
        }
    }

    fun playLevelMusic(level: Int) {
        if (!audio.isEnabled) return
        val meta = LevelMetadata.of(level)
        audio.playMusic(meta.musicTrack, true)
    }

    fun playVictoryMusic() { if (audio.isEnabled) audio.playMusic(AudioCatalog.TRACK_VICTORY, true) }

    fun speak(text: String) {
        storyHighlightRange = null
        if (ttsEnabled && narration.isSupported) narration.speakStory(text)
    }
    fun stopNarration() { storyHighlightRange = null; narration.stop() }

    fun feedback(correct: Boolean) { try { audio.playFeedback(correct) } catch (_: Exception) {} }
}
