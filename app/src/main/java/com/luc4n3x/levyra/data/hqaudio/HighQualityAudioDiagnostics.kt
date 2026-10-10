package com.luc4n3x.levyra.data.hqaudio

import timber.log.Timber

internal object HighQualityAudioDiagnostics {
    private const val MAX_TEXT = 120
    private const val MAX_DETAIL_TEXT = 48
    private const val MAX_REJECTED_DETAILS = 4

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

    fun mediaRoute(providerId: String, providerTrackId: String, route: String, advertised320: Boolean?, host: String) {
        Timber.d(
            "HQ_PROVIDER_MEDIA_ROUTE provider=%s id=%s route=%s advertised320=%s host=%s",
            providerId,
            providerTrackId,
            route,
            advertisedLabel(advertised320),
            host
        )
    }

    fun hydration(providerId: String, providerTrackId: String, outcome: String, advertised320: Boolean?, detail: String) {
        Timber.d(
            "HQ_PROVIDER_HYDRATION provider=%s id=%s outcome=%s advertised320=%s detail=%s",
            providerId,
            providerTrackId,
            outcome,
            advertisedLabel(advertised320),
            detail.ifBlank { "-" }
        )
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

    fun cacheHit(providerId: String, providerTrackId: String) {
        Timber.d("HQ_PROVIDER_CACHE_HIT provider=%s id=%s", providerId, providerTrackId)
    }

    fun staleMapping(providerId: String, providerTrackId: String, cause: String) {
        Timber.d("HQ_PROVIDER_MAPPING_STALE provider=%s id=%s cause=%s", providerId, providerTrackId, cause)
    }

    fun matchAccepted(query: AlternativeTrackQuery, evaluation: AlternativeMatchEvaluation) {
        val candidate = evaluation.candidate
        Timber.d(
            "HQ_PROVIDER_MATCH_ACCEPTED provider=%s id=%s verdict=%s confidence=%d expectedTitle=\"%s\" providerTitle=\"%s\" expectedArtist=\"%s\" primaryArtist=\"%s\" album=\"%s\" providerAlbum=\"%s\" albumRelation=%s durationDelta=%ds version=%s explicit=%s advertised320=%s",
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
            advertisedLabel(candidate.offers320)
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
        val closest = selection.evaluations
            .sortedWith(compareBy<AlternativeMatchEvaluation> { it.rejection != null }.thenByDescending { it.confidence })
            .take(MAX_REJECTED_DETAILS)
            .joinToString(";") { evaluation ->
                val candidate = evaluation.candidate
                "${candidate.providerTrackId}:${evaluation.rejection ?: evaluation.verdict}:${evaluation.confidence}:" +
                    "${candidate.durationSeconds}s:${evaluation.albumRelation}:\"${candidate.title.take(MAX_DETAIL_TEXT)}\""
            }
            .ifBlank { "-" }
        Timber.d(
            "HQ_PROVIDER_MATCH_REJECTED provider=%s reason=%s candidates=%d expectedTitle=\"%s\" expectedArtist=\"%s\" expectedAlbum=\"%s\" expectedDuration=%ds breakdown=%s closest=%s",
            providerId,
            selection.reason,
            selection.evaluations.size,
            query.title.take(MAX_TEXT),
            query.artist.take(MAX_TEXT),
            query.album.take(MAX_TEXT),
            query.durationMs / 1_000L,
            breakdown,
            closest
        )
    }

    fun streamValid(
        providerId: String,
        providerTrackId: String,
        route: String,
        requestedTier: AudioQualityTier,
        host: String,
        validation: StreamValidation.Valid
    ) {
        Timber.d(
            "HQ_PROVIDER_STREAM_VALID provider=%s id=%s route=%s requested=%dkbps verified=%dkbps estimated=%dkbps mime=%s container=%s codec=%s bytes=%d host=%s",
            providerId,
            providerTrackId,
            route,
            requestedTier.kbps,
            validation.tier.kbps,
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
        route: String,
        requestedTier: AudioQualityTier,
        host: String,
        validation: StreamValidation.Invalid
    ) {
        Timber.d(
            "HQ_PROVIDER_STREAM_INVALID provider=%s id=%s route=%s requested=%dkbps reason=%s status=%d estimated=%dkbps host=%s",
            providerId,
            providerTrackId,
            route,
            requestedTier.kbps,
            validation.rejection,
            validation.statusCode,
            validation.estimatedKbps,
            host
        )
    }

    fun selected(stream: ResolvedHighQualityStream, evaluation: AlternativeMatchEvaluation, waitedMs: Long) {
        Timber.d(
            "HQ_PROVIDER_SELECTED provider=%s id=%s tier=%dkbps estimated=%dkbps label=\"%s\" mime=%s host=%s verdict=%s confidence=%d waitedMs=%d",
            stream.providerId,
            stream.providerTrackId,
            stream.tier.kbps,
            stream.estimatedKbps,
            stream.qualityLabel,
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

    private fun advertisedLabel(advertised320: Boolean?): String = advertised320?.toString() ?: "missing"

    private fun versionLabel(signature: Set<String>): String =
        if (signature.isEmpty()) "ORIGINAL" else signature.sorted().joinToString("+")
}
