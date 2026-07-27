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
        val backend = RecordingAudioBackend(failOnReplace = true)
        val controller = IosAudioController(backend)

        // A throwing backend must be swallowed: no exception, and nothing recorded.
        controller.play(byteArrayOf(1))

        assertEquals(0, backend.replacements.size)
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
