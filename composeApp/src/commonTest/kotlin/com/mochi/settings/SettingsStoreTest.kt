package com.mochi.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsStoreTest {
    @Test
    fun missingMotionPreferenceDefaultsToFull() {
        val store = SettingsStore(FakeSettingValues())

        assertEquals(MotionPreference.FULL, store.motionPreference())
    }

    @Test
    fun invalidMotionPreferenceDefaultsToFull() {
        val store = SettingsStore(FakeSettingValues(mutableMapOf("motion_preference" to "BROKEN")))

        assertEquals(MotionPreference.FULL, store.motionPreference())
    }

    @Test
    fun unreadableMotionPreferenceDefaultsToFull() {
        val store = SettingsStore(ThrowingSettingValues())

        assertEquals(MotionPreference.FULL, store.motionPreference())
    }

    @Test
    fun validMotionPreferenceIsRead() {
        val store = SettingsStore(FakeSettingValues(mutableMapOf("motion_preference" to "SYSTEM")))

        assertEquals(MotionPreference.SYSTEM, store.motionPreference())
    }

    @Test
    fun settingMotionPreferencePersistsItsEnumName() {
        val values = FakeSettingValues()
        val store = SettingsStore(values)

        store.setMotionPreference(MotionPreference.REDUCED)

        assertEquals("REDUCED", values.entries["motion_preference"])
    }

    @Test
    fun dailyGoalDefaultsTo20() {
        assertEquals(20, SettingsStore(FakeSettingValues()).dailyGoal())
    }

    @Test
    fun dailyGoalRoundTrips() {
        val store = SettingsStore(FakeSettingValues())

        store.setDailyGoal(30)

        assertEquals(30, store.dailyGoal())
    }

    @Test
    fun dailyGoalFallsBackOnGarbage() {
        val store = SettingsStore(FakeSettingValues(mutableMapOf("daily_goal" to "not-a-number")))

        assertEquals(20, store.dailyGoal())
    }
}

private class ThrowingSettingValues : SettingValues {
    override fun read(key: String): String? = error("storage unavailable")

    override fun write(key: String, value: String) = Unit
}

internal class FakeSettingValues(
    val entries: MutableMap<String, String> = mutableMapOf(),
) : SettingValues {
    override fun read(key: String): String? = entries[key]

    override fun write(key: String, value: String) {
        entries[key] = value
    }
}
