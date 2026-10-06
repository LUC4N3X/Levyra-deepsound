package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistCredits
import com.luc4n3x.levyra.feature.radio.isLiveRadio

internal data class ArtistReference(val name: String, val browseId: String)

internal fun isNavigableArtistName(name: String): Boolean {
    val clean = name.trim()
    return clean.length >= 2 &&
        !clean.equals("YouTube Music", ignoreCase = true) &&
        !clean.equals("YouTube", ignoreCase = true)
}

private fun artistReferenceOfCredit(track: Track, artistIndex: Int): ArtistReference? {
    if (track.isLiveRadio()) return null
    val credit = artistCredits(track.artist, track.artistBrowseIds).getOrNull(artistIndex) ?: return null
    val name = credit.name.trim()
    if (!isNavigableArtistName(name)) return null
    return ArtistReference(name = name, browseId = credit.browseId.trim())
}

internal fun artistReferenceOf(track: Track): ArtistReference? =
    artistReferenceOfCredit(track, 0)

internal fun artistReferenceOf(track: Track, artistIndex: Int): ArtistReference? =
    artistReferenceOfCredit(track, artistIndex)
