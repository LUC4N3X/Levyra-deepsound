package com.luc4n3x.levyra.player.queue

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueuePlaylistExportTest {

    @Test
    fun exportKeepsQueueOrderAndSkipsLocalOrInvalidTracks() {
        val unavailableUri = "content://library/missing"
        val export = prepareQueuePlaylistExport(
            tracks = listOf(
                track("first"),
                track("missing").copy(streamUrl = unavailableUri),
                track("local").copy(streamUrl = "file:///music/local.mp3"),
                track("third"),
                track("")
            ),
            unavailableLocalUris = setOf(unavailableUri)
        )

        assertEquals(listOf("first", "third"), export.tracks.map { it.id })
        assertEquals(3, export.skippedCount)
    }

    @Test
    fun emptyQueueProducesAnEmptyExport() {
        val export = prepareQueuePlaylistExport(emptyList(), emptySet())

        assertTrue(export.tracks.isEmpty())
        assertEquals(0, export.skippedCount)
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist",
        album = "Album",
        durationMs = 1_000L,
        videoUrl = "",
        streamUrl = "",
        videoStreamUrl = "",
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
