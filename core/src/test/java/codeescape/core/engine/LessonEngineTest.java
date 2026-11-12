package codeescape.core.engine;

import codeescape.core.content.GameConfig;
import codeescape.core.flow.FlowTransitions;
import codeescape.core.model.GameSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LessonEngineTest {

    @Test
    void startNewLessonResetsAttemptState() {
        LessonEngine engine = new LessonEngine();
        engine.startNewLesson(42);
        GameSession s = engine.getSession();
        assertEquals(1, s.level);
        assertEquals(0, s.questionIndex);
        assertEquals(GameConfig.STARTING_HEARTS, s.hearts);
        assertEquals(0, s.xp);
        assertEquals(0, s.correctAnswers);
        assertFalse(s.sessionCommitted);
        assertFalse(s.showZeroXpAfterCancelledRun);
        assertEquals(42, s.savedXpBeforeRun);
    }

    @Test
    void correctAnswerAwardsXpAndAdvancesQuestion() {
        LessonEngine engine = new LessonEngine();
        engine.startNewLesson(0);
        int correct = engine.currentQuestion().correct;
        LessonEngine.AnswerResult result = engine.submitChoice(correct);
        assertEquals(LessonEngine.AnswerResult.CORRECT, result);
        assertEquals(GameConfig.XP_PER_CORRECT, engine.getXp());
        assertEquals(1, engine.getCorrectAnswers());
        assertEquals(1, engine.getQuestionIndex());
        assertEquals(GameConfig.STARTING_HEARTS, engine.getHearts());
    }

    @Test
    void wrongAnswerLosesHeartAndKeepsQuestion() {
        LessonEngine engine = new LessonEngine();
        engine.startNewLesson(0);
        int correct = engine.currentQuestion().correct;
        int wrong = (correct + 1) % 4;
        int qi = engine.getQuestionIndex();
        LessonEngine.AnswerResult result = engine.submitChoice(wrong);
        assertEquals(LessonEngine.AnswerResult.WRONG, result);
        assertEquals(GameConfig.STARTING_HEARTS - 1, engine.getHearts());
        assertEquals(0, engine.getXp());
        assertEquals(qi, engine.getQuestionIndex());
    }

    @Test
    void timeoutCountsAsWrongAndCanCauseGameOver() {
        LessonEngine engine = new LessonEngine();
        engine.startNewLesson(0);
        engine.setHearts(1);
        LessonEngine.AnswerResult result = engine.timeout();
        assertEquals(LessonEngine.AnswerResult.GAME_OVER, result);
        assertTrue(engine.getHearts() <= 0);
        assertTrue(FlowTransitions.shouldCommitSession(result));
    }

    @Test
    void completingTenQuestionsAdvancesLevel() {
        LessonEngine engine = new LessonEngine();
        engine.startNewLesson(0);
        LessonEngine.AnswerResult last = null;
        for (int i = 0; i < GameConfig.QUESTIONS_PER_LEVEL; i++) {
            last = engine.submitChoice(engine.currentQuestion().correct);
        }
        assertEquals(LessonEngine.AnswerResult.LEVEL_COMPLETE, last);
        assertEquals(2, engine.getLevel());
        assertEquals(0, engine.getQuestionIndex());
        assertEquals(GameConfig.XP_PER_CORRECT * GameConfig.QUESTIONS_PER_LEVEL, engine.getXp());
        assertFalse(FlowTransitions.shouldCommitSession(last));
    }

    @Test
    void completingAllLevelsReturnsGameComplete() {
        LessonEngine engine = new LessonEngine();
        engine.startNewLesson(0);
        LessonEngine.AnswerResult last = null;
        int total = GameConfig.LEVEL_COUNT * GameConfig.QUESTIONS_PER_LEVEL;
        for (int i = 0; i < total; i++) {
            last = engine.submitChoice(engine.currentQuestion().correct);
        }
        assertEquals(LessonEngine.AnswerResult.GAME_COMPLETE, last);
        assertTrue(FlowTransitions.shouldCommitSession(last));
    }

    @Test
    void cancelRunResetsAttemptAndMarksNoCommitDisplay() {
        LessonEngine engine = new LessonEngine();
        engine.startNewLesson(10);
        engine.submitChoice(engine.currentQuestion().correct);
        engine.cancelRun(55);
        GameSession s = engine.getSession();
        assertEquals(0, s.xp);
        assertEquals(0, s.correctAnswers);
        assertEquals(1, s.level);
        assertEquals(0, s.questionIndex);
        assertEquals(GameConfig.STARTING_HEARTS, s.hearts);
        assertFalse(s.sessionCommitted);
        assertTrue(s.showZeroXpAfterCancelledRun);
        assertEquals(55, s.savedXpBeforeRun);
        assertEquals(0, s.displayedSavedXp());
    }

    @Test
    void markSessionCommittedSetsCommittedFlag() {
        LessonEngine engine = new LessonEngine();
        engine.startNewLesson(5);
        assertTrue(engine.getSession().canCommit());
        engine.markSessionCommitted();
        assertTrue(engine.getSession().sessionCommitted);
        assertFalse(engine.getSession().canCommit());
        assertFalse(engine.getSession().showZeroXpAfterCancelledRun);
    }
}
