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
internal val PlayerStageSpillHeight: Dp = 120.dp

private const val StageTopScrimEnd = 0.24f
private const val StageVeilLead = 0.52f
private const val StageVeilKnee = 0.58f

@Immutable
internal data class PlayerStageMetrics(
    val stageHeightDp: Float,
    val veilSettleFraction: Float
)

/**
 * Resolves how tall the stage is and where its veil has to be fully settled.
 *
 * The stage keeps one constant height whether a Canvas is rendering or only static artwork is
 * available, so the composition never resizes when motion artwork arrives or disappears. The veil
 * finishes exactly where the console content starts, which is what lets the image hand over to the
 * player surface without a visible seam on any screen size.
 */
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

/**
 * The cinematic stage: one large, edge-to-edge surface that hosts either a Canvas or the static
 * artwork.
 *
 * Motion artwork and static artwork share the same frame and the same veil, so the player reads as
 * one composition in both cases. The veil is derived from the live artwork palette and resolves to
 * the exact colour the console is painted with, so the image melts into the player surface instead
 * of sitting on top of it like a wallpaper.
 */
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
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                veilStart to Color.Transparent,
                                veilKnee to veil.copy(alpha = 0.34f),
                                (veilKnee + (settle - veilKnee) * 0.62f) to veil.copy(alpha = 0.78f),
                                settle to veil,
                                1.00f to veil
                            )
                        )
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black.copy(alpha = 0.58f),
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

/**
 * The surface the stage hands the image over to.
 *
 * It starts on the exact veil colour so the two layers read as one continuous material, then loses
 * a little light toward the bottom so the transport still sits on the darkest part of the screen.
 */
@Composable
internal fun PlayerConsoleSurface(
    ambience: PlayerAmbience,
    modifier: Modifier = Modifier
) {
    val base = ambience.base
    val control = ambience.control
    val elevated = ambience.elevated
    val floor = control.playerAmbienceMix(base, 0.42f)
    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to base,
                        0.34f to base.playerAmbienceMix(control, 0.72f),
                        0.66f to elevated,
                        1.00f to floor
                    )
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambience.primary.copy(alpha = 0.13f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.08f),
                    radius = size.maxDimension * 0.92f
                )
            )
        }
    )
}

/**
 * The horizon: the one structural line of the composition.
 *
 * It marks where the image world ends and the information world begins, and it is the only place
 * the live accent colour is allowed to touch the console. Its brightness follows playback, so the
 * seam breathes with the track instead of being static decoration.
 */
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
        targetValue = if (isPlaying) 1f else 0.45f,
        animationSpec = if (animationsEnabled) tween(720, easing = LevyraPlayerDesign.Standard) else snap(),
        label = "player-stage-horizon"
    )
    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to accent.copy(alpha = 0.15f * intensity),
                        0.45f to secondary.copy(alpha = 0.06f * intensity),
                        1.00f to Color.Transparent
                    )
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height)
            )
            val hairline = LevyraPlayerDesign.Hairline.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.22f to accent.copy(alpha = 0.42f * intensity),
                        0.58f to secondary.copy(alpha = 0.26f * intensity),
                        1.00f to Color.Transparent
                    )
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, hairline)
            )
        }
    )
}
