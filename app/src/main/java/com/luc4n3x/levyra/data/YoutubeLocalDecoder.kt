package com.luc4n3x.levyra.data

import android.content.Context
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.services.youtube.YoutubeApiDecoder
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptDecoder
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class YoutubeLocalDecoder private constructor(
    context: Context,
    httpClient: OkHttpClient
) : YoutubeJavaScriptDecoder {
    private val engine = YoutubeLocalDecoderEngine(context.applicationContext, httpClient)

    override fun getPlayerData(videoId: String): YoutubeJavaScriptDecoder.PlayerData {
        return blocking("player metadata") { engine.playerData() }
    }

    override fun decodeBatch(
        playerId: String,
        signatures: MutableList<String>?,
        throttlingParameters: MutableList<String>?
    ): YoutubeApiDecoder.BatchDecodeResult {
        return blocking("batch decode") {
            engine.decodeBatch(
                playerId = playerId,
                signatures = signatures.orEmpty(),
                throttlingParameters = throttlingParameters.orEmpty()
            )
        }
    }

    private fun <T> blocking(label: String, block: suspend () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw ParsingException("Local YouTube decoder invoked on the main thread: $label")
        }
        return try {
            runBlocking(Dispatchers.IO) {
                withTimeout(BLOCKING_TIMEOUT_MS) { block() }
            }
        } catch (error: ParsingException) {
            throw error
        } catch (error: CancellationException) {
            throw ParsingException("Local YouTube decoder cancelled during $label", error)
        } catch (error: Throwable) {
            throw ParsingException("Local YouTube decoder failed during $label", error)
        }
    }

    private suspend fun prewarmInternal() {
        engine.prewarm()
    }

    private suspend fun rejectionInternal(source: String) {
        engine.onStreamRejected(source)
    }

    private suspend fun trimInternal() {
        engine.trimMemory()
    }

    companion object {
        private const val BLOCKING_TIMEOUT_MS = 7_000L
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Volatile
        private var instance: YoutubeLocalDecoder? = null

        fun install(context: Context): YoutubeLocalDecoder {
            return instance ?: synchronized(this) {
                instance ?: YoutubeLocalDecoder(
                    context = context.applicationContext,
                    httpClient = LevyraHttpClientFactory.youtubePlayer()
                ).also { decoder ->
                    instance = decoder
                    YoutubeApiDecoder.setLocalDecoder(decoder)
                }
            }
        }

        suspend fun prewarm() {
            instance?.prewarmInternal()
        }

        fun notifyStreamRejected(source: String) {
            val decoder = instance ?: return
            scope.launch {
                runCatching { decoder.rejectionInternal(source) }
                    .onFailure { Timber.w(it, "Local decoder rejection refresh failed") }
            }
        }

        fun trimMemory() {
            val decoder = instance ?: return
            scope.launch {
                runCatching { decoder.trimInternal() }
                    .onFailure { Timber.w(it, "Local decoder trim failed") }
            }
        }
    }
}

