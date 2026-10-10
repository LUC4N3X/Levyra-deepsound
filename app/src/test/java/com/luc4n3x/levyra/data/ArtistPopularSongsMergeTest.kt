package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistPopularSongsMergeTest {
    @Test
    fun expandedSongsBackfillMissingPreviewDurationsWithoutChangingOrder() {
        val preview = listOf(
            track("a", 0L),
            track("b", 0L),
            track("c", 205_000L)
        )
        val expanded = listOf(
            track("a", 181_000L),
            track("b", 194_000L),
            track("c", 999_000L),
            track("d", 212_000L)
        )

        val merged = mergeArtistSongs(preview, expanded)

        assertEquals(listOf("a", "b", "c", "d"), merged.map { it.id })
        assertEquals(181_000L, merged[0].durationMs)
        assertEquals(194_000L, merged[1].durationMs)
        assertEquals(205_000L, merged[2].durationMs)
    }

    private fun track(id: String, durationMs: Long) = Track(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        durationMs = durationMs,
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
