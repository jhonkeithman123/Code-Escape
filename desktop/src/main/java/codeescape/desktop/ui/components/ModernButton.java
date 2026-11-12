package codeescape.desktop.ui.components;

import javax.swing.*;
import java.awt.*;

public class ModernButton extends JButton {
    public ModernButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(true);
        setFocusPainted(false);
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { putClientProperty("modernHover", Boolean.TRUE); repaint(); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { putClientProperty("modernHover", Boolean.FALSE); repaint(); }
        });
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        Color base = getBackground();
        if (base == null) base = new Color(72, 150, 218);
        boolean hover = Boolean.TRUE.equals(getClientProperty("modernHover")) && isEnabled();
        Color top = hover ? base.brighter() : new Color(Math.min(255, base.getRed() + 14), Math.min(255, base.getGreen() + 14), Math.min(255, base.getBlue() + 14));
        Color bottom = isEnabled() ? base : new Color(125, 137, 151);
        if (getModel().isPressed()) { top = base.darker(); bottom = base.darker(); }
        g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
        g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 18, 18);
        if (hover) { g2.setColor(new Color(255,255,255,34)); g2.fillRoundRect(2,2,getWidth()-4,Math.max(8,getHeight()/2),16,16); }
        g2.dispose();
        setOpaque(false);
        setContentAreaFilled(false);
        super.paintComponent(g);
    }
}
