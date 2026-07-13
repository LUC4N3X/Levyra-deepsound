package com.luc4n3x.levyra.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayDeque
import java.util.Locale

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

    init {
        restore()
    }

    @Synchronized
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
            )
        )
    }

    @Synchronized
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
            )
        )
    }

    @Synchronized
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
            )
        )
    }

    @Synchronized
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
            )
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

    @Synchronized
    fun diagnostics(clientHealth: Map<String, JSONObject>): String {
        val root = JSONObject()
            .put("schemaVersion", 1)
            .put("generatedAt", System.currentTimeMillis())
        val clients = JSONObject()
        clientHealth.toSortedMap().forEach { (name, snapshot) -> clients.put(name, snapshot) }
        root.put("clients", clients)
        val trace = JSONArray()
        events.forEach { event ->
            trace.put(
                JSONObject()
                    .put("atMs", event.atMs)
                    .put("phase", event.phase)
                    .put("profile", event.profile)
                    .put("mode", event.mode)
                    .put("latencyMs", event.latencyMs)
                    .put("outcome", event.outcome)
                    .put("detail", event.detail)
            )
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
            .replace(Regex("https?://[^\\s]+"), "<redacted-url>")
            .replace(Regex("(?i)(authorization|cookie|visitor|token|signature)=[^&\\s]+"), "$1=<redacted>")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(280)
    }

    private fun append(event: PlaybackTraceEvent) {
        while (events.size >= MAX_EVENTS) events.removeFirst()
        events.addLast(event)
        persist()
    }

    private fun restore() {
        val raw = prefs.getString(KEY_EVENTS, null).orEmpty()
        if (raw.isBlank()) return
        runCatching {
            val array = JSONArray(raw)
            val start = (array.length() - MAX_EVENTS).coerceAtLeast(0)
            for (index in start until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                events.addLast(
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
    }

    private fun persist() {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject()
                    .put("atMs", event.atMs)
                    .put("phase", event.phase)
                    .put("profile", event.profile)
                    .put("mode", event.mode)
                    .put("latencyMs", event.latencyMs)
                    .put("outcome", event.outcome)
                    .put("detail", event.detail)
            )
        }
        prefs.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "levyra_playback_resilience"
        const val KEY_EVENTS = "events"
        const val MAX_EVENTS = 80
    }
}
