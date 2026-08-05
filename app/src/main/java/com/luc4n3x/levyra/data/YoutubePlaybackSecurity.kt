package com.luc4n3x.levyra.data

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.LevyraContentLocales
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.LinkedHashMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal data class YoutubeGuestSession(
    val visitorData: String,
    val generation: Long,
    val playbackDeadlineElapsedMs: Long = Long.MAX_VALUE
)

internal data class YoutubePoTokens(
    val playerToken: String,
    val streamingToken: String
)

internal class YoutubePlayerRequestException(
    val httpCode: Int?,
    message: String
) : IllegalStateException(message)

internal class YoutubePoTokenRuntimeUnavailableException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

internal class YoutubePlaybackSecurity private constructor(
    context: Context,
    private val httpClient: OkHttpClient,
    private val apiKey: String,
    private val preferences: LevyraPreferences
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("levyra_youtube_guest", Context.MODE_PRIVATE)
    private val sessionMutex = Mutex()
    private val failureCount = AtomicInteger(0)
    private val tokenGenerator = YoutubeWebPoTokenGenerator(appContext, httpClient)

    private val warmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lastWarmAtMs = AtomicLong(0L)

    fun warmUp() {
        val now = System.currentTimeMillis()
        val previous = lastWarmAtMs.get()
        if (now - previous < WARM_COOLDOWN_MS) return
        if (!lastWarmAtMs.compareAndSet(previous, now)) return
        warmScope.launch {
            try {
                val session = currentSessionRequired()
                if (session.visitorData.isNotBlank()) {
                    tokenGenerator.prewarm(session.visitorData, session.generation)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastWarmAtMs.set(0L)
                Timber.d(error, "PO Token warm-up skipped")
            }
        }
    }

    fun cachedSession(): YoutubeGuestSession {
        return YoutubeGuestSession(
            visitorData = prefs.getString(KEY_VISITOR_DATA, "").orEmpty(),
            generation = prefs.getLong(KEY_GENERATION, 0L)
        )
    }

    suspend fun currentSession(): YoutubeGuestSession {
        val deadline = safeElapsedDeadline(SystemClock.elapsedRealtime(), FAST_WAIT_BUDGET_MS)
        return withPoTokenWaitBudget(FAST_WAIT_BUDGET_MS) {
            ensureCurrentSession().copy(playbackDeadlineElapsedMs = deadline)
        }
    }

    suspend fun currentSessionRequired(): YoutubeGuestSession = ensureCurrentSession()

    private suspend fun ensureCurrentSession(): YoutubeGuestSession = sessionMutex.withLock {
        val cached = cachedSession()
        if (cached.visitorData.isNotBlank()) return@withLock cached
        val fresh = fetchVisitorData()
        persistSession(fresh, cached.generation + 1L)
    }

    fun observeVisitorData(visitorData: String) {
        if (visitorData.isBlank()) return
        val current = prefs.getString(KEY_VISITOR_DATA, "").orEmpty()
        if (current == visitorData) return
        val generation = prefs.getLong(KEY_GENERATION, 0L) + 1L
        prefs.edit()
            .putString(KEY_VISITOR_DATA, visitorData)
            .putLong(KEY_GENERATION, generation)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
        lastWarmAtMs.set(0L)
        tokenGenerator.invalidate()
    }

    suspend fun poTokensRequired(
        videoId: String,
        session: YoutubeGuestSession
    ): YoutubePoTokens {
        require(videoId.isNotBlank() && session.visitorData.isNotBlank()) {
            "PO Token binding and visitor identity are required"
        }
        return tokenGenerator.generate(videoId, session.visitorData, session.generation, FULL_WAIT_BUDGET_MS)
    }

    suspend fun poTokensForPlayback(
        videoId: String,
        session: YoutubeGuestSession
    ): YoutubePoTokens {
        if (videoId.isBlank() || session.visitorData.isBlank()) {
            throw YoutubePoTokenRuntimeUnavailableException("Identita ospite assente per $videoId")
        }
        val remainingBudgetMs = remainingPoTokenWaitBudget(
            deadlineElapsedMs = session.playbackDeadlineElapsedMs,
            fallbackBudgetMs = FAST_WAIT_BUDGET_MS,
            nowElapsedMs = SystemClock.elapsedRealtime()
        )
        if (remainingBudgetMs <= 0L) {
            throw YoutubePoTokenRuntimeUnavailableException("Playback security wait budget exhausted")
        }
        return tokenGenerator.generate(videoId, session.visitorData, session.generation, remainingBudgetMs)
    }

    suspend fun rotateIfNeeded(error: Throwable): Boolean {
        val decision = classifyFailure(error)
        if (!decision.rotate) {
            if (decision.resetCounter) failureCount.set(0)
            return false
        }
        val attempts = failureCount.incrementAndGet()
        if (!decision.immediate && attempts < 2) return false
        return sessionMutex.withLock {
            val lastRotation = prefs.getLong(KEY_LAST_ROTATION, 0L)
            val now = System.currentTimeMillis()
            if (now - lastRotation < ROTATION_COOLDOWN_MS) return@withLock false
            val generation = prefs.getLong(KEY_GENERATION, 0L) + 1L
            prefs.edit()
                .remove(KEY_VISITOR_DATA)
                .putLong(KEY_GENERATION, generation)
                .putLong(KEY_LAST_ROTATION, now)
                .apply()
            lastWarmAtMs.set(0L)
            tokenGenerator.invalidate()
            val fresh = try {
                fetchVisitorData()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "Guest session rotation refresh failed")
                ""
            }
            if (fresh.isNotBlank()) persistSession(fresh, generation)
            failureCount.set(0)
            true
        }
    }

    fun resetFailureState() {
        failureCount.set(0)
    }

    private suspend fun fetchVisitorData(): String {
        val locale = LevyraContentLocales.forLanguage(preferences.languageCode())
        val client = JSONObject()
            .put("clientName", "WEB")
            .put("clientVersion", WEB_CLIENT_VERSION)
            .put("hl", locale.hl)
            .put("gl", locale.gl)
            .put("utcOffsetMinutes", 0)
            .put("timeZone", "UTC")
        val body = JSONObject()
            .put("context", JSONObject().put("client", client))
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("https://youtubei.googleapis.com/youtubei/v1/visitor_id?key=$apiKey&prettyPrint=false")
            .post(body)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", WEB_USER_AGENT)
            .header("X-Youtube-Client-Name", "1")
            .header("X-Youtube-Client-Version", WEB_CLIENT_VERSION)
            .build()
        return httpClient.awaitVisitorData(request)
    }

    private fun persistSession(visitorData: String, generation: Long): YoutubeGuestSession {
        prefs.edit()
            .putString(KEY_VISITOR_DATA, visitorData)
            .putLong(KEY_GENERATION, generation)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
        return YoutubeGuestSession(visitorData, generation)
    }

    private fun classifyFailure(error: Throwable): RotationDecision {
        val chain = generateSequence(error) { it.cause }.toList()
        if (isLocalRuntimeFailure(error)) {
            return RotationDecision(rotate = false, immediate = false, resetCounter = false)
        }
        val requestError = chain.filterIsInstance<YoutubePlayerRequestException>().firstOrNull()
        val blob = chain.joinToString(" ") { it.message.orEmpty() }.lowercase()
        return evaluateFailure(blob, requestError?.httpCode)
    }

    private data class RotationDecision(
        val rotate: Boolean,
        val immediate: Boolean,
        val resetCounter: Boolean
    )

    companion object {
        private const val KEY_VISITOR_DATA = "visitor_data"
        private const val KEY_GENERATION = "generation"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val KEY_LAST_ROTATION = "last_rotation"
        private const val ROTATION_COOLDOWN_MS = 30_000L
        private const val WARM_COOLDOWN_MS = 60_000L

        private const val FULL_WAIT_BUDGET_MS = 35_000L
        private const val FAST_WAIT_BUDGET_MS = 4_000L

        @Volatile
        private var instance: YoutubePlaybackSecurity? = null

        fun getInstance(context: Context): YoutubePlaybackSecurity {
            val appContext = context.applicationContext
            return instance ?: synchronized(this) {
                instance ?: YoutubePlaybackSecurity(
                    appContext,
                    LevyraHttpClientFactory.youtubePlayer(),
                    BuildConfig.YOUTUBE_INNERTUBE_API_KEY,
                    LevyraPreferences(appContext)
                ).also { instance = it }
            }
        }

        private const val WEB_CLIENT_VERSION = "2.20260630.01.00"
        private const val WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val GEO_MARKERS = listOf(
            "not available in your country",
            "not available in your region",
            "non disponibile nel tuo paese",
            "geo_restricted",
            "geographic"
        )
        private val BOT_MARKERS = listOf(
            "bot",
            "automated traffic",
            "unusual traffic",
            "confirm you’re not a bot",
            "confirm you're not a bot",
            "conferma di non essere un bot"
        )
        private val TOKEN_MARKERS = listOf(
            "po token",
            "potoken",
            "po_token",
            "token rejected",
            "serviceintegrity"
        )

        private fun evaluateFailure(blob: String, httpCode: Int?): RotationDecision {
            if (GEO_MARKERS.any(blob::contains)) return RotationDecision(false, false, true)
            val explicitBot = BOT_MARKERS.any(blob::contains)
            val tokenFailure = TOKEN_MARKERS.any(blob::contains)
            val retryableCode = httpCode == 403 || httpCode == 410 || httpCode == 429
            val loginGate = blob.contains("login_required") || blob.contains("sign in to confirm") || blob.contains("accedi per confermare")
            val rotate = explicitBot || tokenFailure || retryableCode || loginGate
            return RotationDecision(rotate, explicitBot || tokenFailure || loginGate, !rotate)
        }

        internal fun shouldRotateGuestSession(message: String, httpCode: Int?): Boolean {
            return evaluateFailure(message.lowercase(), httpCode).rotate
        }

        internal fun isLocalRuntimeFailure(error: Throwable): Boolean {
            return generateSequence(error) { it.cause }
                .any { it is YoutubePoTokenRuntimeUnavailableException }
        }
    }
}

