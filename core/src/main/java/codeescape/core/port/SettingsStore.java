package codeescape.core.port;

import codeescape.core.model.GameSettings;

/**
 * Platform-owned persistence for shared audio/narration settings.
 */
public interface SettingsStore {
    GameSettings load();
    void save(GameSettings settings);
}
