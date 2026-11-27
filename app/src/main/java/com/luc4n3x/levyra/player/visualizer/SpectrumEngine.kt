package com.luc4n3x.levyra.player.visualizer

import android.os.SystemClock
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class PlaybackClockAnchor(
    val periodUid: Any?,
    val chainRootUid: Any?,
    val chainOffsetUs: Long,
    val positionUs: Long,
    val elapsedRealtimeUs: Long,
    val speed: Float,
    val playing: Boolean,
    val durationUs: Long
) {
    fun positionAt(nowUs: Long): Long {
        if (!playing) return positionUs
        val advanced = positionUs + ((nowUs - elapsedRealtimeUs) * speed.toDouble()).toLong()
        return if (durationUs > 0L) advanced.coerceIn(0L, durationUs) else max(0L, advanced)
    }
}

class SpectrumFrame internal constructor() {
    val bars = FloatArray(BAR_BANDS)
    val barPeaks = FloatArray(BAR_BANDS)
    val rtaDb = FloatArray(RTA_BANDS)
    val rtaPeakDb = FloatArray(RTA_BANDS)
    val spectrogramColumn = FloatArray(SPECTROGRAM_ROWS)
    val scope = FloatArray(SCOPE_POINTS)
    var spectrogramSequence: Long = 0L
        internal set
    var sampleRate: Double = 0.0
        internal set
    var live: Boolean = false
        internal set

    internal fun copyFrom(other: SpectrumFrame) {
        other.bars.copyInto(bars)
        other.barPeaks.copyInto(barPeaks)
        other.rtaDb.copyInto(rtaDb)
        other.rtaPeakDb.copyInto(rtaPeakDb)
        other.spectrogramColumn.copyInto(spectrogramColumn)
        other.scope.copyInto(scope)
        spectrogramSequence = other.spectrogramSequence
        sampleRate = other.sampleRate
        live = other.live
    }

    companion object {
        const val BAR_BANDS = 48
        const val RTA_BANDS = 31
        const val SPECTROGRAM_ROWS = 96
        const val SCOPE_POINTS = 512
        const val BAR_FLOOR_DB = -72f
        const val RTA_FLOOR_DB = -90f
        const val BAR_MIN_HZ = 30f
        const val BAR_MAX_HZ = 16_000f
    }
}

internal class SpectrumProcessor(private val analyzer: SpectrumAnalyzer = SpectrumAnalyzer()) {
    private val window = FloatArray(analyzer.fftSize)
    private val rawBarsDb = FloatArray(SpectrumFrame.BAR_BANDS)
    private val rawRtaDb = FloatArray(SpectrumFrame.RTA_BANDS)
    private val rawSpectrogramDb = FloatArray(SpectrumFrame.SPECTROGRAM_ROWS)
    private val barHoldSeconds = FloatArray(SpectrumFrame.BAR_BANDS)
    private val rtaHoldSeconds = FloatArray(SpectrumFrame.RTA_BANDS)
    private var edgesRate = 0.0
    private var barEdges = FloatArray(SpectrumFrame.BAR_BANDS + 1)
    private var spectrogramEdges = FloatArray(SpectrumFrame.SPECTROGRAM_ROWS + 1)
    private val state = SpectrumFrame().also { reset(it) }

    val windowSize: Int get() = analyzer.fftSize

    fun reset(frame: SpectrumFrame = state) {
        frame.bars.fill(0f)
        frame.barPeaks.fill(0f)
        frame.rtaDb.fill(SpectrumFrame.RTA_FLOOR_DB)
        frame.rtaPeakDb.fill(SpectrumFrame.RTA_FLOOR_DB)
        frame.spectrogramColumn.fill(0f)
        frame.scope.fill(0f)
        frame.live = false
        barHoldSeconds.fill(0f)
        rtaHoldSeconds.fill(0f)
    }

    fun buffer(): FloatArray = window

