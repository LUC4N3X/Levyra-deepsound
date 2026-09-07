package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.LevyraCanvasSource
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun legacyBackupPreservesCanvasPreferenceWhenVisualModeMissing() {
        val restoredWithCanvas = backupInterfaceSettingsFromJson(
            JSONObject(),
            legacyVisualMode = PlayerVisualMode.CanvasImmersive
        )
        assertEquals(PlayerVisualMode.CanvasImmersive, restoredWithCanvas.playerVisualMode)

        val restoredWithoutCanvas = backupInterfaceSettingsFromJson(
            JSONObject(),
            legacyVisualMode = PlayerVisualMode.Artwork
        )
        assertEquals(PlayerVisualMode.Artwork, restoredWithoutCanvas.playerVisualMode)
    }

    @Test
    fun backupRoundTripPreservesPlayerVisualAndCanvasSettings() {
        val original = LevyraInterfaceSettings(
            canvasQuality = LevyraCanvasQuality.High,
            canvasSource = LevyraCanvasSource.Tidal,
            enhanceVideoMetadata = true,
            playerVisualMode = PlayerVisualMode.CanvasCard,
            playerBackground = PlayerBackgroundMode.Blur
        )
        val restored = backupInterfaceSettingsFromJson(
            backupInterfaceSettingsToJson(original),
            legacyVisualMode = PlayerVisualMode.Artwork
        )

        assertEquals(LevyraCanvasQuality.High, restored.canvasQuality)
        assertEquals(LevyraCanvasSource.Tidal, restored.canvasSource)
        assertTrue(restored.enhanceVideoMetadata)
        assertEquals(PlayerVisualMode.CanvasCard, restored.playerVisualMode)
        assertEquals(PlayerBackgroundMode.Blur, restored.playerBackground)
    }

    @Test
    fun legacyPureBlackMapsToPureBlackBackgroundWhenUnset() {
        val restored = backupInterfaceSettingsFromJson(JSONObject().put("pureBlack", true))
        assertEquals(PlayerBackgroundMode.PureBlack, restored.playerBackground)
    }

    @Test
    fun defaultPlayerVisualModeIsArtwork() {
        assertEquals(PlayerVisualMode.Artwork, LevyraInterfaceSettings().playerVisualMode)
    }

    @Test
    fun legacyBackupWithoutVisualModeDefaultsToCanvasCard() {
        assertEquals(PlayerVisualMode.CanvasCard, backupInterfaceSettingsFromJson(JSONObject()).playerVisualMode)
    }
}
