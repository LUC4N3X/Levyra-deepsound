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

    @Test
    fun fullCatalogKeepsPopularFirstAndGoesPastPreviewCap() {
        val popular = (1..5).map { track("p$it", 1L) }
        val catalog = (1..300).map { track("s$it", 1L) } + popular

        val merged = mergeArtistSongs(popular, catalog, ARTIST_ALL_SONGS_LIMIT)

        assertEquals(popular.map { it.id }, merged.take(5).map { it.id })
        assertEquals(305, merged.size)
        assertEquals(merged.size, merged.distinctBy { it.id }.size)
    }

    @Test
    fun queueTailAppendsOnlyMissingSongsWhileQueueIsUntouched() {
        val initial = listOf(track("a", 1L), track("b", 1L))
        val catalog = listOf(track("a", 1L), track("c", 1L), track("b", 1L), track("d", 1L))

        assertEquals(listOf("c", "d"), artistQueueTail(initial, initial, catalog).map { it.id })
    }

    @Test
    fun queueTailIsEmptyAfterTheUserChangedTheQueue() {
        val initial = listOf(track("a", 1L), track("b", 1L))
        val changed = listOf(track("x", 1L))
        val catalog = listOf(track("a", 1L), track("c", 1L))

        assertEquals(emptyList<Track>(), artistQueueTail(changed, initial, catalog))
        assertEquals(emptyList<Track>(), artistQueueTail(listOf(track("b", 1L), track("a", 1L)), initial, catalog))
    }

    @Test
    fun queueTailSkipsSongsAlreadyAppendedAfterTheArtistQueue() {
        val initial = listOf(track("a", 1L), track("b", 1L))
        val appended = initial + track("c", 1L)
        val catalog = listOf(track("a", 1L), track("c", 1L), track("d", 1L))

        assertEquals(listOf("d"), artistQueueTail(appended, initial, catalog).map { it.id })
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
