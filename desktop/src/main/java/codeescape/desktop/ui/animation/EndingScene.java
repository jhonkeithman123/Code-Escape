package codeescape.desktop.ui.animation;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Function;

import codeescape.desktop.scheduler.SwingScheduler;

public class EndingScene extends JPanel {
    private static final int KEY_WAITING = 0, KEY_FLYING = 1, OPENING = 2, OPENED = 3;
    private final BufferedImage runSheet;
    private final BufferedImage[] endingRunFrames;
    private final BufferedImage standing;
    private final BufferedImage victoryPose;
    private final BufferedImage keyImage;
    private final JLabel instructions;
    private final javax.swing.Timer timer;
    private final SwingScheduler scheduler;
    private final Function<String, BufferedImage> load;
    private final Runnable onTreasureOpen;
    private final String victoryPoseMissingLabel;
    private final Color gold;

    private int stage = KEY_WAITING;
    private long stageStarted;
    private double runnerX = -120;
    private double keyProgress = 0;
    private int keyFlightStartX = 0;
    private int keyFlightStartY = 0;
    private double lidAngle = 0;
    private boolean completionShown = false;

    public EndingScene(Function<String, BufferedImage> load,
                       Function<String, BufferedImage[]> framesLoader,
                       String runFile, String standingFile, String victoryFile,
                       JLabel instructions, Runnable onTreasureOpen,
                       String victoryPoseMissingLabel, Color gold) {
        this(load, framesLoader, runFile, standingFile, victoryFile, instructions, onTreasureOpen, victoryPoseMissingLabel, gold, null);
    }

