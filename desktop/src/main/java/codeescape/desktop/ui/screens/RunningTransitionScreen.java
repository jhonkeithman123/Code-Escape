package codeescape.desktop.ui.screens;

import codeescape.desktop.ui.components.BackgroundPanel;
import codeescape.desktop.ui.components.GlassPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Function;

public final class RunningTransitionScreen {
    private RunningTransitionScreen() {}

    public static class Result {
        public final JPanel panel;
        public final JProgressBar progressBar;
        public Result(JPanel panel, JProgressBar progressBar) { this.panel = panel; this.progressBar = progressBar; }
    }

    public static Result create(int level, String backgroundName,
                                Function<String, BufferedImage> loader,
                                JComponent runnerComponent,
                                Color cyan, Color cream, Color gold) {
        BackgroundPanel p = new BackgroundPanel(backgroundName, loader);
        p.setLayout(new BorderLayout(0, 12));
        p.setBorder(new EmptyBorder(16, 20, 18, 20));

        GlassPanel header = new GlassPanel(new Color(10, 25, 48, 210));
        header.setLayout(new BorderLayout(14, 0));
        header.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel title = new JLabel("CODE ESCAPE  /  LEVEL " + level);
        title.setForeground(cyan);
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        JLabel subtitle = new JLabel("NEXT AREA UNLOCKING");
        subtitle.setForeground(new Color(210, 225, 245));
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.add(title);
        titleStack.add(Box.createVerticalStrut(4));
        titleStack.add(subtitle);
        header.add(titleStack, BorderLayout.WEST);
        JLabel levelBadge = new JLabel(String.format("%02d  /  05", level));
        levelBadge.setForeground(gold);
        levelBadge.setFont(new Font("SansSerif", Font.BOLD, 18));
        levelBadge.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(levelBadge, BorderLayout.EAST);
        p.add(header, BorderLayout.NORTH);

        if (runnerComponent != null) p.add(runnerComponent, BorderLayout.CENTER);

        GlassPanel footer = new GlassPanel(new Color(10, 25, 48, 218));
        footer.setLayout(new BorderLayout(14, 8));
        footer.setBorder(new EmptyBorder(13, 20, 13, 20));
        JLabel destination = new JLabel("RUNNING TO THE NEXT LEVEL");
        destination.setForeground(cream);
        destination.setFont(new Font("SansSerif", Font.BOLD, 15));
        footer.add(destination, BorderLayout.WEST);
        JLabel hint = new JLabel("KEEP GOING  —  YOU'RE DOING GREAT");
        hint.setForeground(cyan);
        hint.setFont(new Font("SansSerif", Font.BOLD, 11));
        hint.setHorizontalAlignment(SwingConstants.RIGHT);
        footer.add(hint, BorderLayout.EAST);

        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(0);
        progress.setBorderPainted(false);
        progress.setStringPainted(false);
        progress.setOpaque(false);
        progress.setForeground(cyan);
        progress.setBackground(new Color(255, 255, 255, 45));
        progress.setPreferredSize(new Dimension(200, 8));
        progress.setUI(new BasicProgressBarUI() {
            @Override protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = c.getWidth(), h = c.getHeight();
                g2.setColor(new Color(255, 255, 255, 48));
                g2.fillRoundRect(0, 0, w, h, h, h);
                int fill = (int) Math.round(w * progress.getPercentComplete());
                g2.setPaint(new GradientPaint(0, 0, new Color(90, 245, 230), w, h, new Color(63, 151, 255)));
                if (fill > 0) g2.fillRoundRect(0, 0, fill, h, h, h);
                g2.dispose();
            }
        });
        footer.add(progress, BorderLayout.SOUTH);
        p.add(footer, BorderLayout.SOUTH);

        return new Result(p, progress);
    }
}
