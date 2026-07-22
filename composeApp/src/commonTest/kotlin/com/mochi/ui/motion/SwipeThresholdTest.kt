package com.mochi.ui.motion

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwipeThresholdTest {
    @Test
    fun crossingThresholdEmitsOnceUntilRearmed() {
        val first = updateSwipeThreshold(wasOutside = false, progress = 0.36f, enabled = true)
        assertTrue(first.isOutside)
        assertTrue(first.crossedNow)

        val held = updateSwipeThreshold(wasOutside = first.isOutside, progress = 0.8f, enabled = true)
        assertTrue(held.isOutside)
        assertFalse(held.crossedNow)

        val rearmed = updateSwipeThreshold(wasOutside = held.isOutside, progress = 0.1f, enabled = true)
        assertFalse(rearmed.isOutside)
        assertFalse(rearmed.crossedNow)

        val crossedAgain = updateSwipeThreshold(wasOutside = rearmed.isOutside, progress = -0.4f, enabled = true)
        assertTrue(crossedAgain.crossedNow)
    }

    @Test
    fun disabledSwipeNeverCrossesThreshold() {
        val update = updateSwipeThreshold(wasOutside = false, progress = 1f, enabled = false)
        assertFalse(update.isOutside)
        assertFalse(update.crossedNow)
    }
}
