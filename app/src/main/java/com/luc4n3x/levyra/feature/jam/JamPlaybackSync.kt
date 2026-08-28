package com.luc4n3x.levyra.feature.jam

import kotlin.math.abs

object JamPlaybackSync {
    const val BROADCAST_INTERVAL_MS = 1_000L
    const val PLAYING_DRIFT_THRESHOLD_MS = 1_500L
    const val PAUSED_DRIFT_THRESHOLD_MS = 250L
    const val MAX_PREDICTION_MS = 10_000L

    fun isStaleRevision(incomingRevision: Long, lastAppliedRevision: Long): Boolean =
        lastAppliedRevision >= 0L && incomingRevision <= lastAppliedRevision

    fun predictedPositionMs(
        state: JamSessionState,
        receivedAtElapsedMs: Long,
        nowElapsedMs: Long
    ): Long {
        val base = state.positionMs.coerceAtLeast(0L)
        if (!state.playWhenReady) return base
        val elapsed = (nowElapsedMs - receivedAtElapsedMs).coerceIn(0L, MAX_PREDICTION_MS)
        return base + elapsed
    }

    fun shouldSeek(
        localPositionMs: Long,
        predictedPositionMs: Long,
        playWhenReady: Boolean
    ): Boolean {
        val threshold = if (playWhenReady) PLAYING_DRIFT_THRESHOLD_MS else PAUSED_DRIFT_THRESHOLD_MS
        return abs(localPositionMs - predictedPositionMs) > threshold
    }
}
