package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.Track

internal const val HOME_ALBUM_SHELF_TARGET_SIZE = 14

private val HOME_ALBUM_EXPLICIT_CREDIT_SEPARATOR =
    Regex("""(?i)\b(?:feat(?:uring)?\.?|ft\.?|with|con|vs\.?)\b|\s+[x×/]\s+""")
private val HOME_ALBUM_AMBIGUOUS_ARTIST_SEPARATOR =
    Regex(""",|;|\s+(?:&|and|e|y|et|und)\s+""", RegexOption.IGNORE_CASE)

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
        .filter(::hasLikelyAlbumArtwork)
        .mapNotNull(::homeAlbumHitFromTrack)

    val relaxedDerived = prioritizedTracks
        .asSequence()
        .mapNotNull(::homeAlbumHitFromTrack)

    return (primaryAlbums.asSequence() + strictDerived + relaxedDerived)
        .filter(::isCanonicalHomeAlbumHit)
        .distinctBy(::albumRecommendationDeduplicationKey)
        .take(HOME_ALBUM_SHELF_TARGET_SIZE)
        .toList()
}

internal fun isCanonicalHomeAlbumHit(album: AlbumHit): Boolean {
    val title = album.title.trim()
    val artist = album.artist.trim()
    if (title.isBlank() || artist.isBlank() || album.thumbnailUrl.isBlank()) return false
    if (!isPlausibleYoutubeMusicAlbumTitle(title)) return false
    if (album.releaseType != ReleaseType.Unknown && album.releaseType != ReleaseType.Album) return false
    val hasCanonicalIdentity =
        album.browseId.isNotBlank() ||
            album.audioPlaylistId.isNotBlank() ||
            album.upc.isNotBlank() ||
            album.canonicalUrl.isNotBlank()
    if (!hasCanonicalIdentity) return false
    if (album.artistBrowseId.isBlank() && hasAmbiguousHomeAlbumArtistCredit(artist)) return false
    return true
}

internal fun homeAlbumHitFromTrack(track: Track): AlbumHit? {
    if (!isUsableHomeAlbumTrack(track)) return null
    val album = track.album.trim()
    val artist = homeAlbumArtistFromTrack(track) ?: return null
    return AlbumHit(
        title = album,
        artist = artist,
        year = track.year.ifBlank {
            track.releaseDate.take(4).takeIf { value -> value.toIntOrNull() != null }.orEmpty()
        },
        thumbnailUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
        query = listOf(album, artist, "album").filter(String::isNotBlank).joinToString(" "),
        browseId = track.albumBrowseId.trim(),
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

internal fun homeAlbumArtistFromTrack(track: Track): String? {
    val rawArtist = track.artist.trim()
    if (rawArtist.isBlank()) return null
    val artistBrowseIds = track.artistBrowseIds
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { it.lowercase() }
    if (artistBrowseIds.size != 1 || hasAmbiguousHomeAlbumArtistCredit(rawArtist)) return null
    return rawArtist
}

internal fun hasAmbiguousHomeAlbumArtistCredit(value: String): Boolean =
    HOME_ALBUM_EXPLICIT_CREDIT_SEPARATOR.containsMatchIn(value) ||
        HOME_ALBUM_AMBIGUOUS_ARTIST_SEPARATOR.containsMatchIn(value)

private fun isUsableHomeAlbumTrack(track: Track): Boolean {
    val album = track.album.trim()
    if (album.isBlank() || track.artist.isBlank()) return false
    val albumBrowseId = track.albumBrowseId.trim()
    if (!albumBrowseId.startsWith("MPRE", ignoreCase = true)) return false
    if (albumRecommendationTextKey(album) == albumRecommendationTextKey(track.title)) return false
    val albumKey = albumRecommendationTextKey(album)
    if (
        albumKey == "ep" ||
        albumKey.endsWith(" ep") ||
        albumKey.contains(" ep ") ||
        albumKey == "single" ||
        albumKey == "singolo" ||
        albumKey.endsWith(" single") ||
        albumKey.endsWith(" singolo")
    ) {
        return false
    }
    if (album.equals("YouTube Music", ignoreCase = true) || album.equals("YouTube", ignoreCase = true)) return false
    if (track.artist.equals("YouTube Music", ignoreCase = true) || track.artist.equals("YouTube", ignoreCase = true)) return false
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
