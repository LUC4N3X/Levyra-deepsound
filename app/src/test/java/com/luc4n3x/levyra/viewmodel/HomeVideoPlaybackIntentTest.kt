package com.luc4n3x.levyra.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeVideoPlaybackIntentTest {
    @Test
    fun explicitVideoPlaybackModeEnablesVideoAndClearsPendingSwitch() {
        val state = LevyraUiState(isVideoMode = false, pendingVideoMode = false)

        val updated = state.withExplicitPlaybackMode(videoMode = true)

        assertTrue(updated.isVideoMode)
        assertNull(updated.pendingVideoMode)
    }

    @Test
    fun explicitAudioPlaybackModeLeavesVideoAndClearsPendingSwitch() {
        val state = LevyraUiState(isVideoMode = true, pendingVideoMode = true)

        val updated = state.withExplicitPlaybackMode(videoMode = false)

        assertFalse(updated.isVideoMode)
        assertNull(updated.pendingVideoMode)
    }

    @Test
    fun settledPlaybackModeDoesNotPublishANewState() {
        val videoState = LevyraUiState(isVideoMode = true, pendingVideoMode = null)
        val audioState = LevyraUiState(isVideoMode = false, pendingVideoMode = null)

        assertSame(videoState, videoState.withExplicitPlaybackMode(videoMode = true))
        assertSame(audioState, audioState.withExplicitPlaybackMode(videoMode = false))
    }
}
