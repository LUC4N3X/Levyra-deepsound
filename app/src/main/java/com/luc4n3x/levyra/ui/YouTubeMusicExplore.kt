package com.luc4n3x.levyra.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
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

@Composable
internal fun YouTubeMusicExploreContent(
    listState: LazyListState,
    bottomInset: Dp,
    actions: YouTubeMusicHomeActions,
    shortcuts: @Composable () -> Unit,
    editorial: ExploreEditorialContent?,
    moodJourney: @Composable () -> Unit = {}
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraYouTubeMusicSkinStrings.forCode(strings.code) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val dailyCharts = viewModel<com.luc4n3x.levyra.viewmodel.DailyTop100ViewModel>()
    val hasCityCharts by remember(dailyCharts) { dailyCharts.state.map { it.cities.isNotEmpty() }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(dailyCharts.state.value.cities.isNotEmpty())
    val rows = remember(editorial?.news, hasCityCharts) {
        val news = editorial?.news.orEmpty()
        buildList {
            if (news.isNotEmpty()) add(ExploreRow.NewsHero)
            add(ExploreRow.DailyTop100)
            if (hasCityCharts) add(ExploreRow.CityCharts)
            if (news.size > 1) add(ExploreRow.NewsList)
        }
    }
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
            item(key = "explore-mood-journey", contentType = "explore-mood-journey") { moodJourney() }
            if (editorial != null) {
                rows.forEach { row ->
                    item(key = "ytm-explore-${row.key}", contentType = "ytm-explore-${row.key}") { ExploreEditorialRow(row, editorial) }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(statusBarTop).background(YouTubeMusicDesign.background))
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
    editorial: ExploreEditorialContent?,
    onTrackActions: (Track) -> Unit
) {
    YouTubeMusicExploreContent(
        listState = listState,
        bottomInset = bottomInset,
        actions = remember(viewModel, onTrackActions) { youTubeMusicExploreActions(viewModel, onTrackActions) },
        editorial = editorial,
        shortcuts = {
            ExplorePrimaryShortcuts(
                availableAnchors = availableAnchors,
                onSelect = onShortcut,
                onOpenJam = onOpenJam,
                onOpenRadio = { onLiveRadioOpenChange(true) },
                modifier = Modifier
            )
        },
        moodJourney = {
            com.luc4n3x.levyra.ui.moodjourney.MoodJourneyEntryCard(
                onOpen = { preset -> if (preset == null) viewModel.openMoodJourney() else viewModel.openMoodJourney(preset) },
                modifier = Modifier.padding(horizontal = YouTubeMusicDesign.gutter)
            )
        }
    )
}
