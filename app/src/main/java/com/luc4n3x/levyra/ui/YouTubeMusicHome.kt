package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.HomeBlock
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItemKind
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelfStyle
import com.luc4n3x.levyra.ui.i18n.LevyraYouTubeMusicSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.YouTubeMusicSkinCopy
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign
import com.luc4n3x.levyra.viewmodel.YouTubeMusicHomeViewModel

internal class YouTubeMusicHomeActions(
    val home: AppleMusicHomeActions,
    val openPlaylist: (PlaylistHit) -> Unit,
    val openSearch: () -> Unit
)

private typealias YouTubeMusicHomeBlock = Pair<String, @Composable () -> Unit>

private const val YTM_CHIPS_KEY = "ytm-home-chips"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun YouTubeMusicHomeContent(
    data: AppleMusicHomeData,
    actions: YouTubeMusicHomeActions,
    homeListState: LazyListState,
    homeBottomInset: Dp,
    homeRefreshing: Boolean,
    homePullState: PullToRefreshState,
    onPullRefresh: () -> Unit,
    composition: ComposedHomeRequest? = null
) {
    val homeRadio = LocalHomeRadio.current
    val strings = LocalLevyraStrings.current
    val showsFeed = composition?.showsSkinFeed != false
    val copy = remember(strings.code) { LevyraYouTubeMusicSkinStrings.forCode(strings.code) }
    val homeViewModel = viewModel<YouTubeMusicHomeViewModel>()
    val feed by homeViewModel.state.collectAsStateWithLifecycle()
    val previews by homeViewModel.previews.collectAsStateWithLifecycle()
    val allShelves = remember(feed.shelves, feed.discovery) { feed.allShelves }
    LaunchedEffect(strings.code, showsFeed) { if (showsFeed) homeViewModel.load(strings.code) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val levyraBlocks = if (composition == null) youTubeMusicLevyraBlocks(data, actions.home, copy, strings) else emptyList()
    val seed = remember(feed.shelves, showsFeed, data.personalTracks, data.charts) {
        val lead = if (showsFeed) {
            feed.shelves.firstNotNullOfOrNull { it.tracks.firstOrNull() }
        } else {
            data.personalTracks.firstOrNull() ?: data.charts.firstOrNull()
        }
        lead?.accentStart?.let(::Color)
    }
    val composedFeedEndKey = remember(allShelves, composition == null) {
        if (composition == null) null else youTubeMusicLeadingOrder(allShelves).lastOrNull()?.let { "ytm-${it.key}" }
    }
    val nearEnd by remember(homeListState, composedFeedEndKey) {
        derivedStateOf {
            val info = homeListState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            (info.totalItemsCount > 0 && last >= info.totalItemsCount - 3) ||
                (composedFeedEndKey != null && info.visibleItemsInfo.any { it.key == composedFeedEndKey })
        }
    }
    LaunchedEffect(nearEnd, feed.canLoadMore, showsFeed) { if (showsFeed && nearEnd && feed.canLoadMore) homeViewModel.loadMore() }
    val composedKit = remember(copy, data.countryLabel, actions.home) { YouTubeMusicComposedKit(copy, data.countryLabel, actions.home.pickCountry) }
    Box(Modifier.fillMaxSize().background(YouTubeMusicDesign.background)) {
        YouTubeMusicHomeBackdrop(
            seed, height = statusBarTop + 420.dp,
            modifier = Modifier.graphicsLayer {
                val first = homeListState.firstVisibleItemIndex == 0
                translationY = if (first) -homeListState.firstVisibleItemScrollOffset.toFloat() else -size.height
            }
        )
        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize().pullToRefresh(isRefreshing = homeRefreshing, state = homePullState, onRefresh = {
                onPullRefresh()
                if (showsFeed) homeViewModel.load(strings.code, force = true)
            }),
            contentPadding = PaddingValues(top = statusBarTop, bottom = homeBottomInset + 16.dp),
            verticalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.sectionGap)
        ) {
            if (composition == null) {
                youTubeMusicHomeHeaders(
                    copy, actions, feed.chips, feed.selectedChip, homeListState, statusBarTop,
                    homeViewModel::selectChip
                )
                youTubeMusicHomeContentItems(
                    data, actions, copy, strings.personalOrbitTitle, feed.failed, feed.shelves.isNotEmpty(),
                    allShelves, levyraBlocks, previews, homeViewModel::requestPreview, homeRadio
                ) { homeViewModel.load(strings.code, force = true) }
            } else {
                youTubeMusicHomeHeaders(copy, actions, emptyList(), "", homeListState, statusBarTop, homeViewModel::selectChip)
                composedHomeSections(composition, composedKit) {
                    youTubeMusicComposedFeedChips(
                        feed.chips, feed.selectedChip, homeListState, statusBarTop,
                        sticky = composition.blocks.lastOrNull() == HomeBlock.SkinFeed,
                        onSelectChip = homeViewModel::selectChip
                    )
                    if (feed.shelves.isEmpty() && feed.failed) {
                        item(key = "ytm-home-failed", contentType = "ytm-notice") {
                            YouTubeMusicNotice(copy.homeUnavailable, copy.retry) { homeViewModel.load(strings.code, force = true) }
                        }
                    }
                    if (feed.shelves.isEmpty() && feed.loading) {
                        item(key = "ytm-home-loading", contentType = "composed-loading") { composedKit.LoadingShelf("") }
                    }
                    youTubeMusicHomeSections(allShelves, emptyList()) { shelf ->
                        YouTubeMusicFeedShelf(shelf, data.currentId, copy, actions, previews, homeViewModel::requestPreview)
                    }
                }
            }
        }
        YouTubeMusicStatusBarScrim(homeListState, statusBarTop, composition == null && feed.chips.isNotEmpty())
        PullToRefreshDefaults.LoadingIndicator(
            state = homePullState,
            isRefreshing = homeRefreshing,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarTop + 16.dp)
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun LazyListScope.youTubeMusicHomeHeaders(
    copy: YouTubeMusicSkinCopy,
    actions: YouTubeMusicHomeActions,
    chips: List<com.luc4n3x.levyra.domain.YoutubeMusicHomeChip>,
    selectedChip: String,
    homeListState: LazyListState,
    statusBarTop: Dp,
    onSelectChip: (String) -> Unit
) {
    item(key = "ytm-home-bar", contentType = "ytm-bar") {
        YouTubeMusicTopBar(
            title = "Levyra", searchLabel = copy.search, settingsLabel = copy.settings,
            onSearch = actions.openSearch, onSettings = actions.home.openSettings,
            trailing = { com.luc4n3x.levyra.ui.rewind.RewindEntry() }
        )
    }
    if (chips.isNotEmpty()) {
        stickyHeader(key = YTM_CHIPS_KEY, contentType = "ytm-chips") {
            val pinned by remember(homeListState) { derivedStateOf { homeListState.firstVisibleItemIndex >= 1 } }
            YouTubeMusicChipsStickyHeader(chips, selectedChip, pinned, statusBarTop, onSelectChip)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun LazyListScope.youTubeMusicComposedFeedChips(
    chips: List<com.luc4n3x.levyra.domain.YoutubeMusicHomeChip>,
    selectedChip: String,
    homeListState: LazyListState,
    statusBarTop: Dp,
    sticky: Boolean,
    onSelectChip: (String) -> Unit
) {
    if (chips.isEmpty()) return
    if (sticky) {
        stickyHeader(key = YTM_CHIPS_KEY, contentType = "ytm-chips") {
            val pinned by remember(homeListState) {
                derivedStateOf {
                    homeListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == YTM_CHIPS_KEY }
                        ?.let { homeListState.firstVisibleItemIndex >= it.index } == true
                }
            }
            YouTubeMusicChipsStickyHeader(chips, selectedChip, pinned, statusBarTop, onSelectChip)
        }
    } else {
        item(key = YTM_CHIPS_KEY, contentType = "ytm-chips") {
            YouTubeMusicChipsStickyHeader(chips, selectedChip, false, statusBarTop, onSelectChip)
        }
    }
}

@Composable
private fun YouTubeMusicChipsStickyHeader(
    chips: List<com.luc4n3x.levyra.domain.YoutubeMusicHomeChip>,
    selectedChip: String,
    pinned: Boolean,
    statusBarTop: Dp,
    onSelectChip: (String) -> Unit
) {
    val labels = remember(chips) { chips.map { it.title } }
    Box(
        Modifier.fillMaxWidth()
            .background(if (pinned) YouTubeMusicDesign.background else Color.Transparent)
            .padding(top = if (pinned) statusBarTop else 0.dp, bottom = 8.dp)
    ) {
        YouTubeMusicChipRow(labels, chips.indexOfFirst { it.params == selectedChip }, onSelect = { index ->
            chips.getOrNull(index)?.let { onSelectChip(it.params) }
        })
    }
}

private fun LazyListScope.youTubeMusicHomeContentItems(
    data: AppleMusicHomeData,
    actions: YouTubeMusicHomeActions,
    copy: YouTubeMusicSkinCopy,
    orbitTitle: String,
    feedFailed: Boolean,
    hasShelves: Boolean,
    allShelves: List<YoutubeMusicHomeShelf>,
    levyraBlocks: List<YouTubeMusicHomeBlock>,
    previews: Map<String, List<Track>>,
    onRequestPreview: (String) -> Unit,
    homeRadio: HomeRadioState,
    onRetry: () -> Unit
) {
    if (!hasShelves && feedFailed) {
        item(key = "ytm-home-failed", contentType = "ytm-notice") {
            YouTubeMusicNotice(copy.homeUnavailable, copy.retry, onRetry)
        }
    }
    val orbit = data.personalTracks.takeIf { data.showPersonal }.orEmpty()
    if (orbit.isNotEmpty()) {
        item(key = "levyra-orbit", contentType = "ytm-speed-grid") {
            YouTubeMusicSpeedGrid(
                orbitTitle, data.listener, orbit, data.currentId,
                onPlay = { track -> actions.home.playFrom(orbit, track) },
                onTrackActions = actions.home.onTrackActions
            )
        }
    }
    favoriteRadioSection(homeRadio)
    youTubeMusicHomeSections(allShelves, levyraBlocks) { shelf ->
        YouTubeMusicFeedShelf(shelf, data.currentId, copy, actions, previews, onRequestPreview)
    }
}

@Composable
private fun YouTubeMusicStatusBarScrim(homeListState: LazyListState, statusBarTop: Dp, hasChips: Boolean) {
    val scrolled by remember(homeListState) { derivedStateOf { homeListState.firstVisibleItemIndex > 0 || homeListState.firstVisibleItemScrollOffset > 0 } }
    if (scrolled && !hasChips) {
        Box(Modifier.fillMaxWidth().height(statusBarTop).background(YouTubeMusicDesign.background))
    }
}

private fun LazyListScope.youTubeMusicHomeSections(
    shelves: List<YoutubeMusicHomeShelf>,
    levyraBlocks: List<YouTubeMusicHomeBlock>,
    shelfContent: @Composable (YoutubeMusicHomeShelf) -> Unit
) {
    var nextBlock = 0
    youTubeMusicLeadingOrder(shelves).forEachIndexed { index, shelf ->
        item(key = "ytm-${shelf.key}", contentType = "ytm-${shelf.style}") { shelfContent(shelf) }
        if (index % 2 == 1 && nextBlock < levyraBlocks.size) {
            val (key, block) = levyraBlocks[nextBlock++]
            item(key = key, contentType = key) { block() }
        }
    }
    levyraBlocks.drop(nextBlock).forEach { (key, block) -> item(key = key, contentType = key) { block() } }
}

private fun youTubeMusicLeadingOrder(shelves: List<YoutubeMusicHomeShelf>): List<YoutubeMusicHomeShelf> {
    if (shelves.size < 3) return shelves
    val first = shelves.first()
    val feature = shelves.drop(1).firstOrNull { it.style == YoutubeMusicHomeShelfStyle.Cards || it.style == YoutubeMusicHomeShelfStyle.Videos }
        ?: return shelves
    return listOf(first, feature) + shelves.filter { it !== first && it !== feature }
}

@Composable
internal fun YouTubeMusicFeedShelf(
    shelf: YoutubeMusicHomeShelf,
    currentId: String?,
    copy: YouTubeMusicSkinCopy,
    actions: YouTubeMusicHomeActions,
    previews: Map<String, List<Track>>,
    requestPreview: (String) -> Unit
) {
    val tracks = remember(shelf) { shelf.tracks }
    val kinds = remember(shelf) { shelf.items.mapTo(HashSet()) { it.kind } }
    val artistShelf = shelf.headerBrowseId.isNotBlank() && shelf.thumbnailUrl.isNotBlank()
    val playable = shelf.style == YoutubeMusicHomeShelfStyle.ListColumns || shelf.style == YoutubeMusicHomeShelfStyle.Videos ||
        kinds == setOf(YoutubeMusicHomeItemKind.Song)
    Column {
        YouTubeMusicSectionHeader(
            title = shelf.title,
            strapline = shelf.strapline,
            thumbnailUrl = shelf.thumbnailUrl,
            actionLabel = if (playable && tracks.isNotEmpty() && !artistShelf) copy.playAll else null,
            onAction = { actions.home.playAll(tracks) },
            onOpen = if (artistShelf) {
                { actions.home.openArtist(ArtistHit(shelf.title, "", shelf.thumbnailUrl, 0, 0, shelf.headerBrowseId)) }
            } else null
        )
        Spacer(Modifier.height(8.dp))
        when {
            shelf.style == YoutubeMusicHomeShelfStyle.ListColumns -> YouTubeMusicSongColumns(
                remember(shelf) {
                    shelf.items.mapNotNull { item ->
                        val track = item.track ?: return@mapNotNull null
                        YouTubeMusicSongRowModel(item.key, track, item.subtitle, { actions.home.playFrom(tracks, track) }, { actions.home.onTrackActions(track) })
                    }
                },
                currentId, copy.explicit
            )
            kinds == setOf(YoutubeMusicHomeItemKind.Playlist) && !artistShelf -> YouTubeMusicCommunityCarousel(
                shelf.items, previews, copy.explicit, Triple(copy.playAll, copy.radio, copy.save),
                YouTubeMusicCommunityActions(
                    requestPreview = requestPreview,
                    play = { list, from -> (from ?: list.firstOrNull())?.let { actions.home.playFrom(list, it) } },
                    radio = { list -> list.shuffled().let { shuffled -> shuffled.firstOrNull()?.let { actions.home.playFrom(shuffled, it) } } },
                    open = { item -> item.playlist?.let(actions.openPlaylist) },
                    trackActions = actions.home.onTrackActions
                )
            )
            kinds == setOf(YoutubeMusicHomeItemKind.Song) -> YouTubeMusicDiscoveryCarousel(
                remember(shelf) { shelf.items.map { it.toDiscoveryCard() } },
                onOpen = { track -> actions.home.playFrom(tracks, track) },
                onLongClick = actions.home.onTrackActions
            )
            else -> {
                val cardWidth = if (artistShelf) 200.dp else YouTubeMusicDesign.cardArtwork
                val cards = remember(shelf) { shelf.items.map { item -> item.toCard(tracks, actions) } }
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
                    horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
                ) {
                    items(cards.size, key = { cards[it].key }) { index ->
                        val card = cards[index]
                        YouTubeMusicCard(card, width = if (card.wide) 300.dp else cardWidth)
                    }
                }
            }
        }
    }
}

private fun YoutubeMusicHomeItem.toCard(siblings: List<Track>, actions: YouTubeMusicHomeActions): YouTubeMusicCardModel {
    val onClick: () -> Unit = when (kind) {
        YoutubeMusicHomeItemKind.Song, YoutubeMusicHomeItemKind.Video -> {
            { track?.let { actions.home.playFrom(siblings.ifEmpty { listOf(it) }, it) } }
        }
        YoutubeMusicHomeItemKind.Album -> { { album?.let(actions.home.openAlbum) } }
        YoutubeMusicHomeItemKind.Playlist -> { { playlist?.let(actions.openPlaylist) } }
        YoutubeMusicHomeItemKind.Artist -> { { artist?.let(actions.home.openArtist) } }
    }
    return YouTubeMusicCardModel(
        key = key, title = title, subtitle = subtitle, artworkUrl = thumbnailUrl,
        circular = kind == YoutubeMusicHomeItemKind.Artist,
        wide = kind == YoutubeMusicHomeItemKind.Video,
        track = track,
        onClick = onClick,
        onLongClick = track?.let { song -> { actions.home.onTrackActions(song) } }
    )
}

private fun youTubeMusicLevyraBlocks(
    data: AppleMusicHomeData,
    actions: AppleMusicHomeActions,
    copy: YouTubeMusicSkinCopy,
    strings: com.luc4n3x.levyra.ui.i18n.LevyraStrings
): List<YouTubeMusicHomeBlock> = listOfNotNull(
    chartBlock(data, copy, actions),
    dailyDiscoveryBlock(data, copy, actions),
    cardsBlock("levyra-albums", strings.albumsForYou, data.albums.takeIf { data.showAlbums }.orEmpty().map { album ->
        YouTubeMusicCardModel("album-${album.browseId.ifBlank { album.title + album.artist }}", album.title, album.artist, album.thumbnailUrl, onClick = { actions.openAlbum(album) })
    }),
    cardsBlock("levyra-artists", strings.trendingArtists, data.artists.takeIf { data.showArtists }.orEmpty().map { artist ->
        YouTubeMusicCardModel("artist-${artist.browseId.ifBlank { artist.name }}", artist.name, artist.subscribers, artist.thumbnailUrl, circular = true, onClick = { actions.openArtist(artist) })
    }),
    songColumnsBlock("levyra-forgotten", strings.forgottenFavorites, data.forgottenFavorites, data.currentId, copy, actions)
)

private fun songColumnsBlock(
    key: String,
    title: String,
    tracks: List<Track>,
    currentId: String?,
    copy: YouTubeMusicSkinCopy,
    actions: AppleMusicHomeActions
): YouTubeMusicHomeBlock? {
    if (tracks.isEmpty() || title.isBlank()) return null
    return key to @Composable {
        Column {
            YouTubeMusicSectionHeader(title, actionLabel = copy.playAll, onAction = { actions.playAll(tracks) })
            Spacer(Modifier.height(4.dp))
            YouTubeMusicSongColumns(
                remember(tracks) {
                    tracks.map { track -> YouTubeMusicSongRowModel("$key-${track.id}", track, track.artist, { actions.playFrom(tracks, track) }, { actions.onTrackActions(track) }) }
                },
                currentId, copy.explicit
            )
        }
    }
}

private fun chartBlock(data: AppleMusicHomeData, copy: YouTubeMusicSkinCopy, actions: AppleMusicHomeActions): YouTubeMusicHomeBlock? {
    if (data.charts.isEmpty() || data.chartsTitle.isBlank()) return null
    return "levyra-top50" to @Composable {
        Column {
            YouTubeMusicSectionHeader(
                data.chartsTitle,
                actionLabel = data.countryLabel.ifBlank { null }?.let { "$it ▾" },
                onAction = actions.pickCountry
            )
            Spacer(Modifier.height(4.dp))
            YouTubeMusicSongColumns(
                remember(data.charts) {
                    data.charts.mapIndexed { index, track ->
                        YouTubeMusicSongRowModel("top50-${track.id}", track, "${index + 1} • ${track.artist}", { actions.playFrom(data.charts, track) }, { actions.onTrackActions(track) })
                    }
                },
                data.currentId, copy.explicit
            )
        }
    }
}

private fun cardsBlock(key: String, title: String, cards: List<YouTubeMusicCardModel>): YouTubeMusicHomeBlock? {
    if (cards.isEmpty()) return null
    return key to @Composable {
        Column {
            YouTubeMusicSectionHeader(title)
            Spacer(Modifier.height(8.dp))
            YouTubeMusicCardCarousel(cards)
        }
    }
}

private fun dailyDiscoveryBlock(data: AppleMusicHomeData, copy: YouTubeMusicSkinCopy, actions: AppleMusicHomeActions): YouTubeMusicHomeBlock? {
    val tracks = data.quickPicks?.tracks.orEmpty().filter { it.id != data.currentId }.take(DAILY_DISCOVERY_SIZE)
    if (tracks.isEmpty()) return null
    return "levyra-daily-discovery" to @Composable {
        Column {
            YouTubeMusicSectionHeader(copy.dailyDiscovery, actionLabel = copy.playAll, onAction = { actions.playAll(tracks) })
            Spacer(Modifier.height(8.dp))
            YouTubeMusicDiscoveryCarousel(
                remember(tracks) {
                    tracks.map { track ->
                        YouTubeMusicDiscoveryCard("daily-${track.id}", track.title, track.artist, "", "", track)
                    }
                },
                onOpen = { track -> actions.playFrom(tracks, track) },
                onLongClick = actions.onTrackActions
            )
        }
    }
}

private const val DAILY_DISCOVERY_SIZE = 12
