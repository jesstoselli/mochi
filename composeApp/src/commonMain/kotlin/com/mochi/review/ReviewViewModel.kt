package com.mochi.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochi.audio.AudioPlayer
import com.mochi.data.ReviewDeck
import com.mochi.db.Flashcard
import com.mochi.resources.Res
import com.mochi.settings.NewCardLimitSource
import com.mochi.stats.NewCardCounter
import com.mochi.ui.SessionStats
import com.mochi.util.nowMillis
import com.mochi.util.todayEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Owns the review flow as a state machine. Each "session" is the Anki-style day queue:
 * all due reviews plus new cards up to the remaining daily limit. Dependencies are
 * interfaces so the flow can be unit-tested with fakes.
 */
@OptIn(ExperimentalResourceApi::class)
class ReviewViewModel(
    private val deck: ReviewDeck,
    private val newCardCounter: NewCardCounter,
    private val limitSource: NewCardLimitSource,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {

    private val _state = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    private var session: List<Flashcard> = emptyList()
    private var index = 0
    private var reviewed = 0
    private var correct = 0

    // The new-card limit the current session was built with (to detect changes).
    private var sessionNewLimit = limitSource.newCardLimit()

    init {
        viewModelScope.launch {
            deck.ensureSeeded()
            startSession()
        }
    }

    /** Builds the day's queue: due reviews + new cards capped by the daily limit. */
    fun startSession() {
        val today = todayEpochDay()
        val limit = limitSource.newCardLimit()
        sessionNewLimit = limit
        val newToday = newCardCounter.newOnDay(today).toInt()
        val remainingNew = if (limit <= 0) Int.MAX_VALUE else (limit - newToday).coerceAtLeast(0)

        // selectDueForReview returns new cards (next_review == null) first, then due reviews.
        val (newCards, reviews) = deck.due(nowMillis()).partition { it.next_review == null }
        session = reviews + newCards.take(remainingNew)

        if (session.isEmpty()) {
            _state.value = ReviewUiState.CaughtUp
            return
        }
        index = 0
        reviewed = 0
        correct = 0
        emitReviewing()
    }

    fun answer(isCorrect: Boolean) {
        val card = session.getOrNull(index) ?: return
        deck.recordAnswer(card, isCorrect)
        reviewed++
        if (isCorrect) correct++
        if (index < session.lastIndex) {
            index++
            emitReviewing()
        } else {
            _state.value = ReviewUiState.Complete(SessionStats(reviewed = reviewed, correct = correct))
        }
    }

    fun playCurrentAudio() {
        val name = session.getOrNull(index)?.audio
        if (name.isNullOrBlank()) return
        viewModelScope.launch {
            runCatching { audioPlayer.play(Res.readBytes("files/audio/$name")) }
        }
    }

    /** Rebuilds the session if the daily new-card limit changed (e.g. via Settings). */
    fun restartIfLimitChanged() {
        if (limitSource.newCardLimit() != sessionNewLimit) startSession()
    }

    /** Ends the session without loading more (from the summary's "Done for now"). */
    fun finish() {
        _state.value = ReviewUiState.CaughtUp
    }

    private fun emitReviewing() {
        _state.value = ReviewUiState.Reviewing(
            card = session[index],
            position = index + 1,
            total = session.size,
        )
    }

    override fun onCleared() {
        audioPlayer.release()
    }
}
