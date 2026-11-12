package codeescape.core.flow;

import codeescape.core.engine.LessonEngine;

/**
 * Domain/UI events that may drive flow transitions. Java 8 compatible value object.
 */
public final class FlowEvent {
    public enum Type {
        STARTUP_COMPLETE,
        LOGIN_SUCCESS,
        REGISTER_SUCCESS,
        CHARACTER_CONFIRMED,
        STORY_CONTINUE,
        LESSON_STARTED,
        QUESTION_READY,
        ANSWER_RESULT,
        RUN_TRANSITION_FINISHED,
        PROFILE_OPENED,
        DASHBOARD_REQUESTED,
        RUN_CANCELLED,
        LOGOUT_REQUESTED,
        LOGOUT_CONFIRMED
    }

    public final Type type;
    public final LessonEngine.AnswerResult answerResult;
    public final int level;

    private FlowEvent(Type type, LessonEngine.AnswerResult answerResult, int level) {
        this.type = type;
        this.answerResult = answerResult;
        this.level = level;
    }

    public static FlowEvent of(Type type) { return new FlowEvent(type, null, 0); }
    public static FlowEvent answer(LessonEngine.AnswerResult result, int level) {
        return new FlowEvent(Type.ANSWER_RESULT, result, level);
    }
    public static FlowEvent level(Type type, int level) { return new FlowEvent(type, null, level); }
}
