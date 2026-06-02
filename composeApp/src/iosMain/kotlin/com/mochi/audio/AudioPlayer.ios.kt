package com.mochi.audio

/**
 * iOS audio playback is not wired yet. Building NSData from a ByteArray via the
 * Kotlin/Native Foundation interop (NSData.create) didn't resolve in this toolchain, so
 * this is a no-op for now — the shared review flow stays cross-platform and Android plays
 * pronunciation normally. Revisit on-device (e.g. load the bundled file via Res.getUri +
 * AVAudioPlayer(contentsOf:), or a small KMP audio library).
 */
actual class AudioPlayer {
    actual fun play(bytes: ByteArray) = Unit
    actual fun release() = Unit
}
