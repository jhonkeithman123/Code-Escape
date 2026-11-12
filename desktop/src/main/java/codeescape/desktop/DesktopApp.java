package codeescape.desktop;

import codeescape.CodeEscapeApp;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class DesktopApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            // Bridge entrypoint: launches the existing Swing application while core/frontend modularization is in progress.
            CodeEscapeApp app = new CodeEscapeApp();
            app.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
            app.setVisible(true);
        });
    }
}
