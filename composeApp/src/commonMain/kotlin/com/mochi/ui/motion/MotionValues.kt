package com.mochi.ui.motion

internal enum class SwipeRelease {
    THROW,
    FADE,
    SPRING,
    SETTLE,
}

internal fun MotionPolicy.pressScale(pressed: Boolean, pressedScale: Float): Float =
    if (allowSpatialMotion && pressed) pressedScale else 1f

internal fun MotionPolicy.waveAmplitude(requested: Float): Float =
    if (allowInfiniteMotion) requested else 0f

internal fun MotionPolicy.cardTilt(progress: Float, maxTilt: Float): Float =
    if (allowSpatialMotion) progress.coerceIn(-1f, 1f) * maxTilt else 0f

internal fun swipeRelease(passed: Boolean, reduced: Boolean): SwipeRelease = when {
    passed && reduced -> SwipeRelease.FADE
    passed -> SwipeRelease.THROW
    reduced -> SwipeRelease.SETTLE
    else -> SwipeRelease.SPRING
}
