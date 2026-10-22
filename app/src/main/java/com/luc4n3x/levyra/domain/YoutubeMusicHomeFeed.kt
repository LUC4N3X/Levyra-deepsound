package com.luc4n3x.levyra.domain

import androidx.compose.runtime.Immutable

enum class YoutubeMusicHomeItemKind { Song, Video, Album, Playlist, Artist }

enum class YoutubeMusicHomeShelfStyle { ListColumns, Cards, Videos, Artists, Buttons }

@Immutable
data class YoutubeMusicHomeItem(
    val key: String,
    val kind: YoutubeMusicHomeItemKind,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val track: Track? = null,
    val album: AlbumHit? = null,
    val playlist: PlaylistHit? = null,
    val artist: ArtistHit? = null
)

@Immutable
data class YoutubeMusicHomeShelf(
    val key: String,
    val title: String,
    val strapline: String,
    val thumbnailUrl: String,
    val style: YoutubeMusicHomeShelfStyle,
    val items: List<YoutubeMusicHomeItem>,
    val headerBrowseId: String = "",
    val buttons: List<YoutubeMusicHomeButton> = emptyList()
) {
    val tracks: List<Track> get() = items.mapNotNull { it.track }
}

@Immutable
data class YoutubeMusicHomeButton(
    val title: String,
    val stripeColor: Long,
    val browseId: String,
    val params: String
)

@Immutable
data class YoutubeMusicHomeChip(
    val title: String,
    val params: String
)

@Immutable
data class YoutubeMusicHomeFeed(
    val chips: List<YoutubeMusicHomeChip> = emptyList(),
    val shelves: List<YoutubeMusicHomeShelf> = emptyList(),
    val continuation: String = ""
)
