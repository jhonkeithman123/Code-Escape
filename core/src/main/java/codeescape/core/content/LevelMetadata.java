package codeescape.core.content;

public final class LevelMetadata {
    public final int level;
    public final String backgroundImage;
    public final String musicTrack;

    private LevelMetadata(int level, String backgroundImage, String musicTrack) {
        this.level = level;
        this.backgroundImage = backgroundImage;
        this.musicTrack = musicTrack;
    }

    public static LevelMetadata of(int level) {
        int l = Math.max(1, Math.min(GameConfig.LEVEL_COUNT, level));
        return new LevelMetadata(l, "level" + l + ".png", AudioCatalog.levelTrack(l));
    }
}
