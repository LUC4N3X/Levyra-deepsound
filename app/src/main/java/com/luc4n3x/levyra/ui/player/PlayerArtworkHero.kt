package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    modifier: Modifier = Modifier
) {
    val artworkShadow by animateDpAsState(
        targetValue = if (isPlaying) 26.dp else 14.dp,
        animationSpec = if (animationsEnabled) tween(420, easing = FastOutSlowInEasing) else snap(),
        label = "player-artwork-hero-shadow"
    )
    val primary = Color(track.accentStart)
    val artworkShape = RoundedCornerShape(cornerRadius)
    val isImmersive = visualMode == PlayerVisualMode.CanvasImmersive

    Box(
        modifier = modifier
            .playerMorphAnchor(morphAnchors, PlayerMorphSlot.Full)
            .graphicsLayer {
                scaleX = artScale
                scaleY = artScale
                translationX = swipeOffset
                translationY = artOffset.toPx()
                alpha = if (morphActive || isImmersive) 0f else 1f
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
                    ambientColor = primary.copy(alpha = 0.40f),
                    spotColor = Color.Black.copy(alpha = 0.75f)
                )
                .clip(artworkShape)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.14f),
                    shape = artworkShape
                )
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
                        color = Color.White.copy(alpha = 0.12f),
                        shape = artworkShape
                    )
            )
        }
    }
}
