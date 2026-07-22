package com.mochi.ui.motion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.sin

private const val WAVE_DURATION_MS = 1600
private const val WAVE_STEPS = 24

/**
 * A liquid fill: an animated sine wave rising to [progress] (0f..1f) of the height.
 * Draw it inside a clipped/rounded container to get a "liquid in a jar" look.
 */
@Composable
fun LiquidProgress(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    waveHeight: Float = 8f,
) {
    val policy = LocalMotionPolicy.current
    val phase = if (policy.allowInfiniteMotion) animatedLiquidPhase() else 0f
    val amplitude = policy.waveAmplitude(waveHeight)

    Canvas(modifier) {
        val fill = progress.coerceIn(0f, 1f)
        val baseY = size.height * (1f - fill)
        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(0f, baseY)
            for (i in 0..WAVE_STEPS) {
                val x = size.width * i / WAVE_STEPS
                val y = baseY + sin(phase + i.toFloat() / WAVE_STEPS * 2f * PI.toFloat()) * amplitude
                lineTo(x, y)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
private fun animatedLiquidPhase(): Float {
    val transition = rememberInfiniteTransition(label = "liquid")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WAVE_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )
    return phase
}
