package com.mochi.audio

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class AudioPlayer {
    private var player: AVAudioPlayer? = null

    actual fun play(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val data = memScoped {
            NSData.create(bytes = allocArrayOf(bytes), length = bytes.size.toULong())
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
