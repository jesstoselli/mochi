package com.mochi.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Fades a stamp over the card while it is dragged: green "知ってる ✓" to the right,
 * amber "まだ" to the left. [progress] is the horizontal drag in [-1f, 1f].
 */
@Composable
fun SwipeIntentOverlay(progress: Float) {
    if (abs(progress) < 0.02f) return
    val right = progress > 0
    val alignment = if (right) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (right) Color(0xFF2E7D32) else Color(0xFFF9A825)
    val label = if (right) "知ってる ✓" else "まだ"
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = alignment) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.alpha(abs(progress).coerceIn(0f, 1f)),
        ) {
            Text(text = label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
        }
    }
}
