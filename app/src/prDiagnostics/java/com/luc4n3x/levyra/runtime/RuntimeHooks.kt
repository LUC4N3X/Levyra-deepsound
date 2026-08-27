package com.luc4n3x.levyra.runtime

import android.content.Context
import android.os.Debug
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import androidx.annotation.Keep
import androidx.compose.foundation.lazy.LazyListScope
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal object RuntimeSignal {
    const val PLAYER_CREATED = 1
    const val PLAYER_RELEASED = 2
    const val PLAYER_PREPARE = 3
    const val PLAYER_TRANSITION = 4
    const val PLAYER_ERROR = 5
    const val PLAYER_RECOVERY = 6
    const val PLAYER_STATE = 7
    const val MODE_AUDIO = 0
    const val MODE_VIDEO = 1
    const val MODE_DECORATIVE = 2
    const val OUTCOME_SUCCESS = 1
    const val OUTCOME_FAILURE = 2
    const val OUTCOME_TIMEOUT = 3
    const val OUTCOME_CANCELLED = 4
    const val CACHE_HIT = 1
    const val CACHE_MISS = 2
    const val CACHE_EVICTION = 3
    const val CACHE_PREFETCH = 4
    const val DSP_CREATED = 1
    const val DSP_RECREATED = 2
    const val DSP_RELEASED = 3
    const val CANVAS_STARTED = 1
    const val CANVAS_FIRST_FRAME = 2
    const val CANVAS_FALLBACK = 3
    const val CANVAS_RESTARTED = 4
    const val CANVAS_STOPPED = 5
    const val NETWORK_CONNECT = 1
    const val NETWORK_RESOLVE = 2
    const val NETWORK_CONFIG = 3
    const val HOT_PLAYER_CREATE = 1
    const val HOT_PLAYER_PREPARE = 2
    const val HOT_RESOLVER_ATTEMPT = 3
    const val HOT_FALLBACK = 4
    const val HOT_NETWORK_RETRY = 5
    const val HOT_CACHE_ACCESS = 6
    const val HOT_DSP_RECREATE = 7
    const val HOT_CANVAS_RESTART = 8
    const val FAILURE_NETWORK = 14
    const val FAILURE_TIMEOUT = 15
    const val FAILURE_UNKNOWN = 17
}

@Keep
internal object PrDiagnosticsArtifactMarker {
    const val value = DIAGNOSTICS_MARKER
    @JvmField
    val components = arrayOf(
        "LEVYRA_DIAGNOSTICS_FLIGHT_RECORDER",
        "LEVYRA_DIAGNOSTICS_ANOMALY_DETECTOR",
        "LEVYRA_DIAGNOSTICS_PREFLIGHT",
        "LEVYRA_DIAGNOSTICS_INTERNAL_UI",
        "LEVYRA_DIAGNOSTICS_EXPORTER"
    )
}

internal object RuntimeHooks {
    fun start(context: Context) = RuntimeDiagnostics.start(context)

