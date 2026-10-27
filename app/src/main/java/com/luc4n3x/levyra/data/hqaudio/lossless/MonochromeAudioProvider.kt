package com.luc4n3x.levyra.data.hqaudio.lossless

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.HighQualityAudioProvider
import com.luc4n3x.levyra.data.hqaudio.ProviderBackendHealth
import com.luc4n3x.levyra.data.hqaudio.ProviderCircuitBreaker
import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpExchange
import com.luc4n3x.levyra.data.hqaudio.ProviderLookupOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import com.luc4n3x.levyra.data.hqaudio.StreamRejection
import java.net.URLEncoder
import org.json.JSONObject

internal class MonochromeAudioProvider(
    exchange: ProviderHttpExchange,
    private val clock: () -> Long = System::currentTimeMillis,
    circuitBreaker: ProviderCircuitBreaker = ProviderCircuitBreaker(CIRCUIT, clock, failureThreshold = 1)
) : HighQualityAudioProvider {
    override val id: String = MONOCHROME_PROVIDER_ID
    override val displayName: String = "Monochrome"
    override val lossless: Boolean = true
    override val supportsLookup: Boolean = false

    private val support = LosslessProviderSupport(id, exchange, circuitBreaker, clock)

    override suspend fun search(query: String, page: Int): ProviderSearchOutcome {
        if (page > 1) return ProviderSearchOutcome.Found(emptyList())
        val url = "$BASE_URL/search/tracks?q=${URLEncoder.encode(query, Charsets.UTF_8.name())}" +
            "&limit=${LosslessProviderSupport.SEARCH_RESULT_LIMIT}"
        return when (val fetch = support.api(url)) {
            is LosslessProviderSupport.Fetch.Failed -> ProviderSearchOutcome.Failed(fetch.failure)
            is LosslessProviderSupport.Fetch.Received -> MonochromePayloadParser.searchPage(fetch.response.bodyText)
                ?.let { ProviderSearchOutcome.Found(it) }
                ?: ProviderSearchOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
        }
    }

    override suspend fun lookup(providerTrackId: String): ProviderLookupOutcome =
        ProviderLookupOutcome.Failed(ProviderFailure.NOT_FOUND)

    override suspend fun resolveStream(candidate: AlternativeTrackCandidate): ProviderStreamOutcome {
        if (!MonochromePayloadParser.isTrackId(candidate.providerTrackId)) {
            return ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
        }
        return support.probeFlac(
            candidate = candidate,
            url = "$BASE_URL/track/${candidate.providerTrackId}",
            expiresAtMs = clock() + STREAM_TTL_MS,
            route = ROUTE
        )
    }

    override fun reportLookupTimeout() = support.reportLookupTimeout()

    override fun health(): List<ProviderBackendHealth> = listOf(support.health())

    companion object {
        const val MONOCHROME_PROVIDER_ID = "monochrome"
        const val BASE_URL = "https://tracks.monochrome.st"
        const val CIRCUIT = "monochrome-tracks"
        const val STREAM_TTL_MS = 6L * 60L * 60L * 1_000L
        private const val ROUTE = "FLAC_DIRECT"
    }
}

internal object MonochromePayloadParser {
    private val trackId = Regex("^\\d{1,24}$")

    fun isTrackId(value: String): Boolean = trackId.matches(value)

    fun searchPage(body: String): List<AlternativeTrackCandidate>? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val tracks = root.optJSONArray("tracks") ?: return null
        return (0 until tracks.length()).mapNotNull { index -> tracks.optJSONObject(index)?.let(::candidateOf) }
    }

    private fun candidateOf(json: JSONObject): AlternativeTrackCandidate? {
        if (!json.optBoolean("playable", true)) return null
        val id = json.optStringOrBlank("trackId").ifBlank { json.optStringOrBlank("id") }.takeIf(::isTrackId) ?: return null
        val title = json.optStringOrBlank("title").takeIf { it.isNotEmpty() } ?: return null
        val artistsJson = json.optJSONArray("artistNames")
        val artists = (0 until (artistsJson?.length() ?: 0))
            .mapNotNull { artistsJson?.optString(it)?.trim()?.takeIf(String::isNotEmpty) }
        if (artists.isEmpty()) return null
        val durationMs = json.optLong("duration", 0L)
        if (durationMs <= 0L) return null
        return AlternativeTrackCandidate(
            providerId = MonochromeAudioProvider.MONOCHROME_PROVIDER_ID,
            providerTrackId = id,
            title = title,
            primaryArtists = artists.take(1),
            featuredArtists = artists.drop(1),
            album = json.optStringOrBlank("albumTitle").ifBlank { json.optStringOrBlank("releaseTitle") },
            durationSeconds = ((durationMs + 500L) / 1_000L).toInt(),
            explicit = if (json.has("explicit")) json.optBoolean("explicit") else null,
            isrc = json.optStringOrBlank("isrc").uppercase()
        )
    }

    private fun JSONObject.optStringOrBlank(name: String): String =
        if (isNull(name)) "" else optString(name).trim()
}
