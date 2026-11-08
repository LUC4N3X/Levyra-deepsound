package com.luc4n3x.levyra.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.preferredPlayerArtworkUrl
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.supportCopy
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraSegment
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.PlayerViewModel

@Composable
internal fun PlayerFocus(
    viewModel: PlayerViewModel,
    state: LevyraUiState,
    modifier: Modifier = Modifier,
    videoSurface: (@Composable (Track, Modifier) -> Unit)? = null,
    errorContent: (@Composable () -> Unit)? = null
) {
    val words = supportCopy(LocalLevyraStrings.current.code)
    val exit = { viewModel.setPlayerFocus(false) }
    BackHandler(onBack = exit)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val alpha by animateFloatAsState(if (entered) 1f else 0f, tween(if (state.animationsEnabled) 220 else 0), label = "player-focus-enter")
    Column(
        modifier = modifier.fillMaxSize().background(LevyraBlack).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 24.dp).graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(words.focus, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = LevyraMuted)
            IconButton(onClick = exit) { Icon(Icons.Rounded.FullscreenExit, words.exitFocus, tint = LevyraText) }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val track = state.currentTrack
            if (track != null) {
                if (maxWidth > maxHeight) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        FocusArtwork(track, state.isVideoMode, videoSurface, Modifier.weight(1f).fillMaxSize())
                        FocusControls(viewModel, state, errorContent, Modifier.weight(1f))
                    }
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        FocusArtwork(track, state.isVideoMode, videoSurface, Modifier.weight(1f).fillMaxWidth())
                        FocusControls(viewModel, state, errorContent, Modifier.fillMaxWidth().padding(vertical = 24.dp))
                    }
                }
            } else {
                Text(words.focusSubtitle, color = LevyraMuted, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun FocusArtwork(track: Track, videoMode: Boolean, videoSurface: (@Composable (Track, Modifier) -> Unit)?, modifier: Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val artworkModifier = Modifier.size(minOf(maxWidth, maxHeight, 480.dp)).clip(MaterialTheme.shapes.extraLarge)
        if (videoMode && videoSurface != null) {
            videoSurface(track, artworkModifier)
        } else {
            SeamlessArtworkImage(preferredPlayerArtworkUrl(track), artworkModifier, contentDescription = null) {
                Box(Modifier.fillMaxSize().background(LevyraPanel), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, Modifier.size(64.dp), tint = LevyraMuted)
                }
            }
        }
    }
}

@Composable
private fun FocusControls(viewModel: PlayerViewModel, state: LevyraUiState, errorContent: (@Composable () -> Unit)?, modifier: Modifier) {
    val strings = LocalLevyraStrings.current
    val track = state.currentTrack ?: return
    val surfaces = playerSurfaceTokens(LevyraCyan, state.interfaceSettings.pureBlack)
        .copy(content = LevyraText, contentMuted = LevyraMuted, tonal = LevyraPanel)
    val labels = PlayerControlLabels(strings.shuffle, strings.previous, strings.play, strings.pause, strings.next, strings.repeat)
    Column(modifier.widthIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(track.title, style = MaterialTheme.typography.headlineMedium, color = LevyraText, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text(track.artist, style = MaterialTheme.typography.titleMedium, color = LevyraMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!track.isLiveRadio()) {
            PlayerProgress(
                state.positionMs, state.bufferedPositionMs, state.durationMs, LevyraCyan, LevyraMuted, surfaces,
                state.isPlaying, state.audioSettings.playbackSpeed, state.animationsEnabled,
                motionActive = false, compact = false, onSeek = viewModel::seekTo
            )
        }
        errorContent?.invoke()
        Row(Modifier.fillMaxWidth().height(LevyraPlayerDesign.TransportHeight), horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.TransportGap)) {
            PlayerSegmentButton(LevyraSegment.Single, 1f, surfaces.tonal, LevyraPlayerDesign.TransportInnerCorner, labels.previous, state.animationsEnabled, viewModel::previous) {
                PlayerIcon(Icons.Rounded.SkipPrevious, surfaces.content, Modifier.size(32.dp))
            }
            PlayerPlaySegment(state.isPlaying, state.isResolving, surfaces, LevyraPlayerDesign.TransportHeight, LevyraPlayerDesign.TransportPlayGlyph, state.animationsEnabled, labels, 1.6f, viewModel::togglePlay)
            PlayerSegmentButton(LevyraSegment.Single, 1f, surfaces.tonal, LevyraPlayerDesign.TransportInnerCorner, labels.next, state.animationsEnabled, viewModel::next) {
                PlayerIcon(Icons.Rounded.SkipNext, surfaces.content, Modifier.size(32.dp))
            }
        }
    }
}
