package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SpotifyBrowseLocalizationTest {
    @Test
    fun browseKeepsLocalizedZonesAndProviderMoodsInEverySupportedLanguage() {
        LevyraStrings.all().forEach { strings ->
            val zones = ExploreCatalog.getZones(strings)
            val moods = listOf(ExploreCategory(strings.collectionWorkout, "workout", strings.exploreMoodSection))
            val entries = spotifyBrowseEntries(zones, moods, emptyMap(), {}, {})

            assertEquals(strings.code, zones.map { it.label } + moods.map { it.title }, entries.map { it.label })
            assertTrue(strings.code, entries.all { it.label.isNotBlank() })
            assertTrue(strings.code, LevyraSpotifySkinStrings.forCode(strings.code).searchPrompt.isNotBlank())
            assertTrue(strings.code, LevyraSpotifySkinStrings.forCode(strings.code).discoverNew.isNotBlank())
            if (strings.code != "en") assertNotEquals(
                strings.code,
                LevyraSpotifySkinStrings.forCode("en").searchPrompt,
                LevyraSpotifySkinStrings.forCode(strings.code).searchPrompt
            )
        }
    }

    @Test
    fun providerCardsUseOnlyArtworkBelongingToTheirCategory() {
        val categories = listOf(ExploreCategory("Gospel", "gospel"), ExploreCategory("Country", "country"))
        val entries = spotifyBrowseEntries(emptyList(), categories, mapOf("gospel" to "https://example.com/gospel.jpg"), {}, {})

        assertEquals("https://example.com/gospel.jpg", entries[0].artworkUrl)
        assertEquals("", entries[1].artworkUrl)
    }
}
