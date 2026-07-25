package com.luc4n3x.levyra.desktop.core.storage

import com.luc4n3x.levyra.desktop.core.model.Track
import kotlinx.serialization.Serializable

@Serializable
data class LocalPlaylist(
    val id: String,
    val name: String,
    val createdAt: Long,
    val tracks: List<Track> = emptyList()
) {
    val artworkUrl: String get() = tracks.firstOrNull { it.hasArtwork }?.artworkUrl.orEmpty()
}

@Serializable
data class HistoryEntry(
    val track: Track,
    val playedAt: Long
)

@Serializable
data class LibraryData(
    val favorites: List<Track> = emptyList(),
    val playlists: List<LocalPlaylist> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val recentSearches: List<String> = emptyList()
)

@Serializable
data class SessionData(
    val queue: List<Track> = emptyList(),
    val index: Int = 0,
    val positionMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: String = "OFF"
)

@Serializable
data class WindowPlacement(
    val width: Int = 1240,
    val height: Int = 820,
    val x: Int = Int.MIN_VALUE,
    val y: Int = Int.MIN_VALUE,
    val maximized: Boolean = false
) {
    val hasPosition: Boolean get() = x != Int.MIN_VALUE && y != Int.MIN_VALUE
}
