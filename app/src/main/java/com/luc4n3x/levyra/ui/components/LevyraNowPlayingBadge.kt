package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled

@Composable
internal fun LevyraNowPlayingBadge(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isResolving: Boolean = false,
    size: Dp = 28.dp
) {
    val colors = MaterialTheme.colorScheme
    val shape = rememberLevyraNowPlayingShape(
        playing = isPlaying && !isResolving,
        animated = LocalAnimationsEnabled.current
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(colors.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) {
            LevyraLoadingIndicator(
                modifier = Modifier.size(size * 0.62f),
                color = colors.onPrimaryContainer
            )
        } else {
            LevyraPlayingIndicator(
                playing = isPlaying,
                color = colors.onPrimaryContainer,
                width = size * 0.46f,
                height = size * 0.40f,
                barWidth = size * 0.09f
            )
        }
    }
}
