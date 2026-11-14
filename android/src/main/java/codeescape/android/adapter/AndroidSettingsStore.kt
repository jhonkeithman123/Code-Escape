package codeescape.android.adapter

import android.content.Context
import codeescape.core.model.GameSettings
import codeescape.core.port.SettingsStore

/** Persists shared audio/narration settings in SharedPreferences. */
class AndroidSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences("codeescape_settings", Context.MODE_PRIVATE)

    override fun load(): GameSettings = GameSettings(
        prefs.getBoolean("musicEnabled", true),
        prefs.getBoolean("muted", false),
        prefs.getFloat("volume", GameSettings.DEFAULT_VOLUME),
        prefs.getBoolean("ttsEnabled", true)
    )

    override fun save(settings: GameSettings) {
        prefs.edit()
            .putBoolean("musicEnabled", settings.isMusicEnabled)
            .putBoolean("muted", settings.isMuted)
            .putFloat("volume", settings.volume)
            .putBoolean("ttsEnabled", settings.isTtsEnabled)
            .apply()
    }
}
