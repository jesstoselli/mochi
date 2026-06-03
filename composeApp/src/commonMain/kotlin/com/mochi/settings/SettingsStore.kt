package com.mochi.settings

import com.mochi.db.AppDatabase
import com.mochi.reminder.ReminderTime

/** Just the daily new-card limit the review flow needs (lets the ViewModel use a fake). */
interface NewCardLimitSource {
    fun newCardLimit(): Int
}

/** Reads/writes app preferences from the app_setting key/value table. */
class SettingsStore(private val db: AppDatabase) : NewCardLimitSource {

    fun themeMode(): ThemeMode {
        val stored = db.settingsQueries.selectSetting(KEY_THEME).executeAsOneOrNull()
        return stored?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    fun setThemeMode(mode: ThemeMode) {
        db.settingsQueries.upsertSetting(KEY_THEME, mode.name)
    }

    /** New cards introduced per day. 0 means unlimited. */
    override fun newCardLimit(): Int {
        val stored = db.settingsQueries.selectSetting(KEY_NEW_LIMIT).executeAsOneOrNull()
        return stored?.toIntOrNull() ?: DEFAULT_NEW_LIMIT
    }

    fun setNewCardLimit(limit: Int) {
        db.settingsQueries.upsertSetting(KEY_NEW_LIMIT, limit.toString())
    }

    fun reminderEnabled(): Boolean =
        db.settingsQueries.selectSetting(KEY_REMINDER_ENABLED).executeAsOneOrNull() == "1"

    fun setReminderEnabled(enabled: Boolean) {
        db.settingsQueries.upsertSetting(KEY_REMINDER_ENABLED, if (enabled) "1" else "0")
    }

    /** Time of day for the daily reminder (stored as "HH:mm"). */
    fun reminderTime(): ReminderTime {
        val stored = db.settingsQueries.selectSetting(KEY_REMINDER_TIME).executeAsOneOrNull()
        val parts = stored?.split(":")
        val hour = parts?.getOrNull(0)?.toIntOrNull()
        val minute = parts?.getOrNull(1)?.toIntOrNull()
        if (hour == null || minute == null) return ReminderTime(DEFAULT_REMINDER_HOUR, 0)
        return ReminderTime(hour, minute)
    }

    fun setReminderTime(time: ReminderTime) {
        db.settingsQueries.upsertSetting(KEY_REMINDER_TIME, time.formatted())
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_NEW_LIMIT = "new_card_limit"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_TIME = "reminder_time"
        const val DEFAULT_NEW_LIMIT = 20
        const val DEFAULT_REMINDER_HOUR = 20
    }
}
