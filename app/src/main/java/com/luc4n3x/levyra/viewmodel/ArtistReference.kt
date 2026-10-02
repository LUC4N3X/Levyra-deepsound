package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.primaryArtistSegment
import com.luc4n3x.levyra.feature.radio.isLiveRadio

internal data class ArtistReference(val name: String, val browseId: String)

internal fun isNavigableArtistName(name: String): Boolean {
    val clean = name.trim()
    return clean.length >= 2 &&
        !clean.equals("YouTube Music", ignoreCase = true) &&
        !clean.equals("YouTube", ignoreCase = true)
}

internal fun artistReferenceOf(track: Track): ArtistReference? {
    if (track.isLiveRadio()) return null
    val name = primaryArtistSegment(track.artist).ifBlank { track.artist.trim() }
    if (!isNavigableArtistName(name)) return null
    return ArtistReference(name = name, browseId = track.artistBrowseIds.firstOrNull().orEmpty().trim())
}
