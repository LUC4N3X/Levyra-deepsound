package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.security.GoogleApiKeyHeaders
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricWord
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class YoutubeTranscriptLyricsProvider(context: Context) {
    private val appContext = context.applicationContext
    private val client = LevyraHttpClientFactory.youtubePlayer()

    fun fetch(videoId: String, languageCode: String, translate: Boolean): TranscriptResult? {
        if (videoId.isBlank()) return null
        val player = playerResponse(videoId) ?: return null
        val captions = player.optJSONObject("captions")
            ?.optJSONObject("playerCaptionsTracklistRenderer")
            ?: return null
        val tracks = captions.optJSONArray("captionTracks") ?: return null
        val language = languageCode.lowercase(Locale.ROOT).substringBefore('-')
        val selected = selectTrack(tracks, language) ?: return null
        val original = loadTrack(selected.optString("baseUrl"), null)
        if (original.isEmpty()) return null
        val translated = if (translate && selected.optBoolean("isTranslatable") && language.isNotBlank() && !selected.optString("languageCode").startsWith(language, true)) {
            loadTrack(selected.optString("baseUrl"), language)
        } else {
            emptyList()
        }
        val merged = if (translated.isEmpty()) original else mergeTranslation(original, translated)
        val sourceLanguage = selected.optString("languageCode").ifBlank { "auto" }
        return TranscriptResult(
            lines = merged,
            sourceLanguage = sourceLanguage,
            translated = translated.isNotEmpty(),
            automatic = selected.optString("kind").equals("asr", true)
        )
    }

    private fun playerResponse(videoId: String): JSONObject? {
        val body = JSONObject()
            .put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", "WEB")
                        .put("clientVersion", CLIENT_VERSION)
                        .put("hl", "en")
                        .put("gl", "US")
                )
            )
            .put("videoId", videoId)
            .put("contentCheckOk", true)
            .put("racyCheckOk", true)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player?key=${BuildConfig.YOUTUBE_INNERTUBE_API_KEY}&prettyPrint=false")
            .post(body)
            .header("Accept", "application/json")
            .header("Origin", "https://www.youtube.com")
            .header("Referer", "https://www.youtube.com/watch?v=$videoId")
            .header("User-Agent", USER_AGENT)
            .header("X-Youtube-Client-Name", "1")
            .header("X-Youtube-Client-Version", CLIENT_VERSION)
        GoogleApiKeyHeaders.applyTo(requestBuilder, appContext)
        return client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string()?.takeIf { it.isNotBlank() }?.let(::JSONObject)
        }
    }

    private fun selectTrack(tracks: JSONArray, language: String): JSONObject? {
        val candidates = buildList {
            for (index in 0 until tracks.length()) tracks.optJSONObject(index)?.let(::add)
        }
        return candidates.sortedWith(
            compareByDescending<JSONObject> { it.optString("languageCode").startsWith(language, true) }
                .thenBy { it.optString("kind").equals("asr", true) }
                .thenByDescending { it.optString("vssId").startsWith(".") }
        ).firstOrNull()
    }

    private fun loadTrack(baseUrl: String, translationLanguage: String?): List<LyricLine> {
        if (baseUrl.isBlank()) return emptyList()
        val separator = if (baseUrl.contains('?')) "&" else "?"
        val translation = translationLanguage?.takeIf { it.isNotBlank() }
            ?.let { "&tlang=${URLEncoder.encode(it, StandardCharsets.UTF_8.name())}" }
            .orEmpty()
        val url = "$baseUrl${separator}fmt=json3$translation"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val root = response.body?.string()?.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: return emptyList()
            parseEvents(root.optJSONArray("events") ?: JSONArray())
        }
    }

    private fun parseEvents(events: JSONArray): List<LyricLine> {
        val raw = ArrayList<LyricLine>()
        for (index in 0 until events.length()) {
            val event = events.optJSONObject(index) ?: continue
            val start = event.optLong("tStartMs", -1L)
            if (start < 0L) continue
            val segments = event.optJSONArray("segs") ?: continue
            val words = ArrayList<LyricWord>()
            val textBuilder = StringBuilder()
            for (segmentIndex in 0 until segments.length()) {
                val segment = segments.optJSONObject(segmentIndex) ?: continue
                val value = segment.optString("utf8")
                if (value.isBlank() || value == "\n") continue
                textBuilder.append(value)
                val wordStart = start + segment.optLong("tOffsetMs", 0L)
                val wordDuration = segment.optLong("dDurationMs", 0L).coerceAtLeast(120L)
                val clean = value.replace(Regex("\\s+"), " ").trim()
                if (clean.isNotBlank()) words += LyricWord(wordStart, wordStart + wordDuration, clean)
            }
            val text = textBuilder.toString().replace(Regex("\\s+"), " ").trim()
            if (text.isBlank() || text == "[Music]") continue
            val duration = event.optLong("dDurationMs", 0L).coerceAtLeast(900L)
            raw += LyricLine(start, start + duration, text, "", words)
        }
        val sorted = raw.distinctBy { it.startMs to it.text }.sortedBy { it.startMs }
        return sorted.mapIndexed { index, line ->
            val next = sorted.getOrNull(index + 1)?.startMs
            line.copy(endMs = next?.minus(60L)?.coerceAtLeast(line.startMs + 500L) ?: line.endMs.coerceAtLeast(line.startMs + 900L))
        }
    }

    private fun mergeTranslation(original: List<LyricLine>, translated: List<LyricLine>): List<LyricLine> {
        return original.map { line ->
            val match = translated.minByOrNull { kotlin.math.abs(it.startMs - line.startMs) }
                ?.takeIf { kotlin.math.abs(it.startMs - line.startMs) <= 2_500L }
            line.copy(translated = match?.text.orEmpty())
        }
    }

    data class TranscriptResult(
        val lines: List<LyricLine>,
        val sourceLanguage: String,
        val translated: Boolean,
        val automatic: Boolean
    )

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val CLIENT_VERSION = "2.20260630.00.00"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Mobile Safari/537.36"
    }
}
