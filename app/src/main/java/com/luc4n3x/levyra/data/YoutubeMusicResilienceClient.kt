package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.security.GoogleApiKeyHeaders
import com.luc4n3x.levyra.domain.LevyraContentLocales
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max

internal class YoutubeMusicResilienceClient(
    private val context: Context?,
    private val apiKey: String,
    webRemixVersion: String,
    private val clock: () -> Long = System::currentTimeMillis,
    transport: YoutubeMusicTransport? = null
) {
    private val transport = transport ?: OkHttpYoutubeMusicTransport(context)
    private val requestLocks = ConcurrentHashMap<String, Any>()
    private val visitorData = ConcurrentHashMap<String, String>()
    private val health = ConcurrentHashMap<String, YoutubeMusicClientHealth>()
    private val cacheLock = Any()
    private val responseCache = object : LinkedHashMap<String, YoutubeMusicCacheEntry>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, YoutubeMusicCacheEntry>?): Boolean {
            return size > MAX_CACHE_ENTRIES
        }
    }
    private val profiles = listOf(
        YoutubeMusicClientProfile(
            id = "web-remix",
            clientName = "WEB_REMIX",
            clientVersion = webRemixVersion,
            clientHeaderName = "67",
            userAgent = WEB_USER_AGENT,
            host = "https://music.youtube.com",
            origin = "https://music.youtube.com",
            platform = "DESKTOP",
            priority = 0
        ),
        YoutubeMusicClientProfile(
            id = "android-music",
            clientName = "ANDROID_MUSIC",
            clientVersion = "8.10.52",
            clientHeaderName = "21",
            userAgent = ANDROID_MUSIC_USER_AGENT,
            host = "https://youtubei.googleapis.com",
            origin = "",
            platform = "MOBILE",
            priority = 1,
            androidSdkVersion = 35,
            osName = "Android",
            osVersion = "15"
        ),
        YoutubeMusicClientProfile(
            id = "android",
            clientName = "ANDROID",
            clientVersion = "19.44.38",
            clientHeaderName = "3",
            userAgent = ANDROID_USER_AGENT,
            host = "https://youtubei.googleapis.com",
            origin = "",
            platform = "MOBILE",
            priority = 2,
            androidSdkVersion = 35,
            osName = "Android",
            osVersion = "15"
        ),
        YoutubeMusicClientProfile(
            id = "ios",
            clientName = "IOS",
            clientVersion = "20.10.4",
            clientHeaderName = "5",
            userAgent = IOS_USER_AGENT,
            host = "https://youtubei.googleapis.com",
            origin = "",
            platform = "MOBILE",
            priority = 3,
            osName = "iOS",
            osVersion = "18.3"
        ),
        YoutubeMusicClientProfile(
            id = "web",
            clientName = "WEB",
            clientVersion = "2.20260630.01.00",
            clientHeaderName = "1",
            userAgent = WEB_USER_AGENT,
            host = "https://www.youtube.com",
            origin = "https://www.youtube.com",
            platform = "DESKTOP",
            priority = 4
        )
    )

    fun search(query: String, languageCode: String): JSONObject? {
        val clean = query.trim()
        if (clean.length < 2 || apiKey.isBlank()) return null
        return request(
            kind = YoutubeMusicRequestKind.SEARCH,
            languageCode = languageCode,
            browseId = "",
            params = "",
            continuation = "",
            query = clean
        )
    }

    fun browse(
        languageCode: String,
        browseId: String,
        params: String = "",
        continuation: String = ""
    ): JSONObject? {
        val cleanBrowseId = browseId.trim()
        val cleanContinuation = continuation.trim()
        if (apiKey.isBlank() || cleanBrowseId.isBlank() && cleanContinuation.isBlank()) return null
        return request(
            kind = YoutubeMusicRequestKind.BROWSE,
            languageCode = languageCode,
            browseId = cleanBrowseId,
            params = params.trim(),
            continuation = cleanContinuation,
            query = ""
        )
    }

    internal fun diagnostics(): Map<String, YoutubeMusicClientHealth> = health.toMap()

    private fun request(
        kind: YoutubeMusicRequestKind,
        languageCode: String,
        browseId: String,
        params: String,
        continuation: String,
        query: String
    ): JSONObject? {
        val requestKey = listOf(kind.name, languageCode, browseId, params, continuation, query).joinToString("\u001f")
        cached(requestKey)?.let { return it }
        val requestLock = requestLocks.computeIfAbsent(requestKey) { Any() }
        try {
            return synchronized(requestLock) {
                cached(requestKey)?.let { return@synchronized it }
                performRequest(kind, languageCode, browseId, params, continuation, query)?.also { root ->
                    cache(requestKey, root, if (continuation.isBlank()) DEFAULT_CACHE_TTL_MS else CONTINUATION_CACHE_TTL_MS)
                }
            }
        } finally {
            requestLocks.remove(requestKey, requestLock)
        }
    }

    private fun performRequest(
        kind: YoutubeMusicRequestKind,
        languageCode: String,
        browseId: String,
        params: String,
        continuation: String,
        query: String
    ): JSONObject? {
        val now = clock()
        val orderedProfiles = orderedProfiles(now)
        var lastFailure: Throwable? = null
        for (profile in orderedProfiles) {
            val payload = payload(profile, kind, languageCode, browseId, params, continuation, query)
            val request = YoutubeMusicTransportRequest(
                path = if (kind == YoutubeMusicRequestKind.SEARCH) "search" else "browse",
                apiKey = apiKey,
                profile = profile,
                payload = payload.toString(),
                referer = referer(kind, browseId, query),
                visitorData = visitorData[profile.id].orEmpty()
            )
            val response = try {
                transport.execute(request)
            } catch (error: Exception) {
                if (error is InterruptedException) Thread.currentThread().interrupt()
                lastFailure = error
                recordFailure(profile, null, error.message.orEmpty(), null, now)
                continue
            }
            if (response.code !in 200..299) {
                val error = IOException("HTTP ${response.code}")
                lastFailure = error
                recordFailure(profile, response.code, response.body, response.latencyMs, now)
                continue
            }
            val parsed = runCatching { JSONObject(response.body) }
            val root = parsed.getOrNull()
            if (root == null) {
                val error = parsed.exceptionOrNull() ?: IOException("Invalid JSON")
                lastFailure = error
                recordFailure(profile, response.code, "invalid json", response.latencyMs, now)
                continue
            }
            if (!isUseful(kind, root)) {
                recordFailure(profile, response.code, "empty response", response.latencyMs, now)
                continue
            }
            root.optJSONObject("responseContext")
                ?.optString("visitorData")
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?.let { visitorData[profile.id] = it }
            recordSuccess(profile, response.latencyMs, now)
            return root
        }
        lastFailure?.let { Timber.d(it, "YouTube Music fallback chain exhausted") }
        return null
    }

    private fun payload(
        profile: YoutubeMusicClientProfile,
        kind: YoutubeMusicRequestKind,
        languageCode: String,
        browseId: String,
        params: String,
        continuation: String,
        query: String
    ): JSONObject {
        val locale = LevyraContentLocales.forLanguage(languageCode)
        val client = JSONObject()
            .put("clientName", profile.clientName)
            .put("clientVersion", profile.clientVersion)
            .put("hl", locale.hl)
            .put("gl", locale.gl)
            .put("platform", profile.platform)
        if (profile.androidSdkVersion > 0) client.put("androidSdkVersion", profile.androidSdkVersion)
        if (profile.osName.isNotBlank()) client.put("osName", profile.osName)
        if (profile.osVersion.isNotBlank()) client.put("osVersion", profile.osVersion)
        visitorData[profile.id]?.takeIf(String::isNotBlank)?.let { client.put("visitorData", it) }
        val root = JSONObject().put("context", JSONObject().put("client", client))
        when (kind) {
            YoutubeMusicRequestKind.SEARCH -> root.put("query", query)
            YoutubeMusicRequestKind.BROWSE -> {
                if (browseId.isNotBlank()) root.put("browseId", browseId)
                if (params.isNotBlank()) root.put("params", params)
                if (continuation.isNotBlank()) root.put("continuation", continuation)
            }
        }
        return root
    }

    private fun orderedProfiles(now: Long): List<YoutubeMusicClientProfile> {
        val available = profiles.filter { (health[it.id]?.blockedUntilMs ?: 0L) <= now }
        if (available.isEmpty()) {
            return profiles.sortedBy { health[it.id]?.blockedUntilMs ?: Long.MAX_VALUE }.take(1)
        }
        return available.sortedWith(
            compareByDescending<YoutubeMusicClientProfile> { it.priority == 0 }
                .thenByDescending { profile -> health[profile.id]?.score ?: 0.0 }
                .thenBy { it.priority }
        )
    }

    private fun recordSuccess(profile: YoutubeMusicClientProfile, latencyMs: Long, now: Long) {
        health.compute(profile.id) { _, current ->
            val previous = current ?: YoutubeMusicClientHealth()
            val samples = (previous.successes + 1).coerceAtMost(MAX_HEALTH_SAMPLES)
            val average = if (previous.successes <= 0 || previous.averageLatencyMs == Long.MAX_VALUE) {
                latencyMs.coerceAtLeast(1L)
            } else {
                ((previous.averageLatencyMs * 7L) + latencyMs.coerceAtLeast(1L)) / 8L
            }
            previous.copy(
                successes = samples,
                consecutiveFailures = 0,
                averageLatencyMs = average,
                blockedUntilMs = 0L,
                updatedAtMs = now
            )
        }
    }

    private fun recordFailure(
        profile: YoutubeMusicClientProfile,
        statusCode: Int?,
        reason: String,
        latencyMs: Long?,
        now: Long
    ) {
        health.compute(profile.id) { _, current ->
            val previous = current ?: YoutubeMusicClientHealth()
            val failures = (previous.failures + 1).coerceAtMost(MAX_HEALTH_SAMPLES)
            val consecutive = (previous.consecutiveFailures + 1).coerceAtMost(20)
            val hardFailure = statusCode == 403 || statusCode == 410 || statusCode == 429 ||
                reason.contains("sign in", true) || reason.contains("login", true)
            val blockMs = when {
                hardFailure -> HARD_BLOCK_MS
                consecutive >= 4 -> LONG_BLOCK_MS
                consecutive >= 2 -> SHORT_BLOCK_MS
                else -> 0L
            }
            val average = when {
                latencyMs == null || latencyMs <= 0L -> previous.averageLatencyMs
                previous.averageLatencyMs == Long.MAX_VALUE -> latencyMs
                else -> ((previous.averageLatencyMs * 3L) + latencyMs) / 4L
            }
            previous.copy(
                failures = failures,
                consecutiveFailures = consecutive,
                averageLatencyMs = average,
                blockedUntilMs = now + blockMs,
                updatedAtMs = now
            )
        }
    }

    private fun isUseful(kind: YoutubeMusicRequestKind, root: JSONObject): Boolean {
        if (root.has("error")) return false
        val raw = root.toString()
        return when (kind) {
            YoutubeMusicRequestKind.SEARCH -> SEARCH_MARKERS.any(raw::contains)
            YoutubeMusicRequestKind.BROWSE -> BROWSE_MARKERS.any(raw::contains)
        }
    }

    private fun referer(kind: YoutubeMusicRequestKind, browseId: String, query: String): String {
        return when (kind) {
            YoutubeMusicRequestKind.SEARCH -> {
                val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                "https://music.youtube.com/search?q=$encoded"
            }
            YoutubeMusicRequestKind.BROWSE -> browseId
                .takeIf(String::isNotBlank)
                ?.let { "https://music.youtube.com/browse/$it" }
                ?: "https://music.youtube.com/"
        }
    }

    private fun cached(key: String): JSONObject? = synchronized(cacheLock) {
        val entry = responseCache[key] ?: return@synchronized null
        if (clock() >= entry.expiresAtMs) {
            responseCache.remove(key)
            return@synchronized null
        }
        runCatching { JSONObject(entry.payload) }.getOrNull()
    }

    private fun cache(key: String, root: JSONObject, ttlMs: Long) = synchronized(cacheLock) {
        responseCache[key] = YoutubeMusicCacheEntry(root.toString(), clock() + max(1_000L, ttlMs))
    }

    private companion object {
        const val MAX_CACHE_ENTRIES = 96
        const val MAX_HEALTH_SAMPLES = 10_000
        const val DEFAULT_CACHE_TTL_MS = 75_000L
        const val CONTINUATION_CACHE_TTL_MS = 20_000L
        const val SHORT_BLOCK_MS = 25_000L
        const val LONG_BLOCK_MS = 2L * 60L * 1_000L
        const val HARD_BLOCK_MS = 10L * 60L * 1_000L
        const val WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
        const val ANDROID_MUSIC_USER_AGENT = "com.google.android.apps.youtube.music/8.10.52 (Linux; U; Android 15; Pixel 8 Pro Build/AP3A.241105.007) gzip"
        const val ANDROID_USER_AGENT = "com.google.android.youtube/19.44.38 (Linux; U; Android 15; Pixel 8 Pro Build/AP3A.241105.007) gzip"
        const val IOS_USER_AGENT = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3 like Mac OS X)"
        val SEARCH_MARKERS = arrayOf(
            "musicResponsiveListItemRenderer",
            "musicTwoRowItemRenderer",
            "videoRenderer",
            "musicShelfRenderer"
        )
        val BROWSE_MARKERS = arrayOf(
            "musicCarouselShelfRenderer",
            "musicResponsiveListItemRenderer",
            "musicTwoRowItemRenderer",
            "musicShelfRenderer",
            "gridRenderer",
            "musicNavigationButtonRenderer",
            "sectionListRenderer",
            "musicResponsiveHeaderRenderer",
            "musicDetailHeaderRenderer",
            "continuationContents"
        )
    }
}

