package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreGenreEditorialTest {
    @Test
    fun editorialDeduplicatesTracksArtistsAndAlbums() {
        val tracks = listOf(
            track("1", "Artist A", "Album A", "artist-a", "album-a"),
            track("1", "Artist A", "Album A", "artist-a", "album-a"),
            track("2", "Artist A", "Album A", "artist-a", "album-a"),
            track("3", "Artist B", "Album B", "artist-b", "album-b")
        )

        val editorial = buildExploreGenreEditorial(tracks, "rap-drill", 4L)

        assertEquals(listOf("1", "2", "3"), editorial.essentials.map { it.id })
        assertEquals(2, editorial.artists.size)
        assertEquals(2, editorial.albums.size)
        assertEquals(editorial.artists.map { it.key }.toSet().size, editorial.artists.size)
        assertEquals(editorial.albums.map { it.key }.toSet().size, editorial.albums.size)
    }

    @Test
    fun rotationIsStableWithinTheSameWindow() {
        val tracks = (1..8).map { index ->
            track(
                id = index.toString(),
                artist = "Artist $index",
                album = "Album $index",
                artistBrowseId = "artist-$index",
                albumBrowseId = "album-$index"
            )
        }

        val first = buildExploreGenreEditorial(tracks, "rap-drill", 12L)
        val second = buildExploreGenreEditorial(tracks, "rap-drill", 12L)

        assertEquals(first.featured.map { it.id }, second.featured.map { it.id })
        assertEquals(first.artists.map { it.key }, second.artists.map { it.key })
    }

    @Test
    fun laterRotationWindowsCanSurfaceDifferentArtists() {
        val tracks = (1..8).map { index ->
            track(
                id = index.toString(),
                artist = "Artist $index",
                album = "Album $index",
                artistBrowseId = "artist-$index",
                albumBrowseId = "album-$index"
            )
        }

        val first = buildExploreGenreEditorial(tracks, "rap-drill", 1L)
        val later = buildExploreGenreEditorial(tracks, "rap-drill", 2L)

        assertNotEquals(first.artists.map { it.key }, later.artists.map { it.key })
        assertEquals(first.artists.map { it.key }.toSet(), later.artists.map { it.key }.toSet())
    }

    @Test
    fun editorialOmitsBlankArtistAlbumAndArtworkCards() {
        val tracks = listOf(
            track("1", "", "Album A", "", "album-a"),
            track("2", "Artist B", "", "artist-b", ""),
            track("3", "Artist C", "Album C", "artist-c", "album-c", artwork = "")
        )

        val editorial = buildExploreGenreEditorial(tracks, "pop-global", 3L)

        assertTrue(editorial.featured.size <= 2)
        assertEquals(1, editorial.artists.size)
        assertEquals(1, editorial.albums.size)
    }

    @Test
    fun rotationBucketsChangeOnlyEveryThreeDays() {
        assertEquals(0L, exploreGenreRotationBucket(0L))
        assertEquals(0L, exploreGenreRotationBucket(ExploreGenreRotationWindowMs - 1L))
        assertEquals(1L, exploreGenreRotationBucket(ExploreGenreRotationWindowMs))
    }

    private fun track(
        id: String,
        artist: String,
        album: String,
        artistBrowseId: String,
        albumBrowseId: String,
        artwork: String = "https://lh3.googleusercontent.com/$id=w544-h544"
    ): Track = Track(
        id = id,
        title = "Track $id",
        artist = artist,
        album = album,
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = artwork,
        largeThumbnailUrl = artwork,
        source = "test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 0,
        accentStart = 0xFF202020.toInt(),
        accentEnd = 0xFF404040.toInt(),
        albumBrowseId = albumBrowseId,
        artistBrowseIds = listOfNotNull(artistBrowseId.takeIf(String::isNotBlank))
    )
}
