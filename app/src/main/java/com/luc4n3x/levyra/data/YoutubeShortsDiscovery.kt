package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.artistDisplayCandidates
import java.util.Locale

private val ShortsChannelSuffix = Regex("(?i)\\s*(?:-\\s*topic|vevo|official)\\s*$")

private fun shortsArtistKey(artist: String): String =
    artistIdentityKey(artist.replace(ShortsChannelSuffix, ""))

private fun shortsArtistKeys(artist: String): Set<String> =
    (listOf(artist) + artistDisplayCandidates(artist)).map(::shortsArtistKey).filter(String::isNotBlank).toSet()

internal fun youtubeShortGenreTag(tags: Set<String>): String? {
    val normalized = tags.map { it.lowercase(Locale.ROOT) }
    return listOf("rap", "phonk", "rnb", "rock", "electronic", "afrobeats", "reggaeton", "jazz", "pop", "chill").firstOrNull { genre ->
        normalized.any { tag -> tag == genre || tag.startsWith("$genre ") }
    } ?: when {
        normalized.any { it in setOf("hip-hop", "hip hop", "trap", "drill") } -> "rap"
        normalized.any { it in setOf("r&b", "soul") } -> "rnb"
        else -> null
    }
}

internal fun rankYoutubeShortDiscovery(
    samples: List<Track>,
    personalized: List<Track>,
    seeds: List<Track>,
    preferredArtists: List<String>,
    limit: Int
): List<Track> {
    if (limit <= 0) return emptyList()
    val preferred = preferredArtists.flatMap { shortsArtistKeys(it) }.toSet()
    val seedTags = mutableMapOf<String, Set<String>>()
    seeds.forEach { seed -> shortsArtistKeys(seed.artist).forEach { key -> seedTags[key] = seedTags[key].orEmpty() + seed.moodTags } }
    val titles = seeds.map { it.title.trim().lowercase(Locale.ROOT) }.filter { it.length >= 4 }.toSet()
    val candidates = (samples + personalized).filter(::isYoutubeShortTrack).distinctBy { it.id }
    val ranked = candidates.map { track ->
        val artists = shortsArtistKeys(track.artist)
        val title = track.title.lowercase(Locale.ROOT)
        val artistAffinity = when {
            artists.any(preferred::contains) -> 4
            artists.any(seedTags::containsKey) -> 2
            track.album == "YouTube Music previews" -> 1
            else -> 0
        }
        val songAffinity = if (artistAffinity > 0 && titles.any(title::contains)) 3 else 0
        val tags = artists.flatMap { seedTags[it].orEmpty() }.toSet()
        Triple(track.copy(moodTags = track.moodTags + tags), artists, artistAffinity + songAffinity)
    }.sortedByDescending { it.third }
    val relevant = ranked.filter { it.third > 0 }
    val pool = relevant.ifEmpty { ranked.filter { it.first.album == "YouTube Music Samples" } }
    val shownArtists = mutableSetOf<String>()
    return pool.mapNotNull { (track, artists, _) ->
        if (artists.isEmpty() || artists.any(shownArtists::contains)) null else track.also { shownArtists.addAll(artists) }
    }.take(limit)
}
