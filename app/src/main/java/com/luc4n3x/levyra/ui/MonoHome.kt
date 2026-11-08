package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraMediaTitle
import com.luc4n3x.levyra.ui.components.levyraMarquee
import com.luc4n3x.levyra.ui.experiences.LocalMusicExperiences
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.FormatQuote
import com.luc4n3x.levyra.viewmodel.EXPLORE_BROWSE_ID
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.graphics.lerp
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Groups
import com.luc4n3x.levyra.ui.i18n.podcastCopy
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.BiasAlignment
import androidx.compose.foundation.layout.fillMaxHeight
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import androidx.compose.ui.semantics.heading
import com.luc4n3x.levyra.data.ArtworkPalette
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.viewmodel.YouTubeMusicHomeViewModel
import com.luc4n3x.levyra.viewmodel.featuredPlaylistKey
import com.luc4n3x.levyra.viewmodel.CuratedFeaturedPlaylist
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItemKind
import com.luc4n3x.levyra.domain.PlaylistHit
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
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
import com.luc4n3x.levyra.ui.i18n.homeVideoShelfTitle
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
private val MonoAlbumTile = 140.dp
private const val MonoQuickRows = 4
private const val MonoQuickColumns = 5
private val MonoSegmentWidth = 76.dp
private const val MonoChartRows = 5
private const val MonoChartMax = 50
private const val MonoMixedForYouBrowseId = "FEmusic_mixed_for_you"
private const val MonoFeaturedPlaylistsBrowseId = "FEmusic_moods_and_genres"
private const val MonoFeaturedPageLimit = 3
private const val MonoPlaylistShelves = 4
private val MonoGlowHeight = 460.dp
private val MonoSoundtrackHeight = 400.dp
private const val MonoSoundtrackPortraitShare = 0.86f
private const val MonoRailMax = 20
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
            LevyraMediaTitle(
                track.title, explicit = track.explicit, marqueeEnabled = LocalAnimationsEnabled.current, repeatDelayMillis = 2_400,  style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) MonoDesign.accent else MonoDesign.text, maxLines = 1, overflow = TextOverflow.Clip,
                modifier = Modifier
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
    subtitleLines: Int = 1,
    titleLines: Int = 1,
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
            color = if (highlighted) MonoDesign.accent else MonoDesign.text, maxLines = titleLines,
            overflow = if (titleLines > 1) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = if (titleLines > 1) Modifier else Modifier.levyraMarquee(enabled = LocalAnimationsEnabled.current, repeatDelayMillis = 2_400)
        )
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = subtitleLines, overflow = TextOverflow.Ellipsis)
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
    val railState = rememberLazyListState()
    val railMotion = glacierRailMotionEnabled()
    LazyRow(state = railState, contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
        itemsIndexed(tracks, key = { _, track -> track.id }, contentType = { _, _ -> "mono-track-card" }) { index, track ->
            MonoCard(
                modifier = Modifier.glacierRailItemMotion(railMotion, railState, track.id),
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
    val railState = rememberLazyListState()
    val railMotion = glacierRailMotionEnabled()
    LazyRow(state = railState, contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
        items(albums, key = { atelierAlbumKey(it) }, contentType = { "mono-album-card" }) { album ->
            MonoCard(
                modifier = Modifier.glacierRailItemMotion(railMotion, railState, atelierAlbumKey(album)),
                title = album.title,
                subtitle = listOf(album.artist, album.year).filter(String::isNotBlank).joinToString(" · "),
                width = width,
                onClick = { onOpen(album) }
            ) { MonoArtwork(album.thumbnailUrl, it) }
        }
    }
}

@Composable
private fun MonoArtistRail(artists: List<ArtistHit>, onOpen: (ArtistHit) -> Unit) {
    val strings = LocalLevyraStrings.current
    val railState = rememberLazyListState()
    val railMotion = glacierRailMotionEnabled()
    LazyRow(state = railState, contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
        items(artists, key = { it.browseId.ifBlank { it.name } }, contentType = { "mono-artist-card" }) { artist ->
            MonoCard(
                modifier = Modifier.glacierRailItemMotion(railMotion, railState, artist.browseId.ifBlank { artist.name }),
                title = artist.name,
                subtitle = artistAudienceLabel(artist.subscribers, strings.code, strings.monthlyListeners),
                subtitleLines = 2,
                width = MonoDesign.artistCircle,
                onClick = { onOpen(artist) },
                circular = true
            ) { MonoArtwork(artist.thumbnailUrl, it) }
        }
    }
}

@Composable
private fun MonoReleaseRail(releases: List<AlbumHit>, onOpen: (AlbumHit) -> Unit) {
    val albums = remember(releases) { releases.distinctBy(::atelierAlbumKey) }
    val labels = searchDiscographyCopy(LocalLevyraStrings.current.code)
    val railState = rememberLazyListState()
    val railMotion = glacierRailMotionEnabled()
    LazyRow(state = railState, contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
        items(albums, key = { atelierAlbumKey(it) }, contentType = { "mono-release-card" }) { album ->
            val badge = when (album.releaseType) {
                ReleaseType.Album, ReleaseType.Compilation -> labels.album
                ReleaseType.Single -> labels.single
                ReleaseType.Ep -> "EP"
                ReleaseType.Unknown -> ""
            }
            MonoCard(
                modifier = Modifier.glacierRailItemMotion(railMotion, railState, atelierAlbumKey(album)),
                title = album.title,
                subtitle = listOf(badge, album.artist).filter(String::isNotBlank).joinToString(" · "),
                width = MonoDesign.cardWide,
                onClick = { onOpen(album) }
            ) { MonoArtwork(album.thumbnailUrl, it) }
        }
    }
}

internal fun monoWithoutYoutube(text: String): String =
    text.split(" • ", " · ").map(String::trim).filter { it.isNotBlank() && !it.contains("youtube", ignoreCase = true) }.joinToString(" • ")

@Composable
internal fun rememberNewMusicVideos(code: String): List<YoutubeMusicHomeItem> {
    val videosHome = viewModel<YouTubeMusicHomeViewModel>(key = "mono-new-videos")
    val videosState by videosHome.state.collectAsStateWithLifecycle()
    LaunchedEffect(code) { videosHome.load(code, browseId = EXPLORE_BROWSE_ID) }
    return remember(videosState.shelves, code) {
        val items = monoExploreNewVideos(videosState.shelves, Int.MAX_VALUE)
        val byTrack = items.associateBy { it.track?.id }
        com.luc4n3x.levyra.domain.LocalMusicVideoFilter.localFirst(items.mapNotNull { it.track }, code).mapNotNull { byTrack[it.id] }.take(MonoRailMax)
    }
}

@Composable
internal fun rememberCuratedPlaylistShelves(code: String): List<YoutubeMusicHomeShelf> {
    val mixedHome = viewModel<YouTubeMusicHomeViewModel>(key = "mono-mixed-for-you")
    val mixedState by mixedHome.state.collectAsStateWithLifecycle()
    LaunchedEffect(code) { mixedHome.load(code, browseId = MonoMixedForYouBrowseId) }
    val ytHome = viewModel<YouTubeMusicHomeViewModel>(key = "mono-yt-home")
    val ytHomeState by ytHome.state.collectAsStateWithLifecycle()
    LaunchedEffect(code) { ytHome.load(code) }
    val featuredShelves = remember(ytHomeState.allShelves) {
        monoPlaylistShelves(ytHomeState.allShelves.filter { it.headerBrowseId == MonoFeaturedPlaylistsBrowseId })
    }
    LaunchedEffect(featuredShelves.isEmpty(), ytHomeState.pages, ytHomeState.loading, ytHomeState.loadingMore) {
        if (featuredShelves.isEmpty() && ytHomeState.canLoadMore && ytHomeState.pages < MonoFeaturedPageLimit) ytHome.loadMore()
    }
    LaunchedEffect(code) { mixedHome.requestFeaturedPlaylist(CuratedFeaturedPlaylist.YOUTUBE_ID, code) }
    val featuredPlaylists by mixedHome.featuredPlaylists.collectAsStateWithLifecycle()
    val mixLabel = LocalLevyraStrings.current.mix
    val featuredItem = featuredPlaylists[featuredPlaylistKey(CuratedFeaturedPlaylist.YOUTUBE_ID, code)]
        ?.let { item -> if (item.subtitle.isBlank()) item.copy(subtitle = mixLabel) else item }
    return remember(mixedState.allShelves, featuredShelves, ytHomeState.allShelves, featuredItem) {
        withFeaturedCuratedPlaylist(
            monoPlaylistShelves(mixedState.allShelves).ifEmpty { featuredShelves }.ifEmpty { monoPlaylistShelves(ytHomeState.allShelves) },
            featuredItem
        )
    }
}

internal fun withFeaturedCuratedPlaylist(shelves: List<YoutubeMusicHomeShelf>, featured: YoutubeMusicHomeItem?): List<YoutubeMusicHomeShelf> {
    val first = shelves.firstOrNull() ?: return shelves
    if (featured == null || first.items.any { it.playlist?.playlistId == featured.playlist?.playlistId }) return shelves
    val items = first.items.toMutableList().apply { add(minOf(1, size), featured) }
    return listOf(first.copy(items = items)) + shelves.drop(1)
}

internal fun monoPlaylistShelves(shelves: List<YoutubeMusicHomeShelf>): List<YoutubeMusicHomeShelf> =
    shelves.filter { shelf ->
        shelf.items.isNotEmpty() && shelf.items.all { it.kind == YoutubeMusicHomeItemKind.Playlist && it.playlist != null }
    }.take(MonoPlaylistShelves)

@Composable
private fun MonoPlaylistRail(shelfItems: List<YoutubeMusicHomeItem>, onOpen: (PlaylistHit) -> Unit) {
    val items = remember(shelfItems) { shelfItems.distinctBy { it.key } }
    val railState = rememberLazyListState()
    val railMotion = glacierRailMotionEnabled()
    LazyRow(state = railState, contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
        items(items, key = { it.key }, contentType = { "mono-playlist-card" }) { item ->
            MonoCard(
                modifier = Modifier.glacierRailItemMotion(railMotion, railState, item.key),
                title = item.title,
                subtitle = monoWithoutYoutube(item.subtitle),
                titleLines = 2,
                width = MonoDesign.cardWide,
                onClick = { item.playlist?.let(onOpen) }
            ) { MonoArtwork(item.thumbnailUrl, it) }
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
private fun monoDriftState(enabled: Boolean): State<Float> {
    if (!enabled) return remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "mono-drift")
    return transition.animateFloat(0f, 1f, infiniteRepeatable(tween(18000, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "mono-drift-value")
}

internal val LocalMonoRevealed = staticCompositionLocalOf<MutableSet<String>?> { null }

@Composable
internal fun Modifier.monoReveal(key: String): Modifier {
    val seen = LocalMonoRevealed.current
    val animated = LocalAnimationsEnabled.current
    val start = !animated || seen == null || key in seen
    val progress = remember(key) { Animatable(if (start) 1f else 0f) }
    LaunchedEffect(key) {
        seen?.add(key)
        if (progress.value < 1f) progress.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 18.dp.toPx()
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
    Column(Modifier.fillMaxWidth().monoReveal(title).padding(top = MonoDesign.sectionGap)) {
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

internal enum class MonoShortcut { Podcasts, Samples, Radio, Jam }

@Composable
private fun MonoPageSegment(labels: List<String>, page: Int, onPage: (Int) -> Unit) {
    val animated = LocalAnimationsEnabled.current
    val position by animateFloatAsState(page.toFloat(), if (animated) spring(dampingRatio = 0.78f, stiffness = 420f) else snap(), label = "mono-segment")
    Box(
        Modifier.clip(MonoDesign.pillShape).background(MonoDesign.surface).border(1.dp, MonoDesign.hairline, MonoDesign.pillShape).padding(3.dp)
    ) {
        Box(
            Modifier.width(MonoSegmentWidth).height(34.dp)
                .graphicsLayer { translationX = position * MonoSegmentWidth.toPx() }
                .clip(MonoDesign.pillShape).background(MonoDesign.accent)
        )
        Row(Modifier.selectableGroup()) {
            labels.forEachIndexed { index, label ->
                val active = index == page
                Box(
                    Modifier.width(MonoSegmentWidth).height(34.dp).clip(MonoDesign.pillShape)
                        .selectable(active, role = Role.Tab, onClick = { onPage(index) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label, style = MaterialTheme.typography.labelLarge, maxLines = 1,
                        color = lerp(MonoDesign.text, MonoDesign.onAccent, (1f - kotlin.math.abs(position - index)).coerceIn(0f, 1f))
                    )
                }
            }
        }
    }
}

internal object MonoExploreBridge {
    var pending by mutableStateOf<MonoShortcut?>(null)
    var discoverRequested by mutableStateOf(false)
}

@Composable
private fun MonoHomeTopBar(listener: HomeListener, page: Int, copy: MonoHomeCopy, onPage: (Int) -> Unit, onOpenSettings: () -> Unit, onShortcut: (MonoShortcut) -> Unit) {
    val strings = LocalLevyraStrings.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val shortcuts = remember(strings) {
        listOf(
            Triple(MonoShortcut.Podcasts, Icons.Rounded.Podcasts, podcastCopy(strings.code).title),
            Triple(MonoShortcut.Samples, Icons.Rounded.SlowMotionVideo, strings.exploreSamples),
            Triple(MonoShortcut.Radio, Icons.Rounded.Radio, LevyraLiveRadioCatalog.forCode(strings.code).title),
            Triple(MonoShortcut.Jam, Icons.Rounded.Groups, strings.jamTitle)
        )
    }
    Column(Modifier.fillMaxWidth().padding(top = statusTop + 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = MonoDesign.gutter, end = MonoDesign.gutter - 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonoHomeAvatar(listener)
            Spacer(Modifier.weight(1f))
            MonoIconKey(Icons.Rounded.Replay, "Levyra Rewind", LocalMusicExperiences.current.rewind)
            MonoIconKey(Icons.Rounded.Settings, strings.settings, onOpenSettings)
        }
        LazyRow(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            contentPadding = PaddingValues(horizontal = MonoDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item(key = "segment") { MonoPageSegment(listOf(copy.all, copy.mood), page, onPage) }
            item(key = "divider") { Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(22.dp).background(MonoDesign.hairlineStrong)) }
            items(shortcuts, key = { it.first.name }) { (shortcut, icon, label) ->
                Row(
                    Modifier.clip(MonoDesign.pillShape)
                        .background(MonoDesign.surface)
                        .levyraPressable(onClick = { onShortcut(shortcut) }, role = Role.Button, pressedScale = LevyraPressScale.Row)
                        .heightIn(min = 36.dp)
                        .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, null, tint = MonoDesign.accent, modifier = Modifier.size(18.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge, color = MonoDesign.text, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MonoTrackColumns(
    picks: List<Track>,
    rows: Int,
    limit: Int,
    ranked: Boolean,
    currentId: String?,
    isPlaying: Boolean,
    actionsLabel: String,
    onPlay: (List<Track>, Track) -> Unit,
    onActions: (Track) -> Unit
) {
    val columns = remember(picks, rows, limit) { picks.take(limit).withIndex().chunked(rows) }
    val listState = rememberLazyListState()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columnWidth = if (columns.size > 1) maxWidth * 0.86f else maxWidth
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(end = MonoDesign.gutter / 2)
        ) {
            items(columns, key = { column -> "mono-col-${column.first().value.id}" }, contentType = { "mono-track-column" }) { column ->
                Column(Modifier.width(columnWidth)) {
                    column.forEach { (index, track) ->
                        key(track.id) {
                            MonoCompactRow(
                                track = track,
                                isCurrent = track.id == currentId,
                                isPlaying = isPlaying,
                                actionsLabel = actionsLabel,
                                onPlay = { onPlay(picks, track) },
                                onActions = { onActions(track) },
                                rank = if (ranked) index + 1 else null
                            )
                        }
                    }
                }
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
            composition = composition,
            resonanceTracks = if (composition == null && state.interfaceSettings.showResonance) render.derived.resonanceTracks else emptyList()
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

private val MonoQuoteWidth = 296.dp
private val MonoQuoteHeight = 232.dp

@Composable
private fun MonoQuoteCard(quote: ComposedQuote, playLabel: String, modifier: Modifier) {
    Box(
        modifier.width(MonoQuoteWidth).height(MonoQuoteHeight).monoPanel()
            .background(Brush.verticalGradient(0f to MonoDesign.accentGlow, 0.55f to Color.Transparent))
            .levyraPressable(onClick = quote.onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface, onClickLabel = quote.track.title)
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(MonoDesign.raisedHigh), contentAlignment = Alignment.Center) {
                    if (quote.authorAvatarUrl.isNotBlank()) {
                        AsyncImage(quote.authorAvatarUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(quote.author.trimStart('@').take(1).uppercase(), style = MaterialTheme.typography.labelMedium, color = MonoDesign.textSecondary)
                    }
                }
                Text(
                    quote.author, style = MaterialTheme.typography.labelLarge, color = MonoDesign.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                )
                if (quote.count.isNotBlank()) {
                    Row(
                        Modifier.clip(CircleShape).background(MonoDesign.accent.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.FormatQuote, null, tint = MonoDesign.accent, modifier = Modifier.size(14.dp))
                        Text(quote.count, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MonoDesign.accent, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
                when {
                    quote.loading -> MonoWaveLine(playing = true, color = MonoDesign.accentDim, modifier = Modifier.width(96.dp).height(20.dp))
                    quote.quote != quote.track.artist -> Text(
                        quote.quote, style = MaterialTheme.typography.titleMedium, color = MonoDesign.text,
                        maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MonoDesign.hairline))
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(MonoDesign.artShape)) { CoverImage(quote.track, Modifier.fillMaxSize().clearAndSetSemantics {}) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(quote.track.title, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(quote.track.artist, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                MonoIconKey(Icons.Rounded.PlayArrow, playLabel, quote.onPlay)
            }
        }
    }
}

private class MonoHomeModel(
    val data: AppleMusicHomeData,
    val actions: AppleMusicHomeActions,
    val viewModel: HomeViewModel,
    val state: LevyraUiState,
    val releaseTracks: List<Track>,
    val releaseAlbums: List<AlbumHit>,
    val pinnedCards: List<ComposedCard>,
    val composition: ComposedHomeRequest?,
    val resonanceTracks: List<Track>
)

private class MonoHomeFeed(
    val picks: List<Track>,
    val picksTitle: String,
    val charts: List<Track>,
    val rotation: List<ComposedCard>,
    val albums: List<AlbumHit>,
    val newTracks: List<Track>,
    val newAlbums: List<AlbumHit>,
    val artists: List<ArtistHit>,
    val forgotten: List<Track>
) {
    val isEmpty: Boolean
        get() = picks.isEmpty() && charts.isEmpty() && rotation.isEmpty() && albums.isEmpty() &&
            newTracks.isEmpty() && newAlbums.isEmpty() && artists.isEmpty() && forgotten.isEmpty()
}

@Composable
private fun rememberMonoHomeFeed(model: MonoHomeModel, copy: MonoHomeCopy, onOpenOrbit: () -> Unit): MonoHomeFeed {
    val strings = LocalLevyraStrings.current
    val data = model.data
    val actions = model.actions
    val orbit = remember(data.personalTracks, data.showPersonal) {
        if (data.showPersonal) data.personalTracks.distinctBy(Track::id).take(MonoRailMax) else emptyList()
    }
    val rotation = remember(orbit, data.collections, actions, copy, strings.personalOrbitTitle) {
        buildList {
            orbit.firstOrNull()?.let { lead ->
                add(ComposedCard("mono-orbit", strings.personalOrbitTitle, copy.tracksCount.format(orbit.size), lead.largeThumbnailUrl.ifBlank { lead.thumbnailUrl }, track = lead, onClick = onOpenOrbit))
            }
            data.collections.distinctBy { it.id }.filter { it.tracks.isNotEmpty() }.forEach { collection ->
                val lead = collection.tracks.first()
                add(ComposedCard("mono-collection-${collection.id}", collection.title, copy.tracksCount.format(collection.tracks.size), lead.thumbnailUrl, track = lead) { actions.openCollection(collection.id) })
            }
        }
    }
    return MonoHomeFeed(
        picks = remember(orbit, data.quickPicks) { orbit.ifEmpty { data.quickPicks?.tracks.orEmpty().distinctBy(Track::id) } },
        picksTitle = if (orbit.isNotEmpty()) strings.personalOrbitTitle else data.quickPicks?.title?.ifBlank { null } ?: strings.quickPicks,
        charts = remember(data.charts) { data.charts.distinctBy(Track::id).take(MonoChartMax) },
        rotation = rotation,
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
    var orbitOpen by rememberSaveable { mutableStateOf(false) }
    val feed = rememberMonoHomeFeed(model, copy) { orbitOpen = true }
    val zones = remember(strings) { ExploreCatalog.getZones(strings) }
    val browse = remember(state.exploreCategories, zones) { monoBrowseTiles(state.exploreCategories, zones) }
    val viewModel = model.viewModel
    val categoriesLoading = state.isExploreCategoriesLoading
    val moodEntries = remember(browse, categoriesLoading, viewModel) {
        monoBrowseEntries(browse, categoriesLoading, viewModel::openExploreCategoryPage, viewModel::openExploreZone)
    }
    val isPlaying = rememberUpdatedState(state.isPlaying)
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val playlistShelves = rememberCuratedPlaylistShelves(strings.code)
    val mixedHome = viewModel<YouTubeMusicHomeViewModel>(key = "mono-mixed-for-you")
    val ytHome = viewModel<YouTubeMusicHomeViewModel>(key = "mono-yt-home")
    val newThisWeek by viewModel.appleNewThisWeek.collectAsStateWithLifecycle()
    androidx.lifecycle.compose.LifecycleResumeEffect(state.selectedChartId, strings.code) {
        viewModel.ensureAppleNewThisWeek()
        onPauseOrDispose { }
    }
    val albumShelf = newThisWeek.albums
    val newVideos = rememberNewMusicVideos(strings.code)
    val videosHome = viewModel<YouTubeMusicHomeViewModel>(key = "mono-new-videos")
    val glowColor = MonoDesign.accent
    val revealed = remember { mutableSetOf<String>() }
    CompositionLocalProvider(LocalMonoRevealed provides revealed) {
    BoxWithConstraints(Modifier.fillMaxSize().background(MonoDesign.base)) {
        Box(
            Modifier.fillMaxWidth().height(MonoGlowHeight)
                .graphicsLayer {
                    val first = homeListState.firstVisibleItemIndex == 0
                    translationY = if (first) -homeListState.firstVisibleItemScrollOffset.toFloat() else -size.height
                    alpha = if (first) 1f else 0f
                }
                .drawBehind {
                    drawRect(Brush.verticalGradient(0f to glowColor.copy(alpha = 0.20f), 0.45f to glowColor.copy(alpha = 0.07f), 1f to Color.Transparent))
                    drawRect(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.16f), Color.Transparent), center = Offset(size.width * 0.15f, 0f), radius = size.width))
                }
        )
        val heroCard = (maxWidth * 0.56f).coerceIn(180.dp, 260.dp)
        val releaseCard = (maxWidth * 0.62f).coerceIn(200.dp, 300.dp)
        val kit = remember(model.data) { MonoHomeKit(model.data, isPlaying) }
        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize()
                .pullToRefresh(isRefreshing = homeRefreshing, state = homePullState, onRefresh = {
                    onPullRefresh()
                    viewModel.ensureAppleNewThisWeek(force = true)
                    mixedHome.load(strings.code, force = true, browseId = MonoMixedForYouBrowseId)
                    ytHome.load(strings.code, force = true)
                    videosHome.load(strings.code, force = true, browseId = EXPLORE_BROWSE_ID)
                }),
            contentPadding = PaddingValues(bottom = homeBottomInset + MonoDesign.sectionGap)
        ) {
            item(key = "mono-home-top", contentType = "mono-home-top") {
                MonoHomeTopBar(model.data.listener, page, copy, onPage, model.actions.openSettings) { MonoExploreBridge.pending = it; viewModel.openExploreTab() }
            }
            if (page == MonoPageMood) {
                monoBrowseSections("mono-home", moodEntries, categoriesLoading, copy)
                return@LazyColumn
            }
            val composition = model.composition
            if (composition != null) {
                composedHomeSections(composition, kit) {
                    monoHomeFeed(model, feed, copy, strings, kit, heroCard, releaseCard, playlistShelves, albumShelf, newVideos)
                }
                monoHomeFallback(feed.isEmpty && monoComposedSectionsEmpty(composition.content), state, copy, viewModel)
                return@LazyColumn
            }
            monoHomeFallback(feed.isEmpty && model.pinnedCards.isEmpty() && !(homeRadio.visible && homeRadio.stations.isNotEmpty()), state, copy, viewModel)
            monoHomeFeed(model, feed, copy, strings, kit, heroCard, releaseCard, playlistShelves, albumShelf, newVideos)
            favoriteRadioSection(homeRadio)
        }
        PullToRefreshDefaults.LoadingIndicator(
            state = homePullState,
            isRefreshing = homeRefreshing,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = statusTop + 16.dp)
        )
        if (orbitOpen) {
            val orbitTracks = feed.picks
            GlacierCollectionScreen(
                title = strings.personalOrbitTitle, tracks = orbitTracks, currentId = state.currentTrack?.id, isPlaying = state.isPlaying,
                onDismiss = { orbitOpen = false },
                onPlay = { track -> model.actions.playFrom(orbitTracks, track) },
                onPlayAll = { model.actions.playAll(orbitTracks) },
                onShuffle = { orbitTracks.shuffled().let { shuffled -> shuffled.firstOrNull()?.let { model.actions.playFrom(shuffled, it) } } },
                onTrackActions = model.actions.onTrackActions, bottomInset = homeBottomInset
            )
        }
    }
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

@Suppress("ComplexMethod", "CognitiveComplexMethod")
private fun LazyListScope.monoHomeFeed(
    model: MonoHomeModel,
    feed: MonoHomeFeed,
    copy: MonoHomeCopy,
    strings: LevyraStrings,
    kit: MonoHomeKit,
    heroCard: Dp,
    releaseCard: Dp,
    playlistShelves: List<YoutubeMusicHomeShelf>,
    albumShelf: List<AlbumHit>,
    newVideos: List<YoutubeMusicHomeItem>
) {
    val state = model.state
    val data = model.data
    val actions = model.actions
    val currentId = data.currentId
    val settings = state.interfaceSettings
    data.soundtrack?.let { hero ->
        item(key = "mono-soundtrack", contentType = "mono-hero") { kit.Hero(hero) }
    }
    if (feed.picks.isNotEmpty()) {
        item(key = "mono-picks-header", contentType = "mono-header") {
            Column(Modifier.padding(top = 20.dp)) {
                MonoSectionHeader(feed.picksTitle, overline = copy.startHere, action = strings.playAll) { actions.playAll(feed.picks) }
                Spacer(Modifier.height(6.dp))
            }
        }
        item(key = "mono-picks-grid", contentType = "mono-picks-grid") {
            MonoTrackColumns(feed.picks, MonoQuickRows, MonoQuickRows * MonoQuickColumns, ranked = false, currentId, state.isPlaying, strings.songOptions, actions.playFrom, actions.onTrackActions)
        }
    } else if (state.isLoadingHome) {
        monoSkeletonSection("mono-picks-skeleton", feed.picksTitle, copy.startHere, MonoDesign.card, 3)
    }
    item(key = "mono-discover", contentType = "mono-discover") {
        MonoDiscoverTeaser(state.recentListens.ifEmpty { feed.picks }.map { it.thumbnailUrl }) { MonoExploreBridge.discoverRequested = true }
    }
    item(key = "mono-bubble", contentType = "mono-bubble") {
        val bubble = com.luc4n3x.levyra.ui.discovery.LocalHomeDiscovery.current
        if (bubble != null) {
            MonoBubbleEntry(feed.picks.ifEmpty { state.recentListens }.map { it.thumbnailUrl }) {
                bubble.onOpenBubble(com.luc4n3x.levyra.feature.discovery.BubbleLevel.Adventurous)
            }
        }
    }
    if (model.pinnedCards.isNotEmpty()) {
        item(key = "mono-pinned", contentType = "mono-cards") { kit.Cards(strings.speedDialCopy().title, model.pinnedCards) }
    }
    if (playlistShelves.isNotEmpty()) {
        playlistShelves.forEachIndexed { index, shelf ->
            item(key = "mono-mixed-${shelf.key}", contentType = "mono-rail") {
                MonoSection(if (index == 0) copy.mixedForYou else shelf.title, if (index == 0) copy.rotationOverline else shelf.strapline.ifBlank { null }) {
                    MonoPlaylistRail(shelf.items, model.viewModel::openPlaylistHit)
                }
            }
        }
    } else if (feed.rotation.isNotEmpty()) {
        item(key = "mono-mixed-local", contentType = "mono-cards") {
            MonoSection(copy.mixedForYou, copy.rotationOverline) { kit.CardRail(feed.rotation, MonoDesign.cardWide) }
        }
    }
    if (feed.artists.isNotEmpty()) {
        item(key = "mono-artists", contentType = "mono-rail") {
            MonoSection(strings.trendingArtists, copy.trendingOverline) { MonoArtistRail(feed.artists, actions.openArtist) }
        }
    } else if (data.showArtists && state.homeArtistsLoading) {
        monoSkeletonSection("mono-artists-skeleton", strings.trendingArtists, copy.trendingOverline, MonoDesign.artistCircle, 3)
    }
    if (albumShelf.isNotEmpty() && data.showAlbums) {
        item(key = "mono-albums-apple", contentType = "mono-rail") {
            MonoSection(copy.newAlbums, copy.forYouOverline) {
                MonoReleaseRail(albumShelf, actions.openAlbum)
            }
        }
    } else if (feed.albums.isNotEmpty()) {
        item(key = "mono-albums", contentType = "mono-rail") {
            MonoSection(strings.albumsForYou, copy.forYouOverline) { MonoAlbumRail(feed.albums, MonoAlbumTile, actions.openAlbum) }
        }
    } else if (data.showAlbums && state.homeAlbumsLoading) {
        monoSkeletonSection("mono-albums-skeleton", strings.albumsForYou, copy.forYouOverline, MonoAlbumTile, 3)
    }
    if (newVideos.isNotEmpty()) {
        item(key = "mono-new-videos", contentType = "mono-rail") {
            MonoSection(strings.homeVideoShelfTitle, copy.freshOverline) {
                MonoVideoRail(newVideos) { item ->
                    val track = item.track
                    if (track != null) actions.playFrom(newVideos.mapNotNull { it.track }, track)
                }
            }
        }
    }
    if (model.resonanceTracks.isNotEmpty()) {
        item(key = "mono-resonance", contentType = "mono-quotes") {
            val tracks = model.resonanceTracks
            val comments = state.homeResonanceComments
            val quotes = remember(tracks, comments) {
                tracks.map { track -> resonanceQuote(track, tracks, comments, model.viewModel::openYoutubeCommentsFor, actions.playFrom) }
            }
            kit.Quotes(strings.mostCommentedTracks, quotes)
        }
    }
    if (feed.charts.isNotEmpty()) {
        item(key = "mono-top-chart", contentType = "mono-top-chart") {
            MonoSection(
                "Top ${feed.charts.size}", copy.trendingOverline,
                data.countryLabel.ifBlank { null }, actions.pickCountry.takeIf { data.countryLabel.isNotBlank() }
            ) {
                MonoTrackColumns(feed.charts, MonoChartRows, feed.charts.size, ranked = true, currentId, state.isPlaying, strings.songOptions, actions.playFrom, actions.onTrackActions)
            }
        }
    } else if (settings.showCharts && state.isLoadingCharts) {
        monoSkeletonSection("mono-top-chart-skeleton", "Top 50", copy.trendingOverline, MonoDesign.rowArtwork, 5)
    }
    if (feed.forgotten.isNotEmpty()) {
        item(key = "mono-forgotten", contentType = "mono-rail") {
            MonoSection(strings.forgottenFavorites) {
                MonoTrackRail(feed.forgotten, MonoDesign.card, currentId, strings.songOptions, { actions.playFrom(feed.forgotten, it) }, actions.onTrackActions)
            }
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
        val railState = rememberLazyListState()
        val leadKey = cards.firstOrNull()?.key
        LaunchedEffect(leadKey) { if (railState.firstVisibleItemIndex != 0) railState.scrollToItem(0) }
        LazyRow(state = railState, contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap), verticalAlignment = Alignment.Top) {
            items(cards, key = { it.key }, contentType = { if (it.circular) "mono-card-round" else "mono-card" }) { card ->
                MonoCard(
                    modifier = Modifier.glacierRailItemMotion(glacierRailMotionEnabled(), railState, card.key),
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
        val portrait = hero.artistArtworkUrl
        val blur = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
        val drift = monoDriftState(LocalAnimationsEnabled.current)
        Box(
            Modifier.fillMaxWidth().height(MonoSoundtrackHeight)
                .levyraPressable(onClick = hero.onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface, onClickLabel = strings.play)
        ) {
            Box(
                Modifier.matchParentSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.2f to Color.Black, 0.6f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                    }
            ) {
                if (blur) {
                    Box(Modifier.matchParentSize().graphicsLayer { alpha = 0.42f }.blur(60.dp, BlurredEdgeTreatment.Rectangle)) {
                        if (portrait.isNotBlank()) {
                            StableRemoteArtwork(portrait, null, Modifier.fillMaxSize(), ContentScale.Crop)
                        } else {
                            CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
                        }
                    }
                }
                Box(
                    Modifier.align(Alignment.TopEnd).fillMaxHeight().fillMaxWidth(MonoSoundtrackPortraitShare)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                            val zoom = 1f + 0.06f * drift.value
                            scaleX = zoom
                            scaleY = zoom
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.6f, 0.35f)
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.34f to Color.Black.copy(alpha = 0.6f), 0.6f to Color.Black), blendMode = BlendMode.DstIn)
                        }
                ) {
                    if (portrait.isNotBlank()) {
                        StableRemoteArtwork(portrait, null, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true, alignment = BiasAlignment(0f, -0.3f))
                    } else {
                        CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {}, highRes = true)
                    }
                }
                Box(Modifier.matchParentSize().drawBehind {
                    drawRect(Brush.horizontalGradient(0f to MonoDesign.base.copy(alpha = 0.7f), 0.5f to Color.Transparent))
                    drawRect(Brush.radialGradient(listOf(MonoDesign.accent.copy(alpha = 0.12f), Color.Transparent), center = Offset(size.width * 0.85f, size.height * 0.35f), radius = size.width))
                })
            }
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth(0.7f).padding(start = MonoDesign.gutter, end = 8.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hero.label.isNotBlank()) MonoOverline(hero.label, color = MonoDesign.accent)
                LevyraMediaTitle(
                    track.title, explicit = track.explicit,  style = MaterialTheme.typography.headlineLarge, color = MonoDesign.text, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (hero.companionArtworkUrls.isNotEmpty()) {
                        HomeSoundtrackCompanionStack(hero.companionArtworkUrls, MonoDesign.base, MonoDesign.accent)
                    }
                    Text(
                        hero.lead.ifBlank { track.artist }, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.text.copy(alpha = 0.82f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                }
                MonoWaveLine(active, Modifier.padding(top = 4.dp).width(120.dp).height(14.dp), color = MonoDesign.accent, cycles = 5f, strokeWidth = 1.5.dp)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = MonoDesign.gutter, bottom = 30.dp).size(64.dp)
                    .clip(rememberLevyraPlayMorphShape(playing = active, animated = LocalAnimationsEnabled.current, spinWhilePlaying = !hero.isResolving))
                    .background(MonoDesign.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (active) strings.pause else strings.play,
                    tint = MonoDesign.onAccent, modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    @Composable
    override fun Quotes(title: String, quotes: List<ComposedQuote>) {
        val unique = remember(quotes) { quotes.distinctBy(ComposedQuote::key) }
        if (unique.isEmpty()) return
        val strings = LocalLevyraStrings.current
        MonoSection(title, strings.commentsOverline) {
            val railState = rememberLazyListState()
            val railMotion = glacierRailMotionEnabled()
            LazyRow(state = railState, contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
                items(unique, key = { it.key }, contentType = { "mono-quote" }) { quote ->
                    MonoQuoteCard(quote, strings.play, Modifier.glacierRailItemMotion(railMotion, railState, quote.key))
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
