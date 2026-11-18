package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraGroupedInnerCorner
import com.luc4n3x.levyra.ui.components.LevyraGroupedOuterCorner
import com.luc4n3x.levyra.ui.components.LevyraPastelAmber
import com.luc4n3x.levyra.ui.components.LevyraPastelLavender
import com.luc4n3x.levyra.ui.components.LevyraPastelPeriwinkle
import com.luc4n3x.levyra.ui.components.LevyraPastelRose
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraUtilitySeed
import com.luc4n3x.levyra.ui.components.levyraGroupedGridShape
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.levyraUtilityColors
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign

private val DefaultLibraryGap = 3.dp
private val DefaultLibraryGroupGap = 20.dp
private val DefaultLibraryTileHeight = 112.dp
private val DefaultLibraryMedallion = 44.dp
private val DefaultLibraryCover = 44.dp
private val DefaultLibraryHeroHeight = 148.dp

private val DefaultLibraryShelfCategories = setOf(
    LibraryCategory.Playlists,
    LibraryCategory.Albums,
    LibraryCategory.Artists,
    LibraryCategory.Songs
)

@Immutable
private data class DefaultLibraryTile(
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val seed: Color,
    val badge: RoundedPolygon,
    val covers: List<String>,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultLibraryOverview(
    favorites: List<Track>,
    recent: List<Track>,
    mostPlayed: List<Track>,
    entries: List<LibrarySectionEntry>,
    onOpenCollection: (String) -> Unit,
    onPlayTracks: (List<Track>) -> Unit,
    onOpenCategory: (LibraryCategory) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val recentCovers = remember(recent) { recent.coverUrls(2) }
    val playedCovers = remember(mostPlayed) { mostPlayed.coverUrls(2) }
    val smart = listOf(
        DefaultLibraryTile(
            title = strings.recent,
            summary = strings.formatTrackCount(recent.size),
            icon = Icons.Rounded.History,
            seed = LevyraPastelPeriwinkle,
            badge = MaterialShapes.Cookie6Sided,
            covers = recentCovers,
            onClick = { onOpenCollection(SMART_COLLECTION_RECENT) }
        ),
        DefaultLibraryTile(
            title = strings.pulsePlays,
            summary = strings.formatTrackCount(mostPlayed.size),
            icon = Icons.Rounded.Replay,
            seed = LevyraPastelAmber,
            badge = MaterialShapes.Sunny,
            covers = playedCovers,
            onClick = { onOpenCollection(SMART_COLLECTION_MOST_PLAYED) }
        )
    )
    val shelf = entries.filter { it.category in DefaultLibraryShelfCategories }
    val storage = entries.filterNot { it.category in DefaultLibraryShelfCategories }
    Column {
        DefaultLibraryFavoritesHero(
            tracks = favorites,
            onOpen = { onOpenCollection(SMART_COLLECTION_FAVORITES) },
            onPlay = { onPlayTracks(favorites) }
        )
        Spacer(Modifier.height(DefaultLibraryGap))
        DefaultLibraryTileGrid(smart, topJoined = true)
        Spacer(Modifier.height(DefaultLibraryGroupGap))
        DefaultLibraryTileGrid(shelf.map { it.toTile(it.category.libraryLabel(strings), onOpenCategory) })
        if (storage.isNotEmpty()) {
            Spacer(Modifier.height(DefaultLibraryGroupGap))
            Column(verticalArrangement = Arrangement.spacedBy(DefaultLibraryGap)) {
                storage.forEachIndexed { index, entry ->
                    DefaultLibraryStorageRow(
                        tile = entry.toTile(entry.category.libraryLabel(strings), onOpenCategory),
                        shape = levyraGroupedListShape(index, storage.size)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LibrarySectionEntry.toTile(title: String, onOpen: (LibraryCategory) -> Unit) = DefaultLibraryTile(
    title = title,
    summary = summary,
    icon = category.sectionIcon(),
    seed = when (category) {
        LibraryCategory.Playlists, LibraryCategory.Collections -> LevyraPastelLavender
        LibraryCategory.Albums -> LevyraPastelPeriwinkle
        LibraryCategory.Artists -> LevyraPastelRose
        LibraryCategory.Offline -> LevyraPastelAmber
        else -> LevyraUtilitySeed
    },
    badge = when (category) {
        LibraryCategory.Playlists -> MaterialShapes.Cookie9Sided
        LibraryCategory.Albums -> MaterialShapes.Circle
        LibraryCategory.Artists -> MaterialShapes.Clover4Leaf
        LibraryCategory.Songs -> MaterialShapes.Sunny
        LibraryCategory.Offline -> MaterialShapes.Cookie4Sided
        LibraryCategory.Device -> MaterialShapes.Gem
        LibraryCategory.Collections, LibraryCategory.Overview -> MaterialShapes.SoftBurst
    },
    covers = emptyList(),
    onClick = { onOpen(category) }
)

private fun List<Track>.coverUrls(limit: Int): List<String> =
    asSequence()
        .map { track -> track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } }
        .filter(String::isNotBlank)
        .distinct()
        .take(limit)
        .toList()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultLibraryFavoritesHero(tracks: List<Track>, onOpen: () -> Unit, onPlay: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val tone = levyraUtilityColors(LevyraUtilitySeed)
    val covers = remember(tracks) { tracks.coverUrls(3) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DefaultLibraryHeroHeight)
            .clip(
                RoundedCornerShape(
                    topStart = LevyraGroupedOuterCorner,
                    topEnd = LevyraGroupedOuterCorner,
                    bottomStart = LevyraGroupedInnerCorner,
                    bottomEnd = LevyraGroupedInnerCorner
                )
            )
            .background(tone.container)
            .semantics(mergeDescendants = true) {}
            .levyraPressable(onClick = onOpen, role = Role.Button, onClickLabel = strings.favoritesPlain, pressedScale = LevyraPressScale.Surface)
            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                strings.favoritesPlain,
                color = tone.onContainer,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                strings.formatTrackCount(tracks.size),
                color = tone.onContainer.copy(alpha = 0.74f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
        LibraryCoverFan(covers = covers, accent = tone.accent, onAccent = tone.onAccent)
        Spacer(Modifier.width(12.dp))
        FilledIconButton(
            onClick = onPlay,
            enabled = tracks.isNotEmpty(),
            shape = MaterialShapes.Cookie4Sided.toShape(),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = tone.accent,
                contentColor = tone.onAccent,
                disabledContainerColor = tone.accent.copy(alpha = 0.38f),
                disabledContentColor = tone.onAccent.copy(alpha = 0.6f)
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "${strings.play}, ${strings.favoritesPlain}", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun DefaultLibraryTileGrid(tiles: List<DefaultLibraryTile>, topJoined: Boolean = false) {
    val rows = tiles.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(DefaultLibraryGap)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DefaultLibraryGap)) {
                row.forEachIndexed { columnIndex, tile ->
                    val shape = if (topJoined) {
                        levyraGroupedGridShape(rowIndex + 1, columnIndex, rows.size + 1, row.size)
                    } else {
                        levyraGroupedGridShape(rowIndex, columnIndex, rows.size, row.size)
                    }
                    DefaultLibraryTileCard(tile, shape, Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultLibraryTileCard(tile: DefaultLibraryTile, shape: Shape, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .heightIn(min = DefaultLibraryTileHeight)
            .clip(shape)
            .background(colors.surfaceContainer)
            .semantics(mergeDescendants = true) {}
            .levyraPressable(onClick = tile.onClick, role = Role.Button, onClickLabel = tile.title, pressedScale = LevyraPressScale.Tile)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        DefaultLibraryVisual(tile)
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tile.title, color = colors.onSurface, style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (tile.summary.isNotBlank()) {
                Text(tile.summary, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DefaultLibraryStorageRow(tile: DefaultLibraryTile, shape: Shape) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(colors.surfaceContainer)
            .semantics(mergeDescendants = true) {}
            .levyraPressable(onClick = tile.onClick, role = Role.Button, onClickLabel = tile.title, pressedScale = LevyraPressScale.Row)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DefaultLibraryVisual(tile)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tile.title, color = colors.onSurface, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (tile.summary.isNotBlank()) {
                Text(tile.summary, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(24.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultLibraryVisual(tile: DefaultLibraryTile) {
    if (tile.covers.isNotEmpty()) {
        val colors = MaterialTheme.colorScheme
        val shape = LevyraCardDesign.ArtworkShape
        Box(Modifier.width(DefaultLibraryCover + 22.dp * (tile.covers.size - 1)).height(DefaultLibraryCover)) {
            tile.covers.forEachIndexed { index, url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .offset(x = 22.dp * index)
                        .size(DefaultLibraryCover)
                        .clip(shape)
                        .background(colors.surfaceContainerHighest)
                        .border(2.dp, colors.surfaceContainer, shape)
                )
            }
        }
        return
    }
    val tone = levyraUtilityColors(tile.seed)
    Box(
        modifier = Modifier.size(DefaultLibraryMedallion).clip(tile.badge.toShape()).background(tone.accent),
        contentAlignment = Alignment.Center
    ) {
        Icon(tile.icon, contentDescription = null, tint = tone.onAccent, modifier = Modifier.size(22.dp))
    }
}
