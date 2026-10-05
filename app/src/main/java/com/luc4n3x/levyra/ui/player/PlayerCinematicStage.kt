package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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
import com.luc4n3x.levyra.ui.MotionBackdropPalette
import com.luc4n3x.levyra.ui.PlayerAmbience
import com.luc4n3x.levyra.ui.artwork.ArtworkDissolveEdge
import com.luc4n3x.levyra.ui.artwork.LivingArtworkColors
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.artwork.artworkDissolve
import java.util.Locale

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
internal const val PlayerCinematicStackedFade = 0.42f
internal const val PlayerCinematicSideFade = 0.34f
internal const val PlayerCinematicMaxStackedAspect = 1.2f
internal const val PlayerCinematicSideDissolveFraction = 0.16f
internal const val PlayerCinematicFullscreenCanvasDimAlpha = 0.5f
private const val PlayerCinematicBloomReach = 0.42f
private const val PlayerCinematicBloomAlpha = 0.46f
private const val PlayerCinematicBloomEdgeAlpha = 0.32f
private const val PlayerCinematicTopScrimMinAlpha = 0.16f
private const val PlayerCinematicTopScrimMaxAlpha = 0.65f
private const val PlayerCinematicFullscreenDimAnimationMs = 180
private const val SpotifyCanvasHostMarker = "://canvaz.scdn.co/"
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

internal fun playerCinematicDissolveStops(fadeFraction: Float): Array<Pair<Float, Color>> {
    val fade = fadeFraction.coerceIn(0.05f, 1f)
    val start = 1f - fade
    return arrayOf(
        0f to Color.Black,
        start to Color.Black,
        1f to Color.Transparent
    )
}

internal fun playerCinematicTopScrimAlpha(artworkLuminance: Float?): Float {
    val luminance = artworkLuminance?.coerceIn(0f, 1f) ?: 0f
    return PlayerCinematicTopScrimMinAlpha +
        (PlayerCinematicTopScrimMaxAlpha - PlayerCinematicTopScrimMinAlpha) * luminance
}

internal fun playerCinematicFullscreenDimAlpha(fullscreenCanvas: Boolean): Float =
    if (fullscreenCanvas) PlayerCinematicFullscreenCanvasDimAlpha else 0f

internal fun playerCinematicResolvedSideFade(sideDissolve: Boolean): Float =
    if (sideDissolve) PlayerCinematicSideDissolveFraction else 0f

