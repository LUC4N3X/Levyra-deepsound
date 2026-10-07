package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraCardCaption
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

@Composable
internal fun DiscoveryTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    onActions: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val strings = LocalLevyraStrings.current
    val interaction = remember { MutableInteractionSource() }
    val corner = if (LocalAnimationsEnabled.current) {
        levyraExpressiveCorner(
            interactionSource = interaction,
            rest = LevyraCardDesign.EditorialCorner,
            pressed = LevyraCardDesign.ArtworkCorner,
            label = "discoveryArtworkCorner"
        )
    } else {
        LevyraCardDesign.EditorialCorner
    }
    val colors = MaterialTheme.colorScheme
    val scrim = remember { Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))) }
    Column(
        modifier = modifier
            .semantics { selected = isCurrent }
            .levyraPressable(
                onClick = onPlay,
                interactionSource = interaction,
                pressedScale = LevyraPressScale.Tile,
                role = Role.Button,
                onClickLabel = strings.playNow,
                onLongClick = onActions,
                onLongClickLabel = if (onActions != null) strings.songOptions else null
            ),
        verticalArrangement = Arrangement.spacedBy(LevyraCardDesign.CaptionTopGap)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (wide) LevyraHomeDesign.EditorialArtworkRatio else 1f)
                .clip(RoundedCornerShape(corner))
                .background(colors.surfaceContainerHigh)
        ) {
            CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = wide)
            if (wide || isCurrent) {
                Box(Modifier.fillMaxSize().background(scrim))
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(LevyraHomeDesign.HorizontalInset)
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(if (isCurrent) colors.primaryContainer else colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isResolving -> DiscoveryLoadingIndicator(Modifier.size(24.dp), colors.onPrimaryContainer)
                        isCurrent -> LevyraPlayingIndicator(playing = isPlaying, color = colors.onPrimaryContainer)
                        else -> Icon(Icons.Rounded.PlayArrow, null, tint = colors.onSurface)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            LevyraCardCaption(
                title = track.title,
                subtitle = track.artist,
                titleColor = if (isCurrent) colors.primary else colors.onSurface,
                subtitleColor = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
    }
}

@Composable
internal fun DiscoveryLoadingIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    if (LocalAnimationsEnabled.current) {
        LevyraLoadingIndicator(modifier = modifier, color = color)
    } else {
        Box(modifier = modifier.progressSemantics(), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = color)
        }
    }
}

@Composable
internal fun DiscoveryTrackActions(
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.MoreHoriz, strings.songOptions, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(if (isFavorite) strings.removeFromFavorites else strings.addToFavorites) },
                onClick = {
                    expanded = false
                    onFavorite()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.addToPlaylist) },
                onClick = {
                    expanded = false
                    onAddToPlaylist()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.share) },
                onClick = {
                    expanded = false
                    onShare()
                }
            )
        }
    }
}

@Composable
internal fun DiscoveryEditorialCard(
    title: String,
    subtitle: String,
    artwork: Track?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier = modifier
            .clip(LevyraCardDesign.SurfaceShape)
            .background(colors.surfaceContainer)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)
    ) {
        val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.4f
        if (stacked) {
            Column(
                modifier = Modifier.padding(LevyraHomeDesign.EditorialPadding),
                verticalArrangement = Arrangement.spacedBy(LevyraHomeDesign.EditorialPadding)
            ) {
                DiscoveryEditorialText(title, subtitle)
                if (artwork != null) {
                    CoverImage(
                        track = artwork,
                        modifier = Modifier.fillMaxWidth()
                            .aspectRatio(LevyraHomeDesign.EditorialArtworkRatio)
                            .clip(LevyraCardDesign.ArtworkShape)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(LevyraHomeDesign.EditorialPadding),
                horizontalArrangement = Arrangement.spacedBy(LevyraHomeDesign.EditorialPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DiscoveryEditorialText(
                    title = title,
                    subtitle = subtitle,
                    modifier = Modifier.weight(1f).heightIn(min = LevyraHomeDesign.EditorialThumb)
                )
                if (artwork != null) {
                    CoverImage(
                        track = artwork,
                        modifier = Modifier.size(LevyraHomeDesign.EditorialThumb).clip(LevyraCardDesign.ArtworkShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoveryEditorialText(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
        Text(title, style = LevyraType.sectionTitle, color = colors.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = LevyraType.metadata, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
