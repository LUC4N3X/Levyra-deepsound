package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.security.GoogleApiKeyHeaders
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
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
        private const val YOUTUBE_VIDEO_ID_PATTERN = "[A-Za-z0-9_-]{11}"
        private const val LEVYRA_EXTRACTOR_PROVIDER = "LevyraExtractor"
        private const val LEVYRA_EXTRACTOR_HLS_PROVIDER = "LevyraExtractor HLS"
        private val youtubeVideoIdRegex = Regex(YOUTUBE_VIDEO_ID_PATTERN)
        private val youtubeVideoUrlRegex = Regex("(?:v=|/shorts/|/embed/|/live/|youtu\\.be/)($YOUTUBE_VIDEO_ID_PATTERN)")
        private val youtubeSearchResultVideoIdRegex = Regex("""\\?["]videoId\\?["]\s*:\s*\\?["]($YOUTUBE_VIDEO_ID_PATTERN)\\?["]""")

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
    private val clientHealthPrefs = context.getSharedPreferences("levyra_innertube_client_health", Context.MODE_PRIVATE)
    private val userPreferences = LevyraPreferences(context)
    private val streamCache = ConcurrentHashMap<String, CachedStream>()
    private val inFlight = ConcurrentHashMap<String, Deferred<Track>>()
    private val clientHealth = ConcurrentHashMap<String, ClientHealth>()
    private val failedPlaybackUrls = ConcurrentHashMap<String, Long>()
    private val youtubeEngagementCache = ConcurrentHashMap<String, CachedYoutubeEngagement>()
    private val videoSelector = LevyraVideoStreamSelector(context)
    private val youtubeHttpClient = LevyraHttpClientFactory.youtubePlayer()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val playbackSecurity = YoutubePlaybackSecurity(context, youtubeHttpClient, apiKey, userPreferences)
    private val resilienceEngine = PlaybackResilienceEngine(context)
    private val sourceMatchStore = PlaybackSourceMatchStore(LevyraDatabase.get(context).playbackSourceMatchDao())
    private val sourceMatchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fallbackTtlMs = 90L * 60L * 1000L
    private val maxTtlMs = 5L * 60L * 60L * 1000L
    private val youtubeEngagementTtlMs = 12L * 60L * 60L * 1000L
    private val youtubeEngagementNegativeTtlMs = 30L * 60L * 1000L
    private val youtubeEngagementCacheMaxEntries = 192
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
        ClientProfile("ANDROID_VR", "1.65.10", "Android VR", "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; Quest 3 Build/SQ3A.220605.009.A1) gzip", true, 0L, 0, false),
        ClientProfile("ANDROID_MUSIC", "8.10.52", "Android Music", "Mozilla/5.0 (Linux; Android 15; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) com.google.android.apps.youtube.music/8.10.52", true, 0L, 1, false),
        ClientProfile("ANDROID", "19.44.38", "Android", "com.google.android.youtube/19.44.38 (Linux; U; Android 15)", true, 0L, 2, false),
        ClientProfile("IOS", "20.10.4", "iOS", "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3 like Mac OS X; it_IT)", false, 0L, 3, false),
        ClientProfile("WEB_REMIX", "1.20260423.01.00", "YouTube Music Web", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36", false, 0L, 4, true),
        ClientProfile("WEB", "2.20260630.01.00", "YouTube Web", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36", false, 0L, 5, true),
        ClientProfile("WEB_EMBEDDED_PLAYER", "1.20260423.01.00", "Embedded Player", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36", false, 0L, 6, false)
    )

    init {
        restoreCache()
        restoreClientHealth()
    }

    fun setAudioQuality(value: String) {
        selectedAudioQuality = normalizeAudioQuality(value)
    }

    private fun normalizeAudioQuality(value: String): String {
        return when (value.lowercase()) {
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
            "https://music.youtube.com/generate_204",
            "https://youtubei.googleapis.com/generate_204"
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

    suspend fun enrichYoutubeEngagement(track: Track): Track {
        if (track.source.equals("Offline", ignoreCase = true)) return track
        if (track.youtubeLikeCount >= 0L && track.youtubeViewCount >= 0L) return track
        val videoId = extractVideoId(track.videoUrl).ifBlank { track.id.takeIf(youtubeVideoIdRegex::matches).orEmpty() }
        if (videoId.isBlank()) return track
        val now = System.currentTimeMillis()
        youtubeEngagementCache[videoId]?.let { cached ->
            if (now < cached.expiresAtMs) {
                return track.withYoutubeEngagement(cached.likeCount, cached.viewCount)
            }
            youtubeEngagementCache.remove(videoId, cached)
        }
        val videoUrl = track.videoUrl.ifBlank { "https://www.youtube.com/watch?v=$videoId" }
        return withContext(Dispatchers.IO) {
            runCatching {
                NewPipeRuntime.ensure()
                val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
                cacheYoutubeEngagement(videoId, info.likeCount, info.viewCount)
                track.withYoutubeEngagement(info.likeCount, info.viewCount)
            }.getOrElse { error ->
                Timber.d(error, "youtube engagement unavailable for %s", videoId)
                cacheYoutubeEngagement(videoId, -1L, -1L)
                track
            }
        }
    }

    fun cached(track: Track, isVideoMode: Boolean = false): Track? {
        return cached(track, isVideoMode, selectedAudioQuality)
    }

    private fun cached(track: Track, isVideoMode: Boolean, audioQuality: String): Track? {
        if (track.streamUrl.isNotBlank()) {
            if (isPlaybackUrlBlocked(track.streamUrl) || track.videoStreamUrl.isNotBlank() && isPlaybackUrlBlocked(track.videoStreamUrl)) return null
            if (!isVideoMode && !isPlayableAudioUrl(track.streamUrl)) return null
            return if (streamStillFresh(track.streamUrl)) track else null
        }
        val key = cacheKey(track, isVideoMode, audioQuality)
        val hit = streamCache[key] ?: return null
        if (!isFresh(hit.expiresAt)) {
            remove(key)
            return null
        }
        if (!isVideoMode && !isPlayableAudioUrl(hit.track.streamUrl)) {
            remove(key)
            return null
        }
        return hit.track
    }

    fun invalidate(track: Track, isVideoMode: Boolean = false) {
        remove(cacheKey(track, isVideoMode))
    }

    fun reportPlaybackFailure(track: Track, isVideoMode: Boolean, reason: String) {
        invalidate(track, isVideoMode)
        resilienceEngine.recordPlayerFailure(track.id, isVideoMode, reason)
        val now = System.currentTimeMillis()
        val lower = reason.lowercase()
        val recovery = resilienceEngine.recoveryPlan(reason)
        listOf(track.streamUrl, track.videoStreamUrl)
            .filter { it.isNotBlank() }
            .forEach { failedPlaybackUrls[it] = now + recovery.quarantineMs }
        if (recovery.rotateClient) {
            profileFromSource(track.source)?.let { profile ->
                recordClientFailure(profile, null, PlaybackBlockedException(reason))
            }
        }
        if (recovery.refreshSecurity) {
            YoutubeLocalDecoder.notifyStreamRejected(track.source)
        }
        if (recovery.rotateCodec) {
            videoSelector.reportPlaybackFailure(track.videoStreamUrl.ifBlank { track.streamUrl }, lower)
        }
        val sourceMatchQuarantineMs = when {
            lower.contains("403") || lower.contains("410") || lower.contains("expired") || lower.contains("scadut") || lower.contains("signature") -> 0L
            lower.contains("decoder") || lower.contains("codec") -> recovery.quarantineMs
            else -> minOf(recovery.quarantineMs, 20_000L)
        }
        sourceMatchScope.launch {
            runCatching {
                sourceMatchStore.recordFailure(track, isVideoMode, selectedAudioQuality, sourceMatchQuarantineMs)
            }.onFailure { error ->
                Timber.w(error, "persistent source match failure update failed")
            }
        }
    }

    fun playbackDiagnostics(): String {
        val health = clientHealth.mapValues { (_, value) ->
            JSONObject()
                .put("successes", value.successes)
                .put("failures", value.failures)
                .put("consecutiveFailures", value.consecutiveFailures)
                .put("averageLatencyMs", value.averageLatencyMs.takeUnless { it == Long.MAX_VALUE } ?: -1L)
                .put("blockedUntilMs", value.blockedUntilMs)
                .put("score", value.score)
        }
        return resilienceEngine.diagnostics(health)
    }

    private fun isPlaybackUrlBlocked(url: String): Boolean {
        if (url.isBlank()) return true
        val until = failedPlaybackUrls[url] ?: return false
        if (until > System.currentTimeMillis()) return true
        failedPlaybackUrls.remove(url, until)
        return false
    }

    suspend fun resolve(track: Track, isVideoMode: Boolean = false): Track {
        return resolveInternal(
            track = track,
            isVideoMode = isVideoMode,
            timeoutMs = playbackResolveTimeoutMs,
            preferMp4Audio = false,
            requestKind = "playback",
            audioQuality = selectedAudioQuality,
            reuseProvidedStream = true
        )
    }

    suspend fun resolveForOffline(track: Track, audioQualityOverride: String? = null): Track {
        val quality = normalizeAudioQuality(audioQualityOverride ?: selectedAudioQuality)
        return resolveInternal(
            track = track,
            isVideoMode = false,
            timeoutMs = offlineResolveTimeoutMs,
            preferMp4Audio = true,
            requestKind = "offline",
            audioQuality = quality,
            reuseProvidedStream = audioQualityOverride == null
        )
    }

    private suspend fun resolveInternal(
        track: Track,
        isVideoMode: Boolean,
        timeoutMs: Long,
        preferMp4Audio: Boolean,
        requestKind: String,
        audioQuality: String,
        reuseProvidedStream: Boolean
    ): Track = coroutineScope {
        track.streamUrl.takeIf {
            reuseProvidedStream &&
            it.isNotBlank() &&
                !isPlaybackUrlBlocked(it) &&
                (track.videoStreamUrl.isBlank() || !isPlaybackUrlBlocked(track.videoStreamUrl)) &&
                streamStillFresh(it) &&
                (isVideoMode || isPlayableAudioUrl(it)) &&
                (!preferMp4Audio || track.playbackManifest?.supportsMp4AudioExport() == true || isMp4AudioUrl(it))
        }?.let { return@coroutineScope track }
        if (!preferMp4Audio) {
            val cacheLookupTrack = if (reuseProvidedStream) track else track.copy(streamUrl = "", videoStreamUrl = "")
            cached(cacheLookupTrack, isVideoMode, audioQuality)?.let { return@coroutineScope it }
        }

        val key = "${cacheKey(track, isVideoMode, audioQuality)}_$requestKind"
        Timber.d("resolver start kind=%s mode=%s id=%s quality=%s", requestKind, if (isVideoMode) "video" else "audio", track.id, audioQuality)
        val deferred = async(Dispatchers.IO, start = CoroutineStart.LAZY) {
            try {
                withTimeout(timeoutMs) {
                    resolveUncached(track.copy(streamUrl = "", videoStreamUrl = ""), isVideoMode, preferMp4Audio, audioQuality)
                }
            } catch (error: TimeoutCancellationException) {
                val label = if (requestKind == "offline") "Download" else "YouTube"
                throw PlaybackBlockedException("$label lento: sto aspettando lo stream più del previsto, riprova tra qualche secondo")
            }
        }
        val previous = inFlight.putIfAbsent(key, deferred)
        if (previous != null) {
            deferred.cancel()
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
            if (!isVideoMode && !isPlayableAudioUrl(track.streamUrl)) return null
            if (streamStillFresh(track.streamUrl)) {
                store(track, track, isVideoMode)
                return track
            }
            return null
        }
        cached(track, isVideoMode)?.let { return it }
        return runCatching { resolve(track, isVideoMode) }.getOrNull()
    }

    private suspend fun resolveUncached(
        track: Track,
        isVideoMode: Boolean = false,
        preferMp4Audio: Boolean = false,
        audioQuality: String = selectedAudioQuality
    ): Track = withContext(Dispatchers.IO) {
        val errors = Collections.synchronizedList(mutableListOf<String>())

        restorePersistentSource(track, isVideoMode, preferMp4Audio, audioQuality, errors)?.let { restored ->
            store(track, restored, isVideoMode, audioQuality)
            return@withContext restored
        }

        if (isVideoMode) {
            val resolved = coroutineScope {
                val winner = CompletableDeferred<Track?>()
                val extractorJob = launch {
                    delay(LevyraResolverLatency.extractorHedgeDelayMs(isVideoMode = true, preferMp4Audio = false))
                    if (winner.isCompleted) return@launch
                    val result = runCatching { resolveVideoWithLevyraExtractor(track, audioQuality) }
                    result.onSuccess { winner.complete(it) }
                        .onFailure { error ->
                            errors += "LevyraExtractor video: ${error.playbackDiagnostic()}"
                        }
                }
                val innerTubeJob = launch {
                    delay(LevyraResolverLatency.innerTubeFallbackDelayMs(isVideoMode = true, preferMp4Audio = false))
                    if (winner.isCompleted) return@launch
                    val stream = runCatching { hedgedInnerTube(track, errors, true, audioQuality) }.getOrNull()
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
                store(track, resolved, isVideoMode, audioQuality)
                persistResolvedSource(track, resolved, isVideoMode, audioQuality, 92, preferMp4Audio)
                return@withContext resolved
            }
            val reason = errors.firstOrNull { it.contains("age", true) || it.contains("login", true) }
                ?: errors.firstOrNull()
                ?: "Video non disponibile"
            throw PlaybackBlockedException(reason)
        }

        val resolved = resolveAudioFast(track, errors, preferMp4Audio, audioQuality)
        if (resolved != null) {
            store(track, resolved, isVideoMode, audioQuality)
            persistResolvedSource(track, resolved, isVideoMode, audioQuality, 96, preferMp4Audio)
            return@withContext resolved
        }

        val alternate = resolveAudioWithSearchFallback(track, errors, preferMp4Audio, audioQuality)
        if (alternate != null) {
            store(track, alternate, isVideoMode, audioQuality)
            persistResolvedSource(track, alternate, isVideoMode, audioQuality, 84, preferMp4Audio)
            return@withContext alternate
        }

        val reason = errors.firstOrNull { it.startsWith("LevyraExtractor:") }
            ?: errors.firstOrNull { it.contains("age", true) || it.contains("anonymous", true) || it.contains("login", true) || it.contains("accedi", true) }
            ?: errors.firstOrNull()
            ?: "Stream non disponibile"
        throw PlaybackBlockedException(reason)
    }

    private suspend fun resolveAudioFast(
        track: Track,
        errors: MutableList<String>,
        preferMp4Audio: Boolean,
        audioQuality: String
    ): Track? {
        if (preferMp4Audio) return resolveAudioResilient(track, errors, audioQuality)
        return coroutineScope {
            val winner = CompletableDeferred<Track?>()
            val extractorJob = launch {
                delay(LevyraResolverLatency.extractorHedgeDelayMs(isVideoMode = false, preferMp4Audio = false))
                if (winner.isCompleted) return@launch
                val resolved = runCatching { resolveWithLevyraExtractor(track, false, audioQuality) }
                resolved.onSuccess { winner.complete(it) }
                    .onFailure { error ->
                        errors += "LevyraExtractor: ${error.playbackDiagnostic()}"
                    }
            }
            val innerTubeJob = launch {
                delay(LevyraResolverLatency.innerTubeFallbackDelayMs(isVideoMode = false, preferMp4Audio = false))
                if (winner.isCompleted) return@launch
                val stream = runCatching { hedgedInnerTube(track, errors, false, audioQuality) }.getOrNull()
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

    private suspend fun resolveAudioResilient(
        track: Track,
        errors: MutableList<String>,
        audioQuality: String
    ): Track? = coroutineScope {
        val winner = CompletableDeferred<Track?>()
        val extractorJob = launch {
            delay(LevyraResolverLatency.extractorHedgeDelayMs(isVideoMode = false, preferMp4Audio = true))
            if (winner.isCompleted) return@launch
            val resolved = runCatching { resolveWithLevyraExtractor(track, true, audioQuality) }
            resolved.onSuccess { winner.complete(it) }
                .onFailure { error ->
                    errors += "LevyraExtractor: ${error.playbackDiagnostic()}"
                }
        }
        val innerTubeJob = launch {
            delay(LevyraResolverLatency.innerTubeFallbackDelayMs(isVideoMode = false, preferMp4Audio = true))
            if (winner.isCompleted) return@launch
            val stream = runCatching { raceInnerTube(track, errors, false, true, audioQuality) }.getOrNull()
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

    private suspend fun resolveAudioWithSearchFallback(
        track: Track,
        errors: MutableList<String>,
        preferMp4Audio: Boolean,
        audioQuality: String
    ): Track? {
        val candidates = findAlternativeAudioCandidates(track)
        if (candidates.isEmpty()) return null
        for (candidate in candidates) {
            val localErrors = Collections.synchronizedList(mutableListOf<String>())
            val resolved = runCatching { resolveAudioFast(candidate, localErrors, preferMp4Audio, audioQuality) }.getOrNull()
            if (
                resolved != null &&
                resolved.streamUrl.isNotBlank() &&
                streamStillFresh(resolved.streamUrl) &&
                isPlayableAudioUrl(resolved.streamUrl) &&
                (!preferMp4Audio || resolved.playbackManifest?.supportsMp4AudioExport() == true || isMp4AudioUrl(resolved.streamUrl))
            ) {
                return track.copy(
                    streamUrl = resolved.streamUrl,
                    videoUrl = resolved.videoUrl.ifBlank { candidate.videoUrl },
                    thumbnailUrl = track.thumbnailUrl.ifBlank { resolved.thumbnailUrl },
                    largeThumbnailUrl = track.largeThumbnailUrl.ifBlank { resolved.largeThumbnailUrl },
                    durationMs = resolved.durationMs.takeIf { it > 0L } ?: track.durationMs,
                    videoStreamUrl = "",
                    source = "${resolved.source} · fallback ${candidate.id}",
                    youtubeLoudnessDb = resolved.youtubeLoudnessDb,
                    youtubePerceptualLoudnessDb = resolved.youtubePerceptualLoudnessDb,
                    youtubeLikeCount = resolved.youtubeLikeCount,
                    youtubeViewCount = resolved.youtubeViewCount,
                    playbackManifest = resolved.playbackManifest
                )
            }
            localErrors.firstOrNull()?.takeIf { it.isNotBlank() }?.let { errors += "Fallback ${candidate.id}: $it" }
        }
        return null
    }

    private suspend fun restorePersistentSource(
        track: Track,
        isVideoMode: Boolean,
        preferMp4Audio: Boolean,
        audioQuality: String,
        errors: MutableList<String>
    ): Track? {
        val stored = runCatching { sourceMatchStore.load(track, isVideoMode, audioQuality, preferMp4Audio) }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "persistent source match load failed")
            }
            .getOrNull() ?: return null
        val now = System.currentTimeMillis()
        if (stored.entity.blockedUntil > now) return null
        val manifest = stored.manifest
        if (manifest != null && manifest.isFresh(now) && manifestUrlsUsable(manifest, isVideoMode, preferMp4Audio)) {
            recordSourceMatchSuccess(track, isVideoMode, audioQuality, preferMp4Audio)
            return track.applyManifest(
                manifest = manifest,
                sourceVideoUrl = stored.entity.sourceVideoUrl,
                source = stored.entity.provider.ifBlank { "Persistent source match" }
            )
        }
        val sourceVideoId = stored.entity.sourceVideoId
            .takeIf(youtubeVideoIdRegex::matches)
            ?: return null
        val sourceVideoUrl = stored.entity.sourceVideoUrl.ifBlank { "https://www.youtube.com/watch?v=$sourceVideoId" }
        val sourceTrack = track.copy(
            id = sourceVideoId,
            videoUrl = sourceVideoUrl,
            streamUrl = "",
            videoStreamUrl = "",
            playbackManifest = null
        )
        val refreshed = try {
            refreshExactPersistentSource(sourceTrack, isVideoMode, preferMp4Audio, audioQuality, errors)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            errors += "Persistent source $sourceVideoId: ${error.playbackDiagnostic()}"
            null
        }
        if (refreshed == null) {
            recordSourceMatchFailure(track, isVideoMode, audioQuality, 90_000L, preferMp4Audio)
            return null
        }
        val rebased = rebaseResolvedTrack(track, refreshed)
        persistResolvedSource(
            track,
            rebased,
            isVideoMode,
            audioQuality,
            stored.entity.confidence.coerceAtLeast(88),
            preferMp4Audio
        )
        return rebased
    }

    private suspend fun refreshExactPersistentSource(
        sourceTrack: Track,
        isVideoMode: Boolean,
        preferMp4Audio: Boolean,
        audioQuality: String,
        errors: MutableList<String>
    ): Track? {
        val extractor = runCatching {
            if (isVideoMode) {
                resolveVideoWithLevyraExtractor(sourceTrack, audioQuality)
            } else {
                resolveWithLevyraExtractor(sourceTrack, preferMp4Audio, audioQuality)
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            errors += "Persistent LevyraExtractor: ${error.playbackDiagnostic()}"
        }.getOrNull()
        if (extractor != null && manifestUrlsUsable(extractor.playbackManifest, isVideoMode, preferMp4Audio)) return extractor
        val direct = runCatching {
            if (preferMp4Audio) {
                raceInnerTube(sourceTrack, errors, isVideoMode, true, audioQuality)
            } else {
                hedgedInnerTube(sourceTrack, errors, isVideoMode, audioQuality)
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            errors += "Persistent InnerTube: ${error.playbackDiagnostic()}"
        }.getOrNull() ?: return null
        val resolved = sourceTrack.withDirectStream(direct)
        return resolved.takeIf { manifestUrlsUsable(it.playbackManifest, isVideoMode, preferMp4Audio) }
    }

    private fun manifestUrlsUsable(
        manifest: ResolvedPlaybackManifest?,
        isVideoMode: Boolean,
        preferMp4Audio: Boolean = false
    ): Boolean {
        if (manifest == null || manifest.selectedAudioUrl.isBlank()) return false
        if (isPlaybackUrlBlocked(manifest.selectedAudioUrl) || !streamStillFresh(manifest.selectedAudioUrl)) return false
        if (!isVideoMode && !isPlayableAudioUrl(manifest.selectedAudioUrl)) return false
        if (preferMp4Audio && !manifest.supportsMp4AudioExport()) return false
        val videoUrl = manifest.selectedVideoUrl
        if (videoUrl.isNotBlank() && (isPlaybackUrlBlocked(videoUrl) || !streamStillFresh(videoUrl))) return false
        return true
    }

    private fun Track.applyManifest(
        manifest: ResolvedPlaybackManifest,
        sourceVideoUrl: String,
        source: String
    ): Track {
        return copy(
            streamUrl = manifest.selectedAudioUrl,
            videoStreamUrl = manifest.selectedVideoUrl,
            videoUrl = sourceVideoUrl.ifBlank { videoUrl },
            durationMs = manifest.durationMs.takeIf { it > 0L } ?: durationMs,
            source = source,
            youtubeLoudnessDb = manifest.loudnessDb ?: youtubeLoudnessDb,
            youtubePerceptualLoudnessDb = manifest.perceptualLoudnessDb ?: youtubePerceptualLoudnessDb,
            playbackManifest = manifest
        )
    }

    private fun rebaseResolvedTrack(original: Track, resolved: Track): Track {
        val artworkSafe = LevyraPersonalOrbit.preferAlbumArtwork(original, resolved)
        return artworkSafe.copy(
            streamUrl = resolved.streamUrl,
            videoStreamUrl = resolved.videoStreamUrl,
            videoUrl = resolved.videoUrl.ifBlank { original.videoUrl },
            durationMs = resolved.durationMs.takeIf { it > 0L } ?: original.durationMs,
            source = resolved.source,
            youtubeLoudnessDb = resolved.youtubeLoudnessDb ?: original.youtubeLoudnessDb,
            youtubePerceptualLoudnessDb = resolved.youtubePerceptualLoudnessDb ?: original.youtubePerceptualLoudnessDb,
            youtubeLikeCount = resolved.youtubeLikeCount.takeIf { it >= 0L } ?: original.youtubeLikeCount,
            youtubeViewCount = resolved.youtubeViewCount.takeIf { it >= 0L } ?: original.youtubeViewCount,
            playbackManifest = resolved.playbackManifest
        )
    }

    private suspend fun recordSourceMatchSuccess(
        track: Track,
        isVideoMode: Boolean,
        audioQuality: String,
        preferMp4Audio: Boolean = false
    ) {
        try {
            sourceMatchStore.recordSuccess(track, isVideoMode, audioQuality, preferMp4Audio)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "persistent source match success update failed")
        }
    }

    private suspend fun recordSourceMatchFailure(
        track: Track,
        isVideoMode: Boolean,
        audioQuality: String,
        quarantineMs: Long,
        preferMp4Audio: Boolean = false
    ) {
        try {
            sourceMatchStore.recordFailure(track, isVideoMode, audioQuality, quarantineMs, preferMp4Audio)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "persistent source match failure update failed")
        }
    }

    private suspend fun persistResolvedSource(
        original: Track,
        resolved: Track,
        isVideoMode: Boolean,
        audioQuality: String,
        confidence: Int,
        preferMp4Audio: Boolean = false
    ) {
        val manifest = resolved.playbackManifest ?: return
        if (preferMp4Audio && !manifest.supportsMp4AudioExport()) return
        try {
            sourceMatchStore.save(original, resolved, isVideoMode, audioQuality, confidence, preferMp4Audio)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "persistent source match save failed")
        }
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
                youtubeSearchResultVideoIdRegex
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
        youtubeVideoUrlRegex.find(url)?.groupValues?.getOrNull(1)?.let { return it }
        return url.takeIf(youtubeVideoIdRegex::matches).orEmpty()
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

    private suspend fun hedgedInnerTube(
        track: Track,
        errors: MutableList<String>,
        isVideoMode: Boolean,
        audioQuality: String
    ): DirectStream? {
        val ladder = orderedProfiles().map { profile ->
            listOf(
                suspend {
                    runCatching { resolveWithInnerTube(track, profile, isVideoMode, false, audioQuality) }
                        .onFailure { error ->
                            error.message?.takeIf { it.isNotBlank() }?.let { errors += "${profile.label}: $it" }
                        }
                        .getOrNull()
                        ?.takeIf { stream ->
                            val accepted = acceptResolvedStream(stream, isVideoMode, "${profile.label} probe", errors)
                            if (!accepted) recordClientFailure(profile, null, IllegalStateException("Stream non valido o URL scaduto"))
                            accepted
                        }
                }
            )
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
            source = stream.source,
            youtubeLoudnessDb = stream.loudnessDb ?: youtubeLoudnessDb,
            youtubePerceptualLoudnessDb = stream.perceptualLoudnessDb ?: youtubePerceptualLoudnessDb,
            playbackManifest = stream.manifest
        )
    }

    private suspend fun raceInnerTube(
        track: Track,
        errors: MutableList<String>,
        isVideoMode: Boolean = false,
        preferMp4Audio: Boolean = false,
        audioQuality: String = selectedAudioQuality
    ): DirectStream? = coroutineScope {
        val winner = CompletableDeferred<DirectStream?>()
        val fallback = AtomicReference<DirectStream?>(null)
        val workers = orderedProfiles().mapIndexed { index, profile ->
            launch {
                val dynamicDelay = profile.delayMs + index * 20L
                if (dynamicDelay > 0L) delay(dynamicDelay)
                val attempt = runCatching { resolveWithInnerTube(track, profile, isVideoMode, preferMp4Audio, audioQuality) }
                attempt.onSuccess { stream ->
                    if (!acceptResolvedStream(stream, isVideoMode, "${profile.label} probe", errors)) {
                        recordClientFailure(profile, null, IllegalStateException("Stream non valido o URL scaduto"))
                        return@onSuccess
                    }
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

    private fun profileFromSource(source: String): ClientProfile? {
        val normalized = source.lowercase()
        return profiles.firstOrNull { profile ->
            normalized.contains(profile.label.lowercase()) || normalized.contains(profile.clientName.lowercase())
        }
    }

    private fun orderedProfiles(): List<ClientProfile> {
        val now = System.currentTimeMillis()
        val available = profiles.filter { profile -> (clientHealth[profile.clientName]?.blockedUntilMs ?: 0L) <= now }
        val candidates = if (available.isNotEmpty()) available else profiles
        val sorted = candidates.sortedWith(
            compareByDescending<ClientProfile> { profile -> clientHealth[profile.clientName]?.score ?: 50.0 }
                .thenBy { profile -> clientHealth[profile.clientName]?.averageLatencyMs ?: Long.MAX_VALUE }
                .thenBy { it.tier }
        )
        val vr = sorted.firstOrNull { it.clientName == "ANDROID_VR" } ?: return sorted
        val best = sorted.firstOrNull() ?: return sorted
        val vrHealth = clientHealth[vr.clientName]
        val bestScore = clientHealth[best.clientName]?.score ?: 50.0
        val vrScore = vrHealth?.score ?: 50.0
        val keepVrPrimary = vrHealth?.consecutiveFailures.orZero() == 0 && vrScore >= bestScore - 12.0
        return if (keepVrPrimary) listOf(vr) + sorted.filterNot { it === vr } else sorted
    }

    private fun recordClientSuccess(profile: ClientProfile, latencyMs: Long) {
        clientHealth.compute(profile.clientName) { name, current ->
            val previous = current ?: ClientHealth()
            val samples = (previous.successes + 1).coerceAtMost(10_000)
            val average = if (previous.successes <= 0 || previous.averageLatencyMs == Long.MAX_VALUE) {
                latencyMs
            } else {
                ((previous.averageLatencyMs * 7L) + latencyMs) / 8L
            }
            previous.copy(
                successes = samples,
                consecutiveFailures = 0,
                averageLatencyMs = average.coerceAtLeast(1L),
                blockedUntilMs = 0L,
                updatedAtMs = System.currentTimeMillis()
            ).also { persistClientHealth(name, it) }
        }
    }

    private fun recordClientFailure(profile: ClientProfile, latencyMs: Long?, error: Throwable) {
        clientHealth.compute(profile.clientName) { name, current ->
            val previous = current ?: ClientHealth()
            val failures = (previous.failures + 1).coerceAtMost(10_000)
            val consecutive = (previous.consecutiveFailures + 1).coerceAtMost(100)
            val message = error.message.orEmpty().lowercase()
            val hardBlock = message.contains("http 403") || message.contains("http 410") || message.contains("http 429") || message.contains("sign in") || message.contains("login")
            val invalidStream = message.contains("scaduto") || message.contains("stream non valido")
            val blockDurationMs = when {
                hardBlock -> 10L * 60L * 1000L
                invalidStream -> 2L * 60L * 1000L
                consecutive >= 4 -> 2L * 60L * 1000L
                consecutive >= 2 -> 25_000L
                else -> 0L
            }
            val averageLatencyMs = when {
                latencyMs == null -> previous.averageLatencyMs
                previous.averageLatencyMs <= 0L || previous.averageLatencyMs == Long.MAX_VALUE -> latencyMs
                else -> ((previous.averageLatencyMs * 3L) + latencyMs) / 4L
            }
            previous.copy(
                failures = failures,
                consecutiveFailures = consecutive,
                averageLatencyMs = averageLatencyMs,
                blockedUntilMs = System.currentTimeMillis() + blockDurationMs,
                updatedAtMs = System.currentTimeMillis()
            ).also { persistClientHealth(name, it) }
        }
    }

    private fun elapsedMs(startedAt: Long): Long = ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(1L)

    private fun restoreClientHealth() {
        clientHealthPrefs.all.forEach { (name, rawValue) ->
            val raw = rawValue as? String ?: return@forEach
            val json = runCatching { JSONObject(raw) }.getOrNull() ?: return@forEach
            clientHealth[name] = ClientHealth(
                successes = json.optInt("successes", 0),
                failures = json.optInt("failures", 0),
                consecutiveFailures = json.optInt("consecutiveFailures", 0),
                averageLatencyMs = json.optLong("averageLatencyMs", Long.MAX_VALUE),
                blockedUntilMs = json.optLong("blockedUntilMs", 0L),
                updatedAtMs = json.optLong("updatedAtMs", 0L)
            )
        }
    }

    private fun persistClientHealth(name: String, health: ClientHealth) {
        val json = JSONObject()
            .put("successes", health.successes)
            .put("failures", health.failures)
            .put("consecutiveFailures", health.consecutiveFailures)
            .put("averageLatencyMs", health.averageLatencyMs)
            .put("blockedUntilMs", health.blockedUntilMs)
            .put("updatedAtMs", health.updatedAtMs)
        clientHealthPrefs.edit().putString(name, json.toString()).apply()
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
                val manifest = PlaybackManifestCodec.decode(json.optString("manifestJson"))
                val track = json.optJSONObject("track")?.let(TrackJson::fromJson)?.copy(
                    streamUrl = streamUrl,
                    playbackManifest = manifest
                )
                val audioCache = key.contains("_audio_", ignoreCase = true)
                if (track != null && streamUrl.isNotBlank() && now < expiresAt && streamStillFresh(streamUrl) && (!audioCache || isPlayableAudioUrl(streamUrl))) {
                    streamCache[key] = CachedStream(track, expiresAt)
                } else {
                    editor.remove(key)
                    modified = true
                }
            }
            if (modified) editor.apply()
        }
    }

    private fun store(
        requestedTrack: Track,
        resolvedTrack: Track,
        isVideoMode: Boolean = false,
        audioQuality: String = selectedAudioQuality
    ) {
        if (resolvedTrack.streamUrl.isBlank() || !streamStillFresh(resolvedTrack.streamUrl)) return
        if (!isVideoMode && !isPlayableAudioUrl(resolvedTrack.streamUrl)) return
        val key = cacheKey(requestedTrack, isVideoMode, audioQuality)
        val expiresAt = resolvedTrack.playbackManifest?.expiresAtMs
            ?.takeIf { it > System.currentTimeMillis() }
            ?.let { minOf(it, expiresAtFor(resolvedTrack.streamUrl)) }
            ?: expiresAtFor(resolvedTrack.streamUrl)
        streamCache[key] = CachedStream(resolvedTrack, expiresAt)
        if (isVideoMode || resolvedTrack.videoStreamUrl.isNotBlank()) return
        val json = JSONObject()
            .put("expiresAt", expiresAt)
            .put("streamUrl", resolvedTrack.streamUrl)
            .put("manifestJson", resolvedTrack.playbackManifest?.let(PlaybackManifestCodec::encode).orEmpty())
            .put("track", TrackJson.toJson(resolvedTrack.copy(streamUrl = "", playbackManifest = null)))
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

    private fun isPlayableAudioUrl(url: String): Boolean {
        if (url.isBlank() || isLegacyWebmAudioUrl(url)) return false
        return isDirectAudioUrl(url) || isHlsManifestUrl(url)
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
        if (isPlaybackUrlBlocked(stream.url) || stream.videoUrl.isNotBlank() && isPlaybackUrlBlocked(stream.videoUrl)) {
            errors += "$label: stream temporaneamente escluso dopo un errore di riproduzione"
            return false
        }
        if (!streamStillFresh(stream.url)) {
            errors += "$label: URL scaduto"
            return false
        }
        if (isVideoMode) return true
        if (isHlsManifestUrl(stream.url)) {
            if (!isVerifiedHlsManifest(stream.url)) {
                errors += "$label: manifest HLS non valido"
                return false
            }
            return true
        }
        if (!isDirectAudioUrl(stream.url)) {
            errors += "$label: stream audio non supportato"
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

    private fun cacheKey(
        track: Track,
        isVideoMode: Boolean = false,
        audioQuality: String = selectedAudioQuality
    ): String {
        val base = PlaybackSourceIdentity.canonicalKey(track)
        val quality = normalizeAudioQuality(audioQuality).lowercase()
        return if (isVideoMode) "${base}_video_$quality" else "${base}_audio_$quality"
    }

    private suspend fun resolveWithInnerTube(
        track: Track,
        profile: ClientProfile,
        isVideoMode: Boolean = false,
        preferMp4Audio: Boolean = false,
        audioQuality: String = selectedAudioQuality
    ): DirectStream {
        val startedAt = System.nanoTime()
        val mode = if (isVideoMode) "video" else if (preferMp4Audio) "offline" else "audio"
        resilienceEngine.recordAttempt(profile.label, mode)
        return try {
            val firstAttempt = runCatching {
                resolveWithInnerTubeOnce(track, profile, isVideoMode, preferMp4Audio, audioQuality)
            }
            val stream = firstAttempt.getOrElse { firstError ->
                if (!playbackSecurity.rotateIfNeeded(firstError)) throw firstError
                resolveWithInnerTubeOnce(track, profile, isVideoMode, preferMp4Audio, audioQuality)
            }
            playbackSecurity.resetFailureState()
            val latency = elapsedMs(startedAt)
            recordClientSuccess(profile, latency)
            resilienceEngine.recordSuccess(profile.label, mode, latency, stream.source)
            stream
        } catch (error: Throwable) {
            val latency = elapsedMs(startedAt)
            recordClientFailure(profile, latency, error)
            resilienceEngine.recordFailure(profile.label, mode, latency, error)
            throw error
        }
    }

    private suspend fun resolveWithInnerTubeOnce(
        track: Track,
        profile: ClientProfile,
        isVideoMode: Boolean,
        preferMp4Audio: Boolean,
        audioQuality: String
    ): DirectStream = withContext(Dispatchers.IO) {
        val session = if (profile.requiresPoToken) {
            playbackSecurity.currentSession()
        } else {
            playbackSecurity.cachedSession()
        }
        val poTokens = if (profile.requiresPoToken) playbackSecurity.poTokens(track.id, session) else null
        val signatureTimestamp = if (profile.clientName.startsWith("WEB")) {
            runCatching { YoutubeJavaScriptPlayerManager.getSignatureTimestamp(track.id) }.getOrNull()
        } else {
            null
        }
        val endpoint = "https://www.youtube.com/youtubei/v1/player?key=$apiKey&prettyPrint=false"
        val body = buildPlayerBody(
            videoId = track.id,
            profile = profile,
            visitorData = session.visitorData,
            playerPoToken = poTokens?.playerToken,
            signatureTimestamp = signatureTimestamp
        ).toString()
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
        session.visitorData.takeIf { it.isNotBlank() }?.let {
            requestBuilder.header("X-Goog-Visitor-Id", it)
        }
        if (profile.clientName == "ANDROID_VR") {
            requestBuilder.header("X-Goog-Api-Format-Version", "2")
        }
        val request = GoogleApiKeyHeaders.applyTo(requestBuilder, context).build()

        youtubeHttpClient.newCall(request).execute().use responseUse@{ response ->
            val responseText = response.body.string()
            if (!response.isSuccessful) {
                throw YoutubePlayerRequestException(response.code, "HTTP ${response.code}")
            }
            val root = JSONObject(responseText)
            root.optJSONObject("responseContext")
                ?.optString("visitorData")
                ?.takeIf { it.isNotBlank() }
                ?.let(playbackSecurity::observeVisitorData)
            val playability = root.optJSONObject("playabilityStatus")
            val status = playability?.optString("status").orEmpty()
            if (status.isNotBlank() && status != "OK") {
                val reason = playability?.optString("reason").orEmpty()
                val subreason = playability?.optJSONObject("errorScreen")?.toString().orEmpty()
                throw YoutubePlayerRequestException(null, reason.ifBlank { subreason.ifBlank { status } })
            }
            val streamingData = root.optJSONObject("streamingData")
                ?: throw YoutubePlayerRequestException(null, "Nessun blocco streamingData")
            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()
            val muxedFormats = streamingData.optJSONArray("formats") ?: JSONArray()
            val audioConfig = root.optJSONObject("playerConfig")?.optJSONObject("audioConfig")
            val loudnessDb = audioConfig?.finiteFloat("loudnessDb")
            val perceptualLoudnessDb = audioConfig?.finiteFloat("perceptualLoudnessDb")

            val audioCandidates = buildList {
                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.optJSONObject(i) ?: continue
                    val mime = format.optString("mimeType")
                    if (!mime.startsWith("audio/", true)) continue
                    val itag = format.optInt("itag", 0)
                    val bitrate = format.optInt("bitrate", 0)
                    val formatAudioQuality = format.optString("audioQuality")
                    add(
                        Triple(
                            format,
                            scoreAudioFormat(mime, itag, bitrate, formatAudioQuality, preferMp4Audio, audioQuality),
                            formatLabel(mime, itag, bitrate, formatAudioQuality)
                        )
                    )
                }
            }.sortedByDescending { it.second }

            var bestAudioUrl = ""
            var bestAudioLabel = ""
            var bestAudioFormat: JSONObject? = null
            for ((format, _, label) in audioCandidates) {
                val url = format.resolveFormatUrl(
                    videoId = track.id,
                    streamingPoToken = poTokens?.streamingToken,
                    transformThrottling = profile.clientName.startsWith("WEB")
                )
                if (url.isBlank() || isPlaybackUrlBlocked(url)) continue
                bestAudioUrl = url
                bestAudioLabel = label
                bestAudioFormat = format
                break
            }

            if (isVideoMode) {
                val videoOnlyCandidates = buildList {
                    for (i in 0 until adaptiveFormats.length()) {
                        val format = adaptiveFormats.optJSONObject(i) ?: continue
                        val mime = format.optString("mimeType")
                        val url = format.resolveFormatUrl(track.id, poTokens?.streamingToken, profile.clientName.startsWith("WEB"))
                        if (!mime.startsWith("video/", true) || url.isBlank()) continue
                        add(
                            LevyraVideoCandidate(
                                url = url,
                                mimeType = mime.substringBefore(';'),
                                codec = codecFromMimeType(mime),
                                width = format.optInt("width", 0),
                                height = format.optInt("height", 0),
                                fps = format.optInt("fps", 0),
                                bitrate = format.optInt("bitrate", 0),
                                itag = format.optInt("itag", 0),
                                muxed = false,
                                label = "adaptive"
                            )
                        )
                    }
                }
                val muxedCandidates = buildList {
                    for (i in 0 until muxedFormats.length()) {
                        val format = muxedFormats.optJSONObject(i) ?: continue
                        val mime = format.optString("mimeType")
                        val url = format.resolveFormatUrl(track.id, poTokens?.streamingToken, profile.clientName.startsWith("WEB"))
                        if (!mime.startsWith("video/", true) || url.isBlank()) continue
                        add(
                            LevyraVideoCandidate(
                                url = url,
                                mimeType = mime.substringBefore(';'),
                                codec = codecFromMimeType(mime),
                                width = format.optInt("width", 0),
                                height = format.optInt("height", 0),
                                fps = format.optInt("fps", 0),
                                bitrate = format.optInt("bitrate", 0),
                                itag = format.optInt("itag", 0),
                                muxed = true,
                                label = "muxed"
                            )
                        )
                    }
                }
                val selection = videoSelector.select(
                    muxedCandidates = muxedCandidates,
                    videoOnlyCandidates = videoOnlyCandidates,
                    hasSeparateAudio = bestAudioUrl.isNotBlank(),
                    blocked = ::isPlaybackUrlBlocked
                )
                val hlsUrl = if (selection == null) {
                    streamingData.optString("hlsManifestUrl")
                        .takeIf { it.isNotBlank() }
                        ?.let { it.finalizeStreamingUrl(track.id, poTokens?.streamingToken, profile.clientName.startsWith("WEB")) }
                        ?.takeIf { !isPlaybackUrlBlocked(it) && isVerifiedHlsManifest(it) }
                        .orEmpty()
                } else {
                    ""
                }
                val details = root.optJSONObject("videoDetails")
                val duration = details?.optString("lengthSeconds")?.toLongOrNull()?.times(1000L) ?: 0L
                val thumbnail = details?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.bestThumbnail().orEmpty()
                return@responseUse when {
                    selection?.candidate?.muxed == true -> {
                        val manifest = buildManifest(
                            sourceVideoId = track.id,
                            provider = "YouTube ${profile.label}",
                            durationMs = duration,
                            selectedAudioUrl = selection.candidate.url,
                            selectedVideoUrl = "",
                            streams = listOf(videoDescriptor(selection.candidate, true)),
                            loudness = PlaybackLoudness(loudnessDb, perceptualLoudnessDb)
                        )
                        DirectStream(
                            url = selection.candidate.url,
                            videoUrl = "",
                            durationMs = duration,
                            thumbnailUrl = thumbnail,
                            source = "YouTube ${profile.label} · ${selection.reason}",
                            manifest = manifest,
                            loudnessDb = loudnessDb,
                            perceptualLoudnessDb = perceptualLoudnessDb
                        )
                    }
                    selection != null && bestAudioUrl.isNotBlank() -> {
                        val manifest = buildManifest(
                            sourceVideoId = track.id,
                            provider = "YouTube ${profile.label}",
                            durationMs = duration,
                            selectedAudioUrl = bestAudioUrl,
                            selectedVideoUrl = selection.candidate.url,
                            streams = listOf(
                                innerTubeAudioDescriptor(bestAudioFormat, bestAudioUrl, true),
                                videoDescriptor(selection.candidate, true)
                            ),
                            loudness = PlaybackLoudness(loudnessDb, perceptualLoudnessDb)
                        )
                        DirectStream(
                            url = bestAudioUrl,
                            videoUrl = selection.candidate.url,
                            durationMs = duration,
                            thumbnailUrl = thumbnail,
                            source = "YouTube ${profile.label} · ${selection.reason}",
                            manifest = manifest,
                            loudnessDb = loudnessDb,
                            perceptualLoudnessDb = perceptualLoudnessDb
                        )
                    }
                    hlsUrl.isNotBlank() -> {
                        val manifest = buildManifest(
                            sourceVideoId = track.id,
                            provider = "YouTube HLS ${profile.label}",
                            durationMs = duration,
                            selectedAudioUrl = hlsUrl,
                            selectedVideoUrl = "",
                            streams = listOf(hlsDescriptor(hlsUrl)),
                            loudness = PlaybackLoudness(loudnessDb, perceptualLoudnessDb)
                        )
                        DirectStream(
                            url = hlsUrl,
                            videoUrl = "",
                            durationMs = duration,
                            thumbnailUrl = thumbnail,
                            source = "YouTube HLS ${profile.label}",
                            manifest = manifest,
                            loudnessDb = loudnessDb,
                            perceptualLoudnessDb = perceptualLoudnessDb
                        )
                    }
                    else -> throw YoutubePlayerRequestException(null, "Nessuno stream video compatibile disponibile")
                }
            }

            if (bestAudioUrl.isBlank()) throw YoutubePlayerRequestException(null, "URL streaming assente")
            val details = root.optJSONObject("videoDetails")
            val duration = details?.optString("lengthSeconds")?.toLongOrNull()?.times(1000L) ?: 0L
            val thumbnail = details?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.bestThumbnail().orEmpty()
            val manifest = buildManifest(
                sourceVideoId = track.id,
                provider = "YouTube ${profile.label}",
                durationMs = duration,
                selectedAudioUrl = bestAudioUrl,
                selectedVideoUrl = "",
                streams = listOf(innerTubeAudioDescriptor(bestAudioFormat, bestAudioUrl, true)),
                loudness = PlaybackLoudness(loudnessDb, perceptualLoudnessDb)
            )
            DirectStream(
                url = bestAudioUrl,
                videoUrl = "",
                durationMs = duration,
                thumbnailUrl = thumbnail,
                source = "YouTube ${profile.label}${bestAudioLabel.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}",
                manifest = manifest,
                loudnessDb = loudnessDb,
                perceptualLoudnessDb = perceptualLoudnessDb
            )
        }
    }

    private fun selectAudioStream(
        streams: List<AudioStream>,
        preferMp4Audio: Boolean,
        audioQuality: String = selectedAudioQuality
    ): AudioStream? {
        val direct = streams.filter {
            it.isUrl &&
                it.content.isNotBlank() &&
                !isPlaybackUrlBlocked(it.content) &&
                isDirectAudioUrl(it.content)
        }
        val playable = direct.filter { streamStillFresh(it.content) }.ifEmpty { direct }
        return playable.maxByOrNull { scoreExtractorAudio(it, preferMp4Audio, audioQuality) }
    }

    private fun scoreExtractorAudio(stream: AudioStream, preferMp4Audio: Boolean, audioQuality: String): Int {
        val formatName = stream.getFormat()?.name.orEmpty()
        val content = stream.content
        val mime = "$formatName $content"
        return scoreAudioFormat(mime, stream.formatId, stream.averageBitrate, "", preferMp4Audio, audioQuality)
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

    private fun resolveWithLevyraExtractor(
        track: Track,
        preferMp4Audio: Boolean = false,
        audioQuality: String = selectedAudioQuality
    ): Track {
        NewPipeRuntime.ensure()
        val info = StreamInfo.getInfo(ServiceList.YouTube, track.videoUrl)
        val audio = selectAudioStream(info.audioStreams, preferMp4Audio, audioQuality)
        val hlsUrl = if (audio == null && !preferMp4Audio) {
            info.hlsUrl.takeIf { isVerifiedHlsManifest(it) }
        } else {
            null
        }
        val url = audio?.content ?: hlsUrl
            ?: throw IllegalStateException("LevyraExtractor non ha restituito stream audio diretti o HLS per ${track.title}")
        val bestThumb = info.thumbnails.maxByOrNull { image ->
            image.width.coerceAtLeast(0) * image.height.coerceAtLeast(0)
        }?.url.orEmpty()
        val label = audio?.let { streamLabel(it) }.orEmpty()
        val artworkSafe = LevyraPersonalOrbit.preferAlbumArtwork(
            primary = track,
            donor = track.copy(thumbnailUrl = bestThumb, largeThumbnailUrl = bestThumb)
        )
        val durationMs = if (info.duration > 0L) info.duration * 1000L else track.durationMs
        val sourceVideoId = extractVideoId(track.videoUrl).ifBlank { track.id }
        val provider = if (audio != null) LEVYRA_EXTRACTOR_PROVIDER else LEVYRA_EXTRACTOR_HLS_PROVIDER
        val descriptors = if (audio != null) {
            info.audioStreams
                .asSequence()
                .filter { it.isUrl && it.content.isNotBlank() && streamStillFresh(it.content) }
                .map { audioDescriptor(it, it.content == url) }
                .toList()
        } else {
            listOf(hlsDescriptor(url))
        }
        val manifest = buildManifest(
            sourceVideoId = sourceVideoId,
            provider = provider,
            durationMs = durationMs,
            selectedAudioUrl = url,
            selectedVideoUrl = "",
            streams = descriptors
        )
        return artworkSafe.copy(
            streamUrl = url,
            videoStreamUrl = "",
            durationMs = durationMs,
            source = if (audio != null) {
                "LevyraExtractor${label.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}"
            } else {
                LEVYRA_EXTRACTOR_HLS_PROVIDER
            },
            playbackManifest = manifest
        ).withYoutubeEngagement(info.likeCount, info.viewCount).also {
            cacheYoutubeEngagement(sourceVideoId, info.likeCount, info.viewCount)
        }
    }

    private fun resolveVideoWithLevyraExtractor(
        track: Track,
        audioQuality: String = selectedAudioQuality
    ): Track {
        NewPipeRuntime.ensure()
        val info = StreamInfo.getInfo(ServiceList.YouTube, track.videoUrl)
        val bestThumb = info.thumbnails.maxByOrNull { image ->
            image.width.coerceAtLeast(0) * image.height.coerceAtLeast(0)
        }?.url.orEmpty()
        val artworkSafe = LevyraPersonalOrbit.preferAlbumArtwork(
            primary = track,
            donor = track.copy(thumbnailUrl = bestThumb, largeThumbnailUrl = bestThumb)
        ).withYoutubeEngagement(info.likeCount, info.viewCount)
        val sourceVideoId = extractVideoId(track.videoUrl).ifBlank { track.id }
        cacheYoutubeEngagement(sourceVideoId, info.likeCount, info.viewCount)
        val durationMs = if (info.duration > 0L) info.duration * 1000L else track.durationMs
        val selectedAudio = selectAudioStream(info.audioStreams, preferMp4Audio = false, audioQuality = audioQuality)
        val bestAudio = selectedAudio?.content.orEmpty()
        val muxedCandidates = info.videoStreams
            .filter { it.isUrl && it.content.isNotBlank() && streamStillFresh(it.content) }
            .map(::extractorVideoCandidate)
        val videoOnlyCandidates = info.videoOnlyStreams
            .filter { it.isUrl && it.content.isNotBlank() && streamStillFresh(it.content) }
            .map(::extractorVideoCandidate)
        val selection = videoSelector.select(
            muxedCandidates = muxedCandidates,
            videoOnlyCandidates = videoOnlyCandidates,
            hasSeparateAudio = bestAudio.isNotBlank(),
            blocked = ::isPlaybackUrlBlocked
        )
        if (selection != null) {
            val selectedAudioUrl = if (selection.candidate.muxed) selection.candidate.url else bestAudio
            val selectedVideoUrl = if (selection.candidate.muxed) "" else selection.candidate.url
            val descriptors = buildList {
                info.audioStreams
                    .asSequence()
                    .filter { it.isUrl && it.content.isNotBlank() && streamStillFresh(it.content) }
                    .mapTo(this) { audioDescriptor(it, it.content == selectedAudioUrl) }
                muxedCandidates.mapTo(this) { videoDescriptor(it, it.url == selection.candidate.url) }
                videoOnlyCandidates.mapTo(this) { videoDescriptor(it, it.url == selection.candidate.url) }
            }
            val manifest = buildManifest(
                sourceVideoId = sourceVideoId,
                provider = LEVYRA_EXTRACTOR_PROVIDER,
                durationMs = durationMs,
                selectedAudioUrl = selectedAudioUrl,
                selectedVideoUrl = selectedVideoUrl,
                streams = descriptors
            )
            return artworkSafe.copy(
                streamUrl = selectedAudioUrl,
                videoStreamUrl = selectedVideoUrl,
                durationMs = durationMs,
                source = "LevyraExtractor · ${selection.reason}",
                playbackManifest = manifest
            )
        }
        val hls = info.hlsUrl.takeIf { it.isNotBlank() && !isPlaybackUrlBlocked(it) && isVerifiedHlsManifest(it) }
        if (hls != null) {
            val manifest = buildManifest(
                sourceVideoId = sourceVideoId,
                provider = LEVYRA_EXTRACTOR_HLS_PROVIDER,
                durationMs = durationMs,
                selectedAudioUrl = hls,
                selectedVideoUrl = "",
                streams = listOf(hlsDescriptor(hls))
            )
            return artworkSafe.copy(
                streamUrl = hls,
                videoStreamUrl = "",
                durationMs = durationMs,
                source = LEVYRA_EXTRACTOR_HLS_PROVIDER,
                playbackManifest = manifest
            )
        }
        throw IllegalStateException("Nessuno stream video compatibile per ${track.title}")
    }

    private fun Track.withYoutubeEngagement(likeCount: Long, viewCount: Long): Track = copy(
        youtubeLikeCount = likeCount.takeIf { it >= 0L } ?: youtubeLikeCount,
        youtubeViewCount = viewCount.takeIf { it >= 0L } ?: youtubeViewCount
    )

    private fun cacheYoutubeEngagement(videoId: String, likeCount: Long, viewCount: Long) {
        if (videoId.isBlank()) return
        val now = System.currentTimeMillis()
        if (youtubeEngagementCache.size >= youtubeEngagementCacheMaxEntries) {
            youtubeEngagementCache.entries.removeIf { now >= it.value.expiresAtMs }
            if (youtubeEngagementCache.size >= youtubeEngagementCacheMaxEntries) {
                youtubeEngagementCache.entries
                    .minByOrNull { it.value.expiresAtMs }
                    ?.let { youtubeEngagementCache.remove(it.key, it.value) }
            }
        }
        val ttlMs = if (likeCount >= 0L || viewCount >= 0L) {
            youtubeEngagementTtlMs
        } else {
            youtubeEngagementNegativeTtlMs
        }
        youtubeEngagementCache[videoId] = CachedYoutubeEngagement(
            likeCount = likeCount,
            viewCount = viewCount,
            expiresAtMs = now + ttlMs
        )
    }

    private fun extractorVideoCandidate(stream: VideoStream): LevyraVideoCandidate {
        val format = stream.getFormat()
        val resolution = stream.getResolution()
        return LevyraVideoCandidate(
            url = stream.content,
            mimeType = format?.mimeType.orEmpty(),
            codec = stream.codec.orEmpty(),
            width = stream.width,
            height = stream.height.takeIf { it > 0 } ?: heightOf(resolution),
            fps = stream.fps,
            bitrate = stream.bitrate,
            itag = stream.itag,
            muxed = !stream.isVideoOnly(),
            label = resolution.orEmpty()
        )
    }

    private fun codecFromMimeType(mimeType: String): String {
        return Regex("codecs=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
            .find(mimeType)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
    }

    private fun buildManifest(
        sourceVideoId: String,
        provider: String,
        durationMs: Long,
        selectedAudioUrl: String,
        selectedVideoUrl: String,
        streams: List<PlaybackStreamDescriptor>,
        loudness: PlaybackLoudness = PlaybackLoudness()
    ): ResolvedPlaybackManifest {
        val selectedUrls = setOf(selectedAudioUrl, selectedVideoUrl).filter { it.isNotBlank() }.toSet()
        val normalizedStreams = streams
            .filter { it.url.isNotBlank() }
            .map { descriptor -> descriptor.copy(selected = descriptor.url in selectedUrls) }
            .distinctBy { descriptor -> listOf(descriptor.kind.name, descriptor.itag.toString(), descriptor.url).joinToString("|") }
        val selectedExpiry = normalizedStreams
            .filter { it.selected }
            .mapNotNull { it.expiresAtMs.takeIf { expiry -> expiry > 0L } }
            .minOrNull()
            ?: expiresAtFor(selectedAudioUrl)
        return ResolvedPlaybackManifest(
            sourceVideoId = sourceVideoId,
            provider = provider,
            resolvedAtMs = System.currentTimeMillis(),
            expiresAtMs = selectedExpiry,
            durationMs = durationMs,
            selectedAudioUrl = selectedAudioUrl,
            selectedVideoUrl = selectedVideoUrl,
            streams = normalizedStreams,
            loudnessDb = loudness.loudnessDb,
            perceptualLoudnessDb = loudness.perceptualLoudnessDb
        ).compact()
    }

    private fun audioDescriptor(stream: AudioStream, selected: Boolean = false): PlaybackStreamDescriptor {
        val format = stream.getFormat()
        return PlaybackStreamDescriptor(
            url = stream.content,
            kind = PlaybackStreamKind.AUDIO,
            deliveryMethod = if (isHlsManifestUrl(stream.content)) PlaybackDeliveryMethod.HLS else PlaybackDeliveryMethod.PROGRESSIVE,
            container = format?.suffix.orEmpty(),
            mimeType = format?.mimeType.orEmpty(),
            codec = stream.codec.orEmpty(),
            bitrate = stream.bitrate.coerceAtLeast(0),
            averageBitrate = stream.averageBitrate.coerceAtLeast(0),
            itag = stream.itag,
            qualityLabel = stream.quality.orEmpty(),
            expiresAtMs = expiresAtFor(stream.content),
            selected = selected
        )
    }

    private fun videoDescriptor(candidate: LevyraVideoCandidate, selected: Boolean = false): PlaybackStreamDescriptor {
        return PlaybackStreamDescriptor(
            url = candidate.url,
            kind = if (candidate.muxed) PlaybackStreamKind.MUXED else PlaybackStreamKind.VIDEO,
            deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
            container = candidate.mimeType.substringAfter('/', "").substringBefore(';'),
            mimeType = candidate.mimeType.substringBefore(';'),
            codec = candidate.codec,
            bitrate = candidate.bitrate.coerceAtLeast(0),
            width = candidate.width.coerceAtLeast(0),
            height = candidate.height.coerceAtLeast(0),
            fps = candidate.fps.coerceAtLeast(0),
            itag = candidate.itag,
            qualityLabel = candidate.label,
            expiresAtMs = expiresAtFor(candidate.url),
            selected = selected
        )
    }

    private fun hlsDescriptor(url: String, selected: Boolean = true): PlaybackStreamDescriptor {
        return PlaybackStreamDescriptor(
            url = url,
            kind = PlaybackStreamKind.HLS,
            deliveryMethod = PlaybackDeliveryMethod.HLS,
            container = "m3u8",
            mimeType = "application/x-mpegURL",
            expiresAtMs = expiresAtFor(url),
            selected = selected
        )
    }

    private fun innerTubeAudioDescriptor(format: JSONObject?, url: String, selected: Boolean = true): PlaybackStreamDescriptor {
        val mime = format?.optString("mimeType").orEmpty()
        return PlaybackStreamDescriptor(
            url = url,
            kind = PlaybackStreamKind.AUDIO,
            deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
            container = mime.substringBefore(';').substringAfter('/', ""),
            mimeType = mime.substringBefore(';'),
            codec = codecFromMimeType(mime),
            bitrate = format?.optInt("bitrate", 0) ?: 0,
            averageBitrate = format?.optInt("averageBitrate", 0) ?: 0,
            sampleRate = format?.optString("audioSampleRate")?.toIntOrNull() ?: 0,
            itag = format?.optInt("itag", -1) ?: -1,
            qualityLabel = format?.optString("audioQuality").orEmpty(),
            expiresAtMs = expiresAtFor(url),
            selected = selected
        )
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

    private fun buildPlayerBody(
        videoId: String,
        profile: ClientProfile,
        visitorData: String,
        playerPoToken: String?,
        signatureTimestamp: Int?
    ): JSONObject {
        val locale = LevyraContentLocales.forLanguage(userPreferences.languageCode())
        val client = JSONObject()
            .put("clientName", profile.clientName)
            .put("clientVersion", profile.clientVersion)
            .put("hl", locale.hl)
            .put("gl", locale.gl)
            .put("utcOffsetMinutes", 0)
            .put("timeZone", "UTC")
        visitorData.takeIf { it.isNotBlank() }?.let { client.put("visitorData", it) }
        if (profile.android) {
            val vr = profile.clientName == "ANDROID_VR"
            client.put("androidSdkVersion", if (vr) 32 else 35)
                .put("osName", "Android")
                .put("osVersion", if (vr) "12L" else "15")
                .put("platform", "MOBILE")
            if (vr) {
                client.put("deviceMake", "Oculus")
                    .put("deviceModel", "Quest 3")
            }
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
        val contentPlaybackContext = JSONObject().put("html5Preference", "HTML5_PREF_WANTS")
        signatureTimestamp?.let { contentPlaybackContext.put("signatureTimestamp", it) }
        return JSONObject()
            .put("context", JSONObject().put("client", client))
            .put("videoId", videoId)
            .put("contentCheckOk", true)
            .put("racyCheckOk", true)
            .put("playbackContext", JSONObject().put("contentPlaybackContext", contentPlaybackContext))
            .put("params", "CgIQBg")
            .put("watchEndpointMusicSupportedConfigs", JSONObject().put("watchEndpointMusicConfig", JSONObject().put("musicVideoType", "MUSIC_VIDEO_TYPE_ATV")))
            .apply {
                playerPoToken?.takeIf { it.isNotBlank() }?.let {
                    put("serviceIntegrityDimensions", JSONObject().put("poToken", it))
                }
            }
    }

    private fun scoreAudioFormat(
        mime: String,
        itag: Int,
        bitrate: Int,
        formatAudioQuality: String,
        preferMp4Audio: Boolean,
        requestedAudioQuality: String
    ): Int {
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
        val itagBias = if (requestedAudioQuality.equals("low", true)) {
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
            formatAudioQuality.contains("HIGH", true) -> 620_000
            formatAudioQuality.contains("MEDIUM", true) -> 420_000
            formatAudioQuality.contains("LOW", true) -> 120_000
            else -> 0
        }
        val bitrateBias = when (requestedAudioQuality.lowercase()) {
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

    private fun JSONObject.resolveFormatUrl(
        videoId: String,
        streamingPoToken: String?,
        transformThrottling: Boolean
    ): String {
        val direct = optString("url")
        val cipher = optString("signatureCipher").ifBlank { optString("cipher") }
        val initial = if (direct.isNotBlank()) {
            direct
        } else {
            val values = cipher.formValues()
            val base = values["url"].orEmpty()
            if (base.isBlank()) return ""
            val signatureParameter = values["sp"].takeUnless { it.isNullOrBlank() } ?: "signature"
            val signature = values["sig"] ?: values["signature"] ?: values["s"]?.let { obfuscated ->
                decodeSignature(videoId, obfuscated)
            }
            if (signature.isNullOrBlank()) return ""
            base.withQueryParameter(signatureParameter, signature)
        }
        return initial.finalizeStreamingUrl(videoId, streamingPoToken, transformThrottling)
    }

    private fun String.finalizeStreamingUrl(
        videoId: String,
        streamingPoToken: String?,
        transformThrottling: Boolean
    ): String {
        val transformed = if (transformThrottling && containsQueryParameter("n")) {
            decodeThrottlingParameter(videoId, this)
        } else {
            this
        }
        return streamingPoToken
            ?.takeIf { it.isNotBlank() }
            ?.let { transformed.withQueryParameterReplacing("pot", it) }
            ?: transformed
    }

    private fun decodeSignature(videoId: String, value: String): String {
        return runCatching { YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, value) }
            .recoverCatching {
                YoutubeJavaScriptPlayerManager.clearAllCaches()
                YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, value)
            }
            .getOrElse { "" }
    }

    private fun decodeThrottlingParameter(videoId: String, url: String): String {
        return runCatching { YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url) }
            .recoverCatching {
                YoutubeJavaScriptPlayerManager.clearThrottlingParametersCache()
                YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
            }
            .getOrElse { url }
    }

    private fun JSONObject.finiteFloat(key: String): Float? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        return value.takeIf { it.isFinite() }?.toFloat()
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

    private fun String.withQueryParameterReplacing(key: String, value: String): String {
        val pattern = Regex("([?&])${Regex.escape(key)}=[^&]*")
        val encoded = value.urlEncode()
        return if (pattern.containsMatchIn(this)) {
            replace(pattern) { match -> "${match.groupValues[1]}${key.urlEncode()}=$encoded" }
        } else {
            withQueryParameter(key, value)
        }
    }

    private fun String.containsQueryParameter(key: String): Boolean {
        return Regex("(?:[?&])${Regex.escape(key)}=").containsMatchIn(this)
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

private fun Int?.orZero(): Int = this ?: 0

private fun Throwable.playbackDiagnostic(): String {
    val messages = generateSequence(this) { it.cause }
        .mapNotNull { cause -> cause.message?.trim()?.takeIf { it.isNotBlank() } }
        .distinct()
        .toList()
    return messages.joinToString(" → ").ifBlank { this::class.java.simpleName.ifBlank { "errore sconosciuto" } }
}

class PlaybackBlockedException(message: String) : IllegalStateException(message)

private data class ClientProfile(
    val clientName: String,
    val clientVersion: String,
    val label: String,
    val userAgent: String,
    val android: Boolean,
    val delayMs: Long,
    val tier: Int,
    val requiresPoToken: Boolean
) {
    val clientHeaderName: String
        get() = when (clientName) {
            "ANDROID" -> "3"
            "ANDROID_MUSIC" -> "21"
            "ANDROID_VR" -> "28"
            "IOS" -> "5"
            "WEB_REMIX" -> "67"
            "WEB" -> "1"
            "WEB_EMBEDDED_PLAYER" -> "56"
            else -> "1"
        }
}

private data class ClientHealth(
    val successes: Int = 0,
    val failures: Int = 0,
    val consecutiveFailures: Int = 0,
    val averageLatencyMs: Long = Long.MAX_VALUE,
    val blockedUntilMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    val score: Double
        get() {
            val total = successes + failures
            val reliability = if (total <= 0) 0.5 else successes.toDouble() / total.toDouble()
            val latencyBonus = if (averageLatencyMs == Long.MAX_VALUE) 0.0 else 1_500.0 / averageLatencyMs.coerceAtLeast(50L).toDouble()
            return reliability * 100.0 + latencyBonus - consecutiveFailures * 12.0
        }
}

private data class PlaybackLoudness(
    val loudnessDb: Float? = null,
    val perceptualLoudnessDb: Float? = null
)

private data class DirectStream(
    val url: String,
    val videoUrl: String = "",
    val durationMs: Long,
    val thumbnailUrl: String,
    val source: String,
    val manifest: ResolvedPlaybackManifest,
    val loudnessDb: Float? = null,
    val perceptualLoudnessDb: Float? = null
)

private data class CachedStream(
    val track: Track,
    val expiresAt: Long
)

private data class CachedYoutubeEngagement(
    val likeCount: Long,
    val viewCount: Long,
    val expiresAtMs: Long
)
