package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.MoodEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OnboardingPortraitDistinctnessTest {
    @Test
    fun tasteSkipsArtistPortraitsAlreadyShownByAnotherTaste() {
        val shown = mapOf("hits" to listOf("artist-a", "artist-b"))

        assertEquals(
            listOf("artist-c"),
            distinctOnboardingPortraits(listOf("artist-a", "artist-c", "artist-b"), shown)
        )
    }

    @Test
    fun tasteKeepsItsPortraitsWhenNoUnusedAlternativeExists() {
        val shown = mapOf("hits" to listOf("artist-a"))

        assertEquals(listOf("artist-a"), distinctOnboardingPortraits(listOf("artist-a"), shown))
    }

    @Test
    fun hitsAndLocalTastesNeverShareTheSameSearchQuery() {
        val engine = MoodEngine()
        LevyraLanguageCatalog.languages.forEach { language ->
            val tastes = engine.tastesForLanguage(language.code).associateBy { it.id }
            assertNotEquals(
                "Hits and local tastes share a query for ${language.code}",
                tastes.getValue("hits").query,
                tastes.getValue("italiana").query
            )
            val moods = LevyraContentLocales.forLanguage(language.code).moodQueries
            if ("hits" in moods && "italia" in moods) {
                assertNotEquals("Hits and local moods share a query for ${language.code}", moods["hits"], moods["italia"])
            }
        }
    }
}
