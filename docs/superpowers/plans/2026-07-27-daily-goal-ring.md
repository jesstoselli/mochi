# Daily Goal and Progress Ring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a configurable daily review goal shown as a liquid progress ring in the Library header, plus a one-shot Mochi reaction on the study screen when the goal is reached.

**Architecture:** Reuse existing reactive sources — `StatsViewModel.stats.reviewsToday` and a new `SettingsViewModel.dailyGoal` StateFlow — and combine them with a pure `toDailyGoalState(reviewsToday, goal)` mapping (no new ViewModel). The goal-reached celebration is a one-shot `goalReached` flag in `ReviewUiState.Reviewing`, computed in `ReviewViewModel` at the `< goal → >= goal` crossing and routed to the existing `MochiMascot` on `FlashcardScreen`.

**Tech Stack:** Kotlin 2.2.20, Compose Multiplatform 1.10.3, androidx.lifecycle ViewModel, SQLDelight settings table, kotlin.test.

## Global Constraints

- TDD: write the failing test first for every unit with real logic (settings, pure mapping, crossing detection).
- The daily goal counts reviews completed today (`StatsStore.reviewsOnDay`), unchanged data model.
- Goal choices are 10/20/30/50, default 20, always on.
- Reduced motion follows `LocalMotionPolicy` (static fill, no infinite wave).
- Commit each task separately; never push.
- Run `:composeApp:testAndroidHostTest` for fast JVM test feedback; the full gate runs in Task 7.

## File Structure

- `com/mochi/settings/SettingsStore.kt` (modify) — add `DailyGoalSource` interface + `dailyGoal()`/`setDailyGoal()`.
- `com/mochi/goal/DailyGoal.kt` (create) — `DailyGoalState` + pure `toDailyGoalState`.
- `com/mochi/stats/StatsStore.kt` (modify) — implement new `ReviewCountSource`.
- `com/mochi/review/ReviewCountSource.kt` (create) — narrow interface for reviews-on-day.
- `com/mochi/review/ReviewUiState.kt` (modify) — add `goalReached` to `Reviewing`.
- `com/mochi/review/ReviewViewModel.kt` (modify) — inject sources, detect crossing.
- `com/mochi/ui/components/DailyGoalRing.kt` (create) — the circular liquid gauge.
- `com/mochi/settings/SettingsViewModel.kt` (modify) — `dailyGoal` StateFlow + setter.
- `com/mochi/ui/screens/SettingsScreen.kt` + `SettingsPresentation.kt` (modify) — the goal row/dialog.
- `com/mochi/ui/screens/LibraryScreen.kt` (modify) — header ring.
- `com/mochi/ui/screens/FlashcardScreen.kt` (modify) — mascot reacts on `goalReached`.
- `com/mochi/App.kt` (modify) — wire the goal setting, ring state, and constructor args.

---

### Task 1: Daily-goal setting

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/settings/SettingsStore.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/mochi/settings/SettingsStoreTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `SettingsStoreTest.kt` (it uses an in-memory `SettingValues` fake — follow the existing tests in the file for the exact fake name; the fake below is self-contained):

```kotlin
@Test
fun dailyGoalDefaultsTo20() {
    val store = SettingsStore(FakeSettingValues())
    assertEquals(20, store.dailyGoal())
}

@Test
fun dailyGoalRoundTrips() {
    val store = SettingsStore(FakeSettingValues())
    store.setDailyGoal(30)
    assertEquals(30, store.dailyGoal())
}

@Test
fun dailyGoalFallsBackOnGarbage() {
    val values = FakeSettingValues()
    values.write("daily_goal", "not-a-number")
    assertEquals(20, SettingsStore(values).dailyGoal())
}
```

If `SettingsStoreTest.kt` does not already define a `FakeSettingValues`, add this at the bottom of the file:

```kotlin
private class FakeSettingValues : com.mochi.settings.SettingValues {
    private val map = mutableMapOf<String, String>()
    override fun read(key: String): String? = map[key]
    override fun write(key: String, value: String) { map[key] = value }
}
```

