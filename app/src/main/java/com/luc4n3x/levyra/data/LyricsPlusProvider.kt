package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.domain.LyricWord
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.math.absoluteValue

internal data class LyricsPlusProviderResult(
    val lines: List<LyricLine>,
    val synced: Boolean,
    val provider: String,
    val confidence: Int,
    val title: String,
    val artist: String,
    val durationSec: Long
)

internal data class LyricsPlusProviderOutcome(
    val results: List<LyricsPlusProviderResult> = emptyList(),
    val attempted: Boolean = false,
    val hadTransientFailure: Boolean = false
)

internal class LyricsPlusProvider(private val client: OkHttpClient) {
    @Volatile
    private var lastWorkingServer: String? = null

    suspend fun fetchMirrors(
        title: String,
        artist: String,
        album: String,
        durationSec: Long
    ): LyricsPlusProviderOutcome = supervisorScope {
        if (title.isBlank() || artist.isBlank()) return@supervisorScope LyricsPlusProviderOutcome()
        var transientFailure = false
        val preferred = lastWorkingServer?.takeIf { it in SERVERS }
        if (preferred != null) {
            val preferredOutcome = withTimeoutOrNull(PREFERRED_SERVER_TIMEOUT_MS) {
                fetchMirror(preferred, title, artist, album, durationSec)
            } ?: EndpointOutcome.Failure
            when (preferredOutcome) {
                is EndpointOutcome.Found -> return@supervisorScope LyricsPlusProviderOutcome(
                    results = listOf(preferredOutcome.result),
                    attempted = true
                )
                EndpointOutcome.Empty -> Unit
                EndpointOutcome.Failure -> transientFailure = true
            }
        }
        val servers = SERVERS.filterNot { it == preferred }
        val tasks = servers.map { server ->
            async { server to fetchMirror(server, title, artist, album, durationSec) }
        }.toMutableList()
        while (tasks.isNotEmpty()) {
            val completed = select<Pair<Deferred<Pair<String, EndpointOutcome>>, Pair<String, EndpointOutcome>>> {
                tasks.forEach { task ->
                    task.onAwait { value -> task to value }
                }
            }
            tasks.remove(completed.first)
            val server = completed.second.first
            when (val outcome = completed.second.second) {
                is EndpointOutcome.Found -> {
                    lastWorkingServer = server
                    tasks.forEach { it.cancel() }
                    return@supervisorScope LyricsPlusProviderOutcome(
                        results = listOf(outcome.result),
                        attempted = true,
                        hadTransientFailure = transientFailure
                    )
                }
                EndpointOutcome.Empty -> Unit
                EndpointOutcome.Failure -> transientFailure = true
            }
        }
        LyricsPlusProviderOutcome(attempted = true, hadTransientFailure = transientFailure)
    }

    suspend fun fetchBinimum(
        title: String,
        artist: String,
        album: String,
        durationSec: Long
    ): LyricsPlusProviderOutcome {
        if (title.isBlank() || artist.isBlank()) return LyricsPlusProviderOutcome()
        val endpoint = BINIMUM_API_BASE.toHttpUrl().newBuilder()
            .addQueryParameter("track", title)
            .addQueryParameter("artist", artist)
            .apply {
                if (album.isNotBlank()) addQueryParameter("album", album)
                if (durationSec > 0L) addQueryParameter("duration", durationSec.toString())
            }
            .build()
        return when (val response = httpGet(endpoint.toString(), "application/json")) {
            is HttpOutcome.Success -> parseBinimumSearch(response.body, title, artist, durationSec)
            HttpOutcome.NotFound -> LyricsPlusProviderOutcome(attempted = true)
            HttpOutcome.Failure -> LyricsPlusProviderOutcome(attempted = true, hadTransientFailure = true)
        }
    }

