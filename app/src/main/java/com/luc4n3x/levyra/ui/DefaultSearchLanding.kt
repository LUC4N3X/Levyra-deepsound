package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraMediaTitle
import com.luc4n3x.levyra.ui.components.trackContentDescription
import com.luc4n3x.levyra.ui.components.LevyraPastelPeriwinkle
import com.luc4n3x.levyra.ui.components.LevyraPastelRose
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType

private val DefaultBrowseShape = RoundedCornerShape(24.dp)
private val DefaultSectionTop = 22.dp
private val DefaultSampleReelWidth = 148.dp
private val DefaultSampleReelShape = RoundedCornerShape(28.dp)
private val DefaultSampleReelScrim = Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.28f), 0.32f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.86f))

@Composable
internal fun DefaultSearchSamplesRail(title: String, samples: List<Track>, previewEnabled: Boolean, onOpen: (Int) -> Unit) {
    if (samples.isEmpty()) return
    Column(Modifier.padding(top = DefaultSectionTop), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LevyraSectionHeader(title = title, leadingIcon = Icons.Rounded.GraphicEq, leadingSeed = LevyraPastelRose)
        LazyRow(
            modifier = Modifier.recentSearchesBleed(RecentSearchGutter),
            contentPadding = PaddingValues(horizontal = RecentSearchGutter),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(samples, key = { _, track -> "default-sample-${track.id}" }, contentType = { _, _ -> "default-sample-reel" }) { index, track ->
                DefaultSampleReelCard(track, index, previewEnabled && index == 0) { onOpen(index) }
            }
        }
    }
}

@Composable
private fun DefaultSampleReelCard(track: Track, index: Int, preview: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val description = trackContentDescription(track)
    Box(
        Modifier
            .width(DefaultSampleReelWidth)
            .aspectRatio(9f / 16f)
            .clip(DefaultSampleReelShape)
            .background(scheme.surfaceContainerHighest)
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Tile, role = Role.Button)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics { }, highRes = index < 3)
        if (preview) SpotifySamplePreview(track)
        Box(Modifier.matchParentSize().background(DefaultSampleReelScrim))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 12.dp, end = 50.dp, bottom = 14.dp).clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            LevyraMediaTitle(
                track.title, explicit = track.explicit, color = Color.White, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, marqueeEnabled = false
            )
            Text(track.artist, color = Color.White.copy(alpha = 0.74f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(12.dp).size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun DefaultBrowseCard(title: String, artworkUrl: String, accent: Pair<Color, Color>, modifier: Modifier, onClick: () -> Unit) {
    var failed by remember(artworkUrl) { mutableStateOf(false) }
    val (start, end) = accent
    Box(
        modifier.clip(DefaultBrowseShape)
            .background(Brush.linearGradient(listOf(lerp(start, Color.Black, 0.18f), lerp(end, Color.Black, 0.38f))))
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Tile, role = Role.Button)
            .semantics(mergeDescendants = true) { contentDescription = title }
    ) {
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 18.dp, y = 10.dp).size(84.dp)
                .graphicsLayer { rotationZ = 24f; shadowElevation = 10.dp.toPx(); shape = RoundedCornerShape(14.dp); clip = true }
                .background(lerp(end, Color.Black, 0.5f))
                .clearAndSetSemantics { }
        ) {
            if (artworkUrl.isNotBlank() && !failed) AsyncImage(artworkUrl, null, contentScale = ContentScale.Crop, onError = { failed = true }, modifier = Modifier.fillMaxSize())
        }
        Text(
            title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(0.68f).padding(start = 16.dp, top = 14.dp).clearAndSetSemantics { }
        )
    }
}

@Composable
internal fun DefaultRecentSearchesRail(
    title: String,
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    menu: @Composable (track: Track, expanded: Boolean, onDismiss: () -> Unit) -> Unit
) {
    if (tracks.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LevyraSectionHeader(title = title, leadingIcon = Icons.Rounded.History, leadingSeed = LevyraPastelPeriwinkle)
        LazyRow(
            modifier = Modifier.recentSearchesBleed(RecentSearchGutter),
            contentPadding = PaddingValues(horizontal = RecentSearchGutter),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(tracks, key = { _, track -> "default-recent-${track.id}" }, contentType = { _, _ -> "default-recent-search" }) { _, track ->
                DefaultRecentSearchTile(track, onClick = { onTrackClick(track) }, menu = menu)
            }
        }
    }
}

@Composable
private fun DefaultRecentSearchTile(
    track: Track,
    onClick: () -> Unit,
    menu: @Composable (track: Track, expanded: Boolean, onDismiss: () -> Unit) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val actionsLabel = LocalLevyraStrings.current.actions
    val description = trackContentDescription(track)
    var menuExpanded by remember(track.id) { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(interaction, rest = 28.dp, pressed = 14.dp, label = "default-recent-corner")
    Box(Modifier.width(DefaultRecentTileWidth)) {
        Column(
            Modifier
                .fillMaxWidth()
                .levyraPressable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true },
                    onLongClickLabel = actionsLabel,
                    interactionSource = interaction,
                    pressedScale = LevyraPressScale.Tile,
                    role = Role.Button
                )
                .semantics(mergeDescendants = true) { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CoverImage(
                track,
                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(corner)).background(scheme.surfaceContainerHighest).clearAndSetSemantics { }
            )
            Column(Modifier.padding(horizontal = 2.dp).clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                LevyraMediaTitle(
                    track.title, explicit = track.explicit, color = scheme.onSurface, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, marqueeEnabled = false
                )
                Text(track.artist, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        menu(track, menuExpanded) { menuExpanded = false }
    }
}

private val DefaultRecentTileWidth = 132.dp
