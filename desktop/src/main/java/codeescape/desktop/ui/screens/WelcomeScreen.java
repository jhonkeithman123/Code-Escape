package codeescape.desktop.ui.screens;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class WelcomeScreen {
    private WelcomeScreen() {}

    public interface Ui {
        JPanel background(String name);
        JPanel panel(int w, int h);
        JLabel image(String name, int w, int h);
        void addCenter(JPanel p, String text, int size, Color c);
        JLabel label(String text, int size, Color c);
        JButton gold(String text);
        JButton blue(String text);
        Color cream();
    }

    public static JPanel create(Ui ui, Consumer<String> showScreen) {
        JPanel p = ui.background("castle.png");
        p.setLayout(new GridBagLayout());

        JPanel box = ui.panel(360, 640);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(new Color(21, 54, 88, 238));
        box.add(Box.createVerticalStrut(12));

        JLabel logo = ui.image("logo.png", 250, 155);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(logo);
        ui.addCenter(box, "Gamified Programming Learning System", 17, ui.cream());
        box.add(Box.createVerticalStrut(26));

        JButton login = ui.gold("LOGIN");
        login.setPreferredSize(new Dimension(330, 54));
        login.setMaximumSize(new Dimension(330, 54));
        login.setAlignmentX(Component.CENTER_ALIGNMENT);
        login.addActionListener(e -> showScreen.accept("LOGIN"));
        box.add(login);
        box.add(Box.createVerticalStrut(14));

        JButton signup = ui.blue("SIGN UP / REGISTER");
        signup.setPreferredSize(new Dimension(330, 54));
        signup.setMaximumSize(new Dimension(330, 54));
        signup.setAlignmentX(Component.CENTER_ALIGNMENT);
        signup.addActionListener(e -> showScreen.accept("REGISTER"));
        box.add(signup);

        p.add(box);
        return p;
    }
}
