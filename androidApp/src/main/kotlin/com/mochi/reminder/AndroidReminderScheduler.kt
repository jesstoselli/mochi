package com.mochi.reminder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Android implementation of the daily reminder: an AlarmManager alarm that posts a
 * notification once a day (re-armed on each fire and after reboots). Asks for the
 * POST_NOTIFICATIONS permission on Android 13+ via [requestPermission].
 */
class AndroidReminderScheduler(
    private val context: Context,
    private val requestPermission: (String) -> Unit,
) : ReminderScheduler {

    override fun schedule(hour: Int, minute: Int) {
        ReminderAlarms.savePrefs(context, enabled = true, hour = hour, minute = minute)
        ReminderAlarms.ensureChannel(context)
        if (needsPermission()) requestPermission(Manifest.permission.POST_NOTIFICATIONS)
        ReminderAlarms.scheduleNext(context, hour, minute)
    }

    override fun cancel() {
        context.getSharedPreferences(ReminderAlarms.PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ReminderAlarms.KEY_ENABLED, false)
            .apply()
        ReminderAlarms.cancel(context)
    }

    private fun needsPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
}
