package com.luc4n3x.levyra.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal enum class PlaybackAudioStrategy {
    REEL_MUXED,
    REEL_AUDIO,
    PERSISTED,
    DIRECT,
    SEARCH
}

internal enum class PlaybackVideoStrategy {
    PERSISTED,
    STANDARD,
    REEL
}

internal data class PlaybackClientOverride(
    val enabled: Boolean? = null,
    val priority: Int? = null,
    val requiresPoToken: Boolean? = null,
    val clientVersion: String? = null
)

internal data class PlaybackCompatibilityPolicy(
    val schema: Int,
    val revision: Long,
    val audioStrategies: List<PlaybackAudioStrategy>,
    val videoStrategies: List<PlaybackVideoStrategy>,
    val androidReelClientVersion: String,
    val clientOverrides: Map<String, PlaybackClientOverride>,
    val expiresAt: Long,
    val minSupportedAppVersion: Int,
    val maxSupportedAppVersion: Int
) {
    companion object {
        const val SCHEMA = 1
        const val BUNDLED_REVISION = 2026082101L
        const val DEFAULT_ANDROID_REEL_CLIENT_VERSION = "21.03.36"

        fun bundled(): PlaybackCompatibilityPolicy = PlaybackCompatibilityPolicy(
            schema = SCHEMA,
            revision = BUNDLED_REVISION,
            audioStrategies = listOf(
                PlaybackAudioStrategy.REEL_AUDIO,
                PlaybackAudioStrategy.REEL_MUXED,
                PlaybackAudioStrategy.PERSISTED,
                PlaybackAudioStrategy.DIRECT,
                PlaybackAudioStrategy.SEARCH
            ),
            videoStrategies = listOf(
                PlaybackVideoStrategy.PERSISTED,
                PlaybackVideoStrategy.STANDARD,
                PlaybackVideoStrategy.REEL
            ),
            androidReelClientVersion = DEFAULT_ANDROID_REEL_CLIENT_VERSION,
            clientOverrides = emptyMap(),
            expiresAt = 0L,
            minSupportedAppVersion = 0,
            maxSupportedAppVersion = 0
        )
    }

    fun toJson(): String {
        val clients = JSONObject()
        clientOverrides.toSortedMap().forEach { (name, override) ->
            val value = JSONObject()
            override.enabled?.let { value.put("enabled", it) }
            override.priority?.let { value.put("priority", it) }
            override.requiresPoToken?.let { value.put("requiresPoToken", it) }
            override.clientVersion?.let { value.put("clientVersion", it) }
            clients.put(name, value)
        }
        return JSONObject()
            .put("schema", schema)
            .put("revision", revision)
            .put("audioStrategy", JSONArray(audioStrategies.map { it.name }))
            .put("videoStrategy", JSONArray(videoStrategies.map { it.name }))
            .put("androidReelClientVersion", androidReelClientVersion)
            .put("clients", clients)
            .put("expiresAt", expiresAt)
            .put("minSupportedAppVersion", minSupportedAppVersion)
            .put("maxSupportedAppVersion", maxSupportedAppVersion)
            .toString()
    }
}

internal object PlaybackCompatibilityPolicyParser {
    private val clientVersionPattern = Regex("^[A-Za-z0-9._-]{1,32}$")
    private const val MIN_PLAUSIBLE_EXPIRES_AT_MS = 946_684_800_000L
    private const val MAX_PLAUSIBLE_EXPIRES_AT_MS = 4_102_444_800_000L
    private val knownClients = setOf(
        "VISIONOS",
        "ANDROID_VR",
        "ANDROID_MUSIC",
        "ANDROID",
        "IOS",
        "WEB_REMIX",
        "WEB",
        "WEB_EMBEDDED_PLAYER"
    )

