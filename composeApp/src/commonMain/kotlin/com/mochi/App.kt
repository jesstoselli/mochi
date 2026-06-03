package com.mochi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mochi.audio.AudioPlayer
import com.mochi.data.DeckRepository
import com.mochi.data.DriverFactory
import com.mochi.data.createDatabase
import com.mochi.learning.LearningStore
import com.mochi.learning.LearningViewModel
import com.mochi.review.ReviewUiState
import com.mochi.review.ReviewViewModel
import com.mochi.settings.SettingsStore
import com.mochi.settings.SettingsViewModel
import com.mochi.settings.ThemeMode
import com.mochi.stats.StatsStore
import com.mochi.stats.StatsViewModel
import com.mochi.ui.screens.FlashcardScreen
import com.mochi.ui.screens.HomeScreen
import com.mochi.ui.screens.LearningScreen
import com.mochi.ui.screens.SessionCompleteScreen
import com.mochi.ui.screens.SettingsScreen
import com.mochi.ui.screens.StatsScreen
import com.mochi.ui.theme.MochiTheme
import com.mochi.ui.theme.SystemBarsEffect

private enum class Tab(val label: String, val icon: ImageVector) {
    REVIEW("Review", Icons.Filled.School),
    LEARNING("Learning", Icons.Filled.Style),
    STATS("Stats", Icons.Filled.BarChart),
    SETTINGS("Settings", Icons.Filled.Settings),
}

/**
 * Thin entry point: builds dependencies + view models, applies the chosen theme, and
 * hosts a bottom-navigation Scaffold switching between Review, Stats and Settings.
 */
@Composable
fun App(driverFactory: DriverFactory) {
    val db = remember { createDatabase(driverFactory) }
    val repo = remember { DeckRepository(db) }
    val statsStore = remember { StatsStore(db) }
    val settingsStore = remember { SettingsStore(db) }
    val audioPlayer = remember { AudioPlayer() }

    val learningStore = remember { LearningStore(db) }

    val reviewViewModel = viewModel { ReviewViewModel(repo, statsStore, settingsStore, audioPlayer) }
    val settingsViewModel = viewModel { SettingsViewModel(settingsStore) }
    val statsViewModel = viewModel { StatsViewModel(statsStore) }
    val learningViewModel = viewModel { LearningViewModel(learningStore, audioPlayer) }

    val reviewState by reviewViewModel.state.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val newCardLimit by settingsViewModel.newCardLimit.collectAsState()
    val stats by statsViewModel.stats.collectAsState()
    val learningWords by learningViewModel.words.collectAsState()
    var tab by remember { mutableStateOf(Tab.REVIEW) }

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // Refresh stats when opening Stats; rebuild the session if the limit changed when
    // returning to Review.
    LaunchedEffect(tab) {
        when (tab) {
            Tab.STATS -> statsViewModel.refresh()
            Tab.REVIEW -> reviewViewModel.onEnterReviewTab()
            Tab.LEARNING -> Unit // the list is a reactive Flow; nothing to refresh
            Tab.SETTINGS -> Unit
        }
    }

    MochiTheme(darkTheme = darkTheme) {
        SystemBarsEffect(darkTheme)
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = tab == destination,
                            onClick = { tab = destination },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                color = MaterialTheme.colorScheme.background,
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tabs",
                ) { current ->
                    when (current) {
                        Tab.REVIEW -> ReviewContent(reviewState, reviewViewModel)
                        Tab.LEARNING -> LearningScreen(
                            words = learningWords,
                            onPlay = learningViewModel::play,
                        )
                        Tab.STATS -> StatsScreen(stats = stats)
                        Tab.SETTINGS -> SettingsScreen(
                            themeMode = themeMode,
                            onThemeChange = settingsViewModel::setThemeMode,
                            newCardLimit = newCardLimit,
                            onNewCardLimitChange = settingsViewModel::setNewCardLimit,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewContent(state: ReviewUiState, viewModel: ReviewViewModel) {
    when (val s = state) {
        ReviewUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is ReviewUiState.Home -> HomeScreen(
            pending = s.pending,
            onStart = viewModel::startSession,
            onRefresh = viewModel::goHome,
            onPractice = viewModel::startPractice,
        )

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
    }
}
