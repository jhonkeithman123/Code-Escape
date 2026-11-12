package codeescape.core.content;

public final class StoryContent {
    private StoryContent() {}

    public static final class StoryPage {
        public final String screenId;
        public final String title;
        public final String body;
        public final String buttonText;
        public final String backgroundImage;
        public final int level; // 0 for intro

        public StoryPage(String screenId, String title, String body, String buttonText, String backgroundImage, int level) {
            this.screenId = screenId;
            this.title = title;
            this.body = body;
            this.buttonText = buttonText;
            this.backgroundImage = backgroundImage;
            this.level = level;
        }
    }

    public static final StoryPage INTRO = new StoryPage(
            "STORY_INTRO",
            "CODE ESCAPE — SYSTEM FAILURE",
            "A quake has sealed the ancient Code Dungeon, trapping you beneath a ruined digital kingdom. "
                    + "Your chosen explorer enters the first chamber with only three hearts and a flickering map. "
                    + "Decode the HTML ruins to open the stone gate, cross the CSS shadow maze, and reactivate the JavaScript relics. "
                    + "Then survive the Debugger Bridge, where corrupted traps guard the path to the final vault. "
                    + "The last chamber combines every skill. Solve its challenges, claim the key, and escape before the dungeon resets.",
            "BEGIN JOURNEY",
            "castle.png",
            0
    );

    public static final StoryPage LEVEL2 = new StoryPage(
            "STORY_LEVEL2",
            "LEVEL 2 — THE STYLED MAZE",
            "The stone gate slams shut behind you as you enter the Shadow Maze. Its walls shift whenever the styling runes are wrong. "
                    + "Use CSS selectors, colors, spacing, and layout properties to light the rune torches and reveal the hidden passage. "
                    + "A wrong move wakes the dungeon traps, so study each clue before advancing.",
            "ENTER LEVEL 2",
            "level2.png",
            2
    );

    public static final StoryPage LEVEL3 = new StoryPage(
            "STORY_LEVEL3",
            "LEVEL 3 — THE LOGIC CHAMBER",
            "Beyond the maze lies the Relic Chamber, where ancient machines guard a bridge over a glowing abyss. "
                    + "The mechanisms are frozen until JavaScript variables, operators, conditions, and functions restore their logic. "
                    + "Activate the relics in time and the bridge will rise from the darkness.",
            "ENTER LEVEL 3",
            "level3.png",
            3
    );

    public static final StoryPage LEVEL4 = new StoryPage(
            "STORY_LEVEL4",
            "LEVEL 4 — THE CODE BRIDGE",
            "The bridge leads into the Bugkeeper's Gauntlet. Broken code has become living traps, and every warning is a clue. "
                    + "Find missing tags, misspelled names, and broken syntax to disarm the traps and reach the dungeon's inner door.",
            "ENTER LEVEL 4",
            "level4.png",
            4
    );

    public static final StoryPage LEVEL5 = new StoryPage(
            "STORY_LEVEL5",
            "LEVEL 5 — THE FINAL CODE",
            "At last, you reach the Dragon Vault beneath the dungeon. Its lock is powered by one master program, and every skill must work together. "
                    + "Build the structure with HTML, style it with CSS, activate it with JavaScript, and debug every hidden fault. "
                    + "Complete the challenge, claim the ancient key, and escape with the dungeon treasure.",
            "FACE THE FINAL CODE",
            "level5.png",
            5
    );

    public static StoryPage forLevel(int level) {
        switch (level) {
            case 2: return LEVEL2;
            case 3: return LEVEL3;
            case 4: return LEVEL4;
            case 5: return LEVEL5;
            default: return INTRO;
        }
    }
}
