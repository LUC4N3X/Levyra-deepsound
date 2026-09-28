package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.ui.InstantArtworkPlaceholder
import com.luc4n3x.levyra.ui.LevyraPlayerPane
import com.luc4n3x.levyra.ui.MotionArtworkLayer
import com.luc4n3x.levyra.ui.MotionArtworkPresentation
import com.luc4n3x.levyra.ui.PlayerAmbience
import com.luc4n3x.levyra.ui.artwork.ArtworkDissolveEdge
import com.luc4n3x.levyra.ui.artwork.LivingArtworkColors
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.artwork.artworkDissolve

internal enum class PlayerCinematicLayout {
    Stacked,
    SideBySide
}

@Immutable
internal data class PlayerCinematicGeometry(
    val layout: PlayerCinematicLayout,
    val heroWidth: Dp,
    val heroHeight: Dp,
    val sideDissolve: Boolean
)

internal val PlayerCinematicTitleOverlap: Dp = 20.dp
internal const val PlayerCinematicStackedFade = 0.32f
internal const val PlayerCinematicSideFade = 0.34f
internal const val PlayerCinematicMaxStackedAspect = 1.2f
internal const val PlayerCinematicSideDissolveFraction = 0.16f
private const val PlayerCinematicBloomReach = 0.42f
private const val PlayerCinematicBloomAlpha = 0.46f
private const val PlayerCinematicBloomEdgeAlpha = 0.32f
private const val PlayerCinematicLowerVeilStart = 0.42f
private const val PlayerCinematicLowerVeilMid = 0.72f
private val PlayerCinematicChromeScrim: Dp = 104.dp

internal fun playerCinematicStackedHeroBottom(
    statusBarTop: Dp,
    chromeTopPadding: Dp,
    headerHeight: Dp,
    itemSpacing: Dp,
    heroVerticalPadding: Dp,
    artworkSize: Dp,
    titleOverlap: Dp = PlayerCinematicTitleOverlap
): Dp = statusBarTop +
    chromeTopPadding +
    headerHeight +
    itemSpacing +
    heroVerticalPadding * 2 +
    artworkSize +
    itemSpacing +
    titleOverlap

internal fun playerCinematicGeometry(
    pane: LevyraPlayerPane,
    containerWidth: Dp,
    containerHeight: Dp,
    stackedHeroBottom: Dp,
    paneGap: Dp
): PlayerCinematicGeometry = when (pane) {
    LevyraPlayerPane.SideBySide -> PlayerCinematicGeometry(
        layout = PlayerCinematicLayout.SideBySide,
        heroWidth = min(containerWidth, containerWidth / 2 + paneGap / 2),
        heroHeight = containerHeight,
        sideDissolve = false
    )
    LevyraPlayerPane.Stacked -> {
        val heroHeight = min(stackedHeroBottom, containerHeight)
        val heroWidth = min(containerWidth, heroHeight * PlayerCinematicMaxStackedAspect)
        PlayerCinematicGeometry(
            layout = PlayerCinematicLayout.Stacked,
            heroWidth = heroWidth,
            heroHeight = heroHeight,
            sideDissolve = heroWidth < containerWidth
        )
    }
}

