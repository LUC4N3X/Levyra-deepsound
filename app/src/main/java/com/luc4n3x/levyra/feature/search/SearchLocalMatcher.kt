package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.data.deduplicateSearchSongs
import com.luc4n3x.levyra.data.searchSongIdentityKey
import com.luc4n3x.levyra.domain.Track

internal data class LocalSearchCandidate(
    val track: Track,
    val affinity: Int
)

internal data class LocalSearchMatch(
    val track: Track,
    val score: Int,
    val strong: Boolean
)

internal object LocalSearchAffinity {
    const val RECENT = 90
    const val FAVORITE = 80
    const val ORBIT = 70
    const val QUEUE = 60
    const val SESSION = 50
    const val HOME = 40
    const val CHARTS = 30
    const val CACHE = 20
}

internal const val LOCAL_SEARCH_RESULT_LIMIT = 6

private const val SCORE_EXACT_TITLE = 1_000
private const val SCORE_EXACT_TITLE_ARTIST = 960
private const val SCORE_EXACT_ARTIST = 800
private const val SCORE_TITLE_PREFIX = 700
private const val SCORE_TITLE_TOKENS = 600
private const val SCORE_MIXED_TOKENS = 500
private const val STRONG_MATCH_SCORE = SCORE_EXACT_TITLE_ARTIST

internal fun matchLocalSearchTracks(
    query: String,
    candidates: List<LocalSearchCandidate>,
    limit: Int = LOCAL_SEARCH_RESULT_LIMIT,
    accept: (Track) -> Boolean = { true }
): List<LocalSearchMatch> {
    val queryKey = searchQueryKey(query)
    if (queryKey.length < MIN_SEARCH_QUERY_CHARS) return emptyList()
    val tokens = queryKey.split(' ').filter(String::isNotEmpty)
    if (tokens.isEmpty()) return emptyList()

    val seen = HashSet<String>()
    val ranked = ArrayList<Pair<Int, LocalSearchMatch>>()
    candidates.forEachIndexed { index, candidate ->
        val track = candidate.track
        val identity = searchSongIdentityKey(track)
        if (identity.isBlank() || track.title.isBlank() || identity in seen) return@forEachIndexed
        val baseScore = localTrackScore(queryKey, tokens, track) ?: return@forEachIndexed
        if (!accept(track)) return@forEachIndexed
        seen += identity
        val match = LocalSearchMatch(
            track = track,
            score = baseScore + candidate.affinity.coerceIn(0, 99),
            strong = baseScore >= STRONG_MATCH_SCORE
        )
        ranked += index to match
    }
    if (ranked.isEmpty()) return emptyList()

    val ordered = ranked
        .sortedWith(compareByDescending<Pair<Int, LocalSearchMatch>> { it.second.score }.thenBy { it.first })
        .map { it.second }
    val matchByIdentity = ordered.associateBy { searchSongIdentityKey(it.track) }
    return deduplicateSearchSongs(ordered.map(LocalSearchMatch::track))
        .mapNotNull { track -> matchByIdentity[searchSongIdentityKey(track)]?.copy(track = track) }
        .take(limit.coerceAtLeast(0))
}

private fun localTrackScore(queryKey: String, tokens: List<String>, track: Track): Int? {
    val title = searchQueryKey(track.title)
    if (title.isEmpty()) return null
    val artist = searchQueryKey(track.artist)
    val titleWords = title.split(' ')
    val artistWords = artist.split(' ').filter(String::isNotEmpty)
    val everyTokenMatches = tokens.all { token ->
        titleWords.any { it.startsWith(token) } || artistWords.any { it.startsWith(token) }
    }
    if (!everyTokenMatches) return null
    return when {
        title == queryKey -> SCORE_EXACT_TITLE
        artist.isNotEmpty() && ("$title $artist" == queryKey || "$artist $title" == queryKey) -> SCORE_EXACT_TITLE_ARTIST
        artist == queryKey -> SCORE_EXACT_ARTIST
        title.startsWith(queryKey) -> SCORE_TITLE_PREFIX
        tokens.all { token -> titleWords.any { it.startsWith(token) } } -> SCORE_TITLE_TOKENS
        else -> SCORE_MIXED_TOKENS
    }
}
