package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class AutoMixPlan(val transitionMs: Long, val preloadLeadMs: Long)
data class CrossfadeGains(val outgoing: Float, val incoming: Float)

internal fun planAutoMix(
    current: Track,
    next: Track?,
    settings: LevyraAudioSettings,
    repeatMode: RepeatMode,
    videoMode: Boolean,
    lowRam: Boolean
): AutoMixPlan? {
    val normalized = settings.normalized()
    if (!normalized.gaplessEnabled || normalized.crossfadeSeconds <= 0 || next == null) return null
    if (repeatMode == RepeatMode.One || videoMode || lowRam || current.durationMs <= MIN_TRACK_MS) return null
    val base = normalized.crossfadeSeconds * 1_000L
    val adaptive = if (normalized.djSoftMode) {
        val energyDistance = abs(current.energy - next.energy)
        val vocalOverlap = (current.vocal + next.vocal) / 2
        when {
            vocalOverlap >= 70 -> base - 1_500L
            energyDistance <= 15 -> base + 1_500L
            energyDistance >= 45 -> base - 1_000L
            else -> base
        }
    } else base
    val transition = adaptive.coerceIn(MIN_TRANSITION_MS, MAX_TRANSITION_MS)
        .coerceAtMost(current.durationMs / 4L)
    return AutoMixPlan(
        transitionMs = transition,
        preloadLeadMs = (transition + PRELOAD_MARGIN_MS).coerceAtMost(MAX_PRELOAD_LEAD_MS)
    )
}

internal fun equalPowerCrossfade(progress: Float): CrossfadeGains {
    val t = progress.coerceIn(0f, 1f)
    val angle = t * (PI.toFloat() / 2f)
    return CrossfadeGains(outgoing = cos(angle), incoming = sin(angle))
}

internal fun crossfadeHandoffNeedsResync(
    primaryPositionMs: Long,
    secondaryPositionMs: Long,
    toleranceMs: Long = 250L
): Boolean = abs(primaryPositionMs.coerceAtLeast(0L) - secondaryPositionMs.coerceAtLeast(0L)) > toleranceMs.coerceAtLeast(0L)

internal fun crossfadeHandoffSeekPosition(
    secondaryPositionMs: Long,
    durationMs: Long,
    leadMs: Long = 120L
): Long {
    val requested = secondaryPositionMs.coerceAtLeast(0L) + leadMs.coerceAtLeast(0L)
    if (durationMs <= 0L) return requested
    return requested.coerceAtMost((durationMs - 1L).coerceAtLeast(0L))
}

private const val MIN_TRACK_MS = 30_000L
private const val MIN_TRANSITION_MS = 800L
private const val MAX_TRANSITION_MS = 12_000L
private const val PRELOAD_MARGIN_MS = 8_000L
private const val MAX_PRELOAD_LEAD_MS = 20_000L
