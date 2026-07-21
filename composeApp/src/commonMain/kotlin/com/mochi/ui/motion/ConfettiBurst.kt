package com.mochi.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One confetto. Pure data; [advance] returns the next frame's state (no side effects). */
data class Particle(
    val position: Offset,
    val velocity: Offset,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val spin: Float = 0f,
    val ageSeconds: Float = 0f,
) {
    /** Semi-implicit Euler step: gravity accelerates velocity, then velocity moves position. */
    fun advance(dtSeconds: Float, gravity: Float): Particle {
        val newVelocity = Offset(velocity.x, velocity.y + gravity * dtSeconds)
        return copy(
            position = Offset(position.x + velocity.x * dtSeconds, position.y + velocity.y * dtSeconds),
            velocity = newVelocity,
            rotation = rotation + spin * dtSeconds,
            ageSeconds = ageSeconds + dtSeconds,
        )
    }
}

private const val PARTICLE_COUNT = 60
private const val GRAVITY = 900f
private const val LIFETIME_SECONDS = 1.4f
private const val MAX_FRAME_DELTA = 0.05f
private const val NANOS_PER_SECOND = 1_000_000_000.0
private const val UPWARD_BIAS = 300f
private const val BASE_SPEED = 500f
private const val SPEED_STEP = 90f
private const val SPEED_BUCKETS = 7
private const val BASE_SIZE = 8f
private const val SIZE_STEP = 3f
private const val SIZE_BUCKETS = 4
private const val BASE_SPIN = 240f
private const val SPIN_STEP = 60f
private const val SPIN_BUCKETS = 5

// Vibrant palette — the ONE place bright colors are used (keeps the Mochi identity elsewhere).
private val ConfettiColors = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF6BCB77),
    Color(0xFF4D96FF), Color(0xFFB983FF), Color(0xFFFF9F45),
)

/**
 * Fires a burst of confetti from the center whenever [trigger] changes to a new non-null value.
 * Pure Canvas, no assets. Runs a frame loop until all particles expire, then draws nothing.
 */
@Composable
fun ConfettiBurst(trigger: Any?, modifier: Modifier = Modifier) {
    var particles by remember { mutableStateOf(emptyList<Particle>()) }

    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        // Seed particles in a radial fan. Angle/speed vary by index (no RNG needed for determinism).
        particles = List(PARTICLE_COUNT) { i ->
            val angle = (i.toFloat() / PARTICLE_COUNT) * 2f * PI.toFloat()
            val speed = BASE_SPEED + (i % SPEED_BUCKETS) * SPEED_STEP
            Particle(
                position = Offset.Zero,
                velocity = Offset(cos(angle) * speed, sin(angle) * speed - UPWARD_BIAS),
                color = ConfettiColors[i % ConfettiColors.size],
                size = BASE_SIZE + (i % SIZE_BUCKETS) * SIZE_STEP,
                rotation = angle,
                spin = BASE_SPIN + (i % SPIN_BUCKETS) * SPIN_STEP,
            )
        }
        var last = withFrameNanos { it }
        while (particles.any { it.ageSeconds < LIFETIME_SECONDS }) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / NANOS_PER_SECOND).toFloat().coerceIn(0f, MAX_FRAME_DELTA)
            last = now
            particles = particles.map { it.advance(dt, GRAVITY) }
        }
        particles = emptyList()
    }

    Canvas(modifier) {
        val origin = Offset(size.width / 2f, size.height / 2f)
        particles.forEach { p ->
            val alpha = (1f - p.ageSeconds / LIFETIME_SECONDS).coerceIn(0f, 1f)
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size,
                center = origin + p.position,
            )
        }
    }
}
