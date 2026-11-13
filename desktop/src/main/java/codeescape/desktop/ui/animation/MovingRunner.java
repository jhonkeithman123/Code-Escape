package codeescape.desktop.ui.animation;

import codeescape.core.animation.AnimationTiming;
import codeescape.desktop.scheduler.SwingScheduler;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Function;

public class MovingRunner extends JPanel {
    private final Function<String, BufferedImage> loader;
    private final SwingScheduler scheduler;
    BufferedImage sheet;
    BufferedImage[] isolatedFrames;
    double x;
    int frame = 0;
    long startNanos;
    long lastNanos;
    javax.swing.Timer timer;
    public Runnable onFinished;
    public JProgressBar progressBar;
    private int frameCount = 1;
    private int[] runFrames = {0};
    private boolean spriteSheet;
    private boolean staticCharacter;
    private static final long RUN_DURATION_NANOS = AnimationTiming.RUN_DURATION_NANOS;

    public MovingRunner(Function<String, BufferedImage> loader, String file) {
        this(loader, file, null);
    }

    public MovingRunner(Function<String, BufferedImage> loader, String file, SwingScheduler scheduler) {
        this.loader = loader;
        this.scheduler = scheduler;
        setOpaque(false);
        setDoubleBuffered(true);
        sheet = loader.apply(file);
        isolatedFrames = SpriteFrames.loadIsolatedWalkFrames(loader, file);
        spriteSheet = isolatedFrames != null && isolatedFrames.length > 1;
        staticCharacter = !spriteSheet;
        frameCount = spriteSheet ? isolatedFrames.length : 1;
        runFrames = spriteSheet ? SpriteFrames.frameSequenceFor(frameCount) : new int[]{0};
        x = -120;
        startNanos = System.nanoTime();
        lastNanos = startNanos;
        Runnable tick = () -> {
            long now = System.nanoTime();
            long elapsed = now - startNanos;
            lastNanos = now;

            int target = Math.max(10, getWidth() - getDisplayWidth() - 20);
            double startX = -getDisplayWidth() - 4.0;
            double progress = Math.min(1.0, elapsed / (double) RUN_DURATION_NANOS);
            double eased = AnimationTiming.smoothStep(progress);
            x = startX + (target - startX) * eased;
            if (progressBar != null) progressBar.setValue((int) Math.round(eased * 100));

            if (!staticCharacter) {
                frame = AnimationTiming.frameIndex(elapsed, runFrames.length);
            } else {
                frame = 0;
            }

            if (progress >= 1.0) {
                x = target;
                stopTimer();
                repaint();
                if (onFinished != null) SwingUtilities.invokeLater(onFinished);
                return;
            }
            repaint();
        };
        timer = scheduler != null ? scheduler.repeating(15, tick) : new javax.swing.Timer(15, e -> tick.run());
        if (timer != null) timer.stop();
        addHierarchyListener(e -> {
            if (isShowing() && timer != null && !timer.isRunning()) {
                startNanos = System.nanoTime();
                lastNanos = startNanos;
                timer.start();
            } else if (!isShowing() && timer != null && timer.isRunning()) {
                stopTimer();
            }
        });
    }

    private int getFrameWidth() {
        if (spriteSheet && isolatedFrames != null) return isolatedFrames[0].getWidth();
        return sheet == null ? 1 : sheet.getWidth();
    }

    private int getFrameHeight() {
        if (spriteSheet && isolatedFrames != null) return isolatedFrames[0].getHeight();
        return sheet == null ? 1 : sheet.getHeight();
    }

    private int getDisplayHeight() {
        int available = getHeight() > 0 ? getHeight() : 480;
        return Math.min(190, Math.max(160, available / 3));
    }

    private int getDisplayWidth() {
        double sourceRatio = getFrameWidth() / (double) Math.max(1, getFrameHeight());
        return Math.max(68, (int) Math.round(getDisplayHeight() * sourceRatio));
    }

    private void stopTimer() {
        if (timer == null) return;
        if (scheduler != null) scheduler.stop(timer);
        else timer.stop();
    }

    public void stop() {
        stopTimer();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (sheet == null) return;
        int fw = getFrameWidth();
        int fh = getFrameHeight();
        int dh = getDisplayHeight();
        int dw = getDisplayWidth();
        int y = Math.max(0, getHeight() - dh - 8);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int shadowW = Math.max(32, (int) (dw * 0.66));
        int shadowH = 7;
        g2.setColor(new Color(3, 13, 28, 90));
        g2.fillOval((int) Math.round(x) + (dw - shadowW) / 2, getHeight() - 15, shadowW, shadowH);
        int sx = runFrames[frame % runFrames.length];
        if (spriteSheet && isolatedFrames != null) {
            BufferedImage pose = isolatedFrames[sx];
            g2.drawImage(pose, (int) Math.round(x), y,
                    (int) Math.round(x) + dw, y + dh, 0, 0, pose.getWidth(), pose.getHeight(), null);
        } else {
            g2.drawImage(sheet, (int) Math.round(x), y,
                    (int) Math.round(x) + dw, y + dh, 0, 0, fw, fh, null);
        }
        g2.dispose();
    }
}