    fun parse(raw: String, base: PlaybackCompatibilityPolicy): PlaybackCompatibilityPolicy? {
        return runCatching {
            val root = JSONObject(raw)
            val schemaValue = requiredLong(root, "schema")
            require(schemaValue == PlaybackCompatibilityPolicy.SCHEMA.toLong())
            val schema = schemaValue.toInt()
            val revision = requiredLong(root, "revision")
            require(revision > 0L)

            val audioStrategies = if (root.has("audioStrategy")) {
                parseEnumArray(root.getJSONArray("audioStrategy"), base.audioStrategies)
            } else {
                base.audioStrategies
            }
            val videoStrategies = if (root.has("videoStrategy")) {
                parseEnumArray(root.getJSONArray("videoStrategy"), base.videoStrategies)
            } else {
                base.videoStrategies
            }
            require(audioStrategies.isNotEmpty())
            require(videoStrategies.isNotEmpty())

            val reelVersion = if (root.has("androidReelClientVersion")) {
                requiredString(root, "androidReelClientVersion").also {
                    require(clientVersionPattern.matches(it))
                }
            } else {
                base.androidReelClientVersion
            }

            val clientOverrides = if (root.has("clients")) {
                parseClientOverrides(root.getJSONObject("clients"))
            } else {
                base.clientOverrides
            }
            require(knownClients.any { clientOverrides[it]?.enabled != false })

            val expiresAt = if (root.has("expiresAt")) {
                requiredLong(root, "expiresAt").also {
                    require(it == 0L || it in MIN_PLAUSIBLE_EXPIRES_AT_MS..MAX_PLAUSIBLE_EXPIRES_AT_MS)
                }
            } else {
                base.expiresAt
            }

            val minSupportedAppVersion = if (root.has("minSupportedAppVersion")) {
                requiredInt(root, "minSupportedAppVersion").also { require(it >= 0) }
            } else {
                base.minSupportedAppVersion
            }
            val maxSupportedAppVersion = if (root.has("maxSupportedAppVersion")) {
                requiredInt(root, "maxSupportedAppVersion").also { require(it >= 0) }
            } else {
                base.maxSupportedAppVersion
            }
            require(
                minSupportedAppVersion == 0 ||
                    maxSupportedAppVersion == 0 ||
                    minSupportedAppVersion <= maxSupportedAppVersion
            )

            PlaybackCompatibilityPolicy(
                schema = schema,
                revision = revision,
                audioStrategies = audioStrategies,
                videoStrategies = videoStrategies,
                androidReelClientVersion = reelVersion,
                clientOverrides = clientOverrides,
                expiresAt = expiresAt,
                minSupportedAppVersion = minSupportedAppVersion,
                maxSupportedAppVersion = maxSupportedAppVersion
            )
        }.getOrNull()
    }

    private inline fun <reified T : Enum<T>> parseEnumArray(array: JSONArray, fallback: List<T>): List<T> {
        require(array.length() in 1..16)
        val values = ArrayList<T>(array.length())
        repeat(array.length()) { index ->
            val raw = array.get(index)
            require(raw is String)
            val parsedValue = runCatching { enumValueOf<T>(raw) }.getOrNull()
            if (parsedValue != null) values += parsedValue
        }
        require(values.distinct().size == values.size)
        return values.ifEmpty { fallback }
    }

    private fun parseClientOverrides(clients: JSONObject): Map<String, PlaybackClientOverride> {
        val output = LinkedHashMap<String, PlaybackClientOverride>()
        val names = clients.keys()
        while (names.hasNext()) {
            val name = names.next()
            if (name !in knownClients) continue
            val value = clients.get(name)
            require(value is JSONObject)
            val enabled = optionalBoolean(value, "enabled")
            val priority = optionalInt(value, "priority")?.also { require(it in 0..20) }
            val requiresPoToken = optionalBoolean(value, "requiresPoToken")
            val clientVersion = optionalString(value, "clientVersion")?.also {
                require(clientVersionPattern.matches(it))
            }
            output[name] = PlaybackClientOverride(
                enabled = enabled,
                priority = priority,
                requiresPoToken = requiresPoToken,
                clientVersion = clientVersion
            )
        }
        return output
    }

    private fun requiredLong(value: JSONObject, key: String): Long {
        val raw = value.get(key)
        require(raw is Byte || raw is Short || raw is Int || raw is Long)
        return (raw as Number).toLong()
    }

    private fun requiredString(value: JSONObject, key: String): String {
        val raw = value.get(key)
        require(raw is String && raw.isNotBlank())
        return raw
    }

    private fun requiredInt(value: JSONObject, key: String): Int {
        val longValue = requiredLong(value, key)
        require(longValue in Int.MIN_VALUE..Int.MAX_VALUE)
        return longValue.toInt()
    }

