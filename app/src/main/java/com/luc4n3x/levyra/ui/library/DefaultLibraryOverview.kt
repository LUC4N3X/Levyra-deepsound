package com.luc4n3x.levyra.ui.library

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.ui.components.LevyraUtilityColors
import com.luc4n3x.levyra.ui.components.LevyraUtilitySeed
import com.luc4n3x.levyra.ui.components.levyraUtilityColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.SavedPlaylist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.playableTrackCount
import com.luc4n3x.levyra.ui.components.LevyraPastelAmber
import com.luc4n3x.levyra.ui.components.LevyraPastelLavender
import com.luc4n3x.levyra.ui.components.LevyraPastelPeriwinkle
import com.luc4n3x.levyra.ui.components.LevyraPastelRose
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
    strings: LevyraStrings,
    onOpenCollection: (String) -> Unit,
    onPlayFavorites: () -> Unit,
    onPlayRecent: (Track) -> Unit,
    onOpenCategory: (LibraryCategory) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenSavedPlaylist: (SavedPlaylist) -> Unit
) {
    item(key = "default-library-overview") {
        DefaultLibraryFavorites(
            tracks = favorites,
            onOpen = { onOpenCollection(SMART_COLLECTION_FAVORITES) },
            onPlay = onPlayFavorites
        )
    }
    item(key = "default-library-listening") {
        DefaultLibraryListening(recent, mostPlayed, onOpenCollection)
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
    item(key = "default-library-categories") {
        val links = remember(entries, hasPlaylists) {
            entries.filterNot { it.category == LibraryCategory.Playlists && hasPlaylists }
        }
        DefaultLibraryCategoryLinks(links, onOpenCategory)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultLibraryFavorites(tracks: List<Track>, onOpen: () -> Unit, onPlay: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val scheme = MaterialTheme.colorScheme
    val utility = levyraUtilityColors(LevyraUtilitySeed)
    val covers = remember(tracks) { tracks.covers(LibraryFanSize) }
    val hasTracks = tracks.isNotEmpty()
    val heartShape = MaterialShapes.Cookie9Sided.toShape()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LevyraCardDesign.SurfaceShape)
            .background(librarySurface())
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface)
            .padding(20.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(44.dp).clip(heartShape).background(utility.accent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = utility.onAccent, modifier = Modifier.size(22.dp))
                }
                Text(
                    strings.favoritesPlain,
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (hasTracks) strings.formatTrackCount(tracks.size) else strings.tapHeartToAdd,
                    color = scheme.libraryTonalDetail,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (hasTracks) DefaultLibraryPlayFavorites(onPlay)
            }
            LibraryCoverFan(covers, cardSize = 84.dp, accent = utility.accent)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultLibraryPlayFavorites(onPlay: () -> Unit) {
    val utility = levyraUtilityColors(LevyraUtilitySeed)
    val strings = LocalLevyraStrings.current
    FilledIconButton(
        onClick = onPlay,
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = utility.accent, contentColor = utility.onAccent),
        modifier = Modifier.size(48.dp)
    ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = "${strings.play}, ${strings.favoritesPlain}")
    }
}

@Composable
private fun DefaultLibraryListening(
    recent: List<Track>,
    mostPlayed: List<Track>,
    onOpenCollection: (String) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val recentColors = levyraUtilityColors(LevyraUtilitySeed)
    val playedColors = levyraUtilityColors(LevyraPastelPeriwinkle)
    val recentTile: @Composable (Modifier, Boolean) -> Unit = { modifier, stacked ->
        DefaultLibraryListeningTile(
            strings.recent, recent, Icons.Rounded.History, recentColors,
            index = 0, stacked = stacked, modifier = modifier
        ) { onOpenCollection(SMART_COLLECTION_RECENT) }
    }
    val playedTile: @Composable (Modifier, Boolean) -> Unit = { modifier, stacked ->
        DefaultLibraryListeningTile(
            strings.pulsePlays, mostPlayed, Icons.Rounded.Replay, playedColors,
            index = 1, stacked = stacked, modifier = modifier
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
    tone: LevyraUtilityColors,
    index: Int,
    stacked: Boolean,
    modifier: Modifier,
    onOpen: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val covers = remember(tracks) { tracks.covers(2) }
    val shape = if (stacked) levyraGroupedListShape(index, 2) else levyraGroupedGridShape(0, index, 1, 2)
    val surface = modifier
        .clip(shape)
        .background(librarySurface())
        .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)
        .padding(14.dp)
    val badge: @Composable () -> Unit = {
        Box(Modifier.size(36.dp).clip(CircleShape).background(tone.accent), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tone.onAccent, modifier = Modifier.size(20.dp))
        }
    }
    val caption: @Composable () -> Unit = {
        Text(title, style = LevyraType.contentTitle, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(LocalLevyraStrings.current.formatTrackCount(tracks.size), style = LevyraType.caption, color = colors.libraryTonalDetail)
    }
    if (stacked) {
        Row(surface, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            badge()
            Column(Modifier.weight(1f)) { caption() }
            LibraryCoverFan(covers, cardSize = 40.dp, accent = tone.accent, slots = 2)
        }
    } else {
        Column(surface, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                badge()
                LibraryCoverFan(covers, cardSize = 48.dp, accent = tone.accent, slots = 2)
            }
            Column { caption() }
        }
    }
}

