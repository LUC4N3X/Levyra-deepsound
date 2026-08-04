package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.primaryArtistSegment

internal fun deduplicateHomeAlbums(albums: List<AlbumHit>): List<AlbumHit> {
    if (albums.size < 2) return albums
    return albums.distinctBy(::homeAlbumDeduplicationKey)
}

internal fun homeAlbumDeduplicationKey(album: AlbumHit): String {
    val repositoryKey = albumRecommendationDeduplicationKey(album)
    val releaseTitle = repositoryKey
        .takeIf { it.startsWith(HOME_ALBUM_RELEASE_PREFIX) }
        ?.removePrefix(HOME_ALBUM_RELEASE_PREFIX)
        ?.substringBeforeLast('|')
        .orEmpty()
    val primaryArtist = albumRecommendationTextKey(primaryArtistSegment(album.artist))
    return if (releaseTitle.isNotBlank() && primaryArtist.isNotBlank()) {
        "$HOME_ALBUM_RELEASE_PREFIX$releaseTitle|$primaryArtist"
    } else {
        repositoryKey
    }
}

private const val HOME_ALBUM_RELEASE_PREFIX = "release:"