    public EndingScene(Function<String, BufferedImage> load,
                       Function<String, BufferedImage[]> framesLoader,
                       String runFile, String standingFile, String victoryFile,
                       JLabel instructions, Runnable onTreasureOpen,
                       String victoryPoseMissingLabel, Color gold,
                       SwingScheduler scheduler) {
        this.load = load;
        this.scheduler = scheduler;
        this.onTreasureOpen = onTreasureOpen;
        this.victoryPoseMissingLabel = victoryPoseMissingLabel == null ? "VICTORY POSE NOT FOUND" : victoryPoseMissingLabel;
        this.gold = gold == null ? new Color(255, 209, 102) : gold;
        this.runSheet = load.apply(runFile);
        this.endingRunFrames = framesLoader.apply(runFile);
        this.standing = load.apply(standingFile);
        this.victoryPose = load.apply(victoryFile);
        this.keyImage = load.apply("golden_key.png");
        this.instructions = instructions;
        setOpaque(false);
        setPreferredSize(new Dimension(850, 520));
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (stage == KEY_WAITING && keyBounds().contains(e.getPoint())) {
                    Rectangle key = keyBounds();
                    keyFlightStartX = key.x;
                    keyFlightStartY = key.y;
                    keyProgress = 0;
                    stage = KEY_FLYING;
                    stageStarted = System.currentTimeMillis();
                    instructions.setText("THE KEY IS FLYING TO THE ANCIENT VAULT!");
                    repaint();
                }
            }
        });
        timer = scheduler != null ? scheduler.repeating(30, this::tick) : new javax.swing.Timer(30, e -> tick());
        if (timer != null) timer.stop();
    }

    public void start() {
        stageStarted = System.currentTimeMillis();
        instructions.setText("YOUR VICTORY POSE IS READY! CLICK THE KEY TO UNLOCK THE VAULT.");
        if (timer != null && !timer.isRunning()) timer.start();
    }

    private void tick() {
        long elapsed = System.currentTimeMillis() - stageStarted;
        if (stage == KEY_FLYING) {
            keyProgress = Math.min(1.0, elapsed / 720.0);
            if (keyProgress >= 1.0) {
                stage = OPENING; stageStarted = System.currentTimeMillis(); lidAngle = 0;
                instructions.setText("THE ANCIENT VAULT IS OPENING...");
            }
        } else if (stage == OPENING) {
            double openT = Math.min(1.0, elapsed / 760.0);
            lidAngle = openT * openT * (3.0 - 2.0 * openT);
            if (lidAngle >= 1.0) {
                stage = OPENED; if (scheduler != null) scheduler.stop(timer); else if (timer != null) timer.stop();
                instructions.setText("THE VAULT IS OPEN! TREASURE UNLOCKED.");
                repaint();
                if (!completionShown) {
                    completionShown = true;
                    Runnable open = () -> { if (onTreasureOpen != null) onTreasureOpen.run(); };
                    if (scheduler != null) scheduler.once(550, open);
                    else {
                        javax.swing.Timer popup = new javax.swing.Timer(550, ev -> open.run());
                        popup.setRepeats(false);
                        popup.start();
                    }
                }
                return;
            }
        }
        repaint();
    }

    private Rectangle characterBounds(int w, int h) {
        BufferedImage finalCharacter = victoryPose != null ? victoryPose : standing;
        int maxVictoryW = Math.max(90, (int) (w * 0.22));
        int maxVictoryH = Math.max(140, (int) (h * 0.46));
        int victoryW, victoryH;
        if (finalCharacter != null) {
            double victoryScale = Math.min(maxVictoryW / (double) finalCharacter.getWidth(),
                                           maxVictoryH / (double) finalCharacter.getHeight());
            victoryW = Math.max(1, (int) (finalCharacter.getWidth() * victoryScale));
            victoryH = Math.max(1, (int) (finalCharacter.getHeight() * victoryScale));
        } else {
            victoryW = maxVictoryW;
            victoryH = maxVictoryH;
        }
        int victoryX = Math.max((int) (w * 0.52), w - victoryW - (int) (w * 0.06));
        int victoryY = h - victoryH - (int) (h * 0.03);
        return new Rectangle(victoryX, victoryY, victoryW, victoryH);
    }

    private Rectangle keyBounds() {
        int w = getWidth(), h = getHeight();
        Rectangle c = characterBounds(w, h);
        int kw = Math.max(42, (int) (w * 0.075)), kh = Math.max(26, (int) (h * 0.075));
        int popLift = stage == KEY_WAITING
                ? (int) (Math.sin(Math.min(1.0, (System.currentTimeMillis() - stageStarted) / 420.0) * Math.PI / 2.0) * Math.min(18, h * 0.04)) : 0;
        // Place key on the character (near upper body/hand area) so it does not cover the chest.
        int x = Math.max(8, Math.min(w - kw - 8, c.x + (int) (c.width * 0.18)));
        int y = Math.max(8, Math.min(h - kh - 8, c.y + (int) (c.height * 0.28) - popLift));
        return new Rectangle(x, y, kw, kh);
    }

    private void drawImageFit(Graphics2D g, BufferedImage im, int x, int y, int maxW, int maxH) {
        if (im == null) return;
        double scale = Math.min(maxW / (double) im.getWidth(), maxH / (double) im.getHeight());
        int dw = Math.max(1, (int) (im.getWidth() * scale)), dh = Math.max(1, (int) (im.getHeight() * scale));
        g.drawImage(im, x, y, dw, dh, null);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int w = getWidth(), h = getHeight();
        BufferedImage closedChest = load.apply("treasure_chest_real.png");
        BufferedImage chestBody = load.apply("treasure_chest_body.png");
        BufferedImage chestLid = load.apply("treasure_chest_lid.png");
        int topSafe = Math.max(84, (int) (h * 0.16)); // keep space for top labels/instructions
        int vw = Math.min(Math.max(140, (int) (w * .24)), Math.max(140, w - 48));
        int vh = closedChest == null ? Math.max(120, (int) (h * .28))
                : Math.max(120, (int) (vw * closedChest.getHeight() / (double) closedChest.getWidth()));
        int vx = (w - vw) / 2;
        int vy = topSafe;
        int lockX = vx + vw / 2;
        int lockY = vy + (int) (vh * .49);
        if (stage == OPENING || stage == OPENED) {
            if (chestBody != null) {
                g.drawImage(chestBody, vx, vy + (int) (vh * .12), vw, (int) (vh * .88), null);
            } else if (closedChest != null) {
                g.drawImage(closedChest, vx, vy + (int) (vh * .25), vw, (int) (vh * .75), 0,
                        (int) (closedChest.getHeight() * .25), closedChest.getWidth(), closedChest.getHeight(), null);
            }
            int openingY = vy + (int) (vh * .17);
            int openingH = Math.max(16, (int) (vh * .22));
            g.setColor(new Color(45, 21, 13, 235));
            g.fillRoundRect(vx + (int) (vw * .13), openingY, (int) (vw * .74), openingH, 12, 12);
            if (chestLid != null) {
                int lidH = Math.max(35, (int) (vh * .42));
                int lift = (int) (lidAngle * lidH * 0.82);
                Graphics2D lidGraphics = (Graphics2D) g.create();
                lidGraphics.rotate(-Math.toRadians(9.0 * lidAngle), vx + vw / 2.0, vy + lidH * 0.78);
                lidGraphics.drawImage(chestLid, vx, vy - lift, vw, lidH, null);
                lidGraphics.dispose();
            }
            g.setColor(new Color(255, 211, 65, 35 + (int) (90 * lidAngle)));
            g.fillOval(vx - 12, vy - 8, vw + 24, vh + 36);
            g.setColor(new Color(255, 231, 125));
            for (int i = 0; i < 14; i++) {
                double a = i * Math.PI / 7.0;
                int sx = lockX + (int) (Math.cos(a) * (25 + lidAngle * 55));
                int sy = vy + vh / 3 + (int) (Math.sin(a) * (18 + lidAngle * 36));
                g.fillOval(sx, sy, 5, 5);
            }
        } else if (closedChest != null) {
            g.drawImage(closedChest, vx, vy, vw, vh, null);
        } else {
            g.setColor(new Color(110, 57, 24));
            g.fillRoundRect(vx, vy, vw, vh, 18, 18);
            g.setColor(new Color(238, 178, 51));
            g.setStroke(new BasicStroke(Math.max(5, vw / 45f)));
            g.drawRoundRect(vx, vy, vw, vh, 18, 18);
            g.fillRoundRect(lockX - 15, lockY - 12, 30, 36, 7, 7);
            g.setColor(new Color(50, 30, 15));
            g.fillOval(lockX - 4, lockY - 2, 8, 8);
        }

        BufferedImage finalCharacter = victoryPose;
        if (finalCharacter != null) {
            Rectangle c = characterBounds(w, h);
            // Smaller character placed bottom-right so it does not cover the centered chest.
            g.drawImage(finalCharacter, c.x, c.y, c.width, c.height, null);
            g.setColor(new Color(255, 226, 125, 225));
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6.0;
                int sx = c.x + c.width / 2 + (int) (Math.cos(a) * c.width * 0.52);
                int sy = c.y + c.height / 3 + (int) (Math.sin(a) * c.height * 0.20);
                g.fillOval(sx, sy, 4, 4);
            }
        } else {
            g.setColor(new Color(255, 225, 150));
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            String missing = victoryPoseMissingLabel;
            FontMetrics fm = g.getFontMetrics();
            g.drawString(missing, Math.max(12, (w - fm.stringWidth(missing)) / 2), h / 2);
        }

        if (stage == KEY_WAITING || stage == KEY_FLYING) {
            Rectangle b = keyBounds();
            int kx = b.x, ky = b.y;
            if (stage == KEY_FLYING) {
                double t = keyProgress;
                double eased = t * t * (3.0 - 2.0 * t);
                int tx = lockX - 18, ty = lockY - 12;
                kx = (int) (keyFlightStartX + (tx - keyFlightStartX) * eased);
                ky = (int) (keyFlightStartY + (ty - keyFlightStartY) * eased);
            }
            int kw = Math.max(42, (int) (w * 0.075));
            int kh = Math.max(26, (int) (h * 0.075));
            if (keyImage != null) drawImageFit(g, keyImage, kx, ky, kw, kh);
            else { g.setColor(gold); g.setFont(new Font("SansSerif", Font.BOLD, 32)); g.drawString("KEY", kx, ky + 28); }
            if (stage == KEY_WAITING) { g.setColor(new Color(255, 220, 90)); g.setStroke(new BasicStroke(2)); g.drawRoundRect(b.x - 4, b.y - 4, b.width + 8, b.height + 8, 12, 12); }
        }
        g.dispose();
    }
}
