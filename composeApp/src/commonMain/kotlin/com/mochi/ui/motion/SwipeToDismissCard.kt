package com.mochi.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Fraction of card width the drag must pass to count as a dismissal. */
private const val DISMISS_THRESHOLD = 0.35f

internal data class SwipeThresholdUpdate(
    val isOutside: Boolean,
    val crossedNow: Boolean,
)

internal fun updateSwipeThreshold(
    wasOutside: Boolean,
    progress: Float,
    enabled: Boolean,
): SwipeThresholdUpdate {
    val isOutside = enabled && abs(progress) > DISMISS_THRESHOLD
    return SwipeThresholdUpdate(
        isOutside = isOutside,
        crossedNow = isOutside && !wasOutside,
    )
}

/** Resistance applied to drags while [enabled] is false (card not yet flipped). */
private const val LOCKED_RESISTANCE = 0.2f

/** Max tilt (degrees) at full drag, and how far off-screen a dismissal throws the card. */
private const val MAX_TILT = 12f
private const val THROW_DISTANCE = 1.6f

/**
 * Drag-to-rate gesture. When [enabled], dragging past [DISMISS_THRESHOLD] of the width throws the
 * card off-screen and calls [onDismiss] (right = true). Below the threshold, or while disabled
 * (drag is damped so it barely moves), the card springs back to center. [onDrag] reports the
 * horizontal progress in [-1f, 1f]. [onThresholdCrossed] fires once when the drag becomes eligible
 * for dismissal and rearms after the card returns below the threshold.
 * Gesture callbacks and their exactly-once dismissal state intentionally share this lifecycle.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
fun Modifier.swipeToDismissCard(
    enabled: Boolean,
    onDismiss: (right: Boolean) -> Unit,
    onThresholdCrossed: () -> Unit = {},
    onDrag: (progress: Float) -> Unit = {},
): Modifier = composed {
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val alpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val policy = LocalMotionPolicy.current
    var widthPx by remember { mutableStateOf(1f) }
    var thresholdOutside by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    var releaseJob by remember { mutableStateOf<Job?>(null) }
    var pendingDismissRight by remember { mutableStateOf<Boolean?>(null) }
    var dismissalDelivered by remember { mutableStateOf(false) }
    val enabledState by rememberUpdatedState(enabled)
    val policyState by rememberUpdatedState(policy)
    val onDismissState by rememberUpdatedState(onDismiss)
    val onDragState by rememberUpdatedState(onDrag)
    val onThresholdCrossedState by rememberUpdatedState(onThresholdCrossed)

    fun deliverDismiss(right: Boolean) {
        if (!dismissalDelivered) {
            dismissalDelivered = true
            onDismissState(right)
        }
    }

    // Keep the overlay progress in sync with the current offset.
    LaunchedEffect(offset.value, widthPx) {
        onDragState((offset.value.x / widthPx).coerceIn(-1f, 1f))
    }

    LaunchedEffect(policy.reduced) {
        if (policy.reduced && releaseJob?.isActive == true) {
            releaseJob?.cancel()
            val direction = pendingDismissRight
            if (direction != null) {
                alpha.animateTo(0f, animationSpec = tween(durationMillis = SHORT_FADE_DURATION_MS))
                deliverDismiss(direction)
            } else {
                offset.animateTo(Offset.Zero, animationSpec = tween(durationMillis = SETTLE_DURATION_MS))
                thresholdOutside = false
            }
        }
    }

    this
        .pointerInput(Unit) {
            widthPx = size.width.toFloat().coerceAtLeast(1f)
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    if (!isDismissing) {
                        val factor = if (enabledState) 1f else LOCKED_RESISTANCE
                        val delta = Offset(dragAmount.x * factor, dragAmount.y * factor)
                        val threshold = updateSwipeThreshold(
                            wasOutside = thresholdOutside,
                            progress = (offset.value.x + delta.x) / widthPx,
                            enabled = enabledState,
                        )
                        thresholdOutside = threshold.isOutside
                        if (threshold.crossedNow) onThresholdCrossedState()
                        scope.launch {
                            offset.snapTo(offset.value + delta)
                        }
                    }
                },
                onDragEnd = {
                    if (!isDismissing) {
                        val passed = enabledState && abs(offset.value.x) > widthPx * DISMISS_THRESHOLD
                        val right = offset.value.x > 0
                        when (swipeRelease(passed = passed, reduced = policyState.reduced)) {
                            SwipeRelease.THROW -> {
                                isDismissing = true
                                pendingDismissRight = right
                                dismissalDelivered = false
                                releaseJob = scope.launch {
                                    offset.animateTo(
                                        targetValue = Offset(
                                            widthPx * THROW_DISTANCE * if (right) 1f else -1f,
                                            offset.value.y,
                                        ),
                                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    )
                                    deliverDismiss(right)
                                }
                            }
                            SwipeRelease.FADE -> {
                                isDismissing = true
                                pendingDismissRight = right
                                dismissalDelivered = false
                                releaseJob = scope.launch {
                                    alpha.animateTo(
                                        0f,
                                        animationSpec = tween(durationMillis = SHORT_FADE_DURATION_MS),
                                    )
                                    deliverDismiss(right)
                                }
                            }
                            SwipeRelease.SPRING -> releaseJob = scope.launch {
                                pendingDismissRight = null
                                offset.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                                thresholdOutside = false
                            }
                            SwipeRelease.SETTLE -> releaseJob = scope.launch {
                                pendingDismissRight = null
                                offset.animateTo(
                                    Offset.Zero,
                                    animationSpec = tween(durationMillis = SETTLE_DURATION_MS),
                                )
                                thresholdOutside = false
                            }
                        }
                    }
                },
                onDragCancel = {
                    if (!isDismissing) {
                        pendingDismissRight = null
                        releaseJob = scope.launch {
                            if (policyState.reduced) {
                                offset.animateTo(
                                    Offset.Zero,
                                    animationSpec = tween(durationMillis = SETTLE_DURATION_MS),
                                )
                            } else {
                                offset.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                            }
                            thresholdOutside = false
                        }
                    }
                },
            )
        }
        .graphicsLayer {
            translationX = offset.value.x
            translationY = offset.value.y
            rotationZ = policy.cardTilt(progress = offset.value.x / widthPx, maxTilt = MAX_TILT)
            this.alpha = alpha.value
        }
}
