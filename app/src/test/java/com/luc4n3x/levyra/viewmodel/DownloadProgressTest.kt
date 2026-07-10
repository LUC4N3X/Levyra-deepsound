package com.luc4n3x.levyra.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadProgressTest {
    @Test
    fun progressNeverMovesBackward() {
        assertEquals(36, monotonicDownloadProgress(current = 36, incoming = 12))
    }

    @Test
    fun progressStillMovesForward() {
        assertEquals(48, monotonicDownloadProgress(current = 36, incoming = 48))
    }

    @Test
    fun progressStaysInsideVisibleDownloadRange() {
        assertEquals(1, monotonicDownloadProgress(current = null, incoming = 0))
        assertEquals(99, monotonicDownloadProgress(current = 98, incoming = 100))
    }
}
