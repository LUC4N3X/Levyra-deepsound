package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.luc4n3x.levyra.player.VisualizerAudioProcessor
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import kotlinx.coroutines.delay

private const val PresenceActive = 1f
private const val PresencePaused = 0.72f
private const val PresenceMillis = 700
private const val MaxFrameStepSeconds = 0.1f
private const val SheenAlpha = 0.07f
private const val FarLobeReach = 0.34f
private const val BreathingTempoRise = 0.6f
private const val BreathingTempoFall = 0.35f
private const val SilentPollMillis = 100L
private val NoSignal = FloatArray(0)

@Stable
internal class ReactiveAuraClock {
    var phase by mutableFloatStateOf(0f)
        private set
    var loudness by mutableFloatStateOf(0f)
        private set
    var onset by mutableFloatStateOf(0f)
        private set

    private val signal = ReactiveAuraSignal()
    private var tempo = 1f

    val settled: Boolean get() = signal.settled

    fun step(
        dtSeconds: Float,
        tuning: ReactiveAuraTuning,
        samples: FloatArray,
        advancePhase: Boolean
    ) {
        val dt = dtSeconds.coerceIn(0f, MaxFrameStepSeconds)
        signal.advance(samples, dt)
        val target = auraTempoTarget(tuning, signal.loudness)
        tempo = followToward(tempo, target, if (target > tempo) BreathingTempoRise else BreathingTempoFall, dt)
        if (advancePhase && !signal.settled) phase = advanceAuraPhase(phase, dt, tuning.cycleSeconds, tempo)
        loudness = signal.loudness
        onset = signal.onset
    }
}

@Composable
internal fun ReactiveAuraBackdrop(
    primary: State<Color>,
    secondary: State<Color>,
    base: State<Color>,
    isPlaying: Boolean,
    animationsEnabled: Boolean,
    style: ReactiveAuraStyle,
    focusY: Float,
    modifier: Modifier = Modifier
) {
    val tuning = remember(style) { ReactiveAuraTuning.of(style) }
    val clock = remember { ReactiveAuraClock() }
    val decorative = LocalLevyraVisualCapabilities.current.decorativeMotion
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val animate = animationsEnabled && decorative && lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val presence = animateFloatAsState(
        targetValue = if (isPlaying) PresenceActive else PresencePaused,
        animationSpec = LevyraPlayerDesign.motion(animationsEnabled && decorative, LevyraPlayerDesign.emphasizedTween(PresenceMillis)),
        label = "player-aura-presence"
    )

    LaunchedEffect(animate, isPlaying, tuning) {
        if (!animate) return@LaunchedEffect
        var lastNanos = 0L
        var carryNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    carryNanos += now - lastNanos
                    if (carryNanos >= tuning.minFrameNanos) {
                        val samples = if (isPlaying) VisualizerAudioProcessor.waveformState.value else NoSignal
                        clock.step(carryNanos / 1_000_000_000f, tuning, samples, advancePhase = isPlaying)
                        carryNanos = 0L
                    }
                }
                lastNanos = now
            }
            if (!isPlaying && clock.settled) break
            if (clock.settled) delay(SilentPollMillis)
        }
    }

    Box(
        modifier = modifier.drawWithCache {
            val painter = ReactiveAuraPainter(size.width, size.height)
            onDrawBehind {
                painter.draw(
                    scope = this,
                    primary = primary.value,
                    secondary = secondary.value,
                    base = base.value,
                    tuning = tuning,
                    phase = clock.phase,
                    loudness = clock.loudness,
                    onset = clock.onset,
                    presence = presence.value,
                    focusY = focusY
                )
            }
        }
    )
}

