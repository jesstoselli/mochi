package com.mochi.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochi.audio.AudioPlayer
import com.mochi.data.DeckRepository
import com.mochi.db.Flashcard
import com.mochi.resources.Res
import com.mochi.ui.SessionStats
import com.mochi.util.nowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Cards per session — keeps the first run sane (otherwise all 1500 new cards are "due").
private const val SESSION_SIZE = 20

/**
 * Owns the whole review flow as a state machine: seed -> load due cards -> review a
 * capped session -> summary -> continue / caught up. The screens are presentation-only
 * and just render [state] and call these actions.
 */
@OptIn(ExperimentalResourceApi::class)
class ReviewViewModel(
    private val repo: DeckRepository,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {

    private val _state = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    private var session: List<Flashcard> = emptyList()
    private var index = 0
    private var reviewed = 0
    private var correct = 0

    init {
        viewModelScope.launch {
            repo.ensureSeeded()
            startSession()
        }
    }

    /** Loads the next batch of due cards, or shows the "caught up" state if none. */
    fun startSession() {
        val due = repo.dueForReview(nowMillis())
        if (due.isEmpty()) {
            _state.value = ReviewUiState.CaughtUp
            return
        }
        session = due.take(SESSION_SIZE)
        index = 0
        reviewed = 0
        correct = 0
        emitReviewing()
    }

    fun answer(isCorrect: Boolean) {
        val card = session.getOrNull(index) ?: return
        repo.recordAnswer(card, isCorrect, nowMillis())
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
