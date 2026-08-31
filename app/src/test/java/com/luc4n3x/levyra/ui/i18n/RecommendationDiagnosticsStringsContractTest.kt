package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationDiagnosticsStringsContractTest {
    @Test
    fun recommendationDiagnosticsCopyIsCompleteForEverySupportedLocale() {
        LevyraStrings.all().forEach { strings ->
            val copy = strings.recommendationDiagnosticsCopy()
            val values = listOf(
                copy.moreLikeThis,
                copy.lessLikeThis,
                copy.blockArtist,
                copy.diagnosticsTitle,
                copy.close,
                copy.copied,
                copy.track,
                copy.id,
                copy.title,
                copy.artist,
                copy.source,
                copy.mode,
                copy.player,
                copy.state,
                copy.playing,
                copy.position,
                copy.buffered,
                copy.duration,
                copy.speed,
                copy.audioSession,
                copy.errorCode,
                copy.formats,
                copy.audio,
                copy.video,
                copy.network,
                copy.cache,
                copy.transport,
                copy.validated,
                copy.metered,
                copy.resolver,
                copy.successFailure,
                copy.failureStreak,
                copy.circuit,
                copy.averageLatency,
                copy.lastFailure,
                copy.security,
                copy.copyReport,
                copy.statusHealthy,
                copy.statusFallback,
                copy.statusError,
                copy.statusIdle
            )
            assertTrue("Incomplete recommendation/diagnostics copy for ${strings.code}", values.all(String::isNotBlank))
        }
    }
}
