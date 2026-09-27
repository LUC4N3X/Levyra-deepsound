package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraMixLabLocalizationTest {

    private val codes = LevyraLanguageCatalog.languages.map { it.code }

    @Test
    fun everySupportedLanguageHasAMixLabBundleWithAllKeys() {
        assertTrue("en" in codes)
        val english = mixLabLocalizationEntries("en")
        assertEquals(mixLabKeys, english.keys)
        codes.forEach { code ->
            val entries = mixLabLocalizationEntries(code)
            assertEquals("Missing Mix Lab keys for $code", mixLabKeys, entries.keys)
        }
    }

    @Test
    fun noMixLabStringIsBlankInAnyLanguage() {
        codes.forEach { code ->
            val entries = mixLabLocalizationEntries(code)
            entries.forEach { (key, value) ->
                assertTrue("Blank Mix Lab string $code/$key", value.isNotBlank())
            }
        }
    }

    @Test
    fun placeholdersArePreservedInEveryLanguage() {
        codes.forEach { code ->
            val entries = mixLabLocalizationEntries(code)
            assertTrue("$code lost {count} for tracks", entries.getValue("mixLabPreviewTracks").contains("{count}"))
            assertTrue("$code lost {count} for artists", entries.getValue("mixLabPreviewArtists").contains("{count}"))
            assertTrue("$code lost {percent} for familiar", entries.getValue("mixLabPreviewFamiliarShare").contains("{percent}"))
            assertTrue("$code lost {percent} for discovery", entries.getValue("mixLabPreviewDiscoveryShare").contains("{percent}"))
        }
    }

    @Test
    fun nonEnglishBundlesAreNotIdenticalToEnglish() {
        val english = mixLabLocalizationEntries("en")
        codes.filterNot { it == "en" }.forEach { code ->
            val entries = mixLabLocalizationEntries(code)
            assertFalse("Mix Lab strings fall back to English for $code", entries == english)
        }
    }
}
