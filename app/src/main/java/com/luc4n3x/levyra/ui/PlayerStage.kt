package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

internal const val PlayerStageHeightFraction = 0.72f
internal const val PlayerStageMinOverlapDp = 56f
internal val PlayerStageSpillHeight: Dp = 124.dp

private const val StageTopScrimEnd = 0.22f
private const val StageVeilLead = 0.46f
private const val StageVeilKnee = 0.60f
private const val StageSideVignetteAlpha = 0.16f

@Immutable
internal data class PlayerStageMetrics(
    val stageHeightDp: Float,
    val veilSettleFraction: Float
)

internal fun playerStageMetrics(
    availableHeightDp: Float,
    consoleHeightDp: Float
): PlayerStageMetrics {
    if (availableHeightDp <= 0f) return PlayerStageMetrics(0f, 1f)
    val console = consoleHeightDp.coerceIn(0f, availableHeightDp)
    val consoleTop = availableHeightDp - console
    val minimum = (consoleTop + PlayerStageMinOverlapDp).coerceAtMost(availableHeightDp)
    val stage = (availableHeightDp * PlayerStageHeightFraction)
        .coerceAtLeast(minimum)
        .coerceAtMost(availableHeightDp)
    val settle = if (stage <= 0f) 1f else (consoleTop / stage).coerceIn(0.35f, 0.98f)
    return PlayerStageMetrics(stageHeightDp = stage, veilSettleFraction = settle)
}

@Composable
internal fun PlayerStage(
    motionArtwork: MotionArtwork?,
    ambience: PlayerAmbience,
    animationsEnabled: Boolean,
    isPlaying: Boolean,
    canvasQuality: LevyraCanvasQuality,
    veilSettleFraction: Float,
    contentAlpha: () -> Float,
    contentTranslationX: () -> Float,
    contentScale: () -> Float,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    staticArtwork: @Composable () -> Unit
) {
    val veil = ambience.base
    val accent = ambience.primary.playerAmbienceMix(ambience.secondary, 0.34f)
    val handoff = veil.playerAmbienceMix(accent, 0.18f)
    val settle = veilSettleFraction.coerceIn(0.35f, 0.98f)
    val veilStart = (settle - StageVeilLead).coerceIn(0.10f, settle)
    val veilKnee = (veilStart + (settle - veilStart) * StageVeilKnee).coerceIn(veilStart, settle)
    val glowPeak = (settle + 0.015f).coerceIn(0f, 1f)
    val glowTail = (settle + 0.10f).coerceIn(glowPeak, 1f)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha()
                    translationX = contentTranslationX()
                    val scale = contentScale()
                    scaleX = scale
                    scaleY = scale
                }
                .drawWithContent {
                    drawContent()

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black.copy(alpha = StageSideVignetteAlpha),
                                0.18f to Color.Transparent,
                                0.82f to Color.Transparent,
                                1.00f to Color.Black.copy(alpha = StageSideVignetteAlpha * 0.80f)
                            )
                        )
                    )

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.12f),
                                ambience.primary.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.24f, size.height * settle * 0.92f),
                            radius = size.maxDimension * 0.62f
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambience.secondary.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.78f, size.height * settle * 0.82f),
                            radius = size.maxDimension * 0.48f
                        )
                    )

                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                veilStart to Color.Transparent,
                                veilKnee to handoff.copy(alpha = 0.14f),
                                (veilKnee + (settle - veilKnee) * 0.56f) to handoff.copy(alpha = 0.42f),
                                settle to handoff.copy(alpha = 0.78f),
                                1.00f to handoff.copy(alpha = 0.92f)
                            )
                        )
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                (settle - 0.05f).coerceIn(0f, 1f) to Color.Transparent,
                                glowPeak to Color.White.copy(alpha = 0.035f),
                                glowTail to Color.Transparent,
                                1.00f to Color.Transparent
                            )
                        )
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black.copy(alpha = 0.46f),
                                0.08f to Color.Black.copy(alpha = 0.18f),
                                StageTopScrimEnd to Color.Transparent
                            )
                        )
                    )
                }
        ) {
            MotionArtworkLayer(
                artwork = motionArtwork,
                enabled = animationsEnabled,
                isPlaying = isPlaying,
                cornerRadius = 0.dp,
                presentation = MotionArtworkPresentation.Stage,
                quality = canvasQuality,
                modifier = Modifier.fillMaxSize(),
                staticArtwork = staticArtwork
            )
        }
        overlay()
    }
}

@Composable
internal fun PlayerConsoleSurface(
    ambience: PlayerAmbience,
    modifier: Modifier = Modifier
) {
    val base = ambience.base
    val control = ambience.control
    val elevated = ambience.elevated
    val mist = base.playerAmbienceMix(control, 0.54f)
    val deep = elevated.playerAmbienceMix(control, 0.24f)
    val bloom = ambience.primary.playerAmbienceMix(ambience.secondary, 0.40f)
    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to base,
                        0.18f to mist,
                        0.54f to elevated.playerAmbienceMix(mist, 0.18f),
                        0.84f to deep,
                        1.00f to deep.playerAmbienceMix(base, 0.16f)
                    )
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.06f),
                        0.10f to Color.White.copy(alpha = 0.022f),
                        0.26f to Color.Transparent,
                        1.00f to Color.Transparent
                    )
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.primary.copy(alpha = 0.15f),
                        ambience.primary.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.06f),
                    radius = size.maxDimension * 0.82f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.secondary.copy(alpha = 0.11f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.18f),
                    radius = size.maxDimension * 0.64f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bloom.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.50f, size.height * 0.76f),
                    radius = size.maxDimension * 0.48f
                )
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.24f to ambience.primary.copy(alpha = 0.035f),
                        0.52f to bloom.copy(alpha = 0.055f),
                        0.80f to ambience.secondary.copy(alpha = 0.030f),
                        1.00f to Color.Transparent
                    )
                ),
                topLeft = Offset(0f, size.height * 0.26f),
                size = Size(size.width, size.height * 0.44f)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.82f to Color.Transparent,
                        1.00f to Color.Black.copy(alpha = 0.10f)
                    )
                )
            )
        }
    )
}

@Composable
internal fun PlayerStageHorizon(
    ambience: PlayerAmbience,
    animationsEnabled: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = ambience.primary
    val secondary = ambience.secondary
    val blend = accent.playerAmbienceMix(secondary, 0.42f)
    val intensity by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.65f,
        animationSpec = if (animationsEnabled) tween(720, easing = LevyraPlayerDesign.Standard) else snap(),
        label = "player-stage-horizon"
    )
    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.038f * intensity),
                        0.18f to Color.Transparent,
                        1.00f to Color.Transparent
                    )
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.13f * intensity),
                        accent.copy(alpha = 0.04f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.26f, 0f),
                    radius = size.width * 0.70f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondary.copy(alpha = 0.10f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.74f, 0f),
                    radius = size.width * 0.56f
                )
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.28f to blend.copy(alpha = 0.05f * intensity),
                        0.52f to Color.White.copy(alpha = 0.03f * intensity),
                        0.76f to blend.copy(alpha = 0.04f * intensity),
                        1.00f to Color.Transparent
                    )
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height * 0.58f)
            )
        }
    )
}
