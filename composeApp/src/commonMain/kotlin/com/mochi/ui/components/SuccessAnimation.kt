package com.mochi.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CircleGreen = Color(0xFF43C63C)
private val CheckColor = Color(0xFFE9E9E9)

// Geometry is authored in the 512x512 space of the reference design, then scaled to fit.
private const val DESIGN = 512f
private const val INNER_RADIUS = 134.5f
private const val OUTER_RADIUS = 247.5f
private const val CHECK_STROKE = 2f * 14.97f

// Checkmark vertices in design space (top-right -> bottom -> top-left).
private val CheckP0 = Offset(315.9f, 218.1f)
private val CheckP1 = Offset(241.0f, 292.9f)
private val CheckP2 = Offset(196.1f, 248.0f)

/**
 * Success burst drawn entirely with Compose Canvas (no external renderer):
 * a green circle pops in with a spring bounce, a halo ring expands and fades,
 * and a checkmark is stroked on. Replays every time it enters composition.
 */
@Composable
fun SuccessAnimation(modifier: Modifier = Modifier) {
    val circleScale = remember { Animatable(0f) }
    val ringProgress = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            circleScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        launch {
            ringProgress.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        }
        launch {
            delay(timeMillis = 180)
            checkProgress.animateTo(1f, tween(durationMillis = 360, easing = FastOutSlowInEasing))
        }
    }

    Canvas(modifier) {
        val unit = size.minDimension / DESIGN
        val center = Offset(size.width / 2f, size.height / 2f)
        fun design(p: Offset) = Offset(
            x = center.x + (p.x - DESIGN / 2f) * unit,
            y = center.y + (p.y - DESIGN / 2f) * unit,
        )

        // Expanding, fading halo behind the circle.
        if (ringProgress.value < 1f) {
            val ringRadius = (INNER_RADIUS + (OUTER_RADIUS - INNER_RADIUS) * ringProgress.value) * unit
            drawCircle(
                color = CircleGreen.copy(alpha = (1f - ringProgress.value) * 0.5f),
                radius = ringRadius,
                center = center,
            )
        }

        // The green disc popping in.
        drawCircle(color = CircleGreen, radius = INNER_RADIUS * unit * circleScale.value, center = center)

        // Checkmark stroked on progressively via PathMeasure.
        if (checkProgress.value > 0f) {
            val path = Path().apply {
                moveTo(design(CheckP0).x, design(CheckP0).y)
                lineTo(design(CheckP1).x, design(CheckP1).y)
                lineTo(design(CheckP2).x, design(CheckP2).y)
            }
            val measure = PathMeasure().apply { setPath(path, forceClosed = false) }
            val drawn = Path()
            measure.getSegment(0f, measure.length * checkProgress.value, drawn, startWithMoveTo = true)
            drawPath(
                path = drawn,
                color = CheckColor,
                style = Stroke(width = CHECK_STROKE * unit, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
