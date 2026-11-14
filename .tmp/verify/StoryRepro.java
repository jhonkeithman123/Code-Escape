import codeescape.desktop.ui.screens.StoryScreenBuilder;
import codeescape.core.content.StoryContent;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

public class StoryRepro {
    static String storyText;

    static JLabel label(String text, int size, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, size));
        l.setForeground(c);
        return l;
    }

    public static void main(String[] args) throws Exception {
        final StoryContent.StoryPage sp = StoryContent.INTRO;
        final int w = args.length > 0 ? Integer.parseInt(args[0]) : 1280;
        final int h = args.length > 1 ? Integer.parseInt(args[1]) : 800;
        final String out = args.length > 2 ? args[2] : ".tmp/verify/story.png";
        storyText = sp.body;

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                final JFrame f = new JFrame("story smoke");
                f.setSize(w, h);
                f.setContentPane(new JPanel(new BorderLayout()));
                JPanel screen = StoryScreenBuilder.create(new StoryScreenBuilder.Host() {
                    public JPanel background(String name) {
                        JPanel p = new JPanel();
                        p.setBackground(new Color(60, 90, 120));
                        return p;
                    }
                    public JLabel label(String text, int size, Color c) { return StoryRepro.label(text, size, c); }
                    public JButton blue(String text) { return new JButton(text); }
                    public JButton gold(String text) { return new JButton(text); }
                    public Color goldColor() { return new Color(0xFFD54A); }
                    public int getWidthSafe() { return f.getWidth() > 0 ? f.getWidth() : 430; }
                    public void speakStoryKaraoke(String text, JLabel label) { }
                    public void stopStorySpeech() { }
                    public void setActiveStory(JLabel story) { }
                }, sp.title, sp.body, sp.buttonText, sp.backgroundImage, new Runnable() { public void run() { } });
                f.getContentPane().add(screen, BorderLayout.CENTER);
                f.setVisible(true);
            }
        });
        Thread.sleep(900);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                Window[] ws = Window.getWindows();
                Container root = ws[ws.length - 1] instanceof JFrame ? ((JFrame) ws[ws.length - 1]).getContentPane() : null;
                if (root == null) throw new RuntimeException("no window");
                BufferedImage img = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                root.paint(g);
                g.dispose();
                try { ImageIO.write(img, "png", new File(out)); } catch (Exception e) { throw new RuntimeException(e); }
                System.out.println("wrote " + out + " " + img.getWidth() + "x" + img.getHeight());
            }
        });
        System.exit(0);
    }
}
