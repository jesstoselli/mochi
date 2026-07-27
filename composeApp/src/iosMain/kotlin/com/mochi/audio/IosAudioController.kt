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
