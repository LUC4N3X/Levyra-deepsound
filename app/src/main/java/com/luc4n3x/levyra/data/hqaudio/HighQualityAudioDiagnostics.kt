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

    fun matchRejected(query: AlternativeTrackQuery, selection: AlternativeMatchSelection.Rejected) {
        val breakdown = selection.evaluations
            .mapNotNull { it.rejection }
            .groupingBy { it }
            .eachCount()
            .entries
            .joinToString(",") { "${it.key}=${it.value}" }
            .ifBlank { "-" }
        Timber.d(
            "HQ_PROVIDER_MATCH_REJECTED reason=%s candidates=%d expectedTitle=\"%s\" expectedArtist=\"%s\" breakdown=%s",
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
        tier: AudioQualityTier,
        host: String,
        validation: StreamValidation.Valid
    ) {
        Timber.d(
            "HQ_PROVIDER_STREAM_VALID provider=%s id=%s quality=%dkbps estimated=%dkbps mime=%s container=%s codec=%s bytes=%d host=%s",
            providerId,
            providerTrackId,
            tier.kbps,
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
        tier: AudioQualityTier,
        host: String,
        rejection: StreamRejection,
        statusCode: Int
    ) {
        Timber.d(
            "HQ_PROVIDER_STREAM_INVALID provider=%s id=%s quality=%dkbps reason=%s status=%d host=%s",
            providerId,
            providerTrackId,
            tier.kbps,
            rejection,
            statusCode,
            host
        )
    }

    fun selected(stream: ResolvedHighQualityStream, evaluation: AlternativeMatchEvaluation, waitedMs: Long) {
        Timber.d(
            "HQ_PROVIDER_SELECTED provider=%s id=%s quality=%dkbps estimated=%dkbps mime=%s host=%s verdict=%s confidence=%d waitedMs=%d",
            stream.providerId,
            stream.providerTrackId,
            stream.tier.kbps,
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
