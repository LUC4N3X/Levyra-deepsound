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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.luc4n3x.levyra.desktop.app.state.CuratedPageState
import com.luc4n3x.levyra.desktop.app.ui.components.LoadingRow
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.ShelfRow
import com.luc4n3x.levyra.desktop.app.ui.components.SunnyShape
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.ArtworkPalette
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.moods.MoodCategory
import com.luc4n3x.levyra.desktop.core.moods.MoodShelf
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.i18n.curatedCollectionsCopy
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings

@Composable
fun CuratedPlaylistsScreen(
    shelves: List<MoodShelf>,
    page: CuratedPageState,
    onSelectCategory: (String) -> Unit,
    onOpen: (CollectionRef) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val code = LocalStrings.current.languageCode
    val shared = remember(code) { SharedStrings.forCode(code) }
    val copy = remember(code) { LevyraMonoHomeStrings.forCode(code) }
    val browseCopy = remember(code) { curatedCollectionsCopy(code) }
    val title = copy.mixedForYou
    val groups = remember(shelves, page.collections, copy, browseCopy) {
        val collections = page.collections
        val lead = shelves.mapIndexed { index, shelf ->
            if (index == 0) shelf.copy(title = copy.rotationOverline, subtitle = "") else shelf
        }
        val candidates = lead + listOf(
            MoodShelf(browseCopy.decadesTitle, collections?.decades.orEmpty(), subtitle = browseCopy.decadesSubtitle),
            MoodShelf(browseCopy.everydayTitle, collections?.everyday.orEmpty(), subtitle = browseCopy.everydaySubtitle)
        )
        val seen = HashSet<String>()
        candidates.mapNotNull { shelf ->
            val unique = shelf.playlists.filter { seen.add(it.id) }
            shelf.copy(playlists = unique).takeIf { unique.isNotEmpty() }
        }
    }
    val ids = remember(groups) { groups.flatMapTo(HashSet()) { shelf -> shelf.playlists.map { it.id } } }
    val browse = remember(ids, page.playlists) { page.playlists.filterNot { it.id in ids }.take(12) }
    val categories = page.collections?.categories.orEmpty()
    val browseShelf = MoodShelf(browseCopy.browseTitle, browse, subtitle = browseCopy.browseSubtitle)
    val visibleGroups = if (categories.isEmpty()) groups else groups + browseShelf
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
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val jumpItems = if (visibleGroups.size > 1) 3 else 2
            ScrollableColumn(
                state = listState,
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
                    CuratedHeader(title, covers, total, shared.playlistsPlain)
                }
                if (visibleGroups.size > 1) {
                    item(key = "curated-jumps") {
                        ShelfRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(visibleGroups.size) { index ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val target = jumpItems + groups.take(index).sumOf { 1 + (it.playlists.size + columns - 1) / columns }
                                        scope.launch { listState.scrollToItem(target) }
                                    },
                                    label = { Text(visibleGroups[index].title) }
                                )
                            }
                        }
                    }
                }
                if (visibleGroups.isEmpty() && !page.loading) {
                    item(key = "curated-empty") {
                        Text(shared.exploreEmpty, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 48.dp))
                    }
                }
                curatedGroupItems(
                    visibleGroups, groups.size, columns, categories, page.category,
                    browseCopy.decadesTitle, shared.playlistsPlain, onSelectCategory, onOpen
                )
                if (page.loading || page.categoryLoading) {
                    item(key = "curated-loading") { LoadingRow(LocalStrings.current.loading) }
                } else if (categories.isNotEmpty() && browse.isEmpty()) {
                    item(key = "curated-browse-empty") { Text(shared.exploreEmpty) }
                }
            }
        }
    }
}

@Composable
private fun CuratedHeader(title: String, covers: List<String>, total: Int, playlistsLabel: String) {
    Column {
        if (covers.isNotEmpty()) CuratedMosaic(covers, total)
        if (total > 0) {
            Text(
                "$total · $playlistsLabel".uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold),
                color = LevyraBrand.soft,
                modifier = Modifier.padding(top = 22.dp)
            )
        }
        Text(title, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun LazyListScope.curatedGroupItems(
    visibleGroups: List<MoodShelf>,
    categoryGroupIndex: Int,
    columns: Int,
    categories: List<MoodCategory>,
    selectedCategory: String,
    decadesTitle: String,
    playlistsLabel: String,
    onSelectCategory: (String) -> Unit,
    onOpen: (CollectionRef) -> Unit
) {
    visibleGroups.forEachIndexed { group, shelf ->
        val picker = group == categoryGroupIndex && categories.isNotEmpty()
        item(key = "curated-head-$group-${shelf.title}") { GroupHeader(group, shelf, playlistsLabel) }
        if (picker) {
            item(key = "curated-picker") {
                CuratedCategoryPicker(categories, selectedCategory, decadesTitle, onSelectCategory)
            }
        }
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

@Composable
private fun CuratedCategoryPicker(
    categories: List<MoodCategory>,
    selected: String,
    decades: String,
    onSelect: (String) -> Unit
) {
    val groups = remember(categories) { categories.groupBy { it.sectionIndex } }
    val active = categories.firstOrNull { it.params == selected }?.sectionIndex ?: categories.first().sectionIndex
    Column {
        ShelfRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(groups.values.toList(), key = { it.first().sectionIndex }) { group ->
                val first = group.first()
                FilterChip(
                    selected = active == first.sectionIndex,
                    onClick = { onSelect(first.params) },
                    label = { Text(first.section.ifBlank { decades }) }
                )
            }
        }
        ShelfRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(groups[active].orEmpty(), key = { it.params }) { category ->
                FilterChip(
                    selected = selected == category.params,
                    onClick = { onSelect(category.params) },
                    label = { Text(category.title) }
                )
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
            Text(shelf.subtitle.ifBlank { "${shelf.playlists.size} · $playlistsLabel" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
