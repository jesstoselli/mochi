package com.mochi.review

import com.mochi.audio.AudioPlayer
import com.mochi.data.ReviewDeck
import com.mochi.db.Flashcard
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
    fun streakMilestoneIsSignalledEveryTenThenClears() {
        // 12 new cards so a Reviewing state still follows the streak-10 and streak-11 answers
        // (with 11 cards the 11th correct answer would complete the session before we can assert).
        val deck = FakeDeck(List(12) { card(it.toLong(), isNew = true) })
        val vm = viewModel(deck, newToday = 0, limit = 20)
        vm.openUnit(0)
        repeat(9) { vm.answer(isCorrect = true) }
        assertNull((vm.state.value as ReviewUiState.Reviewing).streakMilestone) // at streak 9
        vm.answer(isCorrect = true) // streak hits 10
        assertEquals(10, (vm.state.value as ReviewUiState.Reviewing).streakMilestone)
        vm.answer(isCorrect = true) // streak 11 -> milestone clears
        assertNull((vm.state.value as ReviewUiState.Reviewing).streakMilestone)
    }

    @Test
    fun changingTheLimitRebuildsTheRunningSession() {
        val limit = FakeLimit(1)
        val vm = ReviewViewModel(FakeDeck(List(5) { card(it.toLong(), isNew = true) }), FakeCounter(0), limit, AudioPlayer())
        vm.openUnit(0)
        assertEquals(1, (vm.state.value as ReviewUiState.Reviewing).total)
        limit.value = 3
        vm.onEnterReviewTab()
        assertEquals(3, (vm.state.value as ReviewUiState.Reviewing).total)
    }

    private fun viewModel(deck: ReviewDeck, newToday: Long, limit: Int) =
        ReviewViewModel(deck, FakeCounter(newToday), FakeLimit(limit), AudioPlayer())
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
