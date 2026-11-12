package codeescape.core.model;

import codeescape.core.content.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelBasicsTest {

    @Test
    void questionStoresFields() {
        String[] answers = {"a", "b", "c", "d"};
        Question q = new Question("What?", answers, 2, false, null);
        assertEquals("What?", q.text);
        assertArrayEquals(answers, q.answers);
        assertEquals(2, q.correct);
        assertFalse(q.syntax);
        assertNull(q.expected);

        Question s = new Question("Type", null, -1, true, "int x;");
        assertTrue(s.syntax);
        assertEquals("int x;", s.expected);
    }

    @Test
    void playerProfileDefaultsAndCtor() {
        PlayerProfile p = new PlayerProfile();
        assertEquals("Boy", p.character);
        assertEquals(0, p.outfitVariant);
        assertEquals(0, p.savedXp);
        PlayerProfile named = new PlayerProfile("u", "e@x");
        assertEquals("u", named.username);
        assertEquals("e@x", named.email);
        assertEquals("Boy", named.character);
    }

    @Test
    void gameSessionResetAndCancelDisplayBehavior() {
        GameSession s = new GameSession();
        s.xp = 50;
        s.hearts = 1;
        s.level = 3;
        s.questionIndex = 7;
        s.correctAnswers = 4;
        s.sessionCommitted = true;
        s.showZeroXpAfterCancelledRun = true;

        s.resetForNewLesson(120);
        assertEquals(0, s.xp);
        assertEquals(120, s.savedXpBeforeRun);
        assertFalse(s.showZeroXpAfterCancelledRun);
        assertFalse(s.sessionCommitted);
        assertEquals(GameConfig.STARTING_HEARTS, s.hearts);
        assertEquals(1, s.level);
        assertEquals(0, s.questionIndex);
        assertEquals(0, s.correctAnswers);
        assertEquals(120, s.displayedSavedXp());

        s.resetAfterCancel(90);
        assertEquals(0, s.xp);
        assertEquals(90, s.savedXpBeforeRun);
        assertTrue(s.showZeroXpAfterCancelledRun);
        assertFalse(s.sessionCommitted);
        assertEquals(GameConfig.STARTING_HEARTS, s.hearts);
        assertEquals(1, s.level);
        assertEquals(0, s.questionIndex);
        assertEquals(0, s.correctAnswers);
        assertEquals(0, s.displayedSavedXp());

        s.resetAfterCancel(-5);
        assertEquals(0, s.savedXpBeforeRun);
    }
}
