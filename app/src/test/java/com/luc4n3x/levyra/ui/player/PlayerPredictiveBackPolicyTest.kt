package com.luc4n3x.levyra.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPredictiveBackPolicyTest {

    private fun isPredictiveBackAllowed(
        animationsEnabled: Boolean,
        selectedTabIsPlayer: Boolean,
        showQueue: Boolean = false,
        showLyrics: Boolean = false,
        showSettings: Boolean = false,
        showAlbum: Boolean = false,
        showArtist: Boolean = false,
        showAudioQualityPanel: Boolean = false,
        commentsVisible: Boolean = false,
        showYourSound: Boolean = false,
        showJam: Boolean = false,
        showRecognition: Boolean = false,
        hasOpenPlaylist: Boolean = false,
        hasSharedMediaPreview: Boolean = false,
        showDownloadsFolder: Boolean = false,
        showLanguageRestartDialog: Boolean = false
    ): Boolean {
        return animationsEnabled &&
            selectedTabIsPlayer &&
            !showQueue &&
            !showLyrics &&
            !showSettings &&
            !showAlbum &&
            !showArtist &&
            !showAudioQualityPanel &&
            !commentsVisible &&
            !showYourSound &&
            !showJam &&
            !showRecognition &&
            !hasOpenPlaylist &&
            !hasSharedMediaPreview &&
            !showDownloadsFolder &&
            !showLanguageRestartDialog
    }

    @Test
    fun predictiveBackAllowedWhenOnlyOnPlayerTabWithNoOverlays() {
        assertTrue(isPredictiveBackAllowed(animationsEnabled = true, selectedTabIsPlayer = true))
    }

    @Test
    fun predictiveBackDisabledWhenJamOrRecognitionIsVisible() {
        assertFalse(
            isPredictiveBackAllowed(
                animationsEnabled = true,
                selectedTabIsPlayer = true,
                showJam = true
            )
        )
        assertFalse(
            isPredictiveBackAllowed(
                animationsEnabled = true,
                selectedTabIsPlayer = true,
                showRecognition = true
            )
        )
    }

    @Test
    fun predictiveBackDisabledWhenOtherSheetsOrDialogsAreOpen() {
        assertFalse(isPredictiveBackAllowed(animationsEnabled = true, selectedTabIsPlayer = true, showQueue = true))
        assertFalse(isPredictiveBackAllowed(animationsEnabled = true, selectedTabIsPlayer = true, showSettings = true))
        assertFalse(isPredictiveBackAllowed(animationsEnabled = true, selectedTabIsPlayer = true, showYourSound = true))
    }
}
