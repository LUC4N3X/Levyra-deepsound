package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.qobuz.QobuzAlbum
import com.luc4n3x.levyra.data.qobuz.QobuzPlaylist
import com.luc4n3x.levyra.data.qobuz.QobuzShelfKind
import com.luc4n3x.levyra.data.qobuz.QobuzStore
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.HomeBlock
import com.luc4n3x.levyra.domain.HomeSectionId
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraQobuzSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.QobuzSkinCopy
import com.luc4n3x.levyra.ui.theme.QobuzDesign
import com.luc4n3x.levyra.viewmodel.QobuzDiscoverTab
import com.luc4n3x.levyra.viewmodel.QobuzDiscoverUiState
import com.luc4n3x.levyra.viewmodel.QobuzDiscoverViewModel
import com.luc4n3x.levyra.viewmodel.QobuzPlaylistUiState

internal class QobuzHomeActions(
    val home: AppleMusicHomeActions,
    val openExplore: () -> Unit,
    val openCategory: (ExploreCategory) -> Unit,
    val toggleFollow: (ArtistHit) -> Unit,
    val openPlaylist: (com.luc4n3x.levyra.domain.PlaylistHit) -> Unit,
    val languageCode: String,
    val addToQueue: (Track) -> Unit = {}
)

internal class QobuzHomeExtras(
    val categories: List<ExploreCategory>,
    val followedArtistKeys: Set<String>,
    val chartCountry: String
)

private sealed interface QobuzSeeAll {
    data class Albums(val title: String, val albums: List<QobuzAlbum>, val awards: Boolean) : QobuzSeeAll
    data class Playlists(val title: String) : QobuzSeeAll
    data class Tracks(val title: String, val tracks: List<Track>) : QobuzSeeAll
    data class Category(val category: ExploreCategory) : QobuzSeeAll
    data class Mix(val mix: QobuzMix) : QobuzSeeAll
}

private typealias QobuzBlock = Pair<String, @Composable () -> Unit>

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun QobuzHomeContent(
    data: AppleMusicHomeData,
    actions: QobuzHomeActions,
    extras: QobuzHomeExtras,
    homeListState: LazyListState,
    homeBottomInset: Dp,
    homeRefreshing: Boolean,
    homePullState: PullToRefreshState,
    onPullRefresh: () -> Unit,
    composition: ComposedHomeRequest? = null
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraQobuzSkinStrings.forCode(strings.code) }
    val discover = viewModel<QobuzDiscoverViewModel>()
    val feed by discover.state.collectAsStateWithLifecycle()
    val playlist by discover.playlist.collectAsStateWithLifecycle()
    val locale = remember(strings.code, extras.chartCountry) { QobuzStore.locale(strings.code, extras.chartCountry) }
    val feedActive = composition?.showsSkinFeed != false
    LaunchedEffect(locale, feedActive) { if (feedActive) discover.load(locale) }
    var seeAll by remember { mutableStateOf<QobuzSeeAll?>(null) }
    val coveredSections = remember(composition?.blocks) {
        composition?.blocks.orEmpty().filterIsInstance<HomeBlock.Section>().mapTo(HashSet()) { it.id }
    }
    val kit = remember(copy, data, actions.home) {
        QobuzComposedKit(copy, data, actions.home) { title, tracks -> seeAll = QobuzSeeAll.Tracks(title, tracks) }
    }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxSize().background(QobuzDesign.surfacePrimary)) {
        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize().padding(top = statusTop).pullToRefresh(isRefreshing = homeRefreshing, state = homePullState, onRefresh = {
                onPullRefresh()
                if (feedActive) discover.load(locale, force = true)
            }),
            contentPadding = PaddingValues(bottom = homeBottomInset + 24.dp)
        ) {
            item(key = "qobuz-title", contentType = "qobuz-title") {
                QobuzPageHeader(copy.discover) {
                    QobuzIconAction(Icons.Rounded.Tune, copy.genres, actions.openExplore)
                    QobuzIconAction(Icons.Rounded.Settings, copy.settings, actions.home.openSettings)
                }
            }
            val feedItems: LazyListScope.() -> Unit = {
                stickyHeader(key = "qobuz-tabs", contentType = "qobuz-tabs") {
                    QobuzDiscoverTabs(copy, feed.tab, homeListState.firstVisibleItemIndex > 0, discover::selectTab)
                }
                qobuzHomeSections(data, actions.withCategory { seeAll = QobuzSeeAll.Category(it) }, extras, copy, feed, strings.personalOrbitTitle, strings.quickPicks,
                    onOpenPlaylist = discover::openPlaylist, onSeeAll = { seeAll = it }, onRetry = { discover.load(locale, force = true) },
                    coveredSections = coveredSections, composed = composition != null)
            }
            if (composition == null) {
                feedItems()
            } else {
                composedHomeSections(composition, kit) {
                    feedItems()
                    if (composition.blocks.lastOrNull() != HomeBlock.SkinFeed) {
                        stickyHeader(key = "qobuz-tabs-release", contentType = "qobuz-tabs-release") { Spacer(Modifier.height(1.dp)) }
                    }
                }
                item(key = "qobuz-composed-footer", contentType = "qobuz-composed-footer") {
                    Spacer(Modifier.height(QobuzDesign.sectionGap))
                }
            }
        }
        PullToRefreshDefaults.LoadingIndicator(
            state = homePullState, isRefreshing = homeRefreshing,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = statusTop + 16.dp)
        )
        (seeAll as? QobuzSeeAll.Mix)?.let { target ->
            BackHandler { seeAll = null }
            QobuzMixScreen(target.mix, copy, data.currentId, homeBottomInset, actions.home.playFrom, actions.home.playAll,
                { tracks -> tracks.forEach(actions.addToQueue) }, actions.home.onTrackActions) { seeAll = null }
        }
        (seeAll as? QobuzSeeAll.Category)?.let { target ->
            BackHandler { seeAll = null }
            QobuzCategoryScreen(target.category, actions, statusTop, homeBottomInset) { seeAll = null }
        }
        seeAll?.takeUnless { it is QobuzSeeAll.Category || it is QobuzSeeAll.Mix }?.let { target ->
            BackHandler { seeAll = null }
            QobuzSeeAllScreen(target, feed, copy, data, actions, statusTop, homeBottomInset, discover::openPlaylist) { seeAll = null }
        }
        playlist?.let { open ->
            BackHandler { discover.closePlaylist() }
            QobuzPlaylistScreen(open, copy, data.currentId, actions, statusTop, homeBottomInset, discover::retryPlaylist, discover::closePlaylist)
        }
    }
}

