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

// Small pause after the last card so its celebration is visible before the summary.
private const val SESSION_END_DELAY = 700L

/**
 * Runs a single finite review session over [deck]. Progress sits at the top, the card
 * is centered, and the rating buttons are anchored at the bottom. Celebrates correct
 * answers. When the last card is answered it calls [onSessionComplete]. Presentation-only.
 */
@Composable
fun FlashcardScreen(
    deck: List<Flashcard>,
    onAnswer: (Flashcard, Boolean) -> Unit,
    onPlayAudio: (String) -> Unit,
    onSessionComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keyed on `deck` so a new session resets the position and flags.
    var index by remember(deck) { mutableStateOf(0) }
    var celebrationTick by remember(deck) { mutableStateOf(0) }
    var celebrating by remember(deck) { mutableStateOf(false) }
    var finishing by remember(deck) { mutableStateOf(false) }

    if (deck.isEmpty()) return
    val currentCard = deck[index]

    LaunchedEffect(celebrationTick) {
        if (celebrationTick > 0) {
            celebrating = true
            delay(CELEBRATION_MILLIS)
            celebrating = false
        }
    }
    LaunchedEffect(finishing) {
        if (finishing) {
            delay(SESSION_END_DELAY)
            onSessionComplete()
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { (index + 1).toFloat() / deck.size },
                    modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(99.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${index + 1}/${deck.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CategoryPill(currentCard.category)
                Spacer(Modifier.height(20.dp))

                // key(index) recreates the FlipCard when the card changes, resetting it to the front.
                key(index) {
                    FlipCard(
                        front = currentCard.front,
                        reading = currentCard.reading,
                        meaning = currentCard.back,
                    )
                }

                val audio = currentCard.audio
                if (!audio.isNullOrBlank()) {
                    Spacer(Modifier.height(20.dp))
                    BouncyButton(
                        onClick = { onPlayAudio(audio) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text("🔊  Listen")
                    }
                }
            }

            AnswerButtons(
                onAnswer = { correct ->
                    onAnswer(currentCard, correct)
                    if (correct) celebrationTick++
                    if (index < deck.lastIndex) index++ else finishing = true
                },
            )
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
