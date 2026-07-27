package com.mochi.audio

/**
 * iOS playback delegates to a single [IosAudioController] backed by AVFoundation. The instance is
 * retained for the player's lifetime so rapid autoplay or Listen taps replace the current clip
 * rather than overlapping it.
 */
actual class AudioPlayer {
    private val controller = IosAudioController(AvFoundationAudioBackend())

    actual fun play(bytes: ByteArray) {
        controller.play(bytes)
    }

    actual fun release() {
        controller.release()
    }
}
