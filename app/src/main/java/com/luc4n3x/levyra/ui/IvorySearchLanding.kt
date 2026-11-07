package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText

@Composable
internal fun IvoryRecentSearchesRail(
    title: String,
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    menu: @Composable (track: Track, expanded: Boolean, onDismiss: () -> Unit) -> Unit
) {
    val actionsLabel = LocalLevyraStrings.current.actions
    Column {
        LevyraSectionHeader(title = title, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(
            modifier = Modifier.recentSearchesBleed(RecentSearchGutter),
            contentPadding = PaddingValues(horizontal = RecentSearchGutter),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(tracks, key = { _, track -> "ivory-recent-${track.id}" }) { _, track ->
                var menuExpanded by remember(track.id) { mutableStateOf(false) }
                Row(
                    Modifier.width(244.dp).height(76.dp).clip(RoundedCornerShape(IvoryDesign.surfaceCorner)).background(LevyraPanel)
                        .levyraPressable(
                            onClick = { onTrackClick(track) }, onLongClick = { menuExpanded = true }, onLongClickLabel = actionsLabel,
                            pressedScale = LevyraPressScale.Row, role = Role.Button
                        )
                        .semantics(mergeDescendants = true) { contentDescription = "${track.title}, ${track.artist}" }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(60.dp).clip(IvoryDesign.coverShape)) { CoverImage(track, Modifier.fillMaxSize()) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp).clearAndSetSemantics { }) {
                        Text(track.title, color = LevyraText, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    menu(track, menuExpanded) { menuExpanded = false }
                }
            }
        }
    }
}

@Composable
internal fun IvorySearchSamplesRail(title: String, samples: List<Track>, previewEnabled: Boolean, onOpen: (Int) -> Unit) {
    Column {
        LevyraSectionHeader(title = title, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        LazyRow(
            modifier = Modifier.recentSearchesBleed(RecentSearchGutter),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = RecentSearchGutter)
        ) {
            itemsIndexed(samples, key = { _, track -> "ivory-sample-${track.id}" }) { index, track ->
                Box(
                    Modifier.size(width = 152.dp, height = 212.dp).clip(RoundedCornerShape(IvoryDesign.coverCorner)).clipToBounds().background(LevyraPanel)
                        .levyraPressable(onClick = { onOpen(index) }, pressedScale = LevyraPressScale.Tile, role = Role.Button)
                        .semantics(mergeDescendants = true) { contentDescription = "${track.title}, ${track.artist}" }
                ) {
                    CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                    if (previewEnabled && index == 0) SpotifySamplePreview(track)
                    IvoryInkTab(start = true, Modifier.align(Alignment.BottomStart).padding(bottom = 12.dp).fillMaxWidth(0.88f).height(44.dp)) {
                        Text(
                            track.title, color = IvoryDesign.onInk, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 18.dp).clearAndSetSemantics { }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun IvorySearchBrowseCard(title: String, artworkUrl: String, accent: Pair<Color, Color>, modifier: Modifier, onClick: () -> Unit) {
    var failed by remember(artworkUrl) { mutableStateOf(false) }
    val paper = lerp(IvoryDesign.well, accent.first, 0.20f)
    val halo = lerp(IvoryDesign.well, accent.second, 0.45f)
    Box(
        modifier.clip(RoundedCornerShape(IvoryDesign.surfaceCorner)).background(paper)
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Tile, role = Role.Button)
            .semantics(mergeDescendants = true) { contentDescription = title }
    ) {
        Box(Modifier.align(Alignment.BottomEnd).offset(x = 30.dp, y = 34.dp).size(128.dp).clip(CircleShape).background(halo))
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 14.dp, y = 14.dp).size(86.dp)
                .graphicsLayer { rotationZ = 14f; shadowElevation = 12.dp.toPx(); shape = IvoryDesign.coverShape; clip = true }
                .background(lerp(accent.second, IvoryDesign.ink, 0.35f)).clearAndSetSemantics { }
        ) {
            if (artworkUrl.isNotBlank() && !failed) AsyncImage(artworkUrl, null, contentScale = ContentScale.Crop, onError = { failed = true }, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.align(Alignment.TopStart).fillMaxWidth(0.7f).padding(start = 16.dp, top = 14.dp).clearAndSetSemantics { }) {
            Text(title, color = IvoryDesign.ink, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.size(width = 20.dp, height = 4.dp).clip(CircleShape).background(IvoryDesign.ink))
        }
    }
}
