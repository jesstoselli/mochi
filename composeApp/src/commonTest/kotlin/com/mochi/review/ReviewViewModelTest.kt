package com.mochi.review

import com.mochi.audio.AudioPlayer
import com.mochi.data.ReviewDeck
import com.mochi.db.Flashcard
import com.mochi.settings.DailyGoalSource
import com.mochi.settings.NewCardLimitSource
import com.mochi.stats.NewCardCounter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsIdle() {
        val vm = viewModel(FakeDeck(List(5) { card(it.toLong(), isNew = true) }), newToday = 0, limit = 2)
        assertTrue(vm.state.value is ReviewUiState.Idle)
    }

    @Test
    fun newCardsAreCappedByTheDailyLimit() {
        val vm = viewModel(FakeDeck(List(5) { card(it.toLong(), isNew = true) }), newToday = 0, limit = 2)
        vm.openUnit(0)
        val state = vm.state.value
        assertTrue(state is ReviewUiState.Reviewing)
        assertEquals(2, state.total)
    }

    @Test
    fun dueReviewsAreAlwaysIncludedThenNewCardsUpToTheLimit() {
        val cards = List(2) { card(it.toLong(), isNew = false) } + List(3) { card(100L + it, isNew = true) }
        val vm = viewModel(FakeDeck(cards), newToday = 0, limit = 1)
        vm.openUnit(0)
        assertEquals(3, (vm.state.value as ReviewUiState.Reviewing).total) // 2 due + 1 new
    }

    @Test
    fun alreadyReachedGlobalLimitMeansReviewsOnly() {
        val cards = List(1) { card(it.toLong(), isNew = false) } + List(3) { card(100L + it, isNew = true) }
        val vm = viewModel(FakeDeck(cards), newToday = 2, limit = 2)
        vm.openUnit(0)
        assertEquals(1, (vm.state.value as ReviewUiState.Reviewing).total)
    }

    @Test
    fun openingAUnitWithNothingDueFallsBackToPractice() {
        // All learned and not due (next_review far in the future), no new allowance.
        val cards = List(3) { card(it.toLong(), isNew = false, nextReview = Long.MAX_VALUE) }
        val deck = FakeDeck(cards)
        val vm = viewModel(deck, newToday = 0, limit = 0) // limit 0 = unlimited, but nothing is due
        vm.openUnit(0)
        assertTrue(vm.state.value is ReviewUiState.Reviewing) // practice session opened
        vm.answer(isCorrect = true)
        // Practice never reschedules; it only logs practice answers.
        assertTrue(deck.answers.isEmpty())
        assertTrue(deck.practiceAnswers.isNotEmpty())
    }

    @Test
    fun answeringAdvancesThenCompletesWithStats() {
        val deck = FakeDeck(List(2) { card(it.toLong(), isNew = true) })
        val vm = viewModel(deck, newToday = 0, limit = 10)
        vm.openUnit(0)
        assertEquals(1, (vm.state.value as ReviewUiState.Reviewing).position)
        vm.answer(isCorrect = true)
        assertEquals(2, (vm.state.value as ReviewUiState.Reviewing).position)
        vm.answer(isCorrect = true)
        val state = vm.state.value
        assertTrue(state is ReviewUiState.Complete)
        assertEquals(2, state.stats.reviewed)
        assertEquals(2, state.stats.correct)
    }

    @Test
    fun missedCardReturnsToTheQueueUntilAnsweredRight() {
        val deck = FakeDeck(List(2) { card(it.toLong(), isNew = true) })
        val vm = viewModel(deck, newToday = 0, limit = 10)
        vm.openUnit(0)
        vm.answer(isCorrect = false)
        assertEquals(3, (vm.state.value as ReviewUiState.Reviewing).total)
        vm.answer(isCorrect = true)
        assertEquals(0L, (vm.state.value as ReviewUiState.Reviewing).card.id)
        vm.answer(isCorrect = true)
        assertTrue(vm.state.value is ReviewUiState.Complete)
    }

    @Test
    fun sessionStreakCountsCorrectAnswersAndResetsOnMiss() {
        val deck = FakeDeck(List(4) { card(it.toLong(), isNew = true) })
        val vm = viewModel(deck, newToday = 0, limit = 10)
        vm.openUnit(0)
        vm.answer(isCorrect = true)
        assertEquals(1, (vm.state.value as ReviewUiState.Reviewing).sessionStreak)
        vm.answer(isCorrect = true)
        assertEquals(2, (vm.state.value as ReviewUiState.Reviewing).sessionStreak)
        vm.answer(isCorrect = false)
        assertEquals(0, (vm.state.value as ReviewUiState.Reviewing).sessionStreak)
    }

    @Test
    fun correctMilestoneIsSignalledEveryTenCorrectAnswersThenClears() {
        // Plenty of cards so a Reviewing state still follows the 10th and 11th correct answers.
        val deck = FakeDeck(List(14) { card(it.toLong(), isNew = true) })
        val vm = viewModel(deck, newToday = 0, limit = 20)
        vm.openUnit(0)
        repeat(9) { vm.answer(isCorrect = true) }
        assertNull((vm.state.value as ReviewUiState.Reviewing).correctMilestone) // 9 correct
        vm.answer(isCorrect = true) // 10th correct
        assertEquals(10, (vm.state.value as ReviewUiState.Reviewing).correctMilestone)
        vm.answer(isCorrect = true) // 11th correct -> milestone clears
        assertNull((vm.state.value as ReviewUiState.Reviewing).correctMilestone)
    }

    @Test
    fun milestoneCountsCumulativeCorrectAnswersDespiteAMiss() {
        // A miss must NOT reset progress toward the milestone (it's cumulative, not a streak).
        val deck = FakeDeck(List(15) { card(it.toLong(), isNew = true) })
        val vm = viewModel(deck, newToday = 0, limit = 20)
        vm.openUnit(0)
        repeat(5) { vm.answer(isCorrect = true) } // 5 correct
        vm.answer(isCorrect = false) // miss: resets the 🔥 streak, not the milestone count
        repeat(5) { vm.answer(isCorrect = true) } // 5 more -> 10 correct total
        assertEquals(10, (vm.state.value as ReviewUiState.Reviewing).correctMilestone)
    }

    @Test
    fun changingTheLimitRebuildsTheRunningSession() {
        val limit = FakeLimit(1)
        val deck = FakeDeck(List(5) { card(it.toLong(), isNew = true) })
        val vm = ReviewViewModel(deck, FakeCounter(0), limit, FakeReviewCount(0), FakeGoal(0), AudioPlayer())
        vm.openUnit(0)
        assertEquals(1, (vm.state.value as ReviewUiState.Reviewing).total)
        limit.value = 3
        vm.onEnterReviewTab()
        assertEquals(3, (vm.state.value as ReviewUiState.Reviewing).total)
    }

    @Test
    fun goalReachedFiresOnceWhenReviewsCrossTheDailyGoal() {
        val deck = FakeDeck(List(5) { card(it.toLong(), isNew = true) })
        val vm = viewModel(deck, newToday = 0, limit = 10, reviewsToday = 0, goal = 3)
        vm.openUnit(0)
        vm.answer(isCorrect = true) // global 1
        assertFalse((vm.state.value as ReviewUiState.Reviewing).goalReached)
        vm.answer(isCorrect = true) // global 2
        assertFalse((vm.state.value as ReviewUiState.Reviewing).goalReached)
        vm.answer(isCorrect = true) // global 3 -> crosses the goal
        assertTrue((vm.state.value as ReviewUiState.Reviewing).goalReached)
        vm.answer(isCorrect = true) // global 4 -> no refire
        assertFalse((vm.state.value as ReviewUiState.Reviewing).goalReached)
    }

    @Test
    fun goalReachedDoesNotFireWhenGoalAlreadyMetBeforeTheSession() {
        val deck = FakeDeck(List(5) { card(it.toLong(), isNew = true) })
        val vm = viewModel(deck, newToday = 0, limit = 10, reviewsToday = 3, goal = 3)
        vm.openUnit(0)
        vm.answer(isCorrect = true) // global already 4
        assertFalse((vm.state.value as ReviewUiState.Reviewing).goalReached)
    }

    private fun viewModel(
        deck: ReviewDeck,
        newToday: Long,
        limit: Int,
        reviewsToday: Long = 0,
        goal: Int = 0,
    ) = ReviewViewModel(
        deck,
        FakeCounter(newToday),
        FakeLimit(limit),
        FakeReviewCount(reviewsToday),
        FakeGoal(goal),
        AudioPlayer(),
    )
}

