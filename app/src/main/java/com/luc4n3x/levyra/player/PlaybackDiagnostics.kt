package com.luc4n3x.levyra.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.PlaybackStrategyCircuit
import com.luc4n3x.levyra.data.PlaybackStrategyStats
import com.luc4n3x.levyra.data.PlaybackClientCapabilities
import com.luc4n3x.levyra.data.isAppVersionSupported
import com.luc4n3x.levyra.data.isExpired
import com.luc4n3x.levyra.data.network.YoutubeStreamClientIdentityRegistry
import com.luc4n3x.levyra.data.parsePlaybackStrategyHealthSnapshot
import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DIAGNOSTIC_RECENT_FAILURE_MS = 30L * 60L * 1_000L
private const val DIAGNOSTIC_FIELD_MAX_CHARS = 200
private const val DIAGNOSTIC_MAX_STRATEGIES = 8
private const val DIAGNOSTIC_REDACTED = "[redacted]"
private const val DIAGNOSTIC_MISSING = "-"

private val DIAGNOSTIC_SECRET_PATTERN = Regex(
    "(?i)\\b(authorization|proxy-authorization|cookie|set-cookie|pot|potoken|po_token|token|" +
        "access[_ -]?token|refresh[_ -]?token|id[_ -]?token|session|signature|sig|api[_ -]?key|" +
        "x-goog-api-key|key|secret|password)\\b\\s*[:=]\\s*\\S+"
)
private val DIAGNOSTIC_BEARER_PATTERN = Regex("(?i)\\b(bearer|basic|digest)\\s+\\S+")
private val DIAGNOSTIC_URL_PATTERN = Regex("(?i)(?:[a-z][a-z0-9+.-]*://|\\bwww\\.)\\S*")
private val DIAGNOSTIC_LONG_OPAQUE_PATTERN = Regex("[A-Za-z0-9_\\-]{40,}")

internal enum class PlaybackDiagnosticStatus {
    HEALTHY,
    FALLBACK_HISTORY,
    ERROR,
    IDLE
}

internal data class PlaybackDiagnosticFormat(
    val mimeType: String = "",
    val codecs: String = "",
    val bitrateKbps: Int? = null,
    val channels: Int? = null,
    val sampleRateHz: Int? = null,
    val width: Int? = null,
    val height: Int? = null
) {
    fun summary(): String = buildList {
        mimeType.takeIf(String::isNotBlank)?.let(::add)
        codecs.takeIf(String::isNotBlank)?.let(::add)
        bitrateKbps?.let { add("$it kbps") }
        if (width != null && height != null) add("${width}x$height")
        channels?.let { add("$it ch") }
        sampleRateHz?.let { add("$it Hz") }
    }.joinToString(" · ")
}

internal data class PlaybackDiagnosticStrategy(
    val name: String,
    val successes: Int,
    val failures: Int,
    val consecutiveFailures: Int,
    val averageLatencyMs: Long?,
    val circuit: PlaybackStrategyCircuit,
    val lastFailure: String,
    val lastFailureAtMs: Long
)

internal data class PlaybackDiagnosticClient(
    val name: String = "",
    val version: String = "",
    val requiresProofOfOrigin: Boolean = false
)

internal data class PlaybackDiagnosticPolicy(
    val schema: Int = 0,
    val revision: Long = 0L,
    val expired: Boolean = false,
    val appVersionSupported: Boolean = true,
    val audioStrategies: List<String> = emptyList(),
    val videoStrategies: List<String> = emptyList(),
    val restrictedClients: List<String> = emptyList()
)

