package com.mochi.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.Foundation.NSData

@OptIn(ExperimentalForeignApi::class)
actual class AudioPlayer {
    private var player: AVAudioPlayer? = null

    actual fun play(bytes: ByteArray) {
        if (bytes.isEmpty()) return

        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }

        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        }

        player?.stop()
        player = AVAudioPlayer(data = data, error = null)
        player?.prepareToPlay()
        player?.play()
    }

    actual fun release() {
        player?.stop()
        player = null
    }
}
