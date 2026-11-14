package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.ui.components.levyraShimmer
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.viewmodel.DefaultCuratedCollectionsViewModel
import com.luc4n3x.levyra.viewmodel.HomeViewModel
import com.luc4n3x.levyra.viewmodel.withCuratedMoodShelves

private const val CuratedPageHomeViewModelKey = "levyra-home"
private const val CuratedPageCovers = 5
private const val CuratedPageMaxColumns = 6

internal val LocalCuratedPlaylistsOpener = staticCompositionLocalOf<(() -> Unit)?> { null }

internal class CuratedPageData(val title: String, val groups: List<YoutubeMusicHomeShelf>, val loading: Boolean) {
    val total: Int = groups.sumOf { it.items.size }
    val covers: List<String> = groups.asSequence().flatMap { it.items }.map { it.thumbnailUrl }
        .filter(String::isNotBlank).distinct().take(CuratedPageCovers).toList()
    val empty: Boolean get() = groups.isEmpty() && !loading
}

internal sealed interface CuratedPageRow {
    val key: String
    val group: Int
    data class Header(override val key: String, override val group: Int, val shelf: YoutubeMusicHomeShelf) : CuratedPageRow
    data class Cells(override val key: String, override val group: Int, val offset: Int, val items: List<YoutubeMusicHomeItem>) : CuratedPageRow
}

internal fun curatedPageRows(groups: List<YoutubeMusicHomeShelf>, columns: Int): List<CuratedPageRow> {
    val span = columns.coerceAtLeast(1)
    return groups.flatMapIndexed { index, shelf ->
        listOf(CuratedPageRow.Header("curated-page-head-${shelf.key}", index, shelf)) +
            shelf.items.chunked(span).mapIndexed { row, items ->
                CuratedPageRow.Cells("curated-page-row-${shelf.key}-$row-$span", index, row * span, items)
            }
    }
}

internal fun curatedPageColumns(width: Dp, gutter: Dp, gap: Dp, cellMin: Dp): Int =
    ((width - gutter * 2 + gap) / (cellMin + gap)).toInt().coerceIn(2, CuratedPageMaxColumns)

internal fun curatedPageHeaderIndex(rows: List<CuratedPageRow>, group: Int): Int =
    rows.indexOfFirst { it is CuratedPageRow.Header && it.group == group }

internal fun curatedFocusedGroup(visible: List<Triple<Any, Int, Int>>, threshold: Int, groupOf: Map<Any, Int>): Int {
    val anchor = visible.firstOrNull { (key, offset, size) -> key in groupOf && offset + size > threshold } ?: return 0
    return groupOf.getValue(anchor.first)
}

@Composable
internal fun rememberCuratedFocusedGroup(listState: LazyListState, rows: List<CuratedPageRow>, threshold: () -> Int): State<Int> {
    val groupOf = remember(rows) { rows.associate { row -> row.key as Any to row.group } }
    return remember(listState, groupOf) {
        derivedStateOf {
            val visible = listState.layoutInfo.visibleItemsInfo.map { Triple(it.key, it.offset, it.size) }
            curatedFocusedGroup(visible, threshold(), groupOf)
        }
    }
}

@Composable
internal fun CuratedPlaylistsHost(
    factory: ViewModelProvider.Factory,
    bottomInset: Dp,
    backEnabled: Boolean,
    content: @Composable () -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val opener = remember { { open = true } }
    Box(Modifier.fillMaxSize()) {
        Box(if (open) Modifier.fillMaxSize().clearAndSetSemantics { } else Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalCuratedPlaylistsOpener provides opener, content = content)
        }
        val animated = LocalAnimationsEnabled.current
        AnimatedVisibility(visible = open, enter = curatedPageEnter(animated), exit = curatedPageExit(animated)) {
            BackHandler(enabled = backEnabled && open) { open = false }
            val home: HomeViewModel = viewModel(key = CuratedPageHomeViewModelKey, factory = factory)
            CuratedPlaylistsPage(rememberCuratedPageData(), home::openPlaylistHit, { open = false }, bottomInset)
        }
    }
}

@Composable
private fun rememberCuratedPageData(): CuratedPageData {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoHomeStrings.forCode(strings.code) }
    val curated = rememberCuratedPlaylistShelves(strings.code)
    val moods = rememberCuratedMoodShelves(strings.code)
    val collections = rememberDefaultCuratedCollectionShelves(strings.code)
    val collectionsModel = viewModel<DefaultCuratedCollectionsViewModel>(key = "default-curated-collections")
    val collectionsLoading by collectionsModel.loading.collectAsStateWithLifecycle()
    val loading = collectionsLoading && collections.size < 2
    return remember(curated, moods, collections, loading, copy) {
        val trailing = withCuratedMoodShelves(curated + collections, moods).drop(curated.size)
        CuratedPageData(copy.mixedForYou, glacierCuratedGroups(curated, trailing, copy.rotationOverline), loading)
    }
}

