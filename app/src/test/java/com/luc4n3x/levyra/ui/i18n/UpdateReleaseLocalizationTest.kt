package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateReleaseLocalizationTest {
    @Test
    fun everySupportedLanguageShipsUpdateScreenCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(37, catalogCodes.size)
        assertEquals(catalogCodes, updateReleaseLocalizationCodes())

        catalogCodes.forEach { code ->
            val copy = LevyraStrings.forCode(code).updateReleaseCopy()
            assertTrue("releaseReady is blank for $code", copy.releaseReady.isNotBlank())
            assertTrue("releaseFrom is blank for $code", copy.releaseFrom.isNotBlank())
            assertTrue("releaseTo is blank for $code", copy.releaseTo.isNotBlank())
            assertTrue("releaseHighlights is blank for $code", copy.releaseHighlights.isNotBlank())
            assertTrue("releaseProtection is blank for $code", copy.releaseProtection.isNotBlank())
            assertTrue("releaseProtectionDetail is blank for $code", copy.releaseProtectionDetail.isNotBlank())
        }
    }

    @Test
    fun russianUpdateScreenUsesRussianCopy() {
        val copy = LevyraStrings.forCode("ru").updateReleaseCopy()

        assertEquals("Обновление готово", copy.releaseReady)
        assertEquals("С версии", copy.releaseFrom)
        assertEquals("До версии", copy.releaseTo)
        assertEquals("Основные изменения", copy.releaseHighlights)
        assertEquals("Защищённое обновление", copy.releaseProtection)
        assertTrue(copy.releaseProtectionDetail.contains("Официальный APK"))
        assertTrue(copy.releaseProtectionDetail.contains("перед установкой"))
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
}
