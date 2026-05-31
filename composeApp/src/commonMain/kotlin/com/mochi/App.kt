package com.mochi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mochi.audio.AudioPlayer
import com.mochi.data.DeckRepository
import com.mochi.data.DriverFactory
import com.mochi.data.createDatabase
import com.mochi.data.seedIfNeeded
import com.mochi.db.Flashcard
import com.mochi.resources.Res
import com.mochi.ui.screens.CaughtUpScreen
import com.mochi.ui.screens.FlashcardScreen
import com.mochi.ui.screens.SessionCompleteScreen
import com.mochi.ui.SessionStats
import com.mochi.ui.theme.MochiTheme
import com.mochi.util.nowMillis
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Cards per review session — keeps the first run sane (otherwise all 1500 new cards are "due").
private const val SESSION_SIZE = 20

private enum class Phase { LOADING, REVIEWING, COMPLETE, CAUGHT_UP }

/**
 * Shared entry point. Creates the database, seeds it on first launch, and drives the
 * spaced-repetition loop: load due cards -> review a capped session -> show a summary ->
 * continue or show the "all caught up" screen.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun App(driverFactory: DriverFactory) {
    val db = remember { createDatabase(driverFactory) }
    val repo = remember { DeckRepository(db) }
    val audioPlayer = remember { AudioPlayer() }
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(Phase.LOADING) }
    var session by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var reviewed by remember { mutableStateOf(0) }
    var correct by remember { mutableStateOf(0) }

    fun startSession() {
        val due = repo.dueForReview(nowMillis())
        if (due.isEmpty()) {
            phase = Phase.CAUGHT_UP
        } else {
            session = due.take(SESSION_SIZE)
            reviewed = 0
            correct = 0
            phase = Phase.REVIEWING
        }
    }

    LaunchedEffect(Unit) {
        seedIfNeeded(db)
        startSession()
    }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    MochiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            when (phase) {
                Phase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                Phase.REVIEWING -> FlashcardScreen(
                    deck = session,
                    onAnswer = { card, isCorrect ->
                        repo.recordAnswer(card, isCorrect, nowMillis())
                        reviewed++
                        if (isCorrect) correct++
                    },
                    onPlayAudio = { name ->
                        scope.launch {
                            runCatching { audioPlayer.play(Res.readBytes("files/audio/$name")) }
                        }
                    },
                    onSessionComplete = { phase = Phase.COMPLETE },
                )

                Phase.COMPLETE -> SessionCompleteScreen(
                    stats = SessionStats(reviewed = reviewed, correct = correct),
                    onContinue = { startSession() },
                    onDone = { phase = Phase.CAUGHT_UP },
                )

                Phase.CAUGHT_UP -> CaughtUpScreen(onRefresh = { startSession() })
            }
        }
    }
}
