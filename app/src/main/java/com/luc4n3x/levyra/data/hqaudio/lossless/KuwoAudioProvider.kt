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
import org.json.JSONArray
import org.json.JSONObject

internal class KuwoAudioProvider(
    exchange: ProviderHttpExchange,
    private val clock: () -> Long = System::currentTimeMillis,
    circuitBreaker: ProviderCircuitBreaker = ProviderCircuitBreaker(CIRCUIT, clock)
) : HighQualityAudioProvider {
    override val id: String = KUWO_PROVIDER_ID
    override val displayName: String = "Kuwo"
    override val lossless: Boolean = true
    override val supportsLookup: Boolean = false

    private val support = LosslessProviderSupport(id, exchange, circuitBreaker, clock)

    override suspend fun search(query: String, page: Int): ProviderSearchOutcome {
        if (page !in 1..2) return ProviderSearchOutcome.Found(emptyList())
        val form = mapOf("platform" to PLATFORM, "keyword" to query, "page" to "$page", "size" to "$PAGE_SIZE")
        return when (val fetch = support.api("$ENDPOINT?act=search", form, SITE_HEADERS)) {
            is LosslessProviderSupport.Fetch.Failed -> ProviderSearchOutcome.Failed(fetch.failure)
            is LosslessProviderSupport.Fetch.Received -> KuwoPayloadParser.searchPage(fetch.response.bodyText)
                ?.let { ProviderSearchOutcome.Found(it, hasMore = it.size >= PAGE_SIZE) }
                ?: ProviderSearchOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
        }
    }

    override suspend fun lookup(providerTrackId: String): ProviderLookupOutcome =
        ProviderLookupOutcome.Failed(ProviderFailure.NOT_FOUND)

    override suspend fun resolveStream(candidate: AlternativeTrackCandidate): ProviderStreamOutcome {
        val signature = KuwoPayloadParser.signature(candidate.mediaToken)
        if (!KuwoPayloadParser.isTrackId(candidate.providerTrackId) || signature == null) {
            return ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
        }
        val form = mapOf(
            "platform" to PLATFORM, "songid" to candidate.providerTrackId, "format" to "flac",
            "bitrate" to signature.bitrate, "time" to signature.time, "sign" to signature.sign
        )
        val url = when (val fetch = support.api("$ENDPOINT?act=getUrl", form, SITE_HEADERS)) {
            is LosslessProviderSupport.Fetch.Failed -> return ProviderStreamOutcome.Failed(fetch.failure)
            is LosslessProviderSupport.Fetch.Received -> KuwoPayloadParser.flacUrl(fetch.response.bodyText, candidate.providerTrackId)
                ?: return ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
        }
        return support.probeFlac(candidate, url, clock() + STREAM_TTL_MS, ROUTE)
    }

    override fun reportLookupTimeout() = support.reportLookupTimeout()

    override fun health(): List<ProviderBackendHealth> = listOf(support.health())

    companion object {
        const val KUWO_PROVIDER_ID = "kuwo"
        const val ENDPOINT = "https://flac.music.hi.cn/ajax.php"
        const val CIRCUIT = "kuwo-hicn"
        const val STREAM_TTL_MS = 30L * 60L * 1_000L
        const val PLATFORM = "kuwo"
        const val PAGE_SIZE = 20
        private const val ROUTE = "FLAC_DIRECT"
        private val SITE_HEADERS = mapOf(
            "Accept" to "application/json",
            "Origin" to "https://flac.music.hi.cn",
            "Referer" to "https://flac.music.hi.cn/"
        )
    }
}

internal object KuwoPayloadParser {
    data class Signature(val time: String, val sign: String, val bitrate: String)

    private val trackId = Regex("^\\d{1,24}$")
    private val signValue = Regex("^[0-9a-f]{32}$")
    private val whitespace = Regex("\\s+")
    private val explicitEdition = Regex("\\s*[(\\[]explicit[)\\]]\\s*$", RegexOption.IGNORE_CASE)
    private val versionSuffix = Regex("^[(\\[][^()\\[\\]]{2,60}[)\\]]$")
    private val entities = mapOf("&amp;" to "&", "&quot;" to "\"", "&#39;" to "'", "&apos;" to "'", "&nbsp;" to " ", "&lt;" to "<", "&gt;" to ">")

    fun isTrackId(value: String): Boolean = trackId.matches(value)

    fun signature(token: String): Signature? {
        val parts = token.split('|')
        if (parts.size != 3 || parts[0].toLongOrNull() == null || !signValue.matches(parts[1]) || parts[2].toIntOrNull() == null) return null
        return Signature(parts[0], parts[1], parts[2])
    }

    fun searchPage(body: String): List<AlternativeTrackCandidate>? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (root.optInt("code", -1) != 0) return null
        val rows = root.optJSONObject("data")?.optJSONArray("list") ?: return emptyList()
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.let(::candidateOf) }
    }

    fun flacUrl(body: String, songId: String): String? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (root.optInt("code", -1) != 0) return null
        val data = root.optJSONObject("data") ?: return null
        if (!data.optString("format").equals("flac", true) || data.optString("songid") != songId) return null
        return data.optString("url").takeIf { it.startsWith("https://") }
    }

    private fun candidateOf(json: JSONObject): AlternativeTrackCandidate? {
        val id = json.optString("id").takeIf(::isTrackId) ?: return null
        val bitrate = flacBitrate(json) ?: return null
        val time = json.optString("time").takeIf { it.toLongOrNull() != null } ?: return null
        val sign = json.optString("sign").takeIf(signValue::matches) ?: return null
        val title = decode(json.optString("name")).takeIf { it.isNotEmpty() } ?: return null
        val artists = decode(json.optString("artist")).split('&', '、').map(String::trim).filter(String::isNotEmpty)
        if (artists.isEmpty()) return null
        val duration = json.optString("duration").toIntOrNull()?.takeIf { it in 1..86_400 } ?: return null
        val rawAlbum = decode(json.optString("album_name"))
        val album = rawAlbum.replace(explicitEdition, "").trim()
        val albumVersion = album.takeIf { it.startsWith(title, ignoreCase = true) }
            ?.substring(title.length)?.trim()?.takeIf { versionSuffix.matches(it) }
        return AlternativeTrackCandidate(
            providerId = KuwoAudioProvider.KUWO_PROVIDER_ID,
            providerTrackId = id,
            title = if (albumVersion != null && !title.contains(albumVersion, ignoreCase = true)) "$title $albumVersion" else title,
            primaryArtists = artists.take(1),
            featuredArtists = artists.drop(1),
            album = album,
            durationSeconds = duration,
            explicit = if (album != rawAlbum) true else null,
            artistRolesUnknown = true,
            mediaToken = "$time|$sign|$bitrate"
        )
    }

    private fun flacBitrate(json: JSONObject): Int? {
        val levels = json.optJSONArray("minfo") ?: runCatching { JSONArray(json.optString("minfo")) }.getOrNull() ?: return null
        return (0 until levels.length()).mapNotNull { levels.optJSONObject(it) }
            .filter { it.optString("format").equals("flac", true) }
            .mapNotNull { it.optString("bitrate").toIntOrNull()?.takeIf { rate -> rate in 700..20_000 } }
            .maxOrNull()
    }

    private fun decode(value: String): String = entities.entries.fold(value) { text, (entity, plain) -> text.replace(entity, plain) }
        .replace(whitespace, " ").trim()
}