    fun attachPlayer(player: ExoPlayer) {
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioEnabled(eventTime: AnalyticsListener.EventTime, decoderCounters: DecoderCounters) {
                recordPipeline(PlayerAction.RENDERER_STARTED, PlaybackMode.AUDIO)
            }

            override fun onAudioDisabled(eventTime: AnalyticsListener.EventTime, decoderCounters: DecoderCounters) {
                recordPipeline(PlayerAction.RENDERER_STOPPED, PlaybackMode.AUDIO)
            }

            override fun onVideoEnabled(eventTime: AnalyticsListener.EventTime, decoderCounters: DecoderCounters) {
                recordPipeline(PlayerAction.RENDERER_STARTED, PlaybackMode.VIDEO)
            }

            override fun onVideoDisabled(eventTime: AnalyticsListener.EventTime, decoderCounters: DecoderCounters) {
                recordPipeline(PlayerAction.RENDERER_STOPPED, PlaybackMode.VIDEO)
            }

            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                recordPipeline(PlayerAction.DECODER_CREATED, PlaybackMode.AUDIO, decoderName)
            }

            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                recordPipeline(PlayerAction.DECODER_CREATED, PlaybackMode.VIDEO, decoderName)
            }

            override fun onAudioDecoderReleased(eventTime: AnalyticsListener.EventTime, decoderName: String) {
                recordPipeline(PlayerAction.DECODER_RELEASED, PlaybackMode.AUDIO, decoderName)
            }

            override fun onVideoDecoderReleased(eventTime: AnalyticsListener.EventTime, decoderName: String) {
                recordPipeline(PlayerAction.DECODER_RELEASED, PlaybackMode.VIDEO, decoderName)
            }
        })
    }

    fun player(action: Int, value: Int = 0, mode: Int = RuntimeSignal.MODE_AUDIO, failure: Int = -1) {
        RuntimeDiagnostics.record(
            PlayerEvent(
                timestampMs = nowMs(),
                uptimeMs = uptimeMs(),
                action = action.playerAction(),
                mode = mode.playbackMode(),
                state = value.playerState(),
                failure = failure.failureCategory()
            )
        )
    }

    fun resolver(
        mode: Int,
        strategy: Int,
        client: Int,
        attempt: Int,
        latencyMs: Long,
        outcome: Int,
        failure: Int,
        manifest: ResolvedPlaybackManifest? = null
    ) {
        val playbackMode = mode.playbackMode()
        val format = SafeDiagnosticMetadata.streamFormat(manifest, playbackMode)
        RuntimeDiagnostics.record(
            ResolverEvent(
                timestampMs = nowMs(),
                uptimeMs = uptimeMs(),
                mode = playbackMode,
                strategy = resolverStrategy(playbackMode, strategy),
                client = ResolverClient.entries.getOrElse(client) { ResolverClient.UNKNOWN },
                attempt = attempt.coerceIn(1, 32),
                latencyMs = latencyMs.coerceIn(0L, 120_000L),
                outcome = outcome.diagnosticOutcome(),
                failure = failure.failureCategory(),
                itag = format.itag,
                mime = format.mime,
                container = format.container,
                codec = format.codec,
                bitrate = format.bitrate
            )
        )
    }

    fun cache(action: Int) {
        RuntimeDiagnostics.record(CacheEvent(nowMs(), uptimeMs(), action.cacheAction()))
    }

    fun dsp(action: Int) {
        RuntimeDiagnostics.record(DspEvent(nowMs(), uptimeMs(), action.dspAction()))
    }

    fun canvas(action: Int) {
        RuntimeDiagnostics.record(CanvasEvent(nowMs(), uptimeMs(), action.canvasAction()))
    }

    fun network(
        host: String,
        category: Int,
        latencyMs: Long,
        outcome: Int,
        statusCode: Int = 0,
        retry: Int = 0,
        redirects: Int = 0,
        failure: Int = -1
    ) {
        RuntimeDiagnostics.record(
            NetworkEvent(
                timestampMs = nowMs(),
                uptimeMs = uptimeMs(),
                provider = SafeDiagnosticMetadata.provider(host),
                requestCategory = category.networkCategory(),
                statusCode = statusCode.takeIf { it in 100..599 } ?: 0,
                latencyMs = latencyMs.coerceIn(0L, 120_000L),
                retry = retry.coerceIn(0, 16),
                outcome = outcome.diagnosticOutcome(),
                failure = failure.failureCategory(),
                redirectCount = redirects.coerceIn(0, 16)
            )
        )
    }

    fun hot(operation: Int) {
        RuntimeDiagnostics.record(HotOperationEvent(nowMs(), uptimeMs(), operation.diagnosticOperation()))
    }

    fun internalPanelEntry(scope: LazyListScope) {
        scope.item(key = "pr-internal-diagnostics") { InternalDiagnosticsEntry() }
    }

    private fun recordPipeline(action: PlayerAction, mode: PlaybackMode, decoderName: String = "") {
        RuntimeDiagnostics.record(
            PlayerEvent(
                timestampMs = nowMs(),
                uptimeMs = uptimeMs(),
                action = action,
                mode = mode,
                codec = SafeDiagnosticMetadata.codec(decoderName)
            )
        )
    }
}

internal data class RuntimeDiagnosticsSnapshot(
    val active: Boolean,
    val eventCount: Int,
    val anomalies: List<DiagnosticAnomaly>,
    val currentMemory: MemorySampleEvent?,
    val playerState: DiagnosticPlayerState,
    val resolverState: ResolverEvent?,
    val preflight: PreflightReport?
)

