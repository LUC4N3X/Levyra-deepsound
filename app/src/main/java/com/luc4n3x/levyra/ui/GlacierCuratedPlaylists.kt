package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.MonoCodeFont
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.viewmodel.DefaultCuratedCollectionsViewModel
import kotlinx.coroutines.launch

private val GlacierCuratedCellMin = 150.dp
private val GlacierCuratedSheetTile = 104.dp
private const val GlacierCuratedSheetCovers = 5

internal fun glacierCuratedGroups(
    curated: List<YoutubeMusicHomeShelf>,
    collections: List<YoutubeMusicHomeShelf>,
    leadTitle: String
): List<YoutubeMusicHomeShelf> {
    val seen = HashSet<String>()
    val leadKey = curated.firstOrNull()?.key
    return (curated + collections).distinctBy { it.key }.mapNotNull { shelf ->
        val items = shelf.items.filter { item -> item.playlist?.playlistId?.takeIf(String::isNotBlank)?.let(seen::add) == true }
        when {
            items.isEmpty() -> null
            shelf.key == leadKey -> shelf.copy(title = leadTitle, strapline = "", items = items)
            else -> shelf.copy(items = items)
        }
    }
}

private sealed interface GlacierCuratedRow {
    val key: String
    data class Header(override val key: String, val index: Int, val shelf: YoutubeMusicHomeShelf) : GlacierCuratedRow
    data class Cells(override val key: String, val offset: Int, val items: List<YoutubeMusicHomeItem>) : GlacierCuratedRow
}

private fun glacierCuratedRows(groups: List<YoutubeMusicHomeShelf>, columns: Int): List<GlacierCuratedRow> =
    groups.flatMapIndexed { index, shelf ->
        listOf(GlacierCuratedRow.Header("glacier-curated-head-${shelf.key}", index, shelf)) +
            shelf.items.chunked(columns).mapIndexed { row, items ->
                GlacierCuratedRow.Cells("glacier-curated-row-${shelf.key}-$row-$columns", row * columns, items)
            }
    }

@Composable
internal fun GlacierCuratedPlaylistsScreen(
    title: String,
    leadTitle: String,
    curatedShelves: List<YoutubeMusicHomeShelf>,
    onOpen: (PlaylistHit) -> Unit,
    onDismiss: () -> Unit,
    bottomInset: Dp
) {
    BackHandler(onBack = onDismiss)
    val strings = LocalLevyraStrings.current
    val collections = rememberDefaultCuratedCollectionShelves(strings.code)
    val collectionsModel = viewModel<DefaultCuratedCollectionsViewModel>(key = "default-curated-collections")
    val collectionsLoading by collectionsModel.loading.collectAsStateWithLifecycle()
    val groups = remember(curatedShelves, collections, leadTitle) { glacierCuratedGroups(curatedShelves, collections, leadTitle) }
    val covers = remember(groups) {
        groups.flatMap { it.items }.map { it.thumbnailUrl }.filter(String::isNotBlank).distinct().take(GlacierCuratedSheetCovers)
    }
    val total = remember(groups) { groups.sumOf { it.items.size } }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val animated = LocalAnimationsEnabled.current
    val entrance = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120 } }
    val bandAlpha by animateFloatAsState(if (scrolled) 1f else 0f, tween(220), label = "glacier-curated-band")
    val glow = MonoDesign.accent
    BoxWithConstraints(
        Modifier.fillMaxSize().zIndex(2f).graphicsLayer {
            alpha = entrance.value
            translationY = (1f - entrance.value) * 28.dp.toPx()
        }.background(MonoDesign.base)
    ) {
        val columns = ((maxWidth - MonoDesign.gutter * 2 + MonoDesign.itemGap) / (GlacierCuratedCellMin + MonoDesign.itemGap)).toInt().coerceIn(2, 6)
        val rows = remember(groups, columns) { glacierCuratedRows(groups, columns) }
        val headerIndices = remember(rows) { rows.withIndex().filter { it.value is GlacierCuratedRow.Header }.map { it.index + 2 } }
        Box(
            Modifier.fillMaxWidth().height(460.dp).drawBehind {
                drawRect(Brush.verticalGradient(0f to glow.copy(alpha = 0.18f), 0.6f to glow.copy(alpha = 0.04f), 1f to Color.Transparent))
                drawRect(Brush.radialGradient(listOf(glow.copy(alpha = 0.14f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, 0f), radius = size.width))
            }
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = top + 56.dp, bottom = bottomInset + MonoDesign.sectionGap)
        ) {
            item(key = "glacier-curated-intro", contentType = "glacier-curated-intro") {
                GlacierCuratedIntro(title, total, covers)
            }
            item(key = "glacier-curated-jump", contentType = "glacier-curated-jump") {
                if (groups.size > 1) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = MonoDesign.gutter),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 20.dp)
                    ) {
                        itemsIndexed(groups, key = { _, shelf -> "jump-${shelf.key}" }) { index, shelf ->
                            MonoPill(shelf.title, selected = false, onClick = {
                                headerIndices.getOrNull(index)?.let { target -> scope.launch { listState.animateScrollToItem(target) } }
                            })
                        }
                    }
                }
            }
            rows.forEach { row ->
                when (row) {
                    is GlacierCuratedRow.Header -> item(key = row.key, contentType = "glacier-curated-head") {
                        GlacierCuratedGroupHeader(row.index, row.shelf)
                    }
                    is GlacierCuratedRow.Cells -> item(key = row.key, contentType = "glacier-curated-cells") {
                        Row(
                            Modifier.fillMaxWidth().monoReveal(row.key).padding(horizontal = MonoDesign.gutter).padding(bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)
                        ) {
                            row.items.forEachIndexed { cell, item ->
                                GlacierCuratedCell(item, row.offset + cell + 1, Modifier.weight(1f)) { item.playlist?.let(onOpen) }
                            }
                            repeat(columns - row.items.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            if (collectionsLoading && collections.size < 2) {
                item(key = "glacier-curated-loading", contentType = "glacier-curated-loading") {
                    MonoWaveSkeleton(Modifier.padding(top = MonoDesign.sectionGap), tile = GlacierCuratedSheetTile, tiles = columns)
                }
            } else if (groups.isEmpty()) {
                item(key = "glacier-curated-empty", contentType = "glacier-curated-empty") {
                    Text(
                        strings.exploreEmpty, style = MaterialTheme.typography.bodyLarge, color = MonoDesign.textSecondary,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter, vertical = MonoDesign.sectionGap)
                    )
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(top + 56.dp)
                .graphicsLayer { alpha = bandAlpha }
                .background(MonoDesign.base.copy(alpha = 0.94f))
                .drawBehind { drawRect(MonoDesign.hairline, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 1.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx())) }
        )
        Row(
            Modifier.fillMaxWidth().padding(top = top + 4.dp, start = 8.dp, end = MonoDesign.gutter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MonoDesign.void.copy(alpha = 0.4f))
                    .clickable(role = Role.Button, onClickLabel = strings.back, onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = MonoDesign.text, modifier = Modifier.size(22.dp)) }
            Text(
                title, style = MaterialTheme.typography.titleMedium, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp).weight(1f).graphicsLayer { alpha = bandAlpha }.clearAndSetSemantics {}
            )
        }
    }
}

