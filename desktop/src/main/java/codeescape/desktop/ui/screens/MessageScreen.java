package codeescape.desktop.ui.screens;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public final class MessageScreen {
    private MessageScreen() {}

    public interface Host {
        JPanel background(String name);
        JPanel panel(int w, int h);
        void title(JPanel box, String text);
        void addCenter(JPanel box, String text, int size, Color c);
        JLabel label(String text, int size, Color c);
        JButton gold(String text);
        JButton blue(String text);
        Color cream();
        Color goldColor();
    }

    public static class Result {
        public JPanel panel;
        public JLabel xpLabel; // gameOverXpLabel or completeXpLabel when applicable
    }

    public static Result create(Host h, String titleText, String msg, String detail,
                                String a, ActionListener aa, String b, ActionListener bb, String bg, int xpValue) {
        Result r = new Result();
        JPanel p = h.background(bg);
        p.setLayout(new GridBagLayout());
        JPanel box = h.panel(350, 390);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        h.title(box, titleText);
        h.addCenter(box, msg, 19, h.cream());
        if ("GAME OVER".equals(titleText)) {
            r.xpLabel = h.label("Final XP: " + xpValue, 25, h.goldColor());
            r.xpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(Box.createVerticalStrut(6));
            box.add(r.xpLabel);
        } else if ("LESSON COMPLETE!".equals(titleText)) {
            r.xpLabel = h.label("Final XP: " + xpValue, 25, h.goldColor());
            r.xpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(Box.createVerticalStrut(6));
            box.add(r.xpLabel);
        } else {
            h.addCenter(box, detail, 25, h.goldColor());
        }
        box.add(Box.createVerticalStrut(25));
        JButton x = h.gold(a);
        x.setAlignmentX(Component.CENTER_ALIGNMENT);
        x.addActionListener(aa);
        box.add(x);
        box.add(Box.createVerticalStrut(12));
        JButton y = h.blue(b);
        y.setAlignmentX(Component.CENTER_ALIGNMENT);
        y.addActionListener(bb);
        box.add(y);
        p.add(box);
        r.panel = p;
        return r;
    }
}
