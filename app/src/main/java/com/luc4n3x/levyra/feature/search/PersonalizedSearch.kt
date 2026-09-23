package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.SmartMusicTasteSeed
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.primaryArtistSegment
import java.util.Locale

internal enum class PersonalizedSearchPromptKind {
    ARTIST,
    SIMILAR_TRACK,
    ALBUM
}

internal data class PersonalizedSearchPrompt(
    val kind: PersonalizedSearchPromptKind,
    val value: String
)

internal data class PersonalizedSearchSnapshot(
    val tracks: List<Track> = emptyList(),
    val artistNames: List<String> = emptyList(),
    val prompt: PersonalizedSearchPrompt? = null
)

internal fun buildPersonalizedSearchSnapshot(
    favorites: List<Track>,
    recentListens: List<Track>,
    recentSearches: List<Track>,
    personalOrbitTracks: List<Track>,
    topArtists: List<SmartMusicTasteSeed>,
    dayBucket: Long = System.currentTimeMillis() / DAY_MS
): PersonalizedSearchSnapshot {
    val sources = listOf(
        rotateWindow(recentListens, dayBucket, 12),
        rotateWindow(favorites, dayBucket / 2L, 12),
        rotateWindow(personalOrbitTracks, dayBucket / 3L, 12),
        rotateWindow(recentSearches, dayBucket / 5L, 8)
    )
    val tracks = interleaveDiverseTracks(sources, PERSONALIZED_TRACK_LIMIT)
    val artistNames = buildList {
        topArtists.asSequence().map(SmartMusicTasteSeed::label).forEach { add(it) }
        sources.asSequence().flatten().map(Track::artist).forEach { add(it) }
    }
        .map(::primaryArtistSegment)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .take(PERSONALIZED_ARTIST_LIMIT)
    val prompts = buildList {
        artistNames.take(3).forEach { add(PersonalizedSearchPrompt(PersonalizedSearchPromptKind.ARTIST, it)) }
        tracks.take(3).forEach { track ->
            track.title.trim().takeIf(String::isNotEmpty)?.let {
                add(PersonalizedSearchPrompt(PersonalizedSearchPromptKind.SIMILAR_TRACK, it))
            }
        }
        tracks.asSequence()
            .map(Track::album)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(2)
            .forEach { add(PersonalizedSearchPrompt(PersonalizedSearchPromptKind.ALBUM, it)) }
    }
    val prompt = prompts.getOrNull(stableIndex(dayBucket, prompts.size))
    return PersonalizedSearchSnapshot(tracks = tracks, artistNames = artistNames, prompt = prompt)
}

internal fun rankPersonalizedSearchArtists(
    artists: List<ArtistHit>,
    preferredNames: List<String>
): List<ArtistHit> {
    if (artists.isEmpty() || preferredNames.isEmpty()) return artists
    val positions = preferredNames.mapIndexed { index, name -> name.lowercase(Locale.ROOT) to index }.toMap()
    return artists.withIndex()
        .sortedWith(
            compareBy<IndexedValue<ArtistHit>> {
                positions[it.value.name.trim().lowercase(Locale.ROOT)] ?: Int.MAX_VALUE
            }.thenBy(IndexedValue<ArtistHit>::index)
        )
        .map(IndexedValue<ArtistHit>::value)
}

private fun interleaveDiverseTracks(sources: List<List<Track>>, limit: Int): List<Track> {
    val result = ArrayList<Track>(limit)
    val identities = HashSet<String>()
    val artistCounts = HashMap<String, Int>()
    var row = 0
    while (result.size < limit && sources.any { row < it.size }) {
        for (source in sources) {
            val track = source.getOrNull(row) ?: continue
            val identity = track.id.trim().lowercase(Locale.ROOT)
                .ifEmpty { "${track.title.trim().lowercase(Locale.ROOT)}|${track.artist.trim().lowercase(Locale.ROOT)}" }
            if (identity.isEmpty() || !identities.add(identity)) continue
            val artist = primaryArtistSegment(track.artist).trim().lowercase(Locale.ROOT)
            if (artist.isNotEmpty() && artistCounts.getOrDefault(artist, 0) >= MAX_TRACKS_PER_ARTIST) continue
            result += track
            if (artist.isNotEmpty()) artistCounts[artist] = artistCounts.getOrDefault(artist, 0) + 1
            if (result.size == limit) break
        }
        row += 1
    }
    return result
}

private fun <T> rotateWindow(values: List<T>, seed: Long, maxCandidates: Int): List<T> {
    val candidates = values.take(maxCandidates)
    if (candidates.size < 2) return candidates
    val offset = stableIndex(seed, minOf(candidates.size, ROTATION_WINDOW))
    return candidates.drop(offset) + candidates.take(offset)
}

private fun stableIndex(seed: Long, size: Int): Int =
    if (size <= 0) 0 else Math.floorMod(seed, size.toLong()).toInt()

private const val DAY_MS = 86_400_000L
private const val PERSONALIZED_TRACK_LIMIT = 8
private const val PERSONALIZED_ARTIST_LIMIT = 7
private const val MAX_TRACKS_PER_ARTIST = 2
private const val ROTATION_WINDOW = 4
