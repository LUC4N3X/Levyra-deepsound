package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeOfflineRenderTest {
    private val cachedSection = HomeSection("Quick picks", listOf(track("aaaaaaaaaaa")))

    @Test
    fun offlineHomeExposesLocalSectionsBuiltFromExistingStores() {
        val snapshot = buildHomeRenderSnapshot(offlineState())

        val offlineContent = snapshot.derived.offlineContent
        assertEquals(listOf("bbbbbbbbbbb"), offlineContent.downloads.map { it.id })
        assertEquals(listOf("offline-playlist"), offlineContent.playlists.map { it.id })
        assertEquals(listOf("ccccccccccc"), offlineContent.favorites.map { it.id })
        assertEquals(listOf("ddddddddddd"), offlineContent.recentlyPlayed.map { it.id })
    }

    @Test
    fun offlineHomeKeepsValidCachedRemoteContent() {
        val snapshot = buildHomeRenderSnapshot(offlineState())

        assertEquals(listOf(cachedSection), snapshot.state.homeSections)
        assertTrue(snapshot.derived.contentAvailability.hasUsableContent)
    }

    @Test
    fun onlineHomeBuildsNoOfflineSections() {
        val snapshot = buildHomeRenderSnapshot(offlineState().copy(isDeviceOffline = false))

        assertTrue(snapshot.derived.offlineContent.isEmpty)
        assertEquals(listOf(cachedSection), snapshot.state.homeSections)
    }

    @Test
    fun returningOnlineDropsTheOfflineSectionsAgain() {
        val offline = buildHomeRenderSnapshot(offlineState())
        val online = buildHomeRenderSnapshot(offlineState().copy(isDeviceOffline = false), offline)

        assertFalse(offline.derived.offlineContent.isEmpty)
        assertTrue(online.derived.offlineContent.isEmpty)
    }

    @Test
    fun homeRenderSnapshotReactsToConnectivityChanges() {
        val offline = buildHomeRenderSnapshot(offlineState())
        val online = buildHomeRenderSnapshot(offlineState().copy(isDeviceOffline = false))

        assertFalse(sameHomeRenderSnapshot(offline, online))
    }

    @Test
    fun frozenHomeStructureKeepsTheOfflineStatusStable() {
        val previous = buildHomeRenderSnapshot(offlineState())

        val frozen = buildStableHomeRenderSnapshot(
            state = offlineState().copy(isDeviceOffline = false),
            previous = previous,
            freezeContent = true
        )

        assertTrue(frozen.state.isDeviceOffline)
        assertFalse(frozen.derived.offlineContent.isEmpty)
    }

    private fun offlineState(): LevyraUiState = LevyraUiState(
        isDeviceOffline = true,
        homeSections = listOf(cachedSection),
        tracks = cachedSection.tracks,
        downloads = listOf(download()),
        playlists = listOf(
            Playlist(
                id = "offline-playlist",
                name = "Saved",
                coverUrl = "",
                tracks = listOf(track("bbbbbbbbbbb")),
                createdAt = 1_000L,
                updatedAt = 2_000L
            )
        ),
        favorites = listOf(track("ccccccccccc")),
        recentListens = listOf(track("ddddddddddd"))
    )

    private fun download(): DownloadedTrack = DownloadedTrack(
        id = 1L,
        trackId = "bbbbbbbbbbb",
        title = "Title bbbbbbbbbbb",
        artist = "Artist bbbbbbbbbbb",
        album = "Album bbbbbbbbbbb",
        durationMs = 180_000L,
        fileName = "title.m4a",
        uri = "content://levyra/bbbbbbbbbbb",
        mimeType = "audio/mp4",
        embeddedMetadata = true,
        savedAt = 1_000L
    )

    private fun track(id: String): Track = Track(
        id = id,
        title = "Title $id",
        artist = "Artist $id",
        album = "Album $id",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://music.youtube.com/watch?v=$id",
        thumbnailUrl = "https://example.com/$id.jpg",
        largeThumbnailUrl = "https://example.com/$id.jpg",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
