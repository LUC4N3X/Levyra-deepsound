package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track

internal const val ExploreGenreRotationWindowMs = 3L * 24L * 60L * 60L * 1000L

internal data class ExploreGenreArtistCard(
    val key: String,
    val name: String,
    val artworkUrl: String,
    val track: Track
)

internal data class ExploreGenreAlbumCard(
    val key: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val track: Track
)

internal data class ExploreGenreEditorial(
    val featured: List<Track>,
    val artists: List<ExploreGenreArtistCard>,
    val albums: List<ExploreGenreAlbumCard>,
    val essentials: List<Track>
)

internal fun exploreGenreRotationBucket(nowMs: Long): Long =
    if (nowMs <= 0L) 0L else nowMs / ExploreGenreRotationWindowMs

internal fun buildExploreGenreEditorial(
    tracks: List<Track>,
    zoneId: String,
    rotationBucket: Long
): ExploreGenreEditorial {
    val uniqueTracks = tracks.distinctBy { it.id }
    if (uniqueTracks.isEmpty()) {
        return ExploreGenreEditorial(
            featured = emptyList(),
            artists = emptyList(),
            albums = emptyList(),
            essentials = emptyList()
        )
    }

    val seed = 31 * zoneId.hashCode() + rotationBucket.hashCode()
    val visualTracks = uniqueTracks.filter { track -> artworkUrl(track).isNotBlank() }
    val featured = rotateFromSeed(visualTracks, seed).take(8)
    val rotated = rotateFromSeed(uniqueTracks, seed xor 0x5A17)

    val artists = rotated.asSequence()
        .mapNotNull { track ->
            val name = track.artist.trim()
            val artwork = artworkUrl(track)
            if (name.isBlank() || artwork.isBlank()) return@mapNotNull null
            val browseId = track.artistBrowseIds.firstOrNull().orEmpty().trim()
            ExploreGenreArtistCard(
                key = browseId.ifBlank { name.lowercase() },
                name = name,
                artworkUrl = artwork,
                track = track
            )
        }
        .distinctBy { it.key }
        .take(10)
        .toList()

    val albums = rotated.asSequence()
        .mapNotNull { track ->
            val title = track.album.trim()
            val artwork = artworkUrl(track)
            if (title.isBlank() || artwork.isBlank()) return@mapNotNull null
            ExploreGenreAlbumCard(
                key = track.albumBrowseId.trim().ifBlank { "${track.artist.trim().lowercase()}|${title.lowercase()}" },
                title = title,
                artist = track.artist.trim(),
                artworkUrl = artwork,
                track = track
            )
        }
        .distinctBy { it.key }
        .take(10)
        .toList()

    return ExploreGenreEditorial(
        featured = featured,
        artists = artists,
        albums = albums,
        essentials = uniqueTracks.take(18)
    )
}

private fun artworkUrl(track: Track): String =
    track.largeThumbnailUrl.trim().ifBlank { track.thumbnailUrl.trim() }

private fun <T> rotateFromSeed(items: List<T>, seed: Int): List<T> {
    if (items.size < 2) return items
    val offset = Math.floorMod(seed, items.size)
    if (offset == 0) return items
    return items.drop(offset) + items.take(offset)
}