internal fun playerCinematicUsesFullscreenCanvas(
    layout: PlayerCinematicLayout,
    sideDissolve: Boolean,
    motionUrl: String
): Boolean = layout == PlayerCinematicLayout.Stacked &&
    !sideDissolve &&
    motionUrl.lowercase(Locale.ROOT).contains(SpotifyCanvasHostMarker)

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
    modifier: Modifier = Modifier,
    onDynamicBackdropPalette: (MotionBackdropPalette?) -> Unit = {}
) {
    val stacked = geometry.layout == PlayerCinematicLayout.Stacked
    val fullscreenCanvas = motionEnabled && playerCinematicUsesFullscreenCanvas(
        layout = geometry.layout,
        sideDissolve = geometry.sideDissolve,
        motionUrl = motionArtwork?.url.orEmpty()
    )
    val sideColorField = !stacked &&
        (backgroundMode == PlayerBackgroundMode.Dynamic || backgroundMode == PlayerBackgroundMode.Blur)
    val sideBloom = if (sideColorField) {
        animateColorAsState(
            targetValue = ambience.tint,
            animationSpec = if (animationsEnabled) tween(700, easing = LinearOutSlowInEasing) else snap(),
            label = "player-cinematic-bloom"
        ).value
    } else {
        ambience.tint
    }
    val fullscreenDimAlpha by animateFloatAsState(
        targetValue = playerCinematicFullscreenDimAlpha(fullscreenCanvas),
        animationSpec = if (animationsEnabled) tween(PlayerCinematicFullscreenDimAnimationMs) else snap(),
        label = "player-cinematic-fullscreen-dim"
    )
    val topBandLuminance = livingArtwork?.tones?.firstOrNull()?.luminance() ?: ambience.primary.luminance()
    val topScrimAlpha = playerCinematicTopScrimAlpha(topBandLuminance)

    DisposableEffect(morphAnchors) {
        morphAnchors.updateStageActive(true)
        onDispose { morphAnchors.updateStageActive(false) }
    }

    Box(modifier = modifier) {
        if (sideColorField) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        onDrawBehind {
                            drawSideCinematicBloom(sideBloom, geometry.heroWidth.toPx())
                        }
                    }
            )
        }

        val dissolveModifier = if (stacked) {
            Modifier.playerCinematicBottomDissolve(
                fadeFraction = PlayerCinematicStackedFade,
                sideFadeFraction = playerCinematicResolvedSideFade(geometry.sideDissolve)
            )
        } else {
            Modifier.artworkDissolve(
                edge = ArtworkDissolveEdge.End,
                fadeFraction = PlayerCinematicSideFade,
                sideFadeFraction = playerCinematicResolvedSideFade(geometry.sideDissolve)
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
                .then(dissolveModifier)
        ) {
            MotionArtworkLayer(
                artwork = if (fullscreenCanvas) null else motionArtwork,
                enabled = motionEnabled,
                isPlaying = isPlaying,
                cornerRadius = 0.dp,
                presentation = MotionArtworkPresentation.Cinematic,
                quality = canvasQuality,
                livingArtwork = livingArtwork,
                dynamicBackdropEnabled = backgroundMode == PlayerBackgroundMode.Dynamic,
                onDynamicBackdropPalette = onDynamicBackdropPalette,
                modifier = Modifier.fillMaxSize()
            ) {
                SeamlessArtworkImage(url = artworkUrl, modifier = Modifier.fillMaxSize()) {
                    InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
                }
            }
        }

        if (fullscreenCanvas) {
            MotionArtworkLayer(
                artwork = motionArtwork,
                enabled = true,
                isPlaying = isPlaying,
                cornerRadius = 0.dp,
                presentation = MotionArtworkPresentation.Cinematic,
                quality = canvasQuality,
                livingArtwork = null,
                dynamicBackdropEnabled = backgroundMode == PlayerBackgroundMode.Dynamic,
                onDynamicBackdropPalette = onDynamicBackdropPalette,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = if (morphActive) morphAnchors.stageRevealAlpha() else 1f
                        translationX = swipeOffset() * 0.32f
                    }
            ) { }
        }

        if (fullscreenCanvas || fullscreenDimAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = fullscreenDimAlpha }
                    .background(Color.Black)
            )
        }

        PlayerCinematicChromeScrim(
            alpha = topScrimAlpha,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private fun Modifier.playerCinematicBottomDissolve(
    fadeFraction: Float,
    sideFadeFraction: Float = 0f
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val bottomMask = Brush.verticalGradient(
            colorStops = playerCinematicDissolveStops(fadeFraction),
            startY = 0f,
            endY = size.height
        )
        val sideMask = sideFadeFraction.takeIf { it > 0f }?.let { fraction ->
            val side = fraction.coerceIn(0.01f, 0.5f)
            Brush.horizontalGradient(
                0f to Color.Transparent,
                side to Color.Black,
                1f - side to Color.Black,
                1f to Color.Transparent
            )
        }
        onDrawWithContent {
            drawContent()
            drawRect(brush = bottomMask, blendMode = BlendMode.DstIn)
            if (sideMask != null) {
                drawRect(brush = sideMask, blendMode = BlendMode.DstIn)
            }
        }
    }

@Composable
private fun PlayerCinematicChromeScrim(
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val height = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + PlayerCinematicChromeScrim
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = alpha),
                    0.55f to Color.Black.copy(alpha = alpha * 0.45f),
                    1f to Color.Transparent
                )
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
