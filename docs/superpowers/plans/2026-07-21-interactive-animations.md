# Interactive Animations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a unit-based Library with per-unit study sessions plus four interactive animations (3D flip + physics swipe, Canvas confetti, animated counters/liquid progress, and a Library→session shared-element transition) to the Mochi Compose Multiplatform app.

**Architecture:** All code lives in `commonMain`. Study units are *derived* from frequency rank (no schema change). A new reactive `LibraryStore`/`LibraryViewModel` feeds a `LibraryScreen` grid; the existing `ReviewViewModel` state machine gains per-unit sessions and a session-streak signal. Reusable, data-agnostic animation modifiers live in a new `com.mochi.ui.motion` package and are consumed by the screens. The Review tab is wrapped in a `SharedTransitionLayout`.

**Tech Stack:** Kotlin 2.2.20, Compose Multiplatform 1.10.3, SQLDelight 2.2.1 (+coroutines-extensions), `org.jetbrains.androidx.lifecycle` ViewModel, kotlinx-coroutines(+test), kotlin.test.

---

## Conventions for every task

- **Language:** all code and commit messages in **English**. Lines ≤ **120** chars. Imports sorted (case-sensitive, uppercase before lowercase). Keep ktlint/detekt clean.
- **Commit author** is the local git config (`Jessyca Toselli <toselli.jess@gmail.com>`) — do not pass `--author`.
- **Test command (canonical):** `./gradlew :composeApp:allTests`
  - This runs `commonTest` on the iOS simulator target (Android host tests are disabled in this module). First run compiles Kotlin/Native and is slow (minutes); later runs are faster.
  - **Optional speed-up (do once, Task 1):** enabling `withHostTest {}` adds fast JVM-based Android unit tests.
- **Lint command:** `./gradlew :composeApp:ktlintCheck :composeApp:detekt`
- **Build check (compiles common + Android):** `./gradlew :composeApp:compileDebugKotlinAndroid`

## File structure (created / modified)

**Created:**
- `composeApp/src/commonMain/kotlin/com/mochi/library/LibraryStore.kt` — unit derivation + reactive `units()` Flow; pure `toUnitSummaries()`.
- `composeApp/src/commonMain/kotlin/com/mochi/library/LibraryViewModel.kt` — exposes `StateFlow<List<UnitSummary>>`.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/LibraryScreen.kt` — the 30-unit grid.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/PressBounce.kt` — `Modifier.pressBounce()`.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/SwipeToDismissCard.kt` — `Modifier.swipeToDismissCard(...)`.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/ConfettiBurst.kt` — particle system + `Particle` + pure `advance()`.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/LiquidProgress.kt` — sine-wave fill.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/AnimatedCounter.kt` — odometer digits.
- `composeApp/src/commonTest/kotlin/com/mochi/library/LibraryStoreTest.kt`
- `composeApp/src/commonTest/kotlin/com/mochi/ui/motion/ConfettiBurstTest.kt`

**Modified:**
- `composeApp/src/commonMain/kotlin/com/mochi/data/DeckRepository.kt` — add `cardsInUnit()` to `ReviewDeck` + impl.
- `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewUiState.kt` — replace `Home` with `Idle`; extend `Reviewing`.
- `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewViewModel.kt` — per-unit sessions + session streak.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/components/FlipCard.kt` — spring flip, dynamic shadow, swipe hook.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt` — swipe wiring, streak HUD, liquid bar.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/SessionCompleteScreen.kt` — confetti.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/StatsScreen.kt` — animated counters.
- `composeApp/src/commonMain/kotlin/com/mochi/App.kt` — Library wiring + `SharedTransitionLayout`.
- `composeApp/src/commonTest/kotlin/com/mochi/review/ReviewViewModelTest.kt` — update fakes + per-unit tests.
- **Deleted:** `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/HomeScreen.kt`.

---

## Phase 1 — Library data layer (units derived from frequency rank)

### Task 1: (Optional) Enable fast host tests

**Files:**
- Modify: `composeApp/build.gradle.kts` (inside `kotlin { android { … } }`)

- [ ] **Step 1: Add `withHostTest {}` to the android target**

In the `android { … }` block (after `androidResources { enable = true }`), add:

```kotlin
        @Suppress("UnstableApiUsage")
        withHostTest {}
```

- [ ] **Step 2: Discover the generated task name**

Run: `./gradlew :composeApp:tasks --all | grep -i "hostTest"`
Expected: a task such as `testAndroidHostTest` (or `androidHostTest`) is listed. Note the exact name; it runs `commonTest` on the JVM in seconds.

- [ ] **Step 3: Sanity-run the existing tests**

Run: `./gradlew :composeApp:allTests`
Expected: `BUILD SUCCESSFUL`, existing `ReviewViewModelTest` passes.

- [ ] **Step 4: Commit**

```bash
git add composeApp/build.gradle.kts
git commit -m "test: enable fast Android host tests for commonTest"
```

> If `withHostTest {}` is unavailable in this AGP version, skip this task and use `./gradlew :composeApp:allTests` everywhere.

---

### Task 2: `UnitSummary` + pure `toUnitSummaries()`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/library/LibraryStore.kt`
- Test: `composeApp/src/commonTest/kotlin/com/mochi/library/LibraryStoreTest.kt`

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/mochi/library/LibraryStoreTest.kt`:

```kotlin
package com.mochi.library

