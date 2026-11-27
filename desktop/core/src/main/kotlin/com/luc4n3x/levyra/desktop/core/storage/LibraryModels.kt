package com.luc4n3x.levyra.desktop.core.storage

import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.model.Track
import kotlinx.serialization.Serializable

@Serializable
data class LocalPlaylist(
    val id: String,
    val name: String,
    val createdAt: Long,
    val tracks: List<Track> = emptyList(),
    val updatedAt: Long = 0L
) {
    val artworkUrl: String get() = tracks.firstOrNull { it.hasArtwork }?.artworkUrl.orEmpty()
}

@Serializable
data class HistoryEntry(
    val track: Track,
    val playedAt: Long,
    val playCount: Int = 1
)

@Serializable
data class LibraryData(
    val favorites: List<Track> = emptyList(),
    val playlists: List<LocalPlaylist> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val followedArtists: List<CollectionRef> = emptyList(),
    val pinnedArtists: List<CollectionRef> = emptyList(),
    val excludedArtists: Map<String, String> = emptyMap(),
    val playlistSyncs: List<PlaylistSyncLink> = emptyList()
)

@Serializable
data class PlaylistSyncLink(
    val playlistId: String,
    val accountId: String,
    val remotePlaylistId: String,
    val localTitle: String = "",
    val remoteTitle: String = "",
    val videoIds: List<String> = emptyList(),
    val autoSync: Boolean = true,
    val synced: Boolean = false,
    val localUpdatedAt: Long = 0L,
    val lastSyncedAt: Long = 0L,
    val lastError: String = "",
    val unmatched: Int = 0,
    val keptLocal: Int = 0
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
