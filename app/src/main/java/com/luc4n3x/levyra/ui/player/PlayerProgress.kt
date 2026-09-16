package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.components.PremiumSeekbar
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

@Composable
internal fun PlayerProgress(
    positionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    activeColor: Color,
    secondaryColor: Color,
    surfaces: PlayerSurfaceTokens,
    isPlaying: Boolean,
    animationsEnabled: Boolean,
    compact: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PremiumSeekbar(
            positionMs = positionMs,
            durationMs = durationMs,
            bufferedPositionMs = bufferedPositionMs,
            isPlaying = isPlaying,
            onSeekTo = { seekMs ->
                if (durationMs > 0L) {
                    onSeek((seekMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f))
                }
            },
            activeColor = activeColor,
            trailingColor = secondaryColor,
            inactiveColor = LevyraPlayerDesign.TrackInactive,
            animated = animationsEnabled
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -LevyraPlayerDesign.SpaceXs)
                .padding(horizontal = LevyraPlayerDesign.SpaceXxs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerTimeLabel(formatSeekbarMillis(positionMs), surfaces.contentMuted, compact)
            PlayerTimeLabel(
                if (durationMs > 0L) formatSeekbarMillis(durationMs) else "--:--",
                surfaces.contentFaint,
                compact
            )
        }
    }
}

private val TabularTimeStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun PlayerTimeLabel(text: String, color: Color, compact: Boolean) {
    Text(
        text = text,
        color = color,
        fontSize = if (compact) 11.sp else 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp,
        style = TabularTimeStyle
    )
}
