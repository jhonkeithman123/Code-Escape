package codeescape.core.flow;

import codeescape.core.engine.LessonEngine;

/**
 * Pure transition helpers mapping domain outcomes to navigation targets.
 * Keeps screen-routing rules in core while frontends render/navigate.
 *
 * <p>Frontends remain responsible for rendering, timers, animations, persistence side effects,
 * and platform lifecycle APIs, but should prefer these helpers for parity-safe routing decisions
 * and cross-frontend-consistent event dispatch where a target is unambiguous.</p>
 */
public final class FlowTransitions {
    private FlowTransitions() {}

    public static FlowState storyStateForLevel(int level) {
        switch (level) {
            case 2: return FlowState.STORY_LEVEL2;
            case 3: return FlowState.STORY_LEVEL3;
            case 4: return FlowState.STORY_LEVEL4;
            case 5: return FlowState.STORY_LEVEL5;
            default: return FlowState.STORY_INTRO;
        }
    }

    /** True for story/introduction flow states. */
    public static boolean isStoryState(FlowState state) {
        switch (state) {
            case STORY_INTRO:
            case STORY_LEVEL2:
            case STORY_LEVEL3:
            case STORY_LEVEL4:
            case STORY_LEVEL5:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns the story level represented by a story state (1 for intro/level-1 context, 2..5 for later levels),
     * or -1 if the state is not a story state.
     */
    public static int storyLevelForState(FlowState state) {
        if (state == null) return -1;
        switch (state) {
            case STORY_INTRO: return 1;
            case STORY_LEVEL2: return 2;
            case STORY_LEVEL3: return 3;
            case STORY_LEVEL4: return 4;
            case STORY_LEVEL5: return 5;
            default: return -1;
        }
    }

    /**
     * Cross-frontend parity rule for continuing from a story page:
     * intro (level 1 context) continues to character selection; level 2..5 stories continue to the running transition.
     */
    public static FlowState nextAfterStoryContinue(int storyLevel) {
        return storyLevel <= 1 ? FlowState.CHARACTER : FlowState.RUNNING_TRANSITION;
    }

    /** Overload using a story flow state as context; non-story states default to intro behavior. */
    public static FlowState nextAfterStoryContinue(FlowState storyState) {
        int level = storyLevelForState(storyState);
        return nextAfterStoryContinue(level <= 0 ? 1 : level);
    }

    /** Maps a completed answer/timeout result to the next flow state. */
    public static FlowState nextAfterAnswer(LessonEngine.AnswerResult result, int levelAfterResult) {
        if (result == null) return FlowState.QUESTION;
        switch (result) {
            case CORRECT:
            case WRONG:
            case TIMEOUT_WRONG:
                return FlowState.QUESTION;
            case LEVEL_COMPLETE:
                return storyStateForLevel(levelAfterResult);
            case GAME_COMPLETE:
                return FlowState.ENDING;
            case GAME_OVER:
                return FlowState.GAMEOVER;
            default:
                return FlowState.QUESTION;
        }
    }

    public static boolean shouldCommitSession(LessonEngine.AnswerResult result) {
        return result == LessonEngine.AnswerResult.GAME_COMPLETE
                || result == LessonEngine.AnswerResult.GAME_OVER;
    }

    /** Maps a flow state to the legacy desktop CardLayout screen name where one exists. */
    public static String screenName(FlowState state) {
        if (state == null) return "WELCOME";
        switch (state) {
            case WELCOME: return "WELCOME";
            case LOADING: return "LOADING";
            case LOGIN: return "LOGIN";
            case REGISTER: return "REGISTER";
            case CHARACTER: return "CHARACTER";
            case STORY_INTRO: return "STORY_INTRO";
            case STORY_LEVEL2: return "STORY_LEVEL2";
            case STORY_LEVEL3: return "STORY_LEVEL3";
            case STORY_LEVEL4: return "STORY_LEVEL4";
            case STORY_LEVEL5: return "STORY_LEVEL5";
            case DASHBOARD: return "DASHBOARD";
            case QUESTION: return "QUESTION";
            case RUNNING_TRANSITION: return "RUNNING";
            case PROFILE: return "PROFILE";
            case COMPLETE: return "COMPLETE";
            case GAMEOVER: return "GAMEOVER";
            case ENDING: return "ENDING";
            case LOGOUT: return "LOGOUT";
            default: return state.name();
        }
    }

    /**
     * Maps a flow state to the Android/Compose navigation route id used by the migrated frontend.
     * Story states intentionally share the "story" route (screen state selects content).
     */
    public static String routePath(FlowState state) {
        if (state == null) return "welcome";
        switch (state) {
            case WELCOME: return "welcome";
            case LOADING: return "loading";
            case LOGIN: return "login";
            case REGISTER: return "register";
            case CHARACTER: return "character";
            case STORY_INTRO:
            case STORY_LEVEL2:
            case STORY_LEVEL3:
            case STORY_LEVEL4:
            case STORY_LEVEL5: return "story";
            case DASHBOARD: return "dashboard";
            case QUESTION: return "question";
            case RUNNING_TRANSITION: return "running";
            case PROFILE: return "profile";
            case COMPLETE: return "complete";
            case GAMEOVER: return "gameover";
            case ENDING: return "ending";
            case LOGOUT: return "logout";
            default: return state.name().toLowerCase();
        }
    }

    /** Maps broad app-level flow events to target states where the contract is unambiguous. */
    public static FlowState targetFor(FlowEvent event) {
        if (event == null || event.type == null) return null;
        if (event.isLifecycleEvent()) return null;
        switch (event.type) {
            case STARTUP_COMPLETE: return FlowState.LOGIN;
            case LOGIN_SUCCESS:
            case REGISTER_SUCCESS:
            case DASHBOARD_REQUESTED:
            case RUN_CANCELLED:
            case RUN_TRANSITION_FINISHED: return FlowState.DASHBOARD;
            case CHARACTER_CONFIRMED: return FlowState.STORY_INTRO;
            case STORY_CONTINUE:
                // Event-level story continuation is context-sensitive (intro->character, level story->running).
                // Callers needing the parity decision should use nextAfterStoryContinue/resolve.
                return null;
            case LESSON_STARTED: return FlowState.QUESTION;
            case QUESTION_READY: return FlowState.QUESTION;
            case ANSWER_RESULT: return nextAfterAnswer(event.answerResult, event.level);
            case PROFILE_OPENED: return FlowState.PROFILE;
            case LOGOUT_REQUESTED: return FlowState.LOGOUT;
            case LOGOUT_CONFIRMED: return FlowState.WELCOME;
            default: return null;
        }
    }

    /**
     * Context-sensitive resolution for event dispatch. Falls back to {@link #targetFor(FlowEvent)}
     * for unambiguous events, and resolves selected context-dependent events in a parity-safe way.
     * Returns {@code null} when no navigation should be applied.
     */
    public static FlowState resolve(FlowState current, FlowEvent event) {
        FlowState unambiguous = targetFor(event);
        if (unambiguous != null) return unambiguous;
        if (event == null || event.type == null) return null;
        switch (event.type) {
            case STORY_CONTINUE: {
                int level = event.level;
                if (level <= 0 && isStoryState(current)) level = storyLevelForState(current);
                if (level <= 0) level = 1;
                // Continuation from intro remains CHARACTER (parity). Continuation from level story
                // targets RUNNING_TRANSITION for the pending story level; frontends own runner setup.
                return nextAfterStoryContinue(level);
            }
            case RUN_TRANSITION_FINISHED:
                // Finishing a run/transition commonly resumes question flow; dashboard-specific cases
                // are owned by frontend state and should not be forced here.
                return FlowState.QUESTION;
            default:
                return null;
        }
    }

    /**
     * Dispatches an event through the core contract. If a target state is resolved, the navigator
     * is invoked with the event's level context. Lifecycle events are ignored.
     * @return resolved target state, or {@code null} if no navigation was performed
     */
    public static FlowState dispatch(Navigator navigator, FlowState current, FlowEvent event) {
        if (navigator == null) return null;
        FlowState target = resolve(current, event);
        if (target == null) return null;
        navigator.navigate(target, event != null ? event.level : 0);
        return target;
    }

    /** Lifecycle helper: question timers/progress should pause while app/screen is not resumed in interactive question flow. */
    public static boolean shouldPauseInteractiveQuestion(FlowState state, boolean answered, boolean awaitingAdvance) {
        return state == FlowState.QUESTION && !answered && !awaitingAdvance;
    }
}
