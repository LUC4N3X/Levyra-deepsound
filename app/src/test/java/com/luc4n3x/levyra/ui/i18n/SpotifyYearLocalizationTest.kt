package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyYearLocalizationTest {

    @Test
    fun everySelectableLanguageShipsSpotifyYearCopy() {
        val selectable = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()
        assertTrue((selectable - spotifyYearLocalizationCodes()).toString(), spotifyYearLocalizationCodes().containsAll(selectable))
        selectable.forEach { code ->
            val copy = spotifyYearCopy(code)
            assertTrue(code, copy.topOfYear.contains("%d") && copy.topTracks.isNotBlank())
        }
    }
}
