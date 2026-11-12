package codeescape.core.content;

public final class GameConfig {
    private GameConfig() {}

    public static final int LEVEL_COUNT = 5;
    public static final int QUESTIONS_PER_LEVEL = 10;
    public static final int STARTING_HEARTS = 3;
    public static final int XP_PER_CORRECT = 10;
    public static final int MCQ_SECONDS = 15;
    public static final int SYNTAX_SECONDS = 30;

    // UI/flow timing constants (milliseconds)
    public static final int LOADING_DELAY_MS = 1800;
    public static final int QUESTION_TICK_MS = 1000;
}
