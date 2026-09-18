package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraNewFeatureLocalizationTest {

    @Test
    fun queueSpaceAndLocalLibraryCoverEveryCatalogLanguage() {
        val codes = LevyraLanguageCatalog.languages.map { it.code }
        assertEquals(codes.toSet(), queueSpaceLocalizationCodes())
        assertEquals(codes.toSet(), localLibraryLocalizationCodes())
        val englishQueue = queueSpaceLocalizationEntries("en")
        val englishLocal = localLibraryLocalizationEntries("en")
        codes.filterNot { it == "en" }.forEach { code ->
            val queue = queueSpaceLocalizationEntries(code)
            val local = localLibraryLocalizationEntries(code)
            assertEquals(queueSpaceKeys, queue.keys)
            assertEquals(localLibraryKeys, local.keys)
            assertNotEquals("Queue strings fall back to English for $code", englishQueue, queue)
            assertNotEquals("Local library strings fall back to English for $code", englishLocal, local)
            queue.forEach { (key, value) -> assertTrue("$code/$key is blank", value.isNotBlank()) }
            local.forEach { (key, value) -> assertTrue("$code/$key is blank", value.isNotBlank()) }
        }
    }

    @Test
    fun localTagEditorCoversEveryCatalogLanguageWithoutEnglishFallback() {
        val codes = LevyraLanguageCatalog.languages.map { it.code }
        assertEquals(codes.toSet(), localTagLocalizationCodes())
        val english = localTagLocalizationEntries("en")
        codes.forEach { code ->
            val entries = localTagLocalizationEntries(code)
            assertEquals(localTagKeys, entries.keys)
            entries.forEach { (key, value) ->
                assertTrue("$code/$key is blank", value.isNotBlank())
            }
            if (code != "en") {
                assertNotEquals("Local tag strings fall back to English for $code", english, entries)
            }
        }
    }

    @Test
    fun placeholdersStaySafeAcrossLanguages() {
        LevyraLanguageCatalog.languages.map { it.code }.forEach { code ->
            val strings = LevyraStrings.forCode(code)
            val switched = strings.formatQueueSpaceSwitched("Gym")
            val summary = strings.formatLocalScanSummary(3, 2, 1)
            assertTrue("$code switch message lost the queue name", switched.contains("Gym"))
            assertTrue("$code scan summary lost its counters", listOf("3", "2", "1").all(summary::contains))
            assertTrue("$code scan failure message is blank", strings.localScanFailed.isNotBlank())
        }
    }
}
