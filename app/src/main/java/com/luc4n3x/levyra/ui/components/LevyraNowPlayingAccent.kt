package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.harmonizePlayerAccents

const val LevyraNowPlayingAccentDurationMs: Int = 620

@Composable
fun rememberNowPlayingAccent(track: Track?, fallback: Color): Color {
    val animationsEnabled = LocalAnimationsEnabled.current
    val target = remember(track?.id, track?.thumbnailUrl, track?.largeThumbnailUrl, fallback) {
        resolveNowPlayingAccent(track, fallback)
    }
    val accent by animateColorAsState(
        targetValue = target,
        animationSpec = if (animationsEnabled) {
            tween(LevyraNowPlayingAccentDurationMs, easing = LinearOutSlowInEasing)
        } else {
            snap()
        },
        label = "levyra-now-playing-accent"
    )
    return accent
}

private fun resolveNowPlayingAccent(track: Track?, fallback: Color): Color {
    if (track == null) return fallback
    val cached = ArtworkPaletteCache.peek(
        ArtworkPaletteCache.key(
            trackId = track.id,
            thumbnailUrl = track.thumbnailUrl,
            largeThumbnailUrl = track.largeThumbnailUrl
        )
    )
    val start = cached?.start ?: track.accentStart
    val end = cached?.end ?: track.accentEnd
    if (start == 0 && end == 0) return fallback
    return harmonizePlayerAccents(Color(start), Color(end)).primary
}
