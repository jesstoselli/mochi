# Mochi — Japanese Flashcards

Mochi is a Duolingo-inspired Japanese study app built with **Compose Multiplatform** for
Android and iOS. It turns the **Kaishi 1.5k** deck into 30 progressive study units and combines
Anki-style spaced repetition with tactile gestures, shared-element navigation, Canvas effects,
audio, statistics, reminders and a playful mochi companion — all from one shared Kotlin codebase.

The project started as an animation playground and evolved into a portfolio app focused on
polished interaction, reusable motion primitives and production-minded KMP architecture.

> Want to build and run it? See **[SETUP.md](SETUP.md)**.

## Preview

### Library and unit progress

![Mochi library](docs/screenshots/library.png)

<!-- Slot 1: library screenshot or short GIF showing the 30-unit grid and liquid progress. -->

### Study flow and interactions

![Mochi study flow](docs/screenshots/study-flow.gif)

<!-- Slot 2: screenshot, GIF or video thumbnail showing shared transition, flip, swipe and mascot. -->

## Features

- **30 progressive study units** — the 1,500 Kaishi words are grouped into units of 50 by
  frequency rank, each with learned progress and cards-due information.
- **Per-unit study sessions** — choose a unit and review its due cards plus new cards, while a
  configurable daily new-card limit remains global across the app.
- **Anki-style spaced repetition** — a simplified SM-2 scheduler adjusts intervals and ease;
  missed cards return to the end of the current queue until answered correctly.
- **Tactile 3D flashcards** — cards flip with spring physics, perspective and dynamic shadow.
  After revealing the answer, swipe right for *I knew it* or left for *Still learning*; short
  drags return elastically to the center. On-screen answer buttons remain available.
- **Continuous navigation** — a shared-element transition expands the selected unit card into
  the study session and contracts it back into the library.
- **Native Canvas rewards** — confetti celebrates session completion and every 10th cumulative
  correct answer within a session, while a hand-drawn mochi companion greets the learner and
  returns for those milestones.
- **Organic progress and counters** — unit cards use animated sine-wave liquid progress;
  statistics and the in-session streak use odometer-style number transitions.
- **Still learning list** — words whose latest answer was incorrect are collected in their own
  tab and disappear after a correct answer. Tapping a word plays its pronunciation.
- **Live statistics** — current daily streak, reviews today, words learned and a seven-day chart
  update reactively from SQLDelight flows.
- **Audio and reminders** — pronunciation playback on Android and configurable daily study
  reminders on Android and iOS. iOS pronunciation audio is pending, and reminders still await
  real-device validation on iOS.
- **Mochi Box theme** — System, Light and Dark modes, themed system bars, haptics, bundled fonts
  on Android and a theme-aware Android 12+ splash screen.

## Motion system

Animation code lives in shared `commonMain` code and is independent of persistence. Platform
capabilities such as haptics and audio are accessed through Compose or KMP abstractions. Screens
receive state and emit events; reusable motion building blocks include:

- `swipeToDismissCard` — drag resistance, tilt and spring dismissal/return.
- `pressBounce` — consistent press-scale feedback for interactive controls.
- `ConfettiBurst` — frame-driven Canvas particles with velocity, gravity and fading alpha.
- `LiquidProgress` — an infinitely animated sine-wave fill drawn with `Path`.
- `AnimatedCounter` — vertical odometer transitions for changing values.
- `MochiMascot` — a spring-driven greeting and milestone celebration.

## Architecture

- **MVVM with state hoisting** — screens are presentation-only; ViewModels own session and data
  state while interfaces keep dependencies replaceable in tests.
- **Review state machine** — `Loading → Idle → Reviewing → Complete`, with the Library displayed
  while idle and sessions opened by unit ID.
- **Reactive SQLDelight data** — Library, Learning and Stats queries are observed as `Flow` and
  exposed as `StateFlow`, so the UI updates without polling or manual refresh.
- **Shared UI, focused platform code** — Compose screens, gestures, animations and business logic
  live in `commonMain`; database drivers, audio, fonts, system bars and reminders use platform
  implementations where required.
- **Real persistence** — SQLDelight stores flashcards, settings and review history with migrations.
  The review log powers statistics, limits and the *Still learning* list.

## Tech stack

Kotlin Multiplatform · Compose Multiplatform · Material 3 · SQLDelight · Kotlin coroutines/Flow ·
`androidx.lifecycle` ViewModel · kotlinx.serialization · Android core-splashscreen · ktlint · detekt

## Quality

- Unit tests cover unit derivation, per-unit queues, the global daily limit, in-session relearning,
  cumulative per-session celebration milestones and particle physics.
- `commonTest` runs quickly on the JVM through the Android host-test target and can also run on
  the iOS simulator.
- ktlint and detekt are enforced while preserving the project's intentionally compact Kotlin style.
- Shared code is compile-checked for both Android and iOS.

Useful checks:

```bash
./gradlew :composeApp:testAndroidHostTest
./gradlew :composeApp:ktlintCheck :composeApp:detekt
./gradlew :composeApp:compileAndroidMain
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

## Project structure

```text
composeApp/   Shared KMP library: UI, data, ViewModels, motion and iOS framework
androidApp/   Android application entry points, notifications and platform resources
iosApp/       SwiftUI host application
docs/         Setup, technical context, design specs and media
```

## Credits

Cards come from the open-source **Kaishi 1.5k** Anki deck. Fonts are **Nunito** and
**Zen Maru Gothic** (OFL). Mochi is a portfolio project for exploring tactile interaction,
animation systems and Compose Multiplatform architecture.