    internal fun parseMirrorPayload(
        body: String,
        provider: String,
        fallbackTitle: String,
        fallbackArtist: String,
        fallbackDurationSec: Long
    ): LyricsPlusProviderResult? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val lyrics = root.optJSONArray("lyrics") ?: return null
        if (lyrics.length() == 0) return null
        val type = root.optString("type").trim()
        val metadata = root.optJSONObject("metadata")
        val title = metadata?.optString("title")?.trim().orEmpty().ifBlank { fallbackTitle }
        val durationSec = parseDurationSeconds(metadata?.optString("totalDuration").orEmpty())
            .takeIf { it > 0L }
            ?: fallbackDurationSec
        if (type.equals("None", ignoreCase = true)) {
            val text = buildString {
                for (index in 0 until lyrics.length()) {
                    val value = lyrics.optJSONObject(index)?.optString("text").orEmpty().trim()
                    if (value.isNotBlank()) append(value).append('\n')
                }
            }.trim()
            val lines = UnifiedLyricsParser.parsePlain(text)
            if (lines.isEmpty()) return null
            return LyricsPlusProviderResult(lines, false, provider, 72, title, fallbackArtist, durationSec)
        }

        val singerOrder = LinkedHashSet<String>()
        for (index in 0 until lyrics.length()) {
            lyrics.optJSONObject(index)
                ?.optJSONObject("element")
                ?.optString("singer")
                ?.trim()
                ?.lowercase(Locale.ROOT)
                ?.takeIf(String::isNotBlank)
                ?.let(singerOrder::add)
        }
        val roleBySinger = singerOrder.mapIndexed { index, singer ->
            singer to when {
                singerOrder.size <= 1 -> LyricVocalRole.MAIN
                index % 2 == 0 -> LyricVocalRole.DUET_LEFT
                else -> LyricVocalRole.DUET_RIGHT
            }
        }.toMap()
        val lines = ArrayList<LyricLine>(lyrics.length() * 2)
        for (index in 0 until lyrics.length()) {
            val line = lyrics.optJSONObject(index) ?: continue
            val startMs = line.optLong("time", 0L).coerceAtLeast(0L)
            val durationMs = line.optLong("duration", 0L).coerceAtLeast(0L)
            val fallbackEnd = (startMs + durationMs.coerceAtLeast(900L)).coerceAtLeast(startMs + 120L)
            val wordsArray = line.optJSONArray("syllabus")
            val mainWords = parseWords(wordsArray, background = false, startMs)
            val backgroundWords = parseWords(wordsArray, background = true, startMs)
            val fullBackground = wordsArray != null && mainWords.isEmpty() && backgroundWords.isNotEmpty()
            val mainText = when {
                mainWords.isNotEmpty() -> mainWords.joinToString("") { it.text }.normalizeLyricsText()
                fullBackground -> ""
                else -> line.optString("text").normalizeLyricsText()
            }
            if (mainText.isNotBlank()) {
                val singer = line.optJSONObject("element")
                    ?.optString("singer")
                    ?.trim()
                    ?.lowercase(Locale.ROOT)
                    .orEmpty()
                val role = roleBySinger[singer] ?: LyricVocalRole.MAIN
                lines += LyricLine(
                    startMs = startMs,
                    endMs = mainWords.lastOrNull()?.endMs?.coerceAtLeast(fallbackEnd) ?: fallbackEnd,
                    text = mainText,
                    translated = line.optJSONObject("translation")?.optString("text").orEmpty().normalizeLyricsText(),
                    words = mainWords,
                    romanized = line.optJSONObject("transliteration")?.optString("text").orEmpty().normalizeLyricsText(),
                    role = role
                )
            }
            if (backgroundWords.isNotEmpty()) {
                val backgroundStart = backgroundWords.minOf { it.startMs }
                val backgroundEnd = backgroundWords.maxOf { it.endMs }.coerceAtLeast(backgroundStart + 120L)
                val backgroundText = backgroundWords.joinToString("") { it.text }.normalizeLyricsText()
                if (backgroundText.isNotBlank()) {
                    lines += LyricLine(
                        startMs = backgroundStart,
                        endMs = backgroundEnd,
                        text = backgroundText,
                        translated = "",
                        words = backgroundWords,
                        romanized = "",
                        role = LyricVocalRole.BACKGROUND
                    )
                }
            }
        }
        val cleaned = LyricsCleaner.clean(lines)
        if (cleaned.isEmpty()) return null
        val wordSynced = cleaned.any { it.words.isNotEmpty() }
        val synced = wordSynced || cleaned.distinctBy { it.startMs }.size > 1
        val confidence = when {
            wordSynced -> 96
            synced -> 89
            else -> 72
        }
        return LyricsPlusProviderResult(cleaned, synced, provider, confidence, title, fallbackArtist, durationSec)
    }

    private suspend fun fetchMirror(
        server: String,
        title: String,
        artist: String,
        album: String,
        durationSec: Long
    ): EndpointOutcome {
        val endpoint = "${server.trimEnd('/')}/v2/lyrics/get".toHttpUrl().newBuilder()
            .addQueryParameter("title", title)
            .addQueryParameter("artist", artist)
            .apply {
                if (durationSec > 0L) addQueryParameter("duration", durationSec.toString())
                if (album.isNotBlank()) addQueryParameter("album", album)
            }
            .build()
        return when (val response = httpGet(endpoint.toString(), "application/json")) {
            is HttpOutcome.Success -> {
                val host = endpoint.host
                val result = parseMirrorPayload(response.body, "LyricsPlus · $host", title, artist, durationSec)
                if (result == null) EndpointOutcome.Empty else EndpointOutcome.Found(result)
            }
            HttpOutcome.NotFound -> EndpointOutcome.Empty
            HttpOutcome.Failure -> EndpointOutcome.Failure
        }
    }

    private suspend fun parseBinimumSearch(
        body: String,
        fallbackTitle: String,
        fallbackArtist: String,
        fallbackDurationSec: Long
    ): LyricsPlusProviderOutcome {
        val root = runCatching { JSONObject(body) }.getOrNull()
            ?: return LyricsPlusProviderOutcome(attempted = true, hadTransientFailure = true)
        val results = root.optJSONArray("results")
            ?: return LyricsPlusProviderOutcome(attempted = true)
        val selected = selectBinimumResult(
            results = results,
            title = fallbackTitle,
            artist = fallbackArtist,
            durationSec = fallbackDurationSec
        )
            ?: return LyricsPlusProviderOutcome(attempted = true)
        val lyricsUrl = selected.optString("lyricsUrl").trim()
        if (lyricsUrl.isBlank()) return LyricsPlusProviderOutcome(attempted = true)
        return when (val response = httpGet(lyricsUrl, "application/xml,text/xml,text/plain,*/*")) {
            is HttpOutcome.Success -> {
                val lines = runCatching { UnifiedLyricsParser.parse(response.body) }
                    .onFailure { Timber.w(it, "Binimum lyrics parsing failed") }
                    .getOrDefault(emptyList())
                if (lines.isEmpty()) {
                    LyricsPlusProviderOutcome(attempted = true, hadTransientFailure = true)
                } else {
                    val timingType = selected.optString("timing_type").lowercase(Locale.ROOT)
                    val wordSynced = lines.any { it.words.isNotEmpty() } || timingType == "word"
                    val synced = wordSynced || timingType == "line" || lines.distinctBy { it.startMs }.size > 1
                    val provider = when {
                        wordSynced -> "Binimum · Word"
                        synced -> "Binimum · Line"
                        else -> "Binimum · Plain"
                    }
                    val result = LyricsPlusProviderResult(
                        lines = lines,
                        synced = synced,
                        provider = provider,
                        confidence = if (wordSynced) 97 else if (synced) 90 else 70,
                        title = selected.optString("track_name").trim().ifBlank { fallbackTitle },
                        artist = selected.optString("artist_name").trim().ifBlank { fallbackArtist },
                        durationSec = normalizeDurationSeconds(selected.optLong("duration", fallbackDurationSec))
                    )
                    LyricsPlusProviderOutcome(results = listOf(result), attempted = true)
                }
            }
            HttpOutcome.NotFound -> LyricsPlusProviderOutcome(attempted = true)
            HttpOutcome.Failure -> LyricsPlusProviderOutcome(attempted = true, hadTransientFailure = true)
        }
    }

    private fun selectBinimumResult(
        results: JSONArray,
        title: String,
        artist: String,
        durationSec: Long
    ): JSONObject? {
        var best: JSONObject? = null
        var bestScore = Int.MIN_VALUE
        for (index in 0 until results.length()) {
            val candidate = results.optJSONObject(index) ?: continue
            val lyricsUrl = candidate.optString("lyricsUrl").trim()
            if (!lyricsUrl.startsWith("https://", ignoreCase = true)) continue
            val timing = candidate.optString("timing_type").lowercase(Locale.ROOT)
            val timingScore = when (timing) {
                "word" -> 30
                "line" -> 20
                else -> 5
            }
            val titleScore = LyricsMatcher.similarity(candidate.optString("track_name"), title) * 38 / 100
            val artistScore = LyricsMatcher.similarity(candidate.optString("artist_name"), artist) * 28 / 100
            val candidateDuration = normalizeDurationSeconds(candidate.optLong("duration", 0L))
            val durationScore = if (durationSec > 0L && candidateDuration > 0L) {
                when ((candidateDuration - durationSec).absoluteValue) {
                    in 0L..2L -> 16
                    in 3L..5L -> 13
                    in 6L..10L -> 8
                    in 11L..18L -> 2
                    in 19L..30L -> -8
                    else -> -18
                }
            } else {
                0
            }
            val mismatchPenalty = LyricsMatcher.versionMismatchPenalty(candidate.optString("track_name"), title)
            val score = titleScore + artistScore + timingScore + durationScore - mismatchPenalty
            if (score > bestScore) {
                best = candidate
                bestScore = score
            }
        }
        return best?.takeIf { bestScore >= MIN_BINIMUM_MATCH_SCORE }
    }

    private fun parseWords(
        words: JSONArray?,
        background: Boolean,
        lineStartMs: Long
    ): List<LyricWord> {
        if (words == null) return emptyList()
        val parsed = ArrayList<LyricWord>(words.length())
        for (index in 0 until words.length()) {
            val word = words.optJSONObject(index) ?: continue
            if (word.optBoolean("isBackground", false) != background) continue
            val text = word.optString("text")
            if (text.isBlank()) continue
            val startMs = word.optLong("time", lineStartMs).coerceAtLeast(0L)
            val durationMs = word.optLong("duration", 0L).coerceAtLeast(45L)
            parsed += LyricWord(
                startMs = startMs,
                endMs = startMs + durationMs,
                text = text
            )
        }
        return parsed.sortedBy { it.startMs }
    }

    private suspend fun httpGet(url: String, accept: String): HttpOutcome {
        val request = runCatching {
            Request.Builder()
                .url(url)
                .header("Accept", accept)
                .header("User-Agent", "LEVYRA Lyrics Engine/3.5 Android")
                .get()
                .build()
        }.getOrElse { return HttpOutcome.Failure }
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, exception: IOException) {
                        if (continuation.isActive) {
                            Timber.d(exception, "LyricsPlus request failed for %s", request.url.host)
                            continuation.resume(HttpOutcome.Failure)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val result = runCatching {
                            response.use {
                                when {
                                    it.code == 404 -> HttpOutcome.NotFound
                                    !it.isSuccessful -> HttpOutcome.Failure
                                    else -> it.body?.string()?.takeIf(String::isNotBlank)
                                        ?.let(HttpOutcome::Success)
                                        ?: HttpOutcome.Failure
                                }
                            }
                        }.getOrDefault(HttpOutcome.Failure)
                        if (continuation.isActive) continuation.resume(result)
                    }
                }
            )
        }
    }

    private fun parseDurationSeconds(raw: String): Long {
        val clean = raw.trim()
        clean.toLongOrNull()?.let { return normalizeDurationSeconds(it) }
        val parts = clean.split(':').mapNotNull(String::toLongOrNull)
        return when (parts.size) {
            2 -> parts[0] * 60L + parts[1]
            3 -> parts[0] * 3_600L + parts[1] * 60L + parts[2]
            else -> 0L
        }
    }

    private fun normalizeDurationSeconds(value: Long): Long {
        if (value <= 0L) return 0L
        return if (value > 10_000L) value / 1_000L else value
    }

    private fun String.normalizeLyricsText(): String = replace(Regex("\\s+"), " ").trim()

    private sealed interface EndpointOutcome {
        data class Found(val result: LyricsPlusProviderResult) : EndpointOutcome
        data object Empty : EndpointOutcome
        data object Failure : EndpointOutcome
    }

    private sealed interface HttpOutcome {
        data class Success(val body: String) : HttpOutcome
        data object NotFound : HttpOutcome
        data object Failure : HttpOutcome
    }

    private companion object {
        const val BINIMUM_API_BASE = "https://lyrics-api.binimum.org/"
        const val PREFERRED_SERVER_TIMEOUT_MS = 1_500L
        const val MIN_BINIMUM_MATCH_SCORE = 45
        val SERVERS = listOf(
            "https://lyricsplus.binimum.org",
            "https://lyricsplus.atomix.one",
            "https://lyricsplus.prjktla.my.id",
            "https://lyricsplus-seven.vercel.app"
        )
    }
}
