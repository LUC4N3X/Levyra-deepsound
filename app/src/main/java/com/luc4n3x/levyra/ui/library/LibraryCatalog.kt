package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.FollowedArtist
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import java.text.Normalizer
import java.util.Locale

internal enum class LibraryCategory {
    Overview,
    Playlists,
    Albums,
    Artists,
    Songs,
    Offline
}

internal enum class LibraryLayout {
    List,
    Grid
}

internal enum class LibrarySort {
    Recent,
    Title,
    Artist,
    Album,
    Duration
}

internal data class LibraryAlbum(
    val key: String,
    val title: String,
    val artist: String,
    val year: String,
    val artworkUrl: String,
    val browseId: String,
    val explicit: Boolean,
    val tracks: List<Track>
) {
    val durationMs: Long = tracks.sumOf { it.durationMs.coerceAtLeast(0L) }
}

internal data class LibraryArtist(
    val key: String,
    val name: String,
    val browseId: String,
    val artworkUrl: String,
    val followedAt: Long,
    val tracks: List<Track>
)

internal data class LibraryOfflineItem(
    val key: String,
    val track: Track,
    val download: DownloadedTrack
)

internal data class LibraryCatalog(
    val tracks: List<Track>,
    val albums: List<LibraryAlbum>,
    val artists: List<LibraryArtist>,
    val mostPlayed: List<Track>,
    val recent: List<Track>,
    val offlineItems: List<LibraryOfflineItem>
) {
    val offlineTracks: List<Track>
        get() = offlineItems.map { it.track }
}

