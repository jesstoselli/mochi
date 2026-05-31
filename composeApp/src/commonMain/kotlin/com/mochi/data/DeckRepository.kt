package com.mochi.data

import com.mochi.db.AppDatabase
import com.mochi.db.Flashcard

/**
 * Database access layer. Hides SQLDelight from the UI.
 * `Flashcard` is the data class generated from the table in Flashcard.sq.
 */
class DeckRepository(private val db: AppDatabase) {

    fun all(): List<Flashcard> = db.flashcardQueries.selectAll().executeAsList()

    fun dueForReview(now: Long): List<Flashcard> =
        db.flashcardQueries.selectDueForReview(now).executeAsList()

    /**
     * Simplified SM-2 spaced repetition. `correct` decides whether the interval
     * grows or resets. Study this later — it turns a plain flashcard app into a
     * smart-review one.
     */
    fun recordAnswer(card: Flashcard, correct: Boolean, now: Long) {
        val newEase = (card.ease + if (correct) 0.1 else -0.2).coerceAtLeast(1.3)
        val newInterval = when {
            !correct -> 1L
            card.interval_days <= 0L -> 1L
            else -> (card.interval_days * newEase).toLong()
        }
        val next = now + newInterval * 24L * 60L * 60L * 1000L
        db.flashcardQueries.updateReview(
            nextReview = next,
            intervalDays = newInterval,
            ease = newEase,
            id = card.id,
        )
    }
}
