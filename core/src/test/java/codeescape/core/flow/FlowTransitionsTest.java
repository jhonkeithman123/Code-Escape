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

    @Test
    void lifecycleAndContextSensitiveEventsRequireResolve() {
        assertNull(FlowTransitions.targetFor(FlowEvent.of(FlowEvent.Type.APP_PAUSED)));
        assertNull(FlowTransitions.targetFor(FlowEvent.of(FlowEvent.Type.APP_RESUMED)));
        assertNull(FlowTransitions.targetFor(FlowEvent.of(FlowEvent.Type.APP_STOPPING)));
        assertNull(FlowTransitions.targetFor(FlowEvent.storyContinue(2)));
        assertEquals(FlowState.CHARACTER, FlowTransitions.resolve(FlowState.STORY_INTRO, FlowEvent.storyContinue(1)));
        assertEquals(FlowState.RUNNING_TRANSITION, FlowTransitions.resolve(FlowState.STORY_LEVEL2, FlowEvent.storyContinue(2)));
        assertEquals(FlowState.RUNNING_TRANSITION, FlowTransitions.resolve(FlowState.STORY_LEVEL5, FlowEvent.level(FlowEvent.Type.STORY_CONTINUE, 5)));
        // Preserve existing unambiguous contract for run-transition finish/cancel routing.
        assertEquals(FlowState.DASHBOARD, FlowTransitions.resolve(FlowState.RUNNING_TRANSITION, FlowEvent.of(FlowEvent.Type.RUN_TRANSITION_FINISHED)));
        assertEquals(FlowState.DASHBOARD, FlowTransitions.resolve(FlowState.QUESTION, FlowEvent.of(FlowEvent.Type.RUN_CANCELLED)));
    }

    @Test
    void dispatchInvokesNavigatorOnlyWhenResolved() {
        final FlowState[] seen = new FlowState[1];
        final int[] levels = new int[1];
        Navigator nav = new Navigator() {
            @Override public void navigate(FlowState state) { seen[0] = state; }
            @Override public void navigate(FlowState state, int level) { seen[0] = state; levels[0] = level; }
        };
        assertEquals(FlowState.LOGIN, FlowTransitions.dispatch(nav, FlowState.WELCOME, FlowEvent.of(FlowEvent.Type.STARTUP_COMPLETE)));
        assertEquals(FlowState.LOGIN, seen[0]);
        seen[0] = null;
        assertNull(FlowTransitions.dispatch(nav, FlowState.QUESTION, FlowEvent.of(FlowEvent.Type.APP_PAUSED)));
        assertNull(seen[0]);
        assertEquals(FlowState.RUNNING_TRANSITION, FlowTransitions.dispatch(nav, FlowState.STORY_LEVEL3, FlowEvent.storyContinue(3)));
        assertEquals(3, levels[0]);
    }

    @Test
    void answerEventMapsToNextStateWithLevelContext() {
        FlowEvent e = FlowEvent.answer(LessonEngine.AnswerResult.LEVEL_COMPLETE, 3);
        assertEquals(FlowState.STORY_LEVEL3, FlowTransitions.targetFor(e));
        assertEquals(FlowState.STORY_LEVEL3, FlowTransitions.resolve(FlowState.QUESTION, e));
        assertTrue(FlowTransitions.shouldPauseInteractiveQuestion(FlowState.QUESTION, false, false));
        assertFalse(FlowTransitions.shouldPauseInteractiveQuestion(FlowState.QUESTION, true, false));
        assertFalse(FlowTransitions.shouldPauseInteractiveQuestion(FlowState.DASHBOARD, false, false));
    }

    @Test
    void routePathMatchesAndroidNavigationRoutes() {
        assertEquals("welcome", FlowTransitions.routePath(FlowState.WELCOME));
        assertEquals("login", FlowTransitions.routePath(FlowState.LOGIN));
        assertEquals("story", FlowTransitions.routePath(FlowState.STORY_INTRO));
        assertEquals("story", FlowTransitions.routePath(FlowState.STORY_LEVEL5));
        assertEquals("running", FlowTransitions.routePath(FlowState.RUNNING_TRANSITION));
        assertEquals("question", FlowTransitions.routePath(FlowState.QUESTION));
        assertEquals("ending", FlowTransitions.routePath(FlowState.ENDING));
        assertEquals("welcome", FlowTransitions.routePath(null));
    }

    @Test
    void storyStateHelpersAreConsistentAcrossFrontends() {
        assertTrue(FlowTransitions.isStoryState(FlowState.STORY_INTRO));
        assertTrue(FlowTransitions.isStoryState(FlowState.STORY_LEVEL3));
        assertFalse(FlowTransitions.isStoryState(FlowState.QUESTION));
        assertEquals(1, FlowTransitions.storyLevelForState(FlowState.STORY_INTRO));
        assertEquals(4, FlowTransitions.storyLevelForState(FlowState.STORY_LEVEL4));
        assertEquals(-1, FlowTransitions.storyLevelForState(FlowState.DASHBOARD));
        assertEquals(FlowState.CHARACTER, FlowTransitions.nextAfterStoryContinue(1));
        assertEquals(FlowState.RUNNING_TRANSITION, FlowTransitions.nextAfterStoryContinue(2));
        assertEquals(FlowState.RUNNING_TRANSITION, FlowTransitions.nextAfterStoryContinue(5));
    }

    @Test
    void everyFlowStateHasDesktopScreenAndRouteMappings() {
        for (FlowState state : FlowState.values()) {
            assertNotNull(FlowTransitions.screenName(state), state.name());
            assertFalse(FlowTransitions.screenName(state).trim().isEmpty(), state.name());
            assertNotNull(FlowTransitions.routePath(state), state.name());
            assertFalse(FlowTransitions.routePath(state).trim().isEmpty(), state.name());
        }
    }
}
