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
    val chartsTitle: String = ""
)

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
    val strings = LocalLevyraStrings.current
    val editorialViewModel = viewModel<AppleMusicEditorialViewModel>()
    val editorial by editorialViewModel.state.collectAsStateWithLifecycle()
    val storefront = remember(data.selectedChartId) { ChartsCatalog.region(data.selectedChartId).country }
    LaunchedEffect(storefront, strings.code) { editorialViewModel.load(storefront, strings.code) }
    val copy = remember(strings.code) { LevyraAppleMusicStrings.forCode(strings.code) }
    val shelves = remember(editorial.newMusic) { AppleMusicHomeShelves.select(editorial.newMusic) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val currentId = data.currentId
    val openItem: (AppleCatalogItem, List<Track>) -> Unit = { item, siblings ->
        when (item.kind) {
            AppleCatalogItemKind.Album -> actions.openAlbum(
                AlbumHit(
                    title = item.title, artist = item.subtitle, year = "", thumbnailUrl = item.coverUrl(600),
                    query = "${item.title} ${item.subtitle}".trim(), explicit = item.explicit
                )
            )
            AppleCatalogItemKind.Playlist -> editorialViewModel.openCollection(item)
            AppleCatalogItemKind.Artist -> actions.openArtistByName(item.title)
            AppleCatalogItemKind.Song -> AppleCatalogTracks.fromItem(item)?.let { track ->
                actions.playFrom(siblings.ifEmpty { listOf(track) }, siblings.firstOrNull { it.id == track.id } ?: track)
            }
            else -> Unit
        }
    }
    var levyraChartOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val allLevyraBlocks = appleMusicLevyraBlocks(data, actions, copy.explicit, copy.songs, strings) { levyraChartOpen = true }
    val top50Block = allLevyraBlocks.firstOrNull { it.first == "levyra-top50" }
    val chartsShelfIndex = remember(shelves) { shelves.indexOfFirst { it.role == AppleHomeShelfRole.Charts } }
    val levyraBlocks = if (chartsShelfIndex >= 0) allLevyraBlocks.filter { it.first != "levyra-top50" } else allLevyraBlocks
    var seeAllShelfId by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val seeAllShelf = remember(seeAllShelfId, shelves) { shelves.firstOrNull { it.shelf.id == seeAllShelfId } }
    val levyraAfter = remember(shelves.size) { if (shelves.isEmpty()) emptyList() else listOf(1, 3, 5, 7, 8, 9, 10).map { it.coerceAtMost(shelves.lastIndex) } }
    Box(Modifier.fillMaxSize().background(AppleMusicDesign.background)) {
        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize().pullToRefresh(isRefreshing = homeRefreshing, state = homePullState, onRefresh = {
                onPullRefresh()
                editorialViewModel.load(storefront, strings.code, force = true)
            }),
            contentPadding = PaddingValues(top = statusBarTop + 8.dp, bottom = homeBottomInset),
            verticalArrangement = Arrangement.spacedBy(AppleMusicDesign.sectionGap)
        ) {
            item(key = "apple-home-title", contentType = "apple-home-title") {
                Column {
                    AppleMusicLargeTitle(strings.home, copy.more, actions.openSettings)
                    if (data.countryLabel.isNotBlank()) AppleMusicCountryChip(data.countryLabel, actions.pickCountry)
                }
            }
            if (shelves.isEmpty()) {
                if (editorial.failed) {
                    item(key = "apple-home-unavailable", contentType = "apple-notice") {
                        AppleMusicNotice(copy.editorialUnavailable, copy.editorialRetry) { editorialViewModel.load(storefront, strings.code, force = true) }
                    }
                }
                levyraBlocks.forEach { (key, block) -> item(key = key, contentType = key) { block() } }
            } else {
                if (editorial.stale) {
                    item(key = "apple-home-stale", contentType = "apple-notice") { AppleMusicNotice(copy.editorialOffline, null, null) }
                }
                var levyraIndex = 0
                shelves.forEachIndexed { index, homeShelf ->
                    if (index == chartsShelfIndex && top50Block != null) {
                        item(key = top50Block.first, contentType = top50Block.first) { top50Block.second() }
                    }
                    item(key = "apple-shelf-${homeShelf.shelf.id}-$index", contentType = "apple-${homeShelf.role}") {
                        AppleMusicEditorialShelf(homeShelf, currentId, copy.explicit, openItem, actions.onTrackActions) { seeAllShelfId = homeShelf.shelf.id }
                    }
                    while (levyraIndex < levyraBlocks.size && levyraIndex < levyraAfter.size && levyraAfter[levyraIndex] == index) {
                        val (key, block) = levyraBlocks[levyraIndex]
                        item(key = key, contentType = key) { block() }
                        levyraIndex++
                    }
                }
                levyraBlocks.drop(levyraIndex).forEach { (key, block) -> item(key = key, contentType = key) { block() } }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(statusBarTop + 12.dp)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(AppleMusicDesign.background, AppleMusicDesign.background.copy(alpha = 0.92f), Color.Transparent)))
        )
        PullToRefreshDefaults.LoadingIndicator(
            state = homePullState,
            isRefreshing = homeRefreshing,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarTop + 16.dp)
        )
        if (levyraChartOpen && data.charts.isNotEmpty()) {
            AppleMusicCollectionScreen(
                title = data.chartsTitle, curator = "Levyra", caption = data.countryLabel, description = "",
                artworkUrl = "", artworkTrack = data.charts.firstOrNull(), tracks = data.charts, currentId = currentId,
                loading = false, failed = false, bottomInset = homeBottomInset,
                onDismiss = { levyraChartOpen = false },
                onPlay = { track -> actions.playFrom(data.charts, track) },
                onPlayAll = { actions.playAll(data.charts) },
                onShuffle = {
                    val shuffled = data.charts.shuffled()
                    shuffled.firstOrNull()?.let { first -> actions.playFrom(shuffled, first) }
                },
                onTrackActions = actions.onTrackActions,
                ranked = true
            )
        }
        seeAllShelf?.let { homeShelf ->
            AppleMusicShelfScreen(homeShelf, currentId, copy.explicit, homeBottomInset, openItem, actions.onTrackActions) { seeAllShelfId = null }
        }
        editorial.collection?.let { collection ->
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
                bottomInset = homeBottomInset,
                onDismiss = editorialViewModel::closeCollection,
                onPlay = { track -> actions.playFrom(collection.tracks, track) },
                onPlayAll = { actions.playAll(collection.tracks) },
                onShuffle = {
                    val shuffled = collection.tracks.shuffled()
                    shuffled.firstOrNull()?.let { first -> actions.playFrom(shuffled, first) }
                },
                onTrackActions = actions.onTrackActions,
                onRetry = editorialViewModel::retryCollection,
                ranked = AppleMusicHomeShelves.chartParts(collection.title) != null
            )
        }
    }
}

