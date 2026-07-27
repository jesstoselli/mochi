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

- Pronunciation playback with `AVAudioPlayer` — **Implemented** (in-memory `NSData` over the
  `ambient` audio session, replace-not-overlap). The full iOS app builds and the controller logic is
  covered by simulator unit tests; a runtime simulator smoke-test is still pending.
- Load the bundled Mochi UI and Japanese fonts on iOS — pending (next item).
- Validate audio (Ring/Silent, routing, cross-app mixing), typography, reminders, and shared UI on a
  real iOS device — pending.

## 3. Daily goal and progress ring

- Add a configurable daily review goal.
- Show today's global review progress with a liquid ring and animated count.
- Add a completed state and a reduced-motion presentation.

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
