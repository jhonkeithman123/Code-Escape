package codeescape.desktop.ui.screens;

import codeescape.desktop.ui.components.BackgroundPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class CharacterScreen {
    private CharacterScreen() {}

    public interface Host {
        BufferedImage load(String name);
        Image scale(BufferedImage img, int w, int h);
        Image scaleCharacter(BufferedImage img, int w, int h);
        JLabel label(String text, int size, Color c);
        JButton smallBlue(String text);
        JButton gold(String text);
        Color pink();
        Color blue();
        Color goldColor();
        Color cyan();
        String getSelectedCharacter();
        void setSelectedCharacter(String c);
        int getSelectedOutfit();
        void setSelectedOutfit(int o);
        String frontFacingFile(String character);
        String characterVariantFile(String character, int variant);
        void onSaveAndContinue();
        void refreshCharacterPreview(JLabel preview, JLabel topTitle);
        void refreshVariantButtons(JButton[] buttons);
        void refreshVariantImages(JButton[] buttons);
        JButton characterButton(String img, String txt);
    }

    public static JPanel create(Host h) {
        BackgroundPanel p = new BackgroundPanel("castle.png", h::load);
        p.setLayout(new BorderLayout(8, 8));

        JLabel topTitle = h.label("CHOOSE YOUR CHARACTER", 28,
                "Girl".equalsIgnoreCase(h.getSelectedCharacter()) ? h.pink() : h.blue());
        topTitle.setHorizontalAlignment(SwingConstants.CENTER);
        topTitle.setBorder(new EmptyBorder(12, 0, 4, 0));
        p.add(topTitle, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(12, 8));
        center.setOpaque(false);

        JPanel previewPanel = new JPanel(new BorderLayout(4, 8));
        previewPanel.setOpaque(false);
        previewPanel.setBackground(new Color(0, 0, 0, 0));
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(83, 190, 255), 2),
                new EmptyBorder(8, 8, 8, 8)));
        previewPanel.setPreferredSize(new Dimension(190, 390));
        previewPanel.setMinimumSize(new Dimension(125, 190));
        JLabel characterPreview = new JLabel();
        characterPreview.setHorizontalAlignment(SwingConstants.CENTER);
        characterPreview.setVerticalAlignment(SwingConstants.CENTER);
        previewPanel.add(characterPreview, BorderLayout.CENTER);

        JLabel selectedLabel = h.label("", 14, new Color(20, 45, 80));
        selectedLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewPanel.add(selectedLabel, BorderLayout.SOUTH);
        center.add(previewPanel, BorderLayout.WEST);
        center.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                int availableW = center.getWidth();
                int availableH = center.getHeight();
                if (availableW < 760) {
                    previewPanel.setPreferredSize(new Dimension(Math.max(125, availableW - 20), Math.max(190, Math.min(250, availableH / 3))));
                } else {
                    previewPanel.setPreferredSize(new Dimension(Math.max(190, Math.min(250, availableW / 4)), Math.max(300, availableH - 10)));
                }
                BufferedImage selectedImage = h.load(h.frontFacingFile(h.getSelectedCharacter()));
                if (selectedImage != null) {
                    int pw = Math.max(80, previewPanel.getWidth() - 24);
                    int ph = Math.max(100, previewPanel.getHeight() - 60);
                    characterPreview.setIcon(new ImageIcon(h.scaleCharacter(selectedImage, Math.min(pw, 175), Math.min(ph, 315))));
                }
                previewPanel.revalidate();
            }
        });

        JPanel options = new JPanel();
        options.setOpaque(false);
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setBorder(new EmptyBorder(4, 4, 4, 8));

        JLabel hint = h.label("Choose a character, then choose 1 of 4 variants.", 16, h.goldColor());
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        options.add(hint);
        options.add(Box.createVerticalStrut(8));

        JPanel genders = new JPanel(new GridLayout(1, 2, 8, 0));
        genders.setOpaque(false);
        JButton boy = h.smallBlue("BOY");
        JButton girl = h.smallBlue("GIRL");
        genders.add(boy);
        genders.add(girl);
        options.add(genders);
        options.add(Box.createVerticalStrut(10));

        JLabel variantTitle = h.label("CHARACTER VARIANTS", 14, h.cyan());
        variantTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        options.add(variantTitle);
        options.add(Box.createVerticalStrut(5));

        JPanel variants = new JPanel(new GridLayout(2, 2, 8, 8));
        variants.setOpaque(false);
        JButton[] variantButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            final int variant = i;
            variantButtons[i] = h.characterButton(h.characterVariantFile(h.getSelectedCharacter(), i), "VARIANT " + (i + 1));
            variantButtons[i].setPreferredSize(new Dimension(150, 220));
            variantButtons[i].addActionListener(e -> {
                String chosenCharacter = h.getSelectedCharacter();
                h.setSelectedOutfit(variant);
                h.setSelectedCharacter(chosenCharacter);
                h.refreshVariantButtons(variantButtons);
                h.refreshCharacterPreview(characterPreview, topTitle);
                selectedLabel.setText("SELECTED: " + h.getSelectedCharacter().toUpperCase() + " — VARIANT " + (h.getSelectedOutfit() + 1));
            });
            variants.add(variantButtons[i]);
        }
        options.add(variants);
        center.add(options, BorderLayout.CENTER);

        boy.addActionListener(e -> {
            h.setSelectedCharacter("Boy");
            h.refreshVariantImages(variantButtons);
            h.refreshCharacterPreview(characterPreview, topTitle);
            selectedLabel.setText("SELECTED: BOY — VARIANT " + (h.getSelectedOutfit() + 1));
        });
        girl.addActionListener(e -> {
            h.setSelectedCharacter("Girl");
            h.refreshVariantImages(variantButtons);
            h.refreshCharacterPreview(characterPreview, topTitle);
            selectedLabel.setText("SELECTED: GIRL — VARIANT " + (h.getSelectedOutfit() + 1));
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        JButton cont = h.gold("SAVE CHARACTER & CONTINUE");
        cont.addActionListener(e -> h.onSaveAndContinue());
        bottom.add(cont);
        p.add(center, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);

        h.refreshVariantButtons(variantButtons);
        h.refreshCharacterPreview(characterPreview, topTitle);
        selectedLabel.setText("SELECTED: " + h.getSelectedCharacter().toUpperCase() + " — VARIANT " + (h.getSelectedOutfit() + 1));
        return p;
    }
}
