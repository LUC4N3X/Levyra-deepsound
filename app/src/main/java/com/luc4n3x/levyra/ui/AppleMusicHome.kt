package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.apple.AppleCatalogItem
import com.luc4n3x.levyra.data.apple.AppleCatalogItemKind
import com.luc4n3x.levyra.data.apple.AppleCatalogTracks
import com.luc4n3x.levyra.data.apple.AppleHomeShelf
import com.luc4n3x.levyra.data.apple.AppleHomeShelfRole
import com.luc4n3x.levyra.data.apple.AppleMusicHomeShelves
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraAppleMusicStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import com.luc4n3x.levyra.viewmodel.AppleMusicEditorialViewModel

internal class AppleMusicHomeCollection(val id: String, val title: String, val tracks: List<Track>)

internal class AppleMusicHomeActions(
    val playFrom: (List<Track>, Track) -> Unit,
    val playAll: (List<Track>) -> Unit,
    val openAlbum: (AlbumHit) -> Unit,
    val openArtist: (ArtistHit) -> Unit,
    val openArtistByName: (String) -> Unit,
    val openCollection: (String) -> Unit,
    val openSettings: () -> Unit,
    val pickCountry: () -> Unit,
    val onTrackActions: (Track) -> Unit
)

internal class AppleMusicHomeData(
    val selectedChartId: String,
    val currentId: String?,
    val soundtrack: StudioHero?,
    val personalTracks: List<Track>,
    val showPersonal: Boolean,
    val quickPicks: HomeSection?,
    val albums: List<AlbumHit>,
    val showAlbums: Boolean,
    val collections: List<AppleMusicHomeCollection>,
    val artists: List<ArtistHit>,
    val showArtists: Boolean,
    val forgottenFavorites: List<Track>,
    val countryLabel: String = "",
    val charts: List<Track> = emptyList(),
    val chartsTitle: String = "",
    val listener: HomeListener = HomeListener("", "", 0L, "")
)