@Composable
private fun CuratedPlaylistsPage(page: CuratedPageData, onOpen: (PlaylistHit) -> Unit, onDismiss: () -> Unit, bottomInset: Dp) {
    when {
        LevyraIsNocturne -> NocturneCuratedPlaylistsPage(page, onOpen, onDismiss, bottomInset)
        LevyraIsPrismOnly -> PrismCuratedPlaylistsPage(page, onOpen, onDismiss, bottomInset)
        LevyraIsTorque -> TorqueCuratedPlaylistsPage(page, onOpen, onDismiss, bottomInset)
        LevyraIsIvory -> IvoryCuratedPlaylistsPage(page, onOpen, onDismiss, bottomInset)
        LevyraIsAtelier -> AtelierCuratedPlaylistsPage(page, onOpen, onDismiss, bottomInset)
        com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic -> YouTubeMusicCuratedPlaylistsPage(page, onOpen, onDismiss, bottomInset)
        else -> DefaultCuratedPlaylistsScreen(page, onOpen, onDismiss, bottomInset)
    }
}

private fun curatedPageEnter(animated: Boolean): EnterTransition = when {
    !animated -> EnterTransition.None
    LevyraIsTorque -> slideInHorizontally(tween(LevyraMotion.Durations.Medium, easing = LevyraMotion.Easings.Emphasized)) { it / 5 } +
        fadeIn(tween(LevyraMotion.Durations.Short))
    LevyraIsIvory -> LevyraMotion.sheetEnter(true)
    LevyraIsPrismOnly -> LevyraMotion.panelEnter(true)
    LevyraIsAtelier || LevyraIsNocturne -> LevyraMotion.overlayEnter(true)
    else -> LevyraMotion.pageEnter(true)
}

private fun curatedPageExit(animated: Boolean): ExitTransition = when {
    !animated -> ExitTransition.None
    LevyraIsTorque -> slideOutHorizontally(tween(LevyraMotion.Durations.Short, easing = LevyraMotion.Easings.Accelerate)) { it / 8 } +
        fadeOut(tween(LevyraMotion.Durations.Short))
    LevyraIsIvory -> LevyraMotion.sheetExit(true)
    LevyraIsPrismOnly -> LevyraMotion.panelExit(true)
    LevyraIsAtelier || LevyraIsNocturne -> LevyraMotion.overlayExit(true)
    else -> LevyraMotion.pageExit(true)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun CuratedGroupedList(
    rows: List<CuratedPageRow>,
    page: CuratedPageData,
    listState: LazyListState,
    columns: Int,
    gutter: Dp,
    itemGap: Dp,
    rowGap: Dp,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    intro: @Composable () -> Unit,
    sticky: (@Composable () -> Unit)? = null,
    header: @Composable (CuratedPageRow.Header) -> Unit,
    cell: @Composable (item: YoutubeMusicHomeItem, number: Int, modifier: Modifier) -> Unit,
    placeholder: @Composable () -> Unit,
    empty: @Composable () -> Unit
) {
    LazyColumn(state = listState, modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "curated-page-intro", contentType = "curated-page-intro") { intro() }
        if (sticky != null) stickyHeader(key = "curated-page-sticky", contentType = "curated-page-sticky") { sticky() }
        curatedRowItems(rows, columns, gutter, itemGap, rowGap, header, cell)
        when {
            page.loading -> item(key = "curated-page-loading", contentType = "curated-page-loading") { placeholder() }
            page.empty -> item(key = "curated-page-empty", contentType = "curated-page-empty") { empty() }
        }
    }
}

internal fun curatedLeadingItems(sticky: Boolean): Int = if (sticky) 2 else 1

private fun LazyListScope.curatedRowItems(
    rows: List<CuratedPageRow>,
    columns: Int,
    gutter: Dp,
    itemGap: Dp,
    rowGap: Dp,
    header: @Composable (CuratedPageRow.Header) -> Unit,
    cell: @Composable (item: YoutubeMusicHomeItem, number: Int, modifier: Modifier) -> Unit
) {
    rows.forEach { row ->
        when (row) {
            is CuratedPageRow.Header -> item(key = row.key, contentType = "curated-page-head") { header(row) }
            is CuratedPageRow.Cells -> item(key = row.key, contentType = "curated-page-cells") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = gutter).padding(bottom = rowGap),
                    horizontalArrangement = Arrangement.spacedBy(itemGap)
                ) {
                    row.items.forEachIndexed { index, item -> cell(item, row.offset + index + 1, Modifier.weight(1f)) }
                    repeat(columns - row.items.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
internal fun CuratedSkeletonRow(columns: Int, gutter: Dp, gap: Dp, shape: Shape, base: Color, accent: Color? = null, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = gutter).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        repeat(columns) {
            Box(Modifier.weight(1f).aspectRatio(1f).clip(shape).background(base).levyraShimmer(accent = accent))
        }
    }
}
