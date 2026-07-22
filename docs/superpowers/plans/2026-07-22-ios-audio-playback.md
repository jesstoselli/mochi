# iOS Audio Playback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Mochi's iOS audio no-op with reliable in-memory MP3/WAV playback that respects the Ring/Silent switch and coexists with audio from other apps.

**Architecture:** Keep the shared `AudioPlayer.play(ByteArray)` API unchanged. The iOS `actual` delegates policy to a small `IosAudioController`, tested with a fake backend, while `AvFoundationAudioBackend` exclusively owns `NSData`, `AVAudioSession`, `AVAudioPlayer`, replacement, completion, and release lifecycles.

**Tech Stack:** Kotlin 2.2.20, Compose Multiplatform 1.10.3 resources, Kotlin/Native Objective-C interop, Apple AVFAudio/AVFoundation, kotlin.test.

## Global Constraints

- Keep Android and all shared audio call sites unchanged.
- Read MP3 and WAV assets through the existing `Res.readBytes` flow; do not create temporary files.
- Configure `AVAudioSessionCategoryAmbient`, so clips respect Ring/Silent and mix with other audio.
- A new play request stops and replaces the current clip; clips never overlap.
- Empty or invalid bytes are a safe no-op and never escape into the study UI.
- Retain the active `AVAudioPlayer` strongly until replacement, completion, decode failure, or release.
- Copy bytes into `NSData` before leaving the Kotlin pinning scope.
- Commit each coherent task separately and never push.

---

### Task 1: Add the testable iOS playback boundary

**Files:**
- Create: `composeApp/src/iosMain/kotlin/com/mochi/audio/IosAudioController.kt`
- Create: `composeApp/src/iosTest/kotlin/com/mochi/audio/IosAudioControllerTest.kt`

**Interfaces:**
- Produces: `internal interface IosAudioBackend`
- Produces: `internal class IosAudioController`
- `IosAudioBackend.replace(bytes)` owns replacement semantics.
- `IosAudioController` filters empty data and contains platform failures.

- [ ] **Step 1: Write failing controller tests**

```kotlin
package com.mochi.audio

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class IosAudioControllerTest {
    @Test
    fun emptyBytesDoNotTouchBackend() {
        val backend = RecordingAudioBackend()
        val controller = IosAudioController(backend)

        controller.play(byteArrayOf())

        assertEquals(0, backend.replacements.size)
    }

    @Test
    fun validBytesAreForwardedUnchangedInOrder() {
        val backend = RecordingAudioBackend()
        val controller = IosAudioController(backend)
        val first = byteArrayOf(1, 2, 3)
        val second = byteArrayOf(4, 5)

        controller.play(first)
        controller.play(second)

        assertEquals(2, backend.replacements.size)
        assertContentEquals(first, backend.replacements[0])
        assertContentEquals(second, backend.replacements[1])
    }

    @Test
    fun backendFailureDoesNotEscape() {
        val controller = IosAudioController(RecordingAudioBackend(failOnReplace = true))

        controller.play(byteArrayOf(1))
    }

    @Test
    fun releaseIsSafeToRepeat() {
        val backend = RecordingAudioBackend()
        val controller = IosAudioController(backend)

        controller.release()
        controller.release()

        assertEquals(2, backend.releaseCount)
    }
}

private class RecordingAudioBackend(
    private val failOnReplace: Boolean = false,
) : IosAudioBackend {
    val replacements = mutableListOf<ByteArray>()
    var releaseCount = 0

    override fun replace(bytes: ByteArray) {
        if (failOnReplace) error("playback failed")
        replacements += bytes
    }

    override fun release() {
        releaseCount++
    }
}
```

- [ ] **Step 2: Verify RED**

Run:

```bash
./gradlew :composeApp:iosSimulatorArm64Test
```

Expected: compilation failure because `IosAudioBackend` and `IosAudioController` do not exist.

- [ ] **Step 3: Implement the narrow controller**

