package codeescape.core.flow;

import codeescape.core.engine.LessonEngine;

/**
 * Pure transition helpers mapping domain outcomes to navigation targets.
 * Keeps screen-routing rules in core while frontends render/navigate.
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

    /** Maps broad app-level flow events to target states where the contract is unambiguous. */
    public static FlowState targetFor(FlowEvent event) {
        if (event == null || event.type == null) return null;
        switch (event.type) {
            case STARTUP_COMPLETE: return FlowState.LOGIN;
            case LOGIN_SUCCESS:
            case REGISTER_SUCCESS:
            case DASHBOARD_REQUESTED:
            case RUN_CANCELLED:
            case RUN_TRANSITION_FINISHED: return FlowState.DASHBOARD;
            case CHARACTER_CONFIRMED: return FlowState.STORY_INTRO;
            case STORY_CONTINUE: return storyStateForLevel(event.level);
            case LESSON_STARTED: return FlowState.QUESTION;
            case QUESTION_READY: return FlowState.QUESTION;
            case ANSWER_RESULT: return nextAfterAnswer(event.answerResult, event.level);
            case PROFILE_OPENED: return FlowState.PROFILE;
            case LOGOUT_REQUESTED: return FlowState.LOGOUT;
            case LOGOUT_CONFIRMED: return FlowState.WELCOME;
            default: return null;
        }
    }
}
