package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.CookieShape
import com.luc4n3x.levyra.desktop.app.ui.components.SectionTints
import com.luc4n3x.levyra.desktop.app.ui.components.ShelfRow
import com.luc4n3x.levyra.desktop.app.ui.components.scallopShape
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.icons.OfflineIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings

@Immutable
internal class LibraryTone(val accent: Color, val onAccent: Color)

internal object LibraryTones {
    val Sky = LibraryTone(LevyraBrand.soft, LevyraBrand.onSoft)
    val Periwinkle = LibraryTone(SectionTints.Periwinkle, Color(0xFF16204A))
    val Lavender = LibraryTone(SectionTints.Lavender, Color(0xFF1B1530))
    val Rose = LibraryTone(SectionTints.Rose, Color(0xFF3B0D24))
    val Amber = LibraryTone(SectionTints.Amber, Color(0xFF3A2400))
    val cycle = listOf(Sky, Periwinkle, Lavender, Rose, Amber)
}

internal object LibraryShapes {
    val Surface: Shape = RoundedCornerShape(24.dp)
    val Editorial: Shape = RoundedCornerShape(20.dp)
    val Artwork: Shape = RoundedCornerShape(12.dp)
    val Badge: Shape = scallopShape(lobes = 7, depth = 0.08f)

    fun grouped(row: Int, column: Int, rows: Int, columns: Int): Shape {
        fun corner(outer: Boolean): Dp = if (outer) 24.dp else 6.dp
        return RoundedCornerShape(
            topStart = corner(row == 0 && column == 0),
            topEnd = corner(row == 0 && column == columns - 1),
            bottomEnd = corner(row == rows - 1 && column == columns - 1),
            bottomStart = corner(row == rows - 1 && column == 0)
        )
    }
}

@Composable
internal fun librarySurface(): Color = lerp(MaterialTheme.colorScheme.surfaceContainer, LevyraBrand.soft, 0.035f)

@Composable
private fun libraryDetailColor(): Color =
    lerp(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.onSurface, 0.35f)

@Immutable
internal class LibraryShelfEntry(
    val key: String,
    val title: String,
    val subtitle: String,
    val artwork: String,
    val onOpen: () -> Unit
)

@Immutable
internal class LibraryCategoryEntry(
    val key: String,
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val onOpen: () -> Unit
)

internal fun List<Track>.libraryCovers(limit: Int): List<String> =
    asSequence().map { it.artworkUrl }.filter(String::isNotBlank).distinct().take(limit).toList()

@Composable
internal fun LibraryListeningBlock(
    favorites: List<Track>,
    recent: List<Track>,
    mostPlayed: List<Track>,
    shared: SharedStrings,
    onOpenFavorites: () -> Unit,
    onPlayFavorites: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenMostPlayed: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 860.dp) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                LibraryFavoritesHero(favorites, shared, LibraryShapes.grouped(0, 0, 1, 3), Modifier.weight(2f).fillMaxHeight(), onOpenFavorites, onPlayFavorites)
                LibraryListeningTile(shared.recent, recent, OfflineIcons.History, LibraryTones.Sky, shared, LibraryShapes.grouped(0, 1, 1, 3), Modifier.weight(1f).fillMaxHeight(), onOpenRecent)
                LibraryListeningTile(shared.pulsePlays, mostPlayed, LevyraIcons.Refresh, LibraryTones.Periwinkle, shared, LibraryShapes.grouped(0, 2, 1, 3), Modifier.weight(1f).fillMaxHeight(), onOpenMostPlayed)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LibraryFavoritesHero(favorites, shared, LibraryShapes.Surface, Modifier.fillMaxWidth(), onOpenFavorites, onPlayFavorites)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    LibraryListeningTile(shared.recent, recent, OfflineIcons.History, LibraryTones.Sky, shared, LibraryShapes.grouped(0, 0, 1, 2), Modifier.weight(1f).fillMaxHeight(), onOpenRecent)
                    LibraryListeningTile(shared.pulsePlays, mostPlayed, LevyraIcons.Refresh, LibraryTones.Periwinkle, shared, LibraryShapes.grouped(0, 1, 1, 2), Modifier.weight(1f).fillMaxHeight(), onOpenMostPlayed)
                }
            }
        }
    }
}

@Composable
private fun LibraryFavoritesHero(
    tracks: List<Track>,
    shared: SharedStrings,
    shape: Shape,
    modifier: Modifier,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val tone = LibraryTones.Sky
    Box(
        modifier
            .clip(shape)
            .background(librarySurface())
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(24.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(52.dp).clip(CookieShape).background(tone.accent), contentAlignment = Alignment.Center) {
                    Icon(LevyraIcons.HeartFilled, contentDescription = null, tint = tone.onAccent, modifier = Modifier.size(24.dp))
                }
                Text(
                    shared.favoritesPlain,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (tracks.isNotEmpty()) shared.formatTrackCount(tracks.size) else shared.tapHeartToAdd,
                    color = libraryDetailColor(),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (tracks.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(tone.accent)
                            .clickable(role = Role.Button, onClickLabel = "${shared.play}, ${shared.favoritesPlain}", onClick = onPlay),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(LevyraIcons.Play, contentDescription = "${shared.play}, ${shared.favoritesPlain}", tint = tone.onAccent, modifier = Modifier.size(20.dp))
                    }
                }
            }
            LibraryCoverFan(tracks.libraryCovers(3), cardSize = 104.dp, accent = tone.accent)
        }
    }
}

