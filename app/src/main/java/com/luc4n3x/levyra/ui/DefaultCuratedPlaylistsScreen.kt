package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.LevyraFilterPill
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DefaultCuratedCellMin = 148.dp
private val DefaultCuratedGap = 12.dp
private val DefaultCuratedRowGap = 22.dp
private val DefaultCuratedBarHeight = 64.dp
private val DefaultCuratedBackdrop = 460.dp
private val DefaultCuratedMosaicMax = 220.dp
private const val DefaultCuratedMosaicRatio = 1.8f
private const val DefaultCuratedStaggerMillis = 70L

@Composable
internal fun DefaultCuratedPlaylistsScreen(page: CuratedPageData, onOpen: (PlaylistHit) -> Unit, onDismiss: () -> Unit, bottomInset: Dp) {
    val scheme = MaterialTheme.colorScheme
    val animated = LocalAnimationsEnabled.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var heroPx by remember { mutableIntStateOf(1) }
    var stickyPx by remember { mutableIntStateOf(0) }
    val collapse by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / heroPx.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        }
    }
    val lead = page.covers.firstOrNull().orEmpty()
    val fallback = remember(scheme.primary, scheme.tertiary) { ArtworkPalette(scheme.primary.toArgb(), scheme.tertiary.toArgb()) }
    val palette by rememberArtworkPalette(paletteKey = if (lead.isBlank()) "" else "curated-page-$lead", artworkUrl = lead, fallback = fallback)
    val tone = Color(palette.start)
    val wash = if (LevyraIsLight) 0.26f else 0.40f
    Box(Modifier.fillMaxSize().background(homeCanvasColor(LevyraIsLight)).pointerInput(Unit) {}) {
        Box(
            Modifier.fillMaxWidth().height(DefaultCuratedBackdrop)
                .graphicsLayer { alpha = 1f - collapse * 0.65f }
                .background(Brush.verticalGradient(0f to tone.copy(alpha = wash), 0.55f to tone.copy(alpha = wash * 0.25f), 1f to Color.Transparent))
        )
        Column(Modifier.fillMaxSize()) {
            DefaultCuratedTopBar(page.title, { collapse }, onDismiss)
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val inset = LevyraHomeDesign.HorizontalInset
                val columns = curatedPageColumns(maxWidth, inset, DefaultCuratedGap, DefaultCuratedCellMin)
                val rows = remember(page.groups, columns) { curatedPageRows(page.groups, columns) }
                val chips = page.groups.size > 1
                val focused by rememberCuratedFocusedGroup(listState, rows) { stickyPx }
                val jump: (Int) -> Unit = { group ->
                    val header = curatedPageHeaderIndex(rows, group)
                    if (header >= 0) scope.launch {
                        val target = header + curatedLeadingItems(chips)
                        if (animated) listState.animateScrollToItem(target, -stickyPx) else listState.scrollToItem(target, -stickyPx)
                    }
                }
                CuratedGroupedList(
                    rows = rows, page = page, listState = listState, columns = columns,
                    gutter = inset, itemGap = DefaultCuratedGap, rowGap = DefaultCuratedRowGap,
                    contentPadding = PaddingValues(bottom = bottomInset + 24.dp),
                    intro = {
                        DefaultCuratedHero(
                            page,
                            Modifier.onSizeChanged { heroPx = it.height }.graphicsLayer {
                                val progress = collapse
                                alpha = 1f - progress
                                translationY = size.height * progress * 0.25f
                                scaleX = 1f - progress * 0.05f
                                scaleY = scaleX
                                transformOrigin = TransformOrigin(0f, 1f)
                            }
                        )
                    },
                    sticky = if (chips) {
                        { DefaultCuratedJumpChips(page, focused, { collapse }, Modifier.onSizeChanged { stickyPx = it.height }, jump) }
                    } else {
                        null
                    },
                    header = { DefaultCuratedGroupHeader(it) },
                    cell = { item, _, modifier -> DefaultCuratedPlaylistCard(item, Modifier, modifier) { item.playlist?.let(onOpen) } },
                    placeholder = {
                        CuratedSkeletonRow(
                            columns, inset, DefaultCuratedGap, RoundedCornerShape(28.dp), scheme.surfaceContainerHigh, scheme.primary,
                            Modifier.padding(top = 28.dp)
                        )
                    },
                    empty = { DefaultCuratedEmpty() }
                )
            }
        }
    }
}

@Composable
private fun DefaultCuratedTopBar(title: String, collapse: () -> Float, onDismiss: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val container = MaterialTheme.colorScheme.surfaceContainer
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().graphicsLayer { alpha = collapse() }.background(container))
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(DefaultCuratedBarHeight).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LevyraBackButton(onClick = onDismiss, contentDescription = strings.back)
            Text(
                title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = LevyraText,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).clearAndSetSemantics { }.graphicsLayer {
                    val progress = collapse()
                    alpha = progress
                    translationY = (1f - progress) * 10.dp.toPx()
                }
            )
        }
    }
}

