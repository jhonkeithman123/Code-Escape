package codeescape.android.adapter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidProfileRepositorySnapshotTest {
    @Test
    fun snapshotRoundTrip_preservesContinuityFields() {
        val snap = AndroidProfileRepository.RunSnapshot(
            screen = "ENDING",
            level = 5,
            questionIndex = 3,
            hearts = 2,
            xp = 120,
            correctAnswers = 18,
            savedXpBeforeRun = 40,
            showZeroXpAfterCancelledRun = false,
            sessionCommitted = true,
            pendingStoryLevel = 5,
            questionTimeLeft = 12,
            syntaxInput = "print('x')",
            answered = false,
            runningReason = "GAME_COMPLETE",
            endingChestOpened = true,
            endingTreasureDialogShown = false,
            endingXpDialogShown = true,
            endingFinalXp = 120,
            resultMessage = "",
            feedbackHeading = "CORRECT!",
            feedbackBig = "+10 XP",
            feedbackDetail = "Great Job!",
            lastAnswerCorrect = true,
            exitDialogShown = false,
            loadingTarget = "STORY"
        )
        val restored = AndroidProfileRepository.RunSnapshot.fromJson(snap.toJson())
        assertEquals("ENDING", restored.screen)
        assertEquals(5, restored.level)
        assertEquals(3, restored.questionIndex)
        assertEquals(2, restored.hearts)
        assertEquals(120, restored.xp)
        assertEquals(18, restored.correctAnswers)
        assertEquals(40, restored.savedXpBeforeRun)
        assertFalse(restored.showZeroXpAfterCancelledRun)
        assertTrue(restored.sessionCommitted)
        assertEquals(5, restored.pendingStoryLevel)
        assertEquals(12, restored.questionTimeLeft)
        assertEquals("print('x')", restored.syntaxInput)
        assertFalse(restored.answered)
        assertEquals("GAME_COMPLETE", restored.runningReason)
        assertTrue(restored.endingChestOpened)
        assertFalse(restored.endingTreasureDialogShown)
        assertTrue(restored.endingXpDialogShown)
        assertEquals(120, restored.endingFinalXp)
        assertEquals("CORRECT!", restored.feedbackHeading)
        assertEquals("+10 XP", restored.feedbackBig)
        assertEquals("Great Job!", restored.feedbackDetail)
        assertEquals(true, restored.lastAnswerCorrect)
        assertFalse(restored.exitDialogShown)
        assertEquals("STORY", restored.loadingTarget)
    }

    @Test
    fun snapshotFromLegacyJson_usesBackwardCompatibleDefaults() {
        val legacy = org.json.JSONObject()
            .put("screen", "QUESTION")
            .put("level", 2)
            .put("questionIndex", 1)
            .put("hearts", 3)
            .put("xp", 30)
            .put("correctAnswers", 4)
            .put("savedXpBeforeRun", 10)
            .put("showZeroXpAfterCancelledRun", false)
            .put("sessionCommitted", false)
            .put("pendingStoryLevel", 2)
            .put("questionTimeLeft", 20)
            .put("syntaxInput", "")
            .put("answered", false)
        val restored = AndroidProfileRepository.RunSnapshot.fromJson(legacy)
        assertEquals("QUESTION", restored.screen)
        assertEquals("", restored.runningReason)
        assertFalse(restored.endingChestOpened)
        assertFalse(restored.endingTreasureDialogShown)
        assertFalse(restored.endingXpDialogShown)
        assertEquals(0, restored.endingFinalXp)
        assertEquals("", restored.resultMessage)
        assertNull(restored.lastAnswerCorrect)
        assertFalse(restored.exitDialogShown)
        assertEquals("LOGIN", restored.loadingTarget)
    }
}
