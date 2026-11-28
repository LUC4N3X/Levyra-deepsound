package com.luc4n3x.levyra.viewmodel

import androidx.work.WorkInfo
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.queue.playbackQueueIdentity

internal data class PlaybackResolveRequest(
    val id: Long,
    val startPaused: Boolean = false
)

internal fun shouldStartPlaybackPaused(
    request: PlaybackResolveRequest,
    activeRequestId: Long
): Boolean = request.id == activeRequestId && request.startPaused

internal fun LevyraUiState.withExplicitPlaybackMode(videoMode: Boolean): LevyraUiState =
    if (isVideoMode == videoMode && pendingVideoMode == null) this
    else copy(isVideoMode = videoMode, pendingVideoMode = null)

internal fun shouldDispatchPlaybackStartSideEffects(startPaused: Boolean): Boolean = !startPaused

internal fun resumeStartPositionMs(pendingSeekMs: Long, durationMs: Long): Long =
    pendingSeekMs.takeIf { positionMs ->
        positionMs > 1500L && (durationMs <= 0L || positionMs < durationMs)
    } ?: 0L

internal fun playbackIdentity(track: Track): String = LevyraPersonalOrbit.identityKey(track)

internal fun playbackTrackAfterQueueSync(current: Track?, queued: Track?): Track? {
    if (queued == null) return null
    return current?.takeIf {
        playbackQueueIdentity(it) == playbackQueueIdentity(queued) && it.streamUrl.isNotBlank()
    } ?: queued
}

internal fun queueSyncReplacedTrack(previous: Track?, synced: Track?): Boolean =
    previous != null && synced != null && playbackQueueIdentity(previous) != playbackQueueIdentity(synced)

internal fun playbackPositionForExit(
    playerPositionMs: Long?,
    pendingSeekMs: Long,
    selectedPositionMs: Long = 0L,
    isResolving: Boolean = false
): Long = when {
    isResolving || playerPositionMs == null ->
        pendingSeekMs.takeIf { it > 0L } ?: selectedPositionMs.coerceAtLeast(0L)
    else -> playerPositionMs.takeIf { it > 0L } ?: pendingSeekMs.coerceAtLeast(0L)
}

internal fun canTerminateProcessForExit(downloadStates: List<WorkInfo.State>): Boolean =
    downloadStates.all { it.isFinished }
