package codeescape.desktop.ui.screens;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class LoginScreen {
    private LoginScreen() {}

    public interface Host {
        JPanel background(String name);
        JPanel panel(int w, int h);
        void title(JPanel box, String text);
        JLabel image(String name, int w, int h);
        JTextField textField(String tip);
        JPasswordField passwordField(String tip);
        JButton gold(String text);
        JButton link(String text);
        JButton smallBlue(String text);
        JLabel label(String text, int size, Color c);
        Color cream();
        void onLogin(String username, String password, Runnable onSuccess, Consumer<String> onError);
        void showScreen(String name);
    }

    public static JPanel create(Host h) {
        JPanel p = h.background("level5.png");
        p.setLayout(new GridBagLayout());
        JPanel box = h.panel(350, 560);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel logo = h.image("logo.png", 180, 112);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(logo);
        box.add(Box.createVerticalStrut(10));
        h.title(box, "LOGIN");

        JTextField user = h.textField("Username / Email");
        JPasswordField pass = h.passwordField("Password");
        user.addActionListener(e -> pass.requestFocusInWindow());
        box.add(user);
        box.add(Box.createVerticalStrut(14));
        box.add(pass);
        box.add(Box.createVerticalStrut(24));

        JButton b = h.gold("LOGIN");
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        Runnable doLogin = () -> h.onLogin(user.getText().trim(), new String(pass.getPassword()),
                () -> {}, msg -> JOptionPane.showMessageDialog(p, msg, "Code Escape", JOptionPane.ERROR_MESSAGE));
        b.addActionListener(e -> doLogin.run());
        pass.addActionListener(e -> b.doClick());
        box.add(b);
        box.add(Box.createVerticalStrut(18));

        JLabel signupPrompt = h.label("Don't have an account?", 12, h.cream());
        signupPrompt.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(signupPrompt);
        JButton signup = h.link("SIGN UP / REGISTER");
        signup.setAlignmentX(Component.CENTER_ALIGNMENT);
        signup.addActionListener(e -> h.showScreen("REGISTER"));
        box.add(signup);

        JButton back = h.smallBlue("← BACK");
        back.addActionListener(e -> h.showScreen("WELCOME"));
        box.add(Box.createVerticalStrut(12));
        box.add(back);

        p.add(box);
        return p;
    }
}
