package com.luc4n3x.levyra.domain

import java.util.Locale

data class PlaylistAlbumEntry(
    val entryId: String,
    val album: AlbumHit,
    val tracks: List<Track>,
    val trackOffset: Int,
    val addedAt: Long
) {
    val identityKey: String get() = playlistAlbumIdentity(album)
    val trackCount: Int get() = tracks.size
    val durationMs: Long get() = tracks.sumOf { it.durationMs.coerceAtLeast(0L) }
    val artworkUrl: String
        get() = album.thumbnailUrl.ifBlank {
            tracks.firstNotNullOfOrNull { track ->
                track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.takeIf(String::isNotBlank)
            }.orEmpty()
        }
}

sealed interface PlaylistItem {
    val key: String

    data class Song(val track: Track) : PlaylistItem {
        override val key: String
            get() = track.playlistEntryId.takeIf(String::isNotBlank)?.let { "song:$it" } ?: "song-id:${track.id}"
    }

    data class Album(val entry: PlaylistAlbumEntry) : PlaylistItem {
        override val key: String get() = "album:${entry.entryId}"
    }
}

val Playlist.albumCount: Int get() = albums.size

val Playlist.playableTrackCount: Int get() = tracks.size + albums.sumOf { it.trackCount }

val Playlist.totalDurationMs: Long
    get() = tracks.sumOf { it.durationMs.coerceAtLeast(0L) } + albums.sumOf { it.durationMs }

fun Playlist.items(): List<PlaylistItem> = mergePlaylistItems(tracks, albums)

fun Playlist.containsAlbum(album: AlbumHit): Boolean {
    val identity = playlistAlbumIdentity(album)
    return identity.isNotBlank() && albums.any { it.identityKey == identity }
}

fun mergePlaylistItems(tracks: List<Track>, albums: List<PlaylistAlbumEntry>): List<PlaylistItem> {
    if (albums.isEmpty()) return tracks.map(PlaylistItem::Song)
    val anchored = albums.sortedBy { it.trackOffset.coerceIn(0, tracks.size) }
    val result = ArrayList<PlaylistItem>(tracks.size + albums.size)
    var albumCursor = 0
    for (index in 0..tracks.size) {
        while (albumCursor < anchored.size && anchored[albumCursor].trackOffset.coerceIn(0, tracks.size) == index) {
            result += PlaylistItem.Album(anchored[albumCursor])
            albumCursor++
        }
        if (index < tracks.size) result += PlaylistItem.Song(tracks[index])
    }
    return result
}

fun playlistAlbumIdentity(album: AlbumHit): String {
    album.browseId.trim().takeIf(String::isNotBlank)?.let { return "browse:${it.lowercase(Locale.ROOT)}" }
    album.audioPlaylistId.trim().takeIf(String::isNotBlank)?.let { return "list:${it.lowercase(Locale.ROOT)}" }
    album.canonicalUrl.trim().takeIf(String::isNotBlank)?.let { return "url:${it.lowercase(Locale.ROOT)}" }
    val title = album.title.trim().lowercase(Locale.ROOT)
    if (title.isBlank()) return ""
    return "meta:${album.artist.trim().lowercase(Locale.ROOT)}|$title|${album.year.trim()}"
}

data class PlaylistQueuePlan(
    val tracks: List<Track>,
    val startIndex: Int
)

fun PlaylistItem.playableTracks(): List<Track> = when (this) {
    is PlaylistItem.Song -> listOf(track)
    is PlaylistItem.Album -> entry.tracks
}

fun expandPlaylistQueue(
    items: List<PlaylistItem>,
    startKey: String? = null,
    identity: (Track) -> String
): PlaylistQueuePlan {
    val tracks = ArrayList<Track>()
    val indexByIdentity = HashMap<String, Int>()
    var startIndex = -1
    items.forEach { item ->
        var firstNewIndex = -1
        var firstExistingIndex = -1
        item.playableTracks().forEach { track ->
            if (track.title.isBlank()) return@forEach
            val key = identity(track)
            val existing = indexByIdentity[key]
            if (existing == null) {
                indexByIdentity[key] = tracks.size
                if (firstNewIndex < 0) firstNewIndex = tracks.size
                tracks += track
            } else if (firstExistingIndex < 0) {
                firstExistingIndex = existing
            }
        }
        if (startIndex < 0 && startKey != null && item.key == startKey) {
            startIndex = if (firstNewIndex >= 0) firstNewIndex else firstExistingIndex
        }
    }
    return PlaylistQueuePlan(tracks, if (tracks.isEmpty()) -1 else startIndex.coerceAtLeast(0))
}

data class PlaylistLayoutSlot(val entryId: String, val isAlbum: Boolean)

fun layoutAlbumsAroundTracks(
    previous: List<PlaylistLayoutSlot>,
    nextTrackEntryIds: List<String>
): List<PlaylistLayoutSlot> {
    val albums = previous.filter { it.isAlbum }
    if (albums.isEmpty()) return nextTrackEntryIds.map { PlaylistLayoutSlot(it, isAlbum = false) }
    val surviving = nextTrackEntryIds.toHashSet()
    val leading = ArrayList<PlaylistLayoutSlot>()
    val afterTrack = HashMap<String, MutableList<PlaylistLayoutSlot>>()
    var anchor: String? = null
    previous.forEach { slot ->
        if (slot.isAlbum) {
            val target = anchor
            if (target == null) leading += slot else afterTrack.getOrPut(target) { mutableListOf() } += slot
        } else if (slot.entryId in surviving) {
            anchor = slot.entryId
        }
    }
    val result = ArrayList<PlaylistLayoutSlot>(nextTrackEntryIds.size + albums.size)
    result += leading
    nextTrackEntryIds.forEach { entryId ->
        result += PlaylistLayoutSlot(entryId, isAlbum = false)
        afterTrack.remove(entryId)?.let(result::addAll)
    }
    return result
}
