package codeescape.desktop.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Function;

public class BackgroundPanel extends JPanel {
    private BufferedImage bg;
    private final Function<String, BufferedImage> loader;

    public BackgroundPanel(String name, Function<String, BufferedImage> loader) {
        this.loader = loader;
        this.bg = loader == null ? null : loader.apply(name);
        setOpaque(true);
        setBackground(new Color(19, 43, 72));
    }

    public void setBackgroundImage(String name) {
        this.bg = loader == null ? null : loader.apply(name);
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bg == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(19, 43, 72));
        g2.fillRect(0, 0, getWidth(), getHeight());
        double s = Math.max(getWidth() / (double) bg.getWidth(), getHeight() / (double) bg.getHeight());
        int w = Math.max(1, (int)Math.round(bg.getWidth() * s));
        int h = Math.max(1, (int)Math.round(bg.getHeight() * s));
        int x = (getWidth() - w) / 2;
        int y = 0;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(bg, x, y, w, h, null);
        g2.dispose();
    }
}
