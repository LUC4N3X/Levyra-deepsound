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

/**
 * Removes visually duplicated recommendations even when YouTube returns different browse IDs,
 * channel suffixes, featured-artist variants, or edition labels for the same release.
 */
internal fun deduplicateHomeAlbums(
    albums: List<AlbumHit>,
    limit: Int = 24
): List<AlbumHit> {
    val seenIdentity = HashSet<String>()
    val seenArtwork = HashSet<String>()
    val result = ArrayList<AlbumHit>(minOf(albums.size, limit.coerceIn(1, 48)))

    for (album in albums) {
        if (album.title.isBlank() || album.artist.isBlank()) continue

        val titleKey = album.title.homeAlbumTitleKey()
        val artistKey = album.artist.homeAlbumArtistKey()
        if (titleKey.isBlank() || artistKey.isBlank()) continue

        val identityKey = "$titleKey|$artistKey"
        val artworkUrl = album.thumbnailUrl
            .trim()
            .lowercase(Locale.ROOT)
            .substringBefore('?')
            .substringBefore('=')
        val artworkKey = artworkUrl.takeIf(String::isNotBlank)?.let { "$titleKey|$it" }

        if (identityKey in seenIdentity || artworkKey != null && artworkKey in seenArtwork) continue

        seenIdentity += identityKey
        if (artworkKey != null) seenArtwork += artworkKey
        result += album
        if (result.size >= limit.coerceIn(1, 48)) break
    }

    return result
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

private fun String.homeAlbumTitleKey(): String {
    return normalizedSearchText()
        .replace(HOME_ALBUM_BRACKETED_SUFFIX, " ")
        .replace(HOME_ALBUM_EDITION_SUFFIX, " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun String.homeAlbumArtistKey(): String {
    return normalizedSearchText()
        .replace(HOME_ALBUM_CHANNEL_SUFFIX, " ")
        .split(HOME_ALBUM_ARTIST_SEPARATOR, limit = 2)
        .firstOrNull()
        .orEmpty()
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun String.normalizedSearchText(): String {
    return Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace('&', ' ')
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private val HOME_ALBUM_BRACKETED_SUFFIX = Regex(
    "(?i)\\s*[\\[(](?:deluxe|expanded|anniversary|remaster(?:ed)?|bonus|special|collector|legacy|tour|digital|international|explicit|clean|standard|edition|version).*?[\\])].*$"
)

private val HOME_ALBUM_EDITION_SUFFIX = Regex(
    "(?i)\\s+(?:deluxe|expanded|anniversary|remaster(?:ed)?|bonus|special|collector|legacy|tour|digital|international|explicit|clean|standard)(?:\\s+(?:edition|version|album))?.*$"
)

private val HOME_ALBUM_CHANNEL_SUFFIX = Regex(
    "(?i)\\s+(?:topic|vevo|official|music)$"
)

private val HOME_ALBUM_ARTIST_SEPARATOR = Regex(
    "(?i)\\s*(?:,|;|\\bfeat(?:uring)?\\b|\\bft\\b|\\bwith\\b|\\s+[x×]\\s+)\\s*"
)
