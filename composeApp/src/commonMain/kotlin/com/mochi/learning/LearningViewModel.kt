package com.mochi.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochi.audio.AudioPlayer
import com.mochi.resources.Res
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** A word the user is still learning, shaped for the list UI. */
data class LearningWord(
    val id: Long,
    val word: String,
    val reading: String,
    val meaning: String,
    val audio: String?,
)

/** Exposes the "Still learning" word list and plays a word's pronunciation on tap. */
@OptIn(ExperimentalResourceApi::class)
class LearningViewModel(
    private val store: LearningStore,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {

    private val _words = MutableStateFlow(load())
    val words: StateFlow<List<LearningWord>> = _words.asStateFlow()

    /** Reload — call when the Learning tab becomes visible. */
    fun refresh() {
        _words.value = load()
    }

    /** Plays the word's pronunciation (no-op if it has no audio). */
    fun play(word: LearningWord) {
        val name = word.audio
        if (name.isNullOrBlank()) return
        viewModelScope.launch {
            runCatching { audioPlayer.play(Res.readBytes("files/audio/$name")) }
        }
    }

    private fun load(): List<LearningWord> = store.stillLearning().map {
        LearningWord(
            id = it.id,
            word = it.front,
            reading = it.reading,
            meaning = it.back,
            audio = it.audio,
        )
    }
}
