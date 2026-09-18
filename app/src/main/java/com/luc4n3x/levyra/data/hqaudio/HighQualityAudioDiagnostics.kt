package com.luc4n3x.levyra.data.hqaudio

import timber.log.Timber

internal object HighQualityAudioDiagnostics {
    private const val MAX_TEXT = 120

    fun indiaProfile(providerId: String, operator: String, maskedAddress: String, acceptLanguage: String) {
        Timber.d(
            "HQ_PROVIDER_INDIA_PROFILE provider=%s operator=%s address=%s acceptLanguage=%s",
            providerId,
            operator,
            maskedAddress,
            acceptLanguage
        )
    }

    fun profileRejected(providerId: String, operator: String, maskedAddress: String, statusCode: Int, cooldownMs: Long) {
        Timber.d(
            "HQ_PROVIDER_PROFILE_REJECTED provider=%s operator=%s address=%s status=%d blockCooldownMs=%d",
            providerId,
            operator,
            maskedAddress,
            statusCode,
            cooldownMs
        )
    }

    fun circuit(providerId: String, state: String, detail: String) {
        Timber.d("HQ_PROVIDER_CIRCUIT provider=%s state=%s detail=%s", providerId, state, detail)
    }

    fun mediaRoute(providerId: String, providerTrackId: String, route: String) {
        Timber.d("HQ_PROVIDER_MEDIA_ROUTE provider=%s id=%s route=%s", providerId, providerTrackId, route)
    }

    fun mappingRetained(providerId: String, providerTrackId: String, cause: String) {
        Timber.d("HQ_PROVIDER_MAPPING_RETAINED provider=%s id=%s cause=%s", providerId, providerTrackId, cause)
    }

    fun search(providerId: String, pass: Int, query: String, results: Int) {
        Timber.d(
            "HQ_PROVIDER_SEARCH provider=%s pass=%d results=%d query=\"%s\"",
            providerId,
            pass,
            results,
            query.take(MAX_TEXT)
        )
    }

    fun cacheHit(providerId: String, providerTrackId: String, manual: Boolean) {
        Timber.d("HQ_PROVIDER_CACHE_HIT provider=%s id=%s manual=%s", providerId, providerTrackId, manual)
    }

    fun manualPin(providerId: String, providerTrackId: String) {
        Timber.d("HQ_PROVIDER_MANUAL_PIN provider=%s id=%s", providerId, providerTrackId)
    }

    fun routeAttempt(providerId: String, position: Int, total: Int, preference: HighQualityPreference) {
        Timber.d("HQ_ROUTER_TRY provider=%s position=%d/%d preference=%s", providerId, position, total, preference)
    }

    fun routeHedge(slowProviderId: String, nextProviderId: String, afterMs: Long) {
        Timber.d("HQ_ROUTER_HEDGE slow=%s next=%s afterMs=%d", slowProviderId, nextProviderId, afterMs)
    }

    fun routeOutcome(providerId: String, outcome: String, detail: String) {
        Timber.d("HQ_ROUTER_RESULT provider=%s outcome=%s detail=%s", providerId, outcome, detail.ifBlank { "-" }.take(MAX_TEXT))
    }

    fun backend(providerId: String, backend: String, operation: String, outcome: String, latencyMs: Long) {
        Timber.d(
            "HQ_PROVIDER_BACKEND provider=%s backend=%s op=%s outcome=%s latencyMs=%d",
            providerId,
            backend,
            operation,
            outcome,
            latencyMs
        )
    }

    fun staleMapping(providerId: String, providerTrackId: String, cause: String) {
        Timber.d("HQ_PROVIDER_MAPPING_STALE provider=%s id=%s cause=%s", providerId, providerTrackId, cause)
    }

