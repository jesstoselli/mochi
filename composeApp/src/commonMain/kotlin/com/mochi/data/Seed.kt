package com.mochi.data

import com.mochi.db.AppDatabase
import com.mochi.resources.Res
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Shape of each item in deck.json (produced by convert_apkg.py). */
@Serializable
data class DeckEntry(
    val word: String,
    val reading: String,
    val meaning: String,
    val sentence: String = "",
    @SerialName("sentence_meaning") val sentenceMeaning: String = "",
    val audio: String = "",
    val frequency: Int = 0,
    val category: String = "Vocabulary",
)

/**
 * Seeds the database on first launch by reading the bundled deck.json.
 * Idempotent: does nothing if cards already exist.
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun seedIfNeeded(db: AppDatabase) {
    val q = db.flashcardQueries
    if (q.countCards().executeAsOne() > 0L) return

    val bytes = Res.readBytes("files/deck.json")
    val entries = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<DeckEntry>>(bytes.decodeToString())

    db.transaction {
        entries.forEach { e ->
            q.insertCard(
                front = e.word,
                back = e.meaning,
                reading = e.reading,
                category = e.category,
                sentence = e.sentence.ifBlank { null },
                sentence_meaning = e.sentenceMeaning.ifBlank { null },
                audio = e.audio.ifBlank { null },
                frequency = e.frequency.toLong(),
            )
        }
    }
}
