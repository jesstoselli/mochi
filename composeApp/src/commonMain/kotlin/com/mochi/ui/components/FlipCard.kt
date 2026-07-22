package com.mochi.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochi.ui.motion.LocalMotionPolicy
import com.mochi.ui.motion.SHORT_FADE_DURATION_MS
import com.mochi.ui.motion.cardRotation
import com.mochi.ui.motion.pressScale
import com.mochi.ui.theme.LocalJapaneseFont
import kotlin.math.abs

/**
 * Card that flips 180° on the Y axis to reveal the translation, and gives a soft
 * "squish" (scale-down) while pressed so it feels physical. Front: the Japanese
 * word (Japanese font). Back: reading + meaning.
 */
@Composable
fun FlipCard(
    front: String,
    reading: String,
    meaning: String,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val policy = LocalMotionPolicy.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val rotation by animateFloatAsState(
        targetValue = policy.cardRotation(isFlipped),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "flipRotation",
    )
    val squish by animateFloatAsState(
        targetValue = policy.pressScale(pressed = pressed, pressedScale = 0.96f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "squish",
    )
    val density = LocalDensity.current.density

    Card(
        modifier = modifier
            .size(width = 300.dp, height = 380.dp)
            .scale(squish)
            .graphicsLayer {
                if (policy.allowSpatialMotion) {
                    rotationY = rotation
                    cameraDistance = 12f * density
                    val lift = 1f - abs(rotation - 90f) / 90f
                    shadowElevation = 4.dp.toPx() + lift * 16.dp.toPx()
                }
                shape = RoundedCornerShape(32.dp)
                clip = false
            }
            .clickable(interactionSource = interaction, indication = null) { onFlip() },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (policy.reduced) {
                Crossfade(
                    targetState = isFlipped,
                    animationSpec = tween(durationMillis = SHORT_FADE_DURATION_MS),
                    label = "flipFade",
                ) { showBack ->
                    FlipCardFace(front, reading, meaning, showBack)
                }
            } else {
                FlipCardFace(front, reading, meaning, showBack = rotation > 90f)
            }
        }
    }
}

@Composable
private fun FlipCardFace(
    front: String,
    reading: String,
    meaning: String,
    showBack: Boolean,
) {
    val japaneseFont = LocalJapaneseFont.current
    val policy = LocalMotionPolicy.current
    if (!showBack) {
        BasicText(
            text = front,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = 96.sp, stepSize = 2.sp),
            style = TextStyle(
                fontFamily = japaneseFont,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
    } else {
        Column(
            modifier = Modifier
                .then(if (policy.allowSpatialMotion) Modifier.graphicsLayer { rotationY = 180f } else Modifier)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = reading,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 30.sp, stepSize = 1.sp),
                style = TextStyle(
                    fontFamily = japaneseFont,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            BasicText(
                text = meaning,
                maxLines = 3,
                autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 22.sp, stepSize = 1.sp),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
