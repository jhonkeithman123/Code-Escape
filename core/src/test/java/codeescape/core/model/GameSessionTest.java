package codeescape.core.model;

import codeescape.core.content.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameSessionTest {

    @Test
    void resetForNewLessonClearsRunButKeepsSavedBaseline() {
        GameSession s = new GameSession();
        s.xp = 50; s.hearts = 1; s.level = 3; s.questionIndex = 7; s.correctAnswers = 12; s.sessionCommitted = true;
        s.resetForNewLesson(120);
        assertEquals(0, s.xp);
        assertEquals(GameConfig.STARTING_HEARTS, s.hearts);
        assertEquals(1, s.level);
        assertEquals(0, s.questionIndex);
        assertEquals(0, s.correctAnswers);
        assertFalse(s.sessionCommitted);
        assertFalse(s.showZeroXpAfterCancelledRun);
        assertEquals(120, s.savedXpBeforeRun);
        assertEquals(120, s.displayedSavedXp());
    }

    @Test
    void cancelAndCommitFlagsBehaveAsSpecified() {
        GameSession s = new GameSession();
        s.resetAfterCancel(80);
        assertTrue(s.showZeroXpAfterCancelledRun);
        assertFalse(s.sessionCommitted);
        assertEquals(0, s.displayedSavedXp());
        s.markCommitted();
        assertTrue(s.sessionCommitted);
        assertFalse(s.showZeroXpAfterCancelledRun);
        assertFalse(s.canCommit());
    }
}
