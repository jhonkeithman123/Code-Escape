package codeescape.desktop.ui.animation;

import codeescape.desktop.scheduler.SwingScheduler;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Function;

public class MiniRunner extends JPanel {
    private final Function<String, BufferedImage> loader;
    private final SwingScheduler scheduler;
    BufferedImage sheet;
    int frame = 0;
    int frameCount = 4;
    int runnerX = -1;
    boolean jumping = false;
    int jumpTicks = 0;
    javax.swing.Timer timer;

    public MiniRunner(Function<String, BufferedImage> loader, String file) {
        this(loader, file, null);
    }

    public MiniRunner(Function<String, BufferedImage> loader, String file, SwingScheduler scheduler) {
        this.loader = loader;
        this.scheduler = scheduler;
        setOpaque(false);
        setCharacter(file);
        Runnable tick = () -> {
            frame = (frame + 1) % Math.max(1, frameCount);
            if (runnerX >= 0) {
                runnerX += 5;
                if (runnerX > getWidth()) runnerX = -Math.max(1, getWidth() / 8);
            }
            if (jumpTicks > 0) { jumpTicks--; jumping = true; }
            else jumping = false;
            repaint();
        };
        timer = scheduler != null ? scheduler.repeating(85, tick) : new javax.swing.Timer(85, e -> tick.run());
        if (timer != null) timer.stop();
        addHierarchyListener(e -> {
            if (isShowing() && timer != null && !timer.isRunning()) timer.start();
            else if (!isShowing() && timer != null && timer.isRunning()) {
                if (scheduler != null) scheduler.stop(timer); else timer.stop();
            }
        });
    }

    public void setCharacter(String file) {
        sheet = loader.apply(file);
        frame = 0;
        frameCount = SpriteFrames.detectFrameCount(sheet);
        repaint();
    }

    public void jump() {
        jumpTicks = 12;
        if (timer != null && !timer.isRunning()) timer.start();
    }

    public void stop() {
        if (timer == null) return;
        if (scheduler != null) scheduler.stop(timer);
        else timer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (sheet == null) return;
        int fw = Math.max(1, sheet.getWidth() / Math.max(1, frameCount));
        int fh = sheet.getHeight();
        int dh = Math.min(86, Math.max(64, getHeight() - 6));
        int dw = (int) Math.round(fw * (dh / (double) fh));
        if (runnerX < 0) runnerX = 0;
        int x = runnerX;
        int y = getHeight() - dh - (jumping ? (int) (28 * Math.sin((12 - jumpTicks) * Math.PI / 12.0)) : 0);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int frameIndex = Math.floorMod(frame, Math.max(1, frameCount));
        int sx1 = frameIndex * fw;
        int sx2 = Math.min(sheet.getWidth(), sx1 + fw);
        g2.drawImage(sheet, x, y, x + dw, y + dh, sx1, 0, sx2, fh, null);
        g2.dispose();
    }
}
