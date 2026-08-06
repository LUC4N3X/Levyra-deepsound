package com.luc4n3x.levyra.viewmodel

import org.junit.Assert.assertNotEquals
import org.junit.Test

class ExploreProjectionTest {
    @Test
    fun samplesRuntimeStateChangesProjection() {
        val base = LevyraUiState()
        val baseline = exploreProjection(base)

        assertNotEquals(baseline, exploreProjection(base.copy(isResolving = true)))
        assertNotEquals(baseline, exploreProjection(base.copy(isVideoMode = true)))
        assertNotEquals(baseline, exploreProjection(base.copy(isSamplesLoading = true)))
        assertNotEquals(baseline, exploreProjection(base.copy(samplesLoadFailed = true)))
    }
}