@Composable
private fun QobuzDiscoverTabs(copy: QobuzSkinCopy, tab: QobuzDiscoverTab, elevated: Boolean, onSelect: (QobuzDiscoverTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(QobuzDesign.surfacePrimary)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = QobuzDesign.gutter, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(QobuzDesign.chipGap)
        ) {
            item(key = "home") { QobuzChip(copy.home, tab == QobuzDiscoverTab.Home, { onSelect(QobuzDiscoverTab.Home) }, QobuzHomeIcon) }
            item(key = "editor") { QobuzChip(copy.editorPicks, tab == QobuzDiscoverTab.EditorPicks, { onSelect(QobuzDiscoverTab.EditorPicks) }) }
            item(key = "for-you") { QobuzChip(copy.forYou, tab == QobuzDiscoverTab.ForYou, { onSelect(QobuzDiscoverTab.ForYou) }) }
        }
        if (elevated) Box(Modifier.fillMaxWidth().height(1.dp).background(QobuzDesign.separator))
    }
}

private fun LazyListScope.qobuzHomeSections(
    data: AppleMusicHomeData,
    actions: QobuzHomeActions,
    extras: QobuzHomeExtras,
    copy: QobuzSkinCopy,
    feed: QobuzDiscoverUiState,
    orbitTitle: String,
    quickPicksTitle: String,
    onOpenPlaylist: (QobuzPlaylist) -> Unit,
    onSeeAll: (QobuzSeeAll) -> Unit,
    onRetry: () -> Unit,
    coveredSections: Set<HomeSectionId>,
    composed: Boolean
) {
    val welcome = welcomeBlock(data, actions, copy)
    val newReleases = newReleasesBlock(data, actions, copy, feed, onSeeAll)
    val playlists = playlistsBlock(copy, feed, onOpenPlaylist, onSeeAll)
    val categories = categoriesBlock(copy, extras.categories, actions.openCategory)
    val artists = artistsBlock(data, actions, extras, copy)
    val mixSources = buildList<Pair<HomeSectionId, QobuzMix>> {
        data.soundtrack?.let { hero -> add(HomeSectionId.Spotlight to QobuzMix("soundtrack", copy.welcomeTitle, copy.welcomeBody, (listOf(hero.track) + data.personalTracks).distinctBy { it.id }, 0)) }
        data.personalTracks.takeIf { data.showPersonal && it.isNotEmpty() }?.let { add(HomeSectionId.PersonalOrbit to QobuzMix("orbit", orbitTitle, copy.orbitBaseline, it, 1)) }
        data.quickPicks?.tracks?.takeIf { it.isNotEmpty() }?.let { add(HomeSectionId.QuickPicks to QobuzMix("picks", data.quickPicks.title.ifBlank { quickPicksTitle }, copy.picksMixBody, it, 2)) }
    }
    val mixBlock: (List<QobuzMix>) -> QobuzBlock? = { mixList ->
        if (mixList.isEmpty()) null else "mixes" to @Composable { QobuzMixRow(mixList, copy) { onSeeAll(QobuzSeeAll.Mix(it)) } }
    }
    val mixes = mixBlock(mixSources.map { it.second })
    val homeMixes = mixBlock(mixSources.filterNot { it.first in coveredSections }.map { it.second })
    val seed = data.personalTracks.firstOrNull() ?: data.soundtrack?.track
    val similar: QobuzBlock? = if (seed == null || !data.showAlbums || data.albums.isEmpty()) null else "similar" to @Composable {
        Column {
            QobuzSimilarHeader(seed, copy)
            Spacer(Modifier.height(QobuzDesign.headerToContent + 4.dp))
            QobuzGridRow(data.albums.take(16).chunked(1), QobuzDesign.gridCard) { album ->
                QobuzAlbumCard(QobuzAlbum("", album.title, album.artist, album.thumbnailUrl), copy, QobuzDesign.gridCard, { actions.home.openAlbum(album) })
            }
        }
    }
    val orbit = trackRowBlock("orbit", orbitTitle, copy.orbitBaseline, data.personalTracks.takeIf { data.showPersonal }.orEmpty(), data, actions, copy, onSeeAll)
    val picks = rankedBlock("quick-picks", data.quickPicks?.title?.ifBlank { quickPicksTitle } ?: quickPicksTitle, null, data.quickPicks?.tracks.orEmpty(), false, data, actions, copy, onSeeAll)
    val ideal = albumShelfBlock(QobuzShelfKind.IdealDiscography, copy.idealDiscography, copy.idealDiscographyBaseline, feed, rows = 2, width = QobuzDesign.gridCard, awards = false, actions, copy, onSeeAll)
    val qobuzissime = albumShelfBlock(QobuzShelfKind.Qobuzissime, copy.qobuzissime, copy.qobuzissimeBaseline, feed, rows = 1, width = QobuzDesign.extraWideCard, awards = false, actions, copy, onSeeAll)
    val top50 = rankedBlock("top50", data.chartsTitle, copy.top50Baseline, data.charts, true, data, actions, copy, onSeeAll)
    val press = albumShelfBlock(QobuzShelfKind.PressAwards, copy.pressAwards, copy.pressAwardsBaseline, feed, rows = 1, width = QobuzDesign.wideCard, awards = true, actions, copy, onSeeAll)
    val albumsForYou = levyraAlbumsBlock(data, actions, copy)
    val albumsCovered = HomeSectionId.AlbumsForYou in coveredSections
    val releasesAreLevyraAlbums = feed.shelf(QobuzShelfKind.NewReleases)?.albums.isNullOrEmpty() && !feed.loading
    val blocks = when (feed.tab) {
        QobuzDiscoverTab.Home -> listOf(
            welcome.takeUnless { HomeSectionId.Spotlight in coveredSections },
            newReleases.takeUnless { albumsCovered && releasesAreLevyraAlbums },
            playlists,
            categories,
            artists.takeUnless { HomeSectionId.TrendingArtists in coveredSections },
            homeMixes,
            similar.takeUnless { albumsCovered },
            ideal,
            qobuzissime,
            top50.takeUnless { HomeSectionId.Charts in coveredSections },
            press
        )
        QobuzDiscoverTab.EditorPicks -> listOf(newReleases, playlists, categories, ideal, qobuzissime, press)
        QobuzDiscoverTab.ForYou -> listOf(mixes, artists, similar, orbit, picks, top50)
    }.filterNotNull()
    blocks.forEachIndexed { index, (key, content) ->
        item(key = "qobuz-$key", contentType = key) {
            Box(Modifier.padding(top = if (index == 0) 24.dp else QobuzDesign.sectionGap)) { content() }
        }
    }
    val editorialFailed = feed.failed && feed.shelves.isEmpty()
    if (!composed || editorialFailed) {
        item(key = "qobuz-footer", contentType = "qobuz-footer") {
            Column(Modifier.fillMaxWidth().padding(horizontal = QobuzDesign.gutter, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editorialFailed) {
                    Text(copy.editorialUnavailable, style = QobuzText.baseline)
                    Text(
                        copy.retry, style = MaterialTheme.typography.labelLarge, color = QobuzDesign.textPrimary,
                        modifier = Modifier.clip(QobuzDesign.buttonShape).background(QobuzDesign.surfaceSecondary)
                            .clickable(role = Role.Button, onClick = onRetry).padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

private fun welcomeBlock(data: AppleMusicHomeData, actions: QobuzHomeActions, copy: QobuzSkinCopy): QobuzBlock? {
    val hero = data.soundtrack ?: return null
    return "welcome" to @Composable {
        Column {
            QobuzSectionHeader(copy.getStarted, null, copy.seeAll, null)
            Spacer(Modifier.height(QobuzDesign.headerToContent + 4.dp))
            QobuzWelcomeCard(hero, copy, data.personalTracks, data.artists, Modifier.padding(horizontal = QobuzDesign.contentPadding))
        }
    }
}

@Composable
private fun QobuzWelcomeCard(hero: StudioHero, copy: QobuzSkinCopy, tracks: List<Track>, artists: List<ArtistHit>, modifier: Modifier) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(QobuzDesign.welcomeCorner)
    Row(
        modifier.fillMaxWidth().clip(shape).background(QobuzDesign.surfaceSecondary).border(1.dp, QobuzDesign.border, shape)
            .clickable(role = Role.Button, onClick = hero.onOpen).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 14.dp)) {
            Text(copy.welcomeTitle, style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = QobuzDesign.textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(copy.welcomeBody, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal), color = QobuzDesign.textSecondary,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(18.dp))
            QobuzAvatarStack(hero, tracks, artists)
        }
        Box(Modifier.size(QobuzDesign.welcomeArtwork).clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))) {
            QobuzMixArt(0, Modifier.fillMaxSize())
            Text("Levyra", style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
            QobuzPlayCircle(copy.play, 44.dp, hero.onOpen, Modifier.align(Alignment.BottomEnd).padding(8.dp))
        }
    }
}

@Composable
private fun QobuzWelcomeArt(modifier: Modifier) {
    val animated = LocalAnimationsEnabled.current
    val phase = if (animated) {
        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "qobuz-welcome-art")
        transition.animateFloat(
            initialValue = 0f, targetValue = (2 * Math.PI).toFloat(), animationSpec =
            androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween<Float>(14_000, easing = androidx.compose.animation.core.LinearEasing)),
            label = "qobuz-welcome-phase"
        )
    } else null
    androidx.compose.foundation.Canvas(modifier) {
        val t = phase?.value ?: 0f
        fun at(x: Float, y: Float, dx: Float, dy: Float, shift: Float) = androidx.compose.ui.geometry.Offset(
            size.width * (x + dx * kotlin.math.cos(t + shift)), size.height * (y + dy * kotlin.math.sin(t + shift))
        )
        drawRect(Color(0xFF15102A))
        drawRect(Brush.radialGradient(listOf(Color(0xFFFF3FD0), Color(0x00FF3FD0)), center = at(0.92f, 0.62f, 0.10f, 0.12f, 0f), radius = size.width * 0.75f))
        drawRect(Brush.radialGradient(listOf(Color(0xFFB9F3FF), Color(0x009B6BFF)), center = at(0.32f, 0.45f, 0.14f, 0.10f, 2.1f), radius = size.width * 0.55f))
        drawRect(Brush.radialGradient(listOf(Color(0xFFFFE680), Color(0x00FFE680)), center = at(0.62f, 0.05f, 0.12f, 0.08f, 4.2f), radius = size.width * 0.55f))
        drawRect(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0x99150A2A)))
    }
}

