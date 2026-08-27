package com.luc4n3x.levyra.runtime

import java.util.ArrayDeque
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal const val DIAGNOSTICS_MARKER = "LEVYRA_PR_DIAGNOSTICS_V1"
internal const val RECORDER_CAPACITY = 512
internal const val ANOMALY_CAPACITY = 64
internal const val MEMORY_SAMPLE_INTERVAL_MS = 15_000L
internal const val MAX_EVENTS_EXPORT_BYTES = 1_048_576L
internal const val MAX_DIAGNOSTICS_EXPORT_BYTES = 2_097_152L

internal enum class DiagnosticCategory {
    MEMORY,
    PLAYER,
    RESOLVER,
    CACHE,
    DSP,
    CANVAS,
    NETWORK,
    HOT_OPERATION,
    PREFLIGHT
}

@Serializable
internal enum class PlaybackMode {
    AUDIO,
    VIDEO,
    DECORATIVE,
    UNKNOWN
}

@Serializable
internal enum class PlayerAction {
    CREATED,
    RELEASED,
    PREPARE,
    TRANSITION,
    ERROR,
    RECOVERY,
    RENDERER_STARTED,
    RENDERER_STOPPED,
    DECODER_CREATED,
    DECODER_RELEASED,
    STATE
}

@Serializable
internal enum class DiagnosticPlayerState {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    UNKNOWN
}

@Serializable
internal enum class ResolverStrategy {
    REEL_MUXED,
    REEL_AUDIO,
    PERSISTED,
    DIRECT,
    SEARCH,
    STANDARD_VIDEO,
    REEL_VIDEO,
    UNKNOWN
}

@Serializable
internal enum class ResolverClient {
    VISION_OS,
    ANDROID_VR,
    ANDROID_MUSIC,
    ANDROID,
    IOS,
    WEB_REMIX,
    WEB,
    WEB_EMBEDDED,
    UNKNOWN
}

@Serializable
internal enum class ResolverMime {
    AUDIO_MP4,
    AUDIO_WEBM,
    VIDEO_MP4,
    VIDEO_WEBM,
    HLS,
    OTHER,
    UNKNOWN
}

@Serializable
internal enum class ResolverContainer {
    MP4,
    WEBM,
    M3U8,
    OTHER,
    UNKNOWN
}

@Serializable
internal enum class ResolverCodec {
    AAC,
    OPUS,
    VORBIS,
    AVC,
    HEVC,
    VP9,
    AV1,
    OTHER,
    UNKNOWN
}

@Serializable
internal enum class DiagnosticOutcome {
    SUCCESS,
    FAILURE,
    TIMEOUT,
    CANCELLED
}

@Serializable
internal enum class FailureCategory {
    FORBIDDEN,
    GONE,
    RATE_LIMITED,
    NOT_FOUND,
    RESOURCE_MISSING,
    RANGE_NOT_SATISFIABLE,
    SERVER,
    LOGIN_REQUIRED,
    CONTENT_RESTRICTED,
    EXPIRED,
    SIGNATURE,
    UNSUPPORTED_FORMAT,
    MALFORMED_CONTAINER,
    DECODER,
    NETWORK,
    TIMEOUT,
    TRUNCATED,
    UNKNOWN,
    NONE
}

@Serializable
internal enum class CacheAction {
    HIT,
    MISS,
    EVICTION,
    PREFETCH
}

@Serializable
internal enum class DspAction {
    CREATED,
    RECREATED,
    RELEASED
}

@Serializable
internal enum class CanvasAction {
    STARTED,
    FIRST_FRAME,
    FALLBACK,
    RESTARTED,
    STOPPED
}

@Serializable
internal enum class NetworkProvider {
    YOUTUBE,
    GITHUB,
    OTHER
}

@Serializable
internal enum class NetworkCategory {
    CONNECT,
    RESOLVE,
    CONFIG
}

@Serializable
internal enum class DiagnosticOperation {
    PLAYER_CREATE,
    PLAYER_PREPARE,
    RESOLVER_ATTEMPT,
    FALLBACK,
    NETWORK_RETRY,
    CACHE_ACCESS,
    DSP_RECREATE,
    CANVAS_RESTART
}

internal sealed interface DiagnosticEvent {
    val schemaVersion: Int
    val timestampMs: Long
    val uptimeMs: Long
    val category: DiagnosticCategory
}

@Serializable
internal data class MemorySampleEvent(
    override val timestampMs: Long,
    override val uptimeMs: Long,
    val pssKb: Long,
    val javaHeapKb: Long,
    val nativeHeapKb: Long,
    val rssKb: Long?,
    val threadCount: Int,
    val peakPssKb: Long,
    override val schemaVersion: Int = 1
) : DiagnosticEvent {
    override val category: DiagnosticCategory = DiagnosticCategory.MEMORY
}