internal data class PlaybackDiagnosticSnapshot(
    val status: PlaybackDiagnosticStatus = PlaybackDiagnosticStatus.IDLE,
    val appVersion: String = "",
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val source: String = "",
    val videoMode: Boolean = false,
    val playerState: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val audioSessionId: Int? = null,
    val audioFormat: PlaybackDiagnosticFormat? = null,
    val videoFormat: PlaybackDiagnosticFormat? = null,
    val cacheBytes: Long = 0L,
    val networkTransport: String = "",
    val networkValidated: Boolean = false,
    val networkMetered: Boolean = false,
    val playerErrorCode: String = "",
    val activeStrategy: String = "",
    val client: PlaybackDiagnosticClient = PlaybackDiagnosticClient(),
    val policy: PlaybackDiagnosticPolicy = PlaybackDiagnosticPolicy(),
    val strategies: List<PlaybackDiagnosticStrategy> = emptyList()
) {
    fun playbackRows(): List<Pair<String, String>> = listOf(
        "app" to safeDiagnosticValue(appVersion),
        "state" to safeDiagnosticValue(playerState),
        "playing" to isPlaying.toString(),
        "mode" to (if (videoMode) "video" else "audio"),
        "track_id" to safeDiagnosticValue(trackId),
        "title" to safeDiagnosticValue(title),
        "artist" to safeDiagnosticValue(artist),
        "source" to safeDiagnosticValue(source),
        "position_ms" to positionMs.coerceAtLeast(0L).toString(),
        "buffered_ms" to bufferedPositionMs.coerceAtLeast(0L).toString(),
        "duration_ms" to durationMs.coerceAtLeast(0L).toString(),
        "speed" to String.format(Locale.ROOT, "%.2fx", playbackSpeed),
        "audio_session" to (audioSessionId?.toString() ?: DIAGNOSTIC_MISSING),
        "error_code" to safeDiagnosticValue(playerErrorCode)
    )

    fun formatRows(): List<Pair<String, String>> = listOf(
        "audio" to safeDiagnosticValue(audioFormat?.summary().orEmpty()),
        "video" to safeDiagnosticValue(videoFormat?.summary().orEmpty())
    )

    fun networkRows(): List<Pair<String, String>> = listOf(
        "cache_bytes" to cacheBytes.coerceAtLeast(0L).toString(),
        "transport" to safeDiagnosticValue(networkTransport),
        "validated" to networkValidated.toString(),
        "metered" to networkMetered.toString()
    )

    fun resolverRows(): List<Pair<String, String>> = buildList {
        add("active_strategy" to safeDiagnosticValue(activeStrategy))
        add("client" to safeDiagnosticValue(client.name))
        add("client_version" to safeDiagnosticValue(client.version))
        add("client_proof_of_origin" to client.requiresProofOfOrigin.toString())
        add("policy_schema" to policy.schema.toString())
        add("policy_revision" to policy.revision.toString())
        add("policy_expired" to policy.expired.toString())
        add("policy_app_supported" to policy.appVersionSupported.toString())
        add("policy_audio_order" to safeDiagnosticValue(policy.audioStrategies.joinToString(" > ")))
        add("policy_video_order" to safeDiagnosticValue(policy.videoStrategies.joinToString(" > ")))
        if (policy.restrictedClients.isNotEmpty()) {
            add("policy_restricted_clients" to safeDiagnosticValue(policy.restrictedClients.joinToString(", ")))
        }
        strategies.forEach { strategy ->
            add(
                safeDiagnosticValue(strategy.name) to buildString {
                    append("ok=${strategy.successes} fail=${strategy.failures}")
                    append(" streak=${strategy.consecutiveFailures}")
                    append(" circuit=${strategy.circuit.name}")
                    strategy.averageLatencyMs?.let { append(" avg=${it.coerceAtLeast(0L)}ms") }
                    if (strategy.lastFailure.isNotBlank()) {
                        append(" last=${sanitizeDiagnosticField(strategy.lastFailure)}")
                    }
                }
            )
        }
    }

    fun safeReport(): String = buildString {
        appendLine("LEVYRA PLAYBACK DIAGNOSTICS")
        appendLine("status=${status.name}")
        appendSection("playback", playbackRows())
        appendSection("formats", formatRows())
        appendSection("network", networkRows())
        appendSection("resolver", resolverRows())
        appendLine()
        append(
            "Stream URLs, request headers, cookies, tokens, proof-of-origin values and API keys " +
                "are intentionally excluded from this report."
        )
    }

    private fun StringBuilder.appendSection(title: String, rows: List<Pair<String, String>>) {
        appendLine()
        appendLine("[$title]")
        rows.forEach { (label, value) ->
            appendLine("  ${sanitizeDiagnosticField(label).ifBlank { DIAGNOSTIC_MISSING }}: ${safeDiagnosticValue(value)}")
        }
    }
}

internal fun sanitizeDiagnosticField(value: String): String {
    val singleLine = value
        .replace('\r', ' ')
        .replace('\n', ' ')
        .replace('\t', ' ')
        .trim()
    if (singleLine.isEmpty()) return ""
    val sensitive = DIAGNOSTIC_URL_PATTERN.containsMatchIn(singleLine) ||
        DIAGNOSTIC_SECRET_PATTERN.containsMatchIn(singleLine) ||
        DIAGNOSTIC_BEARER_PATTERN.containsMatchIn(singleLine) ||
        DIAGNOSTIC_LONG_OPAQUE_PATTERN.containsMatchIn(singleLine)
    if (sensitive) return DIAGNOSTIC_REDACTED
    return singleLine.take(DIAGNOSTIC_FIELD_MAX_CHARS)
}

internal fun safeDiagnosticValue(value: String): String =
    sanitizeDiagnosticField(value).ifBlank { DIAGNOSTIC_MISSING }

