package com.mochi.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochi.audio.AudioPlayer
import com.mochi.data.ReviewDeck
import com.mochi.db.Flashcard
import com.mochi.resources.Res
import com.mochi.settings.DailyGoalSource
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

// Cards drilled in a fallback practice session (schedule ignored).
private const val PRACTICE_SIZE = 20

// Confetti + mascot fire each time the session's correct-answer count crosses a multiple of this.
private const val CORRECT_MILESTONE = 10

/**
 * Owns the review flow as a state machine: Idle -> Reviewing -> Complete -> Idle.
 * A session is scoped to one study unit: that unit's due reviews plus new cards up to the
 * remaining GLOBAL daily new-card limit. Missed cards requeue (relearning). If a unit has
 * nothing scheduled, it falls back to a practice drill of that unit. Dependencies are
 * interfaces so the flow is unit-testable.
 */
@OptIn(ExperimentalResourceApi::class)
class ReviewViewModel(
    private val deck: ReviewDeck,
    private val newCardCounter: NewCardCounter,
    private val limitSource: NewCardLimitSource,
    private val reviewCountSource: ReviewCountSource,
    private val goalSource: DailyGoalSource,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {

    private val _state = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    private var session: List<Flashcard> = emptyList()
    private var index = 0
    private var reviewed = 0
    private var correct = 0
    private var sessionStreak = 0

    private var currentUnitId = 0
    private var sessionNewLimit = limitSource.newCardLimit()
    private var practiceMode = false
    private var reviewsAtSessionStart = 0L

    /** The unit the current/most-recent session belongs to (drives the shared-element key). */
    var lastOpenedUnitId: Int = 0
        private set

    init {
        viewModelScope.launch {
            deck.ensureSeeded()
            _state.value = ReviewUiState.Idle
        }
    }

    /**
     * Opens a study session for [unitId]: its scheduled queue if any, otherwise a practice drill
     * of that unit (so tapping a fully-learned unit still does something satisfying).
     */
    fun openUnit(unitId: Int) {
        lastOpenedUnitId = unitId
        currentUnitId = unitId
        sessionNewLimit = limitSource.newCardLimit()
        val queue = buildUnitQueue(unitId)
        if (queue.isEmpty()) {
            startUnitPractice(unitId)
        } else {
            practiceMode = false
            beginSession(queue)
        }
    }

    private fun startUnitPractice(unitId: Int) {
        practiceMode = true
        val queue = deck.cardsInUnit(unitId).shuffled().take(PRACTICE_SIZE)
        if (queue.isEmpty()) {
            _state.value = ReviewUiState.Idle
        } else {
            beginSession(queue)
        }
    }

    private fun beginSession(queue: List<Flashcard>) {
        session = queue
        index = 0
        reviewed = 0
        correct = 0
        sessionStreak = 0
        reviewsAtSessionStart = reviewCountSource.reviewsOnDay(todayEpochDay())
        emitReviewing(milestone = null)
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
        // One-shot when today's global review count first crosses the daily goal this session.
        val goal = goalSource.dailyGoal()
        val goalReached = goal > 0 &&
            (reviewsAtSessionStart + reviewed - 1) < goal &&
            (reviewsAtSessionStart + reviewed) >= goal
        var milestone: Int? = null
        if (isCorrect) {
            correct++
            sessionStreak++
            // Celebrate every Nth correct answer this session (cumulative, not consecutive).
            if (correct % CORRECT_MILESTONE == 0) milestone = correct
        } else {
            sessionStreak = 0
            session = session + updated // relearning: requeue at the end
        }
        if (index < session.lastIndex) {
            index++
            emitReviewing(milestone, goalReached)
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

    /** Ends the session (from the summary's "Done") and returns to the Library. */
    fun finish() {
        _state.value = ReviewUiState.Idle
    }

    /** Called when the Review tab becomes visible: rebuild a running session if the limit changed. */
    fun onEnterReviewTab() {
        val current = state.value
        if (current is ReviewUiState.Reviewing && limitSource.newCardLimit() != sessionNewLimit) {
            openUnit(currentUnitId)
        }
    }

    /** The unit's queue: its due reviews first, then its new cards up to the remaining daily limit. */
    private fun buildUnitQueue(unitId: Int): List<Flashcard> {
        val limit = limitSource.newCardLimit()
        val remainingNew = if (limit <= 0) {
            Int.MAX_VALUE
        } else {
            (limit - newCardCounter.newOnDay(todayEpochDay()).toInt()).coerceAtLeast(0)
        }
        val now = nowMillis()
        val due = deck.cardsInUnit(unitId).filter { it.next_review == null || it.next_review <= now }
        val (newCards, reviews) = due.partition { it.next_review == null }
        return reviews + newCards.take(remainingNew)
    }

    private fun emitReviewing(milestone: Int?, goalReached: Boolean = false) {
        _state.value = ReviewUiState.Reviewing(
            card = session[index],
            position = index + 1,
            total = session.size,
            sessionStreak = sessionStreak,
            correctMilestone = milestone,
            goalReached = goalReached,
        )
    }

    override fun onCleared() {
        audioPlayer.release()
    }
}