    fun matchAccepted(query: AlternativeTrackQuery, evaluation: AlternativeMatchEvaluation) {
        val candidate = evaluation.candidate
        Timber.d(
            "HQ_PROVIDER_MATCH_ACCEPTED provider=%s id=%s verdict=%s confidence=%d expectedTitle=\"%s\" providerTitle=\"%s\" expectedArtist=\"%s\" primaryArtist=\"%s\" album=\"%s\" providerAlbum=\"%s\" albumRelation=%s durationDelta=%ds version=%s explicit=%s offers320=%s",
            candidate.providerId,
            candidate.providerTrackId,
            evaluation.verdict,
            evaluation.confidence,
            query.title.take(MAX_TEXT),
            candidate.title.take(MAX_TEXT),
            query.artist.take(MAX_TEXT),
            candidate.primaryArtists.firstOrNull().orEmpty().take(MAX_TEXT),
            query.album.take(MAX_TEXT),
            candidate.album.take(MAX_TEXT),
            evaluation.albumRelation,
            evaluation.durationDeltaSeconds,
            versionLabel(evaluation.versionSignature),
            candidate.explicit,
            candidate.offers320
        )
    }

    fun matchRejected(providerId: String, query: AlternativeTrackQuery, selection: AlternativeMatchSelection.Rejected) {
        val breakdown = selection.evaluations
            .mapNotNull { it.rejection }
            .groupingBy { it }
            .eachCount()
            .entries
            .joinToString(",") { "${it.key}=${it.value}" }
            .ifBlank { "-" }
        Timber.d(
            "HQ_PROVIDER_MATCH_REJECTED provider=%s reason=%s candidates=%d expectedTitle=\"%s\" expectedArtist=\"%s\" breakdown=%s",
            providerId,
            selection.reason,
            selection.evaluations.size,
            query.title.take(MAX_TEXT),
            query.artist.take(MAX_TEXT),
            breakdown
        )
    }

    fun streamValid(
        providerId: String,
        providerTrackId: String,
        requested: String,
        host: String,
        validation: StreamValidation.Valid
    ) {
        Timber.d(
            "HQ_PROVIDER_STREAM_VALID provider=%s id=%s requested=%s quality=\"%s\" estimated=%dkbps mime=%s container=%s codec=%s bytes=%d host=%s",
            providerId,
            providerTrackId,
            requested,
            validation.quality.label,
            validation.estimatedKbps,
            validation.mimeType,
            validation.container,
            validation.codec.ifBlank { "-" },
            validation.contentLength,
            host
        )
    }

    fun streamInvalid(
        providerId: String,
        providerTrackId: String,
        requested: String,
        host: String,
        rejection: StreamRejection,
        statusCode: Int
    ) {
        Timber.d(
            "HQ_PROVIDER_STREAM_INVALID provider=%s id=%s requested=%s reason=%s status=%d host=%s",
            providerId,
            providerTrackId,
            requested,
            rejection,
            statusCode,
            host
        )
    }

    fun selected(stream: ResolvedHighQualityStream, evaluation: AlternativeMatchEvaluation, waitedMs: Long) {
        Timber.d(
            "HQ_PROVIDER_SELECTED provider=%s id=%s quality=\"%s\" estimated=%dkbps mime=%s host=%s verdict=%s confidence=%d waitedMs=%d",
            stream.providerId,
            stream.providerTrackId,
            stream.quality.label,
            stream.estimatedKbps,
            stream.mimeType,
            stream.host,
            evaluation.verdict,
            evaluation.confidence,
            waitedMs
        )
    }

    fun fallback(reason: HighQualityFallbackReason, detail: String, title: String) {
        Timber.d(
            "HQ_PROVIDER_FALLBACK reason=%s detail=%s title=\"%s\"",
            reason,
            detail.ifBlank { "-" },
            title.take(MAX_TEXT)
        )
    }

    fun hostOf(url: String): String =
        url.substringAfter("://", "").substringBefore('/').substringBefore('?').ifBlank { "-" }

    private fun versionLabel(signature: Set<String>): String =
        if (signature.isEmpty()) "ORIGINAL" else signature.sorted().joinToString("+")
}
