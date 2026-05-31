package com.mochi.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Card that rotates 180° on the Y axis when tapped, revealing the translation.
 * Front: the Japanese word. Back: reading + meaning.
 */
@Composable
fun FlipCard(
    front: String,
    reading: String,
    meaning: String,
    modifier: Modifier = Modifier,
) {
    var isFlipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "flipRotation",
    )
    val density = LocalDensity.current.density

    Card(
        modifier = modifier
            .size(width = 280.dp, height = 360.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { isFlipped = !isFlipped },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (rotation <= 90f) {
                // FRONT
                Text(front, fontSize = 84.sp, fontWeight = FontWeight.Bold)
            } else {
                // BACK — counter-rotated so the text isn't mirrored
                Column(
                    modifier = Modifier
                        .graphicsLayer { rotationY = 180f }
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(reading, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Text(meaning, fontSize = 22.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
