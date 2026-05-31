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

    /** New cards introduced per day. 0 means unlimited. */
    fun newCardLimit(): Int {
        val stored = db.settingsQueries.selectSetting(KEY_NEW_LIMIT).executeAsOneOrNull()
        return stored?.toIntOrNull() ?: DEFAULT_NEW_LIMIT
    }

    fun setNewCardLimit(limit: Int) {
        db.settingsQueries.upsertSetting(KEY_NEW_LIMIT, limit.toString())
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_NEW_LIMIT = "new_card_limit"
        const val DEFAULT_NEW_LIMIT = 20
    }
}
