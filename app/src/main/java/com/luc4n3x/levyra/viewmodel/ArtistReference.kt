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

internal fun artistReferencesOf(track: Track): List<ArtistReference> {
    if (track.isLiveRadio()) return emptyList()
    return artistCredits(track.artist, track.artistBrowseIds)
        .mapNotNull { credit ->
            val name = credit.name.trim()
            if (!isNavigableArtistName(name)) null
            else ArtistReference(name = name, browseId = credit.browseId.trim())
        }
}

internal fun artistReferenceOf(track: Track): ArtistReference? =
    artistReferencesOf(track).firstOrNull()

internal fun artistReferenceOf(track: Track, artistIndex: Int): ArtistReference? =
    artistReferencesOf(track).getOrNull(artistIndex)