internal object RuntimeDiagnostics {
    private val started = AtomicBoolean(false)
    private val recorder = BoundedFlightRecorder()
    private val detector = RuntimeAnomalyDetector()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var peakPssKb = 0L
    private var lastPreflight: PreflightReport? = null
    private lateinit var appContext: Context

    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        check(PrDiagnosticsArtifactMarker.value == DIAGNOSTICS_MARKER)
        check(PrDiagnosticsArtifactMarker.components.size == 5)
        appContext = context.applicationContext
        scope.launch {
            while (isActive) {
                record(memorySample(appContext))
                delay(MEMORY_SAMPLE_INTERVAL_MS)
            }
        }
    }

    fun record(event: DiagnosticEvent) {
        synchronized(lock) {
            recorder.record(event)
            detector.observe(event)
        }
    }

    fun snapshot(): RuntimeDiagnosticsSnapshot = synchronized(lock) {
        val events = recorder.snapshot()
        RuntimeDiagnosticsSnapshot(
            active = started.get(),
            eventCount = events.size,
            anomalies = detector.snapshot(),
            currentMemory = events.filterIsInstance<MemorySampleEvent>().lastOrNull(),
            playerState = events.filterIsInstance<PlayerEvent>().lastOrNull { it.action == PlayerAction.STATE }?.state
                ?: DiagnosticPlayerState.UNKNOWN,
            resolverState = events.filterIsInstance<ResolverEvent>().lastOrNull(),
            preflight = lastPreflight
        )
    }

    fun eventSnapshot(): List<DiagnosticEvent> = synchronized(lock) { recorder.snapshot() }

    fun anomalySnapshot(): List<DiagnosticAnomaly> = synchronized(lock) { detector.snapshot() }

    fun runPreflight(context: Context): PreflightReport {
        val report = RuntimePreflight.run(context.applicationContext)
        synchronized(lock) { lastPreflight = report }
        record(
            PreflightEvent(
                timestampMs = nowMs(),
                uptimeMs = uptimeMs(),
                passCount = report.results.count { it.status == PreflightStatus.PASS },
                warningCount = report.results.count { it.status == PreflightStatus.WARNING },
                failCount = report.results.count { it.status == PreflightStatus.FAIL }
            )
        )
        return report
    }

    fun reset() {
        synchronized(lock) {
            recorder.clear()
            detector.clear()
            peakPssKb = 0L
            lastPreflight = null
        }
    }

    private fun memorySample(context: Context): MemorySampleEvent {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        val runtime = Runtime.getRuntime()
        val pssKb = info.totalPss.toLong().coerceAtLeast(0L)
        val currentPeak = synchronized(lock) {
            peakPssKb = maxOf(peakPssKb, pssKb)
            peakPssKb
        }
        return MemorySampleEvent(
            timestampMs = nowMs(),
            uptimeMs = uptimeMs(),
            pssKb = pssKb,
            javaHeapKb = ((runtime.totalMemory() - runtime.freeMemory()) / 1024L).coerceAtLeast(0L),
            nativeHeapKb = (Debug.getNativeHeapAllocatedSize() / 1024L).coerceAtLeast(0L),
            rssKb = readRssKb(),
            threadCount = (File("/proc/self/task").list()?.size ?: Thread.activeCount()).coerceAtLeast(1),
            peakPssKb = currentPeak
        )
    }
}

internal object SafeDiagnosticMetadata {
    fun provider(host: String): NetworkProvider {
        val normalized = host.trim().lowercase(Locale.ROOT).trimEnd('.')
        return when {
            normalized == "youtube.com" || normalized.endsWith(".youtube.com") ||
                normalized == "googlevideo.com" || normalized.endsWith(".googlevideo.com") ||
                normalized == "googleapis.com" || normalized.endsWith(".googleapis.com") -> NetworkProvider.YOUTUBE
            normalized == "github.com" || normalized.endsWith(".github.com") ||
                normalized == "githubusercontent.com" || normalized.endsWith(".githubusercontent.com") -> NetworkProvider.GITHUB
            else -> NetworkProvider.OTHER
        }
    }