internal fun interface YoutubeMusicTransport {
    fun execute(request: YoutubeMusicTransportRequest): YoutubeMusicTransportResponse
}

internal data class YoutubeMusicTransportRequest(
    val path: String,
    val apiKey: String,
    val profile: YoutubeMusicClientProfile,
    val payload: String,
    val referer: String,
    val visitorData: String
)

internal data class YoutubeMusicTransportResponse(
    val code: Int,
    val body: String,
    val latencyMs: Long
)

internal data class YoutubeMusicClientProfile(
    val id: String,
    val clientName: String,
    val clientVersion: String,
    val clientHeaderName: String,
    val userAgent: String,
    val host: String,
    val origin: String,
    val platform: String,
    val priority: Int,
    val androidSdkVersion: Int = 0,
    val osName: String = "",
    val osVersion: String = ""
)

internal data class YoutubeMusicClientHealth(
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
            val latencyBonus = if (averageLatencyMs == Long.MAX_VALUE) 0.0 else 1_500.0 / averageLatencyMs.coerceAtLeast(50L)
            return reliability * 100.0 + latencyBonus - consecutiveFailures * 12.0
        }
}

private data class YoutubeMusicCacheEntry(
    val payload: String,
    val expiresAtMs: Long
)

private enum class YoutubeMusicRequestKind {
    SEARCH,
    BROWSE
}