private suspend fun OkHttpClient.awaitVisitorData(request: Request): String =
    suspendCancellableCoroutine { continuation ->
        val call = newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeResultIfActive(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        val text = it.body.string()
                        if (!it.isSuccessful) {
                            throw YoutubePlayerRequestException(it.code, "visitor_id HTTP ${it.code}")
                        }
                        JSONObject(text)
                            .optJSONObject("responseContext")
                            ?.optString("visitorData")
                            .orEmpty()
                            .ifBlank { throw IllegalStateException("visitorData assente") }
                    }
                }
                continuation.resumeResultIfActive(result)
            }
        })
    }

private fun <T> CancellableContinuation<T>.resumeResultIfActive(result: Result<T>) {
    if (!isActive) return
    runCatching { resumeWith(result) }
}

private class YoutubeWebPoTokenGenerator(
    private val context: Context,
    private val httpClient: OkHttpClient
) {
    private val lock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val invalidationVersion = AtomicLong(0L)
    private val consecutiveBuildFailures = AtomicInteger(0)
    private val nextBuildAttemptAtMs = AtomicLong(0L)
    private val nextRefreshAttemptAtMs = AtomicLong(0L)

    @Volatile
    private var active: PoTokenSession? = null

    @Volatile
    private var replacement: PoTokenSession? = null

    suspend fun generate(
        videoId: String,
        visitorData: String,
        generation: Long,
        waitBudgetMs: Long
    ): YoutubePoTokens {
        cachedTokens(videoId, visitorData, generation)?.let { cached ->
            refreshAheadIfStale(cached.session, visitorData, generation)
            return cached.tokens
        }
        return withPoTokenWaitBudget(waitBudgetMs) {
            generateWithinBudget(videoId, visitorData, generation)
        }
    }

    private suspend fun generateWithinBudget(
        videoId: String,
        visitorData: String,
        generation: Long
    ): YoutubePoTokens {
        var session = session(visitorData, generation, discard = null)
        var ready = awaitBuild(session)
        var streamingToken: String? = null
        try {
            streamingToken = ready.runtime.generate(videoId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "PO Token mint failed on the active runtime, rebuilding")
        }
        if (streamingToken == null) {
            session = session(visitorData, generation, discard = session)
            ready = awaitBuild(session)
            streamingToken = ready.runtime.generate(videoId)
        }
        refreshAheadIfStale(session, visitorData, generation)
        return YoutubePoTokens(playerToken = ready.playerToken, streamingToken = streamingToken)
    }

    private suspend fun awaitBuild(session: PoTokenSession): ReadyPoTokenRuntime {
        return try {
            session.deferred.await()
        } catch (error: Throwable) {
            throw localizePoTokenBuildFailure(error)
        }
    }

    suspend fun prewarm(visitorData: String, generation: Long) {
        val session = session(visitorData, generation, discard = null)
        session.deferred.await()
    }

    fun invalidate() {
        val invalidatedVersion = invalidationVersion.incrementAndGet()
        resetBuildAndRefreshBackoff()
        scope.launch {
            val retired = lock.withLock {
                val retiredActive = active
                    ?.takeIf { PoTokenInvalidationPolicy.shouldRetire(it.version, invalidatedVersion) }
                if (retiredActive != null) active = null
                val retiredReplacement = replacement
                    ?.takeIf { PoTokenInvalidationPolicy.shouldRetire(it.version, invalidatedVersion) }
                if (retiredReplacement != null) replacement = null
                retiredActive to retiredReplacement
            }
            closeWhenSettled(retired.first, graceMs = RETIRE_GRACE_MS)
            if (retired.second !== retired.first) {
                closeWhenSettled(retired.second, graceMs = RETIRE_GRACE_MS)
            }
        }
    }

    private fun cachedTokens(
        videoId: String,
        visitorData: String,
        generation: Long
    ): CachedPoTokens? {
        val current = active ?: return null
        if (!current.matches(visitorData, generation, invalidationVersion.get())) return null
        val ready = current.ready ?: return null
        if (ready.runtime.isExpired) return null
        val cached = ready.runtime.cachedToken(videoId) ?: return null
        return CachedPoTokens(
            session = current,
            tokens = YoutubePoTokens(playerToken = ready.playerToken, streamingToken = cached)
        )
    }

    private suspend fun session(
        visitorData: String,
        generation: Long,
        discard: PoTokenSession?
    ): PoTokenSession = lock.withLock {
        val version = invalidationVersion.get()
        val current = active
        val pendingReplacement = replacement
        val replacementMatches = pendingReplacement?.matches(visitorData, generation, version) == true
        if (
            PoTokenReplacementPolicy.shouldJoin(
                hasReplacement = pendingReplacement != null,
                replacementMatches = replacementMatches,
                replacementUsable = pendingReplacement?.isUsable == true,
                hasActive = current != null,
                activeDiscarded = current != null && current === discard,
                activeMatches = current?.matches(visitorData, generation, version) == true,
                activeUsable = current?.isUsable == true
            )
        ) {
            return@withLock pendingReplacement!!
        }

        val now = System.currentTimeMillis()
        val cooldownUntil = nextBuildAttemptAtMs.get()
        when (
            PoTokenSessionPolicy.decide(
                hasSession = current != null,
                discarded = current != null && current === discard,
                matches = current?.matches(visitorData, generation, version) == true,
                usable = current?.isUsable == true,
                nowMs = now,
                nextBuildAttemptAtMs = cooldownUntil
            )
        ) {
            PoTokenSessionAction.REUSE -> return@withLock current!!
            PoTokenSessionAction.BACKOFF ->
                throw YoutubePoTokenRuntimeUnavailableException("Integrity runtime in backoff for ${cooldownUntil - now} ms")
            PoTokenSessionAction.BUILD -> Unit
        }

        active = null
        closeWhenSettled(current, graceMs = RETIRE_GRACE_MS)

        val obsoleteReplacement = replacement
        replacement = null
        if (obsoleteReplacement !== current) {
            closeWhenSettled(obsoleteReplacement, graceMs = RETIRE_GRACE_MS)
        }

        val fresh = startSession(visitorData, generation, version, armBackoffOnFailure = true)
        active = fresh
        fresh
    }

    private fun startSession(
        visitorData: String,
        generation: Long,
        version: Long,
        armBackoffOnFailure: Boolean
    ): PoTokenSession {
        val session = PoTokenSession(visitorData, generation, version)
        session.deferred = scope.async(start = CoroutineStart.LAZY) {
            try {
                val runtime = YoutubePoTokenRuntime.create(context, httpClient)
                val playerToken = try {
                    runtime.generate(visitorData)
                } catch (error: Throwable) {
                    runtime.close()
                    throw error
                }
                val ready = ReadyPoTokenRuntime(runtime, playerToken)
                session.ready = ready
                if (version == invalidationVersion.get()) resetBuildBackoff()
                ready
            } catch (error: CancellationException) {
                session.failed = true
                if (
                    armBackoffOnFailure &&
                    version == invalidationVersion.get() &&
                    PoTokenBuildFailurePolicy.shouldArmBackoff(
                        cancellation = true,
                        ownerActive = currentCoroutineContext().isActive
                    )
                ) {
                    armBuildBackoff()
                }
                throw error
            } catch (error: Throwable) {
                session.failed = true
                if (armBackoffOnFailure && version == invalidationVersion.get()) armBuildBackoff()
                throw error
            }
        }
        session.deferred.start()
        return session
    }

    private fun refreshAheadIfStale(
        session: PoTokenSession,
        visitorData: String,
        generation: Long
    ) {
        val ready = session.ready ?: return
        if (!ready.runtime.isStale) return
        val now = System.currentTimeMillis()
        if (now < nextRefreshAttemptAtMs.get()) return
        if (!session.refreshing.compareAndSet(false, true)) return

        scope.launch {
            val version = invalidationVersion.get()
            val candidate = lock.withLock {
                if (
                    active !== session ||
                    !session.matches(visitorData, generation, version)
                ) {
                    return@withLock null
                }
                replacement
                    ?.takeIf { it.matches(visitorData, generation, version) && it.isUsable }
                    ?: startSession(
                        visitorData = visitorData,
                        generation = generation,
                        version = version,
                        armBackoffOnFailure = false
                    ).also { replacement = it }
            }

            if (candidate == null) {
                session.refreshing.set(false)
                return@launch
            }

            val built = try {
                candidate.deferred.await()
            } catch (error: CancellationException) {
                if (!currentCoroutineContext().isActive) throw error
                null
            } catch (error: Throwable) {
                Timber.d(error, "PO Token refresh-ahead build failed")
                null
            }

            if (built == null) {
                if (version == invalidationVersion.get()) {
                    nextRefreshAttemptAtMs.set(System.currentTimeMillis() + REFRESH_RETRY_COOLDOWN_MS)
                }
                lock.withLock {
                    if (replacement === candidate) replacement = null
                }
                closeWhenSettled(candidate, graceMs = 0L)
                session.refreshing.set(false)
                return@launch
            }

            val retired = lock.withLock {
                if (
                    active === session &&
                    invalidationVersion.get() == version &&
                    candidate.matches(visitorData, generation, version)
                ) {
                    active = candidate
                    if (replacement === candidate) replacement = null
                    session
                } else {
                    if (replacement === candidate) replacement = null
                    candidate
                }
            }
            if (version == invalidationVersion.get()) nextRefreshAttemptAtMs.set(0L)
            session.refreshing.set(false)
            closeWhenSettled(retired, graceMs = RETIRE_GRACE_MS)
        }
    }

    private fun armBuildBackoff() {
        val failures = consecutiveBuildFailures.incrementAndGet()
        nextBuildAttemptAtMs.set(System.currentTimeMillis() + PoTokenBackoff.delayMs(failures))
    }

    private fun resetBuildBackoff() {
        consecutiveBuildFailures.set(0)
        nextBuildAttemptAtMs.set(0L)
    }

    private fun resetBuildAndRefreshBackoff() {
        resetBuildBackoff()
        nextRefreshAttemptAtMs.set(0L)
    }

    private fun closeWhenSettled(session: PoTokenSession?, graceMs: Long) {
        if (session == null) return
        scope.launch {
            val ready = runCatching { session.deferred.await() }.getOrNull() ?: return@launch
            if (graceMs > 0L) delay(graceMs)
            ready.runtime.close()
        }
    }

    private data class CachedPoTokens(
        val session: PoTokenSession,
        val tokens: YoutubePoTokens
    )

    private class PoTokenSession(
        val visitorData: String,
        val generation: Long,
        val version: Long
    ) {
        lateinit var deferred: Deferred<ReadyPoTokenRuntime>
        val refreshing = AtomicBoolean(false)

        @Volatile
        var ready: ReadyPoTokenRuntime? = null

        @Volatile
        var failed = false

        fun matches(visitorData: String, generation: Long, version: Long): Boolean =
            this.visitorData == visitorData && this.generation == generation && this.version == version

        val isUsable: Boolean
            get() {
                if (failed) return false
                val current = ready ?: return true
                return !current.runtime.isExpired
            }
    }

    private class ReadyPoTokenRuntime(
        val runtime: YoutubePoTokenRuntime,
        val playerToken: String
    )

    private companion object {
        const val RETIRE_GRACE_MS = 15_000L
        const val REFRESH_RETRY_COOLDOWN_MS = 15_000L
    }
}

