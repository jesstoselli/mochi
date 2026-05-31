package com.mochi.settings

import com.mochi.db.AppDatabase

/** Reads/writes app preferences from the app_setting key/value table. */
class SettingsStore(private val db: AppDatabase) {

    fun themeMode(): ThemeMode {
        val stored = db.settingsQueries.selectSetting(KEY_THEME).executeAsOneOrNull()
        return stored?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    fun setThemeMode(mode: ThemeMode) {
        db.settingsQueries.upsertSetting(KEY_THEME, mode.name)
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
    }
}