@Composable
internal fun QobuzAvatarStack(hero: StudioHero, tracks: List<Track>, artists: List<ArtistHit>) {
    val urls = remember(hero.artistArtworkUrl, hero.companionArtworkUrls, artists) {
        (listOf(hero.artistArtworkUrl) + hero.companionArtworkUrls + artists.map { it.thumbnailUrl }).filter { it.isNotBlank() }.distinct()
    }
    val visible = urls.take(5)
    val names = remember(tracks, artists) { (tracks.map { it.artist.lowercase() } + artists.map { it.name.lowercase() }).distinct().size }
    val extra = (maxOf(urls.size, names) - visible.size).coerceAtLeast(0)
    Row {
        visible.forEachIndexed { index, url ->
            QobuzArtworkBox(
                url,
                Modifier.offset(x = (-14 * index).dp).size(44.dp).border(2.dp, QobuzDesign.surfaceSecondary, CircleShape),
                CircleShape
            )
        }
        if (extra > 0) {
            Box(
                Modifier.offset(x = (-14 * visible.size).dp).size(44.dp).clip(CircleShape).background(QobuzDesign.surfaceQuaternary),
                contentAlignment = Alignment.Center
            ) { Text("+$extra", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium), color = QobuzDesign.textPrimary, maxLines = 1) }
        }
    }
}

