package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationDiagnosticsStringsTest {
    @Test
    fun italianUsesTheSelectedLevyraLocale() {
        val copy = LevyraStrings.forCode("it").recommendationDiagnosticsCopy()

        assertEquals("Più brani così nella radio", copy.moreLikeThis)
        assertEquals("Suggerisci di nuovo questo artista", copy.allowArtistAgain)
        assertEquals("Diagnostica riproduzione", copy.diagnosticsTitle)
        assertEquals("Copia report", copy.copyReport)
    }

    @Test
    fun japaneseUsesDedicatedRecommendationAndDiagnosticCopy() {
        val copy = LevyraStrings.forCode("ja").recommendationDiagnosticsCopy()

        assertEquals("ラジオで似た曲を増やす", copy.moreLikeThis)
        assertEquals("このアーティストを再びおすすめする", copy.allowArtistAgain)
        assertEquals("再生診断", copy.diagnosticsTitle)
        assertEquals("レポートをコピー", copy.copyReport)
    }
}
