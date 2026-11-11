package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricsTranslationState
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray

internal class OnlineLyricsTextService(private val client: OkHttpClient) : LyricsTranslationBackend {
    override val version: String = "online-gtx-v1"

    override suspend fun translate(texts: List<String>, targetLanguageTag: String): LyricsBatchTranslation {
        if (texts.isEmpty()) return LyricsBatchTranslation.Unavailable(LyricsTranslationState.UNAVAILABLE)
        val target = onlineLanguageCode(targetLanguageTag)
            ?: return LyricsBatchTranslation.Unavailable(LyricsTranslationState.UNAVAILABLE)
        val translated = ArrayList<String?>(texts.size)
        var detected: String? = null
        var reachable = false
        for (chunk in chunked(texts)) {
            val response = request(target, "auto", listOf("t"), chunk.joinToString(LINE_SEPARATOR))
            if (response == null) {
                translated += List(chunk.size) { null }
                continue
            }
            reachable = true
            if (detected == null) detected = response.optString(2).takeIf(String::isNotBlank)
            translated += splitLines(joinedTranslation(response), chunk.size)
        }
        if (!reachable) return LyricsBatchTranslation.Unavailable(LyricsTranslationState.FAILED)
        val source = detected
        if (source != null && sameTranslationLanguageVariant(source, targetLanguageTag)) {
            return LyricsBatchTranslation.SameLanguage
        }
        return LyricsBatchTranslation.Success(translated, LyricsTranslationState.ONLINE)
    }

    suspend fun japaneseReadings(texts: List<String>): List<String?> {
        if (texts.isEmpty()) return emptyList()
        val readings = ArrayList<String?>(texts.size)
        for (chunk in chunked(texts)) {
            val response = request("ja", "ja", listOf("rm"), chunk.joinToString(READING_SEPARATOR))
            val reading = response?.let(::sourceTransliteration)
            val parts = reading?.split(READING_SPLIT)?.map(::cleanReading)
            if (parts == null || parts.size != chunk.size) {
                readings += List(chunk.size) { null }
            } else {
                readings += parts.map { it.takeIf(String::isNotBlank) }
            }
        }
        return readings
    }

    private fun chunked(texts: List<String>): List<List<String>> {
        val chunks = ArrayList<List<String>>()
        var current = ArrayList<String>()
        var length = 0
        texts.forEach { text ->
            val line = text.replace('\n', ' ').replace(READING_MARKER, " ").trim()
            if (current.isNotEmpty() && length + line.length > MAX_CHUNK_CHARS) {
                chunks += current
                current = ArrayList()
                length = 0
            }
            current += line
            length += line.length + READING_SEPARATOR.length
        }
        if (current.isNotEmpty()) chunks += current
        return chunks
    }

    private suspend fun request(target: String, source: String, kinds: List<String>, text: String): JSONArray? {
        val url = ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("client", "gtx")
            .addQueryParameter("sl", source)
            .addQueryParameter("tl", target)
            .apply { kinds.forEach { addQueryParameter("dt", it) } }
            .build()
        val request = Request.Builder()
            .url(url)
            .post(FormBody.Builder().add("q", text).build())
            .build()
        val body = suspendCancellableCoroutine<String?> { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) continuation.resume(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val result = runCatching {
                            response.use { if (it.isSuccessful) it.body.string() else null }
                        }.getOrNull()
                        if (continuation.isActive) continuation.resume(result)
                    }
                }
            )
        } ?: return null
        return runCatching { JSONArray(body) }.getOrNull()
    }

    companion object {
        private const val ENDPOINT = "https://translate.googleapis.com/translate_a/single"
        private const val MAX_CHUNK_CHARS = 3_000
        private const val LINE_SEPARATOR = "\n"
        private const val READING_MARKER = "###"
        private const val READING_SEPARATOR = "\n###\n"
        private val READING_SPLIT = Regex("\\s*#\\s*#\\s*#\\s*")
        private val SMALL_TSU = Regex("~tsu\\s*([bcdfghjkmprstwz])", RegexOption.IGNORE_CASE)
        private val SPACE_BEFORE_PUNCTUATION = Regex("\\s+([,.!?;:…])")
        private val WHITESPACE = Regex("\\s+")

        internal fun cleanReading(raw: String): String = raw
            .replace(SMALL_TSU) { match -> match.groupValues[1].lowercase(Locale.ROOT).repeat(2) }
            .replace("~", "")
            .replace(SPACE_BEFORE_PUNCTUATION, "$1")
            .replace(WHITESPACE, " ")
            .trim()
            .lowercase(Locale.ROOT)

        internal fun joinedTranslation(response: JSONArray): String {
            val segments = response.optJSONArray(0) ?: return ""
            return buildString {
                for (index in 0 until segments.length()) {
                    val segment = segments.optJSONArray(index) ?: continue
                    if (segment.isNull(0)) continue
                    append(segment.optString(0))
                }
            }
        }

        internal fun sourceTransliteration(response: JSONArray): String? {
            val segments = response.optJSONArray(0) ?: return null
            for (index in 0 until segments.length()) {
                val segment = segments.optJSONArray(index) ?: continue
                if (segment.length() > 3 && !segment.isNull(3)) {
                    return segment.optString(3).takeIf(String::isNotBlank)
                }
            }
            return null
        }

        internal fun splitLines(joined: String, expected: Int): List<String?> {
            val lines = joined.trimEnd('\n').split('\n').map(String::trim)
            return if (lines.size == expected) lines.map { it.takeIf(String::isNotEmpty) } else List(expected) { null }
        }

        internal fun onlineLanguageCode(languageTag: String): String? {
            val canonical = canonicalTranslationLanguageTag(languageTag)
            val locale = Locale.forLanguageTag(canonical)
            val language = locale.language.lowercase(Locale.ROOT).takeIf(String::isNotBlank) ?: return null
            return when {
                language == "zh" && locale.script.equals("Hant", ignoreCase = true) -> "zh-TW"
                language == "zh" -> "zh-CN"
                language == "fil" -> "tl"
                else -> language
            }
        }
    }
}

internal class FallbackLyricsTranslationBackend(
    private val primary: LyricsTranslationBackend?,
    private val online: LyricsTranslationBackend
) : LyricsTranslationBackend {
    override val version: String = listOfNotNull(primary?.version, online.version).joinToString("+")

    override suspend fun translate(texts: List<String>, targetLanguageTag: String): LyricsBatchTranslation {
        val local = primary?.translate(texts, targetLanguageTag)
        if (local == LyricsBatchTranslation.SameLanguage) return local
        if (local is LyricsBatchTranslation.Success && local.texts.count { it == null } <= texts.size / 10) return local
        val remote = online.translate(texts, targetLanguageTag)
        return if (remote is LyricsBatchTranslation.Unavailable && local != null) local else remote
    }
}
