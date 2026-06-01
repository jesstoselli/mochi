package com.mochi.data

import com.mochi.db.AppDatabase
import com.mochi.db.Flashcard
import com.mochi.util.nowMillis
import com.mochi.util.todayEpochDay

/** What the review flow needs from the deck — lets the ViewModel be tested with fakes. */
interface ReviewDeck {
    suspend fun ensureSeeded()
    fun due(now: Long): List<Flashcard>
    fun allCards(): List<Flashcard>
    fun recordAnswer(card: Flashcard, correct: Boolean)
}

/**
 * Database access layer for cards. Hides SQLDelight from the UI.
 * `Flashcard` is the data class generated from the table in Flashcard.sq.
 */
class DeckRepository(private val db: AppDatabase) : ReviewDeck {

    /** Populates the deck on first launch (idempotent). */
    override suspend fun ensureSeeded() = seedIfNeeded(db)

    /** All cards due now: new ones (never reviewed) and started cards past next_review. */
    override fun due(now: Long): List<Flashcard> =
        db.flashcardQueries.selectDueForReview(now).executeAsList()

    /** Every card, regardless of schedule (used for free practice). */
    override fun allCards(): List<Flashcard> =
        db.flashcardQueries.selectAll().executeAsList()

    /**
     * Records an answer: updates the card's schedule (simplified SM-2) and writes a
     * review-log row (for streak, daily stats and the new-cards-per-day limit).
     */
    override fun recordAnswer(card: Flashcard, correct: Boolean) {
        val now = nowMillis()
        val wasNew = card.next_review == null

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
        db.reviewLogQueries.insertLog(
            card_id = card.id,
            reviewed_at = now,
            day = todayEpochDay(),
            was_new = if (wasNew) 1L else 0L,
            correct = if (correct) 1L else 0L,
        )
    }
}
