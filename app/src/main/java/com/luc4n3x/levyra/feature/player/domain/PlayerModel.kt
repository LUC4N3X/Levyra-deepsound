package com.luc4n3x.levyra.feature.player.domain

import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track

data class PlayerModel(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val isPlaying: Boolean = false,
    val isResolving: Boolean = false,
    val isVideoMode: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val shuffleEnabled: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
    val pendingSeekMs: Long? = null
) {
    val hasTrack: Boolean
        get() = currentTrack != null

    val hasQueue: Boolean
        get() = queue.isNotEmpty()

    val safePositionMs: Long
        get() = positionMs.coerceAtLeast(0L)

    val safeDurationMs: Long
        get() = durationMs.coerceAtLeast(0L)

    val progress: Float
        get() = if (safeDurationMs <= 0L) 0f else (safePositionMs.toFloat() / safeDurationMs.toFloat()).coerceIn(0f, 1f)

    fun normalized(): PlayerModel = copy(
        queue = queue.distinctBy { it.id.ifBlank { it.videoUrl.ifBlank { it.title + it.artist } } },
        positionMs = positionMs.coerceAtLeast(0L),
        durationMs = durationMs.coerceAtLeast(0L),
        pendingSeekMs = pendingSeekMs?.coerceAtLeast(0L),
        errorMessage = errorMessage?.trim()?.takeIf { it.isNotBlank() }
    )
}