    private fun optionalBoolean(value: JSONObject, key: String): Boolean? {
        if (!value.has(key) || value.isNull(key)) return null
        val raw = value.get(key)
        require(raw is Boolean)
        return raw
    }

    private fun optionalInt(value: JSONObject, key: String): Int? {
        if (!value.has(key) || value.isNull(key)) return null
        val raw = value.get(key)
        require(raw is Byte || raw is Short || raw is Int || raw is Long)
        val longValue = (raw as Number).toLong()
        require(longValue in Int.MIN_VALUE..Int.MAX_VALUE)
        return longValue.toInt()
    }

    private fun optionalString(value: JSONObject, key: String): String? {
        if (!value.has(key) || value.isNull(key)) return null
        val raw = value.get(key)
        require(raw is String && raw.isNotBlank())
        return raw
    }
}

internal fun PlaybackCompatibilityPolicy.isExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
    return expiresAt != 0L && expiresAt <= nowMs
}

internal fun isAppVersionSupported(policy: PlaybackCompatibilityPolicy, appVersionCode: Int): Boolean {
    if (appVersionCode == 0) return true
    val belowMin = policy.minSupportedAppVersion != 0 && appVersionCode < policy.minSupportedAppVersion
    val aboveMax = policy.maxSupportedAppVersion != 0 && appVersionCode > policy.maxSupportedAppVersion
    return !belowMin && !aboveMax
}

