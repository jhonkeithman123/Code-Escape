package codeescape.desktop.ui.components;

import javax.swing.*;
import java.awt.*;

public class PlaceholderPasswordField extends JPasswordField {
    private final String placeholder;
    public PlaceholderPasswordField(String placeholder) { this.placeholder = placeholder; }
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getPassword().length == 0 && !isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(190, 205, 225));
            g2.setFont(getFont());
            Insets in = getInsets();
            g2.drawString(placeholder, in.left, getHeight() / 2 + getFont().getSize() / 2 - 3);
            g2.dispose();
        }
    }
}
