# Card Haptic Feedback — Design Spec

## Goal

Make the primary study interaction more tactile without producing continuous or noisy vibration:

- a light haptic whenever the user taps the card to flip it;
- a threshold haptic when a post-flip swipe becomes eligible to rate the card.

The behavior must work from shared `commonMain` code on Android and iOS through Compose
Multiplatform's haptic abstraction.

## Interaction design

### Flip feedback

Each accepted card tap that toggles front/back emits `HapticFeedbackType.VirtualKey`. Compose maps
this to a light impact on iOS and the corresponding platform effect on Android. The feedback occurs
immediately before changing `isFlipped`, so the physical response accompanies the interaction.

### Swipe threshold feedback

`swipeToDismissCard` keeps its existing 35% dismissal threshold. While the flipped card is dragged:

1. Crossing from below the threshold to either eligible side emits one
   `HapticFeedbackType.GestureThresholdActivate` event.
2. Remaining beyond the threshold does not emit additional events.
3. Returning below the threshold rearms feedback.
4. Crossing again — on the same side or the opposite side — emits a new event.
5. Pre-flip resisted dragging never emits threshold feedback because dismissal is disabled.

This gives the learner a precise physical cue for when releasing the card will count as an answer.

## Architecture

`FlashcardScreen` owns `LocalHapticFeedback`, keeping platform feedback at the presentation layer.
Reusable components remain data-agnostic:

- `FlipCard` continues to emit `onFlip`; the screen performs the haptic and toggles state.
- `Modifier.swipeToDismissCard` gains an `onThresholdCrossed` callback. It detects the transition
  into the eligible region but does not know which haptic effect the caller will use.

Threshold state is represented by a small pure function/state transition in `ui.motion`, allowing
the rearm and one-shot behavior to be unit-tested without pointer input or a device.

## Testing

Common tests cover:

- no event below the threshold;
- one event on first crossing;
- no repeated event while remaining outside;
- rearm after returning inside;
- a second event after recrossing;
- disabled/pre-flip dragging never triggers feedback.

Compilation verifies that `VirtualKey` and `GestureThresholdActivate` are available for both Android
and iOS targets in Compose Multiplatform 1.10.3. Existing review-flow tests and linters remain green.

## Scope

No changes to swipe distance, dismissal physics, answer semantics, visual overlays, persistence or
platform-specific haptic implementations. Compose's built-in platform mapping is used as-is.
