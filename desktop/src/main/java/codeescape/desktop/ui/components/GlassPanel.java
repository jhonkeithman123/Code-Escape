package codeescape.desktop.ui.components;

import javax.swing.*;
import java.awt.*;

public class GlassPanel extends JPanel {
    private final Color fill;
    public GlassPanel(Color fill) { this.fill = fill; setOpaque(false); }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
        g2.setColor(new Color(255, 255, 255, 38));
        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 22, 22);
        g2.dispose();
        super.paintComponent(g);
    }
}
