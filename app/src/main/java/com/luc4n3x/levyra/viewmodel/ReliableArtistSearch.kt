package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistIdentityKey

/**
 * Merges the exact, independently verified artist lookup with the artists returned by the
 * general search endpoint. The general endpoint is useful for discovery, but it may omit the
 * requested artist even when songs by that artist are present.
 */
internal fun mergeReliableArtistSearchResults(
    query: String,
    exactArtist: ArtistHit?,
    verifiedArtists: List<ArtistHit>,
    limit: Int = 12
): List<ArtistHit> {
    val queryKey = artistIdentityKey(query)
    val merged = LinkedHashMap<String, ArtistHit>()

    fun add(hit: ArtistHit) {
        if (hit.name.isBlank() || hit.browseId.isBlank() || hit.thumbnailUrl.isBlank()) return
        val key = hit.browseId.trim().lowercase().ifBlank { artistIdentityKey(hit.name) }
        val current = merged[key]
        if (current == null || !current.officialArtwork && hit.officialArtwork) {
            merged[key] = hit
        }
    }

    exactArtist
        ?.takeIf { queryKey.isNotBlank() && artistIdentityKey(it.name) == queryKey }
        ?.let(::add)

    verifiedArtists
        .sortedWith(
            compareByDescending<ArtistHit> { queryKey.isNotBlank() && artistIdentityKey(it.name) == queryKey }
                .thenByDescending { it.officialArtwork }
                .thenBy { it.name.lowercase() }
        )
        .forEach(::add)

    return merged.values.take(limit.coerceIn(1, 24))
}

/**
 * Replaces provider placeholders on song results only when the search query exactly matches an
 * independently verified artist. Real song artist metadata always wins.
 */
internal fun enrichSearchTracksWithExactArtist(
    query: String,
    results: SearchResults,
    reliableArtists: List<ArtistHit>
): SearchResults {
    val queryKey = artistIdentityKey(query)
    val exactArtist = reliableArtists.firstOrNull { artist ->
        queryKey.isNotBlank() && artistIdentityKey(artist.name) == queryKey
    } ?: return results

    fun Track.withExactArtistWhenMissing(): Track {
        val hasProviderPlaceholder = artist.isBlank() ||
            artist.equals("YouTube Music", ignoreCase = true) ||
            artist.equals("YouTube", ignoreCase = true)
        if (!hasProviderPlaceholder) return this
        return copy(
            artist = exactArtist.name,
            artistBrowseIds = artistBrowseIds.ifEmpty { listOf(exactArtist.browseId) }
        )
    }

    return results.copy(
        topTrack = results.topTrack?.withExactArtistWhenMissing(),
        songs = results.songs.map(Track::withExactArtistWhenMissing)
    )
}
