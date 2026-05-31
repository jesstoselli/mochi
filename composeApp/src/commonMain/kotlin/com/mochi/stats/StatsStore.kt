package com.mochi.stats

import com.mochi.db.AppDatabase

/** Read-only access to review history for stats (streak, daily counts, totals). */
class StatsStore(private val db: AppDatabase) {

    fun reviewsOnDay(day: Long): Long = db.reviewLogQueries.countOnDay(day).executeAsOne()

    fun newOnDay(day: Long): Long = db.reviewLogQueries.countNewOnDay(day).executeAsOne()

    fun distinctDaysDesc(): List<Long> = db.reviewLogQueries.distinctDaysDesc().executeAsList()

    fun totalStarted(): Long = db.flashcardQueries.countStarted().executeAsOne()
}
