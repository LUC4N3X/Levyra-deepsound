package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.YoutubeMusicDecades
import com.luc4n3x.levyra.data.YoutubeMusicMoodPlaylist
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelfStyle
import com.luc4n3x.levyra.ui.i18n.CuratedCollectionsCopy
import com.luc4n3x.levyra.ui.i18n.curatedCollectionsCopy
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.viewmodel.CuratedBrowseViewModel
import com.luc4n3x.levyra.viewmodel.curatedBrowseKey
import com.luc4n3x.levyra.viewmodel.curatedMoodShelf

internal const val CuratedBrowseShelfKey = "curated-browse"
private const val CuratedBrowsePlaylists = 12

@Immutable
internal class CuratedBrowseGroup(val label: String, val categories: List<ExploreCategory>)

@Immutable
internal class CuratedBrowse(
    val groups: List<CuratedBrowseGroup>,
    val group: Int,
    val selected: ExploreCategory,
    val loading: Boolean,
    val onGroup: (Int) -> Unit,
    val onCategory: (ExploreCategory) -> Unit
)

@Immutable
internal class CuratedBrowseSection(val browse: CuratedBrowse, val shelf: YoutubeMusicHomeShelf)

internal fun curatedBrowseGroups(categories: List<ExploreCategory>, decades: List<ExploreCategory>, decadesLabel: String): List<CuratedBrowseGroup> {
    val sections = categories.asSequence()
        .filter { it.sectionIndex >= 0 && it.params != YoutubeMusicDecades.PARAMS && it.section.isNotBlank() }
        .groupBy { it.sectionIndex }
        .toSortedMap()
        .values
        .map { list -> CuratedBrowseGroup(list.first().section, list) }
    return sections + listOfNotNull(decades.takeIf { it.isNotEmpty() }?.let { CuratedBrowseGroup(decadesLabel, it) })
}

internal fun curatedBrowseSelection(groups: List<CuratedBrowseGroup>, params: String): Pair<Int, ExploreCategory>? {
    groups.forEachIndexed { index, group ->
        group.categories.firstOrNull { it.params == params }?.let { return index to it }
    }
    return groups.firstOrNull()?.categories?.firstOrNull()?.let { 0 to it }
}

internal fun curatedBrowseShelf(copy: CuratedCollectionsCopy, category: ExploreCategory, playlists: List<YoutubeMusicMoodPlaylist>): YoutubeMusicHomeShelf =
    YoutubeMusicHomeShelf(
        key = CuratedBrowseShelfKey,
        title = copy.browseTitle,
        strapline = copy.browseSubtitle,
        thumbnailUrl = "",
        style = YoutubeMusicHomeShelfStyle.Cards,
        items = curatedMoodShelf(category, playlists)?.items.orEmpty()
    )

internal fun curatedBrowseDistinct(shelf: YoutubeMusicHomeShelf, others: List<YoutubeMusicHomeShelf>): YoutubeMusicHomeShelf {
    val seen = others.flatMapTo(HashSet()) { other -> other.items.mapNotNull { it.playlist?.playlistId?.takeIf(String::isNotBlank) } }
    return shelf.copy(items = shelf.items.filter { it.playlist?.playlistId !in seen }.take(CuratedBrowsePlaylists))
}

@Composable
internal fun rememberCuratedBrowseSection(code: String): CuratedBrowseSection? {
    val model = viewModel<CuratedBrowseViewModel>(key = "curated-browse")
    val categories by model.categories.collectAsStateWithLifecycle()
    val playlists by model.playlists.collectAsStateWithLifecycle()
    val failed by model.failed.collectAsStateWithLifecycle()
    val selectedParams by model.selected.collectAsStateWithLifecycle()
    val previousParams by model.previous.collectAsStateWithLifecycle()
    val decades = rememberDecadeCategories(code)
    LaunchedEffect(code) { model.load(code) }
    val copy = remember(code) { curatedCollectionsCopy(code) }
    val providerCategories = categories[code].orEmpty()
    val groups = remember(providerCategories, decades, copy) { curatedBrowseGroups(providerCategories, decades, copy.decadesTitle) }
    val selection = remember(groups, selectedParams) { curatedBrowseSelection(groups, selectedParams) } ?: return null
    val (group, category) = selection
    LaunchedEffect(code, category.params) { model.request(code, category.params) }
    val key = curatedBrowseKey(code, category.params)
    val loaded = playlists[key]
    val loading = loaded == null && key !in failed
    val items = loaded ?: if (loading) playlists[curatedBrowseKey(code, previousParams)].orEmpty() else emptyList()
    return remember(groups, group, category, items, loading, copy, code) {
        CuratedBrowseSection(
            CuratedBrowse(
                groups = groups,
                group = group,
                selected = category,
                loading = loading,
                onGroup = { index -> groups.getOrNull(index)?.categories?.firstOrNull()?.let { model.select(code, it.params, category.params) } },
                onCategory = { picked -> model.select(code, picked.params, category.params) }
            ),
            curatedBrowseShelf(copy, category, items)
        )
    }
}

@Composable
internal fun CuratedBrowsePicker(
    browse: CuratedBrowse,
    gutter: Dp,
    accent: Brush,
    text: Color,
    muted: Color,
    modifier: Modifier = Modifier,
    tabFont: FontFamily? = null,
    pill: @Composable (label: String, selected: Boolean, onClick: () -> Unit) -> Unit
) {
    val categories = browse.groups.getOrNull(browse.group)?.categories.orEmpty()
    val rowState = rememberLazyListState()
    LaunchedEffect(browse.group) {
        val index = categories.indexOfFirst { it.params == browse.selected.params }.coerceAtLeast(0)
        rowState.scrollToItem((index - 1).coerceAtLeast(0))
    }
    Column(modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        if (browse.groups.size > 1) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = gutter).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                browse.groups.forEachIndexed { index, group ->
                    CuratedBrowseTab(group.label, index == browse.group, accent, text, muted, tabFont) { browse.onGroup(index) }
                }
            }
        }
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).selectableGroup()
        ) {
            items(categories, key = { "curated-browse-${it.params}" }, contentType = { "curated-browse-pill" }) { category ->
                pill(category.title, category.params == browse.selected.params) { browse.onCategory(category) }
            }
        }
    }
}

@Composable
private fun CuratedBrowseTab(label: String, selected: Boolean, accent: Brush, text: Color, muted: Color, font: FontFamily?, onClick: () -> Unit) {
    val animated = LocalAnimationsEnabled.current
    val indicator by animateDpAsState(
        if (selected) 20.dp else 0.dp,
        if (animated) spring(LevyraMotion.Springs.ExpressiveDamping, LevyraMotion.Springs.ExpressiveStiffness) else snap(),
        label = "curated-browse-tab"
    )
    Column(
        Modifier.heightIn(min = 48.dp).selectable(selected = selected, role = Role.Tab, onClick = onClick),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label, style = MaterialTheme.typography.labelLarge, fontFamily = font,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) text else muted,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Box(Modifier.padding(top = 4.dp).width(indicator).height(2.dp).clip(CircleShape).background(accent))
    }
}
