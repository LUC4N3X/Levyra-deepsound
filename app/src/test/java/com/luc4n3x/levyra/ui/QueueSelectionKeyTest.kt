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

    @Test
    fun reorderingDuplicateEntriesPreservesStableKeysAndSelection() {
        val trackA = track("a")
        val trackB = track("b")
        val queue = listOf(trackA, trackA, trackB)
        var entries = buildQueueEntries(queue)

        val firstAKey = entries[0].key
        val secondAKey = entries[1].key
        assertNotEquals(firstAKey, secondAKey)

        // Select the second duplicate
        val selectedKeys = setOf(secondAKey)

        // Reorder: move the second duplicate (index 1) to the top (index 0)
        entries = reorderQueueEntries(entries, from = 1, to = 0)

        // Verify that the entry moved to index 0 retained its key
        assertEquals(secondAKey, entries[0].key)
        assertEquals(firstAKey, entries[1].key)

        // Verify that selection still tracks the moved entry
        val selectedEntries = entries.filter { it.key in selectedKeys }
        assertEquals(1, selectedEntries.size)
        assertEquals(secondAKey, selectedEntries.single().key)
        assertEquals(0, entries.indexOfFirst { it.key in selectedKeys })

        // Reorder again: move index 0 to index 2
        entries = reorderQueueEntries(entries, from = 0, to = 2)
        assertEquals(firstAKey, entries[0].key)
        assertEquals(secondAKey, entries[2].key)
        assertEquals(2, entries.indexOfFirst { it.key in selectedKeys })
    }

    @Test
    fun repeatedReorderingWithThreeDuplicatesPreservesAllIdentities() {
        val trackA = track("a")
        val queue = listOf(trackA, trackA, trackA)
        var entries = buildQueueEntries(queue)

        val k1 = entries[0].key
        val k2 = entries[1].key
        val k3 = entries[2].key
        assertEquals(3, setOf(k1, k2, k3).size)

        // Move 2 to 0 -> [k3, k1, k2]
        entries = reorderQueueEntries(entries, 2, 0)
        assertEquals(listOf(k3, k1, k2), entries.map { it.key })

        // Move 2 to 1 -> [k3, k2, k1]
        entries = reorderQueueEntries(entries, 2, 1)
        assertEquals(listOf(k3, k2, k1), entries.map { it.key })

        // Move 0 to 2 -> [k2, k1, k3]
        entries = reorderQueueEntries(entries, 0, 2)
        assertEquals(listOf(k2, k1, k3), entries.map { it.key })
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
