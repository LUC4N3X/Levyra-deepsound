package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraMediaTitle
import com.luc4n3x.levyra.ui.components.trackContentDescription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText

@Composable
internal fun TorqueRecentSearchesRail(
    title: String,
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    menu: @Composable (track: Track, expanded: Boolean, onDismiss: () -> Unit) -> Unit
) {
    val actionsLabel = LocalLevyraStrings.current.actions
    Column {
        LevyraSectionHeader(title = title, modifier = Modifier.padding(bottom = 6.dp))
        LazyRow(
            modifier = Modifier.recentSearchesBleed(RecentSearchGutter),
            contentPadding = PaddingValues(horizontal = RecentSearchGutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(tracks, key = { _, track -> "torque-recent-${track.id}" }) { index, track ->
                var menuExpanded by remember(track.id) { mutableStateOf(false) }
                val description = trackContentDescription(track)
                Row(
                    Modifier.width(236.dp).height(72.dp).background(LevyraPanel)
                        .levyraPressable(
                            onClick = { onTrackClick(track) }, onLongClick = { menuExpanded = true }, onLongClickLabel = actionsLabel,
                            pressedScale = LevyraPressScale.Row, role = Role.Button
                        )
                        .semantics(mergeDescendants = true) { contentDescription = description },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(72.dp)) { CoverImage(track, Modifier.fillMaxSize()) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp).clearAndSetSemantics { }) {
                        LevyraMediaTitle(track.title, explicit = track.explicit,  color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = LevyraMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        (index + 1).toString().padStart(2, '0'), color = LevyraMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        modifier = Modifier.padding(end = 12.dp).clearAndSetSemantics { }
                    )
                    menu(track, menuExpanded) { menuExpanded = false }
                }
            }
        }
    }
}

@Composable
internal fun TorqueSearchSamplesRail(title: String, samples: List<Track>, previewEnabled: Boolean, onOpen: (Int) -> Unit) {
    Column {
        LevyraSectionHeader(title = title, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        LazyRow(
            modifier = Modifier.recentSearchesBleed(RecentSearchGutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = RecentSearchGutter)
        ) {
            itemsIndexed(samples, key = { _, track -> "torque-sample-${track.id}" }) { index, track ->
                val description = trackContentDescription(track)
                Box(
                    Modifier.size(width = 148.dp, height = 208.dp).clipToBounds().background(LevyraPanel)
                        .levyraPressable(onClick = { onOpen(index) }, pressedScale = LevyraPressScale.Tile, role = Role.Button)
                        .semantics(mergeDescendants = true) { contentDescription = description }
                ) {
                    CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                    if (previewEnabled && index == 0) SpotifySamplePreview(track)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
                    Text(
                        (index + 1).toString().padStart(2, '0'), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopStart).background(Color.Black).padding(horizontal = 8.dp, vertical = 4.dp).clearAndSetSemantics { }
                    )
                    Column(Modifier.align(Alignment.BottomStart).padding(12.dp).clearAndSetSemantics { }) {
                        Box(Modifier.width(22.dp).height(3.dp).background(LevyraActivePalette.blue))
                        LevyraMediaTitle(
                            track.title.uppercase(), explicit = track.explicit,  color = Color.White, fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TorqueBrowseCard(title: String, artworkUrl: String, modifier: Modifier, onClick: () -> Unit) {
    var failed by remember(artworkUrl) { mutableStateOf(false) }
    Box(
        modifier.background(LevyraPanel)
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Tile, role = Role.Button)
            .semantics(mergeDescendants = true) { contentDescription = title }
    ) {
        if (artworkUrl.isNotBlank() && !failed) AsyncImage(
            artworkUrl, null, contentScale = ContentScale.Crop, onError = { failed = true },
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.62f).clearAndSetSemantics { }
        ) else Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.62f).background(LevyraPanelSoft))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0.38f to LevyraPanel, 0.7f to LevyraPanel.copy(alpha = 0.55f), 1f to Color.Transparent)))
        Text(
            title.uppercase(), color = LevyraText, fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.66f).padding(start = 16.dp).clearAndSetSemantics { }
        )
    }
}
