package com.luc4n3x.levyra.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.ColorScheme
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.SavedPlaylist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.playableTrackCount
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.LevyraGroupedOuterCorner
import com.luc4n3x.levyra.ui.components.LevyraPastelPeriwinkle
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.levyraGroupedGridShape
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraType
import com.materialkolor.blend.Blend
import com.materialkolor.hct.Hct

private const val LibraryShelfLimit = 6
private val LibraryOverviewGutter = 16.dp
private val NoArtworkPalette = ArtworkPalette(0, 0)

@Composable
internal fun Modifier.defaultLibraryTopTint(accent: Color, background: Color): Modifier {
    val blue = rememberLibraryBlue(accent)
    val dark = remember(background) { Hct.fromInt(background.toArgb()).tone < 50.0 }
    return drawWithCache {
        val height = minOf(size.height, 148.dp.toPx())
        val tint = Brush.verticalGradient(
            colors = listOf(blue.copy(alpha = if (dark) 0.045f else 0.07f), blue.copy(alpha = 0f)),
            endY = height
        )
        onDrawBehind { drawRect(tint, size = Size(size.width, height)) }
    }
}

@Composable
private fun rememberLibraryBlue(accent: Color): Color =
    remember(accent) { Color(Blend.harmonize(LevyraPastelPeriwinkle.toArgb(), accent.toArgb())) }

@Composable
private fun rememberLibrarySurface(base: Color, blue: Color): Color = remember(base, blue) {
    val tone = Hct.fromInt(base.toArgb()).tone
    val hue = Hct.fromInt(blue.toArgb()).hue
    Color(Hct.from(hue, if (tone < 50.0) 3.5 else 2.0, tone).toInt())
}

@Composable
private fun librarySurface(): Color {
    val scheme = MaterialTheme.colorScheme
    return rememberLibrarySurface(scheme.surfaceContainer, rememberLibraryBlue(scheme.primary))
}

@Immutable
private class LibraryShelfItem(
    key: String,
    title: String,
    subtitle: String,
    artwork: String,
    onOpen: () -> Unit
) {
    val key = key
    val title = title
    val subtitle = subtitle
    val artwork = artwork
    val onOpen = onOpen
}

