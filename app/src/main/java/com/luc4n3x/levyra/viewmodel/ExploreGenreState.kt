package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track

data class ExploreGenreChartState(
    val key: String = "",
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val refreshedAtMs: Long = 0L
)

private const val EXPLORE_GENRE_CHART_REFRESH_MS = 6 * 60 * 60 * 1000L

internal fun ExploreGenreChartState.coversRequest(requestKey: String, nowMs: Long): Boolean {
    if (key != requestKey) return false
    if (isLoading) return true
    return tracks.isNotEmpty() && refreshedAtMs > 0L && nowMs - refreshedAtMs < EXPLORE_GENRE_CHART_REFRESH_MS
}

internal fun ExploreGenreChartState.finishedWith(result: List<Track>, nowMs: Long): ExploreGenreChartState = copy(
    tracks = result.ifEmpty { tracks },
    isLoading = false,
    refreshedAtMs = if (result.isEmpty()) refreshedAtMs else nowMs
)

data class ExploreGenreDiscoveryState(
    val key: String = "",
    val storefront: String = "",
    val apple: List<com.luc4n3x.levyra.data.apple.AppleCatalogShelf> = emptyList(),
    val lastFm: com.luc4n3x.levyra.data.lastfm.LastFmGenreDiscovery? = null,
    val playlists: List<com.luc4n3x.levyra.data.YoutubeMusicMoodSection> = emptyList(),
    val isLoading: Boolean = false
)