private class ReactiveAuraPainter(private val width: Float, private val height: Float) {
    private val extent = (width + height) / 2f
    private val frame = AuraLobeFrame()
    private val scrim = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color.Black.copy(alpha = 0.20f),
            0.18f to Color.Black.copy(alpha = 0.05f),
            0.48f to Color.Black.copy(alpha = 0.10f),
            0.74f to Color.Black.copy(alpha = 0.38f),
            1.00f to Color.Black.copy(alpha = 0.74f)
        ),
        startY = 0f,
        endY = height
    )

    private var cachedPrimary = Color.Unspecified
    private var cachedSecondary = Color.Unspecified
    private var cachedLobeRadius = 0f
    private var primaryBrush: Brush? = null
    private var secondaryBrush: Brush? = null
    private var bridgeBrush: Brush? = null
    private var sheenBrush: Brush? = null
    private var farBrush: Brush? = null
    private val farRadius = extent * FarLobeReach * AuraFarLobe.radiusScale

    fun draw(
        scope: DrawScope,
        primary: Color,
        secondary: Color,
        base: Color,
        tuning: ReactiveAuraTuning,
        phase: Float,
        loudness: Float,
        onset: Float,
        presence: Float,
        focusY: Float
    ) {
        ensureBrushes(primary, secondary, tuning)
        with(scope) {
            drawRect(lerp(base, Color.Black, 0.5f))
            val nearRadius = extent * tuning.lobeRadius
            val breath = auraBreath(onset, tuning)
            val nearAlpha = auraGlow(tuning, tuning.nearAlpha, loudness, onset, presence)
            val farAlpha = auraGlow(tuning, tuning.farAlpha, loudness * 0.5f, 0f, presence)

            placeAuraLobe(AuraFarLobe, phase, -1f, focusY, frame)
            drawLobe(farBrush, farRadius, farAlpha)

            for (spec in AuraNearLobes) {
                placeAuraLobe(spec, phase, 1f, focusY, frame)
                val brush = when (spec.role) {
                    AuraLobeRole.Primary -> primaryBrush
                    AuraLobeRole.Secondary -> secondaryBrush
                    AuraLobeRole.Bridge -> bridgeBrush
                }
                drawLobe(brush, nearRadius * spec.radiusScale * breath, nearAlpha)
            }

            placeAuraLobe(AuraSheenLobe, phase, 1f, focusY, frame)
            drawLobe(sheenBrush, nearRadius * AuraSheenLobe.radiusScale, SheenAlpha * presence)

            drawRect(scrim)
        }
    }

    private fun DrawScope.drawLobe(brush: Brush?, radius: Float, alpha: Float) {
        if (brush == null || alpha <= 0.001f) return
        translate(frame.x * width, frame.y * height) {
            rotate(frame.rotationDegrees, Offset.Zero) {
                scale(frame.stretch, 1f, Offset.Zero) {
                    drawCircle(brush = brush, radius = radius, center = Offset.Zero, alpha = alpha)
                }
            }
        }
    }

    private fun ensureBrushes(primary: Color, secondary: Color, tuning: ReactiveAuraTuning) {
        if (primary == cachedPrimary && secondary == cachedSecondary && tuning.lobeRadius == cachedLobeRadius && primaryBrush != null) return
        cachedPrimary = primary
        cachedSecondary = secondary
        cachedLobeRadius = tuning.lobeRadius
        val first = auraTone(primary)
        val second = auraTone(secondary)
        val bridge = lerp(first, second, 0.5f)
        val nearRadius = extent * tuning.lobeRadius
        primaryBrush = softLobe(first, nearRadius)
        secondaryBrush = softLobe(second, nearRadius)
        bridgeBrush = softLobe(bridge, nearRadius)
        farBrush = softLobe(lerp(bridge, Color.Black, 0.25f), farRadius)
        sheenBrush = softLobe(lerp(first, Color.White, 0.55f), nearRadius * AuraSheenLobe.radiusScale)
    }

    private fun softLobe(color: Color, radius: Float): Brush = Brush.radialGradient(
        colorStops = arrayOf(
            0.00f to color,
            0.38f to color.copy(alpha = 0.60f),
            0.72f to color.copy(alpha = 0.16f),
            1.00f to color.copy(alpha = 0f)
        ),
        center = Offset.Zero,
        radius = radius
    )
}
