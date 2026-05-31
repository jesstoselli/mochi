package com.mochi.review

import com.mochi.db.Flashcard
import com.mochi.ui.SessionStats

/** Everything the review screens need to render, as a single state machine. */
sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data class Reviewing(val card: Flashcard, val position: Int, val total: Int) : ReviewUiState
    data class Complete(val stats: SessionStats) : ReviewUiState
    data object CaughtUp : ReviewUiState
}
