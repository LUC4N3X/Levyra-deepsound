package com.luc4n3x.levyra.data.hqaudio.qobuz

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackText
import java.util.Locale
import kotlin.math.roundToInt
import org.json.JSONObject

internal sealed interface QobuzSearchPayload {
    data class Tracks(val candidates: List<AlternativeTrackCandidate>) : QobuzSearchPayload
    data object Captcha : QobuzSearchPayload
    data object Rejected : QobuzSearchPayload
    data object Malformed : QobuzSearchPayload
}

internal sealed interface QobuzStreamPayload {
    data class Granted(val url: String, val bitDepth: Int?, val sampleRateHz: Int?) : QobuzStreamPayload
    data object Captcha : QobuzStreamPayload
    data object FormatUnavailable : QobuzStreamPayload
    data object Malformed : QobuzStreamPayload
}

internal object QobuzPayloadParser {
    private const val CAPTCHA_MARKER = "captcha"
    private const val MAX_SAMPLE_RATE_KHZ = 768.0

    fun looksLikeMarkup(body: String): Boolean {
        val head = body.trimStart().take(64).lowercase(Locale.ROOT)
        return head.startsWith("<!doctype") || head.startsWith("<html") || head.startsWith("<")
    }

    fun search(body: String, providerId: String): QobuzSearchPayload {
        val root = parse(body) ?: return QobuzSearchPayload.Malformed
        if (!root.optBoolean("success", false)) {
            return if (mentionsCaptcha(root)) QobuzSearchPayload.Captcha else QobuzSearchPayload.Rejected
        }
        val items = root.optJSONObject("data")?.optJSONObject("tracks")?.optJSONArray("items")
            ?: return QobuzSearchPayload.Malformed
        val candidates = (0 until items.length()).mapNotNull { index ->
            items.optJSONObject(index)?.let { candidate(it, providerId) }
        }
        return QobuzSearchPayload.Tracks(candidates)
    }

    fun stream(body: String): QobuzStreamPayload {
        val root = parse(body) ?: return QobuzStreamPayload.Malformed
        if (!root.optBoolean("success", false)) {
            return if (mentionsCaptcha(root)) QobuzStreamPayload.Captcha else QobuzStreamPayload.FormatUnavailable
        }
        val data = root.optJSONObject("data") ?: return QobuzStreamPayload.Malformed
        val url = text(data, "url") ?: return QobuzStreamPayload.FormatUnavailable
        if (!url.startsWith("https://", ignoreCase = true)) return QobuzStreamPayload.Malformed
        return QobuzStreamPayload.Granted(
            url = url,
            bitDepth = positiveInt(data, "bit_depth"),
            sampleRateHz = sampleRateHz(data, "sampling_rate")
        )
    }

    internal fun candidate(item: JSONObject, providerId: String): AlternativeTrackCandidate? {
        if (item.has("streamable") && !item.optBoolean("streamable", true)) return null
        val id = text(item, "id") ?: return null
        val baseTitle = text(item, "title")?.let(AlternativeTrackText::decodeHtmlEntities) ?: return null
        val version = text(item, "version")?.let(AlternativeTrackText::decodeHtmlEntities)
        val album = item.optJSONObject("album")
        val primary = listOfNotNull(
            item.optJSONObject("performer")?.let { text(it, "name") }
                ?: album?.optJSONObject("artist")?.let { text(it, "name") }
        )
        if (primary.isEmpty()) return null
        val duration = positiveInt(item, "duration") ?: return null
        val albumTitle = listOfNotNull(album?.let { text(it, "title") }, album?.let { text(it, "version") })
            .distinct()
            .joinToString(" ")
        return AlternativeTrackCandidate(
            providerId = providerId,
            providerTrackId = id,
            title = titleWithVersion(baseTitle, version),
            primaryArtists = primary.map(AlternativeTrackText::decodeHtmlEntities),
            featuredArtists = emptyList(),
            album = AlternativeTrackText.decodeHtmlEntities(albumTitle),
            durationSeconds = duration,
            explicit = if (item.has("parental_warning")) item.optBoolean("parental_warning") else null,
            isrc = text(item, "isrc").orEmpty(),
            offers320 = true,
            maxBitDepth = positiveInt(item, "maximum_bit_depth") ?: 0,
            maxSampleRateHz = sampleRateHz(item, "maximum_sampling_rate") ?: 0
        )
    }

    private fun titleWithVersion(title: String, version: String?): String {
        if (version.isNullOrBlank()) return title
        val normalizedVersion = AlternativeTrackText.normalize(version)
        if (normalizedVersion.isBlank() || AlternativeTrackText.normalize(title).contains(normalizedVersion)) return title
        return "$title ($version)"
    }

    private fun sampleRateHz(json: JSONObject, key: String): Int? {
        val raw = json.optDouble(key, Double.NaN).takeIf { it.isFinite() && it > 0.0 } ?: return null
        val hz = if (raw <= MAX_SAMPLE_RATE_KHZ) raw * 1_000.0 else raw
        return hz.roundToInt()
    }

    private fun positiveInt(json: JSONObject, key: String): Int? {
        val value = json.optDouble(key, Double.NaN)
        return value.takeIf { it.isFinite() && it > 0.0 }?.roundToInt()
    }

    private fun text(json: JSONObject, key: String): String? {
        if (!json.has(key) || json.isNull(key)) return null
        return json.optString(key).trim().takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }

    private fun mentionsCaptcha(root: JSONObject): Boolean =
        listOf("error", "message").any { key -> text(root, key)?.lowercase(Locale.ROOT)?.contains(CAPTCHA_MARKER) == true }

    private fun parse(body: String): JSONObject? {
        if (body.isBlank() || looksLikeMarkup(body)) return null
        return runCatching { JSONObject(body) }.getOrNull()
    }
}
