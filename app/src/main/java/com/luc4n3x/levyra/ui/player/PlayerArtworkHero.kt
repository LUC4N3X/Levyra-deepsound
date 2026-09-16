package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.ui.InstantArtworkPlaceholder
import com.luc4n3x.levyra.ui.MotionArtworkLayer
import com.luc4n3x.levyra.ui.MotionArtworkPresentation
import com.luc4n3x.levyra.ui.artwork.LivingArtworkColors
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

private const val ArtworkGlowPlaying = 0.42f
private const val ArtworkGlowPaused = 0.18f
private const val ArtworkGlowReach = 0.86f
private const val ArtworkGlowDrop = 0.10f

@Composable
internal fun PlayerArtworkHero(
    track: Track,
    artworkUrl: String,
    visualMode: PlayerVisualMode,
    motionArtwork: MotionArtwork?,
    livingArtwork: LivingArtworkColors?,
    animationsEnabled: Boolean,
    motionEnabled: Boolean,
    isPlaying: Boolean,
    cornerRadius: Dp,
    canvasQuality: LevyraCanvasQuality,
    morphAnchors: PlayerMorphAnchors,
    morphActive: Boolean,
    swipeOffset: Float,
    artScale: Float,
    artOffset: Dp,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    val artworkShadow by animateDpAsState(
        targetValue = if (isPlaying) 28.dp else 12.dp,
        animationSpec = LevyraPlayerDesign.motion(animationsEnabled, LevyraPlayerDesign.emphasizedTween(420)),
        label = "player-artwork-hero-shadow"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPlaying) ArtworkGlowPlaying else ArtworkGlowPaused,
        animationSpec = LevyraPlayerDesign.motion(animationsEnabled, LevyraPlayerDesign.emphasizedTween(520)),
        label = "player-artwork-hero-glow"
    )
    val trackChangeScale = remember { Animatable(1f) }
    var settledTrackId by remember { mutableStateOf(track.id) }
    LaunchedEffect(track.id, animationsEnabled) {
        val trackChanged = settledTrackId != track.id
        settledTrackId = track.id

        trackChangeScale.snapTo(1f)
        if (trackChanged && animationsEnabled) {
            trackChangeScale.snapTo(LevyraPlayerDesign.ArtworkTrackChangeScale)
            trackChangeScale.animateTo(1f, LevyraPlayerDesign.expressiveSpring())
        }
    }
    val artworkShape = RoundedCornerShape(cornerRadius)
    val isImmersive = visualMode == PlayerVisualMode.CanvasImmersive

    Box(
        modifier = modifier
            .playerMorphAnchor(morphAnchors, PlayerMorphSlot.Full)
            .graphicsLayer {
                val scale = artScale * trackChangeScale.value
                scaleX = scale
                scaleY = scale
                translationX = swipeOffset
                translationY = artOffset.toPx()
                alpha = if (morphActive || isImmersive) 0f else 1f
            }
            .drawBehind {
                val radius = size.minDimension * ArtworkGlowReach
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = glowAlpha),
                            glowColor.copy(alpha = glowAlpha * 0.32f),
                            Color.Transparent
                        ),
                        center = center.copy(y = center.y + size.height * ArtworkGlowDrop),
                        radius = radius
                    ),
                    radius = radius,
                    center = center.copy(y = center.y + size.height * ArtworkGlowDrop)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = artworkShadow,
                    shape = artworkShape,
                    clip = false,
                    ambientColor = glowColor.copy(alpha = 0.42f),
                    spotColor = Color.Black.copy(alpha = 0.80f)
                )
                .clip(artworkShape)
                .background(Color.Black.copy(alpha = 0.24f), artworkShape)
        ) {
            when (visualMode) {
                PlayerVisualMode.Artwork -> {
                    SeamlessArtworkImage(url = artworkUrl, modifier = Modifier.fillMaxSize()) {
                        InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
                    }
                }
                PlayerVisualMode.CanvasCard -> {
                    MotionArtworkLayer(
                        artwork = motionArtwork,
                        enabled = motionEnabled,
                        isPlaying = isPlaying,
                        cornerRadius = cornerRadius,
                        presentation = MotionArtworkPresentation.Card,
                        quality = canvasQuality,
                        livingArtwork = livingArtwork,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        SeamlessArtworkImage(url = artworkUrl, modifier = Modifier.fillMaxSize()) {
                            InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                PlayerVisualMode.CanvasImmersive -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = LevyraPlayerDesign.Hairline,
                        color = Color.White.copy(alpha = 0.10f),
                        shape = artworkShape
                    )
            )
        }
    }
}
