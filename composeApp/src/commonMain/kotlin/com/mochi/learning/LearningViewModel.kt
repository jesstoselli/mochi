package com.mochi.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochi.audio.AudioPlayer
import com.mochi.db.StillLearning
import com.mochi.resources.Res
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Keep the DB subscription alive briefly across config/recomposition gaps.
private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

/** A word the user is still learning, shaped for the list UI. */
data class LearningWord(
    val id: Long,
    val word: String,
    val reading: String,
    val meaning: String,
    val audio: String?,
)

/**
 * Exposes the "Still learning" word list as a reactive [StateFlow] (updates automatically as
 * answers come in) and plays a word's pronunciation on tap.
 */
@OptIn(ExperimentalResourceApi::class)
class LearningViewModel(
    store: LearningStore,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {

    val words: StateFlow<List<LearningWord>> = store.stillLearning()
        .map { rows -> rows.map { it.toLearningWord() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    /** Plays the word's pronunciation (no-op if it has no audio). */
    fun play(word: LearningWord) {
        val name = word.audio
        if (name.isNullOrBlank()) return
        viewModelScope.launch {
            runCatching { audioPlayer.play(Res.readBytes("files/audio/$name")) }
        }
    }
}

private fun StillLearning.toLearningWord() = LearningWord(
    id = id,
    word = front,
    reading = reading,
    meaning = back,
    audio = audio,
)
