package com.luc4n3x.levyra.player

import kotlin.math.abs
import kotlin.math.max

internal enum class CrossfadeCurve { EqualPower, EqualGain }

internal sealed interface CrossfadeRampStep {
    data class Gains(val outgoing: Float, val incoming: Float) : CrossfadeRampStep
    data object Complete : CrossfadeRampStep
    data object IncomingSilent : CrossfadeRampStep
}

internal class AudibleStartDetector(private val requiredAdvances: Int = AUDIBLE_ADVANCE_SAMPLES) {
    private var lastPositionMs = Long.MIN_VALUE
    private var advances = 0
    var audibleFromMs: Long = Long.MIN_VALUE
        private set

    val isAudible: Boolean get() = audibleFromMs != Long.MIN_VALUE

    fun observe(positionMs: Long, playing: Boolean): Boolean {
        if (isAudible) return true
        advances = when {
            !playing -> 0
            lastPositionMs != Long.MIN_VALUE && positionMs > lastPositionMs -> advances + 1
            lastPositionMs != Long.MIN_VALUE && positionMs < lastPositionMs -> 0
            else -> advances
        }
        lastPositionMs = if (playing) positionMs else Long.MIN_VALUE
        if (advances >= requiredAdvances) audibleFromMs = positionMs
        return isAudible
    }

    fun reset() {
        lastPositionMs = Long.MIN_VALUE
        advances = 0
        audibleFromMs = Long.MIN_VALUE
    }
}

internal class CrossfadeRamp(
    private val curve: CrossfadeCurve,
    private val fixedSpanMs: Long?,
    private val startTimeoutMs: Long
) {
    private val detector = AudibleStartDetector()
    private var waitStartedAtMs = Long.MIN_VALUE
    private var spanMs = 0L
    private var progress = 0f

    fun step(
        nowMs: Long,
        incomingPositionMs: Long,
        incomingPlaying: Boolean,
        outgoingRemainingMs: Long
    ): CrossfadeRampStep {
        if (!detector.isAudible) {
            if (waitStartedAtMs == Long.MIN_VALUE) waitStartedAtMs = nowMs
            if (!detector.observe(incomingPositionMs, incomingPlaying)) {
                return if (nowMs - waitStartedAtMs > startTimeoutMs) {
                    CrossfadeRampStep.IncomingSilent
                } else {
                    crossfadeGains(curve, 0f)
                }
            }
            spanMs = (fixedSpanMs ?: outgoingRemainingMs).coerceAtLeast(MIN_RAMP_SPAN_MS)
        }
        val elapsed = (incomingPositionMs - detector.audibleFromMs).coerceAtLeast(0L)
        progress = max(progress, (elapsed.toFloat() / spanMs.toFloat()).coerceIn(0f, 1f))
        val outgoingFinished = fixedSpanMs == null && outgoingRemainingMs <= 0L
        if (progress >= 1f || outgoingFinished) return CrossfadeRampStep.Complete
        return crossfadeGains(curve, progress)
    }
}

internal sealed interface HandoffAlignStep {
    data object Wait : HandoffAlignStep
    data class Seek(val positionMs: Long) : HandoffAlignStep
    data class Aligned(val offsetMs: Long) : HandoffAlignStep
    data class Unaligned(val offsetMs: Long) : HandoffAlignStep
    data object PrimarySilent : HandoffAlignStep
}

internal class HandoffAligner(
    private val toleranceMs: Long = HANDOFF_ALIGN_TOLERANCE_MS,
    private val maxSeeks: Int = HANDOFF_MAX_SEEKS,
    private val audibleTimeoutMs: Long = HANDOFF_AUDIBLE_TIMEOUT_MS
) {
    private val detector = AudibleStartDetector()
    private var phaseStartedAtMs = Long.MIN_VALUE
    private var phaseStartPositionMs = 0L
    private var seeks = 0
    private var leadMs = Long.MIN_VALUE
    private val offsetSamples = LongArray(HANDOFF_OFFSET_SAMPLES)
    private var sampleCount = 0

    fun step(
        nowMs: Long,
        primaryPositionMs: Long,
        primaryPlaying: Boolean,
        secondaryPositionMs: Long,
        primaryDurationMs: Long
    ): HandoffAlignStep {
        if (phaseStartedAtMs == Long.MIN_VALUE) {
            phaseStartedAtMs = nowMs
            phaseStartPositionMs = primaryPositionMs
        }
        if (!detector.observe(primaryPositionMs, primaryPlaying)) {
            return if (nowMs - phaseStartedAtMs > audibleTimeoutMs) {
                HandoffAlignStep.PrimarySilent
            } else {
                HandoffAlignStep.Wait
            }
        }
        offsetSamples[sampleCount++] = primaryPositionMs - secondaryPositionMs
        if (sampleCount < offsetSamples.size) return HandoffAlignStep.Wait
        val offset = offsetSamples.sorted()[offsetSamples.size / 2]
        sampleCount = 0
        if (abs(offset) <= toleranceMs) return HandoffAlignStep.Aligned(offset)
        if (seeks >= maxSeeks) return HandoffAlignStep.Unaligned(offset)
        val playedMs = (primaryPositionMs - phaseStartPositionMs).coerceAtLeast(0L)
        val restartLatency = (nowMs - phaseStartedAtMs - playedMs).coerceIn(0L, MAX_HANDOFF_LEAD_MS)
        leadMs = if (leadMs == Long.MIN_VALUE) restartLatency else leadMs - offset
        leadMs = leadMs.coerceIn(0L, MAX_HANDOFF_LEAD_MS)
        seeks++
        detector.reset()
        val target = secondaryPositionMs.coerceAtLeast(0L) + leadMs
        val clamped = if (primaryDurationMs > 0L) target.coerceAtMost(primaryDurationMs - 1L) else target
        phaseStartedAtMs = nowMs
        phaseStartPositionMs = clamped.coerceAtLeast(0L)
        return HandoffAlignStep.Seek(phaseStartPositionMs)
    }
}

internal fun crossfadeGains(curve: CrossfadeCurve, progress: Float): CrossfadeRampStep.Gains {
    val t = progress.coerceIn(0f, 1f)
    return when (curve) {
        CrossfadeCurve.EqualPower -> equalPowerCrossfade(t).let { CrossfadeRampStep.Gains(it.outgoing, it.incoming) }
        CrossfadeCurve.EqualGain -> CrossfadeRampStep.Gains(outgoing = 1f - t, incoming = t)
    }
}

internal const val AUDIBLE_ADVANCE_SAMPLES = 2
internal const val MIN_RAMP_SPAN_MS = 200L
internal const val HANDOFF_ALIGN_TOLERANCE_MS = 20L
internal const val HANDOFF_MAX_SEEKS = 3
internal const val HANDOFF_AUDIBLE_TIMEOUT_MS = 2_500L
internal const val MAX_HANDOFF_LEAD_MS = 1_500L
internal const val HANDOFF_OFFSET_SAMPLES = 5
