package com.luc4n3x.levyra.player.queue

import com.luc4n3x.levyra.domain.Track

internal data class QueuePlaylistExport(
    val tracks: List<Track>,
    val skippedCount: Int
)

internal fun prepareQueuePlaylistExport(tracks: List<Track>): QueuePlaylistExport {
    val eligible = tracks.filter { track ->
        val localUri = track.streamUrl.startsWith("content://", ignoreCase = true) ||
            track.streamUrl.startsWith("file://", ignoreCase = true)
        track.id.isNotBlank() &&
            track.title.isNotBlank() &&
            !localUri
    }
    return QueuePlaylistExport(
        tracks = eligible,
        skippedCount = tracks.size - eligible.size
    )
}