internal fun localizePoTokenBuildFailure(error: Throwable): Throwable = when (error) {
    is CancellationException -> error
    is YoutubePoTokenRuntimeUnavailableException -> error
    else -> YoutubePoTokenRuntimeUnavailableException("Integrity runtime build failed", error)
}

private data class PoTokenBudgetResult<T>(val value: T)

internal suspend fun <T> withPoTokenWaitBudget(
    waitBudgetMs: Long,
    block: suspend () -> T
): T {
    require(waitBudgetMs > 0L) { "PO Token wait budget must be positive" }
    val result = withTimeoutOrNull(waitBudgetMs) {
        PoTokenBudgetResult(block())
    }
    return result?.value
        ?: throw YoutubePoTokenRuntimeUnavailableException(
            "Integrity runtime or token not ready within $waitBudgetMs ms"
        )
}

internal fun safeElapsedDeadline(nowElapsedMs: Long, budgetMs: Long): Long {
    require(budgetMs > 0L) { "PO Token wait budget must be positive" }
    return if (nowElapsedMs > Long.MAX_VALUE - budgetMs) Long.MAX_VALUE else nowElapsedMs + budgetMs
}

internal fun remainingPoTokenWaitBudget(
    deadlineElapsedMs: Long,
    fallbackBudgetMs: Long,
    nowElapsedMs: Long
): Long {
    require(fallbackBudgetMs > 0L) { "Fallback PO Token budget must be positive" }
    if (deadlineElapsedMs == Long.MAX_VALUE) return fallbackBudgetMs
    return (deadlineElapsedMs - nowElapsedMs).coerceAtLeast(0L)
}

