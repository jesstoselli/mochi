package com.mochi.ui.motion

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.test.Test
import kotlin.test.assertEquals

class CardHapticsTest {
    @Test
    fun flipUsesVirtualKeyFeedback() {
        val emitted = mutableListOf<HapticFeedbackType>()
        val haptics = CardHaptics { type: HapticFeedbackType -> emitted += type }

        haptics.onFlip()

        assertEquals(listOf(HapticFeedbackType.VirtualKey), emitted)
    }

    @Test
    fun swipeThresholdUsesGestureActivationFeedback() {
        val emitted = mutableListOf<HapticFeedbackType>()
        val haptics = CardHaptics { type: HapticFeedbackType -> emitted += type }

        haptics.onSwipeThreshold()

        assertEquals(listOf(HapticFeedbackType.GestureThresholdActivate), emitted)
    }
}
