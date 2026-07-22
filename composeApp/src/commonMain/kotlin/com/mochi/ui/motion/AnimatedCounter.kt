package com.mochi.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/**
 * Odometer-style number: when [value] increases the new number slides up from below; when it
 * decreases it slides down. Renders [value] with [prefix]/[suffix] around it (e.g. "🔥 " / " days").
 */
@Composable
fun AnimatedCounter(
    value: Int,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    style: TextStyle = LocalTextStyle.current,
) {
    val policy = LocalMotionPolicy.current
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            if (policy.reduced) {
                fadeIn(tween(durationMillis = 120)) togetherWith fadeOut(tween(durationMillis = 120))
            } else {
                val goingUp = targetState > initialState
                val enter = slideInVertically { h -> if (goingUp) h else -h } + fadeIn()
                val exit = slideOutVertically { h -> if (goingUp) -h else h } + fadeOut()
                (enter togetherWith exit).using(SizeTransform(clip = false))
            }
        },
        modifier = modifier,
        label = "animatedCounter",
    ) { shown ->
        Text(text = "$prefix$shown$suffix", style = style)
    }
}
