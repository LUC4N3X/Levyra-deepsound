package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSmartPlaylistsTest {
    @Test
    fun mostPlayedRanksByListeningTimeThenPlayCount() {
        val first = track("first")
        val second = track("second")

        val result = rankMostPlayedTracks(
            listOf(
                SmartPlaylistListen(first, listenedMs = 30_000L, startedAt = 1L),
                SmartPlaylistListen(first, listenedMs = 30_000L, startedAt = 2L),
                SmartPlaylistListen(second, listenedMs = 60_000L, startedAt = 3L)
            )
        )

        assertEquals(listOf("first", "second"), result.map { it.id })
    }

    @Test
    fun newestMetadataWinsAndResolvedUrlsAreNeverPersistedInPlaylist() {
        val old = track("same").copy(title = "Old", streamUrl = "https://old", videoStreamUrl = "https://video")
        val fresh = track("same").copy(title = "Fresh", streamUrl = "https://fresh")

        val result = rankMostPlayedTracks(
            listOf(
                SmartPlaylistListen(old, listenedMs = 10_000L, startedAt = 10L),
                SmartPlaylistListen(fresh, listenedMs = 10_000L, startedAt = 20L)
            )
        ).single()

        assertEquals("Fresh", result.title)
        assertTrue(result.streamUrl.isBlank())
        assertTrue(result.videoStreamUrl.isBlank())
    }

    private fun track(id: String) = Track(
        id = id,
        title = id,
        artist = "Artist",
        album = "Album",
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
        accentEnd = 0
    )
}
