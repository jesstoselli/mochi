package com.mochi.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mochi.db.Flashcard
import com.mochi.ui.components.AnswerButtons
import com.mochi.ui.components.BouncyButton
import com.mochi.ui.components.FlipCard
import com.mochi.ui.components.SuccessAnimation
import kotlinx.coroutines.delay

// How long the celebration overlay stays up. The Canvas animation takes ~1s; the
// extra time holds the finished checkmark briefly before fading out. Tune to taste.
private const val CELEBRATION_MILLIS = 1400L

/**
 * Presentation-only review screen: renders the current [card] and progress, plays its
 * pronunciation, and reports ratings via [onAnswer]. Celebrates correct answers locally.
 */
@Composable
fun FlashcardScreen(
    card: Flashcard,
    position: Int,
    total: Int,
    onAnswer: (Boolean) -> Unit,
    onPlayAudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var celebrationTick by remember { mutableStateOf(0) }
    var celebrating by remember { mutableStateOf(false) }

    LaunchedEffect(celebrationTick) {
        if (celebrationTick > 0) {
            celebrating = true
            delay(CELEBRATION_MILLIS)
            celebrating = false
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Progress pinned to the top (just below the status bar).
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { position.toFloat() / total },
                    modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(99.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "$position/$total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Card group centered in the remaining space.
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CategoryPill(card.category)
                Spacer(Modifier.height(20.dp))

                // key(card.id) recreates the FlipCard per card, resetting it to the front.
                key(card.id) {
                    FlipCard(
                        front = card.front,
                        reading = card.reading,
                        meaning = card.back,
                    )
                }

                val audio = card.audio
                if (!audio.isNullOrBlank()) {
                    Spacer(Modifier.height(20.dp))
                    BouncyButton(
                        onClick = onPlayAudio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text("🔊  Listen")
                    }
                }

                Spacer(Modifier.height(28.dp))
                AnswerButtons(
                    onAnswer = { correct ->
                        if (correct) celebrationTick++
                        onAnswer(correct)
                    },
                )
            }
        }

        // Celebration overlay — re-enters composition each time, so the animation replays.
        AnimatedVisibility(
            visible = celebrating,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SuccessAnimation(modifier = Modifier.size(220.dp))
            }
        }
    }
}

/** Small rounded tag showing the card's category, in the sakura accent. */
@Composable
private fun CategoryPill(category: String) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}
