package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.animateFloat
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.viewmodel.YouTubeMusicHomeViewModel
import com.luc4n3x.levyra.viewmodel.EXPLORE_BROWSE_ID
import com.luc4n3x.levyra.viewmodel.NEW_VIDEOS_BROWSE_ID
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.ChartMarketDirectory
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.MonoHomeCopy
import com.luc4n3x.levyra.ui.i18n.podcastCopy
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState

private val MonoMoodTileHeight = 108.dp
private val MonoGenreTileHeight = 84.dp
private val MonoReleaseCard = 172.dp
private const val MonoExploreMoods = 6
private const val MonoExploreGenres = 8
private const val MonoExploreCharts = 10
private const val MonoExploreRail = 20
private val MonoVideoWidth = 260.dp

internal class MonoBrowseSource(val moods: List<ExploreCategory>, val genres: List<ExploreCategory>, val fallbackGenres: List<ExploreZone>)

internal class MonoBrowseEntries(val moods: List<MonoTileEntry>, val genres: List<MonoTileEntry>)

internal fun monoBrowseTiles(categories: List<ExploreCategory>, zones: List<ExploreZone>): MonoBrowseSource {
    val sections = buildExploreCategorySections(categories)
    val atmospheric = sections.filter { it.presentation == ExploreCategoryPresentation.Atmospheric }.flatMap { it.categories }
    val rest = sections.filter { it.presentation != ExploreCategoryPresentation.Atmospheric }.flatMap { it.categories }
    val moods = atmospheric.ifEmpty { rest.take(MonoExploreMoods) }
    val genres = if (atmospheric.isEmpty()) rest.drop(moods.size) else rest
    return MonoBrowseSource(moods, genres, if (sections.isEmpty()) exploreFallbackGenres(zones) else emptyList())
}

internal fun monoBrowseEntries(
    source: MonoBrowseSource,
    loading: Boolean,
    openCategory: (ExploreCategory) -> Unit,
    openZone: (ExploreZone) -> Unit,
    artwork: Map<String, String> = emptyMap(),
    requestArtwork: ((String) -> Unit)? = null
): MonoBrowseEntries {
    fun tile(prefix: String, category: ExploreCategory) = MonoTileEntry(
        "$prefix-${category.params}", category.title, artwork[category.params].orEmpty(),
        requestArtwork?.let { request -> { request(category.params) } }
    ) { openCategory(category) }
    return MonoBrowseEntries(
        moods = source.moods.map { tile("mood", it) },
        genres = source.genres.map { tile("genre", it) } +
            if (loading) emptyList() else source.fallbackGenres.map { MonoTileEntry("zone-${it.id}", it.label) { openZone(it) } }
    )
}

private val MonoTileHues = listOf(
    Color(0xFF3F6FB0), Color(0xFFC2524E), Color(0xFF8A5AB8), Color(0xFF2C8C80), Color(0xFFCB8536),
    Color(0xFFB64A7C), Color(0xFF5E7F3A), Color(0xFF4D57B0), Color(0xFFCE6748), Color(0xFF2D80B2)
)

private fun monoTileSeed(title: String): Int = title.lowercase().fold(7) { acc, c -> acc * 31 + c.code } and 0x7FFFFFFF

