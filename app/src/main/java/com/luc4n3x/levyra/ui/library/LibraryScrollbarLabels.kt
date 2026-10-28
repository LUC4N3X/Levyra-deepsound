package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.domain.LibrarySort
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.levyraScrollbarGlyph

internal val LibraryScrollbarCategories: Set<LibraryCategory> = setOf(
    LibraryCategory.Songs,
    LibraryCategory.Albums,
    LibraryCategory.Artists,
    LibraryCategory.Playlists,
    LibraryCategory.Offline
)

private fun trackSortText(track: Track, sort: LibrarySort): String? = when (sort) {
    LibrarySort.Title -> track.title
    LibrarySort.Artist -> track.artist
    LibrarySort.Album -> track.album
    LibrarySort.Recent, LibrarySort.Duration -> null
}

private fun alphabetical(sort: LibrarySort): Boolean =
    sort == LibrarySort.Title || sort == LibrarySort.Artist || sort == LibrarySort.Album

internal fun libraryScrollbarLabels(
    category: LibraryCategory,
    sort: LibrarySort,
    tracks: List<Track>,
    albums: List<LibraryAlbum>,
    artists: List<LibraryArtist>,
    playlists: List<Playlist>,
    offline: List<LibraryOfflineItem>
): Map<String, String> {
    if (!alphabetical(sort)) return emptyMap()
    val labels = HashMap<String, String>()
    fun put(key: String, text: String?) {
        levyraScrollbarGlyph(text)?.let { labels[key] = it }
    }
    when (category) {
        LibraryCategory.Songs -> tracks.forEach { put("song-${libraryTrackKey(it)}", trackSortText(it, sort)) }
        LibraryCategory.Offline -> offline.forEach { put("offline-${it.key}", trackSortText(it.track, sort)) }
        LibraryCategory.Albums -> {
            val text = { album: LibraryAlbum -> if (sort == LibrarySort.Artist) album.artist else album.title }
            albums.forEach { put("album-${it.key}", text(it)) }
            albums.chunked(2).forEach { row ->
                put("album-grid-row-${row.joinToString("-") { it.key }}", text(row.first()))
            }
        }
        LibraryCategory.Artists -> {
            artists.forEach { put("artist-${it.key}", it.name) }
            artists.chunked(2).forEach { row ->
                put("artist-grid-row-${row.joinToString("-") { it.key }}", row.first().name)
            }
        }
        LibraryCategory.Playlists -> {
            playlists.forEach { put("playlist-${it.id}", it.name) }
            playlists.chunked(2).forEach { row ->
                put("playlist-grid-row-${row.joinToString("-") { it.id }}", row.first().name)
            }
        }
        LibraryCategory.Overview, LibraryCategory.Device, LibraryCategory.Collections -> Unit
    }
    return labels
}
