package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDownloadStageTest {

    @Test
    fun mapsWorkManagerAndEngineStates() {
        assertEquals(OfflineDownloadStage.Queued, offlineDownloadStageOf("QUEUED"))
        assertEquals(OfflineDownloadStage.Queued, offlineDownloadStageOf("enqueued"))
        assertEquals(OfflineDownloadStage.Downloading, offlineDownloadStageOf("RUNNING"))
        assertEquals(OfflineDownloadStage.Paused, offlineDownloadStageOf(" paused "))
        assertEquals(OfflineDownloadStage.Retrying, offlineDownloadStageOf("RETRYING"))
        assertEquals(OfflineDownloadStage.Failed, offlineDownloadStageOf("FAILED"))
        assertEquals(OfflineDownloadStage.Completed, offlineDownloadStageOf("SUCCEEDED"))
        assertEquals(OfflineDownloadStage.Cancelled, offlineDownloadStageOf("CANCELLED"))
    }

    @Test
    fun unknownStateFallsBackToQueued() {
        assertEquals(OfflineDownloadStage.Queued, offlineDownloadStageOf("something-else"))
    }

    @Test
    fun activeAndProgressFlagsMatchUiExpectations() {
        assertTrue(OfflineDownloadStage.Downloading.isActive)
        assertTrue(OfflineDownloadStage.Queued.isActive)
        assertFalse(OfflineDownloadStage.Completed.isActive)
        assertFalse(OfflineDownloadStage.Failed.isActive)
        assertTrue(OfflineDownloadStage.Downloading.showsProgress)
        assertFalse(OfflineDownloadStage.Queued.showsProgress)
        assertFalse(OfflineDownloadStage.Completed.showsProgress)
    }
}
