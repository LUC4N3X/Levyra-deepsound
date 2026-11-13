package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.artwork.ArtworkBackdropWash
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.genreChartCopy
import com.luc4n3x.levyra.ui.i18n.genreChartGenreNames
import com.luc4n3x.levyra.ui.media.ImmersiveMediaActionRow
import com.luc4n3x.levyra.ui.media.ImmersiveMediaColors
import com.luc4n3x.levyra.ui.media.ImmersiveMediaHero
import com.luc4n3x.levyra.ui.media.ImmersiveMediaPrimaryAction
import com.luc4n3x.levyra.ui.media.ImmersiveMediaTopBar
import com.luc4n3x.levyra.ui.media.animatedImmersiveMediaColors
import com.luc4n3x.levyra.ui.media.immersiveHeroHeight
import com.luc4n3x.levyra.ui.media.immersiveMediaColors
import com.luc4n3x.levyra.ui.media.immersiveMediaGutter
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraType
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.viewmodel.ExploreViewModel

private const val GenreChartDestinationPrefix = "explore-destination-genre-chart:"
private const val GenreChartHeroArtists = 3

private val GenreChartShelfOrder = listOf(
    "pop-global", "rap-drill", "rock-alt", "elettronica", "rnb-soul", "latino", "chart-kpop",
    "lofi-chill", "chart-metal", "chart-indie", "anime-jpop", "afrobeats", "chart-jazz"
)

internal fun exploreGenreChartZones(zones: List<ExploreZone>, languageCode: String): List<ExploreZone> {
    val names = genreChartGenreNames(languageCode)
    val extra = listOf(
        ExploreZone("chart-kpop", names.kpop, "✦", "", 0xFFFF5CA8.toInt(), 0xFF7C4DFF.toInt()),
        ExploreZone("chart-metal", names.metal, "✦", "", 0xFF6F7A8C.toInt(), 0xFF22262E.toInt()),
        ExploreZone("chart-indie", names.indie, "✦", "", 0xFF26A69A.toInt(), 0xFF00695C.toInt()),
        ExploreZone("chart-jazz", names.jazz, "✦", "", 0xFFC8963E.toInt(), 0xFF5D4037.toInt())
    )
    val byId = (zones + extra).associateBy { zone -> zone.id }
    return GenreChartShelfOrder.mapNotNull(byId::get)
}

private val GenreChartCoverTones = mapOf(
    "rap-drill" to 0xFFB388FF,
    "chart-metal" to 0xFFFF5A4E,
    "afrobeats" to 0xFFFFB74D
)

internal fun genreChartCoverTone(zone: ExploreZone): Color =
    Color(GenreChartCoverTones[zone.id] ?: (zone.accentStart.toLong() and 0xFFFFFFFFL))

internal fun exploreGenreChartDestination(zoneId: String): String = "$GenreChartDestinationPrefix$zoneId"

internal fun exploreGenreChartDestinationId(destination: String?): String? =
    destination?.takeIf { it.startsWith(GenreChartDestinationPrefix) }
        ?.removePrefix(GenreChartDestinationPrefix)
        ?.takeIf(String::isNotBlank)

@Immutable
internal class GenreChartActions(
    val onShuffle: (List<Track>) -> Unit,
    val onDownload: (List<Track>) -> Unit
)

internal fun genreChartActions(viewModel: ExploreViewModel, offlineLabel: String): GenreChartActions = GenreChartActions(
    onShuffle = { tracks ->
        val shuffled = tracks.shuffled()
        shuffled.firstOrNull()?.let { first -> viewModel.playFrom(shuffled, first) }
    },
    onDownload = { tracks -> viewModel.exportTracks(tracks, offlineLabel) }
)

