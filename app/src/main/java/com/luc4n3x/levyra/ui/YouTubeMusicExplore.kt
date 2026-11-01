package com.luc4n3x.levyra.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.levyraPressable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeMusicHomeButton
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelfStyle
import com.luc4n3x.levyra.ui.i18n.LevyraYouTubeMusicSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign
import com.luc4n3x.levyra.viewmodel.EXPLORE_BROWSE_ID
import com.luc4n3x.levyra.viewmodel.NEW_VIDEOS_BROWSE_ID
import com.luc4n3x.levyra.viewmodel.YouTubeMusicHomeViewModel

private const val MoodGridRows = 4

@Composable
internal fun YouTubeMusicExploreContent(
    listState: LazyListState,
    currentId: String?,
    bottomInset: Dp,
    actions: YouTubeMusicHomeActions,
    onOpenMood: (String, String) -> Unit,
    shortcuts: @Composable () -> Unit,
    levyraMix: @Composable () -> Unit = {}
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraYouTubeMusicSkinStrings.forCode(strings.code) }
    val exploreViewModel = viewModel<YouTubeMusicHomeViewModel>(key = "ytm-explore")
    val feed by exploreViewModel.state.collectAsStateWithLifecycle()
    val previews by exploreViewModel.previews.collectAsStateWithLifecycle()
    LaunchedEffect(strings.code) { exploreViewModel.load(strings.code, browseId = EXPLORE_BROWSE_ID) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxSize().background(YouTubeMusicDesign.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = statusBarTop, bottom = bottomInset + 16.dp),
            verticalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.sectionGap)
        ) {
            item(key = "ytm-explore-bar", contentType = "ytm-bar") {
                YouTubeMusicTopBar(
                    title = "Levyra", searchLabel = copy.search, settingsLabel = copy.settings,
                    onSearch = actions.openSearch, onSettings = actions.home.openSettings
                )
            }
            item(key = "ytm-explore-shortcuts", contentType = "ytm-shortcuts") { shortcuts() }
            item(key = "ytm-explore-levyra-mix", contentType = "ytm-levyra-mix") { levyraMix() }
            if (feed.shelves.isEmpty() && feed.failed) {
                item(key = "ytm-explore-failed", contentType = "ytm-notice") {
                    YouTubeMusicNotice(copy.homeUnavailable, copy.retry) { exploreViewModel.load(strings.code, force = true, browseId = EXPLORE_BROWSE_ID) }
                }
            }
            val shelves = feed.allShelves.filterNot { it.headerBrowseId == NEW_VIDEOS_BROWSE_ID }
            shelves.forEach { shelf ->
                item(key = "ytm-explore-${shelf.key}", contentType = "ytm-explore-${shelf.style}") {
                    when (shelf.style) {
                        YoutubeMusicHomeShelfStyle.Buttons -> YouTubeMusicMoodShelf(shelf, onOpenMood)
                        YoutubeMusicHomeShelfStyle.ListColumns -> YouTubeMusicRankedShelf(shelf, currentId, copy.playAll, copy.explicit, actions)
                        else -> YouTubeMusicFeedShelf(shelf, currentId, copy, actions, previews, exploreViewModel::requestPreview)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(statusBarTop).background(YouTubeMusicDesign.background))
    }
}

@Composable
private fun YouTubeMusicMoodShelf(shelf: YoutubeMusicHomeShelf, onOpenMood: (String, String) -> Unit) {
    Column {
        YouTubeMusicSectionHeader(shelf.title)
        Spacer(Modifier.height(8.dp))
        LazyHorizontalGrid(
            rows = GridCells.Fixed(MoodGridRows),
            modifier = Modifier.fillMaxWidth().height(56.dp * MoodGridRows + 12.dp * (MoodGridRows - 1)),
            contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shelf.buttons, key = { it.params }) { button ->
                YouTubeMusicMoodTile(button.title, button.stripe(), onClick = { onOpenMood(button.title, button.params) }, modifier = Modifier.width(176.dp))
            }
        }
    }
}

