package com.mochi.audio

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.darwin.NSObject

/**
 * AVFoundation-backed playback. Owns the `NSData`/`AVAudioSession`/`AVAudioPlayer` lifecycle and
 * keeps the current player strongly referenced until it is replaced, finishes, fails to decode, or
 * is released. Obj-C delegate callbacks are handled by a separate [PlaybackDelegate] because
 * Kotlin/Native forbids mixing a Kotlin supertype ([IosAudioBackend]) with Obj-C supertypes.
 *
 * Must be driven from the main thread: `player` is mutated without synchronization, and callers
 * (ViewModel/Compose scopes) plus AVFoundation's delegate callbacks are all main-thread confined.
 */
@OptIn(ExperimentalForeignApi::class)
internal class AvFoundationAudioBackend : IosAudioBackend {
    private val session = AVAudioSession.sharedInstance()

    // backend -> delegate -> lambda -> backend is a reference cycle. Kotlin/Native's tracing GC
    // collects the island once it is unreferenced, and this instance is app-lifetime anyway, so the
    // cycle is intentional and does not leak.
    private val delegate = PlaybackDelegate { finished -> onPlaybackEnded(finished) }
    private var player: AVAudioPlayer? = null

    override fun replace(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        stopCurrentPlayer()

        runCatching {
            check(session.setCategory(AVAudioSessionCategoryAmbient, error = null))
            check(session.setActive(true, null))

            val nextPlayer = AVAudioPlayer(bytes.toNSData(), error = null).apply {
                setDelegate(delegate)
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

    private fun onPlaybackEnded(finished: AVAudioPlayer) {
        // Runs from an AVFoundation delegate callback; keep failures from escaping into Obj-C.
        runCatching {
            if (player === finished) clearPlayerAndDeactivate()
        }
    }

    private fun stopCurrentPlayer() {
        player?.apply {
            setDelegate(null)
            stop()
        }
        player = null
    }

    private fun clearPlayerAndDeactivate() {
        stopCurrentPlayer()
        session.setActive(
            false,
            AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
            null,
        )
    }
}

/**
 * Bridges `AVAudioPlayer` completion/decode-error callbacks back to the backend. Kept as a
 * dedicated `NSObject` so the backend itself stays a plain Kotlin class.
 */
@OptIn(ExperimentalForeignApi::class)
private class PlaybackDelegate(
    private val onEnded: (AVAudioPlayer) -> Unit,
) : NSObject(),
    AVAudioPlayerDelegateProtocol {
    override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
        onEnded(player)
    }

    override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) {
        onEnded(player)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = size.toULong(),
        )
    }
