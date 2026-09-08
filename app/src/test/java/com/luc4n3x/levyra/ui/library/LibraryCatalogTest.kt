package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.LibrarySort
import com.luc4n3x.levyra.domain.LibrarySortDirection
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryCatalogTest {
    @Test
    fun searchIsAccentInsensitiveAcrossTitleArtistAndAlbum() {
        val tracks = listOf(
            track(id = "1", title = "Però", artist = "Lazza", album = "Sirio"),
            track(id = "2", title = "Bellissima", artist = "Annalisa", album = "E poi siamo finiti")
        )

        assertEquals(listOf("1"), filterLibraryTracks(tracks, "pero", LibrarySort.Recent).map { it.id })
        assertEquals(listOf("2"), filterLibraryTracks(tracks, "siamo", LibrarySort.Recent).map { it.id })
    }

    @Test
    fun explicitAndCleanTracksWithoutIdsKeepDistinctKeys() {
        val clean = track(id = "", explicit = false)
        val explicit = clean.copy(explicit = true)

        assertNotEquals(libraryTrackKey(clean), libraryTrackKey(explicit))
    }

    @Test
    fun catalogDeduplicatesProviderIdsWhenIsrcMatches() {
        val first = track(id = "provider-a").copy(isrc = "IT-A00-26-00001")
        val second = track(id = "provider-b").copy(isrc = "it-a00-26-00001")

        val catalog = buildLibraryCatalog(
            favorites = listOf(first),
            playlists = listOf(Playlist("p", "Mix", "", listOf(second), 1L, 2L)),
            downloads = emptyList(),
            recentListens = emptyList(),
            followedArtists = emptyList()
        )

        assertEquals(1, catalog.tracks.size)
    }

    @Test
    fun catalogDeduplicatesSameTrackAcrossSources() {
        val song = track(id = "song-1")
        val playlist = Playlist("p", "Mix", "", listOf(song), 1L, 2L)
        val download = DownloadedTrack(
            id = 1L,
            trackId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs,
            fileName = "song.m4a",
            uri = "content://song",
            mimeType = "audio/mp4",
            embeddedMetadata = true,
            savedAt = 3L
        )

        val catalog = buildLibraryCatalog(
            favorites = listOf(song),
            playlists = listOf(playlist),
            downloads = listOf(download),
            recentListens = listOf(song, song),
            followedArtists = emptyList(),
            mostPlayedTracks = listOf(song)
        )

        assertEquals(1, catalog.tracks.size)
        assertEquals(1, catalog.offlineTracks.size)
        assertEquals(1, catalog.albums.size)
        assertTrue(catalog.mostPlayed.first().id == song.id)
    }


    @Test
    fun recentOnlyTracksStayInRecentButDoNotPolluteSavedLibrary() {
        val recentOnly = track(id = "recent-only")

        val catalog = buildLibraryCatalog(
            favorites = emptyList(),
            playlists = emptyList(),
            downloads = emptyList(),
            recentListens = listOf(recentOnly),
            followedArtists = emptyList()
        )

        assertTrue(catalog.tracks.isEmpty())
        assertEquals(listOf("recent-only"), catalog.recent.map { it.id })
    }

    @Test
    fun artistGroupingDoesNotSplitNamesContainingCommaAmpersandOrSlash() {
        val tracks = listOf(
            track(id = "1", artist = "Tyler, The Creator"),
            track(id = "2", artist = "Simon & Garfunkel"),
            track(id = "3", artist = "AC/DC")
        )

        val catalog = buildLibraryCatalog(
            favorites = tracks,
            playlists = emptyList(),
            downloads = emptyList(),
            recentListens = emptyList(),
            followedArtists = emptyList()
        )

        assertEquals(
            setOf("Tyler, The Creator", "Simon & Garfunkel", "AC/DC"),
            catalog.artists.map { it.name }.toSet()
        )
    }

    @Test
    fun offlineCatalogKeepsDistinctFilesForTheSameTrack() {
        val song = track(id = "same-song")
        val downloads = listOf(
            downloaded(id = 1L, track = song, fileName = "song-low.m4a"),
            downloaded(id = 2L, track = song, fileName = "song-high.m4a")
        )

        val catalog = buildLibraryCatalog(
            favorites = listOf(song),
            playlists = emptyList(),
            downloads = downloads,
            recentListens = emptyList(),
            followedArtists = emptyList()
        )

        assertEquals(1, catalog.tracks.size)
        assertEquals(2, catalog.offlineItems.size)
        assertEquals(setOf(1L, 2L), catalog.offlineItems.map { it.download.id }.toSet())
    }

    @Test
    fun albumGroupingUsesBrowseIdBeforeTextMetadata() {
        val first = track(id = "1", albumBrowseId = "MPREb_album", album = "Album")
        val second = track(id = "2", albumBrowseId = "MPREb_album", album = "Album Deluxe")

        val catalog = buildLibraryCatalog(
            favorites = listOf(first, second),
            playlists = emptyList(),
            downloads = emptyList(),
            recentListens = emptyList(),
            followedArtists = emptyList()
        )

        assertEquals(1, catalog.albums.size)
        assertEquals(2, catalog.albums.single().tracks.size)
    }

    @Test
    fun filterLibraryOfflineItemsSortsRecentNewestAndOldest() {
        val t1 = track(id = "1", title = "First")
        val t2 = track(id = "2", title = "Second")
        val items = listOf(
            LibraryOfflineItem("1", t1, downloaded(100L, t1, "1.m4a")),
            LibraryOfflineItem("2", t2, downloaded(200L, t2, "2.m4a"))
        )

        val newest = filterLibraryOfflineItems(items, "", LibrarySort.Recent, LibrarySortDirection.Descending)
        assertEquals(listOf("2", "1"), newest.map { it.track.id })

        val oldest = filterLibraryOfflineItems(items, "", LibrarySort.Recent, LibrarySortDirection.Ascending)
        assertEquals(listOf("1", "2"), oldest.map { it.track.id })
    }

    @Test
    fun filterLibraryOfflineItemsSortsTitleAscendingAndDescendingWithDiacritics() {
        val t1 = track(id = "1", title = "Àlbero")
        val t2 = track(id = "2", title = "Barca")
        val t3 = track(id = "3", title = "Casa")
        val items = listOf(
            LibraryOfflineItem("3", t3, downloaded(3L, t3, "3.m4a")),
            LibraryOfflineItem("1", t1, downloaded(1L, t1, "1.m4a")),
            LibraryOfflineItem("2", t2, downloaded(2L, t2, "2.m4a"))
        )

        val az = filterLibraryOfflineItems(items, "", LibrarySort.Title, LibrarySortDirection.Ascending)
        assertEquals(listOf("1", "2", "3"), az.map { it.track.id })

        val za = filterLibraryOfflineItems(items, "", LibrarySort.Title, LibrarySortDirection.Descending)
        assertEquals(listOf("3", "2", "1"), za.map { it.track.id })
    }

    @Test
    fun filterLibraryOfflineItemsSortsArtistAndPutsEmptyAtEnd() {
        val t1 = track(id = "1", title = "Song A", artist = "Adele")
        val t2 = track(id = "2", title = "Song B", artist = "Coldplay")
        val tEmpty = track(id = "3", title = "Song C", artist = "")
        val items = listOf(
            LibraryOfflineItem("3", tEmpty, downloaded(3L, tEmpty, "3.m4a")),
            LibraryOfflineItem("2", t2, downloaded(2L, t2, "2.m4a")),
            LibraryOfflineItem("1", t1, downloaded(1L, t1, "1.m4a"))
        )

        val az = filterLibraryOfflineItems(items, "", LibrarySort.Artist, LibrarySortDirection.Ascending)
        assertEquals(listOf("1", "2", "3"), az.map { it.track.id })

        val za = filterLibraryOfflineItems(items, "", LibrarySort.Artist, LibrarySortDirection.Descending)
        assertEquals(listOf("2", "1", "3"), za.map { it.track.id })
    }

    @Test
    fun filterLibraryOfflineItemsSortsDurationLongestAndShortest() {
        val tShort = track(id = "1", title = "Short").copy(durationMs = 60_000L)
        val tLong = track(id = "2", title = "Long").copy(durationMs = 300_000L)
        val tZero = track(id = "3", title = "Zero").copy(durationMs = 0L)
        val items = listOf(
            LibraryOfflineItem("1", tShort, downloaded(1L, tShort, "1.m4a")),
            LibraryOfflineItem("3", tZero, downloaded(3L, tZero, "3.m4a")),
            LibraryOfflineItem("2", tLong, downloaded(2L, tLong, "2.m4a"))
        )

        val longest = filterLibraryOfflineItems(items, "", LibrarySort.Duration, LibrarySortDirection.Descending)
        assertEquals(listOf("2", "1", "3"), longest.map { it.track.id })

        val shortest = filterLibraryOfflineItems(items, "", LibrarySort.Duration, LibrarySortDirection.Ascending)
        assertEquals(listOf("1", "2", "3"), shortest.map { it.track.id })
    }

    private fun downloaded(id: Long, track: Track, fileName: String) = DownloadedTrack(
        id = id,
        trackId = track.id,
        title = track.title,
        artist = track.artist,
        album = track.album,
        durationMs = track.durationMs,
        fileName = fileName,
        uri = "content://$id",
        mimeType = "audio/mp4",
        embeddedMetadata = true,
        savedAt = id
    )

    private fun track(
        id: String,
        title: String = "Titolo",
        artist: String = "Artista",
        album: String = "Album",
        explicit: Boolean = false,
        albumBrowseId: String = ""
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        explicit = explicit,
        albumBrowseId = albumBrowseId
    )
}
