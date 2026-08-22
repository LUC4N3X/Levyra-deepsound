package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.feature.motion.MotionArtwork

internal const val PlayerStageHeightFraction = 0.64f
internal const val PlayerStageMinOverlapDp = 48f
internal val PlayerStageSpillHeight: Dp = 96.dp

private const val StageTopScrimEnd = 0.20f
private const val StageSideVignetteAlpha = 0.035f

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
    val fadeStart = (settle - 0.15f).coerceIn(0.66f, 0.76f)
    val fadeMid = (settle - 0.035f).coerceIn(fadeStart, 0.86f)
    val fadeStrong = (settle + 0.065f).coerceIn(fadeMid, 0.94f)
    val songColor = ambience.primary.playerAmbienceMix(ambience.secondary, 0.46f)
    val handoff = ambience.base
        .playerAmbienceMix(ambience.tint, 0.34f)
        .playerAmbienceMix(songColor, 0.10f)
    val deep = ambience.elevated
        .playerAmbienceMix(ambience.tint, 0.36f)
        .playerAmbienceMix(songColor, 0.14f)

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
                                0.10f to Color.Transparent,
                                0.90f to Color.Transparent,
                                1.00f to Color.Black.copy(alpha = StageSideVignetteAlpha)
                            )
                        )
                    )

                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                fadeStart to Color.Transparent,
                                fadeMid to handoff.copy(alpha = 0.18f),
                                fadeStrong to handoff.copy(alpha = 0.48f),
                                0.965f to deep.copy(alpha = 0.84f),
                                1.00f to deep
                            )
                        )
                    )

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambience.primary.copy(alpha = 0.055f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.22f, size.height),
                            radius = size.width * 0.78f
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambience.secondary.copy(alpha = 0.045f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.82f, size.height * 0.96f),
                            radius = size.width * 0.66f
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
    val songColor = ambience.primary.playerAmbienceMix(ambience.secondary, 0.46f)
    val deep = ambience.elevated
        .playerAmbienceMix(ambience.tint, 0.36f)
        .playerAmbienceMix(songColor, 0.14f)
    val middle = deep.playerAmbienceMix(ambience.tint, 0.14f)
    val lower = deep.playerAmbienceMix(songColor, 0.08f)

    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind

            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to deep,
                        0.36f to middle,
                        0.72f to lower,
                        1.00f to deep
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
                    center = Offset(size.width * 0.16f, size.height * 0.10f),
                    radius = size.width * 0.88f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.secondary.copy(alpha = 0.085f),
                        ambience.secondary.copy(alpha = 0.020f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.84f, size.height * 0.26f),
                    radius = size.width * 0.76f
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
    Box(modifier = modifier)
}
