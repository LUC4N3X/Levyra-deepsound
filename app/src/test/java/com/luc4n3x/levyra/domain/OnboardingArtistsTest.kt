package com.luc4n3x.levyra.domain

import com.luc4n3x.levyra.ui.i18n.LevyraOnboardingArtistStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingArtistsTest {
    @Test
    fun `each language offers local artists followed by unique international artists`() {
        LevyraLanguageCatalog.selectableLanguages.forEach { language ->
            val sections = OnboardingArtists.sections(language.code)
            val artists = sections.flatMap { it.artists }
            assertEquals(language.code, "italiana", sections.first().id)
            assertTrue(language.code, sections.first().artists.size >= OnboardingArtists.InitialSectionSize)
            assertTrue(language.code, sections.all { it.label.isNotBlank() && it.artists.isNotEmpty() })
            assertEquals(language.code, artists.size, artists.map { artistIdentityKey(it.name) }.distinct().size)
        }
        assertEquals("Sfera Ebbasta", OnboardingArtists.sections("it").first().artists.first().name)
        assertEquals("Gazo", OnboardingArtists.sections("fr").first().artists.first().name)
        assertEquals("Anitta", OnboardingArtists.sections("pt-BR").first().artists.first().name)
        assertNotEquals(OnboardingArtists.sections("pt"), OnboardingArtists.sections("pt-BR"))
    }

    @Test
    fun `artist choices lead home queries and preserve legacy genre preferences`() {
        val artists = linkedSetOf("artist:Lazza", "artist:Radiohead", "artist:SZA")
        val engine = MoodEngine()
        assertEquals(listOf("Lazza", "Radiohead", "SZA"), engine.queriesForTastes(artists, "it"))
        assertEquals(listOf("Lazza", "Radiohead", "SZA"), LevyraLocalizedDiscovery.homeBoostQueries("it", artists).take(3))
        assertEquals(listOf("Lazza", "Radiohead", "SZA"), engine.queriesForTastes(artists, "fr"))
        assertEquals(listOf(LevyraContentLocales.forLanguage("it").queryForTaste("rock")), engine.queriesForTastes(setOf("rock"), "it"))
        assertEquals(LevyraContentLocales.forLanguage("it").homeQueries, engine.queriesForTastes(emptySet(), "it"))
        assertEquals(listOf("Lazza", LevyraContentLocales.forLanguage("it").queryForTaste("rock")), engine.queriesForTastes(setOf("artist:Lazza", "rock"), "it"))
    }

    @Test
    fun `stored artist names ignore legacy ids blank entries and duplicates`() {
        assertEquals(listOf("SZA"), OnboardingArtists.selectedNames(setOf("rock", "artist:", "artist:SZA", "artist:sza")))
        assertTrue(OnboardingArtists.selectedNames(setOf("artist:${"a".repeat(121)}")).isEmpty())
    }

    @Test
    fun `onboarding artist copy is localized for every selectable language`() {
        val english = LevyraOnboardingArtistStrings.forCode("en")
        LevyraLanguageCatalog.selectableLanguages.forEach { language ->
            val copy = LevyraOnboardingArtistStrings.forCode(language.code)
            assertTrue(language.code, listOf(copy.title, copy.body, copy.empty).all(String::isNotBlank))
            if (language.code != "en") assertNotEquals(language.code, english.title, copy.title)
            assertFalse(language.code, copy.body.contains("genre", ignoreCase = true))
        }
    }
}
