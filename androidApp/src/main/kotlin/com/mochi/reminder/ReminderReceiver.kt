package com.mochi.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires the daily reminder: posts the notification and arms the next occurrence.
 * Also re-arms the alarm after a reboot (BOOT_COMPLETED), which only reschedules.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(ReminderAlarms.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(ReminderAlarms.KEY_ENABLED, false)) return
        val hour = prefs.getInt(ReminderAlarms.KEY_HOUR, ReminderAlarms.DEFAULT_HOUR)
        val minute = prefs.getInt(ReminderAlarms.KEY_MINUTE, 0)
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            ReminderAlarms.showNotification(context)
        }
        ReminderAlarms.scheduleNext(context, hour, minute)
    }
}
