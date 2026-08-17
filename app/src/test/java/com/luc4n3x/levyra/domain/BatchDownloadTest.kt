package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchDownloadTest {

    @Test
    fun `progress averages child progress across the batch`() {
        assertEquals(50, batchDownloadProgress(total = 4, progressSum = 200))
        assertEquals(0, batchDownloadProgress(total = 0, progressSum = 500))
        assertEquals(100, batchDownloadProgress(total = 2, progressSum = 400))
    }

    @Test
    fun `queued batch reports queued until a child makes progress`() {
        assertEquals(
            BatchDownloadState.Queued,
            batchDownloadState(total = 14, completed = 0, failed = 0, active = 14, progressSum = 0)
        )
        assertEquals(
            BatchDownloadState.Downloading,
            batchDownloadState(total = 14, completed = 0, failed = 0, active = 14, progressSum = 40)
        )
        assertEquals(
            BatchDownloadState.Downloading,
            batchDownloadState(total = 14, completed = 8, failed = 0, active = 6, progressSum = 812)
        )
    }

    @Test
    fun `batch completes only when every child succeeded`() {
        assertEquals(
            BatchDownloadState.Completed,
            batchDownloadState(total = 14, completed = 14, failed = 0, active = 0, progressSum = 1400)
        )
        assertEquals(
            BatchDownloadState.Failed,
            batchDownloadState(total = 14, completed = 12, failed = 2, active = 0, progressSum = 1200)
        )
        assertEquals(
            BatchDownloadState.Cancelled,
            batchDownloadState(total = 14, completed = 3, failed = 0, active = 0, progressSum = 300)
        )
    }

    @Test
    fun `empty batch is treated as cancelled`() {
        assertEquals(
            BatchDownloadState.Cancelled,
            batchDownloadState(total = 0, completed = 0, failed = 0, active = 0, progressSum = 0)
        )
    }

    @Test
    fun `retry is offered only for settled batches with failures`() {
        assertTrue(batch(total = 10, completed = 8, failed = 2, active = 0).canRetry)
        assertFalse(batch(total = 10, completed = 8, failed = 2, active = 1).canRetry)
        assertFalse(batch(total = 10, completed = 10, failed = 0, active = 0).canRetry)
    }

    @Test
    fun `batch key prefers the canonical identifier`() {
        assertEquals(
            "album:mpreb_ghost",
            batchDownloadKey(BatchDownloadKind.Album, canonicalId = "MPREb_ghost", fallbackTitle = "Ghost Stories")
        )
        assertEquals(
            "playlist:chill-hits",
            batchDownloadKey(BatchDownloadKind.Playlist, canonicalId = "", fallbackTitle = "Chill Hits")
        )
        assertEquals(
            "",
            batchDownloadKey(BatchDownloadKind.Album, canonicalId = "", fallbackTitle = "   ")
        )
    }

    @Test
    fun `unknown kind labels fall back to album`() {
        assertEquals(BatchDownloadKind.Playlist, batchDownloadKindOf("Playlist"))
        assertEquals(BatchDownloadKind.Playlist, batchDownloadKindOf("PLAYLIST"))
        assertEquals(BatchDownloadKind.Album, batchDownloadKindOf(""))
        assertEquals(BatchDownloadKind.Album, batchDownloadKindOf("something-else"))
    }

    @Test
    fun `settled batches leave the active list but failed ones stay for retry`() {
        val batches = listOf(
            batchWith(BatchDownloadState.Downloading, "a"),
            batchWith(BatchDownloadState.Completed, "b"),
            batchWith(BatchDownloadState.Failed, "c"),
            batchWith(BatchDownloadState.Cancelled, "d"),
            batchWith(BatchDownloadState.Queued, "e")
        )

        assertEquals(listOf("a", "c", "e"), visibleDownloadBatches(batches).map { it.key })
    }

    @Test
    fun `a track already owned by an unsettled batch keeps its first membership`() {
        assertTrue(
            retainsExistingBatchMembership(
                previousBatchKey = "album:first",
                previousState = "RUNNING",
                requestedBatchKey = "playlist:second"
            )
        )
        assertTrue(
            retainsExistingBatchMembership(
                previousBatchKey = "album:first",
                previousState = "FAILED",
                requestedBatchKey = "playlist:second"
            )
        )
    }

    @Test
    fun `a settled batch releases the track to a new batch`() {
        assertFalse(
            retainsExistingBatchMembership(
                previousBatchKey = "album:first",
                previousState = "SUCCEEDED",
                requestedBatchKey = "playlist:second"
            )
        )
        assertFalse(
            retainsExistingBatchMembership(
                previousBatchKey = "album:first",
                previousState = "CANCELLED",
                requestedBatchKey = "playlist:second"
            )
        )
    }

    @Test
    fun `retrying the same batch is not treated as a competing membership`() {
        assertFalse(
            retainsExistingBatchMembership(
                previousBatchKey = "album:first",
                previousState = "FAILED",
                requestedBatchKey = "album:first"
            )
        )
    }

    @Test
    fun `a single track download does not clear an existing batch membership`() {
        assertTrue(
            retainsExistingBatchMembership(
                previousBatchKey = "album:first",
                previousState = "SUCCEEDED",
                requestedBatchKey = ""
            )
        )
        assertFalse(
            retainsExistingBatchMembership(
                previousBatchKey = "",
                previousState = "QUEUED",
                requestedBatchKey = ""
            )
        )
    }

    private fun batchWith(state: BatchDownloadState, key: String) = BatchDownload(
        key = key,
        kind = BatchDownloadKind.Album,
        title = key,
        artworkUrl = "",
        total = 4,
        completed = 1,
        failed = 0,
        active = 1,
        progress = 25,
        state = state
    )

    private fun batch(total: Int, completed: Int, failed: Int, active: Int) = BatchDownload(
        key = "album:test",
        kind = BatchDownloadKind.Album,
        title = "Test",
        artworkUrl = "",
        total = total,
        completed = completed,
        failed = failed,
        active = active,
        progress = batchDownloadProgress(total, completed * 100),
        state = batchDownloadState(total, completed, failed, active, completed * 100)
    )
}
