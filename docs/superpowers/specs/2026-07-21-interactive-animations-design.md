# Mochi — Interactive Animations Design

**Date:** 2026-07-21
**Status:** Approved (design phase)
**Scope:** Add four interactive-animation features to the existing Compose Multiplatform app,
introducing a unit-based Library as the entry point to study sessions. All code lives in
`commonMain` (no external animation libraries); the existing "Mochi Box" visual identity is
preserved — vibrancy comes from motion and accents, not a theme repaint.

## Goals

- Make studying tactile and rewarding: physical 3D flip, physics-based swipe rating,
  particle celebrations, animated counters/liquid progress, and a shared-element transition
  from the Library into a study session.
- Keep the shared codebase clean and portfolio-grade: reusable gesture/animation modifiers in
  a dedicated design-system package, strict state hoisting, animations decoupled from data.
- Change no database schema (units are derived), and keep streak/stats/"Still learning" global.

## Approved decisions

| Topic | Decision |
|---|---|
| Swipe model | Swipe enabled **only after the card is flipped**; before flip it resists and springs back |
| Library | A grid of **30 units of 50 words**, membership derived from frequency rank |
| Shared element | The tapped unit card **expands** into the study session; contracts back on Done |
| Aesthetic | Keep the Mochi identity; vibrancy from motion + accents (colored grid, confetti, streak glow) |
| Session scope | Sessions are **per-unit**; the daily new-card limit stays **global** |
| Streak milestones | Confetti burst every **10** consecutive correct answers within a session (10, 20, 30…) |

## Non-goals (YAGNI)

- No new database columns or migrations (units are computed, not stored).
- No re-tagging of words into semantic categories (verb/JLPT/etc.).
- No full theme/palette redesign; `Color.kt` base palette is untouched.
- No per-unit streaks, per-unit daily limits, or per-unit stats.
- iOS audio/bundled fonts remain deferred (unchanged by this work).

---

## Section 1 — Architecture & Navigation

### Unit model (derived, no schema change)

- The 1500 cards are ordered by `frequency ASC` and sliced by **rank**: `unitId = rank / 50`,
  giving 30 units (`Unit 0` = ranks 0–49, the most common words, … `Unit 29` = ranks 1450–1499).
- Unit membership is computed in `DeckRepository` (via a `ROW_NUMBER()`-ordered query or by
  indexing the frequency-ordered list in Kotlin). No `flashcard` table changes.

### Navigation flow

The **Review** tab is wrapped in a `SharedTransitionLayout`. Its landing state changes from the
single-button `HomeScreen` to a `LibraryScreen` grid.

```
Library (grid of 30 unit cards)
   │  tap a unit
   ▼  [sharedElement: the unit card expands to full screen]
Session for that unit (Reviewing)
   ▼  finish
SessionComplete (confetti)
   ▼  Done  [sharedElement contracts back into the grid]
Library
```

### Session behavior (per-unit, global daily limit)

- A unit session pulls that unit's cards that are **due** plus its **new** cards, capped by the
  **remaining global daily new-card limit** (the limit is not split across units).
- **Streak, Stats, and the "Still learning" list stay global** — none are sliced per unit.
- In-session relearning (Anki-style requeue of missed cards) is unchanged.
- "Practice anyway" becomes "practice this unit" when the unit has nothing due
  (still logged with `practice = 1`, excluded from streak/daily stats as today).

### Code impact

- **`DeckRepository`**
  - `fun unitSummaries(): Flow<List<UnitSummary>>` — reactive (SQLDelight Flow) so the Library
    updates itself as cards are learned/become due.
  - `suspend fun cardsForUnit(unitId: Int): List<Flashcard>` — the unit's due + new cards,
    respecting the remaining global daily new-card allowance.
  - `UnitSummary(unitId, learnedCount, totalCount = 50, dueCount, sampleFront)` — data for a grid card.
    - `learnedCount` = cards in the unit with `next_review IS NOT NULL` (matches the existing
      "words learned" stat, `countStarted`), so the ring fill is consistent with Stats.
    - `dueCount` = cards in the unit already due for review now (`next_review <= now`), i.e. the
      "due" badge counts review-ready cards only; brand-new (never-seen) cards are not counted as
      due but are still offered in the session up to the daily limit.
- **`ReviewViewModel`**
  - `startSession(unitId: Int)` (was parameterless). The `Home` UI state is replaced by
    `Library(units: List<UnitSummary>)`. The rest of the state machine
    (`Reviewing` → `Complete`) is unchanged.
  - Derives a **session streak**: a counter of consecutive correct answers, reset to 0 on a miss;
    exposes a one-shot signal when it crosses a multiple of 10 so the UI can fire confetti.
- **`App.kt`** — wraps the Review tab content in `SharedTransitionLayout` + `AnimatedContent`
  between `Library` and `Reviewing`/`Complete`. Learning, Stats, Settings tabs are untouched.
- **`HomeScreen`** — retired; its "start/practice/refresh" affordances move into `LibraryScreen`
  (start = tap a unit; refresh = the reactive Flow; practice = per-unit).

---

## Section 2 — Animation features

### 2.1 — 3D Flip + Physics Swipe

Evolves `ui/components/FlipCard.kt`, decomposed into reusable modifiers.

- **Flip 3D:** replace the `tween` on `rotationY` with a `spring`. Keep `cameraDistance` and the
  existing "hide front content past 90°" logic. Add a **dynamic shadow**: shadow/elevation in
  `graphicsLayer` peaks around 90° so the card visibly lifts mid-flip.
