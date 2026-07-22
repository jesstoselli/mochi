package com.mochi.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/** Squishes the element while [interactionSource] reports a press, springing back on release. */
@Composable
fun Modifier.pressBounce(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.9f,
): Modifier {
    val policy = LocalMotionPolicy.current
    if (!policy.allowSpatialMotion) return this

    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = policy.pressScale(pressed, pressedScale),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "pressBounce",
    )
    return this.scale(scale)
}
