package com.luc4n3x.levyra.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreProjectionTest {
    @Test
    fun samplesRuntimeStateChangesProjection() {
        val baseline = exploreProjection(LevyraUiState())

        assertFalse(baseline.isResolving)
        assertFalse(baseline.isVideoMode)
        assertFalse(baseline.isSamplesLoading)
        assertFalse(baseline.samplesLoadFailed)

        assertTrue(exploreProjection(LevyraUiState(isResolving = true)).isResolving)
        assertTrue(exploreProjection(LevyraUiState(isVideoMode = true)).isVideoMode)
        assertTrue(exploreProjection(LevyraUiState(isSamplesLoading = true)).isSamplesLoading)
        assertTrue(exploreProjection(LevyraUiState(samplesLoadFailed = true)).samplesLoadFailed)
    }
}
