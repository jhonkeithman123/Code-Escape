package codeescape.core.flow;

import codeescape.core.engine.LessonEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FlowTransitionsTest {

    @Test
    void mapsAnswerResultsToExpectedFlowStates() {
        assertEquals(FlowState.QUESTION, FlowTransitions.nextAfterAnswer(LessonEngine.AnswerResult.CORRECT, 1));
        assertEquals(FlowState.QUESTION, FlowTransitions.nextAfterAnswer(LessonEngine.AnswerResult.WRONG, 1));
        assertEquals(FlowState.QUESTION, FlowTransitions.nextAfterAnswer(LessonEngine.AnswerResult.TIMEOUT_WRONG, 1));
        assertEquals(FlowState.STORY_LEVEL2, FlowTransitions.nextAfterAnswer(LessonEngine.AnswerResult.LEVEL_COMPLETE, 2));
        assertEquals(FlowState.STORY_LEVEL5, FlowTransitions.nextAfterAnswer(LessonEngine.AnswerResult.LEVEL_COMPLETE, 5));
        assertEquals(FlowState.ENDING, FlowTransitions.nextAfterAnswer(LessonEngine.AnswerResult.GAME_COMPLETE, 5));
        assertEquals(FlowState.GAMEOVER, FlowTransitions.nextAfterAnswer(LessonEngine.AnswerResult.GAME_OVER, 3));
    }

    @Test
    void commitRulesMatchCompleteOrGameOverOnly() {
        assertFalse(FlowTransitions.shouldCommitSession(LessonEngine.AnswerResult.CORRECT));
        assertFalse(FlowTransitions.shouldCommitSession(LessonEngine.AnswerResult.WRONG));
        assertFalse(FlowTransitions.shouldCommitSession(LessonEngine.AnswerResult.TIMEOUT_WRONG));
        assertFalse(FlowTransitions.shouldCommitSession(LessonEngine.AnswerResult.LEVEL_COMPLETE));
        assertTrue(FlowTransitions.shouldCommitSession(LessonEngine.AnswerResult.GAME_COMPLETE));
        assertTrue(FlowTransitions.shouldCommitSession(LessonEngine.AnswerResult.GAME_OVER));
    }

    @Test
    void screenNamesMatchLegacyDesktopCards() {
        assertEquals("QUESTION", FlowTransitions.screenName(FlowState.QUESTION));
        assertEquals("RUNNING", FlowTransitions.screenName(FlowState.RUNNING_TRANSITION));
        assertEquals("STORY_LEVEL3", FlowTransitions.screenName(FlowState.STORY_LEVEL3));
        assertEquals("ENDING", FlowTransitions.screenName(FlowState.ENDING));
    }

    @Test
    void targetForMapsAppLevelEventsToUnambiguousStates() {
        assertEquals(FlowState.LOGIN, FlowTransitions.targetFor(FlowEvent.of(FlowEvent.Type.STARTUP_COMPLETE)));
        assertEquals(FlowState.DASHBOARD, FlowTransitions.targetFor(FlowEvent.of(FlowEvent.Type.DASHBOARD_REQUESTED)));
        assertEquals(FlowState.PROFILE, FlowTransitions.targetFor(FlowEvent.of(FlowEvent.Type.PROFILE_OPENED)));
        assertEquals(FlowState.QUESTION, FlowTransitions.targetFor(FlowEvent.of(FlowEvent.Type.LESSON_STARTED)));
        assertEquals(FlowState.WELCOME, FlowTransitions.targetFor(FlowEvent.of(FlowEvent.Type.LOGOUT_CONFIRMED)));
        assertNull(FlowTransitions.targetFor(null));
    }
}
