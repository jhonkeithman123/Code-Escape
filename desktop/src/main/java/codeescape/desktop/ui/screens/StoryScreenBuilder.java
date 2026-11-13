package codeescape.desktop.ui.screens;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class StoryScreenBuilder {
    private StoryScreenBuilder() {}

    private static final class State {
        final String text;
        final List<int[]> ranges;
        final int maxChars;
        int highlightStart = -1;
        int highlightEnd = -1;
        State(String text, List<int[]> ranges, int maxChars) {
            this.text = text;
            this.ranges = ranges;
            this.maxChars = maxChars;
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
        int maxChars = screenW <= 500 ? 23 : 36;
        register(story, storyText, initialRanges, maxChars);
        h.setActiveStory(story, initialRanges);
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

    public static void register(JLabel label, String text, List<int[]> ranges, int maxChars) {
        if (label == null) return;
        synchronized (STATES) { STATES.put(label, new State(text == null ? "" : text, ranges, maxChars)); }
        clearHighlight(label);
    }

    public static void clearHighlight(JLabel label) {
        if (label == null) return;
        State st;
        synchronized (STATES) { st = STATES.get(label); }
        if (st == null) return;
        st.highlightStart = -1; st.highlightEnd = -1;
        render(label, st);
    }

    public static void highlightLine(JLabel label, int index) {
        if (label == null) return;
        State st;
        synchronized (STATES) { st = STATES.get(label); }
        if (st == null || st.ranges == null || index < 0 || index >= st.ranges.size()) return;
        int[] r = st.ranges.get(index);
        highlightRange(label, r[0], r[1]);
    }

    public static void highlightRange(JLabel label, int start, int end) {
        if (label == null) return;
        State st;
        synchronized (STATES) { st = STATES.get(label); }
        if (st == null) return;
        st.highlightStart = Math.max(0, start);
        st.highlightEnd = Math.min(st.text.length(), Math.max(st.highlightStart, end));
        render(label, st);
    }

    private static void render(JLabel label, State st) {
        String full = st.text == null ? "" : st.text.trim();
        String html;
        int w = Math.max(220, label.getPreferredSize() != null ? label.getPreferredSize().width : 360);
        if (st.highlightStart >= 0 && st.highlightEnd > st.highlightStart && st.highlightEnd <= st.text.length()) {
            int s = Math.max(0, Math.min(st.highlightStart, full.length()));
            int e = Math.max(s, Math.min(st.highlightEnd, full.length()));
            // Adjust indices against trimmed full text if needed (ranges were built from original text)
            String original = st.text == null ? "" : st.text;
            int trimOffset = original.indexOf(full);
            if (trimOffset > 0) {
                s = Math.max(0, st.highlightStart - trimOffset);
                e = Math.max(s, st.highlightEnd - trimOffset);
                s = Math.min(s, full.length());
                e = Math.min(e, full.length());
            }
            String before = full.substring(0, s);
            String active = full.substring(s, e);
            String after = full.substring(e);
            // Always show full story text; highlight the currently read range within it.
            html = "<html><div style='text-align:center;width:" + w + "px;'>"
                    + "<span style='color:#E8F1FF;'>" + escape(before) + "</span>"
                    + "<span style='color:#FFFFFF;background-color:#2E6FA8;font-weight:bold;'>" + escape(active) + "</span>"
                    + "<span style='color:#E8F1FF;'>" + escape(after) + "</span>"
                    + "</div></html>";
        } else {
            html = "<html><div style='text-align:center;width:" + w + "px;'>"
                    + "<span style='color:#E8F1FF;'>" + escape(full) + "</span>"
                    + "</div></html>";
        }
        Runnable r = () -> label.setText(html);
        if (SwingUtilities.isEventDispatchThread()) r.run(); else SwingUtilities.invokeLater(r);
    }

    private static java.util.ArrayList<String> wrapLines(String text, int maxChars) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        if (text == null) return lines;
        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (current.length() == 0) current.append(word);
            else if (current.length() + 1 + word.length() <= maxChars) current.append(' ').append(word);
            else { lines.add(current.toString()); current.setLength(0); current.append(word); }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
