package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class HighQualityProviderLane(
    val provider: HighQualityAudioProvider,
    private val mappingStore: HighQualityMappingStore,
    private val matcher: AlternativeTrackMatcher,
    private val clock: () -> Long,
    private val isTrackQuarantined: (providerId: String, providerTrackId: String) -> Boolean
) {
    suspend fun resolve(
        identityKey: String,
        query: AlternativeTrackQuery,
        preference: HighQualityPreference
    ): HighQualityResolution = try {
        val queryFingerprint = AlternativeTrackFingerprint.of(query)
        val stored = mappingStore.load(identityKey, provider.id, queryFingerprint)
        val refreshed = stored?.let { refreshStoredMapping(identityKey, query, it, preference) }
        refreshed ?: searchAndResolve(identityKey, query, queryFingerprint, preference)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        HighQualityResolution.Fallback(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, error.javaClass.simpleName)
    }

    fun pinnedMapping(query: AlternativeTrackQuery, candidate: AlternativeTrackCandidate): StoredAlternativeMapping? {
        if (candidate.providerId != provider.id) return null
        val evaluation = matcher.evaluate(query, candidate)
        if (!matcher.acceptsManualPin(evaluation)) return null
        return StoredAlternativeMapping(
            providerId = provider.id,
            providerTrackId = candidate.providerTrackId,
            queryFingerprint = AlternativeTrackFingerprint.of(query),
            candidateFingerprint = AlternativeTrackFingerprint.of(candidate),
            verdict = AlternativeMatchVerdict.HIGH,
            confidence = maxOf(evaluation.confidence, AlternativeTrackMatcher.PERSISTABLE_CONFIDENCE),
            storedAtMs = clock(),
            manual = true,
            snapshot = snapshotOf(candidate)
        )
    }

    private suspend fun refreshStoredMapping(
        identityKey: String,
        query: AlternativeTrackQuery,
        mapping: StoredAlternativeMapping,
        preference: HighQualityPreference
    ): HighQualityResolution? {
        if (isTrackQuarantined(provider.id, mapping.providerTrackId)) {
            mappingStore.remove(identityKey, provider.id)
            return null
        }
        val candidate = when (val outcome = currentCandidate(mapping)) {
            is ProviderLookupOutcome.Found -> outcome.candidate
            ProviderLookupOutcome.Missing -> {
                HighQualityAudioDiagnostics.staleMapping(provider.id, mapping.providerTrackId, "provider track missing")
                mappingStore.remove(identityKey, provider.id)
                return null
            }
            is ProviderLookupOutcome.Failed -> {
                HighQualityAudioDiagnostics.mappingRetained(provider.id, mapping.providerTrackId, outcome.failure.name)
                return HighQualityResolution.Fallback(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, outcome.failure.name)
            }
        }
        val evaluation = matcher.evaluate(query, candidate)
        val unchanged = AlternativeTrackFingerprint.of(candidate) == mapping.candidateFingerprint
        val acceptable = evaluation.accepted || (mapping.manual && matcher.acceptsManualPin(evaluation))
        if (!unchanged || !acceptable) {
            HighQualityAudioDiagnostics.staleMapping(
                provider.id,
                mapping.providerTrackId,
                if (!unchanged) "metadata changed" else "${evaluation.rejection}"
            )
            mappingStore.remove(identityKey, provider.id)
            return null
        }
        HighQualityAudioDiagnostics.cacheHit(provider.id, mapping.providerTrackId, mapping.manual)
        val trusted = if (mapping.manual) manualEvaluation(evaluation, mapping) else evaluation
        return resolveStream(identityKey, trusted, mapping.queryFingerprint, preference, mapping.manual)
    }

    private suspend fun currentCandidate(mapping: StoredAlternativeMapping): ProviderLookupOutcome {
        if (provider.supportsLookup) return provider.lookup(mapping.providerTrackId)
        val snapshot = mapping.snapshot ?: return ProviderLookupOutcome.Missing
        return ProviderLookupOutcome.Found(snapshot)
    }

    private fun manualEvaluation(
        evaluation: AlternativeMatchEvaluation,
        mapping: StoredAlternativeMapping
    ): AlternativeMatchEvaluation =
        if (evaluation.accepted) {
            evaluation
        } else {
            evaluation.copy(verdict = mapping.verdict, confidence = mapping.confidence, rejection = null)
        }

    private suspend fun searchAndResolve(
        identityKey: String,
        query: AlternativeTrackQuery,
        queryFingerprint: String,
        preference: HighQualityPreference
    ): HighQualityResolution {
        val collected = LinkedHashMap<String, AlternativeTrackCandidate>()
        var selection: AlternativeMatchSelection = AlternativeMatchSelection.Rejected(MatchRejection.NO_CANDIDATES, emptyList())
        for ((index, text) in provider.searchQueries(query).withIndex()) {
            currentCoroutineContext().ensureActive()
            when (val outcome = provider.search(text)) {
                is ProviderSearchOutcome.Failed ->
                    return HighQualityResolution.Fallback(HighQualityFallbackReason.PROVIDER_UNAVAILABLE, outcome.failure.name)
                is ProviderSearchOutcome.Found -> {
                    HighQualityAudioDiagnostics.search(provider.id, index + 1, text, outcome.candidates.size)
                    outcome.candidates
                        .filterNot { isTrackQuarantined(provider.id, it.providerTrackId) }
                        .forEach { collected.putIfAbsent(it.providerTrackId, it) }
                }
            }
            selection = matcher.select(query, collected.values.toList())
            val decisive = when (val current = selection) {
                is AlternativeMatchSelection.Accepted ->
                    current.evaluation.verdict == AlternativeMatchVerdict.EXACT ||
                        current.evaluation.confidence >= ISRC_CONFIRMED_CONFIDENCE ||
                        index >= 1
                is AlternativeMatchSelection.Rejected -> current.reason == MatchRejection.AMBIGUOUS
            }
            if (decisive) break
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
                resolveStream(identityKey, finalSelection.evaluation, queryFingerprint, preference, manual = false)
            }
        }
    }

    private suspend fun resolveStream(
        identityKey: String,
        evaluation: AlternativeMatchEvaluation,
        queryFingerprint: String,
        preference: HighQualityPreference,
        manual: Boolean
    ): HighQualityResolution = when (val outcome = provider.resolveStream(evaluation.candidate, preference)) {
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
                    storedAtMs = clock(),
                    manual = manual,
                    snapshot = snapshotOf(evaluation.candidate)
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

    private fun snapshotOf(candidate: AlternativeTrackCandidate): AlternativeTrackCandidate? =
        if (provider.supportsLookup) null else candidate.copy(mediaToken = "")

    companion object {
        const val ISRC_CONFIRMED_CONFIDENCE = 100
    }
}
