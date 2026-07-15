package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.domain.LyricWord
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class LyricsRepository(context: Context? = null) {
    private val appContext = context?.applicationContext
    private val cacheDir = appContext?.cacheDir?.let { File(it, "lyrics_pro") }
    private val youtubeTranscript = appContext?.let(::YoutubeTranscriptLyricsProvider)
    private val youtubeMusic = YoutubeMusicWatchRepository(appContext)
    private val memoryLock = Any()
    private val negativeLock = Any()
    private val memory = object : LinkedHashMap<String, LyricsResult>(MEMORY_CACHE_SIZE + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LyricsResult>?): Boolean = size > MEMORY_CACHE_SIZE
    }
    private val negativeCache = object : LinkedHashMap<String, Long>(NEGATIVE_CACHE_SIZE + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > NEGATIVE_CACHE_SIZE
    }
    private val httpClient = LevyraHttpClientFactory.media(appContext).newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .callTimeout(9, TimeUnit.SECONDS)
        .build()

    data class LyricsResult(
        val synced: Boolean,
        val lines: List<LyricLine>,
        val provider: String,
        val confidence: Int,
        val cached: Boolean
    )

    private data class ProviderAttempt(
        val candidates: List<LyricsCandidate> = emptyList(),
        val attempted: Boolean = false,
        val hadTransientFailure: Boolean = false
    )

    private data class PreferredFetch(
        val result: LyricsResult?,
        val attempted: Boolean,
        val hadTransientFailure: Boolean
    )

    private sealed interface HttpGetResult {
        data class Success(val body: String) : HttpGetResult
        data object NotFound : HttpGetResult
        data object Failure : HttpGetResult
    }

    suspend fun fetch(
        title: String,
        artist: String,
        durationSec: Long,
        videoId: String = "",
        languageCode: String = "",
        translate: Boolean = false
    ): LyricsResult? = withContext(Dispatchers.IO) {
        val queryTitle = cleanTitle(title)
        val queryArtist = cleanArtist(artist)
        val requestedTitle = title.trim().ifBlank { queryTitle }
        val requestedArtist = artist.trim().ifBlank { queryArtist }
        if (queryTitle.length < 2) return@withContext null
        val key = cacheKey(requestedTitle, requestedArtist, durationSec, videoId, languageCode, translate)
        memoryGet(key)?.let { return@withContext it.copy(cached = true) }
        readCache(key)?.let { cached ->
            memoryPut(key, cached)
            return@withContext cached
        }
        if (isNegativeCached(key)) return@withContext null

        val request = LyricsRequest(requestedTitle, requestedArtist, durationSec)
        val preferred = fetchPreferredCandidate(
            request = request,
            queryTitle = queryTitle,
            queryArtist = queryArtist,
            requestedTitle = requestedTitle,
            requestedArtist = requestedArtist,
            durationSec = durationSec,
            videoId = videoId,
            languageCode = languageCode,
            translate = translate
        )
        val fallback = if (preferred.result == null && queryArtist.length >= 2) {
            lyricsOvh(queryTitle, queryArtist)
        } else {
            ProviderAttempt()
        }
        val best = preferred.result ?: fallback.candidates.firstOrNull()?.result
        val normalized = best
            ?.let { normalizeTiming(it, durationSec) }
            ?.let(::cleanAndEnrich)
            ?.takeIf { it.lines.isNotEmpty() }

        if (normalized == null) {
            val allAttemptedProvidersCompleted = preferred.attempted &&
                !preferred.hadTransientFailure &&
                fallback.attempted &&
                !fallback.hadTransientFailure
            if (allAttemptedProvidersCompleted) negativePut(key)
            return@withContext null
        }

        val stable = normalized.copy(cached = false)
        memoryPut(key, stable)
        writeCache(key, stable)
        stable
    }

    private suspend fun fetchPreferredCandidate(
        request: LyricsRequest,
        queryTitle: String,
        queryArtist: String,
        requestedTitle: String,
        requestedArtist: String,
        durationSec: Long,
        videoId: String,
        languageCode: String,
        translate: Boolean
    ): PreferredFetch = supervisorScope {
        val tasks = ArrayList<Deferred<ProviderAttempt>>()
        if (videoId.isNotBlank()) {
            tasks += async {
                youtubeMusicAttempt(videoId, languageCode, requestedTitle, requestedArtist, durationSec)
            }
            tasks += async {
                transcriptAttempt(videoId, requestedTitle, requestedArtist, durationSec, languageCode, translate)
            }
        }
        if (queryArtist.length >= 2) {
            tasks += async { getLrcLibExact(queryTitle, queryArtist, durationSec) }
            tasks += async { searchLrcLib(queryTitle, queryArtist) }
        }
        if (tasks.isEmpty()) return@supervisorScope PreferredFetch(null, attempted = false, hadTransientFailure = false)

        val candidates = ArrayList<LyricsCandidate>()
        var attempted = false
        var hadTransientFailure = false
        while (tasks.isNotEmpty()) {
            val completed = select<Pair<Deferred<ProviderAttempt>, ProviderAttempt>> {
                tasks.forEach { task ->
                    task.onAwait { result -> task to result }
                }
            }
            tasks.remove(completed.first)
            val attempt = completed.second
            attempted = attempted || attempt.attempted
            hadTransientFailure = hadTransientFailure || attempt.hadTransientFailure
            candidates += attempt.candidates
            val best = LyricsResultRanker.best(candidates, request)
            if (best != null && isFastPathAcceptable(best)) {
                tasks.forEach { it.cancel() }
                return@supervisorScope PreferredFetch(best, attempted, hadTransientFailure)
            }
        }
        PreferredFetch(LyricsResultRanker.best(candidates, request), attempted, hadTransientFailure)
    }

    private suspend fun youtubeMusicAttempt(
        videoId: String,
        languageCode: String,
        title: String,
        artist: String,
        durationSec: Long
    ): ProviderAttempt {
        return try {
            val candidate = youtubeMusic.getLyricsForVideo(videoId, languageCode)
                ?.toCandidate(title, artist, durationSec)
            if (candidate == null) {
                ProviderAttempt(attempted = true, hadTransientFailure = true)
            } else {
                ProviderAttempt(candidates = listOf(candidate), attempted = true)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            Timber.w(exception, "YouTube Music lyrics request failed")
            ProviderAttempt(attempted = true, hadTransientFailure = true)
        }
    }

    private suspend fun transcriptAttempt(
        videoId: String,
        title: String,
        artist: String,
        durationSec: Long,
        languageCode: String,
        translate: Boolean
    ): ProviderAttempt {
        return try {
            val candidate = fetchTranscriptCandidate(videoId, title, artist, durationSec, languageCode, translate)
            if (candidate == null) {
                ProviderAttempt(attempted = true, hadTransientFailure = true)
            } else {
                ProviderAttempt(candidates = listOf(candidate), attempted = true)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            Timber.w(exception, "YouTube transcript lyrics request failed")
            ProviderAttempt(attempted = true, hadTransientFailure = true)
        }
    }

    private fun isFastPathAcceptable(result: LyricsResult): Boolean {
        if (result.lines.isEmpty()) return false
        if (result.synced && result.confidence >= FAST_SYNCED_CONFIDENCE) return true
        return result.confidence >= FAST_PLAIN_CONFIDENCE
    }

    private suspend fun fetchTranscriptCandidate(
        videoId: String,
        title: String,
        artist: String,
        durationSec: Long,
        languageCode: String,
        translate: Boolean
    ): LyricsCandidate? {
        if (videoId.isBlank()) return null
        val transcript = runCatching { youtubeTranscript?.fetch(videoId, languageCode, translate) }
            .getOrNull()
            ?.takeIf { it.lines.isNotEmpty() }
            ?: return null
        val provider = buildString {
            append("YouTube Transcript")
            if (transcript.automatic) append(" Auto")
            append(" · ").append(transcript.sourceLanguage)
            if (transcript.translated) append(" → ").append(languageCode)
        }
        return LyricsCandidate(
            result = LyricsResult(true, transcript.lines, provider, if (transcript.automatic) 72 else 82, false),
            title = title,
            artist = artist,
            durationSec = durationSec
        )
    }

    private fun YoutubeMusicNativeLyrics.toCandidate(
        title: String,
        artist: String,
        durationSec: Long
    ): LyricsCandidate {
        val provider = buildString {
            append("YouTube Music")
            if (source.isNotBlank()) append(" · ").append(source)
        }
        return LyricsCandidate(
            result = LyricsResult(
                synced = synced,
                lines = lines,
                provider = provider,
                confidence = if (synced) 100 else 90,
                cached = false
            ),
            title = title,
            artist = artist,
            durationSec = durationSec
        )
    }

    private suspend fun getLrcLibExact(title: String, artist: String, durationSec: Long): ProviderAttempt {
        val url = buildString {
            append("https://lrclib.net/api/get?track_name=")
            append(enc(title))
            append("&artist_name=")
            append(enc(artist))
            if (durationSec > 0) append("&duration=").append(durationSec)
        }
        return when (val response = httpGet(url, "application/json")) {
            is HttpGetResult.Success -> runCatching {
                val json = JSONObject(response.body)
                val result = parseLrcLibEntry(json, "LRCLIB Exact")
                val candidates = result?.let {
                    listOf(
                        LyricsCandidate(
                            result = it,
                            title = json.optString("trackName", title),
                            artist = json.optString("artistName", artist),
                            durationSec = json.optLong("duration", durationSec)
                        )
                    )
                }.orEmpty()
                ProviderAttempt(candidates = candidates, attempted = true)
            }.getOrElse {
                Timber.w(it, "LRCLIB exact response parsing failed")
                ProviderAttempt(attempted = true, hadTransientFailure = true)
            }
            HttpGetResult.NotFound -> ProviderAttempt(attempted = true)
            HttpGetResult.Failure -> ProviderAttempt(attempted = true, hadTransientFailure = true)
        }
    }

    private suspend fun searchLrcLib(title: String, artist: String): ProviderAttempt {
        val url = "https://lrclib.net/api/search?track_name=${enc(title)}&artist_name=${enc(artist)}"
        return when (val response = httpGet(url, "application/json")) {
            is HttpGetResult.Success -> runCatching {
                val array = JSONArray(response.body)
                val out = ArrayList<LyricsCandidate>()
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    val result = parseLrcLibEntry(json, "LRCLIB Search") ?: continue
                    out += LyricsCandidate(
                        result = result,
                        title = json.optString("trackName", title),
                        artist = json.optString("artistName", artist),
                        durationSec = json.optLong("duration", 0L)
                    )
                }
                ProviderAttempt(candidates = out.take(16), attempted = true)
            }.getOrElse {
                Timber.w(it, "LRCLIB search response parsing failed")
                ProviderAttempt(attempted = true, hadTransientFailure = true)
            }
            HttpGetResult.NotFound -> ProviderAttempt(attempted = true)
            HttpGetResult.Failure -> ProviderAttempt(attempted = true, hadTransientFailure = true)
        }
    }

    private suspend fun lyricsOvh(title: String, artist: String): ProviderAttempt {
        val url = "https://api.lyrics.ovh/v1/${encPath(artist)}/${encPath(title)}"
        return when (val response = httpGet(url, "application/json")) {
            is HttpGetResult.Success -> runCatching {
                val lyrics = JSONObject(response.body).optString("lyrics").trim()
                val lines = UnifiedLyricsParser.parsePlain(lyrics)
                val candidate = if (lines.isEmpty()) {
                    null
                } else {
                    LyricsCandidate(LyricsResult(false, lines, "Lyrics.ovh", 54, false), title, artist, 0L)
                }
                ProviderAttempt(candidates = listOfNotNull(candidate), attempted = true)
            }.getOrElse {
                Timber.w(it, "Lyrics.ovh response parsing failed")
                ProviderAttempt(attempted = true, hadTransientFailure = true)
            }
            HttpGetResult.NotFound -> ProviderAttempt(attempted = true)
            HttpGetResult.Failure -> ProviderAttempt(attempted = true, hadTransientFailure = true)
        }
    }

    private fun parseLrcLibEntry(json: JSONObject, provider: String): LyricsResult? {
        val syncedText = json.optString("syncedLyrics").takeIf { it.isMeaningfulLyrics() }
        if (syncedText != null) {
            val lines = UnifiedLyricsParser.parse(syncedText)
            if (lines.isNotEmpty()) {
                val wordSynced = lines.any { it.words.isNotEmpty() }
                return LyricsResult(true, lines, provider, if (wordSynced) 94 else 88, false)
            }
        }
        val plain = json.optString("plainLyrics").takeIf { it.isMeaningfulLyrics() } ?: return null
        val lines = UnifiedLyricsParser.parsePlain(plain)
        if (lines.isEmpty()) return null
        return LyricsResult(false, lines, provider, 68, false)
    }

    private suspend fun httpGet(url: String, accept: String): HttpGetResult {
        val request = Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("User-Agent", "LEVYRA Lyrics Engine/3.3 Android")
            .get()
            .build()
        return suspendCancellableCoroutine { continuation ->
            val call = httpClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, exception: IOException) {
                        if (continuation.isActive) {
                            Timber.w(exception, "Lyrics request failed for %s", request.url.host)
                            continuation.resume(HttpGetResult.Failure)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val result = runCatching {
                            response.use {
                                when {
                                    it.code == 404 -> HttpGetResult.NotFound
                                    !it.isSuccessful -> HttpGetResult.Failure
                                    else -> it.body?.string()?.takeIf(String::isNotBlank)
                                        ?.let(HttpGetResult::Success)
                                        ?: HttpGetResult.Failure
                                }
                            }
                        }.onFailure { Timber.w(it, "Lyrics response failed for %s", request.url.host) }
                            .getOrDefault(HttpGetResult.Failure)
                        if (continuation.isActive) continuation.resume(result)
                    }
                }
            )
        }
    }

    private fun readCache(key: String): LyricsResult? {
        val dir = cacheDir ?: return null
        val file = File(dir, "$key.json")
        if (!file.isFile || System.currentTimeMillis() - file.lastModified() > CACHE_TTL_MS) return null
        return runCatching {
            val json = JSONObject(file.readText())
            val linesJson = json.optJSONArray("lines") ?: JSONArray()
            val parsed = ArrayList<LyricLine>()
            for (index in 0 until linesJson.length()) {
                val item = linesJson.optJSONObject(index) ?: continue
                val wordsJson = item.optJSONArray("words") ?: JSONArray()
                val words = ArrayList<LyricWord>()
                for (wordIndex in 0 until wordsJson.length()) {
                    val word = wordsJson.optJSONObject(wordIndex) ?: continue
                    words += LyricWord(
                        startMs = word.optLong("startMs"),
                        endMs = word.optLong("endMs"),
                        text = word.optString("text"),
                        romanized = word.optString("romanized")
                    )
                }
                val role = runCatching {
                    LyricVocalRole.valueOf(item.optString("role", LyricVocalRole.MAIN.name))
                }.getOrDefault(LyricVocalRole.MAIN)
                parsed += LyricLine(
                    startMs = item.optLong("startMs"),
                    endMs = item.optLong("endMs"),
                    text = item.optString("text"),
                    translated = item.optString("translated"),
                    words = words,
                    romanized = item.optString("romanized"),
                    role = role,
                    isInstrumental = item.optBoolean("instrumental"),
                    isMetadata = item.optBoolean("metadata")
                )
            }
            if (parsed.isEmpty()) {
                null
            } else {
                LyricsResult(
                    synced = json.optBoolean("synced"),
                    lines = parsed,
                    provider = json.optString("provider"),
                    confidence = json.optInt("confidence", 70),
                    cached = true
                )
            }
        }.onFailure { Timber.w(it, "Lyrics cache restore failed") }.getOrNull()
    }

    private fun writeCache(key: String, result: LyricsResult) {
        val dir = cacheDir ?: return
        runCatching {
            if (!dir.isDirectory && !dir.mkdirs()) return
            val linesJson = JSONArray()
            result.lines.take(MAX_CACHE_LINES).forEach { line ->
                val wordsJson = JSONArray()
                line.words.take(MAX_CACHE_WORDS_PER_LINE).forEach { word ->
                    wordsJson.put(
                        JSONObject()
                            .put("startMs", word.startMs)
                            .put("endMs", word.endMs)
                            .put("text", word.text)
                            .put("romanized", word.romanized)
                    )
                }
                linesJson.put(
                    JSONObject()
                        .put("startMs", line.startMs)
                        .put("endMs", line.endMs)
                        .put("text", line.text)
                        .put("translated", line.translated)
                        .put("romanized", line.romanized)
                        .put("role", line.role.name)
                        .put("instrumental", line.isInstrumental)
                        .put("metadata", line.isMetadata)
                        .put("words", wordsJson)
                )
            }
            val target = File(dir, "$key.json")
            val temporary = File(dir, "$key.tmp")
            temporary.writeText(
                JSONObject()
                    .put("version", CACHE_VERSION)
                    .put("synced", result.synced)
                    .put("provider", result.provider)
                    .put("confidence", result.confidence)
                    .put("lines", linesJson)
                    .toString()
            )
            val renamed = if (target.exists() && !target.delete()) false else temporary.renameTo(target)
            if (!renamed) {
                target.writeText(temporary.readText())
                temporary.delete()
            }
            pruneDiskCache(dir)
        }.onFailure { Timber.w(it, "Lyrics cache save failed") }
    }

    private fun cleanAndEnrich(result: LyricsResult): LyricsResult {
        val cleaned = LyricsCleaner.clean(result.lines)
        val enriched = cleaned.map { line ->
            val lineRomanized = line.romanized.ifBlank { LyricsRomanizer.romanize(line.text) }
            val enrichedWords = if (line.words.isEmpty()) {
                emptyList()
            } else {
                line.words.map { word ->
                    word.copy(romanized = word.romanized.ifBlank { LyricsRomanizer.romanize(word.text) })
                }
            }
            line.copy(romanized = lineRomanized, words = enrichedWords)
        }
        return result.copy(lines = enriched)
    }

    private fun cleanTitle(title: String): String = title
        .replace(Regex("(?i)\\s*[(\\[].*?(remaster|radio edit|video|official|lyrics|prod\\.|feat\\.|ft\\.).*?[)\\]]"), "")
        .replace(Regex("(?i)\\s*-\\s*(official|video|audio|lyrics).*$"), "")
        .trim()

    private fun cleanArtist(artist: String): String = artist
        .replace(Regex("(?i)\\s*VEVO$"), "")
        .trim()


    private fun String.isMeaningfulLyrics(): Boolean {
        val clean = trim()
        return clean.length >= 16 && !clean.equals("null", ignoreCase = true)
    }

    private fun cacheKey(
        title: String,
        artist: String,
        durationSec: Long,
        videoId: String,
        languageCode: String,
        translate: Boolean
    ): String {
        val seed = "${LyricsMatcher.normalize(title)}|${LyricsMatcher.normalize(artist)}|${durationSec.coerceAtLeast(0L) / 5L}|${videoId.trim()}|${languageCode.lowercase(Locale.ROOT)}|$translate|$CACHE_VERSION"
        return MessageDigest.getInstance("SHA-256")
            .digest(seed.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun normalizeTiming(result: LyricsResult, durationSec: Long): LyricsResult {
        val durationMs = durationSec.coerceAtLeast(0L) * 1_000L
        if (result.lines.isEmpty()) return result
        val normalizedLines = result.lines.sortedWith(compareBy<LyricLine> { it.startMs }.thenBy { it.role.ordinal })
        val nextStartByIndex = arrayOfNulls<Long>(normalizedLines.size)
        var nextMainStart: Long? = null
        var nextBackgroundStart: Long? = null
        for (index in normalizedLines.indices.reversed()) {
            val line = normalizedLines[index]
            if (line.role == LyricVocalRole.BACKGROUND) {
                nextStartByIndex[index] = nextBackgroundStart
                nextBackgroundStart = line.startMs
            } else {
                nextStartByIndex[index] = nextMainStart
                nextMainStart = line.startMs
            }
        }
        val corrected = normalizedLines.mapIndexed { index, line ->
            val lineStart = if (durationMs > 0L) line.startMs.coerceIn(0L, durationMs) else line.startMs.coerceAtLeast(0L)
            val nextStart = nextStartByIndex[index]
            val naturalEnd = line.endMs.coerceAtLeast(lineStart + 120L)
            val limitedEnd = nextStart?.minus(45L)?.coerceAtLeast(lineStart + 120L)?.let { minOf(naturalEnd, it) } ?: naturalEnd
            val finalEnd = if (durationMs > 0L) limitedEnd.coerceIn(lineStart, durationMs) else limitedEnd
            line.copy(
                startMs = lineStart,
                endMs = finalEnd,
                words = line.words.map { word ->
                    val wordStart = word.startMs.coerceIn(lineStart, finalEnd)
                    word.copy(
                        startMs = wordStart,
                        endMs = word.endMs.coerceIn(wordStart, finalEnd)
                    )
                }
            )
        }
        return result.copy(lines = corrected)
    }

    private fun memoryGet(key: String): LyricsResult? = synchronized(memoryLock) { memory[key] }

    private fun memoryPut(key: String, value: LyricsResult) {
        synchronized(memoryLock) { memory[key] = value }
    }

    private fun isNegativeCached(key: String): Boolean {
        val now = System.currentTimeMillis()
        return synchronized(negativeLock) {
            val timestamp = negativeCache[key] ?: return@synchronized false
            if (now - timestamp > NEGATIVE_CACHE_TTL_MS) {
                negativeCache.remove(key)
                false
            } else {
                true
            }
        }
    }

    private fun negativePut(key: String) {
        synchronized(negativeLock) { negativeCache[key] = System.currentTimeMillis() }
    }

    private fun pruneDiskCache(dir: File) {
        val files = dir.listFiles { file -> file.isFile && file.extension == "json" }.orEmpty()
        if (files.size <= MAX_DISK_CACHE_FILES) return
        files.sortedBy(File::lastModified)
            .take(files.size - MAX_DISK_CACHE_FILES)
            .forEach(File::delete)
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun encPath(value: String): String = value.split("/").joinToString("%2F") { enc(it) }

    companion object {
        private const val CACHE_VERSION = 5
        private const val CACHE_TTL_MS = 30L * 24L * 60L * 60L * 1_000L
        private const val NEGATIVE_CACHE_TTL_MS = 15L * 60L * 1_000L
        private const val MEMORY_CACHE_SIZE = 64
        private const val NEGATIVE_CACHE_SIZE = 96
        private const val MAX_DISK_CACHE_FILES = 180
        private const val MAX_CACHE_LINES = 700
        private const val MAX_CACHE_WORDS_PER_LINE = 120
        private const val FAST_SYNCED_CONFIDENCE = 68
        private const val FAST_PLAIN_CONFIDENCE = 86
    }
}