@Composable
private fun DefaultCuratedHero(page: CuratedPageData, modifier: Modifier) {
    val strings = LocalLevyraStrings.current
    Column(modifier.fillMaxWidth().padding(horizontal = LevyraHomeDesign.HorizontalInset).padding(top = 4.dp, bottom = 18.dp)) {
        if (page.covers.isNotEmpty()) DefaultCuratedMosaic(page.covers, page.total)
        if (page.total > 0) {
            Text(
                "${page.total} · ${strings.playlistsPlain}".uppercase(), style = LevyraType.overline, color = MaterialTheme.colorScheme.primary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 22.dp)
            )
        }
        Text(
            page.title, style = LevyraType.heroTitle, color = LevyraText, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp).semantics { heading() }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultCuratedMosaic(covers: List<String>, total: Int) {
    val scheme = MaterialTheme.colorScheme
    val cookie = MaterialShapes.Cookie9Sided.toShape()
    val arch = MaterialShapes.Arch.toShape()
    val clover = MaterialShapes.Clover4Leaf.toShape()
    val sunny = MaterialShapes.Sunny.toShape()
    BoxWithConstraints(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
        val side = (maxWidth / DefaultCuratedMosaicRatio).coerceAtMost(DefaultCuratedMosaicMax)
        Box(Modifier.fillMaxWidth().height(side)) {
            DefaultMosaicTile(covers[0], cookie, side, 0.dp, 0.dp, 0)
            covers.getOrNull(1)?.let { DefaultMosaicTile(it, arch, side * 0.62f, side * 0.92f, 0.dp, 1) }
            covers.getOrNull(2)?.let { DefaultMosaicTile(it, clover, side * 0.56f, side * 1.24f, side * 0.44f, 2) }
            if (total > 0) {
                DefaultMosaicPiece(sunny, side * 0.4f, side * 0.74f, side * 0.6f, 3, scheme.primaryContainer) {
                    Text(
                        total.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = scheme.onPrimaryContainer, maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultMosaicTile(url: String, shape: Shape, size: Dp, x: Dp, y: Dp, order: Int) {
    DefaultMosaicPiece(shape, size, x, y, order, MaterialTheme.colorScheme.surfaceContainerHighest) {
        AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun DefaultMosaicPiece(shape: Shape, size: Dp, x: Dp, y: Dp, order: Int, container: Color, content: @Composable () -> Unit) {
    val animated = LocalAnimationsEnabled.current
    val reveal = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (reveal.value < 1f) {
            delay(order * DefaultCuratedStaggerMillis)
            reveal.animateTo(1f, spring(LevyraMotion.Springs.ExpressiveDamping, LevyraMotion.Springs.ExpressiveStiffness))
        }
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

@Composable
private fun DefaultCuratedJumpChips(page: CuratedPageData, selected: Int, collapse: () -> Float, modifier: Modifier, onJump: (Int) -> Unit) {
    val animated = LocalAnimationsEnabled.current
    val container = MaterialTheme.colorScheme.surfaceContainer
    val rowState = rememberLazyListState()
    LaunchedEffect(selected) {
        val target = (selected - 1).coerceAtLeast(0)
        if (animated) rowState.animateScrollToItem(target) else rowState.scrollToItem(target)
    }
    Box(modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().graphicsLayer { alpha = collapse() }.background(container))
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = LevyraHomeDesign.HorizontalInset),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).selectableGroup()
        ) {
            itemsIndexed(page.groups, key = { _, shelf -> "curated-jump-${shelf.key}" }, contentType = { _, _ -> "curated-jump" }) { index, shelf ->
                LevyraFilterPill(label = shelf.title, selected = index == selected, onClick = { onJump(index) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultCuratedGroupHeader(row: CuratedPageRow.Header) {
    val strings = LocalLevyraStrings.current
    val scheme = MaterialTheme.colorScheme
    val badge = when (row.group % 4) {
        0 -> MaterialShapes.Cookie9Sided
        1 -> MaterialShapes.Clover4Leaf
        2 -> MaterialShapes.Sunny
        else -> MaterialShapes.Arch
    }.toShape()
    val strapline = remember(row.shelf.strapline) { monoWithoutYoutube(row.shelf.strapline) }
    val count = row.shelf.items.size
    Row(
        Modifier.fillMaxWidth().padding(horizontal = LevyraHomeDesign.HorizontalInset).padding(top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(40.dp).graphicsLayer { shape = badge; clip = true }.background(scheme.secondaryContainer).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            Text((row.group + 1).toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSecondaryContainer)
        }
        LevyraSectionHeader(
            title = row.shelf.title,
            subtitle = if (strapline.isBlank()) "$count · ${strings.playlistsPlain}" else "$strapline · $count",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DefaultCuratedEmpty() {
    Text(
        LocalLevyraStrings.current.exploreEmpty, style = MaterialTheme.typography.bodyLarge, color = LevyraMuted, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp)
    )
}