internal fun playbackDiagnosticStatus(
    errorCode: String,
    playbackState: Int?,
    strategies: List<PlaybackDiagnosticStrategy>,
    nowMs: Long
): PlaybackDiagnosticStatus = when {
    errorCode.isNotBlank() -> PlaybackDiagnosticStatus.ERROR
    playbackState == null || playbackState == Player.STATE_IDLE -> PlaybackDiagnosticStatus.IDLE
    strategies.any { strategy ->
        strategy.circuit != PlaybackStrategyCircuit.CLOSED ||
            (
                strategy.lastFailureAtMs > 0L &&
                    nowMs >= strategy.lastFailureAtMs &&
                    nowMs - strategy.lastFailureAtMs <= DIAGNOSTIC_RECENT_FAILURE_MS
                )
    } -> PlaybackDiagnosticStatus.FALLBACK_HISTORY

    else -> PlaybackDiagnosticStatus.HEALTHY
}

@UnstableApi
internal class PlaybackDiagnosticsReader(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
    private val strategyHealthPreferences = appContext.getSharedPreferences(
        PLAYBACK_STRATEGY_HEALTH_PREFS,
        Context.MODE_PRIVATE
    )

    suspend fun capture(
        fallbackTrack: Track? = null,
        activeStrategy: String = ""
    ): PlaybackDiagnosticSnapshot {
        val playerState = withContext(Dispatchers.Main.immediate) { capturePlayerState(fallbackTrack) }
        val now = System.currentTimeMillis()
        return withContext(Dispatchers.IO) {
            val strategies = readStrategyHealth(playerState.videoMode, now)
            val network = networkSnapshot()
            PlaybackDiagnosticSnapshot(
                status = playbackDiagnosticStatus(
                    errorCode = playerState.playerErrorCode,
                    playbackState = playerState.playbackState,
                    strategies = strategies,
                    nowMs = now
                ),
                appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                trackId = playerState.trackId,
                title = playerState.title,
                artist = playerState.artist,
                source = playerState.source,
                videoMode = playerState.videoMode,
                playerState = playerState.playbackState?.let(::playerStateName) ?: "UNAVAILABLE",
                isPlaying = playerState.isPlaying,
                positionMs = playerState.positionMs,
                bufferedPositionMs = playerState.bufferedPositionMs,
                durationMs = playerState.durationMs,
                playbackSpeed = playerState.playbackSpeed,
                audioSessionId = playerState.audioSessionId,
                audioFormat = playerState.audioFormat,
                videoFormat = playerState.videoFormat,
                cacheBytes = LevyraMediaCache.currentCacheSpace(),
                networkTransport = network.transport,
                networkValidated = network.validated,
                networkMetered = network.metered,
                playerErrorCode = playerState.playerErrorCode,
                activeStrategy = activeStrategy,
                client = playerState.client,
                policy = readPolicy(),
                strategies = strategies
            )
        }
    }

    private fun capturePlayerState(fallbackTrack: Track?): PlayerDiagnosticState {
        val player = PlaybackService.activePlayer
        val item = player?.currentMediaItem
        val extras = item?.mediaMetadata?.extras
        val selected = player?.let(::selectedFormats).orEmpty()
        val duration = player?.duration?.takeIf { it != C.TIME_UNSET && it >= 0L }
            ?: fallbackTrack?.durationMs?.coerceAtLeast(0L)
            ?: 0L
        val videoMode = extras?.getBoolean(PlaybackService.EXTRA_VIDEO_MODE, false) ?: false
        val identity = fallbackTrack?.let { track ->
            val url = if (videoMode) track.videoStreamUrl.ifBlank { track.streamUrl } else track.streamUrl
            url.takeIf(String::isNotBlank)?.let(YoutubeStreamClientIdentityRegistry::find)
        }
        return PlayerDiagnosticState(
            trackId = item?.mediaId.orEmpty().ifBlank { fallbackTrack?.id.orEmpty() },
            title = item?.mediaMetadata?.title?.toString().orEmpty().ifBlank { fallbackTrack?.title.orEmpty() },
            artist = item?.mediaMetadata?.artist?.toString().orEmpty().ifBlank { fallbackTrack?.artist.orEmpty() },
            source = extras?.getString(EXTRA_SOURCE).orEmpty().ifBlank { fallbackTrack?.source.orEmpty() },
            videoMode = videoMode,
            playbackState = player?.playbackState,
            isPlaying = player?.isPlaying == true,
            positionMs = player?.currentPosition?.coerceAtLeast(0L) ?: 0L,
            bufferedPositionMs = player?.bufferedPosition?.coerceAtLeast(0L) ?: 0L,
            durationMs = duration,
            playbackSpeed = player?.playbackParameters?.speed ?: 1f,
            audioSessionId = player?.audioSessionId?.takeIf { it > 0 },
            audioFormat = selected.firstOrNull { it.sampleMimeType?.startsWith("audio/") == true }?.toDiagnosticFormat(),
            videoFormat = selected.firstOrNull { it.sampleMimeType?.startsWith("video/") == true }?.toDiagnosticFormat(),
            playerErrorCode = player?.playerError?.errorCodeName.orEmpty(),
            client = PlaybackDiagnosticClient(
                name = identity?.clientName.orEmpty(),
                version = identity?.clientVersion.orEmpty(),
                requiresProofOfOrigin = identity?.requiresPoToken == true
            )
        )
    }

    private fun readStrategyHealth(videoMode: Boolean, nowMs: Long): List<PlaybackDiagnosticStrategy> {
        val prefix = if (videoMode) "video::" else "audio::"
        val raw = runCatching {
            strategyHealthPreferences.getString(PLAYBACK_STRATEGY_HEALTH_KEY, null)
        }.getOrNull()
        return parsePlaybackStrategyHealthSnapshot(raw)
            .asSequence()
            .filter { (key, _) -> key.startsWith(prefix) }
            .sortedWith(
                compareByDescending<Map.Entry<String, PlaybackStrategyStats>> { it.value.updatedAtMs }
                    .thenBy { it.key }
            )
            .take(DIAGNOSTIC_MAX_STRATEGIES)
            .map { (key, stats) ->
                PlaybackDiagnosticStrategy(
                    name = key.removePrefix(prefix),
                    successes = stats.successes,
                    failures = stats.failures,
                    consecutiveFailures = stats.consecutiveFailures,
                    averageLatencyMs = stats.averageLatencyMs.takeIf { it != Long.MAX_VALUE },
                    circuit = stats.circuitAt(nowMs),
                    lastFailure = stats.lastFailureKind?.name.orEmpty(),
                    lastFailureAtMs = stats.lastFailureAtMs
                )
            }
            .toList()
    }

    private fun readPolicy(): PlaybackDiagnosticPolicy {
        val policy = PlaybackClientCapabilities.active()
        return PlaybackDiagnosticPolicy(
            schema = policy.schema,
            revision = policy.revision,
            expired = policy.isExpired(),
            appVersionSupported = isAppVersionSupported(policy, BuildConfig.VERSION_CODE),
            audioStrategies = policy.audioStrategies.map { it.name },
            videoStrategies = policy.videoStrategies.map { it.name },
            restrictedClients = policy.clientOverrides
                .filterValues { it.enabled == false }
                .keys
                .sorted()
        )
    }

    private fun networkSnapshot(): PlaybackDiagnosticNetwork {
        val manager = connectivityManager ?: return PlaybackDiagnosticNetwork("unavailable", false, false)
        val capabilities = manager.activeNetwork?.let(manager::getNetworkCapabilities)
        val transport = when {
            capabilities == null -> "offline"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
            else -> "other"
        }
        return PlaybackDiagnosticNetwork(
            transport = transport,
            validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            metered = manager.isActiveNetworkMetered
        )
    }

    private companion object {
        const val PLAYBACK_STRATEGY_HEALTH_PREFS = "levyra_playback_strategy_health"
        const val PLAYBACK_STRATEGY_HEALTH_KEY = "health"
        const val EXTRA_SOURCE = "levyra.source"
    }
}

