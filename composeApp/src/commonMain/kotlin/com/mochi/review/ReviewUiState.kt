package com.mochi.review

import com.mochi.db.Flashcard
import com.mochi.ui.SessionStats

/** Everything the review screens need to render, as a single state machine. */
sealed interface ReviewUiState {
    data object Loading : ReviewUiState

    /** Not in a session — the Library grid is shown instead. */
    data object Idle : ReviewUiState

    /**
     * An active card. [sessionStreak] is the run of consecutive correct answers this session (the
     * 🔥 combo in the HUD); [correctMilestone] is non-null (e.g. 10, 20, …) only on the emission
     * where the session's cumulative correct-answer count just crossed a multiple of 10, so the UI
     * fires the confetti + mascot cheer exactly once.
     */
    data class Reviewing(
        val card: Flashcard,
        val position: Int,
        val total: Int,
        val sessionStreak: Int,
        val correctMilestone: Int?,
    ) : ReviewUiState

    data class Complete(val stats: SessionStats) : ReviewUiState
}
