package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.ui.InstantArtworkPlaceholder
import com.luc4n3x.levyra.ui.MotionArtworkLayer
import com.luc4n3x.levyra.ui.MotionArtworkPresentation
import com.luc4n3x.levyra.ui.PlayerAmbience
import com.luc4n3x.levyra.ui.artwork.LivingArtworkColors
import com.luc4n3x.levyra.ui.playerAmbienceMix

@Composable
internal fun PlayerVisualHost(
    visualMode: PlayerVisualMode,
    backgroundMode: PlayerBackgroundMode,
    track: Track?,
    artworkUrl: String,
    motionArtwork: MotionArtwork?,
    livingArtwork: LivingArtworkColors?,
    ambience: PlayerAmbience,
    animationsEnabled: Boolean,
    isPlaying: Boolean,
    canvasQuality: LevyraCanvasQuality,
    morphActive: Boolean,
    swipeOffset: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isImmersive = visualMode == PlayerVisualMode.CanvasImmersive && track != null
    val backdropArtworkUrl = if (isImmersive) "" else artworkUrl

    Box(modifier = modifier) {
        PlayerBackdrop(
            mode = backgroundMode,
            artworkUrl = backdropArtworkUrl,
            ambience = ambience,
            isPlaying = isPlaying,
            animationsEnabled = animationsEnabled,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isImmersive,
            enter = EnterTransition.None,
            exit = if (animationsEnabled) fadeOut(tween(260, easing = LinearOutSlowInEasing)) else ExitTransition.None,
            modifier = Modifier.fillMaxSize()
        ) {
            if (track != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MotionArtworkLayer(
                        artwork = motionArtwork,
                        enabled = animationsEnabled,
                        isPlaying = isPlaying,
                        cornerRadius = 0.dp,
                        presentation = MotionArtworkPresentation.Immersive,
                        quality = canvasQuality,
                        livingArtwork = livingArtwork,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = if (morphActive) 0f else 1f
                                translationX = swipeOffset * 0.32f
                            }
                    ) {
                        if (artworkUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(LevyraArtworkCache.large(artworkUrl))
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
                        }
                    }
                    PlayerCanvasFusionScrim(
                        ambience = ambience,
                        animationsEnabled = animationsEnabled,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlayerCanvasFusionScrim(
    ambience: PlayerAmbience,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val base = animateColorAsState(
        targetValue = ambience.base,
        animationSpec = if (animationsEnabled) tween(700, easing = LinearOutSlowInEasing) else snap(),
        label = "player-canvas-fusion-base"
    )
    val control = animateColorAsState(
        targetValue = ambience.control,
        animationSpec = if (animationsEnabled) tween(700, easing = LinearOutSlowInEasing) else snap(),
        label = "player-canvas-fusion-control"
    )

    Box(
        modifier = modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind
            val baseColor = base.value
            val controlColor = control.value
            drawRect(
                Brush.verticalGradient(
                    colorStops = arrayOf<Pair<Float, Color>>(
                        0.00f to Color.Black.copy(alpha = 0.50f),
                        0.08f to Color.Black.copy(alpha = 0.25f),
                        0.16f to Color.Transparent,
                        0.48f to Color.Transparent,
                        0.60f to controlColor.copy(alpha = 0.35f),
                        0.72f to controlColor.copy(alpha = 0.72f),
                        0.84f to controlColor.copy(alpha = 0.94f),
                        0.94f to controlColor.playerAmbienceMix(baseColor, 0.45f),
                        1.00f to baseColor
                    )
                )
            )
        }
    )
}
