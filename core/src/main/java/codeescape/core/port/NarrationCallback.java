package codeescape.core.port;

import java.util.List;

/**
 * Callback for line-by-line (karaoke) story narration. Java 8 compatible.
 * Adapters that only support whole-text narration may ignore onLine and
 * invoke onComplete when speech finishes.
 */
public interface NarrationCallback {
    void onLine(int index, String text, List<int[]> ranges);

    /** Optional completion signal; adapters may use a default no-op. */
    default void onComplete() {}
}
