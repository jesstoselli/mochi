package com.mochi.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mochi.ui.motion.LocalMotionPolicy
import com.mochi.ui.motion.SHORT_FADE_DURATION_MS
import kotlinx.coroutines.delay

private const val GREET_HOLD_MS = 1800L
private const val REACT_HOLD_MS = 1000L
private const val TUCK_FACTOR = 1.4f // how far below its own height the mascot hides
private const val HOP_PEAK = 1.18f // slight overshoot upward before it drops away
private const val HOP_DURATION_MS = 160
private const val OUT_DURATION_MS = 380

/**
 * The mochi as a Duolingo-style companion. It springs up from the bottom-left corner to greet
 * whenever [greet] changes to a new value (once per study session), holds briefly, then tucks
 * away. On [react] (e.g. a streak milestone) it pops back up for a quick cheer. Purely
 * presentational — driven by trigger values, so callers just pass changing keys.
 */
@Composable
fun MochiMascot(
    greet: Any?,
    react: Any?,
    modifier: Modifier = Modifier,
) {
    val motionPolicy = LocalMotionPolicy.current
    val reveal = remember { Animatable(0f) } // 0 = tucked below the edge, 1 = fully up
    val haptics = LocalHapticFeedback.current
    var lastGreetTrigger by remember { mutableStateOf<Any?>(null) }
    var lastReactTrigger by remember { mutableStateOf<Any?>(null) }

    suspend fun popUp(holdMs: Long, performHaptic: Boolean) {
        if (performHaptic) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (motionPolicy.reduced) {
            reveal.snapTo(reveal.value.coerceIn(0f, 1f))
            reveal.animateTo(1f, animationSpec = tween(durationMillis = SHORT_FADE_DURATION_MS))
        } else {
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        delay(holdMs)
        if (motionPolicy.reduced) {
            reveal.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SHORT_FADE_DURATION_MS),
            )
        } else {
            reveal.animateTo(targetValue = HOP_PEAK, animationSpec = tween(durationMillis = HOP_DURATION_MS))
            reveal.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = OUT_DURATION_MS))
        }
    }

    LaunchedEffect(greet, motionPolicy.reduced) {
        if (greet != null) {
            val performHaptic = greet != lastGreetTrigger
            lastGreetTrigger = greet
            popUp(GREET_HOLD_MS, performHaptic)
        }
    }
    LaunchedEffect(react, motionPolicy.reduced) {
        if (react != null) {
            val performHaptic = react != lastReactTrigger
            lastReactTrigger = react
            popUp(REACT_HOLD_MS, performHaptic)
        }
    }

    Box(modifier.fillMaxSize()) {
        MochiLogo(
            animateEntry = false,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
                .size(88.dp)
                .graphicsLayer {
                    val r = reveal.value
                    translationY = if (motionPolicy.allowSpatialMotion) {
                        (1f - r) * size.height * TUCK_FACTOR
                    } else {
                        0f
                    }
                    alpha = r.coerceIn(0f, 1f)
                },
        )
    }
}
