package com.mochi.ui

/** Outcome of a single review session. */
data class SessionStats(val reviewed: Int, val correct: Int) {
    val accuracyPercent: Int get() = if (reviewed == 0) 0 else correct * 100 / reviewed
}
