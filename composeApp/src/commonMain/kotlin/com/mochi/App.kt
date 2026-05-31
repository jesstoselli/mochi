package com.mochi

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.mochi.data.DeckRepository
import com.mochi.data.DriverFactory
import com.mochi.data.createDatabase
import com.mochi.data.seedIfNeeded
import com.mochi.db.Flashcard
import com.mochi.ui.FlashcardScreen

/**
 * Shared entry point. Receives the platform DriverFactory (Android/iOS),
 * creates the database, seeds it on first launch and loads the deck.
 */
@Composable
fun App(driverFactory: DriverFactory) {
    val db = remember { createDatabase(driverFactory) }
    val repo = remember { DeckRepository(db) }

    var deck by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        seedIfNeeded(db)
        deck = repo.all()
        loading = false
    }

    MaterialTheme {
        Surface {
            if (!loading) {
                FlashcardScreen(deck = deck)
            }
        }
    }
}
