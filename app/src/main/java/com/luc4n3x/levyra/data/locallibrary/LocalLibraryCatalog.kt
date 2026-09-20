package com.luc4n3x.levyra.data.locallibrary

import com.luc4n3x.levyra.data.local.LocalMediaEntity
import com.luc4n3x.levyra.domain.LibrarySort
import com.luc4n3x.levyra.domain.LibrarySortDirection
import com.luc4n3x.levyra.domain.Track

data class LocalLibraryEntry(
    val track: Track,
    val albumKey: String,
    val artistKey: String,
    val folderKey: String,
    val discNumber: Int,
    val trackNumber: Int,
    val dateAddedMs: Long,
    val isLevyraDownload: Boolean
)

data class LocalAlbumGroup(
    val key: String,
    val title: String,
    val artist: String,
    val year: String,
    val artworkModel: String,
    val durationMs: Long,
    val tracks: List<Track>
)

data class LocalArtistGroup(
    val key: String,
    val name: String,
    val artworkModel: String,
    val albumCount: Int,
    val tracks: List<Track>
)

data class LocalFolderGroup(
    val key: String,
    val name: String,
    val path: String,
    val volumeName: String,
    val durationMs: Long,
    val tracks: List<Track>
)

data class LocalLibraryCatalog(
    val songs: List<Track> = emptyList(),
    val albums: List<LocalAlbumGroup> = emptyList(),
    val artists: List<LocalArtistGroup> = emptyList(),
    val folders: List<LocalFolderGroup> = emptyList(),
    val mediaByUri: Map<String, LocalMediaEntity> = emptyMap(),
    val totalCount: Int = 0,
    val levyraDownloadCount: Int = 0,
    val hiddenDuplicateCount: Int = 0
)

fun LocalMediaEntity.toLocalTrack(): Track {
    val artwork = localArtworkModel(contentUri, albumId)
    val replayGain = parseLocalReplayGainTags(customTags)
    return Track(
        id = levyraTrackId.ifEmpty { LOCAL_MEDIA_TRACK_ID_PREFIX + identityKey },
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = contentUri,
        videoUrl = "",
        thumbnailUrl = artwork,
        largeThumbnailUrl = artwork,
        source = LOCAL_MEDIA_SOURCE,
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        replayGainTrackDb = replayGain.trackGainDb,
        replayGainAlbumDb = replayGain.albumGainDb,
        replayGainTrackPeak = replayGain.trackPeak,
        replayGainAlbumPeak = replayGain.albumPeak,
        year = year.takeIf { it > 0 }?.toString().orEmpty(),
        trackNumber = trackNumber,
        discNumber = discNumber
    )
}

