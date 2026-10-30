@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne

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
import com.luc4n3x.levyra.ui.components.SeekbarThumbShape
import com.luc4n3x.levyra.ui.components.LevyraWavySeekbar
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

internal enum class PlayerProgressStyle {
    Classic,
    Wavy,
    Expressive
}

@Composable
internal fun PlayerProgress(
    positionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    activeColor: Color,
    secondaryColor: Color,
    surfaces: PlayerSurfaceTokens,
    isPlaying: Boolean,
    playbackSpeed: Float,
    animationsEnabled: Boolean,
    motionActive: Boolean,
    compact: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    style: PlayerProgressStyle = PlayerProgressStyle.Classic
) {
    if (PlayerPrismStyle) {
        PrismSeekbar(positionMs, bufferedPositionMs, durationMs, onSeek, modifier, isPlaying)
        return
    }
    val context = LocalContext.current.applicationContext
    val activePlayer by PlaybackService.activePlayerFlow.collectAsStateWithLifecycle()
    val revision by WaveSeekStore.revision.collectAsStateWithLifecycle()
    val mediaId = activePlayer?.currentMediaItem?.mediaId.orEmpty()
    val waveform = remember(context, mediaId, durationMs, compact, revision) {
        WaveSeekStore.load(context, mediaId, durationMs)
            ?.bars(if (compact) 72 else 96)
    }

    val appearance = remember(style, activeColor, secondaryColor, motionActive, waveform, LevyraIsNocturne) {
        playerProgressAppearance(style, activeColor, secondaryColor, motionActive, waveform)
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (appearance.wavy) {
            LevyraWavySeekbar(
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                animated = animationsEnabled && motionActive,
                activeColor = activeColor,
                inactiveColor = LevyraPlayerDesign.TrackInactive,
                onSeek = onSeek
            )
        } else {
            PremiumSeekbar(
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                onSeekTo = { seekMs ->
                    if (durationMs > 0L) {
                        onSeek((seekMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f))
                    }
                },
                activeColor = activeColor,
                trailingColor = appearance.trailing,
                thumbColor = appearance.thumb,
                inactiveColor = LevyraPlayerDesign.TrackInactive,
                animated = animationsEnabled,
                motionActive = appearance.motion,
                waveform = appearance.waveform,
                interactionKey = mediaId,
                thumbShape = appearance.thumbShape
            )
        }
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

private class PlayerProgressAppearance(
    val wavy: Boolean, val trailing: Color, val thumb: Color, val motion: Boolean,
    val waveform: FloatArray?, val thumbShape: SeekbarThumbShape
)

private fun playerProgressAppearance(style: PlayerProgressStyle, active: Color, secondary: Color, motion: Boolean, waveform: FloatArray?): PlayerProgressAppearance {
    val thumb = if (style == PlayerProgressStyle.Expressive) SeekbarThumbShape.Notched else SeekbarThumbShape.Round
    return if (LevyraIsNocturne) PlayerProgressAppearance(false, active, active, false, null, thumb)
    else PlayerProgressAppearance(style == PlayerProgressStyle.Wavy, secondary, Color.White, motion, waveform, thumb)
}
