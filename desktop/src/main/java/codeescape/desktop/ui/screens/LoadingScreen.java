package codeescape.desktop.ui.screens;

import javax.swing.JPanel;
import java.util.function.Supplier;

public final class LoadingScreen {
    private LoadingScreen() {}

    public static JPanel create(Supplier<JPanel> backgroundPanelFactory) {
        return backgroundPanelFactory.get();
    }
}
