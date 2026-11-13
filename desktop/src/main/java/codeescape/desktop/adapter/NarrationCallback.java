package codeescape.desktop.adapter;

import java.util.List;

/**
 * Desktop narration callback contract. Extends the core port callback so
 * adapters and UI code can share a single narration progress/complete type.
 */
public interface NarrationCallback extends codeescape.core.port.NarrationCallback {
    @Override
    void onLine(int index, String text, List<int[]> ranges);

    @Override
    void onComplete();
}
