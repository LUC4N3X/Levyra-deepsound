@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.player.waveseek.WaveSeekStore
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
    val context = LocalContext.current.applicationContext
    val activePlayer by PlaybackService.activePlayerFlow.collectAsStateWithLifecycle()
    val revision by WaveSeekStore.revision.collectAsStateWithLifecycle()
    val mediaId = activePlayer?.currentMediaItem?.mediaId.orEmpty()
    val waveform = remember(context, mediaId, durationMs, compact, revision) {
        WaveSeekStore.load(context, mediaId, durationMs)
            ?.bars(if (compact) 72 else 96)
    }

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
            animated = animationsEnabled,
            waveform = waveform,
            interactionKey = mediaId
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
