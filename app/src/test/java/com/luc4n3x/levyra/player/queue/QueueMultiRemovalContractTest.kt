package com.luc4n3x.levyra.player.queue

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueMultiRemovalContractTest {

    @Test
    fun removalsBeforeCurrentShiftCurrentBackByRemovedCount() {
        assertEquals(
            2,
            queueMultiRemovalCurrentIndex(
                removedIndices = setOf(0, 1),
                currentIndex = 4,
                newLastIndex = 3
            )
        )
    }

    @Test
    fun removalsAfterCurrentKeepCurrent() {
        assertEquals(
            1,
            queueMultiRemovalCurrentIndex(
                removedIndices = setOf(3, 4),
                currentIndex = 1,
                newLastIndex = 2
            )
        )
    }

    @Test
    fun mixedRemovalsOnlyCountEntriesBeforeCurrent() {
        assertEquals(
            2,
            queueMultiRemovalCurrentIndex(
                removedIndices = setOf(0, 5, 6),
                currentIndex = 3,
                newLastIndex = 3
            )
        )
    }

    @Test
    fun removingCurrentLandsOnTheNextSurvivingEntry() {
        assertEquals(
            1,
            queueMultiRemovalCurrentIndex(
                removedIndices = setOf(0, 2),
                currentIndex = 2,
                newLastIndex = 2
            )
        )
    }

    @Test
    fun removingEveryTrackYieldsInvalidCurrent() {
        assertEquals(
            -1,
            queueMultiRemovalCurrentIndex(
                removedIndices = setOf(0, 1, 2),
                currentIndex = 1,
                newLastIndex = -1
            )
        )
    }

    @Test
    fun queueWithoutCurrentTrackStaysWithoutCurrent() {
        assertEquals(
            -1,
            queueMultiRemovalCurrentIndex(
                removedIndices = setOf(0),
                currentIndex = -1,
                newLastIndex = 1
            )
        )
    }

    @Test
    fun resultIsClampedIntoTheNewBounds() {
        assertEquals(
            1,
            queueMultiRemovalCurrentIndex(
                removedIndices = setOf(4, 5),
                currentIndex = 3,
                newLastIndex = 1
            )
        )
    }

    @Test
    fun singleRemovalMatchesTheSingleEntryContract() {
        listOf(
            Triple(0, 2, 3),
            Triple(2, 3, 4),
            Triple(4, 1, 4),
            Triple(5, 0, 4)
        ).forEach { (removed, current, newLastIndex) ->
            assertEquals(
                queueRemovalCurrentIndex(removed, current, newLastIndex),
                queueMultiRemovalCurrentIndex(setOf(removed), current, newLastIndex)
            )
        }
    }
}
