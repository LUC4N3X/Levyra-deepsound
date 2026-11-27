package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SpectrumLocalizationTest {
    @Test
    fun everyCatalogLanguageHasCompleteSpectrumCopy() {
        val catalog = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalog - spectrumCompleteLocalizationCodes(), emptySet<String>())
        assertEquals(spectrumLocalizationCodes(), spectrumCompleteLocalizationCodes())
    }

    @Test
    fun localizedCopyIsUsedInsteadOfEnglish() {
        val english = LevyraStrings.forCode("en").spectrumCopy
        val italian = LevyraStrings.forCode("it").spectrumCopy
        val russian = LevyraStrings.forCode("ru").spectrumCopy
        assertEquals("Visualizer", english[SpectrumText.Title])
        assertEquals("Visualizzatore", italian[SpectrumText.Title])
        assertEquals("Визуализатор", russian[SpectrumText.Title])
        assertNotEquals(english[SpectrumText.LatencyDesc], italian[SpectrumText.LatencyDesc])
    }
}
