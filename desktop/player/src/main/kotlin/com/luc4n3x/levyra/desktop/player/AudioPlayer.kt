package com.luc4n3x.levyra.desktop.player

import kotlinx.coroutines.flow.SharedFlow

interface AudioPlayer : AutoCloseable {
    val events: SharedFlow<PlayerEvent>

    fun play(url: String, startAtMs: Long = 0L)

    fun resume()

    fun pause()

    fun stop()

    fun seekTo(positionMs: Long)

    fun setVolume(volume: Int)

    fun setMuted(muted: Boolean)

    fun applyEqualizer(enabled: Boolean, preamp: Float, amps: List<Float>)

    fun positionMs(): Long

    fun durationMs(): Long

    fun isPlaying(): Boolean
}

class AudioPlayerUnavailableException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