internal fun buildLibraryCatalog(
    favorites: List<Track>,
    playlists: List<Playlist>,
    downloads: List<DownloadedTrack>,
    recentListens: List<Track>,
    followedArtists: List<FollowedArtist>
): LibraryCatalog {
    val knownTracks = sequenceOf(
        favorites.asSequence(),
        playlists.asSequence().flatMap { it.tracks.asSequence() },
        recentListens.asSequence()
    )
        .flatten()
        .filter(::isUsableLibraryTrack)
        .distinctBy(::libraryTrackKey)
        .toList()

    val knownById = knownTracks
        .filter { it.id.isNotBlank() }
        .associateBy { it.id }

    val offlineItems = downloads.map { download ->
        val known = knownById[download.trackId] ?: knownTracks.firstOrNull { track ->
            libraryDownloadForTrack(track, listOf(download)) != null
        }
        LibraryOfflineItem(
            key = "download:${download.id}",
            track = known?.copy(streamUrl = "") ?: download.toLibraryTrack(),
            download = download
        )
    }

    val allTracks = (favorites + offlineItems.map { it.track } + playlists.flatMap { it.tracks })
        .filter(::isUsableLibraryTrack)
        .distinctBy(::libraryTrackKey)

    val albums = allTracks
        .filter { it.album.trim().isNotBlank() && !isGenericAlbumName(it.album) }
        .groupBy(::libraryAlbumKey)
        .map { (key, groupedTracks) ->
            val representative = groupedTracks.maxByOrNull(::libraryMetadataScore) ?: groupedTracks.first()
            LibraryAlbum(
                key = key,
                title = representative.album.trim(),
                artist = representative.artist.trim(),
                year = representative.year.ifBlank { representative.releaseDate.take(4) },
                artworkUrl = representative.largeThumbnailUrl.ifBlank { representative.thumbnailUrl },
                browseId = representative.albumBrowseId,
                explicit = groupedTracks.any { it.explicit },
                tracks = groupedTracks.sortedWith(
                    compareBy<Track> { it.discNumber.takeIf { value -> value > 0 } ?: Int.MAX_VALUE }
                        .thenBy { it.trackNumber.takeIf { value -> value > 0 } ?: Int.MAX_VALUE }
                        .thenBy { it.title.lowercase(Locale.ROOT) }
                )
            )
        }
        .sortedBy { normalizeLibraryText(it.title) }

    val tracksByArtist = allTracks
        .flatMap { track ->
            val artistNames = splitLibraryArtists(track.artist)
            artistNames.mapIndexed { index, artistName ->
                LibraryArtistMembership(
                    name = artistName,
                    browseId = track.artistBrowseIds.getOrNull(index).orEmpty().trim().ifBlank {
                        if (artistNames.size == 1) track.artistBrowseIds.firstOrNull().orEmpty().trim() else ""
                    },
                    track = track
                )
            }
        }
        .groupBy { normalizeLibraryText(it.name) }

    val followedByName = followedArtists.associateBy { normalizeLibraryText(it.name) }
    val artistKeys = linkedSetOf<String>().apply {
        addAll(followedArtists.map { normalizeLibraryText(it.name) }.filter(String::isNotBlank))
        addAll(tracksByArtist.keys.filter(String::isNotBlank))
    }
    val artists = artistKeys.mapNotNull { key ->
        val memberships = tracksByArtist[key].orEmpty()
        val tracks = memberships.map { it.track }.distinctBy(::libraryTrackKey)
        val followed = followedByName[key]
        val representative = tracks.maxByOrNull(::libraryMetadataScore)
        val groupedName = memberships.firstOrNull()?.name.orEmpty().trim()
        val name = followed?.name?.trim().orEmpty().ifBlank { groupedName }
        if (name.isBlank()) return@mapNotNull null
        val browseId = followed?.browseId.orEmpty().trim().ifBlank {
            memberships.firstNotNullOfOrNull { membership ->
                membership.browseId.takeIf(String::isNotBlank)
            }.orEmpty()
        }
        LibraryArtist(
            key = browseId.takeIf(String::isNotBlank)?.let { "browse:$it" } ?: "artist:$key",
            name = name,
            browseId = browseId,
            artworkUrl = followed?.thumbnailUrl.orEmpty().ifBlank {
                representative?.largeThumbnailUrl?.ifBlank { representative.thumbnailUrl }.orEmpty()
            },
            followedAt = followed?.followedAt ?: 0L,
            tracks = tracks
        )
    }.sortedWith(
        compareByDescending<LibraryArtist> { it.followedAt }
            .thenBy { normalizeLibraryText(it.name) }
    )

    val playCountByKey = recentListens.groupingBy(::libraryTrackKey).eachCount()
    val mostPlayed = recentListens
        .filter(::isUsableLibraryTrack)
        .distinctBy(::libraryTrackKey)
        .sortedWith(
            compareByDescending<Track> { playCountByKey[libraryTrackKey(it)] ?: 0 }
                .thenBy { normalizeLibraryText(it.title) }
        )

    return LibraryCatalog(
        tracks = allTracks,
        albums = albums,
        artists = artists,
        mostPlayed = mostPlayed,
        recent = recentListens.filter(::isUsableLibraryTrack).distinctBy(::libraryTrackKey),
        offlineItems = offlineItems
    )
}

internal fun filterLibraryTracks(
    tracks: List<Track>,
    query: String,
    sort: LibrarySort
): List<Track> {
    val normalizedQuery = normalizeLibraryText(query)
    val filtered = if (normalizedQuery.isBlank()) {
        tracks
    } else {
        tracks.filter { track ->
            sequenceOf(track.title, track.artist, track.album, track.year)
                .map(::normalizeLibraryText)
                .any { it.contains(normalizedQuery) }
        }
    }
    return when (sort) {
        LibrarySort.Recent -> filtered
        LibrarySort.Title -> filtered.sortedBy { normalizeLibraryText(it.title) }
        LibrarySort.Artist -> filtered.sortedWith(
            compareBy<Track> { normalizeLibraryText(it.artist) }
                .thenBy { normalizeLibraryText(it.title) }
        )
        LibrarySort.Album -> filtered.sortedWith(
            compareBy<Track> { normalizeLibraryText(it.album) }
                .thenBy { it.discNumber }
                .thenBy { it.trackNumber }
                .thenBy { normalizeLibraryText(it.title) }
        )
        LibrarySort.Duration -> filtered.sortedByDescending { it.durationMs }
    }
}

