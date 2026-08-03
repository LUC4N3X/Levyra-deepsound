package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.ArtistHit
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
