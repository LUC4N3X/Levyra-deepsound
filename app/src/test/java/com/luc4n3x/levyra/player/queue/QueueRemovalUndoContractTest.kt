package com.luc4n3x.levyra.player.queue

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueRemovalUndoContractTest {

    @Test
    fun removeFirstKeepsCurrentIndexAligned() {
        assertEquals(1, queueRemovalCurrentIndex(removedIndex = 0, currentIndex = 2, newLastIndex = 3))
    }

    @Test
    fun removeBeforeCurrentShiftsCurrentBack() {
        assertEquals(0, queueRemovalCurrentIndex(removedIndex = 0, currentIndex = 1, newLastIndex = 2))
    }

    @Test
    fun removeMiddleBeforeCurrentShiftsCurrentBack() {
        assertEquals(2, queueRemovalCurrentIndex(removedIndex = 2, currentIndex = 3, newLastIndex = 4))
    }

    @Test
    fun removeAfterCurrentKeepsCurrent() {
        assertEquals(1, queueRemovalCurrentIndex(removedIndex = 4, currentIndex = 1, newLastIndex = 4))
    }

    @Test
    fun removeLastAfterCurrentKeepsCurrent() {
        assertEquals(0, queueRemovalCurrentIndex(removedIndex = 5, currentIndex = 0, newLastIndex = 4))
    }

    @Test
    fun removeCurrentSelectsSameClampedIndex() {
        assertEquals(2, queueRemovalCurrentIndex(removedIndex = 2, currentIndex = 2, newLastIndex = 3))
    }

    @Test
    fun removeCurrentAtTailClampsToNewLast() {
        assertEquals(3, queueRemovalCurrentIndex(removedIndex = 4, currentIndex = 4, newLastIndex = 3))
    }

    @Test
    fun removeOnlyTrackYieldsInvalidCurrent() {
        assertEquals(-1, queueRemovalCurrentIndex(removedIndex = 0, currentIndex = 0, newLastIndex = -1))
    }

    @Test
    fun undoRestoresOriginalPosition() {
        assertEquals(2, queueUndoInsertionIndex(originalIndex = 2, size = 4))
    }

    @Test
    fun undoAfterQueueMutationClampsToBounds() {
        assertEquals(4, queueUndoInsertionIndex(originalIndex = 9, size = 4))
        assertEquals(0, queueUndoInsertionIndex(originalIndex = -3, size = 4))
    }

    @Test
    fun undoInsertedBeforeCurrentShiftsCurrentForward() {
        assertEquals(3, queueUndoCurrentIndex(insertionIndex = 2, currentIndex = 2))
    }

    @Test
    fun undoInsertedAfterCurrentKeepsCurrent() {
        assertEquals(1, queueUndoCurrentIndex(insertionIndex = 4, currentIndex = 1))
    }

    @Test
    fun undoIntoEmptyQueueSelectsInsertedEntry() {
        assertEquals(0, queueUndoCurrentIndex(insertionIndex = 0, currentIndex = -1))
    }

    @Test
    fun rapidRemovalsKeepSingleBoundedUndoEntry() {
        val firstRemovalIndex = queueUndoInsertionIndex(originalIndex = 1, size = 4)
        val lastRemovalIndex = queueUndoInsertionIndex(originalIndex = 3, size = 4)

        assertEquals(1, firstRemovalIndex)
        assertEquals(3, lastRemovalIndex)
    }
}
