package com.mochi.goal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyGoalTest {
    @Test
    fun progressIsTheFraction() {
        val state = toDailyGoalState(reviewsToday = 5, goal = 20)
        assertEquals(0.25f, state.progress)
        assertFalse(state.reached)
    }

    @Test
    fun progressClampsAtOneWhenOverGoal() {
        val state = toDailyGoalState(reviewsToday = 25, goal = 20)
        assertEquals(1f, state.progress)
        assertTrue(state.reached)
    }

    @Test
    fun reachedIsInclusiveAtTheGoal() {
        assertTrue(toDailyGoalState(reviewsToday = 20, goal = 20).reached)
    }

    @Test
    fun zeroReviewsIsEmpty() {
        val state = toDailyGoalState(reviewsToday = 0, goal = 20)
        assertEquals(0f, state.progress)
        assertFalse(state.reached)
    }
}
