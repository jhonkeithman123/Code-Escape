package codeescape.core.content;

import codeescape.core.model.Question;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentAndConfigTest {

    @Test
    void gameConfigHasExpectedConstants() {
        assertEquals(5, GameConfig.LEVEL_COUNT);
        assertEquals(10, GameConfig.QUESTIONS_PER_LEVEL);
        assertEquals(3, GameConfig.STARTING_HEARTS);
        assertEquals(10, GameConfig.XP_PER_CORRECT);
        assertEquals(15, GameConfig.MCQ_SECONDS);
        assertEquals(30, GameConfig.SYNTAX_SECONDS);
        assertEquals(1800, GameConfig.LOADING_DELAY_MS);
        assertEquals(1000, GameConfig.QUESTION_TICK_MS);
        assertTrue(GameConfig.QUESTION_TICK_MS > 0);
    }

    @Test
    void questionBankHasLevelsAndQuestions() {
        Question[][] all = QuestionBank.all();
        assertEquals(GameConfig.LEVEL_COUNT, all.length);
        for (int i = 0; i < all.length; i++) {
            assertNotNull(all[i], "level " + (i + 1));
            assertEquals(GameConfig.QUESTIONS_PER_LEVEL, all[i].length, "level " + (i + 1) + " count");
            for (Question q : all[i]) {
                assertNotNull(q);
                assertNotNull(q.text);
                assertFalse(q.text.trim().isEmpty());
                if (q.syntax) {
                    assertNotNull(q.expected);
                } else {
                    assertNotNull(q.answers);
                    assertEquals(4, q.answers.length);
                    assertTrue(q.correct >= 0 && q.correct < q.answers.length);
                }
            }
        }
        Question[][] all2 = QuestionBank.all();
        assertNotSame(all, all2);
        assertNotSame(all[0], all2[0]);
    }

    @Test
    void storyContentPagesAndLookup() {
        assertEquals("STORY_INTRO", StoryContent.INTRO.screenId);
        assertEquals(0, StoryContent.INTRO.level);
        assertEquals(StoryContent.INTRO, StoryContent.forLevel(0));
        assertEquals(StoryContent.INTRO, StoryContent.forLevel(1));
        assertEquals(StoryContent.LEVEL2, StoryContent.forLevel(2));
        assertEquals(StoryContent.LEVEL3, StoryContent.forLevel(3));
        assertEquals(StoryContent.LEVEL4, StoryContent.forLevel(4));
        assertEquals(StoryContent.LEVEL5, StoryContent.forLevel(5));
        assertEquals("level2.png", StoryContent.LEVEL2.backgroundImage);
        assertNotNull(StoryContent.LEVEL5.body);
    }

    @Test
    void levelMetadataAndAudioCatalogMappings() {
        for (int l = 1; l <= GameConfig.LEVEL_COUNT; l++) {
            LevelMetadata m = LevelMetadata.of(l);
            assertEquals(l, m.level);
            assertEquals("level" + l + ".png", m.backgroundImage);
            assertEquals(AudioCatalog.levelTrack(l), m.musicTrack);
            assertNotNull(m.musicTrack);
        }
        assertEquals(1, LevelMetadata.of(0).level);
        assertEquals(GameConfig.LEVEL_COUNT, LevelMetadata.of(99).level);
        assertEquals(AudioCatalog.TRACK_LEVEL_1, AudioCatalog.levelTrack(1));
        assertEquals(AudioCatalog.TRACK_MAIN_MENU, AudioCatalog.levelTrack(999));
        assertNotNull(AudioCatalog.TRACK_DUNGEON);
        assertNotNull(AudioCatalog.TRACK_VICTORY);
    }
}
