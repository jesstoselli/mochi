package com.mochi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mochi.audio.AudioPlayer
import com.mochi.data.DeckRepository
import com.mochi.data.DriverFactory
import com.mochi.data.createDatabase
import com.mochi.review.ReviewUiState
import com.mochi.review.ReviewViewModel
import com.mochi.settings.SettingsStore
import com.mochi.settings.SettingsViewModel
import com.mochi.settings.ThemeMode
import com.mochi.ui.screens.CaughtUpScreen
import com.mochi.ui.screens.FlashcardScreen
import com.mochi.ui.screens.SessionCompleteScreen
import com.mochi.ui.screens.SettingsScreen
import com.mochi.ui.theme.MochiTheme

private const val NAV_ANIM_MILLIS = 320

/**
 * Thin entry point: builds dependencies, hosts the view models, applies the chosen
 * theme, and switches between the review flow and the settings screen with a slide.
 */
@Composable
fun App(driverFactory: DriverFactory) {
    val db = remember { createDatabase(driverFactory) }
    val repo = remember { DeckRepository(db) }
    val audioPlayer = remember { AudioPlayer() }
    val settingsStore = remember { SettingsStore(db) }

    val reviewViewModel = viewModel { ReviewViewModel(repo, audioPlayer) }
    val settingsViewModel = viewModel { SettingsViewModel(settingsStore) }

    val reviewState by reviewViewModel.state.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MochiTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnimatedContent(
                targetState = showSettings,
                transitionSpec = {
                    val direction = if (targetState) 1 else -1
                    (
                        slideInHorizontally(tween(NAV_ANIM_MILLIS)) { width -> direction * width } + fadeIn()
                    ) togetherWith (
                        slideOutHorizontally(tween(NAV_ANIM_MILLIS)) { width -> -direction * width } + fadeOut()
                    )
                },
                label = "settingsNav",
            ) { settings ->
                if (settings) {
                    SettingsScreen(
                        themeMode = themeMode,
                        onThemeChange = settingsViewModel::setThemeMode,
                        onBack = { showSettings = false },
                    )
                } else {
                    when (val s = reviewState) {
                        ReviewUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }

                        is ReviewUiState.Reviewing -> FlashcardScreen(
                            card = s.card,
                            position = s.position,
                            total = s.total,
                            onAnswer = reviewViewModel::answer,
                            onPlayAudio = reviewViewModel::playCurrentAudio,
                            onOpenSettings = { showSettings = true },
                        )

                        is ReviewUiState.Complete -> SessionCompleteScreen(
                            stats = s.stats,
                            onContinue = reviewViewModel::startSession,
                            onDone = reviewViewModel::finish,
                        )

                        ReviewUiState.CaughtUp -> CaughtUpScreen(onRefresh = reviewViewModel::startSession)
                    }
                }
            }
        }
    }
}
