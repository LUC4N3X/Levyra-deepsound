package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistAudienceWeight
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.artistIdentityKeys
import com.luc4n3x.levyra.domain.artistIdentityMatches

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
    val merged = LinkedHashMap<String, ArtistHit>()

    fun add(hit: ArtistHit) {
        if (hit.name.isBlank() || hit.browseId.isBlank() || hit.thumbnailUrl.isBlank()) return
        val key = hit.browseId.trim().lowercase().ifBlank { artistIdentityKey(hit.name) }
        val current = merged[key]
        if (current == null || !current.officialArtwork && hit.officialArtwork) {
            merged[key] = hit
        }
    }

    val candidates = (listOfNotNull(exactArtist) + verifiedArtists)
    val typoDistance = allowedArtistTypoDistance(query)
    val nearExactMatches = candidates.filter { candidate ->
        candidate.name.isNotBlank() &&
            candidate.browseId.isNotBlank() &&
            candidate.thumbnailUrl.isNotBlank() &&
            artistNameEditDistance(query, candidate.name) <= typoDistance
    }
    if (nearExactMatches.isNotEmpty()) {
        nearExactMatches.maxWithOrNull(artistAuthorityOrderFor(query))?.let(::add)
        return merged.values.take(limit.coerceIn(1, 24))
    }

    verifiedArtists
        .sortedWith(
            compareByDescending<ArtistHit> { artistIdentityMatches(it.name, query) }
                .then(artistAuthorityOrderFor(query).reversed())
                .thenBy { it.name.lowercase() }
        )
        .forEach(::add)

    return merged.values.take(limit.coerceIn(1, 24))
}

/**
 * Resolves an artist reference. A browseId that no longer answers for the requested identity must
 * still fall back to the canonical name lookup instead of failing the whole screen, and the
 * fallback is skipped once the caller is no longer active.
 */
internal suspend fun resolveArtistProfileReference(
    browseId: String,
    name: String,
    isActive: () -> Boolean,
    profileByBrowseId: suspend (String, String) -> ArtistProfile?,
    profileByName: suspend (String) -> ArtistProfile?
): ArtistProfile? {
    val normalizedBrowseId = browseId.trim()
    val direct = if (normalizedBrowseId.isNotBlank()) {
        runCatching { profileByBrowseId(normalizedBrowseId, name) }.getOrNull()
    } else {
        null
    }
    if (direct != null) return direct
    if (!isActive()) return null
    return runCatching { profileByName(name) }.getOrNull()
}

private fun allowedArtistTypoDistance(query: String): Int {
    val shortestKey = artistIdentityKeys(query).minOfOrNull(String::length) ?: return 0
    return when {
        shortestKey >= 12 -> 2
        shortestKey >= 6 -> 1
        else -> 0
    }
}

private fun artistNameEditDistance(query: String, candidate: String): Int {
    val queryKeys = artistIdentityKeys(query)
    val candidateKeys = artistIdentityKeys(candidate)
    if (queryKeys.isEmpty() || candidateKeys.isEmpty()) return Int.MAX_VALUE
    return queryKeys.minOf { queryKey ->
        candidateKeys.minOf { candidateKey -> levenshteinDistance(queryKey, candidateKey) }
    }
}

private fun levenshteinDistance(first: String, second: String): Int {
    if (first == second) return 0
    if (first.isEmpty()) return second.length
    if (second.isEmpty()) return first.length

    var previous = IntArray(second.length + 1) { it }
    first.forEachIndexed { firstIndex, firstChar ->
        val current = IntArray(second.length + 1)
        current[0] = firstIndex + 1
        second.forEachIndexed { secondIndex, secondChar ->
            val substitution = previous[secondIndex] + if (firstChar == secondChar) 0 else 1
            current[secondIndex + 1] = minOf(
                current[secondIndex] + 1,
                previous[secondIndex + 1] + 1,
                substitution
            )
        }
        previous = current
    }
    return previous[second.length]
}

/**
 * Ranks artists that match the query equally well: a verified official page and a real audience
 * beat a same-named channel with a handful of subscribers.
 */
private val artistAuthorityOrder: Comparator<ArtistHit> =
    compareBy<ArtistHit> { it.officialArtwork }
        .thenBy { artistAudienceWeight(it.subscribers) }
        .thenBy { it.thumbnailUrl.isNotBlank() }

private fun artistAuthorityOrderFor(query: String): Comparator<ArtistHit> =
    compareBy<ArtistHit> { artistIdentityMatches(it.name, query) }
        .then(artistAuthorityOrder)

/**
 * Replaces provider placeholders on song results only when the search query exactly matches an
 * independently verified artist. Real song artist metadata always wins.
 */
internal fun enrichSearchTracksWithExactArtist(
    query: String,
    results: SearchResults,
    reliableArtists: List<ArtistHit>
): SearchResults {
    val exactArtist = reliableArtists
        .filter { artist -> artistIdentityMatches(artist.name, query) }
        .maxWithOrNull(artistAuthorityOrderFor(query))
        ?: return results

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
