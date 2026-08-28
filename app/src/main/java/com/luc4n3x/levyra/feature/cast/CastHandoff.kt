package com.luc4n3x.levyra.feature.cast

import com.luc4n3x.levyra.domain.RepeatMode

data class LocalPlaybackSnapshot(
    val queueIds: List<String>,
    val currentIndex: Int,
    val positionMs: Long,
    val playing: Boolean,
    val shuffle: Boolean,
    val repeatMode: RepeatMode
)

data class CastHandoffState(
    val queueWindowIds: List<String>,
    val windowStartIndex: Int,
    val currentIndex: Int,
    val positionMs: Long,
    val playing: Boolean,
    val shuffle: Boolean,
    val repeatMode: RepeatMode
)

data class LocalResumeState(
    val queueIds: List<String>,
    val currentIndex: Int,
    val positionMs: Long,
    val playing: Boolean,
    val shuffle: Boolean,
    val repeatMode: RepeatMode
)

internal fun castHandoffStartPositionMs(
    sameMediaItem: Boolean,
    requestedPositionMs: Long,
    livePositionMs: Long,
    remotePlayback: Boolean
): Long {
    val requested = requestedPositionMs.coerceAtLeast(0L)
    if (!remotePlayback || !sameMediaItem) return requested
    return maxOf(requested, livePositionMs.coerceAtLeast(0L))
}

internal fun castWindowNeedsRefresh(
    expectedUpcomingIds: List<String>,
    remoteForwardIds: List<String>,
    minimumBufferedItems: Int = 3
): Boolean {
    if (expectedUpcomingIds.isEmpty()) return false
    val comparable = minOf(expectedUpcomingIds.size, remoteForwardIds.size)
    if (remoteForwardIds.take(comparable) != expectedUpcomingIds.take(comparable)) return true
    return remoteForwardIds.size < minOf(expectedUpcomingIds.size, minimumBufferedItems.coerceAtLeast(1))
}

object CastHandoffConverter {
    const val DEFAULT_QUEUE_WINDOW_RADIUS = 2

    fun toHandoff(
        snapshot: LocalPlaybackSnapshot,
        windowRadius: Int = DEFAULT_QUEUE_WINDOW_RADIUS
    ): CastHandoffState {
        if (snapshot.queueIds.isEmpty()) {
            return CastHandoffState(
                queueWindowIds = emptyList(),
                windowStartIndex = 0,
                currentIndex = -1,
                positionMs = snapshot.positionMs.coerceAtLeast(0L),
                playing = snapshot.playing,
                shuffle = snapshot.shuffle,
                repeatMode = snapshot.repeatMode
            )
        }
        val safeCurrent = snapshot.currentIndex.coerceIn(0, snapshot.queueIds.lastIndex)
        val radius = windowRadius.coerceAtLeast(0)
        val start = (safeCurrent - radius).coerceAtLeast(0)
        val endExclusive = (safeCurrent + radius + 1).coerceAtMost(snapshot.queueIds.size)
        return CastHandoffState(
            queueWindowIds = snapshot.queueIds.subList(start, endExclusive),
            windowStartIndex = start,
            currentIndex = safeCurrent - start,
            positionMs = snapshot.positionMs.coerceAtLeast(0L),
            playing = snapshot.playing,
            shuffle = snapshot.shuffle,
            repeatMode = snapshot.repeatMode
        )
    }

    fun toLocalResumeState(
        handoff: CastHandoffState,
        originalQueueIds: List<String>
    ): LocalResumeState {
        val selectedId = handoff.queueWindowIds.getOrNull(handoff.currentIndex)
        val restoredIndex = selectedId?.let(originalQueueIds::indexOf)?.takeIf { it >= 0 }
            ?: handoff.windowStartIndex.plus(handoff.currentIndex.coerceAtLeast(0))
                .coerceIn(0, originalQueueIds.lastIndex.coerceAtLeast(0))
        return LocalResumeState(
            queueIds = originalQueueIds,
            currentIndex = if (originalQueueIds.isEmpty()) -1 else restoredIndex,
            positionMs = handoff.positionMs.coerceAtLeast(0L),
            playing = handoff.playing,
            shuffle = handoff.shuffle,
            repeatMode = handoff.repeatMode
        )
    }
}
