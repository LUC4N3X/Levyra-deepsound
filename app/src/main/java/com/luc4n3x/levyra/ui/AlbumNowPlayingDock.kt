package com.luc4n3x.levyra.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.ui.components.LevyraDockProgress
import com.luc4n3x.levyra.ui.components.LevyraRowText
import com.luc4n3x.levyra.ui.components.levyraDockSurface
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify

@Composable
internal fun AlbumNowPlayingDock(
    track: Track,
    isPlaying: Boolean,
    isResolving: Boolean,
    progress: Float,
    animated: Boolean,
    onToggle: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (LevyraIsSpotify) {
        SpotifyNowPlayingBar(track, isPlaying, isResolving, progress, onToggle, onOpenPlayer, modifier)
        return
    }
    if (YouTubeMusicDetailDock(track, isPlaying, isResolving, progress, onToggle, onOpenPlayer, modifier)) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .levyraDockSurface()
            .clickable(onClick = onOpenPlayer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(LevyraCardDesign.ThumbShape)
            ) {
                CoverImage(track, Modifier.fillMaxSize())
            }
            LevyraRowText(
                title = track.title,
                subtitle = track.artist,
                modifier = Modifier.weight(1f)
            )
            MiniPlayerToggleButton(
                isPlaying = isPlaying,
                isResolving = isResolving,
                liveRadio = track.isLiveRadio(),
                progress = { progress },
                accent = LevyraCyan,
                animated = animated,
                onToggle = onToggle
            )
        }
        LevyraDockProgress(progress = progress)
    }
}
