package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.viewmodel.LevyraUiState
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraPreferencesDefaultsTest {
    @Test
    fun sponsorBlockIsEnabledByDefault() {
        assertTrue(DEFAULT_SPONSORBLOCK_ENABLED)
        assertTrue(LevyraUiState().sponsorBlockEnabled)
    }
}
