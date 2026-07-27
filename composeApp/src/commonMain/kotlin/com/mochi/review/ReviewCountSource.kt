package com.mochi.review

/** Reviews logged on a given epoch-day — the daily-goal crossing needs the running total. */
interface ReviewCountSource {
    fun reviewsOnDay(day: Long): Long
}
