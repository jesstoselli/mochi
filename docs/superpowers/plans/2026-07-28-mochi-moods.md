# Mochi Moods Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the Mochi mascot facial expressions that react to progress — a face that grows
happier as the current unit fills (4 quartile levels incl. a `-_-` "NotHappy"), gentle
encouragement after a miss, and celebrations for the daily goal and unit completion, with a defined
priority so simultaneous events never overlap.

**Architecture:** A pure `MochiMood` layer (enum + mapping functions) decides *which* face; the
`ReviewViewModel` emits the raw signals (`unitProgress`, `unitCompleted`, `lastAnswerWrong`) on the
`Reviewing` state; `FlashcardScreen` maps signals → moods and drives `MochiMascot`; `MochiLogo`
renders the chosen face from vector paths + Canvas primitives. Reaction priority and baseline
thresholds live in the pure layer so they are unit-tested; the artwork is verified in the iOS
simulator.

**Tech Stack:** Kotlin 2.2.20, Compose Multiplatform 1.10.3, kotlin.test (`commonTest`, JVM host),
Canvas/DrawScope, androidx.lifecycle ViewModel.

**Spec:** `docs/superpowers/specs/2026-07-28-mochi-moods-design.md`

**Quality-gate commands (used in Task 6):**
```bash
./gradlew :composeApp:testAndroidHostTest
./gradlew :composeApp:iosSimulatorArm64Test
./gradlew :composeApp:ktlintCheck :composeApp:detekt
./gradlew :composeApp:compileAndroidMain
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```
`./gradlew ktlintFormat` auto-formats before checking. `commonTest` runs on the JVM host.

---

## File Structure

| File | Responsibility |
|------|----------------|
| `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiMood.kt` | **New.** `MochiMood` enum + pure `moodForUnitRatio` / `moodForUnitProgress` / `resolveReaction`. |
| `composeApp/src/commonTest/kotlin/com/mochi/ui/components/MochiMoodTest.kt` | **New.** Tests for the pure mood layer. |
| `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewUiState.kt` | Adds `unitProgress`, `unitCompleted`, `lastAnswerWrong` to `Reviewing`. |
| `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewViewModel.kt` | Computes the three signals in `answer()` / `beginSession()`. |
| `composeApp/src/commonTest/kotlin/com/mochi/review/ReviewViewModelTest.kt` | New signal tests; `FakeDeck` made faithful (writes `next_review`). |
| `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiLogo.kt` | Adds `mood` param + per-mood eyes/mouth/extras drawing. |
| `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiMascot.kt` | Adds `restMood`/`reactMood`; renders the mood per event. |
| `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt` | Maps signals → moods; drives `MochiMascot`. |
| `composeApp/src/commonMain/kotlin/com/mochi/App.kt` | Threads the three new fields into `FlashcardScreen`. |
| `docs/ROADMAP.md`, `docs/CONTEXT.md` | Docs (Task 6). |

---

