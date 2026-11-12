package codeescape.desktop.ui.screens;

import codeescape.desktop.ui.components.BackgroundPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Function;

public final class EndingScreen {
    private EndingScreen() {}

    public static class Result {
        public final JPanel panel;
        public final JLabel instructions;
        public Result(JPanel panel, JLabel instructions) { this.panel = panel; this.instructions = instructions; }
    }

    public static Result create(Function<String, BufferedImage> loader, JComponent scene, Color gold) {
        BackgroundPanel background = new BackgroundPanel("castle.png", loader);
        background.setLayout(new BorderLayout());
        JLabel title = new JLabel("THE FINAL TREASURE", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(gold);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(14, 8, 4, 8));
        background.add(title, BorderLayout.NORTH);

        JLabel instructions = new JLabel("YOUR CHARACTER IS RUNNING TOWARD THE ANCIENT VAULT...", SwingConstants.CENTER);
        instructions.setFont(new Font("SansSerif", Font.BOLD, 15));
        instructions.setForeground(gold);
        instructions.setHorizontalAlignment(SwingConstants.CENTER);
        instructions.setBorder(new EmptyBorder(4, 8, 12, 8));
        background.add(instructions, BorderLayout.SOUTH);

        if (scene != null) background.add(scene, BorderLayout.CENTER);
        return new Result(background, instructions);
    }
}
