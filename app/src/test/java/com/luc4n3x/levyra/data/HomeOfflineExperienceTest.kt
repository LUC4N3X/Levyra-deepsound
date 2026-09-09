package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeOfflineExperienceTest {
    @Test
    fun offlineDeviceSelectsTheOfflineHome() {
        assertTrue(HomeOfflinePolicy.showOfflineHome(deviceOffline = true))
        assertFalse(HomeOfflinePolicy.shouldAttemptRemoteRefresh(deviceOffline = true))
    }

    @Test
    fun backendFailureWithConnectivityIsNotTreatedAsDeviceOffline() {
        assertFalse(HomeOfflinePolicy.showOfflineHome(deviceOffline = false))
        assertTrue(HomeOfflinePolicy.shouldAttemptRemoteRefresh(deviceOffline = false))
        assertEquals(
            "Home unavailable",
            HomeOfflinePolicy.homeErrorAfterRemoteFailure(deviceOffline = false, fallback = "Home unavailable")
        )
    }

    @Test
    fun remotePlaceholdersNeverStayPendingWhenClearlyOffline() {
        assertFalse(HomeOfflinePolicy.remoteLoading(requested = true, deviceOffline = true))
        assertTrue(HomeOfflinePolicy.remoteLoading(requested = true, deviceOffline = false))
        assertFalse(HomeOfflinePolicy.shouldAttemptRemoteRefresh(deviceOffline = true))
    }

    @Test
    fun remoteFailureKeepsTheErrorOnlyWhenConnectivityExists() {
        assertNull(
            HomeOfflinePolicy.homeErrorAfterRemoteFailure(deviceOffline = true, fallback = "Home unavailable")
        )
        assertEquals(
            "Home unavailable",
            HomeOfflinePolicy.homeErrorAfterRemoteFailure(deviceOffline = false, fallback = "Home unavailable")
        )
    }

    @Test
    fun recoveryRunsOnlyOnTheOfflineToOnlineTransition() {
        assertTrue(HomeOfflinePolicy.shouldRecoverOnReconnect(previousOffline = true, deviceOffline = false))
        assertFalse(HomeOfflinePolicy.shouldRecoverOnReconnect(previousOffline = false, deviceOffline = false))
        assertFalse(HomeOfflinePolicy.shouldRecoverOnReconnect(previousOffline = true, deviceOffline = true))
    }

    @Test
    fun offlineContentExposesEveryAvailableLocalSection() {
        val favorite = track("fav1", "Fade", "Kanye")
        val recent = track("rec1", "Blue", "Joni")
        val content = HomeOfflineContentBuilder.build(
            deviceOffline = true,
            downloads = listOf(download(1L, "dl1", "Fade", "Kanye")),
            playlists = listOf(playlist("p1", listOf(track("dl1", "Fade", "Kanye")))),
            favorites = listOf(favorite),
            recentListens = listOf(recent),
            artworkPool = listOf(favorite, recent)
        )

        assertEquals(listOf("dl1"), content.downloads.map { it.id })
        assertEquals(listOf("p1"), content.playlists.map { it.id })
        assertEquals(listOf("fav1"), content.favorites.map { it.id })
        assertEquals(listOf("rec1"), content.recentlyPlayed.map { it.id })
        assertFalse(content.isEmpty)
    }

    @Test
    fun offlineDownloadsBecomePlayableLocalTracks() {
        val content = HomeOfflineContentBuilder.build(
            deviceOffline = true,
            downloads = listOf(download(1L, "dl1", "Fade", "Kanye")),
            playlists = emptyList(),
            favorites = emptyList(),
            recentListens = emptyList(),
            artworkPool = emptyList()
        )

        val offlineTrack = content.downloads.single()
        assertEquals("content://levyra/dl1", offlineTrack.streamUrl)
        assertEquals("Offline", offlineTrack.source)
        assertTrue(offlineTrack.hasPlayableStream)
    }

    @Test
    fun offlineDownloadsReuseKnownArtworkWhenAvailable() {
        val known = track("dl1", "Fade", "Kanye")
        val content = HomeOfflineContentBuilder.build(
            deviceOffline = true,
            downloads = listOf(download(1L, "", "Fade", "Kanye")),
            playlists = emptyList(),
            favorites = emptyList(),
            recentListens = listOf(known),
            artworkPool = listOf(known)
        )

        assertEquals(known.thumbnailUrl, content.downloads.single().thumbnailUrl)
    }

    @Test
    fun downloadsWithoutStorageUriAreNotSurfaced() {
        val content = HomeOfflineContentBuilder.build(
            deviceOffline = true,
            downloads = listOf(download(1L, "dl1", "Fade", "Kanye").copy(uri = "")),
            playlists = emptyList(),
            favorites = emptyList(),
            recentListens = emptyList(),
            artworkPool = emptyList()
        )

        assertTrue(content.downloads.isEmpty())
        assertTrue(content.isEmpty)
    }

    @Test
    fun playlistsWithoutDownloadedTracksAreNotSurfaced() {
        val content = HomeOfflineContentBuilder.build(
            deviceOffline = true,
            downloads = listOf(download(1L, "dl1", "Fade", "Kanye")),
            playlists = listOf(playlist("p1", listOf(track("other", "Blue", "Joni")))),
            favorites = emptyList(),
            recentListens = emptyList(),
            artworkPool = emptyList()
        )

        assertTrue(content.playlists.isEmpty())
    }

    @Test
    fun hiddenPlaylistsStayHiddenOffline() {
        val content = HomeOfflineContentBuilder.build(
            deviceOffline = true,
            downloads = listOf(download(1L, "dl1", "Fade", "Kanye")),
            playlists = listOf(playlist("p1", listOf(track("dl1", "Fade", "Kanye"))).copy(hidden = true)),
            favorites = emptyList(),
            recentListens = emptyList(),
            artworkPool = emptyList()
        )

        assertTrue(content.playlists.isEmpty())
    }

    @Test
    fun onlineDeviceBuildsNoOfflineContent() {
        val content = HomeOfflineContentBuilder.build(
            deviceOffline = false,
            downloads = listOf(download(1L, "dl1", "Fade", "Kanye")),
            playlists = listOf(playlist("p1", listOf(track("dl1", "Fade", "Kanye")))),
            favorites = listOf(track("fav1", "Blue", "Joni")),
            recentListens = listOf(track("rec1", "Help", "Beatles")),
            artworkPool = emptyList()
        )

        assertTrue(content.isEmpty)
    }

    @Test
    fun offlineShelvesStayBounded() {
        val downloads = (1..80).map { index -> download(index.toLong(), "dl$index", "Song $index", "Artist $index") }
        val favorites = (1..80).map { index -> track("fav$index", "Fav $index", "Artist $index") }

        val content = HomeOfflineContentBuilder.build(
            deviceOffline = true,
            downloads = downloads,
            playlists = emptyList(),
            favorites = favorites,
            recentListens = emptyList(),
            artworkPool = emptyList()
        )

        assertEquals(HomeOfflineContentBuilder.SHELF_LIMIT, content.downloads.size)
        assertEquals(HomeOfflineContentBuilder.SHELF_LIMIT, content.favorites.size)
    }

    private fun download(id: Long, trackId: String, title: String, artist: String): DownloadedTrack = DownloadedTrack(
        id = id,
        trackId = trackId,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 180_000L,
        fileName = "$title.m4a",
        uri = "content://levyra/${trackId.ifBlank { id.toString() }}",
        mimeType = "audio/mp4",
        embeddedMetadata = true,
        savedAt = 1_000L
    )

    private fun playlist(id: String, tracks: List<Track>): Playlist = Playlist(
        id = id,
        name = "Playlist $id",
        coverUrl = "",
        tracks = tracks,
        createdAt = 1_000L,
        updatedAt = 2_000L
    )

    private fun track(id: String, title: String, artist: String): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "https://example.com/$id.jpg",
        largeThumbnailUrl = "https://example.com/$id-large.jpg",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 0,
        accentStart = 10,
        accentEnd = 20
    )
}
