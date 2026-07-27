# Mochi Product Roadmap

This roadmap records the agreed order for the next product improvements. Each phase gets its own
design spec, implementation plan, test cycle, and granular commits. Nothing is pushed by the
assistant.

## Completed foundation

- Card flip haptic feedback.
- One-shot haptic tick when a swipe crosses the answer threshold.
- 3D card flip, physics swipe, shared-element navigation, liquid progress, confetti, animated
  counters, and the Mochi session companion.

## 1. Reduced-motion accessibility — Complete

- Added `System`, `Full`, and `Reduced` modes with `Full` as the default.
- `System` observes Android's animator duration scale and iOS Reduce Motion.
- A shared `MotionPolicy` controls the complete animation system.
- Direct-manipulation gestures and haptics remain available in every mode.
- Reduced variants use short fades and static states for cards, shared elements, confetti, liquid
  progress, counters, press feedback, navigation, the Mochi companion and session completion.
- Settings now uses grouped preference cards with focused selection dialogs.
- Android host tests, iOS simulator tests, Android/iOS compilation, ktlint and detekt pass.

## 2. iOS platform parity

- Pronunciation playback with `AVAudioPlayer` — **Complete** (in-memory `NSData` over the `ambient`
  audio session, replace-not-overlap). Simulator smoke-test passed: session activation, rapid-tap
  replacement, and release all crash-free.
- Load the bundled Mochi UI and Japanese fonts on iOS — **Complete** (single commonMain
  `rememberMochiFonts` over the shared `Res.font` accessors; both platform font loaders removed).
  Verified rendering on the iOS 17 simulator (Nunito UI + Zen Maru Japanese).
- Validate audio, typography, reminders, and shared UI — simulator smoke-test **done** (audio,
  fonts, navigation). No physical iOS device is available, so real-device-only behaviors
  (Ring/Silent, audio routing, cross-app mixing, reminder firing) stay on their API contracts.

## 3. Daily goal and progress ring — Complete

- Configurable daily review goal in Settings (10/20/30/50, default 20, always on).
- Library-header liquid ring shows today's global review progress with an animated count, filling
  reactively as reviews are logged; completed (✓) state once the goal is met.
- The Mochi mascot cheers in-the-moment on the study screen when a review crosses the goal
  (one-shot `goalReached` from the review flow, reusing the existing mascot).
- Reduced-motion presentation (static fill, no wave) via `MotionPolicy`.
- Verified on the iOS 17 simulator: ring renders and fills, the Settings goal change updates it
  reactively (8/20 → 8/10), and reviews cross the goal (reviews-today reached 10/10).

## 4. Unit completion celebration

- Detect the transition to `50/50` learned cards.
- Keep a completion badge on the unit card.
- Trigger a special one-shot celebration, with an accessible reduced-motion alternative.

## 5. Mochi moods

- Add a gentle encouragement reaction after an incorrect answer.
- Make Mochi progressively happier as the current unit fills.
- Reuse daily-goal and unit-completion events for special reactions.
- Define reaction priority so simultaneous events do not overlap.

## 6. Learning path

- Present units as a sequential path with locked, available, active, and completed states.
- Place Mochi at the learner's current position.
- Never relock a unit that has already been started.
- Keep due reviews accessible independently from progression locks.
- Start with an 80% previous-unit unlock threshold, subject to confirmation during its design phase.
- Provide a reduced-motion path experience without large spatial movement.

## Working agreements

- App code, documentation, and commit messages remain in English; product discussion can be in
  Portuguese.
- Shared UI and behavior stay in `commonMain`; platform APIs use narrow Android/iOS adapters.
- Screens receive state and emit events; animation code stays independent from persistence and SRS.
- New behavior is developed test-first and verified on Android and iOS where applicable.
- Commits stay small and logically separated. The assistant never pushes.
