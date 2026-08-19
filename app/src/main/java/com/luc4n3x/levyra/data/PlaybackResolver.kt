package com.luc4n3x.levyra.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.luc4n3x.levyra.domain.hasVideoPlaybackPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal const val EDITORIAL_ARTWORK_LOCK_TAG = "editorial-artwork-lock"

internal fun isMp4OfflineAudioCandidate(mimeOrFormat: String, url: String): Boolean {
    val format = mimeOrFormat.lowercase()
    val cleanUrl = url.lowercase()
    val path = cleanUrl.substringBefore('?').substringBefore('#')
    return format.contains("mpeg_4") ||
        format.contains("mpeg-4") ||
        format.contains("m4a") ||
        format.contains("mp4") ||
        cleanUrl.contains("mime=audio%2fmp4") ||
        cleanUrl.contains("mime=audio/mp4") ||
        path.endsWith(".m4a") ||
        path.endsWith(".mp4")
}

internal fun playbackStreamDiagnostics(url: String): String {
    if (url.isBlank()) return "empty"
    val query = url.substringAfter('?', "")
    fun parameter(name: String): String = query.split('&')
        .firstOrNull { it.startsWith("$name=") }
        ?.substringAfter('=')
        ?.takeIf { it.isNotEmpty() }
        ?: "-"
    val proofOfOrigin = if (parameter("pot") == "-") "no" else "yes"
    return "itag=${parameter("itag")} mime=${parameter("mime")} client=${parameter("c")} " +
        "ratebypass=${parameter("ratebypass")} pot=$proofOfOrigin expire=${parameter("expire")}"
}

internal fun isMuxedMp4ExportUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains("mime=video%2fmp4") || lower.contains("mime=video/mp4")
}

internal fun supportsOfflineExport(manifest: ResolvedPlaybackManifest): Boolean {
    if (manifest.selectedVideoUrl.isNotBlank()) return false
    if (!YoutubeStreamCapability.servesCompleteStream(manifest.selectedAudioUrl)) return false
    return manifest.supportsMp4AudioExport() || isMuxedMp4ExportUrl(manifest.selectedAudioUrl)
}

internal fun preserveEditorialArtwork(presented: Track, resolved: Track): Track {
    val artworkLocked = presented.source.equals("Levyra Editorial", ignoreCase = true) ||
        EDITORIAL_ARTWORK_LOCK_TAG in presented.moodTags
    if (!artworkLocked) return resolved
    val artwork = presented.largeThumbnailUrl.trim().ifBlank { presented.thumbnailUrl.trim() }
    if (artwork.isBlank()) return resolved
    return resolved.copy(
        thumbnailUrl = artwork,
        largeThumbnailUrl = artwork,
        moodTags = resolved.moodTags + EDITORIAL_ARTWORK_LOCK_TAG
    )
}

private const val YOUTUBE_NONCE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
private const val MAX_YOUTUBE_JSON_RESPONSE_BYTES = 8L * 1024L * 1024L
private val youtubeNonceRandom = SecureRandom()

private fun normalizedYoutubeCountryCode(countryCode: String): String {
    val normalized = countryCode.trim().uppercase()
    return normalized.takeIf { it.length == 2 && it.all { char -> char in 'A'..'Z' } } ?: "US"
}

internal fun visionOsUserAgent(countryCode: String): String {
    val country = normalizedYoutubeCountryCode(countryCode)
    return "com.google.visionos.youtube/1.02(RealityDevice14,1; U; CPU visionOS 25_6_0 like Mac OS X; $country)"
}

internal fun androidReelUserAgent(
    countryCode: String,
    clientVersion: String = PlaybackCompatibilityPolicy.DEFAULT_ANDROID_REEL_CLIENT_VERSION
): String {
    val country = normalizedYoutubeCountryCode(countryCode)
    return "com.google.android.youtube/$clientVersion (Linux; U; Android 15; $country) gzip"
}

internal fun applyVisionOsClientIdentity(client: JSONObject): JSONObject {
    return client
        .put("deviceMake", "Apple")
        .put("deviceModel", "RealityDevice14,1")
        .put("osName", "visionOS")
        .put("osVersion", "25.6.0.23O471")
        .put("platform", "MOBILE")
        .put("clientScreen", "WATCH")
}

internal fun buildAndroidReelContext(
    hl: String,
    gl: String,
    visitorData: String,
    clientVersion: String = PlaybackCompatibilityPolicy.DEFAULT_ANDROID_REEL_CLIENT_VERSION
): JSONObject {
    val client = JSONObject()
        .put("clientName", "ANDROID")
        .put("clientVersion", clientVersion)
        .put("clientScreen", "WATCH")
        .put("platform", "MOBILE")
        .put("osName", "Android")
        .put("osVersion", "16")
        .put("androidSdkVersion", 36)
        .put("hl", hl.trim().ifBlank { "en" })
        .put("gl", normalizedYoutubeCountryCode(gl))
        .put("utcOffsetMinutes", 0)
    visitorData.trim().takeIf { it.isNotBlank() }?.let { client.put("visitorData", it) }
    return JSONObject()
        .put("client", client)
        .put(
            "request",
            JSONObject()
                .put("internalExperimentFlags", JSONArray())
                .put("useSsl", true)
        )
        .put("user", JSONObject().put("lockedSafetyMode", false))
}

internal fun buildAndroidReelRequestBody(
    videoId: String,
    cpn: String,
    hl: String,
    gl: String,
    visitorData: String,
    clientVersion: String = PlaybackCompatibilityPolicy.DEFAULT_ANDROID_REEL_CLIENT_VERSION
): JSONObject {
    return JSONObject()
        .put("context", buildAndroidReelContext(hl, gl, visitorData, clientVersion))
        .put(
            "playerRequest",
            JSONObject()
                .put("videoId", videoId)
                .put("cpn", cpn)
                .put("contentCheckOk", true)
                .put("racyCheckOk", true)
        )
        .put("disablePlayerResponse", false)
}

internal fun androidReelFormats(response: JSONObject): JSONArray {
    return response
        .optJSONObject("playerResponse")
        ?.optJSONObject("streamingData")
        ?.optJSONArray("formats")
        ?: JSONArray()
}

internal fun generateYoutubeNonce(length: Int): String {
    require(length > 0)
    return buildString(length) {
        synchronized(youtubeNonceRandom) {
            repeat(length) {
                append(YOUTUBE_NONCE_ALPHABET[youtubeNonceRandom.nextInt(YOUTUBE_NONCE_ALPHABET.length)])
            }
        }
    }
}

internal fun readBoundedYoutubeJsonBody(
    body: ResponseBody,
    maxBytes: Long = MAX_YOUTUBE_JSON_RESPONSE_BYTES
): String {
    require(maxBytes in 1..Int.MAX_VALUE.toLong())
    val declaredLength = body.contentLength()
    if (declaredLength > maxBytes) {
        throw IOException("YouTube JSON response exceeds $maxBytes byte limit")
    }
    val output = ByteArrayOutputStream(
        declaredLength.takeIf { it in 1..maxBytes }?.toInt() ?: 8192
    )
    body.byteStream().use { input ->
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) {
                throw IOException("YouTube JSON response exceeds $maxBytes byte limit")
            }
            output.write(buffer, 0, read)
        }
    }
    return output.toByteArray().toString(StandardCharsets.UTF_8)
}