private class YoutubeLocalDecoderEngine(
    private val context: Context,
    httpClient: OkHttpClient
) {
    private val configStore = YoutubePlayerConfigStore(context, httpClient)
    private val playerSource = YoutubePlayerJsSource(context, httpClient, configStore)
    private val runtimeMutex = Mutex()
    private val decodeCache = BoundedStringCache(768)
    @Volatile
    private var decodeCacheEpoch = -1L
    private var runtime: YoutubeCipherWebRuntime? = null
    private var runtimeHash = ""
    private var runtimeConfigKey = ""
    private var runtimeConfigEpoch = -1L
    private var runtimeConfigOrigin = YoutubePlayerConfigOrigin.VALIDATED
    private val lastSuccessfulDecodeAtMs = AtomicLong(0L)

    @Volatile
    private var lastSuccessfulDecodePlayerHash = ""

    @Volatile
    private var lastSuccessfulDecodeOrigin = YoutubePlayerConfigOrigin.VALIDATED

    suspend fun playerData(): YoutubeJavaScriptDecoder.PlayerData {
        var player = playerSource.get(forceRefresh = false)
        var config = configStore.configFor(player.configKey, refreshUnknown = true)
        if (config == null) {
            player = playerSource.get(forceRefresh = false)
            config = configStore.configFor(player.configKey, refreshUnknown = false)
        }
        val resolvedConfig = config ?: player.analyzedConfig
        val sts = player.signatureTimestamp ?: resolvedConfig?.signatureTimestamp
            ?: throw ParsingException("Signature timestamp unavailable for player ${player.hash}")
        return YoutubeJavaScriptDecoder.PlayerData(player.hash, sts)
    }

    suspend fun decodeBatch(
        playerId: String,
        signatures: List<String>,
        throttlingParameters: List<String>
    ): YoutubeApiDecoder.BatchDecodeResult {
        if (!YoutubePlayerConfigParser.isValidHash(playerId)) {
            throw ParsingException("Invalid YouTube player ID: $playerId")
        }
        val signatureInputs = signatures.filter { it.isNotBlank() }.distinct()
        val nInputs = throttlingParameters.filter { it.isNotBlank() }.distinct()
        if (signatureInputs.isEmpty() && nInputs.isEmpty()) {
            return YoutubeApiDecoder.BatchDecodeResult(emptyMap(), emptyMap())
        }

        val signatureResults = LinkedHashMap<String, String>()
        val nResults = LinkedHashMap<String, String>()
        val missingSignatures = ArrayList<String>()
        val missingNs = ArrayList<String>()

        val lookupEpoch = configStore.epoch
        if (decodeCacheEpoch != lookupEpoch) {
            decodeCache.clear()
            decodeCacheEpoch = lookupEpoch
        }
        signatureInputs.forEach { value ->
            val cached = decodeCache.get(cacheKey(lookupEpoch, playerId, "sig", value))
            if (cached == null) missingSignatures += value else signatureResults[value] = cached
        }
        nInputs.forEach { value ->
            val cached = decodeCache.get(cacheKey(lookupEpoch, playerId, "n", value))
            if (cached == null) missingNs += value else nResults[value] = cached
        }

        if (missingSignatures.isNotEmpty() || missingNs.isNotEmpty()) {
            val decoded = try {
                decodeAttempt(playerId, missingSignatures, missingNs, forceRefresh = false)
            } catch (firstError: Throwable) {
                Timber.w(firstError, "Local decoder first attempt failed for player %s", playerId)
                invalidateRuntime()
                configStore.refresh(force = true, reason = "decode-failure")
                playerSource.invalidate()
                try {
                    decodeAttempt(playerId, missingSignatures, missingNs, forceRefresh = true)
                } catch (secondError: Throwable) {
                    secondError.addSuppressed(firstError)
                    throw ParsingException("Local decode failed for player $playerId", secondError)
                }
            }
            decoded.signatures.forEach { (input, output) ->
                signatureResults[input] = output
                if (decoded.configEpoch == configStore.epoch) {
                    decodeCache.put(cacheKey(decoded.configEpoch, playerId, "sig", input), output)
                    decodeCacheEpoch = decoded.configEpoch
                }
            }
            decoded.nValues.forEach { (input, output) ->
                nResults[input] = output
                if (decoded.configEpoch == configStore.epoch) {
                    decodeCache.put(cacheKey(decoded.configEpoch, playerId, "n", input), output)
                    decodeCacheEpoch = decoded.configEpoch
                }
            }
        }

        return YoutubeApiDecoder.BatchDecodeResult(signatureResults, nResults)
    }

    suspend fun prewarm() {
        try {
            configStore.refresh(force = false, reason = "prewarm")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "Local decoder config prewarm failed")
        }
        var player = playerSource.get(forceRefresh = false)
        var config = configStore.configFor(player.configKey, refreshUnknown = true)
        if (config == null) {
            player = playerSource.get(forceRefresh = false)
            config = configStore.configFor(player.configKey, refreshUnknown = false)
        }
        val resolvedConfig = config ?: player.analyzedConfig ?: return
        runtimeMutex.withLock {
            ensureRuntime(player, resolvedConfig)
        }
    }

    suspend fun onStreamRejected(source: String) {
        val feedbackAt = lastSuccessfulDecodeAtMs.get()
        val now = System.currentTimeMillis()
        if (!YoutubeLocalDecoderFeedbackPolicy.shouldRefresh(source, now, feedbackAt)) return
        val rejectedHash = lastSuccessfulDecodePlayerHash
        val rejectedOrigin = lastSuccessfulDecodeOrigin
        if (
            rejectedOrigin == YoutubePlayerConfigOrigin.ANALYZED &&
            rejectedHash.isNotBlank() &&
            lastSuccessfulDecodeAtMs.get() == feedbackAt
        ) {
            playerSource.rejectAnalyzedConfig(rejectedHash)
        }
        when (configStore.refreshAfterStreamRejection()) {
            YoutubeStreamRefreshResult.CHANGED -> {
                decodeCache.clear()
                decodeCacheEpoch = configStore.epoch
                runtimeMutex.withLock {
                    if (lastSuccessfulDecodeAtMs.get() == feedbackAt) invalidateRuntimeLocked()
                }
            }
            YoutubeStreamRefreshResult.UNCHANGED -> runtimeMutex.withLock {
                if (lastSuccessfulDecodeAtMs.get() == feedbackAt) invalidateRuntimeLocked()
            }
            YoutubeStreamRefreshResult.SKIPPED,
            YoutubeStreamRefreshResult.NETWORK_FAILURE -> Unit
        }
    }

    suspend fun trimMemory() {
        runtimeMutex.withLock { invalidateRuntimeLocked() }
        playerSource.trimMemory()
        decodeCache.clear()
    }

    private suspend fun decodeAttempt(
        playerId: String,
        signatures: List<String>,
        nValues: List<String>,
        forceRefresh: Boolean
    ): YoutubeDecodedBatch {
        var player = playerSource.get(forceRefresh)
        if (player.hash != playerId) {
            player = playerSource.get(forceRefresh = true)
        }
        if (player.hash != playerId) {
            throw ParsingException("Player changed from $playerId to ${player.hash}")
        }
        var config = configStore.configFor(player.configKey, refreshUnknown = true)
        if (config == null) {
            player = playerSource.get(forceRefresh = false)
            config = configStore.configFor(player.configKey, refreshUnknown = false)
        }
        val resolvedConfig = config ?: player.analyzedConfig ?: throw ParsingException(
            "No validated or analyzed local config for player $playerId using key ${player.configKey}"
        )

        return try {
            runtimeMutex.withLock {
                val active = ensureRuntime(player, resolvedConfig)
                val signatureResults = LinkedHashMap<String, String>()
                val nResults = LinkedHashMap<String, String>()
                signatures.forEach { input ->
                    val output = active.decodeSignature(input)
                    if (output.isBlank()) throw ParsingException("Empty local signature result")
                    signatureResults[input] = output
                }
                nValues.forEach { input ->
                    val output = active.transformN(input)
                    if (output.isBlank()) throw ParsingException("Empty local n-transform result")
                    nResults[input] = output
                }
                lastSuccessfulDecodePlayerHash = player.hash
                lastSuccessfulDecodeOrigin = resolvedConfig.origin
                lastSuccessfulDecodeAtMs.set(System.currentTimeMillis())
                YoutubeDecodedBatch(signatureResults, nResults, runtimeConfigEpoch)
            }
        } catch (error: Throwable) {
            if (resolvedConfig.origin == YoutubePlayerConfigOrigin.ANALYZED) {
                playerSource.rejectAnalyzedConfig(player.hash)
            }
            throw error
        }
    }

    private fun cacheKey(epoch: Long, playerId: String, kind: String, value: String): String {
        return "$epoch:$playerId:$kind:$value"
    }

    private data class YoutubeDecodedBatch(
        val signatures: Map<String, String>,
        val nValues: Map<String, String>,
        val configEpoch: Long
    )

    private suspend fun ensureRuntime(
        player: YoutubePlayerScript,
        config: YoutubePlayerCipherConfig
    ): YoutubeCipherWebRuntime {
        val current = runtime
        if (
            current != null &&
            YoutubeRuntimeReusePolicy.canReuse(
                isDead = current.isDead,
                runtimeHash = runtimeHash,
                requestedHash = player.hash,
                runtimeConfigKey = runtimeConfigKey,
                requestedConfigKey = player.configKey,
                runtimeEpoch = runtimeConfigEpoch,
                currentEpoch = configStore.epoch
            )
        ) {
            return current
        }
        invalidateRuntimeLocked()
        val created = YoutubeCipherWebRuntime.create(context, player, config)
        runtime = created
        runtimeHash = player.hash
        runtimeConfigKey = player.configKey
        runtimeConfigEpoch = configStore.epoch
        runtimeConfigOrigin = config.origin
        return created
    }

    private suspend fun invalidateRuntime() {
        runtimeMutex.withLock { invalidateRuntimeLocked() }
    }

    private suspend fun invalidateRuntimeLocked() {
        val old = runtime
        runtime = null
        runtimeHash = ""
        runtimeConfigKey = ""
        runtimeConfigEpoch = -1L
        runtimeConfigOrigin = YoutubePlayerConfigOrigin.VALIDATED
        if (old != null) old.close()
    }
}

internal enum class YoutubePlayerConfigOrigin {
    VALIDATED,
    ANALYZED
}

internal data class YoutubePlayerCipherConfig(
    val primaryHash: String,
    val signatureExpression: String,
    val nClass: String?,
    val signatureTimestamp: Int,
    val nExpressionOverride: String? = null,
    val origin: YoutubePlayerConfigOrigin = YoutubePlayerConfigOrigin.VALIDATED
) {
    val nExpression: String
        get() = nExpressionOverride ?: YoutubePlayerConfigParser.buildNExpression(
            nClass ?: throw IllegalStateException("nClass unavailable")
        )

    val identity: String
        get() = listOf(
            primaryHash,
            signatureExpression,
            nClass.orEmpty(),
            signatureTimestamp.toString(),
            nExpressionOverride.orEmpty(),
            origin.name
        ).joinToString(":")
}

