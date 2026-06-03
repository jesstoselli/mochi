package com.mochi.reminder

import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

private const val REQUEST_ID = "mochi_daily_reminder"

/**
 * iOS implementation: a repeating calendar-triggered local notification via
 * UNUserNotificationCenter — the system fires it daily at the chosen time, no alarm
 * re-arming needed.
 */
class IosReminderScheduler : ReminderScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun schedule(hour: Int, minute: Int) {
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { granted, _ ->
            if (granted) addRequest(hour, minute)
        }
    }

    override fun cancel() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(REQUEST_ID))
    }

    private fun addRequest(hour: Int, minute: Int) {
        val content = UNMutableNotificationContent()
        content.setTitle("もち misses you!")
        content.setBody("Time for today's review — keep your streak going 🍡")

        val components = NSDateComponents()
        components.hour = hour.toLong()
        components.minute = minute.toLong()

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = components,
            repeats = true,
        )
        val request = UNNotificationRequest.requestWithIdentifier(REQUEST_ID, content, trigger)
        center.addNotificationRequest(request) { _ -> }
    }
}
