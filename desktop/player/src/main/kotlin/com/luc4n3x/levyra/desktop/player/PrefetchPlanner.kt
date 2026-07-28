package com.luc4n3x.levyra.desktop.player

import com.luc4n3x.levyra.desktop.core.model.Track

object PrefetchPlanner {
    const val LEAD_MS = 45_000L

    fun nextTrack(queue: PlayerQueue): Track? {
        if (queue.items.isEmpty() || queue.index < 0) return null
        if (queue.repeat == RepeatMode.ONE) return null
        val nextIndex = queue.index + 1
        val candidate = when {
            nextIndex < queue.items.size -> queue.items[nextIndex]
            queue.repeat == RepeatMode.ALL -> queue.items.firstOrNull()
            else -> null
        } ?: return null
        if (candidate.id == queue.current?.id) return null
        if (candidate.offlinePath.isNotBlank()) return null
        return candidate
    }

    fun withinLeadWindow(positionMs: Long, durationMs: Long): Boolean {
        if (durationMs <= 0L) return false
        if (durationMs <= LEAD_MS) return positionMs > 0L
        return positionMs >= durationMs - LEAD_MS
    }
}
