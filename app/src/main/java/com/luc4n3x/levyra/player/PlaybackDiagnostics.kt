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
import com.luc4n3x.levyra.data.parsePlaybackStrategyHealthSnapshot
import com.luc4n3x.levyra.domain.Track
import java.util.Locale

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
    val lastFailure: String
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
        appendLine("App: $appVersion")
        appendLine("Status: ${status.name}")
        appendLine()
        appendLine("Track")
        appendLine("  ID: ${trackId.ifBlank { "-" }}")
        appendLine("  Title: ${title.ifBlank { "-" }}")
        appendLine("  Artist: ${artist.ifBlank { "-" }}")
        appendLine("  Source: ${source.ifBlank { "-" }}")
        appendLine("  Mode: ${if (videoMode) "video" else "audio"}")
        appendLine()
        appendLine("Player")
        appendLine("  State: $playerState")
        appendLine("  Playing: $isPlaying")
        appendLine("  Position: ${positionMs.coerceAtLeast(0L)} ms")
        appendLine("  Buffered: ${bufferedPositionMs.coerceAtLeast(0L)} ms")
        appendLine("  Duration: ${durationMs.coerceAtLeast(0L)} ms")
        appendLine("  Speed: ${String.format(Locale.ROOT, "%.2fx", playbackSpeed)}")
        audioSessionId?.let { appendLine("  Audio session: $it") }
        if (playerErrorCode.isNotBlank()) appendLine("  Error code: $playerErrorCode")
        appendLine()
        appendLine("Formats")
        appendLine("  Audio: ${audioFormat?.summary().orEmpty().ifBlank { "-" }}")
        appendLine("  Video: ${videoFormat?.summary().orEmpty().ifBlank { "-" }}")
        appendLine()
        appendLine("Cache / Network")
        appendLine("  Cache: $cacheBytes bytes")
        appendLine("  Transport: $networkTransport")
        appendLine("  Validated: $networkValidated")
        appendLine("  Metered: $networkMetered")
        if (strategies.isNotEmpty()) {
            appendLine()
            appendLine("Resolver strategy health")
            strategies.forEach { strategy ->
                append("  ${strategy.name}: ok=${strategy.successes} fail=${strategy.failures}")
                append(" streak=${strategy.consecutiveFailures} circuit=${strategy.circuit.name}")
                strategy.averageLatencyMs?.let { append(" avg=${it}ms") }
                if (strategy.lastFailure.isNotBlank()) append(" last=${strategy.lastFailure}")
                appendLine()
            }
        }
        appendLine()
        append("Security: signed media URLs, request headers, cookies, tokens and API keys are intentionally excluded.")
    }
}

@UnstableApi
internal class PlaybackDiagnosticsReader(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)

    fun capture(fallbackTrack: Track? = null): PlaybackDiagnosticSnapshot {
        val player = PlaybackService.activePlayer
        val item = player?.currentMediaItem
        val extras = item?.mediaMetadata?.extras
        val selectedFormats = player?.let(::selectedFormats).orEmpty()
        val audio = selectedFormats.firstOrNull { it.sampleMimeType?.startsWith("audio/") == true }
        val video = selectedFormats.firstOrNull { it.sampleMimeType?.startsWith("video/") == true }
        val strategyHealth = readPersistedStrategyHealth()
        val errorCode = player?.playerError?.errorCodeName.orEmpty()
        val state = player?.playbackState?.let(::playerStateName) ?: "UNAVAILABLE"
        val status = when {
            errorCode.isNotBlank() -> PlaybackDiagnosticStatus.ERROR
            player == null || player.playbackState == Player.STATE_IDLE -> PlaybackDiagnosticStatus.IDLE
            strategyHealth.any { it.consecutiveFailures > 0 || it.circuit != PlaybackStrategyCircuit.CLOSED } ->
                PlaybackDiagnosticStatus.FALLBACK_HISTORY
            else -> PlaybackDiagnosticStatus.HEALTHY
        }
        val network = networkSnapshot()
        val title = item?.mediaMetadata?.title?.toString().orEmpty().ifBlank { fallbackTrack?.title.orEmpty() }
        val artist = item?.mediaMetadata?.artist?.toString().orEmpty().ifBlank { fallbackTrack?.artist.orEmpty() }
        val source = extras?.getString("levyra.source").orEmpty().ifBlank { fallbackTrack?.source.orEmpty() }
        val duration = player?.duration?.takeIf { it != C.TIME_UNSET && it >= 0L }
            ?: fallbackTrack?.durationMs?.coerceAtLeast(0L)
            ?: 0L

        return PlaybackDiagnosticSnapshot(
            status = status,
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            trackId = item?.mediaId.orEmpty().ifBlank { fallbackTrack?.id.orEmpty() },
            title = title,
            artist = artist,
            source = source,
            videoMode = extras?.getBoolean(PlaybackService.EXTRA_VIDEO_MODE, false) ?: false,
            playerState = state,
            isPlaying = player?.isPlaying == true,
            positionMs = player?.currentPosition?.coerceAtLeast(0L) ?: 0L,
            bufferedPositionMs = player?.bufferedPosition?.coerceAtLeast(0L) ?: 0L,
            durationMs = duration,
            playbackSpeed = player?.playbackParameters?.speed ?: 1f,
            audioSessionId = player?.audioSessionId?.takeIf { it > 0 },
            audioFormat = audio?.toDiagnosticFormat(),
            videoFormat = video?.toDiagnosticFormat(),
            cacheBytes = LevyraMediaCache.currentCacheSpace(),
            networkTransport = network.transport,
            networkValidated = network.validated,
            networkMetered = network.metered,
            playerErrorCode = errorCode,
            strategies = strategyHealth
        )
    }

    private fun readPersistedStrategyHealth(): List<PlaybackDiagnosticStrategy> {
        // Read-only access avoids constructing PlaybackStrategyHealthStore, which owns a persistence executor.
        val raw = appContext
            .getSharedPreferences("levyra_playback_strategy_health", Context.MODE_PRIVATE)
            .getString("health", null)
        val now = System.currentTimeMillis()
        return parsePlaybackStrategyHealthSnapshot(raw)
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, com.luc4n3x.levyra.data.PlaybackStrategyStats>> {
                    it.value.updatedAtMs
                }.thenBy { it.key }
            )
            .take(8)
            .map { (key, stats) ->
                PlaybackDiagnosticStrategy(
                    name = key,
                    successes = stats.successes,
                    failures = stats.failures,
                    consecutiveFailures = stats.consecutiveFailures,
                    averageLatencyMs = stats.averageLatencyMs.takeIf { it != Long.MAX_VALUE },
                    circuit = stats.circuitAt(now),
                    lastFailure = stats.lastFailureKind?.name.orEmpty()
                )
            }
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
