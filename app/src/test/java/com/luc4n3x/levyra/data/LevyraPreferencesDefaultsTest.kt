package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.LevyraCanvasSource
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.LevyraVisualPerformance
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.PlayerColorMode
import com.luc4n3x.levyra.domain.PlayerDoubleTapAction
import com.luc4n3x.levyra.domain.PlayerLongPressAction
import com.luc4n3x.levyra.domain.PlayerVerticalSwipeAction
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.ReactiveAuraPreset
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraPreferencesDefaultsTest {
    @Test
    fun prismBackupPreservesVariantsAndOldBackupsUseSafeDefaults() {
        val prism = com.luc4n3x.levyra.domain.PrismSettings(
            appearance = com.luc4n3x.levyra.domain.PrismAppearance.Automatic,
            playerLayout = com.luc4n3x.levyra.domain.PrismPlayerLayout.Focus,
            translucency = com.luc4n3x.levyra.domain.PrismTranslucency.Reduced
        )
        val original = LevyraInterfaceSettings(skin = com.luc4n3x.levyra.domain.LevyraSkin.Torque, prism = prism, torque = com.luc4n3x.levyra.domain.TorqueSettings(com.luc4n3x.levyra.domain.TorqueAccent.Red), ivory = com.luc4n3x.levyra.domain.IvorySettings(com.luc4n3x.levyra.domain.IvoryAppearance.Dark))
        assertEquals(original, backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(original)))
        val spotify = LevyraInterfaceSettings(skin = com.luc4n3x.levyra.domain.LevyraSkin.SpotifyStyle)
        assertEquals(spotify, backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(spotify)))
        assertEquals(com.luc4n3x.levyra.domain.PrismSettings(), backupInterfaceSettingsFromJson(JSONObject()).prism)
        assertEquals(com.luc4n3x.levyra.domain.PrismSettings(), backupInterfaceSettingsFromJson(JSONObject().put("prismAppearance", "unknown")).prism)
    }

    @Test
    fun skinBackupIsBackwardCompatibleAndPreservesPersonalSettings() {
        val original = LevyraInterfaceSettings(
            skin = com.luc4n3x.levyra.domain.LevyraSkin.Nocturne78,
            pureBlack = true,
            playerColorMode = PlayerColorMode.Custom,
            playerColorAccent = 0xFF1256AB.toInt()
        )
        assertEquals(original, backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(original)))
        assertEquals(com.luc4n3x.levyra.domain.LevyraSkin.Default, backupInterfaceSettingsFromJson(JSONObject()).skin)
        assertEquals(com.luc4n3x.levyra.domain.LevyraSkin.Default, LevyraInterfaceSettings().skin)
    }

    @Test
    fun sponsorBlockIsEnabledByDefault() {
        assertTrue(DEFAULT_SPONSORBLOCK_ENABLED)
        assertTrue(LevyraUiState().sponsorBlockEnabled)
    }

    @Test
    fun liquidGlassDefaultsToEnabledEverywhere() {
        assertTrue(LevyraInterfaceSettings().liquidGlassEnabled)
        assertTrue(LevyraUiState().interfaceSettings.liquidGlassEnabled)
        assertTrue(backupInterfaceSettingsFromJson(JSONObject()).liquidGlassEnabled)
        assertTrue(backupInterfaceSettingsFromJson(null).liquidGlassEnabled)
    }

    @Test
    fun backupRoundTripPreservesLiquidGlassChoice() {
        listOf(true, false).forEach { enabled ->
            assertEquals(
                enabled,
                backupInterfaceSettingsFromJson(
                    backupInterfaceSettingsToJson(LevyraInterfaceSettings(liquidGlassEnabled = enabled))
                ).liquidGlassEnabled
            )
        }
    }

    @Test
    fun expressiveGlassStyleDefaultsAndBackupRoundTrip() {
        val defaults = backupInterfaceSettingsFromJson(JSONObject())
        assertEquals(LevyraInterfaceSettings.DEFAULT_GLASS_LEVEL, defaults.glassBlur)
        assertEquals(LevyraInterfaceSettings.DEFAULT_GLASS_LEVEL, defaults.glassTransparency)
        assertEquals(LevyraInterfaceSettings.DEFAULT_GLASS_LEVEL, defaults.glassTint)
        assertTrue(defaults.glassRefraction)

        val custom = LevyraInterfaceSettings(
            glassBlur = 80,
            glassTransparency = 20,
            glassTint = 65,
            glassRefraction = false
        )
        val restored = backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(custom))
        assertEquals(80, restored.glassBlur)
        assertEquals(20, restored.glassTransparency)
        assertEquals(65, restored.glassTint)
        assertFalse(restored.glassRefraction)
        assertEquals(100, LevyraInterfaceSettings(glassBlur = 140).normalized().glassBlur)
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
    fun visualPerformanceDefaultsToFullEverywhere() {
        assertEquals(LevyraVisualPerformance.Full, LevyraInterfaceSettings().visualPerformance)
        assertEquals(LevyraVisualPerformance.Full, LevyraUiState().interfaceSettings.visualPerformance)
        assertEquals(LevyraVisualPerformance.Full, LevyraVisualPerformance.from(""))
        assertEquals(LevyraVisualPerformance.Full, LevyraVisualPerformance.from("unknown"))
        assertEquals(LevyraVisualPerformance.Full, backupInterfaceSettingsFromJson(JSONObject()).visualPerformance)
        assertEquals(LevyraVisualPerformance.Full, backupInterfaceSettingsFromJson(null).visualPerformance)
    }

    @Test
    fun backupRoundTripPreservesVisualPerformance() {
        LevyraVisualPerformance.entries.forEach { mode ->
            val restored = backupInterfaceSettingsFromJson(
                backupInterfaceSettingsToJson(LevyraInterfaceSettings(visualPerformance = mode))
            )
            assertEquals(mode, restored.visualPerformance)
        }
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
            keepScreenOnWhilePlaying = true,
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
        assertTrue(restored.keepScreenOnWhilePlaying)
        assertFalse(backupInterfaceSettingsFromJson(JSONObject()).keepScreenOnWhilePlaying)
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
    fun backupRoundTripPreservesReactiveAuraSettings() {
        val original = LevyraInterfaceSettings(
            playerBackground = PlayerBackgroundMode.ReactiveAura,
            reactiveAuraPreset = ReactiveAuraPreset.Pulse,
            reactiveAuraIntensity = 75
        )

        val restored = backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(original))

        assertEquals(PlayerBackgroundMode.ReactiveAura, restored.playerBackground)
        assertEquals(ReactiveAuraPreset.Pulse, restored.reactiveAuraPreset)
        assertEquals(75, restored.reactiveAuraIntensity)
    }

    @Test
    fun defaultReactiveAuraSettingsAreFlowAnd50() {
        val defaults = backupInterfaceSettingsFromJson(JSONObject())
        assertEquals(ReactiveAuraPreset.Flow, defaults.reactiveAuraPreset)
        assertEquals(50, defaults.reactiveAuraIntensity)
    }

    @Test
    fun defaultPlayerVisualModeIsFlow() {
        assertEquals(PlayerVisualMode.Flow, LevyraInterfaceSettings().playerVisualMode)
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

    @Test
    fun playerColorsDefaultToDynamicAndRoundTripThroughBackup() {
        assertEquals(PlayerColorMode.Dynamic, LevyraInterfaceSettings().playerColorMode)
        assertEquals(PlayerColorMode.Dynamic, backupInterfaceSettingsFromJson(JSONObject()).playerColorMode)
        assertEquals(PlayerColorMode.Dynamic, PlayerColorMode.from("unknown"))
        val original = LevyraInterfaceSettings(playerColorMode = PlayerColorMode.Custom, playerColorAccent = 0x0033AAFF)
        val restored = backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(original))
        assertEquals(PlayerColorMode.Custom, restored.playerColorMode)
        assertEquals(0xFF33AAFF.toInt(), restored.playerColorAccent)
        val theme = backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(LevyraInterfaceSettings(playerColorMode = PlayerColorMode.Theme)))
        assertEquals(PlayerColorMode.Theme, theme.playerColorMode)
    }
}
