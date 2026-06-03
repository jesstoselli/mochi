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

// Cards in a free-practice session (review regardless of schedule).
private const val PRACTICE_SIZE = 20

/**
 * Owns the review flow as a state machine: Home -> Reviewing -> Complete -> Home.
 * A "session" is the Anki-style day queue: all due reviews plus new cards up to the
 * remaining daily limit. Missed cards go back to the end of the queue (relearning) until
 * answered correctly. Dependencies are interfaces so the flow can be unit-tested.
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

    // Free practice doesn't touch the schedule or stats — but it still logs the answer
    // (flagged as practice) so the "Still learning" list stays current.
    private var practiceMode = false

    init {
        viewModelScope.launch {
            deck.ensureSeeded()
            goHome()
        }
    }

    /** Shows the landing screen with the current count of cards ready to study. */
    fun goHome() {
        _state.value = ReviewUiState.Home(pending = buildQueue().size)
    }

    /** Starts (or restarts) a study session from the current day queue. */
    fun startSession() {
        practiceMode = false
        sessionNewLimit = limitSource.newCardLimit()
        session = buildQueue()
        if (session.isEmpty()) {
            goHome()
            return
        }
        index = 0
        reviewed = 0
        correct = 0
        emitReviewing()
    }

    /** Free practice over a shuffled sample of all cards — ignores schedule, logs as practice. */
    fun startPractice() {
        practiceMode = true
        session = deck.allCards().shuffled().take(PRACTICE_SIZE)
        if (session.isEmpty()) {
            goHome()
            return
        }
        index = 0
        reviewed = 0
        correct = 0
        emitReviewing()
    }

    fun answer(isCorrect: Boolean) {
        val card = session.getOrNull(index) ?: return
        val updated = if (practiceMode) {
            deck.recordPractice(card, isCorrect)
            card
        } else {
            deck.recordAnswer(card, isCorrect)
        }
        reviewed++
        if (isCorrect) {
            correct++
        } else {
            // Relearning: a missed card returns to the end of the queue until answered right.
            session = session + updated
        }
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

    /** Ends the session (from the summary's "Done") and returns to Home. */
    fun finish() = goHome()

    /**
     * Called when the Review tab becomes visible: refresh the Home count, or rebuild the
     * running session if the daily new-card limit changed (e.g. via Settings).
     */
    fun onEnterReviewTab() {
        when (state.value) {
            is ReviewUiState.Home -> goHome()
            is ReviewUiState.Reviewing -> if (limitSource.newCardLimit() != sessionNewLimit) startSession()
            else -> Unit
        }
    }

    /** The day's queue: due reviews first, then new cards up to the remaining daily limit. */
    private fun buildQueue(): List<Flashcard> {
        val limit = limitSource.newCardLimit()
        val remainingNew = if (limit <= 0) {
            Int.MAX_VALUE
        } else {
            (limit - newCardCounter.newOnDay(todayEpochDay()).toInt()).coerceAtLeast(0)
        }
        val (newCards, reviews) = deck.due(nowMillis()).partition { it.next_review == null }
        return reviews + newCards.take(remainingNew)
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
