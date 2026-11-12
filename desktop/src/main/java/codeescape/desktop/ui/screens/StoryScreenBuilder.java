package codeescape.desktop.ui.screens;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public final class StoryScreenBuilder {
    private StoryScreenBuilder() {}

    public interface Host {
        JPanel background(String name);
        JLabel label(String text, int size, Color c);
        JButton blue(String text);
        JButton gold(String text);
        Color goldColor();
        int getWidthSafe();
        int getHeightSafe();
        List<int[]> buildStoryRanges(String text);
        String wrapStoryHtml(String text, int maxChars);
        void speakStoryKaraoke(String text, JLabel label);
        void stopStorySpeech();
        void setActiveStory(JLabel story, List<int[]> ranges);
    }

    public static JPanel create(Host h, String titleText, String storyText, String buttonText, String background, Runnable nextAction) {
        JPanel p = h.background(background);
        p.setLayout(new GridBagLayout());

        JPanel content = new JPanel(new BorderLayout(10, 16));
        content.setOpaque(false);
        int screenW = h.getWidthSafe();
        int screenH = h.getHeightSafe();
        int cw = Math.min(980, Math.max(320, screenW - 34));
        int ch = Math.min(680, Math.max(470, screenH - 55));
        content.setPreferredSize(new Dimension(cw, ch));
        content.setMaximumSize(new Dimension(980, 680));
        content.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel title = h.label(titleText, screenW <= 500 ? 25 : 30, h.goldColor());
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title, BorderLayout.NORTH);

        JPanel storyHolder = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 43, 72, 158));
                g2.fillRoundRect(12,12,getWidth()-24,getHeight()-24,28,28);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        storyHolder.setOpaque(false);
        storyHolder.setBorder(new EmptyBorder(14, 18, 14, 18));
        int storyCardWidth = Math.min(cw - 36, screenW <= 500 ? 360 : 620);
        storyHolder.setPreferredSize(new Dimension(Math.max(300, storyCardWidth), 205));
        storyHolder.setMaximumSize(new Dimension(Math.max(300, storyCardWidth), 220));

        JLabel story = new JLabel();
        story.setHorizontalAlignment(SwingConstants.CENTER);
        story.setVerticalAlignment(SwingConstants.CENTER);
        story.setForeground(Color.WHITE);
        story.setFont(new Font("SansSerif", Font.BOLD, screenW <= 500 ? 19 : 23));
        story.setOpaque(false);
        int storyWidth = Math.max(260, Math.min(storyCardWidth - 28, screenW <= 500 ? 315 : 560));
        int storyHeight = 165;
        story.setPreferredSize(new Dimension(storyWidth, storyHeight));
        story.setMaximumSize(new Dimension(storyWidth, storyHeight));
        List<int[]> initialRanges = h.buildStoryRanges(storyText);
        String initialLine = initialRanges.isEmpty() ? storyText : storyText.substring(initialRanges.get(0)[0], initialRanges.get(0)[1]).trim();
        story.setText(h.wrapStoryHtml(initialLine, screenW <= 500 ? 23 : 36));
        h.setActiveStory(story, h.buildStoryRanges(storyText));
        storyHolder.add(story, new GridBagConstraints());
        content.add(storyHolder, BorderLayout.CENTER);

        JButton read=h.blue("🔊 READ STORY"); read.setPreferredSize(new Dimension(165,48)); read.addActionListener(e->h.speakStoryKaraoke(storyText,story));
        JButton skip=h.blue("SKIP ▶"); skip.setPreferredSize(new Dimension(135,48)); skip.addActionListener(e->{h.stopStorySpeech();nextAction.run();});
        JButton next=h.gold(buttonText); next.setPreferredSize(new Dimension(190,48)); next.addActionListener(e->{h.stopStorySpeech();nextAction.run();});
        JPanel bottom=new JPanel(new FlowLayout(FlowLayout.CENTER,10,5)); bottom.setOpaque(false); bottom.add(read); bottom.add(skip); bottom.add(next);
        content.add(bottom,BorderLayout.SOUTH);

        p.addComponentListener(new java.awt.event.ComponentAdapter(){
            @Override public void componentShown(java.awt.event.ComponentEvent e){ h.speakStoryKaraoke(storyText,story); }
            @Override public void componentHidden(java.awt.event.ComponentEvent e){ h.stopStorySpeech(); }
        });
        p.add(content);
        return p;
    }
}