private fun appleMusicLevyraBlocks(
    data: AppleMusicHomeData,
    actions: AppleMusicHomeActions,
    explicitLabel: String,
    songs: (Int) -> String,
    strings: com.luc4n3x.levyra.ui.i18n.LevyraStrings,
    openCharts: () -> Unit
): List<Pair<String, @Composable () -> Unit>> = buildList {
    val currentId = data.currentId
    if (data.charts.isNotEmpty()) {
        add("levyra-top50" to @Composable {
            Column {
                AppleMusicSectionHeader(data.chartsTitle, openCharts)
                AppleMusicSongColumns(data.charts.take(20), 4, currentId, explicitLabel, { track -> actions.playFrom(data.charts, track) }, actions.onTrackActions)
            }
        })
    }
    data.soundtrack?.let { hero ->
        add("levyra-soundtrack" to @Composable {
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
        })
    }
    if (data.showPersonal && data.personalTracks.isNotEmpty()) {
        add("levyra-orbit" to @Composable {
            Column {
                AppleMusicSectionHeader(strings.personalOrbitTitle)
                AppleMusicSongColumns(data.personalTracks, 4, currentId, explicitLabel, { track -> actions.playFrom(data.personalTracks, track) }, actions.onTrackActions)
            }
        })
    }
    data.quickPicks?.takeIf { it.tracks.isNotEmpty() }?.let { section ->
        add("levyra-quick-picks" to @Composable {
            Column {
                AppleMusicSectionHeader(section.title.ifBlank { strings.quickPicks })
                AppleMusicSongColumns(section.tracks, 4, currentId, explicitLabel, { track -> actions.playFrom(section.tracks, track) }, actions.onTrackActions)
            }
        })
    }
    if (data.showAlbums && data.albums.isNotEmpty()) {
        add("levyra-albums" to @Composable {
            Column {
                AppleMusicSectionHeader(strings.albumsForYou)
                AppleMusicCardCarousel(
                    data.albums.map { album ->
                        AppleMusicCardModel("album-${album.title}-${album.artist}", album.title, album.artist, album.thumbnailUrl, album.explicit) { actions.openAlbum(album) }
                    },
                    explicitLabel = explicitLabel
                )
            }
        })
    }
    if (data.collections.isNotEmpty()) {
        add("levyra-collections" to @Composable {
            Column {
                AppleMusicSectionHeader("Levyra")
                AppleMusicCardCarousel(
                    data.collections.map { collection ->
                        val cover = collection.tracks.firstOrNull()
                        AppleMusicCardModel(
                            "collection-${collection.id}", collection.title, songs(collection.tracks.size),
                            cover?.largeThumbnailUrl?.ifBlank { cover.thumbnailUrl }.orEmpty()
                        ) { actions.openCollection(collection.id) }
                    }
                )
            }
        })
    }
    if (data.showArtists && data.artists.isNotEmpty()) {
        add("levyra-artists" to @Composable {
            Column {
                AppleMusicSectionHeader(strings.trendingArtists)
                AppleMusicCardCarousel(
                    data.artists.map { artist ->
                        AppleMusicCardModel("artist-${artist.browseId.ifBlank { artist.name }}", artist.name, "", artist.thumbnailUrl, circular = true) { actions.openArtist(artist) }
                    }
                )
            }
        })
    }
    if (data.forgottenFavorites.isNotEmpty()) {
        add("levyra-forgotten" to @Composable {
            Column {
                AppleMusicSectionHeader(strings.forgottenFavorites)
                AppleMusicSongColumns(data.forgottenFavorites, 4, currentId, explicitLabel, { track -> actions.playFrom(data.forgottenFavorites, track) }, actions.onTrackActions)
            }
        })
    }
}

@Composable
private fun AppleMusicEditorialShelf(
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
private fun AppleMusicShelfScreen(
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
