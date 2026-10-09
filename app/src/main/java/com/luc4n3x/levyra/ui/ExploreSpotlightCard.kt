package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

@Composable
internal fun ExploreSpotlightCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onOpenReleases: () -> Unit,
    onPlay: () -> Unit,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    val scrim = remember {
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.13f),
            0.38f to Color.Transparent,
            0.70f to Color.Black.copy(alpha = 0.60f),
            1f to Color.Black.copy(alpha = 0.93f)
        )
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val aspectRatio = if (maxWidth > 600.dp) 1.85f else 1.42f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(LevyraCardDesign.SurfaceShape)
                .background(colors.surfaceContainerHigh)
                .levyraPressable(
                    onClick = onOpenReleases,
                    onClickLabel = strings.exploreNewReleases,
                    role = Role.Button,
                    pressedScale = LevyraPressScale.Tile
                )
        ) {
            CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = true)
            Box(Modifier.fillMaxSize().background(scrim))
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                color = Color.Black.copy(alpha = 0.48f),
                shape = CircleShape
            ) {
                Text(
                    text = strings.exploreNewReleases,
                    style = LevyraType.caption,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                )
            }
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                actions()
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = track.title,
                        style = LevyraType.screenTitle,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = LevyraType.metadata,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onPlay,
                    enabled = !isResolving,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer)
                ) {
                    if (isResolving) {
                        DiscoveryLoadingIndicator(
                            modifier = Modifier.size(25.dp),
                            color = colors.onPrimaryContainer
                        )
                    } else if (isCurrent) {
                        LevyraSpotlightPlaybackIcon(isPlaying = isPlaying, onColor = colors.onPrimaryContainer)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = strings.play,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(29.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevyraSpotlightPlaybackIcon(isPlaying: Boolean, onColor: Color) {
    val strings = LocalLevyraStrings.current
    Icon(
        imageVector = if (isPlaying) androidx.compose.material.icons.rounded.Pause else Icons.Rounded.PlayArrow,
        contentDescription = if (isPlaying) strings.pause else strings.play,
        tint = onColor,
        modifier = Modifier.size(29.dp)
    )
}
