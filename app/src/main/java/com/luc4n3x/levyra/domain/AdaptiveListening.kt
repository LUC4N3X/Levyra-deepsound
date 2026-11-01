package com.luc4n3x.levyra.domain

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class AdaptiveSensitivity(val storageValue: String, val deadbandDb: Float, val slope: Float) {
    LOW("low", 10f, 0.25f),
    BALANCED("balanced", 6f, 0.35f),
    HIGH("high", 4f, 0.5f);

    companion object {
        fun fromStorage(value: String?): AdaptiveSensitivity = entries.firstOrNull { it.storageValue == value } ?: BALANCED
    }
}

object AdaptiveListeningLimits {
    const val MIN_BOOST_DB = 1f
    const val MAX_BOOST_DB = 6f
    const val DEFAULT_BOOST_DB = 4f
    const val SILENCE_DB = -120f

    fun normalizedMaxBoost(value: Float): Float =
        if (value.isFinite()) value.coerceIn(MIN_BOOST_DB, MAX_BOOST_DB) else DEFAULT_BOOST_DB
}

enum class AdaptiveListeningCondition {
    Off, Waiting, Calibrating, Listening, Adapting, SpeakerOutput, PlaybackBleed, Unstable, Overload,
    MicrophoneSilent, PermissionMissing, Unavailable
}

data class AdaptiveListeningSnapshot(
    val condition: AdaptiveListeningCondition = AdaptiveListeningCondition.Off,
    val ambientDb: Float = Float.NaN,
    val referenceDb: Float = Float.NaN,
    val boostDb: Float = 0f,
    val history: List<Float> = emptyList(),
    val boostHistory: List<Float> = emptyList()
) {
    val reliable: Boolean
        get() = condition == AdaptiveListeningCondition.Listening || condition == AdaptiveListeningCondition.Adapting
}

class AmbientFrameMeter(private val sampleRateHz: Int, frameMs: Int = FRAME_MS) {
    private val frameSamples = (sampleRateHz * frameMs / 1000).coerceAtLeast(1)
    private val highPassCoefficient: Float
    private var previousInput = 0f
    private var previousOutput = 0f
    private var sumSquares = 0.0
    private var samplesInFrame = 0
    private var clippedInFrame = 0

    init {
        val rc = 1.0 / (2.0 * Math.PI * HIGH_PASS_HZ)
        val dt = 1.0 / sampleRateHz.coerceAtLeast(1)
        highPassCoefficient = (rc / (rc + dt)).toFloat()
    }

    fun interface FrameSink {
        fun onFrame(levelDb: Float, clipped: Boolean)
    }

    fun process(samples: ShortArray, count: Int, sink: FrameSink) {
        val limit = count.coerceIn(0, samples.size)
        for (index in 0 until limit) {
            val raw = samples[index]
            if (raw >= CLIP_THRESHOLD || raw <= -CLIP_THRESHOLD) clippedInFrame++
            val input = raw / 32768f
            val filtered = highPassCoefficient * (previousOutput + input - previousInput)
            previousInput = input
            previousOutput = filtered
            sumSquares += filtered * filtered
            samplesInFrame++
            if (samplesInFrame >= frameSamples) {
                val meanSquare = sumSquares / samplesInFrame
                val level = if (meanSquare <= SILENT_MEAN_SQUARE) AdaptiveListeningLimits.SILENCE_DB
                else (10.0 * log10(meanSquare)).toFloat().coerceAtLeast(AdaptiveListeningLimits.SILENCE_DB)
                sink.onFrame(level, clippedInFrame * CLIP_FRAME_DENOMINATOR >= samplesInFrame)
                sumSquares = 0.0
                samplesInFrame = 0
                clippedInFrame = 0
            }
        }
    }

    fun reset() {
        previousInput = 0f
        previousOutput = 0f
        sumSquares = 0.0
        samplesInFrame = 0
        clippedInFrame = 0
    }

    companion object {
        const val FRAME_MS = 100
        private const val HIGH_PASS_HZ = 120.0
        private const val CLIP_THRESHOLD: Short = 32_000
        private const val CLIP_FRAME_DENOMINATOR = 100
        private const val SILENT_MEAN_SQUARE = 1e-12
    }
}

