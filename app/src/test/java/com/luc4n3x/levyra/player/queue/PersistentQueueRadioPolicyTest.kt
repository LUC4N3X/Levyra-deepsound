package com.luc4n3x.levyra.player.queue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentQueueRadioPolicyTest {

    @Test
    fun trimPreservesCurrentAndRecentHistory() {
        val history = (0..20).toList()

        val removable = radioHistoryTrimIndices(
            history = history,
            currentIndex = 20,
            slotsNeeded = 5,
            historyReserve = 8
        )

        assertEquals(setOf(0, 1, 2, 3, 4), removable)
        assertTrue(20 !in removable)
        assertTrue((13..20).none(removable::contains))
    }

    @Test
    fun trimNeverInventsSlotsWithoutOldHistory() {
        assertTrue(
            radioHistoryTrimIndices(
                history = listOf(1, 2, 3),
                currentIndex = 3,
                slotsNeeded = 5,
                historyReserve = 8
            ).isEmpty()
        )
    }

    @Test
    fun trimDeduplicatesRepeatedHistoryIndices() {
        val removable = radioHistoryTrimIndices(
            history = listOf(1, 2, 1, 3, 4, 5),
            currentIndex = 5,
            slotsNeeded = 3,
            historyReserve = 2
        )

        assertEquals(setOf(1, 2, 3), removable)
    }
}