@Composable
internal fun PlayerCinematicStage(
    track: Track,
    artworkUrl: String,
    motionArtwork: MotionArtwork?,
    livingArtwork: LivingArtworkColors?,
    ambience: PlayerAmbience,
    backgroundMode: PlayerBackgroundMode,
    geometry: PlayerCinematicGeometry,
    motionEnabled: Boolean,
    animationsEnabled: Boolean,
    isPlaying: Boolean,
    canvasQuality: LevyraCanvasQuality,
    morphAnchors: PlayerMorphAnchors,
    morphActive: Boolean,
    swipeOffset: () -> Float,
    modifier: Modifier = Modifier
) {
    val stacked = geometry.layout == PlayerCinematicLayout.Stacked
    val colorField = backgroundMode == PlayerBackgroundMode.Dynamic ||
        backgroundMode == PlayerBackgroundMode.Blur
    val bloom = animateColorAsState(
        targetValue = ambience.tint,
        animationSpec = if (animationsEnabled) tween(700, easing = LinearOutSlowInEasing) else snap(),
        label = "player-cinematic-bloom"
    )

    DisposableEffect(morphAnchors) {
        morphAnchors.updateStageActive(true)
        onDispose { morphAnchors.updateStageActive(false) }
    }

    Box(modifier = modifier) {
        if (colorField) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (stacked) {
                            drawStackedCinematicField(ambience)
                        } else {
                            drawSideCinematicBloom(bloom.value, geometry.heroWidth.toPx())
                        }
                    }
            )
        }

        Box(
            modifier = Modifier
                .align(if (stacked) Alignment.TopCenter else Alignment.TopStart)
                .size(width = geometry.heroWidth, height = geometry.heroHeight)
                .playerMorphAnchor(morphAnchors, PlayerMorphSlot.Stage)
                .graphicsLayer {
                    alpha = if (morphActive) morphAnchors.stageRevealAlpha() else 1f
                    translationX = swipeOffset() * 0.32f
                }
                .artworkDissolve(
                    edge = if (stacked) ArtworkDissolveEdge.Bottom else ArtworkDissolveEdge.End,
                    fadeFraction = if (stacked) PlayerCinematicStackedFade else PlayerCinematicSideFade,
                    sideFadeFraction = if (geometry.sideDissolve) PlayerCinematicSideDissolveFraction else 0f
                )
        ) {
            MotionArtworkLayer(
                artwork = motionArtwork,
                enabled = motionEnabled,
                isPlaying = isPlaying,
                cornerRadius = 0.dp,
                presentation = MotionArtworkPresentation.Cinematic,
                quality = canvasQuality,
                livingArtwork = livingArtwork,
                modifier = Modifier.fillMaxSize()
            ) {
                SeamlessArtworkImage(url = artworkUrl, modifier = Modifier.fillMaxSize()) {
                    InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
                }
            }
        }

        if (stacked && colorField) {
            PlayerCinematicLowerVeil(
                primary = ambience.primary,
                secondary = ambience.secondary,
                base = ambience.base,
                modifier = Modifier.fillMaxSize()
            )
        }
        PlayerCinematicChromeScrim(modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun PlayerCinematicLowerVeil(
    primary: Color,
    secondary: Color,
    base: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.drawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.72f),
                    radius = size.maxDimension * 0.58f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(secondary.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.64f),
                    radius = size.maxDimension * 0.52f
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        PlayerCinematicLowerVeilStart to Color.Transparent,
                        PlayerCinematicLowerVeilMid to base.copy(alpha = 0.24f),
                        1f to Color.Black.copy(alpha = 0.70f)
                    )
                )
            )
        }
    )
}

@Composable
private fun PlayerCinematicChromeScrim(modifier: Modifier = Modifier) {
    val height = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + PlayerCinematicChromeScrim
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.52f),
                    0.55f to Color.Black.copy(alpha = 0.22f),
                    1f to Color.Transparent
                )
            )
    )
}

private fun DrawScope.drawStackedCinematicField(ambience: PlayerAmbience) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(ambience.primary.copy(alpha = 0.34f), Color.Transparent),
            center = Offset(size.width * 0.18f, size.height * 0.70f),
            radius = size.maxDimension * 0.72f
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(ambience.secondary.copy(alpha = 0.28f), Color.Transparent),
            center = Offset(size.width * 0.88f, size.height * 0.58f),
            radius = size.maxDimension * 0.64f
        )
    )
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.56f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.62f)
        )
    )
}

private fun DrawScope.drawSideCinematicBloom(color: Color, heroWidthPx: Float) {
    val heroEnd = heroWidthPx.coerceAtMost(size.width)
    val start = heroEnd * (1f - PlayerCinematicSideFade)
    val end = (heroEnd * (1f + PlayerCinematicBloomReach)).coerceAtMost(size.width)
    if (end <= start) return
    val edge = ((heroEnd - start) / (end - start)).coerceIn(0f, 1f)
    val rtl = layoutDirection == LayoutDirection.Rtl
    drawRect(
        brush = Brush.horizontalGradient(
            0f to color.copy(alpha = PlayerCinematicBloomAlpha),
            edge to color.copy(alpha = PlayerCinematicBloomEdgeAlpha),
            1f to Color.Transparent,
            startX = if (rtl) size.width - start else start,
            endX = if (rtl) size.width - end else end
        ),
        topLeft = Offset(if (rtl) size.width - end else start, 0f),
        size = Size(end - start, size.height)
    )
}
