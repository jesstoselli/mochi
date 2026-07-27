package com.mochi.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.mochi.ui.motion.AnimatedCounter
import com.mochi.ui.motion.LocalMotionPolicy
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private const val WAVE_STEPS = 24
private const val WAVE_DURATION_MS = 1600
private const val WAVE_AMPLITUDE = 6f

/**
 * A circular "liquid in a jar" gauge: a ring border whose interior fills with an animated sine
 * wave up to [progress], with today's count in the center. When [reached] it shows a check.
 * Presentation-only; reduced motion (via [LocalMotionPolicy]) uses a static fill.
 */
@Composable
fun DailyGoalRing(
    reviewsToday: Int,
    goal: Int,
    progress: Float,
    reached: Boolean,
    modifier: Modifier = Modifier,
) {
    val policy = LocalMotionPolicy.current
    val ring = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = ring.copy(alpha = 0.35f)
    val phase = if (policy.allowInfiniteMotion) rememberWavePhase() else 0f
    // No wave once the goal is reached, so the completed ring reads as fully flooded.
    val amplitude = if (policy.allowInfiniteMotion && !reached) WAVE_AMPLITUDE else 0f

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val strokePx = 8.dp.toPx()
            val radius = (min(size.width, size.height) - strokePx) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val inner = radius - strokePx / 2f
            val circle = Path().apply {
                addOval(
                    Rect(center.x - inner, center.y - inner, center.x + inner, center.y + inner),
                )
            }
            clipPath(circle) {
                val clamped = progress.coerceIn(0f, 1f)
                val top = center.y + inner - 2f * inner * clamped
                val wave = Path().apply {
                    moveTo(center.x - inner, center.y + inner)
                    lineTo(center.x - inner, top)
                    for (i in 0..WAVE_STEPS) {
                        val x = center.x - inner + 2f * inner * i / WAVE_STEPS
                        val y = top + sin(phase + i.toFloat() / WAVE_STEPS * 2f * PI.toFloat()) * amplitude
                        lineTo(x, y)
                    }
                    lineTo(center.x + inner, center.y + inner)
                    close()
                }
                drawPath(wave, fill)
            }
            drawCircle(color = track, radius = radius, center = center, style = Stroke(strokePx))
        }
        if (reached) {
            Text("✓", style = MaterialTheme.typography.headlineMedium, color = ring)
        } else {
            AnimatedCounter(
                value = reviewsToday,
                suffix = " / $goal",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun rememberWavePhase(): Float {
    val transition = rememberInfiniteTransition(label = "goalRing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WAVE_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "goalWavePhase",
    )
    return phase
}
