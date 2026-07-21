package com.mochi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import com.mochi.library.LibraryStore
import com.mochi.library.LibraryViewModel
import com.mochi.library.UnitSummary
import com.mochi.reminder.ReminderScheduler
import com.mochi.review.ReviewUiState
import com.mochi.review.ReviewViewModel
import com.mochi.settings.SettingsStore
import com.mochi.settings.SettingsViewModel
import com.mochi.settings.ThemeMode
import com.mochi.stats.StatsStore
import com.mochi.stats.StatsViewModel
import com.mochi.ui.screens.FlashcardScreen
import com.mochi.ui.screens.LearningScreen
import com.mochi.ui.screens.LibraryScreen
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
 * hosts a bottom-navigation Scaffold switching between Review, Learning, Stats and Settings.
 */
@Composable
fun App(driverFactory: DriverFactory, reminderScheduler: ReminderScheduler) {
    val db = remember { createDatabase(driverFactory) }
    val repo = remember { DeckRepository(db) }
    val statsStore = remember { StatsStore(db) }
    val settingsStore = remember { SettingsStore(db) }
    val audioPlayer = remember { AudioPlayer() }

    val learningStore = remember { LearningStore(db) }
    val libraryStore = remember { LibraryStore(db) }

    val reviewViewModel = viewModel { ReviewViewModel(repo, statsStore, settingsStore, audioPlayer) }
    val settingsViewModel = viewModel { SettingsViewModel(settingsStore, reminderScheduler) }
    val statsViewModel = viewModel { StatsViewModel(statsStore) }
    val learningViewModel = viewModel { LearningViewModel(learningStore, audioPlayer) }
    val libraryViewModel = viewModel { LibraryViewModel(libraryStore) }

    val reviewState by reviewViewModel.state.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val newCardLimit by settingsViewModel.newCardLimit.collectAsState()
    val reminderEnabled by settingsViewModel.reminderEnabled.collectAsState()
    val reminderTime by settingsViewModel.reminderTime.collectAsState()
    val stats by statsViewModel.stats.collectAsState()
    val learningWords by learningViewModel.words.collectAsState()
    val units by libraryViewModel.units.collectAsState()
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
            Tab.REVIEW -> reviewViewModel.onEnterReviewTab()
            // Stats and the Still-learning list are reactive Flows — nothing to refresh.
            Tab.STATS -> Unit
            Tab.LEARNING -> Unit
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
                        Tab.REVIEW -> ReviewContent(reviewState, units, reviewViewModel)
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
                            reminderEnabled = reminderEnabled,
                            onReminderEnabledChange = settingsViewModel::setReminderEnabled,
                            reminderTime = reminderTime,
                            onReminderTimeChange = settingsViewModel::setReminderTime,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ReviewContent(
    state: ReviewUiState,
    units: List<UnitSummary>,
    viewModel: ReviewViewModel,
) {
    SharedTransitionLayout(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it::class }, // animate only when the state TYPE changes
            label = "reviewShared",
        ) { s ->
            when (s) {
                ReviewUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                ReviewUiState.Idle -> LibraryScreen(
                    units = units,
                    onOpenUnit = viewModel::openUnit,
                    sharedScope = this@SharedTransitionLayout,
                    animatedScope = this@AnimatedContent,
                )

                is ReviewUiState.Reviewing -> FlashcardScreen(
                    card = s.card,
                    position = s.position,
                    total = s.total,
                    streakMilestone = s.streakMilestone,
                    sessionStreak = s.sessionStreak,
                    onAnswer = viewModel::answer,
                    onPlayAudio = viewModel::playCurrentAudio,
                    sharedScope = this@SharedTransitionLayout,
                    animatedScope = this@AnimatedContent,
                    sharedKey = "unit-${viewModel.lastOpenedUnitId}",
                )

                is ReviewUiState.Complete -> SessionCompleteScreen(
                    stats = s.stats,
                    onContinue = viewModel::finish,
                    onDone = viewModel::finish,
                )
            }
        }
    }
}