@Composable
private fun GlacierCuratedIntro(title: String, total: Int, covers: List<String>) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter)) {
        if (covers.isNotEmpty()) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(GlacierCuratedSheetTile + 24.dp).clearAndSetSemantics {}) {
                val tile = GlacierCuratedSheetTile
                val step = if (covers.size > 1) ((maxWidth - tile) / (covers.size - 1)).coerceAtMost(tile * 0.78f) else 0.dp
                covers.forEachIndexed { index, url ->
                    val lift = if (index % 2 == 0) 0.dp else 18.dp
                    Box(
                        Modifier.offset(x = step * index, y = lift).zIndex(index.toFloat()).size(tile)
                            .shadow(14.dp, MonoDesign.artShape).clip(MonoDesign.artShape).background(MonoDesign.raised)
                            .border(1.dp, MonoDesign.hairlineStrong, MonoDesign.artShape)
                    ) { MonoArtwork(url, Modifier.fillMaxSize()) }
                }
            }
            MonoWaveLine(playing = false, color = MonoDesign.accentDim, modifier = Modifier.fillMaxWidth().height(22.dp).padding(top = 10.dp))
        }
        if (total > 0) {
            Text(
                "${monoRank(total)} · ${strings.playlistsPlain}".uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoCodeFont), color = MonoDesign.accent, maxLines = 1,
                modifier = Modifier.padding(top = 18.dp)
            )
        }
        Text(
            title, style = MaterialTheme.typography.displaySmall, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp).semantics { heading() }
        )
    }
}

@Composable
private fun GlacierCuratedGroupHeader(index: Int, shelf: YoutubeMusicHomeShelf) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.fillMaxWidth().monoReveal("glacier-curated-head-${shelf.key}")) {
        Row(
            Modifier.fillMaxWidth()
                .padding(start = MonoDesign.gutter, end = MonoDesign.gutter, top = MonoDesign.sectionGap, bottom = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(monoRank(index + 1), style = monoRankStyle(30), color = MonoDesign.accent, modifier = Modifier.clearAndSetSemantics {})
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    shelf.title, style = MaterialTheme.typography.titleLarge, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                val strapline = monoWithoutYoutube(shelf.strapline)
                MonoOverline(if (strapline.isBlank()) "${shelf.items.size} · ${strings.playlistsPlain}" else "$strapline · ${shelf.items.size}")
            }
        }
        Box(Modifier.padding(horizontal = MonoDesign.gutter).padding(bottom = 14.dp).fillMaxWidth().height(1.dp).background(MonoDesign.hairline))
    }
}

@Composable
private fun GlacierCuratedCell(item: YoutubeMusicHomeItem, number: Int, modifier: Modifier, onClick: () -> Unit) {
    val subtitle = remember(item.subtitle) { monoWithoutYoutube(item.subtitle) }
    Column(
        modifier.levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = listOf(item.title, subtitle).filter(String::isNotBlank).joinToString(", ") }
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(MonoDesign.artShape).background(MonoDesign.raised)
                .border(1.dp, MonoDesign.hairline, MonoDesign.artShape)
        ) {
            MonoArtwork(item.thumbnailUrl, Modifier.fillMaxSize())
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().height(44.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, MonoDesign.void.copy(alpha = 0.62f))))
            )
            Text(
                monoRank(number), style = monoRankStyle(12), color = MonoDesign.text,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clearAndSetSemantics {}
            )
        }
        Text(
            item.title, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text,
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            subtitle, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary,
            minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
        )
    }
}
