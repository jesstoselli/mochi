package com.mochi.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
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
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Fraction of card width the drag must pass to count as a dismissal. */
private const val DISMISS_THRESHOLD = 0.35f

/** Resistance applied to drags while [enabled] is false (card not yet flipped). */
private const val LOCKED_RESISTANCE = 0.2f

/** Max tilt (degrees) at full drag, and how far off-screen a dismissal throws the card. */
private const val MAX_TILT = 12f
private const val THROW_DISTANCE = 1.6f

/**
 * Drag-to-rate gesture. When [enabled], dragging past [DISMISS_THRESHOLD] of the width throws the
 * card off-screen and calls [onDismiss] (right = true). Below the threshold, or while disabled
 * (drag is damped so it barely moves), the card springs back to center. [onDrag] reports the
 * horizontal progress in [-1f, 1f] so callers can render an intent overlay.
 */
fun Modifier.swipeToDismissCard(
    enabled: Boolean,
    onDismiss: (right: Boolean) -> Unit,
    onDrag: (progress: Float) -> Unit = {},
): Modifier = composed {
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    var widthPx by remember { mutableStateOf(1f) }
    val enabledState by rememberUpdatedState(enabled)
    val onDismissState by rememberUpdatedState(onDismiss)
    val onDragState by rememberUpdatedState(onDrag)

    // Keep the overlay progress in sync with the current offset.
    LaunchedEffect(offset.value, widthPx) {
        onDragState((offset.value.x / widthPx).coerceIn(-1f, 1f))
    }

    this
        .pointerInput(Unit) {
            widthPx = size.width.toFloat().coerceAtLeast(1f)
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    val factor = if (enabledState) 1f else LOCKED_RESISTANCE
                    scope.launch {
                        offset.snapTo(offset.value + Offset(dragAmount.x * factor, dragAmount.y * factor))
                    }
                },
                onDragEnd = {
                    val passed = enabledState && abs(offset.value.x) > widthPx * DISMISS_THRESHOLD
                    if (passed) {
                        val right = offset.value.x > 0
                        scope.launch {
                            offset.animateTo(
                                targetValue = Offset(widthPx * THROW_DISTANCE * if (right) 1f else -1f, offset.value.y),
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                            )
                            onDismissState(right)
                        }
                    } else {
                        scope.launch {
                            offset.animateTo(
                                targetValue = Offset.Zero,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            )
                        }
                    }
                },
            )
        }
        .graphicsLayer {
            translationX = offset.value.x
            translationY = offset.value.y
            rotationZ = (offset.value.x / widthPx).coerceIn(-1f, 1f) * MAX_TILT
        }
}