private class OkHttpYoutubeMusicTransport(context: Context?) : YoutubeMusicTransport {
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val client = LevyraHttpClientFactory.youtubePlayer().newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(14, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .build()
    private val appContext = context?.applicationContext

    override fun execute(request: YoutubeMusicTransportRequest): YoutubeMusicTransportResponse {
        val url = "${request.profile.host}/youtubei/v1/${request.path}?key=${request.apiKey}&prettyPrint=false"
        val builder = Request.Builder()
            .url(url)
            .post(request.payload.toRequestBody(mediaType))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Accept-Language", "*")
            .header("Referer", request.referer)
            .header("User-Agent", request.profile.userAgent)
            .header("X-Youtube-Client-Name", request.profile.clientHeaderName)
            .header("X-Youtube-Client-Version", request.profile.clientVersion)
        if (request.profile.origin.isNotBlank()) builder.header("Origin", request.profile.origin)
        if (request.profile.androidSdkVersion > 0) builder.header("X-Goog-Api-Format-Version", "2")
        request.visitorData.takeIf(String::isNotBlank)?.let { builder.header("X-Goog-Visitor-Id", it) }
        val httpRequest = GoogleApiKeyHeaders.applyTo(builder, appContext).build()
        val startedAt = System.nanoTime()
        return client.newCall(httpRequest).execute().use { response ->
            YoutubeMusicTransportResponse(
                code = response.code,
                body = response.body.string(),
                latencyMs = ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(1L)
            )
        }
    }
}