private fun YoutubeMusicHomeButton.stripe(): Color =
    if (stripeColor == 0L) YouTubeMusicDesign.textTertiary else Color(stripeColor.toInt())

@Composable
private fun YouTubeMusicRankedShelf(
    shelf: YoutubeMusicHomeShelf,
    currentId: String?,
    playAllLabel: String,
    explicitLabel: String,
    actions: YouTubeMusicHomeActions
) {
    val tracks = remember(shelf) { shelf.tracks }
    val columns = remember(shelf) { shelf.items.filter { it.track != null }.chunked(4) }
    val width = YouTubeMusicDesign.quickPickColumnWidth(youTubeMusicViewportWidth())
    Column {
        YouTubeMusicSectionHeader(shelf.title, actionLabel = playAllLabel.takeIf { tracks.isNotEmpty() }, onAction = { actions.home.playAll(tracks) })
        Spacer(Modifier.height(4.dp))
        LazyRow {
            items(columns.size, key = { columns[it].first().key }) { columnIndex ->
                Column(Modifier.width(width)) {
                    columns[columnIndex].forEachIndexed { rowIndex, item ->
                        val track = item.track ?: return@forEachIndexed
                        val rank = columnIndex * 4 + rowIndex + 1
                        YouTubeMusicSongRow(
                            track = track, subtitle = item.subtitle, current = track.id == currentId, explicitLabel = explicitLabel,
                            onClick = { actions.home.playFrom(tracks, track) }, onMore = { actions.home.onTrackActions(track) },
                            leading = { YouTubeMusicRankArtwork(rank, track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeMusicRankArtwork(rank: Int, track: Track) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            rank.toString(), color = YouTubeMusicDesign.text, style = MaterialTheme.typography.headlineMedium,
            fontSize = 30.sp, fontWeight = FontWeight.Light, modifier = Modifier.width(40.dp)
        )
        Box(Modifier.size(YouTubeMusicDesign.listArtwork).clip(YouTubeMusicDesign.thumbShape).background(YouTubeMusicDesign.placeholder)) {
            CoverImage(track, Modifier.fillMaxSize())
        }
    }
}

internal fun youTubeMusicExploreActions(
    viewModel: com.luc4n3x.levyra.viewmodel.ExploreViewModel,
    onTrackActions: (Track) -> Unit
): YouTubeMusicHomeActions = YouTubeMusicHomeActions(
    home = AppleMusicHomeActions(
        playFrom = { tracks, track -> viewModel.playFrom(tracks, track) },
        playAll = viewModel::playAll,
        openAlbum = viewModel::openAlbum,
        openArtist = viewModel::openArtistFromHit,
        openArtistByName = viewModel::openArtistByName,
        openCollection = {},
        openSettings = viewModel::openSettings,
        pickCountry = {},
        onTrackActions = onTrackActions
    ),
    openPlaylist = viewModel::openPlaylistHit,
    openSearch = viewModel::openSearch
)

@Composable
internal fun YouTubeMusicExploreScreen(
    viewModel: com.luc4n3x.levyra.viewmodel.ExploreViewModel,
    listState: LazyListState,
    currentId: String?,
    bottomInset: Dp,
    availableAnchors: Set<ExploreAnchor>,
    onShortcut: (ExploreShortcut) -> Unit,
    onOpenJam: () -> Unit,
    onLiveRadioOpenChange: (Boolean) -> Unit,
    onOpenMood: (String, String) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    YouTubeMusicExploreContent(
        listState = listState,
        currentId = currentId,
        bottomInset = bottomInset,
        actions = remember(viewModel, onTrackActions) { youTubeMusicExploreActions(viewModel, onTrackActions) },
        onOpenMood = onOpenMood,
        shortcuts = {
            ExplorePrimaryShortcuts(
                availableAnchors = availableAnchors,
                onSelect = onShortcut,
                onOpenJam = onOpenJam,
                onOpenRadio = { onLiveRadioOpenChange(true) },
                modifier = Modifier
            )
        },
        levyraMix = { YouTubeMusicLevyraMix(viewModel) }
    )
}

private class YouTubeMusicMixCard(val key: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val colors: List<Color>, val onClick: () -> Unit)

@Composable
private fun YouTubeMusicLevyraMix(viewModel: com.luc4n3x.levyra.viewmodel.ExploreViewModel) {
    val strings = LocalLevyraStrings.current
    val mixFlow = remember(viewModel) { viewModel.state.map { it.mixFamiliarity to it.mixLoading }.distinctUntilChanged() }
    val initialMix = remember(viewModel) { viewModel.state.value.let { it.mixFamiliarity to it.mixLoading } }
    val mix by mixFlow.collectAsStateWithLifecycle(initialMix)
    val (familiarity, loading) = mix
    val cards = remember(strings, viewModel) {
        listOf(
            YouTubeMusicMixCard("mix-for-you", strings.mixForYou, Icons.Rounded.GraphicEq, listOf(Color(0xFFB0124B), Color(0xFF3D0A2A))) {
                viewModel.startLevyraMix(com.luc4n3x.levyra.domain.LevyraMixKind.Personalized)
            },
            YouTubeMusicMixCard("mix-surprise", strings.surpriseMe, Icons.Rounded.Casino, listOf(Color(0xFF1E5BB8), Color(0xFF0B1F45))) {
                viewModel.startLevyraMix(com.luc4n3x.levyra.domain.LevyraMixKind.SurpriseMe)
            },
            YouTubeMusicMixCard("mix-your-sound", strings.yourSound, Icons.Rounded.Equalizer, listOf(Color(0xFF0E8A6B), Color(0xFF06302A)), viewModel::openYourSound),
            YouTubeMusicMixCard("mix-lab", strings.mixLab, Icons.Rounded.Tune, listOf(Color(0xFF8A5A0E), Color(0xFF33210A))) { viewModel.openMixLab() },
            YouTubeMusicMixCard(
                "mood-journey",
                com.luc4n3x.levyra.ui.i18n.smartMusicCopy(strings.code)[com.luc4n3x.levyra.ui.i18n.SmartMusicText.MoodTitle],
                Icons.Rounded.Timeline,
                listOf(Color(0xFF5B2A86), Color(0xFF1E0E33)),
                viewModel::openMoodJourney
            )
        )
    }
    Column {
        YouTubeMusicSectionHeader(strings.levyraMix, strapline = strings.mixCreate)
        Spacer(Modifier.height(8.dp))
        YouTubeMusicChipRow(
            listOf(strings.mixFamiliarLabel, strings.mixDiscoveryLabel),
            if (familiarity >= 0.5f) 0 else 1,
            onSelect = { index -> viewModel.setMixFamiliarity(if (index == 0) 0.8f else 0.2f) }
        )
        Spacer(Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
        ) {
            items(cards.size, key = { cards[it].key }) { index ->
                val card = cards[index]
                Column(
                    Modifier.width(YouTubeMusicDesign.cardArtwork)
                        .levyraPressable(onClick = card.onClick, enabled = !loading, role = Role.Button, pressedScale = 1f)
                ) {
                    Box(
                        Modifier.size(YouTubeMusicDesign.cardArtwork).clip(YouTubeMusicDesign.cardShape)
                            .background(Brush.linearGradient(card.colors)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (loading && index == 0) LevyraLoadingIndicator(color = Color.White)
                        else Icon(card.icon, null, tint = Color.White, modifier = Modifier.size(56.dp))
                        Text(
                            card.title, color = Color.White, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(card.title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(strings.levyraMix, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                }
            }
        }
    }
}
