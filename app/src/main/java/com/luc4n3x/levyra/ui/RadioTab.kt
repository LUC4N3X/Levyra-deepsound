package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.RadioTabCopy
import com.luc4n3x.levyra.ui.i18n.podcastCopy
import com.luc4n3x.levyra.ui.i18n.radioTabCopy
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsQobuz
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.ui.theme.LevyraSkinManagesLayout
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

internal val LevyraRadioReplacesSearch: Boolean
    get() = levyraSearchOwnsBrowse(LevyraIsSpotify, LevyraIsAppleMusic, LevyraIsAtelier || LevyraIsQobuz || LevyraIsYouTubeMusic)

internal fun levyraBarTabs(order: List<LevyraTab>, radioReplacesSearch: Boolean): List<LevyraTab> =
    if (radioReplacesSearch) order.filterNot { it == LevyraTab.Search || it == LevyraTab.Radio } + LevyraTab.Radio else order

internal fun navStudioRows(order: List<LevyraTab>, radioReplacesSearch: Boolean): List<LevyraTab> =
    if (radioReplacesSearch) order - LevyraTab.Search else order

internal fun navStudioOrder(full: List<LevyraTab>, rows: List<LevyraTab>, radioReplacesSearch: Boolean): List<LevyraTab> {
    val at = full.indexOf(LevyraTab.Search)
    if (!radioReplacesSearch || at < 0) return rows
    return rows.toMutableList().apply { add(at.coerceAtMost(size), LevyraTab.Search) }
}

@Composable
internal fun rememberLevyraBarEntries(order: List<LevyraTab>): List<LevyraTabEntry> {
    val all = rememberLevyraTabEntries()
    val radio = LevyraRadioReplacesSearch
    return remember(all, order, radio) {
        levyraBarTabs(order, radio).mapNotNull { tab -> all.firstOrNull { it.tab == tab } }
    }
}

internal fun radioTabEntry(code: String, nocturne: Boolean): LevyraTabEntry =
    LevyraTabEntry(LevyraTab.Radio, if (nocturne) Icons.Outlined.Radio else Icons.Rounded.Radio, Icons.Outlined.Radio, radioTabCopy(code).tab)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LevyraTopBarSearchAction(onClick: () -> Unit) {
    val label = LocalLevyraStrings.current.search
    if (LevyraSkinManagesLayout) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Search, label, tint = LevyraText) }
    } else {
        HomeHeaderIconButton(icon = Icons.Rounded.Search, contentDescription = label, polygon = MaterialShapes.Cookie4Sided, onClick = onClick)
    }
}

@Composable
internal fun LevyraTopLevelSearchRow(
    onSearch: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        content()
        if (onSearch != null) LevyraTopBarSearchAction(onSearch)
    }
}

@Composable
internal fun RadioTabScreen(viewModel: LevyraViewModel, state: LevyraUiState, backEnabled: Boolean) {
    val code = LocalLevyraStrings.current.code
    val copy = remember(code) { radioTabCopy(code) }
    val podcastsTitle = remember(code) { podcastCopy(code).title }
    val startRadioLabel = LocalLevyraStrings.current.startRadio
    val seeds = remember(state.mostPlayedTracks, state.recentListens) {
        exploreRadioSeeds(state.mostPlayedTracks, state.recentListens)
    }
    var podcastsOpen by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LiveRadioScreen(
            languageCode = state.languageCode,
            currentStationId = state.currentTrack?.takeIf { it.isLiveRadio() }?.id?.removePrefix("live-radio:"),
            isPlaying = state.isPlaying,
            backEnabled = false,
            onBack = {},
            onPlay = viewModel::playLiveRadio,
            tabTitle = copy.tab,
            onSearch = { viewModel.selectTab(LevyraTab.Search) },
            leadingItems = {
                radioTabLeadingItems(seeds, copy, podcastsTitle, startRadioLabel, viewModel::startSongRadioFrom) { podcastsOpen = true }
            }
        )
        if (podcastsOpen) {
            PodcastLibraryScreen(
                languageCode = state.languageCode,
                playback = PodcastPlayback(state.currentTrack?.id, state.isPlaying),
                bottomInset = exploreDestinationBottomInset(state),
                backEnabled = backEnabled,
                onBack = { podcastsOpen = false },
                onPlay = { list, track, position -> viewModel.playAudioFrom(list, track, startPositionMs = position) }
            )
        }
    }
}

private fun LazyListScope.radioTabLeadingItems(
    seeds: List<Track>,
    copy: RadioTabCopy,
    podcastsTitle: String,
    startRadioLabel: String,
    onStartRadio: (Track) -> Unit,
    onOpenPodcasts: () -> Unit
) {
    if (seeds.isNotEmpty()) {
        item(key = "radio-tab-for-you-title", contentType = "radio-tab-title") { RadioTabSectionHeader(copy.forYou, copy.forYouSubtitle) }
        item(key = "radio-tab-for-you", contentType = "radio-tab-for-you") { RadioForYouRail(seeds, startRadioLabel, onStartRadio) }
    }
    item(key = "radio-tab-podcasts", contentType = "radio-tab-podcasts") {
        RadioPodcastsEntry(podcastsTitle, copy.podcastsSubtitle, onOpenPodcasts)
    }
}

@Composable
private fun RadioTabSectionHeader(title: String, subtitle: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            title,
            color = colors.onSurface,
            fontSize = 22.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(22.sp),
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.semantics { heading() }
        )
        Text(subtitle, color = colors.onSurfaceVariant, fontSize = 13.sp, lineHeight = LevyraTypeRhythm.lineHeight(13.sp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RadioForYouRail(seeds: List<Track>, startRadioLabel: String, onStartRadio: (Track) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shapes = listOf(
        MaterialShapes.Cookie9Sided.toShape(),
        MaterialShapes.Clover4Leaf.toShape(),
        MaterialShapes.Cookie6Sided.toShape(),
        MaterialShapes.SoftBurst.toShape()
    )
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(seeds, key = { _, track -> "radio-seed-${track.artist.trim().lowercase()}" }) { index, track ->
            Column(
                Modifier
                    .width(148.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.surfaceContainer)
                    .levyraPressable(
                        onClick = { onStartRadio(track) },
                        role = Role.Button,
                        onClickLabel = startRadioLabel,
                        pressedScale = LevyraPressScale.Surface
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    CoverImage(track = track, modifier = Modifier.size(128.dp).clip(shapes[index % shapes.size]))
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(40.dp).clip(CircleShape).background(colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Radio, null, tint = colors.onPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Text(
                    track.artist,
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    track.title,
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RadioPodcastsEntry(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surfaceContainer)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Surface)
            .heightIn(min = 76.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(52.dp).clip(MaterialShapes.Puffy.toShape()).background(colors.secondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Podcasts, null, tint = colors.onSecondary, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = colors.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = colors.onSurfaceVariant, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.onSurfaceVariant)
    }
}
