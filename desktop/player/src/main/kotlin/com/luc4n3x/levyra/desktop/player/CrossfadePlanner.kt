package com.luc4n3x.levyra.desktop.player

import com.luc4n3x.levyra.desktop.core.model.Track
import kotlin.math.cos
import kotlin.math.sin

object CrossfadePlanner {

    const val PREPARE_LEAD_MS = 2_500L
    const val STEP_MS = 60L
    private const val MIN_TRANSITION_MS = 500L
    private const val HALF_PI = (Math.PI / 2.0).toFloat()

    fun effectiveDurationMs(requestedMs: Int, currentDurationMs: Long, nextDurationMs: Long): Long {
        if (requestedMs <= 0 || currentDurationMs <= 0L) return 0L
        var duration = requestedMs.toLong()
        duration = minOf(duration, currentDurationMs / 2L)
        if (nextDurationMs > 0L) {
            duration = minOf(duration, nextDurationMs / 2L)
        }
        return if (duration < MIN_TRANSITION_MS) 0L else duration
    }

    fun sharesAlbum(current: Track, next: Track): Boolean {
        val currentAlbum = current.album.trim()
        val nextAlbum = next.album.trim()
        if (currentAlbum.isEmpty() || nextAlbum.isEmpty()) return false
        return currentAlbum.equals(nextAlbum, ignoreCase = true)
    }

    fun transitionDurationMs(
        requestedMs: Int,
        smartCrossfade: Boolean,
        current: Track,
        next: Track,
        currentDurationMs: Long
    ): Long {
        if (smartCrossfade && sharesAlbum(current, next)) return 0L
        return effectiveDurationMs(requestedMs, currentDurationMs, next.durationMs)
    }

    fun prepareThresholdMs(durationMs: Long, transitionMs: Long): Long {
        if (durationMs <= 0L) return Long.MAX_VALUE
        val lead = transitionMs + PREPARE_LEAD_MS
        return (durationMs - lead).coerceAtLeast(0L)
    }

    fun outgoingGain(fraction: Float): Float = cos(fraction.coerceIn(0f, 1f) * HALF_PI)

    fun incomingGain(fraction: Float): Float = sin(fraction.coerceIn(0f, 1f) * HALF_PI)

    fun volumeFor(baseVolume: Int, gain: Float): Int =
        (baseVolume * gain).toInt().coerceIn(0, 100)
}