internal fun filterLibraryPlaylists(
    playlists: List<Playlist>,
    query: String,
    sort: LibrarySort
): List<Playlist> {
    val normalizedQuery = normalizeLibraryText(query)
    val filtered = playlists.filter { playlist ->
        normalizedQuery.isBlank() || normalizeLibraryText(playlist.name).contains(normalizedQuery) ||
            playlist.tracks.any { track ->
                normalizeLibraryText(track.title).contains(normalizedQuery) ||
                    normalizeLibraryText(track.artist).contains(normalizedQuery)
            }
    }
    return when (sort) {
        LibrarySort.Recent -> filtered.sortedByDescending { it.updatedAt }
        LibrarySort.Title, LibrarySort.Artist, LibrarySort.Album -> filtered.sortedBy { normalizeLibraryText(it.name) }
        LibrarySort.Duration -> filtered.sortedByDescending { playlist -> playlist.tracks.sumOf { it.durationMs } }
    }
}

internal fun filterLibraryAlbums(
    albums: List<LibraryAlbum>,
    query: String,
    sort: LibrarySort
): List<LibraryAlbum> {
    val normalizedQuery = normalizeLibraryText(query)
    val filtered = albums.filter { album ->
        normalizedQuery.isBlank() || sequenceOf(album.title, album.artist, album.year)
            .map(::normalizeLibraryText)
            .any { it.contains(normalizedQuery) }
    }
    return when (sort) {
        LibrarySort.Recent -> filtered.sortedWith(
            compareByDescending<LibraryAlbum> { it.year.toIntOrNull() ?: 0 }
                .thenBy { normalizeLibraryText(it.title) }
        )
        LibrarySort.Title -> filtered.sortedBy { normalizeLibraryText(it.title) }
        LibrarySort.Artist -> filtered.sortedWith(
            compareBy<LibraryAlbum> { normalizeLibraryText(it.artist) }
                .thenBy { normalizeLibraryText(it.title) }
        )
        LibrarySort.Album -> filtered.sortedBy { normalizeLibraryText(it.title) }
        LibrarySort.Duration -> filtered.sortedByDescending { it.durationMs }
    }
}

internal fun filterLibraryArtists(
    artists: List<LibraryArtist>,
    query: String,
    sort: LibrarySort
): List<LibraryArtist> {
    val normalizedQuery = normalizeLibraryText(query)
    val filtered = artists.filter { artist ->
        normalizedQuery.isBlank() || normalizeLibraryText(artist.name).contains(normalizedQuery) ||
            artist.tracks.any { normalizeLibraryText(it.title).contains(normalizedQuery) }
    }
    return when (sort) {
        LibrarySort.Recent -> filtered.sortedWith(
            compareByDescending<LibraryArtist> { it.followedAt }
                .thenBy { normalizeLibraryText(it.name) }
        )
        LibrarySort.Duration -> filtered.sortedByDescending { artist -> artist.tracks.sumOf { it.durationMs } }
        else -> filtered.sortedBy { normalizeLibraryText(it.name) }
    }
}

internal fun filterLibraryOfflineItems(
    items: List<LibraryOfflineItem>,
    query: String,
    sort: LibrarySort
): List<LibraryOfflineItem> {
    val normalizedQuery = normalizeLibraryText(query)
    val filtered = items.filter { item ->
        normalizedQuery.isBlank() || sequenceOf(
            item.track.title,
            item.track.artist,
            item.track.album,
            item.download.fileName,
            item.download.mimeType
        ).map(::normalizeLibraryText).any { it.contains(normalizedQuery) }
    }
    return when (sort) {
        LibrarySort.Recent -> filtered.sortedByDescending { it.download.savedAt }
        LibrarySort.Title -> filtered.sortedBy { normalizeLibraryText(it.track.title) }
        LibrarySort.Artist -> filtered.sortedWith(
            compareBy<LibraryOfflineItem> { normalizeLibraryText(it.track.artist) }
                .thenBy { normalizeLibraryText(it.track.title) }
        )
        LibrarySort.Album -> filtered.sortedWith(
            compareBy<LibraryOfflineItem> { normalizeLibraryText(it.track.album) }
                .thenBy { normalizeLibraryText(it.track.title) }
        )
        LibrarySort.Duration -> filtered.sortedByDescending { it.track.durationMs }
    }
}

