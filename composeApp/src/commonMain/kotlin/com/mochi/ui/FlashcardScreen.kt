package com.mochi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.mochi.db.Flashcard
import kotlinx.coroutines.delay

// How long the celebration overlay stays up. The Lottie itself is ~5s; we show a
// snappy slice of it so the flow between cards stays quick. Tune to taste.
private const val CELEBRATION_MILLIS = 2000L

/**
 * Main screen: shows one card at a time, lets the user self-rate, and celebrates
 * correct answers with a Lottie burst. Stays presentation-only — answers are
 * reported via [onAnswer] so the data layer is wired in [com.mochi.App].
 */
@Composable
fun FlashcardScreen(
    deck: List<Flashcard>,
    onAnswer: (Flashcard, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (deck.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cards available.")
        }
        return
    }

    var index by remember { mutableStateOf(0) }
    var celebrationTick by remember { mutableStateOf(0) }
    var celebrating by remember { mutableStateOf(false) }
    val currentCard = deck[index]

    // Restart the celebration whenever a correct answer bumps the tick.
    LaunchedEffect(celebrationTick) {
        if (celebrationTick > 0) {
            celebrating = true
            delay(CELEBRATION_MILLIS)
            celebrating = false
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("${index + 1} / ${deck.size}", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(16.dp))

            // key(index) recreates the FlipCard when the card changes, resetting it to the front.
            key(index) {
                FlipCard(
                    front = currentCard.front,
                    reading = currentCard.reading,
                    meaning = currentCard.back,
                )
            }

            Spacer(Modifier.height(32.dp))

            AnswerButtons(
                onAnswer = { correct ->
                    onAnswer(currentCard, correct)
                    if (correct) celebrationTick++
                    index = (index + 1) % deck.size
                },
            )
        }

        // Celebration overlay — re-enters composition each time, so the Lottie replays.
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