- **`Modifier.swipeToDismissCard(enabled: Boolean, onDismiss: (right: Boolean) -> Unit)`**
  - Wraps `pointerInput` + `detectDragGestures`; an `Animatable<Offset>` tracks displacement.
  - **Before flip (`enabled = false`):** displacement is damped (~0.2×, clamped) and springs back
    to center on release (`DampingRatioMediumBouncy`) — teaches "flip first".
  - **After flip:** free drag. `rotationZ` (tilt) and `alpha` scale with distance from center.
    On release past the **threshold (~35% of width)** → animate off-screen (elastic) and call
    `onDismiss(right)`; otherwise spring back.
  - **Intent overlay:** during drag, a stamp fades in — green **知ってる ✓** (right) /
    amber **まだ** (left).
- **Buttons coexist:** `AnswerButtons` remain and call the same `onAnswer`. Right = correct
  (`I knew it`), left = wrong (`Still learning`).

### 2.2 — Reward micro-interactions (particles)

- **`ConfettiBurst` (pure Canvas)**, same spirit as `ui/components/SuccessAnimation.kt`.
  - `data class Particle(position: Offset, velocity: Offset, color: Color, size: Float, rotation: Float)`.
  - A coroutine advances the system with `withFrameNanos`, applying velocity + light gravity and
    fading `alpha` to zero; draws stars/circles bursting from the center.
  - **Vibrant palette confined here** (per decision A) — the rest of the app stays Mochi-cozy.
  - **Triggers:** (1) completing a unit → large burst on `SessionComplete`; (2) session streak
    crossing a multiple of **10** → smaller burst over the card.
- **`Modifier.pressBounce()`** — extract the existing `BouncyButton` press-scale (~0.95 on press,
  spring back) into a reusable modifier applied consistently.

### 2.3 — Animated counters & liquid progress

- **`AnimatedCounter(value)`** — odometer effect via `AnimatedContent` with
  `slideInVertically`/`slideOutVertically`; used for the session streak HUD and Stats streak.
- **`LiquidProgress` (Canvas)** — `rememberInfiniteTransition` animates the phase of a sine wave
  drawn with `Path`, filling bottom-to-top. Used for (a) each unit card's progress ring/fill
  (`learned/50`) in the Library and (b) optionally the session progress bar
  (`FlashcardScreen.kt:62`, currently a flat `LinearProgressIndicator`).
- **Stats numbers** (reviews today, words learned) use `animateIntAsState` to count up smoothly.

### 2.4 — Shared Element Transition (Library → session)

- `SharedTransitionLayout` at the top of the Review tab; `AnimatedContent` between `Library` and
  `Reviewing`.
- The unit card and the study card share a `sharedElement` key (`"unit-<id>"`); the container
  expands from the grid to full screen while the surrounding grid `fadeOut`s. On Done it contracts
  back into the grid. The unit's pastel accent color rides along the transition.

---

## Section 3 — Structure, state hoisting, testing

### Package structure

New design-system package under `composeApp/src/commonMain/kotlin/com/mochi/ui/`:

- `com.mochi.ui.motion` — reusable, data-agnostic animation building blocks:
  - `SwipeToDismissCard.kt` (`Modifier.swipeToDismissCard(...)`)
  - `PressBounce.kt` (`Modifier.pressBounce()`)
  - `ConfettiBurst.kt` (particle system + `Particle`)
  - `LiquidProgress.kt`
  - `AnimatedCounter.kt`
- `FlipCard.kt` stays in `ui/components` but consumes the `motion` modifiers.
- `LibraryScreen.kt` added to `ui/screens`; `HomeScreen.kt` removed.

### State hoisting / testability

- All `motion` composables are **presentation-only**: they receive state (progress, enabled,
  particle trigger) and emit events (`onDismiss`, `onAnswer`) — no VM/DB/coroutine-scope coupling.
- `LibraryScreen(units, onOpenUnit)` and the evolved `FlashcardScreen` take `UiState` in and emit
  callbacks out, matching the codebase's existing hoisting convention.
- Animation triggers (confetti, streak milestone) are driven by state/one-shot events from the VM,
  not called imperatively from inside composables.

### Testing

- **ViewModel unit tests** (existing pattern with fakes): unit-scoped session assembly
  (`cardsForUnit` respects the remaining global daily limit), `unitId` routing, session-streak
  counter (increments on correct, resets on miss, fires the milestone signal at each multiple
  of 10).
- **Repository tests:** `unitSummaries` groups by frequency rank into 30 units of 50 with correct
  `learnedCount`/`dueCount`.
- Gesture/particle **visual** behavior is validated by running the app (Android first, then iOS),
  not by pixel assertions — consistent with how `SuccessAnimation`/`SevenDayChart` are handled.
- Keep ktlint/detekt clean (imports sorted, lines ≤120).

### Suggested phasing (for the implementation plan)

1. Unit model + `LibraryScreen` grid + reactive `unitSummaries` (no shared transition yet).
2. Per-unit sessions in `ReviewViewModel` (with global daily limit) + tests.
3. Flip spring + dynamic shadow + `swipeToDismissCard` (flip-gated) + intent overlay.
4. `ConfettiBurst` + session-streak milestone signal + unit-complete burst.
5. `LiquidProgress` (unit rings + session bar) + `AnimatedCounter` + Stats count-up.
6. `SharedTransitionLayout` wiring (Library ⇄ session) as the final polish pass.

### Platform notes

- Everything targets `commonMain`; verify on Android first, then confirm the iOS build in Xcode.
- No new dependencies expected (`SharedTransitionLayout`, `graphicsLayer`, `Canvas`,
  `withFrameNanos`, `detectDragGestures` are all in the current Compose Multiplatform 1.10.3).
