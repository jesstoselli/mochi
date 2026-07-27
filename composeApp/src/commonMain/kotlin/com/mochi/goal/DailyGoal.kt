package com.mochi.goal

/** Snapshot of today's progress toward the daily review goal. */
data class DailyGoalState(
    val reviewsToday: Int,
    val goal: Int,
    val progress: Float,
    val reached: Boolean,
)

/**
 * Pure mapping from today's review count and the configured goal to the ring's state.
 * [goal] is always >= 10 in the app, so there is no divide-by-zero; a defensive guard keeps
 * the function total anyway.
 */
fun toDailyGoalState(reviewsToday: Int, goal: Int): DailyGoalState {
    val safeGoal = goal.coerceAtLeast(1)
    val progress = (reviewsToday.toFloat() / safeGoal).coerceIn(0f, 1f)
    return DailyGoalState(
        reviewsToday = reviewsToday,
        goal = goal,
        progress = progress,
        reached = reviewsToday >= goal,
    )
}