Note: `SettingValues` is `internal` — this test is in the same module, so it is visible.

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "*SettingsStoreTest*"`
Expected: FAIL — `dailyGoal`/`setDailyGoal` unresolved.

- [ ] **Step 3: Implement the setting + `DailyGoalSource`**

In `SettingsStore.kt`, add the interface near `NewCardLimitSource`:

```kotlin
/** Just the daily review goal the ring and review flow need (lets callers use a fake). */
interface DailyGoalSource {
    fun dailyGoal(): Int
}
```

Change the class declaration to also implement it:

```kotlin
class SettingsStore internal constructor(
    private val values: SettingValues,
) : NewCardLimitSource, DailyGoalSource {
```

Add the accessor (next to `newCardLimit`):

```kotlin
/** Reviews targeted per day for the daily-goal ring. */
override fun dailyGoal(): Int {
    val stored = values.read(KEY_DAILY_GOAL)
    return stored?.toIntOrNull() ?: DEFAULT_DAILY_GOAL
}

fun setDailyGoal(goal: Int) {
    values.write(KEY_DAILY_GOAL, goal.toString())
}
```

Add to the `companion object`:

```kotlin
const val KEY_DAILY_GOAL = "daily_goal"
const val DEFAULT_DAILY_GOAL = 20
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "*SettingsStoreTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/settings/SettingsStore.kt \
  composeApp/src/commonTest/kotlin/com/mochi/settings/SettingsStoreTest.kt
git commit -m "feat: add daily review goal setting"
```

---

### Task 2: Pure daily-goal state mapping

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/goal/DailyGoal.kt`
- Create: `composeApp/src/commonTest/kotlin/com/mochi/goal/DailyGoalTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
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
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "*DailyGoalTest*"`
Expected: FAIL — `toDailyGoalState`/`DailyGoalState` unresolved.

- [ ] **Step 3: Implement the pure mapping**

```kotlin
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
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "*DailyGoalTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/goal/DailyGoal.kt \
  composeApp/src/commonTest/kotlin/com/mochi/goal/DailyGoalTest.kt
git commit -m "feat: add pure daily-goal state mapping"
```

---

### Task 3: goalReached crossing in the review flow

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewCountSource.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/stats/StatsStore.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewUiState.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewViewModel.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/mochi/review/ReviewViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

First update the test's VM construction helper and fakes so the file compiles with the new
constructor, then add the two new tests. Replace the `viewModel(...)` helper and add fakes:

```kotlin
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

private class FakeReviewCount(private val count: Long) : com.mochi.review.ReviewCountSource {
    override fun reviewsOnDay(day: Long): Long = count
}

private class FakeGoal(private val goal: Int) : com.mochi.settings.DailyGoalSource {
    override fun dailyGoal(): Int = goal
}
```

Update the one direct construction inside `changingTheLimitRebuildsTheRunningSession` to match:

```kotlin
val vm = ReviewViewModel(deck, FakeCounter(0), limit, FakeReviewCount(0), FakeGoal(0), AudioPlayer())
```

Add the new tests:

```kotlin
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
```

Add the imports `import kotlin.test.assertFalse` (assertTrue is already imported).

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "*ReviewViewModelTest*"`
Expected: FAIL — new constructor params, `ReviewCountSource`, and `goalReached` unresolved.

- [ ] **Step 3: Create `ReviewCountSource` and implement it on `StatsStore`**

Create `ReviewCountSource.kt`:

```kotlin
package com.mochi.review

/** Reviews logged on a given epoch-day — the daily-goal crossing needs the running total. */
interface ReviewCountSource {
    fun reviewsOnDay(day: Long): Long
}
```

In `StatsStore.kt`, change the class declaration and mark the existing method `override`:

```kotlin
class StatsStore(private val db: AppDatabase) : NewCardCounter, com.mochi.review.ReviewCountSource {

    override fun reviewsOnDay(day: Long): Long = db.reviewLogQueries.countOnDay(day).executeAsOne()
```

(Keep the rest of `StatsStore` unchanged; `reviewsOnDay` was already defined — only add `override` and the interface.)

- [ ] **Step 4: Add `goalReached` to the state**

In `ReviewUiState.kt`, add the field (with a default so unrelated construction stays valid) and extend the KDoc:

```kotlin
data class Reviewing(
    val card: Flashcard,
    val position: Int,
    val total: Int,
    val sessionStreak: Int,
    val correctMilestone: Int?,
    val goalReached: Boolean = false,
) : ReviewUiState
```

Add to the KDoc above it: "[goalReached] is true only on the emission where today's global review count first crosses the configured daily goal, so the mascot cheers once."

- [ ] **Step 5: Detect the crossing in `ReviewViewModel`**

Add the two constructor params (after `limitSource`, before `audioPlayer`):

```kotlin
class ReviewViewModel(
    private val deck: ReviewDeck,
    private val newCardCounter: NewCardCounter,
    private val limitSource: NewCardLimitSource,
    private val reviewCountSource: ReviewCountSource,
    private val goalSource: com.mochi.settings.DailyGoalSource,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {
```

Add a field near the other session fields:

```kotlin
private var reviewsAtSessionStart = 0L
```

In `beginSession`, capture the starting global count (add as the first line of the body):

```kotlin
reviewsAtSessionStart = reviewCountSource.reviewsOnDay(todayEpochDay())
```

In `answer()`, compute the crossing after `reviewed++` and before the milestone block, then pass it
through. Replace the milestone/emit tail of `answer()` with:

```kotlin
        reviewed++
        val goal = goalSource.dailyGoal()
        val goalReached = goal > 0 &&
            (reviewsAtSessionStart + reviewed - 1) < goal &&
            (reviewsAtSessionStart + reviewed) >= goal
        var milestone: Int? = null
        if (isCorrect) {
            correct++
            sessionStreak++
            if (correct % CORRECT_MILESTONE == 0) milestone = correct
        } else {
            sessionStreak = 0
            session = session + updated
        }
        if (index < session.lastIndex) {
            index++
            emitReviewing(milestone, goalReached)
        } else {
            _state.value = ReviewUiState.Complete(SessionStats(reviewed = reviewed, correct = correct))
        }
```

Update `emitReviewing` to take and forward the flag:

```kotlin
private fun emitReviewing(milestone: Int?, goalReached: Boolean = false) {
    _state.value = ReviewUiState.Reviewing(
        card = session[index],
        position = index + 1,
        total = session.size,
        sessionStreak = sessionStreak,
        correctMilestone = milestone,
        goalReached = goalReached,
    )
}
```

(`beginSession`'s existing `emitReviewing(milestone = null)` call now defaults `goalReached` to false — no change needed there. Note: if the crossing happens on the session's final card the state goes to `Complete` and the mascot reaction is skipped; the session-complete confetti covers that case. This is intentional.)

- [ ] **Step 6: Run to verify pass**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "*ReviewViewModelTest*"`
Expected: PASS — all prior tests plus the two new ones.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/review/ReviewCountSource.kt \
  composeApp/src/commonMain/kotlin/com/mochi/stats/StatsStore.kt \
  composeApp/src/commonMain/kotlin/com/mochi/review/ReviewUiState.kt \
  composeApp/src/commonMain/kotlin/com/mochi/review/ReviewViewModel.kt \
  composeApp/src/commonTest/kotlin/com/mochi/review/ReviewViewModelTest.kt
git commit -m "feat: signal daily-goal crossing from the review flow"
```

---

### Task 4: DailyGoalRing composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/ui/components/DailyGoalRing.kt`

No unit test: this is a Canvas visual (its logic lives in the tested `toDailyGoalState`). It is verified in the Task 7 simulator smoke-test.

- [ ] **Step 1: Implement the ring**

```kotlin
package com.mochi.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.mochi.ui.motion.AnimatedCounter
import com.mochi.ui.motion.LocalMotionPolicy
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private const val WAVE_STEPS = 24
private const val WAVE_DURATION_MS = 1600
private const val WAVE_AMPLITUDE = 6f

/**
 * A circular "liquid in a jar" gauge: a ring border whose interior fills with an animated sine
 * wave up to [progress], with today's count in the center. When [reached] it shows a check.
 * Presentation-only; reduced motion (via [LocalMotionPolicy]) uses a static fill.
 */
@Composable
fun DailyGoalRing(
    reviewsToday: Int,
    goal: Int,
    progress: Float,
    reached: Boolean,
    modifier: Modifier = Modifier,
) {
    val policy = LocalMotionPolicy.current
    val ring = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = ring.copy(alpha = 0.35f)
    val phase = if (policy.allowInfiniteMotion) rememberWavePhase() else 0f
    val amplitude = if (policy.allowInfiniteMotion) WAVE_AMPLITUDE else 0f

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val strokePx = 8.dp.toPx()
            val radius = (min(size.width, size.height) - strokePx) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val inner = radius - strokePx / 2f
            val circle = Path().apply {
                addOval(
                    Rect(center.x - inner, center.y - inner, center.x + inner, center.y + inner),
                )
            }
            clipPath(circle) {
                val clamped = progress.coerceIn(0f, 1f)
                val top = center.y + inner - 2f * inner * clamped
                val wave = Path().apply {
                    moveTo(center.x - inner, center.y + inner)
                    lineTo(center.x - inner, top)
                    for (i in 0..WAVE_STEPS) {
                        val x = center.x - inner + 2f * inner * i / WAVE_STEPS
                        val y = top + sin(phase + i.toFloat() / WAVE_STEPS * 2f * PI.toFloat()) * amplitude
                        lineTo(x, y)
                    }
                    lineTo(center.x + inner, center.y + inner)
                    close()
                }
                drawPath(wave, fill)
            }
            drawCircle(color = track, radius = radius, center = center, style = Stroke(strokePx))
        }
        if (reached) {
            Text("✓", style = MaterialTheme.typography.headlineMedium, color = ring)
        } else {
            AnimatedCounter(
                value = reviewsToday,
                suffix = " / $goal",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun rememberWavePhase(): Float {
    val transition = rememberInfiniteTransition(label = "goalRing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WAVE_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "goalWavePhase",
    )
    return phase
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :composeApp:compileAndroidMain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/components/DailyGoalRing.kt
git commit -m "feat: add the daily-goal liquid ring component"
```

---

### Task 5: Configure the goal in Settings

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/settings/SettingsViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/SettingsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/SettingsPresentation.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/App.kt`

- [ ] **Step 1: Expose the goal from the ViewModel**

In `SettingsViewModel.kt`, add the flow (next to `_newCardLimit`):

```kotlin
private val _dailyGoal = MutableStateFlow(store.dailyGoal())
val dailyGoal: StateFlow<Int> = _dailyGoal.asStateFlow()
```

and the setter (next to `setNewCardLimit`):

```kotlin
fun setDailyGoal(goal: Int) {
    store.setDailyGoal(goal)
    _dailyGoal.value = goal
}
```

- [ ] **Step 2: Add the goal choices constant**

In `SettingsScreen.kt`, next to `NewCardOptions`:

```kotlin
private val DailyGoalOptions = listOf(10, 20, 30, 50)
```

Add a `DAILY_GOAL` entry to the `SettingsDialog` enum:

```kotlin
private enum class SettingsDialog {
    THEME,
    MOTION,
    NEW_CARDS,
    DAILY_GOAL,
}
```

- [ ] **Step 3: Add the parameters and the preference row**

Add two parameters to `SettingsScreen(...)` (after `newCardLimit`/`onNewCardLimitChange`):

```kotlin
    dailyGoal: Int,
    onDailyGoalChange: (Int) -> Unit,
```

In the `SETTINGS_STUDY_RHYTHM` section, add a row + divider directly after the "New cards" row's
`PreferenceRow(...)` (before its following `HorizontalDivider`):

```kotlin
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PreferenceRow(
                icon = Icons.Filled.Flag,
                label = "Daily goal",
                supportingText = "Reviews to aim for each day",
                value = dailyGoal.toString(),
                testTag = "daily-goal-setting",
                iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = { openDialog = SettingsDialog.DAILY_GOAL },
            )
```

Add the icon import at the top:

```kotlin
import androidx.compose.material.icons.filled.Flag
```

Add the dialog branch in the `when (openDialog)` block (after the `NEW_CARDS` branch):

```kotlin
        SettingsDialog.DAILY_GOAL -> ChoiceDialog(
            title = "Daily goal",
            selected = dailyGoal,
            choices = DailyGoalOptions.map { value ->
                Choice(value, value.toString(), testTag = "daily-goal-option-$value")
            },
            onSelect = onDailyGoalChange,
            onDismiss = { openDialog = null },
        )
```

- [ ] **Step 4: Wire it in `App.kt`**

Collect the flow (next to `newCardLimit`):

```kotlin
val dailyGoal by settingsViewModel.dailyGoal.collectAsState()
```

Pass to `SettingsScreen(...)` (after the `onNewCardLimitChange` argument):

```kotlin
                                dailyGoal = dailyGoal,
                                onDailyGoalChange = settingsViewModel::setDailyGoal,
```

- [ ] **Step 5: Verify it compiles and existing settings tests pass**

Run: `./gradlew :composeApp:compileAndroidMain :composeApp:testAndroidHostTest --tests "*Settings*"`
Expected: BUILD SUCCESSFUL, tests pass. (If `SettingsPresentationTest` asserts a fixed set of rows or dialogs, add the analogous `daily-goal` expectations; otherwise no change is needed.)

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/settings/SettingsViewModel.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/screens/SettingsScreen.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/screens/SettingsPresentation.kt \
  composeApp/src/commonMain/kotlin/com/mochi/App.kt
git commit -m "feat: configure the daily goal in Settings"
```

---

### Task 6: Surface the ring in the Library + Mochi reaction

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/LibraryScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/App.kt`

- [ ] **Step 1: Add the ring to the Library header**

In `LibraryScreen.kt`, add a parameter (after `units`):

```kotlin
    dailyGoal: com.mochi.goal.DailyGoalState,
```

Replace the header `Text("Library", …)` / subtitle block so the ring sits to its right. Change the
top of the `Column` body to:

```kotlin
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Library", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${units.size} units • tap to study",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DailyGoalRing(
                reviewsToday = dailyGoal.reviewsToday,
                goal = dailyGoal.goal,
                progress = dailyGoal.progress,
                reached = dailyGoal.reached,
                modifier = Modifier.size(64.dp),
            )
        }
```

Add imports:

```kotlin
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import com.mochi.ui.components.DailyGoalRing
```

(`Column`, `Alignment`, `fillMaxWidth`, `dp` are already imported in this file.)

- [ ] **Step 2: React with the mascot on `goalReached`**

In `FlashcardScreen.kt`, add a parameter (after `correctMilestone`):

```kotlin
    goalReached: Boolean,
```

Replace the mascot call at the bottom of the outer `Box` so it reacts to either the milestone or the
goal crossing (confetti stays tied to the milestone only):

```kotlin
        val mascotReact = remember(correctMilestone, goalReached) {
            if (correctMilestone != null || goalReached) Any() else null
        }
        ConfettiBurst(trigger = correctMilestone, modifier = Modifier.fillMaxSize())
        MochiMascot(greet = greetTrigger, react = mascotReact, modifier = Modifier.fillMaxSize())
```

- [ ] **Step 3: Wire both in `App.kt`**

Compute the ring state from the two reactive sources already collected (`stats` and `dailyGoal`).
Add, near the other derived values (after `val dailyGoal by …`):

```kotlin
val dailyGoalState = remember(stats.reviewsToday, dailyGoal) {
    com.mochi.goal.toDailyGoalState(stats.reviewsToday.toInt(), dailyGoal)
}
```

Thread it into `ReviewContent` — update the call site and signature:

```kotlin
Tab.REVIEW -> ReviewContent(reviewState, units, dailyGoalState, reviewViewModel)
```

```kotlin
private fun ReviewContent(
    state: ReviewUiState,
    units: List<UnitSummary>,
    dailyGoalState: com.mochi.goal.DailyGoalState,
    viewModel: ReviewViewModel,
) {
```

Pass it to `LibraryScreen` (in the `ReviewUiState.Idle` branch):

```kotlin
                ReviewUiState.Idle -> LibraryScreen(
                    units = units,
                    dailyGoal = dailyGoalState,
                    onOpenUnit = viewModel::openUnit,
                    sharedScope = this@SharedTransitionLayout,
                    animatedScope = this@AnimatedContent,
                )
```

Pass `goalReached` to `FlashcardScreen` (in the `is ReviewUiState.Reviewing` branch, after
`correctMilestone = s.correctMilestone,`):

```kotlin
                    goalReached = s.goalReached,
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :composeApp:compileAndroidMain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/screens/LibraryScreen.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt \
  composeApp/src/commonMain/kotlin/com/mochi/App.kt
git commit -m "feat: show the daily-goal ring and cheer on reaching it"
```

---

### Task 7: Quality gate, simulator smoke-test, docs

**Files:**
- Modify: `docs/CONTEXT.md`
- Modify: `docs/ROADMAP.md`

- [ ] **Step 1: Run the complete quality gate**

Run:

```bash
./gradlew :composeApp:iosSimulatorArm64Test :composeApp:testAndroidHostTest \
  :composeApp:ktlintCheck :composeApp:detekt \
  :composeApp:compileAndroidMain :composeApp:compileKotlinIosSimulatorArm64 \
  :composeApp:linkDebugFrameworkIosSimulatorArm64
```

Expected: BUILD SUCCESSFUL; all tests and static analysis pass. Fix any ktlint/detekt findings in
the new files (follow the repo's existing style; add `@Suppress` only with a one-line rationale, as
elsewhere in the codebase).

- [ ] **Step 2: Simulator smoke-test**

Build and run the iOS app (a booted simulator is required; the panel is optional). Verify:

- the Library header shows the ring with today's count and it fills as reviews are answered;
- changing "Daily goal" in Settings updates the ring's denominator;
- answering the review that reaches the goal triggers the Mochi mascot reaction on the study screen;
- the ring shows the completed (✓) state once `reviewsToday >= goal`;
- both Full and Reduced motion behave (Reduced: static fill, no wave; counter fades).

Also do a quick Android build sanity check if convenient. Record device-only concerns as N/A (no
physical iOS device).

- [ ] **Step 3: Update docs**

- In `docs/ROADMAP.md`, mark section "3. Daily goal and progress ring" complete with a one-line
  summary (configurable goal, Library ring, Mochi reaction, reduced-motion) and the simulator
  verification status.
- In `docs/CONTEXT.md`, add the daily-goal feature to the relevant section: the `dailyGoal` setting,
  the `DailyGoalRing` in the Library header, the `toDailyGoalState` pure mapping reusing
  `reviewsToday`, and the one-shot `goalReached` signal reusing the study-screen mascot.

- [ ] **Step 4: Commit**

```bash
git add docs/CONTEXT.md docs/ROADMAP.md
git commit -m "docs: document the daily goal and progress ring"
```

- [ ] **Step 5: Review before integration**

Invoke `superpowers:requesting-code-review`, address only verified findings, and rerun the complete
quality gate. Use `superpowers:finishing-a-development-branch` for the no-push handoff.

## Self-Review

**Spec coverage:** goal counts reviews-today (Task 3 uses `reviewsOnDay`, Task 6 uses
`stats.reviewsToday`); configurable 10/20/30/50 default 20 always-on (Tasks 1, 5); ring in Library
header (Tasks 4, 6); animated count + completed state (Task 4); Mochi reacts in-the-moment (Tasks 3,
6); reduced-motion (Task 4 + AnimatedCounter); once-per-day crossing (Task 3); tests for mapping,
crossing, settings (Tasks 1–3); platform verification + smoke-test (Task 7). All covered.

**Placeholders:** none — every code step shows full code.

**Type consistency:** `DailyGoalState`/`toDailyGoalState` (`com.mochi.goal`) used identically in
Tasks 2, 6; `DailyGoalSource` (`com.mochi.settings`) defined in Task 1, consumed in Task 3;
`ReviewCountSource` (`com.mochi.review`) defined in Task 3 and implemented on `StatsStore` there;
`goalReached: Boolean` consistent across `ReviewUiState`, `ReviewViewModel`, `FlashcardScreen`,
`App.kt`; `dailyGoal`/`setDailyGoal` consistent across `SettingsStore`, `SettingsViewModel`,
`SettingsScreen`, `App.kt`.
