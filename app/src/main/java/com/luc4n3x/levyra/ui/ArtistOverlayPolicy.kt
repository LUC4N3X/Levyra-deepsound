package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track

internal const val ARTIST_POPULAR_COLLAPSED_COUNT = 5
internal const val ARTIST_POPULAR_MAX_COUNT = 100

internal fun artistPopularTracksForDisplay(tracks: List<Track>): List<Track> =
    tracks
        .distinctBy { track -> track.id.ifBlank { track.artist + "|" + track.title } }
        .take(ARTIST_POPULAR_MAX_COUNT)

internal fun visibleArtistPopularTracks(tracks: List<Track>, expanded: Boolean): List<Track> {
    val ranked = artistPopularTracksForDisplay(tracks)
    return if (expanded) ranked else ranked.take(ARTIST_POPULAR_COLLAPSED_COUNT)
}

internal fun shouldShowArtistError(hasError: Boolean, hasProfile: Boolean): Boolean {
    return hasError || !hasProfile
}
