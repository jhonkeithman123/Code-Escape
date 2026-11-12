package codeescape.core.model;

import codeescape.core.content.GameConfig;

public class GameSession {
    public String username = "";
    public int xp = 0;
    public int savedXpBeforeRun = 0;
    public boolean showZeroXpAfterCancelledRun = false;
    public int hearts = GameConfig.STARTING_HEARTS;
    public int level = 1;
    public int questionIndex = 0;
    public int correctAnswers = 0;
    public boolean sessionCommitted = false;

    public void resetForNewLesson(int savedXp) {
        this.xp = 0;
        this.savedXpBeforeRun = Math.max(0, savedXp);
        this.showZeroXpAfterCancelledRun = false;
        this.sessionCommitted = false;
        this.hearts = GameConfig.STARTING_HEARTS;
        this.level = 1;
        this.questionIndex = 0;
        this.correctAnswers = 0;
    }

    public void resetAfterCancel(int savedXp) {
        this.xp = 0;
        this.savedXpBeforeRun = Math.max(0, savedXp);
        this.showZeroXpAfterCancelledRun = true;
        this.sessionCommitted = false;
        this.hearts = GameConfig.STARTING_HEARTS;
        this.level = 1;
        this.questionIndex = 0;
        this.correctAnswers = 0;
    }

    public void markCommitted() {
        this.sessionCommitted = true;
        this.showZeroXpAfterCancelledRun = false;
    }

    public boolean canCommit() {
        return !sessionCommitted;
    }

    public int displayedSavedXp() {
        return showZeroXpAfterCancelledRun ? 0 : savedXpBeforeRun;
    }
}
