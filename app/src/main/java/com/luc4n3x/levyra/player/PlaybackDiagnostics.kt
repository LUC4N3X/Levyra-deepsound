package com.luc4n3x.levyra.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.PlaybackStrategyCircuit
import com.luc4n3x.levyra.data.PlaybackStrategyStats
import com.luc4n3x.levyra.data.parsePlaybackStrategyHealthSnapshot
import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PLAYBACK_DIAGNOSTIC_RECENT_FAILURE_MS = 30L * 60L * 1_000L
private const val PLAYBACK_DIAGNOSTIC_FIELD_MAX_CHARS = 240
private val PLAYBACK_DIAGNOSTIC_SECRET_PATTERN = Regex(
    """(?i)\b(authorization|cookie|set-cookie|pot|potoken|token|access[_ -]?token|api[_ -]?key|x-goog-api-key)\s*[:=]\s*\S+"""
)
private val PLAYBACK_DIAGNOSTIC_BEARER_PATTERN = Regex("""(?i)\bbearer\s+\S+""")

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
        bitrateKbps?.let { add("${it} kbps") }
        if (width != null && height != null) add("${width}x$height")
        channels?.let { add("${it} ch") }
        sampleRateHz?.let { add("${it} Hz") }
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

internal data class PlaybackDiagnosticSnapshot(
    val status: PlaybackDiagnosticStatus,
    val appVersion: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val source: String,
    val videoMode: Boolean,
    val playerState: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    val bufferedPositionMs: Long,
    val durationMs: Long,
    val playbackSpeed: Float,
    val audioSessionId: Int?,
    val audioFormat: PlaybackDiagnosticFormat?,
    val videoFormat: PlaybackDiagnosticFormat?,
    val cacheBytes: Long,
    val networkTransport: String,
    val networkValidated: Boolean,
    val networkMetered: Boolean,
    val playerErrorCode: String,
    val strategies: List<PlaybackDiagnosticStrategy>
) {
    fun safeReport(): String = buildString {
        appendLine("LEVYRA PLAYBACK DIAGNOSTICS")
        appendLine("App: ${sanitizeDiagnosticField(appVersion)}")
        appendLine("Status: ${status.name}")
        appendLine()
        appendLine("Track")
        appendLine("  ID: ${safeDiagnosticValue(trackId)}")
        appendLine("  Title: ${safeDiagnosticValue(title)}")
        appendLine("  Artist: ${safeDiagnosticValue(artist)}")
        appendLine("  Source: ${safeDiagnosticValue(source)}")
        appendLine("  Mode: ${if (videoMode) "video" else "audio"}")
        appendLine()
        appendLine("Player")
        appendLine("  State: ${safeDiagnosticValue(playerState)}")
        appendLine("  Playing: $isPlaying")
        appendLine("  Position: ${positionMs.coerceAtLeast(0L)} ms")
        appendLine("  Buffered: ${bufferedPositionMs.coerceAtLeast(0L)} ms")
        appendLine("  Duration: ${durationMs.coerceAtLeast(0L)} ms")
        appendLine("  Speed: ${String.format(Locale.ROOT, "%.2fx", playbackSpeed)}")
        audioSessionId?.let { appendLine("  Audio session: $it") }
        if (playerErrorCode.isNotBlank()) appendLine("  Error code: ${safeDiagnosticValue(playerErrorCode)}")
        appendLine()
        appendLine("Formats")
        appendLine("  Audio: ${safeDiagnosticValue(audioFormat?.summary().orEmpty())}")
        appendLine("  Video: ${safeDiagnosticValue(videoFormat?.summary().orEmpty())}")
        appendLine()
        appendLine("Cache / Network")
        appendLine("  Cache: ${cacheBytes.coerceAtLeast(0L)} bytes")
        appendLine("  Transport: ${safeDiagnosticValue(networkTransport)}")
        appendLine("  Validated: $networkValidated")
        appendLine("  Metered: $networkMetered")
        if (strategies.isNotEmpty()) {
            appendLine()
            appendLine("Resolver strategy health")
            strategies.forEach { strategy ->
                append("  ${safeDiagnosticValue(strategy.name)}: ok=${strategy.successes} fail=${strategy.failures}")
                append(" streak=${strategy.consecutiveFailures} circuit=${strategy.circuit.name}")
                strategy.averageLatencyMs?.let { append(" avg=${it.coerceAtLeast(0L)}ms") }
                if (strategy.lastFailure.isNotBlank()) append(" last=${safeDiagnosticValue(strategy.lastFailure)}")
                appendLine()
            }
        }
        appendLine()
        append("Security: signed media URLs, request headers, cookies, tokens and API keys are intentionally excluded.")
    }
}

internal fun sanitizeDiagnosticField(value: String): String {
    val singleLine = value
        .replace('\r', ' ')
        .replace('\n', ' ')
        .trim()
    if (singleLine.isBlank()) return ""
    if (singleLine.contains("://", ignoreCase = true)) return "[redacted]"
    return PLAYBACK_DIAGNOSTIC_BEARER_PATTERN
        .replace(
            PLAYBACK_DIAGNOSTIC_SECRET_PATTERN.replace(singleLine) { match ->
                "${match.groupValues[1]}=[redacted]"
            },
            "Bearer [redacted]"
        )
        .take(PLAYBACK_DIAGNOSTIC_FIELD_MAX_CHARS)
}

private fun safeDiagnosticValue(value: String): String = sanitizeDiagnosticField(value).ifBlank { "-" }

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
            (strategy.lastFailureAtMs > 0L &&
                nowMs >= strategy.lastFailureAtMs &&
                nowMs - strategy.lastFailureAtMs <= PLAYBACK_DIAGNOSTIC_RECENT_FAILURE_MS)
    } -> PlaybackDiagnosticStatus.FALLBACK_HISTORY
    else -> PlaybackDiagnosticStatus.HEALTHY
}

