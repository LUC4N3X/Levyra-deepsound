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
    fun multiArtistTrackCreditsNeverCreateAlbumCardsDirectly() {
        val cases = listOf(
            "Madame, Marracash",
            "Shiva feat. Geolier",
            "Alpha x Beta"
        )

        cases.forEachIndexed { index, credit ->
            val album = homeAlbumHitFromTrack(
                track(
                    id = "multi-$index",
                    album = "Album $index",
                    artist = credit,
                    artistBrowseIds = listOf("MPLA_PRIMARY_$index", "MPLA_GUEST_$index")
                )
            )

            assertEquals(null, album)
        }
    }

    @Test
    fun ambiguousTrackCreditWithoutCanonicalArtistReferencesIsRejected() {
        val album = homeAlbumHitFromTrack(
            track(
                id = "ambiguous",
                album = "Disincanto",
                artist = "Madame, Marracash",
                artistBrowseIds = emptyList()
            )
        )

        assertEquals(null, album)
    }

    @Test
    fun legacyCachedAlbumWithoutCanonicalIdentityIsRejected() {
        val legacy = AlbumHit(
            title = "Disincanto",
            artist = "Madame, Marracash",
            year = "",
            thumbnailUrl = "https://example.com/disincanto.jpg",
            query = "Disincanto Madame Marracash album"
        )

        assertTrue(!isCanonicalHomeAlbumHit(legacy))
    }

    @Test
    fun trackWithoutCanonicalAlbumIdentityCannotBecomeAnAlbumCard() {
        val album = homeAlbumHitFromTrack(
            track(
                id = "no-album-id",
                album = "Invented Album",
                artist = "Artist",
                albumBrowseId = ""
            )
        )

        assertEquals(null, album)
    }

    @Test
    fun simpleSingleArtistTrackCanStillCreateCanonicalAlbumCard() {
        val album = homeAlbumHitFromTrack(
            track(
                id = "solo",
                album = "Solo Album",
                artist = "Solo Artist",
                artistBrowseIds = listOf("MPLA_SOLO")
            )
        )

        assertEquals("Solo Album", album?.title)
        assertEquals("Solo Artist", album?.artist)
        assertEquals("MPLA_SOLO", album?.artistBrowseId)
    }

    @Test
    fun singlesAndEpsCannotEnterAlbumForYou() {
        val single = homeAlbumHitFromTrack(
            track(
                id = "single",
                album = "Example Single",
                artist = "Artist"
            )
        )
        val ep = homeAlbumHitFromTrack(
            track(
                id = "ep",
                album = "Example EP",
                artist = "Artist"
            )
        )

        assertEquals(null, single)
        assertEquals(null, ep)
    }

    @Test
    fun authoritativeCollaborativeAlbumEntityKeepsAllAlbumLevelArtists() {
        val collaborative = album("Santana Money Gang", "Sfera Ebbasta, Shiva")
            .copy(artistBrowseId = "MPLA_SFERA")

        val result = buildPersonalizedHomeAlbumShelf(
            primaryAlbums = listOf(collaborative),
            personalTracks = emptyList(),
            recentTracks = emptyList(),
            favoriteTracks = emptyList(),
            quickPickTracks = emptyList(),
            localizedReleaseTracks = emptyList(),
            localizedSections = emptyList(),
            chartTracks = emptyList(),
            fallbackTracks = emptyList()
        )

        assertEquals("Santana Money Gang", result.single().title)
        assertEquals("Sfera Ebbasta, Shiva", result.single().artist)
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
        artistBrowseIds: List<String> = listOf("UC_$id"),
        albumBrowseId: String = "MPREb_$id"
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
        albumBrowseId = albumBrowseId,
        artistBrowseIds = artistBrowseIds
    )
}
