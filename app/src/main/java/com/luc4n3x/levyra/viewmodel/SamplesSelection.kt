package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.PlaybackSourceIdentity
import com.luc4n3x.levyra.data.SPOTIFY_CLIP_ID_PREFIX
import com.luc4n3x.levyra.data.isYoutubeShortTrack
import com.luc4n3x.levyra.domain.Track

internal fun LevyraUiState.withPublishedSamples(
    tracks: List<Track>,
    loading: Boolean = false,
    failed: Boolean = false
): LevyraUiState = copy(
    exploreSamples = tracks,
    isSamplesLoading = loading,
    samplesLoadFailed = failed
)

internal fun selectYoutubeShortSample(list: List<Track>, requested: Track): Track? {
    if (list.isEmpty()) return null
    val selected = list.firstOrNull { candidate -> sameYoutubeShortSample(candidate, requested) }
        ?: requested
    return selected.takeIf(::isYoutubeShortTrack)
}

internal fun sameYoutubeShortSample(left: Track, right: Track): Boolean {
    if (left.id.startsWith(SPOTIFY_CLIP_ID_PREFIX) || right.id.startsWith(SPOTIFY_CLIP_ID_PREFIX)) {
        return left.id == right.id
    }
    val videoId = PlaybackSourceIdentity.sourceVideoId(left)
    return videoId.isNotBlank() && videoId == PlaybackSourceIdentity.sourceVideoId(right)
}

internal fun samplePlaybackVideoMode(track: Track): Boolean = !track.id.startsWith(SPOTIFY_CLIP_ID_PREFIX)