data class AmbientWindow(
    val frameLevelsDb: FloatArray,
    val clippedFrames: Int,
    val outputLevelDb: Float,
    val headphonesActive: Boolean,
    val atMs: Long
)

class AdaptiveListeningEngine(
    private var sensitivity: AdaptiveSensitivity,
    private var maxBoostDb: Float,
    storedReferenceDb: Float = Float.NaN
) {
    private val windowHistory = FloatArray(HISTORY_WINDOWS)
    private val boostHistory = FloatArray(HISTORY_WINDOWS)
    private var historyCount = 0
    private var historyHead = 0
    private val bleedMic = FloatArray(BLEED_WINDOWS)
    private val bleedOutput = FloatArray(BLEED_WINDOWS)
    private var bleedCount = 0
    private var bleedHead = 0
    private var smoothedDb = Float.NaN
    private var lastWindowAtMs = -1L
    private var reliableWindows = 0
    private var silentWindows = 0
    private var sessionMinimumDb = Float.NaN
    private var currentBoostDb = 0f
    private var lastChangeAtMs = Long.MIN_VALUE / 2
    private val persistedReferenceDb = storedReferenceDb.takeIf { it.isFinite() && it > MIN_PLAUSIBLE_REFERENCE_DB }
    private val sortScratch = FloatArray(MAX_FRAMES_PER_WINDOW)

    var snapshot: AdaptiveListeningSnapshot = AdaptiveListeningSnapshot(AdaptiveListeningCondition.Waiting)
        private set

    val referenceDb: Float
        get() {
            val session = sessionMinimumDb
            val stored = persistedReferenceDb
            return when {
                session.isFinite() && stored != null -> minOf(session, stored)
                session.isFinite() -> session
                stored != null -> stored
                else -> Float.NaN
            }
        }

    fun configure(sensitivity: AdaptiveSensitivity, maxBoostDb: Float) {
        this.sensitivity = sensitivity
        this.maxBoostDb = AdaptiveListeningLimits.normalizedMaxBoost(maxBoostDb)
        if (currentBoostDb > this.maxBoostDb) currentBoostDb = this.maxBoostDb
    }

    fun suspend(condition: AdaptiveListeningCondition) {
        currentBoostDb = 0f
        smoothedDb = Float.NaN
        reliableWindows = 0
        bleedCount = 0
        bleedHead = 0
        lastWindowAtMs = -1L
        snapshot = snapshot.copy(condition = condition, boostDb = 0f)
    }

    fun onWindow(window: AmbientWindow): AdaptiveListeningSnapshot {
        val frames = window.frameLevelsDb
        if (frames.isEmpty()) return snapshot
        unreliableInput(window, frames)?.let { return it }
        val floorDb = noiseFloor(frames)
        recordHistory(floorDb)
        recordBleed(floorDb, window.outputLevelDb)
        if (bleedDetected()) {
            return settle(AdaptiveListeningCondition.PlaybackBleed, floorDb, window.atMs, dropBoost = true)
        }
        if (recentSpreadDb() > UNSTABLE_SPREAD_DB) return holdWhileUnstable(floorDb, window.atMs)
        return adapt(floorDb, window.atMs)
    }

    private fun unreliableInput(window: AmbientWindow, frames: FloatArray): AdaptiveListeningSnapshot? {
        if (!window.headphonesActive) {
            return settle(AdaptiveListeningCondition.SpeakerOutput, Float.NaN, window.atMs, dropBoost = true)
        }
        if (frames.all { it <= DIGITAL_SILENCE_DB }) {
            silentWindows++
            val muted = silentWindows >= SILENT_WINDOWS_LIMIT
            val condition = if (muted) AdaptiveListeningCondition.MicrophoneSilent else snapshot.condition
            return settle(condition, Float.NaN, window.atMs, dropBoost = muted)
        }
        silentWindows = 0
        if (window.clippedFrames * 2 > frames.size) {
            return settle(AdaptiveListeningCondition.Overload, Float.NaN, window.atMs, dropBoost = false)
        }
        return null
    }

    private fun holdWhileUnstable(floorDb: Float, atMs: Long): AdaptiveListeningSnapshot {
        val smoothed = smoothedDb
        if (smoothed.isFinite() && floorDb < smoothed) {
            smooth(floorDb, atMs)
            val desired = desiredBoostDb(smoothedDb - referenceDb)
            if (desired < currentBoostDb) applyHysteresis(desired, atMs)
        }
        return settle(AdaptiveListeningCondition.Unstable, floorDb, atMs, dropBoost = false)
    }

    private fun adapt(floorDb: Float, atMs: Long): AdaptiveListeningSnapshot {
        smooth(floorDb, atMs)
        reliableWindows++
        val smoothed = smoothedDb
        if (reliableWindows >= CALIBRATION_WINDOWS && smoothed.isFinite()) {
            sessionMinimumDb = if (sessionMinimumDb.isFinite()) minOf(sessionMinimumDb, smoothed) else smoothed
        }
        if (reliableWindows < CALIBRATION_WINDOWS) {
            return settle(AdaptiveListeningCondition.Calibrating, floorDb, atMs, dropBoost = false)
        }
        applyHysteresis(desiredBoostDb(smoothed - referenceDb), atMs)
        val condition = if (currentBoostDb > 0f) AdaptiveListeningCondition.Adapting else AdaptiveListeningCondition.Listening
        return publish(condition, floorDb)
    }

    fun desiredBoostDb(excessDb: Float): Float {
        if (!excessDb.isFinite()) return 0f
        val raw = ((excessDb - sensitivity.deadbandDb) * sensitivity.slope).coerceIn(0f, maxBoostDb)
        return (raw * 2f).roundToInt() / 2f
    }

    private fun applyHysteresis(desired: Float, atMs: Long) {
        val difference = desired - currentBoostDb
        if (abs(difference) < if (difference > 0f) HYSTERESIS_DB else LOWER_HYSTERESIS_DB) return
        if (difference > 0f && atMs - lastChangeAtMs < HOLD_BEFORE_RAISE_MS) return
        currentBoostDb = if (difference > 0f) minOf(desired, currentBoostDb + MAX_RAISE_PER_DECISION_DB) else desired
        lastChangeAtMs = atMs
    }

    private fun settle(condition: AdaptiveListeningCondition, floorDb: Float, atMs: Long, dropBoost: Boolean): AdaptiveListeningSnapshot {
        if (dropBoost && currentBoostDb != 0f) {
            currentBoostDb = 0f
            lastChangeAtMs = atMs
        }
        if (dropBoost) reliableWindows = 0
        return publish(condition, floorDb)
    }

    private fun publish(condition: AdaptiveListeningCondition, floorDb: Float): AdaptiveListeningSnapshot {
        recordBoost()
        snapshot = AdaptiveListeningSnapshot(
            condition = condition,
            ambientDb = if (floorDb.isFinite()) floorDb else snapshot.ambientDb,
            referenceDb = referenceDb,
            boostDb = currentBoostDb,
            history = ordered(windowHistory),
            boostHistory = ordered(boostHistory)
        )
        return snapshot
    }

    private fun noiseFloor(frames: FloatArray): Float {
        val count = minOf(frames.size, sortScratch.size)
        System.arraycopy(frames, 0, sortScratch, 0, count)
        java.util.Arrays.sort(sortScratch, 0, count)
        val take = (count * FLOOR_PERCENTILE).roundToInt().coerceIn(1, count)
        var energy = 0.0
        for (index in 0 until take) energy += Math.pow(10.0, sortScratch[index] / 10.0)
        return (10.0 * log10(energy / take)).toFloat()
    }

    private fun smooth(floorDb: Float, atMs: Long) {
        val previous = smoothedDb
        if (!previous.isFinite() || lastWindowAtMs < 0L) {
            smoothedDb = floorDb
        } else {
            val dtSeconds = (atMs - lastWindowAtMs).coerceIn(1L, 10_000L) / 1000f
            val tau = if (floorDb > previous) RISE_TAU_SECONDS else FALL_TAU_SECONDS
            val alpha = 1f - exp(-dtSeconds / tau)
            smoothedDb = previous + (floorDb - previous) * alpha
        }
        lastWindowAtMs = atMs
    }

    private fun recordHistory(value: Float) {
        windowHistory[historyHead] = value
        boostHistory[historyHead] = currentBoostDb
        historyHead = (historyHead + 1) % HISTORY_WINDOWS
        if (historyCount < HISTORY_WINDOWS) historyCount++
    }

    private fun recordBoost() {
        if (historyCount == 0) return
        boostHistory[(historyHead - 1 + HISTORY_WINDOWS) % HISTORY_WINDOWS] = currentBoostDb
    }

    private fun ordered(source: FloatArray): List<Float> {
        if (historyCount == 0) return emptyList()
        val start = if (historyCount < HISTORY_WINDOWS) 0 else historyHead
        return List(historyCount) { index -> source[(start + index) % HISTORY_WINDOWS] }
    }

    private fun recordBleed(micDb: Float, outputDb: Float) {
        if (!outputDb.isFinite() || outputDb <= OUTPUT_SILENCE_DB) return
        bleedMic[bleedHead] = micDb
        bleedOutput[bleedHead] = outputDb
        bleedHead = (bleedHead + 1) % BLEED_WINDOWS
        if (bleedCount < BLEED_WINDOWS) bleedCount++
    }

    internal fun bleedDetected(): Boolean {
        if (bleedCount < MIN_BLEED_WINDOWS) return false
        val correlation = pearson(bleedMic, bleedOutput, bleedCount) ?: return false
        return correlation >= BLEED_CORRELATION
    }

    private fun recentSpreadDb(): Float {
        val count = minOf(historyCount, SPREAD_WINDOWS)
        if (count < SPREAD_WINDOWS) return 0f
        var sum = 0f
        for (offset in 1..count) sum += windowHistory[(historyHead - offset + HISTORY_WINDOWS) % HISTORY_WINDOWS]
        val mean = sum / count
        var variance = 0f
        for (offset in 1..count) {
            val delta = windowHistory[(historyHead - offset + HISTORY_WINDOWS) % HISTORY_WINDOWS] - mean
            variance += delta * delta
        }
        return sqrt(variance / count)
    }

    companion object {
        const val WINDOW_MS = 1_000L
        const val HISTORY_WINDOWS = 60
        const val CALIBRATION_WINDOWS = 5
        const val HYSTERESIS_DB = 1f
        const val LOWER_HYSTERESIS_DB = 0.5f
        const val HOLD_BEFORE_RAISE_MS = 6_000L
        const val MAX_RAISE_PER_DECISION_DB = 1.5f
        const val RISE_TAU_SECONDS = 10f
        const val FALL_TAU_SECONDS = 5f
        const val BLEED_WINDOWS = 20
        const val MIN_BLEED_WINDOWS = 12
        const val BLEED_CORRELATION = 0.7
        const val MIN_OUTPUT_SPREAD_DB = 2.0
        const val UNSTABLE_SPREAD_DB = 9f
        const val SPREAD_WINDOWS = 6
        const val SILENT_WINDOWS_LIMIT = 3
        const val DIGITAL_SILENCE_DB = -100f
        const val OUTPUT_SILENCE_DB = -70f
        const val MIN_PLAUSIBLE_REFERENCE_DB = -95f
        const val FLOOR_PERCENTILE = 0.4f
        const val MAX_FRAMES_PER_WINDOW = 64

        internal fun pearson(first: FloatArray, second: FloatArray, count: Int): Double? {
            if (count < 3) return null
            var meanA = 0.0
            var meanB = 0.0
            for (index in 0 until count) {
                meanA += first[index]
                meanB += second[index]
            }
            meanA /= count
            meanB /= count
            var covariance = 0.0
            var varianceA = 0.0
            var varianceB = 0.0
            for (index in 0 until count) {
                val a = first[index] - meanA
                val b = second[index] - meanB
                covariance += a * b
                varianceA += a * a
                varianceB += b * b
            }
            if (sqrt(varianceB / count) < MIN_OUTPUT_SPREAD_DB || varianceA <= 0.0) return null
            return covariance / sqrt(varianceA * varianceB)
        }
    }
}
