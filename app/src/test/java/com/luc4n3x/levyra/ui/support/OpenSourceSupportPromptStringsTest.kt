package com.luc4n3x.levyra.ui.support

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSourceSupportPromptStringsTest {
    @Test
    fun supportPromptCoversEverySupportedLanguage() {
        val expected = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(expected, OpenSourceSupportStrings.supportedCodes())
    }

    @Test
    fun everySupportPromptBundleIsCompleteAndProfessional() {
        OpenSourceSupportStrings.supportedCodes().forEach { code ->
            val copy = OpenSourceSupportStrings.forCode(code)
            listOf(copy.badge, copy.title, copy.body, copy.starAction, copy.continueAction).forEach { value ->
                assertTrue("Blank support prompt copy for $code", value.isNotBlank())
            }
            assertTrue("Support prompt body is too short for $code", copy.body.length >= 90)
            assertFalse("Support prompt must not request money for $code", copy.body.contains("€"))
        }
    }

    @Test
    fun fallbackAndRtlBundlesResolveCorrectly() {
        assertEquals("en", fallbackLanguageCode("xx-YY"))
        assertTrue(OpenSourceSupportStrings.forCode("ar-SA").body.contains("GitHub"))
        assertTrue(OpenSourceSupportStrings.forCode("he-IL").body.contains("GitHub"))
        assertEquals("Levyra è gratuita. Davvero.", OpenSourceSupportStrings.forCode("it-IT").title)
    }

    private fun fallbackLanguageCode(code: String): String {
        val normalized = LevyraLanguageCatalog.normalize(code)
        return if (normalized in OpenSourceSupportStrings.supportedCodes()) normalized else "en"
    }
}
