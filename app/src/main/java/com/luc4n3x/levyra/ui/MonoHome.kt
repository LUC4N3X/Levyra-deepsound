package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.SpeedDialPin
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistAudienceWeight
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.MonoHomeCopy
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.theme.MonoCodeFont
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.viewmodel.HomeViewModel
import com.luc4n3x.levyra.viewmodel.LevyraUiState

internal val MonoRailPadding = PaddingValues(horizontal = MonoDesign.gutter)
private val MonoCurrentScrim = Color.Black.copy(alpha = 0.55f)
private val MonoRankWidth = 40.dp
private val MonoListenTile = 132.dp
private val MonoAlbumTile = 140.dp
private const val MonoQuickRows = 4
private const val MonoRailMax = 20
private const val MonoArtistRanks = 10
private const val MonoRankStagger = 0.06f
private const val MonoRankSpan = 0.46f
private const val MonoPageAll = 0
private const val MonoPageMood = 1

internal class MonoTileEntry(val key: String, val title: String, val artwork: String = "", val onAppear: (() -> Unit)? = null, val onClick: () -> Unit)

@Composable
internal fun monoRankStyle(size: Int): TextStyle =
    MaterialTheme.typography.labelMedium.copy(
        fontFamily = MonoCodeFont,
        fontSize = size.sp,
        lineHeight = (size + 4).sp,
        letterSpacing = (if (size > 30) -2 else 0).sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum"
    )

internal fun monoRank(rank: Int): String = rank.toString().padStart(2, '0')