private fun newReleasesBlock(
    data: AppleMusicHomeData,
    actions: QobuzHomeActions,
    copy: QobuzSkinCopy,
    feed: QobuzDiscoverUiState,
    onSeeAll: (QobuzSeeAll) -> Unit
): QobuzBlock? {
    val qobuz = feed.shelf(QobuzShelfKind.NewReleases)?.albums.orEmpty()
    if (qobuz.isEmpty()) {
        if (feed.loading) return "new-releases" to @Composable { QobuzShelfSkeleton(copy.newReleases, copy.newReleasesQobuz, rows = 2) }
        val fallback = data.albums.takeIf { data.showAlbums }.orEmpty().take(24)
        if (fallback.isEmpty()) return null
        return "new-releases" to @Composable {
            Column {
                QobuzSectionHeader(copy.newReleases, copy.newReleasesLevyra, copy.seeAll, null)
                Spacer(Modifier.height(QobuzDesign.headerToContent))
                QobuzGridRow(fallback.chunked(2), QobuzDesign.gridCard) { album ->
                    QobuzAlbumCard(QobuzAlbum("", album.title, album.artist, album.thumbnailUrl), copy, QobuzDesign.gridCard, { actions.home.openAlbum(album) })
                }
            }
        }
    }
    return "new-releases" to @Composable {
        Column {
            QobuzSectionHeader(copy.newReleases, copy.newReleasesQobuz, copy.seeAll, { onSeeAll(QobuzSeeAll.Albums(copy.newReleases, qobuz, false)) })
            Spacer(Modifier.height(QobuzDesign.headerToContent))
            QobuzGridRow(qobuz.take(24).chunked(2), QobuzDesign.gridCard) { album ->
                QobuzAlbumCard(album, copy, QobuzDesign.gridCard, { actions.home.openAlbum(album.toAlbumHit()) })
            }
        }
    }
}

