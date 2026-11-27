package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeNormalizationLocalizationTest {

    @Test
    fun copyCoversEveryCatalogLanguage() {
        val catalog = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet() + LevyraLanguageCatalog.BRAZILIAN_PORTUGUESE
        assertEquals(catalog, volumeNormalizationLocalizationCodes())
        val englishTitle = volumeNormalizationTitle("en")
        val englishSubtitle = volumeNormalizationSubtitle("en")
        catalog.forEach { code ->
            assertTrue("$code has blank title", volumeNormalizationTitle(code).isNotBlank())
            assertTrue("$code has blank subtitle", volumeNormalizationSubtitle(code).isNotBlank())
            if (code != "en") assertNotEquals("$code subtitle falls back to English", englishSubtitle, volumeNormalizationSubtitle(code))
        }
        assertNotEquals(englishTitle, volumeNormalizationTitle("it"))
    }

    @Test
    fun brazilianPortugueseIsDistinctFromEuropean() {
        assertNotEquals(volumeNormalizationTitle("pt"), volumeNormalizationTitle("pt-BR"))
        assertNotEquals(volumeNormalizationSubtitle("pt"), volumeNormalizationSubtitle("pt-BR"))
    }
}
