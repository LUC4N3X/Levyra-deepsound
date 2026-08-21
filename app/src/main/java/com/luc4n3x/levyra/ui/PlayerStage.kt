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

internal const val PlayerStageHeightFraction = 0.58f
internal const val PlayerStageMinOverlapDp = 32f
internal val PlayerStageSpillHeight: Dp = 144.dp

private const val StageTopScrimEnd = 0.20f
private const val StageVeilLead = 0.48f
private const val StageVeilKnee = 0.56f
private const val StageSideVignetteAlpha = 0.055f

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
    val settle = veilSettleFraction.coerceIn(0.35f, 0.98f)
    val veilStart = (settle - StageVeilLead).coerceIn(0.08f, settle)
    val veilKnee = (veilStart + (settle - veilStart) * StageVeilKnee).coerceIn(veilStart, settle)
    val handoff = ambience.base.playerAmbienceMix(ambience.control, 0.20f)
    val bloom = ambience.primary.playerAmbienceMix(ambience.secondary, 0.38f)

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
                                0.12f to Color.Transparent,
                                0.88f to Color.Transparent,
                                1.00f to Color.Black.copy(alpha = StageSideVignetteAlpha)
                            )
                        )
                    )

                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                veilStart to Color.Transparent,
                                (veilStart + (veilKnee - veilStart) * 0.42f) to handoff.copy(alpha = 0.035f),
                                veilKnee to handoff.copy(alpha = 0.10f),
                                (veilKnee + (settle - veilKnee) * 0.52f) to handoff.copy(alpha = 0.24f),
                                settle to handoff.copy(alpha = 0.46f),
                                (settle + (1f - settle) * 0.46f) to handoff.copy(alpha = 0.68f),
                                1.00f to handoff.copy(alpha = 0.90f)
                            )
                        )
                    )

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                bloom.copy(alpha = 0.075f),
                                ambience.primary.copy(alpha = 0.025f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.24f, size.height * 0.92f),
                            radius = size.maxDimension * 0.58f
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambience.secondary.copy(alpha = 0.055f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.80f, size.height * 0.88f),
                            radius = size.maxDimension * 0.46f
                        )
                    )

                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black.copy(alpha = 0.36f),
                                0.07f to Color.Black.copy(alpha = 0.11f),
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
    val handoff = ambience.base.playerAmbienceMix(ambience.control, 0.20f)
    val lower = ambience.elevated.playerAmbienceMix(ambience.base, 0.48f)
    val bloom = ambience.primary.playerAmbienceMix(ambience.secondary, 0.40f)

    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind

            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to handoff.copy(alpha = 0.90f),
                        0.24f to handoff.copy(alpha = 0.94f),
                        0.62f to lower.copy(alpha = 0.96f),
                        1.00f to lower
                    )
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.primary.copy(alpha = 0.10f),
                        ambience.primary.copy(alpha = 0.025f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.18f, 0f),
                    radius = size.width * 0.92f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.secondary.copy(alpha = 0.075f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.84f, size.height * 0.16f),
                    radius = size.width * 0.78f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bloom.copy(alpha = 0.045f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.50f, size.height * 0.72f),
                    radius = size.maxDimension * 0.62f
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
    val handoff = ambience.base.playerAmbienceMix(ambience.control, 0.20f)
    val bloom = ambience.primary.playerAmbienceMix(ambience.secondary, 0.40f)
    val intensity by animateFloatAsState(
        targetValue = if (isPlaying) 0.72f else 0.54f,
        animationSpec = if (animationsEnabled) tween(720, easing = LevyraPlayerDesign.Standard) else snap(),
        label = "player-stage-handoff"
    )

    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind

            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.16f to handoff.copy(alpha = 0.025f * intensity),
                        0.38f to handoff.copy(alpha = 0.075f * intensity),
                        0.58f to handoff.copy(alpha = 0.14f * intensity),
                        0.78f to handoff.copy(alpha = 0.075f * intensity),
                        1.00f to Color.Transparent
                    )
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height)
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bloom.copy(alpha = 0.065f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.30f, size.height * 0.50f),
                    radius = size.width * 0.82f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.secondary.copy(alpha = 0.045f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.78f, size.height * 0.42f),
                    radius = size.width * 0.66f
                )
            )
        }
    )
}
