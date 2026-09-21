package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.LevyraCanvasSource
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.PlayerDoubleTapAction
import com.luc4n3x.levyra.domain.PlayerLongPressAction
import com.luc4n3x.levyra.domain.PlayerVerticalSwipeAction
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
            releaseNotificationsEnabled = true,
            canvasQuality = LevyraCanvasQuality.High,
            canvasSource = LevyraCanvasSource.Tidal,
            motionArtworkWifiOnly = true,
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
        assertTrue(restored.motionArtworkWifiOnly)
        assertTrue(restored.enhanceVideoMetadata)
        assertTrue(restored.releaseNotificationsEnabled)
        assertEquals(PlayerVisualMode.CanvasCard, restored.playerVisualMode)
        assertEquals(PlayerBackgroundMode.Blur, restored.playerBackground)
    }

    @Test
    fun backupRoundTripPreservesPlayerGestureActions() {
        val original = LevyraInterfaceSettings(
            swipeTrackChangeEnabled = false,
            doubleTapAction = PlayerDoubleTapAction.Favorite,
            longPressAction = PlayerLongPressAction.Lyrics,
            verticalSwipeAction = PlayerVerticalSwipeAction.Volume
        )

        val restored = backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(original))

        assertFalse(restored.swipeTrackChangeEnabled)
        assertEquals(PlayerDoubleTapAction.Favorite, restored.doubleTapAction)
        assertEquals(PlayerLongPressAction.Lyrics, restored.longPressAction)
        assertEquals(PlayerVerticalSwipeAction.Volume, restored.verticalSwipeAction)
    }

    @Test
    fun legacyBackupKeepsProfessionalGestureDefaults() {
        val restored = backupInterfaceSettingsFromJson(JSONObject())

        assertTrue(restored.swipeTrackChangeEnabled)
        assertEquals(PlayerDoubleTapAction.Seek, restored.doubleTapAction)
        assertEquals(PlayerLongPressAction.Speed, restored.longPressAction)
        assertEquals(PlayerVerticalSwipeAction.BrightnessAndVolume, restored.verticalSwipeAction)
    }

    @Test
    fun legacyPureBlackMapsToPureBlackBackgroundWhenUnset() {
        val restored = backupInterfaceSettingsFromJson(JSONObject().put("pureBlack", true))
        assertEquals(PlayerBackgroundMode.PureBlack, restored.playerBackground)
    }

    @Test
    fun defaultPlayerVisualModeIsCanvasImmersive() {
        assertEquals(PlayerVisualMode.CanvasImmersive, LevyraInterfaceSettings().playerVisualMode)
        assertFalse(LevyraInterfaceSettings().motionArtworkWifiOnly)
    }

    @Test
    fun legacyBackupWithoutMotionArtworkWifiOnlyDefaultsToFalse() {
        assertFalse(backupInterfaceSettingsFromJson(JSONObject()).motionArtworkWifiOnly)
        assertFalse(backupInterfaceSettingsFromJson(JSONObject()).releaseNotificationsEnabled)
    }

    @Test
    fun legacyBackupWithoutVisualModeDefaultsToCanvasCard() {
        assertEquals(PlayerVisualMode.CanvasCard, backupInterfaceSettingsFromJson(JSONObject()).playerVisualMode)
    }
}
