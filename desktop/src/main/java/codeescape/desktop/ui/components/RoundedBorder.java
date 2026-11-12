package codeescape.desktop.ui.components;

import javax.swing.border.Border;
import java.awt.*;

public class RoundedBorder implements Border {
    private final Color color;
    private final int thickness;
    private final int arc;
    public RoundedBorder(Color color, int thickness, int arc) {
        this.color = color; this.thickness = thickness; this.arc = arc;
    }
    @Override public Insets getBorderInsets(Component c) { int pad = Math.max(5, thickness + 4); return new Insets(pad, pad + 2, pad, pad + 2); }
    @Override public boolean isBorderOpaque() { return false; }
    @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(thickness));
        int off = thickness;
        g2.drawRoundRect(x + off, y + off, Math.max(0, w - off * 2 - 1), Math.max(0, h - off * 2 - 1), arc, arc);
        g2.dispose();
    }
}
