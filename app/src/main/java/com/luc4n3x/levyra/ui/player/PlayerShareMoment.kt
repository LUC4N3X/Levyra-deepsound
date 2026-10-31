package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.data.youtubeWatchUrl

internal const val SHARE_MOMENT_MIN_POSITION_MS = 5_000L

internal fun shareMomentSeconds(positionMs: Long, durationMs: Long): Long? {
    if (positionMs < SHARE_MOMENT_MIN_POSITION_MS) return null
    if (durationMs > 0L && positionMs >= durationMs) return null
    return positionMs / 1_000L
}

internal fun shareMomentUrl(videoId: String, seconds: Long): String =
    "${youtubeWatchUrl(videoId)}&t=${seconds}s"

internal fun formatShareMomentTimestamp(seconds: Long): String {
    val hours = seconds / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    val secs = seconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}

internal fun shareMomentText(
    title: String,
    artist: String,
    videoId: String,
    seconds: Long,
    fromLabel: String
): String = buildString {
    append(title)
    if (artist.isNotBlank()) append(" — ").append(artist)
    append(" (").append(fromLabel).append(' ').append(formatShareMomentTimestamp(seconds)).append(')')
    append("\n").append(shareMomentUrl(videoId, seconds))
}
