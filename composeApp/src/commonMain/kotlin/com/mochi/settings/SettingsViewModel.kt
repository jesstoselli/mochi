package com.mochi.settings

import androidx.lifecycle.ViewModel
import com.mochi.reminder.ReminderScheduler
import com.mochi.reminder.ReminderTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds the user preferences (theme, daily new-card limit, reminder) and persists changes. */
class SettingsViewModel(
    private val store: SettingsStore,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _themeMode = MutableStateFlow(store.themeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _newCardLimit = MutableStateFlow(store.newCardLimit())
    val newCardLimit: StateFlow<Int> = _newCardLimit.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(store.reminderEnabled())
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _reminderTime = MutableStateFlow(store.reminderTime())
    val reminderTime: StateFlow<ReminderTime> = _reminderTime.asStateFlow()

    init {
        // Re-register on app start so the reminder survives reboots and app updates.
        if (_reminderEnabled.value) {
            val time = _reminderTime.value
            reminderScheduler.schedule(time.hour, time.minute)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        store.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun setNewCardLimit(limit: Int) {
        store.setNewCardLimit(limit)
        _newCardLimit.value = limit
    }

    fun setReminderEnabled(enabled: Boolean) {
        store.setReminderEnabled(enabled)
        _reminderEnabled.value = enabled
        val time = _reminderTime.value
        if (enabled) reminderScheduler.schedule(time.hour, time.minute) else reminderScheduler.cancel()
    }

    fun setReminderTime(hour: Int, minute: Int) {
        val time = ReminderTime(hour, minute)
        store.setReminderTime(time)
        _reminderTime.value = time
        if (_reminderEnabled.value) reminderScheduler.schedule(hour, minute)
    }
}
