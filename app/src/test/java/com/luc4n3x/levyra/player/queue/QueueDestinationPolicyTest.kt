package com.luc4n3x.levyra.player.queue

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueDestinationPolicyTest {
    @Test
    fun `one queue adds directly but multiple queues require destination selection`() {
        assertFalse(shouldPromptForQueueDestination(emptyList()))
        assertFalse(shouldPromptForQueueDestination(listOf(space("current"))))
        assertTrue(shouldPromptForQueueDestination(listOf(space("current"), space("gym"))))
    }

    @Test
    fun `pending destination keeps every unique track across repeated adds`() {
        val first = track("1")
        val second = track("2")

        val pending = mergePendingQueueDestinationTracks(
            mergePendingQueueDestinationTracks(emptyList(), listOf(first)),
            listOf(second, first)
        )

        assertEquals(listOf("1", "2"), pending.map { it.id })
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist",
        album = "",
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

    private fun space(id: String) = QueueSpaceSummary(
        id = id,
        name = id,
        createdAt = 0L,
        updatedAt = 0L,
        lastActiveAt = 0L,
        isActive = id == "current",
        trackCount = 0,
        durationMs = 0L,
        artworkUrls = emptyList()
    )
}