private fun card(id: Long, isNew: Boolean, nextReview: Long? = null) = Flashcard(
    id = id,
    front = "front$id",
    back = "back$id",
    reading = "reading$id",
    category = "Test",
    sentence = null,
    sentence_meaning = null,
    audio = null,
    frequency = id,
    next_review = when {
        isNew -> null
        nextReview != null -> nextReview
        else -> 1L
    },
    interval_days = if (isNew) 0L else 1L,
    ease = 2.5,
)

private class FakeDeck(var cards: List<Flashcard>) : ReviewDeck {
    val answers = mutableListOf<Pair<Long, Boolean>>()
    val practiceAnswers = mutableListOf<Pair<Long, Boolean>>()
    override suspend fun ensureSeeded() = Unit
    override fun due(now: Long): List<Flashcard> =
        cards.filter { it.next_review == null || it.next_review <= now }
    override fun allCards(): List<Flashcard> = cards
    override fun cardsInUnit(unitId: Int): List<Flashcard> = cards // tests use a single unit (0)
    override fun recordAnswer(card: Flashcard, correct: Boolean): Flashcard {
        answers += card.id to correct
        return card
    }
    override fun recordPractice(card: Flashcard, correct: Boolean) {
        practiceAnswers += card.id to correct
    }
}

private class FakeCounter(private val newToday: Long) : NewCardCounter {
    override fun newOnDay(day: Long): Long = newToday
}

private class FakeLimit(var value: Int) : NewCardLimitSource {
    override fun newCardLimit(): Int = value
}

private class FakeReviewCount(private val count: Long) : ReviewCountSource {
    override fun reviewsOnDay(day: Long): Long = count
}

private class FakeGoal(private val goal: Int) : DailyGoalSource {
    override fun dailyGoal(): Int = goal
}
