# Mochi — Japanese Flashcards

A Duolingo-inspired flashcard app for studying Japanese, built with **Compose Multiplatform**
(Android + iOS) around the **Kaishi 1.5k** deck (1500 words). It started as a playground for
**native animations** (flip + spring) and grew into a small but complete Anki-style study app:
spaced repetition, pronunciation audio, a "still learning" word list, live stats/streaks, a
daily new-card limit, theming, haptics, a playful tappable mascot and a theme-aware splash —
one shared Kotlin codebase across both platforms, with a four-tab bottom-nav layout
(Review · Learning · Stats · Settings).

> Looking to build and run it? See **[SETUP.md](SETUP.md)**.

## Screenshots

> Drop a PNG for each screen into `docs/screenshots/` and the images below render
> automatically. (Android shown; light + dark are both supported.)

### Home

![Home](docs/screenshots/home.png)
<!-- Mascot (taps to bounce) + もち/Mochi, how many cards are ready, "Start studying" / "Practice anyway". -->

### Review (flashcard)

![Review](docs/screenshots/review.png)
<!-- Progress bar, category pill, flip card (auto-sizing JP text), Listen button, answer buttons. -->

### Session complete

![Session complete](docs/screenshots/session-complete.png)
<!-- End-of-session Canvas checkmark celebration + Continue / Done. -->

### Still learning

![Still learning](docs/screenshots/learning.png)
<!-- Words whose latest answer was "Still learning": Japanese word + meaning, tap to hear it. -->

### Stats

![Stats](docs/screenshots/stats.png)
<!-- Streak, reviews today, words learned + 7-day bar chart. -->

### Settings

![Settings](docs/screenshots/settings.png)
<!-- Theme (System/Light/Dark) + new cards per day (10/20/30/Unlimited). -->

## Features

- **Anki-style spaced repetition** — each session is the day's queue: due reviews plus new
  cards up to a configurable daily limit, scheduled with a simplified SM-2 algorithm.
- **Flip cards with native animations** — spring-bounced flip, animated card-to-card
  transitions, a press "squish", and auto-sizing text so Japanese never overflows.
- **"Still learning" list** — words whose most recent answer was *Still learning* collect on
  their own tab (Japanese word + meaning, tap to hear it) so you can revisit them anytime; a
  word drops off once you answer *I knew it* — and free practice counts too.
- **Pronunciation audio** — tap *Listen* to hear each word (Android; iOS pending).
- **Stats & streaks** — current streak, reviews today, words learned, and a 7-day bar chart,
  all derived from a review log.
- **Live updates** — the stats and the "still learning" list are reactive: they refresh
  themselves the moment you answer a card, no manual reload.
- **Playful mascot** — a Canvas-drawn mochi that bounces in on launch and again on tap, with a
  soft "pop" sound and haptic feedback.
- **Tactile feel** — haptic feedback on buttons and settings, plus a theme-aware splash screen
  (Android 12+) so a dark-mode launch never flashes white.
- **Theming** — System / Light / Dark, with themed native system bars in dark mode.
- **End-of-session celebration** — a checkmark drawn entirely on a Compose Canvas (no Lottie,
  no assets), so it renders identically on both platforms.

## Tech stack

Kotlin Multiplatform · Compose Multiplatform · Material 3 · SQLDelight (reactive Flows) ·
`androidx.lifecycle` ViewModel (MVVM) · Kotlin coroutines/Flow · kotlinx.serialization ·
core-splashscreen · ktlint + detekt.

## Highlights

- **One shared UI** for Android and iOS via Compose Multiplatform, with `expect`/`actual` only
  where the platform differs (database driver, audio, fonts, system bars).
- **MVVM** — the study flow lives in a single `ReviewViewModel` state machine; screens are
  presentation-only (data in, callbacks out) and `App` is a thin host that routes tabs.
- **Reactive data layer** — SQLDelight queries are observed as Flows (`asFlow().mapToList`) and
  exposed as `StateFlow` via `stateIn`, so the UI reflects the database without polling or
  manual refresh.
- **Real persistence** — SQLDelight schema with migrations for cards, settings, and a review
  log that powers stats, the daily new-card limit and the "still learning" list.
- **Drawn, not imported** — the mascot and the success checkmark are pure Compose `Canvas`
  vector drawing (via `PathParser`), so there are no image assets to ship and they render
  identically on both platforms.
- **Tested** — `ReviewViewModel` is unit-tested with fakes (interfaces for the deck, counters
  and limits) using `kotlinx-coroutines-test`.

## Credits

Cards from the open-source **Kaishi 1.5k** Anki deck. Fonts: **Nunito** and **Zen Maru Gothic**
(both OFL). Built as a portfolio project to explore Compose Multiplatform.
