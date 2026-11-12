package codeescape.desktop.ui.screens;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class ProfileScreen {
    private ProfileScreen() {}

    public interface Host {
        JPanel background(String name);
        JPanel panel(int w, int h);
        JButton navButton(String text);
        void title(JPanel box, String text);
        JLabel image(String name, int w, int h);
        JLabel label(String text, int size, Color c);
        Color gold();
        Color cream();
        Color cyan();
        Color pink();
        Color peach();
        void stopQuestionTimer();
        void setAcceptingAnswer(boolean accepting);
        void refreshDashboard();
        void showScreen(String name);
        void startNewLesson();
    }

    public static class Refs {
        public JLabel avatar;
        public JLabel playerLabel;
        public JLabel characterLabel;
        public JLabel xpLabel;
        public JLabel correctLabel;
        public JLabel levelLabel;
        public JLabel heartsLabel;
    }

    public static class Result {
        public final JPanel panel;
        public final Refs refs;
        public Result(JPanel panel, Refs refs) { this.panel = panel; this.refs = refs; }
    }

    public static Result create(Host h, String username, String selectedCharacter, int selectedOutfit,
                                String outfitFile, int totalXp, int correctAnswers, int level, int hearts) {
        Refs r = new Refs();
        JPanel p = h.background("level5.png");
        p.setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
        top.setBackground(new Color(19, 53, 88, 245));

        JButton home = h.navButton("HOME");
        JButton play = h.navButton("PLAY");
        JButton profile = h.navButton("PROFILE");

        home.addActionListener(e -> {
            h.stopQuestionTimer();
            h.setAcceptingAnswer(false);
            h.refreshDashboard();
            h.showScreen("DASHBOARD");
        });
        play.addActionListener(e -> h.startNewLesson());
        profile.setEnabled(false);

        top.add(home);
        top.add(play);
        top.add(profile);
        p.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        JPanel box = h.panel(350, 560);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        h.title(box, "PROFILE");

        r.avatar = h.image(outfitFile, 145, 210);
        r.avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(r.avatar);

        r.playerLabel = h.label("Player: " + (username == null || username.isEmpty() ? "Guest" : username), 20, h.cream());
        r.playerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(Box.createVerticalStrut(6));
        box.add(r.playerLabel);

        r.characterLabel = h.label("Character: " + selectedCharacter, 18,
                "Girl".equalsIgnoreCase(selectedCharacter) ? h.pink() : h.cyan());
        r.characterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(Box.createVerticalStrut(6));
        box.add(r.characterLabel);

        r.xpLabel = h.label("Total XP: " + totalXp, 24, h.gold());
        r.xpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(Box.createVerticalStrut(6));
        box.add(r.xpLabel);

        r.correctLabel = h.label("Correct Answers: " + correctAnswers, 17, h.cream());
        r.correctLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(r.correctLabel);

        r.levelLabel = h.label("Current Level: " + level, 17, h.cream());
        r.levelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(r.levelLabel);

        r.heartsLabel = h.label("Hearts: " + hearts, 17, h.peach());
        r.heartsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(r.heartsLabel);

        center.add(box);
        p.add(center, BorderLayout.CENTER);

        return new Result(p, r);
    }
}
