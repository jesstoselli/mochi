package com.mochi.review

import com.mochi.db.Flashcard
import com.mochi.ui.SessionStats

/** Everything the review screens need to render, as a single state machine. */
sealed interface ReviewUiState {
    data object Loading : ReviewUiState

    /** Landing screen. [pending] is how many cards are ready right now (0 = caught up). */
    data class Home(val pending: Int) : ReviewUiState

    data class Reviewing(val card: Flashcard, val position: Int, val total: Int) : ReviewUiState
    data class Complete(val stats: SessionStats) : ReviewUiState
}
