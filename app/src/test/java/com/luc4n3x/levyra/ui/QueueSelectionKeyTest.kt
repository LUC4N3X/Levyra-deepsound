package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class QueueSelectionKeyTest {

    @Test
    fun eachQueueEntryGetsOneKey() {
        val queue = listOf(track("a"), track("b"), track("c"))

        assertEquals(queue.size, queueSelectionKeys(queue).size)
    }

    @Test
    fun duplicatedTracksKeepDistinctKeys() {
        val duplicated = track("a")
        val keys = queueSelectionKeys(listOf(duplicated, track("b"), duplicated))

        assertEquals(3, keys.toSet().size)
        assertNotEquals(keys[0], keys[2])
    }

    @Test
    fun keysAreStableAcrossMetadataRefreshOfTheSameQueue() {
        val original = listOf(track("a"), track("b"))
        val refreshed = original.map { it.copy(title = "refreshed ${it.id}", streamUrl = "https://cdn/${it.id}") }

        assertEquals(queueSelectionKeys(original), queueSelectionKeys(refreshed))
    }

    @Test
    fun appendingToTheQueueKeepsExistingKeys() {
        val original = listOf(track("a"), track("b"))
        val extended = original + track("c")

        assertEquals(queueSelectionKeys(original), queueSelectionKeys(extended).take(original.size))
    }

    @Test
    fun differentVideoUrlsForTheSameIdStayDistinct() {
        val keys = queueSelectionKeys(
            listOf(
                track("a", videoUrl = "https://levyra.test/a"),
                track("a", videoUrl = "https://levyra.test/a-alt")
            )
        )

        assertNotEquals(keys[0], keys[1])
    }

    @Test
    fun emptyQueueProducesNoKeys() {
        assertEquals(emptyList<String>(), queueSelectionKeys(emptyList()))
    }

    private fun track(
        id: String,
        videoUrl: String = "https://levyra.test/$id"
    ): Track = Track(
        id = id,
        title = "Title $id",
        artist = "Artist $id",
        album = "Album $id",
        durationMs = 190_000L,
        streamUrl = "",
        videoUrl = videoUrl,
        thumbnailUrl = "https://levyra.test/$id.jpg",
        largeThumbnailUrl = "https://levyra.test/$id-large.jpg",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0xFF00E5FF.toInt(),
        accentEnd = 0xFF2979FF.toInt()
    )
}
