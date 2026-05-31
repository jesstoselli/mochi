package com.mochi.stats

import androidx.lifecycle.ViewModel
import com.mochi.util.todayEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StatsUiState(
    val streak: Int = 0,
    val reviewsToday: Long = 0,
    val totalLearned: Long = 0,
)

/** Computes the essential stats (streak, today's reviews, words learned) from the log. */
class StatsViewModel(private val statsStore: StatsStore) : ViewModel() {

    private val _stats = MutableStateFlow(compute())
    val stats: StateFlow<StatsUiState> = _stats.asStateFlow()

    /** Recompute — call when the stats tab becomes visible. */
    fun refresh() {
        _stats.value = compute()
    }

    private fun compute(): StatsUiState {
        val today = todayEpochDay()
        return StatsUiState(
            streak = streakLength(statsStore.distinctDaysDesc(), today),
            reviewsToday = statsStore.reviewsOnDay(today),
            totalLearned = statsStore.totalStarted(),
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
