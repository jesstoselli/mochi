package com.mochi.audio

/**
 * Plays a short audio clip from its raw bytes. Platform-specific:
 * Android uses MediaPlayer (with an in-memory data source), iOS uses AVAudioPlayer.
 */
expect class AudioPlayer() {
    fun play(bytes: ByteArray)
    fun release()
}
