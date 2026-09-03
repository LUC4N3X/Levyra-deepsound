package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.data.fullResolutionArtworkUrl
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track

internal fun preferredPlayerArtworkUrl(track: Track): String {
    val candidates = sequenceOf(track.largeThumbnailUrl, track.thumbnailUrl)
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()
    val albumArtwork = candidates.firstOrNull { !LevyraPersonalOrbit.isVideoFrameArtworkUrl(it) }
    return highResolutionPlayerArtworkUrl(albumArtwork ?: candidates.firstOrNull().orEmpty())
}

internal fun highResolutionPlayerArtworkUrl(url: String): String = fullResolutionArtworkUrl(url)
