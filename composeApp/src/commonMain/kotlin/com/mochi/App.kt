package com.mochi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mochi.audio.AudioPlayer
import com.mochi.data.DeckRepository
import com.mochi.data.DriverFactory
import com.mochi.data.createDatabase
import com.mochi.review.ReviewUiState
import com.mochi.review.ReviewViewModel
import com.mochi.ui.screens.CaughtUpScreen
import com.mochi.ui.screens.FlashcardScreen
import com.mochi.ui.screens.SessionCompleteScreen
import com.mochi.ui.theme.MochiTheme

/**
 * Thin entry point: builds the dependencies, hosts the [ReviewViewModel], and routes
 * the observed [ReviewUiState] to the right screen.
 */
@Composable
fun App(driverFactory: DriverFactory) {
    val db = remember { createDatabase(driverFactory) }
    val repo = remember { DeckRepository(db) }
    val audioPlayer = remember { AudioPlayer() }
    val viewModel = viewModel { ReviewViewModel(repo, audioPlayer) }
    val state by viewModel.state.collectAsState()

    MochiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            when (val s = state) {
                ReviewUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                is ReviewUiState.Reviewing -> FlashcardScreen(
                    card = s.card,
                    position = s.position,
                    total = s.total,
                    onAnswer = viewModel::answer,
                    onPlayAudio = viewModel::playCurrentAudio,
                )

                is ReviewUiState.Complete -> SessionCompleteScreen(
                    stats = s.stats,
                    onContinue = viewModel::startSession,
                    onDone = viewModel::finish,
                )

                ReviewUiState.CaughtUp -> CaughtUpScreen(onRefresh = viewModel::startSession)
            }
        }
    }
}
