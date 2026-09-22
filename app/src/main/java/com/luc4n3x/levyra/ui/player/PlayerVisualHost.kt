package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.ui.PlayerAmbience
import com.luc4n3x.levyra.ui.artwork.LivingArtworkColors
import com.luc4n3x.levyra.ui.theme.LevyraMotion

internal fun playerBackdropArtworkUrl(
    immersive: Boolean,
    backgroundMode: PlayerBackgroundMode,
    artworkUrl: String
): String = if (immersive && backgroundMode != PlayerBackgroundMode.Blur) "" else artworkUrl

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
    motionEnabled: Boolean,
    isPlaying: Boolean,
    canvasQuality: LevyraCanvasQuality,
    morphAnchors: PlayerMorphAnchors,
    morphActive: Boolean,
    swipeOffset: () -> Float,
    cinematicGeometry: PlayerCinematicGeometry,
    modifier: Modifier = Modifier,
    isVideoMode: Boolean = false,
    backdropFocus: Float = 0.34f
) {
    val isImmersive = visualMode.showsCinematicStage() && track != null && !isVideoMode
    val backdropArtworkUrl = playerBackdropArtworkUrl(isImmersive, backgroundMode, artworkUrl)

    Box(modifier = modifier) {
        PlayerBackdrop(
            mode = backgroundMode,
            artworkUrl = backdropArtworkUrl,
            ambience = ambience,
            isPlaying = isPlaying,
            animationsEnabled = animationsEnabled,
            focusY = backdropFocus,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isImmersive,
            enter = EnterTransition.None,
            exit = if (animationsEnabled) fadeOut(LevyraMotion.fade(LevyraMotion.Durations.Medium)) else ExitTransition.None,
            modifier = Modifier.fillMaxSize()
        ) {
            if (track != null) {
                PlayerCinematicStage(
                    track = track,
                    artworkUrl = artworkUrl,
                    motionArtwork = motionArtwork,
                    livingArtwork = livingArtwork,
                    ambience = ambience,
                    backgroundMode = backgroundMode,
                    geometry = cinematicGeometry,
                    motionEnabled = motionEnabled,
                    animationsEnabled = animationsEnabled,
                    isPlaying = isPlaying,
                    canvasQuality = canvasQuality,
                    morphAnchors = morphAnchors,
                    morphActive = morphActive,
                    swipeOffset = swipeOffset,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