fun buildLocalLibraryCatalog(
    rows: List<LocalMediaEntity>,
    sort: LibrarySort = LibrarySort.Title,
    direction: LibrarySortDirection = LibrarySortDirection.Ascending
): LocalLibraryCatalog {
    if (rows.isEmpty()) return LocalLibraryCatalog()
    val visible = preferredLocalCopies(rows)
    val entries = visible.map { row ->
        LocalLibraryEntry(
            track = row.toLocalTrack(),
            albumKey = row.albumKey,
            artistKey = row.artistKey,
            folderKey = row.folderKey,
            discNumber = row.discNumber,
            trackNumber = row.trackNumber,
            dateAddedMs = row.dateAddedMs,
            isLevyraDownload = row.isLevyraDownload
        )
    }
    val albumTitles = HashMap<String, String>()
    val albumArtists = HashMap<String, String>()
    val albumYears = HashMap<String, Int>()
    val artistNames = HashMap<String, String>()
    val folderNames = HashMap<String, String>()
    val folderPaths = HashMap<String, String>()
    val folderVolumes = HashMap<String, String>()
    for (row in visible) {
        if (row.album.isNotEmpty()) albumTitles.putIfAbsent(row.albumKey, row.album)
        val albumArtist = row.albumArtist.ifEmpty { row.artist }
        if (albumArtist.isNotEmpty()) albumArtists.putIfAbsent(row.albumKey, localPrimaryArtist(albumArtist))
        if (row.year > (albumYears[row.albumKey] ?: 0)) albumYears[row.albumKey] = row.year
        if (row.artist.isNotEmpty()) artistNames.putIfAbsent(row.artistKey, localPrimaryArtist(row.artist))
        folderNames.putIfAbsent(row.folderKey, row.folderName)
        folderPaths.putIfAbsent(row.folderKey, row.relativePath)
        folderVolumes.putIfAbsent(row.folderKey, row.volumeName)
    }

    val albums = entries.groupBy { it.albumKey }.map { (key, group) ->
        val ordered = group.sortedWith(albumTrackOrder).map { it.track }
        LocalAlbumGroup(
            key = key,
            title = albumTitles[key].orEmpty(),
            artist = albumArtists[key].orEmpty(),
            year = albumYears[key]?.takeIf { it > 0 }?.toString().orEmpty(),
            artworkModel = ordered.firstOrNull()?.thumbnailUrl.orEmpty(),
            durationMs = ordered.sumOf { it.durationMs.coerceAtLeast(0L) },
            tracks = ordered
        )
    }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

    val artists = entries.groupBy { it.artistKey }.map { (key, group) ->
        val ordered = group.sortedWith(titleOrder).map { it.track }
        LocalArtistGroup(
            key = key,
            name = artistNames[key].orEmpty(),
            artworkModel = ordered.firstOrNull()?.thumbnailUrl.orEmpty(),
            albumCount = group.mapTo(hashSetOf()) { it.albumKey }.size,
            tracks = ordered
        )
    }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    val folders = entries.groupBy { it.folderKey }.map { (key, group) ->
        val ordered = group.sortedWith(albumTrackOrder).map { it.track }
        LocalFolderGroup(
            key = key,
            name = folderNames[key].orEmpty(),
            path = folderPaths[key].orEmpty(),
            volumeName = folderVolumes[key].orEmpty(),
            durationMs = ordered.sumOf { it.durationMs.coerceAtLeast(0L) },
            tracks = ordered
        )
    }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.path })

    return LocalLibraryCatalog(
        songs = sortLocalSongs(entries, sort, direction),
        albums = albums,
        artists = artists,
        folders = folders,
        mediaByUri = visible.associateBy { it.contentUri },
        totalCount = entries.size,
        levyraDownloadCount = entries.count { it.isLevyraDownload },
        hiddenDuplicateCount = rows.size - visible.size
    )
}

internal fun sortLocalSongs(
    entries: List<LocalLibraryEntry>,
    sort: LibrarySort,
    direction: LibrarySortDirection
): List<Track> {
    val comparator = when (sort) {
        LibrarySort.Recent -> compareBy<LocalLibraryEntry> { it.dateAddedMs }.then(titleOrder)
        LibrarySort.Title -> titleOrder
        LibrarySort.Artist -> artistOrder.then(titleOrder)
        LibrarySort.Album -> albumNameOrder.then(albumTrackOrder)
        LibrarySort.Duration -> compareBy<LocalLibraryEntry> { it.track.durationMs }.then(titleOrder)
    }
    val ordered = entries.sortedWith(comparator)
    return (if (direction == LibrarySortDirection.Descending) ordered.asReversed() else ordered).map { it.track }
}

private val artistOrder: Comparator<LocalLibraryEntry> =
    compareBy(String.CASE_INSENSITIVE_ORDER) { entry: LocalLibraryEntry -> entry.track.artist }

private val albumNameOrder: Comparator<LocalLibraryEntry> =
    compareBy(String.CASE_INSENSITIVE_ORDER) { entry: LocalLibraryEntry -> entry.track.album }

private val titleOrder: Comparator<LocalLibraryEntry> =
    compareBy(String.CASE_INSENSITIVE_ORDER) { entry: LocalLibraryEntry -> entry.track.title }

private val albumTrackOrder: Comparator<LocalLibraryEntry> = compareBy<LocalLibraryEntry> { entry ->
    entry.discNumber.takeIf { it > 0 } ?: 1
}
    .thenBy { entry -> entry.trackNumber.takeIf { it > 0 } ?: Int.MAX_VALUE }
    .then(titleOrder)
