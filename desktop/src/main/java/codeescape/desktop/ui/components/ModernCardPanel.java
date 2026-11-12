package codeescape.desktop.ui.components;

import javax.swing.*;
import java.awt.*;

public class ModernCardPanel extends JPanel {
    public ModernCardPanel() { setOpaque(false); }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color c = getBackground() == null ? new Color(20, 51, 86, 238) : getBackground();
        Color top = new Color(Math.min(255, c.getRed() + 10), Math.min(255, c.getGreen() + 14), Math.min(255, c.getBlue() + 18), c.getAlpha());
        g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), c));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
        g2.setColor(new Color(255, 255, 255, 18));
        g2.drawRoundRect(2, 2, Math.max(0, getWidth() - 5), Math.max(0, getHeight() - 5), 22, 22);
        g2.dispose();
        super.paintComponent(g);
    }
}