private fun playlistsBlock(
    copy: QobuzSkinCopy,
    feed: QobuzDiscoverUiState,
    onOpenPlaylist: (QobuzPlaylist) -> Unit,
    onSeeAll: (QobuzSeeAll) -> Unit
): QobuzBlock? {
    val playlists = feed.shelf(QobuzShelfKind.Playlists)?.playlists.orEmpty()
    if (playlists.isEmpty()) {
        return if (feed.loading) "playlists" to @Composable { QobuzShelfSkeleton(copy.playlists, copy.playlistsBaseline, rows = 2) } else null
    }
    return "playlists" to @Composable {
        Column {
            QobuzSectionHeader(copy.playlists, copy.playlistsBaseline, copy.seeAll, { onSeeAll(QobuzSeeAll.Playlists(copy.playlists)) })
            Spacer(Modifier.height(QobuzDesign.headerToContent))
            QobuzGridRow(playlists.take(16).chunked(2), QobuzDesign.gridCard) { playlist ->
                QobuzPlaylistCard(playlist, copy.curatedByQobuz, QobuzDesign.gridCard) { onOpenPlaylist(playlist) }
            }
        }
    }
}

private fun categoriesBlock(copy: QobuzSkinCopy, categories: List<ExploreCategory>, onOpen: (ExploreCategory) -> Unit): QobuzBlock? {
    val visible = categories.distinctBy { it.title.lowercase() }
    if (visible.isEmpty()) return null
    return "categories" to @Composable {
        Column {
            QobuzSectionHeader(copy.categories, copy.categoriesBaseline, copy.seeAll, null)
            Spacer(Modifier.height(QobuzDesign.headerToContent + 4.dp))
            QobuzGridRow(visible.chunked(2), QobuzDesign.tagCardWidth, verticalGap = 12.dp, horizontalGap = 12.dp) { category ->
                QobuzCategoryTile(category.title, qobuzTagPalette(category.title), QobuzDesign.tagCardWidth) { onOpen(category) }
            }
        }
    }
}

private fun artistsBlock(data: AppleMusicHomeData, actions: QobuzHomeActions, extras: QobuzHomeExtras, copy: QobuzSkinCopy): QobuzBlock? {
    val artists = data.artists.takeIf { data.showArtists }.orEmpty().filter { it.name.isNotBlank() }
    if (artists.isEmpty()) return null
    return "artists" to @Composable {
        Column {
            QobuzSectionHeader(copy.artistsToFollow, copy.artistsToFollowBaseline, copy.seeAll, null)
            Spacer(Modifier.height(QobuzDesign.headerToContent + 4.dp))
            LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
                items(artists, key = { it.browseId.ifBlank { it.name } }) { artist ->
                    val followed = (artist.browseId.isNotBlank() && artist.browseId in extras.followedArtistKeys) ||
                        artist.name.trim().lowercase() in extras.followedArtistKeys
                    QobuzArtistCard(artist, copy, followed, { actions.home.openArtist(artist) }, { actions.toggleFollow(artist) })
                }
            }
        }
    }
}

private fun trackRowBlock(
    key: String,
    title: String,
    baseline: String?,
    tracks: List<Track>,
    data: AppleMusicHomeData,
    actions: QobuzHomeActions,
    copy: QobuzSkinCopy,
    onSeeAll: (QobuzSeeAll) -> Unit
): QobuzBlock? {
    if (tracks.isEmpty() || title.isBlank()) return null
    return key to @Composable {
        Column {
            QobuzSectionHeader(title, baseline, copy.seeAll, { onSeeAll(QobuzSeeAll.Tracks(title, tracks)) })
            Spacer(Modifier.height(QobuzDesign.headerToContent))
            LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
                items(tracks.take(20), key = { it.id }) { track ->
                    QobuzTrackCard(track, QobuzDesign.gridCard, { actions.home.playFrom(tracks, track) }, { actions.home.onTrackActions(track) })
                }
            }
        }
    }
}

