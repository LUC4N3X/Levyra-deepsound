package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.ui.PlayerAmbience

@Composable
internal fun PlayerVisualHost(
    backgroundMode: PlayerBackgroundMode,
    artworkUrl: String,
    ambience: PlayerAmbience,
    animationsEnabled: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    backdropFocus: Float = 0.34f
) {
    Box(modifier = modifier) {
        PlayerBackdrop(
            mode = backgroundMode,
            artworkUrl = artworkUrl,
            ambience = ambience,
            isPlaying = isPlaying,
            animationsEnabled = animationsEnabled,
            focusY = backdropFocus,
            modifier = Modifier.fillMaxSize()
        )
    }
}
