package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.security.GoogleApiKeyHeaders
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import timber.log.Timber
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class PlaybackResolver private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var instance: PlaybackResolver? = null

        fun getInstance(context: Context): PlaybackResolver {
            return instance ?: synchronized(this) {
                instance ?: PlaybackResolver(context.applicationContext).also { instance = it }
            }
        }
    }

    private val apiKey = BuildConfig.YOUTUBE_INNERTUBE_API_KEY
    private val prefs = context.getSharedPreferences("levyra_stream_cache", Context.MODE_PRIVATE)
    private val userPreferences = LevyraPreferences(context)
    private val streamCache = ConcurrentHashMap<String, CachedStream>()
    private val inFlight = ConcurrentHashMap<String, Deferred<Track>>()
    private val youtubeHttpClient = LevyraHttpClientFactory.youtubePlayer()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val fallbackTtlMs = 90L * 60L * 1000L
    private val maxTtlMs = 5L * 60L * 60L * 1000L
    private val playbackResolveTimeoutMs = 30_000L
    private val offlineResolveTimeoutMs = 60_000L
    private val hedgeBudgetMs = LevyraResolverLatency.INNER_TUBE_HEDGE_BUDGET_MS
    private val streamProbeClient: OkHttpClient = youtubeHttpClient.newBuilder()
        .connectTimeout(450, TimeUnit.MILLISECONDS)
        .readTimeout(800, TimeUnit.MILLISECONDS)
        .writeTimeout(350, TimeUnit.MILLISECONDS)
        .callTimeout(950, TimeUnit.MILLISECONDS)
        .build()
    private val searchFallbackClient: OkHttpClient = youtubeHttpClient.newBuilder()
        .connectTimeout(800, TimeUnit.MILLISECONDS)
        .readTimeout(2_000, TimeUnit.MILLISECONDS)
        .writeTimeout(500, TimeUnit.MILLISECONDS)
        .callTimeout(2_400, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var selectedAudioQuality = userPreferences.audioQuality()

    @Volatile
    private var lastNetworkWarmAt = 0L

    private val profiles = listOf(
        ClientProfile("ANDROID_MUSIC", "8.10.52", "Android Music", "Mozilla/5.0 (Linux; Android 15; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) com.google.android.apps.youtube.music/8.10.52", true, 0L, 0),
        ClientProfile("ANDROID", "19.44.38", "Android", "com.google.android.youtube/19.44.38 (Linux; U; Android 15)", true, 0L, 1),
        ClientProfile("IOS", "20.10.4", "iOS", "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3 like Mac OS X; it_IT)", false, 0L, 2),
        ClientProfile("WEB_REMIX", "1.20260423.01.00", "YouTube Music Web", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36", false, 0L, 3),
        ClientProfile("WEB_EMBEDDED_PLAYER", "1.20260423.01.00", "Embedded Player", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36", false, 0L, 4)
    )

    init {
        restoreCache()
    }

    fun setAudioQuality(value: String) {
        selectedAudioQuality = when (value.lowercase()) {
            "high" -> "High"
            "low" -> "Low"
            else -> "Auto"
        }
    }

    fun warmNetwork() {
        val now = System.currentTimeMillis()
        if (now - lastNetworkWarmAt < 15_000L) return
        lastNetworkWarmAt = now
        listOf(
            "https://www.youtube.com/generate_204",
            "https://music.youtube.com/generate_204"
        ).forEach { url ->
            val request = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", profiles.first().userAgent)
                .build()
            youtubeHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Timber.d(e, "youtube warmup failed")
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    response.close()
                }
            })
        }
    }

    fun cached(track: Track, isVideoMode: Boolean = false): Track? {
        if (track.streamUrl.isNotBlank()) {
            if (!isVideoMode && !isDirectAudioUrl(track.streamUrl)) return null
            return if (streamStillFresh(track.streamUrl)) track else null
        }
        val key = cacheKey(track, isVideoMode)
        val hit = streamCache[key] ?: return null
        if (!isFresh(hit.expiresAt)) {
            remove(key)
            return null
        }
        if (!isVideoMode && !isDirectAudioUrl(hit.track.streamUrl)) {
            remove(key)
            return null
        }
        return hit.track
    }

    fun invalidate(track: Track, isVideoMode: Boolean = false) {
        remove(cacheKey(track, isVideoMode))
    }

    suspend fun resolve(track: Track, isVideoMode: Boolean = false): Track {
        return resolveInternal(
            track = track,
            isVideoMode = isVideoMode,
            timeoutMs = playbackResolveTimeoutMs,
            preferMp4Audio = false,
            requestKind = "playback"
        )
    }

    suspend fun resolveForOffline(track: Track): Track {
        return resolveInternal(
            track = track,
            isVideoMode = false,
            timeoutMs = offlineResolveTimeoutMs,
            preferMp4Audio = true,
            requestKind = "offline"
        )
    }

    private suspend fun resolveInternal(
        track: Track,
        isVideoMode: Boolean,
        timeoutMs: Long,
        preferMp4Audio: Boolean,
        requestKind: String
    ): Track = coroutineScope {
        track.streamUrl.takeIf { it.isNotBlank() && streamStillFresh(it) && (isVideoMode || isDirectAudioUrl(it)) }?.let { return@coroutineScope track }
        cached(track, isVideoMode)?.let { return@coroutineScope it }

        val key = "${cacheKey(track, isVideoMode)}_$requestKind"
        Timber.d("resolver start kind=%s mode=%s id=%s quality=%s", requestKind, if (isVideoMode) "video" else "audio", track.id, selectedAudioQuality)
        val deferred = async(Dispatchers.IO, start = CoroutineStart.LAZY) {
            try {
                withTimeout(timeoutMs) {
                    resolveUncached(track.copy(streamUrl = ""), isVideoMode, preferMp4Audio)
                }
            } catch (error: TimeoutCancellationException) {
                val label = if (requestKind == "offline") "Download" else "YouTube"
                throw PlaybackBlockedException("$label lento: sto aspettando lo stream più del previsto, riprova tra qualche secondo")
            }
        }
        val previous = inFlight.putIfAbsent(key, deferred)
        if (previous != null) {
            Timber.d("resolver in-flight join kind=%s mode=%s id=%s", requestKind, if (isVideoMode) "video" else "audio", track.id)
            return@coroutineScope previous.await()
        }

        try {
            deferred.start()
            return@coroutineScope deferred.await()
        } finally {
            inFlight.remove(key, deferred)
        }
    }

    suspend fun prefetch(track: Track, isVideoMode: Boolean = false): Track? {
        if (track.streamUrl.isNotBlank()) {
            if (!isVideoMode && !isDirectAudioUrl(track.streamUrl)) return null
            if (streamStillFresh(track.streamUrl)) {
                store(track, isVideoMode)
                return track
            }
            return null
        }
        cached(track, isVideoMode)?.let { return it }
        return runCatching { resolve(track, isVideoMode) }.getOrNull()
    }

    private suspend fun resolveUncached(track: Track, isVideoMode: Boolean = false, preferMp4Audio: Boolean = false): Track = withContext(Dispatchers.IO) {
        val errors = Collections.synchronizedList(mutableListOf<String>())

        if (isVideoMode) {
            val resolved = coroutineScope {
                val winner = CompletableDeferred<Track?>()
                val extractorJob = launch {
                    val result = runCatching { resolveVideoWithMetrolistExtractor(track) }
                    result.onSuccess { winner.complete(it) }
                        .onFailure { error ->
                            error.message?.takeIf { it.isNotBlank() }
                                ?.let { errors += "MetrolistExtractor video: $it" }
                        }
                }
                val innerTubeJob = launch {
                    delay(LevyraResolverLatency.innerTubeFallbackDelayMs(isVideoMode = true, preferMp4Audio = false))
                    if (winner.isCompleted) return@launch
                    val stream = runCatching { hedgedInnerTube(track, errors, true) }.getOrNull()
                    if (stream != null) {
                        winner.complete(track.withDirectStream(stream))
                    }
                }
                launch {
                    extractorJob.join()
                    innerTubeJob.join()
                    winner.complete(null)
                }
                val result = winner.await()
                coroutineContext.cancelChildren()
                result
            }
            if (resolved != null) {
                store(resolved, isVideoMode)
                return@withContext resolved
            }
            val reason = errors.firstOrNull { it.contains("age", true) || it.contains("login", true) }
                ?: errors.firstOrNull()
                ?: "Video non disponibile"
            throw PlaybackBlockedException(reason)
        }

        val resolved = resolveAudioFast(track, errors, preferMp4Audio)
        if (resolved != null) {
            store(resolved, isVideoMode)
            return@withContext resolved
        }

        val alternate = resolveAudioWithSearchFallback(track, errors, preferMp4Audio)
        if (alternate != null) {
            store(alternate, isVideoMode)
            return@withContext alternate
        }

        val reason = errors.firstOrNull { it.contains("age", true) || it.contains("anonymous", true) || it.contains("login", true) || it.contains("accedi", true) }
            ?: errors.firstOrNull()
            ?: "Stream non disponibile"
        throw PlaybackBlockedException(reason)
    }

    private suspend fun resolveAudioFast(track: Track, errors: MutableList<String>, preferMp4Audio: Boolean): Track? {
        if (preferMp4Audio) return resolveAudioResilient(track, errors)
        return coroutineScope {
            val winner = CompletableDeferred<Track?>()
            val extractorJob = launch {
                val resolved = runCatching { resolveWithMetrolistExtractor(track, false) }
                resolved.onSuccess { winner.complete(it) }
                    .onFailure { error ->
                        error.message?.takeIf { it.isNotBlank() }
                            ?.let { errors += "MetrolistExtractor: $it" }
                    }
            }
            val innerTubeJob = launch {
                delay(LevyraResolverLatency.innerTubeFallbackDelayMs(isVideoMode = false, preferMp4Audio = false))
                if (winner.isCompleted) return@launch
                val stream = runCatching { hedgedInnerTube(track, errors, false) }.getOrNull()
                if (stream != null) winner.complete(track.withDirectStream(stream))
            }
            launch {
                extractorJob.join()
                innerTubeJob.join()
                winner.complete(null)
            }
            val result = winner.await()
            coroutineContext.cancelChildren()
            result
        }
    }

    private suspend fun resolveAudioResilient(track: Track, errors: MutableList<String>): Track? = coroutineScope {
        val winner = CompletableDeferred<Track?>()
        val extractorJob = launch {
            val resolved = runCatching { resolveWithMetrolistExtractor(track, true) }
            resolved.onSuccess { winner.complete(it) }
                .onFailure { error ->
                    error.message?.takeIf { it.isNotBlank() }
                        ?.let { errors += "MetrolistExtractor: $it" }
                }
        }
        val innerTubeJob = launch {
            delay(LevyraResolverLatency.innerTubeFallbackDelayMs(isVideoMode = false, preferMp4Audio = true))
            if (winner.isCompleted) return@launch
            val stream = runCatching { raceInnerTube(track, errors, false, true) }.getOrNull()
            if (stream != null) winner.complete(track.withDirectStream(stream))
        }
        launch {
            extractorJob.join()
            innerTubeJob.join()
            winner.complete(null)
        }
        val result = winner.await()
        coroutineContext.cancelChildren()
        result
    }

    private suspend fun resolveAudioWithSearchFallback(track: Track, errors: MutableList<String>, preferMp4Audio: Boolean): Track? {
        val candidates = findAlternativeAudioCandidates(track)
        if (candidates.isEmpty()) return null
        for (candidate in candidates) {
            val localErrors = Collections.synchronizedList(mutableListOf<String>())
            val resolved = runCatching { resolveAudioFast(candidate, localErrors, preferMp4Audio) }.getOrNull()
            if (resolved != null && resolved.streamUrl.isNotBlank() && streamStillFresh(resolved.streamUrl) && isDirectAudioUrl(resolved.streamUrl)) {
                return track.copy(
                    streamUrl = resolved.streamUrl,
                    videoUrl = resolved.videoUrl.ifBlank { candidate.videoUrl },
                    thumbnailUrl = track.thumbnailUrl.ifBlank { resolved.thumbnailUrl },
                    largeThumbnailUrl = track.largeThumbnailUrl.ifBlank { resolved.largeThumbnailUrl },
                    durationMs = resolved.durationMs.takeIf { it > 0L } ?: track.durationMs,
                    videoStreamUrl = "",
                    source = "${resolved.source} · fallback ${candidate.id}"
                )
            }
            localErrors.firstOrNull()?.takeIf { it.isNotBlank() }?.let { errors += "Fallback ${candidate.id}: $it" }
        }
        return null
    }

    private suspend fun findAlternativeAudioCandidates(track: Track): List<Track> = withContext(Dispatchers.IO) {
        val output = LinkedHashMap<String, Track>()
        val queries = alternativeSearchQueries(track)
        val repository = YoutubeMusicRepository(context)
        for (query in queries) {
            searchYouTubeWebCandidates(track, query)
                .asSequence()
                .filter { !sameVideoIdentity(track, it) }
                .forEach { candidate -> output.putIfAbsent(candidate.id, candidate) }
            if (output.size < 4) {
                runCatching { repository.search(query, 6, userPreferences.languageCode()) }
                    .getOrDefault(emptyList())
                    .asSequence()
                    .filter { it.id.isNotBlank() }
                    .filter { !sameVideoIdentity(track, it) }
                    .sortedByDescending { scoreAlternativeCandidate(track, it) }
                    .forEach { candidate ->
                        output.putIfAbsent(candidate.id, candidate.copy(streamUrl = "", videoStreamUrl = ""))
                    }
            }
            if (output.size >= 12) break
        }
        output.values
            .sortedByDescending { scoreAlternativeCandidate(track, it) }
            .take(12)
    }

    private fun alternativeSearchQueries(track: Track): List<String> {
        val title = track.title.cleanSearchToken()
        val artist = track.artist.cleanSearchToken()
        val base = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ").ifBlank { title.ifBlank { track.id } }
        return listOf(
            "$base official audio",
            "$base official video",
            "$base visual video",
            "$base topic",
            base
        ).map { it.trim() }.filter { it.length >= 2 }.distinct()
    }

    private fun searchYouTubeWebCandidates(track: Track, query: String): List<Track> {
        val encoded = query.urlEncode()
        val request = Request.Builder()
            .url("https://www.youtube.com/results?search_query=$encoded")
            .get()
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("User-Agent", profiles.first { it.clientName == "WEB_REMIX" }.userAgent)
            .build()
        return runCatching {
            searchFallbackClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val html = response.body.string()
                Regex("""\\?["]videoId\\?["]\s*:\s*\\?["]([A-Za-z0-9_-]{11})\\?["]""")
                    .findAll(html)
                    .mapNotNull { match -> match.groupValues.getOrNull(1) }
                    .distinct()
                    .take(8)
                    .map { id ->
                        track.copy(
                            id = id,
                            streamUrl = "",
                            videoStreamUrl = "",
                            videoUrl = "https://www.youtube.com/watch?v=$id",
                            source = "YouTube Web Fallback"
                        )
                    }
                    .toList()
            }
        }.getOrDefault(emptyList())
    }

    private fun sameVideoIdentity(left: Track, right: Track): Boolean {
        val leftId = left.id.trim()
        val rightId = right.id.trim()
        if (leftId.isNotBlank() && rightId.isNotBlank() && leftId == rightId) return true
        val leftVideoId = extractVideoId(left.videoUrl)
        val rightVideoId = extractVideoId(right.videoUrl)
        return leftVideoId.isNotBlank() && leftVideoId == rightVideoId
    }

    private fun extractVideoId(url: String): String {
        if (url.isBlank()) return ""
        Regex("(?:v=|/shorts/|/embed/|youtu\\.be/)([A-Za-z0-9_-]{11})").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        return url.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }.orEmpty()
    }

    private fun scoreAlternativeCandidate(original: Track, candidate: Track): Int {
        val originalTitle = original.title.lowercase()
        val originalArtist = original.artist.lowercase()
        val title = candidate.title.lowercase()
        val artist = candidate.artist.lowercase()
        val source = candidate.source.lowercase()
        var score = 0
        if (title == originalTitle) score += 400
        if (title.contains(originalTitle) || originalTitle.contains(title)) score += 180
        if (artist.contains(originalArtist) || originalArtist.contains(artist)) score += 160
        if (source.contains("youtube music")) score += 80
        if (title.contains("official")) score += 70
        if (title.contains("audio")) score += 60
        if (title.contains("video")) score += 35
        if (title.contains("lyrics") || title.contains("testo") || title.contains("karaoke") || title.contains("cover")) score -= 140
        val originalDuration = original.durationMs.takeIf { it > 0L }
        val candidateDuration = candidate.durationMs.takeIf { it > 0L }
        if (originalDuration != null && candidateDuration != null) {
            val delta = kotlin.math.abs(originalDuration - candidateDuration)
            score += when {
                delta <= 4_000L -> 180
                delta <= 12_000L -> 100
                delta <= 30_000L -> 30
                delta > 90_000L -> -200
                else -> -40
            }
        }
        return score
    }

    private fun String.cleanSearchToken(): String {
        return replace(Regex("\\s+"), " ")
            .replace(Regex("[\u0000-\u001F]"), "")
            .trim()
    }

    private suspend fun hedgedInnerTube(track: Track, errors: MutableList<String>, isVideoMode: Boolean): DirectStream? {
        val ladder = profiles
            .groupBy { it.tier }
            .toSortedMap()
            .map { (_, group) ->
                group.map { profile ->
                    suspend {
                        runCatching { resolveWithInnerTube(track, profile, isVideoMode, false) }
                            .onFailure { error ->
                                error.message?.takeIf { it.isNotBlank() }?.let { errors += "${profile.label}: $it" }
                            }
                            .getOrNull()
                            ?.takeIf { stream -> acceptResolvedStream(stream, isVideoMode, "${profile.label} probe", errors) }
                    }
                }
            }
        return hedgedFirst(ladder, hedgeBudgetMs)
    }

    private fun Track.withDirectStream(stream: DirectStream): Track {
        val artworkSafe = LevyraPersonalOrbit.preferAlbumArtwork(
            primary = this,
            donor = copy(thumbnailUrl = stream.thumbnailUrl, largeThumbnailUrl = stream.thumbnailUrl)
        )
        return artworkSafe.copy(
            streamUrl = stream.url,
            videoStreamUrl = stream.videoUrl,
            durationMs = stream.durationMs.takeIf { it > 0L } ?: durationMs,
            source = stream.source
        )
    }

    private suspend fun raceInnerTube(
        track: Track,
        errors: MutableList<String>,
        isVideoMode: Boolean = false,
        preferMp4Audio: Boolean = false
    ): DirectStream? = coroutineScope {
        val winner = CompletableDeferred<DirectStream?>()
        val fallback = AtomicReference<DirectStream?>(null)
        val workers = profiles.map { profile ->
            launch {
                if (profile.delayMs > 0L) delay(profile.delayMs)
                val attempt = runCatching { resolveWithInnerTube(track, profile, isVideoMode, preferMp4Audio) }
                attempt.onSuccess { stream ->
                    if (!acceptResolvedStream(stream, isVideoMode, "${profile.label} probe", errors)) return@onSuccess
                    if (!isVideoMode && preferMp4Audio && !isMp4AudioUrl(stream.url)) {
                        fallback.compareAndSet(null, stream)
                    } else {
                        winner.complete(stream)
                    }
                }.onFailure { error ->
                    error.message?.takeIf { it.isNotBlank() }?.let { errors += "${profile.label}: $it" }
                }
            }
        }
        launch {
            workers.joinAll()
            val candidate = fallback.get()
            if (!isVideoMode && preferMp4Audio && candidate != null) delay(700L)
            winner.complete(candidate)
        }
        val result = winner.await()
        coroutineContext.cancelChildren()
        result
    }

    private fun restoreCache() {
        runCatching {
            val now = System.currentTimeMillis()
            val editor = prefs.edit()
            var modified = false
            prefs.all.forEach { (key, value) ->
                val raw = value as? String ?: return@forEach
                val json = runCatching { JSONObject(raw) }.getOrNull() ?: return@forEach
                val streamUrl = json.optString("streamUrl")
                val expiresAt = json.optLong("expiresAt", json.optLong("at", 0L) + fallbackTtlMs)
                val track = json.optJSONObject("track")?.let(TrackJson::fromJson)?.copy(streamUrl = streamUrl)
                val audioCache = key.contains("_audio_", ignoreCase = true)
                if (track != null && streamUrl.isNotBlank() && now < expiresAt && streamStillFresh(streamUrl) && (!audioCache || isDirectAudioUrl(streamUrl))) {
                    streamCache[key] = CachedStream(track, expiresAt)
                } else {
                    editor.remove(key)
                    modified = true
                }
            }
            if (modified) editor.apply()
        }
    }

    private fun store(track: Track, isVideoMode: Boolean = false) {
        if (track.streamUrl.isBlank() || !streamStillFresh(track.streamUrl)) return
        if (!isVideoMode && !isDirectAudioUrl(track.streamUrl)) return
        val key = cacheKey(track, isVideoMode)
        val expiresAt = expiresAtFor(track.streamUrl)
        streamCache[key] = CachedStream(track, expiresAt)
        if (isVideoMode || track.videoStreamUrl.isNotBlank()) return
        val json = JSONObject()
            .put("expiresAt", expiresAt)
            .put("streamUrl", track.streamUrl)
            .put("track", TrackJson.toJson(track.copy(streamUrl = "")))
        prefs.edit().putString(key, json.toString()).apply()
    }

    private fun remove(key: String) {
        streamCache.remove(key)
        prefs.edit().remove(key).apply()
    }

    private fun isFresh(expiresAt: Long): Boolean = System.currentTimeMillis() < expiresAt

    private fun streamStillFresh(url: String): Boolean {
        val expire = expireSeconds(url) ?: return true
        return System.currentTimeMillis() + 90_000L < expire * 1000L
    }

    private fun isDirectAudioUrl(url: String): Boolean {
        if (isLegacyWebmAudioUrl(url) || isHlsManifestUrl(url)) return false
        return true
    }

    private fun isLegacyWebmAudioUrl(url: String): Boolean {
        val clean = url.lowercase()
        return clean.contains("mime=audio%2fwebm") && clean.contains("expire=0")
    }

    private fun isHlsManifestUrl(url: String): Boolean {
        val clean = url.substringBefore('#').lowercase()
        val path = clean.substringBefore('?')
        return path.endsWith(".m3u8") ||
            path.contains("/hls_playlist") ||
            path.contains("/manifest/hls") ||
            clean.contains("mime=application%2fx-mpegurl") ||
            clean.contains("mime=application/vnd.apple.mpegurl") ||
            clean.contains("type=application%2fx-mpegurl")
    }

    private fun isMp4AudioUrl(url: String): Boolean {
        val clean = url.lowercase()
        val path = clean.substringBefore('?')
        return clean.contains("mime=audio%2fmp4") || clean.contains("mime=audio/mp4") || path.endsWith(".m4a") || path.endsWith(".mp4")
    }

    private fun acceptResolvedStream(stream: DirectStream, isVideoMode: Boolean, label: String, errors: MutableList<String>): Boolean {
        if (stream.url.isBlank()) return false
        if (!streamStillFresh(stream.url)) {
            errors += "$label: URL scaduto"
            return false
        }
        if (isVideoMode) return true
        if (!isDirectAudioUrl(stream.url)) {
            errors += "$label: stream non audio diretto"
            return false
        }
        if (!verifyDirectAudioUrlFast(stream.url)) {
            errors += "$label: stream diretto non confermato"
            return false
        }
        return true
    }

    private fun verifyDirectAudioUrlFast(url: String): Boolean {
        if (url.isBlank() || !streamStillFresh(url) || !isDirectAudioUrl(url)) return false
        if (isTrustedGoogleVideoUrl(url)) return true
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Range", "bytes=0-8191")
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")
            .header("User-Agent", profiles.first().userAgent)
            .build()
        return runCatching {
            streamProbeClient.newCall(request).execute().use { response ->
                if (response.code == 403 || response.code == 404 || response.code == 410 || response.code == 416 || response.code == 429) return@use false
                if (response.code !in 200..299 && response.code != 206) return@use false
                val contentType = response.header("Content-Type").orEmpty().lowercase()
                if (contentType.contains("text/html") || contentType.contains("application/json")) return@use false
                val sample = response.peekBody(32L).bytes()
                sample.isNotEmpty()
            }
        }.getOrDefault(false)
    }

    private fun isTrustedGoogleVideoUrl(url: String): Boolean {
        val clean = url.lowercase()
        if (!clean.startsWith("https://")) return false
        if (!clean.contains("googlevideo.com/")) return false
        if (clean.contains("mime=audio%2f") || clean.contains("mime=audio/")) return true
        return clean.contains("/videoplayback") && !isHlsManifestUrl(clean)
    }

    private fun expiresAtFor(url: String): Long {
        val now = System.currentTimeMillis()
        val fromUrl = expireSeconds(url)?.times(1000L)?.minus(4L * 60L * 1000L)
        val fallback = now + fallbackTtlMs
        val capped = now + maxTtlMs
        return when {
            fromUrl == null -> fallback
            fromUrl <= now -> now
            else -> minOf(fromUrl, capped)
        }
    }

    private fun expireSeconds(url: String): Long? {
        return Regex("(?:[?&])expire=(\\d+)").find(url)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    private fun cacheKey(track: Track, isVideoMode: Boolean = false): String {
        val base = track.id.trim().ifBlank { track.videoUrl.trim() }
        val quality = selectedAudioQuality.lowercase()
        return if (isVideoMode) "${base}_video_$quality" else "${base}_audio_$quality"
    }

    private fun resolveWithInnerTube(track: Track, profile: ClientProfile, isVideoMode: Boolean = false, preferMp4Audio: Boolean = false): DirectStream {
        val endpoint = "https://www.youtube.com/youtubei/v1/player?key=$apiKey&prettyPrint=false"
        val body = buildPlayerBody(track.id, profile).toString()
        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(body.toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Origin", if (profile.clientName == "WEB_REMIX") "https://music.youtube.com" else "https://www.youtube.com")
            .header("Referer", if (profile.clientName == "WEB_EMBEDDED_PLAYER") "https://www.youtube.com/embed/${track.id}" else track.videoUrl)
            .header("User-Agent", profile.userAgent)
            .header("X-Youtube-Client-Name", profile.clientHeaderName)
            .header("X-Youtube-Client-Version", profile.clientVersion)
        val request = GoogleApiKeyHeaders.applyTo(requestBuilder, context).build()

        youtubeHttpClient.newCall(request).execute().use { response ->
            val responseText = response.body.string()
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            val root = JSONObject(responseText)
            val playability = root.optJSONObject("playabilityStatus")
            val status = playability?.optString("status").orEmpty()
            if (status.isNotBlank() && status != "OK") {
                val reason = playability?.optString("reason").orEmpty()
                val subreason = playability?.optJSONObject("errorScreen")?.toString().orEmpty()
                throw IllegalStateException(reason.ifBlank { subreason.ifBlank { status } })
            }
            val streamingData = root.optJSONObject("streamingData") ?: throw IllegalStateException("Nessun blocco streamingData")
            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()
            val muxedFormats = streamingData.optJSONArray("formats") ?: JSONArray()

            var bestAudioUrl = ""
            var bestAudioScore = Int.MIN_VALUE
            var bestAudioLabel = ""
            for (i in 0 until adaptiveFormats.length()) {
                val format = adaptiveFormats.optJSONObject(i) ?: continue
                val mime = format.optString("mimeType")
                val url = format.directFormatUrl()
                if (!mime.startsWith("audio/", true) || url.isBlank()) continue
                val itag = format.optInt("itag", 0)
                val bitrate = format.optInt("bitrate", 0)
                val audioQuality = format.optString("audioQuality")
                val score = scoreAudioFormat(mime, itag, bitrate, audioQuality, preferMp4Audio)
                if (score > bestAudioScore) {
                    bestAudioScore = score
                    bestAudioUrl = url
                    bestAudioLabel = formatLabel(mime, itag, bitrate, audioQuality)
                }
            }

            if (isVideoMode) {
                var bestVideoUrl = ""
                var bestVideoScore = -1
                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.optJSONObject(i) ?: continue
                    val mime = format.optString("mimeType")
                    val url = format.directFormatUrl()
                    if (!mime.startsWith("video/", true) || url.isBlank()) continue
                    val height = format.optInt("height", 0)
                    val penalty = if (height > 1080) -1 else 0
                    val mimeBoost = if (mime.contains("mp4", true)) 5000 else 0
                    val score = height + mimeBoost + penalty
                    if (score > bestVideoScore) {
                        bestVideoScore = score
                        bestVideoUrl = url
                    }
                }

                var muxedUrl = ""
                var muxedScore = -1
                for (i in 0 until muxedFormats.length()) {
                    val format = muxedFormats.optJSONObject(i) ?: continue
                    val mime = format.optString("mimeType")
                    val url = format.directFormatUrl()
                    if (!mime.startsWith("video/", true) || url.isBlank()) continue
                    val height = format.optInt("height", 0)
                    if (height > muxedScore) {
                        muxedScore = height
                        muxedUrl = url
                    }
                }

                val hlsUrl = streamingData.optString("hlsManifestUrl").takeIf { isVerifiedHlsManifest(it) }.orEmpty()

                val details = root.optJSONObject("videoDetails")
                val duration = details?.optString("lengthSeconds")?.toLongOrNull()?.times(1000L) ?: 0L
                val thumbnail = details?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.bestThumbnail().orEmpty()

                return when {
                    muxedUrl.isNotBlank() && muxedScore >= 480 -> DirectStream(
                        url = muxedUrl,
                        videoUrl = "",
                        durationMs = duration,
                        thumbnailUrl = thumbnail,
                        source = "YouTube Fast Muxed ${profile.label}"
                    )

                    bestAudioUrl.isNotBlank() && bestVideoUrl.isNotBlank() -> DirectStream(
                        url = bestAudioUrl,
                        videoUrl = bestVideoUrl,
                        durationMs = duration,
                        thumbnailUrl = thumbnail,
                        source = "YouTube Video ${profile.label}"
                    )

                    muxedUrl.isNotBlank() -> DirectStream(
                        url = muxedUrl,
                        videoUrl = "",
                        durationMs = duration,
                        thumbnailUrl = thumbnail,
                        source = "YouTube Muxed ${profile.label}"
                    )

                    hlsUrl.isNotBlank() -> DirectStream(
                        url = hlsUrl,
                        videoUrl = "",
                        durationMs = duration,
                        thumbnailUrl = thumbnail,
                        source = "YouTube HLS ${profile.label}"
                    )
                    else -> throw IllegalStateException("Nessuno stream video disponibile")
                }
            }

            if (bestAudioUrl.isBlank()) throw IllegalStateException("URL streaming assente")
            val details = root.optJSONObject("videoDetails")
            val duration = details?.optString("lengthSeconds")?.toLongOrNull()?.times(1000L) ?: 0L
            val thumbnail = details?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.bestThumbnail().orEmpty()
            return DirectStream(
                url = bestAudioUrl,
                videoUrl = "",
                durationMs = duration,
                thumbnailUrl = thumbnail,
                source = "YouTube ${profile.label}${bestAudioLabel.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}"
            )
        }
    }

    private fun selectAudioStream(streams: List<AudioStream>, preferMp4Audio: Boolean): AudioStream? {
        val direct = streams.filter { it.isUrl && it.content.isNotBlank() && isDirectAudioUrl(it.content) }
        val playable = direct.filter { streamStillFresh(it.content) }.ifEmpty { direct }
        return playable.maxByOrNull { scoreExtractorAudio(it, preferMp4Audio) }
    }

    private fun scoreExtractorAudio(stream: AudioStream, preferMp4Audio: Boolean): Int {
        val formatName = stream.getFormat()?.name.orEmpty()
        val content = stream.content
        val mime = "$formatName $content"
        return scoreAudioFormat(mime, stream.formatId, stream.averageBitrate, "", preferMp4Audio)
    }

    private fun isMp4AudioStream(stream: AudioStream): Boolean {
        val formatName = stream.getFormat()?.name.orEmpty()
        val content = stream.content.lowercase()
        return formatName.contains("MPEG", ignoreCase = true) ||
            formatName.contains("M4A", ignoreCase = true) ||
            content.contains("mime=audio%2fmp4") ||
            content.contains("mime=audio/mp4") ||
            content.substringBefore('?').endsWith(".m4a") ||
            content.substringBefore('?').endsWith(".mp4")
    }

    private fun isVerifiedHlsManifest(url: String): Boolean {
        if (url.isBlank() || !isHlsManifestUrl(url)) return false
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/vnd.apple.mpegurl,application/x-mpegURL,*/*")
            .header("User-Agent", profiles.first().userAgent)
            .header("Range", "bytes=0-2047")
            .build()
        return runCatching {
            youtubeHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val head = response.peekBody(2048L).string().trimStart('\uFEFF', ' ', '\n', '\r', '\t')
                head.startsWith("#EXTM3U")
            }
        }.getOrDefault(false)
    }

    private fun resolveWithMetrolistExtractor(track: Track, preferMp4Audio: Boolean = false): Track {
        NewPipeRuntime.ensure()
        val info = StreamInfo.getInfo(ServiceList.YouTube, track.videoUrl)
        val audio = selectAudioStream(info.audioStreams, preferMp4Audio)
        val url = audio?.content
            ?: throw IllegalStateException("Nessuno stream audio diretto disponibile per ${track.title}")
        val bestThumb = info.thumbnails.maxByOrNull { image ->
            image.width.coerceAtLeast(0) * image.height.coerceAtLeast(0)
        }?.url.orEmpty()
        val label = streamLabel(audio)
        val artworkSafe = LevyraPersonalOrbit.preferAlbumArtwork(
            primary = track,
            donor = track.copy(thumbnailUrl = bestThumb, largeThumbnailUrl = bestThumb)
        )
        return artworkSafe.copy(
            streamUrl = url,
            durationMs = if (info.duration > 0L) info.duration * 1000L else track.durationMs,
            source = "MetrolistExtractor${label.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}"
        )
    }

    private fun resolveVideoWithMetrolistExtractor(track: Track): Track {
        NewPipeRuntime.ensure()
        val info = StreamInfo.getInfo(ServiceList.YouTube, track.videoUrl)

        val bestThumb = info.thumbnails.maxByOrNull { image ->
            image.width.coerceAtLeast(0) * image.height.coerceAtLeast(0)
        }?.url.orEmpty()
        val artworkSafe = LevyraPersonalOrbit.preferAlbumArtwork(
            primary = track,
            donor = track.copy(thumbnailUrl = bestThumb, largeThumbnailUrl = bestThumb)
        )
        val durationMs = if (info.duration > 0L) info.duration * 1000L else track.durationMs

        val bestAudio = selectAudioStream(info.audioStreams, preferMp4Audio = false)?.content

        val muxed = info.videoStreams
            .filter { it.isUrl && it.content.isNotBlank() && streamStillFresh(it.content) }
            .maxByOrNull { heightOf(it.getResolution()) }
            ?: info.videoStreams
                .filter { it.isUrl && it.content.isNotBlank() }
                .maxByOrNull { heightOf(it.getResolution()) }

        if (muxed != null && heightOf(muxed.getResolution()) >= 480) {
            return artworkSafe.copy(
                streamUrl = muxed.content,
                videoStreamUrl = "",
                durationMs = durationMs,
                source = "MetrolistExtractor Fast Muxed"
            )
        }

        val bestVideoOnly = info.videoOnlyStreams
            .filter { it.isUrl && it.content.isNotBlank() && streamStillFresh(it.content) }
            .ifEmpty { info.videoOnlyStreams.filter { it.isUrl && it.content.isNotBlank() } }
            .filter { heightOf(it.getResolution()) in 1..1080 }
            .maxWithOrNull(
                compareBy<VideoStream> { heightOf(it.getResolution()) }
                    .thenBy { if (it.getFormat()?.name?.contains("MPEG", true) == true) 1 else 0 }
            )
            ?.content

        if (bestVideoOnly != null && bestAudio != null) {
            return artworkSafe.copy(
                streamUrl = bestAudio,
                videoStreamUrl = bestVideoOnly,
                durationMs = durationMs,
                source = "MetrolistExtractor Video"
            )
        }

        if (muxed != null) {
            return artworkSafe.copy(
                streamUrl = muxed.content,
                videoStreamUrl = "",
                durationMs = durationMs,
                source = "MetrolistExtractor Muxed"
            )
        }

        val hls = info.hlsUrl.takeIf { isVerifiedHlsManifest(it) }
        if (hls != null) {
            return artworkSafe.copy(
                streamUrl = hls,
                videoStreamUrl = "",
                durationMs = durationMs,
                source = "MetrolistExtractor HLS"
            )
        }

        throw IllegalStateException("Nessuno stream video disponibile per ${track.title}")
    }

    private fun streamLabel(stream: AudioStream): String {
        val format = stream.getFormat()?.name.orEmpty().ifBlank { "audio" }
        val bitrate = stream.averageBitrate.takeIf { it > 0 }?.let { "${it / 1000}kbps" }.orEmpty()
        val itag = stream.formatId.takeIf { it > 0 }?.let { "itag $it" }.orEmpty()
        return listOf(format, bitrate, itag).filter { it.isNotBlank() }.joinToString(" · ")
    }

    private fun heightOf(resolution: String?): Int {
        if (resolution.isNullOrBlank()) return 0
        return Regex("(\\d+)p").find(resolution)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    private fun buildPlayerBody(videoId: String, profile: ClientProfile): JSONObject {
        val locale = LevyraContentLocales.forLanguage(userPreferences.languageCode())
        val client = JSONObject()
            .put("clientName", profile.clientName)
            .put("clientVersion", profile.clientVersion)
            .put("hl", locale.hl)
            .put("gl", locale.gl)
        if (profile.android) {
            client.put("androidSdkVersion", 35)
                .put("osName", "Android")
                .put("osVersion", "15")
                .put("platform", "MOBILE")
        }
        if (profile.clientName == "IOS") {
            client.put("deviceMake", "Apple")
                .put("deviceModel", "iPhone16,2")
                .put("osName", "iPhone")
                .put("osVersion", "18.3")
                .put("platform", "MOBILE")
        }
        if (profile.clientName == "WEB_EMBEDDED_PLAYER") {
            client.put("clientScreen", "EMBED")
                .put("thirdParty", JSONObject().put("embedUrl", "https://www.youtube.com/embed/$videoId"))
        }
        return JSONObject()
            .put("context", JSONObject().put("client", client))
            .put("videoId", videoId)
            .put("contentCheckOk", true)
            .put("racyCheckOk", true)
            .put("playbackContext", JSONObject().put("contentPlaybackContext", JSONObject().put("html5Preference", "HTML5_PREF_WANTS")))
            .put("params", "CgIQBg")
            .put("watchEndpointMusicSupportedConfigs", JSONObject().put("watchEndpointMusicConfig", JSONObject().put("musicVideoType", "MUSIC_VIDEO_TYPE_ATV")))
    }

    private fun scoreAudioFormat(mime: String, itag: Int, bitrate: Int, audioQuality: String, preferMp4Audio: Boolean): Int {
        val clean = mime.lowercase()
        val isMp4 = clean.contains("mp4") || clean.contains("m4a") || clean.contains("mpeg")
        val isOpus = clean.contains("opus") || clean.contains("webm")
        val formatBias = when {
            preferMp4Audio && isMp4 -> 3_000_000
            preferMp4Audio && isOpus -> -300_000
            isOpus -> 620_000
            isMp4 -> 420_000
            else -> 0
        }
        val itagBias = if (selectedAudioQuality.equals("low", true)) {
            when (itag) {
                139 -> 420_000
                249 -> 380_000
                140 -> 260_000
                250 -> 180_000
                251 -> 80_000
                141 -> 40_000
                else -> 0
            }
        } else {
            when (itag) {
                251 -> 760_000
                141 -> 700_000
                140 -> 560_000
                250 -> 480_000
                249 -> 280_000
                139 -> 120_000
                else -> 0
            }
        }
        val qualityBias = when {
            audioQuality.contains("HIGH", true) -> 620_000
            audioQuality.contains("MEDIUM", true) -> 420_000
            audioQuality.contains("LOW", true) -> 120_000
            else -> 0
        }
        val bitrateBias = when (selectedAudioQuality.lowercase()) {
            "low" -> -bitrate
            "high" -> bitrate
            else -> bitrate / 2
        }
        return formatBias + itagBias + qualityBias + bitrateBias
    }

    private fun formatLabel(mime: String, itag: Int, bitrate: Int, audioQuality: String): String {
        val codec = when {
            mime.contains("opus", true) -> "Opus"
            mime.contains("webm", true) -> "WebM"
            mime.contains("mp4", true) || mime.contains("m4a", true) -> "M4A"
            else -> "Audio"
        }
        val br = bitrate.takeIf { it > 0 }?.let { "${it / 1000}kbps" }.orEmpty()
        val tag = itag.takeIf { it > 0 }?.let { "itag $it" }.orEmpty()
        val quality = audioQuality.removePrefix("AUDIO_QUALITY_").lowercase().replaceFirstChar { it.uppercase() }
        return listOf(codec, br, tag, quality).filter { it.isNotBlank() }.joinToString(" · ")
    }

    private fun JSONObject.directFormatUrl(): String {
        optString("url").takeIf { it.isNotBlank() }?.let { return it }
        val cipher = optString("signatureCipher").ifBlank { optString("cipher") }
        if (cipher.isBlank()) return ""
        val values = cipher.formValues()
        val base = values["url"].orEmpty()
        if (base.isBlank()) return ""
        val signature = values["sig"] ?: values["signature"]
        if (signature.isNullOrBlank()) return ""
        val signatureParameter = values["sp"].takeUnless { it.isNullOrBlank() } ?: "signature"
        return base.withQueryParameter(signatureParameter, signature)
    }

    private fun String.formValues(): Map<String, String> {
        val output = LinkedHashMap<String, String>()
        split('&').forEach { part ->
            val key = part.substringBefore('=', "")
            if (key.isBlank()) return@forEach
            val value = part.substringAfter('=', "")
            output[key.urlDecode()] = value.urlDecode()
        }
        return output
    }

    private fun String.withQueryParameter(key: String, value: String): String {
        val separator = if (contains('?')) "&" else "?"
        return "$this$separator${key.urlEncode()}=${value.urlEncode()}"
    }

    private fun String.urlDecode(): String {
        return runCatching { URLDecoder.decode(this, StandardCharsets.UTF_8.name()) }.getOrElse { this }
    }

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8.name())
    }

    private fun JSONArray.bestThumbnail(): String {
        var best = ""
        var score = -1
        for (i in 0 until length()) {
            val item = optJSONObject(i) ?: continue
            val url = item.optString("url")
            val current = item.optInt("width", 0) * item.optInt("height", 0)
            if (url.isNotBlank() && current >= score) {
                best = url
                score = current
            }
        }
        return best
    }
}

class PlaybackBlockedException(message: String) : IllegalStateException(message)

private data class ClientProfile(
    val clientName: String,
    val clientVersion: String,
    val label: String,
    val userAgent: String,
    val android: Boolean,
    val delayMs: Long,
    val tier: Int
) {
    val clientHeaderName: String
        get() = when (clientName) {
            "ANDROID" -> "3"
            "ANDROID_MUSIC" -> "21"
            "IOS" -> "5"
            "WEB_REMIX" -> "67"
            "WEB_EMBEDDED_PLAYER" -> "56"
            else -> "1"
        }
}

private data class DirectStream(
    val url: String,
    val videoUrl: String = "",
    val durationMs: Long,
    val thumbnailUrl: String,
    val source: String
)

private data class CachedStream(
    val track: Track,
    val expiresAt: Long
)
