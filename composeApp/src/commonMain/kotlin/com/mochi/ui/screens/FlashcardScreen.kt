package com.mochi.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.mochi.ui.motion.AnimatedCounter
import com.mochi.ui.motion.ConfettiBurst
import com.mochi.ui.motion.swipeToDismissCard

/**
 * Presentation-only review screen: renders the current [card] and progress, auto-plays its
 * pronunciation, and reports ratings via [onAnswer]. The success celebration is shown once
 * at the end of the session (on the summary screen), not per card.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FlashcardScreen(
    card: Flashcard,
    position: Int,
    total: Int,
    streakMilestone: Int?,
    sessionStreak: Int,
    onAnswer: (Boolean) -> Unit,
    onPlayAudio: () -> Unit,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
    sharedKey: String,
    modifier: Modifier = Modifier,
) {
    // Auto-play the pronunciation each time a new card appears.
    LaunchedEffect(card.id) {
        onPlayAudio()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                with(sharedScope) {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = sharedKey),
                        animatedVisibilityScope = animatedScope,
                    )
                },
            ),
    ) {
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
                if (sessionStreak > 0) {
                    Spacer(Modifier.width(12.dp))
                    AnimatedCounter(
                        value = sessionStreak,
                        prefix = "🔥 ",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            // Card group centered in the remaining space.
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CategoryPill(card.category)
                Spacer(Modifier.height(20.dp))

                // Slide the next card in from the right; resets the flip per card.
                AnimatedContent(
                    targetState = card,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> -width } + fadeOut())
                    },
                    label = "card",
                ) { current ->
                    var isFlipped by remember(current.id) { mutableStateOf(false) }
                    FlipCard(
                        front = current.front,
                        reading = current.reading,
                        meaning = current.back,
                        isFlipped = isFlipped,
                        onFlip = { isFlipped = !isFlipped },
                        modifier = Modifier.swipeToDismissCard(
                            enabled = isFlipped,
                            onDismiss = { right -> onAnswer(right) },
                        ),
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
                AnswerButtons(onAnswer = onAnswer)
            }
        }
        ConfettiBurst(trigger = streakMilestone, modifier = Modifier.fillMaxSize())
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
