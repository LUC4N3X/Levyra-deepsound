package com.luc4n3x.levyra.desktop.core.storage

import com.luc4n3x.levyra.desktop.core.model.Track
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibraryStore(
    private val store: JsonFileStore<LibraryData>,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val state = MutableStateFlow(store.read())

    val library: StateFlow<LibraryData> = state.asStateFlow()

    val current: LibraryData get() = state.value

    fun isFavorite(trackId: String): Boolean = state.value.favorites.any { it.id == trackId }

    fun toggleFavorite(track: Track) = mutate { data ->
        val existing = data.favorites.any { it.id == track.id }
        val favorites = if (existing) {
            data.favorites.filterNot { it.id == track.id }
        } else {
            listOf(track) + data.favorites
        }
        data.copy(favorites = favorites)
    }

    fun createPlaylist(name: String): String {
        val trimmed = name.trim().ifBlank { "Nuova playlist" }
        val id = UUID.randomUUID().toString()
        mutate { data ->
            data.copy(
                playlists = data.playlists + LocalPlaylist(
                    id = id,
                    name = trimmed,
                    createdAt = nowMillis()
                )
            )
        }
        return id
    }

    fun renamePlaylist(playlistId: String, name: String) = mutate { data ->
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@mutate data
        data.copy(
            playlists = data.playlists.map { playlist ->
                if (playlist.id == playlistId) playlist.copy(name = trimmed) else playlist
            }
        )
    }

    fun deletePlaylist(playlistId: String) = mutate { data ->
        data.copy(playlists = data.playlists.filterNot { it.id == playlistId })
    }

    fun addToPlaylist(playlistId: String, tracks: List<Track>) = mutate { data ->
        if (tracks.isEmpty()) return@mutate data
        data.copy(
            playlists = data.playlists.map { playlist ->
                if (playlist.id != playlistId) {
                    playlist
                } else {
                    val existing = playlist.tracks.map { it.id }.toSet()
                    val additions = tracks.distinctBy { it.id }.filterNot { it.id in existing }
                    playlist.copy(tracks = playlist.tracks + additions)
                }
            }
        )
    }

    fun removeFromPlaylist(playlistId: String, trackId: String) = mutate { data ->
        data.copy(
            playlists = data.playlists.map { playlist ->
                if (playlist.id != playlistId) {
                    playlist
                } else {
                    playlist.copy(tracks = playlist.tracks.filterNot { it.id == trackId })
                }
            }
        )
    }

    fun recordPlayback(track: Track) = mutate { data ->
        val entry = HistoryEntry(track = track, playedAt = nowMillis())
        val history = (listOf(entry) + data.history.filterNot { it.track.id == track.id })
            .take(MAX_HISTORY)
        data.copy(history = history)
    }

    fun clearHistory() = mutate { data -> data.copy(history = emptyList()) }

    fun recordSearch(query: String) = mutate { data ->
        val trimmed = query.trim()
        if (trimmed.length < 2) return@mutate data
        val searches = (listOf(trimmed) + data.recentSearches.filterNot { it.equals(trimmed, ignoreCase = true) })
            .take(MAX_RECENT_SEARCHES)
        data.copy(recentSearches = searches)
    }

    fun clearRecentSearches() = mutate { data -> data.copy(recentSearches = emptyList()) }

    @Synchronized
    private fun mutate(transform: (LibraryData) -> LibraryData) {
        val updated = transform(state.value)
        if (updated == state.value) return
        store.write(updated)
        state.value = updated
    }

    companion object {
        const val MAX_HISTORY = 200
        const val MAX_RECENT_SEARCHES = 12

        fun create(paths: AppPaths): LibraryStore = LibraryStore(
            JsonFileStore(
                file = paths.libraryFile,
                serializer = LibraryData.serializer(),
                defaultValue = { LibraryData() }
            )
        )
    }
}
