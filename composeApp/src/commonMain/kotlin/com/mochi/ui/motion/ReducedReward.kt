package com.mochi.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

private val RewardColors = listOf(
    Color(0xFFFFD93D),
    Color(0xFF6BCB77),
    Color(0xFF4D96FF),
    Color(0xFFFF9F45),
)

/** Fixed celebratory marks that only fade, with no moving or expanding geometry. */
@Composable
internal fun ReducedReward(trigger: Any?, modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        alpha.snapTo(0f)
        alpha.animateTo(1f, tween(durationMillis = 120))
        delay(700)
        alpha.animateTo(0f, tween(durationMillis = 120))
    }

    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.18f
        repeat(12) { index ->
            val angle = index * (kotlin.math.PI.toFloat() * 2f / 12f)
            val position = center + Offset(kotlin.math.cos(angle), kotlin.math.sin(angle)) * radius
            drawCircle(
                color = RewardColors[index % RewardColors.size].copy(alpha = alpha.value),
                radius = if (index % 2 == 0) 7f else 4f,
                center = position,
            )
        }
    }
}
