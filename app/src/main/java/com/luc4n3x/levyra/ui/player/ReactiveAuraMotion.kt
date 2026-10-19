package com.luc4n3x.levyra.ui.player

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.ReactiveAuraPreset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Immutable
internal data class ReactiveAuraStyle(
    val preset: ReactiveAuraPreset = ReactiveAuraPreset.Recommended,
    val intensity: Int = LevyraInterfaceSettings.DEFAULT_REACTIVE_AURA_INTENSITY
) {
    companion object {
        val Default = ReactiveAuraStyle()
    }
}

@Immutable
internal data class ReactiveAuraTuning(
    val cycleSeconds: Float,
    val tempoGain: Float,
    val loudnessGain: Float,
    val onsetGain: Float,
    val nearAlpha: Float,
    val farAlpha: Float,
    val lobeRadius: Float,
    val maxFps: Int
) {
    val minFrameNanos: Long get() = 1_000_000_000L / maxFps

    companion object {
        private const val MaxNearAlpha = 0.55f
        private const val MaxFarAlpha = 0.32f

        fun of(style: ReactiveAuraStyle): ReactiveAuraTuning {
            val amount = intensityAmount(style.intensity)
            val base = when (style.preset) {
                ReactiveAuraPreset.Calm -> Calm
                ReactiveAuraPreset.Flow -> Flow
                ReactiveAuraPreset.Pulse -> Pulse
            }
            return base.copy(
                tempoGain = base.tempoGain * amount,
                loudnessGain = base.loudnessGain * amount,
                onsetGain = base.onsetGain * amount,
                nearAlpha = (base.nearAlpha * amount).coerceAtMost(MaxNearAlpha),
                farAlpha = (base.farAlpha * amount).coerceAtMost(MaxFarAlpha)
            )
        }

        fun intensityAmount(intensity: Int): Float = 0.5f + intensity.coerceIn(0, 100) / 100f

        private val Calm = ReactiveAuraTuning(
            cycleSeconds = 150f,
            tempoGain = 0.12f,
            loudnessGain = 0.10f,
            onsetGain = 0f,
            nearAlpha = 0.30f,
            farAlpha = 0.17f,
            lobeRadius = 0.36f,
            maxFps = 24
        )

        private val Flow = ReactiveAuraTuning(
            cycleSeconds = 100f,
            tempoGain = 0.40f,
            loudnessGain = 0.22f,
            onsetGain = 0.10f,
            nearAlpha = 0.37f,
            farAlpha = 0.21f,
            lobeRadius = 0.40f,
            maxFps = 30
        )

        private val Pulse = ReactiveAuraTuning(
            cycleSeconds = 84f,
            tempoGain = 0.50f,
            loudnessGain = 0.34f,
            onsetGain = 0.70f,
            nearAlpha = 0.39f,
            farAlpha = 0.22f,
            lobeRadius = 0.40f,
            maxFps = 30
        )
    }
}

internal fun followToward(current: Float, target: Float, rate: Float, dtSeconds: Float): Float {
    if (dtSeconds <= 0f) return current
    val blend = 1f - exp(-rate * dtSeconds)
    return current + (target - current) * blend
}

internal fun auraLoudness(samples: FloatArray): Float {
    if (samples.isEmpty()) return 0f
    var sum = 0f
    for (sample in samples) sum += abs(sample)
    val mean = sum / samples.size
    return (1f - exp(-mean * LoudnessCompression)).coerceIn(0f, 1f)
}

internal class ReactiveAuraSignal {
    var loudness = 0f
        private set
    var onset = 0f
        private set
    var hasSignal = false
        private set

    private var fast = 0f
    private var slow = 0f

    val settled: Boolean get() = loudness < SettledEpsilon && onset < SettledEpsilon

    fun advance(samples: FloatArray, dtSeconds: Float) {
        hasSignal = samples.isNotEmpty()
        val level = auraLoudness(samples)
        loudness = followToward(loudness, level, if (level > loudness) LoudnessRise else LoudnessFall, dtSeconds)
        fast = followToward(fast, level, FastRate, dtSeconds)
        slow = followToward(slow, level, SlowRate, dtSeconds)
        val lift = if (hasSignal) ((fast - slow) / (slow + OnsetFloor) * OnsetScale).coerceIn(0f, 1f) else 0f
        onset = followToward(onset, lift, if (lift > onset) OnsetRise else OnsetFall, dtSeconds)
    }

    fun reset() {
        loudness = 0f
        onset = 0f
        fast = 0f
        slow = 0f
        hasSignal = false
    }

    private companion object {
        const val LoudnessRise = 2.6f
        const val LoudnessFall = 1.1f
        const val FastRate = 9f
        const val SlowRate = 0.7f
        const val OnsetRise = 7f
        const val OnsetFall = 2.4f
        const val OnsetFloor = 0.15f
        const val OnsetScale = 2f
        const val SettledEpsilon = 0.003f
    }
}

