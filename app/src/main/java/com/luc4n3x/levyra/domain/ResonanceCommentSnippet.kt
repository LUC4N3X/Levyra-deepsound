package com.luc4n3x.levyra.domain

import androidx.compose.runtime.Immutable

/**
 * Real YouTube comment snippet displayed in the "Voci che risuonano" (most discussed tracks)
 * home shelf. Holds authentic comment text, count, and author metadata without invented content.
 */
@Immutable
data class ResonanceCommentSnippet(
    val videoId: String,
    val countText: String = "",
    val author: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val likeCountText: String = "",
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val disabled: Boolean = false,
    val updatedAtMs: Long = 0L
) {
    val hasComment: Boolean
        get() = text.isNotBlank()
}

internal fun resonanceCommentsForTracks(
    tracks: List<Track>,
    comments: Map<String, ResonanceCommentSnippet>
): Map<String, ResonanceCommentSnippet> = buildMap {
    tracks.asSequence()
        .map(Track::id)
        .filter(String::isNotBlank)
        .distinct()
        .forEach { videoId ->
            comments[videoId]
                ?.takeIf { it.videoId == videoId }
                ?.let { put(videoId, it) }
        }
}