internal fun LazyListScope.defaultLibraryOverview(
    favorites: List<Track>,
    recent: List<Track>,
    mostPlayed: List<Track>,
    entries: List<LibrarySectionEntry>,
    playlists: List<Playlist>,
    savedPlaylists: List<SavedPlaylist>,
    albums: List<LibraryAlbum>,
    strings: LevyraStrings,
    animationsEnabled: Boolean,
    onOpenCollection: (String) -> Unit,
    onPlayFavorites: () -> Unit,
    onPlayRecent: (Track) -> Unit,
    onOpenCategory: (LibraryCategory) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenSavedPlaylist: (SavedPlaylist) -> Unit,
    onOpenAlbum: (LibraryAlbum) -> Unit
) {
    item(key = "default-library-overview") {
        DefaultLibraryFavorites(
            tracks = favorites,
            animated = animationsEnabled,
            onOpen = { onOpenCollection(SMART_COLLECTION_FAVORITES) },
            onPlay = onPlayFavorites
        )
    }
    item(key = "default-library-listening") {
        DefaultLibraryListening(recent, mostPlayed, animationsEnabled, onOpenCollection)
    }
    if (recent.isNotEmpty()) {
        item(key = "default-library-recent") {
            val shelf = remember(recent, onPlayRecent) {
                recent.take(LibraryShelfLimit).map { track ->
                    LibraryShelfItem(
                        key = libraryTrackKey(track),
                        title = track.title,
                        subtitle = track.artist,
                        artwork = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl },
                        onOpen = { onPlayRecent(track) }
                    )
                }
            }
            DefaultLibraryShelf(strings.listeningHistory, shelf, LibraryCategory.Songs) {
                onOpenCollection(SMART_COLLECTION_RECENT)
            }
        }
    }
    val hasPlaylists = playlists.isNotEmpty() || savedPlaylists.isNotEmpty()
    if (hasPlaylists) {
        item(key = "default-library-playlists") {
            val shelf = remember(playlists, savedPlaylists, strings, onOpenPlaylist, onOpenSavedPlaylist) {
                buildList {
                    playlists.take(LibraryShelfLimit).forEach { playlist ->
                        add(
                            LibraryShelfItem(
                                key = "local:${playlist.id}",
                                title = playlist.name,
                                subtitle = strings.formatTrackCount(playlist.playableTrackCount),
                                artwork = playlist.coverUrl,
                                onOpen = { onOpenPlaylist(playlist) }
                            )
                        )
                    }
                    savedPlaylists.take(LibraryShelfLimit - size).forEach { saved ->
                        add(
                            LibraryShelfItem(
                                key = "saved:${saved.key}",
                                title = saved.hit.title,
                                subtitle = saved.hit.author.ifBlank { saved.hit.trackCountLabel },
                                artwork = saved.hit.thumbnailUrl,
                                onOpen = { onOpenSavedPlaylist(saved) }
                            )
                        )
                    }
                }
            }
            DefaultLibraryShelf(strings.playlists, shelf, LibraryCategory.Playlists) {
                onOpenCategory(LibraryCategory.Playlists)
            }
        }
    }
    if (albums.isNotEmpty()) {
        item(key = "default-library-albums") {
            val shelf = remember(albums, onOpenAlbum) {
                albums.take(LibraryShelfLimit).map { album ->
                    LibraryShelfItem(album.key, album.title, album.artist, album.artworkUrl) { onOpenAlbum(album) }
                }
            }
            DefaultLibraryShelf(LibraryCategory.Albums.libraryLabel(strings), shelf, LibraryCategory.Albums) {
                onOpenCategory(LibraryCategory.Albums)
            }
        }
    }
    item(key = "default-library-categories") {
        val links = remember(entries, hasPlaylists, albums.isNotEmpty()) {
            entries.filterNot {
                (it.category == LibraryCategory.Playlists && hasPlaylists) ||
                    (it.category == LibraryCategory.Albums && albums.isNotEmpty())
            }
        }
        DefaultLibraryCategoryLinks(links, onOpenCategory)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultLibraryFavorites(tracks: List<Track>, animated: Boolean, onOpen: () -> Unit, onPlay: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val scheme = MaterialTheme.colorScheme
    val artwork = remember(tracks) { tracks.firstCover() }
    val container = rememberLibraryContainer(artwork, scheme.primaryContainer, animated)
    val hasTracks = tracks.isNotEmpty()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LevyraCardDesign.SurfaceShape)
            .drawBehind { drawRect(container.value) }
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface)
            .padding(16.dp)
    ) {
        val coverSize = if (hasTracks) (maxWidth * 0.42f).coerceIn(72.dp, 144.dp) else 52.dp
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (artwork.isNotBlank()) {
                val coverShape = RoundedCornerShape(
                    topStart = LevyraGroupedOuterCorner,
                    topEnd = LevyraCardDesign.ArtworkCorner,
                    bottomEnd = LevyraGroupedOuterCorner,
                    bottomStart = LevyraCardDesign.ArtworkCorner
                )
                LibraryArtwork(artwork, strings.favoritesPlain, Modifier.size(coverSize), coverShape, false)
            } else {
                Box(
                    modifier = Modifier.size(coverSize).clip(MaterialShapes.Cookie9Sided.toShape())
                        .drawBehind { drawRect(scheme.primary.copy(alpha = 0.08f).compositeOver(container.value)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    strings.favoritesPlain,
                    color = scheme.onSurface,
                    style = if (hasTracks) MaterialTheme.typography.headlineSmallEmphasized else MaterialTheme.typography.titleLargeEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (hasTracks) strings.formatTrackCount(tracks.size) else strings.tapHeartToAdd,
                    color = scheme.libraryTonalDetail,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (hasTracks) DefaultLibraryPlayFavorites(onPlay, enabled = true)
            }
            if (!hasTracks) DefaultLibraryPlayFavorites(onPlay, enabled = false)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultLibraryPlayFavorites(onPlay: () -> Unit, enabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalLevyraStrings.current
    FilledIconButton(
        onClick = onPlay,
        enabled = enabled,
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary),
        modifier = Modifier.size(48.dp)
    ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = "${strings.play}, ${strings.favoritesPlain}")
    }
}

@Composable
private fun DefaultLibraryListening(
    recent: List<Track>,
    mostPlayed: List<Track>,
    animated: Boolean,
    onOpenCollection: (String) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val scheme = MaterialTheme.colorScheme
    val recentTile: @Composable (Modifier, Boolean) -> Unit = { modifier, stacked ->
        DefaultLibraryListeningTile(
            strings.recent, recent, Icons.Rounded.History, scheme.secondaryContainer,
            animated, index = 0, stacked = stacked, modifier = modifier
        ) { onOpenCollection(SMART_COLLECTION_RECENT) }
    }
    val playedTile: @Composable (Modifier, Boolean) -> Unit = { modifier, stacked ->
        DefaultLibraryListeningTile(
            strings.pulsePlays, mostPlayed, Icons.Rounded.Replay, scheme.tertiaryContainer,
            animated, index = 1, stacked = stacked, modifier = modifier
        ) { onOpenCollection(SMART_COLLECTION_MOST_PLAYED) }
    }
    BoxWithConstraints {
        if (maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.45f) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                recentTile(Modifier.fillMaxWidth(), true)
                playedTile(Modifier.fillMaxWidth(), true)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                recentTile(Modifier.weight(1f).fillMaxHeight(), false)
                playedTile(Modifier.weight(1f).fillMaxHeight(), false)
            }
        }
    }
}

