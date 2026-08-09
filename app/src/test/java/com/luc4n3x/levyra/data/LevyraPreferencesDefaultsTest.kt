package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraPreferencesDefaultsTest {
    @Test
    fun sponsorBlockIsEnabledByDefault() {
        assertTrue(DEFAULT_SPONSORBLOCK_ENABLED)
        assertTrue(LevyraUiState().sponsorBlockEnabled)
    }

    @Test
    fun backupRoundTripPreservesPreampAndLimiterSettings() {
        val restored = backupAudioSettingsFromJson(
            backupAudioSettingsToJson(
                LevyraAudioSettings(preampDb = -4.5f, limiterEnabled = false)
            )
        )

        assertEquals(-4.5f, restored.preampDb, 0f)
        assertFalse(restored.limiterEnabled)
    }

    @Test
    fun legacyBackupKeepsPreampAndLimiterDefaults() {
        val restored = backupAudioSettingsFromJson(JSONObject())

        assertEquals(0f, restored.preampDb, 0f)
        assertTrue(restored.limiterEnabled)
    }
}
