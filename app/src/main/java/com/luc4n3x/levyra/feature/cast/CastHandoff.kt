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

data class CastHandoff(
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
    ): CastHandoff {
        require(windowRadius >= 0) { "windowRadius must be >= 0" }
        val queue = snapshot.queueIds
        val safePosition = snapshot.positionMs.coerceAtLeast(0L)
        if (queue.isEmpty()) {
            return CastHandoff(
                queueWindowIds = emptyList(),
                windowStartIndex = 0,
                currentIndex = -1,
                positionMs = safePosition,
                playing = snapshot.playing,
                shuffle = snapshot.shuffle,
                repeatMode = snapshot.repeatMode
            )
        }
        val safeCurrentIndex = snapshot.currentIndex.coerceIn(0, queue.lastIndex)
        val windowStart = (safeCurrentIndex - windowRadius).coerceAtLeast(0)
        val windowEnd = (safeCurrentIndex + windowRadius).coerceAtMost(queue.lastIndex)
        val windowIds = queue.subList(windowStart, windowEnd + 1).toList()
        return CastHandoff(
            queueWindowIds = windowIds,
            windowStartIndex = windowStart,
            currentIndex = safeCurrentIndex - windowStart,
            positionMs = safePosition,
            playing = snapshot.playing,
            shuffle = snapshot.shuffle,
            repeatMode = snapshot.repeatMode
        )
    }

    fun toLocalResumeState(
        handoff: CastHandoff,
        localQueueIds: List<String> = handoff.queueWindowIds
    ): LocalResumeState {
        val queue = localQueueIds.ifEmpty { handoff.queueWindowIds }
        if (queue.isEmpty()) {
            return LocalResumeState(
                queueIds = emptyList(),
                currentIndex = -1,
                positionMs = handoff.positionMs.coerceAtLeast(0L),
                playing = handoff.playing,
                shuffle = handoff.shuffle,
                repeatMode = handoff.repeatMode
            )
        }
        val windowIndex = handoff.currentIndex.coerceAtLeast(0)
        val currentId = handoff.queueWindowIds.getOrNull(windowIndex)
        val absoluteIndex = when {
            currentId == null -> (handoff.windowStartIndex + windowIndex).coerceIn(0, queue.lastIndex)
            queue.getOrNull(handoff.windowStartIndex + windowIndex) == currentId ->
                handoff.windowStartIndex + windowIndex
            else -> queue.indexOf(currentId).takeIf { it >= 0 }
                ?: (handoff.windowStartIndex + windowIndex).coerceIn(0, queue.lastIndex)
        }
        return LocalResumeState(
            queueIds = queue,
            currentIndex = absoluteIndex.coerceIn(0, queue.lastIndex),
            positionMs = handoff.positionMs.coerceAtLeast(0L),
            playing = handoff.playing,
            shuffle = handoff.shuffle,
            repeatMode = handoff.repeatMode
        )
    }
}
