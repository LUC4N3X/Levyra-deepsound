package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifyHomeStrings
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.searchProjection
import org.junit.Assert.*
import org.junit.Test

class SpotifyHomePagesTest {
    @Test
    fun radioRemainsAvailableWithoutSavedStations() {
        assertTrue(SpotifyHomeFilter.Radio in SpotifyHomeFilter.available(emptyList()))
    }

    @Test
    fun nativeVideoModeInvalidatesSearchPreviewProjection() {
        val state = LevyraUiState()
        assertNotEquals(searchProjection(state), searchProjection(state.copy(isVideoMode = true)))
    }

    @Test
    fun homePromptsHaveCopyForEverySupportedLanguage() {
        val codes = listOf("en", "it", "es", "fr", "de", "pt", "nl", "pl", "ru", "uk", "ja", "ko", "zh", "zh-Hant", "ar", "ro", "sq", "bg", "el", "sv", "da", "cs", "sk", "bn", "kk", "sr", "hr", "hu", "fi", "et", "nb", "ca", "tr", "fa", "hi", "id", "ms", "vi", "th", "fil", "he")
        codes.forEach { code ->
            val copy = LevyraSpotifyHomeStrings.forCode(code)
            assertTrue(code, listOf(copy.popularRadio, copy.popularAlbums, copy.popularArtists, copy.latestReleases, copy.betterRecommendations, copy.chooseDescription, copy.chooseArtists).all(String::isNotBlank))
        }
    }
}
