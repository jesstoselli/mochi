package com.mochi.ui.motion

import com.mochi.settings.MotionPreference
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MotionPolicyTest {
    @Test
    fun fullIgnoresTheSystemPreference() {
        assertFalse(resolveMotionPolicy(MotionPreference.FULL, systemReduced = false).reduced)
        assertFalse(resolveMotionPolicy(MotionPreference.FULL, systemReduced = true).reduced)
    }

    @Test
    fun reducedIgnoresTheSystemPreference() {
        assertTrue(resolveMotionPolicy(MotionPreference.REDUCED, systemReduced = false).reduced)
        assertTrue(resolveMotionPolicy(MotionPreference.REDUCED, systemReduced = true).reduced)
    }

    @Test
    fun systemTracksTheNativePreference() {
        assertFalse(resolveMotionPolicy(MotionPreference.SYSTEM, systemReduced = false).reduced)
        assertTrue(resolveMotionPolicy(MotionPreference.SYSTEM, systemReduced = true).reduced)
    }

    @Test
    fun reducedPolicyDisablesSpatialDecorativeAndInfiniteMotion() {
        val policy = MotionPolicy.Reduced

        assertFalse(policy.allowSpatialMotion)
        assertFalse(policy.allowDecorativeMotion)
        assertFalse(policy.allowInfiniteMotion)
        assertTrue(policy.allowShortFades)
    }
}
