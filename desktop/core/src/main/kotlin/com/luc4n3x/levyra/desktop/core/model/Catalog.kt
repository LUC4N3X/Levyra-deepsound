package com.luc4n3x.levyra.desktop.core.model

import kotlinx.serialization.Serializable
import org.schabi.newpipe.extractor.Page

enum class SearchFilter(val contentFilter: String) {
    SONGS("music_songs"),
    VIDEOS("music_videos"),
    ALBUMS("music_albums"),
    PLAYLISTS("music_playlists"),
    ARTISTS("music_artists")
}

enum class CollectionKind {
    ALBUM,
    PLAYLIST,
    ARTIST
}

@Serializable
data class CollectionRef(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val artworkUrl: String = "",
    val url: String,
    val kind: CollectionKind,
    val itemCount: Long = 0L
)

@JvmInline
value class PageCursor(val page: Page)

data class CatalogPage(
    val tracks: List<Track> = emptyList(),
    val collections: List<CollectionRef> = emptyList(),
    val cursor: PageCursor? = null
) {
    val isEmpty: Boolean get() = tracks.isEmpty() && collections.isEmpty()

    fun mergedWith(other: CatalogPage): CatalogPage {
        val knownTracks = tracks.mapTo(HashSet()) { it.id }
        val knownCollections = collections.mapTo(HashSet()) { it.id }
        return CatalogPage(
            tracks = tracks + other.tracks.filterNot { it.id in knownTracks },
            collections = collections + other.collections.filterNot { it.id in knownCollections },
            cursor = other.cursor
        )
    }
}

data class CollectionDetail(
    val ref: CollectionRef,
    val page: CatalogPage
)