@Composable
private fun DefaultLibraryListeningTile(
    title: String,
    tracks: List<Track>,
    icon: ImageVector,
    role: Color,
    animated: Boolean,
    index: Int,
    stacked: Boolean,
    modifier: Modifier,
    onOpen: () -> Unit
) {
    val artwork = remember(tracks) { tracks.firstCover() }
    val container = rememberLibraryContainer(artwork, role, animated)
    val colors = MaterialTheme.colorScheme
    val shape = if (stacked) levyraGroupedListShape(index, 2) else levyraGroupedGridShape(0, index, 1, 2)
    val surface = modifier
        .clip(shape)
        .drawBehind { drawRect(container.value) }
        .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)
        .padding(12.dp)
    val thumbnail: @Composable () -> Unit = {
        if (artwork.isBlank()) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.CenterStart) {
                Icon(icon, contentDescription = null, tint = colors.libraryTonalDetail)
            }
        } else {
            LibraryArtwork(artwork, title, Modifier.size(40.dp), LevyraCardDesign.ThumbShape, false)
        }
    }
    val caption: @Composable () -> Unit = {
        Text(title, style = LevyraType.contentTitle, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(LocalLevyraStrings.current.formatTrackCount(tracks.size), style = LevyraType.caption, color = colors.libraryTonalDetail)
    }
    if (stacked) {
        Row(surface, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            thumbnail()
            Column(Modifier.weight(1f)) { caption() }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
        }
    } else {
        Column(surface, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                thumbnail()
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
            }
            Column { caption() }
        }
    }
}

@Composable
private fun DefaultLibraryShelf(
    title: String,
    shelf: List<LibraryShelfItem>,
    category: LibraryCategory,
    onOpen: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LevyraSectionHeader(title = title, onTitleClick = onOpen)
        if (shelf.size == 1) {
            val entry = shelf.single()
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(LevyraCardDesign.EditorialShape)
                    .background(librarySurface())
                    .levyraPressable(onClick = entry.onOpen, role = Role.Button, pressedScale = LevyraPressScale.Row)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DefaultLibraryShelfArtwork(entry, category, Modifier.size(88.dp))
                DefaultLibraryShelfCaption(entry, Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
            }
            return@Column
        }
        LazyRow(
            modifier = Modifier.libraryCategoriesBleed(LibraryOverviewGutter),
            contentPadding = PaddingValues(horizontal = LibraryOverviewGutter),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shelf, key = { it.key }) { entry ->
                Column(
                    modifier = Modifier
                        .width(124.dp)
                        .clip(LevyraCardDesign.ArtworkShape)
                        .levyraPressable(onClick = entry.onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultLibraryShelfArtwork(entry, category, Modifier.fillMaxWidth().aspectRatio(1f))
                    DefaultLibraryShelfCaption(entry)
                }
            }
        }
    }
}

@Composable
private fun DefaultLibraryShelfArtwork(entry: LibraryShelfItem, category: LibraryCategory, modifier: Modifier) {
    val shape = LevyraCardDesign.ArtworkShape
    if (entry.artwork.isNotBlank()) {
        LibraryArtwork(entry.artwork, entry.title, modifier, shape, false)
    } else {
        val scheme = MaterialTheme.colorScheme
        Box(modifier.clip(shape).background(scheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(category.sectionIcon(), contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun DefaultLibraryShelfCaption(entry: LibraryShelfItem, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(entry.title, style = LevyraType.cardTitle, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (entry.subtitle.isNotBlank()) {
            Text(entry.subtitle, style = LevyraType.caption, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DefaultLibraryCategoryLinks(entries: List<LibrarySectionEntry>, onOpen: (LibraryCategory) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalLevyraStrings.current
    val surface = librarySurface()
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        entries.forEachIndexed { index, entry ->
            val title = entry.category.libraryLabel(strings)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clip(levyraGroupedListShape(index, entries.size))
                    .background(surface)
                    .levyraPressable(onClick = { onOpen(entry.category) }, role = Role.Button, pressedScale = LevyraPressScale.Row)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(entry.category.sectionIcon(), contentDescription = null, tint = scheme.libraryTonalDetail, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = LevyraType.contentTitle, color = scheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (entry.summary.isNotBlank() && entry.summary != title) {
                        Text(entry.summary, style = LevyraType.caption, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
            }
        }
    }
}

private fun List<Track>.firstCover(): String =
    firstNotNullOfOrNull { track -> track.thumbnailUrl.ifBlank { track.largeThumbnailUrl }.takeIf(String::isNotBlank) }.orEmpty()

private val ColorScheme.libraryTonalDetail: Color
    get() = lerp(onSurfaceVariant, onSurface, 0.35f)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberLibraryContainer(artwork: String, role: Color, animated: Boolean): State<Color> {
    val scheme = MaterialTheme.colorScheme
    val surface = librarySurface()
    val content = scheme.libraryTonalDetail
    val paletteKey = remember(artwork) {
        if (artwork.isBlank()) "" else ArtworkPaletteCache.key("library-overview", artwork, "")
    }
    val palette by rememberArtworkPalette(
        paletteKey = paletteKey,
        artworkUrl = artwork,
        fallback = NoArtworkPalette
    )
    val target = remember(palette.start, role, surface, content) {
        Color(libraryArtworkContainer(palette.start, role.toArgb(), surface.toArgb(), content.toArgb()))
    }
    return if (animated) {
        key(role, surface, content) {
            animateColorAsState(target, animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(), label = "library-container")
        }
    } else {
        rememberUpdatedState(target)
    }
}
