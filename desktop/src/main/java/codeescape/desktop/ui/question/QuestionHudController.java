package codeescape.desktop.ui.question;

import codeescape.desktop.ui.screens.QuestionScreen;

import javax.swing.*;
import java.awt.*;

/**
 * HUD/timer helper for Question screen. Encapsulates timer lifecycle and HUD updates
 * while keeping strings/colors owned by caller to preserve exact parity.
 */
public final class QuestionHudController {
    private final QuestionScreen.HudRefs hudRefs;
    private final Runnable onTimeUp;
    private Timer timer;
    private int secondsLeft;
    private boolean acceptingAnswer = true;

    public QuestionHudController(QuestionScreen.HudRefs hudRefs, Runnable onTimeUp) {
        this.hudRefs = hudRefs;
        this.onTimeUp = onTimeUp;
    }

    public int getSecondsLeft() { return secondsLeft; }
    public void setSecondsLeft(int secondsLeft) { this.secondsLeft = secondsLeft; }
    public void setAcceptingAnswer(boolean acceptingAnswer) { this.acceptingAnswer = acceptingAnswer; }
    public boolean isAcceptingAnswer() { return acceptingAnswer; }

    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    /** Starts/restarts timer using caller-provided label formatting. */
    public void restart(int seconds, int tickMs, Runnable updateLabels, Runnable onExpired) {
        stop();
        this.secondsLeft = Math.max(0, seconds);
        if (updateLabels != null) updateLabels.run();
        timer = new Timer(tickMs, e -> {
            if (!acceptingAnswer) return;
            secondsLeft--;
            if (updateLabels != null) updateLabels.run();
            if (secondsLeft <= 0) {
                stop();
                if (onExpired != null) onExpired.run();
                else if (onTimeUp != null) onTimeUp.run();
            }
        });
        timer.start();
    }

    public void applyHud(String heartsText, String xpText, String timerText, Color timerColor) {
        QuestionScreen.updateHud(hudRefs, heartsText, xpText, timerText, timerColor);
    }
}
