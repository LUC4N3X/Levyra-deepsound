package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.luc4n3x.levyra.update.releaseNoteHighlights
import org.junit.Test
import java.io.File

class UpdateReleaseLocalizationTest {
    @Test
    fun everySupportedLanguageShipsUpdateScreenCopy() {
        val catalogCodes = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()
        assertEquals(47, catalogCodes.size)
        assertEquals(catalogCodes, updateReleaseLocalizationCodes())
        assertEquals(catalogCodes, update2512HighlightLocalizationCodes())

        catalogCodes.forEach { code ->
            val strings = LevyraStrings.forCode(code)
            val copy = strings.updateReleaseCopy()
            val highlights = strings.updateReleaseHighlights("2.5.12")
            assertTrue("releaseReady is blank for $code", copy.releaseReady.isNotBlank())
            assertTrue("releaseFrom is blank for $code", copy.releaseFrom.isNotBlank())
            assertTrue("releaseTo is blank for $code", copy.releaseTo.isNotBlank())
            assertTrue("releaseHighlights is blank for $code", copy.releaseHighlights.isNotBlank())
            assertTrue("releaseProtection is blank for $code", copy.releaseProtection.isNotBlank())
            assertTrue("releaseProtectionDetail is blank for $code", copy.releaseProtectionDetail.isNotBlank())
            assertEquals("unexpected highlight count for $code", 4, highlights.size)
            assertTrue("blank highlight for $code", highlights.all { it.isNotBlank() })
        }
    }

    @Test
    fun russianUpdateScreenUsesRussianCopy() {
        val strings = LevyraStrings.forCode("ru")
        val copy = strings.updateReleaseCopy()
        val highlights = strings.updateReleaseHighlights("v2.5.12")

        assertEquals("Обновление готово", copy.releaseReady)
        assertEquals("С версии", copy.releaseFrom)
        assertEquals("До версии", copy.releaseTo)
        assertEquals("Основные изменения", copy.releaseHighlights)
        assertEquals("Защищённое обновление", copy.releaseProtection)
        assertTrue(copy.releaseProtectionDetail.contains("Официальный APK"))
        assertTrue(copy.releaseProtectionDetail.contains("перед установкой"))
        assertTrue(highlights.first().contains("Синхронизированный"))
        assertFalse(highlights.any { it.contains("Synced lyrics", ignoreCase = true) })
    }

    @Test
    fun rtlAndTraditionalChineseUpdateCopyStayNative() {
        val arabic = LevyraStrings.forCode("ar").updateReleaseCopy()
        val persian = LevyraStrings.forCode("fa").updateReleaseCopy()
        val hebrew = LevyraStrings.forCode("he").updateReleaseCopy()
        val traditionalChinese = LevyraStrings.forCode("zh-Hant").updateReleaseCopy()

        assertEquals("التحديث جاهز", arabic.releaseReady)
        assertEquals("به‌روزرسانی آماده است", persian.releaseReady)
        assertEquals("העדכון מוכן", hebrew.releaseReady)
        assertEquals("更新已就緒", traditionalChinese.releaseReady)
    }

    @Test
    fun unknownReleaseFallsBackToSelectedLanguageCopy() {
        assertEquals(
            listOf(LevyraStrings.forCode("ru").updateDescription),
            LevyraStrings.forCode("ru").updateReleaseHighlights("9.9.9")
        )
        assertEquals(
            listOf(LevyraStrings.forCode("ja").updateDescription),
            LevyraStrings.forCode("ja").updateReleaseHighlights("9.9.9")
        )
    }

    @Test
    fun releaseNotesShipHighlightsForEveryLanguage() {
        val notes = listOf(File("../docs/RELEASE_NOTES.md"), File("docs/RELEASE_NOTES.md")).first(File::exists).readText()
        val highlights = releaseNoteHighlights(notes)
        val catalogCodes = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()

        assertEquals(catalogCodes, highlights.keys)
        catalogCodes.forEach { code ->
            val localized = LevyraStrings.forCode(code).updateReleaseHighlights("0.0.0", highlights)
            assertEquals("unexpected highlight count for $code", highlights.getValue("en").size, localized.size)
            assertEquals(highlights.getValue(code), localized)
        }
    }

    @Test
    fun publishedHighlightsFallBackSafely() {
        val italian = LevyraStrings.forCode("it")
        val englishOnly = "# Notes\n<!-- levyra-highlights {\"en\": [\"Faster start.\"]} -->"

        assertEquals(listOf("Faster start."), italian.updateReleaseHighlights("9.9.9", releaseNoteHighlights(englishOnly)))
        assertTrue(releaseNoteHighlights("<!-- levyra-highlights {broken -->").isEmpty())
        assertEquals(
            listOf(italian.updateDescription),
            italian.updateReleaseHighlights("9.9.9", releaseNoteHighlights("<!-- levyra-highlights {\"it\": [\"Solo italiano\"]} -->"))
        )
        assertTrue(releaseNoteHighlights("<!-- levyra-highlights {\"en\": [\"\", 3]} -->").isEmpty())
    }
}
