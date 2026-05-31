package com.mochi

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mochi.audio.AudioPlayer
import com.mochi.data.DeckRepository
import com.mochi.data.DriverFactory
import com.mochi.data.createDatabase
import com.mochi.data.seedIfNeeded
import com.mochi.db.Flashcard
import com.mochi.resources.Res
import com.mochi.ui.FlashcardScreen
import com.mochi.util.nowMillis
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Shared entry point. Receives the platform DriverFactory (Android/iOS),
 * creates the database, seeds it on first launch and loads the deck.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun App(driverFactory: DriverFactory) {
    val db = remember { createDatabase(driverFactory) }
    val repo = remember { DeckRepository(db) }
    val audioPlayer = remember { AudioPlayer() }
    val scope = rememberCoroutineScope()

    var deck by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        seedIfNeeded(db)
        deck = repo.all()
        loading = false
    }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    MaterialTheme {
        Surface {
            if (!loading) {
                FlashcardScreen(
                    deck = deck,
                    onAnswer = { card, correct -> repo.recordAnswer(card, correct, nowMillis()) },
                    onPlayAudio = { name ->
                        scope.launch {
                            runCatching { audioPlayer.play(Res.readBytes("files/audio/$name")) }
                        }
                    },
                )
            }
        }
    }
}
