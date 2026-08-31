package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAlbumShelfEngineTest {
    @Test
    fun fillsSparseRepositoryAlbumsFromPersonalAndLocalizedSignals() {
        val primary = listOf(
            album("Primary One", "Artist A"),
            album("Primary Two", "Artist B")
        )
        val personal = (1..4).map { index -> track("personal-$index", "Personal Album $index", "Personal Artist $index") }
        val localized = (1..6).map { index -> track("local-$index", "Local Album $index", "Local Artist $index") }

        val result = buildPersonalizedHomeAlbumShelf(
            primaryAlbums = primary,
            personalTracks = personal,
            recentTracks = emptyList(),
            favoriteTracks = emptyList(),
            quickPickTracks = emptyList(),
            localizedReleaseTracks = emptyList(),
            localizedSections = listOf(HomeSection("Localized", localized)),
            chartTracks = emptyList(),
            fallbackTracks = emptyList()
        )

        assertTrue(result.size >= HOME_ALBUM_SHELF_MIN_SIZE)
        assertEquals("Primary One", result[0].title)
        assertEquals("Primary Two", result[1].title)
        assertEquals("Personal Album 1", result[2].title)
    }

    @Test
    fun deduplicatesTheSameVisibleReleaseAcrossSignals() {
        val duplicatePersonal = track("p1", "Shared Album", "Shared Artist")
        val duplicateLocalized = track("l1", "Shared Album", "Shared Artist")
        val uniqueFallbacks = (1..12).map { index -> track("f-$index", "Fallback $index", "Artist $index") }

        val result = buildPersonalizedHomeAlbumShelf(
            primaryAlbums = listOf(album("Shared Album", "Shared Artist")),
            personalTracks = listOf(duplicatePersonal),
            recentTracks = emptyList(),
            favoriteTracks = emptyList(),
            quickPickTracks = emptyList(),
            localizedReleaseTracks = listOf(duplicateLocalized),
            localizedSections = emptyList(),
            chartTracks = emptyList(),
            fallbackTracks = uniqueFallbacks
        )

        assertEquals(1, result.count { it.title == "Shared Album" && it.artist == "Shared Artist" })
        assertTrue(result.size >= HOME_ALBUM_SHELF_MIN_SIZE)
    }

    private fun album(title: String, artist: String): AlbumHit = AlbumHit(
        title = title,
        artist = artist,
        year = "2026",
        thumbnailUrl = "https://lh3.googleusercontent.com/$title=w544-h544",
        query = "$title $artist album",
        browseId = "MPREb_${title.replace(' ', '_')}"
    )

    private fun track(id: String, album: String, artist: String): Track = Track(
        id = id,
        title = "$album Song",
        artist = artist,
        album = album,
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "https://lh3.googleusercontent.com/$id=w544-h544",
        largeThumbnailUrl = "https://lh3.googleusercontent.com/$id=w1200-h1200",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0xFF3366FF.toInt(),
        accentEnd = 0xFF6633FF.toInt(),
        albumBrowseId = "MPREb_$id",
        artistBrowseIds = listOf("UC_$id")
    )
}