internal sealed class YoutubePlayerConfigParseResult {
    data class Success(
        val configs: Map<String, YoutubePlayerCipherConfig>,
        val skippedEntries: List<String>
    ) : YoutubePlayerConfigParseResult()

    data class Failure(val reason: String) : YoutubePlayerConfigParseResult()
}

internal object YoutubePlayerConfigParser {
    private val hashRegex = Regex("^[a-f0-9]{8}$")
    private val signatureRegex = Regex("^[A-Za-z0-9${'$'}_]{1,8}\\(\\d+,\\d+,INPUT\\)$")
    private val nClassRegex = Regex("^[A-Za-z0-9${'$'}_]{1,8}$")

    fun isValidHash(value: String): Boolean = hashRegex.matches(value)

    fun buildNExpression(nClass: String): String {
        require(nClassRegex.matches(nClass))
        return "(function(n){try{var u=new g.$nClass('https://x.googlevideo.com/videoplayback?n='+n,true);var t=u.get('n');return(t&&t!==n)?t:n;}catch(e){return n;}})(INPUT)"
    }

    fun parse(jsonText: String): YoutubePlayerConfigParseResult {
        val root = try {
            JSONObject(jsonText)
        } catch (error: Throwable) {
            return YoutubePlayerConfigParseResult.Failure("malformed JSON: ${error.message}")
        }
        val schemaValue = root.opt("schemaVersion")
        val schema = (schemaValue as? Number)?.toInt()
            ?: return YoutubePlayerConfigParseResult.Failure("schemaVersion missing or not an int")
        if (schema != 1) return YoutubePlayerConfigParseResult.Failure("unsupported schemaVersion $schema")
        val players = root.optJSONObject("players")
            ?: return YoutubePlayerConfigParseResult.Failure("players missing or not an object")

        val output = LinkedHashMap<String, YoutubePlayerCipherConfig>()
        val skipped = ArrayList<String>()
        val names = players.keys().asSequence().toList()
        names.forEach { hash ->
            val entry = parseEntry(hash, players.optJSONObject(hash))
            if (entry == null) {
                skipped += hash
                return@forEach
            }
            val keys = buildList {
                add(hash)
                addAll(entry.second)
            }
            val repeatedWithinEntry = keys.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.key
            val repeatedAcrossEntries = keys.firstOrNull { output.containsKey(it) }
            val duplicate = repeatedWithinEntry ?: repeatedAcrossEntries
            if (duplicate != null) {
                return YoutubePlayerConfigParseResult.Failure("duplicate hash/alias '$duplicate' in entry $hash")
            }
            keys.forEach { output[it] = entry.first }
        }
        return YoutubePlayerConfigParseResult.Success(output, skipped)
    }

    fun merge(
        bundled: Map<String, YoutubePlayerCipherConfig>,
        remote: Map<String, YoutubePlayerCipherConfig>
    ): Map<String, YoutubePlayerCipherConfig> {
        if (remote.isEmpty()) return bundled
        val overriddenPrimaryHashes = remote.values.mapTo(HashSet()) { it.primaryHash }
        return LinkedHashMap<String, YoutubePlayerCipherConfig>(bundled.size + remote.size).apply {
            bundled.forEach { (key, value) ->
                if (value.primaryHash !in overriddenPrimaryHashes) put(key, value)
            }
            putAll(remote)
        }
    }

    private fun parseEntry(
        hash: String,
        entry: JSONObject?
    ): Pair<YoutubePlayerCipherConfig, List<String>>? {
        if (!hashRegex.matches(hash) || entry == null) return null
        val sigValue = entry.opt("sig")
        val sig = sigValue as? String ?: return null
        if (!signatureRegex.matches(sig)) return null
        val nClassValue = entry.opt("nClass")
        val nClass = nClassValue as? String ?: return null
        if (!nClassRegex.matches(nClass)) return null
        val stsValue = entry.opt("sts")
        val sts = (stsValue as? Number)?.toInt() ?: return null
        if (sts <= 0) return null
        val aliases = parseAliases(entry.opt("aliases")) ?: return null
        return YoutubePlayerCipherConfig(hash, sig, nClass, sts) to aliases
    }

    private fun parseAliases(value: Any?): List<String>? {
        if (value == null || value === JSONObject.NULL) return emptyList()
        val array = value as? JSONArray ?: return null
        val aliases = ArrayList<String>(array.length())
        for (index in 0 until array.length()) {
            val alias = array.opt(index) as? String ?: return null
            if (!hashRegex.matches(alias)) return null
            aliases += alias
        }
        return aliases
    }
}

internal class YoutubeRefreshCooldowns(
    private val unknownWindowMs: Long,
    private val rejectionWindowMs: Long
) {
    private val unknownStamp = AtomicLong(0L)
    private val rejectionStamp = AtomicLong(0L)

    fun claimUnknown(now: Long): Boolean = claim(unknownStamp, now, unknownWindowMs)

    fun claimRejection(now: Long): Boolean = claim(rejectionStamp, now, rejectionWindowMs)

    fun resetUnknown() {
        unknownStamp.set(0L)
    }

    fun resetRejection() {
        rejectionStamp.set(0L)
    }

    internal fun unknownActive(now: Long): Boolean = YoutubePlayerConfigStore.withinWindow(
        now,
        unknownStamp.get(),
        unknownWindowMs
    )

    internal fun rejectionActive(now: Long): Boolean = YoutubePlayerConfigStore.withinWindow(
        now,
        rejectionStamp.get(),
        rejectionWindowMs
    )

    private fun claim(clock: AtomicLong, now: Long, cooldown: Long): Boolean {
        while (true) {
            val previous = clock.get()
            if (YoutubePlayerConfigStore.withinWindow(now, previous, cooldown)) return false
            if (clock.compareAndSet(previous, now)) return true
        }
    }
}

internal enum class YoutubeStreamRefreshResult {
    SKIPPED,
    NETWORK_FAILURE,
    UNCHANGED,
    CHANGED
}

private data class YoutubeConfigRefreshOutcome(
    val changed: Boolean,
    val reachedServer: Boolean
)

