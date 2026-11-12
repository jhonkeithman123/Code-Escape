package codeescape.desktop.adapter;

import java.util.List;

public interface NarrationCallback {
    void onLine(int index, String text, List<int[]> ranges);
    void onComplete();
}
