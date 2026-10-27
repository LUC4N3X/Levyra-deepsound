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
import java.net.URI
import java.net.URLEncoder
import org.json.JSONObject

internal class ArcodAudioProvider(
    exchange: ProviderHttpExchange,
    private val clock: () -> Long = System::currentTimeMillis
) : HighQualityAudioProvider {
    override val id = "arcod"
    override val displayName = "Arcod"
    override val lossless = true
    override val supportsLookup = false
    private val support = LosslessProviderSupport(id, exchange,
        ProviderCircuitBreaker("arcod-qobuz", clock, failureThreshold = 1), clock)

    override suspend fun search(query: String, page: Int): ProviderSearchOutcome {
        if (page > 1) return ProviderSearchOutcome.Found(emptyList())
        val url = "$BASE_URL/api/get-music?q=${URLEncoder.encode(query, "UTF-8")}&offset=0&source=qobuz"
        return when (val result = support.api(url)) {
            is LosslessProviderSupport.Fetch.Failed -> ProviderSearchOutcome.Failed(result.failure)
            is LosslessProviderSupport.Fetch.Received -> ArcodPayloadParser.searchPage(result.response.bodyText)
                ?.let { ProviderSearchOutcome.Found(it) }
                ?: ProviderSearchOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
        }
    }

    override suspend fun lookup(providerTrackId: String): ProviderLookupOutcome = ProviderLookupOutcome.Missing

    override suspend fun resolveStream(candidate: AlternativeTrackCandidate): ProviderStreamOutcome {
        if (candidate.providerId != id || !ArcodPayloadParser.isTrackId(candidate.providerTrackId)) {
            return ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
        }
        val result = support.api("$BASE_URL/api/player/stream/${candidate.providerTrackId}?quality=6")
        if (result is LosslessProviderSupport.Fetch.Failed) return ProviderStreamOutcome.Failed(result.failure)
        val body = (result as LosslessProviderSupport.Fetch.Received).response.bodyText
        val url = runCatching { JSONObject(body).optString("url") }.getOrNull()
            ?.takeIf(ArcodPayloadParser::isStreamUrl)
            ?: return ProviderStreamOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
        return support.probeFlac(candidate, url, clock() + STREAM_TTL_MS, "FLAC_CD")
    }

    override fun reportLookupTimeout() = support.reportLookupTimeout()
    override fun health(): List<ProviderBackendHealth> = listOf(support.health())

    companion object {
        private const val BASE_URL = "https://arcod.xyz"
        private const val STREAM_TTL_MS = 5L * 60L * 1_000L
    }
}

internal object ArcodPayloadParser {
    private val numericId = Regex("^[0-9]{1,24}$")
    fun isTrackId(value: String): Boolean = numericId.matches(value)

    fun isStreamUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme == "https" && uri.host == "api.arcod.xyz" && uri.rawUserInfo == null &&
            uri.port in listOf(-1, 443) && uri.rawPath == "/v2/stream/play" &&
            !uri.rawQuery.isNullOrBlank() && uri.rawFragment == null
    }.getOrDefault(false)

    fun searchPage(body: String): List<AlternativeTrackCandidate>? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (!root.optBoolean("success")) return null
        val items = root.optJSONObject("data")?.optJSONObject("tracks")?.optJSONArray("items") ?: return null
        return (0 until minOf(items.length(), 50)).mapNotNull { index ->
            val row = items.optJSONObject(index) ?: return@mapNotNull null
            candidate(row.optJSONObject("item") ?: row)
        }
    }

    private fun candidate(json: JSONObject): AlternativeTrackCandidate? {
        val id = json.text("id").takeIf(::isTrackId) ?: return null
        val title = json.text("title").takeIf(String::isNotBlank) ?: return null
        val artist = json.optJSONObject("performer")?.text("name")
            ?.takeIf(String::isNotBlank) ?: return null
        val duration = json.optInt("duration").takeIf { it in 1..86_400 } ?: return null
        val version = json.text("version")
        return AlternativeTrackCandidate(
            providerId = "arcod", providerTrackId = id,
            title = if (version.isBlank() || title.contains(version, ignoreCase = true)) title else "$title ($version)",
            primaryArtists = listOf(artist), featuredArtists = emptyList(),
            album = json.optJSONObject("album")?.text("title").orEmpty(),
            durationSeconds = duration,
            explicit = if (json.has("parental_warning")) json.optBoolean("parental_warning") else null,
            isrc = json.text("isrc").uppercase(java.util.Locale.ROOT)
        )
    }

    private fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key).trim()
}