internal class HomeListener(val name: String, val photoPath: String, val photoVersion: Long, val avatarUrl: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppleMusicHomeContent(
    data: AppleMusicHomeData,
    actions: AppleMusicHomeActions,
    homeListState: LazyListState,
    homeBottomInset: Dp,
    homeRefreshing: Boolean,
    homePullState: PullToRefreshState,
    onPullRefresh: () -> Unit
) {
    val homeRadio = LocalHomeRadio.current
    val strings = LocalLevyraStrings.current
    val editorialViewModel = viewModel<AppleMusicEditorialViewModel>()
    val editorial by editorialViewModel.state.collectAsStateWithLifecycle()
    val storefront = remember(data.selectedChartId) { ChartsCatalog.region(data.selectedChartId).country }
    LaunchedEffect(storefront, strings.code) { editorialViewModel.load(storefront, strings.code) }
    val copy = remember(strings.code) { LevyraAppleMusicStrings.forCode(strings.code) }
    val shelves = remember(editorial.newMusic) { AppleMusicHomeShelves.select(editorial.newMusic) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val openItem = remember(actions, editorialViewModel) { appleMusicItemOpener(actions, editorialViewModel::openCollection) }
    var levyraChartOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var seeAllShelfId by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val plan = AppleMusicHomePlan.of(shelves, appleMusicLevyraBlocks(data, actions, copy, strings) { levyraChartOpen = true })
    val reload = { editorialViewModel.load(storefront, strings.code, force = true) }
    Box(Modifier.fillMaxSize().background(AppleMusicDesign.background)) {
        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize().pullToRefresh(isRefreshing = homeRefreshing, state = homePullState, onRefresh = {
                onPullRefresh()
                reload()
            }),
            contentPadding = PaddingValues(top = statusBarTop + 8.dp, bottom = homeBottomInset),
            verticalArrangement = Arrangement.spacedBy(AppleMusicDesign.sectionGap)
        ) {
            item(key = "apple-home-title", contentType = "apple-home-title") {
                AppleMusicHomeTitle(strings.home, copy.more, data.countryLabel, actions)
            }
            appleMusicHomeNotices(editorial, shelves.isEmpty(), copy, reload)
            favoriteRadioSection(homeRadio)
            appleMusicHomeSections(plan) { homeShelf ->
                AppleMusicEditorialShelf(homeShelf, data.currentId, copy.explicit, openItem, actions.onTrackActions) { seeAllShelfId = homeShelf.shelf.id }
            }
        }
        AppleMusicHomeTopChrome(statusBarTop, homeRefreshing, homePullState)
        if (levyraChartOpen && data.charts.isNotEmpty()) {
            AppleMusicTrackListScreen(
                title = data.chartsTitle, curator = "Levyra", caption = data.countryLabel, tracks = data.charts,
                currentId = data.currentId, bottomInset = homeBottomInset, actions = actions, ranked = true
            ) { levyraChartOpen = false }
        }
        plan.shelf(seeAllShelfId)?.let { homeShelf ->
            AppleMusicShelfScreen(homeShelf, data.currentId, copy.explicit, homeBottomInset, openItem, actions.onTrackActions) { seeAllShelfId = null }
        }
        editorial.collection?.let { collection ->
            AppleMusicEditorialCollectionOverlay(collection, data.currentId, homeBottomInset, actions, editorialViewModel)
        }
    }
}

private class AppleMusicHomePlan(
    val shelves: List<AppleHomeShelf>,
    val leading: List<AppleHomeBlock>,
    val chartBlock: AppleHomeBlock?,
    val chartIndex: Int
) {
    fun shelf(id: String?): AppleHomeShelf? = id?.let { wanted -> shelves.firstOrNull { it.shelf.id == wanted } }

    fun blocksAfter(index: Int): List<AppleHomeBlock> {
        val slot = SLOTS.indexOfFirst { it.coerceAtMost(shelves.lastIndex) == index }
        if (slot < 0) return emptyList()
        val last = SLOTS.indexOfLast { it.coerceAtMost(shelves.lastIndex) == index }
        return leading.subList(slot.coerceAtMost(leading.size), (last + 1).coerceAtMost(leading.size))
    }

    fun trailing(): List<AppleHomeBlock> = leading.drop(SLOTS.size)

    companion object {
        private val SLOTS = listOf(1, 3, 5, 7, 8, 9, 10)

        fun of(shelves: List<AppleHomeShelf>, blocks: List<AppleHomeBlock>): AppleMusicHomePlan {
            val chartIndex = shelves.indexOfFirst { it.role == AppleHomeShelfRole.Charts }
            val chartBlock = blocks.firstOrNull { it.first == TOP50_KEY }?.takeIf { chartIndex >= 0 }
            val leading = if (chartBlock != null) blocks.filter { it.first != TOP50_KEY } else blocks
            return AppleMusicHomePlan(shelves, leading, chartBlock, chartIndex)
        }
    }
}

private typealias AppleHomeBlock = Pair<String, @Composable () -> Unit>

private const val TOP50_KEY = "levyra-top50"

private fun androidx.compose.foundation.lazy.LazyListScope.appleMusicHomeNotices(
    editorial: com.luc4n3x.levyra.viewmodel.AppleMusicEditorialUiState,
    noShelves: Boolean,
    copy: com.luc4n3x.levyra.ui.i18n.AppleMusicSkinCopy,
    reload: () -> Unit
) {
    if (noShelves && editorial.failed) {
        item(key = "apple-home-unavailable", contentType = "apple-notice") { AppleMusicNotice(copy.editorialUnavailable, copy.editorialRetry, reload) }
    }
    if (!noShelves && editorial.stale) {
        item(key = "apple-home-stale", contentType = "apple-notice") { AppleMusicNotice(copy.editorialOffline, null, null) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appleMusicHomeSections(
    plan: AppleMusicHomePlan,
    shelfContent: @Composable (AppleHomeShelf) -> Unit
) {
    if (plan.shelves.isEmpty()) {
        appleMusicHomeBlocks(plan.leading)
        return
    }
    plan.shelves.forEachIndexed { index, homeShelf ->
        if (index == plan.chartIndex) plan.chartBlock?.let { appleMusicHomeBlocks(listOf(it)) }
        item(key = "apple-shelf-${homeShelf.shelf.id}-$index", contentType = "apple-${homeShelf.role}") { shelfContent(homeShelf) }
        appleMusicHomeBlocks(plan.blocksAfter(index))
    }
    appleMusicHomeBlocks(plan.trailing())
}

private fun androidx.compose.foundation.lazy.LazyListScope.appleMusicHomeBlocks(blocks: List<AppleHomeBlock>) {
    blocks.forEach { (key, block) -> item(key = key, contentType = key) { block() } }
}

private fun appleMusicItemOpener(
    actions: AppleMusicHomeActions,
    openCollection: (AppleCatalogItem) -> Unit
): (AppleCatalogItem, List<Track>) -> Unit = { item, siblings ->
    when (item.kind) {
        AppleCatalogItemKind.Album -> actions.openAlbum(item.toAlbumHit())
        AppleCatalogItemKind.Playlist -> openCollection(item)
        AppleCatalogItemKind.Artist -> actions.openArtistByName(item.title)
        AppleCatalogItemKind.Song -> AppleCatalogTracks.fromItem(item)?.let { track -> actions.playFrom(siblings.ifEmpty { listOf(track) }, track) }
        else -> Unit
    }
}

private fun AppleMusicHomeActions.playShuffled(tracks: List<Track>) {
    val shuffled = tracks.shuffled()
    shuffled.firstOrNull()?.let { first -> playFrom(shuffled, first) }
}

@Composable
private fun AppleMusicHomeTitle(title: String, moreLabel: String, countryLabel: String, actions: AppleMusicHomeActions) {
    Column {
        AppleMusicLargeTitle(title, moreLabel, actions.openSettings)
        if (countryLabel.isNotBlank()) AppleMusicCountryChip(countryLabel, actions.pickCountry)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun androidx.compose.foundation.layout.BoxScope.AppleMusicHomeTopChrome(statusBarTop: Dp, refreshing: Boolean, pullState: PullToRefreshState) {
    Box(
        Modifier.fillMaxWidth().height(statusBarTop + 12.dp)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(AppleMusicDesign.background, AppleMusicDesign.background.copy(alpha = 0.92f), Color.Transparent)))
    )
    PullToRefreshDefaults.LoadingIndicator(
        state = pullState,
        isRefreshing = refreshing,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarTop + 16.dp)
    )
}

@Composable
private fun AppleMusicTrackListScreen(
    title: String,
    curator: String,
    caption: String,
    tracks: List<Track>,
    currentId: String?,
    bottomInset: Dp,
    actions: AppleMusicHomeActions,
    ranked: Boolean,
    onDismiss: () -> Unit
) {
    AppleMusicCollectionScreen(
        title = title, curator = curator, caption = caption, description = "",
        artworkUrl = "", artworkTrack = tracks.firstOrNull(), tracks = tracks, currentId = currentId,
        loading = false, failed = false, bottomInset = bottomInset, onDismiss = onDismiss,
        onPlay = { track -> actions.playFrom(tracks, track) },
        onPlayAll = { actions.playAll(tracks) },
        onShuffle = { actions.playShuffled(tracks) },
        onTrackActions = actions.onTrackActions,
        ranked = ranked
    )
}

@Composable
private fun AppleMusicEditorialCollectionOverlay(
    collection: com.luc4n3x.levyra.viewmodel.AppleMusicCollectionUiState,
    currentId: String?,
    bottomInset: Dp,
    actions: AppleMusicHomeActions,
    editorialViewModel: AppleMusicEditorialViewModel
) {
    AppleMusicCollectionScreen(
        title = collection.title,
        curator = collection.curator,
        caption = "",
        description = collection.description,
        artworkUrl = collection.artworkUrl,
        artworkTrack = collection.tracks.firstOrNull(),
        tracks = collection.tracks,
        currentId = currentId,
        loading = collection.loading,
        failed = collection.failed,
        bottomInset = bottomInset,
        onDismiss = editorialViewModel::closeCollection,
        onPlay = { track -> actions.playFrom(collection.tracks, track) },
        onPlayAll = { actions.playAll(collection.tracks) },
        onShuffle = { actions.playShuffled(collection.tracks) },
        onTrackActions = actions.onTrackActions,
        onRetry = editorialViewModel::retryCollection,
        ranked = AppleMusicHomeShelves.chartParts(collection.title) != null
    )
}

private fun appleMusicLevyraBlocks(
    data: AppleMusicHomeData,
    actions: AppleMusicHomeActions,
    copy: com.luc4n3x.levyra.ui.i18n.AppleMusicSkinCopy,
    strings: com.luc4n3x.levyra.ui.i18n.LevyraStrings,
    openCharts: () -> Unit
): List<AppleHomeBlock> = listOfNotNull(
    songsBlock(TOP50_KEY, data.chartsTitle, data.charts, data, actions, copy.explicit, openCharts, limit = 20),
    soundtrackBlock(data.soundtrack),
    songsBlock("levyra-orbit", strings.personalOrbitTitle, data.personalTracks.takeIf { data.showPersonal }.orEmpty(), data, actions, copy.explicit),
    data.quickPicks?.let { section -> songsBlock("levyra-quick-picks", section.title.ifBlank { strings.quickPicks }, section.tracks, data, actions, copy.explicit) },
    cardsBlock("levyra-albums", strings.albumsForYou, data.albums.takeIf { data.showAlbums }.orEmpty().map { album ->
        AppleMusicCardModel("album-${album.title}-${album.artist}", album.title, album.artist, album.thumbnailUrl, album.explicit) { actions.openAlbum(album) }
    }, copy.explicit),
    cardsBlock("levyra-collections", "Levyra", data.collections.map { collection -> collection.toCard(copy.songs, actions) }, copy.explicit),
    cardsBlock("levyra-artists", strings.trendingArtists, data.artists.takeIf { data.showArtists }.orEmpty().map { artist ->
        AppleMusicCardModel("artist-${artist.browseId.ifBlank { artist.name }}", artist.name, "", artist.thumbnailUrl, circular = true) { actions.openArtist(artist) }
    }, copy.explicit),
    songsBlock("levyra-forgotten", strings.forgottenFavorites, data.forgottenFavorites, data, actions, copy.explicit)
)

private fun AppleMusicHomeCollection.toCard(songs: (Int) -> String, actions: AppleMusicHomeActions): AppleMusicCardModel {
    val cover = tracks.firstOrNull()
    return AppleMusicCardModel(
        "collection-$id", title, songs(tracks.size), cover?.largeThumbnailUrl?.ifBlank { cover.thumbnailUrl }.orEmpty()
    ) { actions.openCollection(id) }
}

private fun songsBlock(
    key: String,
    title: String,
    tracks: List<Track>,
    data: AppleMusicHomeData,
    actions: AppleMusicHomeActions,
    explicitLabel: String,
    onSeeAll: (() -> Unit)? = null,
    limit: Int = tracks.size
): AppleHomeBlock? {
    if (tracks.isEmpty()) return null
    return key to @Composable {
        Column {
            AppleMusicSectionHeader(title, onSeeAll)
            AppleMusicSongColumns(tracks.take(limit), 4, data.currentId, explicitLabel, { track -> actions.playFrom(tracks, track) }, actions.onTrackActions)
        }
    }
}

private fun cardsBlock(key: String, title: String, cards: List<AppleMusicCardModel>, explicitLabel: String): AppleHomeBlock? {
    if (cards.isEmpty()) return null
    return key to @Composable {
        Column {
            AppleMusicSectionHeader(title)
            AppleMusicCardCarousel(cards, explicitLabel = explicitLabel)
        }
    }
}

private fun soundtrackBlock(hero: StudioHero?): AppleHomeBlock? {
    hero ?: return null
    return "levyra-soundtrack" to @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppleMusicSectionHeader(hero.label)
            Box(Modifier.padding(horizontal = AppleMusicDesign.gutter)) {
                AppleMusicHeroCard(
                    AppleMusicHeroModel(
                        key = "soundtrack-${hero.track.id}", heading = "", title = hero.track.artist, subtitle = hero.track.title,
                        description = hero.lead, imageUrl = hero.artistArtworkUrl,
                        coverUrl = hero.track.largeThumbnailUrl.ifBlank { hero.track.thumbnailUrl },
                        backgroundColor = Color(hero.track.accentStart), onClick = hero.onOpen
                    )
                )
            }
        }
    }
}
@Composable
internal fun AppleMusicEditorialShelf(
    homeShelf: AppleHomeShelf,
    currentId: String?,
    explicitLabel: String,
    openItem: (AppleCatalogItem, List<Track>) -> Unit,
    onTrackActions: (Track) -> Unit,
    onSeeAll: () -> Unit
) {
    val shelf = homeShelf.shelf
    when (homeShelf.role) {
        AppleHomeShelfRole.Hero -> AppleMusicHeroCarousel(
            remember(shelf) {
                shelf.items.map { item ->
                    AppleMusicHeroModel(
                        key = item.id, heading = item.heading, title = item.title, subtitle = item.subtitle,
                        description = item.description, imageUrl = item.heroUrl(1200),
                        coverUrl = if (item.coverArtworkTemplate.isNotBlank()) item.coverUrl(200) else "",
                        backgroundColor = item.backgroundColor?.let(::Color), onClick = { openItem(item, emptyList()) }
                    )
                }
            }
        )
        AppleHomeShelfRole.Songs -> {
            val pairs = remember(shelf) { shelf.items.mapNotNull { item -> AppleCatalogTracks.fromItem(item)?.let { item to it } } }
            val tracks = remember(pairs) { pairs.map { it.second } }
            Column {
                AppleMusicSectionHeader(shelf.title, onSeeAll)
                AppleMusicSongColumns(tracks, shelf.rows.coerceAtLeast(1), currentId, explicitLabel, { track ->
                    pairs.firstOrNull { it.second.id == track.id }?.let { (item, _) -> openItem(item, tracks) }
                }, onTrackActions)
            }
        }
        AppleHomeShelfRole.Cards -> Column {
            AppleMusicSectionHeader(shelf.title, onSeeAll)
            AppleMusicCardCarousel(
                remember(shelf) {
                    shelf.items.map { item ->
                        AppleMusicCardModel(item.id, item.title, item.subtitle, item.artworkUrl(400), item.explicit, item.kind == AppleCatalogItemKind.Artist) { openItem(item, emptyList()) }
                    }
                },
                rows = shelf.rows,
                explicitLabel = explicitLabel
            )
        }
        AppleHomeShelfRole.Charts -> Column {
            AppleMusicSectionHeader(shelf.title, onSeeAll)
            AppleMusicChartCarousel(
                remember(shelf) {
                    shelf.items.mapNotNull { item ->
                        val (badge, name) = AppleMusicHomeShelves.chartParts(item.title) ?: return@mapNotNull null
                        AppleMusicChartCardModel(item.id, badge, name, item.title, item.subtitle) { openItem(item, emptyList()) }
                    }
                }
            )
        }
    }
}

@Composable
internal fun AppleMusicShelfScreen(
    homeShelf: AppleHomeShelf,
    currentId: String?,
    explicitLabel: String,
    bottomInset: Dp,
    openItem: (AppleCatalogItem, List<Track>) -> Unit,
    onTrackActions: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    val shelf = homeShelf.shelf
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val collapsed by remember { androidx.compose.runtime.derivedStateOf { gridState.firstVisibleItemIndex > 0 } }
    val tracks = remember(shelf) { shelf.items.mapNotNull(AppleCatalogTracks::fromItem) }
    Box(Modifier.fillMaxSize().background(AppleMusicDesign.background)) {
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(if (homeShelf.role == AppleHomeShelfRole.Songs) 1 else 2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = AppleMusicDesign.gutter, end = AppleMusicDesign.gutter, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 52.dp, bottom = bottomInset + 24.dp),
            horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap),
            verticalArrangement = Arrangement.spacedBy(if (homeShelf.role == AppleHomeShelfRole.Songs) 0.dp else 20.dp)
        ) {
            item(key = "title", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                androidx.compose.material3.Text(
                    shelf.title, style = androidx.compose.material3.MaterialTheme.typography.headlineLarge, color = AppleMusicDesign.label,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            when (homeShelf.role) {
                AppleHomeShelfRole.Songs -> items(tracks.size, key = { tracks[it].id }) { index ->
                    val track = tracks[index]
                    AppleMusicSongRow(track, track.id == currentId, explicitLabel, index < tracks.lastIndex, {
                        shelf.items.firstOrNull { AppleCatalogTracks.fromItem(it)?.id == track.id }?.let { openItem(it, tracks) }
                    }, { onTrackActions(track) })
                }
                AppleHomeShelfRole.Charts -> items(shelf.items.size, key = { shelf.items[it].id }) { index ->
                    val item = shelf.items[index]
                    val parts = AppleMusicHomeShelves.chartParts(item.title)
                    if (parts != null) {
                        AppleMusicChartCard(AppleMusicChartCardModel(item.id, parts.first, parts.second, item.title, item.subtitle) { openItem(item, emptyList()) })
                    }
                }
                else -> items(shelf.items.size, key = { shelf.items[it].id }) { index ->
                    val item = shelf.items[index]
                    AppleMusicSquareCard(
                        AppleMusicCardModel(item.id, item.title, item.subtitle, item.artworkUrl(400), item.explicit, item.kind == AppleCatalogItemKind.Artist) { openItem(item, emptyList()) },
                        explicitLabel
                    )
                }
            }
        }
        AppleMusicNavigationBar(title = shelf.title, collapsed = collapsed, onBack = onDismiss)
    }
}
