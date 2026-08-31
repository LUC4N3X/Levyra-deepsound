package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.Track

internal const val HOME_ALBUM_SHELF_TARGET_SIZE = 14

internal fun buildPersonalizedHomeAlbumShelf(
    primaryAlbums: List<AlbumHit>,
    personalTracks: List<Track>,
    recentTracks: List<Track>,
    favoriteTracks: List<Track>,
    quickPickTracks: List<Track>,
    localizedReleaseTracks: List<Track>,
    localizedSections: List<HomeSection>,
    chartTracks: List<Track>,
    fallbackTracks: List<Track>
): List<AlbumHit> {
    val prioritizedTracks = buildList {
        addAll(personalTracks)
        addAll(recentTracks)
        addAll(favoriteTracks)
        addAll(quickPickTracks)
        addAll(localizedReleaseTracks)
        localizedSections.forEach { section -> addAll(section.tracks) }
        addAll(chartTracks)
        addAll(fallbackTracks)
    }
        .distinctBy { track -> track.id.ifBlank { "${track.title}|${track.artist}|${track.album}" } }

    val strictDerived = prioritizedTracks
        .asSequence()
        .filter(::isUsableAlbumTrack)
        .filter(::hasLikelyAlbumArtwork)
        .map(::trackToHomeAlbumHit)

    val relaxedDerived = prioritizedTracks
        .asSequence()
        .filter(::isUsableAlbumTrack)
        .map(::trackToHomeAlbumHit)

    return (primaryAlbums.asSequence() + strictDerived + relaxedDerived)
        .filter(::isUsableHomeAlbum)
        .distinctBy(::albumRecommendationDeduplicationKey)
        .take(HOME_ALBUM_SHELF_TARGET_SIZE)
        .toList()
}

private fun isUsableHomeAlbum(album: AlbumHit): Boolean =
    album.title.isNotBlank() &&
        album.artist.isNotBlank() &&
        album.thumbnailUrl.isNotBlank() &&
        (album.browseId.isNotBlank() || album.query.isNotBlank())

private fun isUsableAlbumTrack(track: Track): Boolean {
    val album = track.album.trim()
    val artist = track.artist.trim()
    if (album.isBlank() || artist.isBlank()) return false
    if (album.equals(track.title.trim(), ignoreCase = true)) return false
    if (album.equals("YouTube Music", ignoreCase = true) || album.equals("YouTube", ignoreCase = true)) return false
    if (artist.equals("YouTube Music", ignoreCase = true) || artist.equals("YouTube", ignoreCase = true)) return false
    return track.largeThumbnailUrl.isNotBlank() || track.thumbnailUrl.isNotBlank()
}

private fun hasLikelyAlbumArtwork(track: Track): Boolean {
    val url = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.lowercase()
    if (url.isBlank()) return false
    return !url.contains("/vi/") &&
        !url.contains("/vi_webp/") &&
        !url.contains("hqdefault") &&
        !url.contains("mqdefault") &&
        !url.contains("sddefault") &&
        !url.contains("maxresdefault")
}

private fun trackToHomeAlbumHit(track: Track): AlbumHit {
    val album = track.album.trim()
    val artist = track.artist.trim()
    return AlbumHit(
        title = album,
        artist = artist,
        year = track.year.ifBlank {
            track.releaseDate.take(4).takeIf { value -> value.toIntOrNull() != null }.orEmpty()
        },
        thumbnailUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
        query = listOf(album, artist, "album").filter(String::isNotBlank).joinToString(" "),
        browseId = track.albumBrowseId,
        artistBrowseId = track.artistBrowseIds.firstOrNull().orEmpty(),
        explicit = track.explicit,
        releaseDate = track.releaseDate,
        upc = track.upc,
        canonicalUrl = track.canonicalAlbumUrl,
        metadataProvider = track.metadataProvider,
        metadataConfidence = track.metadataConfidence,
        releaseType = ReleaseType.Album
    )
}
