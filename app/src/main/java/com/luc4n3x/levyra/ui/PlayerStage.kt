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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

internal const val PlayerStageHeightFraction = 0.58f
internal const val PlayerStageMinOverlapDp = 32f
internal val PlayerStageSpillHeight: Dp = 176.dp

private const val StageTopScrimEnd = 0.20f
private const val StageSideVignetteAlpha = 0.045f

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
    val fadeStart = (settle - 0.26f).coerceIn(0.58f, 0.72f)
    val fadeSoft = (fadeStart + 0.10f).coerceAtMost(0.82f)
    val fadeMid = (fadeStart + 0.18f).coerceAtMost(0.90f)
    val fadeLow = (fadeStart + 0.25f).coerceAtMost(0.96f)
    val handoff = ambience.base.playerAmbienceMix(ambience.control, 0.18f)
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
                    compositingStrategy = CompositingStrategy.Offscreen
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
                                fadeStart to Color.Transparent,
                                fadeSoft to handoff.copy(alpha = 0.045f),
                                fadeMid to handoff.copy(alpha = 0.10f),
                                fadeLow to handoff.copy(alpha = 0.15f),
                                1.00f to handoff.copy(alpha = 0.18f)
                            )
                        )
                    )

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                bloom.copy(alpha = 0.065f),
                                ambience.primary.copy(alpha = 0.020f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.24f, size.height * 0.84f),
                            radius = size.maxDimension * 0.62f
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambience.secondary.copy(alpha = 0.045f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.80f, size.height * 0.88f),
                            radius = size.maxDimension * 0.50f
                        )
                    )

                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black.copy(alpha = 0.34f),
                                0.07f to Color.Black.copy(alpha = 0.10f),
                                StageTopScrimEnd to Color.Transparent
                            )
                        )
                    )

                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.White,
                                fadeStart to Color.White,
                                fadeSoft to Color.White.copy(alpha = 0.94f),
                                fadeMid to Color.White.copy(alpha = 0.70f),
                                fadeLow to Color.White.copy(alpha = 0.34f),
                                1.00f to Color.Transparent
                            )
                        ),
                        blendMode = BlendMode.DstIn
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
    val handoff = ambience.base.playerAmbienceMix(ambience.control, 0.18f)
    val lower = ambience.elevated.playerAmbienceMix(ambience.base, 0.46f)
    val bloom = ambience.primary.playerAmbienceMix(ambience.secondary, 0.40f)

    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind

            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.18f to handoff.copy(alpha = 0.035f),
                        0.38f to handoff.copy(alpha = 0.11f),
                        0.62f to lower.copy(alpha = 0.30f),
                        0.82f to lower.copy(alpha = 0.52f),
                        1.00f to lower.copy(alpha = 0.70f)
                    )
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.primary.copy(alpha = 0.070f),
                        ambience.primary.copy(alpha = 0.020f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.06f),
                    radius = size.width * 0.96f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.secondary.copy(alpha = 0.055f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.84f, size.height * 0.20f),
                    radius = size.width * 0.82f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bloom.copy(alpha = 0.035f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.52f, size.height * 0.74f),
                    radius = size.maxDimension * 0.64f
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
    val mist = ambience.base.playerAmbienceMix(ambience.control, 0.16f)
    val bloom = ambience.primary.playerAmbienceMix(ambience.secondary, 0.42f)
    val intensity by animateFloatAsState(
        targetValue = if (isPlaying) 0.60f else 0.46f,
        animationSpec = if (animationsEnabled) tween(760, easing = LevyraPlayerDesign.Standard) else snap(),
        label = "player-stage-ambient-mist"
    )

    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind

            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.24f to mist.copy(alpha = 0.020f * intensity),
                        0.48f to mist.copy(alpha = 0.060f * intensity),
                        0.68f to mist.copy(alpha = 0.045f * intensity),
                        1.00f to Color.Transparent
                    )
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height)
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bloom.copy(alpha = 0.050f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.30f, size.height * 0.46f),
                    radius = size.width * 0.88f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.secondary.copy(alpha = 0.038f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.78f, size.height * 0.54f),
                    radius = size.width * 0.72f
                )
            )
        }
    )
}
