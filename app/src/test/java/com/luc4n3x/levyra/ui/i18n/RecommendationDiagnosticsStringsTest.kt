package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationDiagnosticsStringsTest {
    @Test
    fun italianUsesTheSelectedLevyraLocale() {
        val copy = LevyraStrings.forCode("it").recommendationDiagnosticsCopy()

        assertEquals("Più brani come questo", copy.moreLikeThis)
        assertEquals("Diagnostica riproduzione", copy.diagnosticsTitle)
        assertEquals("Copia report", copy.copyReport)
    }

    @Test
    fun unsupportedDedicatedCopyFallsBackToEnglish() {
        val copy = LevyraStrings.forCode("ja").recommendationDiagnosticsCopy()

        assertEquals("More like this", copy.moreLikeThis)
        assertEquals("Playback diagnostics", copy.diagnosticsTitle)
        assertEquals("Copy report", copy.copyReport)
    }
}
