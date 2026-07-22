package com.mochi.ui.motion

internal const val SHORT_FADE_DURATION_MS = 120
internal const val SETTLE_DURATION_MS = 100
private const val FULL_NAVIGATION_DURATION_MS = 300

internal enum class SwipeRelease {
    THROW,
    FADE,
    SPRING,
    SETTLE,
}

internal enum class RewardPresentation {
    PARTICLES,
    STATIC,
}

internal fun MotionPolicy.pressScale(pressed: Boolean, pressedScale: Float): Float =
    if (allowSpatialMotion && pressed) pressedScale else 1f

internal fun MotionPolicy.waveAmplitude(requested: Float): Float =
    if (allowInfiniteMotion) requested else 0f

internal fun MotionPolicy.cardTilt(progress: Float, maxTilt: Float): Float =
    if (allowSpatialMotion) progress.coerceIn(-1f, 1f) * maxTilt else 0f

internal fun MotionPolicy.cardRotation(isFlipped: Boolean): Float =
    if (allowSpatialMotion && isFlipped) 180f else 0f

internal fun MotionPolicy.navigationDurationMillis(): Int =
    if (reduced) SHORT_FADE_DURATION_MS else FULL_NAVIGATION_DURATION_MS

internal fun MotionPolicy.rewardPresentation(): RewardPresentation =
    if (allowDecorativeMotion) RewardPresentation.PARTICLES else RewardPresentation.STATIC

internal fun swipeRelease(passed: Boolean, reduced: Boolean): SwipeRelease = when {
    passed && reduced -> SwipeRelease.FADE
    passed -> SwipeRelease.THROW
    reduced -> SwipeRelease.SETTLE
    else -> SwipeRelease.SPRING
}