```kotlin
package com.mochi.audio

internal interface IosAudioBackend {
    fun replace(bytes: ByteArray)
    fun release()
}

internal class IosAudioController(
    private val backend: IosAudioBackend,
) {
    fun play(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        runCatching { backend.replace(bytes) }
    }

    fun release() {
        runCatching { backend.release() }
    }
}
```

- [ ] **Step 4: Verify GREEN**

Run:

```bash
./gradlew :composeApp:iosSimulatorArm64Test
```

Expected: 4 tests pass, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/mochi/audio/IosAudioController.kt \
  composeApp/src/iosTest/kotlin/com/mochi/audio/IosAudioControllerTest.kt
git commit -m "feat: add iOS audio playback controller"
```

### Task 2: Implement the AVFoundation backend

**Files:**
- Create: `composeApp/src/iosMain/kotlin/com/mochi/audio/AvFoundationAudioBackend.kt`
- Modify: `composeApp/build.gradle.kts` only if framework linking proves AVFAudio is not propagated automatically.

**Interfaces:**
- Implements: `IosAudioBackend`
- Consumes: `AVAudioSession.sharedInstance`, `AVAudioSessionCategoryAmbient`, `AVAudioPlayer`
- Produces: `ByteArray.toNSData()` as a private copied conversion
- Implements: `AVAudioPlayerDelegateProtocol` completion and decode-error callbacks

- [ ] **Step 1: Add the AVFAudio implementation behind the tested backend contract**

Use the Kotlin/Native 2.2.21 platform signatures resolved from the local SDK metadata:

```kotlin
package com.mochi.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal class AvFoundationAudioBackend :
    NSObject(),
    IosAudioBackend,
    AVAudioPlayerDelegateProtocol {
    private val session = AVAudioSession.sharedInstance()
    private var player: AVAudioPlayer? = null

    override fun replace(bytes: ByteArray) {
        stopCurrentPlayer()

        runCatching {
            check(session.setCategory(AVAudioSessionCategoryAmbient, error = null))
            check(session.setActive(true, error = null))

            val nextPlayer = AVAudioPlayer(bytes.toNSData(), error = null).apply {
                delegate = this@AvFoundationAudioBackend
            }
            check(nextPlayer.prepareToPlay())
            player = nextPlayer
            check(nextPlayer.play())
        }.onFailure {
            clearPlayerAndDeactivate()
        }
    }

    override fun release() {
        clearPlayerAndDeactivate()
    }

    override fun audioPlayerDidFinishPlaying(
        player: AVAudioPlayer,
        successfully: Boolean,
    ) {
        if (this.player === player) clearPlayerAndDeactivate()
    }

    override fun audioPlayerDecodeErrorDidOccur(
        player: AVAudioPlayer,
        error: NSError?,
    ) {
        if (this.player === player) clearPlayerAndDeactivate()
    }

    private fun stopCurrentPlayer() {
        player?.apply {
            delegate = null
            stop()
        }
        player = null
    }

    private fun clearPlayerAndDeactivate() {
        stopCurrentPlayer()
        session.setActive(
            active = false,
            withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
            error = null,
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.dataWithBytes(
            bytes = pinned.addressOf(0),
            length = size.toULong(),
        )
    }
```

`NSData.dataWithBytes` copies the payload synchronously, so `AVAudioPlayer` never retains the
address of movable Kotlin memory. Do not replace it with a no-copy initializer.

- [ ] **Step 2: Compile and link the backend**

Run:

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64 \
  :composeApp:linkDebugFrameworkIosSimulatorArm64
```

Expected: `BUILD SUCCESSFUL` with AVFAudio symbols resolved.

If and only if the link step reports unresolved AVFAudio symbols, append
`linkerOpts("-framework", "AVFAudio")` beside the existing SQLite linker option in
`composeApp/build.gradle.kts`, rerun the same command, and include that file in the commit. The
Xcode application target already links AVFoundation; no `.pbxproj` change is planned.

- [ ] **Step 3: Verify controller tests still pass**

Run:

```bash
./gradlew :composeApp:iosSimulatorArm64Test
```

Expected: 4 tests pass, 0 failures.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/mochi/audio/AvFoundationAudioBackend.kt
git add composeApp/build.gradle.kts # only when the conditional linker change was required
git commit -m "feat: implement AVFoundation audio backend"
```

### Task 3: Wire the iOS actual AudioPlayer

**Files:**
- Modify: `composeApp/src/iosMain/kotlin/com/mochi/audio/AudioPlayer.ios.kt`

**Interfaces:**
- Preserves: `actual class AudioPlayer`
- Delegates: `play` and `release` to one retained `IosAudioController`

- [ ] **Step 1: Replace the no-op implementation**

```kotlin
package com.mochi.audio

actual class AudioPlayer {
    private val controller = IosAudioController(AvFoundationAudioBackend())

    actual fun play(bytes: ByteArray) {
        controller.play(bytes)
    }

    actual fun release() {
        controller.release()
    }
}
```

The single controller/backend instance is intentional: it retains the active native player and
ensures rapid autoplay or Listen taps replace rather than overlap the current clip.

- [ ] **Step 2: Run cross-platform regression verification**

```bash
./gradlew :composeApp:iosSimulatorArm64Test \
  :composeApp:testAndroidHostTest \
  :composeApp:compileAndroidMain \
  :composeApp:compileKotlinIosSimulatorArm64
```

Expected: `BUILD SUCCESSFUL`; iOS tests pass and Android remains unchanged.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/mochi/audio/AudioPlayer.ios.kt
git commit -m "feat: enable audio playback on iOS"
```

### Task 4: Update parity documentation and complete validation

**Files:**
- Modify: `README.md`
- Modify: `docs/CONTEXT.md`
- Modify: `docs/ROADMAP.md`

- [ ] **Step 1: Update project documentation**

- In `README.md`, describe pronunciation playback as available on Android and iOS; retain the
  real-device caveat only for iOS reminder/audio route validation where accurate.
- In `docs/CONTEXT.md`, replace both iOS audio no-op/deferred statements with the
  `NSData` + `AVAudioPlayer` implementation, `.ambient` behavior, replacement semantics, and
  simulator verification status.
- In `docs/ROADMAP.md`, mark iOS pronunciation playback complete while leaving bundled iOS fonts
  and physical-device validation pending.

- [ ] **Step 2: Run the complete quality gate**

```bash
./gradlew :composeApp:iosSimulatorArm64Test \
  :composeApp:testAndroidHostTest \
  :composeApp:ktlintCheck \
  :composeApp:detekt \
  :composeApp:compileAndroidMain \
  :composeApp:compileKotlinIosSimulatorArm64 \
  :composeApp:linkDebugFrameworkIosSimulatorArm64
```

Expected: `BUILD SUCCESSFUL`, all tests and static analysis pass, and both platform targets compile.

- [ ] **Step 3: Perform simulator smoke validation**

Launch the iOS app and verify:

- review autoplay speaks the presented card;
- Listen works in review and Still learning;
- rapid repeated Listen taps replace the current clip without overlap;
- the Mochi click sound plays;
- leaving the relevant screen releases playback without a crash.

Record physical-device checks as pending rather than claiming them from the simulator: Ring/Silent,
speaker/headphone routing, and mixing with another app's audio.

- [ ] **Step 4: Commit documentation**

```bash
git add README.md docs/CONTEXT.md docs/ROADMAP.md
git commit -m "docs: document iOS audio parity"
```

- [ ] **Step 5: Review before integration**

Invoke `toutbox-claude-skills:architecture-review`, then
`superpowers:requesting-code-review`, address only verified findings, and rerun the complete quality
gate. Use `superpowers:verification-before-completion` before claiming success and
`superpowers:finishing-a-development-branch` for the no-push integration handoff.
