package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.domain.PlaylistItem
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

private class PlaylistSortEntry<T>(
    val value: T,
    val position: Int,
    val title: String,
    val artist: String,
    val addedAt: Long,
    val durationMs: Long
)

private fun <T> playlistSortComparator(sort: PlaylistTrackSort): Comparator<PlaylistSortEntry<T>>? {
    val titleOrder = compareBy<PlaylistSortEntry<T>> { it.title.isEmpty() }.thenBy { it.title }
    val tieBreak = compareBy<PlaylistSortEntry<T>> { it.artist }.thenBy { it.position }
    val durationKnown = compareBy<PlaylistSortEntry<T>> { it.durationMs <= 0L }
    return when (sort) {
        PlaylistTrackSort.Custom -> null
        PlaylistTrackSort.TitleAscending -> titleOrder.then(tieBreak)
        PlaylistTrackSort.TitleDescending -> compareBy<PlaylistSortEntry<T>> { it.title.isEmpty() }
            .thenByDescending { it.title }
            .then(tieBreak)
        PlaylistTrackSort.NewestFirst -> compareByDescending<PlaylistSortEntry<T>> { it.addedAt }
            .thenByDescending { it.position }
        PlaylistTrackSort.OldestFirst -> compareBy<PlaylistSortEntry<T>> { it.addedAt }
            .thenBy { it.position }
        PlaylistTrackSort.ShortestFirst -> durationKnown
            .thenBy { it.durationMs }
            .then(titleOrder)
            .then(tieBreak)
        PlaylistTrackSort.LongestFirst -> durationKnown
            .thenByDescending { it.durationMs }
            .then(titleOrder)
            .then(tieBreak)
    }
}

internal fun sortPlaylistTracks(tracks: List<Track>, sort: PlaylistTrackSort): List<Track> {
    if (tracks.size < 2) return tracks
    val comparator = playlistSortComparator<Track>(sort) ?: return tracks
    return tracks.mapIndexed { index, track ->
        PlaylistSortEntry(
            value = track,
            position = index,
            title = normalizeLibraryText(track.title),
            artist = normalizeLibraryText(track.artist),
            addedAt = track.playlistAddedAt,
            durationMs = track.durationMs
        )
    }.sortedWith(comparator).map { it.value }
}

internal fun sortPlaylistItems(items: List<PlaylistItem>, sort: PlaylistTrackSort): List<PlaylistItem> {
    if (items.size < 2) return items
    val comparator = playlistSortComparator<PlaylistItem>(sort) ?: return items
    return items.mapIndexed { index, item ->
        when (item) {
            is PlaylistItem.Song -> PlaylistSortEntry<PlaylistItem>(
                value = item,
                position = index,
                title = normalizeLibraryText(item.track.title),
                artist = normalizeLibraryText(item.track.artist),
                addedAt = item.track.playlistAddedAt,
                durationMs = item.track.durationMs
            )
            is PlaylistItem.Album -> PlaylistSortEntry<PlaylistItem>(
                value = item,
                position = index,
                title = normalizeLibraryText(item.entry.album.title),
                artist = normalizeLibraryText(item.entry.album.artist),
                addedAt = item.entry.addedAt,
                durationMs = item.entry.durationMs
            )
        }
    }.sortedWith(comparator).map { it.value }
}

internal fun buildPlaylistItemSearchIndex(items: List<PlaylistItem>): PlaylistSearchIndex =
    items.associate { item ->
        val fields = when (item) {
            is PlaylistItem.Song -> listOf(item.track.title, item.track.artist, item.track.album)
            is PlaylistItem.Album -> listOf(item.entry.album.title, item.entry.album.artist) +
                item.entry.tracks.map { it.title }
        }
        item.key to fields.joinToString("\u0000", transform = ::normalizeLibraryText)
    }

internal fun filterPlaylistItems(
    items: List<PlaylistItem>,
    query: String,
    searchIndex: PlaylistSearchIndex
): List<PlaylistItem> {
    val normalizedQuery = normalizeLibraryText(query)
    if (normalizedQuery.isEmpty()) return items
    return items.filter { item -> searchIndex[item.key]?.contains(normalizedQuery) == true }
}

internal fun selectedPlaylistItems(items: List<PlaylistItem>, selectedKeys: Set<String>): List<PlaylistItem> =
    items.filter { it.key in selectedKeys }

internal fun selectAllPlaylistItemKeys(items: List<PlaylistItem>): Set<String> =
    items.mapTo(linkedSetOf()) { it.key }

internal fun areAllPlaylistItemsSelected(items: List<PlaylistItem>, selectedKeys: Set<String>): Boolean =
    items.isNotEmpty() && items.all { it.key in selectedKeys }
