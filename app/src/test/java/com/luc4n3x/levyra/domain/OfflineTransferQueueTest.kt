package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineTransferQueueTest {
    @Test
    fun laneProgressAveragesFinishedAndRunningTracks() {
        val progress = OfflineLaneProgress.of(total = 4, completed = 2, progressSum = 100 + 100 + 50 + 0)
        assertEquals(4, progress.total)
        assertEquals(2, progress.completed)
        assertEquals(62, progress.percent)
    }

    @Test
    fun laneProgressClampsBadRows() {
        assertEquals(OfflineLaneProgress.Empty, OfflineLaneProgress.of(total = 0, completed = 3, progressSum = 900))
        assertEquals(100, OfflineLaneProgress.of(total = 2, completed = 9, progressSum = 999).percent)
        assertEquals(2, OfflineLaneProgress.of(total = 2, completed = 9, progressSum = 999).completed)
    }

    @Test
    fun preloadProgressCountsCompleteTracksAsFull() {
        val items = listOf(
            PreloadQueueItem("a", "A", "X", progress = 40, complete = true),
            PreloadQueueItem("b", "B", "X", progress = 50, complete = false),
            PreloadQueueItem("c", "C", "X", progress = 0, complete = false)
        )
        val progress = items.preloadLaneProgress()
        assertEquals(3, progress.total)
        assertEquals(1, progress.completed)
        assertEquals(50, progress.percent)
    }
}