@Serializable
internal data class PlayerEvent(
    override val timestampMs: Long,
    override val uptimeMs: Long,
    val action: PlayerAction,
    val mode: PlaybackMode,
    val state: DiagnosticPlayerState = DiagnosticPlayerState.UNKNOWN,
    val failure: FailureCategory = FailureCategory.NONE,
    val codec: ResolverCodec = ResolverCodec.UNKNOWN,
    override val schemaVersion: Int = 1
) : DiagnosticEvent {
    override val category: DiagnosticCategory = DiagnosticCategory.PLAYER
}

@Serializable
internal data class ResolverEvent(
    override val timestampMs: Long,
    override val uptimeMs: Long,
    val mode: PlaybackMode,
    val strategy: ResolverStrategy,
    val client: ResolverClient,
    val attempt: Int,
    val latencyMs: Long,
    val outcome: DiagnosticOutcome,
    val failure: FailureCategory,
    val itag: Int = -1,
    val mime: ResolverMime = ResolverMime.UNKNOWN,
    val container: ResolverContainer = ResolverContainer.UNKNOWN,
    val codec: ResolverCodec = ResolverCodec.UNKNOWN,
    val bitrate: Int = 0,
    override val schemaVersion: Int = 1
) : DiagnosticEvent {
    override val category: DiagnosticCategory = DiagnosticCategory.RESOLVER
}

@Serializable
internal data class CacheEvent(
    override val timestampMs: Long,
    override val uptimeMs: Long,
    val action: CacheAction,
    override val schemaVersion: Int = 1
) : DiagnosticEvent {
    override val category: DiagnosticCategory = DiagnosticCategory.CACHE
}

@Serializable
internal data class DspEvent(
    override val timestampMs: Long,
    override val uptimeMs: Long,
    val action: DspAction,
    override val schemaVersion: Int = 1
) : DiagnosticEvent {
    override val category: DiagnosticCategory = DiagnosticCategory.DSP
}

@Serializable
internal data class CanvasEvent(
    override val timestampMs: Long,
    override val uptimeMs: Long,
    val action: CanvasAction,
    override val schemaVersion: Int = 1
) : DiagnosticEvent {
    override val category: DiagnosticCategory = DiagnosticCategory.CANVAS
}

@Serializable
internal data class NetworkEvent(
    override val timestampMs: Long,
    override val uptimeMs: Long,
    val provider: NetworkProvider,
    val requestCategory: NetworkCategory,
    val statusCode: Int,
    val latencyMs: Long,
    val retry: Int,
    val outcome: DiagnosticOutcome,
    val failure: FailureCategory,
    val redirectCount: Int,
    override val schemaVersion: Int = 1
) : DiagnosticEvent {
    override val category: DiagnosticCategory = DiagnosticCategory.NETWORK
}

@Serializable
internal data class HotOperationEvent(
    override val timestampMs: Long,
    override val uptimeMs: Long,
    val operation: DiagnosticOperation,
    override val schemaVersion: Int = 1
) : DiagnosticEvent {
    override val category: DiagnosticCategory = DiagnosticCategory.HOT_OPERATION
}

@Serializable
internal data class PreflightEvent(
    override val timestampMs: Long,
    override val uptimeMs: Long,
    val passCount: Int,
    val warningCount: Int,
    val failCount: Int,
    override val schemaVersion: Int = 1
) : DiagnosticEvent {
    override val category: DiagnosticCategory = DiagnosticCategory.PREFLIGHT
}

internal class BoundedFlightRecorder(private val capacity: Int = RECORDER_CAPACITY) {
    private val events = ArrayDeque<DiagnosticEvent>(capacity)

    init {
        require(capacity > 0)
    }

    @Synchronized
    fun record(event: DiagnosticEvent) {
        while (events.size >= capacity) events.removeFirst()
        events.addLast(event)
    }

    @Synchronized
    fun snapshot(): List<DiagnosticEvent> = events.toList()

    @Synchronized
    fun clear() = events.clear()

    @Synchronized
    fun size(): Int = events.size
}

internal val diagnosticsJson = Json {
    encodeDefaults = true
    explicitNulls = false
}

internal fun DiagnosticEvent.encodeJson(): String = when (this) {
    is MemorySampleEvent -> diagnosticsJson.encodeToString(this)
    is PlayerEvent -> diagnosticsJson.encodeToString(this)
    is ResolverEvent -> diagnosticsJson.encodeToString(this)
    is CacheEvent -> diagnosticsJson.encodeToString(this)
    is DspEvent -> diagnosticsJson.encodeToString(this)
    is CanvasEvent -> diagnosticsJson.encodeToString(this)
    is NetworkEvent -> diagnosticsJson.encodeToString(this)
    is HotOperationEvent -> diagnosticsJson.encodeToString(this)
    is PreflightEvent -> diagnosticsJson.encodeToString(this)
}
