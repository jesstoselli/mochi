# Reduced Motion and Settings Redesign

**Date:** 2026-07-22  
**Status:** Approved design

## Summary

Mochi will add an explicit motion preference and adapt every existing animation to a balanced
reduced-motion presentation. The same change will reorganize Settings into compact grouped cards
that preserve Mochi's soft, playful identity while improving scanability, supporting text, touch
targets, and future growth.

This phase covers reduced-motion behavior across the app and accessibility of the redesigned
Settings screen. It does not include a complete screen-reader audit of every screen.

## Product decisions

The Motion preference has three values:

- `Full`: always use Mochi's complete animation language.
- `System`: follow the current platform accessibility preference.
- `Reduced`: always use the balanced reduced-motion presentation.

`Full` is the default for a missing, invalid, or unreadable stored value. This preserves the current
experience for existing users. Selecting an option applies and persists it immediately.

Balanced reduced motion means:

- direct manipulation continues to track the user's finger;
- haptic feedback remains enabled;
- short opacity transitions remain available;
- 3D rotation, bounce, particles, continuous waves, large spatial movement, and decorative travel
  are removed or replaced;
- Mochi's state and reward information remain visible instead of disappearing with the motion.

## Architecture

Three independent concepts keep persistence, platform APIs, and rendering separate.

### MotionPreference

`MotionPreference` is the user's persisted choice: `FULL`, `SYSTEM`, or `REDUCED`. It belongs to
the settings domain and is exposed by `SettingsViewModel` as state.

### SystemMotionPreference

`SystemMotionPreference` is a narrow platform adapter that exposes whether reduced motion is active
on the device. It updates while the app is running.

- Android observes the system animator duration scale. A zero scale means reduced motion.
- iOS reads `UIAccessibilityIsReduceMotionEnabled` and observes the corresponding accessibility
  setting-change notification.
- If a platform signal cannot be read, the adapter reports that reduced motion is inactive.

Platform implementations do not know about the saved Mochi preference or UI components.

### MotionPolicy

A pure resolver combines `MotionPreference` with the current platform signal:

| Saved preference | Platform reduced motion | Effective mode |
|---|---:|---|
| `FULL` | Either | Full |
| `REDUCED` | Either | Reduced |
| `SYSTEM` | `false` | Full |
| `SYSTEM` | `true` | Reduced |

The effective `MotionPolicy` is provided at the app root through a `CompositionLocal`. Reusable
animation components consume the policy directly. They do not read the database, ViewModel, or
platform API. Pure policy decisions are independently testable.

Changing the saved or native preference updates the policy immediately. An animation that is
running when reduced motion becomes active must settle into its valid final or static state without
triggering the original flourish.

## Animation behavior

| Experience | Full motion | Reduced motion |
|---|---|---|
| Card flip | Spring-driven 3D rotation and lift | Short crossfade between front and back |
| Card drag | Horizontal movement, tilt, and proportional opacity | Horizontal movement follows the finger without tilt |
| Confirmed swipe | Card is thrown off screen | Short fade, then answer immediately |
| Cancelled swipe | Elastic spring to center | Short non-bouncy return to center |
| Unit-to-session navigation | Shared-element expansion | Crossfade between library and session |
| Liquid progress | Continuously moving wave | Static progress fill at the same value |
| Animated counters | Vertical odometer | Short crossfade to the new number |
| Press feedback | Spring scale compression | Ripple and haptic only |
| Confetti rewards | Moving Canvas particles | Static celebratory glow or badge with a short fade |
| Mochi companion | Hops and spatial entrance/exit | Appears in place, holds, and fades |
| Completion artwork | Animated check, ring, and scale | Final check state with a short fade |
| Tab changes | Existing crossfade | Shorter crossfade |

Reduced motion changes presentation only. Review answers, thresholds, streak events, session
progress, navigation destinations, and haptic event timing remain unchanged.

## Settings redesign

Settings follows the approved **Mochi cards** direction.

### Page structure

- Title: `Settings`
- Subtitle: `Make Mochi feel right for you.`
- Introductory card:
  - title: `Study your way`
  - supporting text: `Tune the look, motion, and rhythm anytime.`
  - a small decorative Mochi illustration that remains static in reduced mode
- `Look & feel` grouped card:
  - Theme row
  - Motion row
- `Study rhythm` grouped card:
  - New cards row
  - Daily reminder row
  - Time row, visible only while the reminder is enabled

Each grouped card uses Mochi's existing colors, rounded shapes, and typography. Preference rows use
a small pastel icon tile, a primary label, concise supporting text, and the selected value at the
trailing edge. Decoration stays restrained so the introductory Mochi is the screen's single visual
signature.

### Preference interactions

Theme, Motion, and New cards each open a focused single-choice dialog. Tapping an option persists
the new value and closes the dialog. A centered dialog is preferred over a bottom sheet because it
does not depend on a large entrance or exit movement.

Motion choices use user-facing English copy:

- `Full` — `Play all Mochi animations.`
- `System` — `Follow your device accessibility setting.`
- `Reduced` — `Use fades and simpler transitions.`

The reminder row keeps a trailing switch. When enabled, the Time row appears directly below it and
opens the existing time picker. Existing theme, new-card limit, reminder scheduling, and time
semantics do not change.

### Settings accessibility

- Every interactive row has a touch target of at least 48 dp.
- A row and its trailing control expose one coherent action rather than competing click targets.
- Selected values and radio states are available through semantics, not color alone.
- Decorative icons and the introductory Mochi do not add redundant screen-reader stops.
- Text can wrap without clipping at enlarged system font scales.
- Dark mode uses the existing Mochi dark color scheme and preserves contrast.
- The screen remains vertically scrollable at all supported sizes.

## State and error handling

- `SettingsStore` persists the enum name in the existing key/value table, so no database migration
  is required.
- Missing or unrecognized Motion values resolve to `FULL`.
- A failed native preference read resolves to full motion only when `SYSTEM` is selected.
- Platform observers are released with their owning lifecycle and cannot retain Android or iOS UI
  objects after disposal.
- UI components always render a valid static state even if animation is disabled during an active
  transition.

## Testing and verification

Automated coverage includes:

- Settings persistence defaults, valid values, invalid values, and updates.
- `SettingsViewModel` initial state and immediate Motion changes.
- Every `MotionPreference` and native-signal resolver combination.
- Full and reduced decisions exposed by `MotionPolicy`.
- Gesture threshold and answer callbacks remaining unchanged in reduced mode.
- Static progress and reward state calculations that do not require a running animation clock.

Project verification includes:

- common host tests;
- ktlint and detekt;
- Android main compilation;
- iOS simulator framework compilation;
- manual review of every row in the animation behavior table under `FULL`, `REDUCED`, and
  `SYSTEM`;
- Settings review in light mode, dark mode, enlarged text, and both platform targets.

## Out of scope

- A global switch for haptic feedback.
- Changes to review, SRS, streak, or daily-limit rules.
- New reward events or Mochi moods.
- Daily goal, unit completion, learning path, iOS audio, and iOS bundled fonts.
- A full application-wide screen-reader and switch-control audit.