@Composable
internal fun MonoBrowseTile(entry: MonoTileEntry, index: Int, height: Dp, modifier: Modifier = Modifier) {
    val seed = remember(entry.title) { monoTileSeed(entry.title) }
    val hue = MonoTileHues[seed % MonoTileHues.size]
    val cycles = 1.4f + seed % 5 * 0.35f
    val phase = seed % 360 / 57.3f
    val path = remember { Path() }
    Box(
        modifier.height(height)
            .levyraPressable(onClick = entry.onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .clip(MonoDesign.cardShape)
            .drawWithCache {
                val fill = Brush.linearGradient(listOf(lerp(hue, Color.White, 0.08f), lerp(hue, Color.Black, 0.38f)), Offset.Zero, Offset(size.width * 0.6f, size.height))
                val stroke = 9.dp.toPx()
                val band = size.height * 0.34f
                val baseY = size.height * 0.84f
                path.reset()
                val steps = 48
                for (i in 0..steps) {
                    val t = i / steps.toFloat()
                    val x = size.width * (0.18f + t * 0.95f)
                    val y = baseY - kotlin.math.sin(t * cycles * 2f * kotlin.math.PI.toFloat() + phase) * band * (0.35f + 0.65f * t)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                val waveStyle = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                val echoStyle = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
                onDrawBehind {
                    drawRect(fill)
                    drawPath(path, Color.Black.copy(alpha = 0.16f), style = waveStyle)
                    translate(top = -stroke * 1.4f) { drawPath(path, Color.White.copy(alpha = 0.34f), style = echoStyle) }
                }
            }
    ) {
        entry.onAppear?.let { request -> LaunchedEffect(entry.key) { if (entry.artwork.isBlank()) request() } }
        if (entry.artwork.isNotBlank()) {
            MonoArtwork(
                entry.artwork,
                Modifier.align(Alignment.BottomEnd)
                    .size(height * 0.78f)
                    .graphicsLayer {
                        translationX = size.width * 0.18f
                        translationY = size.height * 0.16f
                        rotationZ = -12f
                        shadowElevation = 10.dp.toPx()
                        shape = MonoDesign.artShape
                        clip = true
                    }
            )
        }
        Box(Modifier.matchParentSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(
            entry.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.82f)
        )
        Text(
            monoRank(index + 1),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.align(Alignment.BottomStart).clearAndSetSemantics { }
        )
        }
    }
}

private fun LazyListScope.monoTileGrid(
    key: String,
    entries: List<MonoTileEntry>,
    tile: @Composable (MonoTileEntry, Int, Modifier) -> Unit
) {
    items(entries.chunked(2), key = { row -> "$key-${row.first().key}" }, contentType = { key }) { row ->
        val start = entries.indexOf(row.first())
        Row(
            Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter).padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            row.forEachIndexed { offset, entry -> tile(entry, start + offset, Modifier.weight(1f)) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

private fun LazyListScope.monoBrowseHeader(key: String, title: String, overline: String, action: String?, onAction: (() -> Unit)?) {
    item(key = key, contentType = "mono-header") {
        Column(Modifier.padding(top = MonoDesign.sectionGap, bottom = MonoDesign.headerToContent)) {
            MonoSectionHeader(title, overline = overline, action = action, onAction = onAction)
        }
    }
}

internal fun LazyListScope.monoBrowseSections(
    keyPrefix: String,
    entries: MonoBrowseEntries,
    loading: Boolean,
    copy: MonoHomeCopy,
    seeAllLabel: String? = null,
    moodLimit: Int = Int.MAX_VALUE,
    genreLimit: Int = Int.MAX_VALUE,
    onSeeAll: (() -> Unit)? = null
) {
    val moods = entries.moods.take(moodLimit)
    val genres = entries.genres.take(genreLimit)
    if (moods.isNotEmpty()) {
        monoBrowseHeader("$keyPrefix-moods-header", copy.moodsTitle, copy.momentsOverline, seeAllLabel, onSeeAll)
        monoTileGrid("$keyPrefix-moods", moods) { entry, index, modifier ->
            MonoBrowseTile(entry, index, MonoMoodTileHeight, modifier)
        }
    } else if (loading) {
        item(key = "$keyPrefix-moods-skeleton", contentType = "mono-skeleton-section") {
            Column(Modifier.padding(top = MonoDesign.sectionGap)) {
                MonoSectionHeader(copy.moodsTitle, overline = copy.momentsOverline)
                MonoWaveSkeleton(Modifier.padding(top = MonoDesign.headerToContent), tile = MonoDesign.card, tiles = 3)
            }
        }
    }
    if (genres.isNotEmpty()) {
        monoBrowseHeader("$keyPrefix-genres-header", copy.genresTitle, copy.genresOverline, seeAllLabel, onSeeAll)
        monoTileGrid("$keyPrefix-genres", genres) { entry, index, modifier ->
            MonoBrowseTile(entry, index, MonoGenreTileHeight, modifier)
        }
    }
}

@Composable
internal fun MonoExploreEntry(
    state: LevyraUiState,
    listState: LazyListState,
    bottomInset: Dp,
    editorial: ExploreEditorialContent?,
    onShortcut: (ExploreShortcut) -> Unit,
    onOpenRadio: () -> Unit,
    onOpenJam: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoHomeStrings.forCode(strings.code) }
    val pending = MonoExploreBridge.pending
    LaunchedEffect(pending) {
        when (pending) {
            MonoShortcut.Podcasts -> onShortcut(ExploreShortcut.Podcasts)
            MonoShortcut.Samples -> onShortcut(ExploreShortcut.Samples)
            MonoShortcut.Radio -> onOpenRadio()
            MonoShortcut.Jam -> onOpenJam()
            null -> Unit
        }
        MonoExploreBridge.pending = null
    }
    val nothingLoaded = editorial?.news.isNullOrEmpty()
    val stillLoading = state.isNewReleasesLoading
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(MonoDesign.base),
        contentPadding = PaddingValues(bottom = bottomInset + MonoDesign.sectionGap)
    ) {
        item(key = "mono-explore-top", contentType = "mono-explore-top") {
            Column(Modifier.padding(top = statusTop + 18.dp)) { MonoScreenTitle(strings.exploreTitle) }
        }
        if (editorial != null && editorial.news.isNotEmpty()) {
            item(key = "mono-explore-news-hero", contentType = "mono-explore-news") { ExploreEditorialRow(ExploreRow.NewsHero, editorial) }
        }
        if (editorial != null) {
            item(key = "mono-explore-top100", contentType = "mono-explore-top100") { ExploreEditorialRow(ExploreRow.DailyTop100, editorial) }
        }
        if (editorial != null && editorial.news.size > 1) {
            item(key = "mono-explore-news-desk", contentType = "mono-explore-news") { ExploreEditorialRow(ExploreRow.NewsList, editorial) }
        }
        if (nothingLoaded) {
            item(key = "mono-explore-fallback", contentType = "mono-fallback") {
                if (stillLoading) {
                    MonoWaveSkeleton(Modifier.padding(top = MonoDesign.sectionGap))
                } else {
                    Text(
                        strings.exploreEmpty,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MonoDesign.textSecondary,
                        modifier = Modifier.padding(horizontal = MonoDesign.gutter).padding(top = MonoDesign.sectionGap)
                    )
                }
            }
        }
    }
}

@Composable
internal fun MonoVideoRail(videos: List<YoutubeMusicHomeItem>, onPlay: (YoutubeMusicHomeItem) -> Unit) {
    LazyRow(contentPadding = MonoRailPadding, horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
        items(videos, key = { it.key }, contentType = { "mono-video-card" }) { item ->
            Column(
                Modifier.width(MonoVideoWidth)
                    .levyraPressable(onClick = { onPlay(item) }, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = item.title)
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MonoDesign.cardShape).background(MonoDesign.surface)) {
                    coil3.compose.AsyncImage(
                        item.thumbnailUrl, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, alpha = 0.45f,
                        modifier = Modifier.fillMaxSize().blur(24.dp)
                    )
                    coil3.compose.AsyncImage(item.thumbnailUrl, null, contentScale = androidx.compose.ui.layout.ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    Box(
                        Modifier.align(Alignment.Center).size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.42f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                }
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
internal fun MonoDiscoverTeaser(covers: List<String>, onOpen: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { discoveryCopy(strings.code) }
    val shown = remember(covers) { covers.filter(String::isNotBlank).distinct().take(3) }
    Column(Modifier.fillMaxWidth().padding(top = MonoDesign.sectionGap)) {
        MonoSectionHeader(copy.title, overline = LevyraMonoHomeStrings.forCode(strings.code).forYouOverline)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter).padding(top = MonoDesign.headerToContent)
                .height(168.dp)
                .monoPanel(MonoDesign.panelShape, MonoDesign.surface)
                .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface, onClickLabel = copy.start)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(copy.lead, style = MaterialTheme.typography.titleMedium, color = MonoDesign.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Row(
                    Modifier.clip(MonoDesign.pillShape).background(MonoDesign.accent).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = MonoDesign.onAccent, modifier = Modifier.size(16.dp))
                    Text(copy.start, style = MaterialTheme.typography.labelLarge, color = MonoDesign.onAccent, maxLines = 1)
                }
            }
            val sway = monoDiscoverSway(LocalAnimationsEnabled.current)
            Box(Modifier.width(120.dp).height(132.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                val slots = if (shown.isEmpty()) listOf("", "", "") else shown
                slots.forEachIndexed { index, url ->
                    val offset = index - (slots.size - 1) / 2f
                    Box(
                        Modifier.size(width = 82.dp, height = 104.dp).graphicsLayer {
                            val swing = sway.value
                            rotationZ = offset * (8f + 4f * swing)
                            translationX = offset * (18f + 8f * swing).dp.toPx()
                            translationY = kotlin.math.abs(offset) * 6.dp.toPx()
                            shadowElevation = 8.dp.toPx()
                            shape = MonoDesign.cardShape
                            clip = true
                        }.background(MonoDesign.raised).border(1.dp, MonoDesign.hairlineStrong, MonoDesign.cardShape)
                    ) { if (url.isNotBlank()) MonoArtwork(url, Modifier.fillMaxSize()) }
                }
            }
        }
    }
}

@Composable
private fun monoDiscoverSway(enabled: Boolean): androidx.compose.runtime.State<Float> {
    if (!enabled) return remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "mono-discover")
    return transition.animateFloat(
        0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(3200, easing = androidx.compose.animation.core.FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "mono-discover-sway"
    )
}

@Composable
internal fun MonoBubbleEntry(covers: List<String>, onOpen: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val bubble = com.luc4n3x.levyra.ui.i18n.smartMusicCopy(strings.code)
    val shown = remember(covers) { covers.filter(String::isNotBlank).distinct().take(3) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter).padding(top = MonoDesign.sectionGap)
            .heightIn(min = 64.dp)
            .clip(MonoDesign.cardShape)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Row, onClickLabel = bubble[com.luc4n3x.levyra.ui.i18n.SmartMusicText.BubbleTitle])
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(width = 84.dp, height = 44.dp).clearAndSetSemantics { }) {
            repeat(3) { index ->
                Box(
                    Modifier.padding(start = (index * 20).dp).size(44.dp).clip(CircleShape).background(MonoDesign.raised)
                        .border(1.5.dp, if (index == 2) MonoDesign.accent else MonoDesign.base, CircleShape)
                ) { shown.getOrNull(index)?.let { MonoArtwork(it, Modifier.fillMaxSize()) } }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(bubble[com.luc4n3x.levyra.ui.i18n.SmartMusicText.BubbleTitle], style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(bubble[com.luc4n3x.levyra.ui.i18n.SmartMusicText.BubbleEntry], style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(36.dp).clip(CircleShape).border(1.dp, MonoDesign.hairlineStrong, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MonoDesign.text, modifier = Modifier.size(18.dp))
        }
    }
}

internal fun monoExploreNewVideos(shelves: List<YoutubeMusicHomeShelf>, limit: Int): List<YoutubeMusicHomeItem> =
    shelves.filter { it.headerBrowseId == NEW_VIDEOS_BROWSE_ID }
        .flatMap { it.items }
        .filter { it.track != null }
        .distinctBy { it.key }
        .take(limit)