@Composable
internal fun GenreChartDestinationScreen(
    genreKey: String,
    genreTitle: String,
    accent: Color,
    artworkUrl: String,
    chart: List<Track>,
    loading: Boolean,
    currentTrackId: String?,
    isPlaying: Boolean,
    strings: LevyraStrings,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit,
    bottomInset: Dp,
    artists: List<String> = emptyList(),
    actions: GenreChartActions? = null
) {
    BackHandler(enabled = backEnabled, onBack = onBack)
    val copy = remember(strings.code) { genreChartCopy(strings.code) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val animated = LocalAnimationsEnabled.current
    val lightTheme = LevyraIsLight
    val targetColors = remember(accent, lightTheme) {
        immersiveMediaColors(accent, lerp(accent, LevyraBlack, 0.45f), lightTheme)
    }
    val colors = animatedImmersiveMediaColors(targetColors, animated = animated, labelPrefix = "genre-chart")
    val onPlayAll: () -> Unit = { chart.firstOrNull()?.let(onPlayTrack) }
    val byline = remember(artists, copy) {
        artists.take(GenreChartHeroArtists).joinToString(", ").ifBlank { copy.subtitle }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(24f)
            .background(colors.base)
    ) {
        val wide = resolvePlayerPane(maxWidth.value, maxHeight.value) == LevyraPlayerPane.SideBySide
        val topBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
        val heroHeight = immersiveHeroHeight(wide, maxWidth, maxHeight, topBarHeight)
        val collapseThreshold = with(density) { (heroHeight - topBarHeight).coerceAtLeast(0.dp).toPx() }
        val collapsedState = remember(listState, collapseThreshold) {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapseThreshold
            }
        }
        val gutter = immersiveMediaGutter(maxWidth)
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        ArtworkBackdropWash(
            artworkUrl = artworkUrl,
            tint = colors.fieldTop,
            base = colors.base,
            modifier = Modifier.fillMaxSize()
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset)
        ) {
            item(key = "genre-chart-hero", contentType = "genre-chart-hero") {
                GenreChartHero(
                    hero = GenreChartHeroModel(genreTitle, copy.title, byline, if (chart.isEmpty()) "" else strings.formatTrackCount(chart.size)),
                    accent = accent,
                    artworkUrl = artworkUrl,
                    colors = colors,
                    layout = GenreChartHeroLayout(wide, viewportWidth, viewportHeight, topBarHeight),
                    strings = strings,
                    chart = chart,
                    actions = actions,
                    onPlayAll = onPlayAll
                )
            }
            genreChartRows(chart, colors, currentTrackId, isPlaying, gutter, onPlayTrack, onRequestTrackArtwork)
            genreChartFooter(chart, loading, colors, gutter, strings.exploreEmpty, copy.credit)
        }
        ImmersiveMediaTopBar(
            title = genreTitle,
            colors = colors,
            collapsedState = collapsedState,
            height = topBarHeight,
            animated = animated,
            backLabel = strings.back,
            onBack = onBack,
            actions = {}
        )
    }
}

private data class GenreChartHeroModel(val title: String, val overline: String, val byline: String, val metadata: String)

private data class GenreChartHeroLayout(val wide: Boolean, val width: Dp, val height: Dp, val topBarHeight: Dp)

@Composable
private fun GenreChartHero(
    hero: GenreChartHeroModel,
    accent: Color,
    artworkUrl: String,
    colors: ImmersiveMediaColors,
    layout: GenreChartHeroLayout,
    strings: LevyraStrings,
    chart: List<Track>,
    actions: GenreChartActions?,
    onPlayAll: () -> Unit
) {
    ImmersiveMediaHero(
        title = hero.title,
        overline = hero.overline,
        subtitle = hero.byline,
        metadata = hero.metadata,
        colors = colors,
        wide = layout.wide,
        viewportWidth = layout.width,
        viewportHeight = layout.height,
        topBarHeight = layout.topBarHeight,
        onSubtitleClick = null,
        actions = {
            ImmersiveMediaActionRow(
                primary = ImmersiveMediaPrimaryAction(
                    enabled = chart.isNotEmpty(),
                    label = strings.play,
                    contentDescription = strings.play,
                    icon = Icons.Rounded.PlayArrow,
                    onClick = onPlayAll
                ),
                shuffleLabel = strings.shuffle,
                downloadLabel = strings.downloadPlaylist,
                colors = colors,
                shuffleEnabled = actions != null && chart.size > 1,
                downloadEnabled = actions != null && chart.isNotEmpty(),
                onShuffle = { actions?.onShuffle?.invoke(chart) },
                onDownload = { actions?.onDownload?.invoke(chart) }
            )
        },
        artwork = {
            GenreChartArtwork(accent = accent, artworkUrl = artworkUrl, modifier = Modifier.fillMaxSize())
        }
    )
}