internal enum class PoTokenSessionAction { REUSE, BACKOFF, BUILD }

internal object PoTokenSessionPolicy {
    fun decide(
        hasSession: Boolean,
        discarded: Boolean,
        matches: Boolean,
        usable: Boolean,
        nowMs: Long,
        nextBuildAttemptAtMs: Long
    ): PoTokenSessionAction {
        if (hasSession && !discarded && matches && usable) return PoTokenSessionAction.REUSE
        if (nowMs < nextBuildAttemptAtMs) return PoTokenSessionAction.BACKOFF
        return PoTokenSessionAction.BUILD
    }
}

internal object PoTokenReplacementPolicy {
    fun shouldJoin(
        hasReplacement: Boolean,
        replacementMatches: Boolean,
        replacementUsable: Boolean,
        hasActive: Boolean,
        activeDiscarded: Boolean,
        activeMatches: Boolean,
        activeUsable: Boolean
    ): Boolean {
        if (!hasReplacement || !replacementMatches || !replacementUsable) return false
        return !hasActive || activeDiscarded || !activeMatches || !activeUsable
    }
}

internal object PoTokenInvalidationPolicy {
    fun shouldRetire(sessionVersion: Long, invalidatedVersion: Long): Boolean =
        sessionVersion < invalidatedVersion
}

