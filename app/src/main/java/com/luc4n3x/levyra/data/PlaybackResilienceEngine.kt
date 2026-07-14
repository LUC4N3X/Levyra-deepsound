package com.luc4n3x.levyra.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal enum class PlaybackFailureKind {
    Forbidden,
    Gone,
    RateLimited,
    ExpiredUrl,
    Signature,
    Decoder,
    Network,
    Timeout,
    Unknown
}

internal data class PlaybackRecoveryPlan(
    val invalidateStream: Boolean,
    val rotateClient: Boolean,
    val rotateCodec: Boolean,
    val refreshSecurity: Boolean,
    val quarantineMs: Long
)

internal data class PlaybackTraceEvent(
    val atMs: Long,
    val phase: String,
    val profile: String,
    val mode: String,
    val latencyMs: Long,
    val outcome: String,
    val detail: String
)

internal class PlaybackResilienceEngine(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val events = ArrayDeque<PlaybackTraceEvent>(MAX_EVENTS)
    private val eventLock = Any()
    private val persistenceScheduled = AtomicBoolean(false)
    private val persistenceExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "levyra-playback-trace").apply { isDaemon = true }
    }

    init {
        restore()
    }

    fun recordAttempt(profile: String, mode: String) {
        append(
            PlaybackTraceEvent(
                atMs = System.currentTimeMillis(),
                phase = "resolve",
                profile = profile,
                mode = mode,
                latencyMs = 0L,
                outcome = "attempt",
                detail = ""
            ),
            persist = false
        )
    }

    fun recordSuccess(profile: String, mode: String, latencyMs: Long, source: String) {
        append(
            PlaybackTraceEvent(
                atMs = System.currentTimeMillis(),
                phase = "resolve",
                profile = profile,
                mode = mode,
                latencyMs = latencyMs.coerceAtLeast(1L),
                outcome = "success",
                detail = sanitize(source)
            ),
            persist = true
        )
    }

    fun recordFailure(profile: String, mode: String, latencyMs: Long?, error: Throwable) {
        val detail = error.message.orEmpty().ifBlank { error::class.java.simpleName }
        append(
            PlaybackTraceEvent(
                atMs = System.currentTimeMillis(),
                phase = "resolve",
                profile = profile,
                mode = mode,
                latencyMs = latencyMs?.coerceAtLeast(1L) ?: 0L,
                outcome = classify(detail).name,
                detail = sanitize(detail)
            ),
            persist = true
        )
    }

    fun recordPlayerFailure(trackId: String, videoMode: Boolean, reason: String) {
        append(
            PlaybackTraceEvent(
                atMs = System.currentTimeMillis(),
                phase = "player",
                profile = "active",
                mode = if (videoMode) "video" else "audio",
                latencyMs = 0L,
                outcome = classify(reason).name,
                detail = "${trackId.take(20)} ${sanitize(reason)}".trim()
            ),
            persist = true
        )
    }

    fun recoveryPlan(reason: String): PlaybackRecoveryPlan {
        return when (classify(reason)) {
            PlaybackFailureKind.Forbidden,
            PlaybackFailureKind.Gone,
            PlaybackFailureKind.RateLimited -> PlaybackRecoveryPlan(true, true, false, true, 10L * 60L * 1000L)
            PlaybackFailureKind.ExpiredUrl -> PlaybackRecoveryPlan(true, true, false, false, 2L * 60L * 1000L)
            PlaybackFailureKind.Signature -> PlaybackRecoveryPlan(true, true, false, true, 10L * 60L * 1000L)
            PlaybackFailureKind.Decoder -> PlaybackRecoveryPlan(true, false, true, false, 30L * 60L * 1000L)
            PlaybackFailureKind.Timeout,
            PlaybackFailureKind.Network -> PlaybackRecoveryPlan(true, true, false, false, 45_000L)
            PlaybackFailureKind.Unknown -> PlaybackRecoveryPlan(true, true, true, false, 20_000L)
        }
    }

    fun diagnostics(clientHealth: Map<String, JSONObject>): String {
        val eventSnapshot = synchronized(eventLock) { events.toList() }
        val root = JSONObject()
            .put("schemaVersion", 1)
            .put("generatedAt", System.currentTimeMillis())
        val clients = JSONObject()
        clientHealth.toSortedMap().forEach { (name, snapshot) -> clients.put(name, snapshot) }
        root.put("clients", clients)
        val trace = JSONArray()
        eventSnapshot.forEach { event ->
            trace.put(event.toJson())
        }
        root.put("trace", trace)
        return root.toString(2)
    }

    private fun classify(raw: String): PlaybackFailureKind {
        val value = raw.lowercase(Locale.ROOT)
        return when {
            value.contains("403") || value.contains("forbidden") -> PlaybackFailureKind.Forbidden
            value.contains("410") || value.contains("gone") -> PlaybackFailureKind.Gone
            value.contains("429") || value.contains("rate limit") -> PlaybackFailureKind.RateLimited
            value.contains("expired") || value.contains("scadut") || value.contains("stream non valido") -> PlaybackFailureKind.ExpiredUrl
            value.contains("signature") || value.contains("n-transform") || value.contains("potoken") || value.contains("po token") -> PlaybackFailureKind.Signature
            value.contains("decoder") || value.contains("codec") || value.contains("format") -> PlaybackFailureKind.Decoder
            value.contains("timeout") || value.contains("timed out") || value.contains("lento") -> PlaybackFailureKind.Timeout
            value.contains("network") || value.contains("socket") || value.contains("dns") || value.contains("connection") || value.contains("host") -> PlaybackFailureKind.Network
            else -> PlaybackFailureKind.Unknown
        }
    }

    private fun sanitize(value: String): String {
        return value
            .replace(Regex("""https?://[^\s]+"""), "<redacted-url>")
            .replace(Regex("""(?i)(authorization|cookie|visitor|token|signature)=[^&\s]+""")) { match ->
                "${match.groupValues[1]}=<redacted>"
            }
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(280)
    }

    private fun append(event: PlaybackTraceEvent, persist: Boolean) {
        synchronized(eventLock) {
            while (events.size >= MAX_EVENTS) events.removeFirst()
            events.addLast(event)
        }
        if (persist) schedulePersist()
    }

    private fun restore() {
        val raw = prefs.getString(KEY_EVENTS, null).orEmpty()
        if (raw.isBlank()) return
        runCatching {
            val array = JSONArray(raw)
            val start = (array.length() - MAX_EVENTS).coerceAtLeast(0)
            val restored = buildList {
                for (index in start until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    add(
                        PlaybackTraceEvent(
                            atMs = json.optLong("atMs"),
                            phase = json.optString("phase"),
                            profile = json.optString("profile"),
                            mode = json.optString("mode"),
                            latencyMs = json.optLong("latencyMs"),
                            outcome = json.optString("outcome"),
                            detail = json.optString("detail")
                        )
                    )
                }
            }
            synchronized(eventLock) { restored.forEach(events::addLast) }
        }
    }

    private fun schedulePersist() {
        if (!persistenceScheduled.compareAndSet(false, true)) return
        persistenceExecutor.schedule(
            {
                persistenceScheduled.set(false)
                persistSnapshot()
            },
            PERSIST_DEBOUNCE_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun persistSnapshot() {
        val snapshot = synchronized(eventLock) {
            events.filterNot { it.outcome == "attempt" }
                .takeLast(MAX_PERSISTED_EVENTS)
        }
        val array = JSONArray()
        snapshot.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    private fun PlaybackTraceEvent.toJson(): JSONObject = JSONObject()
        .put("atMs", atMs)
        .put("phase", phase)
        .put("profile", profile)
        .put("mode", mode)
        .put("latencyMs", latencyMs)
        .put("outcome", outcome)
        .put("detail", detail)

    private companion object {
        const val PREFS_NAME = "levyra_playback_resilience"
        const val KEY_EVENTS = "events"
        const val MAX_EVENTS = 80
        const val MAX_PERSISTED_EVENTS = 64
        const val PERSIST_DEBOUNCE_MS = 1_500L
    }
}
