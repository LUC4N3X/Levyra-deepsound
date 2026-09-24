package com.luc4n3x.levyra.domain

import java.util.Locale

private val ARTIST_CREDIT_SEPARATOR = Regex(
    """(?i)\s+(?:feat(?:uring)?\.?|ft\.?|with|con|vs\.?)\s+|\s*[;,]\s*|\s+[x×/]\s+|\s+(?:&|and|e|y|et|und)\s+"""
)

internal fun trackBelongsToArtist(
    track: Track,
    artistName: String,
    artistBrowseId: String
): Boolean {
    val targetId = artistBrowseId.trim()
    val trackIds = canonicalArtistBrowseIds(track.artistBrowseIds)

    if (targetId.isNotBlank() && trackIds.isNotEmpty()) {
        return trackIds.any { it.equals(targetId, ignoreCase = true) }
    }

    val targetKeys = artistIdentityKeys(artistName)
    if (targetKeys.isEmpty()) return false

    if (artistIdentityKeys(track.artist).any(targetKeys::contains)) return true
    return ARTIST_CREDIT_SEPARATOR
        .split(track.artist)
        .asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .any { credit -> artistIdentityKeys(credit).any(targetKeys::contains) }
}

internal fun likedTracksByArtist(
    favorites: List<Track>,
    profile: ArtistProfile
): List<Track> {
    if (favorites.isEmpty()) return emptyList()
    val seen = HashSet<String>(favorites.size)
    return favorites.filter { track ->
        trackBelongsToArtist(track, profile.name, profile.browseId) &&
            seen.add(artistFavoriteIdentity(track))
    }
}

private fun artistFavoriteIdentity(track: Track): String {
    track.id.trim().takeIf(String::isNotBlank)?.let {
        return "id:" + it.lowercase(Locale.ROOT)
    }
    track.isrc.trim().takeIf(String::isNotBlank)?.let {
        return "isrc:" + it.lowercase(Locale.ROOT)
    }
    track.audioVideoId.trim().takeIf(String::isNotBlank)?.let {
        return "audio:" + it.lowercase(Locale.ROOT)
    }
    track.counterpartVideoId.trim().takeIf(String::isNotBlank)?.let {
        return "video:" + it.lowercase(Locale.ROOT)
    }
    val title = artistIdentityKey(track.title)
    val artist = artistIdentityKey(track.artist)
    return "meta:" + title + "|" + artist + "|" + (track.durationMs.coerceAtLeast(0L) / 1_000L)
}
