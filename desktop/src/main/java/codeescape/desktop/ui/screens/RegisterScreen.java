package codeescape.desktop.ui.screens;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class RegisterScreen {
    private RegisterScreen() {}

    public interface Host {
        JPanel background(String name);
        JPanel panel(int w, int h);
        void title(JPanel box, String text);
        JLabel image(String name, int w, int h);
        JTextField textField(String tip);
        JPasswordField passwordField(String tip);
        JButton gold(String text);
        JButton smallBlue(String text);
        void onRegister(String username, String email, String password, String confirm,
                        Runnable onSuccess, Consumer<String> onError);
        void showScreen(String name);
    }

    public static JPanel create(Host h) {
        JPanel p = h.background("castle.png");
        p.setLayout(new GridBagLayout());
        JPanel box = h.panel(360, 640);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel logo = h.image("logo.png", 170, 106);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(logo);
        box.add(Box.createVerticalStrut(10));
        h.title(box, "REGISTER");

        JTextField user = h.textField("Username");
        JTextField mail = h.textField("Email");
        JPasswordField pass = h.passwordField("Password");
        JPasswordField confirm = h.passwordField("Confirm Password");
        user.addActionListener(e -> mail.requestFocusInWindow());
        mail.addActionListener(e -> pass.requestFocusInWindow());
        pass.addActionListener(e -> confirm.requestFocusInWindow());
        box.add(user); box.add(Box.createVerticalStrut(10));
        box.add(mail); box.add(Box.createVerticalStrut(10));
        box.add(pass); box.add(Box.createVerticalStrut(10));
        box.add(confirm); box.add(Box.createVerticalStrut(20));

        JButton b = h.gold("REGISTER");
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        Runnable doReg = () -> h.onRegister(user.getText().trim(), mail.getText().trim(),
                new String(pass.getPassword()), new String(confirm.getPassword()),
                () -> {}, msg -> JOptionPane.showMessageDialog(p, msg, "Code Escape", JOptionPane.ERROR_MESSAGE));
        b.addActionListener(e -> doReg.run());
        confirm.addActionListener(e -> b.doClick());
        box.add(b);
        JButton back = h.smallBlue("← BACK TO LOGIN");
        back.addActionListener(e -> h.showScreen("LOGIN"));
        box.add(Box.createVerticalStrut(12)); box.add(back);
        p.add(box);
        return p;
    }
}
