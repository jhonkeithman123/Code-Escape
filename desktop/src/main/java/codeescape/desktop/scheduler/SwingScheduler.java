package codeescape.desktop.scheduler;

import javax.swing.Timer;

/**
 * Minimal desktop scheduler abstraction/adapter around Swing timers.
 * Keeps scheduling concerns explicit without changing existing Swing behavior.
 */
public class SwingScheduler {
    public Timer once(int delayMs, Runnable action) {
        Timer t = new Timer(Math.max(0, delayMs), e -> {
            if (action != null) action.run();
        });
        t.setRepeats(false);
        t.start();
        return t;
    }

    public Timer repeating(int delayMs, Runnable tick) {
        Timer t = new Timer(Math.max(1, delayMs), e -> {
            if (tick != null) tick.run();
        });
        t.setRepeats(true);
        t.start();
        return t;
    }

    public void stop(Timer timer) {
        if (timer != null) timer.stop();
    }
}