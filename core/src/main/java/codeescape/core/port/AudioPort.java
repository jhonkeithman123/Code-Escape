package codeescape.core.port;

public interface AudioPort {
    void setEnabled(boolean enabled);
    boolean isEnabled();
    void setMuted(boolean muted);
    boolean isMuted();
    void setVolume(float volume01);
    float getVolume();
    void playMusic(String trackId, boolean loop);
    void stopMusic();
    void playFeedback(boolean correct);
}