## Task 1: MochiMood pure layer

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiMood.kt`
- Test: `composeApp/src/commonTest/kotlin/com/mochi/ui/components/MochiMoodTest.kt`

- [ ] **Step 1: Write the failing test**

Create `MochiMoodTest.kt`:

```kotlin
package com.mochi.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MochiMoodTest {

    @Test
    fun baselineByCountAcrossQuartiles() {
        assertEquals(MochiMood.NotHappy, moodForUnitProgress(0, 50))
        assertEquals(MochiMood.NotHappy, moodForUnitProgress(12, 50)) // 24%
        assertEquals(MochiMood.Content, moodForUnitProgress(13, 50)) // 26%
        assertEquals(MochiMood.Content, moodForUnitProgress(25, 50)) // 50%
        assertEquals(MochiMood.Happy, moodForUnitProgress(26, 50)) // 52%
        assertEquals(MochiMood.Happy, moodForUnitProgress(37, 50)) // 74%
        assertEquals(MochiMood.Radiant, moodForUnitProgress(38, 50)) // 76%
        assertEquals(MochiMood.Radiant, moodForUnitProgress(50, 50)) // 100%
    }

    @Test
    fun baselineEmptyUnitIsNotHappy() {
        assertEquals(MochiMood.NotHappy, moodForUnitProgress(0, 0))
    }

    @Test
    fun baselineByRatioBoundaries() {
        assertEquals(MochiMood.NotHappy, moodForUnitRatio(0.25f))
        assertEquals(MochiMood.Content, moodForUnitRatio(0.50f))
        assertEquals(MochiMood.Happy, moodForUnitRatio(0.75f))
        assertEquals(MochiMood.Radiant, moodForUnitRatio(0.76f))
    }

    @Test
    fun reactionUnitCompletedWinsEverything() {
        assertEquals(MochiMood.Radiant, resolveReaction(true, true, 10, true))
    }

    @Test
    fun reactionGoalBeatsMilestone() {
        assertEquals(MochiMood.Celebrating, resolveReaction(false, true, 10, false))
    }

    @Test
    fun reactionMilestoneAlone() {
        assertEquals(MochiMood.Celebrating, resolveReaction(false, false, 10, false))
    }

    @Test
    fun reactionWrongAlone() {
        assertEquals(MochiMood.Encouraging, resolveReaction(false, false, null, true))
    }

    @Test
    fun reactionWrongYieldsToPositiveEvents() {
        assertEquals(MochiMood.Celebrating, resolveReaction(false, true, null, true))
        assertEquals(MochiMood.Radiant, resolveReaction(true, false, null, true))
    }

    @Test
    fun reactionNoneIsNull() {
        assertNull(resolveReaction(false, false, null, false))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.mochi.ui.components.MochiMoodTest"`
Expected: FAIL to compile — `MochiMood`, `moodForUnitProgress`, `moodForUnitRatio`,
`resolveReaction` are unresolved references.

- [ ] **Step 3: Write the implementation**

Create `MochiMood.kt`:

```kotlin
package com.mochi.ui.components

/**
 * The mascot's facial expressions. [NotHappy]..[Radiant] are the four baseline levels that scale
 * with a unit's fill; [Encouraging] and [Celebrating] are transient reaction faces.
 */
enum class MochiMood {
    NotHappy,
    Content,
    Happy,
    Radiant,
    Encouraging,
    Celebrating,
}

/** Baseline resting mood from a unit's fill [ratio] (0f..1f), across four quartile levels. */
fun moodForUnitRatio(ratio: Float): MochiMood = when {
    ratio <= 0.25f -> MochiMood.NotHappy
    ratio <= 0.50f -> MochiMood.Content
    ratio <= 0.75f -> MochiMood.Happy
    else -> MochiMood.Radiant
}

/** Baseline resting mood from [learned] of [total] cards in the current unit (empty -> NotHappy). */
fun moodForUnitProgress(learned: Int, total: Int): MochiMood =
    moodForUnitRatio(if (total <= 0) 0f else learned.toFloat() / total)

/**
 * The single transient reaction to show, resolved by priority (highest first) so simultaneous
 * events never overlap: unit completed > daily goal > correct milestone > wrong answer. Returns
 * null when nothing reacts, leaving the baseline rest face in place.
 */
fun resolveReaction(
    unitCompleted: Boolean,
    goalReached: Boolean,
    correctMilestone: Int?,
    wrong: Boolean,
): MochiMood? = when {
    unitCompleted -> MochiMood.Radiant
    goalReached -> MochiMood.Celebrating
    correctMilestone != null -> MochiMood.Celebrating
    wrong -> MochiMood.Encouraging
    else -> null
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.mochi.ui.components.MochiMoodTest"`
Expected: PASS (all 9 tests).

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiMood.kt \
        composeApp/src/commonTest/kotlin/com/mochi/ui/components/MochiMoodTest.kt
git commit -m "feat: add MochiMood pure layer (baseline levels + reaction priority)"
```

---

## Task 2: ReviewViewModel mood signals

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewUiState.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewViewModel.kt`
- Test: `composeApp/src/commonTest/kotlin/com/mochi/review/ReviewViewModelTest.kt`

- [ ] **Step 1: Add the new fields to `Reviewing`**

In `ReviewUiState.kt`, extend the `Reviewing` data class (keep existing KDoc, append a sentence):

```kotlin
    data class Reviewing(
        val card: Flashcard,
        val position: Int,
        val total: Int,
        val sessionStreak: Int,
        val correctMilestone: Int?,
        val goalReached: Boolean = false,
        val unitProgress: Float = 0f,
        val unitCompleted: Boolean = false,
        val lastAnswerWrong: Boolean = false,
    ) : ReviewUiState
```

Append to the `Reviewing` KDoc:

```
     * [unitProgress] is the current unit's learned/total ratio (drives the mascot's baseline mood);
     * [unitCompleted] is true only on the emission where the unit's last card first becomes learned;
     * [lastAnswerWrong] is true only on the emission immediately after a wrong answer.
```

- [ ] **Step 2: Make `FakeDeck` faithful, then write the failing signal tests**

The current `FakeDeck.recordAnswer` returns the card unchanged, so `cardsInUnit` never reflects
learning and unit-completion can't be exercised. Make it mirror `DeckRepository` (which always sets
`next_review`). In `ReviewViewModelTest.kt`, replace the `FakeDeck` class body with:

```kotlin
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
        // Like DeckRepository: answering always schedules the card, so a new card becomes "learned".
        val updated = card.copy(next_review = card.next_review ?: 1L)
        cards = cards.map { if (it.id == card.id) updated else it }
        return updated
    }
    override fun recordPractice(card: Flashcard, correct: Boolean) {
        practiceAnswers += card.id to correct
    }
}
```

Then add these tests to `ReviewViewModelTest`:

```kotlin
    @Test
    fun unitProgressReflectsLearnedOverTotal() {
        val deck = FakeDeck(List(4) { card(it.toLong(), isNew = true) })
        val vm = viewModel(deck, newToday = 0, limit = 10)
        vm.openUnit(0)
        assertEquals(0f, (vm.state.value as ReviewUiState.Reviewing).unitProgress)
        vm.answer(isCorrect = true) // 1/4 learned
        assertEquals(0.25f, (vm.state.value as ReviewUiState.Reviewing).unitProgress)
        vm.answer(isCorrect = true) // 2/4 learned
        assertEquals(0.5f, (vm.state.value as ReviewUiState.Reviewing).unitProgress)
    }

    @Test
    fun unitCompletedFiresOnceOnTheCrossing() {
        // Miss card 0 first so it requeues; completing the unit then lands on a Reviewing emission
        // (not the session's final answer, which would go straight to Complete).
        val deck = FakeDeck(List(2) { card(it.toLong(), isNew = true) })
        val vm = viewModel(deck, newToday = 0, limit = 10)
        vm.openUnit(0)
        vm.answer(isCorrect = false) // card 0 learned + requeued: 1/2
        assertFalse((vm.state.value as ReviewUiState.Reviewing).unitCompleted)
        vm.answer(isCorrect = true) // card 1 learned: 2/2 crosses
        assertTrue((vm.state.value as ReviewUiState.Reviewing).unitCompleted)
        vm.answer(isCorrect = false) // requeued card 0 again: no refire
        assertFalse((vm.state.value as ReviewUiState.Reviewing).unitCompleted)
    }

    @Test
    fun unitCompletedNeverFiresInPracticeMode() {
        val cards = List(2) { card(it.toLong(), isNew = false, nextReview = Long.MAX_VALUE) }
        val vm = viewModel(FakeDeck(cards), newToday = 0, limit = 0) // nothing due -> practice
        vm.openUnit(0)
        vm.answer(isCorrect = true)
        assertFalse((vm.state.value as ReviewUiState.Reviewing).unitCompleted)
    }

    @Test
    fun lastAnswerWrongTogglesPerAnswer() {
        val deck = FakeDeck(List(3) { card(it.toLong(), isNew = true) })
        val vm = viewModel(deck, newToday = 0, limit = 10)
        vm.openUnit(0)
        vm.answer(isCorrect = false)
        assertTrue((vm.state.value as ReviewUiState.Reviewing).lastAnswerWrong)
        vm.answer(isCorrect = true)
        assertFalse((vm.state.value as ReviewUiState.Reviewing).lastAnswerWrong)
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.mochi.review.ReviewViewModelTest"`
Expected: FAIL — `unitProgress`, `unitCompleted`, `lastAnswerWrong` are unresolved on `Reviewing`
(and the new assertions fail once the fields exist but the VM doesn't compute them).

- [ ] **Step 4: Implement the signal computation**

In `ReviewViewModel.kt`, at the top of `answer()`, capture the pre-answer unit snapshot right after
the null-guard on `card`:

```kotlin
    fun answer(isCorrect: Boolean) {
        val card = session.getOrNull(index) ?: return
        val unitCards = deck.cardsInUnit(currentUnitId)
        val unitTotal = unitCards.size
        val learnedBefore = unitCards.count { it.next_review != null }
        val wasNew = card.next_review == null
```

Leave the existing body (recordAnswer, `reviewed++`, `goal`, `goalReached`, `milestone`,
streak/requeue) unchanged. Then change the emit branch at the end of `answer()` from:

```kotlin
        if (index < session.lastIndex) {
            index++
            emitReviewing(milestone, goalReached)
        } else {
            _state.value = ReviewUiState.Complete(SessionStats(reviewed = reviewed, correct = correct))
        }
    }
```

to:

```kotlin
        val learnedAfter = (learnedBefore + if (wasNew && !practiceMode) 1 else 0).coerceAtMost(unitTotal)
        val unitCompleted = !practiceMode && learnedBefore < unitTotal && learnedAfter == unitTotal
        val unitProgress = if (unitTotal <= 0) 0f else learnedAfter.toFloat() / unitTotal
        if (index < session.lastIndex) {
            index++
            emitReviewing(
                milestone = milestone,
                goalReached = goalReached,
                unitProgress = unitProgress,
                unitCompleted = unitCompleted,
                lastAnswerWrong = !isCorrect,
            )
        } else {
            _state.value = ReviewUiState.Complete(SessionStats(reviewed = reviewed, correct = correct))
        }
    }
```

Update `beginSession()` to seed the baseline from the unit's current fill:

```kotlin
    private fun beginSession(queue: List<Flashcard>) {
        session = queue
        index = 0
        reviewed = 0
        correct = 0
        sessionStreak = 0
        reviewsAtSessionStart = reviewCountSource.reviewsOnDay(todayEpochDay())
        val unitCards = deck.cardsInUnit(currentUnitId)
        val unitTotal = unitCards.size
        val learned = unitCards.count { it.next_review != null }
        emitReviewing(
            milestone = null,
            unitProgress = if (unitTotal <= 0) 0f else learned.toFloat() / unitTotal,
        )
    }
```

Extend `emitReviewing()` with the new parameters (defaults keep other call sites valid):

```kotlin
    private fun emitReviewing(
        milestone: Int?,
        goalReached: Boolean = false,
        unitProgress: Float = 0f,
        unitCompleted: Boolean = false,
        lastAnswerWrong: Boolean = false,
    ) {
        _state.value = ReviewUiState.Reviewing(
            card = session[index],
            position = index + 1,
            total = session.size,
            sessionStreak = sessionStreak,
            correctMilestone = milestone,
            goalReached = goalReached,
            unitProgress = unitProgress,
            unitCompleted = unitCompleted,
            lastAnswerWrong = lastAnswerWrong,
        )
    }
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.mochi.review.ReviewViewModelTest"`
Expected: PASS — new tests green AND all pre-existing tests (streak, milestone, goalReached,
requeue, practice) still green.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/review/ReviewUiState.kt \
        composeApp/src/commonMain/kotlin/com/mochi/review/ReviewViewModel.kt \
        composeApp/src/commonTest/kotlin/com/mochi/review/ReviewViewModelTest.kt
git commit -m "feat: emit unit progress, completion, and wrong-answer signals from review"
```

---

## Task 3: MochiLogo expressions

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiLogo.kt`

This task is visual; correctness is verified by compilation here and by the simulator in Task 6.
Coordinates are on the existing `0..64` viewBox (eye centers ≈ `(24.3, 35.8)` and `(39.6, 35.8)`;
mouth ≈ `(32, 36)`). Values may be nudged during the Task 6 simulator pass.

- [ ] **Step 1: Add imports and accent colors**

Add these imports to `MochiLogo.kt` (keep the list lexicographically ordered):

```kotlin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
```

(`StrokeCap`, `Stroke`, `Offset`, `Size`, `Path`, `Color`, `DrawScope`, `scale` are already
imported.) Below the existing `private val Highlight` line, add:

```kotlin
private val SparkleGold = Color(0xFFFFC93C)
private val SweatBlue = Color(0xFF8FD3E8)
private val MouthOpen = Color(0xFFC96A82)

private val eyeLeftCenter = Offset(24.3f, 35.8f)
private val eyeRightCenter = Offset(39.6f, 35.8f)

// Sweat drop near the upper-right of the head (Encouraging).
private val sweatPath =
    path("M47 17.5 C 45.3 20.2 45.3 22.2 47 22.2 C 48.7 22.2 48.7 20.2 47 17.5 Z")
```

- [ ] **Step 2: Add the `mood` parameter and route the face**

Change the `MochiLogo` signature to accept a mood (default keeps every existing caller on the
current face):

```kotlin
@Composable
fun MochiLogo(
    modifier: Modifier = Modifier,
    animateEntry: Boolean = true,
    interactive: Boolean = true,
    mood: MochiMood = MochiMood.Content,
) {
```

Inside the `scale(s, s, pivot = Offset.Zero) { ... }` block, replace the three lines that draw the
smile and the two eyes:

```kotlin
            drawPath(
                smilePath,
                SakuraPink,
                style = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(eyeLeftPath, Kurogoma)
            drawPath(eyeRightPath, Kurogoma)
```

with a single call:

```kotlin
            drawMoodFace(mood)
```

Keep everything else in the block (body fill before it; cheeks, belly, highlights, body stroke
after it) exactly as-is.

- [ ] **Step 3: Add the drawing helpers**

Append these `DrawScope` helpers at the end of `MochiLogo.kt` (after the `highlight` function):

```kotlin
private val faceSmileStroke = Stroke(width = 1f, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Draws the eyes, mouth, and any extras for [mood]. Body/cheeks/belly are drawn by the caller. */
private fun DrawScope.drawMoodFace(mood: MochiMood) {
    when (mood) {
        MochiMood.NotHappy -> {
            drawDashEye(eyeLeftCenter)
            drawDashEye(eyeRightCenter)
            drawFlatMouth()
        }
        MochiMood.Content -> {
            drawOpenEyes()
            drawPath(smilePath, SakuraPink, style = faceSmileStroke)
        }
        MochiMood.Happy -> {
            drawOpenEyes()
            drawSmileArc(width = 9f, height = 6f)
        }
        MochiMood.Radiant -> {
            drawOpenEyes()
            drawSmileArc(width = 11f, height = 8f)
            drawSparkle(19.5f, 30f)
            drawSparkle(44.5f, 30f)
        }
        MochiMood.Encouraging -> {
            drawArcEye(eyeLeftCenter)
            drawArcEye(eyeRightCenter)
            drawSmileArc(width = 5f, height = 2.5f)
            drawPath(sweatPath, SweatBlue)
        }
        MochiMood.Celebrating -> {
            drawArcEye(eyeLeftCenter)
            drawArcEye(eyeRightCenter)
            drawOpenMouth()
        }
    }
}

private fun DrawScope.drawOpenEyes() {
    drawPath(eyeLeftPath, Kurogoma)
    drawPath(eyeRightPath, Kurogoma)
}

/** A flat "-" eye (NotHappy). */
private fun DrawScope.drawDashEye(center: Offset) {
    drawLine(
        color = Kurogoma,
        start = Offset(center.x - 2.2f, center.y),
        end = Offset(center.x + 2.2f, center.y),
        strokeWidth = 1.6f,
        cap = StrokeCap.Round,
    )
}

/** An upward "∩" closed-happy eye (Encouraging/Celebrating). */
private fun DrawScope.drawArcEye(center: Offset) {
    val w = 4.6f
    val h = 4.2f
    drawArc(
        color = Kurogoma,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
        size = Size(w, h),
        style = Stroke(width = 1.6f, cap = StrokeCap.Round),
    )
}

/** A downward "∪" smile arc centered under the eyes. */
private fun DrawScope.drawSmileArc(width: Float, height: Float) {
    drawArc(
        color = SakuraPink,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(32f - width / 2f, 35f - height / 2f),
        size = Size(width, height),
        style = Stroke(width = 1.4f, cap = StrokeCap.Round),
    )
}

/** A straight neutral mouth (NotHappy). */
private fun DrawScope.drawFlatMouth() {
    drawLine(
        color = SakuraPink,
        start = Offset(29.5f, 37.5f),
        end = Offset(34.5f, 37.5f),
        strokeWidth = 1.4f,
        cap = StrokeCap.Round,
    )
}

/** A filled open mouth (Celebrating): the lower half of a small ellipse. */
private fun DrawScope.drawOpenMouth() {
    val w = 9f
    val h = 8f
    drawArc(
        color = MouthOpen,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(32f - w / 2f, 35f - h / 2f),
        size = Size(w, h),
        style = Fill,
    )
}

/** A small 4-point star sparkle (Radiant). */
private fun DrawScope.drawSparkle(cx: Float, cy: Float) {
    val r = 2.4f
    val n = r * 0.32f // waist of the star
    val star = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + n, cy - n)
        lineTo(cx + r, cy)
        lineTo(cx + n, cy + n)
        lineTo(cx, cy + r)
        lineTo(cx - n, cy + n)
        lineTo(cx - r, cy)
        lineTo(cx - n, cy - n)
        close()
    }
    drawPath(star, SparkleGold)
}
```

Also add these imports if not already present (keep them ordered): `drawLine` and `drawArc` are
`DrawScope` member functions (no import needed); `Fill` was added in Step 1.

- [ ] **Step 4: Verify it compiles on both targets**

Run:
```bash
./gradlew :composeApp:compileAndroidMain :composeApp:compileKotlinIosSimulatorArm64
```
Expected: BUILD SUCCESSFUL. (Visual correctness is confirmed in Task 6.)

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiLogo.kt
git commit -m "feat: render six MochiLogo expressions via a mood parameter"
```

---

## Task 4: MochiMascot mood wiring

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiMascot.kt`

- [ ] **Step 1: Add mood parameters and per-event face state**

Change the `MochiMascot` signature to:

```kotlin
@Composable
fun MochiMascot(
    greet: Any?,
    react: Any?,
    restMood: MochiMood = MochiMood.Content,
    reactMood: MochiMood = MochiMood.Content,
    modifier: Modifier = Modifier,
) {
```

Add the import (lexicographic order, alongside the existing `com.mochi.ui.motion.*` imports):

```kotlin
import com.mochi.ui.motion.MascotEvent
```

(already imported — leave as-is). No new import is needed for `MochiMood` since it is in the same
package (`com.mochi.ui.components`).

Right after the `completedReact` state declaration, add the face state:

```kotlin
    var faceMood by remember { mutableStateOf(restMood) }
```

- [ ] **Step 2: Set the face on each event and render it**

In the `LaunchedEffect(greet, react, motionPolicy.reduced)` dispatch, set `faceMood` before each
pop:

```kotlin
            MascotEvent.REACT -> {
                completedGreet = greet
                faceMood = reactMood
                val performHaptic = react != startedReact
                startedReact = react
                popUp(REACT_HOLD_MS, performHaptic)
                completedReact = react
            }
            MascotEvent.GREET -> {
                faceMood = restMood
                val performHaptic = greet != startedGreet
                startedGreet = greet
                popUp(GREET_HOLD_MS, performHaptic)
                completedGreet = greet
            }
```

Pass the mood to the logo:

```kotlin
        MochiLogo(
            animateEntry = false,
            mood = faceMood,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
                .size(88.dp)
                .graphicsLayer {
                    val r = reveal.value
                    translationY = if (motionPolicy.allowSpatialMotion) {
                        (1f - r) * size.height * TUCK_FACTOR
                    } else {
                        0f
                    }
                    alpha = r.coerceIn(0f, 1f)
                },
        )
```

- [ ] **Step 2b: Update the mascot KDoc**

Append to the `MochiMascot` KDoc: "Shows [restMood] when greeting/at rest and [reactMood] during a
reaction pop."

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :composeApp:compileAndroidMain`
Expected: BUILD SUCCESSFUL. (It will still fail at `FlashcardScreen` call sites only if that file
was already changed; it has not — the new params have defaults, so this compiles standalone.)

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiMascot.kt
git commit -m "feat: MochiMascot renders rest vs reaction mood per event"
```

---

## Task 5: Wire moods into the study screen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/App.kt`

- [ ] **Step 1: Add the new inputs to `FlashcardScreen`**

Add three parameters after `goalReached` in the `FlashcardScreen` signature:

```kotlin
    correctMilestone: Int?,
    goalReached: Boolean,
    unitProgress: Float,
    unitCompleted: Boolean,
    lastAnswerWrong: Boolean,
    sessionStreak: Int,
```

- [ ] **Step 2: Map signals → moods and drive the mascot**

Replace the `mascotReact`/`MochiMascot` block near the bottom of the `Box`:

```kotlin
        // Confetti stays tied to the correct-answer milestone; the mascot also cheers when the
        // daily goal is reached.
        val mascotReact = remember(correctMilestone, goalReached) {
            if (correctMilestone != null || goalReached) Any() else null
        }
        ConfettiBurst(trigger = correctMilestone, modifier = Modifier.fillMaxSize())
        MochiMascot(greet = greetTrigger, react = mascotReact, modifier = Modifier.fillMaxSize())
```

with:

```kotlin
        // Confetti stays tied to the correct-answer milestone. The mascot's baseline face scales
        // with unit progress; a single reaction (priority-resolved) pops for the strongest event.
        val restMood = moodForUnitRatio(unitProgress)
        val reaction = resolveReaction(unitCompleted, goalReached, correctMilestone, lastAnswerWrong)
        val mascotReact = remember(correctMilestone, goalReached, unitCompleted, lastAnswerWrong) {
            if (reaction != null) Any() else null
        }
        ConfettiBurst(trigger = correctMilestone, modifier = Modifier.fillMaxSize())
        MochiMascot(
            greet = greetTrigger,
            react = mascotReact,
            restMood = restMood,
            reactMood = reaction ?: restMood,
            modifier = Modifier.fillMaxSize(),
        )
```

Add imports to `FlashcardScreen.kt` (lexicographic order):

```kotlin
import com.mochi.ui.components.moodForUnitRatio
import com.mochi.ui.components.resolveReaction
```

- [ ] **Step 3: Thread the fields from `App.kt`**

In `App.kt`, the `is ReviewUiState.Reviewing -> FlashcardScreen(` call, add the three arguments
between `goalReached` and `sessionStreak`:

```kotlin
                is ReviewUiState.Reviewing -> FlashcardScreen(
                    card = s.card,
                    position = s.position,
                    total = s.total,
                    correctMilestone = s.correctMilestone,
                    goalReached = s.goalReached,
                    unitProgress = s.unitProgress,
                    unitCompleted = s.unitCompleted,
                    lastAnswerWrong = s.lastAnswerWrong,
                    sessionStreak = s.sessionStreak,
                    onAnswer = viewModel::answer,
                    onPlayAudio = viewModel::playCurrentAudio,
                    sharedScope = this@SharedTransitionLayout,
                    animatedScope = this@AnimatedContent,
                    sharedKey = "unit-${viewModel.lastOpenedUnitId}",
                )
```

- [ ] **Step 4: Verify both targets compile**

Run:
```bash
./gradlew :composeApp:compileAndroidMain :composeApp:compileKotlinIosSimulatorArm64
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt \
        composeApp/src/commonMain/kotlin/com/mochi/App.kt
git commit -m "feat: drive Mochi moods from review signals on the study screen"
```

---

## Task 6: Quality gate, simulator smoke-test, and docs

**Files:**
- Modify: `docs/ROADMAP.md`, `docs/CONTEXT.md`

- [ ] **Step 1: Run the full quality gate**

```bash
./gradlew ktlintFormat
./gradlew :composeApp:testAndroidHostTest \
          :composeApp:iosSimulatorArm64Test \
          :composeApp:ktlintCheck \
          :composeApp:detekt \
          :composeApp:compileAndroidMain \
          :composeApp:compileKotlinIosSimulatorArm64
```
Expected: all green. If detekt flags the `when` in `drawMoodFace` or a helper count, prefer a small
refactor; only `@Suppress` with a one-line rationale if a rule is genuinely over-opinionated (per
`docs/CONTEXT.md`).

- [ ] **Step 2: Simulator smoke-test (iOS)**

Build and run the iOS app on a booted simulator (the live panel is optional; verification is
headless via screenshots). Confirm:

- On opening a barely-started unit, the greeting Mochi shows the `-_-` NotHappy face; opening a
  mostly-full unit greets with Happy/Radiant.
- Answering a card **wrong** pops the Encouraging face (soft `^^` eyes + sweat drop).
- Every 10th correct answer pops the Celebrating face (alongside the existing confetti).
- Reaching the daily goal pops Celebrating; learning the unit's last new card pops Radiant.
- When events coincide, exactly one reaction shows, following the priority order.
- Reduced motion: expressions still render (static); no pop spring — the face fades in per the
  existing reduced-motion path.

Tune expression coordinates in `MochiLogo.kt` if a face reads wrong, then re-run Step 1. Record any
device-only concerns as N/A (no physical iOS device — the simulator is the verification ceiling).

- [ ] **Step 3: Update docs**

- In `docs/ROADMAP.md`, mark "## 5. Mochi moods" complete with a one-line summary (four baseline
  faces scaling with unit fill, encouragement on miss, celebration for goal/unit-completion,
  priority-resolved single reaction) and the simulator verification status. Note that
  unit-completion is derived here (independent of the not-yet-built item 4 celebration screen).
- In `docs/CONTEXT.md`, add the moods feature: the `MochiMood` pure layer
  (`moodForUnitRatio`/`moodForUnitProgress`/`resolveReaction`), the `Reviewing` signals
  (`unitProgress`, `unitCompleted`, `lastAnswerWrong`), and the `MochiLogo` `mood` parameter with
  the six expressions.

- [ ] **Step 4: Commit**

```bash
git add docs/ROADMAP.md docs/CONTEXT.md
git commit -m "docs: document Mochi moods (roadmap item 5)"
```

- [ ] **Step 5: Review before integration**

Invoke `superpowers:requesting-code-review`, address only verified findings, and rerun the complete
quality gate. Then use `superpowers:finishing-a-development-branch` for the no-push handoff.

---

## Self-Review

**Spec coverage:**
- *Gentle encouragement after an incorrect answer* — `lastAnswerWrong` signal (Task 2) →
  `resolveReaction` → Encouraging face (Tasks 1, 3, 5). ✓
- *Progressively happier as the unit fills* — `unitProgress` (Task 2) → `moodForUnitRatio` four
  quartile levels incl. NotHappy (Tasks 1, 3, 5). ✓
- *Reuse daily-goal and unit-completion events* — existing `goalReached` reused; `unitCompleted`
  derived in the VM without item 4 (Task 2); both feed `resolveReaction` (Tasks 1, 5). ✓
- *Reaction priority, no overlap* — `resolveReaction` returns a single mood by priority; the screen
  fires at most one pop (Tasks 1, 5), tested (Task 1). ✓
- *Accessibility* — expressions are static (Task 3); pop/entry remain motion-gated (Task 4, unchanged
  MochiMascot motion path); verified in Task 6. ✓

**Placeholders:** none — every code step shows complete code.

**Type consistency:** `MochiMood` (`com.mochi.ui.components`) defined in Task 1, consumed by
`MochiLogo` (Task 3), `MochiMascot` (Task 4), `FlashcardScreen` (Task 5). `moodForUnitRatio` /
`moodForUnitProgress` / `resolveReaction` signatures identical across Tasks 1, 2-tests, and 5.
`unitProgress: Float` / `unitCompleted: Boolean` / `lastAnswerWrong: Boolean` consistent across
`ReviewUiState.Reviewing`, `ReviewViewModel.emitReviewing`, `FlashcardScreen`, and `App.kt`.
`MochiMascot(restMood, reactMood)` params match the call in Task 5.

**Known, intentional limitation:** if the answer that completes the unit (or crosses the goal) is
the session's final card, the flow transitions straight to `Complete` and that reaction is not
shown — identical to the existing `goalReached`/`correctMilestone` behavior. The session-complete
screen is its own celebration. Not expanded here (YAGNI, consistency).
