package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LevyraKeepScreenOnTest {
    private fun uiState(
        setting: Boolean = false,
        playing: Boolean = false,
        ambient: Boolean = false
    ) = LevyraUiState(
        interfaceSettings = LevyraInterfaceSettings(keepScreenOnWhilePlaying = setting),
        isPlaying = playing,
        showAmbient = ambient
    )

    @Test
    fun settingIsOffByDefaultAndPlaybackAloneHoldsNothing() {
        assertFalse(LevyraInterfaceSettings().keepScreenOnWhilePlaying)
        assertFalse(LevyraUiState().shouldKeepScreenOn())
        assertFalse(uiState(playing = true).shouldKeepScreenOn())
    }

    @Test
    fun settingHoldsOnlyWhilePlaying() {
        assertTrue(uiState(setting = true, playing = true).shouldKeepScreenOn())
        assertFalse(uiState(setting = true, playing = false).shouldKeepScreenOn())
    }

    @Test
    fun pausingDuringAmbientDoesNotReleaseAmbientHold() {
        assertTrue(uiState(setting = true, playing = true, ambient = true).shouldKeepScreenOn())
        assertTrue(uiState(setting = true, playing = false, ambient = true).shouldKeepScreenOn())
    }

    @Test
    fun ambientHoldsIndependentlyFromTheSetting() {
        assertTrue(uiState(setting = false, playing = false, ambient = true).shouldKeepScreenOn())
        assertTrue(uiState(setting = false, playing = true, ambient = true).shouldKeepScreenOn())
    }

    @Test
    fun closingAmbientKeepsHoldOnlyWhenSettingStillApplies() {
        assertTrue(uiState(setting = true, playing = true, ambient = false).shouldKeepScreenOn())
        assertFalse(uiState(setting = false, playing = true, ambient = false).shouldKeepScreenOn())
    }

    @Test
    fun windowFlagHasExactlyOneOwner() {
        val root = listOf(
            File("src/main/java/com/luc4n3x/levyra"),
            File("app/src/main/java/com/luc4n3x/levyra")
        ).first { it.isDirectory }
        val writers = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && "FLAG_KEEP_SCREEN_ON" in it.readText() }
            .map { it.name }
            .toList()
        assertEquals(listOf("LevyraKeepScreenOn.kt"), writers)
    }
}
