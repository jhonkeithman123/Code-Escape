package codeescape.core.content;

/** Canonical audio track identifiers and level/screen mappings (platform-agnostic). */
public final class AudioCatalog {
    private AudioCatalog() {}

    public static final String TRACK_MAIN_MENU = "code_escape_theme.wav";
    public static final String TRACK_DUNGEON = "dungeon.wav";
    public static final String TRACK_VICTORY = "victory.wav";

    public static final String TRACK_LEVEL_1 = "adventure.wav";
    public static final String TRACK_LEVEL_2 = "blizzard.wav";
    public static final String TRACK_LEVEL_3 = "magma.wav";
    public static final String TRACK_LEVEL_4 = "sky.wav";
    public static final String TRACK_LEVEL_5 = "dark_castle.wav";

    public static String levelTrack(int levelNumber) {
        switch (levelNumber) {
            case 1: return TRACK_LEVEL_1;
            case 2: return TRACK_LEVEL_2;
            case 3: return TRACK_LEVEL_3;
            case 4: return TRACK_LEVEL_4;
            case 5: return TRACK_LEVEL_5;
            default: return TRACK_MAIN_MENU;
        }
    }
}