    fun analyze(sampleRate: Double, dtSeconds: Float) {
        if (sampleRate != edgesRate) {
            val top = min(SpectrumFrame.BAR_MAX_HZ, (sampleRate * 0.49).toFloat())
            barEdges = SpectrumAnalyzer.logEdges(SpectrumFrame.BAR_BANDS, SpectrumFrame.BAR_MIN_HZ, top)
            spectrogramEdges = SpectrumAnalyzer.logEdges(SpectrumFrame.SPECTROGRAM_ROWS, SpectrumFrame.BAR_MIN_HZ, top)
            edgesRate = sampleRate
        }
        analyzer.analyze(window, sampleRate)
        analyzer.peakBandsDb(barEdges, rawBarsDb)
        analyzer.bandPowerDb(SpectrumAnalyzer.thirdOctaveEdgesHz, rawRtaDb)
        analyzer.peakBandsDb(spectrogramEdges, rawSpectrogramDb)
        val nyquist = (sampleRate / 2.0).toFloat()
        for (band in rawRtaDb.indices) {
            if (SpectrumAnalyzer.thirdOctaveEdgesHz[band] >= nyquist) rawRtaDb[band] = SpectrumAnalyzer.FLOOR_DB
        }
        val frame = state
        for (band in rawBarsDb.indices) {
            val target = normalize(rawBarsDb[band], SpectrumFrame.BAR_FLOOR_DB)
            frame.bars[band] = release(frame.bars[band], target, dtSeconds, BAR_RELEASE_SECONDS)
            updatePeak(frame.barPeaks, barHoldSeconds, band, frame.bars[band], dtSeconds, BAR_PEAK_FALL_PER_SECOND)
        }
        for (band in rawRtaDb.indices) {
            val target = max(SpectrumFrame.RTA_FLOOR_DB, rawRtaDb[band])
            frame.rtaDb[band] = if (target >= frame.rtaDb[band]) {
                target
            } else {
                max(target, frame.rtaDb[band] - RTA_RELEASE_DB_PER_SECOND * dtSeconds)
            }
            updatePeak(frame.rtaPeakDb, rtaHoldSeconds, band, frame.rtaDb[band], dtSeconds, RTA_PEAK_FALL_DB_PER_SECOND)
        }
        for (row in rawSpectrogramDb.indices) {
            frame.spectrogramColumn[row] = normalize(rawSpectrogramDb[row], SpectrumFrame.BAR_FLOOR_DB)
        }
        frame.spectrogramSequence++
        captureScope(frame.scope)
        frame.sampleRate = sampleRate
        frame.live = true
    }

    fun decay(dtSeconds: Float): Boolean {
        val frame = state
        var active = false
        for (band in frame.bars.indices) {
            frame.bars[band] = release(frame.bars[band], 0f, dtSeconds, BAR_RELEASE_SECONDS)
            updatePeak(frame.barPeaks, barHoldSeconds, band, frame.bars[band], dtSeconds, BAR_PEAK_FALL_PER_SECOND)
            if (frame.barPeaks[band] > SILENCE_EPSILON) active = true
        }
        for (band in frame.rtaDb.indices) {
            frame.rtaDb[band] = max(SpectrumFrame.RTA_FLOOR_DB, frame.rtaDb[band] - RTA_RELEASE_DB_PER_SECOND * dtSeconds)
            updatePeak(frame.rtaPeakDb, rtaHoldSeconds, band, frame.rtaDb[band], dtSeconds, RTA_PEAK_FALL_DB_PER_SECOND)
            if (frame.rtaPeakDb[band] > SpectrumFrame.RTA_FLOOR_DB) active = true
        }
        var scopeActive = false
        for (index in frame.scope.indices) {
            frame.scope[index] *= exp(-dtSeconds / BAR_RELEASE_SECONDS)
            if (kotlin.math.abs(frame.scope[index]) > SILENCE_EPSILON) scopeActive = true
        }
        if (frame.live) {
            frame.spectrogramColumn.fill(0f)
            frame.spectrogramSequence++
        }
        frame.live = false
        return active || scopeActive
    }

    fun publishInto(target: SpectrumFrame) = target.copyFrom(state)

    private fun captureScope(out: FloatArray) {
        val span = min(window.size / 2, out.size * 2)
        val searchEnd = window.size - span
        var trigger = searchEnd
        var index = window.size / 2
        while (index > 1) {
            if (window[index - 1] < 0f && window[index] >= 0f) {
                trigger = index
                break
            }
            index--
        }
        val start = trigger.coerceIn(0, searchEnd)
        val stride = span.toFloat() / out.size
        for (point in out.indices) {
            out[point] = window[start + (point * stride).toInt()]
        }
    }

