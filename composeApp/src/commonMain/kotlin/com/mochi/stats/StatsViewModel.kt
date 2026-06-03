package com.mochi.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochi.util.todayEpochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Keep the DB subscription alive briefly across config/recomposition gaps.
private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

data class StatsUiState(
    val streak: Int = 0,
    val reviewsToday: Long = 0,
    val totalLearned: Long = 0,
    // Reviews per day for the last 7 days, oldest first (… , yesterday, today).
    val last7Days: List<Long> = emptyList(),
)

/**
 * Computes the essential stats (streak, today's reviews, words learned) from the log as a
 * reactive [StateFlow] — it recomputes automatically whenever the review data changes.
 */
class StatsViewModel(private val statsStore: StatsStore) : ViewModel() {

    val stats: StateFlow<StatsUiState> = statsStore.changes()
        .map { compute() }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = StatsUiState(),
        )

    private fun compute(): StatsUiState {
        val today = todayEpochDay()
        return StatsUiState(
            streak = streakLength(statsStore.distinctDaysDesc(), today),
            reviewsToday = statsStore.reviewsOnDay(today),
            totalLearned = statsStore.totalStarted(),
            last7Days = (6L downTo 0L).map { statsStore.reviewsOnDay(today - it) },
        )
    }

    /** Consecutive days with at least one review, ending today (or yesterday). */
    private fun streakLength(daysDesc: List<Long>, today: Long): Int {
        if (daysDesc.isEmpty()) return 0
        val days = daysDesc.toHashSet()
        var day = if (today in days) today else today - 1
        if (day !in days) return 0
        var length = 0
        while (day in days) {
            length++
            day--
        }
        return length
    }
}
