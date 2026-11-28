package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ReleaseRadarEntry

data class AppleNewThisWeek(
    val key: String = "",
    val albums: List<com.luc4n3x.levyra.domain.AlbumHit> = emptyList(),
    val loading: Boolean = false
)

internal fun releaseRadarAlbumHit(entry: ReleaseRadarEntry): AlbumHit? {
    val release = entry.release
    val browseId = release.browseId.trim()
    if (browseId.isBlank()) return null
    val artist = entry.artistName.trim().ifBlank { release.subtitle.trim() }
    return AlbumHit(
        title = release.title,
        artist = artist,
        year = release.year,
        thumbnailUrl = release.thumbnailUrl,
        query = listOf(artist, release.title).filter(String::isNotBlank).joinToString(" "),
        browseId = browseId,
        artistBrowseId = entry.artistBrowseId.trim(),
        explicit = release.explicit
    )
}

internal fun releaseRadarSearchQuery(entry: ReleaseRadarEntry): String =
    listOf(entry.release.title, entry.artistName).map(String::trim).filter(String::isNotBlank).joinToString(" ")

internal fun prioritizeNewReleasesForUser(
    releases: List<AlbumHit>,
    preferredArtists: List<String>,
    limit: Int
): List<AlbumHit> {
    if (limit <= 0) return emptyList()
    val preferences = preferredArtists
        .map { artist -> artist.trim().lowercase(java.util.Locale.ROOT) }
        .filter(String::isNotBlank)
        .distinct()
    if (preferences.isEmpty()) return releases.take(limit)
    val (matched, remaining) = releases.partition { release ->
        val artist = release.artist.trim().lowercase(java.util.Locale.ROOT)
        preferences.any { preferred ->
            artist == preferred || artist.contains(preferred) || preferred.contains(artist)
        }
    }
    return (matched + remaining)
        .distinctBy { release ->
            release.browseId.ifBlank {
                "${release.artist.lowercase(java.util.Locale.ROOT)}|${release.title.lowercase(java.util.Locale.ROOT)}"
            }
        }
        .take(limit)
}
