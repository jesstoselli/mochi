# Mochi Moods — Design (roadmap item 5)

**Goal:** Give the Mochi mascot emotional expressions that react to the learner's progress —
gentle encouragement after a miss, a face that grows happier as the current unit fills, and
special celebrations for the daily goal and unit completion, with a defined priority so
simultaneous events never overlap.

**Status:** Approved design, ready for implementation plan.

**Scope note:** Roadmap item 5 asks to reuse "unit-completion events". Item 4 (the 50/50
celebration screen) is not built. We do NOT depend on it: the unit-completion *signal* is derived
during review from the same unit-progress data we already need for the baseline mood, as a one-shot
identical in shape to the existing `goalReached`.

---

## 1. Expressions (the moods)

Five distinct faces, each drawn as vector-path variants inside `MochiLogo` on the existing
`0..64` viewBox. All share the current body, cheeks, belly, and stroke — only eyes, mouth, and
small extras change. A new `MochiMood` parameter selects the face.

| Mood          | Face                                                            |
|---------------|----------------------------------------------------------------|
| `Content`     | open eyes + small resting smile (the current face)             |
| `Happy`       | open eyes + wider smile, cheeks more present                   |
| `Radiant`     | big smile + sparkle/star eyes ✨                                |
| `Encouraging` | soft upward `^^` eyes + tiny sweat drop + gentle small mouth   |
| `Celebrating` | closed happy-arc eyes + open joyful mouth                      |

Face swaps are **static** (rendering a different set of paths), so they are always shown, including
under reduced motion. The spring pop-up and entry bounce remain gated by `MotionPolicy` exactly as
today — moods add no new motion.

## 2. Baseline mood — progressively happier as the unit fills

The mascot's resting/greet face scales with the current unit's fill ratio
(`learnedCount / totalCount` of `currentUnitId`), via a pure function:

```
moodForUnitRatio(ratio: Float): MochiMood
  ratio >= 0.67f -> Radiant
  ratio >= 0.34f -> Happy
  else           -> Content

moodForUnitProgress(learned: Int, total: Int): MochiMood =
  moodForUnitRatio(if (total <= 0) 0f else learned.toFloat() / total)
```

`total <= 0` yields `Content` (defensive; never expected in a real session). `FlashcardScreen`
uses `moodForUnitRatio` directly (it already has the ratio); the ViewModel/tests use the
count-based `moodForUnitProgress`.

## 3. Reactions (transient pops) + priority

A reaction is a one-shot mascot pop carrying a specific expression. When several trigger on the
same answer, exactly one is shown, resolved by a pure function in priority order (highest first):

```
resolveReaction(unitCompleted, goalReached, milestone: Int?, wrong): MochiMood?
  unitCompleted    -> Radiant       // 1. biggest achievement
  goalReached      -> Celebrating   // 2. daily goal
  milestone != null-> Celebrating   // 3. every-10 correct milestone
  wrong            -> Encouraging   // 4. gentle nudge after a miss
  else             -> null          // 5. no reaction; baseline rest face shows
```

Rationale for positive-over-encouragement: if a wrong answer also completes the unit or crosses the
goal (a wrong answer can still set `next_review` and still counts toward the daily review total),
the mascot celebrates the larger moment rather than consoling.

## 4. Data flow

### `ReviewViewModel` (new computed signals, per emission)

Alongside the existing `correctMilestone`, `goalReached`, `sessionStreak`, each `Reviewing`
emission now also carries:

- **`unitProgress: Float`** — `learnedCount / total` of `currentUnitId` after the answer is
  recorded. Drives the baseline mood.
- **`unitCompleted: Boolean`** — one-shot, true only on the emission where the unit's learned count
  first reaches its total. Guarded by `!practiceMode` (like `goalReached`).
- **`lastAnswerWrong: Boolean`** — one-shot, true only on the emission immediately following a
  wrong answer.

**Detecting `unitCompleted` without depending on write-through reads:** in `answer()`, before
`deck.recordAnswer`, capture `wasNew = (card.next_review == null)` and
`learnedBefore = deck.cardsInUnit(currentUnitId).count { it.next_review != null }`. A new card
becomes "learned" the first time it is answered, so:

```
total        = deck.cardsInUnit(currentUnitId).size
learnedAfter = learnedBefore + (if (wasNew && !practiceMode) 1 else 0)
unitCompleted = !practiceMode && learnedBefore < total && learnedAfter == total
unitProgress  = if (total <= 0) 0f else learnedAfter.toFloat() / total
```

