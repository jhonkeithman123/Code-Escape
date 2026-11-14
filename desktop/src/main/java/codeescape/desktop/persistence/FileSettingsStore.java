package codeescape.desktop.persistence;

import codeescape.core.model.GameSettings;
import codeescape.core.port.SettingsStore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/** Persists shared audio/narration settings in a simple properties file. */
public class FileSettingsStore implements SettingsStore {
    private final File file;

    public FileSettingsStore(File file) {
        this.file = file;
    }

    @Override
    public GameSettings load() {
        GameSettings settings = new GameSettings();
        if (file == null || !file.isFile()) return settings;
        Properties props = new Properties();
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            props.load(in);
            settings.setMusicEnabled(parseBoolean(props.getProperty("musicEnabled"), true));
            settings.setMuted(parseBoolean(props.getProperty("muted"), false));
            settings.setVolume(parseVolume(props.getProperty("volume")));
            settings.setTtsEnabled(parseBoolean(props.getProperty("ttsEnabled"), true));
        } catch (Exception ignored) {
            // Corrupt/unreadable settings fall back to defaults.
        }
        return settings;
    }

    @Override
    public void save(GameSettings settings) {
        if (file == null || settings == null) return;
        Properties props = new Properties();
        props.setProperty("musicEnabled", Boolean.toString(settings.isMusicEnabled()));
        props.setProperty("muted", Boolean.toString(settings.isMuted()));
        props.setProperty("volume", Float.toString(settings.getVolume()));
        props.setProperty("ttsEnabled", Boolean.toString(settings.isTtsEnabled()));
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            props.store(out, "Code Escape settings");
        } catch (Exception ignored) {
            // Settings persistence is best-effort; gameplay must continue.
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if (value == null) return fallback;
        return Boolean.parseBoolean(value.trim());
    }

    private static float parseVolume(String value) {
        try {
            return Float.parseFloat(value.trim());
        } catch (Exception e) {
            return GameSettings.DEFAULT_VOLUME;
        }
    }
}
