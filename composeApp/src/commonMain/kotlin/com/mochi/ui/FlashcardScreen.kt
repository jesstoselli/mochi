package com.mochi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mochi.db.Flashcard

/** Main screen: shows one card at a time and advances with the springy button. */
@Composable
fun FlashcardScreen(deck: List<Flashcard>, modifier: Modifier = Modifier) {
    if (deck.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cards available.")
        }
        return
    }

    var index by remember { mutableStateOf(0) }
    val currentCard = deck[index]

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
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

        NextButton(onClick = { index = (index + 1) % deck.size })
    }
}
