package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.domain.SearchResults
import java.util.concurrent.ConcurrentHashMap

internal enum class SearchLatencyMark(val label: String) {
    LOCAL("query_to_local_ms"),
    SUGGESTION("query_to_suggestion_ms"),
    NETWORK_FIRST("query_to_network_first_ms"),
    SONG_VISIBLE("query_to_song_visible_ms"),
    ARTIST_VISIBLE("query_to_artist_visible_ms"),
    ALBUM_VISIBLE("query_to_album_visible_ms"),
    VERIFIED("query_to_verified_ms"),
    SETTLED("query_to_settled_ms")
}

internal data class SearchLatencyReport(
    val elapsedMs: Map<SearchLatencyMark, Long>,
    val queryLength: Int,
    val cacheHit: Boolean
) {
    fun format(): String = buildString {
        SearchLatencyMark.entries.forEach { mark ->
            append(mark.label).append('=').append(elapsedMs[mark]?.toString() ?: "-").append(' ')
        }
        append("query_len=").append(queryLength).append(" cache_hit=").append(cacheHit)
    }
}

internal class SearchLatencyTrace(private val clock: () -> Long) {
    private val startedAtMs = clock()
    private val marks = ConcurrentHashMap<SearchLatencyMark, Long>()

    fun mark(mark: SearchLatencyMark) {
        marks.putIfAbsent(mark, (clock() - startedAtMs).coerceAtLeast(0L))
    }

    fun markVisible(results: SearchResults) {
        if (results.topTrack != null || results.songs.isNotEmpty()) mark(SearchLatencyMark.SONG_VISIBLE)
        if (results.artists.isNotEmpty()) mark(SearchLatencyMark.ARTIST_VISIBLE)
        if (results.albums.isNotEmpty()) mark(SearchLatencyMark.ALBUM_VISIBLE)
    }

    fun report(queryLength: Int, cacheHit: Boolean): SearchLatencyReport =
        SearchLatencyReport(HashMap(marks), queryLength, cacheHit)
}
