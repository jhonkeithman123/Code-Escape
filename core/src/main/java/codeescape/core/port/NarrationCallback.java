package codeescape.core.port;

import java.util.List;

/**
 * Callback for line-by-line (karaoke) story narration. Java 8 compatible.
 * Adapters that only support whole-text narration may ignore onLine and
 * invoke onComplete when speech finishes.
 */
public interface NarrationCallback {
    void onLine(int index, String text, List<int[]> ranges);

    /**
     * Optional character-range progress for karaoke highlighting (best-effort;
     * engine support varies). Defaults to no-op for adapters that only report lines.
     */
    default void onRange(int start, int end) {}

    /** Optional completion signal; adapters may use a default no-op. */
    default void onComplete() {}
}
