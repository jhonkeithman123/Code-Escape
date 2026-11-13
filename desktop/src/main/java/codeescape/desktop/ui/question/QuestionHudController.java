package codeescape.desktop.ui.question;

import codeescape.desktop.scheduler.SwingScheduler;
import codeescape.desktop.ui.screens.QuestionScreen;

import javax.swing.*;
import java.awt.*;

/**
 * HUD/timer helper for Question screen. Encapsulates timer lifecycle and HUD updates
 * while keeping label composition/strings controlled by caller to preserve parity.
 */
public final class QuestionHudController {
    private final QuestionScreen.HudRefs hudRefs;
    private final Runnable onTimeUp;
    private final SwingScheduler scheduler;
    private Timer timer;
    private int secondsLeft;
    private boolean acceptingAnswer = true;

    public QuestionHudController(QuestionScreen.HudRefs hudRefs, Runnable onTimeUp) {
        this(hudRefs, onTimeUp, null);
    }

    public QuestionHudController(QuestionScreen.HudRefs hudRefs, Runnable onTimeUp, SwingScheduler scheduler) {
        this.hudRefs = hudRefs;
        this.onTimeUp = onTimeUp;
        this.scheduler = scheduler;
    }

    public int getSecondsLeft() { return secondsLeft; }
    public void setSecondsLeft(int secondsLeft) { this.secondsLeft = Math.max(0, secondsLeft); }
    public void setAcceptingAnswer(boolean acceptingAnswer) { this.acceptingAnswer = acceptingAnswer; }
    public boolean isAcceptingAnswer() { return acceptingAnswer; }

    public void stop() {
        if (timer != null) {
            if (scheduler != null) scheduler.stop(timer);
            else timer.stop();
            timer = null;
        }
    }

    /** Starts/restarts countdown; updateLabels is invoked after init and every tick with current seconds. */
    public void restart(int seconds, int tickMs, Runnable updateLabels, Runnable onExpired) {
        stop();
        this.secondsLeft = Math.max(0, seconds);
        if (updateLabels != null) updateLabels.run();
        Runnable tick = new Runnable() {
            @Override public void run() {
                if (!acceptingAnswer) return;
                secondsLeft--;
                if (updateLabels != null) updateLabels.run();
                if (secondsLeft <= 0) {
                    stop();
                    if (onExpired != null) onExpired.run();
                    else if (onTimeUp != null) onTimeUp.run();
                }
            }
        };
        if (scheduler != null) {
            timer = scheduler.repeating(Math.max(1, tickMs), tick);
        } else {
            timer = new Timer(Math.max(1, tickMs), e -> tick.run());
            timer.start();
        }
    }

    /** Applies HUD label texts/colors. */
    public void applyHud(String heartsText, String xpText, String timerText, Color timerColor) {
        QuestionScreen.updateHud(hudRefs, heartsText, xpText, timerText, timerColor);
    }

    /** Syncs timer countdown display and emits standard timer HUD update with caller-owned strings/colors. */
    public void syncTimerHud(String timerText, Color timerColor) {
        QuestionScreen.updateHud(hudRefs, null, null, timerText, timerColor);
    }

    /** Applies full HUD from current state; timer text/color are derived to centralize formatting. */
    public void renderHud(String heartsText, String xpText, Color normalTimerColor, Color urgentTimerColor) {
        applyHud(heartsText, xpText, formatTimerLabel(secondsLeft), timerColor(secondsLeft, normalTimerColor, urgentTimerColor));
    }

    /** Legacy-compatible timer label text (prefix preserved). */
    public static String formatTimerLabel(int secondsLeft) {
        return "⏱ " + secondsLeft + "s";
    }

    public static Color timerColor(int secondsLeft, Color warningOrNormal, Color urgent) {
        return secondsLeft <= 5 ? urgent : warningOrNormal;
    }
}