internal const val AuraTwoPi = (2.0 * PI).toFloat()
private const val LoudnessCompression = 6f

internal fun advanceAuraPhase(phase: Float, dtSeconds: Float, cycleSeconds: Float, tempo: Float): Float {
    val next = phase + dtSeconds * AuraTwoPi / cycleSeconds * tempo
    return next % AuraTwoPi
}

internal fun auraTempoTarget(tuning: ReactiveAuraTuning, loudness: Float): Float =
    1f + tuning.tempoGain * loudness.coerceIn(0f, 1f)

internal enum class AuraLobeRole { Primary, Secondary, Bridge }

internal class AuraLobeSpec(
    val role: AuraLobeRole,
    val centerX: Float,
    val centerY: Float,
    val driftX: Float,
    val driftY: Float,
    val harmonicX: Int,
    val harmonicY: Int,
    val offsetX: Float,
    val offsetY: Float,
    val rotationDegrees: Float,
    val rotationSwingDegrees: Float,
    val stretch: Float,
    val radiusScale: Float,
    val focusLinked: Boolean = false
)

internal class AuraLobeFrame {
    var x = 0f
    var y = 0f
    var rotationDegrees = 0f
    var stretch = 1f
}

internal val AuraNearLobes: Array<AuraLobeSpec> = arrayOf(
    AuraLobeSpec(AuraLobeRole.Primary, 0.24f, 0.24f, 0.15f, 0.09f, 1, 2, 0f, 1.1f, -22f, 10f, 1.30f, 1.00f, focusLinked = true),
    AuraLobeSpec(AuraLobeRole.Secondary, 0.80f, 0.54f, 0.11f, 0.13f, 2, 1, 2.0f, 0.4f, 24f, 12f, 1.35f, 0.94f),
    AuraLobeSpec(AuraLobeRole.Bridge, 0.34f, 0.84f, 0.17f, 0.06f, 1, 3, 4.1f, 2.2f, 8f, 9f, 1.25f, 0.88f)
)

internal val AuraFarLobe = AuraLobeSpec(
    AuraLobeRole.Bridge, 0.52f, 0.46f, 0.20f, 0.16f, 1, 1, 3.3f, 0.7f, 0f, 14f, 1.12f, 1.55f
)

internal val AuraSheenLobe = AuraLobeSpec(
    AuraLobeRole.Primary, 0.50f, 0.07f, 0.16f, 0.025f, 1, 2, 0.6f, 1.9f, 0f, 4f, 2.2f, 0.62f
)

internal fun placeAuraLobe(
    spec: AuraLobeSpec,
    phase: Float,
    direction: Float,
    focusY: Float,
    out: AuraLobeFrame
) {
    val angle = phase * direction
    val baseY = if (spec.focusLinked) 0.12f + focusY * 0.35f else spec.centerY
    out.x = spec.centerX + spec.driftX * sin(angle * spec.harmonicX + spec.offsetX)
    out.y = baseY + spec.driftY * cos(angle * spec.harmonicY + spec.offsetY)
    out.rotationDegrees = spec.rotationDegrees +
        spec.rotationSwingDegrees * sin(angle + spec.offsetX * 0.5f)
    out.stretch = spec.stretch
}

internal fun auraBreath(onset: Float, tuning: ReactiveAuraTuning): Float =
    1f + 0.05f * tuning.onsetGain * onset.coerceIn(0f, 1f)

internal fun auraGlow(tuning: ReactiveAuraTuning, base: Float, loudness: Float, onset: Float, presence: Float): Float {
    val lifted = base * (1f + tuning.loudnessGain * loudness.coerceIn(0f, 1f) +
        0.28f * tuning.onsetGain * onset.coerceIn(0f, 1f))
    return (lifted * presence).coerceIn(0f, 0.62f)
}

internal fun auraTone(color: Color): Color {
    val red = color.red
    val green = color.green
    val blue = color.blue
    val maxChannel = max(red, max(green, blue))
    val minChannel = min(red, min(green, blue))
    val delta = maxChannel - minChannel
    val saturation = if (maxChannel <= 0f) 0f else delta / maxChannel
    val hue = when {
        delta <= 0f -> 0f
        maxChannel == red -> 60f * (((green - blue) / delta + 6f) % 6f)
        maxChannel == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    return Color.hsv(
        hue = hue,
        saturation = saturation.coerceAtMost(AuraMaxSaturation),
        value = maxChannel.coerceIn(AuraMinValue, AuraMaxValue)
    )
}

private const val AuraMaxSaturation = 0.62f
private const val AuraMinValue = 0.42f
private const val AuraMaxValue = 0.80f
