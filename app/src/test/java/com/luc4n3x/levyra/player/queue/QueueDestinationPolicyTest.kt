package com.luc4n3x.levyra.player.queue

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
