package com.mochi.settings

import com.mochi.reminder.ReminderScheduler
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsViewModelTest {
    @Test
    fun startsWithTheStoredMotionPreference() {
        val values = FakeSettingValues(mutableMapOf("motion_preference" to "SYSTEM"))
        val viewModel = SettingsViewModel(SettingsStore(values), FakeReminderScheduler())

        assertEquals(MotionPreference.SYSTEM, viewModel.motionPreference.value)
    }

    @Test
    fun changingMotionUpdatesStateAndStorageImmediately() {
        val values = FakeSettingValues()
        val viewModel = SettingsViewModel(SettingsStore(values), FakeReminderScheduler())

        viewModel.setMotionPreference(MotionPreference.REDUCED)

        assertEquals(MotionPreference.REDUCED, viewModel.motionPreference.value)
        assertEquals("REDUCED", values.entries["motion_preference"])
    }
}

private class FakeReminderScheduler : ReminderScheduler {
    override fun schedule(hour: Int, minute: Int) = Unit

    override fun cancel() = Unit
}
