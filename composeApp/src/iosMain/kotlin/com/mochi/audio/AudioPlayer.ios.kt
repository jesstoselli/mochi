package com.mochi.audio

/**
 * iOS audio playback isn't wired yet (AVFoundation interop is a follow-up). No-op so the
 * shared review flow stays cross-platform; on Android audio plays normally.
 */
actual class AudioPlayer {
    actual fun play(bytes: ByteArray) = Unit
    actual fun release() = Unit
}
