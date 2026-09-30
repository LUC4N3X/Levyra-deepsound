package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSoundtrackLocalizationTest {

    private val codes = LevyraLanguageCatalog.languages.map { it.code }

    @Test
    fun everySupportedLanguageHasATranslatedSoundtrackBundle() {
        val english = homeSoundtrackLocalizationEntries("en")
        assertEquals(homeSoundtrackKeys, english.keys)
        assertEquals(37, codes.size)
        codes.forEach { code ->
            val entries = homeSoundtrackLocalizationEntries(code)
            assertEquals("Missing soundtrack keys for $code", homeSoundtrackKeys, entries.keys)
            assertTrue("Blank soundtrack string for $code", entries.values.all(String::isNotBlank))
            if (code != "en") {
                assertFalse(
                    "Soundtrack title falls back to English for $code",
                    entries.getValue("homeSoundtrackTitle") == english.getValue("homeSoundtrackTitle")
                )
            }
            assertTrue("$code lost {artists}", entries.getValue("homeSoundtrackLeadArtists").contains("{artists}"))
        }
    }

    @Test
    fun leadListsUpToThreeDistinctArtists() {
        val italian = LevyraStrings.forCode("it")
        assertEquals(
            "Inizia con Capo Plaza, Emis Killa e Sfera Ebbasta",
            italian.homeSoundtrackLead(listOf("Capo Plaza", "Emis Killa", "capo plaza", "Sfera Ebbasta", "Geolier"))
        )
        assertEquals("Inizia con Sfera Ebbasta", italian.homeSoundtrackLead(listOf(" Sfera Ebbasta ", "")))
        assertEquals("Una radio che cresce a ogni tuo ascolto.", italian.homeSoundtrackLead(emptyList()))
        assertEquals("Starts with A and B", LevyraStrings.forCode("en").homeSoundtrackLead(listOf("A", "B")))
    }

    @Test
    fun formattedLeadFillsThePlaceholderInEveryLanguage() {
        codes.forEach { code ->
            val strings = LevyraStrings.forCode(code)
            val lead = strings.homeSoundtrackLead(listOf("Alpha", "Beta", "Gamma"))
            assertFalse("$code left a placeholder", lead.contains('{'))
            assertTrue("$code dropped an artist", listOf("Alpha", "Beta", "Gamma").all(lead::contains))
            assertTrue(strings.homeSoundtrackRadio.isNotBlank())
            assertTrue(strings.homeSoundtrackTitle.isNotBlank())
        }
    }
}
