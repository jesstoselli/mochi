package com.mochi.stats

import app.cash.sqldelight.coroutines.asFlow
import com.mochi.db.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Just the new-cards-today count the review flow needs (lets the ViewModel use a fake). */
interface NewCardCounter {
    fun newOnDay(day: Long): Long
}

/** Read-only access to review history for stats (streak, daily counts, totals). */
class StatsStore(private val db: AppDatabase) : NewCardCounter, com.mochi.review.ReviewCountSource {

    override fun reviewsOnDay(day: Long): Long = db.reviewLogQueries.countOnDay(day).executeAsOne()

    override fun newOnDay(day: Long): Long = db.reviewLogQueries.countNewOnDay(day).executeAsOne()

    fun distinctDaysDesc(): List<Long> = db.reviewLogQueries.distinctDaysDesc().executeAsList()

    fun totalStarted(): Long = db.flashcardQueries.countStarted().executeAsOne()

    /** Emits once on collection and again whenever the review log or card progress changes. */
    fun changes(): Flow<Unit> = combine(
        db.reviewLogQueries.distinctDaysDesc().asFlow(),
        db.flashcardQueries.countStarted().asFlow(),
    ) { _, _ -> }
}
