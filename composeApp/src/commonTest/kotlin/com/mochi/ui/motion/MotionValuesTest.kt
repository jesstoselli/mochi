package com.mochi.ui.motion

import kotlin.test.Test
import kotlin.test.assertEquals

class MotionValuesTest {
    @Test
    fun reducedPressNeverScales() {
        assertEquals(1f, MotionPolicy.Reduced.pressScale(pressed = true, pressedScale = 0.9f))
        assertEquals(0.9f, MotionPolicy.Full.pressScale(pressed = true, pressedScale = 0.9f))
    }

    @Test
    fun reducedLiquidProgressHasNoWaveAmplitude() {
        assertEquals(0f, MotionPolicy.Reduced.waveAmplitude(8f))
        assertEquals(8f, MotionPolicy.Full.waveAmplitude(8f))
    }

    @Test
    fun reducedCardsNeverTilt() {
        assertEquals(0f, MotionPolicy.Reduced.cardTilt(progress = 0.5f, maxTilt = 12f))
        assertEquals(6f, MotionPolicy.Full.cardTilt(progress = 0.5f, maxTilt = 12f))
    }

    @Test
    fun reducedFlipNeverRotatesInThreeDimensions() {
        assertEquals(0f, MotionPolicy.Reduced.cardRotation(isFlipped = true))
        assertEquals(180f, MotionPolicy.Full.cardRotation(isFlipped = true))
        assertEquals(0f, MotionPolicy.Full.cardRotation(isFlipped = false))
    }

    @Test
    fun reducedNavigationUsesShortFadeDuration() {
        assertEquals(120, MotionPolicy.Reduced.navigationDurationMillis())
        assertEquals(300, MotionPolicy.Full.navigationDurationMillis())
    }

    @Test
    fun reducedRewardsUseStaticPresentation() {
        assertEquals(RewardPresentation.STATIC, MotionPolicy.Reduced.rewardPresentation())
        assertEquals(RewardPresentation.PARTICLES, MotionPolicy.Full.rewardPresentation())
    }

    @Test
    fun completedMascotEventsDoNotReplay() {
        assertEquals(
            null,
            nextMascotEvent(greet = "session", completedGreet = "session", react = 10, completedReact = 10),
        )
    }

    @Test
    fun interruptedMascotEventRemainsPending() {
        assertEquals(
            MascotEvent.GREET,
            nextMascotEvent(greet = "session", completedGreet = null, react = null, completedReact = null),
        )
    }

    @Test
    fun newMascotReactionTakesPriorityOverGreeting() {
        assertEquals(
            MascotEvent.REACT,
            nextMascotEvent(greet = "session", completedGreet = null, react = 10, completedReact = null),
        )
    }

    @Test
    fun swipeReleaseChoosesReducedAndFullPresentations() {
        assertEquals(SwipeRelease.FADE, swipeRelease(passed = true, reduced = true))
        assertEquals(SwipeRelease.THROW, swipeRelease(passed = true, reduced = false))
        assertEquals(SwipeRelease.SETTLE, swipeRelease(passed = false, reduced = true))
        assertEquals(SwipeRelease.SPRING, swipeRelease(passed = false, reduced = false))
    }
}