internal class YoutubePlayerConfigStore(
    private val context: Context,
    private val httpClient: OkHttpClient
) {
    private val mutex = Mutex()
    private val cacheDir = File(context.filesDir, "youtube_decoder")
    private val remoteFile = File(cacheDir, "player_configs_remote.json")
    private val metadataFile = File(cacheDir, "player_configs_meta.json")
    private val cooldowns = YoutubeRefreshCooldowns(
        UNKNOWN_REFRESH_COOLDOWN_MS,
        REJECTION_REFRESH_COOLDOWN_MS
    )
    private var bundledConfigs: Map<String, YoutubePlayerCipherConfig> = emptyMap()
    private var remoteConfigs: Map<String, YoutubePlayerCipherConfig> = emptyMap()

    @Volatile
    private var mergedConfigs: Map<String, YoutubePlayerCipherConfig> = emptyMap()

    @Volatile
    private var initialized = false

    private val epochCounter = AtomicLong(1L)

    val epoch: Long
        get() = epochCounter.get()

    suspend fun configFor(hash: String, refreshUnknown: Boolean): YoutubePlayerCipherConfig? {
        ensureInitialized()
        mergedConfigs[hash]?.let { return it }
        if (refreshUnknown) refreshUnknownPlayer(hash)
        return mergedConfigs[hash]
    }

    suspend fun refreshAfterStreamRejection(): YoutubeStreamRefreshResult {
        ensureInitialized()
        return mutex.withLock {
            val now = System.currentTimeMillis()
            if (!cooldowns.claimRejection(now)) return@withLock YoutubeStreamRefreshResult.SKIPPED
            val outcome = refreshLocked(force = true, reason = "stream-rejected")
            if (!outcome.reachedServer) {
                cooldowns.resetRejection()
                return@withLock YoutubeStreamRefreshResult.NETWORK_FAILURE
            }
            if (outcome.changed) YoutubeStreamRefreshResult.CHANGED else YoutubeStreamRefreshResult.UNCHANGED
        }
    }

    suspend fun refresh(force: Boolean, reason: String): Boolean {
        ensureInitialized()
        return mutex.withLock { refreshLocked(force, reason).changed }
    }

    private suspend fun refreshUnknownPlayer(hash: String): Boolean {
        return mutex.withLock {
            if (mergedConfigs.containsKey(hash)) return@withLock true
            val now = System.currentTimeMillis()
            if (!cooldowns.claimUnknown(now)) return@withLock false
            val outcome = refreshLocked(force = true, reason = "unknown-player-$hash")
            if (!outcome.reachedServer) cooldowns.resetUnknown()
            mergedConfigs.containsKey(hash)
        }
    }

    private suspend fun ensureInitialized() {
        if (initialized) return
        mutex.withLock {
            if (initialized) return
            val loaded = withContext(Dispatchers.IO) {
                loadBundled() to loadRemoteFromDisk()
            }
            bundledConfigs = loaded.first
            remoteConfigs = loaded.second
            mergedConfigs = YoutubePlayerConfigParser.merge(bundledConfigs, remoteConfigs)
            initialized = true
        }
    }

    private suspend fun refreshLocked(force: Boolean, reason: String): YoutubeConfigRefreshOutcome {
        val metadata = withContext(Dispatchers.IO) { readMetadata() }
        val now = System.currentTimeMillis()
        if (!force && withinWindow(now, metadata.checkedAtMs, CONFIG_TTL_MS)) {
            return YoutubeConfigRefreshOutcome(changed = false, reachedServer = false)
        }

        val request = Request.Builder()
            .url(REMOTE_CONFIG_URL)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .apply {
                metadata.etag.takeIf { it.isNotBlank() }?.let { header("If-None-Match", it) }
            }
            .build()

        val response = try {
            httpClient.awaitText(request, MAX_CONFIG_BYTES)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "Player config refresh failed reason=%s", reason)
            return YoutubeConfigRefreshOutcome(changed = false, reachedServer = false)
        }

        if (response.code == 304) {
            withContext(Dispatchers.IO) {
                runCatching { writeMetadata(metadata.copy(checkedAtMs = now)) }
                    .onFailure { Timber.w(it, "Player config metadata persistence failed") }
            }
            return YoutubeConfigRefreshOutcome(changed = false, reachedServer = true)
        }
        if (response.code !in 200..299) {
            Timber.w("Player config refresh failed HTTP %s reason=%s", response.code, reason)
            return YoutubeConfigRefreshOutcome(changed = false, reachedServer = true)
        }
        val body = response.body
        if (body.length !in 32..MAX_CONFIG_BYTES) {
            Timber.w("Player config refresh rejected size=%s reason=%s", body.length, reason)
            return YoutubeConfigRefreshOutcome(changed = false, reachedServer = true)
        }
        val parsed = YoutubePlayerConfigParser.parse(body)
        if (parsed !is YoutubePlayerConfigParseResult.Success) {
            val failure = parsed as YoutubePlayerConfigParseResult.Failure
            Timber.w("Player config refresh rejected: %s", failure.reason)
            return YoutubeConfigRefreshOutcome(changed = false, reachedServer = true)
        }
        if (parsed.configs.isEmpty()) {
            Timber.w("Player config refresh rejected empty table reason=%s", reason)
            return YoutubeConfigRefreshOutcome(changed = false, reachedServer = true)
        }

        val nextRemote = parsed.configs
        val nextMerged = YoutubePlayerConfigParser.merge(bundledConfigs, nextRemote)
        val changed = fingerprint(mergedConfigs) != fingerprint(nextMerged)
        remoteConfigs = nextRemote
        mergedConfigs = nextMerged
        if (changed) epochCounter.incrementAndGet()

        withContext(Dispatchers.IO) {
            runCatching {
                writeAtomic(remoteFile, body)
                writeMetadata(
                    YoutubeConfigMetadata(
                        etag = response.etag.ifBlank { metadata.etag },
                        checkedAtMs = now,
                        contentSha256 = sha256(body)
                    )
                )
            }.onFailure { Timber.w(it, "Player config persistence failed after in-memory update") }
        }
        Timber.d(
            "Player config refresh completed changed=%s epoch=%s entries=%s skipped=%s reason=%s",
            changed,
            epoch,
            parsed.configs.size,
            parsed.skippedEntries.size,
            reason
        )
        return YoutubeConfigRefreshOutcome(changed = changed, reachedServer = true)
    }

    private fun loadBundled(): Map<String, YoutubePlayerCipherConfig> {
        val text = context.assets.open(BUNDLED_CONFIG_ASSET).bufferedReader().use { it.readText() }
        return when (val parsed = YoutubePlayerConfigParser.parse(text)) {
            is YoutubePlayerConfigParseResult.Success -> parsed.configs.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Bundled player config is empty")
            is YoutubePlayerConfigParseResult.Failure -> throw IllegalStateException("Invalid bundled player config: ${parsed.reason}")
        }
    }

    private fun loadRemoteFromDisk(): Map<String, YoutubePlayerCipherConfig> {
        if (!remoteFile.isFile) return emptyMap()
        return runCatching {
            val body = remoteFile.readText()
            val metadata = readMetadata()
            if (metadata.contentSha256.isNotBlank() && metadata.contentSha256 != sha256(body)) {
                throw IllegalStateException("Remote player config checksum mismatch")
            }
            when (val parsed = YoutubePlayerConfigParser.parse(body)) {
                is YoutubePlayerConfigParseResult.Success -> parsed.configs.takeIf { it.isNotEmpty() }
                    ?: throw IllegalStateException("Cached remote player config is empty")
                is YoutubePlayerConfigParseResult.Failure -> throw IllegalStateException(parsed.reason)
            }
        }.onFailure {
            Timber.w(it, "Discarding invalid cached remote player config")
            remoteFile.delete()
            metadataFile.delete()
        }.getOrDefault(emptyMap())
    }

    private fun readMetadata(): YoutubeConfigMetadata {
        if (!metadataFile.isFile) return YoutubeConfigMetadata()
        return runCatching {
            val json = JSONObject(metadataFile.readText())
            YoutubeConfigMetadata(
                etag = json.optString("etag"),
                checkedAtMs = json.optLong("checkedAtMs"),
                contentSha256 = json.optString("contentSha256")
            )
        }.getOrDefault(YoutubeConfigMetadata())
    }

    private fun writeMetadata(metadata: YoutubeConfigMetadata) {
        val json = JSONObject()
            .put("etag", metadata.etag)
            .put("checkedAtMs", metadata.checkedAtMs)
            .put("contentSha256", metadata.contentSha256)
        writeAtomic(metadataFile, json.toString())
    }

    private fun fingerprint(configs: Map<String, YoutubePlayerCipherConfig>): String {
        return configs.entries
            .sortedBy { it.key }
            .joinToString("|") { (key, value) -> "$key:${value.identity}" }
    }

    companion object {
        private const val BUNDLED_CONFIG_ASSET = "player_configs.json"
        private const val REMOTE_CONFIG_URL = "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/library/src/main/assets/player_configs.json"
        private const val USER_AGENT = "Levyra/2.3.12 Android local-decoder"
        private const val CONFIG_TTL_MS = 6L * 60L * 60L * 1000L
        private const val UNKNOWN_REFRESH_COOLDOWN_MS = 60_000L
        private const val REJECTION_REFRESH_COOLDOWN_MS = 5L * 60L * 1000L
        private const val MAX_CONFIG_BYTES = 512_000

        internal fun withinWindow(now: Long, timestamp: Long, window: Long): Boolean {
            return timestamp > 0L && (now - timestamp) in 0 until window
        }

        internal fun writeAtomic(file: File, text: String) {
            file.parentFile?.mkdirs()
            val parent = file.parentFile ?: throw IOException("Missing parent for ${file.name}")
            val temp = File(parent, ".${file.name}.${UUID.randomUUID()}.tmp")
            val backup = File(parent, ".${file.name}.${UUID.randomUUID()}.bak")
            try {
                FileOutputStream(temp).use { output ->
                    output.write(text.toByteArray(StandardCharsets.UTF_8))
                    output.flush()
                    output.fd.sync()
                }
                if (!file.exists()) {
                    if (!temp.renameTo(file)) throw IOException("Unable to install ${file.name}")
                    return
                }
                if (!file.renameTo(backup)) throw IOException("Unable to preserve ${file.name}")
                if (!temp.renameTo(file)) {
                    backup.renameTo(file)
                    throw IOException("Unable to replace ${file.name}")
                }
                backup.delete()
            } finally {
                temp.delete()
                if (backup.exists() && !file.exists()) backup.renameTo(file)
                if (backup.exists() && file.exists()) backup.delete()
            }
        }
    }
}

