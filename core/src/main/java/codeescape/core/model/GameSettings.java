package codeescape.core.model;

/**
 * Shared audio/narration settings used by both the desktop and Android frontends.
 * Persistence is owned by the platform-specific SettingsStore implementation.
 */
public class GameSettings {
    public static final float DEFAULT_VOLUME = 0.45f;

    private boolean musicEnabled = true;
    private boolean muted = false;
    private float volume = DEFAULT_VOLUME;
    private boolean ttsEnabled = true;

    public GameSettings() {}

    public GameSettings(boolean musicEnabled, boolean muted, float volume, boolean ttsEnabled) {
        this.musicEnabled = musicEnabled;
        this.muted = muted;
        setVolume(volume);
        this.ttsEnabled = ttsEnabled;
    }

    public boolean isMusicEnabled() { return musicEnabled; }
    public void setMusicEnabled(boolean musicEnabled) { this.musicEnabled = musicEnabled; }

    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }

    public float getVolume() { return volume; }
    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
    }

    public boolean isTtsEnabled() { return ttsEnabled; }
    public void setTtsEnabled(boolean ttsEnabled) { this.ttsEnabled = ttsEnabled; }
}