private fun LazyListScope.genreChartRows(
    chart: List<Track>,
    colors: ImmersiveMediaColors,
    currentTrackId: String?,
    isPlaying: Boolean,
    gutter: Dp,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    itemsIndexed(
        items = chart,
        key = { _, track -> "genre-chart-${track.id}" },
        contentType = { _, _ -> "genre-chart-row" }
    ) { index, track ->
        LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
        GenreChartRankedRow(
            rank = index + 1,
            track = track,
            colors = colors,
            isCurrent = track.id == currentTrackId,
            isPlaying = isPlaying && track.id == currentTrackId,
            onClick = { onPlayTrack(track) },
            modifier = Modifier.padding(horizontal = gutter - 6.dp)
        )
    }
}

private fun LazyListScope.genreChartFooter(
    chart: List<Track>,
    loading: Boolean,
    colors: ImmersiveMediaColors,
    gutter: Dp,
    emptyLabel: String,
    credit: String
) {
    when {
        loading -> item(key = "genre-chart-loading", contentType = "genre-chart-state") {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                LevyraLoadingIndicator(modifier = Modifier.size(28.dp), color = colors.accent)
            }
        }
        chart.isEmpty() -> item(key = "genre-chart-empty", contentType = "genre-chart-state") {
            Text(
                text = emptyLabel,
                color = colors.contentMuted,
                style = LevyraType.metadata,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 32.dp)
            )
        }
    }
    if (chart.isNotEmpty()) item(key = "genre-chart-credit", contentType = "genre-chart-credit") {
        Text(
            text = credit,
            color = colors.contentMuted,
            fontSize = 11.5.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(11.5.sp),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 14.dp)
        )
    }
}

@Composable
private fun GenreChartRankedRow(
    rank: Int,
    track: Track,
    colors: ImmersiveMediaColors,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rankColor = when {
        isCurrent || rank <= 3 -> colors.accent
        else -> colors.contentMuted
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) colors.accent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick, onClickLabel = track.title, role = Role.Button)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = rank.toString(),
            color = rankColor,
            fontSize = 19.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = TextStyle(fontFeatureSettings = "tnum"),
            modifier = Modifier.width(30.dp)
        )
        AsyncImage(
            model = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(colors.secondaryFill)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = track.title,
                color = if (isCurrent) colors.accent else colors.content,
                style = LevyraType.contentTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = colors.contentMuted,
                style = LevyraType.metadata,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isPlaying) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun ExploreZoneChartDestination(
    zone: ExploreZone,
    chart: List<Track>,
    loading: Boolean,
    currentTrackId: String?,
    isPlaying: Boolean,
    languageCode: String,
    strings: LevyraStrings,
    backEnabled: Boolean,
    bottomInset: Dp,
    onOpenChart: (ExploreZone) -> Unit,
    onPlay: (List<Track>, Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit,
    onBack: () -> Unit,
    actions: GenreChartActions? = null
) {
    LaunchedEffect(zone.id, languageCode) { onOpenChart(zone) }
    val cover = rememberGenreChartCover(zone)
    GenreChartDestinationScreen(
        genreKey = zone.id,
        genreTitle = zone.label,
        accent = genreChartCoverTone(zone),
        artworkUrl = cover.artworkUrl,
        chart = chart,
        loading = loading,
        currentTrackId = currentTrackId,
        isPlaying = isPlaying,
        strings = strings,
        backEnabled = backEnabled,
        onBack = onBack,
        onPlayTrack = { track -> onPlay(chart, track) },
        onRequestTrackArtwork = onRequestTrackArtwork,
        bottomInset = bottomInset,
        artists = cover.artists,
        actions = actions
    )
}
