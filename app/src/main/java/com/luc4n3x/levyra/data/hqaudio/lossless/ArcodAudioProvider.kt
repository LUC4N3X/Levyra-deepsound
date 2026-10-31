package com.luc4n3x.levyra.data.hqaudio.lossless

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackText
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
import java.util.Locale
import kotlin.math.abs
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
        ProviderCircuitBreaker("arcod-qobuz", clock), clock)

    override suspend fun search(query: String, page: Int): ProviderSearchOutcome {
        if (page !in 1..MAX_SEARCH_PAGE) return ProviderSearchOutcome.Found(emptyList())
        val offset = (page - 1) * SEARCH_PAGE_SIZE
        val url = "$BASE_URL/api/get-music?q=${URLEncoder.encode(query, "UTF-8")}&offset=$offset&source=qobuz"
        return when (val result = support.api(url)) {
            is LosslessProviderSupport.Fetch.Failed -> ProviderSearchOutcome.Failed(result.failure)
            is LosslessProviderSupport.Fetch.Received -> ArcodPayloadParser.searchPage(result.response.bodyText, offset)
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
        internal const val SEARCH_PAGE_SIZE = 50
        internal const val MAX_SEARCH_PAGE = 3
        private const val BASE_URL = "https://arcod.xyz"
        private const val STREAM_TTL_MS = 5L * 60L * 1_000L
    }
}

internal object ArcodPayloadParser {
    private val numericId = Regex("^[0-9]{1,24}$")
    private val featuringCredit = Regex("\\s+(?:feat\\.?|ft\\.?|featuring)\\s+", RegexOption.IGNORE_CASE)
    fun isTrackId(value: String): Boolean = numericId.matches(value)

    fun isStreamUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme == "https" && uri.host == "api.arcod.xyz" && uri.rawUserInfo == null &&
            uri.port in listOf(-1, 443) && uri.rawPath == "/v2/stream/play" &&
            !uri.rawQuery.isNullOrBlank() && uri.rawFragment == null
    }.getOrDefault(false)

    fun searchPage(body: String, offset: Int = 0): ProviderSearchOutcome.Found? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (!root.optBoolean("success")) return null
        val tracks = root.optJSONObject("data")?.optJSONObject("tracks") ?: return null
        val items = tracks.optJSONArray("items") ?: return null
        val rows = (0 until minOf(items.length(), ArcodAudioProvider.SEARCH_PAGE_SIZE)).mapNotNull { index ->
            val row = items.optJSONObject(index) ?: return@mapNotNull null
            val track = row.optJSONObject("item") ?: row
            candidate(track)?.let { SearchCandidate(it, track.text("title"), track.text("version")) }
        }
        return ProviderSearchOutcome.Found(
            reconcileSingleVersions(rows),
            hasMore = items.length() > 0 &&
                offset + items.length().toLong() < tracks.optLong("total", 0L) &&
                offset + ArcodAudioProvider.SEARCH_PAGE_SIZE <
                ArcodAudioProvider.MAX_SEARCH_PAGE * ArcodAudioProvider.SEARCH_PAGE_SIZE
        )
    }

    private data class SearchCandidate(
        val candidate: AlternativeTrackCandidate,
        val title: String,
        val version: String
    )

    private fun reconcileSingleVersions(rows: List<SearchCandidate>): List<AlternativeTrackCandidate> {
        val unversioned = rows.filter {
            it.version.isBlank() && AlternativeTrackText.title(it.title).versionSignature.isEmpty()
        }.groupBy { normalizedIsrc(it.candidate.isrc) }
        return rows.map { row ->
            val isrc = normalizedIsrc(row.candidate.isrc)
            val baseTitle = AlternativeTrackText.title(row.title)
            val corroborated = row.version == "Single Version" && isrc.isNotEmpty() &&
                baseTitle.versionSignature.isEmpty() && unversioned[isrc].orEmpty().any { peer ->
                    val a = row.candidate
                    val b = peer.candidate
                    val peerTitle = AlternativeTrackText.title(peer.title)
                    val aExplicit = a.explicit ?: baseTitle.explicitHint
                    val bExplicit = b.explicit ?: peerTitle.explicitHint
                    a.providerTrackId != b.providerTrackId && baseTitle.core == peerTitle.core &&
                        a.primaryArtists.flatMap(AlternativeTrackText::artistNames).toSet() ==
                        b.primaryArtists.flatMap(AlternativeTrackText::artistNames).toSet() &&
                        (a.featuredArtists.flatMap(AlternativeTrackText::artistNames).toSet() + baseTitle.featuredArtists) ==
                        (b.featuredArtists.flatMap(AlternativeTrackText::artistNames).toSet() + peerTitle.featuredArtists) &&
                        (aExplicit == null || bExplicit == null || aExplicit == bExplicit) &&
                        abs(a.durationSeconds - b.durationSeconds) <= 1
                }
            if (corroborated) row.candidate.copy(title = row.title) else row.candidate
        }
    }

    private fun normalizedIsrc(value: String): String =
        value.uppercase(Locale.ROOT).filter { it in 'A'..'Z' || it in '0'..'9' }

    private fun candidate(json: JSONObject): AlternativeTrackCandidate? {
        val id = json.text("id").takeIf(::isTrackId) ?: return null
        val title = json.text("title").takeIf(String::isNotBlank) ?: return null
        val artist = json.optJSONObject("performer")?.text("name")
            ?.takeIf(String::isNotBlank) ?: return null
        val artistParts = featuringCredit.split(artist, limit = 2)
        val duration = json.optInt("duration").takeIf { it in 1..86_400 } ?: return null
        val version = json.text("version")
        return AlternativeTrackCandidate(
            providerId = "arcod", providerTrackId = id,
            title = if (version.isBlank() || title.contains(version, ignoreCase = true)) title else "$title ($version)",
            primaryArtists = listOf(artistParts.first()), featuredArtists = artistParts.drop(1),
            album = json.optJSONObject("album")?.text("title").orEmpty(),
            durationSeconds = duration,
            explicit = if (json.has("parental_warning")) json.optBoolean("parental_warning") else null,
            isrc = json.text("isrc").uppercase(java.util.Locale.ROOT)
        )
    }

    private fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key).trim()
}
