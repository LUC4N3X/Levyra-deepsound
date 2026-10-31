package com.luc4n3x.levyra.data.hqaudio.lossless

import com.luc4n3x.levyra.data.hqaudio.AlternativeMatchEvaluation
import com.luc4n3x.levyra.data.hqaudio.AlternativeMatchSelection
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackMatcher
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackQuery
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackText
import com.luc4n3x.levyra.data.hqaudio.HighQualityFallbackReason
import com.luc4n3x.levyra.data.hqaudio.HighQualityResolution
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal object LosslessRecordingFallback {
    private const val MAX_STREAM_CANDIDATES = 3

    suspend fun resolve(
        query: AlternativeTrackQuery,
        selection: AlternativeMatchSelection.Accepted,
        resolveStream: suspend (AlternativeMatchEvaluation) -> HighQualityResolution
    ): HighQualityResolution {
        var resolution = resolveStream(selection.evaluation)
        if (!resolution.isUnavailableStream()) return resolution
        val explicitKnown = query.explicit != null || AlternativeTrackText.title(query.title).explicitHint != null
        val alternatives = selection.evaluations
            .filter {
                it.accepted && it.candidate.providerTrackId != selection.evaluation.candidate.providerTrackId &&
                    sameRecording(selection.evaluation, it, explicitKnown)
            }
            .sortedByDescending { it.confidence }
            .take(MAX_STREAM_CANDIDATES - 1)
        for (evaluation in alternatives) {
            currentCoroutineContext().ensureActive()
            resolution = resolveStream(evaluation)
            if (!resolution.isUnavailableStream()) return resolution
        }
        return resolution
    }

    private fun sameRecording(
        selected: AlternativeMatchEvaluation,
        alternative: AlternativeMatchEvaluation,
        explicitKnown: Boolean
    ): Boolean {
        val a = selected.candidate
        val b = alternative.candidate
        if (a.providerId != b.providerId) return false
        val selectedIsrc = normalizedIsrc(a.isrc)
        val alternateIsrc = normalizedIsrc(b.isrc)
        if (selectedIsrc.isNotEmpty() && alternateIsrc.isNotEmpty() && selectedIsrc != alternateIsrc) return false
        val aTitle = AlternativeTrackText.title(a.title)
        val bTitle = AlternativeTrackText.title(b.title)
        val aPrimary = a.primaryArtists.flatMap(AlternativeTrackText::artistNames).toSet()
        val bPrimary = b.primaryArtists.flatMap(AlternativeTrackText::artistNames).toSet()
        val aFeatured = a.featuredArtists.flatMap(AlternativeTrackText::artistNames).toSet() + aTitle.featuredArtists
        val bFeatured = b.featuredArtists.flatMap(AlternativeTrackText::artistNames).toSet() + bTitle.featuredArtists
        val aExplicit = a.explicit ?: aTitle.explicitHint
        val bExplicit = b.explicit ?: bTitle.explicitHint
        return aTitle.core == bTitle.core &&
            aTitle.versionSignature == bTitle.versionSignature &&
            aPrimary.isNotEmpty() && aPrimary == bPrimary &&
            (aFeatured.isEmpty() || bFeatured.isEmpty() || aFeatured == bFeatured) &&
            abs(a.durationSeconds - b.durationSeconds) <= AlternativeTrackMatcher.SAME_RECORDING_DURATION_SECONDS &&
            (!explicitKnown || aExplicit == null || bExplicit == null || aExplicit == bExplicit) &&
            (a.language.isBlank() || b.language.isBlank() || a.language == b.language)
    }

    private fun HighQualityResolution.isUnavailableStream(): Boolean =
        this is HighQualityResolution.Fallback && reason == HighQualityFallbackReason.STREAM_UNAVAILABLE

    private fun normalizedIsrc(value: String): String =
        value.uppercase(Locale.ROOT).filter { it in 'A'..'Z' || it in '0'..'9' }
}
