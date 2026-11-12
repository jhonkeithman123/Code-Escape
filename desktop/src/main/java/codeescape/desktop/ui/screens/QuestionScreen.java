package codeescape.desktop.ui.screens;

import codeescape.core.content.GameConfig;
import codeescape.desktop.ui.components.BackgroundPanel;
import codeescape.desktop.ui.components.RoundedBorder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class QuestionScreen {
    private QuestionScreen() {}

    public static class HudRefs {
        public JLabel heartsLabel;
        public JLabel timerLabel;
        public JLabel xpLabel;
    }

    public static class Refs {
        public BackgroundPanel questionBackground;
        public JLabel questionCounterLabel;
        public JLabel questionLabel;
        public JPanel answerGrid;
        public JButton[] answerButtons = new JButton[4];
        public JPanel syntaxPanel;
        public JTextField syntaxAnswerField;
        public JButton syntaxSubmitButton;
        public HudRefs hud = new HudRefs();
    }

    /** Applies HUD label texts/colors. Pass nulls to leave fields unchanged. */
    public static void updateHud(HudRefs hud, String heartsText, String xpText, String timerText, Color timerColor) {
        if (hud == null) return;
        if (hud.heartsLabel != null && heartsText != null) hud.heartsLabel.setText(heartsText);
        if (hud.xpLabel != null && xpText != null) hud.xpLabel.setText(xpText);
        if (hud.timerLabel != null) {
            if (timerText != null) hud.timerLabel.setText(timerText);
            if (timerColor != null) hud.timerLabel.setForeground(timerColor);
        }
    }

    /** Prepares question UI for a loaded question (presentation only; controller owns timing/progression). */
    public static void renderQuestion(Refs r, String counterText, String questionHtml, boolean syntax,
                                      String[] answers, String syntaxPromptPlaceholder) {
        if (r == null) return;
        if (r.questionCounterLabel != null && counterText != null) r.questionCounterLabel.setText(counterText);
        if (r.questionLabel != null && questionHtml != null) r.questionLabel.setText(questionHtml);
        if (r.answerGrid != null) r.answerGrid.setVisible(!syntax);
        if (r.syntaxPanel != null) r.syntaxPanel.setVisible(syntax);
        if (syntax) {
            if (r.syntaxAnswerField != null) {
                r.syntaxAnswerField.setText("");
                r.syntaxAnswerField.setEnabled(true);
                if (syntaxPromptPlaceholder != null) r.syntaxAnswerField.setToolTipText(syntaxPromptPlaceholder);
            }
            if (r.syntaxSubmitButton != null) r.syntaxSubmitButton.setEnabled(true);
        } else if (r.answerButtons != null && answers != null) {
            for (int i = 0; i < r.answerButtons.length && i < answers.length; i++) {
                if (r.answerButtons[i] != null) {
                    r.answerButtons[i].setText(answers[i] == null ? "" : answers[i]);
                    r.answerButtons[i].setEnabled(true);
                }
            }
        }
        if (r.questionBackground != null) {
            r.questionBackground.revalidate();
            r.questionBackground.repaint();
        }
    }

    public static void setAnswerInputsEnabled(Refs r, boolean enabled) {
        if (r == null) return;
        if (r.answerButtons != null) {
            for (JButton b : r.answerButtons) if (b != null) b.setEnabled(enabled);
        }
        if (r.syntaxSubmitButton != null) r.syntaxSubmitButton.setEnabled(enabled);
        if (r.syntaxAnswerField != null) r.syntaxAnswerField.setEnabled(enabled);
    }

    public interface Ui {
        JPanel panel(int w, int h);
        JLabel label(String text, int size, Color c);
        JButton answerButton(String text);
        JTextField textField(String tip);
        JButton gold(String text);
        JButton base(String text);
        Color gold();
        Color cream();
        Color cyan();
        Color red();
        Function<String, BufferedImage> loader();
        String heartsText();
        int xp();
        int secondsLeft();
    }

    public static Refs create(Ui ui, IntConsumer onAnswer, Runnable onSyntaxSubmit, Runnable onExit) {
        Refs r = new Refs();
        r.questionBackground = new BackgroundPanel("level1.png", ui.loader());
        r.questionBackground.setLayout(new BorderLayout());

        JPanel hud = new JPanel(new BorderLayout());
        hud.setBackground(new Color(19, 53, 88, 245));
        hud.setBorder(new EmptyBorder(8, 12, 8, 12));
        JPanel stats = new JPanel(new GridLayout(1, 3));
        stats.setOpaque(false);
        r.hud.heartsLabel = ui.label(ui.heartsText(), 24, ui.red());
        r.hud.heartsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        r.hud.timerLabel = ui.label("⏱ " + ui.secondsLeft() + "s", 24, ui.gold());
        r.hud.timerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        r.hud.xpLabel = ui.label("⭐ XP: " + ui.xp(), 24, ui.gold());
        r.hud.xpLabel.setHorizontalAlignment(SwingConstants.CENTER);
        stats.add(r.hud.heartsLabel);
        stats.add(r.hud.timerLabel);
        stats.add(r.hud.xpLabel);

        JButton exit = circularExitButton(ui.base("EXIT"), ui.red());
        exit.addActionListener(e -> onExit.run());
        hud.add(stats, BorderLayout.CENTER);
        hud.add(exit, BorderLayout.EAST);
        r.questionBackground.add(hud, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        JPanel card = ui.panel(350, 540);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(111, 196, 255, 220), 2, 22),
                new EmptyBorder(14, 12, 18, 12)));

        r.questionCounterLabel = ui.label("LEVEL 1 — QUESTION 1 / " + GameConfig.QUESTIONS_PER_LEVEL, 21, ui.gold());
        r.questionCounterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        r.questionCounterLabel.setHorizontalAlignment(SwingConstants.CENTER);

        r.questionLabel = new JLabel();
        r.questionLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        r.questionLabel.setForeground(ui.cream());
        r.questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        r.questionLabel.setVerticalAlignment(SwingConstants.CENTER);
        JPanel questionHolder = new JPanel(new BorderLayout());
        questionHolder.setOpaque(false);
        questionHolder.setPreferredSize(new Dimension(320, 150));
        questionHolder.setMinimumSize(new Dimension(280, 120));
        questionHolder.setMaximumSize(new Dimension(340, 180));
        questionHolder.add(r.questionLabel, BorderLayout.CENTER);

        r.answerGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        r.answerGrid.setOpaque(false);
        r.answerGrid.setBorder(new EmptyBorder(12, 6, 8, 6));
        r.answerGrid.setPreferredSize(new Dimension(320, 190));
        r.answerGrid.setMaximumSize(new Dimension(340, 210));
        for (int i = 0; i < 4; i++) {
            final int choice = i;
            r.answerButtons[i] = ui.answerButton("");
            r.answerButtons[i].addActionListener(e -> onAnswer.accept(choice));
            r.answerGrid.add(r.answerButtons[i]);
        }

        r.syntaxPanel = new JPanel();
        r.syntaxPanel.setOpaque(false);
        r.syntaxPanel.setLayout(new BoxLayout(r.syntaxPanel, BoxLayout.Y_AXIS));
        JLabel sh = ui.label("Type the missing syntax below:", 14, ui.cyan());
        sh.setAlignmentX(Component.CENTER_ALIGNMENT);
        r.syntaxAnswerField = ui.textField("Complete the syntax here");
        r.syntaxAnswerField.setMaximumSize(new Dimension(320, 48));
        r.syntaxSubmitButton = ui.gold("SUBMIT ANSWER");
        r.syntaxSubmitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        r.syntaxSubmitButton.addActionListener(e -> onSyntaxSubmit.run());
        r.syntaxAnswerField.addActionListener(e -> onSyntaxSubmit.run());
        r.syntaxPanel.add(sh);
        r.syntaxPanel.add(Box.createVerticalStrut(10));
        r.syntaxPanel.add(r.syntaxAnswerField);
        r.syntaxPanel.add(Box.createVerticalStrut(12));
        r.syntaxPanel.add(r.syntaxSubmitButton);

        card.add(Box.createVerticalStrut(12));
        card.add(r.questionCounterLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(questionHolder);
        card.add(Box.createVerticalStrut(10));
        card.add(r.answerGrid);
        card.add(r.syntaxPanel);
        card.add(Box.createVerticalGlue());
        center.add(card);
        r.questionBackground.add(center, BorderLayout.CENTER);
        return r;
    }

    private static JButton circularExitButton(JButton b, Color red) {
        b.setPreferredSize(new Dimension(105, 42));
        b.setMinimumSize(new Dimension(105, 42));
        b.setMaximumSize(new Dimension(105, 42));
        b.setFont(new Font("SansSerif", Font.BOLD, 17));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(120, 28, 45));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(red, 2),
            new EmptyBorder(6, 18, 6, 18)
        ));
        b.setToolTipText("Exit");
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