internal fun libraryTrackKey(track: Track): String {
    track.isrc.trim().lowercase(Locale.ROOT).takeIf(String::isNotBlank)?.let { return "isrc:$it" }
    track.id.trim().takeIf(String::isNotBlank)?.let { return "id:$it" }
    val videoId = sequenceOf(track.counterpartVideoId, track.videoUrl)
        .map(String::trim)
        .firstOrNull(String::isNotBlank)
        .orEmpty()
    if (videoId.isNotBlank()) return "video:$videoId"
    val durationBucket = if (track.durationMs > 0L) track.durationMs / 1_000L else 0L
    return listOf(
        normalizeLibraryText(track.title),
        normalizeLibraryText(track.artist),
        normalizeLibraryText(track.album),
        durationBucket.toString(),
        track.explicit.toString(),
        track.trackNumber.toString(),
        track.discNumber.toString()
    ).joinToString("|")
}

internal fun playlistEntryKey(track: Track): String {
    return track.id.trim().takeIf(String::isNotBlank)?.let { "track-id:$it" }
        ?: "track-fallback:${libraryTrackKey(track)}"
}

internal fun libraryDownloadForTrack(
    track: Track,
    downloads: List<DownloadedTrack>
): DownloadedTrack? {
    if (track.id.isNotBlank()) {
        downloads.firstOrNull { it.trackId == track.id }?.let { return it }
    }
    val title = normalizeLibraryText(track.title)
    val artist = normalizeLibraryText(track.artist)
    return downloads.firstOrNull { download ->
        val sameAlbum = track.album.isBlank() || download.album.isBlank() ||
            normalizeLibraryText(download.album) == normalizeLibraryText(track.album)
        val sameDuration = track.durationMs <= 0L || download.durationMs <= 0L ||
            kotlin.math.abs(download.durationMs - track.durationMs) <= 2_000L
        normalizeLibraryText(download.title) == title &&
            normalizeLibraryText(download.artist) == artist &&
            sameAlbum &&
            sameDuration
    }
}

internal fun normalizeLibraryText(value: String): String {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private data class LibraryArtistMembership(
    val name: String,
    val browseId: String,
    val track: Track
)

private fun libraryAlbumKey(track: Track): String {
    track.albumBrowseId.trim().takeIf(String::isNotBlank)?.let { return "browse:$it" }
    track.upc.trim().lowercase(Locale.ROOT).takeIf(String::isNotBlank)?.let { return "upc:$it" }
    return "album:${normalizeLibraryText(track.artist)}|${normalizeLibraryText(track.album)}"
}

private fun isUsableLibraryTrack(track: Track): Boolean {
    return track.title.isNotBlank() && (track.artist.isNotBlank() || track.id.isNotBlank() || track.videoUrl.isNotBlank())
}

private fun isGenericAlbumName(value: String): Boolean {
    return normalizeLibraryText(value) in setOf("single", "singolo", "unknown", "sconosciuto", "music", "musica")
}

private fun libraryMetadataScore(track: Track): Int {
    return listOf(
        track.largeThumbnailUrl,
        track.thumbnailUrl,
        track.albumBrowseId,
        track.upc,
        track.year,
        track.releaseDate
    ).count(String::isNotBlank) * 10 + track.metadataConfidence
}

private fun splitLibraryArtists(value: String): List<String> {
    return value
        .split(Regex("\\s+(?:feat\\.?|ft\\.?|x|×)\\s+|\\s*;\\s*|\\s+·\\s+", RegexOption.IGNORE_CASE))
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy(::normalizeLibraryText)
        .ifEmpty { listOf(value.trim()).filter(String::isNotBlank) }
}

private fun DownloadedTrack.toLibraryTrack(): Track = Track(
    id = trackId.ifBlank { "offline-$id" },
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    streamUrl = "",
    videoUrl = "",
    thumbnailUrl = "",
    largeThumbnailUrl = "",
    source = "Offline",
    moodTags = emptySet(),
    energy = 0,
    vocal = 0,
    replayScore = 0,
    cacheScore = 0,
    accentStart = 0,
    accentEnd = 0
)