private fun rankedBlock(
    key: String,
    title: String,
    baseline: String?,
    tracks: List<Track>,
    numbered: Boolean,
    data: AppleMusicHomeData,
    actions: QobuzHomeActions,
    copy: QobuzSkinCopy,
    onSeeAll: (QobuzSeeAll) -> Unit
): QobuzBlock? {
    if (tracks.isEmpty() || title.isBlank()) return null
    return key to @Composable {
        Column {
            QobuzSectionHeader(title, baseline, copy.seeAll, { onSeeAll(QobuzSeeAll.Tracks(title, tracks)) })
            if (numbered && data.countryLabel.isNotBlank()) {
                Text(
                    data.countryLabel, style = MaterialTheme.typography.labelLarge, color = QobuzDesign.textAccent,
                    modifier = Modifier.padding(horizontal = QobuzDesign.gutter).clip(QobuzDesign.buttonShape)
                        .clickable(role = Role.Button, onClick = actions.home.pickCountry).padding(vertical = 10.dp)
                )
            }
            Spacer(Modifier.height(QobuzDesign.headerToContent))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val pageWidth = maxWidth - QobuzDesign.gutter * 2 - 40.dp
                val pages = remember(tracks) { tracks.take(50).withIndex().toList().chunked(3) }
                LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(pages, key = { page -> "$key-${page.first().value.id}" }) { page ->
                        Column(Modifier.width(pageWidth)) {
                            page.forEach { (index, track) ->
                                QobuzRankedRow(
                                    if (numbered) index + 1 else null, track, copy, track.id == data.currentId,
                                    { actions.home.playFrom(tracks, track) }, { actions.home.onTrackActions(track) },
                                    Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun albumShelfBlock(
    kind: QobuzShelfKind,
    title: String,
    baseline: String,
    feed: QobuzDiscoverUiState,
    rows: Int,
    width: Dp,
    awards: Boolean,
    actions: QobuzHomeActions,
    copy: QobuzSkinCopy,
    onSeeAll: (QobuzSeeAll) -> Unit
): QobuzBlock? {
    val albums = feed.shelf(kind)?.albums.orEmpty()
    if (albums.isEmpty()) return null
    return kind.cacheName to @Composable {
        Column {
            QobuzSectionHeader(title, baseline, copy.seeAll, { onSeeAll(QobuzSeeAll.Albums(title, albums, awards)) })
            Spacer(Modifier.height(QobuzDesign.headerToContent))
            QobuzGridRow(albums.take(if (rows == 1) 12 else 24).chunked(rows), width) { album ->
                QobuzAlbumCard(album, copy, width, { actions.home.openAlbum(album.toAlbumHit()) }, showAward = awards)
            }
        }
    }
}

private fun levyraAlbumsBlock(data: AppleMusicHomeData, actions: QobuzHomeActions, copy: QobuzSkinCopy): QobuzBlock? {
    val albums = data.albums.takeIf { data.showAlbums }.orEmpty().take(16)
    if (albums.isEmpty()) return null
    return "levyra-albums" to @Composable {
        Column {
            QobuzSectionHeader(copy.newReleases, copy.newReleasesLevyra, copy.seeAll, null)
            Spacer(Modifier.height(QobuzDesign.headerToContent))
            QobuzGridRow(albums.chunked(1), QobuzDesign.gridCard) { album ->
                QobuzAlbumCard(QobuzAlbum("", album.title, album.artist, album.thumbnailUrl), copy, QobuzDesign.gridCard, { actions.home.openAlbum(album) })
            }
        }
    }
}

@Composable
private fun <T> QobuzGridRow(
    columns: List<List<T>>,
    width: Dp,
    verticalGap: Dp = 20.dp,
    horizontalGap: Dp = QobuzDesign.itemGap,
    item: @Composable (T) -> Unit
) {
    LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(horizontalGap)) {
        items(columns.size, key = { it }) { index ->
            Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(verticalGap)) {
                columns[index].forEach { item(it) }
            }
        }
    }
}

@Composable
internal fun QobuzShelfSkeleton(title: String, baseline: String, rows: Int) {
    Column {
        QobuzSectionHeader(title, baseline, "", null)
        Spacer(Modifier.height(QobuzDesign.headerToContent))
        Row(Modifier.padding(horizontal = QobuzDesign.gutter), horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
            repeat(3) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    repeat(rows) {
                        Column {
                            Box(Modifier.size(QobuzDesign.gridCard).clip(QobuzDesign.cardShape).background(QobuzDesign.surfaceSecondary))
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.width(110.dp).height(12.dp).clip(QobuzDesign.cardShape).background(QobuzDesign.surfaceSecondary))
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.width(80.dp).height(12.dp).clip(QobuzDesign.cardShape).background(QobuzDesign.surfaceSecondary))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QobuzBackBar(title: String, statusTop: Dp, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(QobuzDesign.surfacePrimary).padding(top = statusTop).height(QobuzDesign.topBarHeight).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QobuzIconAction(Icons.AutoMirrored.Rounded.ArrowBack, LocalLevyraStrings.current.back, onBack)
        Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium), color = QobuzDesign.textPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun QobuzSeeAllScreen(
    target: QobuzSeeAll,
    feed: QobuzDiscoverUiState,
    copy: QobuzSkinCopy,
    data: AppleMusicHomeData,
    actions: QobuzHomeActions,
    statusTop: Dp,
    bottomInset: Dp,
    onOpenPlaylist: (QobuzPlaylist) -> Unit,
    onBack: () -> Unit
) {
    val title = when (target) {
        is QobuzSeeAll.Albums -> target.title
        is QobuzSeeAll.Playlists -> target.title
        is QobuzSeeAll.Tracks -> target.title
        is QobuzSeeAll.Category -> target.category.title
        is QobuzSeeAll.Mix -> target.mix.title
    }
    Column(Modifier.fillMaxSize().background(QobuzDesign.surfacePrimary).clickable(enabled = false) {}) {
        QobuzBackBar(title, statusTop, onBack)
        Box(Modifier.fillMaxWidth().height(1.dp).background(QobuzDesign.separator))
        when (target) {
            is QobuzSeeAll.Tracks -> LazyColumn(contentPadding = PaddingValues(start = QobuzDesign.gutter, end = QobuzDesign.gutter, top = 8.dp, bottom = bottomInset + 24.dp)) {
                itemsIndexed(target.tracks, key = { _, track -> track.id }) { index, track ->
                    QobuzRankedRow(index + 1, track, copy, track.id == data.currentId,
                        { actions.home.playFrom(target.tracks, track) }, { actions.home.onTrackActions(track) }, Modifier.fillMaxWidth())
                }
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(QobuzDesign.gridCard),
                contentPadding = PaddingValues(start = QobuzDesign.gutter, end = QobuzDesign.gutter, top = 16.dp, bottom = bottomInset + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (target is QobuzSeeAll.Albums) {
                    items(target.albums, key = { it.id }) { album ->
                        QobuzAlbumCard(album, copy, Dp.Unspecified, { actions.home.openAlbum(album.toAlbumHit()) }, showAward = target.awards)
                    }
                } else {
                    items(feed.shelf(QobuzShelfKind.Playlists)?.playlists.orEmpty(), key = { it.id }) { playlist ->
                        QobuzPlaylistCard(playlist, copy.curatedByQobuz, Dp.Unspecified) { onOpenPlaylist(playlist) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QobuzPlaylistScreen(
    open: QobuzPlaylistUiState,
    copy: QobuzSkinCopy,
    currentId: String?,
    actions: QobuzHomeActions,
    statusTop: Dp,
    bottomInset: Dp,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val detail = open.detail
    val tracks = detail?.tracks.orEmpty()
    Column(Modifier.fillMaxSize().background(QobuzDesign.surfacePrimary).clickable(enabled = false) {}) {
        QobuzBackBar(detail?.title?.ifBlank { open.playlist.title } ?: open.playlist.title, statusTop, onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = bottomInset + 24.dp)) {
            item(key = "cover") {
                Column(Modifier.fillMaxWidth().padding(horizontal = QobuzDesign.gutter)) {
                    QobuzArtworkBox(open.playlist.imageUrl.ifBlank { detail?.imageUrl.orEmpty() }, Modifier.fillMaxWidth().aspectRatio(2.15f))
                    Spacer(Modifier.height(16.dp))
                    Text(detail?.title?.ifBlank { null } ?: open.playlist.title, style = MaterialTheme.typography.headlineMedium, color = QobuzDesign.textPrimary)
                    detail?.curator?.takeIf { it.isNotBlank() }?.let {
                        Text(copy.playlistBy.format(it), style = QobuzText.baseline)
                    }
                    detail?.summary?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = QobuzDesign.textTertiary)
                    }
                    Spacer(Modifier.height(16.dp))
                    if (tracks.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            QobuzPlayCircle(copy.play, 56.dp, { actions.home.playAll(tracks) })
                            QobuzIconAction(Icons.Rounded.Shuffle, LocalLevyraStrings.current.shuffle) { actions.home.playAll(tracks.shuffled()) }
                        }
                    }
                }
            }
            when {
                open.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = QobuzDesign.textAccent)
                    }
                }
                open.failed -> item(key = "failed") {
                    Column(Modifier.fillMaxWidth().padding(QobuzDesign.gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(copy.playlistUnavailable, style = QobuzText.baseline)
                        Text(copy.retry, style = MaterialTheme.typography.labelLarge, color = QobuzDesign.textPrimary,
                            modifier = Modifier.clip(QobuzDesign.buttonShape).background(QobuzDesign.surfaceSecondary)
                                .clickable(role = Role.Button, onClick = onRetry).padding(horizontal = 16.dp, vertical = 10.dp))
                    }
                }
                else -> items(tracks, key = { it.id }) { track ->
                    QobuzRankedRow(null, track, copy, track.id == currentId, { actions.home.playFrom(tracks, track) }, { actions.home.onTrackActions(track) },
                        Modifier.fillMaxWidth().padding(horizontal = QobuzDesign.gutter))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QobuzHomeEntry(
    data: AppleMusicHomeData,
    homeActions: AppleMusicHomeActions,
    viewModel: com.luc4n3x.levyra.viewmodel.HomeViewModel,
    state: com.luc4n3x.levyra.viewmodel.LevyraUiState,
    homeListState: LazyListState,
    homeBottomInset: Dp,
    homeRefreshing: Boolean,
    homePullState: PullToRefreshState,
    onPullRefresh: () -> Unit,
    composition: ComposedHomeRequest? = null
) {
    val strings = LocalLevyraStrings.current
    val needsExplore = state.exploreCategories.isEmpty() && composition?.showsSkinFeed != false
    LaunchedEffect(needsExplore, strings.code) { if (needsExplore) viewModel.ensureExplore(strings) }
    QobuzHomeContent(
        data = data,
        actions = QobuzHomeActions(
            home = homeActions,
            openExplore = viewModel::openExploreTab,
            openCategory = viewModel::openExploreCategory,
            toggleFollow = viewModel::toggleFollowArtist,
            openPlaylist = viewModel::openPlaylistHit,
            languageCode = state.languageCode,
            addToQueue = viewModel::addToQueue
        ),
        extras = QobuzHomeExtras(state.exploreCategories, state.followedArtistKeys, state.selectedChartId),
        homeListState = homeListState,
        homeBottomInset = homeBottomInset,
        homeRefreshing = homeRefreshing,
        homePullState = homePullState,
        onPullRefresh = onPullRefresh,
        composition = composition
    )
}

private fun QobuzHomeActions.withCategory(open: (ExploreCategory) -> Unit) =
    QobuzHomeActions(home, openExplore, open, toggleFollow, openPlaylist, languageCode, addToQueue)

@Composable
private fun QobuzCategoryScreen(category: ExploreCategory, actions: QobuzHomeActions, statusTop: Dp, bottomInset: Dp, onBack: () -> Unit) {
    val page = viewModel<com.luc4n3x.levyra.viewmodel.MoodGenrePageViewModel>()
    val state by page.state.collectAsStateWithLifecycle()
    LaunchedEffect(category.params, actions.languageCode) { page.load(category.params, actions.languageCode) }
    val key = "${actions.languageCode}:${category.params}"
    val entries = remember(state.sections, state.key, key) {
        if (state.key != key) emptyList()
        else state.sections.flatMap { section -> section.playlists.map { section.title to it } }
            .distinctBy { (_, playlist) -> playlist.playlistId.ifBlank { playlist.browseId } }
    }
    Column(Modifier.fillMaxSize().background(QobuzDesign.surfacePrimary).clickable(enabled = false) {}) {
        QobuzBackBar(category.title, statusTop, onBack)
        Box(Modifier.fillMaxWidth().height(1.dp).background(QobuzDesign.separator))
        when {
            state.key == key && state.failed -> Column(Modifier.padding(QobuzDesign.gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(LevyraQobuzSkinStrings.forCode(LocalLevyraStrings.current.code).playlistUnavailable, style = QobuzText.baseline)
                Text(
                    LevyraQobuzSkinStrings.forCode(LocalLevyraStrings.current.code).retry, style = MaterialTheme.typography.labelLarge, color = QobuzDesign.textPrimary,
                    modifier = Modifier.clip(QobuzDesign.buttonShape).background(QobuzDesign.surfaceSecondary)
                        .clickable(role = Role.Button) { page.load(category.params, actions.languageCode) }.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            entries.isEmpty() -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = QobuzDesign.textAccent)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = QobuzDesign.gutter, end = QobuzDesign.gutter, top = 20.dp, bottom = bottomInset + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(entries, key = { (_, playlist) -> playlist.playlistId.ifBlank { playlist.browseId } }) { (section, playlist) ->
                    QobuzPlaylistCard(playlist.title, playlist.thumbnailUrl, section.uppercase(), Dp.Unspecified) { actions.openPlaylist(playlist.toHit()) }
                }
            }
        }
    }
}
