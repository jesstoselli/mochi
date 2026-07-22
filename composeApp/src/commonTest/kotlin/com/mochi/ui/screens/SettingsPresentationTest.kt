package com.mochi.ui.screens

import com.mochi.settings.MotionPreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsPresentationTest {
    @Test
    fun motionChoicesUseTheApprovedCopy() {
        assertEquals("Full", MotionPreference.FULL.label())
        assertEquals("Play all Mochi animations.", MotionPreference.FULL.description())
        assertEquals("System", MotionPreference.SYSTEM.label())
        assertEquals(
            "Follow your device accessibility setting.",
            MotionPreference.SYSTEM.description(),
        )
        assertEquals("Reduced", MotionPreference.REDUCED.label())
        assertEquals("Use fades and simpler transitions.", MotionPreference.REDUCED.description())
    }

    @Test
    fun reminderTimeVisibilityTracksTheToggle() {
        assertTrue(shouldShowReminderTime(reminderEnabled = true))
        assertFalse(shouldShowReminderTime(reminderEnabled = false))
    }

    @Test
    fun sectionCopyMatchesTheApprovedLayout() {
        assertEquals("Make Mochi feel right for you.", SETTINGS_SUBTITLE)
        assertEquals("Study your way", SETTINGS_INTRO_TITLE)
        assertEquals("Look & feel", SETTINGS_LOOK_AND_FEEL)
        assertEquals("Study rhythm", SETTINGS_STUDY_RHYTHM)
    }
}