private data class PlayerDiagnosticState(
    val trackId: String,
    val title: String,
    val artist: String,
    val source: String,
    val videoMode: Boolean,
    val playbackState: Int?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val bufferedPositionMs: Long,
    val durationMs: Long,
    val playbackSpeed: Float,
    val audioSessionId: Int?,
    val audioFormat: PlaybackDiagnosticFormat?,
    val videoFormat: PlaybackDiagnosticFormat?,
    val playerErrorCode: String,
    val client: PlaybackDiagnosticClient
)

private data class PlaybackDiagnosticNetwork(
    val transport: String,
    val validated: Boolean,
    val metered: Boolean
)

@UnstableApi
private fun selectedFormats(player: ExoPlayer): List<Format> = buildList {
    player.currentTracks.groups.forEach { group ->
        for (index in 0 until group.length) {
            if (group.isTrackSelected(index)) add(group.getTrackFormat(index))
        }
    }
}

private fun Format.toDiagnosticFormat(): PlaybackDiagnosticFormat = PlaybackDiagnosticFormat(
    mimeType = sampleMimeType.orEmpty(),
    codecs = codecs.orEmpty(),
    bitrateKbps = averageBitrate.takeIf { it > 0 }?.div(1_000),
    channels = channelCount.takeIf { it > 0 },
    sampleRateHz = sampleRate.takeIf { it > 0 },
    width = width.takeIf { it > 0 },
    height = height.takeIf { it > 0 }
)

private fun playerStateName(state: Int): String = when (state) {
    Player.STATE_IDLE -> "IDLE"
    Player.STATE_BUFFERING -> "BUFFERING"
    Player.STATE_READY -> "READY"
    Player.STATE_ENDED -> "ENDED"
    else -> "UNKNOWN"
}