    fun streamFormat(manifest: ResolvedPlaybackManifest?, mode: PlaybackMode): SafeResolverFormat {
        val descriptor = manifest?.streams?.firstOrNull { stream ->
            stream.selected && when (mode) {
                PlaybackMode.AUDIO -> stream.kind == PlaybackStreamKind.AUDIO || stream.kind == PlaybackStreamKind.MUXED
                PlaybackMode.VIDEO -> stream.kind != PlaybackStreamKind.AUDIO
                else -> false
            }
        } ?: return SafeResolverFormat()
        val mime = descriptor.mimeType.substringBefore(';').trim().lowercase(Locale.ROOT)
        val container = descriptor.container.trim().lowercase(Locale.ROOT)
        val codec = descriptor.codec.trim().lowercase(Locale.ROOT)
        return SafeResolverFormat(
            itag = descriptor.itag.coerceIn(-1, 99_999),
            mime = when (mime) {
                "audio/mp4" -> ResolverMime.AUDIO_MP4
                "audio/webm" -> ResolverMime.AUDIO_WEBM
                "video/mp4" -> ResolverMime.VIDEO_MP4
                "video/webm" -> ResolverMime.VIDEO_WEBM
                "application/x-mpegurl", "application/vnd.apple.mpegurl" -> ResolverMime.HLS
                "" -> ResolverMime.UNKNOWN
                else -> ResolverMime.OTHER
            },
            container = when (container) {
                "mp4", "m4a" -> ResolverContainer.MP4
                "webm" -> ResolverContainer.WEBM
                "m3u8" -> ResolverContainer.M3U8
                "" -> ResolverContainer.UNKNOWN
                else -> ResolverContainer.OTHER
            },
            codec = codec(codec),
            bitrate = maxOf(descriptor.bitrate, descriptor.averageBitrate).coerceIn(0, 100_000_000)
        )
    }

    fun codec(rawValue: String): ResolverCodec {
        val value = rawValue.trim().lowercase(Locale.ROOT)
        return when {
            value.isBlank() -> ResolverCodec.UNKNOWN
            "mp4a" in value || "aac" in value -> ResolverCodec.AAC
            "opus" in value -> ResolverCodec.OPUS
            "vorbis" in value -> ResolverCodec.VORBIS
            "avc" in value || "h264" in value -> ResolverCodec.AVC
            "hvc" in value || "hevc" in value -> ResolverCodec.HEVC
            "vp9" in value || "vp09" in value -> ResolverCodec.VP9
            "av1" in value || "av01" in value -> ResolverCodec.AV1
            else -> ResolverCodec.OTHER
        }
    }
}

internal data class SafeResolverFormat(
    val itag: Int = -1,
    val mime: ResolverMime = ResolverMime.UNKNOWN,
    val container: ResolverContainer = ResolverContainer.UNKNOWN,
    val codec: ResolverCodec = ResolverCodec.UNKNOWN,
    val bitrate: Int = 0
)

private fun nowMs(): Long = System.currentTimeMillis()
private fun uptimeMs(): Long = SystemClock.elapsedRealtime()

private fun readRssKb(): Long? = runCatching {
    val pages = File("/proc/self/statm").useLines { lines ->
        lines.firstOrNull()?.trim()?.split(' ')?.getOrNull(1)?.toLongOrNull()
    } ?: return@runCatching null
    pages * (Os.sysconf(OsConstants._SC_PAGESIZE) / 1024L)
}.getOrNull()

private fun Int.playbackMode(): PlaybackMode = when (this) {
    RuntimeSignal.MODE_AUDIO -> PlaybackMode.AUDIO
    RuntimeSignal.MODE_VIDEO -> PlaybackMode.VIDEO
    RuntimeSignal.MODE_DECORATIVE -> PlaybackMode.DECORATIVE
    else -> PlaybackMode.UNKNOWN
}

private fun Int.playerAction(): PlayerAction = when (this) {
    RuntimeSignal.PLAYER_CREATED -> PlayerAction.CREATED
    RuntimeSignal.PLAYER_RELEASED -> PlayerAction.RELEASED
    RuntimeSignal.PLAYER_PREPARE -> PlayerAction.PREPARE
    RuntimeSignal.PLAYER_TRANSITION -> PlayerAction.TRANSITION
    RuntimeSignal.PLAYER_ERROR -> PlayerAction.ERROR
    RuntimeSignal.PLAYER_RECOVERY -> PlayerAction.RECOVERY
    else -> PlayerAction.STATE
}

