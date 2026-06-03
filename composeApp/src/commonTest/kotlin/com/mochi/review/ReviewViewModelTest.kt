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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

    // Unconfined Main dispatcher so the ViewModel's init coroutine (seed + goHome)
    // runs synchronously during construction.
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsOnHomeWithThePendingCount() {
        // 5 new cards, limit 2 -> 2 are ready.
        val vm = viewModel(FakeDeck(List(5) { card(it.toLong(), isNew = true) }), newToday = 0, limit = 2)
        val state = vm.state.value
        assertTrue(state is ReviewUiState.Home)
        assertEquals(2, state.pending)
    }

    @Test
    fun newCardsAreCappedByTheDailyLimit() {
        val vm = viewModel(FakeDeck(List(5) { card(it.toLong(), isNew = true) }), newToday = 0, limit = 2)
        vm.startSession()
        val state = vm.state.value
        assertTrue(state is ReviewUiState.Reviewing)
        assertEquals(2, state.total)
    }

    @Test
    fun dueReviewsAreAlwaysIncludedThenNewCardsUpToTheLimit() {
        val cards = List(2) { card(it.toLong(), isNew = false) } + List(3) { card(100L + it, isNew = true) }
        val vm = viewModel(FakeDeck(cards), newToday = 0, limit = 1)
        vm.startSession()
        // 2 due reviews + 1 new (capped) = 3
        assertEquals(3, (vm.state.value as ReviewUiState.Reviewing).total)
    }

    @Test
    fun alreadyReachedLimitMeansReviewsOnly() {
        val cards = List(1) { card(it.toLong(), isNew = false) } + List(3) { card(100L + it, isNew = true) }
        // newToday already equals the limit, so no new cards should be added.
        val vm = viewModel(FakeDeck(cards), newToday = 2, limit = 2)
        vm.startSession()
        assertEquals(1, (vm.state.value as ReviewUiState.Reviewing).total)
    }

    @Test
    fun answeringAdvancesThenCompletesWithStats() {
        val deck = FakeDeck(List(2) { card(it.toLong(), isNew = true) })
        val vm = viewModel(deck, newToday = 0, limit = 10)
        vm.startSession()

        assertEquals(1, (vm.state.value as ReviewUiState.Reviewing).position)
        vm.answer(isCorrect = true)
        assertEquals(2, (vm.state.value as ReviewUiState.Reviewing).position)
        vm.answer(isCorrect = false)

        val state = vm.state.value
        assertTrue(state is ReviewUiState.Complete)
        assertEquals(2, state.stats.reviewed)
        assertEquals(1, state.stats.correct)
        assertEquals(2, deck.answers.size)
    }

    @Test
    fun emptyQueueStaysHomeWithZeroPending() {
        val vm = viewModel(FakeDeck(emptyList()), newToday = 0, limit = 20)
        val state = vm.state.value
        assertTrue(state is ReviewUiState.Home)
        assertEquals(0, state.pending)
    }

    @Test
    fun changingTheLimitRebuildsTheRunningSession() {
        val limit = FakeLimit(1)
        val vm = ReviewViewModel(FakeDeck(List(5) { card(it.toLong(), isNew = true) }), FakeCounter(0), limit, AudioPlayer())
        vm.startSession()
        assertEquals(1, (vm.state.value as ReviewUiState.Reviewing).total)

        limit.value = 3
        vm.onEnterReviewTab()
        assertEquals(3, (vm.state.value as ReviewUiState.Reviewing).total)
    }

    @Test
    fun practiceLogsAnswersButDoesNotReschedule() {
        val deck = FakeDeck(List(3) { card(it.toLong(), isNew = false) })
        val vm = viewModel(deck, newToday = 0, limit = 20)
        vm.startPractice()

        assertTrue(vm.state.value is ReviewUiState.Reviewing)
        vm.answer(isCorrect = true)
        vm.answer(isCorrect = true)
        vm.answer(isCorrect = false)
        // Practice logs each answer (so the "Still learning" list updates) but never reschedules.
        assertTrue(deck.answers.isEmpty())
        assertEquals(3, deck.practiceAnswers.size)
        assertTrue(vm.state.value is ReviewUiState.Complete)
    }

    private fun viewModel(deck: ReviewDeck, newToday: Long, limit: Int) =
        ReviewViewModel(deck, FakeCounter(newToday), FakeLimit(limit), AudioPlayer())
}

private fun card(id: Long, isNew: Boolean) = Flashcard(
    id = id,
    front = "front$id",
    back = "back$id",
    reading = "reading$id",
    category = "Test",
    sentence = null,
    sentence_meaning = null,
    audio = null,
    frequency = id,
    next_review = if (isNew) null else 1L,
    interval_days = if (isNew) 0L else 1L,
    ease = 2.5,
)

private class FakeDeck(var cards: List<Flashcard>) : ReviewDeck {
    val answers = mutableListOf<Pair<Long, Boolean>>()
    val practiceAnswers = mutableListOf<Pair<Long, Boolean>>()
    override suspend fun ensureSeeded() = Unit
    override fun due(now: Long): List<Flashcard> = cards
    override fun allCards(): List<Flashcard> = cards
    override fun recordAnswer(card: Flashcard, correct: Boolean) {
        answers += card.id to correct
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
