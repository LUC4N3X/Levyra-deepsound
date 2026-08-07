package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SamplesLoadSurfaceStateTest {
    @Test
    fun onlyActiveLoadingShowsSpinnerState() {
        assertEquals(SamplesLoadSurfaceState.Loading, samplesLoadSurfaceState(isLoading = true, loadFailed = false))
        assertEquals(SamplesLoadSurfaceState.Error, samplesLoadSurfaceState(isLoading = false, loadFailed = true))
        assertEquals(SamplesLoadSurfaceState.Empty, samplesLoadSurfaceState(isLoading = false, loadFailed = false))
    }
}
