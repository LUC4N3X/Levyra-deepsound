package com.luc4n3x.levyra.player

object PlaybackAutomationPolicy {
    const val MAX_CONSECUTIVE_ERROR_SKIPS = 3
    const val ROUTE_RESUME_WINDOW_MS = 5L * 60L * 1_000L

    fun shouldResumeOnRouteReconnect(
        enabled: Boolean,
        pausedByRouteLossAtMs: Long?,
        nowMs: Long,
        playerReady: Boolean,
        hasQueueItem: Boolean,
        alreadyPlaying: Boolean
    ): Boolean {
        if (!enabled || alreadyPlaying || !playerReady || !hasQueueItem) return false
        val pausedAt = pausedByRouteLossAtMs ?: return false
        val elapsed = nowMs - pausedAt
        return elapsed in 0L..ROUTE_RESUME_WINDOW_MS
    }

    fun shouldPauseForMute(enabled: Boolean, streamVolume: Int, isPlaying: Boolean): Boolean =
        enabled && isPlaying && streamVolume <= 0

    fun shouldAutoDownloadFavorite(
        enabled: Boolean,
        becameFavorite: Boolean,
        trackId: String,
        downloadedTrackIds: Set<String>,
        downloadingKeys: Set<String>,
        downloadKey: String
    ): Boolean {
        if (!enabled || !becameFavorite) return false
        if (trackId.isBlank()) return false
        if (trackId in downloadedTrackIds) return false
        return downloadKey !in downloadingKeys
    }
}

class ConsecutivePlaybackFailureGuard(
    private val maxSkips: Int = PlaybackAutomationPolicy.MAX_CONSECUTIVE_ERROR_SKIPS
) {
    private var failures = 0

    val consecutiveFailures: Int
        get() = failures

    fun onHealthyPlayback() {
        failures = 0
    }

    fun shouldSkipAfterUnrecoverableError(enabled: Boolean, hasQueueItem: Boolean): Boolean {
        if (!enabled || !hasQueueItem) return false
        if (failures >= maxSkips) return false
        failures += 1
        return true
    }
}
