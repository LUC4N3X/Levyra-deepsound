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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

internal class YoutubeMusicResilienceClient(
    private val context: Context?,
    private val apiKey: String,
    webRemixVersion: String,
    private val clock: () -> Long = System::currentTimeMillis,
    private val monotonicClock: () -> Long = { System.nanoTime() / 1_000_000L },
    transport: YoutubeMusicTransport? = null
) {
    private val transport = transport ?: OkHttpYoutubeMusicTransport(context)
    private val inFlight = ConcurrentHashMap<String, YoutubeMusicInFlightRequest>()
    private val visitorData = ConcurrentHashMap<String, String>()
    private val health = ConcurrentHashMap<String, YoutubeMusicClientHealth>()
    private val cacheLock = Any()
    private val responseCache = LinkedHashMap<String, YoutubeMusicCacheEntry>(32, 0.75f, true)
    private var responseCacheBytes = 0L

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

    fun search(
        query: String,
        languageCode: String,
        params: String = "",
        continuation: String = ""
    ): JSONObject? {
        val clean = query.trim()
        val cleanContinuation = continuation.trim()
        if (apiKey.isBlank()) return null
        if (clean.length < 2 && cleanContinuation.isBlank()) return null
        return request(
            kind = YoutubeMusicRequestKind.SEARCH,
            languageCode = languageCode,
            browseId = "",
            params = params.trim(),
            continuation = cleanContinuation,
            query = clean
        )
    }

    fun searchSuggestions(query: String, languageCode: String): JSONObject? {
        val clean = query.trim()
        if (apiKey.isBlank() || clean.length < 2) return null
        return request(
            kind = YoutubeMusicRequestKind.SEARCH_SUGGESTIONS,
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
        if (apiKey.isBlank() || (cleanBrowseId.isBlank() && cleanContinuation.isBlank())) return null
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

    internal fun cacheDiagnostics(): YoutubeMusicCacheDiagnostics = synchronized(cacheLock) {
        YoutubeMusicCacheDiagnostics(responseCache.size, responseCacheBytes)
    }

    internal fun inFlightReferenceCounts(): List<Int> = inFlight.values.map { it.referenceCount() }

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

        val leader = AtomicBoolean(false)
        val shared = inFlight.compute(requestKey) { _, current ->
            if (current == null) {
                leader.set(true)
                YoutubeMusicInFlightRequest()
            } else {
                current.retain()
            }
        } ?: return null

        try {
            if (leader.get()) {
                try {
                    val root = cached(requestKey)
                        ?: performRequest(kind, languageCode, browseId, params, continuation, query)
                    if (root != null) {
                        cache(
                            requestKey,
                            root,
                            if (continuation.isBlank()) DEFAULT_CACHE_TTL_MS else CONTINUATION_CACHE_TTL_MS
                        )
                    }
                    shared.complete(root?.toString())
                } catch (error: Exception) {
                    shared.completeExceptionally(error)
                }
            }
            return shared.await(TOTAL_REQUEST_BUDGET_MS)?.let(::JSONObject)
        } finally {
            inFlight.computeIfPresent(requestKey) { _, current ->
                when {
                    current !== shared -> current
                    shared.release() -> null
                    else -> current
                }
            }
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
        val deadline = monotonicClock() + TOTAL_REQUEST_BUDGET_MS
        val affectsProfileHealth = kind != YoutubeMusicRequestKind.SEARCH_SUGGESTIONS
        val orderedProfiles = orderedProfiles(clock()).let { candidates ->
            if (kind == YoutubeMusicRequestKind.SEARCH_SUGGESTIONS) {
                candidates.filter { it.id == "web-remix" }
            } else {
                candidates
            }
        }
        val deferredDenials = mutableListOf<YoutubeMusicDeferredFailure>()
        var lastFailure: Throwable? = null

        for (profile in orderedProfiles) {
            val remainingMs = deadline - monotonicClock()
            if (remainingMs <= 0L) {
                lastFailure = IOException("YouTube Music fallback deadline exceeded")
                break
            }

            val request = YoutubeMusicTransportRequest(
                path = when (kind) {
                    YoutubeMusicRequestKind.SEARCH -> "search"
                    YoutubeMusicRequestKind.SEARCH_SUGGESTIONS -> "music/get_search_suggestions"
                    YoutubeMusicRequestKind.BROWSE -> "browse"
                },
                apiKey = apiKey,
                profile = profile,
                payload = payload(profile, kind, languageCode, browseId, params, continuation, query).toString(),
                referer = referer(kind, browseId, query),
                visitorData = visitorData[profile.id].orEmpty(),
                timeoutMs = min(PROFILE_REQUEST_TIMEOUT_MS, remainingMs).coerceAtLeast(1L)
            )
            val attemptStartedAt = monotonicClock()
            val response = try {
                transport.execute(request)
            } catch (error: Exception) {
                if (error is InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                }
                lastFailure = error
                if (affectsProfileHealth) {
                    recordFailure(
                        profile = profile,
                        statusCode = null,
                        reason = error.message.orEmpty(),
                        latencyMs = (monotonicClock() - attemptStartedAt).takeIf { it > 0L },
                        now = clock()
                    )
                }
                continue
            }

            if (response.code !in 200..299) {
                lastFailure = IOException("HTTP ${response.code}")
                if (affectsProfileHealth) {
                    if (isRequestScopedDenial(response.code, response.body)) {
                        deferredDenials += YoutubeMusicDeferredFailure(
                            profile = profile,
                            statusCode = response.code,
                            reason = response.body,
                            latencyMs = response.latencyMs,
                            now = clock()
                        )
                    } else {
                        recordFailure(profile, response.code, response.body, response.latencyMs, clock())
                    }
                }
                continue
            }

            val parsed = runCatching { JSONObject(response.body) }
            val root = parsed.getOrNull()
            if (root == null) {
                lastFailure = parsed.exceptionOrNull() ?: IOException("Invalid JSON")
                if (affectsProfileHealth) {
                    recordFailure(profile, response.code, "invalid json", response.latencyMs, clock())
                }
                continue
            }
            if (!isUseful(kind, root)) {
                if (affectsProfileHealth) {
                    if (isRequestScopedDenial(response.code, response.body)) {
                        deferredDenials += YoutubeMusicDeferredFailure(
                            profile = profile,
                            statusCode = response.code,
                            reason = response.body,
                            latencyMs = response.latencyMs,
                            now = clock()
                        )
                    } else {
                        recordFailure(profile, response.code, "empty response", response.latencyMs, clock())
                    }
                }
                continue
            }

            if (affectsProfileHealth) {
                deferredDenials.forEach { recordRequestScopedDenial(it, recoveredByFallback = true) }
            }
            root.optJSONObject("responseContext")
                ?.optString("visitorData")
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?.let { visitorData[profile.id] = it }

            if (affectsProfileHealth) {
                recordSuccess(profile, response.latencyMs, clock())
            }
            return root
        }

        if (affectsProfileHealth) {
            deferredDenials.forEach { recordRequestScopedDenial(it, recoveredByFallback = false) }
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
            YoutubeMusicRequestKind.SEARCH -> {
                if (query.isNotBlank()) root.put("query", query)
                if (params.isNotBlank()) root.put("params", params)
                if (continuation.isNotBlank()) root.put("continuation", continuation)
            }
            YoutubeMusicRequestKind.SEARCH_SUGGESTIONS -> {
                if (query.isNotBlank()) root.put("input", query)
            }
            YoutubeMusicRequestKind.BROWSE -> {
                if (browseId.isNotBlank()) root.put("browseId", browseId)
                if (params.isNotBlank()) root.put("params", params)
                if (continuation.isNotBlank()) root.put("continuation", continuation)
            }
        }
        return root
    }

    private fun orderedProfiles(now: Long): List<YoutubeMusicClientProfile> {
        val permitted = profiles.filter {
            PlaybackClientCapabilities.isEnabled(it.clientName, PlaybackClientCapability.BROWSE)
        }.ifEmpty { profiles }
        val available = permitted.filter { (health[it.id]?.blockedUntilMs ?: 0L) <= now }
        if (available.isEmpty()) return emptyList()
        return available.sortedWith(
            compareByDescending<YoutubeMusicClientProfile> {
                health[it.id]?.score ?: DEFAULT_PROFILE_SCORE
            }.thenBy { it.priority }
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
                consecutiveDenials = 0,
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
            val blockMs = when {
                statusCode == 429 -> HARD_BLOCK_MS
                consecutive >= 4 -> LONG_BLOCK_MS
                consecutive >= 2 -> SHORT_BLOCK_MS
                else -> 0L
            }
            previous.copy(
                failures = failures,
                consecutiveFailures = consecutive,
                averageLatencyMs = updatedAverage(previous.averageLatencyMs, latencyMs),
                blockedUntilMs = max(previous.blockedUntilMs, now + blockMs),
                updatedAtMs = now
            )
        }
        if (reason.isNotBlank()) Timber.v("YouTube Music profile %s failed: %s", profile.id, reason)
    }

    private fun recordRequestScopedDenial(
        failure: YoutubeMusicDeferredFailure,
        recoveredByFallback: Boolean
    ) {
        health.compute(failure.profile.id) { _, current ->
            val previous = current ?: YoutubeMusicClientHealth()
            previous.copy(
                denials = (previous.denials + 1).coerceAtMost(MAX_HEALTH_SAMPLES),
                consecutiveDenials = (previous.consecutiveDenials + 1).coerceAtMost(20),
                averageLatencyMs = updatedAverage(previous.averageLatencyMs, failure.latencyMs),
                updatedAtMs = failure.now
            )
        }
        Timber.v(
            "YouTube Music profile %s denied request with HTTP %s; fallbackRecovered=%s",
            failure.profile.id,
            failure.statusCode,
            recoveredByFallback
        )
    }

    private fun updatedAverage(previous: Long, latencyMs: Long?): Long {
        return when {
            latencyMs == null || latencyMs <= 0L -> previous
            previous == Long.MAX_VALUE -> latencyMs
            else -> ((previous * 3L) + latencyMs) / 4L
        }
    }

    private fun isRequestScopedDenial(statusCode: Int?, reason: String): Boolean {
        return statusCode == 401 || statusCode == 403 || statusCode == 410 ||
            reason.contains("sign in", ignoreCase = true) ||
            reason.contains("login", ignoreCase = true)
    }

    private fun isUseful(kind: YoutubeMusicRequestKind, root: JSONObject): Boolean {
        if (root.has("error")) return false
        val raw = root.toString()
        return when (kind) {
            YoutubeMusicRequestKind.SEARCH -> SEARCH_MARKERS.any(raw::contains)
            YoutubeMusicRequestKind.SEARCH_SUGGESTIONS -> SEARCH_SUGGESTION_MARKERS.any(raw::contains)
            YoutubeMusicRequestKind.BROWSE -> BROWSE_MARKERS.any(raw::contains)
        }
    }

    private fun referer(kind: YoutubeMusicRequestKind, browseId: String, query: String): String {
        return when (kind) {
            YoutubeMusicRequestKind.SEARCH -> {
                val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                "https://music.youtube.com/search?q=$encoded"
            }
            YoutubeMusicRequestKind.SEARCH_SUGGESTIONS -> "https://music.youtube.com/"
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
            responseCacheBytes -= entry.sizeBytes
            return@synchronized null
        }
        val parsed = runCatching { JSONObject(entry.payload) }.getOrNull()
        if (parsed == null) {
            responseCache.remove(key)
            responseCacheBytes -= entry.sizeBytes
        }
        parsed
    }

    private fun cache(key: String, root: JSONObject, ttlMs: Long) = synchronized(cacheLock) {
        val payload = root.toString()
        val sizeBytes = payload.toByteArray(StandardCharsets.UTF_8).size.toLong()
        if (sizeBytes > MAX_SINGLE_CACHE_ENTRY_BYTES) return@synchronized

        responseCache.remove(key)?.let { responseCacheBytes -= it.sizeBytes }
        responseCache[key] = YoutubeMusicCacheEntry(
            payload = payload,
            expiresAtMs = clock() + max(1_000L, ttlMs),
            sizeBytes = sizeBytes
        )
        responseCacheBytes += sizeBytes
        trimCache()
    }

    private fun trimCache() {
        val iterator = responseCache.entries.iterator()
        while (
            iterator.hasNext() &&
            (responseCache.size > MAX_CACHE_ENTRIES || responseCacheBytes > MAX_CACHE_BYTES)
        ) {
            val entry = iterator.next().value
            responseCacheBytes -= entry.sizeBytes
            iterator.remove()
        }
    }

    private companion object {
        const val MAX_CACHE_ENTRIES = 96
        const val MAX_CACHE_BYTES = 2L * 1024L * 1024L
        const val MAX_SINGLE_CACHE_ENTRY_BYTES = 512L * 1024L
        const val MAX_HEALTH_SAMPLES = 10_000
        const val DEFAULT_CACHE_TTL_MS = 75_000L
        const val CONTINUATION_CACHE_TTL_MS = 20_000L
        const val TOTAL_REQUEST_BUDGET_MS = 35_000L
        const val PROFILE_REQUEST_TIMEOUT_MS = 14_000L
        const val SHORT_BLOCK_MS = 25_000L
        const val LONG_BLOCK_MS = 2L * 60L * 1_000L
        const val HARD_BLOCK_MS = 10L * 60L * 1_000L
        const val DEFAULT_PROFILE_SCORE = 50.0
        const val WEB_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
        const val ANDROID_MUSIC_USER_AGENT =
            "com.google.android.apps.youtube.music/8.10.52 (Linux; U; Android 15; Pixel 8 Pro Build/AP3A.241105.007) gzip"
        const val ANDROID_USER_AGENT =
            "com.google.android.youtube/19.44.38 (Linux; U; Android 15; Pixel 8 Pro Build/AP3A.241105.007) gzip"
        const val IOS_USER_AGENT =
            "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3 like Mac OS X)"

        /**
         * Renderers the search parser can actually read. Structural wrappers such as
         * `tabbedSearchResultsRenderer` or `itemSectionRenderer` are deliberately absent: they are
         * present on empty and blocked responses too, so on their own they must not stop the
         * fallback chain. A wrapper carrying real results still matches through the item renderers
         * nested inside it.
         */
        val SEARCH_MARKERS = arrayOf(
            "musicResponsiveListItemRenderer",
            "musicTwoRowItemRenderer",
            "videoRenderer",
            "musicCardShelfRenderer",
            "playlistPanelVideoRenderer"
        )
        val SEARCH_SUGGESTION_MARKERS = arrayOf(
            "searchSuggestionsSectionRenderer",
            "searchSuggestionRenderer",
            "historySuggestionRenderer"
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
    val visitorData: String,
    val timeoutMs: Long
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
    val denials: Int = 0,
    val consecutiveFailures: Int = 0,
    val consecutiveDenials: Int = 0,
    val averageLatencyMs: Long = Long.MAX_VALUE,
    val blockedUntilMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    val score: Double
        get() {
            val weightedFailures = failures.toDouble() + denials.toDouble() * 0.35
            val reliability = (successes + 1.0) / (successes + weightedFailures + 2.0)
            val latencyBonus =
                if (averageLatencyMs == Long.MAX_VALUE) 0.0 else 1_500.0 / averageLatencyMs.coerceAtLeast(50L)
            return reliability * 100.0 + latencyBonus - consecutiveFailures * 12.0 - consecutiveDenials * 6.0
        }
}

internal data class YoutubeMusicCacheDiagnostics(
    val entries: Int,
    val bytes: Long
)

private data class YoutubeMusicCacheEntry(
    val payload: String,
    val expiresAtMs: Long,
    val sizeBytes: Long
)

private data class YoutubeMusicDeferredFailure(
    val profile: YoutubeMusicClientProfile,
    val statusCode: Int?,
    val reason: String,
    val latencyMs: Long?,
    val now: Long
)

private class YoutubeMusicInFlightRequest {
    private val references = AtomicInteger(1)
    private val completed = CountDownLatch(1)

    @Volatile
    private var payload: String? = null

    @Volatile
    private var failure: Throwable? = null

    fun retain(): YoutubeMusicInFlightRequest {
        references.incrementAndGet()
        return this
    }

    fun complete(payload: String?) {
        this.payload = payload
        completed.countDown()
    }

    fun completeExceptionally(error: Throwable) {
        failure = error
        completed.countDown()
    }

    fun await(timeoutMs: Long): String? {
        return try {
            if (!completed.await(timeoutMs, TimeUnit.MILLISECONDS)) return null
            if (failure != null) null else payload
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        }
    }

    fun referenceCount(): Int = references.get()

    fun release(): Boolean = references.decrementAndGet() == 0 && completed.count == 0L
}

private enum class YoutubeMusicRequestKind {
    SEARCH,
    SEARCH_SUGGESTIONS,
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
        return client.newCall(httpRequest)
            .apply { timeout().timeout(request.timeoutMs, TimeUnit.MILLISECONDS) }
            .execute()
            .use { response ->
                YoutubeMusicTransportResponse(
                    code = response.code,
                    body = response.body.string(),
                    latencyMs = ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(1L)
                )
            }
    }
}
