package codeescape.core.port;

public interface NarrationPort {
    boolean isSupported();
    void speakStory(String text);
    void stop();

    /**
     * Optional line-progress (karaoke) narration contract. Default falls back
     * to plain speakStory and completes immediately, so existing adapters
     * remain compatible without implementing the callback variant.
     */
    default void speakStory(String text, NarrationCallback callback) {
        speakStory(text);
        if (callback != null) callback.onComplete();
    }
}