@Composable
internal fun MonoArtwork(url: String, modifier: Modifier = Modifier) {
    Box(modifier.background(MonoDesign.raised), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.MusicNote, null, tint = MonoDesign.textFaint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
internal fun MonoIconKey(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, filled: Boolean = false) {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(if (filled) MonoDesign.accent else MonoDesign.raised)
                .border(1.dp, if (filled) MonoDesign.accent else MonoDesign.hairline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (filled) MonoDesign.onAccent else MonoDesign.text, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun MonoCompactRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    actionsLabel: String,
    onPlay: () -> Unit,
    onActions: () -> Unit,
    rank: Int? = null
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = MonoDesign.rowHeight)
            .levyraPressable(
                onClick = onPlay,
                role = Role.Button,
                pressedScale = LevyraPressScale.Row,
                onLongClick = onActions,
                onLongClickLabel = actionsLabel
            )
            .padding(start = MonoDesign.gutter, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Text(
                monoRank(rank),
                style = monoRankStyle(15),
                color = if (rank <= 3) MonoDesign.text else MonoDesign.textFaint,
                modifier = Modifier.width(MonoRankWidth)
            )
        }
        Box(Modifier.size(MonoDesign.rowArtwork).clip(MonoDesign.artShape).background(MonoDesign.raised)) {
            CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
            if (isCurrent) {
                Box(Modifier.matchParentSize().background(MonoCurrentScrim))
                MonoWaveLine(
                    isPlaying,
                    Modifier.matchParentSize().padding(horizontal = 6.dp, vertical = 14.dp),
                    color = MonoDesign.accent,
                    cycles = 3f,
                    strokeWidth = 1.5.dp
                )
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                track.title, style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) MonoDesign.accent else MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = actionsLabel, onClick = onActions),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = actionsLabel, tint = MonoDesign.textSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun MonoCard(
    title: String,
    subtitle: String,
    width: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    longClickLabel: String? = null,
    circular: Boolean = false,
    highlighted: Boolean = false,
    badge: String? = null,
    artwork: @Composable (Modifier) -> Unit
) {
    val shape = if (circular) CircleShape else MonoDesign.artShape
    Column(
        modifier.width(width).levyraPressable(
            onClick = onClick,
            role = Role.Button,
            pressedScale = LevyraPressScale.Tile,
            onLongClick = onLongClick,
            onLongClickLabel = longClickLabel
        )
    ) {
        Box(Modifier.size(width).clip(shape).background(MonoDesign.raised).border(1.dp, MonoDesign.hairline, shape)) {
            artwork(Modifier.fillMaxSize())
            if (badge != null) {
                Text(
                    badge,
                    style = monoRankStyle(12),
                    color = MonoDesign.text,
                    modifier = Modifier.padding(8.dp).clip(MonoDesign.pillShape).background(MonoCurrentScrim)
                        .padding(horizontal = 8.dp, vertical = 3.dp).clearAndSetSemantics {}
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title, style = MaterialTheme.typography.titleSmall,
            color = if (highlighted) MonoDesign.accent else MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun MonoTrackRail(
    tracks: List<Track>,
    width: Dp,
    currentId: String?,
    actionsLabel: String,
    onPlay: (Track) -> Unit,
    onActions: (Track) -> Unit,
    ranked: Boolean = false
) {
    LazyRow(contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
        itemsIndexed(tracks, key = { _, track -> track.id }, contentType = { _, _ -> "mono-track-card" }) { index, track ->
            MonoCard(
                title = track.title,
                subtitle = track.artist,
                width = width,
                onClick = { onPlay(track) },
                onLongClick = { onActions(track) },
                longClickLabel = actionsLabel,
                highlighted = track.id == currentId,
                badge = if (ranked) monoRank(index + 1) else null
            ) { CoverImage(track, it.clearAndSetSemantics {}) }
        }
    }
}

@Composable
internal fun MonoAlbumRail(albums: List<AlbumHit>, width: Dp, onOpen: (AlbumHit) -> Unit) {
    LazyRow(contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
        items(albums, key = { atelierAlbumKey(it) }, contentType = { "mono-album-card" }) { album ->
            MonoCard(
                title = album.title,
                subtitle = listOf(album.artist, album.year).filter(String::isNotBlank).joinToString(" · "),
                width = width,
                onClick = { onOpen(album) }
            ) { MonoArtwork(album.thumbnailUrl, it) }
        }
    }
}

@Composable
internal fun MonoWaveSkeleton(modifier: Modifier = Modifier, tile: Dp = MonoDesign.card, tiles: Int = 3) {
    Column(modifier.fillMaxWidth().clearAndSetSemantics {}) {
        MonoWaveLine(
            playing = true,
            modifier = Modifier.fillMaxWidth().height(26.dp).padding(horizontal = MonoDesign.gutter),
            color = MonoDesign.hairlineStrong,
            cycles = 6f,
            strokeWidth = 1.5.dp
        )
        Row(
            Modifier.fillMaxWidth().padding(MonoRailPadding).padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)
        ) {
            repeat(tiles) {
                Column {
                    Box(Modifier.size(tile).clip(MonoDesign.artShape).background(MonoDesign.surface))
                    Box(Modifier.padding(top = 10.dp).width(tile * 0.7f).height(10.dp).clip(MonoDesign.pillShape).background(MonoDesign.surface))
                    Box(Modifier.padding(top = 6.dp).width(tile * 0.45f).height(8.dp).clip(MonoDesign.pillShape).background(MonoDesign.surface))
                }
            }
        }
    }
}

@Composable
internal fun MonoSection(
    title: String,
    overline: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = MonoDesign.sectionGap)) {
        MonoSectionHeader(title, overline = overline, action = action, onAction = onAction)
        Spacer(Modifier.height(MonoDesign.headerToContent))
        content()
    }
}

@Composable
private fun MonoHomeAvatar(listener: HomeListener) {
    val initial = remember(listener.name) { homePersonalOrbitInitial(listener.name.trim()) }
    Box(Modifier.size(40.dp).clip(CircleShape).background(MonoDesign.raised), contentAlignment = Alignment.Center) {
        if (initial != null || listener.avatarUrl.isNotBlank() || listener.photoPath.isNotBlank()) {
            HomeOrbitAvatar(initial.orEmpty(), listener.photoPath, listener.photoVersion, listener.avatarUrl)
        } else {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = MonoDesign.textSecondary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun MonoHomeTopBar(listener: HomeListener, page: Int, copy: MonoHomeCopy, onPage: (Int) -> Unit, onOpenSettings: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Row(
        Modifier.fillMaxWidth().padding(top = statusTop + 8.dp, start = MonoDesign.gutter, end = MonoDesign.gutter - 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MonoHomeAvatar(listener)
        MonoPill(copy.all, page == MonoPageAll, { onPage(MonoPageAll) }, Modifier.semantics { selected = page == MonoPageAll })
        MonoPill(copy.mood, page == MonoPageMood, { onPage(MonoPageMood) }, Modifier.semantics { selected = page == MonoPageMood })
        Spacer(Modifier.weight(1f))
        MonoIconKey(Icons.Rounded.Settings, strings.settings, onOpenSettings)
    }
}

private fun Modifier.monoEntrance(progress: () -> Float, index: Int, lift: Float): Modifier = graphicsLayer {
    val p = monoStagger(progress(), index)
    alpha = p
    translationY = (1f - p) * lift
}

private fun monoStagger(value: Float, index: Int): Float =
    ((value - index * MonoRankStagger) / MonoRankSpan).coerceIn(0f, 1f)

@Composable
private fun MonoArtistChart(artists: List<ArtistHit>, copy: MonoHomeCopy, onOpen: (ArtistHit) -> Unit) {
    val ranked = remember(artists) { artists.take(MonoArtistRanks) }
    val animations = LocalAnimationsEnabled.current
    val entrance = remember(ranked) { Animatable(if (animations) 0f else 1f) }
    LaunchedEffect(ranked, animations) {
        if (animations) entrance.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) else entrance.snapTo(1f)
    }
    val progress = { entrance.value }
    Column(Modifier.fillMaxWidth()) {
        ranked.forEachIndexed { index, artist ->
            key(artist.browseId.ifBlank { artist.name }) {
                MonoArtistRankRow(index + 1, artist, progress, index, copy) { onOpen(artist) }
            }
        }
    }
}

@Composable
private fun MonoArtistRankRow(
    rank: Int,
    artist: ArtistHit,
    progress: () -> Float,
    index: Int,
    copy: MonoHomeCopy,
    onOpen: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 80.dp)
            .monoEntrance(progress, index, 28f)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Row, onClickLabel = artist.name)
            .padding(horizontal = MonoDesign.gutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            rank.toString(),
            style = monoRankStyle(28),
            color = if (rank <= 3) MonoDesign.text else MonoDesign.textSecondary,
            modifier = Modifier.width(44.dp).semantics { contentDescription = copy.rankLabel.format(rank) }
        )
        Box(Modifier.size(64.dp).clip(CircleShape).background(MonoDesign.surface)) {
            MonoArtwork(artist.thumbnailUrl, Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f).padding(start = 18.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(artist.name, style = MaterialTheme.typography.titleMedium, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (artist.subscribers.isNotBlank()) {
                Text(artist.subscribers, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MonoHomeEmpty(message: String, retry: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter).padding(top = MonoDesign.sectionGap)) {
        MonoWaveLine(false, Modifier.fillMaxWidth().height(24.dp), color = MonoDesign.hairlineStrong)
        Text(message, style = MaterialTheme.typography.titleMedium, color = MonoDesign.textSecondary, modifier = Modifier.padding(vertical = 16.dp))
        MonoPill(retry, selected = false, onClick = onRetry)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonoHomeEntry(
    data: AppleMusicHomeData,
    homeActions: AppleMusicHomeActions,
    viewModel: HomeViewModel,
    state: LevyraUiState,
    homeListState: LazyListState,
    homeBottomInset: Dp,
    homeRefreshing: Boolean,
    homePullState: PullToRefreshState,
    onPullRefresh: () -> Unit,
    composition: ComposedHomeRequest? = null,
    speedDialPins: List<SpeedDialPin> = emptyList()
) {
    val strings = LocalLevyraStrings.current
    val render by viewModel.renderState.collectAsStateWithLifecycle()
    val releaseSection = render.derived.newReleases
    val showReleases = state.interfaceSettings.showNewReleases
    var page by rememberSaveable { mutableIntStateOf(MonoPageAll) }
    val needsReleases = composition == null && showReleases && releaseSection == null && state.exploreNewReleases.isEmpty()
    val needsMoods = page == MonoPageMood && state.exploreCategories.isEmpty()
    LaunchedEffect(needsReleases || needsMoods, strings.code) { if (needsReleases || needsMoods) viewModel.ensureExplore(strings) }
    MonoHomeContent(
        MonoHomeModel(
            data = data,
            actions = homeActions,
            viewModel = viewModel,
            state = state,
            releaseTracks = if (showReleases) releaseSection?.tracks.orEmpty() else emptyList(),
            releaseAlbums = if (showReleases) state.exploreNewReleases else emptyList(),
            pinnedCards = remember(speedDialPins, viewModel) { speedDialCards(speedDialPins, viewModel::openSpeedDialPin) },
            composition = composition
        ),
        page = page,
        onPage = { page = it },
        homeListState = homeListState,
        homeBottomInset = homeBottomInset,
        homeRefreshing = homeRefreshing,
        homePullState = homePullState,
        onPullRefresh = onPullRefresh
    )
}

private class MonoHomeModel(
    val data: AppleMusicHomeData,
    val actions: AppleMusicHomeActions,
    val viewModel: HomeViewModel,
    val state: LevyraUiState,
    val releaseTracks: List<Track>,
    val releaseAlbums: List<AlbumHit>,
    val pinnedCards: List<ComposedCard>,
    val composition: ComposedHomeRequest?
)

private class MonoHomeFeed(
    val picks: List<Track>,
    val picksTitle: String,
    val charts: List<Track>,
    val rotation: List<ComposedCard>,
    val recents: List<Track>,
    val albums: List<AlbumHit>,
    val newTracks: List<Track>,
    val newAlbums: List<AlbumHit>,
    val artists: List<ArtistHit>,
    val forgotten: List<Track>
) {
    val isEmpty: Boolean
        get() = picks.isEmpty() && charts.isEmpty() && rotation.isEmpty() && recents.isEmpty() && albums.isEmpty() &&
            newTracks.isEmpty() && newAlbums.isEmpty() && artists.isEmpty() && forgotten.isEmpty()
}

@Composable
private fun rememberMonoHomeFeed(model: MonoHomeModel, copy: MonoHomeCopy): MonoHomeFeed {
    val strings = LocalLevyraStrings.current
    val data = model.data
    val actions = model.actions
    val recordings = remember(model.state.recentListens) { LevyraPersonalOrbit.distinctRecordings(model.state.recentListens) }
    val orbit = remember(data.personalTracks, data.showPersonal) {
        if (data.showPersonal) data.personalTracks.distinctBy(Track::id).take(MonoRailMax) else emptyList()
    }
    val rotation = remember(orbit, data.collections, actions, copy, strings.personalOrbitTitle) {
        buildList {
            orbit.firstOrNull()?.let { lead ->
                add(ComposedCard("mono-orbit", strings.personalOrbitTitle, copy.tracksCount.format(orbit.size), lead.largeThumbnailUrl.ifBlank { lead.thumbnailUrl }, track = lead) { actions.playAll(orbit) })
            }
            data.collections.distinctBy { it.id }.filter { it.tracks.isNotEmpty() }.forEach { collection ->
                val lead = collection.tracks.first()
                add(ComposedCard("mono-collection-${collection.id}", collection.title, copy.tracksCount.format(collection.tracks.size), lead.thumbnailUrl, track = lead) { actions.openCollection(collection.id) })
            }
        }
    }
    return MonoHomeFeed(
        picks = remember(data.quickPicks) { data.quickPicks?.tracks.orEmpty().distinctBy(Track::id) },
        picksTitle = data.quickPicks?.title?.ifBlank { null } ?: strings.quickPicks,
        charts = remember(data.charts) { data.charts.distinctBy(Track::id).take(MonoRailMax) },
        rotation = rotation,
        recents = remember(recordings) { recordings.distinctBy(Track::id).take(MonoRailMax) },
        albums = remember(data.albums, data.showAlbums) { if (data.showAlbums) data.albums.distinctBy(::atelierAlbumKey).take(MonoRailMax) else emptyList() },
        newTracks = remember(model.releaseTracks) { model.releaseTracks.distinctBy(Track::id).take(MonoRailMax) },
        newAlbums = remember(model.releaseAlbums) { model.releaseAlbums.distinctBy(::atelierAlbumKey).take(MonoRailMax) },
        artists = remember(data.artists, data.showArtists) {
            if (data.showArtists) data.artists.filter { it.name.isNotBlank() }.distinctBy { it.browseId.ifBlank { it.name } } else emptyList()
        },
        forgotten = remember(data.forgottenFavorites) { data.forgottenFavorites.distinctBy(Track::id).take(MonoRailMax) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MonoHomeContent(
    model: MonoHomeModel,
    page: Int,
    onPage: (Int) -> Unit,
    homeListState: LazyListState,
    homeBottomInset: Dp,
    homeRefreshing: Boolean,
    homePullState: PullToRefreshState,
    onPullRefresh: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoHomeStrings.forCode(strings.code) }
    val homeRadio = LocalHomeRadio.current
    val state = model.state
    val feed = rememberMonoHomeFeed(model, copy)
    val zones = remember(strings) { ExploreCatalog.getZones(strings) }
    val browse = remember(state.exploreCategories, zones) { monoBrowseTiles(state.exploreCategories, zones) }
    val viewModel = model.viewModel
    val categoriesLoading = state.isExploreCategoriesLoading
    val moodEntries = remember(browse, categoriesLoading, viewModel) {
        monoBrowseEntries(browse, categoriesLoading, viewModel::openExploreCategoryPage, viewModel::openExploreZone)
    }
    val isPlaying = rememberUpdatedState(state.isPlaying)
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    BoxWithConstraints(Modifier.fillMaxSize().background(MonoDesign.base)) {
        val heroCard = (maxWidth * 0.56f).coerceIn(180.dp, 260.dp)
        val releaseCard = (maxWidth * 0.62f).coerceIn(200.dp, 300.dp)
        val kit = remember(model.data) { MonoHomeKit(model.data, isPlaying) }
        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize()
                .pullToRefresh(isRefreshing = homeRefreshing, state = homePullState, onRefresh = onPullRefresh),
            contentPadding = PaddingValues(bottom = homeBottomInset + MonoDesign.sectionGap)
        ) {
            item(key = "mono-home-top", contentType = "mono-home-top") {
                MonoHomeTopBar(model.data.listener, page, copy, onPage, model.actions.openSettings)
            }
            if (page == MonoPageMood) {
                monoBrowseSections("mono-home", moodEntries, categoriesLoading, copy)
                return@LazyColumn
            }
            val composition = model.composition
            if (composition != null) {
                composedHomeSections(composition, kit) {
                    monoHomeFeed(model, feed, copy, strings, kit, heroCard, releaseCard)
                }
                monoHomeFallback(feed.isEmpty && monoComposedSectionsEmpty(composition.content), state, copy, viewModel)
                return@LazyColumn
            }
            monoHomeFallback(feed.isEmpty && model.pinnedCards.isEmpty() && !(homeRadio.visible && homeRadio.stations.isNotEmpty()), state, copy, viewModel)
            monoHomeFeed(model, feed, copy, strings, kit, heroCard, releaseCard)
            favoriteRadioSection(homeRadio)
        }
        PullToRefreshDefaults.LoadingIndicator(
            state = homePullState,
            isRefreshing = homeRefreshing,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = statusTop + 16.dp)
        )
    }
}

private fun monoComposedSectionsEmpty(content: ComposedHomeContent): Boolean =
    content.newReleases.isEmpty() && content.videos.isEmpty() && content.moreSections.isEmpty() &&
        content.speedDialPins.isEmpty() && content.releaseRadar.isEmpty() && content.similarArtists.isEmpty() &&
        content.resonanceTracks.isEmpty() && content.discovery == null && !content.radio.visible

private fun LazyListScope.monoHomeFallback(nothingToShow: Boolean, state: LevyraUiState, copy: MonoHomeCopy, viewModel: HomeViewModel) {
    val loading = state.isLoadingHome || state.isLoadingCharts || state.homeAlbumsLoading || state.homeArtistsLoading || state.isNewReleasesLoading
    if (!nothingToShow || loading) return
    item(key = "mono-home-empty", contentType = "mono-empty") {
        MonoHomeEmpty(state.homeError ?: copy.emptyBody, copy.retry, viewModel::retryHomeContent)
    }
}

private fun LazyListScope.monoSkeletonSection(key: String, title: String, overline: String?, tile: Dp, tiles: Int) {
    item(key = key, contentType = "mono-skeleton-section") {
        MonoSection(title, overline) { MonoWaveSkeleton(tile = tile, tiles = tiles) }
    }
}

private fun LazyListScope.monoHomeFeed(
    model: MonoHomeModel,
    feed: MonoHomeFeed,
    copy: MonoHomeCopy,
    strings: LevyraStrings,
    kit: MonoHomeKit,
    heroCard: Dp,
    releaseCard: Dp
) {
    val state = model.state
    val data = model.data
    val actions = model.actions
    val currentId = data.currentId
    val settings = state.interfaceSettings
    if (feed.picks.isNotEmpty()) {
        item(key = "mono-picks-header", contentType = "mono-header") {
            Column(Modifier.padding(top = 20.dp)) {
                MonoSectionHeader(feed.picksTitle, overline = copy.startHere, action = strings.playAll) { actions.playAll(feed.picks) }
                Spacer(Modifier.height(6.dp))
            }
        }
        items(feed.picks.take(MonoQuickRows), key = { "mono-pick-${it.id}" }, contentType = { "mono-compact-row" }) { track ->
            MonoCompactRow(
                track = track,
                isCurrent = track.id == currentId,
                isPlaying = state.isPlaying,
                actionsLabel = strings.songOptions,
                onPlay = { actions.playFrom(feed.picks, track) },
                onActions = { actions.onTrackActions(track) }
            )
        }
    } else if (state.isLoadingHome) {
        monoSkeletonSection("mono-picks-skeleton", feed.picksTitle, copy.startHere, MonoDesign.card, 3)
    }
    if (model.pinnedCards.isNotEmpty()) {
        item(key = "mono-pinned", contentType = "mono-cards") { kit.Cards(strings.speedDialCopy().title, model.pinnedCards) }
    }
    data.soundtrack?.let { hero ->
        item(key = "mono-soundtrack", contentType = "mono-hero") { kit.Hero(hero) }
    }
    if (feed.charts.isNotEmpty()) {
        item(key = "mono-popular", contentType = "mono-popular") {
            MonoSection(copy.popularNow, copy.trendingOverline, data.countryLabel.ifBlank { null }, actions.pickCountry.takeIf { data.countryLabel.isNotBlank() }) {
                MonoPopularRail(feed.charts, heroCard, currentId, actions)
            }
        }
    } else if (settings.showCharts && state.isLoadingCharts) {
        monoSkeletonSection("mono-popular-skeleton", copy.popularNow, copy.trendingOverline, MonoDesign.cardWide, 2)
    }
    if (feed.rotation.isNotEmpty()) {
        item(key = "mono-rotation", contentType = "mono-cards") {
            MonoSection(copy.rotation, copy.rotationOverline) { kit.CardRail(feed.rotation, MonoDesign.card) }
        }
    }
    if (feed.recents.isNotEmpty()) {
        item(key = "mono-again", contentType = "mono-rail") {
            MonoSection(copy.listenAgain, copy.againOverline) {
                MonoTrackRail(feed.recents, MonoListenTile, currentId, strings.songOptions, { track ->
                    if (track.id == state.currentTrack?.id) {
                        if (!state.isResolving) model.viewModel.togglePlay()
                    } else {
                        actions.playFrom(feed.recents, track)
                    }
                }, actions.onTrackActions)
            }
        }
    }
    if (feed.newAlbums.isNotEmpty() || feed.newTracks.isNotEmpty()) {
        item(key = "mono-new", contentType = "mono-rail") {
            MonoSection(copy.newAlbums, copy.freshOverline) {
                if (feed.newAlbums.isNotEmpty()) {
                    MonoAlbumRail(feed.newAlbums, releaseCard, actions.openAlbum)
                } else {
                    MonoTrackRail(feed.newTracks, releaseCard, currentId, strings.songOptions, { actions.playFrom(feed.newTracks, it) }, actions.onTrackActions)
                }
            }
        }
    } else if (settings.showNewReleases && state.isNewReleasesLoading) {
        monoSkeletonSection("mono-new-skeleton", copy.newAlbums, copy.freshOverline, MonoDesign.cardWide, 2)
    }
    if (feed.albums.isNotEmpty()) {
        item(key = "mono-albums", contentType = "mono-rail") {
            MonoSection(strings.albumsForYou, copy.forYouOverline) { MonoAlbumRail(feed.albums, MonoAlbumTile, actions.openAlbum) }
        }
    } else if (data.showAlbums && state.homeAlbumsLoading) {
        monoSkeletonSection("mono-albums-skeleton", strings.albumsForYou, copy.forYouOverline, MonoAlbumTile, 3)
    }
    if (feed.artists.isNotEmpty()) {
        item(key = "mono-artists", contentType = "mono-artists") {
            MonoSection(copy.topArtists, copy.artistChartOverline) { MonoArtistChart(feed.artists, copy, actions.openArtist) }
        }
    } else if (data.showArtists && state.homeArtistsLoading) {
        monoSkeletonSection("mono-artists-skeleton", copy.topArtists, copy.artistChartOverline, MonoDesign.rowArtwork, 5)
    }
    if (feed.forgotten.isNotEmpty()) {
        item(key = "mono-forgotten", contentType = "mono-rail") {
            MonoSection(strings.forgottenFavorites) {
                MonoTrackRail(feed.forgotten, MonoDesign.card, currentId, strings.songOptions, { actions.playFrom(feed.forgotten, it) }, actions.onTrackActions)
            }
        }
    }
}

@Composable
private fun MonoPopularRail(charts: List<Track>, heroCard: Dp, currentId: String?, actions: AppleMusicHomeActions) {
    val strings = LocalLevyraStrings.current
    LazyRow(
        contentPadding = MonoRailPadding,
        horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap),
        verticalAlignment = Alignment.Top
    ) {
        itemsIndexed(charts, key = { _, track -> track.id }, contentType = { index, _ -> if (index == 0) "mono-popular-lead" else "mono-popular-card" }) { index, track ->
            MonoCard(
                title = track.title,
                subtitle = track.artist,
                width = if (index == 0) heroCard else MonoDesign.cardWide,
                onClick = { actions.playFrom(charts, track) },
                onLongClick = { actions.onTrackActions(track) },
                longClickLabel = strings.songOptions,
                highlighted = track.id == currentId,
                badge = monoRank(index + 1)
            ) { CoverImage(track, it.clearAndSetSemantics {}, highRes = index == 0) }
        }
    }
}

internal class MonoHomeKit(
    private val data: AppleMusicHomeData,
    private val isPlaying: State<Boolean>
) : SkinHomeKit {

    @Composable
    override fun Songs(
        sectionKey: String,
        title: String,
        tracks: List<Track>,
        ranked: Boolean,
        currentId: String?,
        onPlay: (Track) -> Unit,
        onPlayAll: () -> Unit,
        onTrackActions: (Track) -> Unit
    ) {
        val strings = LocalLevyraStrings.current
        val unique = remember(tracks) { tracks.distinctBy(Track::id).take(50) }
        if (unique.isEmpty()) return
        MonoSection(title, if (ranked) data.countryLabel.ifBlank { null } else null, strings.playAll, onPlayAll) {
            if (ranked || sectionKey == "quick-picks") {
                var expanded by rememberSaveable(sectionKey) { mutableStateOf(false) }
                val collapsed = if (ranked) 10 else 5
                Column {
                    (if (expanded) unique else unique.take(collapsed)).forEachIndexed { index, track ->
                        key(track.id) {
                            MonoCompactRow(
                                track = track,
                                isCurrent = track.id == currentId,
                                isPlaying = isPlaying.value,
                                actionsLabel = strings.songOptions,
                                onPlay = { onPlay(track) },
                                onActions = { onTrackActions(track) },
                                rank = if (ranked) index + 1 else null
                            )
                        }
                    }
                    if (unique.size > collapsed) {
                        MonoPill(
                            if (expanded) strings.showLess else strings.showMore, selected = false,
                            onClick = { expanded = !expanded },
                            modifier = Modifier.padding(horizontal = MonoDesign.gutter, vertical = 8.dp)
                        )
                    }
                }
            } else {
                MonoTrackRail(unique, MonoDesign.card, currentId, strings.songOptions, onPlay, onTrackActions)
            }
        }
    }

    @Composable
    fun CardRail(cards: List<ComposedCard>, width: Dp) {
        val strings = LocalLevyraStrings.current
        LazyRow(contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap), verticalAlignment = Alignment.Top) {
            items(cards, key = { it.key }, contentType = { if (it.circular) "mono-card-round" else "mono-card" }) { card ->
                MonoCard(
                    title = card.title,
                    subtitle = card.subtitle,
                    width = if (card.wide) width * 1.6f else width,
                    onClick = card.onClick,
                    onLongClick = card.onLongClick,
                    longClickLabel = card.onLongClick?.let { strings.songOptions },
                    circular = card.circular
                ) { modifier ->
                    val track = card.track
                    if (track != null) CoverImage(track, modifier.clearAndSetSemantics {}) else MonoArtwork(card.artworkUrl, modifier)
                }
            }
        }
    }

    @Composable
    override fun Cards(title: String, cards: List<ComposedCard>) {
        val unique = remember(cards) { cards.distinctBy(ComposedCard::key) }
        if (unique.isEmpty()) return
        MonoSection(title) { CardRail(unique, MonoDesign.card) }
    }

    @Composable
    override fun Chips(labels: List<String>, onSelect: (Int) -> Unit) {
        LazyRow(
            modifier = Modifier.padding(top = MonoDesign.sectionGap),
            contentPadding = MonoRailPadding,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(labels, key = { index, label -> "$index-$label" }, contentType = { _, _ -> "mono-chip" }) { index, label ->
                MonoPill(label, selected = false, onClick = { onSelect(index) })
            }
        }
    }

    @Composable
    override fun Hero(hero: StudioHero) {
        val strings = LocalLevyraStrings.current
        val track = hero.track
        val active = hero.isCurrent && hero.isPlaying
        Column(
            Modifier.fillMaxWidth().padding(top = MonoDesign.sectionGap).padding(horizontal = MonoDesign.gutter)
                .monoPanel(MonoDesign.panelShape)
                .levyraPressable(onClick = hero.onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface, onClickLabel = strings.play)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(96.dp).clip(MonoDesign.artShape)) { CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {}, highRes = true) }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (hero.label.isNotBlank()) MonoOverline(hero.label, color = MonoDesign.accent)
                    Text(track.title, style = MaterialTheme.typography.titleLarge, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                MonoIconKey(if (active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (active) strings.pause else strings.play, hero.onOpen, filled = true)
            }
            if (hero.lead.isNotBlank()) {
                Text(hero.lead, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
            }
            MonoWaveLine(active, Modifier.fillMaxWidth().padding(top = 12.dp).height(22.dp), color = MonoDesign.accent, cycles = 8f, strokeWidth = 1.5.dp)
        }
    }

    @Composable
    override fun Quotes(title: String, quotes: List<ComposedQuote>) {
        val unique = remember(quotes) { quotes.distinctBy(ComposedQuote::key) }
        if (unique.isEmpty()) return
        val strings = LocalLevyraStrings.current
        MonoSection(title) {
            LazyRow(contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
                items(unique, key = { it.key }, contentType = { "mono-quote" }) { quote ->
                    Column(
                        Modifier.width(280.dp).monoPanel()
                            .levyraPressable(onClick = quote.onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface)
                            .padding(16.dp)
                    ) {
                        if (quote.meta.isNotBlank()) MonoOverline(quote.meta)
                        if (quote.quote != quote.track.artist) {
                            Text(quote.quote, style = MaterialTheme.typography.titleMedium, color = MonoDesign.text, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        }
                        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(MonoDesign.artShape)) { CoverImage(quote.track, Modifier.fillMaxSize().clearAndSetSemantics {}) }
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(quote.track.title, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(quote.track.artist, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            MonoIconKey(Icons.Rounded.PlayArrow, strings.play, quote.onPlay)
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Discovery(model: ComposedDiscovery) {
        val unique = remember(model.cards) { model.cards.distinctBy(ComposedCard::key) }
        MonoSection(model.title) {
            Column(Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter).monoPanel().padding(16.dp)) {
                Text(model.lead, style = MaterialTheme.typography.titleMedium, color = MonoDesign.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                MonoPill(model.startLabel, selected = true, onClick = model.onOpen, modifier = Modifier.padding(top = 12.dp))
            }
            when {
                unique.isNotEmpty() -> Box(Modifier.padding(top = MonoDesign.headerToContent)) { CardRail(unique, MonoDesign.card) }
                model.loading -> MonoWaveSkeleton(Modifier.padding(top = MonoDesign.headerToContent))
            }
        }
    }

    @Composable
    override fun Notice(message: String, actionLabel: String?, onAction: (() -> Unit)?) {
        Column(Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter).padding(top = MonoDesign.sectionGap)) {
            Text(message, style = MaterialTheme.typography.titleMedium, color = MonoDesign.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (actionLabel != null && onAction != null) {
                MonoPill(actionLabel, selected = false, onClick = onAction, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }

    @Composable
    override fun LoadingShelf(title: String) {
        MonoSection(title) { MonoWaveSkeleton() }
    }
}
