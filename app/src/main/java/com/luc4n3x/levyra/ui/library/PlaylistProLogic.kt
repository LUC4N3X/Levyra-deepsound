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

internal fun selectPlaylistTracks(
    selectedKeys: Set<String>,
    tracks: List<Track>
): Set<String> = selectedKeys + selectAllPlaylistTrackKeys(tracks)

internal fun areAllPlaylistTracksSelected(
    tracks: List<Track>,
    selectedKeys: Set<String>
): Boolean = tracks.isNotEmpty() && tracks.all { playlistEntryKey(it) in selectedKeys }

internal fun clearPlaylistTrackSelection(): Set<String> = emptySet()

internal fun selectedPlaylistTracks(tracks: List<Track>, selectedKeys: Set<String>): List<Track> =
    tracks.filter { playlistEntryKey(it) in selectedKeys }

internal enum class PlaylistTrackSort {
    Custom,
    TitleAscending,
    TitleDescending,
    NewestFirst,
    OldestFirst,
    ShortestFirst,
    LongestFirst
}

private class PlaylistSortEntry(
    val track: Track,
    val position: Int,
    val title: String,
    val artist: String
)

private val playlistTitleOrder: Comparator<PlaylistSortEntry> =
    compareBy<PlaylistSortEntry> { it.title.isEmpty() }.thenBy { it.title }

private val playlistTieBreakOrder: Comparator<PlaylistSortEntry> =
    compareBy<PlaylistSortEntry> { it.artist }.thenBy { it.position }

private val playlistDurationOrder: Comparator<PlaylistSortEntry> =
    compareBy { it.track.durationMs <= 0L }

internal fun sortPlaylistTracks(tracks: List<Track>, sort: PlaylistTrackSort): List<Track> {
    if (sort == PlaylistTrackSort.Custom || tracks.size < 2) return tracks
    val entries = tracks.mapIndexed { index, track ->
        PlaylistSortEntry(
            track = track,
            position = index,
            title = normalizeLibraryText(track.title),
            artist = normalizeLibraryText(track.artist)
        )
    }
    val comparator: Comparator<PlaylistSortEntry> = when (sort) {
        PlaylistTrackSort.Custom -> return tracks
        PlaylistTrackSort.TitleAscending -> playlistTitleOrder.then(playlistTieBreakOrder)
        PlaylistTrackSort.TitleDescending -> compareBy<PlaylistSortEntry> { it.title.isEmpty() }
            .thenByDescending { it.title }
            .then(playlistTieBreakOrder)
        PlaylistTrackSort.NewestFirst -> compareByDescending<PlaylistSortEntry> { it.track.playlistAddedAt }
            .thenByDescending { it.position }
        PlaylistTrackSort.OldestFirst -> compareBy<PlaylistSortEntry> { it.track.playlistAddedAt }
            .thenBy { it.position }
        PlaylistTrackSort.ShortestFirst -> playlistDurationOrder
            .thenBy { it.track.durationMs }
            .then(playlistTitleOrder)
            .then(playlistTieBreakOrder)
        PlaylistTrackSort.LongestFirst -> playlistDurationOrder
            .thenByDescending { it.track.durationMs }
            .then(playlistTitleOrder)
            .then(playlistTieBreakOrder)
    }
    return entries.sortedWith(comparator).map { it.track }
}
