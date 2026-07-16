package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.LyricsCacheDao
import com.luc4n3x.levyra.data.local.LyricsCacheEntity
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricSection
import com.luc4n3x.levyra.domain.LyricSectionType
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
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
    private val lyricsCacheDao: LyricsCacheDao? = appContext?.let { LevyraDatabase.get(it).lyricsCacheDao() }
    private val legacyCacheDir = appContext?.cacheDir?.let { File(it, "lyrics_pro") }
    private val youtubeTranscript = appContext?.let(::YoutubeTranscriptLyricsProvider)
    private val youtubeMusic = YoutubeMusicWatchRepository(appContext)
    private val lyricsPlusClient = LevyraHttpClientFactory.media(appContext).newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()
    private val lyricsPlus = LyricsPlusProvider(lyricsPlusClient)
    private val memoryLock = Any()
    private val negativeLock = Any()
    private val memory = object : LinkedHashMap<String, MemoryEntry>(MEMORY_CACHE_SIZE + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MemoryEntry>?): Boolean = size > MEMORY_CACHE_SIZE
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
        val cached: Boolean,
        val sections: List<LyricSection> = emptyList()
    )

    private data class QuerySpec(
        val requestedTitle: String,
        val requestedArtist: String,
        val album: String,
        val queryTitle: String,
        val queryArtist: String,
        val durationSec: Long,
        val videoId: String,
        val languageCode: String,
        val translate: Boolean,
        val key: String
    )

    private data class MemoryEntry(
        val result: LyricsResult,
        val updatedAt: Long,
        val expiresAt: Long
    )

    private data class CacheLookup(
        val result: LyricsResult? = null,
        val negative: Boolean = false,
        val refreshRequired: Boolean = true
    )

    private data class ProviderAttempt(
        val candidates: List<LyricsCandidate> = emptyList(),
        val attempted: Boolean = false,
        val hadTransientFailure: Boolean = false
    )

    private data class NetworkOutcome(
        val best: LyricsResult?,
        val attempted: Boolean,
        val hadTransientFailure: Boolean
    )

    private sealed interface HttpGetResult {
        data class Success(val body: String) : HttpGetResult
        data object NotFound : HttpGetResult
        data object Failure : HttpGetResult
    }

    fun observe(
        title: String,
        artist: String,
        durationSec: Long,
        album: String = "",
        videoId: String = "",
        languageCode: String = "",
        translate: Boolean = false
    ): Flow<LyricsResult> = channelFlow {
        val query = querySpec(title, artist, durationSec, album, videoId, languageCode, translate) ?: return@channelFlow
        var current: LyricsResult? = null
        val cached = readCached(query)
        cached.result?.let { result ->
            current = result.copy(cached = true)
            send(current!!)
        }
        if (cached.negative) return@channelFlow
        if (!cached.refreshRequired) return@channelFlow

        val outcome = fetchNetworkProgressive(query) { candidate ->
            val previous = current
            if (shouldUpgrade(previous, candidate)) {
                val stable = candidate.copy(cached = false)
                current = stable
                memoryPut(query.key, stable, System.currentTimeMillis())
                persistPositive(query, stable)
                send(stable)
            }
        }

        val final = outcome.best
        if (final != null && shouldUpgrade(current, final)) {
            val stable = final.copy(cached = false)
            current = stable
            memoryPut(query.key, stable, System.currentTimeMillis())
            persistPositive(query, stable)
            send(stable)
        } else if (final != null && current != null) {
            persistPositive(query, current!!.copy(cached = false))
        }

        if (current == null && outcome.attempted && !outcome.hadTransientFailure) {
            persistNegative(query)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun fetch(
        title: String,
        artist: String,
        durationSec: Long,
        album: String = "",
        videoId: String = "",
        languageCode: String = "",
        translate: Boolean = false
    ): LyricsResult? = observe(title, artist, durationSec, album, videoId, languageCode, translate).lastOrNull()

    suspend fun prefetch(
        title: String,
        artist: String,
        durationSec: Long,
        album: String = "",
        videoId: String = "",
        languageCode: String = "",
        translate: Boolean = false
    ) {
        observe(title, artist, durationSec, album, videoId, languageCode, translate).collect { }
    }

    private suspend fun fetchNetworkProgressive(
        query: QuerySpec,
        onCandidate: suspend (LyricsResult) -> Unit
    ): NetworkOutcome = supervisorScope {
        val request = LyricsRequest(query.requestedTitle, query.requestedArtist, query.durationSec)
        val tasks = ArrayList<Deferred<ProviderAttempt>>()
        if (query.videoId.isNotBlank()) {
            tasks += async {
                providerWithin(YOUTUBE_MUSIC_TIMEOUT_MS) {
                    youtubeMusicAttempt(
                        query.videoId,
                        query.languageCode,
                        query.requestedTitle,
                        query.requestedArtist,
                        query.durationSec
                    )
                }
            }
            tasks += async {
                providerWithin(TRANSCRIPT_TIMEOUT_MS) {
                    transcriptAttempt(
                        query.videoId,
                        query.requestedTitle,
                        query.requestedArtist,
                        query.durationSec,
                        query.languageCode,
                        query.translate
                    )
                }
            }
        }
        if (query.queryArtist.length >= 2) {
            tasks += async { providerWithin(FAST_PROVIDER_TIMEOUT_MS) { getLrcLibExact(query.queryTitle, query.queryArtist, query.durationSec) } }
            tasks += async { providerWithin(FAST_PROVIDER_TIMEOUT_MS) { searchLrcLib(query.queryTitle, query.queryArtist) } }
            tasks += async { providerWithin(LYRICS_PLUS_TIMEOUT_MS) { lyricsPlusMirrorAttempt(query) } }
            tasks += async { providerWithin(LYRICS_PLUS_TIMEOUT_MS) { binimumAttempt(query) } }
            tasks += async { providerWithin(FAST_PROVIDER_TIMEOUT_MS) { lyricsOvh(query.queryTitle, query.queryArtist) } }
        }
        if (tasks.isEmpty()) return@supervisorScope NetworkOutcome(null, attempted = false, hadTransientFailure = false)

        val candidates = ArrayList<LyricsCandidate>()
        var attempted = false
        var hadTransientFailure = false
        var emitted: LyricsResult? = null
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
            attempt.candidates.mapNotNullTo(candidates) { prepareCandidate(it, query.durationSec) }
            val best = LyricsResultRanker.best(candidates, request)
            if (best != null && best.confidence >= INSTANT_MIN_CONFIDENCE && shouldUpgrade(emitted, best)) {
                emitted = best
                onCandidate(best)
            }
        }

        val best = LyricsResultRanker.best(candidates, request)
        NetworkOutcome(best, attempted, hadTransientFailure)
    }


    private suspend fun providerWithin(
        timeoutMs: Long,
        block: suspend () -> ProviderAttempt
    ): ProviderAttempt = withTimeoutOrNull(timeoutMs) { block() }
        ?: ProviderAttempt(attempted = true, hadTransientFailure = true)

    private suspend fun lyricsPlusMirrorAttempt(query: QuerySpec): ProviderAttempt {
        val outcome = lyricsPlus.fetchMirrors(
            title = query.queryTitle,
            artist = query.queryArtist,
            album = query.album,
            durationSec = query.durationSec
        )
        return outcome.toProviderAttempt()
    }

    private suspend fun binimumAttempt(query: QuerySpec): ProviderAttempt {
        val outcome = lyricsPlus.fetchBinimum(
            title = query.queryTitle,
            artist = query.queryArtist,
            album = query.album,
            durationSec = query.durationSec
        )
        return outcome.toProviderAttempt()
    }

    private fun LyricsPlusProviderOutcome.toProviderAttempt(): ProviderAttempt {
        val candidates = results.map { result ->
            LyricsCandidate(
                result = LyricsResult(
                    synced = result.synced,
                    lines = result.lines,
                    provider = result.provider,
                    confidence = result.confidence,
                    cached = false
                ),
                title = result.title,
                artist = result.artist,
                durationSec = result.durationSec
            )
        }
        return ProviderAttempt(
            candidates = candidates,
            attempted = attempted,
            hadTransientFailure = hadTransientFailure
        )
    }

    private fun prepareCandidate(candidate: LyricsCandidate, durationSec: Long): LyricsCandidate? {
        val normalized = normalizeTiming(candidate.result, durationSec)
        val enriched = cleanAndEnrich(normalized)
        if (enriched.lines.isEmpty()) return null
        return candidate.copy(result = enriched)
    }

    internal fun shouldUpgrade(previous: LyricsResult?, current: LyricsResult): Boolean {
        if (current.lines.isEmpty()) return false
        if (previous == null) return true
        if (sameResult(previous, current)) {
            return current.confidence > previous.confidence ||
                (current.confidence == previous.confidence && previous.cached && !current.cached)
        }
        val previousWordTimed = previous.lines.any { it.words.isNotEmpty() }
        val currentWordTimed = current.lines.any { it.words.isNotEmpty() }
        if (currentWordTimed && !previousWordTimed && current.confidence >= previous.confidence - 5) return true
        if (current.synced && !previous.synced && current.confidence >= previous.confidence - 3) return true
        if (current.sections.size > previous.sections.size && current.confidence >= previous.confidence) return true
        if (current.lines.any { it.translated.isNotBlank() } && previous.lines.none { it.translated.isNotBlank() } && current.confidence >= previous.confidence) return true
        return current.confidence >= previous.confidence + MIN_QUALITY_UPGRADE
    }

    private fun sameResult(left: LyricsResult, right: LyricsResult): Boolean {
        if (left.synced != right.synced || left.lines.size != right.lines.size || left.sections != right.sections) return false
        return left.lines.zip(right.lines).all { (first, second) ->
            first.startMs == second.startMs &&
                first.endMs == second.endMs &&
                first.text.equals(second.text, ignoreCase = true) &&
                first.translated == second.translated &&
                first.romanized == second.romanized &&
                first.role == second.role &&
                first.isInstrumental == second.isInstrumental &&
                first.isMetadata == second.isMetadata &&
                first.words == second.words
        }
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

    private suspend fun fetchTranscriptCandidate(
        videoId: String,
        title: String,
        artist: String,
        durationSec: Long,
        languageCode: String,
        translate: Boolean
    ): LyricsCandidate? {
        if (videoId.isBlank()) return null
        val transcript = youtubeTranscript?.fetch(videoId, languageCode, translate)
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
            .header("User-Agent", "LEVYRA Lyrics Engine/3.4 Android")
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

    private suspend fun readCached(query: QuerySpec): CacheLookup {
        val now = System.currentTimeMillis()
        memoryGet(query.key)?.let { entry ->
            if (entry.expiresAt >= now - STALE_CACHE_TTL_MS) {
                return CacheLookup(
                    result = entry.result.copy(cached = true),
                    refreshRequired = shouldRefresh(entry.result, entry.updatedAt, entry.expiresAt, now)
                )
            }
            memoryRemove(query.key)
        }
        if (isNegativeCached(query.key)) return CacheLookup(negative = true, refreshRequired = false)

        val dao = lyricsCacheDao
        if (dao != null) {
            suspend fun restore(entity: LyricsCacheEntity): CacheLookup? {
                if (entity.expiresAt < now - STALE_CACHE_TTL_MS) {
                    runCatching { dao.delete(entity.cacheKey) }
                    return null
                }
                val result = deserializeResult(entity.payload)?.copy(
                    synced = entity.synced,
                    provider = entity.provider,
                    confidence = entity.confidence,
                    cached = true
                )
                if (result == null || result.lines.isEmpty()) {
                    runCatching { dao.delete(entity.cacheKey) }
                    return null
                }
                memoryPut(query.key, result, entity.updatedAt, entity.expiresAt)
                if (now - entity.lastAccessedAt >= ACCESS_TOUCH_INTERVAL_MS) {
                    runCatching { dao.touch(entity.cacheKey, now) }
                }
                return CacheLookup(
                    result = result,
                    refreshRequired = shouldRefresh(result, entity.updatedAt, entity.expiresAt, now)
                )
            }

            val exact = runCatching { dao.get(query.key) }.getOrNull()
            if (exact?.negative == true) {
                if (exact.expiresAt > now) {
                    negativePut(query.key, exact.expiresAt)
                    return CacheLookup(negative = true, refreshRequired = false)
                }
                runCatching { dao.delete(exact.cacheKey) }
            } else if (exact != null) {
                restore(exact)?.let { return it }
            }

            val titleKey = LyricsMatcher.normalize(query.requestedTitle)
            val artistKey = LyricsMatcher.normalize(query.requestedArtist)
            val durationBucket = query.durationSec.coerceAtLeast(0L) / 5L
            if (titleKey.isNotBlank() && artistKey.isNotBlank()) {
                val alias = runCatching {
                    dao.findBestPositive(
                        titleKey = titleKey,
                        artistKey = artistKey,
                        durationBucket = durationBucket,
                        minimumDurationBucket = (durationBucket - 1L).coerceAtLeast(0L),
                        maximumDurationBucket = durationBucket + 1L,
                        languageCode = query.languageCode.lowercase(Locale.ROOT),
                        translate = query.translate
                    )
                }.getOrNull()
                if (alias != null && alias.cacheKey != query.key) {
                    restore(alias)?.let { return it }
                }
            }
        }

        val legacy = readLegacyCache(query.key)
        if (legacy != null) {
            val enriched = cleanAndEnrich(legacy)
            if (enriched.lines.isNotEmpty()) {
                persistPositive(query, enriched)
                memoryPut(query.key, enriched, now)
                File(legacyCacheDir, "${query.key}.json").delete()
                return CacheLookup(result = enriched.copy(cached = true), refreshRequired = true)
            }
        }
        return CacheLookup()
    }

    private fun shouldRefresh(result: LyricsResult, updatedAt: Long, expiresAt: Long, now: Long): Boolean {
        if (expiresAt <= now) return true
        if (now - updatedAt >= CACHE_REFRESH_INTERVAL_MS) return true
        if (!result.synced || result.confidence < QUALITY_REFRESH_THRESHOLD) return true
        return result.lines.none { it.words.isNotEmpty() } && now - updatedAt >= WORD_TIMING_REFRESH_INTERVAL_MS
    }

    private suspend fun persistPositive(query: QuerySpec, result: LyricsResult) {
        val dao = lyricsCacheDao ?: return
        val now = System.currentTimeMillis()
        val existingCreatedAt = runCatching { dao.get(query.key)?.createdAt }.getOrNull() ?: now
        val entity = LyricsCacheEntity(
            cacheKey = query.key,
            titleKey = LyricsMatcher.normalize(query.requestedTitle),
            artistKey = LyricsMatcher.normalize(query.requestedArtist),
            durationBucket = query.durationSec.coerceAtLeast(0L) / 5L,
            videoId = query.videoId,
            languageCode = query.languageCode.lowercase(Locale.ROOT),
            translate = query.translate,
            synced = result.synced,
            provider = result.provider,
            confidence = result.confidence,
            payload = serializeResult(result),
            negative = false,
            createdAt = existingCreatedAt,
            updatedAt = now,
            lastAccessedAt = now,
            expiresAt = now + POSITIVE_CACHE_TTL_MS
        )
        runCatching {
            dao.upsert(entity)
            pruneRoomCache(dao, now)
        }.onFailure { Timber.w(it, "Lyrics Room cache save failed") }
    }

    private suspend fun persistNegative(query: QuerySpec) {
        val now = System.currentTimeMillis()
        val expiresAt = now + NEGATIVE_CACHE_TTL_MS
        negativePut(query.key, expiresAt)
        val dao = lyricsCacheDao ?: return
        val entity = LyricsCacheEntity(
            cacheKey = query.key,
            titleKey = LyricsMatcher.normalize(query.requestedTitle),
            artistKey = LyricsMatcher.normalize(query.requestedArtist),
            durationBucket = query.durationSec.coerceAtLeast(0L) / 5L,
            videoId = query.videoId,
            languageCode = query.languageCode.lowercase(Locale.ROOT),
            translate = query.translate,
            synced = false,
            provider = "",
            confidence = 0,
            payload = "",
            negative = true,
            createdAt = now,
            updatedAt = now,
            lastAccessedAt = now,
            expiresAt = expiresAt
        )
        runCatching {
            dao.upsert(entity)
            pruneRoomCache(dao, now)
        }.onFailure { Timber.w(it, "Lyrics negative cache save failed") }
    }

    private suspend fun pruneRoomCache(dao: LyricsCacheDao, now: Long) {
        dao.deleteExpired(now, now - STALE_CACHE_TTL_MS)
        val count = dao.count()
        if (count > MAX_ROOM_CACHE_ENTRIES) dao.deleteOldest(count - MAX_ROOM_CACHE_ENTRIES)
    }

    internal fun serializeResult(result: LyricsResult): String {
        val serializedLines = result.lines.take(MAX_CACHE_LINES)
        val linesJson = JSONArray()
        serializedLines.forEach { line ->
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
        val sectionsJson = JSONArray()
        val serializedIndices = serializedLines.indices
        result.sections
            .asSequence()
            .filter { section ->
                section.startLineIndex in serializedIndices &&
                    section.endLineIndex in serializedIndices &&
                    section.endLineIndex >= section.startLineIndex
            }
            .forEach { section ->
                sectionsJson.put(
                    JSONObject()
                        .put("type", section.type.name)
                        .put("ordinal", section.ordinal)
                        .put("startLineIndex", section.startLineIndex)
                        .put("endLineIndex", section.endLineIndex)
                        .put("startMs", section.startMs)
                        .put("endMs", section.endMs)
                        .put("confidence", section.confidence)
                )
            }
        return JSONObject()
            .put("version", CACHE_VERSION)
            .put("synced", result.synced)
            .put("provider", result.provider)
            .put("confidence", result.confidence)
            .put("lines", linesJson)
            .put("sections", sectionsJson)
            .toString()
    }

    internal fun deserializeResult(payload: String): LyricsResult? {
        if (payload.isBlank()) return null
        return runCatching {
            val json = JSONObject(payload)
            if (json.optInt("version", -1) != CACHE_VERSION) return@runCatching null
            val linesJson = json.optJSONArray("lines") ?: JSONArray()
            val lines = ArrayList<LyricLine>()
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
                lines += LyricLine(
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
            val sectionsJson = json.optJSONArray("sections") ?: JSONArray()
            val sections = ArrayList<LyricSection>()
            for (index in 0 until sectionsJson.length()) {
                val item = sectionsJson.optJSONObject(index) ?: continue
                val type = runCatching {
                    LyricSectionType.valueOf(item.optString("type"))
                }.getOrNull() ?: continue
                val startLineIndex = item.optInt("startLineIndex", -1)
                val endLineIndex = item.optInt("endLineIndex", -1)
                if (startLineIndex !in lines.indices || endLineIndex !in lines.indices || endLineIndex < startLineIndex) continue
                val startMs = item.optLong("startMs", lines[startLineIndex].startMs)
                val endMs = item.optLong("endMs", lines[endLineIndex].endMs).coerceAtLeast(startMs)
                sections += LyricSection(
                    type = type,
                    ordinal = item.optInt("ordinal", 1).coerceAtLeast(1),
                    startLineIndex = startLineIndex,
                    endLineIndex = endLineIndex,
                    startMs = startMs,
                    endMs = endMs,
                    confidence = item.optInt("confidence", 50).coerceIn(0, 100)
                )
            }
            if (lines.isEmpty()) null else LyricsResult(
                synced = json.optBoolean("synced"),
                lines = lines,
                provider = json.optString("provider"),
                confidence = json.optInt("confidence", 70),
                cached = true,
                sections = sections
            )
        }.onFailure { Timber.w(it, "Lyrics cache decode failed") }.getOrNull()
    }

    private fun readLegacyCache(key: String): LyricsResult? {
        val dir = legacyCacheDir ?: return null
        val file = File(dir, "$key.json")
        if (!file.isFile || System.currentTimeMillis() - file.lastModified() > LEGACY_CACHE_TTL_MS) return null
        return runCatching { deserializeResult(file.readText()) }
            .onFailure { Timber.w(it, "Legacy lyrics cache restore failed") }
            .getOrNull()
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
        val detection = LyricsSectionDetector.detect(enriched)
        return result.copy(lines = detection.lines, sections = detection.sections)
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

    private fun querySpec(
        title: String,
        artist: String,
        durationSec: Long,
        album: String,
        videoId: String,
        languageCode: String,
        translate: Boolean
    ): QuerySpec? {
        val queryTitle = cleanTitle(title)
        val queryArtist = cleanArtist(artist)
        val requestedTitle = title.trim().ifBlank { queryTitle }
        val requestedArtist = artist.trim().ifBlank { queryArtist }
        if (queryTitle.length < 2) return null
        val key = cacheKey(requestedTitle, requestedArtist, durationSec, videoId, languageCode, translate)
        return QuerySpec(
            requestedTitle = requestedTitle,
            requestedArtist = requestedArtist,
            album = album.trim(),
            queryTitle = queryTitle,
            queryArtist = queryArtist,
            durationSec = durationSec,
            videoId = videoId.trim(),
            languageCode = languageCode.trim(),
            translate = translate,
            key = key
        )
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
            val sortedWords = line.words.sortedBy { it.startMs }
            val correctedWords = sortedWords.mapIndexed { wordIndex, word ->
                val wordStart = word.startMs.coerceIn(lineStart, finalEnd)
                val nextWordStart = sortedWords.getOrNull(wordIndex + 1)?.startMs?.coerceIn(wordStart, finalEnd)
                val naturalWordEnd = word.endMs.coerceAtLeast(wordStart + MIN_WORD_DURATION_MS)
                val wordEnd = nextWordStart
                    ?.minus(WORD_GAP_MS)
                    ?.coerceAtLeast(wordStart + MIN_WORD_DURATION_MS)
                    ?.let { minOf(naturalWordEnd, it) }
                    ?: naturalWordEnd
                word.copy(startMs = wordStart, endMs = wordEnd.coerceIn(wordStart, finalEnd))
            }
            line.copy(startMs = lineStart, endMs = finalEnd, words = correctedWords)
        }
        return result.copy(lines = corrected)
    }

    private fun memoryGet(key: String): MemoryEntry? = synchronized(memoryLock) { memory[key] }

    private fun memoryPut(key: String, value: LyricsResult, updatedAt: Long, expiresAt: Long = updatedAt + POSITIVE_CACHE_TTL_MS) {
        synchronized(memoryLock) { memory[key] = MemoryEntry(value, updatedAt, expiresAt) }
    }

    private fun memoryRemove(key: String) {
        synchronized(memoryLock) { memory.remove(key) }
    }

    private fun isNegativeCached(key: String): Boolean {
        val now = System.currentTimeMillis()
        return synchronized(negativeLock) {
            val expiresAt = negativeCache[key] ?: return@synchronized false
            if (now >= expiresAt) {
                negativeCache.remove(key)
                false
            } else {
                true
            }
        }
    }

    private fun negativePut(key: String, expiresAt: Long) {
        synchronized(negativeLock) { negativeCache[key] = expiresAt }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun encPath(value: String): String = value.split("/").joinToString("%2F") { enc(it) }

    companion object {
        internal const val CACHE_VERSION = 8
        private const val POSITIVE_CACHE_TTL_MS = 90L * 24L * 60L * 60L * 1_000L
        private const val STALE_CACHE_TTL_MS = 90L * 24L * 60L * 60L * 1_000L
        private const val LEGACY_CACHE_TTL_MS = 30L * 24L * 60L * 60L * 1_000L
        private const val CACHE_REFRESH_INTERVAL_MS = 7L * 24L * 60L * 60L * 1_000L
        private const val WORD_TIMING_REFRESH_INTERVAL_MS = 24L * 60L * 60L * 1_000L
        private const val ACCESS_TOUCH_INTERVAL_MS = 24L * 60L * 60L * 1_000L
        private const val NEGATIVE_CACHE_TTL_MS = 15L * 60L * 1_000L
        private const val MEMORY_CACHE_SIZE = 64
        private const val NEGATIVE_CACHE_SIZE = 96
        private const val MAX_ROOM_CACHE_ENTRIES = 420
        internal const val MAX_CACHE_LINES = 700
        private const val MAX_CACHE_WORDS_PER_LINE = 120
        private const val INSTANT_MIN_CONFIDENCE = 48
        private const val FAST_PROVIDER_TIMEOUT_MS = 4_500L
        private const val LYRICS_PLUS_TIMEOUT_MS = 5_500L
        private const val YOUTUBE_MUSIC_TIMEOUT_MS = 5_500L
        private const val TRANSCRIPT_TIMEOUT_MS = 6_000L
        private const val QUALITY_REFRESH_THRESHOLD = 84
        private const val MIN_QUALITY_UPGRADE = 4
        private const val MIN_WORD_DURATION_MS = 45L
        private const val WORD_GAP_MS = 12L
    }
}