@UnstableApi
internal class PlaybackDiagnosticsReader(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
    private val strategyHealthPreferences = appContext.getSharedPreferences(
        "levyra_playback_strategy_health",
        Context.MODE_PRIVATE
    )

    suspend fun capture(fallbackTrack: Track? = null): PlaybackDiagnosticSnapshot {
        val playerSnapshot = withContext(Dispatchers.Main.immediate) {
            capturePlayerState(fallbackTrack)
        }
        val now = System.currentTimeMillis()
        val strategyHealth = withContext(Dispatchers.IO) {
            readPersistedStrategyHealth(playerSnapshot.videoMode, now)
        }
        return PlaybackDiagnosticSnapshot(
            status = playbackDiagnosticStatus(
                errorCode = playerSnapshot.playerErrorCode,
                playbackState = playerSnapshot.playbackState,
                strategies = strategyHealth,
                nowMs = now
            ),
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            trackId = playerSnapshot.trackId,
            title = playerSnapshot.title,
            artist = playerSnapshot.artist,
            source = playerSnapshot.source,
            videoMode = playerSnapshot.videoMode,
            playerState = playerSnapshot.playbackState?.let(::playerStateName) ?: "UNAVAILABLE",
            isPlaying = playerSnapshot.isPlaying,
            positionMs = playerSnapshot.positionMs,
            bufferedPositionMs = playerSnapshot.bufferedPositionMs,
            durationMs = playerSnapshot.durationMs,
            playbackSpeed = playerSnapshot.playbackSpeed,
            audioSessionId = playerSnapshot.audioSessionId,
            audioFormat = playerSnapshot.audioFormat,
            videoFormat = playerSnapshot.videoFormat,
            cacheBytes = playerSnapshot.cacheBytes,
            networkTransport = playerSnapshot.network.transport,
            networkValidated = playerSnapshot.network.validated,
            networkMetered = playerSnapshot.network.metered,
            playerErrorCode = playerSnapshot.playerErrorCode,
            strategies = strategyHealth
        )
    }

    private fun capturePlayerState(fallbackTrack: Track?): PlaybackPlayerDiagnosticState {
        val player = PlaybackService.activePlayer
        val item = player?.currentMediaItem
        val extras = item?.mediaMetadata?.extras
        val selectedFormats = player?.let(::selectedFormats).orEmpty()
        val audio = selectedFormats.firstOrNull { it.sampleMimeType?.startsWith("audio/") == true }
        val video = selectedFormats.firstOrNull { it.sampleMimeType?.startsWith("video/") == true }
        val title = item?.mediaMetadata?.title?.toString().orEmpty().ifBlank { fallbackTrack?.title.orEmpty() }
        val artist = item?.mediaMetadata?.artist?.toString().orEmpty().ifBlank { fallbackTrack?.artist.orEmpty() }
        val source = extras?.getString("levyra.source").orEmpty().ifBlank { fallbackTrack?.source.orEmpty() }
        val duration = player?.duration?.takeIf { it != C.TIME_UNSET && it >= 0L }
            ?: fallbackTrack?.durationMs?.coerceAtLeast(0L)
            ?: 0L
        return PlaybackPlayerDiagnosticState(
            trackId = item?.mediaId.orEmpty().ifBlank { fallbackTrack?.id.orEmpty() },
            title = title,
            artist = artist,
            source = source,
            videoMode = extras?.getBoolean(PlaybackService.EXTRA_VIDEO_MODE, false) ?: false,
            playbackState = player?.playbackState,
            isPlaying = player?.isPlaying == true,
            positionMs = player?.currentPosition?.coerceAtLeast(0L) ?: 0L,
            bufferedPositionMs = player?.bufferedPosition?.coerceAtLeast(0L) ?: 0L,
            durationMs = duration,
            playbackSpeed = player?.playbackParameters?.speed ?: 1f,
            audioSessionId = player?.audioSessionId?.takeIf { it > 0 },
            audioFormat = audio?.toDiagnosticFormat(),
            videoFormat = video?.toDiagnosticFormat(),
            cacheBytes = LevyraMediaCache.currentCacheSpace(),
            network = networkSnapshot(),
            playerErrorCode = player?.playerError?.errorCodeName.orEmpty()
        )
    }

    private fun readPersistedStrategyHealth(
        videoMode: Boolean,
        nowMs: Long
    ): List<PlaybackDiagnosticStrategy> {
        val mode = if (videoMode) "video" else "audio"
        val prefix = "$mode::"
        val raw = strategyHealthPreferences.getString("health", null)
        return parsePlaybackStrategyHealthSnapshot(raw)
            .entries
            .asSequence()
            .filter { (key, _) -> key.startsWith(prefix) }
            .sortedWith(
                compareByDescending<Map.Entry<String, PlaybackStrategyStats>> { it.value.updatedAtMs }
                    .thenBy { it.key }
            )
            .take(8)
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

    private fun networkSnapshot(): PlaybackDiagnosticNetwork {
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
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
            metered = connectivityManager.isActiveNetworkMetered
        )
    }
}

private data class PlaybackPlayerDiagnosticState(
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
    val cacheBytes: Long,
    val network: PlaybackDiagnosticNetwork,
    val playerErrorCode: String
)

private data class PlaybackDiagnosticNetwork(
    val transport: String,
    val validated: Boolean,
    val metered: Boolean
)

private fun selectedFormats(player: androidx.media3.exoplayer.ExoPlayer): List<Format> = buildList {
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
