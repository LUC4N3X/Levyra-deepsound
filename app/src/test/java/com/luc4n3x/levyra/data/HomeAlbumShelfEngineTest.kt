package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAlbumShelfEngineTest {
    private val expectedMinimumShelfSize = 10

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

        assertTrue(result.size >= expectedMinimumShelfSize)
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
        assertTrue(result.size >= expectedMinimumShelfSize)
    }

    @Test
    fun derivedAlbumUsesLeadArtistInsteadOfTrackFeaturingCredit() {
        val result = buildPersonalizedHomeAlbumShelf(
            primaryAlbums = emptyList(),
            personalTracks = listOf(
                track(
                    id = "vangelo",
                    album = "Vangelo",
                    artist = "Shiva, Geolier",
                    artistBrowseIds = listOf("UC_SHIVA", "UC_GEOLIER")
                )
            ),
            recentTracks = emptyList(),
            favoriteTracks = emptyList(),
            quickPickTracks = emptyList(),
            localizedReleaseTracks = emptyList(),
            localizedSections = emptyList(),
            chartTracks = emptyList(),
            fallbackTracks = emptyList()
        )

        assertEquals("Vangelo", result.single().title)
        assertEquals("Shiva", result.single().artist)
        assertEquals("UC_SHIVA", result.single().artistBrowseId)
    }

    @Test
    fun canonicalPrimaryAlbumDropsTrackLevelFeaturingFromArtistLabel() {
        val result = buildPersonalizedHomeAlbumShelf(
            primaryAlbums = listOf(
                album("Vangelo", "Shiva, Geolier").copy(artistBrowseId = "UC_SHIVA")
            ),
            personalTracks = emptyList(),
            recentTracks = emptyList(),
            favoriteTracks = emptyList(),
            quickPickTracks = emptyList(),
            localizedReleaseTracks = emptyList(),
            localizedSections = emptyList(),
            chartTracks = emptyList(),
            fallbackTracks = emptyList()
        )

        assertEquals("Shiva", result.single().artist)
        assertEquals("Vangelo Shiva album", result.single().query)
    }

    @Test
    fun trackTitleCannotMasqueradeAsAlbumAfterCreditNormalization() {
        val result = buildPersonalizedHomeAlbumShelf(
            primaryAlbums = emptyList(),
            personalTracks = listOf(
                track(
                    id = "per-noi",
                    album = "PER NOI",
                    artist = "Geolier",
                    title = "PER NOI feat. Sfera Ebbasta"
                )
            ),
            recentTracks = emptyList(),
            favoriteTracks = emptyList(),
            quickPickTracks = emptyList(),
            localizedReleaseTracks = emptyList(),
            localizedSections = emptyList(),
            chartTracks = emptyList(),
            fallbackTracks = emptyList()
        )

        assertTrue(result.none { it.title.equals("PER NOI", ignoreCase = true) })
    }

    private fun album(title: String, artist: String): AlbumHit = AlbumHit(
        title = title,
        artist = artist,
        year = "2026",
        thumbnailUrl = "https://lh3.googleusercontent.com/$title=w544-h544",
        query = "$title $artist album",
        browseId = "MPREb_${title.replace(' ', '_')}"
    )

    private fun track(
        id: String,
        album: String,
        artist: String,
        title: String = "$album Song",
        artistBrowseIds: List<String> = listOf("UC_$id")
    ): Track = Track(
        id = id,
        title = title,
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
        artistBrowseIds = artistBrowseIds
    )
}