private data class YoutubeConfigMetadata(
    val etag: String = "",
    val checkedAtMs: Long = 0L,
    val contentSha256: String = ""
)

private data class YoutubePlayerScript(
    val hash: String,
    val configKey: String,
    val javascript: String,
    val signatureTimestamp: Int?,
    val analyzedConfig: YoutubePlayerCipherConfig? = null
)

private class YoutubePlayerJsSource(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val configStore: YoutubePlayerConfigStore
) {
    private val mutex = Mutex()
    private val diskMutex = Mutex()
    private val cacheDir = File(context.filesDir, "youtube_decoder")
    private val metadataFile = File(cacheDir, "current_player.json")
    private val rejectedAnalyzers = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var memory: YoutubePlayerScript? = null

    suspend fun get(forceRefresh: Boolean): YoutubePlayerScript = mutex.withLock {
        if (!forceRefresh) {
            memory?.let { return@withLock rebindConfig(it, refreshUnknown = false) }
            withContext(Dispatchers.IO) { readDisk() }?.let { cached ->
                val rebound = rebindConfig(cached, refreshUnknown = false)
                memory = rebound
                return@withLock rebound
            }
        }
        val hash = fetchPlayerHash()
        val javascript = downloadPlayerJs(hash)
        val player = resolvePlayer(hash, javascript, refreshUnknown = true)
        writeDisk(player)
        memory = player
        player
    }

    suspend fun rejectAnalyzedConfig(hash: String) = mutex.withLock {
        rejectedAnalyzers += hash
        if (memory?.hash == hash) memory = memory?.copy(analyzedConfig = null)
    }

    private suspend fun resolvePlayer(
        hash: String,
        javascript: String,
        refreshUnknown: Boolean
    ): YoutubePlayerScript {
        val configKey = resolveConfigKey(hash, javascript, refreshUnknown) ?: hash
        val validated = configStore.configFor(configKey, refreshUnknown = false)
        val analyzed = if (validated == null && hash !in rejectedAnalyzers) {
            YoutubePlayerJsAnalyzer.analyze(hash, javascript)
        } else {
            null
        }
        val sts = extractSignatureTimestamp(javascript)
            ?: validated?.signatureTimestamp
            ?: analyzed?.signatureTimestamp
        return YoutubePlayerScript(hash, configKey, javascript, sts, analyzed)
    }

    private suspend fun rebindConfig(
        player: YoutubePlayerScript,
        refreshUnknown: Boolean
    ): YoutubePlayerScript {
        val configKey = resolveConfigKey(player.hash, player.javascript, refreshUnknown) ?: player.hash
        val validated = configStore.configFor(configKey, refreshUnknown = false)
        val analyzed = when {
            validated != null -> null
            player.hash in rejectedAnalyzers -> null
            player.analyzedConfig != null -> player.analyzedConfig
            else -> YoutubePlayerJsAnalyzer.analyze(player.hash, player.javascript)
        }
        return player.copy(
            configKey = configKey,
            signatureTimestamp = extractSignatureTimestamp(player.javascript)
                ?: validated?.signatureTimestamp
                ?: analyzed?.signatureTimestamp,
            analyzedConfig = analyzed
        )
    }

    private suspend fun resolveConfigKey(
        hash: String,
        javascript: String,
        refreshUnknown: Boolean
    ): String? {
        if (configStore.configFor(hash, refreshUnknown = false) != null) return hash
        val fingerprint = YoutubePlayerJsSupport.playerFingerprint(javascript)
        if (configStore.configFor(fingerprint, refreshUnknown = false) != null) return fingerprint
        if (!refreshUnknown) return null
        configStore.configFor(hash, refreshUnknown = true)
        return when {
            configStore.configFor(hash, refreshUnknown = false) != null -> hash
            configStore.configFor(fingerprint, refreshUnknown = false) != null -> fingerprint
            else -> null
        }
    }

    suspend fun invalidate() = mutex.withLock {
        memory = null
        diskMutex.withLock {
            withContext(Dispatchers.IO) {
                cacheDir.listFiles()
                    ?.filter { YoutubePlayerJsSupport.isPlayerJsCacheFile(it.name) || it.name == metadataFile.name }
                    ?.forEach { it.delete() }
            }
        }
    }

    suspend fun trimMemory() = mutex.withLock {
        memory = null
    }

    private fun readDisk(): YoutubePlayerScript? {
        return runCatching {
            if (!metadataFile.isFile) return null
            val json = JSONObject(metadataFile.readText())
            val hash = json.optString("hash")
            val savedAt = json.optLong("savedAt")
            if (!YoutubePlayerConfigParser.isValidHash(hash)) return null
            if (!YoutubePlayerConfigStore.withinWindow(System.currentTimeMillis(), savedAt, PLAYER_TTL_MS)) return null
            val file = File(cacheDir, "player_$hash.js")
            if (!file.isFile) return null
            val javascript = file.readText()
            if (!isValidPlayerJs(javascript)) return null
            val expected = json.optString("sha256")
            if (expected.isNotBlank() && expected != sha256(javascript)) return null
            val configKey = json.optString("configKey")
                .takeIf { YoutubePlayerConfigParser.isValidHash(it) }
                ?: hash
            val sts = json.optInt("signatureTimestamp", 0).takeIf { it > 0 }
                ?: extractSignatureTimestamp(javascript)
            YoutubePlayerScript(hash, configKey, javascript, sts)
        }.onFailure { Timber.w(it, "Cached player JS rejected") }.getOrNull()
    }

    private suspend fun writeDisk(player: YoutubePlayerScript) {
        diskMutex.withLock {
            withContext(Dispatchers.IO) {
                cacheDir.mkdirs()
                val file = File(cacheDir, "player_${player.hash}.js")
                YoutubePlayerConfigStore.writeAtomic(file, player.javascript)
                val persisted = file.readText()
                if (!isValidPlayerJs(persisted) || sha256(persisted) != sha256(player.javascript)) {
                    throw IOException("Player JS cache verification failed")
                }
                val metadata = JSONObject()
                    .put("hash", player.hash)
                    .put("configKey", player.configKey)
                    .put("savedAt", System.currentTimeMillis())
                    .put("signatureTimestamp", player.signatureTimestamp ?: 0)
                    .put("sha256", sha256(player.javascript))
                YoutubePlayerConfigStore.writeAtomic(metadataFile, metadata.toString())
                prunePlayerFiles(player.hash)
            }
        }
    }

    private fun prunePlayerFiles(currentHash: String) {
        val files = cacheDir.listFiles()
            ?.filter { YoutubePlayerJsSupport.isPlayerJsCacheFile(it.name) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        val keep = files.filter { it.name == "player_$currentHash.js" }.toMutableList()
        files.filterNot { it.name == "player_$currentHash.js" }
            .take(MAX_CACHED_PLAYERS - keep.size)
            .forEach(keep::add)
        val keepPaths = keep.mapTo(HashSet()) { it.absolutePath }
        files.filterNot { it.absolutePath in keepPaths }.forEach { it.delete() }
    }

    private suspend fun fetchPlayerHash(): String {
        val request = Request.Builder()
            .url(IFRAME_API_URL)
            .get()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .build()
        val response = httpClient.awaitText(request, MAX_IFRAME_BYTES)
        if (response.code !in 200..299) throw ParsingException("iframe_api HTTP ${response.code}")
        return YoutubePlayerJsSupport.extractPlayerHash(response.body)
            ?: throw ParsingException("Unable to extract YouTube player hash")
    }

    private suspend fun downloadPlayerJs(hash: String): String {
        val failures = ArrayList<String>()
        PLAYER_LOCALES.forEach { locale ->
            val url = "https://www.youtube.com/s/player/$hash/player_ias.vflset/$locale/base.js"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .build()
            try {
                val response = httpClient.awaitText(request, MAX_PLAYER_JS_BYTES)
                if (response.code !in 200..299) throw ParsingException("HTTP ${response.code}")
                if (!isValidPlayerJs(response.body)) throw ParsingException("invalid player JS")
                return response.body
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                failures += "$locale:${error.message.orEmpty()}"
            }
        }
        throw ParsingException("Unable to download player JS for $hash (${failures.joinToString()})")
    }

    companion object {
        private const val IFRAME_API_URL = "https://www.youtube.com/iframe_api"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/150.0.0.0 Mobile Safari/537.36"
        private const val PLAYER_TTL_MS = 6L * 60L * 60L * 1000L
        private const val MAX_IFRAME_BYTES = 1_000_000
        private const val MAX_PLAYER_JS_BYTES = 6_000_000
        private const val MAX_CACHED_PLAYERS = 2
        private val PLAYER_LOCALES = listOf("en_GB", "en_US", "it_IT")

        private fun isValidPlayerJs(value: String): Boolean {
            return value.length >= 100_000 && value.contains("_yt_player") && value.contains("})(_yt_player);")
        }

        private fun extractSignatureTimestamp(javascript: String): Int? {
            return YoutubePlayerJsAnalyzer.extractSignatureTimestamp(javascript)
        }
    }
}

internal object YoutubePlayerJsAnalyzer {
    private data class Rule(
        val regex: Regex,
        val expression: (MatchResult) -> String?
    )

    private val signatureRules = listOf(
        Rule(
            Regex("&&\\s*\\(\\s*[A-Za-z0-9_$]+\\s*=\\s*([A-Za-z0-9_$]+)\\s*\\(\\s*(\\d+)\\s*,\\s*decodeURIComponent\\s*\\(\\s*[A-Za-z0-9_$]+\\s*\\)"),
            { match -> safeName(match.groupValues[1])?.let { "$it(${match.groupValues[2]},INPUT)" } }
        ),
        Rule(
            Regex("\\b[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\(([A-Za-z0-9_$]+)\\("),
            { match -> safeName(match.groupValues[1])?.let { "$it(INPUT)" } }
        ),
        Rule(
            Regex("\\b[A-Za-z0-9_$]+\\s*&&\\s*[A-Za-z0-9_$]+\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\(([A-Za-z0-9_$]+)\\("),
            { match -> safeName(match.groupValues[1])?.let { "$it(INPUT)" } }
        ),
        Rule(
            Regex("\\bm=([A-Za-z0-9_$]{2,})\\(decodeURIComponent\\(h\\.s\\)\\)"),
            { match -> safeName(match.groupValues[1])?.let { "$it(INPUT)" } }
        ),
        Rule(
            Regex("\\bc\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*(?:encodeURIComponent\\s*\\()?([A-Za-z0-9_$]+)\\("),
            { match -> safeName(match.groupValues[1])?.let { "$it(INPUT)" } }
        )
    )

    private val nRules = listOf(
        Rule(
            Regex("\\.get\\(\\\"n\\\"\\)\\)&&\\(b=([A-Za-z0-9_$]+)(?:\\[(\\d+)])?\\([A-Za-z0-9_$]\\)"),
            { match -> functionExpression(match.groupValues[1], match.groupValues.getOrNull(2), "INPUT") }
        ),
        Rule(
            Regex("\\.get\\(\\\"n\\\"\\)\\)\\s*&&\\s*\\(([A-Za-z0-9_$]+)\\s*=\\s*([A-Za-z0-9_$]+)(?:\\[(\\d+)])?\\(\\1\\)"),
            { match -> functionExpression(match.groupValues[2], match.groupValues.getOrNull(3), "INPUT") }
        ),
        Rule(
            Regex("([A-Za-z0-9_$]+)\\s*=\\s*function\\([A-Za-z0-9_$]\\)\\s*\\{[^}]{0,2000}?enhanced_except_"),
            { match -> functionExpression(match.groupValues[1], null, "INPUT") }
        )
    )

    private val anchoredSts = Regex("signatureTimestamp['\\\":\\s]+(\\d{4,8})")
    private val looseSts = Regex("[,{]sts\\s*:\\s*(\\d{4,8})")
    private val safeFunctionName = Regex("^[A-Za-z_$][A-Za-z0-9_$]{0,31}$")

    fun analyze(hash: String, javascript: String): YoutubePlayerCipherConfig? {
        if (!YoutubePlayerConfigParser.isValidHash(hash)) return null
        val signature = uniqueExpression(javascript, signatureRules) ?: return null
        val nExpression = uniqueExpression(javascript, nRules) ?: return null
        val sts = extractSignatureTimestamp(javascript) ?: return null
        return YoutubePlayerCipherConfig(
            primaryHash = hash,
            signatureExpression = signature,
            nClass = null,
            signatureTimestamp = sts,
            nExpressionOverride = nExpression,
            origin = YoutubePlayerConfigOrigin.ANALYZED
        )
    }

    fun extractSignatureTimestamp(javascript: String): Int? {
        return anchoredSts.find(javascript)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: looseSts.find(javascript)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun uniqueExpression(javascript: String, rules: List<Rule>): String? {
        val expressions = LinkedHashSet<String>()
        for (rule in rules) {
            rule.regex.findAll(javascript).take(MAX_MATCHES_PER_RULE).forEach { match ->
                rule.expression(match)?.let(expressions::add)
            }
            if (expressions.size > 1) return null
        }
        return expressions.singleOrNull()
    }

    private fun functionExpression(name: String, rawIndex: String?, input: String): String? {
        val safe = safeName(name) ?: return null
        val index = rawIndex.orEmpty().takeIf { it.isNotBlank() }?.toIntOrNull()
        return if (index == null) "$safe($input)" else "$safe[$index]($input)"
    }

    private fun safeName(name: String): String? = name.takeIf(safeFunctionName::matches)

    private const val MAX_MATCHES_PER_RULE = 4
}

internal object YoutubeLocalDecoderFeedbackPolicy {
    private const val FEEDBACK_WINDOW_MS = 10L * 60L * 1000L

    fun shouldRefresh(source: String, now: Long, lastDecodeAtMs: Long): Boolean {
        val normalized = source.lowercase()
        val relevantSource = normalized.contains("web") || normalized.contains("levyraextractor")
        return relevantSource && YoutubePlayerConfigStore.withinWindow(now, lastDecodeAtMs, FEEDBACK_WINDOW_MS)
    }
}

internal object YoutubeRuntimeReusePolicy {
    fun canReuse(
        isDead: Boolean,
        runtimeHash: String,
        requestedHash: String,
        runtimeConfigKey: String,
        requestedConfigKey: String,
        runtimeEpoch: Long,
        currentEpoch: Long
    ): Boolean {
        return !isDead &&
            runtimeHash == requestedHash &&
            runtimeConfigKey == requestedConfigKey &&
            runtimeEpoch == currentEpoch
    }
}

internal object YoutubeCipherRuntimeFailurePolicy {
    fun marksRuntimeDead(error: Throwable): Boolean {
        return error is TimeoutCancellationException ||
            (error !is CancellationException &&
                error.message?.contains("timeout", ignoreCase = true) == true)
    }
}

internal object YoutubePlayerJsSupport {
    private val hashRegex = Regex("/s/player/([A-Za-z0-9_-]{8,32})/")
    private val playerCacheFileRegex = Regex("^player_[a-f0-9]{8}\\.js$")
    private val transformedNRegex = Regex("^[A-Za-z0-9_-]+$")

    fun isPlayerJsCacheFile(name: String): Boolean = playerCacheFileRegex.matches(name)

    fun extractPlayerHash(iframeApiBody: String): String? {
        val normalized = iframeApiBody.replace("\\/", "/")
        return hashRegex.find(normalized)?.groupValues?.getOrNull(1)
            ?.takeIf { YoutubePlayerConfigParser.isValidHash(it) }
    }


    fun playerFingerprint(javascript: String): String {
        val bytes = javascript.toByteArray(StandardCharsets.UTF_8)
        val length = minOf(bytes.size, 10_000)
        return MessageDigest.getInstance("MD5")
            .digest(bytes.copyOfRange(0, length))
            .joinToString("") { byte -> "%02x".format(byte) }
            .take(8)
    }

    fun isValidNTransform(input: String, output: String): Boolean {
        return output != input && output.length >= 5 && transformedNRegex.matches(output)
    }

    fun injectExports(javascript: String, config: YoutubePlayerCipherConfig): String {
        val signature = config.signatureExpression.replace("INPUT", "sig")
        val nExpression = config.nExpression.replace("INPUT", "n")
        val exports = ";window.__levyraSig=function(sig){return $signature;};window.__levyraN=function(n){return $nExpression;};"
        val marker = "})(_yt_player);"
        val index = javascript.lastIndexOf(marker)
        if (index < 0) throw ParsingException("YouTube player export marker not found")
        return javascript.substring(0, index) + exports + javascript.substring(index)
    }
}

private class YoutubeCipherWebRuntime private constructor(
    private val webView: WebView,
    private val ready: CompletableDeferred<Unit>,
    private val strictSelfTest: Boolean
) {
    private val waiters = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val closed = AtomicBoolean(false)

    @Volatile
    var isDead: Boolean = false
        private set

    suspend fun decodeSignature(value: String): String = evaluate("sig", value)

    suspend fun transformN(value: String): String = evaluate("n", value)

    suspend fun close() {
        if (!closed.compareAndSet(false, true)) return
        isDead = true
        val error = ParsingException("Local decoder runtime closed")
        waiters.values.forEach { it.completeExceptionally(error) }
        waiters.clear()
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

    private suspend fun evaluate(kind: String, value: String): String {
        if (isDead || closed.get()) throw ParsingException("Local decoder runtime unavailable")
        var requestId: String? = null
        try {
            withTimeout(READY_TIMEOUT_MS) { ready.await() }
            val id = UUID.randomUUID().toString()
            requestId = id
            val deferred = CompletableDeferred<String>()
            waiters[id] = deferred
            withContext(Dispatchers.Main.immediate) {
                if (isDead || closed.get()) throw ParsingException("Local decoder renderer unavailable")
                val script = "window.__levyraDecode(${JSONObject.quote(kind)},${JSONObject.quote(value)},${JSONObject.quote(id)});"
                webView.evaluateJavascript(script, null)
            }
            val output = withTimeout(EVALUATION_TIMEOUT_MS) { deferred.await() }
            if (kind == "n" && !YoutubePlayerJsSupport.isValidNTransform(value, output)) {
                isDead = true
                throw ParsingException("Invalid local n-transform result")
            }
            return output
        } catch (error: Throwable) {
            if (YoutubeCipherRuntimeFailurePolicy.marksRuntimeDead(error)) isDead = true
            throw error
        } finally {
            requestId?.let { waiters.remove(it) }
        }
    }

    private fun complete(requestId: String, result: String) {
        waiters[requestId]?.complete(result)
    }

    private fun fail(requestId: String, message: String) {
        waiters[requestId]?.completeExceptionally(ParsingException(message))
    }

    private fun rendererGone(message: String) {
        isDead = true
        val error = ParsingException(message)
        if (!ready.isCompleted) ready.completeExceptionally(error)
        waiters.values.forEach { it.completeExceptionally(error) }
        waiters.clear()
    }

    private class Bridge(private val runtime: YoutubeCipherWebRuntime) {
        @JavascriptInterface
        fun onReady(
            signatureAvailable: Boolean,
            signatureValidated: Boolean,
            nAvailable: Boolean,
            nValidated: Boolean
        ) {
            if (!signatureAvailable || !nAvailable || !nValidated || (runtime.strictSelfTest && !signatureValidated)) {
                runtime.ready.completeExceptionally(
                    ParsingException(
                        "Local decoder exports unavailable sig=$signatureAvailable sigValid=$signatureValidated n=$nAvailable nValid=$nValidated"
                    )
                )
                return
            }
            runtime.ready.complete(Unit)
        }

        @JavascriptInterface
        fun onResult(requestId: String, result: String) {
            runtime.complete(requestId, result)
        }

        @JavascriptInterface
        fun onError(requestId: String, error: String) {
            runtime.fail(requestId, error)
        }

        @JavascriptInterface
        fun onLoadError(error: String) {
            runtime.rendererGone("Local player JS load failed: $error")
        }
    }

    companion object {
        private const val JS_INTERFACE = "LevyraDecoderBridge"
        private const val READY_TIMEOUT_MS = 6_000L
        private const val EVALUATION_TIMEOUT_MS = 2_500L

        suspend fun create(
            context: Context,
            player: YoutubePlayerScript,
            config: YoutubePlayerCipherConfig
        ): YoutubeCipherWebRuntime {
            val directory = withContext(Dispatchers.IO) {
                val modified = YoutubePlayerJsSupport.injectExports(player.javascript, config)
                val dir = File(context.cacheDir, "youtube_local_decoder").apply { mkdirs() }
                YoutubePlayerConfigStore.writeAtomic(File(dir, "player.js"), modified)
                dir
            }
            val runtime = withContext(Dispatchers.Main.immediate) {
                val webView = WebView(context)
                val ready = CompletableDeferred<Unit>()
                val created = YoutubeCipherWebRuntime(
                    webView,
                    ready,
                    strictSelfTest = config.origin == YoutubePlayerConfigOrigin.ANALYZED
                )
                val bridge = Bridge(created)
                webView.settings.javaScriptEnabled = true
                webView.settings.allowFileAccess = true
                webView.settings.allowContentAccess = false
                webView.settings.javaScriptCanOpenWindowsAutomatically = false
                webView.settings.setSupportMultipleWindows(false)
                @Suppress("DEPRECATION")
                run { webView.settings.allowFileAccessFromFileURLs = true }
                webView.settings.blockNetworkLoads = true
                webView.settings.domStorageEnabled = false
                webView.addJavascriptInterface(bridge, JS_INTERFACE)
                webView.webChromeClient = WebChromeClient()
                webView.webViewClient = object : WebViewClient() {
                    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                        created.rendererGone("Local decoder render process gone")
                        return true
                    }
                }
                webView.loadDataWithBaseURL(
                    "file://${directory.absolutePath}/",
                    HTML,
                    "text/html",
                    StandardCharsets.UTF_8.name(),
                    null
                )
                created
            }
            try {
                withTimeout(READY_TIMEOUT_MS) { runtime.ready.await() }
                return runtime
            } catch (error: Throwable) {
                runtime.close()
                throw error
            }
        }

        private val HTML = """
            <!doctype html><html><head><meta charset="utf-8"><script>
            window.__levyraValidTransform=function(input,result){
              return typeof result==='string'&&result!==input&&result.length>=5&&/^[A-Za-z0-9_-]+$/.test(result);
            };
            window.__levyraValidN=window.__levyraValidTransform;
            window.__levyraReady=function(){
              var sigAvailable=typeof window.__levyraSig==='function';
              var nAvailable=typeof window.__levyraN==='function';
              var sigValidated=false;
              var nValidated=false;
              if(sigAvailable){
                try{
                  var sigProbe='abcdefghijklmnopqrstuvwxyz';
                  sigValidated=window.__levyraValidTransform(sigProbe,String(window.__levyraSig(sigProbe)));
                }catch(error){sigValidated=false;}
              }
              if(nAvailable){
                try{
                  var probe='KdrqFlzJXl9EcCwlmEy';
                  nValidated=window.__levyraValidN(probe,String(window.__levyraN(probe)));
                }catch(error){nValidated=false;}
              }
              LevyraDecoderBridge.onReady(sigAvailable,sigValidated,nAvailable,nValidated);
            };
            window.__levyraDecode=function(kind,input,id){
              try{
                var fn=kind==='sig'?window.__levyraSig:window.__levyraN;
                if(typeof fn!=='function'){LevyraDecoderBridge.onError(id,'decoder function unavailable: '+kind);return;}
                var result=fn(input);
                if(result===null||result===undefined||String(result).length===0){LevyraDecoderBridge.onError(id,'empty decoder result: '+kind);return;}
                result=String(result);
                if(kind==='n'&&!window.__levyraValidN(input,result)){LevyraDecoderBridge.onError(id,'invalid n-transform result');return;}
                LevyraDecoderBridge.onResult(id,result);
              }catch(error){LevyraDecoderBridge.onError(id,String(error&&error.stack?error.stack:error));}
            };
            </script><script src="player.js" onload="window.__levyraReady()" onerror="LevyraDecoderBridge.onLoadError('player.js')"></script></head><body></body></html>
        """.trimIndent()
    }
}

private class BoundedStringCache(private val maxEntries: Int) {
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

private fun sha256(value: String): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

private data class YoutubeHttpTextResponse(
    val code: Int,
    val body: String,
    val etag: String
)

private suspend fun OkHttpClient.awaitText(request: Request, maxBytes: Int): YoutubeHttpTextResponse {
    return suspendCancellableCoroutine { continuation ->
        val call = newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) runCatching { continuation.resumeWithException(e) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!continuation.isActive) return
                    runCatching {
                        val body = response.body
                        val declaredLength = body.contentLength()
                        if (declaredLength > maxBytes) {
                            throw IOException("Response exceeded $maxBytes bytes")
                        }
                        val bytes = body.bytes()
                        if (bytes.size > maxBytes) {
                            throw IOException("Response exceeded $maxBytes bytes")
                        }
                        YoutubeHttpTextResponse(
                            code = response.code,
                            body = String(bytes, StandardCharsets.UTF_8),
                            etag = response.header("ETag").orEmpty()
                        )
                    }.onSuccess { result ->
                        if (continuation.isActive) runCatching { continuation.resume(result) }
                    }.onFailure { error ->
                        if (continuation.isActive) runCatching { continuation.resumeWithException(error) }
                    }
                }
            }
        })
    }
}
