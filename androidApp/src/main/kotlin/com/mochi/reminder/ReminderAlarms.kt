package com.mochi.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mochi.MainActivity
import com.mochi.R
import java.util.Calendar

/** Shared helpers for scheduling the daily reminder alarm and posting its notification. */
internal object ReminderAlarms {
    const val PREFS = "mochi_reminder"
    const val KEY_ENABLED = "enabled"
    const val KEY_HOUR = "hour"
    const val KEY_MINUTE = "minute"
    const val DEFAULT_HOUR = 20

    private const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 1001
    private const val REQUEST_CODE = 2001

    /** Mirrors the reminder settings so the receiver can reschedule after firing/reboot. */
    fun savePrefs(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily reminder",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "A daily nudge to review your flashcards" }
        manager.createNotificationChannel(channel)
    }

    /** Arms a single alarm for the next occurrence of [hour]:[minute] (rearmed on each fire). */
    fun scheduleNext(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerMillis(hour, minute),
            alarmIntent(context),
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmIntent(context))
    }

    fun showNotification(context: Context) {
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mochi)
            .setContentTitle("もち misses you!")
            .setContentText("Time for today's review — keep your streak going 🍡")
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        // POST_NOTIFICATIONS may have been revoked between scheduling and firing.
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    private fun alarmIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }
}
