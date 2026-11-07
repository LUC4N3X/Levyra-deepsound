package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings

private val DefaultRecentShape = RoundedCornerShape(22.dp)
private val DefaultSampleShape = RoundedCornerShape(28.dp)
private val DefaultBrowseShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultRecentSearchesRail(
    title: String,
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    menu: @Composable (track: Track, expanded: Boolean, onDismiss: () -> Unit) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val actionsLabel = LocalLevyraStrings.current.actions
    val artShape = MaterialShapes.Cookie9Sided.toShape()
    Column {
        LevyraSectionHeader(title = title, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(
            modifier = Modifier.recentSearchesBleed(RecentSearchGutter),
            contentPadding = PaddingValues(horizontal = RecentSearchGutter),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(tracks, key = { _, track -> "default-recent-${track.id}" }) { _, track ->
                var menuExpanded by remember(track.id) { mutableStateOf(false) }
                Row(
                    Modifier.width(248.dp).height(76.dp).clip(DefaultRecentShape).background(scheme.surfaceContainerHigh)
                        .levyraPressable(
                            onClick = { onTrackClick(track) }, onLongClick = { menuExpanded = true }, onLongClickLabel = actionsLabel,
                            pressedScale = LevyraPressScale.Row, role = Role.Button
                        )
                        .semantics(mergeDescendants = true) { contentDescription = "${track.title}, ${track.artist}" }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(60.dp).clip(artShape)) { CoverImage(track, Modifier.fillMaxSize()) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp).clearAndSetSemantics { }) {
                        Text(track.title, color = scheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    menu(track, menuExpanded) { menuExpanded = false }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultSearchSamplesRail(title: String, samples: List<Track>, previewEnabled: Boolean, onOpen: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column {
        LevyraSectionHeader(title = title, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        LazyRow(
            modifier = Modifier.recentSearchesBleed(RecentSearchGutter),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = RecentSearchGutter)
        ) {
            itemsIndexed(samples, key = { _, track -> "default-sample-${track.id}" }) { index, track ->
                Box(
                    Modifier.size(width = 152.dp, height = 212.dp).clip(DefaultSampleShape).background(scheme.surfaceContainerHighest)
                        .levyraPressable(onClick = { onOpen(index) }, pressedScale = LevyraPressScale.Tile, role = Role.Button)
                        .semantics(mergeDescendants = true) { contentDescription = "${track.title}, ${track.artist}" }
                ) {
                    CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                    if (previewEnabled && index == 0) SpotifySamplePreview(track)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))))
                    Text(
                        track.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 14.dp, bottom = 16.dp).clearAndSetSemantics { }
                    )
                }
            }
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
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(0.72f).padding(start = 16.dp, top = 14.dp).clearAndSetSemantics { }
        )
    }
}
