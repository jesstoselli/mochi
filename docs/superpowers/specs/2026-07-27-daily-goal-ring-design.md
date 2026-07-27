# Daily Goal and Progress Ring

**Date:** 2026-07-27
**Status:** Approved design

## Summary

Mochi gains a configurable **daily review goal** surfaced as a liquid progress **ring** in the
Library header, plus an in-the-moment Mochi celebration when the goal is reached during study. The
feature reuses existing data (`reviewsToday`), existing motion primitives (`AnimatedCounter`,
`MotionPolicy`, the `LiquidProgress` sine-wave idea), and the existing `MochiMascot` — no new data
schema and no new platform code.

## Product decisions

- The goal counts **reviews completed today** (any answer, correct or not) — the same
  `reviewsToday` the Stats screen already shows. No new column or query.
- The goal is configured in **Settings → Study rhythm** with fixed choices **10 / 20 / 30 / 50**,
  default **20**, always on (no "off" state). Mirrors the existing "New cards" preference exactly.
- The ring lives in the **Library header** (the Review-tab landing screen), above the units grid,
  and greets the learner on every open.
- The ring shows today's progress (`reviewsToday / goal`) with the count animated by
  `AnimatedCounter`. When `reviewsToday >= goal` it shows a **completed state** (full ring + a
  "Goal reached" label/check).
- The **celebration is a one-shot Mochi reaction on the study screen**, fired at the instant the
  review that crosses the goal is answered — reusing the `MochiMascot` already present there via a
  `goalReached` signal analogous to the existing `correctMilestone`. It fires at most once per day.
- **Reduced motion**: the ring uses a static fill with a fade instead of the animated wave, and the
  celebration follows the mascot's existing reduced-motion behavior. Driven by the shared
  `MotionPolicy`, consistent with the rest of the app.

## Architecture

Four cooperating units, each independently testable:

1. **`SettingsStore` (extended)** — add `dailyGoal(): Int` / `setDailyGoal(Int)` over a new
   `KEY_DAILY_GOAL` with `DEFAULT_DAILY_GOAL = 20`, exactly like `newCardLimit`. A narrow
   `DailyGoalSource { fun dailyGoal(): Int }` interface lets ViewModels use a fake in tests.

2. **`DailyGoalViewModel` + pure state** — combines the reactive `reviewsToday` (from `StatsStore`)
   with the `dailyGoal` setting into a `DailyGoalUiState(reviewsToday, goal, progress, reached)`
   exposed as a `StateFlow`, using the same `stateIn(WhileSubscribed(5s))` pattern as
   `StatsViewModel`. The mapping is a **pure function** `toDailyGoalState(reviewsToday, goal)`:
   `progress = (reviewsToday / goal).coerceIn(0f, 1f)` (goal is always ≥ 10, so no divide-by-zero),
   `reached = reviewsToday >= goal`.

3. **`DailyGoalRing` composable** — a presentation-only circular liquid gauge: a ring stroke whose
   interior fills with a sine wave up to `progress` (the circular analogue of `LiquidProgress`),
   with the `AnimatedCounter` count in the center ("12 / 20") and a completed state at 100%. Takes
   `progress`, `reviewsToday`, `goal`, `reached`, `modifier`; reads `LocalMotionPolicy` for the
   reduced-motion fallback. No data or ViewModel dependency.

4. **Goal-reached signal (study flow)** — `ReviewViewModel` gains a one-shot `goalReached` signal
   (mirroring `correctMilestone`) in its UI state. It reads the day's review count and the goal
   through narrow injected sources, and when logging an answer moves the count from `< goal` to
   `>= goal` for the first time that day, it raises the signal, which clears on the next state
   emission. `FlashcardScreen` passes it to the existing `MochiMascot`'s `react` parameter.

`App.kt` wires the `DailyGoalViewModel` and passes `DailyGoalUiState` into the Library screen; the
Settings screen/ViewModel gain the goal preference alongside the existing ones.

## Data flow

- **Ring:** `StatsStore.reviewsOnDay(today)` + `SettingsStore.dailyGoal()` → `DailyGoalViewModel`
  → `DailyGoalUiState` → Library header `DailyGoalRing`. Fully reactive: answering a card updates
  the review log, which re-emits `reviewsToday`, which updates the ring.
- **Celebration:** answer logged in `ReviewViewModel` → crossing detected against
  `reviewsToday`/`goal` → one-shot `goalReached` in `ReviewUiState.Reviewing` → `FlashcardScreen` →
  `MochiMascot(react = goalReached)`.

## Once-per-day semantics

The crossing is detected in the review flow at the transition `< goal → >= goal`. Because it fires
only on the upward crossing (not while already at/over the goal), it naturally fires once per day:
subsequent reviews keep `reviewsToday >= goal` without re-crossing, and a new day resets
`reviewsToday` to 0 so the next crossing re-arms. No persisted "celebrated" flag is required for
Approach A (the study-screen reaction), which is why it is preferred over a Library-hosted mascot.

## Error handling

- `dailyGoal()` falls back to `DEFAULT_DAILY_GOAL` on a missing/garbage stored value, like
  `newCardLimit`.
- `progress` is always clamped to `[0f, 1f]`; the goal choices guarantee `goal >= 10`.
- The ring is presentation-only and cannot fail; a stale or zero `reviewsToday` simply renders an
  empty ring.

## Testing and verification

Automated (commonTest, JVM host + iOS simulator):

- `toDailyGoalState`: progress fraction, clamping at/over goal, `reached` boundary
  (`reviewsToday == goal` is reached).
- `SettingsStore` daily-goal read/write, default, and garbage fallback.
- `ReviewViewModel`: `goalReached` fires exactly on the `< goal → >= goal` crossing, does **not**
  refire on further reviews the same day, and clears on the next emission.
- `DailyGoalViewModel`: emits the mapped state from combined sources (with fakes).

Platform verification: Android host tests, iOS simulator tests, Android + iOS compilation, iOS
framework link, ktlint, detekt. A simulator smoke-test confirms the ring renders and fills, the
count animates, the Settings row changes the goal, and the Mochi reaction fires on crossing (in both
Full and Reduced motion).

## Out of scope

- Progressive Mochi moods and reactions to other events (roadmap item 5).
- A weekly/streak goal, goal history, or per-unit goals.
- Turning the goal off / hiding the ring.
- Any change to how reviews are counted or logged.
