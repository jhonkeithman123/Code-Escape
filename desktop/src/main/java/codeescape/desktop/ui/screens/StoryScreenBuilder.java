package codeescape.desktop.ui.screens;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Desktop story screen layout/rendering: wide horizontal banner paragraph display with stable wrapping and proper centering.
 */
public final class StoryScreenBuilder {
    private StoryScreenBuilder() {}

    private static final class State {
        final String text;
        final int textWidth;
        State(String text, int textWidth) {
            this.text = text == null ? "" : text;
            // Never exceed the width computed for this screen: a larger value
            // makes the HTML view wider than the card and clips every line.
            this.textWidth = Math.max(160, textWidth);
        }
    }

    private static final Map<JLabel, State> STATES = new WeakHashMap<>();

    public interface Host {
        JPanel background(String name);
        JLabel label(String text, int size, Color c);
        JButton blue(String text);
        JButton gold(String text);
        Color goldColor();
        int getWidthSafe();
        void speakStoryKaraoke(String text, JLabel label);
        void stopStorySpeech();
        void setActiveStory(JLabel story);
    }

    public static JPanel create(Host h, String titleText, String storyText, String buttonText, String background, Runnable nextAction) {
        JPanel p = h.background(background);
        // Fill the screen instead of centering a fixed-size panel inside a
        // GridBagLayout. GridBagLayout collapses a child to its minimum size
        // whenever the child's preferred height is a few pixels taller than the
        // window, which flattened the story card and clipped the paragraph.
        p.setLayout(new BorderLayout());

        JPanel content = new JPanel(new BorderLayout(10, 16));
        content.setOpaque(false);
        int screenW = h.getWidthSafe();
        int cw = Math.min(1120, Math.max(320, screenW - 20));
        content.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel title = h.label(titleText, screenW <= 500 ? 25 : 30, h.goldColor());
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title, BorderLayout.NORTH);

        // Spacious horizontal banner width
        int cardWidth = Math.min(cw - 16, screenW <= 500 ? 360 : 940);
        int textWidth = cardWidth - 64; // Generous inner width for smooth, long lines
        int fontSize = screenW <= 500 ? 15 : 18;

        JLabel story = new JLabel();
        story.setHorizontalAlignment(SwingConstants.CENTER);
        story.setVerticalAlignment(SwingConstants.CENTER);
        story.setForeground(Color.WHITE);
        story.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        story.setOpaque(false);

        String initialText = storyText == null ? "" : storyText;
        story.setText(buildHtml(initialText, textWidth));

        // Translucent card container
        JPanel storyHolder = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 43, 72, 170));
                g2.fillRoundRect(12, 12, getWidth() - 24, getHeight() - 24, 28, 28);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        storyHolder.setOpaque(false);
        storyHolder.setBorder(new EmptyBorder(24, 32, 24, 32));
        
        GridBagConstraints storyGbc = new GridBagConstraints();
        storyGbc.gridx = 0;
        storyGbc.gridy = 0;
        storyGbc.fill = GridBagConstraints.HORIZONTAL;
        storyGbc.weightx = 1.0;
        storyHolder.add(story, storyGbc);

        // Card width is fixed, height follows the paragraph (updated in renderFull)
        storyHolder.setPreferredSize(new Dimension(cardWidth, 230));

        // Wrapper to center the card perfectly both horizontally and vertically
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        GridBagConstraints holderGbc = new GridBagConstraints();
        holderGbc.gridx = 0;
        holderGbc.gridy = 0;
        holderGbc.anchor = GridBagConstraints.CENTER;
        centerWrapper.add(storyHolder, holderGbc);

        register(story, initialText, textWidth);
        h.setActiveStory(story);
        content.add(centerWrapper, BorderLayout.CENTER);

        JButton read = h.blue("🔊 READ STORY"); read.setPreferredSize(new Dimension(165, 48)); read.addActionListener(e -> h.speakStoryKaraoke(storyText, story));
        JButton skip = h.blue("SKIP ▶"); skip.setPreferredSize(new Dimension(135, 48)); skip.addActionListener(e -> { h.stopStorySpeech(); nextAction.run(); });
        JButton next = h.gold(buttonText); next.setPreferredSize(new Dimension(190, 48)); next.addActionListener(e -> { h.stopStorySpeech(); nextAction.run(); });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5)); bottom.setOpaque(false); bottom.add(read); bottom.add(skip); bottom.add(next);
        content.add(bottom, BorderLayout.SOUTH);

        p.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { h.speakStoryKaraoke(storyText, story); }
            @Override public void componentHidden(java.awt.event.ComponentEvent e) { h.stopStorySpeech(); }
        });
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    public static void register(JLabel label, String text, int textWidth) {
        if (label == null) return;
        synchronized (STATES) { STATES.put(label, new State(text, textWidth)); }
        clearHighlight(label);
    }

    public static void clearHighlight(JLabel label) {
        renderFull(label);
    }

    private static void renderFull(JLabel label) {
        if (label == null) return;
        State st;
        synchronized (STATES) { st = STATES.get(label); }
        if (st == null) return;
        String full = st.text.trim();
        if (full.isEmpty()) full = " ";

        String html = buildHtml(full, st.textWidth);

        final String finalHtml = html;
        Runnable r = () -> {
            label.setText(finalHtml);
            // Measure the HTML at its real width: clear any previously forced
            // preferred size, otherwise the height stays stale and the label
            // centre-clips the paragraph.
            label.setPreferredSize(null);
            label.setMinimumSize(null);
            label.setMaximumSize(null);
            Dimension pref = label.getPreferredSize();
            int h = pref != null ? Math.max(120, pref.height) : 180;

            Dimension size = new Dimension(st.textWidth, h);
            label.setPreferredSize(size);
            // Keep the label at least as tall as its content so layouts cannot
            // squash it and cut off the first/last lines.
            label.setMinimumSize(size);
            label.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
            label.revalidate();
            label.repaint();

            Container holder = label.getParent();
            if (holder != null) {
                int cardW = holder.getPreferredSize().width;
                if (cardW <= 0) cardW = st.textWidth + 64;
                // Keep the banner comfortably tall even for short paragraphs.
                Dimension holderSize = new Dimension(cardW, Math.max(230, h + 48)); // border: 24 + 24
                holder.setPreferredSize(holderSize);
                holder.setMinimumSize(holderSize);
                holder.revalidate();
                holder.repaint();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) r.run(); else SwingUtilities.invokeLater(r);
    }

    private static String buildHtml(String text, int width) {
        String safeText = text == null ? "" : text.trim();
        if (safeText.isEmpty()) safeText = " ";
        // NOTE: Swing's HTML/CSS parser ignores "px" on div widths. "width: 876"
        // is honoured, "width: 876px" is not, which made the label lay the text
        // out wider than the card and clip both ends of every line.
        return "<html><div style='width: " + width + "; text-align: center; color: #E8F1FF;'>"
                + escape(safeText).replace("\n", "<br>")
                + "</div></html>";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}