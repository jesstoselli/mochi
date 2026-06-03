package com.mochi.reminder

private const val PAD_WIDTH = 2

/** A local time of day for the daily study reminder. */
data class ReminderTime(val hour: Int, val minute: Int) {
    /** "HH:mm" for display and storage. */
    fun formatted(): String =
        "${hour.toString().padStart(PAD_WIDTH, '0')}:${minute.toString().padStart(PAD_WIDTH, '0')}"
}

/**
 * Schedules (or clears) the once-a-day study reminder notification.
 * Implemented per platform: AlarmManager + notification on Android,
 * UNUserNotificationCenter with a repeating calendar trigger on iOS.
 */
interface ReminderScheduler {
    /** (Re)schedules the daily reminder, asking for notification permission if needed. */
    fun schedule(hour: Int, minute: Int)

    fun cancel()
}
