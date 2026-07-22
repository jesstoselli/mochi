# iOS Audio Playback

**Date:** 2026-07-22  
**Status:** Approved design

## Summary

Mochi will replace the iOS `AudioPlayer` no-op with native in-memory playback through
AVFoundation. The shared `AudioPlayer.play(ByteArray)` contract remains unchanged, so review
autoplay, the Listen actions, the Still learning list, and the Mochi click sound gain iOS support
without platform checks or resource-path coupling in shared code.

## Product decisions

- Pronunciation and interaction clips respect the iPhone Ring/Silent switch.
- Playback uses the AVAudioSession `ambient` category and may mix with audio from other apps.
- Starting a clip immediately stops and replaces the previous clip; clips never overlap.
- MP3 pronunciation assets and the WAV click asset continue to load through `Res.readBytes`.
- Empty or invalid audio data fails silently and never interrupts the study flow.
- No third-party audio dependency, temporary file, or shared API change is introduced.

## Architecture

The common `expect class AudioPlayer` remains the public boundary:

```kotlin
expect class AudioPlayer() {
    fun play(bytes: ByteArray)
    fun release()
}
```

The iOS implementation contains two internal layers:

1. `IosAudioController` owns platform-independent playback decisions. It ignores empty data,
   replaces the current request, and forwards release. Its narrow backend contract is exercised
   with a fake in `iosTest`.
2. `AvFoundationAudioBackend` owns Objective-C interop and lifecycle. It converts `ByteArray` to
   `NSData` from pinned memory, configures and activates the shared audio session, creates and
   retains `AVAudioPlayer`, and acts as its completion delegate.

`AudioPlayer` is a thin `actual` wrapper around the controller. AVFoundation types do not escape
`iosMain`, and common ViewModels remain unchanged.

## Playback lifecycle

For each non-empty `play(bytes)` call:

1. Stop the current player, clear its delegate, and release its strong reference.
2. Configure `AVAudioSession.sharedInstance()` with the `ambient` category.
3. Activate the session immediately before playback.
4. Copy the pinned Kotlin bytes into `NSData`; native playback must not retain a pointer to Kotlin
   memory after the pinning scope ends.
5. Initialize `AVAudioPlayer` from the in-memory data.
6. Set the backend as delegate, call `prepareToPlay()`, retain the player, and call `play()`.

When playback finishes, the delegate clears the player if it is still the active instance and
deactivates the session with notification to other audio sessions. `release()` performs the same
cleanup synchronously and is idempotent. A replacement request cleans up the previous player but
keeps the transition internal to one `play()` operation.

## Error handling

- Empty arrays are rejected before touching AVFoundation.
- Session configuration, activation, `NSData` conversion, player initialization, preparation, and
  playback are treated as fallible platform operations.
- Any failure clears partial player state and attempts to deactivate the session.
- Failure is intentionally not surfaced to shared UI in this phase; existing call sites already
  treat audio as optional enrichment and wrap resource loading/playback in `runCatching`.
- `release()` remains safe before playback, after completion, and after a failed start.

## Threading

Existing call sites invoke `AudioPlayer` from ViewModel or Compose main scopes. The iOS backend is
main-thread confined and does not introduce background workers. Delegate callbacks are handled on
the player/session lifecycle used by AVFoundation, and all mutable backend state belongs to one
instance.

## Testing and verification

Automated iOS tests use a fake backend to verify:

- empty bytes do not touch the backend;
- valid bytes are forwarded unchanged;
- a second request replaces the first through the same backend contract;
- release delegates cleanup and is safe repeatedly;
- a backend start failure does not escape the controller.

Platform verification includes:

- `iosSimulatorArm64Test`;
- iOS simulator compilation and framework linking;
- the existing Android host tests and Android compilation, proving the shared contract did not
  regress;
- ktlint and detekt.

Manual validation on an iOS simulator checks autoplay, Listen, Still learning, rapid repeated taps,
and the Mochi click sound. A physical device remains required to confirm Ring/Silent behavior,
speaker/headphone routing, and interaction with another app's audio.

## Documentation references

- [AVAudioPlayer](https://developer.apple.com/documentation/avfaudio/avaudioplayer)
- [AVAudioSession](https://developer.apple.com/documentation/avfaudio/avaudiosession)
- [`ambient` category](https://developer.apple.com/documentation/avfaudio/avaudiosession/category-swift.struct/ambient)

## Out of scope

- Background audio, lock-screen controls, remote commands, playlists, caching, or preloading.
- A user-facing audio or autoplay preference.
- Audio interruption/resumption UI.
- Changes to Android playback behavior.
- Bundled iOS fonts, which remain the following roadmap item after audio.