internal class PlaybackCompatibilityPolicyStore(
    context: Context,
    httpClient: OkHttpClient,
    private val appVersionCode: Int = 0
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val lastAttemptAtMs = AtomicLong(0L)
    private val rejectionRefreshAtMs = AtomicLong(0L)
    private val bundledPolicy = PlaybackCompatibilityPolicy.bundled()
    private val currentPolicy = AtomicReference(loadCachedPolicy())
    private val client = httpClient.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(900, TimeUnit.MILLISECONDS)
        .readTimeout(1_400, TimeUnit.MILLISECONDS)
        .writeTimeout(700, TimeUnit.MILLISECONDS)
        .callTimeout(1_800, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var checkedAtMs: Long = preferences.getLong(KEY_CHECKED_AT_MS, 0L)

    fun current(): PlaybackCompatibilityPolicy {
        val policy = currentPolicy.get()
        return if (policy.isExpired()) bundledPolicy else policy
    }

    fun needsRefresh(nowMs: Long = System.currentTimeMillis()): Boolean {
        val stale = currentPolicy.get().isExpired(nowMs) || checkedAtMs <= 0L || nowMs - checkedAtMs >= REFRESH_TTL_MS
        if (!stale) return false
        val lastAttempt = lastAttemptAtMs.get()
        return lastAttempt <= 0L || nowMs - lastAttempt >= FAILED_FETCH_BACKOFF_MS
    }

    suspend fun refresh(force: Boolean, reason: String): Boolean {
        if (!force && !needsRefresh()) return false
        return mutex.withLock {
            if (!force && !needsRefresh()) return@withLock false
            refreshLocked(reason)
        }
    }

    suspend fun refreshAfterRejection(): Boolean {
        val now = System.currentTimeMillis()
        while (true) {
            val previous = rejectionRefreshAtMs.get()
            if (previous > 0L && now - previous < REJECTION_REFRESH_COOLDOWN_MS) return false
            if (rejectionRefreshAtMs.compareAndSet(previous, now)) break
        }
        return refresh(force = true, reason = "stream-rejected")
    }

    private suspend fun refreshLocked(reason: String): Boolean {
        lastAttemptAtMs.set(System.currentTimeMillis())
        val etag = preferences.getString(KEY_ETAG, "").orEmpty()
        val request = Request.Builder()
            .url(REMOTE_POLICY_URL)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .apply { etag.takeIf { it.isNotBlank() }?.let { header("If-None-Match", it) } }
            .build()

        val response = try {
            withContext(Dispatchers.IO) { await(request) }
        } catch (error: IOException) {
            Timber.w(error, "Playback compatibility policy fetch failed reason=%s", reason)
            return false
        }

        response.use { value ->
            if (value.code == 304) {
                markChecked(value.header("ETag").orEmpty().ifBlank { etag })
                return false
            }
            if (!value.isSuccessful) {
                markCheckedWithoutEtagChange()
                Timber.w("Playback compatibility policy HTTP %d reason=%s", value.code, reason)
                return false
            }
            val raw = try {
                readBoundedYoutubeJsonBody(value.body, MAX_POLICY_BYTES)
            } catch (error: IOException) {
                markCheckedWithoutEtagChange()
                Timber.w(error, "Playback compatibility policy response rejected")
                return false
            }
            val before = currentPolicy.get()
            val candidate = PlaybackCompatibilityPolicyParser.parse(raw, before)
            if (candidate == null) {
                markCheckedWithoutEtagChange()
                Timber.w("Playback compatibility policy payload rejected reason=%s", reason)
                return false
            }
            if (candidate.isExpired()) {
                markCheckedWithoutEtagChange()
                Timber.w("Playback compatibility policy expired revision=%d", candidate.revision)
                return false
            }
            if (!isAppVersionSupported(candidate, appVersionCode)) {
                markCheckedWithoutEtagChange()
                Timber.w(
                    "Playback compatibility policy app version %d outside supported range [%d,%d]",
                    appVersionCode,
                    candidate.minSupportedAppVersion,
                    candidate.maxSupportedAppVersion
                )
                return false
            }
            if (candidate.revision < before.revision) {
                markCheckedWithoutEtagChange()
                Timber.w(
                    "Playback compatibility policy downgrade ignored current=%d remote=%d",
                    before.revision,
                    candidate.revision
                )
                return false
            }
            if (candidate.revision == before.revision && candidate != before) {
                markCheckedWithoutEtagChange()
                Timber.w("Playback compatibility policy changed without revision bump: %d", candidate.revision)
                return false
            }

            val changed = candidate != before
            if (changed) currentPolicy.set(candidate)
            persistPolicy(candidate, value.header("ETag").orEmpty().ifBlank { etag })
            Timber.i(
                "Playback compatibility policy active revision=%d changed=%s reason=%s",
                candidate.revision,
                changed,
                reason
            )
            return changed
        }
    }

    private fun loadCachedPolicy(): PlaybackCompatibilityPolicy {
        val raw = preferences.getString(KEY_POLICY_JSON, "").orEmpty()
        if (raw.isBlank()) return bundledPolicy
        val cached = PlaybackCompatibilityPolicyParser.parse(raw, bundledPolicy) ?: return bundledPolicy
        if (cached.isExpired()) return bundledPolicy
        if (!isAppVersionSupported(cached, appVersionCode)) return bundledPolicy
        return cached.takeIf { it.revision >= bundledPolicy.revision } ?: bundledPolicy
    }

    private fun persistPolicy(policy: PlaybackCompatibilityPolicy, etag: String) {
        val now = System.currentTimeMillis()
        checkedAtMs = now
        preferences.edit()
            .putString(KEY_POLICY_JSON, policy.toJson())
            .putString(KEY_ETAG, etag)
            .putLong(KEY_CHECKED_AT_MS, now)
            .apply()
    }

    private fun markChecked(etag: String) {
        val now = System.currentTimeMillis()
        checkedAtMs = now
        preferences.edit()
            .putString(KEY_ETAG, etag)
            .putLong(KEY_CHECKED_AT_MS, now)
            .apply()
    }

    private fun markCheckedWithoutEtagChange() {
        val now = System.currentTimeMillis()
        checkedAtMs = now
        preferences.edit()
            .putLong(KEY_CHECKED_AT_MS, now)
            .apply()
    }

    private suspend fun await(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, value, _ -> value.close() }
            }
        })
    }

    companion object {
        private const val PREFS_NAME = "levyra_playback_compatibility_policy"
        private const val KEY_POLICY_JSON = "policy_json"
        private const val KEY_ETAG = "etag"
        private const val KEY_CHECKED_AT_MS = "checked_at_ms"
        private const val REFRESH_TTL_MS = 5L * 60L * 1000L
        private const val FAILED_FETCH_BACKOFF_MS = 30_000L
        private const val REJECTION_REFRESH_COOLDOWN_MS = 30_000L
        private const val MAX_POLICY_BYTES = 64L * 1024L
        private const val USER_AGENT = "Levyra playback-policy/1"
        private const val REMOTE_POLICY_URL =
            "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/main/config/playback_policy.json"
    }
}
