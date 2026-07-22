package com.mochi.ui.screens

import com.mochi.settings.MotionPreference
import com.mochi.settings.ThemeMode

internal const val SETTINGS_SUBTITLE = "Make Mochi feel right for you."
internal const val SETTINGS_INTRO_TITLE = "Study your way"
internal const val SETTINGS_LOOK_AND_FEEL = "Look & feel"
internal const val SETTINGS_STUDY_RHYTHM = "Study rhythm"

internal fun MotionPreference.label(): String = when (this) {
    MotionPreference.FULL -> "Full"
    MotionPreference.SYSTEM -> "System"
    MotionPreference.REDUCED -> "Reduced"
}

internal fun MotionPreference.description(): String = when (this) {
    MotionPreference.FULL -> "Play all Mochi animations."
    MotionPreference.SYSTEM -> "Follow your device accessibility setting."
    MotionPreference.REDUCED -> "Use fades and simpler transitions."
}

internal fun shouldShowReminderTime(reminderEnabled: Boolean): Boolean = reminderEnabled

internal fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

internal fun Int.label(): String = if (this == 0) "Unlimited" else toString()
