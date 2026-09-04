package com.luc4n3x.levyra.ui.ambient

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.LevyraAmbientSettings
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.feature.motion.MotionArtwork

@Immutable
internal data class AmbientUiState(
    val hasTrack: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lyricLine: String = "",
    val motionArtwork: MotionArtwork? = null,
    val animationsEnabled: Boolean = true,
    val canvasQuality: LevyraCanvasQuality = LevyraCanvasQuality.Auto,
    val settings: LevyraAmbientSettings = LevyraAmbientSettings()
)
