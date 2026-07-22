package com.mochi.ui.motion

import androidx.compose.ui.hapticfeedback.HapticFeedbackType

internal class CardHaptics(private val perform: (HapticFeedbackType) -> Unit) {
    fun onFlip() = perform(HapticFeedbackType.VirtualKey)

    fun onSwipeThreshold() = perform(HapticFeedbackType.GestureThresholdActivate)
}
