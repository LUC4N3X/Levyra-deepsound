package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.domain.Track

internal typealias PlaylistSearchIndex = Map<String, String>

internal fun buildPlaylistSearchIndex(tracks: List<Track>): PlaylistSearchIndex =
    tracks.associate { track ->
        playlistEntryKey(track) to listOf(track.title, track.artist, track.album)
            .joinToString("\u0000", transform = ::normalizeLibraryText)
    }

internal fun filterPlaylistTracks(
    tracks: List<Track>,
    query: String,
    searchIndex: PlaylistSearchIndex
): List<Track> {
    val normalizedQuery = normalizeLibraryText(query)
    if (normalizedQuery.isEmpty()) return tracks
    return tracks.filter { track ->
        searchIndex[playlistEntryKey(track)]?.contains(normalizedQuery) == true
    }
}

internal fun togglePlaylistTrackSelection(selectedKeys: Set<String>, key: String): Set<String> =
    if (key in selectedKeys) selectedKeys - key else selectedKeys + key

internal fun selectAllPlaylistTrackKeys(tracks: List<Track>): Set<String> =
    tracks.mapTo(linkedSetOf(), ::playlistEntryKey)

internal fun clearPlaylistTrackSelection(): Set<String> = emptySet()

internal fun selectedPlaylistTracks(tracks: List<Track>, selectedKeys: Set<String>): List<Track> =
    tracks.filter { playlistEntryKey(it) in selectedKeys }