private fun Int.playerState(): DiagnosticPlayerState = when (this) {
    1 -> DiagnosticPlayerState.IDLE
    2 -> DiagnosticPlayerState.BUFFERING
    3 -> DiagnosticPlayerState.READY
    4 -> DiagnosticPlayerState.ENDED
    else -> DiagnosticPlayerState.UNKNOWN
}

private fun Int.failureCategory(): FailureCategory = FailureCategory.entries.getOrElse(this) { FailureCategory.NONE }

private fun Int.diagnosticOutcome(): DiagnosticOutcome = when (this) {
    RuntimeSignal.OUTCOME_SUCCESS -> DiagnosticOutcome.SUCCESS
    RuntimeSignal.OUTCOME_TIMEOUT -> DiagnosticOutcome.TIMEOUT
    RuntimeSignal.OUTCOME_CANCELLED -> DiagnosticOutcome.CANCELLED
    else -> DiagnosticOutcome.FAILURE
}

private fun resolverStrategy(mode: PlaybackMode, ordinal: Int): ResolverStrategy = when (mode) {
    PlaybackMode.AUDIO -> listOf(
        ResolverStrategy.REEL_MUXED,
        ResolverStrategy.REEL_AUDIO,
        ResolverStrategy.PERSISTED,
        ResolverStrategy.DIRECT,
        ResolverStrategy.SEARCH
    ).getOrElse(ordinal) { ResolverStrategy.UNKNOWN }
    PlaybackMode.VIDEO -> listOf(
        ResolverStrategy.PERSISTED,
        ResolverStrategy.STANDARD_VIDEO,
        ResolverStrategy.REEL_VIDEO
    ).getOrElse(ordinal) { ResolverStrategy.UNKNOWN }
    else -> ResolverStrategy.UNKNOWN
}

private fun Int.cacheAction(): CacheAction = when (this) {
    RuntimeSignal.CACHE_HIT -> CacheAction.HIT
    RuntimeSignal.CACHE_EVICTION -> CacheAction.EVICTION
    RuntimeSignal.CACHE_PREFETCH -> CacheAction.PREFETCH
    else -> CacheAction.MISS
}

private fun Int.dspAction(): DspAction = when (this) {
    RuntimeSignal.DSP_RECREATED -> DspAction.RECREATED
    RuntimeSignal.DSP_RELEASED -> DspAction.RELEASED
    else -> DspAction.CREATED
}

private fun Int.canvasAction(): CanvasAction = when (this) {
    RuntimeSignal.CANVAS_FIRST_FRAME -> CanvasAction.FIRST_FRAME
    RuntimeSignal.CANVAS_FALLBACK -> CanvasAction.FALLBACK
    RuntimeSignal.CANVAS_RESTARTED -> CanvasAction.RESTARTED
    RuntimeSignal.CANVAS_STOPPED -> CanvasAction.STOPPED
    else -> CanvasAction.STARTED
}

private fun Int.networkCategory(): NetworkCategory = when (this) {
    RuntimeSignal.NETWORK_RESOLVE -> NetworkCategory.RESOLVE
    RuntimeSignal.NETWORK_CONFIG -> NetworkCategory.CONFIG
    else -> NetworkCategory.CONNECT
}

private fun Int.diagnosticOperation(): DiagnosticOperation = when (this) {
    RuntimeSignal.HOT_PLAYER_CREATE -> DiagnosticOperation.PLAYER_CREATE
    RuntimeSignal.HOT_PLAYER_PREPARE -> DiagnosticOperation.PLAYER_PREPARE
    RuntimeSignal.HOT_FALLBACK -> DiagnosticOperation.FALLBACK
    RuntimeSignal.HOT_NETWORK_RETRY -> DiagnosticOperation.NETWORK_RETRY
    RuntimeSignal.HOT_CACHE_ACCESS -> DiagnosticOperation.CACHE_ACCESS
    RuntimeSignal.HOT_DSP_RECREATE -> DiagnosticOperation.DSP_RECREATE
    RuntimeSignal.HOT_CANVAS_RESTART -> DiagnosticOperation.CANVAS_RESTART
    else -> DiagnosticOperation.RESOLVER_ATTEMPT
}
