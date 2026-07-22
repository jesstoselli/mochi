package com.mochi.settings

import com.mochi.db.AppDatabase
import com.mochi.reminder.ReminderTime

/** Just the daily new-card limit the review flow needs (lets the ViewModel use a fake). */
interface NewCardLimitSource {
    fun newCardLimit(): Int
}

internal interface SettingValues {
    fun read(key: String): String?

    fun write(key: String, value: String)
}

private class DatabaseSettingValues(private val db: AppDatabase) : SettingValues {
    override fun read(key: String): String? =
        db.settingsQueries.selectSetting(key).executeAsOneOrNull()

    override fun write(key: String, value: String) {
        db.settingsQueries.upsertSetting(key, value)
    }
}

/** Reads/writes app preferences from the app_setting key/value table. */
class SettingsStore internal constructor(
    private val values: SettingValues,
) : NewCardLimitSource {

    constructor(db: AppDatabase) : this(DatabaseSettingValues(db))

    fun themeMode(): ThemeMode {
        val stored = values.read(KEY_THEME)
        return stored?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    fun setThemeMode(mode: ThemeMode) {
        values.write(KEY_THEME, mode.name)
    }

    fun motionPreference(): MotionPreference =
        values.read(KEY_MOTION)
            ?.let { runCatching { MotionPreference.valueOf(it) }.getOrNull() }
            ?: MotionPreference.FULL

    fun setMotionPreference(preference: MotionPreference) {
        values.write(KEY_MOTION, preference.name)
    }

    /** New cards introduced per day. 0 means unlimited. */
    override fun newCardLimit(): Int {
        val stored = values.read(KEY_NEW_LIMIT)
        return stored?.toIntOrNull() ?: DEFAULT_NEW_LIMIT
    }

    fun setNewCardLimit(limit: Int) {
        values.write(KEY_NEW_LIMIT, limit.toString())
    }

    fun reminderEnabled(): Boolean =
        values.read(KEY_REMINDER_ENABLED) == "1"

    fun setReminderEnabled(enabled: Boolean) {
        values.write(KEY_REMINDER_ENABLED, if (enabled) "1" else "0")
    }

    /** Time of day for the daily reminder (stored as "HH:mm"). */
    fun reminderTime(): ReminderTime {
        val stored = values.read(KEY_REMINDER_TIME)
        val parts = stored?.split(":")
        val hour = parts?.getOrNull(0)?.toIntOrNull()
        val minute = parts?.getOrNull(1)?.toIntOrNull()
        if (hour == null || minute == null) return ReminderTime(DEFAULT_REMINDER_HOUR, 0)
        return ReminderTime(hour, minute)
    }

    fun setReminderTime(time: ReminderTime) {
        values.write(KEY_REMINDER_TIME, time.formatted())
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_MOTION = "motion_preference"
        const val KEY_NEW_LIMIT = "new_card_limit"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_TIME = "reminder_time"
        const val DEFAULT_NEW_LIMIT = 20
        const val DEFAULT_REMINDER_HOUR = 20
    }
}