@Composable
private fun LibraryCoverFan(covers: List<String>, cardSize: Dp, accent: Color, slots: Int = LibraryFanSize) {
    val step = cardSize * 0.34f
    Box(Modifier.size(width = cardSize + step * (slots - 1), height = cardSize), contentAlignment = Alignment.CenterStart) {
        repeat(slots) { slot ->
            val cover = covers.getOrNull(slot)
            Box(
                Modifier
                    .offset(x = step * slot)
                    .zIndex(slot.toFloat())
                    .size(cardSize)
                    .drawWithCache {
                        val radius = size.minDimension / 2f
                        val groove = Stroke(width = 1.dp.toPx())
                        onDrawBehind {
                            drawCircle(LibraryVinylBase, radius)
                            drawCircle(accent.copy(alpha = LibraryVinylDiscAlpha), radius)
                            LibraryVinylGrooves.forEach { ratio ->
                                drawCircle(accent.copy(alpha = LibraryVinylGrooveAlpha), radius * ratio, style = groove)
                            }
                            if (cover == null) drawCircle(accent.copy(alpha = LibraryVinylLabelAlpha), radius * LibraryVinylLabel)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (cover != null) {
                    LibraryArtwork(cover, "", Modifier.fillMaxSize(LibraryVinylLabel).clip(CircleShape), CircleShape, false)
                }
                Box(Modifier.size(cardSize * 0.06f).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)))
            }
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
        val utility = levyraUtilityColors(LevyraUtilitySeed)
        Box(modifier.clip(shape).background(utility.container), contentAlignment = Alignment.Center) {
            Icon(category.sectionIcon(), contentDescription = null, tint = utility.onContainer, modifier = Modifier.size(28.dp))
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
    if (entries.isEmpty()) return
    BoxWithConstraints {
        val columns = if (maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.45f) 1 else 2
        val rows = entries.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            rows.forEachIndexed { row, items ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    items.forEachIndexed { column, entry ->
                        val index = row * columns + column
                        DefaultLibraryCategoryTile(
                            entry = entry,
                            tone = LibraryCategoryTones[index % LibraryCategoryTones.size],
                            shape = levyraGroupedGridShape(row, column, rows.size, items.size),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onOpen = onOpen
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultLibraryCategoryTile(
    entry: LibrarySectionEntry,
    tone: Color,
    shape: Shape,
    modifier: Modifier,
    onOpen: (LibraryCategory) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalLevyraStrings.current
    val colors = levyraUtilityColors(tone)
    val title = entry.category.libraryLabel(strings)
    val badgeShape = MaterialShapes.Cookie7Sided.toShape()
    Column(
        modifier = modifier
            .heightIn(min = 112.dp)
            .clip(shape)
            .background(librarySurface())
            .levyraPressable(onClick = { onOpen(entry.category) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(Modifier.size(40.dp).clip(badgeShape).background(colors.accent), contentAlignment = Alignment.Center) {
            Icon(entry.category.sectionIcon(), contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.padding(top = 12.dp)) {
            Text(title, style = LevyraType.contentTitle, color = scheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (entry.summary.isNotBlank() && entry.summary != title) {
                Text(entry.summary, style = LevyraType.caption, color = scheme.libraryTonalDetail, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private val LibraryCategoryTones = listOf(
    LevyraUtilitySeed,
    LevyraPastelPeriwinkle,
    LevyraPastelLavender,
    LevyraPastelRose,
    LevyraPastelAmber
)

private fun List<Track>.covers(limit: Int): List<String> =
    asSequence()
        .map { track -> track.thumbnailUrl.ifBlank { track.largeThumbnailUrl } }
        .filter(String::isNotBlank)
        .distinct()
        .take(limit)
        .toList()

private const val LibraryFanSize = 3
private const val LibraryVinylDiscAlpha = 0.18f
private const val LibraryVinylGrooveAlpha = 0.32f
private const val LibraryVinylLabelAlpha = 0.85f
private val LibraryVinylGrooves = floatArrayOf(0.92f, 0.82f, 0.72f, 0.62f)
private const val LibraryVinylLabel = 0.6f
private val LibraryVinylBase = Color(0xFF0E1216)

private val ColorScheme.libraryTonalDetail: Color
    get() = lerp(onSurfaceVariant, onSurface, 0.35f)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultSmartCollectionHero(
    title: String,
    subtitle: String,
    countLabel: String,
    accent: Color,
    icon: ImageVector,
    tracks: List<Track>,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalLevyraStrings.current
    val tone = levyraUtilityColors(accent)
    val covers = remember(tracks) { tracks.covers(LibraryFanSize) }
    val badgeShape = MaterialShapes.Cookie9Sided.toShape()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LevyraCardDesign.SurfaceShape)
            .background(librarySurface())
            .padding(start = 12.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            LevyraBackButton(onClick = onClose, contentDescription = strings.back)
            Spacer(Modifier.weight(1f))
            LibraryCoverFan(covers, cardSize = 76.dp, accent = tone.accent)
        }
        Row(
            modifier = Modifier.padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).clip(badgeShape).background(tone.accent), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tone.onAccent, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    color = scheme.onSurface,
                    style = LevyraType.screenTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    if (subtitle.isNotBlank()) "$countLabel · $subtitle" else countLabel,
                    color = scheme.libraryTonalDetail,
                    style = LevyraType.metadata,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