internal object PoTokenBuildFailurePolicy {
    fun shouldArmBackoff(cancellation: Boolean, ownerActive: Boolean): Boolean =
        !cancellation || ownerActive
}

internal object PoTokenBackoff {
    private const val BASE_MS = 2_000L
    private const val MAX_MS = 60_000L
    private const val MAX_SHIFT = 5

    fun delayMs(consecutiveFailures: Int): Long {
        if (consecutiveFailures <= 0) return 0L
        val shift = (consecutiveFailures - 1).coerceAtMost(MAX_SHIFT)
        return (BASE_MS shl shift).coerceAtMost(MAX_MS)
    }
}

@Keep
internal class YoutubePoTokenRuntime private constructor(
    context: Context,
    private val httpClient: OkHttpClient
) {
    private val webView = WebView(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ready = CompletableDeferred<Unit>()
    private val tokenWaiters = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val tokenCache = BoundedPoTokenCache(MAX_TOKEN_CACHE_ENTRIES)
    private val inFlightTokens = ConcurrentHashMap<String, Deferred<String>>()
    private val closed = AtomicBoolean(false)
    private val dead = AtomicBoolean(false)
    private val initializationStarted = AtomicBoolean(false)

    @Volatile
    private var expiresAtMs = 0L

    @Volatile
    private var refreshAtMs = 0L

    init {
        webView.settings.javaScriptEnabled = true
        webView.settings.userAgentString = USER_AGENT
        webView.settings.blockNetworkLoads = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.settings.javaScriptCanOpenWindowsAutomatically = false
        webView.settings.setSupportMultipleWindows(false)
        webView.settings.domStorageEnabled = false
        webView.addJavascriptInterface(this, JS_INTERFACE)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    Timber.w("PO Token JS: %s", consoleMessage.message())
                }
                return true
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                downloadAndRunBotguard()
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                markDead(IllegalStateException("PO Token render process gone"))
                return true
            }
        }
        webView.loadDataWithBaseURL("https://www.youtube.com", HTML, "text/html", "utf-8", null)
    }

    val isExpired: Boolean
        get() = closed.get() || dead.get() || expiresAtMs <= System.currentTimeMillis()

    val isStale: Boolean
        get() = isExpired || refreshAtMs <= System.currentTimeMillis()

    fun cachedToken(identifier: String): String? {
        if (isUnavailable()) return null
        val binding = identifier.trim()
        if (binding.isEmpty() || binding.length > MAX_BINDING_LENGTH) return null
        return tokenCache.get(binding)
    }

    suspend fun generate(identifier: String): String {
        val binding = identifier.trim()
        require(binding.isNotEmpty() && binding.length <= MAX_BINDING_LENGTH) { "Invalid PO Token binding" }
        tokenCache.get(binding)?.let { return it }
        ensureActive()
        return try {
            mintJob(binding).await()
        } catch (error: CancellationException) {
            if (currentCoroutineContext().isActive) {
                throw YoutubePoTokenRuntimeUnavailableException("Integrity runtime closed while minting", error)
            }
            throw error
        }
    }

    private fun mintJob(binding: String): Deferred<String> {
        inFlightTokens[binding]?.takeIf { !it.isCompleted }?.let { return it }
        synchronized(inFlightTokens) {
            inFlightTokens[binding]?.takeIf { !it.isCompleted }?.let { return it }
            val job = scope.async { mintToken(binding) }
            inFlightTokens[binding] = job
            job.invokeOnCompletion { inFlightTokens.remove(binding, job) }
            return job
        }
    }

    private suspend fun mintToken(binding: String): String {
        ensureActive()
        try {
            withTimeout(INIT_TIMEOUT_MS) { ready.await() }
        } catch (error: TimeoutCancellationException) {
            markDead(error)
            throw error
        } catch (error: CancellationException) {
            throw error
        }
        ensureActive()
        val requestId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<String>()
        tokenWaiters[requestId] = deferred
        try {
            withContext(Dispatchers.Main.immediate) {
                ensureActive()
                val identifierBytes = jsUint8Array(binding.toByteArray(StandardCharsets.UTF_8))
                webView.evaluateJavascript(
                    "try{obtainPoToken($identifierBytes).then(function(v){$JS_INTERFACE.onToken(${JSONObject.quote(requestId)},Array.from(v).join(','));}).catch(function(e){$JS_INTERFACE.onTokenError(${JSONObject.quote(requestId)},String(e));});}catch(e){$JS_INTERFACE.onTokenError(${JSONObject.quote(requestId)},String(e));}",
                    null
                )
            }
            val token = try {
                withTimeout(TOKEN_TIMEOUT_MS) { deferred.await() }
            } catch (error: TimeoutCancellationException) {
                markDead(error)
                throw error
            }
            if (!isValidPoToken(token)) {
                val error = IllegalStateException("Invalid PO Token output")
                markDead(error)
                throw error
            }
            tokenCache.put(binding, token)
            return token
        } finally {
            tokenWaiters.remove(requestId)
        }
    }

    @JavascriptInterface
    fun downloadAndRunBotguard() {
        if (isUnavailable() || !initializationStarted.compareAndSet(false, true)) return
        scope.launch {
            try {
                val challenge = requestBotguard(CREATE_URL, JSONArray().put(REQUEST_KEY).toString())
                val parsed = parseChallengeData(challenge)
                webView.evaluateJavascript(
                    "try{const data=$parsed;runBotGuard(data).then(function(r){window.__levyraWebPoSignalOutput=r.webPoSignalOutput;$JS_INTERFACE.onBotguardResult(String(r.botguardResponse));}).catch(function(e){$JS_INTERFACE.onInitError(String(e));});}catch(e){$JS_INTERFACE.onInitError(String(e));}",
                    null
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                failInitialization(error)
            }
        }
    }

    @JavascriptInterface
    fun onBotguardResult(response: String) {
        if (isUnavailable()) return
        if (response.isBlank()) {
            failInitialization(IllegalStateException("Empty BotGuard response"))
            return
        }
        scope.launch {
            try {
                val body = JSONArray().put(REQUEST_KEY).put(response).toString()
                val integrityResponse = requestBotguard(GENERATE_URL, body)
                val parsed = parseIntegrityTokenData(integrityResponse)
                val issuedAt = System.currentTimeMillis()
                expiresAtMs = safeExpiryAt(issuedAt, parsed.second)
                refreshAtMs = refreshDeadline(issuedAt, expiresAtMs)
                webView.evaluateJavascript(
                    "try{createPoTokenMinter(window.__levyraWebPoSignalOutput,${parsed.first}).then(function(){$JS_INTERFACE.onMinterReady();}).catch(function(e){$JS_INTERFACE.onInitError(String(e));});}catch(e){$JS_INTERFACE.onInitError(String(e));}",
                    null
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                failInitialization(error)
            }
        }
    }

    @JavascriptInterface
    fun onMinterReady() {
        if (!isUnavailable()) ready.complete(Unit)
    }

    @JavascriptInterface
    fun onInitError(error: String) {
        failInitialization(IllegalStateException(error.ifBlank { "PO Token initialization failed" }))
    }

    @JavascriptInterface
    fun onToken(requestId: String, bytes: String) {
        val result = runCatching { byteCsvToBase64Url(bytes) }
        result.onSuccess { token ->
            if (isValidPoToken(token)) {
                tokenWaiters[requestId]?.complete(token)
            } else {
                tokenWaiters[requestId]?.completeExceptionally(IllegalStateException("Invalid PO Token"))
            }
        }.onFailure { tokenWaiters[requestId]?.completeExceptionally(it) }
    }

    @JavascriptInterface
    fun onTokenError(requestId: String, error: String) {
        tokenWaiters[requestId]?.completeExceptionally(
            IllegalStateException(error.ifBlank { "PO Token generation failed" })
        )
    }

    suspend fun close() {
        if (!closed.compareAndSet(false, true)) return
        dead.set(true)
        scope.cancel()
        val error = IllegalStateException("PO Token runtime chiuso")
        if (!ready.isCompleted) ready.completeExceptionally(error)
        tokenWaiters.values.forEach { it.completeExceptionally(error) }
        tokenWaiters.clear()
        tokenCache.clear()
        withContext(NonCancellable + Dispatchers.Main.immediate) {
            runCatching {
                webView.removeJavascriptInterface(JS_INTERFACE)
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.clearHistory()
                webView.removeAllViews()
                webView.destroy()
            }
        }
    }

    private suspend fun requestBotguard(url: String, data: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .post(data.toRequestBody())
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json+protobuf")
            .header("x-goog-api-key", botguardApiKey)
            .header("x-user-agent", "grpc-web-javascript/0.1")
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                throw YoutubePlayerRequestException(response.code, "BotGuard HTTP ${response.code}")
            }
            requireNonBlankBotguardBody(body)
        }
    }

    private fun ensureActive() {
        if (isUnavailable()) throw IllegalStateException("PO Token runtime unavailable")
    }

    private fun isUnavailable(): Boolean = closed.get() || dead.get()

    private fun failInitialization(error: Throwable) {
        markDead(error)
    }

    private fun markDead(error: Throwable) {
        if (!dead.compareAndSet(false, true) && ready.isCompleted) return
        if (!ready.isCompleted) ready.completeExceptionally(error)
        tokenWaiters.values.forEach { it.completeExceptionally(error) }
        tokenWaiters.clear()
        tokenCache.clear()
    }

    private class BoundedPoTokenCache(private val maxEntries: Int) {
        private val values = object : LinkedHashMap<String, String>(maxEntries, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
                return size > maxEntries
            }
        }

        @Synchronized
        fun get(key: String): String? = values[key]

        @Synchronized
        fun put(key: String, value: String) {
            values[key] = value
        }

        @Synchronized
        fun clear() {
            values.clear()
        }
    }

    companion object {
        private const val JS_INTERFACE = "LevyraPoToken"
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private val botguardApiKey = "AIzaSyDyT5W0Jh49F30Pqq" + "tyfdf7pDLFKLJoAnw"
        private const val CREATE_URL = "https://www.youtube.com/api/jnn/v1/Create"
        private const val GENERATE_URL = "https://www.youtube.com/api/jnn/v1/GenerateIT"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private const val INIT_TIMEOUT_MS = 20_000L
        private const val TOKEN_TIMEOUT_MS = 12_000L
        private const val MAX_BINDING_LENGTH = 4096
        private const val MAX_TOKEN_CACHE_ENTRIES = 64
        private const val MIN_TOKEN_LENGTH = 16
        private const val MAX_TOKEN_LENGTH = 8192
        private const val MIN_TTL_SECONDS = 30L
        private const val MAX_TTL_SECONDS = 86_400L
        private val TOKEN_PATTERN = Regex("^[A-Za-z0-9_-]+$")
        private val HTML = """
            <!doctype html><html><head><meta charset="utf-8"><script>
            let bgVmFunctions=null;let bgVm=null;let bgProgram=null;let poTokenMinter=null;
            function loadBotGuard(challengeData){bgVm=window[challengeData.globalName];bgProgram=challengeData.program;bgVmFunctions=null;if(!bgVm||!bgVm.a)throw new Error('BotGuard VM unavailable');const cb=function(a,b,c,d){bgVmFunctions={asyncSnapshotFunction:a,shutdownFunction:b,passEventFunction:c,checkCameraFunction:d};};bgVm.a(bgProgram,cb,true,undefined,function(){},[[],[]]);return new Promise(function(resolve,reject){let n=0;const timer=setInterval(function(){if(bgVmFunctions&&bgVmFunctions.asyncSnapshotFunction){clearInterval(timer);resolve({vmFunctions:bgVmFunctions});}else if(++n>=10000){clearInterval(timer);reject(new Error('BotGuard initialization timeout'));}},1);});}
            function snapshot(botguard,args){return new Promise(function(resolve,reject){try{botguard.vmFunctions.asyncSnapshotFunction(function(value){resolve(value);},[args.contentBinding,args.signedTimestamp,args.webPoSignalOutput,args.skipPrivacyBuffer]);}catch(e){reject(e);}});}
            function runBotGuard(challengeData){const code=challengeData.interpreterJavascript.privateDoNotAccessOrElseSafeScriptWrappedValue;if(!code)throw new Error('BotGuard interpreter unavailable');new Function(code)();const output=[];return loadBotGuard({globalName:challengeData.globalName,program:challengeData.program}).then(function(botguard){return snapshot(botguard,{webPoSignalOutput:output});}).then(function(response){return{webPoSignalOutput:output,botguardResponse:response};});}
            async function createPoTokenMinter(output,integrityToken){if(!output||typeof output[0]!=='function')throw new Error('PO Token minter factory unavailable');const candidate=output[0](integrityToken);poTokenMinter=candidate&&typeof candidate.then==='function'?await candidate:candidate;if(typeof poTokenMinter!=='function')throw new Error('PO Token minter unavailable');}
            async function obtainPoToken(identifier){if(typeof poTokenMinter!=='function')throw new Error('PO Token minter not initialized');const candidate=poTokenMinter(identifier);const result=candidate&&typeof candidate.then==='function'?await candidate:candidate;if(!(result instanceof Uint8Array)||result.length===0)throw new Error('Invalid PO Token result');return result;}
            </script></head><body></body></html>
        """.trimIndent()

        suspend fun create(context: Context, httpClient: OkHttpClient): YoutubePoTokenRuntime = withContext(Dispatchers.Main.immediate) {
            val runtime = YoutubePoTokenRuntime(context.applicationContext, httpClient)
            try {
                withTimeout(INIT_TIMEOUT_MS) { runtime.ready.await() }
                runtime
            } catch (error: Throwable) {
                runtime.close()
                throw error
            }
        }

        internal fun requireNonBlankBotguardBody(raw: String): String {
            return raw.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("BotGuard returned an empty response")
        }

        internal fun parseChallengeData(raw: String): String {
            requireNonBlankBotguardBody(raw)
            val scrambled = JSONArray(raw)
            val challenge = if (scrambled.length() > 1 && scrambled.opt(1) is String) {
                JSONArray(descramble(scrambled.getString(1)))
            } else {
                scrambled.optJSONArray(1) ?: throw IllegalStateException("Challenge BotGuard non valido")
            }
            val messageId = challenge.optString(0)
            val interpreterJavascript = firstString(challenge.optJSONArray(1))
            val interpreterUrl = firstString(challenge.optJSONArray(2))
            val interpreterHash = challenge.optString(3)
            val program = challenge.optString(4)
            val globalName = challenge.optString(5)
            if (
                messageId.isBlank() ||
                interpreterJavascript.isBlank() ||
                interpreterHash.isBlank() ||
                program.isBlank() ||
                globalName.isBlank()
            ) {
                throw IllegalStateException("Challenge BotGuard incompleto")
            }
            return JSONObject()
                .put("messageId", messageId)
                .put(
                    "interpreterJavascript",
                    JSONObject()
                        .put("privateDoNotAccessOrElseSafeScriptWrappedValue", interpreterJavascript)
                        .put("privateDoNotAccessOrElseTrustedResourceUrlWrappedValue", interpreterUrl)
                )
                .put("interpreterHash", interpreterHash)
                .put("program", program)
                .put("globalName", globalName)
                .put("clientExperimentsStateBlob", challenge.optString(7))
                .toString()
        }

        internal fun parseIntegrityTokenData(raw: String): Pair<String, Long> {
            requireNonBlankBotguardBody(raw)
            val array = JSONArray(raw)
            if (array.length() == 0) throw IllegalStateException("Integrity token assente")
            val bytes = decodeYoutubeBase64(array.getString(0))
            if (bytes.isEmpty()) throw IllegalStateException("Integrity token vuoto")
            val ttl = array.optLong(1, 3600L)
            if (ttl !in MIN_TTL_SECONDS..MAX_TTL_SECONDS) {
                throw IllegalStateException("Integrity token TTL non valido")
            }
            return jsUint8Array(bytes) to ttl
        }

        internal fun refreshDeadline(issuedAtMs: Long, expiresAtMs: Long): Long {
            if (expiresAtMs <= issuedAtMs) return expiresAtMs
            return issuedAtMs + (expiresAtMs - issuedAtMs) * 3L / 4L
        }

        internal fun safeExpiryAt(nowMs: Long, ttlSeconds: Long): Long {
            val boundedTtl = ttlSeconds.coerceIn(MIN_TTL_SECONDS, MAX_TTL_SECONDS)
            val margin = minOf(600L, maxOf(15L, boundedTtl / 10L))
            val usable = maxOf(1L, boundedTtl - margin)
            return nowMs + usable * 1000L
        }

        internal fun isValidPoToken(value: String): Boolean {
            return value.length in MIN_TOKEN_LENGTH..MAX_TOKEN_LENGTH && TOKEN_PATTERN.matches(value)
        }

        private fun firstString(array: JSONArray?): String {
            if (array == null) return ""
            for (index in 0 until array.length()) {
                val value = array.opt(index)
                if (value is String && value.isNotBlank()) return value
            }
            return ""
        }

        private fun descramble(value: String): String {
            val bytes = decodeYoutubeBase64(value)
            val decoded = bytes.map { ((it.toInt() + 97) and 0xFF).toByte() }.toByteArray()
            return String(decoded, StandardCharsets.UTF_8)
        }

        private fun decodeYoutubeBase64(value: String): ByteArray {
            if (value.isBlank()) throw IllegalArgumentException("Empty base64 payload")
            var normalized = value.replace('-', '+').replace('_', '/').replace('.', '=')
            val padding = (4 - normalized.length % 4) % 4
            normalized += "=".repeat(padding)
            return Base64.decode(normalized, Base64.DEFAULT)
        }

        private fun jsUint8Array(bytes: ByteArray): String {
            return "new Uint8Array([${bytes.joinToString(",") { (it.toInt() and 0xFF).toString() }}])"
        }

        private fun byteCsvToBase64Url(value: String): String {
            val parts = value.split(',').filter { it.isNotBlank() }
            if (parts.isEmpty()) throw IllegalArgumentException("Empty PO Token bytes")
            val bytes = parts.map { part ->
                val number = part.trim().toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid PO Token byte")
                if (number !in 0..255) throw IllegalArgumentException("PO Token byte out of range")
                number.toByte()
            }.toByteArray()
            return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }
    }
}
