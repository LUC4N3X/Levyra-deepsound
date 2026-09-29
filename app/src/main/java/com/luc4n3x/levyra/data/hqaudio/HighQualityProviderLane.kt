package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull

internal class HighQualityProviderLane(
    val provider: HighQualityAudioProvider,
    private val mappingStore: HighQualityMappingStore,
    private val matcher: AlternativeTrackMatcher,
    private val clock: () -> Long,
    private val isTrackQuarantined: (providerTrackId: String) -> Boolean
) {
    suspend fun resolve(identityKey: String, query: AlternativeTrackQuery): HighQualityResolution = try {
        val queryFingerprint = AlternativeTrackFingerprint.of(query)
        val stored = mappingStore.load(identityKey, provider.id, queryFingerprint)
        val refreshed = stored?.let { refreshStoredMapping(identityKey, query, it) }
        refreshed ?: searchAndResolve(identityKey, query, queryFingerprint)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        HighQualityResolution.Fallback(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, error.javaClass.simpleName)
    }

    private suspend fun refreshStoredMapping(
        identityKey: String,
        query: AlternativeTrackQuery,
        mapping: StoredAlternativeMapping
    ): HighQualityResolution? {
        if (isTrackQuarantined(mapping.providerTrackId)) {
            mappingStore.remove(identityKey, provider.id)
            return null
        }
        return when (val outcome = provider.lookup(mapping.providerTrackId)) {
            is ProviderLookupOutcome.Found -> {
                val evaluation = matcher.evaluate(query, outcome.candidate)
                val unchanged = AlternativeTrackFingerprint.of(outcome.candidate) == mapping.candidateFingerprint
                if (!unchanged || !evaluation.accepted) {
                    HighQualityAudioDiagnostics.staleMapping(
                        provider.id,
                        mapping.providerTrackId,
                        if (!unchanged) "metadata changed" else "${evaluation.rejection}"
                    )
                    mappingStore.remove(identityKey, provider.id)
                    null
                } else {
                    HighQualityAudioDiagnostics.cacheHit(provider.id, mapping.providerTrackId)
                    resolveStream(identityKey, evaluation, mapping.queryFingerprint)
                }
            }
            ProviderLookupOutcome.Missing -> {
                HighQualityAudioDiagnostics.staleMapping(provider.id, mapping.providerTrackId, "provider track missing")
                mappingStore.remove(identityKey, provider.id)
                null
            }
            is ProviderLookupOutcome.Failed -> {
                HighQualityAudioDiagnostics.mappingRetained(provider.id, mapping.providerTrackId, outcome.failure.name)
                HighQualityResolution.Fallback(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, outcome.failure.name)
            }
        }
    }

    private suspend fun searchAndResolve(
        identityKey: String,
        query: AlternativeTrackQuery,
        queryFingerprint: String
    ): HighQualityResolution {
        val collected = LinkedHashMap<String, AlternativeTrackCandidate>()
        var selection: AlternativeMatchSelection = AlternativeMatchSelection.Rejected(MatchRejection.NO_CANDIDATES, emptyList())
        for ((index, text) in AlternativeSearchPlan.queries(query).withIndex()) {
            currentCoroutineContext().ensureActive()
            when (val outcome = provider.search(text)) {
                is ProviderSearchOutcome.Failed ->
                    return HighQualityResolution.Fallback(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, outcome.failure.name)
                is ProviderSearchOutcome.Found -> {
                    HighQualityAudioDiagnostics.search(provider.id, index + 1, text, outcome.candidates.size)
                    outcome.candidates
                        .filterNot { isTrackQuarantined(it.providerTrackId) }
                        .forEach { collected.putIfAbsent(it.providerTrackId, it) }
                }
            }
            selection = matcher.select(query, collected.values.toList())
            if (isDecisive(selection, index)) break
        }
        return when (val finalSelection = selection) {
            is AlternativeMatchSelection.Rejected -> {
                HighQualityAudioDiagnostics.matchRejected(provider.id, query, finalSelection)
                val reason = if (finalSelection.reason == MatchRejection.AMBIGUOUS) {
                    HighQualityFallbackReason.AMBIGUOUS
                } else {
                    HighQualityFallbackReason.NO_MATCH
                }
                HighQualityResolution.Fallback(reason, finalSelection.reason.name)
            }
            is AlternativeMatchSelection.Accepted -> {
                HighQualityAudioDiagnostics.matchAccepted(query, finalSelection.evaluation)
                when (val hydration = hydrate(query, finalSelection.evaluation)) {
                    is Hydration.Ready -> resolveStream(identityKey, hydration.evaluation, queryFingerprint)
                    is Hydration.Refused -> hydration.fallback
                }
            }
        }
    }

    private suspend fun hydrate(query: AlternativeTrackQuery, selected: AlternativeMatchEvaluation): Hydration {
        val searched = selected.candidate
        val outcome = withTimeoutOrNull(HYDRATION_TIMEOUT_MS) { provider.lookup(searched.providerTrackId) }
        return when (outcome) {
            null -> keepSearched(selected, "TIMEOUT", "${HYDRATION_TIMEOUT_MS}ms")
            is ProviderLookupOutcome.Failed -> keepSearched(selected, "UNAVAILABLE", outcome.failure.name)
            ProviderLookupOutcome.Missing -> refuse(searched, "MISSING", "details missing or restricted")
            is ProviderLookupOutcome.Found -> {
                val details = outcome.candidate
                val evaluation = matcher.evaluate(query, details)
                val sameTrack = details.providerId == searched.providerId &&
                    details.providerTrackId == searched.providerTrackId
                if (sameTrack && evaluation.accepted) {
                    HighQualityAudioDiagnostics.hydration(
                        provider.id,
                        searched.providerTrackId,
                        "HYDRATED",
                        details.offers320,
                        "verdict=${evaluation.verdict} confidence=${evaluation.confidence}"
                    )
                    Hydration.Ready(evaluation)
                } else {
                    refuse(searched, "CONFLICT", if (sameTrack) "${evaluation.rejection}" else "track id changed")
                }
            }
        }
    }

    private fun keepSearched(selected: AlternativeMatchEvaluation, outcome: String, detail: String): Hydration {
        val candidate = selected.candidate
        HighQualityAudioDiagnostics.hydration(provider.id, candidate.providerTrackId, outcome, candidate.offers320, detail)
        return Hydration.Ready(selected)
    }

    private fun refuse(searched: AlternativeTrackCandidate, outcome: String, detail: String): Hydration {
        HighQualityAudioDiagnostics.hydration(provider.id, searched.providerTrackId, outcome, searched.offers320, detail)
        return Hydration.Refused(HighQualityResolution.Fallback(HighQualityFallbackReason.NO_MATCH, "HYDRATION_$outcome"))
    }

    private fun isDecisive(selection: AlternativeMatchSelection, index: Int): Boolean = when (selection) {
        is AlternativeMatchSelection.Accepted ->
            selection.evaluation.verdict == AlternativeMatchVerdict.EXACT ||
                selection.evaluation.confidence >= ISRC_CONFIRMED_CONFIDENCE ||
                index >= 1
        is AlternativeMatchSelection.Rejected -> selection.reason == MatchRejection.AMBIGUOUS
    }

    private suspend fun resolveStream(
        identityKey: String,
        evaluation: AlternativeMatchEvaluation,
        queryFingerprint: String
    ): HighQualityResolution = when (val outcome = provider.resolveStream(evaluation.candidate)) {
        is ProviderStreamOutcome.Resolved -> {
            mappingStore.save(
                identityKey,
                StoredAlternativeMapping(
                    providerId = provider.id,
                    providerTrackId = evaluation.candidate.providerTrackId,
                    queryFingerprint = queryFingerprint,
                    candidateFingerprint = AlternativeTrackFingerprint.of(evaluation.candidate),
                    verdict = evaluation.verdict,
                    confidence = evaluation.confidence,
                    storedAtMs = clock()
                )
            )
            HighQualityResolution.Selected(outcome.stream, evaluation)
        }
        is ProviderStreamOutcome.Unavailable -> {
            mappingStore.remove(identityKey, provider.id)
            HighQualityResolution.Fallback(
                HighQualityFallbackReason.STREAM_UNAVAILABLE,
                outcome.rejections.joinToString(",") { it.name }
            )
        }
        is ProviderStreamOutcome.Failed ->
            HighQualityResolution.Fallback(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, outcome.failure.name)
    }

    private sealed interface Hydration {
        data class Ready(val evaluation: AlternativeMatchEvaluation) : Hydration
        data class Refused(val fallback: HighQualityResolution.Fallback) : Hydration
    }

    companion object {
        const val ISRC_CONFIRMED_CONFIDENCE = 100
        const val HYDRATION_TIMEOUT_MS = 1_500L
    }
}