`beginSession` emits with `unitCompleted = false`, `lastAnswerWrong = false`, and `unitProgress`
computed from the unit's current learned count (so the greet face reflects existing progress).

### `Reviewing` state (3 new fields)

```
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

### `FlashcardScreen` wiring

```
val restMood = moodForUnitRatio(unitProgress)
val reaction = resolveReaction(unitCompleted, goalReached, correctMilestone, lastAnswerWrong)
val mascotReact = remember(correctMilestone, goalReached, unitCompleted, lastAnswerWrong) {
    if (reaction != null) Any() else null
}
MochiMascot(
    greet = greetTrigger,
    react = mascotReact,
    restMood = restMood,
    reactMood = reaction ?: restMood,
    modifier = Modifier.fillMaxSize(),
)
```

`FlashcardScreen` already receives `unitProgress` as a ratio, so it uses the ratio-based
`moodForUnitRatio(ratio: Float)`. The count-based `moodForUnitProgress(learned, total)` is the
primary unit-tested entry; `moodForUnitRatio` delegates to the same thresholds (see §2).

Confetti stays tied to `correctMilestone` only — unchanged. `App.kt:220` threads the three new
fields from `Reviewing` into `FlashcardScreen`.

### `MochiMascot` signature

```
MochiMascot(
    greet: Any?,
    react: Any?,
    restMood: MochiMood = MochiMood.Content,
    reactMood: MochiMood = MochiMood.Content,
    modifier: Modifier = Modifier,
)
```

On a GREET event it renders `restMood`; on a REACT event it renders `reactMood`. The
`nextMascotEvent` greet-vs-react timing logic is unchanged. The mood is passed straight down to
`MochiLogo(mood = …)`.

## 5. Accessibility / reduced motion

- Expression swaps are static path renders → always shown, including reduced motion.
- The pop-up spring and entry bounce remain gated by `MotionPolicy.allowSpatialMotion` /
  `reduced`, exactly as the current mascot. Moods introduce no new animation.

## 6. Testing

**Pure functions (`MochiMoodTest`):**
- `moodForUnitProgress` boundaries: `0/50 → Content`, `16/50` (32%) → `Content`,
  `17/50` (34%) → `Happy`, `33/50` (66%) → `Happy`, `34/50` (68%) → `Radiant`,
  `50/50 → Radiant`, `total = 0 → Content`.
- `moodForUnitRatio` agrees with the count-based version at the same ratios.
- `resolveReaction` permutations: all-true → `Radiant`; `goal + milestone` → `Celebrating`;
  `milestone` only → `Celebrating`; `wrong` only → `Encouraging`; `wrong + goal` → `Celebrating`;
  `wrong + unitCompleted` → `Radiant`; none → `null`.

**`ReviewViewModel` tests:**
- `unitCompleted` fires exactly once, on the emission where the last new card of the unit is
  learned, and never in practice mode.
- `lastAnswerWrong` is true after a wrong answer and false after a correct one.
- `unitProgress` equals learned/total for the current unit after each answer.
- Practice mode: no `unitCompleted`, no `goalReached` (existing), baseline reflects progress.

**UI:** verified in the **iOS simulator** (the verification ceiling — no physical iOS device).
Confirm each expression renders and that reactions pick the correct face by priority.

## 7. Files

| File | Change |
|------|--------|
| `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiMood.kt` | **New** — `MochiMood` enum + `moodForUnitProgress` / `moodForUnitRatio` / `resolveReaction` pure functions |
| `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiLogo.kt` | Add `mood` param; author per-mood eye/mouth/extra vector paths |
| `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiMascot.kt` | Add `restMood`/`reactMood` params; render mood per event |
| `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewUiState.kt` | Add `unitProgress`, `unitCompleted`, `lastAnswerWrong` |
| `composeApp/src/commonMain/kotlin/com/mochi/review/ReviewViewModel.kt` | Compute the three new signals in `answer()` / `beginSession()` |
| `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt` | Derive `restMood`/`reaction`; wire `MochiMascot` |
| `composeApp/src/commonMain/kotlin/com/mochi/App.kt` | Thread the 3 new fields into `FlashcardScreen` (~line 220) |
| `composeApp/src/commonTest/kotlin/com/mochi/ui/components/MochiMoodTest.kt` | **New** — pure-function tests |
| `composeApp/src/commonTest/kotlin/com/mochi/review/ReviewViewModelTest.kt` | Add signal tests |
