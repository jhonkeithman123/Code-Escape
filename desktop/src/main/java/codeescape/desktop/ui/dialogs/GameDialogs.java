package codeescape.desktop.ui.dialogs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Function;
import java.util.function.Supplier;

public final class GameDialogs {
    private GameDialogs() {}

    public static void showGameExitDialog(Frame owner,
                                          Color gold, Color cream, Color red,
                                          Function<String, JLabel> labelFactory,
                                          Function<String, JButton> goldButtonFactory,
                                          Function<String, JButton> baseButtonFactory,
                                          Supplier<Integer> secondsLeft,
                                          Runnable onContinue,
                                          Runnable onExit) {
        final JDialog dialog = new JDialog(owner, "EXIT GAME", true);
        dialog.setUndecorated(true);
        dialog.setSize(360, 230);
        dialog.setLocationRelativeTo(owner);
        dialog.setResizable(false);

        JPanel box = new JPanel();
        box.setBackground(new Color(5, 28, 67, 250));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(gold, 3),
            new EmptyBorder(22, 25, 22, 25)
        ));
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel title = labelFactory.apply("EXIT GAME?");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(gold);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(title);
        box.add(Box.createVerticalStrut(10));

        JLabel msg = labelFactory.apply("Your unfinished run will not be saved.");
        msg.setFont(new Font("SansSerif", Font.BOLD, 15));
        msg.setForeground(cream);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(msg);
        box.add(Box.createVerticalStrut(20));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);

        JButton continueButton = goldButtonFactory.apply("CONTINUE GAME");
        continueButton.setPreferredSize(new Dimension(150, 45));
        continueButton.addActionListener(e -> {
            dialog.dispose();
            onContinue.run();
            if (secondsLeft.get() > 0) {
                // caller handles restart timer if needed via onContinue context; kept for parity
            }
        });

        JButton exitButton = baseButtonFactory.apply("EXIT GAME");
        exitButton.setPreferredSize(new Dimension(130, 45));
        exitButton.setBackground(new Color(120, 28, 45));
        exitButton.setForeground(Color.BLACK);
        exitButton.setBorder(BorderFactory.createLineBorder(red, 2));
        exitButton.addActionListener(e -> {
            dialog.dispose();
            onExit.run();
        });

        buttons.add(continueButton);
        buttons.add(exitButton);
        box.add(buttons);

        dialog.setContentPane(box);
        dialog.setVisible(true);
    }

    public static void openTreasureChest(Frame owner,
                                         Function<String, BufferedImage> load,
                                         Function<BufferedImage, Image> scale350x390,
                                         Function<String, JLabel> labelFactory,
                                         Function<String, JButton> goldButtonFactory,
                                         Color gold,
                                         Runnable playVictoryMusic,
                                         Runnable onContinue) {
        playVictoryMusic.run();
        JDialog dialog = new JDialog(owner, "Final Treasure", true);
        dialog.setSize(430, 620);
        dialog.setLocationRelativeTo(owner);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(new Color(5, 18, 43));

        JLabel title = labelFactory.apply("YOU FOUND THE FINAL TREASURE!");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(gold);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        dialog.add(title, BorderLayout.NORTH);

        JPanel notePanel = new JPanel(new BorderLayout());
        notePanel.setOpaque(false);
        BufferedImage note = load.apply("treasure_note.png");
        if (note != null) {
            JLabel noteImage = new JLabel(new ImageIcon(scale350x390.apply(note)));
            noteImage.setHorizontalAlignment(SwingConstants.CENTER);
            notePanel.add(noteImage, BorderLayout.CENTER);
        }
        dialog.add(notePanel, BorderLayout.CENTER);

        JButton finish = goldButtonFactory.apply("CONTINUE");
        finish.setPreferredSize(new Dimension(180, 48));
        finish.addActionListener(e -> {
            dialog.dispose();
            onContinue.run();
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(finish);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    public static void showEndingXpDialog(Frame owner,
                                          int savedXp,
                                          Function<String, JLabel> labelFactory,
                                          Function<String, JButton> goldButtonFactory,
                                          Function<String, JButton> smallBlueButtonFactory,
                                          Color gold, Color cream,
                                          Runnable onMenu,
                                          Runnable onRetry) {
        JDialog result = new JDialog(owner, "ATTEMPT RESULTS", true);
        result.setSize(390, 270);
        result.setLocationRelativeTo(owner);
        result.setResizable(false);
        JPanel box = new JPanel();
        box.setBackground(new Color(5, 18, 43));
        box.setBorder(new EmptyBorder(22, 24, 22, 24));
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel heading = labelFactory.apply("LEVELS COMPLETED!");
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        heading.setForeground(gold);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel earned = labelFactory.apply("XP collected this login: " + savedXp);
        earned.setFont(new Font("SansSerif", Font.BOLD, 18));
        earned.setForeground(cream);
        earned.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(heading);
        box.add(Box.createVerticalStrut(14));
        box.add(earned);
        box.add(Box.createVerticalStrut(24));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actions.setOpaque(false);
        JButton menu = goldButtonFactory.apply("MENU");
        menu.addActionListener(e -> {
            result.dispose();
            onMenu.run();
        });
        JButton retry = smallBlueButtonFactory.apply("TRY AGAIN");
        retry.addActionListener(e -> {
            result.dispose();
            onRetry.run();
        });
        actions.add(menu);
        actions.add(retry);
        box.add(actions);
        result.setContentPane(box);
        result.setVisible(true);
    }
}