class PlaybackResolver private constructor(private val context: Context) {
    companion object {
        private const val YOUTUBE_VIDEO_ID_PATTERN = "[A-Za-z0-9_-]{11}"
        private const val LEVYRA_EXTRACTOR_PROVIDER = "LevyraExtractor"
        private const val LEVYRA_EXTRACTOR_HLS_PROVIDER = "LevyraExtractor HLS"
        private const val AUDIO_HEALTH_MODE = "audio"
        private const val VIDEO_HEALTH_MODE = "video"
        private const val MAX_STRATEGY_ORIGINS = 256
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
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val prefs = context.getSharedPreferences("levyra_stream_cache", Context.MODE_PRIVATE)
    private val clientHealthPrefs = context.getSharedPreferences("levyra_innertube_client_health", Context.MODE_PRIVATE)
    private val userPreferences = LevyraPreferences(context)
    private val streamCache = ConcurrentHashMap<String, CachedStream>()
    private val resolveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val singleFlight = PlaybackSingleFlight<String, Track>(resolveScope)
    private val clientHealth = ConcurrentHashMap<String, ClientHealth>()
    private val failedPlaybackUrls = ConcurrentHashMap<String, Long>()
    private val youtubeEngagementCache = ConcurrentHashMap<String, CachedYoutubeEngagement>()
    private val videoSelector = LevyraVideoStreamSelector(context)
    private val youtubeHttpClient = LevyraHttpClientFactory.youtubePlayer()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val playbackSecurity = YoutubePlaybackSecurity.getInstance(context)
    private val playbackPolicyStore = PlaybackCompatibilityPolicyStore(
        context,
        youtubeHttpClient,
        BuildConfig.VERSION_CODE
    )
    private val resilienceEngine = PlaybackResilienceEngine(context)
    private val strategyHealth = PlaybackStrategyHealthStore(context)
    private val strategyOriginByUrl = ConcurrentHashMap<String, PlaybackStrategyOrigin>()
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
        ClientProfile("VISIONOS", "1.02", "Apple Vision Pro", visionOsUserAgent("US"), false, 0L, 0, false),
        ClientProfile("ANDROID_VR", "1.65.10", "Android VR", "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; Quest 3 Build/SQ3A.220605.009.A1) gzip", true, 0L, 1, true),
        ClientProfile("ANDROID_MUSIC", "8.10.52", "Android Music", "Mozilla/5.0 (Linux; Android 15; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) com.google.android.apps.youtube.music/8.10.52", true, 0L, 2, false),
        ClientProfile("ANDROID", "19.44.38", "Android", "com.google.android.youtube/19.44.38 (Linux; U; Android 15)", true, 0L, 3, false),
        ClientProfile("IOS", "20.10.4", "iOS", "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3 like Mac OS X; it_IT)", false, 0L, 4, false),
        ClientProfile("WEB_REMIX", "1.20260423.01.00", "YouTube Music Web", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36", false, 0L, 5, true),
        ClientProfile("WEB", "2.20260630.01.00", "YouTube Web", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36", false, 0L, 6, true),
        ClientProfile("WEB_EMBEDDED_PLAYER", "1.20260423.01.00", "Embedded Player", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36", false, 0L, 7, false)
    )

    init {
        restoreCache()
        restoreClientHealth()
        refreshPlaybackPolicyInBackground(force = true, reason = "startup")
    }

    fun setAudioQuality(value: String) {
        selectedAudioQuality = normalizeAudioQuality(value)
    }

    private fun refreshPlaybackPolicyInBackground(force: Boolean, reason: String) {
        if (!force && !playbackPolicyStore.needsRefresh()) return
        resolveScope.launch {
            val changed = runCatchingPreservingCancellation {
                playbackPolicyStore.refresh(force = force, reason = reason)
            }.onFailure { error ->
                Timber.w(error, "Playback compatibility policy refresh failed reason=%s", reason)
            }.getOrDefault(false)
            if (changed) clearResolvedStreamCaches()
        }
    }

    private suspend fun clearResolvedStreamCaches() {
        streamCache.clear()
        strategyOriginByUrl.clear()
        prefs.edit().clear().apply()
        sourceMatchStore.clearOnline()
    }

    private fun normalizeAudioQuality(value: String): String {
        return when (value.lowercase()) {
            "high" -> "High"
            "low" -> "Low"
            else -> "Auto"
        }
    }

    fun warmNetwork() {
        if (!hasInternetCapableNetwork()) return
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
                .header("User-Agent", effectiveProfiles().firstOrNull()?.userAgent ?: profiles.first().userAgent)
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

    fun warmPlaybackSecurity() {
        if (!hasInternetCapableNetwork()) return
        playbackSecurity.warmUp()
    }

    suspend fun enrichYoutubeEngagement(track: Track): Track {
        if (track.source.equals("Offline", ignoreCase = true)) return track
        if (track.youtubeLikeCount >= 0L && track.youtubeViewCount >= 0L) return track
        val videoId = PlaybackSourceIdentity.sourceVideoId(track)
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
            runCatchingPreservingCancellation {
                NewPipeRuntime.ensure()
                StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
            }.fold(
                onSuccess = { info ->
                    cacheYoutubeEngagement(videoId, info.likeCount, info.viewCount)
                    track.withYoutubeEngagement(info.likeCount, info.viewCount)
                },
                onFailure = { error ->
                    Timber.d(error, "youtube engagement unavailable for %s", videoId)
                    cacheYoutubeEngagement(videoId, -1L, -1L)
                    track
                }
            )
        }
    }

    fun cached(track: Track, isVideoMode: Boolean = false): Track? {
        return cached(track, isVideoMode, selectedAudioQuality)
    }

    private fun cached(track: Track, isVideoMode: Boolean, audioQuality: String): Track? {
        if (track.streamUrl.isNotBlank()) {
            if (isPlaybackUrlBlocked(track.streamUrl) || track.videoStreamUrl.isNotBlank() && isPlaybackUrlBlocked(track.videoStreamUrl)) return null
            if (!isVideoMode && !isPlayableAudioUrl(track.streamUrl)) return null
            if (isVideoMode && !track.hasVideoPlaybackPayload()) return null
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
        if (isVideoMode && !hit.track.hasVideoPlaybackPayload()) {
            remove(key)
            return null
        }
        return hit.track
    }

    fun invalidate(track: Track, isVideoMode: Boolean = false, offlineExport: Boolean = false) {
        if (offlineExport) return
        remove(cacheKey(track, isVideoMode))
    }

    fun reportPlaybackFailure(
        track: Track,
        isVideoMode: Boolean,
        reason: String,
        isOfflineExport: Boolean = false,
        audioQuality: String? = null
    ) {
        if (isLocalPlaybackTrack(track)) {
            resilienceEngine.recordPlayerFailure(track.id, isVideoMode, reason)
            return
        }
        if (!hasValidatedInternet() && isNetworkFailureReason(reason)) {
            clearTransientClientPenalties()
            resilienceEngine.recordPlayerFailure(track.id, isVideoMode, reason)
            return
        }
        Timber.w(
            "playback failure source=%s mode=%s reason=%s stream=%s",
            track.source,
            if (isVideoMode) "video" else if (isOfflineExport) "offline" else "audio",
            reason.take(120),
            playbackStreamDiagnostics(track.streamUrl)
        )
        invalidate(track, isVideoMode, isOfflineExport)
        resilienceEngine.recordPlayerFailure(track.id, isVideoMode, reason)
        if (!isOfflineExport) {
            strategyOriginFor(track, isVideoMode)?.let { origin ->
                strategyHealth.recordRuntimeFailure(
                    mode = origin.mode,
                    strategy = origin.strategy,
                    kind = classifyPlaybackFailureReason(reason)
                )
            }
        }
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
            resolveScope.launch {
                val policyChanged = runCatchingPreservingCancellation {
                    playbackPolicyStore.refreshAfterRejection()
                }.onFailure { error ->
                    Timber.w(error, "Playback compatibility policy rejection refresh failed")
                }.getOrDefault(false)
                if (policyChanged) clearResolvedStreamCaches()
                runCatchingPreservingCancellation {
                    playbackSecurity.rotateIfNeeded(PlaybackBlockedException(reason))
                }.onFailure { error ->
                    Timber.w(error, "YouTube playback security refresh failed")
                }
            }
        }
        if (recovery.rotateCodec && !isOfflineExport) {
            videoSelector.reportPlaybackFailure(track.videoStreamUrl.ifBlank { track.streamUrl }, lower)
        }
        val sourceMatchQuarantineMs = when {
            lower.contains("403") || lower.contains("410") || lower.contains("expired") || lower.contains("scadut") || lower.contains("signature") -> 0L
            lower.contains("decoder") || lower.contains("codec") -> recovery.quarantineMs
            else -> minOf(recovery.quarantineMs, 20_000L)
        }
        sourceMatchScope.launch {
            runCatchingPreservingCancellation {
                sourceMatchStore.recordFailure(
                    track = track,
                    videoMode = isVideoMode,
                    audioQuality = audioQuality?.let(::normalizeAudioQuality) ?: selectedAudioQuality,
                    quarantineMs = sourceMatchQuarantineMs,
                    preferMp4Audio = isOfflineExport
                )
            }.onFailure { error ->
                Timber.w(error, "persistent source match failure update failed")
            }
        }
    }

    private fun rememberStrategyOrigin(track: Track, mode: String, strategy: String) {
        if (strategyOriginByUrl.size >= MAX_STRATEGY_ORIGINS) strategyOriginByUrl.clear()
        val origin = PlaybackStrategyOrigin(mode, strategy)
        listOf(track.streamUrl, track.videoStreamUrl)
            .filter { it.isNotBlank() }
            .forEach { strategyOriginByUrl[strategyOriginKey(mode, it)] = origin }
    }

    private fun strategyOriginFor(track: Track, isVideoMode: Boolean): PlaybackStrategyOrigin? {
        val expectedMode = if (isVideoMode) VIDEO_HEALTH_MODE else AUDIO_HEALTH_MODE
        return listOf(track.videoStreamUrl, track.streamUrl)
            .asSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { strategyOriginByUrl[strategyOriginKey(expectedMode, it)] }
            .firstOrNull()
    }

    private fun strategyOriginKey(mode: String, url: String): String = "$mode\u0000$url"

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
        val diagnostics = JSONObject(resilienceEngine.diagnostics(health))
        val strategyHealthJson = JSONObject()
        strategyHealth.snapshot().forEach { (key, value) -> strategyHealthJson.put(key, value) }
        diagnostics.put("strategyHealth", strategyHealthJson)
        return diagnostics.toString(2)
    }

    private fun isLocalPlaybackTrack(track: Track): Boolean =
        track.source.equals("Offline", ignoreCase = true) || isLocalPlaybackUri(track.streamUrl)

    private fun isLocalPlaybackUri(value: String): Boolean {
        val clean = value.trim()
        return clean.startsWith("content://", ignoreCase = true) ||
            clean.startsWith("file://", ignoreCase = true)
    }

    private fun hasValidatedInternet(): Boolean {
        val manager = connectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun hasInternetCapableNetwork(): Boolean {
        val manager = connectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isNetworkFailureReason(reason: String): Boolean {
        val value = reason.lowercase()
        return value.contains("network") ||
            value.contains("socket") ||
            value.contains("dns") ||
            value.contains("connection") ||
            value.contains("host") ||
            value.contains("rete")
    }

    private fun clearTransientClientPenalties() {
        val now = System.currentTimeMillis()
        clientHealth.replaceAll { name, health ->
            if (health.consecutiveFailures == 0 && health.blockedUntilMs == 0L) {
                health
            } else {
                health.copy(
                    consecutiveFailures = 0,
                    blockedUntilMs = 0L,
                    updatedAtMs = now
                ).also { persistClientHealth(name, it) }
            }
        }
    }

    private fun isPlaybackUrlBlocked(url: String): Boolean {
        if (url.isBlank()) return true
        val until = failedPlaybackUrls[url] ?: return false
        if (until > System.currentTimeMillis()) return true
        failedPlaybackUrls.remove(url, until)
        return false
    }

    suspend fun resolve(track: Track, isVideoMode: Boolean = false): Track {
        val resolved = resolveInternal(
            track = track,
            isVideoMode = isVideoMode,
            timeoutMs = playbackResolveTimeoutMs,
            preferMp4Audio = false,
            requestKind = "playback",
            audioQuality = selectedAudioQuality,
            reuseProvidedStream = true
        )
        return preserveEditorialArtwork(track, resolved)
    }

    suspend fun resolveForOffline(track: Track, audioQualityOverride: String? = null): Track {
        val quality = normalizeAudioQuality(audioQualityOverride ?: selectedAudioQuality)
        val reel = runCatchingPreservingCancellation {
            resolveVideoWithAndroidReel(track.copy(streamUrl = "", videoStreamUrl = ""))
        }.onFailure { error ->
            Timber.d(error, "Offline Android Reel primary unavailable")
        }.getOrNull()
        val reelManifest = reel?.playbackManifest
        if (reel != null && reelManifest != null && supportsOfflineExport(reelManifest)) {
            return preserveEditorialArtwork(track, reel)
        }
        val resolved = resolveInternal(
            track = track,
            isVideoMode = false,
            timeoutMs = offlineResolveTimeoutMs,
            preferMp4Audio = true,
            requestKind = "offline",
            audioQuality = quality,
            reuseProvidedStream = audioQualityOverride == null
        )
        return preserveEditorialArtwork(track, resolved)
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
        if (requestKind == "playback") {
            refreshPlaybackPolicyInBackground(force = false, reason = "resolve")
        }
        if (isLocalPlaybackTrack(track)) {
            if (!isLocalPlaybackUri(track.streamUrl)) {
                throw PlaybackBlockedException("File offline non disponibile per ${track.title}")
            }
            return@coroutineScope track.copy(videoStreamUrl = "")
        }
        track.streamUrl.takeIf {
            reuseProvidedStream &&
            it.isNotBlank() &&
                !isPlaybackUrlBlocked(it) &&
                (track.videoStreamUrl.isBlank() || !isPlaybackUrlBlocked(track.videoStreamUrl)) &&
                streamStillFresh(it) &&
                (if (isVideoMode) track.hasVideoPlaybackPayload() else isPlayableAudioUrl(it)) &&
                (
                    !preferMp4Audio ||
                        track.playbackManifest?.let(::supportsOfflineExport) == true ||
                        (isMp4AudioUrl(it) && YoutubeStreamCapability.servesCompleteStream(it))
                    )
        }?.let { return@coroutineScope track }
        if (!preferMp4Audio) {
            val cacheLookupTrack = if (reuseProvidedStream) track else track.copy(streamUrl = "", videoStreamUrl = "")
            cached(cacheLookupTrack, isVideoMode, audioQuality)?.let { return@coroutineScope it }
        }

        if (!hasInternetCapableNetwork()) {
            clearTransientClientPenalties()
            val offlineTrack = track.copy(streamUrl = "", videoStreamUrl = "")
            val restored = withContext(Dispatchers.IO) {
                restorePersistentSource(
                    track = offlineTrack,
                    isVideoMode = isVideoMode,
                    preferMp4Audio = preferMp4Audio,
                    audioQuality = audioQuality,
                    errors = mutableListOf(),
                    allowNetworkRefresh = false
                )?.also { store(offlineTrack, it, isVideoMode, audioQuality, preferMp4Audio) }
            }
            restored?.let { return@coroutineScope it }
            throw PlaybackBlockedException("Connessione Internet non disponibile")
        }

        val key = "${cacheKey(track, isVideoMode, audioQuality)}_$requestKind"
        Timber.d("resolver start kind=%s mode=%s id=%s quality=%s", requestKind, if (isVideoMode) "video" else "audio", track.id, audioQuality)
        return@coroutineScope singleFlight.run(key) {
            try {
                withTimeout(timeoutMs) {
                    resolveUncached(track.copy(streamUrl = "", videoStreamUrl = ""), isVideoMode, preferMp4Audio, audioQuality)
                }
            } catch (error: TimeoutCancellationException) {
                val label = if (requestKind == "offline") "Download" else "YouTube"
                throw PlaybackBlockedException("$label lento: sto aspettando lo stream più del previsto, riprova tra qualche secondo")
            }
        }
    }

    suspend fun prefetch(track: Track, isVideoMode: Boolean = false): Track? {
        if (isLocalPlaybackTrack(track)) return track.takeIf { isLocalPlaybackUri(it.streamUrl) }
        if (track.streamUrl.isNotBlank()) {
            if (!isVideoMode && !isPlayableAudioUrl(track.streamUrl)) return null
            if (isVideoMode && !track.hasVideoPlaybackPayload()) return null
            if (streamStillFresh(track.streamUrl)) {
                store(track, track, isVideoMode)
                return track
            }
            return null
        }
        cached(track, isVideoMode)?.let { return it }
        if (!hasInternetCapableNetwork()) return null
        return runCatchingPreservingCancellation { resolve(track, isVideoMode) }.getOrNull()
    }

    private suspend fun resolveUncached(
        track: Track,
        isVideoMode: Boolean = false,
        preferMp4Audio: Boolean = false,
        audioQuality: String = selectedAudioQuality
    ): Track = withContext(Dispatchers.IO) {
        val errors = Collections.synchronizedList(mutableListOf<String>())

        if (!preferMp4Audio && !isVideoMode) {
            val resolved = resolveAudioByCompatibilityPolicy(track, audioQuality, errors)
            if (resolved != null) return@withContext resolved

            val reason = errors.firstOrNull { it.startsWith("LevyraExtractor:") }
                ?: errors.firstOrNull {
                    it.contains("age", true) ||
                        it.contains("anonymous", true) ||
                        it.contains("login", true) ||
                        it.contains("accedi", true)
                }
                ?: errors.firstOrNull()
                ?: "Stream non disponibile"
            Timber.w(
                "audio resolve failed offlineExport=false policyRevision=%d errors=%s",
                playbackPolicyStore.current().revision,
                errors.joinToString(" | ").take(900)
            )
            throw PlaybackBlockedException(reason)
        }

        if (preferMp4Audio) {
            restorePersistentSource(track, isVideoMode, preferMp4Audio, audioQuality, errors)?.let { restored ->
                store(track, restored, isVideoMode, audioQuality, preferMp4Audio)
                return@withContext restored
            }
        }

        if (isVideoMode) {
            val resolved = resolveVideoByCompatibilityPolicy(track, audioQuality, errors)
            if (resolved != null) return@withContext resolved

            val reason = errors.firstOrNull { it.contains("age", true) || it.contains("login", true) }
                ?: errors.firstOrNull()
                ?: "Video non disponibile"
            throw PlaybackBlockedException(reason)
        }

        val resolved = resolveAudioFast(track, errors, preferMp4Audio, audioQuality)
        if (resolved != null) {
            store(track, resolved, isVideoMode, audioQuality, preferMp4Audio)
            persistResolvedSource(track, resolved, isVideoMode, audioQuality, 90, preferMp4Audio)
            return@withContext resolved
        }

        val alternate = resolveAudioWithSearchFallback(track, errors, preferMp4Audio, audioQuality)
        if (alternate != null) {
            store(track, alternate, isVideoMode, audioQuality, preferMp4Audio)
            persistResolvedSource(track, alternate, isVideoMode, audioQuality, 84, preferMp4Audio)
            return@withContext alternate
        }

        val reason = errors.firstOrNull { it.startsWith("LevyraExtractor:") }
            ?: errors.firstOrNull { it.contains("age", true) || it.contains("anonymous", true) || it.contains("login", true) || it.contains("accedi", true) }
            ?: errors.firstOrNull()
            ?: "Stream non disponibile"
        Timber.w(
            "audio resolve failed offlineExport=%s errors=%s",
            preferMp4Audio,
            errors.joinToString(" | ").take(900)
        )
        throw PlaybackBlockedException(reason)
    }

    private suspend fun resolveAudioByCompatibilityPolicy(
        track: Track,
        audioQuality: String,
        errors: MutableList<String>
    ): Track? {
        val policy = playbackPolicyStore.current()
        for (strategy in strategyHealth.order(AUDIO_HEALTH_MODE, policy.audioStrategies)) {
            currentCoroutineContext().ensureActive()
            val startedAt = System.currentTimeMillis()
            val errorsBefore = errors.size
            val resolved = when (strategy) {
                PlaybackAudioStrategy.REEL_MUXED -> runCatchingPreservingCancellation {
                    resolveVideoWithAndroidReel(track)
                }.onFailure { error ->
                    errors += "Android Reel muxed: ${error.playbackDiagnostic()}"
                }.getOrNull()

                PlaybackAudioStrategy.REEL_AUDIO -> runCatchingPreservingCancellation {
                    resolveAudioWithAndroidReel(track, audioQuality)
                }.onFailure { error ->
                    errors += "Android Reel audio: ${error.playbackDiagnostic()}"
                }.getOrNull()

                PlaybackAudioStrategy.PERSISTED -> restorePersistentSource(
                    track = track,
                    isVideoMode = false,
                    preferMp4Audio = false,
                    audioQuality = audioQuality,
                    errors = errors
                )

                PlaybackAudioStrategy.DIRECT -> resolveAudioFast(
                    track = track,
                    errors = errors,
                    preferMp4Audio = false,
                    audioQuality = audioQuality
                )

                PlaybackAudioStrategy.SEARCH -> resolveAudioWithSearchFallback(
                    track = track,
                    errors = errors,
                    preferMp4Audio = false,
                    audioQuality = audioQuality
                )
            }
            val elapsedMs = System.currentTimeMillis() - startedAt
            if (resolved != null) {
                strategyHealth.recordSuccess(AUDIO_HEALTH_MODE, strategy.name, elapsedMs)
                rememberStrategyOrigin(resolved, AUDIO_HEALTH_MODE, strategy.name)
                store(track, resolved, false, audioQuality, false)
                val confidence = when (strategy) {
                    PlaybackAudioStrategy.REEL_MUXED,
                    PlaybackAudioStrategy.REEL_AUDIO -> 96
                    PlaybackAudioStrategy.DIRECT -> 90
                    PlaybackAudioStrategy.SEARCH -> 84
                    PlaybackAudioStrategy.PERSISTED -> null
                }
                confidence?.let {
                    persistResolvedSource(track, resolved, false, audioQuality, it, false)
                }
                return resolved
            }
            val failureReason = synchronized(errors) { errors.toList() }
        .drop(errorsBefore)
        .lastOrNull()
            strategyHealth.recordFailure(
                AUDIO_HEALTH_MODE,
                strategy.name,
                elapsedMs,
                failureReason?.let(::classifyPlaybackFailureReason) ?: PlaybackFailureKind.Unknown
            )
        }
        return null
    }

    private suspend fun resolveVideoByCompatibilityPolicy(
        track: Track,
        audioQuality: String,
        errors: MutableList<String>
    ): Track? {
        val policy = playbackPolicyStore.current()
        for (strategy in strategyHealth.order(VIDEO_HEALTH_MODE, policy.videoStrategies)) {
            currentCoroutineContext().ensureActive()
            val startedAt = System.currentTimeMillis()
            val errorsBefore = errors.size
            val resolved = when (strategy) {
                PlaybackVideoStrategy.PERSISTED -> restorePersistentSource(
                    track = track,
                    isVideoMode = true,
                    preferMp4Audio = false,
                    audioQuality = audioQuality,
                    errors = errors
                )

                PlaybackVideoStrategy.STANDARD -> resolveStandardVideo(track, audioQuality, errors)
                PlaybackVideoStrategy.REEL -> runCatchingPreservingCancellation {
                    resolveVideoWithAndroidReel(track)
                }.onFailure { error ->
                    errors += "Android Reel: ${error.playbackDiagnostic()}"
                }.getOrNull()
            }
            val elapsedMs = System.currentTimeMillis() - startedAt
            if (resolved != null) {
                strategyHealth.recordSuccess(VIDEO_HEALTH_MODE, strategy.name, elapsedMs)
                rememberStrategyOrigin(resolved, VIDEO_HEALTH_MODE, strategy.name)
                store(track, resolved, true, audioQuality)
                val confidence = when (strategy) {
                    PlaybackVideoStrategy.PERSISTED -> null
                    PlaybackVideoStrategy.STANDARD -> 92
                    PlaybackVideoStrategy.REEL -> 78
                }
                confidence?.let {
                    persistResolvedSource(
                        original = track,
                        resolved = resolved,
                        isVideoMode = true,
                        audioQuality = audioQuality,
                        confidence = it,
                        preferMp4Audio = false
                    )
                }
                return resolved
            }
            val failureReason = synchronized(errors) { errors.toList() }
        .drop(errorsBefore)
        .lastOrNull()
            strategyHealth.recordFailure(
                VIDEO_HEALTH_MODE,
                strategy.name,
                elapsedMs,
                failureReason?.let(::classifyPlaybackFailureReason) ?: PlaybackFailureKind.Unknown
            )
        }
        return null
    }

    private suspend fun resolveStandardVideo(
        track: Track,
        audioQuality: String,
        errors: MutableList<String>
    ): Track? = coroutineScope {
        val winner = CompletableDeferred<Track?>()
        val extractorJob = launch {
            delay(LevyraResolverLatency.extractorHedgeDelayMs(isVideoMode = true, preferMp4Audio = false))
            if (winner.isCompleted) return@launch
            val result = runCatchingPreservingCancellation {
                resolveVideoWithLevyraExtractor(track, audioQuality)
            }
            result.onSuccess { winner.complete(it) }
                .onFailure { error ->
                    errors += "LevyraExtractor video: ${error.playbackDiagnostic()}"
                }
        }
        val innerTubeJob = launch {
            delay(LevyraResolverLatency.innerTubeFallbackDelayMs(isVideoMode = true, preferMp4Audio = false))
            if (winner.isCompleted) return@launch
            val stream = runCatchingPreservingCancellation {
                hedgedInnerTube(track, errors, true, audioQuality)
            }.getOrNull()
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

    private suspend fun resolveAudioWithAndroidReel(
        track: Track,
        audioQuality: String
    ): Track {
        val sourceVideoId = PlaybackSourceIdentity.sourceVideoId(track)
            .takeIf(youtubeVideoIdRegex::matches)
            ?: throw YoutubePlayerRequestException(null, "Identità video YouTube assente o non valida")
        val locale = LevyraContentLocales.forLanguage(userPreferences.languageCode())
        val reelClientVersion = playbackPolicyStore.current().androidReelClientVersion
        val userAgent = androidReelUserAgent(locale.gl, reelClientVersion)
        val cachedVisitorData = playbackSecurity.cachedSession().visitorData
        val visitorData = runCatchingPreservingCancellation {
            fetchAndroidReelVisitorData(locale.hl, locale.gl, userAgent, reelClientVersion)
        }.getOrNull().orEmpty().ifBlank { cachedVisitorData }
        currentCoroutineContext().ensureActive()

        val cpn = generateYoutubeNonce(16)
        val tParameter = generateYoutubeNonce(12)
        val body = buildAndroidReelRequestBody(
            videoId = sourceVideoId,
            cpn = cpn,
            hl = locale.hl,
            gl = locale.gl,
            visitorData = visitorData,
            clientVersion = reelClientVersion
        ).toString()
        val endpoint = "https://youtubei.googleapis.com/youtubei/v1/reel/reel_item_watch" +
            "?prettyPrint=false&t=$tParameter&id=$sourceVideoId&\$fields=playerResponse"
        val request = Request.Builder()
            .url(endpoint)
            .post(body.toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
            .header("X-Goog-Api-Format-Version", "2")
            .build()
        val root = executeYoutubeJson(request, "Android Reel audio")
        currentCoroutineContext().ensureActive()
        val playerResponse = root.optJSONObject("playerResponse")
            ?: throw YoutubePlayerRequestException(null, "Android Reel non ha restituito playerResponse")
        val playability = playerResponse.optJSONObject("playabilityStatus")
        val status = playability?.optString("status").orEmpty()
        if (status.isNotBlank() && status != "OK") {
            val reason = playability?.optString("reason").orEmpty()
            throw YoutubePlayerRequestException(null, reason.ifBlank { status })
        }

        val streamingData = playerResponse.optJSONObject("streamingData")
            ?: throw YoutubePlayerRequestException(null, "Android Reel non ha restituito streamingData")
        val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()
        val audioCandidates = buildList {
            for (i in 0 until adaptiveFormats.length()) {
                val format = adaptiveFormats.optJSONObject(i) ?: continue
                val mime = format.optString("mimeType")
                if (!mime.startsWith("audio/", true)) continue
                val url = format.resolveFormatUrl(
                    videoId = sourceVideoId,
                    streamingPoToken = null,
                    transformThrottling = true
                ).takeIf { it.isNotBlank() }
                    ?.withQueryParameterReplacing("cpn", cpn)
                    .orEmpty()
                if (url.isBlank() || isPlaybackUrlBlocked(url) || !streamStillFresh(url)) continue
                add(
                    Triple(
                        format,
                        url,
                        scoreAudioFormat(
                            mime = mime,
                            itag = format.optInt("itag", 0),
                            bitrate = format.optInt("bitrate", 0),
                            formatAudioQuality = format.optString("audioQuality"),
                            preferMp4Audio = false,
                            requestedAudioQuality = audioQuality
                        )
                    )
                )
            }
        }.sortedByDescending { it.third }

        var selectedAudio: Triple<JSONObject, String, Int>? = null
        for (candidate in audioCandidates) {
            if (verifyDirectAudioUrlFast(candidate.second)) {
                selectedAudio = candidate
                break
            }
        }
        if (selectedAudio != null) {
            val (format, url, _) = selectedAudio
            val details = playerResponse.optJSONObject("videoDetails")
            val duration = details?.optString("lengthSeconds")?.toLongOrNull()?.times(1000L)
                ?.takeIf { it > 0L }
                ?: track.durationMs
            val thumbnail = details
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?.bestThumbnail()
                .orEmpty()
                .ifBlank { track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } }
            val manifest = buildManifest(
                sourceVideoId = sourceVideoId,
                provider = "YouTube Android Reel Audio",
                durationMs = duration,
                selectedAudioUrl = url,
                selectedVideoUrl = "",
                streams = listOf(innerTubeAudioDescriptor(format, url, true))
            )
            return track.withDirectStream(
                DirectStream(
                    url = url,
                    videoUrl = "",
                    durationMs = duration,
                    thumbnailUrl = thumbnail,
                    source = "YouTube Android Reel Audio",
                    manifest = manifest
                )
            )
        }

        val formats = androidReelFormats(root)
        val muxedCandidates = buildList {
            for (i in 0 until formats.length()) {
                val format = formats.optJSONObject(i) ?: continue
                val mime = format.optString("mimeType")
                if (!mime.startsWith("video/", true)) continue
                val url = format.resolveFormatUrl(
                    videoId = sourceVideoId,
                    streamingPoToken = null,
                    transformThrottling = true
                ).takeIf { it.isNotBlank() }
                    ?.withQueryParameterReplacing("cpn", cpn)
                    .orEmpty()
                if (url.isBlank() || isPlaybackUrlBlocked(url) || !streamStillFresh(url)) continue
                add(
                    Triple(
                        format,
                        url,
                        if (format.optInt("itag", 0) == 18) Int.MIN_VALUE else format.optInt("bitrate", Int.MAX_VALUE)
                    )
                )
            }
        }.sortedBy { it.third }
        var selectedMuxed: Triple<JSONObject, String, Int>? = null
        for (candidate in muxedCandidates) {
            if (verifyDirectAudioUrlFast(candidate.second)) {
                selectedMuxed = candidate
                break
            }
        }
        val muxed = selectedMuxed
            ?: throw YoutubePlayerRequestException(null, "Android Reel non ha restituito uno stream audio riproducibile")
        val (muxedFormat, muxedUrl, _) = muxed
        val details = playerResponse.optJSONObject("videoDetails")
        val duration = details?.optString("lengthSeconds")?.toLongOrNull()?.times(1000L)
            ?.takeIf { it > 0L }
            ?: track.durationMs
        val thumbnail = details
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.bestThumbnail()
            .orEmpty()
            .ifBlank { track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } }
        val muxedCandidate = LevyraVideoCandidate(
            url = muxedUrl,
            mimeType = muxedFormat.optString("mimeType").substringBefore(';'),
            codec = codecFromMimeType(muxedFormat.optString("mimeType")),
            width = muxedFormat.optInt("width", 0),
            height = muxedFormat.optInt("height", 0),
            fps = muxedFormat.optInt("fps", 0),
            bitrate = muxedFormat.optInt("bitrate", 0),
            itag = muxedFormat.optInt("itag", 0),
            muxed = true,
            label = "android-reel-audio-fallback"
        )
        val manifest = buildManifest(
            sourceVideoId = sourceVideoId,
            provider = "YouTube Android Reel Audio",
            durationMs = duration,
            selectedAudioUrl = muxedUrl,
            selectedVideoUrl = "",
            streams = listOf(videoDescriptor(muxedCandidate, true))
        )
        return track.withDirectStream(
            DirectStream(
                url = muxedUrl,
                videoUrl = "",
                durationMs = duration,
                thumbnailUrl = thumbnail,
                source = "YouTube Android Reel Audio · muxed fallback",
                manifest = manifest
            )
        )
    }

    private suspend fun resolveVideoWithAndroidReel(track: Track): Track {
        val sourceVideoId = PlaybackSourceIdentity.sourceVideoId(track)
            .takeIf(youtubeVideoIdRegex::matches)
            ?: throw YoutubePlayerRequestException(null, "Identità video YouTube assente o non valida")
        val locale = LevyraContentLocales.forLanguage(userPreferences.languageCode())
        val reelClientVersion = playbackPolicyStore.current().androidReelClientVersion
        val userAgent = androidReelUserAgent(locale.gl, reelClientVersion)
        val cachedVisitorData = playbackSecurity.cachedSession().visitorData
        val visitorData = runCatchingPreservingCancellation {
            fetchAndroidReelVisitorData(locale.hl, locale.gl, userAgent, reelClientVersion)
        }.getOrNull().orEmpty().ifBlank { cachedVisitorData }
        currentCoroutineContext().ensureActive()

        val cpn = generateYoutubeNonce(16)
        val tParameter = generateYoutubeNonce(12)
        val body = buildAndroidReelRequestBody(
            videoId = sourceVideoId,
            cpn = cpn,
            hl = locale.hl,
            gl = locale.gl,
            visitorData = visitorData,
            clientVersion = reelClientVersion
        ).toString()
        val endpoint = "https://youtubei.googleapis.com/youtubei/v1/reel/reel_item_watch" +
            "?prettyPrint=false&t=$tParameter&id=$sourceVideoId&\$fields=playerResponse"
        val request = Request.Builder()
            .url(endpoint)
            .post(body.toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
            .header("X-Goog-Api-Format-Version", "2")
            .build()
        val root = executeYoutubeJson(request, "Android Reel")
        currentCoroutineContext().ensureActive()
        val playerResponse = root.optJSONObject("playerResponse")
            ?: throw YoutubePlayerRequestException(null, "Android Reel non ha restituito playerResponse")
        val playability = playerResponse.optJSONObject("playabilityStatus")
        val status = playability?.optString("status").orEmpty()
        if (status.isNotBlank() && status != "OK") {
            val reason = playability?.optString("reason").orEmpty()
            throw YoutubePlayerRequestException(null, reason.ifBlank { status })
        }
        val formats = androidReelFormats(root)
        if (formats.length() == 0) {
            throw YoutubePlayerRequestException(null, "Android Reel non ha restituito formati muxed")
        }
        val muxedCandidates = buildList {
            for (i in 0 until formats.length()) {
                val format = formats.optJSONObject(i) ?: continue
                val mime = format.optString("mimeType")
                if (!mime.startsWith("video/", true)) continue
                val url = format.resolveFormatUrl(
                    videoId = sourceVideoId,
                    streamingPoToken = null,
                    transformThrottling = true
                ).takeIf { it.isNotBlank() }
                    ?.withQueryParameterReplacing("cpn", cpn)
                    .orEmpty()
                if (url.isBlank() || isPlaybackUrlBlocked(url) || !streamStillFresh(url)) continue
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
                        label = "android-reel"
                    )
                )
            }
        }
        val selection = videoSelector.select(
            muxedCandidates = muxedCandidates,
            videoOnlyCandidates = emptyList(),
            hasSeparateAudio = false,
            blocked = ::isPlaybackUrlBlocked
        ) ?: throw YoutubePlayerRequestException(null, "Android Reel non ha restituito uno stream video compatibile")

        val details = playerResponse.optJSONObject("videoDetails")
        val duration = details?.optString("lengthSeconds")?.toLongOrNull()?.times(1000L)
            ?.takeIf { it > 0L }
            ?: track.durationMs
        val thumbnail = details
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.bestThumbnail()
            .orEmpty()
            .ifBlank { track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } }
        val manifest = buildManifest(
            sourceVideoId = sourceVideoId,
            provider = "YouTube Android Reel",
            durationMs = duration,
            selectedAudioUrl = selection.candidate.url,
            selectedVideoUrl = "",
            streams = listOf(videoDescriptor(selection.candidate, true))
        )
        return track.withDirectStream(
            DirectStream(
                url = selection.candidate.url,
                videoUrl = "",
                durationMs = duration,
                thumbnailUrl = thumbnail,
                source = "YouTube Android Reel · ${selection.reason}",
                manifest = manifest
            )
        )
    }

    private fun fetchAndroidReelVisitorData(
        hl: String,
        gl: String,
        userAgent: String,
        clientVersion: String
    ): String {
        val body = JSONObject()
            .put("context", buildAndroidReelContext(hl, gl, "", clientVersion))
            .toString()
        val request = Request.Builder()
            .url("https://youtubei.googleapis.com/youtubei/v1/visitor_id?prettyPrint=false")
            .post(body.toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
            .header("X-Goog-Api-Format-Version", "2")
            .build()
        return executeYoutubeJson(request, "Android visitor")
            .optJSONObject("responseContext")
            ?.optString("visitorData")
            ?.takeIf { it.isNotBlank() }
            ?: throw YoutubePlayerRequestException(null, "Android visitorData assente")
    }

    private fun executeYoutubeJson(request: Request, label: String): JSONObject {
        return youtubeHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw YoutubePlayerRequestException(response.code, "$label HTTP ${response.code}")
            }
            val responseText = readBoundedYoutubeJsonBody(response.body)
            runCatching { JSONObject(responseText) }
                .getOrElse { error ->
                    throw YoutubePlayerRequestException(
                        null,
                        "$label risposta JSON non valida: ${error.message.orEmpty()}"
                    )
                }
        }
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
                val resolved = runCatchingPreservingCancellation { resolveWithLevyraExtractor(track, false, audioQuality) }
                resolved.onSuccess { winner.complete(it) }
                    .onFailure { error ->
                        errors += "LevyraExtractor: ${error.playbackDiagnostic()}"
                    }
            }
            val innerTubeJob = launch {
                delay(LevyraResolverLatency.innerTubeFallbackDelayMs(isVideoMode = false, preferMp4Audio = false))
                if (winner.isCompleted) return@launch
                val stream = runCatchingPreservingCancellation { hedgedInnerTube(track, errors, false, audioQuality) }.getOrNull()
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
            val resolved = runCatchingPreservingCancellation { resolveWithLevyraExtractor(track, true, audioQuality) }
            resolved.onSuccess { winner.complete(it) }
                .onFailure { error ->
                    errors += "LevyraExtractor: ${error.playbackDiagnostic()}"
                }
        }
        val innerTubeJob = launch {
            delay(LevyraResolverLatency.innerTubeFallbackDelayMs(isVideoMode = false, preferMp4Audio = true))
            if (winner.isCompleted) return@launch
            val stream = runCatchingPreservingCancellation { raceInnerTube(track, errors, false, true, audioQuality) }.getOrNull()
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
            val resolved = runCatchingPreservingCancellation { resolveAudioFast(candidate, localErrors, preferMp4Audio, audioQuality) }.getOrNull()
            if (
                resolved != null &&
                resolved.streamUrl.isNotBlank() &&
                streamStillFresh(resolved.streamUrl) &&
                isPlayableAudioUrl(resolved.streamUrl) &&
                (
                    !preferMp4Audio ||
                        resolved.playbackManifest?.let(::supportsOfflineExport) == true ||
                        (
                            isMp4AudioUrl(resolved.streamUrl) &&
                                YoutubeStreamCapability.servesCompleteStream(resolved.streamUrl)
                            )
                    )
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
        errors: MutableList<String>,
        allowNetworkRefresh: Boolean = true
    ): Track? {
        val stored = runCatchingPreservingCancellation { sourceMatchStore.load(track, isVideoMode, audioQuality, preferMp4Audio) }
            .onFailure { error ->
                Timber.w(error, "persistent source match load failed")
            }
            .getOrNull() ?: return null
        val now = System.currentTimeMillis()
        if (stored.entity.blockedUntil > now) return null
        val manifest = stored.manifest
        if (manifest != null && manifest.isFresh(now) && manifestUrlsUsable(manifest, isVideoMode, preferMp4Audio)) {
            val restored = track.applyManifest(
                manifest = manifest,
                sourceVideoUrl = stored.entity.sourceVideoUrl,
                source = stored.entity.provider.ifBlank { "Persistent source match" }
            )
            if (!isVideoMode || restored.hasVideoPlaybackPayload()) {
                recordSourceMatchSuccess(track, isVideoMode, audioQuality, preferMp4Audio)
                return restored
            }
        }
        if (!allowNetworkRefresh) return null
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
        val extractor = runCatchingPreservingCancellation {
            if (isVideoMode) {
                resolveVideoWithLevyraExtractor(sourceTrack, audioQuality)
            } else {
                resolveWithLevyraExtractor(sourceTrack, preferMp4Audio, audioQuality)
            }
        }.onFailure { error ->
            errors += "Persistent LevyraExtractor: ${error.playbackDiagnostic()}"
        }.getOrNull()
        if (extractor != null && manifestUrlsUsable(extractor.playbackManifest, isVideoMode, preferMp4Audio)) return extractor
        val direct = runCatchingPreservingCancellation {
            if (preferMp4Audio) {
                raceInnerTube(sourceTrack, errors, isVideoMode, true, audioQuality)
            } else {
                hedgedInnerTube(sourceTrack, errors, isVideoMode, audioQuality)
            }
        }.onFailure { error ->
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
        if (preferMp4Audio && !supportsOfflineExport(manifest)) return false
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
        if (preferMp4Audio && !supportsOfflineExport(manifest)) return
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
                runCatchingPreservingCancellation { repository.search(query, 6, userPreferences.languageCode()) }
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

    private suspend fun searchYouTubeWebCandidates(track: Track, query: String): List<Track> {
        val encoded = query.urlEncode()
        val request = Request.Builder()
            .url("https://www.youtube.com/results?search_query=$encoded")
            .get()
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("User-Agent", profiles.first { it.clientName == "WEB_REMIX" }.userAgent)
            .build()
        val result = runCatchingPreservingCancellation {
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
        currentCoroutineContext().ensureActive()
        return result
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
                    runCatchingPreservingCancellation { resolveWithInnerTube(track, profile, isVideoMode, false, audioQuality) }
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
                val attempt = runCatchingPreservingCancellation { resolveWithInnerTube(track, profile, isVideoMode, preferMp4Audio, audioQuality) }
                attempt.onSuccess { stream ->
                    if (!acceptResolvedStream(stream, isVideoMode, "${profile.label} probe", errors)) {
                        recordClientFailure(profile, null, IllegalStateException("Stream non valido o URL scaduto"))
                        return@onSuccess
                    }
                    if (!isVideoMode && preferMp4Audio) {
                        val exportable = isMp4AudioUrl(stream.url) &&
                            YoutubeStreamCapability.servesCompleteStream(stream.url)
                        Timber.i(
                            "offline candidate client=%s mp4=%s complete=%s",
                            profile.label,
                            isMp4AudioUrl(stream.url),
                            YoutubeStreamCapability.servesCompleteStream(stream.url)
                        )
                        if (exportable) winner.complete(stream) else fallback.compareAndSet(null, stream)
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

    private fun effectiveProfiles(): List<ClientProfile> {
        val overrides = playbackPolicyStore.current().clientOverrides
        val effective = profiles.mapNotNull { base ->
            val override = overrides[base.clientName]
            if (override?.enabled == false) return@mapNotNull null
            val version = override?.clientVersion ?: base.clientVersion
            base.copy(
                clientVersion = version,
                userAgent = if (version == base.clientVersion) {
                    base.userAgent
                } else {
                    base.userAgent.replace(base.clientVersion, version)
                },
                tier = override?.priority ?: base.tier,
                requiresPoToken = override?.requiresPoToken ?: base.requiresPoToken
            )
        }
        return effective.ifEmpty { profiles }
    }

    private fun profileFromSource(source: String): ClientProfile? {
        val normalized = source.lowercase()
        return effectiveProfiles().firstOrNull { profile ->
            normalized.contains(profile.label.lowercase()) || normalized.contains(profile.clientName.lowercase())
        } ?: profiles.firstOrNull { profile ->
            normalized.contains(profile.label.lowercase()) || normalized.contains(profile.clientName.lowercase())
        }
    }

    private fun orderedProfiles(): List<ClientProfile> {
        val now = System.currentTimeMillis()
        val configured = effectiveProfiles()
        val available = configured.filter { profile ->
            (clientHealth[profile.clientName]?.blockedUntilMs ?: 0L) <= now
        }
        val candidates = if (available.isNotEmpty()) available else configured
        val sorted = candidates.sortedWith(
            compareByDescending<ClientProfile> { it.requiresPoToken }
                .thenByDescending { profile -> clientHealth[profile.clientName]?.score ?: 50.0 }
                .thenBy { profile -> clientHealth[profile.clientName]?.averageLatencyMs ?: Long.MAX_VALUE }
                .thenBy { it.tier }
        )
        val vr = sorted.firstOrNull { it.clientName in PROGRESSIVE_STREAM_CLIENTS } ?: return sorted
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
        if (!hasValidatedInternet()) {
            clearTransientClientPenalties()
            return
        }
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
        val now = System.currentTimeMillis()
        clientHealthPrefs.all.forEach { (name, rawValue) ->
            val raw = rawValue as? String ?: return@forEach
            val json = runCatching { JSONObject(raw) }.getOrNull() ?: return@forEach
            val restored = ClientHealth(
                successes = json.optInt("successes", 0),
                failures = json.optInt("failures", 0),
                consecutiveFailures = json.optInt("consecutiveFailures", 0),
                averageLatencyMs = json.optLong("averageLatencyMs", Long.MAX_VALUE),
                blockedUntilMs = json.optLong("blockedUntilMs", 0L),
                updatedAtMs = json.optLong("updatedAtMs", 0L)
            )
            val normalized = if (restored.blockedUntilMs in 1L..now) {
                restored.copy(consecutiveFailures = 0, blockedUntilMs = 0L, updatedAtMs = now)
            } else {
                restored
            }
            clientHealth[name] = normalized
            if (normalized != restored) persistClientHealth(name, normalized)
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
        audioQuality: String = selectedAudioQuality,
        preferMp4Audio: Boolean = false
    ) {
        if (preferMp4Audio) return
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

    private suspend fun acceptResolvedStream(stream: DirectStream, isVideoMode: Boolean, label: String, errors: MutableList<String>): Boolean {
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

    private suspend fun verifyDirectAudioUrlFast(url: String): Boolean {
        if (url.isBlank() || !streamStillFresh(url) || !isDirectAudioUrl(url)) return false
        if (isTrustedGoogleVideoUrl(url) && url.containsQueryParameter("pot")) return true
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Range", "bytes=0-8191")
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")
            .header("User-Agent", profiles.first().userAgent)
            .build()
        val result = runCatchingPreservingCancellation {
            streamProbeClient.newCall(request).execute().use { response ->
                if (response.code == 403 || response.code == 404 || response.code == 410 || response.code == 416 || response.code == 429) return@use false
                if (response.code !in 200..299 && response.code != 206) return@use false
                val contentType = response.header("Content-Type").orEmpty().lowercase()
                if (contentType.contains("text/html") || contentType.contains("application/json")) return@use false
                val sample = response.peekBody(32L).bytes()
                sample.isNotEmpty()
            }
        }.getOrDefault(false)
        currentCoroutineContext().ensureActive()
        return result
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
            val firstAttempt = runCatchingPreservingCancellation {
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
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            val latency = elapsedMs(startedAt)
            if (hasValidatedInternet()) {
                recordClientFailure(profile, latency, error)
                resilienceEngine.recordFailure(profile.label, mode, latency, error)
            } else {
                clearTransientClientPenalties()
                Timber.d(error, "resolver skipped client penalty while offline")
            }
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
        val sourceVideoId = PlaybackSourceIdentity.sourceVideoId(track)
            .takeIf(youtubeVideoIdRegex::matches)
            ?: throw YoutubePlayerRequestException(null, "Identità video YouTube assente o non valida")
        val sourceVideoUrl = "https://www.youtube.com/watch?v=$sourceVideoId"
        val session = if (profile.requiresPoToken) {
            playbackSecurity.currentSession()
        } else {
            playbackSecurity.cachedSession()
        }
        val poTokens = if (profile.requiresPoToken) playbackSecurity.poTokensForPlayback(sourceVideoId, session) else null
        val signatureTimestamp = if (profile.clientName.startsWith("WEB")) {
            runCatchingPreservingCancellation {
                YoutubeJavaScriptPlayerManager.getSignatureTimestamp(sourceVideoId)
            }.getOrNull()
        } else {
            null
        }
        val endpointHost = if (profile.clientName == "ANDROID_VR") {
            "https://youtubei.googleapis.com"
        } else {
            "https://www.youtube.com"
        }
        val endpoint = "$endpointHost/youtubei/v1/player?key=$apiKey&prettyPrint=false"
        val body = buildPlayerBody(
            videoId = sourceVideoId,
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
            .header("Referer", if (profile.clientName == "WEB_EMBEDDED_PLAYER") "https://www.youtube.com/embed/$sourceVideoId" else sourceVideoUrl)
            .header("User-Agent", playerUserAgent(profile))
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
                    if (preferMp4Audio && !isMp4OfflineAudioCandidate(mime, format.optString("url"))) continue
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
                    videoId = sourceVideoId,
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
                        val url = format.resolveFormatUrl(sourceVideoId, poTokens?.streamingToken, profile.clientName.startsWith("WEB"))
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
                        val url = format.resolveFormatUrl(sourceVideoId, poTokens?.streamingToken, profile.clientName.startsWith("WEB"))
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
                        ?.let { it.finalizeStreamingUrl(sourceVideoId, poTokens?.streamingToken, profile.clientName.startsWith("WEB")) }
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
                            sourceVideoId = sourceVideoId,
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
                            sourceVideoId = sourceVideoId,
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
                            sourceVideoId = sourceVideoId,
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
                sourceVideoId = sourceVideoId,
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
        val compatible = if (preferMp4Audio) playable.filter(::isMp4AudioStream) else playable
        return compatible.maxByOrNull { scoreExtractorAudio(it, preferMp4Audio, audioQuality) }
    }

    private fun scoreExtractorAudio(stream: AudioStream, preferMp4Audio: Boolean, audioQuality: String): Int {
        val formatName = stream.getFormat()?.name.orEmpty()
        val content = stream.content
        val mime = "$formatName $content"
        return scoreAudioFormat(mime, stream.formatId, stream.averageBitrate, "", preferMp4Audio, audioQuality)
    }

    private fun isMp4AudioStream(stream: AudioStream): Boolean {
        val formatName = stream.getFormat()?.name.orEmpty()
        return isMp4OfflineAudioCandidate(formatName, stream.content)
    }

    private suspend fun isVerifiedHlsManifest(url: String): Boolean {
        if (url.isBlank() || !isHlsManifestUrl(url)) return false
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/vnd.apple.mpegurl,application/x-mpegURL,*/*")
            .header("User-Agent", profiles.first().userAgent)
            .header("Range", "bytes=0-2047")
            .build()
        val result = runCatchingPreservingCancellation {
            youtubeHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val head = response.peekBody(2048L).string().trimStart('\uFEFF', ' ', '\n', '\r', '\t')
                head.startsWith("#EXTM3U")
            }
        }.getOrDefault(false)
        currentCoroutineContext().ensureActive()
        return result
    }

    private suspend fun resolveWithLevyraExtractor(
        track: Track,
        preferMp4Audio: Boolean = false,
        audioQuality: String = selectedAudioQuality
    ): Track {
        NewPipeRuntime.ensure()
        val sourceVideoId = PlaybackSourceIdentity.sourceVideoId(track)
            .takeIf(youtubeVideoIdRegex::matches)
            ?: throw IllegalStateException("Identità video YouTube assente o non valida")
        val sourceVideoUrl = "https://www.youtube.com/watch?v=$sourceVideoId"
        val info = StreamInfo.getInfo(ServiceList.YouTube, sourceVideoUrl)
        currentCoroutineContext().ensureActive()
        val completeAudioStreams = info.audioStreams.filter {
            it.isUrl &&
                it.content.isNotBlank() &&
                YoutubeStreamCapability.servesCompleteStream(it.content) &&
                !isPlaybackUrlBlocked(it.content) &&
                streamStillFresh(it.content)
        }
        val audio = selectAudioStream(completeAudioStreams, preferMp4Audio, audioQuality)
        val muxedAudioSource = if (audio == null) {
            info.videoStreams.firstOrNull {
                it.isUrl &&
                    it.content.isNotBlank() &&
                    YoutubeStreamCapability.servesCompleteStream(it.content) &&
                    !isPlaybackUrlBlocked(it.content) &&
                    streamStillFresh(it.content) &&
                    (!preferMp4Audio || isMuxedMp4ExportUrl(it.content))
            }
        } else {
            null
        }
        val hlsUrl = if (audio == null && muxedAudioSource == null && !preferMp4Audio) {
            info.hlsUrl.takeIf { isVerifiedHlsManifest(it) }
        } else {
            null
        }
        val url = audio?.content ?: muxedAudioSource?.content ?: hlsUrl
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
        val provider = if (audio != null || muxedAudioSource != null) {
            LEVYRA_EXTRACTOR_PROVIDER
        } else {
            LEVYRA_EXTRACTOR_HLS_PROVIDER
        }
        val descriptors = when {
            audio != null -> completeAudioStreams
                .asSequence()
                .filter { it.content.isNotBlank() && streamStillFresh(it.content) }
                .map { audioDescriptor(it, it.content == url) }
                .toList()

            muxedAudioSource != null -> listOf(videoDescriptor(extractorVideoCandidate(muxedAudioSource), true))
            else -> listOf(hlsDescriptor(url))
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
            source = when {
                audio != null -> "LevyraExtractor${label.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}"
                muxedAudioSource != null -> LEVYRA_EXTRACTOR_PROVIDER
                else -> LEVYRA_EXTRACTOR_HLS_PROVIDER
            },
            playbackManifest = manifest
        ).withYoutubeEngagement(info.likeCount, info.viewCount).also {
            cacheYoutubeEngagement(sourceVideoId, info.likeCount, info.viewCount)
        }
    }

    private suspend fun resolveVideoWithLevyraExtractor(
        track: Track,
        audioQuality: String = selectedAudioQuality
    ): Track {
        NewPipeRuntime.ensure()
        val sourceVideoId = PlaybackSourceIdentity.sourceVideoId(track)
            .takeIf(youtubeVideoIdRegex::matches)
            ?: throw IllegalStateException("Identità video YouTube assente o non valida")
        val sourceVideoUrl = "https://www.youtube.com/watch?v=$sourceVideoId"
        val info = StreamInfo.getInfo(ServiceList.YouTube, sourceVideoUrl)
        currentCoroutineContext().ensureActive()
        val bestThumb = info.thumbnails.maxByOrNull { image ->
            image.width.coerceAtLeast(0) * image.height.coerceAtLeast(0)
        }?.url.orEmpty()
        val artworkSafe = LevyraPersonalOrbit.preferAlbumArtwork(
            primary = track,
            donor = track.copy(thumbnailUrl = bestThumb, largeThumbnailUrl = bestThumb)
        ).withYoutubeEngagement(info.likeCount, info.viewCount)
        cacheYoutubeEngagement(sourceVideoId, info.likeCount, info.viewCount)
        val durationMs = if (info.duration > 0L) info.duration * 1000L else track.durationMs
        val completeAudioStreams = info.audioStreams.filter {
            it.isUrl && YoutubeStreamCapability.servesCompleteStream(it.content)
        }
        val selectedAudio = selectAudioStream(completeAudioStreams, preferMp4Audio = false, audioQuality = audioQuality)
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
                completeAudioStreams
                    .asSequence()
                    .filter {
                        it.content.isNotBlank() &&
                            !isPlaybackUrlBlocked(it.content) &&
                            streamStillFresh(it.content)
                    }
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

    private fun playerUserAgent(profile: ClientProfile): String {
        if (profile.clientName != "VISIONOS") return profile.userAgent
        val locale = LevyraContentLocales.forLanguage(userPreferences.languageCode())
        return visionOsUserAgent(locale.gl)
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
        if (profile.clientName == "VISIONOS") {
            applyVisionOsClientIdentity(client)
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
            decodeThrottlingParameter(videoId, this) ?: return ""
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

    private fun decodeThrottlingParameter(videoId: String, url: String): String? {
        val deobfuscated = runCatching {
            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
        }
            .recoverCatching {
                YoutubeJavaScriptPlayerManager.clearThrottlingParametersCache()
                YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
            }
            .onFailure { error ->
                Timber.w(error, "Throttling parameter deobfuscation failed for %s", videoId)
            }
            .getOrNull()
            ?: return null
        if (throttlingParameterOf(deobfuscated) == throttlingParameterOf(url)) {
            Timber.w("Throttling parameter unchanged for %s, treating stream as throttled", videoId)
            return null
        }
        return deobfuscated
    }

    private fun throttlingParameterOf(url: String): String =
        url.substringAfter("&n=", "").ifBlank { url.substringAfter("?n=", "") }.substringBefore('&')

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

private val PROGRESSIVE_STREAM_CLIENTS = setOf("VISIONOS", "ANDROID_VR")

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
            "VISIONOS" -> "101"
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

private data class PlaybackStrategyOrigin(
    val mode: String,
    val strategy: String
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
