package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.CookieShape
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.SunnyShape
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.ArtworkPalette
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.moods.MoodShelf
import kotlinx.coroutines.delay
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings

@Composable
fun CuratedPlaylistsScreen(
    title: String,
    shelves: List<MoodShelf>,
    onOpen: (CollectionRef) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val code = LocalStrings.current.languageCode
    val shared = remember(code) { SharedStrings.forCode(code) }
    val groups = remember(shelves) { shelves.filter { it.playlists.isNotEmpty() } }
    val covers = remember(groups) { groups.flatMap { it.playlists }.map { it.artworkUrl }.filter(String::isNotBlank).distinct().take(3) }
    val total = remember(groups) { groups.flatMap { it.playlists }.distinctBy { it.id }.size }
    val tone by produceState(LevyraBrand.soft, covers.firstOrNull()) {
        value = covers.firstOrNull()?.let { ArtworkPalette.accentFor(it) } ?: LevyraBrand.soft
    }
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxWidth().height(420.dp)
                .background(Brush.verticalGradient(0f to tone.copy(alpha = 0.40f), 0.55f to tone.copy(alpha = 0.10f), 1f to Color.Transparent))
        )
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = ((maxWidth - Gutter * 2 + CellGap) / (CellMin + CellGap)).toInt().coerceIn(2, 8)
            ScrollableColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item(key = "curated-bar") {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(role = Role.Button, onClickLabel = shared.back, onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(LevyraIcons.ChevronLeft, contentDescription = shared.back, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }
                }
                item(key = "curated-hero") {
                    Column {
                        if (covers.isNotEmpty()) CuratedMosaic(covers, total)
                        if (total > 0) {
                            Text(
                                "$total · ${shared.playlistsPlain}".uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold),
                                color = LevyraBrand.soft,
                                modifier = Modifier.padding(top = 22.dp)
                            )
                        }
                        Text(title, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (groups.isEmpty()) {
                    item(key = "curated-empty") {
                        Text(shared.exploreEmpty, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 48.dp))
                    }
                }
                groups.forEachIndexed { group, shelf ->
                    item(key = "curated-head-$group-${shelf.title}") { GroupHeader(group, shelf, shared.playlistsPlain) }
                    val rows = shelf.playlists.chunked(columns)
                    rows.forEachIndexed { rowIndex, row ->
                        item(key = "curated-row-$group-$rowIndex") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                                row.forEach { playlist ->
                                    CuratedCard(
                                        playlist.title,
                                        playlist.subtitle,
                                        playlist.artworkUrl,
                                        Modifier.weight(1f)
                                    ) { onOpen(playlist.toCollectionRef()) }
                                }
                                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(group: Int, shelf: MoodShelf, playlistsLabel: String) {
    val badge = GroupBadges[group % GroupBadges.size]
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(40.dp).clip(badge).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Text("${group + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column {
            Text(shelf.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${shelf.playlists.size} · $playlistsLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CuratedMosaic(covers: List<String>, total: Int) {
    val side = MosaicSide
    Box(Modifier.fillMaxWidth().height(side)) {
        MosaicPiece(CookieShape, side, 0.dp, 0.dp, 0, MaterialTheme.colorScheme.surfaceContainerHighest) { Artwork(covers[0], Modifier.fillMaxSize(), cornerRadius = 0.dp) }
        covers.getOrNull(1)?.let { url ->
            MosaicPiece(RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomStartPercent = 12, bottomEndPercent = 12), side * 0.62f, side * 0.92f, 0.dp, 1, MaterialTheme.colorScheme.surfaceContainerHighest) {
                Artwork(url, Modifier.fillMaxSize(), cornerRadius = 0.dp)
            }
        }
        covers.getOrNull(2)?.let { url ->
            MosaicPiece(CircleShape, side * 0.56f, side * 1.24f, side * 0.44f, 2, MaterialTheme.colorScheme.surfaceContainerHighest) {
                Artwork(url, Modifier.fillMaxSize(), cornerRadius = 0.dp)
            }
        }
        if (total > 0) {
            MosaicPiece(SunnyShape, side * 0.4f, side * 0.74f, side * 0.6f, 3, MaterialTheme.colorScheme.primaryContainer) {
                Text("$total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun MosaicPiece(shape: Shape, size: Dp, x: Dp, y: Dp, order: Int, container: Color, content: @Composable () -> Unit) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(order * STAGGER_MS)
        reveal.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 380f))
    }
    Box(
        Modifier.offset(x, y).size(size)
            .graphicsLayer {
                val progress = reveal.value
                scaleX = 0.6f + 0.4f * progress
                scaleY = scaleX
                alpha = progress.coerceIn(0f, 1f)
                this.shape = shape
                clip = true
            }
            .background(container),
        contentAlignment = Alignment.Center
    ) { content() }
}

private val GroupBadges: List<Shape> = listOf(CookieShape, RoundedCornerShape(40), SunnyShape, RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomStartPercent = 14, bottomEndPercent = 14))
private val Gutter = 32.dp
private val CellGap = 18.dp
private val CellMin = 180.dp
private val MosaicSide = 168.dp
private const val STAGGER_MS = 70L
