package com.luc4n3x.levyra.data

import android.content.Context
import android.util.Base64
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import com.luc4n3x.levyra.domain.LevyraContentLocales
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.util.LinkedHashMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal data class YoutubeGuestSession(
    val visitorData: String,
    val generation: Long
)

internal data class YoutubePoTokens(
    val playerToken: String,
    val streamingToken: String
)

internal class YoutubePlayerRequestException(
    val httpCode: Int?,
    message: String
) : IllegalStateException(message)

internal class YoutubePlaybackSecurity(
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

    fun cachedSession(): YoutubeGuestSession {
        return YoutubeGuestSession(
            visitorData = prefs.getString(KEY_VISITOR_DATA, "").orEmpty(),
            generation = prefs.getLong(KEY_GENERATION, 0L)
        )
    }

    suspend fun currentSession(): YoutubeGuestSession = sessionMutex.withLock {
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
        tokenGenerator.invalidate()
    }

    suspend fun poTokens(videoId: String, session: YoutubeGuestSession): YoutubePoTokens? {
        if (videoId.isBlank() || session.visitorData.isBlank()) return null
        return try {
            tokenGenerator.generate(videoId, session.visitorData, session.generation)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "PO Token generation failed")
            null
        }
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

    private suspend fun fetchVisitorData(): String = withContext(Dispatchers.IO) {
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
        httpClient.newCall(request).execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) throw YoutubePlayerRequestException(response.code, "visitor_id HTTP ${response.code}")
            JSONObject(text)
                .optJSONObject("responseContext")
                ?.optString("visitorData")
                .orEmpty()
                .ifBlank { throw IllegalStateException("visitorData assente") }
        }
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
    }
}

private class YoutubeWebPoTokenGenerator(
    private val context: Context,
    private val httpClient: OkHttpClient
) {
    private val mutex = Mutex()
    private val closeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val invalidationVersion = AtomicLong(0L)
    private var runtime: YoutubePoTokenRuntime? = null
    private var runtimeVersion = -1L
    private var sessionId = ""
    private var sessionGeneration = -1L
    private var playerRequestToken = ""

    suspend fun generate(videoId: String, visitorData: String, generation: Long): YoutubePoTokens {
        return mutex.withLock {
            val version = invalidationVersion.get()
            val recreate = runtime == null ||
                runtime?.isExpired == true ||
                runtimeVersion != version ||
                sessionId != visitorData ||
                sessionGeneration != generation
            if (recreate) recreate(visitorData, generation, version)
            var active = runtime ?: throw IllegalStateException("Runtime PO Token assente")
            val streamingToken = try {
                active.generate(videoId)
            } catch (error: TimeoutCancellationException) {
                recreate(visitorData, generation, invalidationVersion.get())
                active = runtime ?: throw error
                active.generate(videoId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                recreate(visitorData, generation, invalidationVersion.get())
                active = runtime ?: throw error
                active.generate(videoId)
            }
            YoutubePoTokens(
                playerToken = playerRequestToken,
                streamingToken = streamingToken
            )
        }
    }

    fun invalidate() {
        invalidationVersion.incrementAndGet()
        closeScope.launch {
            mutex.withLock { clearRuntime() }
        }
    }

    private suspend fun recreate(visitorData: String, generation: Long, version: Long) {
        clearRuntime()
        val fresh = YoutubePoTokenRuntime.create(context, httpClient)
        val sessionToken = try {
            fresh.generate(visitorData)
        } catch (error: Throwable) {
            fresh.close()
            throw error
        }
        runtime = fresh
        runtimeVersion = version
        sessionId = visitorData
        sessionGeneration = generation
        playerRequestToken = sessionToken
    }

    private suspend fun clearRuntime() {
        val old = runtime
        runtime = null
        runtimeVersion = -1L
        sessionId = ""
        sessionGeneration = -1L
        playerRequestToken = ""
        old?.close()
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
    private val closed = AtomicBoolean(false)
    private val dead = AtomicBoolean(false)
    private val initializationStarted = AtomicBoolean(false)

    @Volatile
    private var expiresAtMs = 0L

    init {
        webView.settings.javaScriptEnabled = true
        webView.settings.userAgentString = USER_AGENT
        webView.settings.blockNetworkLoads = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.settings.javaScriptCanOpenWindowsAutomatically = false
        webView.settings.setSupportMultipleWindows(false)
        webView.settings.domStorageEnabled = false
        webView.settings.databaseEnabled = false
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

    suspend fun generate(identifier: String): String {
        val binding = identifier.trim()
        require(binding.isNotEmpty() && binding.length <= MAX_BINDING_LENGTH) { "Invalid PO Token binding" }
        tokenCache.get(binding)?.let { return it }
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
                expiresAtMs = safeExpiryAt(System.currentTimeMillis(), parsed.second)
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