@Composable
private fun LibraryListeningTile(
    title: String,
    tracks: List<Track>,
    icon: ImageVector,
    tone: LibraryTone,
    shared: SharedStrings,
    shape: Shape,
    modifier: Modifier,
    onOpen: () -> Unit
) {
    Column(
        modifier
            .clip(shape)
            .background(librarySurface())
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(tone.accent), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tone.onAccent, modifier = Modifier.size(20.dp))
            }
            LibraryCoverFan(tracks.libraryCovers(2), cardSize = 56.dp, accent = tone.accent, slots = 2)
        }
        Column(Modifier.padding(top = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(shared.formatTrackCount(tracks.size), style = MaterialTheme.typography.bodyMedium, color = libraryDetailColor())
        }
    }
}

@Composable
internal fun LibraryCoverFan(covers: List<String>, cardSize: Dp, accent: Color, slots: Int = 3) {
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
                            drawCircle(VinylBase, radius)
                            drawCircle(accent.copy(alpha = 0.18f), radius)
                            VinylGrooves.forEach { drawCircle(accent.copy(alpha = 0.32f), radius * it, style = groove) }
                            if (cover == null) drawCircle(accent.copy(alpha = 0.85f), radius * VinylLabel)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (cover != null) {
                    Artwork(url = cover, modifier = Modifier.fillMaxSize(VinylLabel).clip(CircleShape), cornerRadius = cardSize, iconSize = 12.dp)
                }
                Box(Modifier.size(cardSize * 0.06f).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)))
            }
        }
    }
}

@Composable
internal fun LibrarySectionTitle(title: String, detail: String = "", onClick: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (onClick != null) {
                Icon(
                    LevyraIcons.ChevronLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f }
                )
            }
        }
        if (detail.isNotBlank()) {
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun LibraryShelf(title: String, entries: List<LibraryShelfEntry>, fallbackIcon: ImageVector, onOpenAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LibrarySectionTitle(title, onClick = onOpenAll)
        val single = entries.singleOrNull()
        if (single != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(LibraryShapes.Editorial)
                    .background(librarySurface())
                    .clickable(role = Role.Button, onClick = single.onOpen)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibraryShelfArtwork(single, fallbackIcon, Modifier.size(104.dp))
                LibraryShelfCaption(single, Modifier.weight(1f))
                Icon(
                    LevyraIcons.ChevronLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f }
                )
            }
            return@Column
        }
        ShelfRow(contentPadding = PaddingValues(0.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(entries.size, key = { entries[it].key }) { index ->
                val entry = entries[index]
                Column(
                    Modifier
                        .width(168.dp)
                        .clip(LibraryShapes.Artwork)
                        .clickable(role = Role.Button, onClick = entry.onOpen),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryShelfArtwork(entry, fallbackIcon, Modifier.fillMaxWidth().aspectRatio(1f))
                    LibraryShelfCaption(entry)
                }
            }
        }
    }
}

@Composable
private fun LibraryShelfArtwork(entry: LibraryShelfEntry, fallbackIcon: ImageVector, modifier: Modifier) {
    if (entry.artwork.isNotBlank()) {
        Artwork(url = entry.artwork, modifier = modifier, cornerRadius = 12.dp, iconSize = 28.dp)
    } else {
        Box(modifier.clip(LibraryShapes.Artwork).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(fallbackIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun LibraryShelfCaption(entry: LibraryShelfEntry, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(entry.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (entry.subtitle.isNotBlank()) {
            Text(entry.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun LibraryCategoryGrid(entries: List<LibraryCategoryEntry>) {
    if (entries.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 1100.dp -> 4
            maxWidth >= 760.dp -> 3
            else -> 2
        }.coerceAtMost(entries.size)
        val rows = entries.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            rows.forEachIndexed { row, items ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    items.forEachIndexed { column, entry ->
                        val index = row * columns + column
                        LibraryCategoryTile(
                            entry = entry,
                            tone = LibraryTones.cycle[index % LibraryTones.cycle.size],
                            shape = LibraryShapes.grouped(row, column, rows.size, columns),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    repeat(columns - items.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun LibraryCategoryTile(entry: LibraryCategoryEntry, tone: LibraryTone, shape: Shape, modifier: Modifier) {
    Column(
        modifier
            .heightIn(min = 132.dp)
            .clip(shape)
            .background(librarySurface())
            .clickable(role = Role.Button, onClick = entry.onOpen)
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(Modifier.size(48.dp).clip(LibraryShapes.Badge).background(tone.accent), contentAlignment = Alignment.Center) {
            Icon(entry.icon, contentDescription = null, tint = tone.onAccent, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.padding(top = 14.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (entry.summary.isNotBlank() && entry.summary != entry.title) {
                Text(entry.summary, style = MaterialTheme.typography.bodyMedium, color = libraryDetailColor(), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private val VinylBase = Color(0xFF0E1216)
private val VinylGrooves = floatArrayOf(0.92f, 0.82f, 0.72f, 0.62f)
private const val VinylLabel = 0.6f
