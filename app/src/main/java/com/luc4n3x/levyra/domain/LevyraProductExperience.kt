package com.luc4n3x.levyra.domain

import java.text.Normalizer
import java.util.Locale

/** Primary destinations that remain permanently visible in the bottom navigation. */
internal val LevyraPrimaryTabs: List<LevyraTab> = listOf(
    LevyraTab.Home,
    LevyraTab.Search,
    LevyraTab.Library
)

/**
 * Keeps video results separate from ordinary audio results using provider fields that actually
 * describe media type. `videoUrl` cannot be used here because every YouTube track has a watch URL.
 */
internal fun Track.isSearchVideo(): Boolean {
    return counterpartVideoId.isNotBlank() ||
        videoType.contains("video", ignoreCase = true) ||
        source.contains("video", ignoreCase = true)
}

internal fun searchFiltersFor(
    hasArtists: Boolean,
    hasAlbums: Boolean,
    hasVideos: Boolean,
    hasPlaylists: Boolean
): List<SearchFilter> = buildList {
    add(SearchFilter.All)
    add(SearchFilter.Songs)
    if (hasVideos) add(SearchFilter.Videos)
    if (hasArtists) add(SearchFilter.Artists)
    if (hasAlbums) add(SearchFilter.Albums)
    if (hasPlaylists) add(SearchFilter.Playlists)
}

internal fun filterPlaylistsForSearch(
    query: String,
    playlists: List<Playlist>,
    limit: Int = 12
): List<Playlist> {
    val normalizedQuery = query.normalizedSearchText()
    if (normalizedQuery.isBlank()) return emptyList()

    return playlists.asSequence()
        .map { playlist ->
            val normalizedName = playlist.name.normalizedSearchText()
            val score = when {
                normalizedName == normalizedQuery -> 0
                normalizedName.startsWith(normalizedQuery) -> 1
                normalizedName.contains(normalizedQuery) -> 2
                else -> Int.MAX_VALUE
            }
            score to playlist
        }
        .filter { (score, _) -> score != Int.MAX_VALUE }
        .sortedWith(
            compareBy<Pair<Int, Playlist>> { it.first }
                .thenByDescending { it.second.updatedAt }
                .thenBy { it.second.name.lowercase(Locale.ROOT) }
        )
        .map { it.second }
        .take(limit.coerceIn(1, 24))
        .toList()
}

private fun String.normalizedSearchText(): String {
    return Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
}
