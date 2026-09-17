@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.ui.WaveformVisualizer
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

private val PulseSignalHeight = 30.dp
private val PulseSpecHeight = 18.dp
private val PulseLiveDot = 6.dp
private const val PulseSignalGain = 3f
private val PulseSpecStyle = TextStyle(fontFeatureSettings = "tnum, case")

@Composable
internal fun PlayerPulseDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    isPlaying: Boolean,
    animated: Boolean,
    compact: Boolean,
    scrollable: Boolean,
    scrollingHeroHeight: Dp,
    gutter: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(
                start = gutter,
                end = gutter,
                top = LevyraPlayerDesign.SpaceXs,
                bottom = if (compact) LevyraPlayerDesign.SpaceMd else LevyraPlayerDesign.SpaceXl
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        slots.header()
        slots.stage(
            track,
            if (scrollable) {
                Modifier
                    .height(scrollingHeroHeight)
                    .padding(vertical = LevyraPlayerDesign.SpaceMd)
            } else {
                Modifier
                    .weight(1f)
                    .padding(
                        start = LevyraPlayerDesign.SpaceXl,
                        end = LevyraPlayerDesign.SpaceXl,
                        top = if (compact) LevyraPlayerDesign.SpaceSm else LevyraPlayerDesign.SpaceLg,
                        bottom = LevyraPlayerDesign.SpaceMd
                    )
            },
            LevyraPlayerDesign.CornerMd
        )
        PulseSignal(
            surfaces = surfaces,
            accent = accent,
            active = isPlaying,
            animated = animated
        )
        PulseSpecLine(
            surfaces = surfaces,
            modifier = Modifier.padding(
                top = LevyraPlayerDesign.SpaceXs,
                bottom = if (compact) LevyraPlayerDesign.SpaceSm else LevyraPlayerDesign.SpaceMd
            )
        )
        slots.controls(this, track)
    }
}

@Composable
private fun PulseSignal(
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    active: Boolean,
    animated: Boolean
) {
    val dotColor by animateColorAsState(
        targetValue = if (active) accent else surfaces.contentFaint,
        animationSpec = LevyraMotion.spec(animated, LevyraMotion.fade()),
        label = "player-pulse-dot"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PulseSignalHeight)
            .clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
    ) {
        Box(
            modifier = Modifier
                .size(PulseLiveDot)
                .background(dotColor, CircleShape)
        )
        WaveformVisualizer(
            color = accent,
            active = active && animated,
            idleColor = surfaces.contentFaint.copy(alpha = 0.4f),
            gain = PulseSignalGain,
            modifier = Modifier
                .weight(1f)
                .height(PulseSignalHeight)
        )
        Box(modifier = Modifier.width(PulseLiveDot))
    }
}

@Composable
private fun PulseSpecLine(
    surfaces: PlayerSurfaceTokens,
    modifier: Modifier = Modifier
) {
    val spec by rememberPlayerAudioSpec()
    val labels = remember(spec) { playerAudioSpecLabels(spec) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PulseSpecHeight),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = LevyraPlayerDesign.SpaceSm)
                        .size(width = LevyraPlayerDesign.Hairline, height = 10.dp)
                        .background(surfaces.contentFaint.copy(alpha = 0.5f))
                )
            }
            Text(
                text = label,
                color = if (index == 0) surfaces.contentMuted else surfaces.contentFaint,
                fontSize = 11.sp,
                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 1.sp,
                style = PulseSpecStyle,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun rememberPlayerAudioSpec(): State<PlayerAudioSpec> {
    val player by PlaybackService.activePlayerFlow.collectAsStateWithLifecycle()
    val spec = remember { mutableStateOf(PlayerAudioSpec()) }
    DisposableEffect(player) {
        val current = player
        if (current == null) {
            spec.value = PlayerAudioSpec()
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    spec.value = playerAudioSpecOf(tracks)
                }
            }
            spec.value = playerAudioSpecOf(current.currentTracks)
            current.addListener(listener)
            onDispose { current.removeListener(listener) }
        }
    }
    return spec
}

private fun playerAudioSpecOf(tracks: Tracks): PlayerAudioSpec {
    for (group in tracks.groups) {
        if (group.type != C.TRACK_TYPE_AUDIO) continue
        for (index in 0 until group.length) {
            if (!group.isTrackSelected(index)) continue
            val format = group.getTrackFormat(index)
            val bitrate = format.averageBitrate.takeIf { it > 0 } ?: format.bitrate.takeIf { it > 0 }
            return PlayerAudioSpec(
                codec = playerAudioCodecLabel(format.sampleMimeType, format.codecs),
                bitrateKbps = bitrate?.div(1_000),
                sampleRateHz = format.sampleRate.takeIf { it > 0 },
                channels = format.channelCount.takeIf { it > 0 }
            )
        }
    }
    return PlayerAudioSpec()
}
