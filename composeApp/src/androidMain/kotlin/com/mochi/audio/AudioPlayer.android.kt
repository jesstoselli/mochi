package com.mochi.audio

import android.media.MediaDataSource
import android.media.MediaPlayer

actual class AudioPlayer {
    private var player: MediaPlayer? = null

    actual fun play(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        player?.release()
        val mp = MediaPlayer()
        mp.setDataSource(ByteArrayMediaDataSource(bytes))
        mp.setOnPreparedListener { it.start() }
        mp.setOnCompletionListener {
            it.release()
            if (player === it) player = null
        }
        mp.setOnErrorListener { p, _, _ ->
            p.release()
            true
        }
        mp.prepareAsync()
        player = mp
    }

    actual fun release() {
        player?.release()
        player = null
    }
}

/** Feeds an in-memory byte array to MediaPlayer without writing a temp file. */
private class ByteArrayMediaDataSource(private val data: ByteArray) : MediaDataSource() {
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= data.size) return -1
        val end = minOf(data.size.toLong(), position + size)
        val length = (end - position).toInt()
        System.arraycopy(data, position.toInt(), buffer, offset, length)
        return length
    }

    override fun getSize(): Long = data.size.toLong()

    override fun close() = Unit
}
