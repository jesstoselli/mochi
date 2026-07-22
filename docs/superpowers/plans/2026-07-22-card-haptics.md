# Card Haptic Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add light haptic feedback when a flashcard flips and a one-shot threshold haptic when a post-flip swipe becomes eligible for dismissal.

**Architecture:** `FlashcardScreen` owns `LocalHapticFeedback`. `FlipCard` keeps its existing callback-only API, while `swipeToDismissCard` exposes a new `onThresholdCrossed` event backed by a pure, unit-tested state transition that rearms below the threshold.

**Tech Stack:** Kotlin 2.2.20, Compose Multiplatform 1.10.3, commonMain/commonTest, kotlin.test.

## Global Constraints

- Keep all behavior in `commonMain`; do not add platform-specific implementations.
- Use `HapticFeedbackType.VirtualKey` for card flips.
- Use `HapticFeedbackType.GestureThresholdActivate` for swipe threshold activation.
- Keep the existing dismissal threshold at `0.35f` and preserve current swipe physics.
- Emit no threshold feedback before the card is flipped.
- Commit granularly and never push.

---

### Task 1: Testable swipe-threshold transition

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/SwipeToDismissCard.kt`
- Create: `composeApp/src/commonTest/kotlin/com/mochi/ui/motion/SwipeThresholdTest.kt`

**Interfaces:**
- Produces: `internal data class SwipeThresholdUpdate(val isOutside: Boolean, val crossedNow: Boolean)`
- Produces: `internal fun updateSwipeThreshold(wasOutside: Boolean, progress: Float, enabled: Boolean): SwipeThresholdUpdate`
- Produces: optional callback `onThresholdCrossed: () -> Unit = {}` on `Modifier.swipeToDismissCard`.

- [ ] **Step 1: Write failing state-transition tests**

```kotlin
package com.mochi.ui.motion

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwipeThresholdTest {
    @Test
    fun crossingThresholdEmitsOnceUntilRearmed() {
        val first = updateSwipeThreshold(wasOutside = false, progress = 0.36f, enabled = true)
        assertTrue(first.isOutside)
        assertTrue(first.crossedNow)

        val held = updateSwipeThreshold(wasOutside = first.isOutside, progress = 0.8f, enabled = true)
        assertTrue(held.isOutside)
        assertFalse(held.crossedNow)

        val rearmed = updateSwipeThreshold(wasOutside = held.isOutside, progress = 0.1f, enabled = true)
        assertFalse(rearmed.isOutside)
        assertFalse(rearmed.crossedNow)

        val crossedAgain = updateSwipeThreshold(wasOutside = rearmed.isOutside, progress = -0.4f, enabled = true)
        assertTrue(crossedAgain.crossedNow)
    }

    @Test
    fun disabledSwipeNeverCrossesThreshold() {
        val update = updateSwipeThreshold(wasOutside = false, progress = 1f, enabled = false)
        assertFalse(update.isOutside)
        assertFalse(update.crossedNow)
    }
}
```

- [ ] **Step 2: Verify RED**

Run:

```bash
./gradlew :composeApp:testAndroidHostTest --tests '*SwipeThresholdTest*'
```

Expected: compilation failure because `updateSwipeThreshold` does not exist.

- [ ] **Step 3: Implement the pure transition**

Add beside `DISMISS_THRESHOLD`:

```kotlin
internal data class SwipeThresholdUpdate(
    val isOutside: Boolean,
    val crossedNow: Boolean,
)

internal fun updateSwipeThreshold(
    wasOutside: Boolean,
    progress: Float,
    enabled: Boolean,
): SwipeThresholdUpdate {
    val isOutside = enabled && abs(progress) > DISMISS_THRESHOLD
    return SwipeThresholdUpdate(
        isOutside = isOutside,
        crossedNow = isOutside && !wasOutside,
    )
}
```

- [ ] **Step 4: Wire the transition into the gesture modifier**

Add `onThresholdCrossed: () -> Unit = {}` to `swipeToDismissCard`, retain it with
`rememberUpdatedState`, and keep `var thresholdOutside by remember { mutableStateOf(false) }`.
During `onDrag`, calculate the next horizontal progress, call `updateSwipeThreshold`, update
`thresholdOutside`, and invoke the callback only when `crossedNow`. Reset the state in both
`onDragEnd` and `onDragCancel`.

- [ ] **Step 5: Verify GREEN**

Run:

```bash
./gradlew :composeApp:testAndroidHostTest --tests '*SwipeThresholdTest*'
```

Expected: 2 tests pass, 0 failures.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/motion/SwipeToDismissCard.kt \
  composeApp/src/commonTest/kotlin/com/mochi/ui/motion/SwipeThresholdTest.kt
git commit -m "feat: emit one-shot swipe threshold events"
```

### Task 2: Perform haptics from FlashcardScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt`

**Interfaces:**
- Consumes: `onThresholdCrossed: () -> Unit` from Task 1.
- Uses: `LocalHapticFeedback`, `HapticFeedbackType.VirtualKey`, and
  `HapticFeedbackType.GestureThresholdActivate` from Compose UI.

- [ ] **Step 1: Add presentation-layer haptic ownership**

Inside `FlashcardScreen`, read:

```kotlin
val haptics = LocalHapticFeedback.current
```

Update the card callbacks:

```kotlin
onFlip = {
    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
    isFlipped = !isFlipped
},
modifier = Modifier.swipeToDismissCard(
    enabled = isFlipped,
    onDismiss = { right -> onAnswer(right) },
    onThresholdCrossed = {
        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    },
),
```

- [ ] **Step 2: Run complete verification**

```bash
./gradlew :composeApp:testAndroidHostTest \
  :composeApp:ktlintCheck \
  :composeApp:detekt \
  :composeApp:compileAndroidMain \
  :composeApp:compileKotlinIosSimulatorArm64
```

Expected: `BUILD SUCCESSFUL`, all tests pass, Android and iOS compilation succeeds.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt
git commit -m "feat: add haptic feedback to card flip and swipe"
```