import com.mochi.db.Flashcard
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryStoreTest {

    // now = 100; a card is "due" if next_review == null (new) OR next_review <= now.
    private val now = 100L

    private fun card(rank: Long, nextReview: Long?) = Flashcard(
        id = rank,
        front = "front$rank",
        back = "back$rank",
        reading = "reading$rank",
        category = "Kaishi 1.5k",
        sentence = null,
        sentence_meaning = null,
        audio = null,
        frequency = rank,
        next_review = nextReview,
        interval_days = 0L,
        ease = 2.5,
    )

    @Test
    fun chunksCardsIntoUnitsOfFifty() {
        // 120 cards -> 3 units (50, 50, 20).
        val cards = List(120) { card(it.toLong(), nextReview = null) }
        val units = toUnitSummaries(cards, now)
        assertEquals(3, units.size)
        assertEquals(50, units[0].totalCount)
        assertEquals(50, units[1].totalCount)
        assertEquals(20, units[2].totalCount)
        assertEquals(0, units[0].unitId)
        assertEquals(2, units[2].unitId)
    }

    @Test
    fun countsLearnedAndDuePerUnit() {
        // Unit 0: 50 cards. 10 learned & due (next_review=50<=100),
        // 5 learned & not due (next_review=200>100), rest new (null, counted new not due).
        val cards = List(50) { i ->
            val nr = when {
                i < 10 -> 50L      // learned, due now
                i < 15 -> 200L     // learned, not due
                else -> null       // new
            }
            card(i.toLong(), nr)
        }
        val unit = toUnitSummaries(cards, now).single()
        assertEquals(15, unit.learnedCount) // next_review != null
        assertEquals(10, unit.dueCount)     // next_review <= now
    }

    @Test
    fun sampleFrontIsTheFirstCardOfTheUnit() {
        val cards = List(50) { card(it.toLong(), nextReview = null) }
        assertEquals("front0", toUnitSummaries(cards, now).single().sampleFront)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:allTests`
Expected: FAIL — unresolved reference `toUnitSummaries` / `UnitSummary`.

- [ ] **Step 3: Write minimal implementation**

Create `composeApp/src/commonMain/kotlin/com/mochi/library/LibraryStore.kt`:

```kotlin
package com.mochi.library

import com.mochi.db.Flashcard

/** How many words make up one study unit. */
const val UNIT_SIZE = 50

/**
 * A single row in the Library grid. Units are derived from frequency rank (no schema change):
 * unit N holds the cards ranked [N*UNIT_SIZE, N*UNIT_SIZE + UNIT_SIZE).
 */
data class UnitSummary(
    val unitId: Int,
    val learnedCount: Int,
    val totalCount: Int,
    val dueCount: Int,
    val sampleFront: String,
)

/**
 * Groups frequency-ordered [cards] into units of [UNIT_SIZE].
 * - learnedCount = cards with next_review != null (matches the "words learned" stat).
 * - dueCount = cards due for review now (next_review <= now); brand-new cards are not counted.
 *
 * Pure and DB-free so it can be unit-tested. [cards] MUST already be ordered by frequency ASC.
 */
fun toUnitSummaries(cards: List<Flashcard>, now: Long): List<UnitSummary> =
    cards.chunked(UNIT_SIZE).mapIndexed { index, chunk ->
        UnitSummary(
            unitId = index,
            learnedCount = chunk.count { it.next_review != null },
            totalCount = chunk.size,
            dueCount = chunk.count { it.next_review != null && it.next_review <= now },
            sampleFront = chunk.first().front,
        )
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :composeApp:allTests`
Expected: PASS (all three new tests green).

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/library/LibraryStore.kt composeApp/src/commonTest/kotlin/com/mochi/library/LibraryStoreTest.kt
git commit -m "feat: derive study units of 50 from frequency rank"
```

---

### Task 3: Reactive `units()` Flow on `LibraryStore`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/library/LibraryStore.kt`

- [ ] **Step 1: Add the reactive class**

Append to `LibraryStore.kt` (add imports at the top, sorted):

```kotlin
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.mochi.db.AppDatabase
import com.mochi.util.nowMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
```

```kotlin
/** Reactive read model for the Library grid. Re-emits whenever card progress changes. */
class LibraryStore(private val db: AppDatabase) {
    /** All units, recomputed on every card change (selectAll is ordered by frequency ASC). */
    fun units(): Flow<List<UnitSummary>> =
        db.flashcardQueries.selectAll().asFlow()
            .mapToList(Dispatchers.Default)
            .map { toUnitSummaries(it, nowMillis()) }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/library/LibraryStore.kt
git commit -m "feat: expose reactive units() flow for the library"
```

---

### Task 4: `LibraryViewModel`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/library/LibraryViewModel.kt`

- [ ] **Step 1: Implement (mirrors StatsViewModel's stateIn pattern)**

Create `composeApp/src/commonMain/kotlin/com/mochi/library/LibraryViewModel.kt`:

```kotlin
package com.mochi.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Exposes the Library grid as a self-refreshing StateFlow. */
class LibraryViewModel(store: LibraryStore) : ViewModel() {
    val units: StateFlow<List<UnitSummary>> =
        store.units().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/library/LibraryViewModel.kt
git commit -m "feat: add LibraryViewModel exposing units StateFlow"
```

---

## Phase 2 — Per-unit sessions + session streak

### Task 5: Add `cardsInUnit()` to `ReviewDeck`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/data/DeckRepository.kt`

- [ ] **Step 1: Add the interface method**

In `DeckRepository.kt`, add to the `ReviewDeck` interface (after `fun allCards(): List<Flashcard>`):

```kotlin
    /** The 50 cards of the given study unit, by frequency rank (may be fewer in the last unit). */
    fun cardsInUnit(unitId: Int): List<Flashcard>
```

- [ ] **Step 2: Implement it in `DeckRepository`**

Add the import at the top (sorted): `import com.mochi.library.UNIT_SIZE`
Add the override (after `allCards()`):

```kotlin
    override fun cardsInUnit(unitId: Int): List<Flashcard> =
        db.flashcardQueries.selectAll().executeAsList()
            .drop(unitId * UNIT_SIZE)
            .take(UNIT_SIZE)
```

- [ ] **Step 3: Verify it compiles (expect the test fake to now be incomplete)**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL` for main. (Tests won't compile yet — fixed in Task 6.)

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/data/DeckRepository.kt
git commit -m "feat: add cardsInUnit to the deck repository"
```

---

### Task 6: Rework `ReviewUiState` + `ReviewViewModel` for per-unit sessions & streak

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewUiState.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewViewModel.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/mochi/review/ReviewViewModelTest.kt`

- [ ] **Step 1: Update the failing tests first**

Replace the whole body of `ReviewViewModelTest.kt` with the version below. Changes: `FakeDeck` implements `cardsInUnit`; all cards are treated as unit 0; `startSession()` → `openUnit(0)`; new tests cover the session streak milestone and per-unit practice fallback.

```kotlin
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
        // 11 new cards so we can reach a streak of 10 without a miss.
        val deck = FakeDeck(List(11) { card(it.toLong(), isNew = true) })
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
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:allTests`
Expected: FAIL — unresolved references `ReviewUiState.Idle`, `openUnit`, `sessionStreak`, `streakMilestone`.

- [ ] **Step 3: Update `ReviewUiState`**

Replace `ReviewUiState.kt` with:

```kotlin
package com.mochi.review

import com.mochi.db.Flashcard
import com.mochi.ui.SessionStats

/** Everything the review screens need to render, as a single state machine. */
sealed interface ReviewUiState {
    data object Loading : ReviewUiState

    /** Not in a session — the Library grid is shown instead. */
    data object Idle : ReviewUiState

    /**
     * An active card. [sessionStreak] is the run of consecutive correct answers this session;
     * [streakMilestone] is non-null (e.g. 10) only on the emission where the streak just crossed
     * a multiple of 10, so the UI fires confetti exactly once.
     */
    data class Reviewing(
        val card: Flashcard,
        val position: Int,
        val total: Int,
        val sessionStreak: Int,
        val streakMilestone: Int?,
    ) : ReviewUiState

    data class Complete(val stats: SessionStats) : ReviewUiState
}
```

- [ ] **Step 4: Update `ReviewViewModel`**

Replace `ReviewViewModel.kt` with:

```kotlin
package com.mochi.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochi.audio.AudioPlayer
import com.mochi.data.ReviewDeck
import com.mochi.db.Flashcard
import com.mochi.resources.Res
import com.mochi.settings.NewCardLimitSource
import com.mochi.stats.NewCardCounter
import com.mochi.ui.SessionStats
import com.mochi.util.nowMillis
import com.mochi.util.todayEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Cards drilled in a fallback practice session (schedule ignored).
private const val PRACTICE_SIZE = 20

// Confetti fires each time the session streak crosses a multiple of this.
private const val STREAK_MILESTONE = 10

/**
 * Owns the review flow as a state machine: Idle -> Reviewing -> Complete -> Idle.
 * A session is scoped to one study unit: that unit's due reviews plus new cards up to the
 * remaining GLOBAL daily new-card limit. Missed cards requeue (relearning). If a unit has
 * nothing scheduled, it falls back to a practice drill of that unit. Dependencies are
 * interfaces so the flow is unit-testable.
 */
@OptIn(ExperimentalResourceApi::class)
class ReviewViewModel(
    private val deck: ReviewDeck,
    private val newCardCounter: NewCardCounter,
    private val limitSource: NewCardLimitSource,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {

    private val _state = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    private var session: List<Flashcard> = emptyList()
    private var index = 0
    private var reviewed = 0
    private var correct = 0
    private var sessionStreak = 0

    private var currentUnitId = 0
    private var sessionNewLimit = limitSource.newCardLimit()
    private var practiceMode = false

    init {
        viewModelScope.launch {
            deck.ensureSeeded()
            _state.value = ReviewUiState.Idle
        }
    }

    /**
     * Opens a study session for [unitId]: its scheduled queue if any, otherwise a practice drill
     * of that unit (so tapping a fully-learned unit still does something satisfying).
     */
    fun openUnit(unitId: Int) {
        currentUnitId = unitId
        sessionNewLimit = limitSource.newCardLimit()
        val queue = buildUnitQueue(unitId)
        if (queue.isEmpty()) {
            startUnitPractice(unitId)
        } else {
            practiceMode = false
            beginSession(queue)
        }
    }

    private fun startUnitPractice(unitId: Int) {
        practiceMode = true
        val queue = deck.cardsInUnit(unitId).shuffled().take(PRACTICE_SIZE)
        if (queue.isEmpty()) {
            _state.value = ReviewUiState.Idle
        } else {
            beginSession(queue)
        }
    }

    private fun beginSession(queue: List<Flashcard>) {
        session = queue
        index = 0
        reviewed = 0
        correct = 0
        sessionStreak = 0
        emitReviewing(milestone = null)
    }

    fun answer(isCorrect: Boolean) {
        val card = session.getOrNull(index) ?: return
        val updated = if (practiceMode) {
            deck.recordPractice(card, isCorrect)
            card
        } else {
            deck.recordAnswer(card, isCorrect)
        }
        reviewed++
        var milestone: Int? = null
        if (isCorrect) {
            correct++
            sessionStreak++
            if (sessionStreak % STREAK_MILESTONE == 0) milestone = sessionStreak
        } else {
            sessionStreak = 0
            session = session + updated // relearning: requeue at the end
        }
        if (index < session.lastIndex) {
            index++
            emitReviewing(milestone)
        } else {
            _state.value = ReviewUiState.Complete(SessionStats(reviewed = reviewed, correct = correct))
        }
    }

    fun playCurrentAudio() {
        val name = session.getOrNull(index)?.audio
        if (name.isNullOrBlank()) return
        viewModelScope.launch {
            runCatching { audioPlayer.play(Res.readBytes("files/audio/$name")) }
        }
    }

    /** Ends the session (from the summary's "Done") and returns to the Library. */
    fun finish() {
        _state.value = ReviewUiState.Idle
    }

    /** Called when the Review tab becomes visible: rebuild a running session if the limit changed. */
    fun onEnterReviewTab() {
        val current = state.value
        if (current is ReviewUiState.Reviewing && limitSource.newCardLimit() != sessionNewLimit) {
            openUnit(currentUnitId)
        }
    }

    /** The unit's queue: its due reviews first, then its new cards up to the remaining daily limit. */
    private fun buildUnitQueue(unitId: Int): List<Flashcard> {
        val limit = limitSource.newCardLimit()
        val remainingNew = if (limit <= 0) {
            Int.MAX_VALUE
        } else {
            (limit - newCardCounter.newOnDay(todayEpochDay()).toInt()).coerceAtLeast(0)
        }
        val now = nowMillis()
        val due = deck.cardsInUnit(unitId).filter { it.next_review == null || it.next_review <= now }
        val (newCards, reviews) = due.partition { it.next_review == null }
        return reviews + newCards.take(remainingNew)
    }

    private fun emitReviewing(milestone: Int?) {
        _state.value = ReviewUiState.Reviewing(
            card = session[index],
            position = index + 1,
            total = session.size,
            sessionStreak = sessionStreak,
            streakMilestone = milestone,
        )
    }

    override fun onCleared() {
        audioPlayer.release()
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :composeApp:allTests`
Expected: PASS (all `ReviewViewModelTest` cases green).

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/review/ReviewUiState.kt composeApp/src/commonMain/kotlin/com/mochi/review/ReviewViewModel.kt composeApp/src/commonTest/kotlin/com/mochi/review/ReviewViewModelTest.kt
git commit -m "feat: scope review sessions to a unit and track session streak"
```

---

## Phase 3 — Library screen wiring (no shared transition yet)

### Task 7: Build `LibraryScreen` and delete `HomeScreen`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/LibraryScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/App.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/HomeScreen.kt`

- [ ] **Step 1: Create `LibraryScreen` (presentation-only; plain circular progress for now)**

Create `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/LibraryScreen.kt`:

```kotlin
package com.mochi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mochi.library.UnitSummary
import com.mochi.ui.theme.LocalJapaneseFont

/**
 * The Library: a grid of study units. Each unit shows its number, a sample kanji, a progress
 * label (learned/total) and a "due" badge. Tapping a unit opens its study session.
 */
@Composable
fun LibraryScreen(
    units: List<UnitSummary>,
    onOpenUnit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Library", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "${units.size} units • tap to study",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(units, key = { it.unitId }) { unit ->
                UnitCard(unit = unit, onClick = { onOpenUnit(unit.unitId) })
            }
        }
    }
}

@Composable
private fun UnitCard(unit: UnitSummary, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = unitColor(unit.unitId),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Unit ${unit.unitId + 1}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart),
            )
            if (unit.dueCount > 0) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        text = "${unit.dueCount} due",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                text = unit.sampleFront,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = LocalJapaneseFont.current,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                text = "${unit.learnedCount}/${unit.totalCount} learned",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

/** A distinct pastel per unit (cycled), tinted onto the surface — keeps the Mochi palette. */
@Composable
private fun unitColor(unitId: Int): androidx.compose.ui.graphics.Color {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(
        scheme.surfaceVariant,
        scheme.secondaryContainer,
        scheme.tertiaryContainer,
        scheme.primaryContainer,
    )
    return palette[unitId % palette.size]
}
```

- [ ] **Step 2: Delete `HomeScreen.kt` and wire the Library into `App.kt`**

Delete the file:

```bash
git rm composeApp/src/commonMain/kotlin/com/mochi/ui/screens/HomeScreen.kt
```

In `App.kt`:
1. Add imports (sorted): `import com.mochi.library.LibraryStore`, `import com.mochi.library.LibraryViewModel`, `import com.mochi.ui.screens.LibraryScreen`. Remove `import com.mochi.ui.screens.HomeScreen`.
2. After `val learningStore = remember { LearningStore(db) }` add:

```kotlin
    val libraryStore = remember { LibraryStore(db) }
```

3. After `val learningViewModel = viewModel { ... }` add:

```kotlin
    val libraryViewModel = viewModel { LibraryViewModel(libraryStore) }
```

4. After `val learningWords by learningViewModel.words.collectAsState()` add:

```kotlin
    val units by libraryViewModel.units.collectAsState()
```

5. Replace the whole `ReviewContent(...)` function with:

```kotlin
@Composable
private fun ReviewContent(
    state: ReviewUiState,
    units: List<com.mochi.library.UnitSummary>,
    viewModel: ReviewViewModel,
) {
    when (val s = state) {
        ReviewUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        ReviewUiState.Idle -> LibraryScreen(
            units = units,
            onOpenUnit = viewModel::openUnit,
        )

        is ReviewUiState.Reviewing -> FlashcardScreen(
            card = s.card,
            position = s.position,
            total = s.total,
            onAnswer = viewModel::answer,
            onPlayAudio = viewModel::playCurrentAudio,
        )

        is ReviewUiState.Complete -> SessionCompleteScreen(
            stats = s.stats,
            onContinue = { viewModel.finish() },
            onDone = viewModel::finish,
        )
    }
}
```

6. Update the call site `Tab.REVIEW -> ReviewContent(reviewState, reviewViewModel)` to:

```kotlin
                        Tab.REVIEW -> ReviewContent(reviewState, units, reviewViewModel)
```

> Note: `onContinue` now returns to the Library (`finish()`) instead of immediately starting a new global session, because sessions are unit-scoped. The user picks the next unit from the grid.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run the app and verify the Library appears**

Run the Android app (Android Studio, or `./gradlew :androidApp:installDebug` then launch). 
Expected: Review tab shows a 2-column grid of 30 unit cards; "due" badges show on units with due cards; tapping a unit opens a flashcard session; finishing returns to the grid.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: replace Home with a unit Library grid on the Review tab"
```

---

## Phase 4 — Flip spring, dynamic shadow, and physics swipe

### Task 8: Extract `Modifier.pressBounce()` and reuse it in `BouncyButton`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/PressBounce.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/components/BouncyButton.kt`

- [ ] **Step 1: Create the modifier**

Create `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/PressBounce.kt`:

```kotlin
package com.mochi.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/** Squishes the element while [interactionSource] reports a press, springing back on release. */
@Composable
fun Modifier.pressBounce(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.9f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "pressBounce",
    )
    return this.scale(scale)
}
```

- [ ] **Step 2: Use it in `BouncyButton`**

In `BouncyButton.kt`, remove the local `animateFloatAsState`/`scale` block and the now-unused imports (`Spring`, `animateFloatAsState`, `spring`, `getValue`, `scale`), add `import com.mochi.ui.motion.pressBounce`, and change the `Button`'s modifier line to:

```kotlin
        modifier = modifier.pressBounce(interactionSource),
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/motion/PressBounce.kt composeApp/src/commonMain/kotlin/com/mochi/ui/components/BouncyButton.kt
git commit -m "refactor: extract reusable Modifier.pressBounce"
```

---

### Task 9: `Modifier.swipeToDismissCard()`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/SwipeToDismissCard.kt`

- [ ] **Step 1: Create the modifier**

Create `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/SwipeToDismissCard.kt`:

```kotlin
package com.mochi.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Fraction of card width the drag must pass to count as a dismissal. */
private const val DISMISS_THRESHOLD = 0.35f

/** Resistance applied to drags while [enabled] is false (card not yet flipped). */
private const val LOCKED_RESISTANCE = 0.2f

/** Max tilt (degrees) at full drag, and how far off-screen a dismissal throws the card. */
private const val MAX_TILT = 12f
private const val THROW_DISTANCE = 1.6f

/**
 * Drag-to-rate gesture. When [enabled], dragging past [DISMISS_THRESHOLD] of the width throws the
 * card off-screen and calls [onDismiss] (right = true). Below the threshold, or while disabled
 * (drag is damped so it barely moves), the card springs back to center. [onDrag] reports the
 * horizontal progress in [-1f, 1f] so callers can render an intent overlay.
 */
fun Modifier.swipeToDismissCard(
    enabled: Boolean,
    onDismiss: (right: Boolean) -> Unit,
    onDrag: (progress: Float) -> Unit = {},
): Modifier = composed {
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var widthPx by remember { mutableStateOf(1f) }
    val enabledState by rememberUpdatedState(enabled)
    val onDismissState by rememberUpdatedState(onDismiss)
    val onDragState by rememberUpdatedState(onDrag)

    // Keep the overlay progress in sync with the current offset.
    LaunchedEffect(offset.value, widthPx) {
        onDragState((offset.value.x / widthPx).coerceIn(-1f, 1f))
    }

    this
        .pointerInput(Unit) {
            widthPx = size.width.toFloat().coerceAtLeast(1f)
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    val factor = if (enabledState) 1f else LOCKED_RESISTANCE
                    launch {
                        offset.snapTo(offset.value + Offset(dragAmount.x * factor, dragAmount.y * factor))
                    }
                },
                onDragEnd = {
                    val passed = enabledState && abs(offset.value.x) > widthPx * DISMISS_THRESHOLD
                    if (passed) {
                        val right = offset.value.x > 0
                        launch {
                            offset.animateTo(
                                targetValue = Offset(widthPx * THROW_DISTANCE * if (right) 1f else -1f, offset.value.y),
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                            )
                            onDismissState(right)
                        }
                    } else {
                        launch {
                            offset.animateTo(
                                targetValue = Offset.Zero,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            )
                        }
                    }
                },
            )
        }
        .graphicsLayer {
            translationX = offset.value.x
            translationY = offset.value.y
            rotationZ = (offset.value.x / widthPx).coerceIn(-1f, 1f) * MAX_TILT
        }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/motion/SwipeToDismissCard.kt
git commit -m "feat: add swipeToDismissCard physics gesture modifier"
```

---

### Task 10: Spring flip + dynamic shadow in `FlipCard`, expose flip state

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/components/FlipCard.kt`

- [ ] **Step 1: Make the flip spring-driven, add a mid-flip shadow, and hoist `isFlipped`**

In `FlipCard.kt`:
1. Change the signature to hoist flip state so the screen can gate the swipe on it:

```kotlin
fun FlipCard(
    front: String,
    reading: String,
    meaning: String,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

2. Remove the internal `var isFlipped by remember { … }`.
3. Replace the `rotation` animation spec (currently `tween(500)`) with a spring:

```kotlin
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "flipRotation",
    )
```

4. In the `graphicsLayer` block on the `Card`, add a dynamic shadow that peaks at 90° (add `import kotlin.math.abs` and `import androidx.compose.ui.unit.dp` is already present):

```kotlin
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
                // Lift the card as it turns; strongest edge-on at 90°.
                val lift = 1f - abs(rotation - 90f) / 90f // 0 at faces, 1 at 90°
                shadowElevation = 4.dp.toPx() + lift * 16.dp.toPx()
                shape = RoundedCornerShape(32.dp)
                clip = false
            }
```

5. Change the `.clickable { … }` to call the hoisted callback:

```kotlin
            .clickable(interactionSource = interaction, indication = null) { onFlip() },
```

- [ ] **Step 2: Verify it compiles (FlashcardScreen call site will break — fixed in Task 11)**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: FAIL at `FlashcardScreen.kt` (missing `isFlipped`/`onFlip` args). This is expected; proceed to Task 11.

- [ ] **Step 3: (No commit yet — commit together with Task 11 so the build stays green.)**

---

### Task 11: Wire swipe + flip into `FlashcardScreen` with an intent overlay

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt`

- [ ] **Step 1: Manage flip + drag state and gate the swipe on flip**

In `FlashcardScreen.kt`, replace the `AnimatedContent(targetState = card, …) { current -> FlipCard(...) }` block with the following, and add the needed imports (sorted): `import androidx.compose.foundation.layout.Box`, `import androidx.compose.runtime.getValue`, `import androidx.compose.runtime.mutableFloatStateOf`, `import androidx.compose.runtime.mutableStateOf`, `import androidx.compose.runtime.remember`, `import androidx.compose.runtime.setValue`, `import androidx.compose.ui.graphics.graphicsLayer`, `import com.mochi.ui.components.SwipeIntentOverlay`, `import com.mochi.ui.motion.swipeToDismissCard`.

```kotlin
            AnimatedContent(
                targetState = card,
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -width } + fadeOut())
                },
                label = "card",
            ) { current ->
                var isFlipped by remember(current.id) { mutableStateOf(false) }
                var dragProgress by remember(current.id) { mutableFloatStateOf(0f) }
                Box {
                    FlipCard(
                        front = current.front,
                        reading = current.reading,
                        meaning = current.back,
                        isFlipped = isFlipped,
                        onFlip = { isFlipped = !isFlipped },
                        modifier = Modifier.swipeToDismissCard(
                            enabled = isFlipped,
                            onDismiss = { right -> onAnswer(right) },
                            onDrag = { dragProgress = it },
                        ),
                    )
                    SwipeIntentOverlay(progress = if (isFlipped) dragProgress else 0f)
                }
            }
```

- [ ] **Step 2: Add the `SwipeIntentOverlay` composable**

Create it in `composeApp/src/commonMain/kotlin/com/mochi/ui/components/SwipeIntentOverlay.kt`:

```kotlin
package com.mochi.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Fades a stamp over the card while it is dragged: green "知ってる ✓" to the right,
 * amber "まだ" to the left. [progress] is the horizontal drag in [-1f, 1f].
 */
@Composable
fun SwipeIntentOverlay(progress: Float) {
    if (abs(progress) < 0.02f) return
    val right = progress > 0
    val alignment = if (right) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (right) Color(0xFF2E7D32) else Color(0xFFF9A825)
    val label = if (right) "知ってる ✓" else "まだ"
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = alignment) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.alpha(abs(progress).coerceIn(0f, 1f)),
        ) {
            Text(text = label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run the app and verify the interaction**

Launch the Android app, open a unit.
Expected: before flipping, dragging the card barely moves it and it springs back; tapping flips with a bouncy spring and a visible mid-flip shadow "lift"; after flipping, dragging shows the green/amber stamp, and a drag past ~1/3 width throws the card off-screen and advances (right = "I knew it", left = "Still learning"); the on-screen buttons still work.

- [ ] **Step 5: Commit (Tasks 10 + 11 together)**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/components/FlipCard.kt composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt composeApp/src/commonMain/kotlin/com/mochi/ui/components/SwipeIntentOverlay.kt
git commit -m "feat: spring flip with dynamic shadow and physics swipe-to-rate"
```

---

## Phase 5 — Confetti, liquid progress, animated counters

### Task 12: Confetti particle system (`ConfettiBurst`) with a tested `advance()`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/ConfettiBurst.kt`
- Test: `composeApp/src/commonTest/kotlin/com/mochi/ui/motion/ConfettiBurstTest.kt`

- [ ] **Step 1: Write the failing test for the pure physics step**

Create `composeApp/src/commonTest/kotlin/com/mochi/ui/motion/ConfettiBurstTest.kt`:

```kotlin
package com.mochi.ui.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfettiBurstTest {

    private fun particle(pos: Offset, vel: Offset) =
        Particle(position = pos, velocity = vel, color = Color.Red, size = 4f, rotation = 0f)

    @Test
    fun advanceMovesByVelocityScaledByDeltaAndAddsGravity() {
        val p = particle(Offset(0f, 0f), Offset(10f, 0f))
        // dt = 1.0 so math is easy to assert; gravity pulls y down (positive).
        val next = p.advance(dtSeconds = 1f, gravity = 100f)
        assertEquals(10f, next.position.x)
        assertEquals(0f, next.position.y) // velocity.y was 0 at the start of this step
        assertEquals(100f, next.velocity.y) // gravity accelerated it
    }

    @Test
    fun advanceIntegratesGravityOverTwoSteps() {
        var p = particle(Offset(0f, 0f), Offset(0f, 0f))
        p = p.advance(dtSeconds = 1f, gravity = 100f) // v.y -> 100
        p = p.advance(dtSeconds = 1f, gravity = 100f) // pos.y += 100, v.y -> 200
        assertEquals(100f, p.position.y)
        assertEquals(200f, p.velocity.y)
    }

    @Test
    fun rotationAdvancesEachStep() {
        val p = particle(Offset.Zero, Offset.Zero).copy(spin = 90f)
        val next = p.advance(dtSeconds = 1f, gravity = 0f)
        assertTrue(next.rotation == 90f)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :composeApp:allTests`
Expected: FAIL — unresolved `Particle` / `advance` / `spin`.

- [ ] **Step 3: Implement `ConfettiBurst.kt`**

Create `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/ConfettiBurst.kt`:

```kotlin
package com.mochi.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/** One confetto. Pure data; [advance] returns the next frame's state (no side effects). */
data class Particle(
    val position: Offset,
    val velocity: Offset,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val spin: Float = 0f,
    val ageSeconds: Float = 0f,
) {
    /** Semi-implicit Euler step: gravity accelerates velocity, then velocity moves position. */
    fun advance(dtSeconds: Float, gravity: Float): Particle {
        val newVelocity = Offset(velocity.x, velocity.y + gravity * dtSeconds)
        return copy(
            position = Offset(position.x + velocity.x * dtSeconds, position.y + velocity.y * dtSeconds),
            velocity = newVelocity,
            rotation = rotation + spin * dtSeconds,
            ageSeconds = ageSeconds + dtSeconds,
        )
    }
}

private const val PARTICLE_COUNT = 60
private const val GRAVITY = 900f
private const val LIFETIME_SECONDS = 1.4f

// Vibrant palette — the ONE place bright colors are used (keeps the Mochi identity elsewhere).
private val ConfettiColors = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF6BCB77),
    Color(0xFF4D96FF), Color(0xFFB983FF), Color(0xFFFF9F45),
)

/**
 * Fires a burst of confetti from the center whenever [trigger] changes to a new non-null value.
 * Pure Canvas, no assets. Runs a frame loop until all particles expire, then draws nothing.
 */
@Composable
fun ConfettiBurst(trigger: Any?, modifier: Modifier = Modifier) {
    var particles by remember { mutableStateOf(emptyList<Particle>()) }

    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        // Seed particles in a radial fan. Angle/speed vary by index (no RNG needed for determinism).
        particles = List(PARTICLE_COUNT) { i ->
            val angle = (i.toFloat() / PARTICLE_COUNT) * 2f * kotlin.math.PI.toFloat()
            val speed = 500f + (i % 7) * 90f
            Particle(
                position = Offset.Zero,
                velocity = Offset(cos(angle) * speed, sin(angle) * speed - 300f),
                color = ConfettiColors[i % ConfettiColors.size],
                size = 8f + (i % 4) * 3f,
                rotation = angle,
                spin = 240f + (i % 5) * 60f,
            )
        }
        var last = withFrameNanos { it }
        while (particles.any { it.ageSeconds < LIFETIME_SECONDS }) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            last = now
            particles = particles.map { it.advance(dt, GRAVITY) }
        }
        particles = emptyList()
    }

    Canvas(modifier) {
        val origin = Offset(size.width / 2f, size.height / 2f)
        particles.forEach { p ->
            val alpha = (1f - p.ageSeconds / LIFETIME_SECONDS).coerceIn(0f, 1f)
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size,
                center = origin + p.position,
            )
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :composeApp:allTests`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/motion/ConfettiBurst.kt composeApp/src/commonTest/kotlin/com/mochi/ui/motion/ConfettiBurstTest.kt
git commit -m "feat: add Canvas confetti particle system"
```

---

### Task 13: Fire confetti on session complete and on streak milestones

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/SessionCompleteScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/App.kt`

- [ ] **Step 1: Confetti on the completion screen**

In `SessionCompleteScreen.kt`, wrap the content in a `Box` and overlay the burst. Add imports (sorted): `import androidx.compose.foundation.layout.Box`, `import com.mochi.ui.motion.ConfettiBurst`. Change the root from `Column(...) { … }` to:

```kotlin
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ...existing SuccessAnimation + texts + buttons unchanged...
        }
        ConfettiBurst(trigger = Unit, modifier = Modifier.fillMaxSize())
    }
```

(Keep the existing children inside the inner `Column` exactly as they are.)

- [ ] **Step 2: Confetti on streak milestones during review**

`FlashcardScreen` needs the milestone value. Add a parameter and overlay. In `FlashcardScreen.kt`:
1. Add `streakMilestone: Int?,` to the parameter list (after `total: Int,`).
2. Add imports (sorted): `import com.mochi.ui.motion.ConfettiBurst`.
3. Wrap the outer `Column` in a `Box` and add the burst at the end:

```kotlin
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ...existing progress row + card group unchanged...
        }
        ConfettiBurst(trigger = streakMilestone, modifier = Modifier.fillMaxSize())
    }
```

(`ConfettiBurst` only fires when `trigger` changes to a non-null value, so a null milestone draws nothing.)

- [ ] **Step 3: Pass the milestone from `App.kt`**

In `App.kt`, update the `is ReviewUiState.Reviewing ->` branch of `ReviewContent` to pass it:

```kotlin
        is ReviewUiState.Reviewing -> FlashcardScreen(
            card = s.card,
            position = s.position,
            total = s.total,
            streakMilestone = s.streakMilestone,
            onAnswer = viewModel::answer,
            onPlayAudio = viewModel::playCurrentAudio,
        )
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run the app and verify**

Launch, open a unit, answer 10 correct in a row → a confetti burst pops over the card; finish a session → burst on the completion screen.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/screens/SessionCompleteScreen.kt composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt composeApp/src/commonMain/kotlin/com/mochi/App.kt
git commit -m "feat: celebrate session completion and 10-streaks with confetti"
```

---

### Task 14: `LiquidProgress` sine-wave fill

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/LiquidProgress.kt`

- [ ] **Step 1: Implement the composable**

Create `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/LiquidProgress.kt`:

```kotlin
package com.mochi.ui.motion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.sin

/**
 * A liquid fill: an animated sine wave rising to [progress] (0f..1f) of the height.
 * Draw it inside a clipped/rounded container to get a "liquid in a jar" look.
 */
@Composable
fun LiquidProgress(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    waveHeight: Float = 8f,
) {
    val transition = rememberInfiniteTransition(label = "liquid")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )

    Canvas(modifier) {
        val fill = progress.coerceIn(0f, 1f)
        val baseY = size.height * (1f - fill)
        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(0f, baseY)
            val steps = 24
            for (i in 0..steps) {
                val x = size.width * i / steps
                val y = baseY + sin(phase + i.toFloat() / steps * 2f * PI.toFloat()) * waveHeight
                lineTo(x, y)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(path = path, color = color)
    }
}
```

- [ ] **Step 2: Use it for each unit card's progress in `LibraryScreen`**

In `LibraryScreen.kt`, inside `UnitCard`'s `Box`, add a thin liquid bar at the bottom behind the label. Add imports (sorted): `import androidx.compose.foundation.clip` is not needed; add `import androidx.compose.foundation.layout.height`, `import androidx.compose.foundation.layout.fillMaxWidth` (already present), `import androidx.compose.ui.draw.clip`, `import androidx.compose.foundation.shape.RoundedCornerShape` (already present), `import com.mochi.ui.motion.LiquidProgress`. Just above the `${learnedCount}/${totalCount} learned` text, add:

```kotlin
            LiquidProgress(
                progress = unit.learnedCount.toFloat() / unit.totalCount,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run the app and verify**

Launch → each unit card shows a gently rippling liquid fill proportional to learned/50.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/motion/LiquidProgress.kt composeApp/src/commonMain/kotlin/com/mochi/ui/screens/LibraryScreen.kt
git commit -m "feat: add liquid sine-wave progress to unit cards"
```

---

### Task 15: `AnimatedCounter` odometer + animated Stats numbers + session-streak HUD

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/AnimatedCounter.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/StatsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt`

- [ ] **Step 1: Implement `AnimatedCounter`**

Create `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/AnimatedCounter.kt`:

```kotlin
package com.mochi.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/**
 * Odometer-style number: when [value] increases the new number slides up from below; when it
 * decreases it slides down. Renders [value] with [prefix]/[suffix] around it (e.g. "🔥 " / " days").
 */
@Composable
fun AnimatedCounter(
    value: Int,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    style: TextStyle = LocalTextStyle.current,
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val goingUp = targetState > initialState
            val enter = slideInVertically { h -> if (goingUp) h else -h } + fadeIn()
            val exit = slideOutVertically { h -> if (goingUp) -h else h } + fadeOut()
            (enter togetherWith exit).using(SizeTransform(clip = false))
        },
        modifier = modifier,
        label = "animatedCounter",
    ) { shown ->
        Text(text = "$prefix$shown$suffix", style = style)
    }
}
```

- [ ] **Step 2: Use it in `StatsScreen` for streak, reviews and learned**

In `StatsScreen.kt`, `StatCard` currently takes a `value: String`. Add an overload that animates an integer. Replace the three `StatCard(...)` calls in `StatsScreen` with animated versions and add an `AnimatedStatCard`:

Add imports (sorted): `import com.mochi.ui.motion.AnimatedCounter`.

Change the three calls:

```kotlin
        val days = if (stats.streak == 1) "day" else "days"
        AnimatedStatCard(label = "Current streak", value = stats.streak, prefix = "🔥 ", suffix = " $days")
        AnimatedStatCard(label = "Reviews today", value = stats.reviewsToday.toInt())
        AnimatedStatCard(label = "Words learned", value = stats.totalLearned.toInt())
```

Add this composable next to `StatCard` (reuses its Surface styling):

```kotlin
@Composable
private fun AnimatedStatCard(label: String, value: Int, prefix: String = "", suffix: String = "") {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            AnimatedCounter(
                value = value,
                prefix = prefix,
                suffix = suffix,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}
```

> Verify the types of `stats.reviewsToday` / `stats.totalLearned` in `StatsUiState`; if they are `Long`, `.toInt()` (as written) is correct. If any is already `Int`, drop the `.toInt()`. The original `StatCard(label, value: String)` may now be unused — if so, delete it to keep detekt clean.

- [ ] **Step 3: Add a session-streak HUD to `FlashcardScreen`**

In `FlashcardScreen.kt`, add a `sessionStreak: Int,` parameter (after `streakMilestone: Int?,`). In the top progress `Row`, show the streak next to the counter when > 0. Add import `import com.mochi.ui.motion.AnimatedCounter`. After the existing `Text("$position/$total", …)` inside the `Row`, add:

```kotlin
            if (sessionStreak > 0) {
                Spacer(Modifier.width(12.dp))
                AnimatedCounter(
                    value = sessionStreak,
                    prefix = "🔥 ",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
```

Then in `App.kt`, pass `sessionStreak = s.sessionStreak,` in the `FlashcardScreen(...)` call (in the `Reviewing` branch).

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`. (If the compiler reports `reviewsToday`/`totalLearned` type mismatches, adjust `.toInt()` per the note in Step 2.)

- [ ] **Step 5: Run the app and verify**

Stats numbers roll up like an odometer when they change; during a session the 🔥 streak counter ticks up per correct answer and resets on a miss.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/motion/AnimatedCounter.kt composeApp/src/commonMain/kotlin/com/mochi/ui/screens/StatsScreen.kt composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt composeApp/src/commonMain/kotlin/com/mochi/App.kt
git commit -m "feat: odometer counters for stats and session streak"
```

---

## Phase 6 — Shared element transition (Library ⇄ session)

### Task 16: Wrap the Review tab in `SharedTransitionLayout` and share the unit card

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/App.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/LibraryScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt`

> **Approach:** `SharedTransitionLayout` + an inner `AnimatedContent` that switches on whether we're in the Library (`Idle`) or a session. The tapped `unitId` drives a shared key `"unit-<id>"`. `SharedTransitionScope`/`AnimatedVisibilityScope` are passed down to the two screens so they can tag the shared bounds. This is opt-in experimental API (`@OptIn(ExperimentalSharedTransitionApi::class)`).

- [ ] **Step 1: Restructure `ReviewContent` in `App.kt`**

Replace `ReviewContent` with a `SharedTransitionLayout` version. Add imports (sorted):
`import androidx.compose.animation.ExperimentalSharedTransitionApi`,
`import androidx.compose.animation.SharedTransitionLayout`,
`import androidx.compose.animation.AnimatedContent`,
`import androidx.compose.animation.fadeIn`,
`import androidx.compose.animation.fadeOut`,
`import androidx.compose.animation.togetherWith`.

```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ReviewContent(
    state: ReviewUiState,
    units: List<com.mochi.library.UnitSummary>,
    viewModel: ReviewViewModel,
) {
    SharedTransitionLayout(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it::class }, // animate only when the state TYPE changes
            label = "reviewShared",
        ) { s ->
            when (s) {
                ReviewUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                ReviewUiState.Idle -> LibraryScreen(
                    units = units,
                    onOpenUnit = viewModel::openUnit,
                    sharedScope = this@SharedTransitionLayout,
                    animatedScope = this@AnimatedContent,
                )

                is ReviewUiState.Reviewing -> FlashcardScreen(
                    card = s.card,
                    position = s.position,
                    total = s.total,
                    streakMilestone = s.streakMilestone,
                    sessionStreak = s.sessionStreak,
                    onAnswer = viewModel::answer,
                    onPlayAudio = viewModel::playCurrentAudio,
                    sharedScope = this@SharedTransitionLayout,
                    animatedScope = this@AnimatedContent,
                    sharedKey = "unit-${'$'}{viewModel.lastOpenedUnitId}",
                )

                is ReviewUiState.Complete -> SessionCompleteScreen(
                    stats = s.stats,
                    onContinue = viewModel::finish,
                    onDone = viewModel::finish,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Expose the opened unit id from the ViewModel**

In `ReviewViewModel.kt`, expose the current unit for the shared key. Add below the private fields:

```kotlin
    /** The unit the current/most-recent session belongs to (drives the shared-element key). */
    var lastOpenedUnitId: Int = 0
        private set
```

And in `openUnit`, set it first line after the signature:

```kotlin
        lastOpenedUnitId = unitId
```

(Keep `currentUnitId` as-is; `lastOpenedUnitId` is its read-only public mirror.)

- [ ] **Step 3: Tag the shared bounds in both screens**

Add parameters to `LibraryScreen` and tag each `UnitCard`. In `LibraryScreen.kt`:
- Signature adds (before `modifier`):

```kotlin
    sharedScope: androidx.compose.animation.SharedTransitionScope,
    animatedScope: androidx.compose.animation.AnimatedVisibilityScope,
```

- Add `@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)` above the `fun LibraryScreen`.
- Pass scopes into `UnitCard(unit, onClick, sharedScope, animatedScope)` and add the same two params there, then on the `UnitCard`'s root `Surface` modifier chain add (using `with(sharedScope) { … }`):

```kotlin
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(
                with(sharedScope) {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "unit-${'$'}{unit.unitId}"),
                        animatedVisibilityScope = animatedScope,
                    )
                },
            )
            .clickable(onClick = onClick),
```

Add imports (sorted): `import androidx.compose.animation.ExperimentalSharedTransitionApi`.

In `FlashcardScreen.kt`:
- Add parameters (before `modifier`):

```kotlin
    sharedScope: androidx.compose.animation.SharedTransitionScope,
    animatedScope: androidx.compose.animation.AnimatedVisibilityScope,
    sharedKey: String,
```

- Add `@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)` above `fun FlashcardScreen`.
- On the outer `Box(modifier = modifier.fillMaxSize())` created in Task 13, add the matching shared bounds so the session container morphs from the unit card:

```kotlin
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                with(sharedScope) {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = sharedKey),
                        animatedVisibilityScope = animatedScope,
                    )
                },
            ),
    ) {
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`. Resolve any missing `rememberSharedContentState` import: it is a member of `SharedTransitionScope`, so it must be called inside `with(sharedScope) { … }` (as written).

- [ ] **Step 5: Run the app and verify the transition**

Launch, tap a unit card.
Expected: the tapped card visibly expands/morphs into the study session (its bounds grow to fill the screen); pressing "Done" contracts back toward the grid. The rest of the grid fades during the transition.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/App.kt composeApp/src/commonMain/kotlin/com/mochi/ui/screens/LibraryScreen.kt composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt composeApp/src/commonMain/kotlin/com/mochi/review/ReviewViewModel.kt
git commit -m "feat: shared-element transition from library unit to session"
```

---

## Phase 7 — Final verification

### Task 17: Full test + lint + iOS build check

- [ ] **Step 1: Run all tests**

Run: `./gradlew :composeApp:allTests`
Expected: `BUILD SUCCESSFUL`; `LibraryStoreTest`, `ConfettiBurstTest`, `ReviewViewModelTest` all green.

- [ ] **Step 2: Lint**

Run: `./gradlew :composeApp:ktlintCheck :composeApp:detekt`
Expected: `BUILD SUCCESSFUL`. Fix any line-length (≤120) or import-order issues reported.

- [ ] **Step 3: Android smoke test**

Launch the Android app and walk the full flow: Library grid → open a unit (shared transition) → flip (spring + shadow) → swipe to rate (physics + overlay) → hit a 10-streak (confetti) → finish (confetti) → back to grid (liquid progress updated) → Stats (odometer). Confirm the other tabs (Learning, Settings) are unaffected.

- [ ] **Step 4: iOS build check**

Open `iosApp/iosApp.xcodeproj` in Xcode and build/run on a simulator (or `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`).
Expected: builds and runs; all animations are Compose-common so they render identically (iOS audio/fonts remain deferred, unchanged by this work).

- [ ] **Step 5: Final commit (if any lint fixes were made)**

```bash
git add -A
git commit -m "chore: lint fixes for interactive animations"
```

---

## Self-review notes (author checklist — already reconciled)

- **Spec coverage:** unit model (Task 2–5), per-unit sessions + global limit (Task 6), streak milestones every 10 (Task 6, 13), 3D flip spring + dynamic shadow (Task 10), physics swipe post-flip with resistance/spring/overlay (Task 9, 11), confetti (Task 12–13), liquid progress (Task 14), animated counters (Task 15), shared-element transition (Task 16), design-system package `com.mochi.ui.motion` + state hoisting (throughout), identity preserved / vibrant colors confined to confetti (Task 12). All covered.
- **Type consistency:** `openUnit(Int)`, `UnitSummary(unitId, learnedCount, totalCount, dueCount, sampleFront)`, `toUnitSummaries(List<Flashcard>, Long)`, `Particle.advance(dtSeconds, gravity)`, `ReviewUiState.Reviewing(card, position, total, sessionStreak, streakMilestone)` are used identically across tasks.
- **Known adjustment point:** `stats.reviewsToday`/`stats.totalLearned` numeric type is confirmed at compile time in Task 15 Step 4 (`.toInt()` adjusted if needed).
- **Practice semantics:** `openUnit` falls back to a practice drill when a unit has nothing scheduled; practice never reschedules (verified by test `openingAUnitWithNothingDueFallsBackToPractice`).
</content>
