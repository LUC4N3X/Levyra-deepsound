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
internal val PlayerStageSpillHeight: Dp = 136.dp

private const val StageTopScrimEnd = 0.22f
private const val StageVeilLead = 0.46f
private const val StageVeilKnee = 0.60f
private const val StageSideVignetteAlpha = 0.24f

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
    val settle = veilSettleFraction.coerceIn(0.35f, 0.98f)
    val veilStart = (settle - StageVeilLead).coerceIn(0.10f, settle)
    val veilKnee = (veilStart + (settle - veilStart) * StageVeilKnee).coerceIn(veilStart, settle)

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
                                0.16f to Color.Transparent,
                                0.80f to Color.Transparent,
                                1.00f to Color.Black.copy(alpha = StageSideVignetteAlpha * 0.82f)
                            )
                        )
                    )

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.16f),
                                ambience.primary.copy(alpha = 0.07f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.22f, size.height * settle * 0.92f),
                            radius = size.maxDimension * 0.64f
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambience.secondary.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.82f, size.height * settle * 0.76f),
                            radius = size.maxDimension * 0.50f
                        )
                    )

                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                veilStart to Color.Transparent,
                                veilKnee to veil.copy(alpha = 0.28f),
                                (veilKnee + (settle - veilKnee) * 0.58f) to veil.copy(alpha = 0.70f),
                                settle to veil,
                                1.00f to veil
                            )
                        )
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black.copy(alpha = 0.52f),
                                0.08f to Color.Black.copy(alpha = 0.24f),
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
    val floor = control.playerAmbienceMix(base, 0.36f)
    val mid = base.playerAmbienceMix(control, 0.62f)
    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to base,
                        0.22f to mid,
                        0.64f to elevated,
                        1.00f to floor
                    )
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.primary.copy(alpha = 0.18f),
                        ambience.primary.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.20f, size.height * 0.02f),
                    radius = size.maxDimension * 0.76f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.secondary.copy(alpha = 0.11f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.78f, size.height * 0.30f),
                    radius = size.maxDimension * 0.60f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.primary.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.50f, size.height * 0.78f),
                    radius = size.maxDimension * 0.44f
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.78f to Color.Transparent,
                        1.00f to Color.Black.copy(alpha = 0.14f)
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
    val intensity by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.55f,
        animationSpec = if (animationsEnabled) tween(720, easing = LevyraPlayerDesign.Standard) else snap(),
        label = "player-stage-horizon"
    )
    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.18f * intensity),
                        accent.copy(alpha = 0.05f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.30f, 0f),
                    radius = size.width * 0.58f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondary.copy(alpha = 0.12f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.72f, 0f),
                    radius = size.width * 0.46f
                )
            )
            val hairline = LevyraPlayerDesign.Hairline.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.18f to accent.copy(alpha = 0.18f * intensity),
                        0.52f to secondary.copy(alpha = 0.12f * intensity),
                        0.84f to accent.copy(alpha = 0.08f * intensity),
                        1.00f to Color.Transparent
                    )
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, hairline)
            )
        }
    )
}
