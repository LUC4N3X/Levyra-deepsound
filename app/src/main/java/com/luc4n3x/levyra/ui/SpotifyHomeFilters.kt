package com.luc4n3x.levyra.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.HomeSectionId
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.SpotifyDesign

internal enum class SpotifyHomeFilter {
    All, Music, Following, Videos, Radio;

    val parent: SpotifyHomeFilter? get() = if (this == Following) Music else null

    fun accepts(id: HomeSectionId): Boolean = when (this) {
        All -> true
        Music -> id != HomeSectionId.FavoriteRadio && id != HomeSectionId.MusicVideos
        Following -> id == HomeSectionId.ReleaseRadar || id == HomeSectionId.SimilarArtists
        Videos -> id == HomeSectionId.MusicVideos
        Radio -> id == HomeSectionId.FavoriteRadio
    }

    companion object {
        fun available(order: List<HomeSectionId>): List<SpotifyHomeFilter> = buildList {
            add(All)
            add(Music)
            if (HomeSectionId.MusicVideos in order) add(Videos)
            if (HomeSectionId.FavoriteRadio in order) add(Radio)
        }

        fun from(name: String): SpotifyHomeFilter = entries.firstOrNull { it.name == name } ?: All

        fun selected(name: String, filters: List<SpotifyHomeFilter>): SpotifyHomeFilter =
            from(name).takeIf { it == Following || it in filters } ?: All
    }
}

private val SpotifyChipOverlap = 21.dp
private const val SpotifyChipRevealMillis = 300

@Composable
internal fun SpotifyHomeFilterBar(
    name: String,
    selected: SpotifyHomeFilter,
    filters: List<SpotifyHomeFilter>,
    onSelect: (SpotifyHomeFilter) -> Unit,
    onSettings: () -> Unit,
    onOpenZone: (ExploreZone) -> Unit,
    onRewind: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifySkinStrings.forCode(strings.code) }
    val zones = remember(strings) { ExploreCatalog.getZones(strings) }
    val animate = LocalAnimationsEnabled.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val parentIndex = filters.indexOf(selected.parent ?: selected).coerceAtLeast(0)
    LaunchedEffect(parentIndex) {
        val offset = with(density) { -32.dp.roundToPx() }
        if (parentIndex == 0) listState.animateScrollToItem(0)
        else if (animate) listState.animateScrollToItem(parentIndex, offset)
        else listState.scrollToItem(parentIndex, offset)
    }
    fun label(filter: SpotifyHomeFilter) = when (filter) {
        SpotifyHomeFilter.All -> copy.all
        SpotifyHomeFilter.Music -> copy.music
        SpotifyHomeFilter.Following -> copy.following
        SpotifyHomeFilter.Videos -> strings.video
        SpotifyHomeFilter.Radio -> copy.radio
    }
    Box(modifier.fillMaxWidth().background(SpotifyDesign.base).statusBarsPadding().height(SpotifyDesign.filterRowHeight)) {
        LazyRow(
            Modifier.fillMaxSize().padding(start = 40.dp).selectableGroup(),
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = SpotifyDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(filters, key = { "spotify-filter-${it.name}" }) { filter ->
                if (filter == SpotifyHomeFilter.Music) {
                    SpotifyFilterGroup(
                        parent = label(filter),
                        child = label(SpotifyHomeFilter.Following),
                        selected = selected,
                        animate = animate,
                        onSelect = onSelect
                    )
                } else {
                    SpotifyFilterChip(label(filter), selected == filter, onClick = { onSelect(if (selected == filter) SpotifyHomeFilter.All else filter) })
                }
            }
            items(zones, key = { "spotify-zone-${it.id}" }) { zone ->
                SpotifyFilterChip(zone.label, false, role = Role.Button, onClick = { onOpenZone(zone) })
            }
            item(key = "spotify-rewind") {
                SpotifyFilterChip("Rewind", false, role = Role.Button, onClick = onRewind)
            }
        }
        Box(
            Modifier.padding(start = 40.dp).width(12.dp).fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(SpotifyDesign.base, Color.Transparent)))
        )
        Box(
            Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(48.dp).clip(CircleShape)
                .levyraPressable(onClick = onSettings, onClickLabel = strings.settings, role = Role.Button),
            contentAlignment = Alignment.Center
        ) { SpotifyAvatar(name, 32.dp) }
    }
}

@Composable
private fun SpotifyFilterGroup(
    parent: String,
    child: String,
    selected: SpotifyHomeFilter,
    animate: Boolean,
    onSelect: (SpotifyHomeFilter) -> Unit
) {
    val expanded = selected == SpotifyHomeFilter.Music || selected == SpotifyHomeFilter.Following
    val reveal = tween<androidx.compose.ui.unit.IntSize>(if (animate) SpotifyChipRevealMillis else 0)
    Row(verticalAlignment = Alignment.CenterVertically) {
        SpotifyFilterChip(
            parent, expanded, modifier = Modifier.zIndex(1f),
            onClick = { onSelect(if (expanded) SpotifyHomeFilter.All else SpotifyHomeFilter.Music) }
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally(reveal, Alignment.Start),
            exit = shrinkHorizontally(reveal, Alignment.Start)
        ) {
            Row(
                Modifier
                    .layout { measurable, constraints ->
                        val overlap = SpotifyChipOverlap.roundToPx()
                        val placeable = measurable.measure(constraints)
                        layout((placeable.width - overlap).coerceAtLeast(0), placeable.height) { placeable.place(-overlap, 0) }
                    }
                    .height(SpotifyDesign.chipHeight).clip(CircleShape).background(SpotifyDesign.chip)
                    .padding(start = SpotifyChipOverlap, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpotifyFilterChip(
                    child, selected == SpotifyHomeFilter.Following, horizontalPadding = 12.dp, idle = Color.Transparent,
                    onClick = { onSelect(if (selected == SpotifyHomeFilter.Following) SpotifyHomeFilter.Music else SpotifyHomeFilter.Following) }
                )
            }
        }
    }
}

@Composable
private fun SpotifyFilterChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    role: Role = Role.Tab,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    idle: Color = SpotifyDesign.chip,
    onClick: () -> Unit
) {
    val animate = LocalAnimationsEnabled.current
    val spec = tween<Color>(if (animate) SpotifyChipRevealMillis else 0)
    val fill by animateColorAsState(if (selected) SpotifyDesign.green else idle, spec, label = "spotify-chip-fill")
    val ink by animateColorAsState(if (selected) Color.Black else SpotifyDesign.text, spec, label = "spotify-chip-ink")
    val interaction = remember { MutableInteractionSource() }
    val toggle = role == Role.Tab
    Box(
        modifier.minimumInteractiveComponentSize()
            .then(
                if (toggle) Modifier.selectable(selected = selected, interactionSource = interaction, indication = null, role = role, onClick = onClick)
                else Modifier.clickable(interactionSource = interaction, indication = null, role = role, onClick = onClick)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.height(SpotifyDesign.chipHeight).clip(CircleShape).indication(interaction, ripple())
                .background(fill).padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = ink, fontSize = 16.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
