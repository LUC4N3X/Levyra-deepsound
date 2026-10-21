package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.ui.theme.*
import androidx.compose.material3.HorizontalDivider
import com.luc4n3x.levyra.ui.theme.Nocturne78Icons

import androidx.compose.foundation.shape.RoundedCornerShape
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.Nocturne78Design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.graphics.shapes.RoundedPolygon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraGroupedInnerCorner
import com.luc4n3x.levyra.ui.components.LevyraGroupedOuterCorner
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraGroupedGridShape
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

@Immutable
internal data class LibrarySectionEntry(val category: LibraryCategory, val summary: String)

@Immutable
private data class LibrarySectionTone(val container: Color, val content: Color)

private val LibrarySectionGap = 3.dp
private val LibrarySectionBadge = 44.dp
private val LibraryFanCoverSize = 66.dp
private val LibraryFanStep = 16.dp
private val LibraryIvoryTileHeight = 132.dp

@Composable
internal fun LibrarySectionGrid(
    entries: List<LibrarySectionEntry>,
    onOpen: (LibraryCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    if (LevyraIsSpotify) {
        Column(modifier) {
            entries.forEach { entry -> SpotifyLibraryRow(entry, onOpen) }
        }
        return
    }
    if (LevyraIsIvory) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            entries.chunked(2).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { columnIndex, entry ->
                        IvoryLibraryTile(entry, rowIndex == 0 && columnIndex == 0, onOpen, Modifier.weight(1f))
                    }
                }
            }
        }
        return
    }
    if (LevyraIsPrism) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(if (LevyraIsTorque) 2.dp else 8.dp)) {
            entries.forEach { entry ->
                LibrarySectionTile(entry, LibrarySectionTone(colors.surfaceContainer, colors.primary), RoundedCornerShape(PrismDesign.controlCorner), onOpen, Modifier.fillMaxWidth())
            }
        }
        return
    }
    if (LevyraIsNocturne) {
        Column(modifier) {
            entries.forEach { entry ->
                LibrarySectionTile(entry, LibrarySectionTone(colors.surfaceContainerHigh, colors.onSurfaceVariant), RoundedCornerShape(0.dp), onOpen)
                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
            }
        }
        return
    }
    val rows = entries.chunked(2)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LibrarySectionGap)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LibrarySectionGap)) {
                row.forEachIndexed { columnIndex, entry ->
                    LibrarySectionTile(
                        entry = entry,
                        tone = librarySectionTone(colors, rowIndex, columnIndex),
                        shape = levyraGroupedGridShape(rowIndex, columnIndex, rows.size, row.size),
                        onOpen = onOpen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun IvoryLibraryTile(entry: LibrarySectionEntry, featured: Boolean, onOpen: (LibraryCategory) -> Unit, modifier: Modifier) {
    val strings = LocalLevyraStrings.current
    val label = entry.category.libraryLabel(strings)
    val surface = if (featured) IvoryDesign.ink else LevyraActivePalette.panel
    val content = if (featured) IvoryDesign.onInk else LevyraText
    val button = if (featured) IvoryDesign.onInk else IvoryDesign.ink
    val glyph = if (featured) IvoryDesign.ink else IvoryDesign.onInk
    val inset = LibraryIvoryTileHeight * IvoryPocketInset
    val badge = LibraryIvoryTileHeight * 0.21f
    Box(modifier.height(LibraryIvoryTileHeight)) {
        Column(
            Modifier.fillMaxSize().clip(IvoryPocketShape).background(surface)
                .levyraPressable(onClick = { onOpen(entry.category) }, onClickLabel = label, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                .padding(start = 18.dp, end = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(label, color = content, style = MaterialTheme.typography.titleMedium, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (entry.summary.isNotBlank()) Text(entry.summary, color = content.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = inset - badge, end = inset - badge).size(badge * 2).clip(CircleShape).background(button),
            contentAlignment = Alignment.Center
        ) {
            Icon(entry.category.sectionIcon(), null, tint = glyph, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SpotifyLibraryRow(entry: LibrarySectionEntry, onOpen: (LibraryCategory) -> Unit) {
    val strings = LocalLevyraStrings.current
    val label = entry.category.libraryLabel(strings)
    val round = entry.category == LibraryCategory.Artists
    val shape = if (round) CircleShape else RoundedCornerShape(SpotifyDesign.cornerSmall)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .levyraPressable(onClick = { onOpen(entry.category) }, onClickLabel = label, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(SpotifyDesign.libraryCover).clip(shape).background(entry.category.spotifyTileBrush()), contentAlignment = Alignment.Center) {
            Icon(
                if (entry.category == LibraryCategory.Songs) Icons.Rounded.Favorite else entry.category.sectionIcon(), null,
                tint = if (entry.category.spotifyTileBrush() == SpotifyNeutralTile) SpotifyDesign.subdued else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = SpotifyDesign.text, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (entry.summary.isNotBlank()) Text(entry.summary, color = SpotifyDesign.subdued, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val SpotifyNeutralTile = SolidColor(SpotifyDesign.highlight)
private val SpotifyLikedTile = Brush.linearGradient(listOf(Color(0xFF450AF5), Color(0xFF5B4FC8), Color(0xFF3E7F6E)))
private val SpotifyOfflineTile = SolidColor(Color(0xFF006450))
private val SpotifyCollectionsTile = Brush.linearGradient(listOf(Color(0xFF5038A0), Color(0xFF1E3264)))

private fun LibraryCategory.spotifyTileBrush(): Brush = when (this) {
    LibraryCategory.Songs -> SpotifyLikedTile
    LibraryCategory.Offline -> SpotifyOfflineTile
    LibraryCategory.Collections -> SpotifyCollectionsTile
    else -> SpotifyNeutralTile
}

private fun librarySectionTone(colors: ColorScheme, row: Int, column: Int): LibrarySectionTone =
    if ((row + column) % 2 == 0) {
        LibrarySectionTone(colors.primaryContainer, colors.onPrimaryContainer)
    } else {
        LibrarySectionTone(colors.secondaryContainer, colors.onSecondaryContainer)
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LibraryCategory.sectionBadgePolygon(): RoundedPolygon = when (this) {
    LibraryCategory.Playlists -> MaterialShapes.Cookie9Sided
    LibraryCategory.Songs -> MaterialShapes.Sunny
    LibraryCategory.Albums -> MaterialShapes.Circle
    LibraryCategory.Artists -> MaterialShapes.Clover4Leaf
    LibraryCategory.Offline -> MaterialShapes.Cookie4Sided
    LibraryCategory.Device -> MaterialShapes.Gem
    LibraryCategory.Collections, LibraryCategory.Overview -> MaterialShapes.SoftBurst
}

private fun LibraryCategory.sectionIcon(): ImageVector = when (this) {
    LibraryCategory.Playlists -> Icons.AutoMirrored.Rounded.QueueMusic
    LibraryCategory.Songs -> Icons.Rounded.MusicNote
    LibraryCategory.Albums -> Icons.Rounded.Album
    LibraryCategory.Artists -> Icons.Rounded.Mic
    LibraryCategory.Offline -> Icons.Rounded.DownloadDone
    LibraryCategory.Device -> Icons.Rounded.PhoneAndroid
    LibraryCategory.Collections, LibraryCategory.Overview -> Icons.Rounded.CollectionsBookmark
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibrarySectionTile(
    entry: LibrarySectionEntry,
    tone: LibrarySectionTone,
    shape: Shape,
    onOpen: (LibraryCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val strings = LocalLevyraStrings.current
    val label = entry.category.libraryLabel(strings)
    val appearance = librarySectionAppearance(entry, tone, shape)
    if (LevyraIsPrism) {
        Row(
            modifier.heightIn(min = PrismDesign.rowHeight + 8.dp).clip(shape).background(LevyraPanel)
                .levyraPressable(onClick = { onOpen(entry.category) }, onClickLabel = label, role = Role.Button)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(Modifier.size(PrismDesign.iconTile).clip(PrismDesign.cut(PrismDesign.controlCorner)).background(if (LevyraIsTorque) Color.Transparent else PrismAccent.Tint), contentAlignment = Alignment.Center) {
                Icon(appearance.icon, null, tint = if (LevyraIsTorque) LevyraText else LevyraCyan, modifier = Modifier.size(if (LevyraIsTorque) 26.dp else 22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = LevyraText, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (entry.summary.isNotBlank()) Text(entry.summary, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        return
    }
    Row(
        modifier = modifier
            .heightIn(min = appearance.height)
            .clip(appearance.shape)
            .background(appearance.background)
            .semantics(mergeDescendants = true) {}
            .levyraPressable(
                onClick = { onOpen(entry.category) },
                role = Role.Button,
                onClickLabel = label,
                pressedScale = LevyraPressScale.Tile
            )
            .padding(horizontal = 12.dp, vertical = appearance.padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(LibrarySectionBadge).clip(appearance.badgeShape).background(appearance.badgeBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(appearance.icon, contentDescription = null, tint = appearance.content, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = colors.onSurface,
                style = LevyraType.cardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.summary.isNotBlank()) {
                Text(
                    text = entry.summary,
                    color = colors.onSurfaceVariant,
                    style = LevyraType.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun LibraryFavoritesSpotlight(
    tracks: List<Track>,
    shape: Shape,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val strings = LocalLevyraStrings.current
    val covers = remember(tracks) {
        tracks.asSequence()
            .map { track -> track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } }
            .filter(String::isNotBlank)
            .distinct()
            .take(3)
            .toList()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(colors.primary.copy(alpha = 0.16f), colors.surfaceContainerHigh.copy(alpha = 0.62f))
                )
            )
            .semantics(mergeDescendants = true) {}
            .levyraPressable(
                onClick = onOpen,
                role = Role.Button,
                onClickLabel = strings.favoritesPlain,
                pressedScale = LevyraPressScale.Surface
            )
            .padding(start = 18.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = strings.favoritesPlain,
                color = colors.onSurface,
                style = LevyraType.sectionTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = strings.formatTrackCount(tracks.size),
                color = colors.onSurfaceVariant,
                style = LevyraType.caption,
                maxLines = 1
            )
        }
        LibraryCoverFan(covers = covers)
        Spacer(Modifier.width(10.dp))
        FilledIconButton(
            onClick = onPlay,
            enabled = tracks.isNotEmpty(),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            ),
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "${strings.play}, ${strings.favoritesPlain}",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryCoverFan(covers: List<String>) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.width(LibraryFanCoverSize + LibraryFanStep * 2).height(96.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (covers.isEmpty()) {
            val heartShape = MaterialShapes.Cookie9Sided.toShape()
            Box(
                modifier = Modifier
                    .offset(x = LibraryFanStep)
                    .size(LibraryFanCoverSize)
                    .clip(heartShape)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(30.dp))
            }
        } else {
            val shape = LevyraCardDesign.ArtworkShape
            covers.forEachIndexed { index, url ->
                val tilt = (index - (covers.size - 1) / 2f) * 9f
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .offset(x = LibraryFanStep * (index + (3 - covers.size) / 2f))
                        .graphicsLayer { rotationZ = tilt }
                        .size(LibraryFanCoverSize)
                        .clip(shape)
                        .background(colors.surfaceContainerHighest)
                        .border(2.dp, colors.surface, shape)
                )
            }
        }
    }
}

internal val LibrarySpotlightShape: Shape = RoundedCornerShape(
    topStart = LevyraGroupedOuterCorner,
    topEnd = LevyraGroupedOuterCorner,
    bottomStart = LevyraGroupedInnerCorner,
    bottomEnd = LevyraGroupedInnerCorner
)

internal fun libraryShortcutShape(column: Int): Shape = levyraGroupedGridShape(1, column, 2, 2)

internal class LibraryOverviewSearchActions(
    val onShowAll: (LibraryCategory) -> Unit,
    val onOpenPlaylist: (Playlist) -> Unit,
    val onPlayPlaylist: (Playlist) -> Unit,
    val onOpenAlbum: (LibraryAlbum) -> Unit,
    val onPlayAlbum: (LibraryAlbum) -> Unit,
    val onOpenArtist: (LibraryArtist) -> Unit,
    val onPlayArtist: (LibraryArtist) -> Unit,
    val trackRow: @Composable (Track, Modifier) -> Unit
)

private const val OverviewSearchTrackLimit = 5
private const val OverviewSearchCollectionLimit = 3

internal fun LazyListScope.libraryOverviewSearch(
    tracks: List<Track>,
    playlists: List<Playlist>,
    albums: List<LibraryAlbum>,
    artists: List<LibraryArtist>,
    strings: LevyraStrings,
    actions: LibraryOverviewSearchActions
) {
    if (tracks.isEmpty() && playlists.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
        item(key = "overview-search-empty") { LibraryEmpty(Icons.Rounded.Search, strings.emptySearchPrompt) }
        return
    }
    if (tracks.isNotEmpty()) {
        item(key = "overview-search-songs-title") {
            LibrarySearchSectionTitle(strings.songsPlain, tracks.size, LibraryCategory.Songs, OverviewSearchTrackLimit, actions)
        }
        items(tracks.take(OverviewSearchTrackLimit), key = { "overview-search-song-${libraryTrackKey(it)}" }) { track ->
            actions.trackRow(track, Modifier.animateItem())
        }
    }
    if (playlists.isNotEmpty()) {
        item(key = "overview-search-playlists-title") {
            LibrarySearchSectionTitle(strings.playlists, playlists.size, LibraryCategory.Playlists, OverviewSearchCollectionLimit, actions)
        }
        items(playlists.take(OverviewSearchCollectionLimit), key = { "overview-search-playlist-${it.id}" }) { playlist ->
            LibraryPlaylistRow(
                playlist = playlist,
                selected = false,
                selectionActive = false,
                onClick = { actions.onOpenPlaylist(playlist) },
                onLongClick = { actions.onOpenPlaylist(playlist) },
                onPlay = { actions.onPlayPlaylist(playlist) }
            )
        }
    }
    if (albums.isNotEmpty()) {
        item(key = "overview-search-albums-title") {
            LibrarySearchSectionTitle(strings.albumsPlain, albums.size, LibraryCategory.Albums, OverviewSearchCollectionLimit, actions)
        }
        items(albums.take(OverviewSearchCollectionLimit), key = { "overview-search-album-${it.key}" }) { album ->
            LibraryAlbumRow(
                album = album,
                selected = false,
                selectionActive = false,
                onClick = { actions.onOpenAlbum(album) },
                onLongClick = { actions.onOpenAlbum(album) },
                onPlay = { actions.onPlayAlbum(album) }
            )
        }
    }
    if (artists.isNotEmpty()) {
        item(key = "overview-search-artists-title") {
            LibrarySearchSectionTitle(strings.artists, artists.size, LibraryCategory.Artists, OverviewSearchCollectionLimit, actions)
        }
        items(artists.take(OverviewSearchCollectionLimit), key = { "overview-search-artist-${it.key}" }) { artist ->
            LibraryArtistRow(
                artist = artist,
                selected = false,
                selectionActive = false,
                onClick = { actions.onOpenArtist(artist) },
                onLongClick = { actions.onOpenArtist(artist) },
                onPlay = { actions.onPlayArtist(artist) }
            )
        }
    }
}

@Composable
private fun LibrarySearchSectionTitle(
    title: String,
    count: Int,
    category: LibraryCategory,
    limit: Int,
    actions: LibraryOverviewSearchActions
) {
    val strings = LocalLevyraStrings.current
    val canExpand = count > limit
    LibrarySectionTitle(
        title = title,
        detail = count.toString(),
        action = if (canExpand) strings.showAll else null,
        onAction = { actions.onShowAll(category) }.takeIf { canExpand }
    )
}

private data class LibrarySectionAppearance(
    val height: androidx.compose.ui.unit.Dp,
    val padding: androidx.compose.ui.unit.Dp,
    val shape: Shape,
    val badgeShape: Shape,
    val background: Color,
    val badgeBackground: Color,
    val content: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun librarySectionAppearance(entry: LibrarySectionEntry, tone: LibrarySectionTone, shape: Shape): LibrarySectionAppearance {
    val colors = MaterialTheme.colorScheme
    val icon = entry.category.sectionIcon()
    return if (LevyraIsPrism) LibrarySectionAppearance(
        104.dp, 14.dp, shape, RoundedCornerShape(14.dp),
        colors.surfaceContainer, colors.primaryContainer, colors.onPrimaryContainer, icon
    ) else if (LevyraIsNocturne) LibrarySectionAppearance(
        64.dp, 10.dp, RoundedCornerShape(0.dp), RoundedCornerShape(Nocturne78Design.artworkCorner),
        Color.Transparent, colors.surfaceContainerHigh, colors.onSurfaceVariant, Nocturne78Icons.resolve(icon)
    ) else LibrarySectionAppearance(
        72.dp, 14.dp, shape, entry.category.sectionBadgePolygon().toShape(),
        colors.surfaceContainerHigh.copy(alpha = 0.62f), tone.container, tone.content, icon
    )
}
