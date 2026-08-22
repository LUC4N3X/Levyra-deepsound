package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
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
    val handoff = ambience.base.playerAmbienceMix(Color.Black, 0.18f)
    val deep = handoff.playerAmbienceMix(Color.Black, 0.30f)

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
                                fadeMid to handoff.copy(alpha = 0.20f),
                                fadeStrong to handoff.copy(alpha = 0.52f),
                                0.965f to handoff.copy(alpha = 0.84f),
                                1.00f to deep
                            )
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
    val handoff = ambience.base.playerAmbienceMix(Color.Black, 0.18f)
    val deep = handoff.playerAmbienceMix(Color.Black, 0.30f)

    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to deep,
                        0.44f to deep,
                        1.00f to deep.playerAmbienceMix(Color.Black, 0.22f)
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
    Box(modifier = modifier)
}
