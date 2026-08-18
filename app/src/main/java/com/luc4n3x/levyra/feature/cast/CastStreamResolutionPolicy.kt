package com.luc4n3x.levyra.feature.cast

object CastStreamResolutionPolicy {
    const val DEFAULT_RESOLUTION_WINDOW_RADIUS = 2
    const val DEFAULT_STREAM_URL_TTL_MS = 5 * 60 * 1000L

    const val PERSISTENCE_POLICY_NOTE =
        "Resolved remote-playback stream URLs (e.g. googlevideo) are short-lived and signed. " +
            "They must never be written to Room, DataStore, files, backups, or logs. " +
            "Keep them in memory only, scoped to the active cast session."

    fun itemsNeedingResolution(
        queueIds: List<String>,
        currentIndex: Int,
        alreadyResolvedIds: Set<String>,
        windowRadius: Int = DEFAULT_RESOLUTION_WINDOW_RADIUS
    ): List<String> {
        require(windowRadius >= 0) { "windowRadius must be >= 0" }
        if (queueIds.isEmpty()) return emptyList()
        val safeIndex = currentIndex.coerceIn(0, queueIds.lastIndex)
        val start = (safeIndex - windowRadius).coerceAtLeast(0)
        val end = (safeIndex + windowRadius).coerceAtMost(queueIds.lastIndex)
        return queueIds.subList(start, end + 1).filterNot { it in alreadyResolvedIds }
    }

    fun isStale(resolvedAtMs: Long, ttlMs: Long, nowMs: Long): Boolean {
        if (ttlMs <= 0L) return true
        val ageMs = nowMs - resolvedAtMs
        return ageMs < 0L || ageMs >= ttlMs
    }
}