    private fun normalize(db: Float, floorDb: Float): Float = ((db - floorDb) / -floorDb).coerceIn(0f, 1f)

    private fun release(current: Float, target: Float, dtSeconds: Float, seconds: Float): Float =
        if (target >= current) target else max(target, current * exp(-dtSeconds / seconds))

    private fun updatePeak(
        peaks: FloatArray,
        holds: FloatArray,
        index: Int,
        value: Float,
        dtSeconds: Float,
        fallPerSecond: Float
    ) {
        if (value >= peaks[index]) {
            peaks[index] = value
            holds[index] = PEAK_HOLD_SECONDS
        } else if (holds[index] > 0f) {
            holds[index] -= dtSeconds
        } else {
            peaks[index] = max(value, peaks[index] - fallPerSecond * dtSeconds)
        }
    }

    companion object {
        const val BAR_RELEASE_SECONDS = 0.16f
        const val BAR_PEAK_FALL_PER_SECOND = 0.9f
        const val RTA_RELEASE_DB_PER_SECOND = 24f
        const val RTA_PEAK_FALL_DB_PER_SECOND = 12f
        const val PEAK_HOLD_SECONDS = 0.6f
        private const val SILENCE_EPSILON = 0.002f
    }
}

object SpectrumEngine {
    internal val history = PcmHistory()
    private val location = PcmHistory.Location()

    @Volatile
    internal var clock: PlaybackClockAnchor? = null

    @Volatile
    var outputLatencyOffsetMs: Long = 0L

    private val buffers = Array(3) { SpectrumFrame() }
    private var nextBuffer = 0
    private val mutableFrame = MutableStateFlow<SpectrumFrame?>(null)
    val frame: StateFlow<SpectrumFrame?> = mutableFrame.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val active: StateFlow<Boolean> = mutableFrame.subscriptionCount
        .map { it > 0 }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launch {
            active.collectLatest { observed -> if (observed) runLoop() }
        }
    }

    private suspend fun runLoop() {
        val processor = SpectrumProcessor()
        var lastNanos = SystemClock.elapsedRealtimeNanos()
        var idle = false
        try {
            while (scope.isActive) {
                val nowNanos = SystemClock.elapsedRealtimeNanos()
                val dt = ((nowNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, MAX_STEP_SECONDS)
                lastNanos = nowNanos
                val analyzed = analyzeStep(processor, nowNanos / 1_000L, dt)
                val moving = analyzed || processor.decay(dt)
                if (moving || !idle) publish(processor)
                idle = !moving
                val spent = (SystemClock.elapsedRealtimeNanos() - nowNanos) / 1_000_000L
                delay(((if (idle) IDLE_FRAME_MS else FRAME_MS) - spent).coerceAtLeast(MIN_SLEEP_MS))
            }
        } finally {
            processor.reset()
            mutableFrame.value = null
        }
    }

    private fun analyzeStep(processor: SpectrumProcessor, nowUs: Long, dt: Float): Boolean {
        val anchor = clock ?: return false
        if (!anchor.playing || !history.enabled) return false
        val audibleUs = anchor.positionAt(nowUs) - outputLatencyOffsetMs * 1_000L
        if (!history.locate(anchor.periodUid, audibleUs, anchor.chainRootUid, anchor.chainOffsetUs, location)) {
            return false
        }
        val half = processor.windowSize / 2
        val end = min(location.endFrame + half, history.availableEnd())
        if (!history.read(end, processor.buffer())) return false
        processor.analyze(location.sampleRate, dt)
        return true
    }

    private fun publish(processor: SpectrumProcessor) {
        val target = buffers[nextBuffer]
        nextBuffer = (nextBuffer + 1) % buffers.size
        processor.publishInto(target)
        mutableFrame.value = target
    }

    private const val FRAME_MS = 33L
    private const val IDLE_FRAME_MS = 120L
    private const val MIN_SLEEP_MS = 4L
    private const val MAX_STEP_SECONDS = 0.1f
}
