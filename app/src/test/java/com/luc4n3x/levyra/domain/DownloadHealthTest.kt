package com.luc4n3x.levyra.domain

import org.junit.Assert.*
import org.junit.Test

class DownloadHealthTest {
    @Test fun readableAudioIsHealthyWithoutClaimingDurationWhenUnknown() {
        assertEquals(DownloadHealth.Healthy, downloadAudioHealth(1_000L, true, 0L, 180_000L))
        assertEquals(DownloadHealth.Healthy, downloadAudioHealth(-1L, true, 180_000L, 180_000L))
    }

    @Test fun emptyAndNonAudioAreDistinctFailures() {
        assertEquals(DownloadHealth.Empty, downloadAudioHealth(0L, false, 0L, 0L))
        assertEquals(DownloadHealth.Unreadable, downloadAudioHealth(1_000L, false, 180_000L, 180_000L))
    }

    @Test fun durationComparisonToleratesSmallMetadataDifferences() {
        assertEquals(DownloadHealth.Healthy, downloadAudioHealth(1_000L, true, 175_000L, 180_000L))
        assertEquals(DownloadHealth.Healthy, downloadAudioHealth(1_000L, true, 25_000L, 30_000L))
        assertEquals(DownloadHealth.Incomplete, downloadAudioHealth(1_000L, true, 60_000L, 180_000L))
    }

    @Test fun permissionAndUnknownResultsCannotTriggerReplacement() {
        assertFalse(canRepairDownload("a", DownloadHealth.AccessDenied))
        assertFalse(canRepairDownload("a", DownloadHealth.Unavailable))
        assertFalse(canRepairDownload("a", DownloadHealth.Healthy))
        assertFalse(canRepairDownload("", DownloadHealth.Missing))
        assertTrue(canRepairDownload("a", DownloadHealth.Missing))
    }
}
