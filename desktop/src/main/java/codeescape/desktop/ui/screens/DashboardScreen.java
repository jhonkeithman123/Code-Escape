package codeescape.desktop.ui.screens;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Extracted dashboard screen builder (phase 4 modularization start).
 * The host supplies stateful widgets/actions so behavior remains in the app controller during migration.
 */
public final class DashboardScreen {
    private DashboardScreen() {}

    public interface Host {
        JPanel background(String name);
        JLabel image(String name, int w, int h);
        JLabel label(String text, int size, Color c);
        JButton dashboardActionButton(String text, Color accent);
        Color gold();
        Color cream();
        Color cyan();
        Color pink();
        Color blue();
        Color red();
    }

    public static class Refs {
        public JLabel dashboardAvatar;
        public JLabel dashboardCharacterLabel;
        public JLabel dashboardWelcome;
        public JLabel dashboardXpLabel;
    }

    public static JPanel create(Host h, Refs r,
                                String username, String selectedCharacter, int selectedOutfit, String outfitFile,
                                int displayedXp,
                                Runnable onPlay, Runnable onProfile, Runnable onExit) {
        JPanel p = h.background("castle.png");
        p.setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new javax.swing.border.EmptyBorder(10, 16, 0, 18));
        top.add(h.image("logo.png", 105, 62), BorderLayout.WEST);
        p.add(top, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new javax.swing.border.EmptyBorder(20, 28, 28, 28));

        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.gridx = 0; leftGbc.gridy = 0; leftGbc.weightx = 0.52; leftGbc.weighty = 1.0;
        leftGbc.fill = GridBagConstraints.BOTH; leftGbc.anchor = GridBagConstraints.CENTER;
        leftGbc.insets = new Insets(0, 0, 0, 22);

        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 1; rightGbc.gridy = 0; rightGbc.weightx = 0.48; rightGbc.weighty = 1.0;
        rightGbc.fill = GridBagConstraints.BOTH; rightGbc.anchor = GridBagConstraints.CENTER;
        rightGbc.insets = new Insets(0, 22, 0, 0);

        JPanel characterPanel = new JPanel();
        characterPanel.setOpaque(false);
        characterPanel.setLayout(new BoxLayout(characterPanel, BoxLayout.Y_AXIS));

        JLabel welcomeTitle = h.label("CODE ESCAPE", 32, h.gold());
        welcomeTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        characterPanel.add(welcomeTitle);

        r.dashboardWelcome = h.label("WELCOME, " + (username == null || username.isEmpty() ? "PLAYER" : username).toUpperCase(), 17, h.cream());
        r.dashboardWelcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        characterPanel.add(Box.createVerticalStrut(5));
        characterPanel.add(r.dashboardWelcome);
        characterPanel.add(Box.createVerticalStrut(12));

        JPanel characterCard = new JPanel();
        characterCard.setOpaque(true);
        characterCard.setBackground(new Color(5, 28, 67, 225));
        characterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(88, 218, 255), 2),
                new javax.swing.border.EmptyBorder(12, 18, 14, 18)));
        characterCard.setLayout(new BoxLayout(characterCard, BoxLayout.Y_AXIS));
        characterCard.setAlignmentX(Component.CENTER_ALIGNMENT);

        r.dashboardAvatar = h.image(outfitFile, 210, 300);
        r.dashboardAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        characterCard.add(r.dashboardAvatar);

        r.dashboardCharacterLabel = h.label((selectedCharacter == null ? "BOY" : selectedCharacter).toUpperCase(), 25,
                "Girl".equalsIgnoreCase(selectedCharacter) ? h.pink() : h.cyan());
        r.dashboardCharacterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        characterCard.add(Box.createVerticalStrut(4));
        characterCard.add(r.dashboardCharacterLabel);

        r.dashboardXpLabel = h.label("★  XP: " + displayedXp, 28, h.gold());
        r.dashboardXpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        characterCard.add(Box.createVerticalStrut(4));
        characterCard.add(r.dashboardXpLabel);
        characterPanel.add(characterCard);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JLabel menuTitle = h.label("MAIN MENU", 30, h.gold());
        menuTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(menuTitle);
        buttonPanel.add(Box.createVerticalStrut(18));

        JButton play = h.dashboardActionButton("▶  PLAY", h.gold());
        play.addActionListener(e -> onPlay.run());
        buttonPanel.add(play); buttonPanel.add(Box.createVerticalStrut(12));

        JButton profile = h.dashboardActionButton("PROFILE", h.blue());
        profile.addActionListener(e -> onProfile.run());
        buttonPanel.add(profile); buttonPanel.add(Box.createVerticalStrut(12));

        JButton exit = h.dashboardActionButton("EXIT", h.red());
        exit.addActionListener(e -> onExit.run());
        buttonPanel.add(exit);

        content.add(characterPanel, leftGbc);
        content.add(buttonPanel, rightGbc);
        p.add(content, BorderLayout.CENTER);
        return p;
    }
}
