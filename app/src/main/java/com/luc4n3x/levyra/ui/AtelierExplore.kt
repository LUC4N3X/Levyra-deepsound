package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.fullResolutionArtworkUrl
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ChartMarketDirectory
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.viewmodel.ExploreViewModel
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import kotlin.random.Random

private const val AtelierExploreCollapsedCharts = 10
private const val AtelierCrateSize = 4
private const val AtelierCratePoolMax = 48
private val AtelierCrateLean = floatArrayOf(-2.5f, 1.5f, -1f, 2.5f)

@Immutable
private class AtelierCrateItem(
    val key: String,
    val track: Track?,
    val album: AlbumHit?,
    val title: String,
    val subtitle: String
)

@Composable
private fun AtelierExploreMasthead(statusTop: Dp, onOpenSearch: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.fillMaxWidth().padding(top = statusTop + 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.exploreTitle,
                style = MaterialTheme.typography.displayMedium,
                color = AtelierDesign.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            AtelierIconAction(Icons.Rounded.Search, strings.search, onOpenSearch)
        }
        Text(
            text = strings.exploreSubtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = AtelierDesign.textSecondary,
            modifier = Modifier.padding(horizontal = AtelierDesign.gutter).padding(top = 4.dp, bottom = 12.dp)
        )
    }
}

@Composable
private fun AtelierCrateArt(item: AtelierCrateItem) {
    when {
        item.track != null -> CoverImage(item.track, Modifier.fillMaxSize().clearAndSetSemantics {})
        item.album != null -> AtelierArtwork(item.album.thumbnailUrl, Modifier.clearAndSetSemantics {})
        else -> AtelierArtwork("")
    }
}

@Composable
private fun AtelierCrate(items: List<AtelierCrateItem>, roll: Int, onReroll: () -> Unit, onOpen: (AtelierCrateItem) -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    val animated = LocalAnimationsEnabled.current
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val settle = remember { Animatable(1f) }
    LaunchedEffect(roll) {
        if (roll > 0 && animated) {
            settle.snapTo(0f)
            settle.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f))
        }
    }
    Column {
        AtelierSectionLabel(copy.crateTitle) {
            AtelierHardwareKey(
                label = copy.crateReroll,
                lit = false,
                onClick = onReroll,
                modifier = Modifier.padding(end = 8.dp).size(52.dp, 48.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Rounded.Refresh, null, tint = AtelierDesign.accentBright, modifier = Modifier.size(22.dp))
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(top = 6.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEachIndexed { index, item ->
                    key(item.key) {
                        val lean = AtelierCrateLean[index % AtelierCrateLean.size]
                        val description = remember(item.title, item.subtitle) {
                            listOf(item.title, item.subtitle).filter(String::isNotBlank).joinToString(", ")
                        }
                        Box(
                            Modifier.weight(1f).aspectRatio(1f)
                                .graphicsLayer {
                                    val progress = settle.value
                                    rotationZ = lean * direction * (1f + (1f - progress) * 3f)
                                    translationY = (1f - progress) * -30.dp.toPx() * (1f + index * 0.25f)
                                    alpha = 0.35f + 0.65f * progress
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                    shadowElevation = 10.dp.toPx()
                                    shape = AtelierDesign.sleeveShape
                                    clip = true
                                    ambientShadowColor = Color.Black
                                    spotShadowColor = Color.Black
                                }
                                .background(AtelierDesign.panel)
                                .levyraPressable(onClick = { onOpen(item) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                                .semantics { contentDescription = description }
                        ) { AtelierCrateArt(item) }
                    }
                }
                repeat(AtelierCrateSize - items.size) { Spacer(Modifier.weight(1f)) }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(30.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                    .drawWithCache {
                        val wood = Brush.verticalGradient(listOf(Color(0xFF332A22), Color(0xFF16120F)))
                        val rim = Color.White.copy(alpha = 0.14f)
                        val slot = Color.Black.copy(alpha = 0.55f)
                        onDrawBehind {
                            drawRect(wood)
                            drawRect(rim, size = Size(size.width, 1.dp.toPx()))
                            val slotWidth = 44.dp.toPx()
                            val slotHeight = 6.dp.toPx()
                            drawRoundRect(
                                slot,
                                Offset((size.width - slotWidth) / 2f, (size.height - slotHeight) / 2f + 2.dp.toPx()),
                                Size(slotWidth, slotHeight),
                                CornerRadius(slotHeight / 2f)
                            )
                        }
                    }
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter + 6.dp).padding(top = 10.dp)
                .clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEach { item ->
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = AtelierDesign.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(AtelierCrateSize - items.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
internal fun AtelierCabinetKnob(angle: Float, modifier: Modifier) {
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        drawCircle(Brush.verticalGradient(listOf(Color(0xFF2C2C31), Color(0xFF141417))), radius)
        drawCircle(AtelierDesign.hairlineStrong, radius, style = Stroke(1.dp.toPx()))
        val radians = Math.toRadians(angle.toDouble())
        val reach = radius * 0.68f
        drawLine(
            AtelierDesign.accentBright,
            center,
            Offset(center.x + reach * kotlin.math.cos(radians).toFloat(), center.y + reach * kotlin.math.sin(radians).toFloat()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
internal fun AtelierExploreEntry(
    state: LevyraUiState,
    viewModel: ExploreViewModel,
    freshTracks: List<Track>,
    listState: LazyListState,
    bottomInset: Dp,
    editorial: ExploreEditorialContent?
) {
    val strings = LocalLevyraStrings.current
    val currentId = state.currentTrack?.id
    val releases = remember(state.exploreNewReleases) { state.exploreNewReleases.distinctBy(::atelierAlbumKey).take(20) }
    val fresh = remember(freshTracks) { freshTracks.distinctBy(Track::id).take(20) }
    val showCharts = state.interfaceSettings.showCharts
    val charts = remember(state.charts, showCharts) { atelierExploreChartTracks(state.charts, showCharts) }
    val cratePool = remember(releases, fresh, charts) { atelierExploreCratePool(releases, fresh, charts) }
    val crateTracks = remember(cratePool) { cratePool.mapNotNull { it.track } }
    var crateRoll by rememberSaveable { mutableIntStateOf(0) }
    val crate = remember(cratePool, crateRoll) { atelierExploreCrateSelection(cratePool, crateRoll) }
    val nothingLoaded = releases.isEmpty() && fresh.isEmpty() && charts.isEmpty() && editorial?.news.isNullOrEmpty()
    val stillLoading = state.isExploreCategoriesLoading || state.isFreshCurrentsLoading || state.isNewReleasesLoading
    val hasCityCharts = rememberHasCityCharts()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val play: (List<Track>, Track) -> Unit = { list, track -> atelierExplorePlay(list, track, currentId, state, viewModel) }
    val openCrate: (AtelierCrateItem) -> Unit = { item -> atelierExploreOpenCrate(item, crateTracks, viewModel, play) }
    Box(Modifier.fillMaxSize().background(AtelierDesign.room)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset + AtelierDesign.sectionGap)
        ) {
            if (editorial != null && editorial.news.isNotEmpty()) {
                item(key = "atelier-explore-gazette", contentType = "atelier-gazette") {
                    AtelierGazette(editorial.news, statusTop, viewModel::openSearch, editorial.onOpenNews)
                }
            } else {
                item(key = "atelier-explore-masthead", contentType = "atelier-masthead") { AtelierExploreMasthead(statusTop, viewModel::openSearch) }
            }
            if (editorial != null) {
                item(key = "atelier-explore-top100", contentType = "atelier-top100") {
                    ExploreDailyTop100(onPlay = editorial.onPlayChart, onTrackActions = editorial.onTrackMenu)
                }
                if (hasCityCharts) {
                    item(key = "atelier-explore-city-charts", contentType = "atelier-city-charts") {
                        ExploreCityCharts(onPlay = editorial.onPlayChart, onTrackActions = editorial.onTrackMenu)
                    }
                }
            }
            atelierExploreCrateAndCabinet(crate, crateRoll, { crateRoll += 1 }, openCrate)
            if (editorial != null && editorial.news.size > ExploreNewsFrontSize) {
                item(key = "atelier-explore-news-desk", contentType = "atelier-news-desk") { AtelierNewsDesk(editorial.news, editorial.onOpenNews) }
            }
            item(key = "atelier-explore-journey", contentType = "atelier-explore-journey") {
                com.luc4n3x.levyra.ui.moodjourney.MoodJourneyEntryCard(
                    onOpen = { preset -> if (preset == null) viewModel.openMoodJourney() else viewModel.openMoodJourney(preset) },
                    modifier = Modifier.padding(horizontal = AtelierDesign.gutter, vertical = AtelierDesign.sectionGap / 2)
                )
            }
            atelierExploreFallback(nothingLoaded, stillLoading, strings)
        }
    }
}

private fun atelierExploreChartTracks(charts: List<Track>, showCharts: Boolean): List<Track> =
    if (showCharts) charts.distinctBy(Track::id).take(50) else emptyList()

private fun atelierExploreCratePool(releases: List<AlbumHit>, fresh: List<Track>, charts: List<Track>): List<AtelierCrateItem> =
    buildList {
        releases.drop(1).forEach { album ->
            add(AtelierCrateItem("album-${atelierAlbumKey(album)}", null, album, album.title, album.artist))
        }
        fresh.forEach { track -> add(AtelierCrateItem("track-${track.id}", track, null, track.title, track.artist)) }
        charts.forEach { track -> add(AtelierCrateItem("track-${track.id}", track, null, track.title, track.artist)) }
    }.distinctBy { it.key }.take(AtelierCratePoolMax)

private fun atelierExploreCrateSelection(cratePool: List<AtelierCrateItem>, crateRoll: Int): List<AtelierCrateItem> =
    if (crateRoll == 0) cratePool.take(AtelierCrateSize) else cratePool.shuffled(Random(crateRoll)).take(AtelierCrateSize)

private fun atelierExplorePlay(
    list: List<Track>,
    track: Track,
    currentId: String?,
    state: LevyraUiState,
    viewModel: ExploreViewModel
) {
    when {
        track.id != currentId -> viewModel.playFrom(list, track)
        !state.isResolving -> viewModel.togglePlay()
    }
}

private fun atelierExploreOpenCrate(
    item: AtelierCrateItem,
    crateTracks: List<Track>,
    viewModel: ExploreViewModel,
    play: (List<Track>, Track) -> Unit
) {
    when {
        item.track != null -> play(crateTracks, item.track)
        item.album != null -> viewModel.openAlbum(item.album)
    }
}

private fun LazyListScope.atelierExploreCrateAndCabinet(
    crate: List<AtelierCrateItem>,
    crateRoll: Int,
    onRollCrate: () -> Unit,
    openCrate: (AtelierCrateItem) -> Unit
) {
    if (crate.isNotEmpty()) {
        item(key = "atelier-explore-crate", contentType = "atelier-crate") {
            AtelierCrate(crate, crateRoll, onRollCrate, openCrate)
        }
    }
}

private fun LazyListScope.atelierExploreFallback(nothingLoaded: Boolean, stillLoading: Boolean, strings: LevyraStrings) {
    if (nothingLoaded) {
        if (stillLoading) {
            item(key = "atelier-explore-skeleton-first", contentType = "atelier-skeleton") { AtelierShelfSkeleton() }
            item(key = "atelier-explore-skeleton-second", contentType = "atelier-skeleton") { AtelierShelfSkeleton() }
        } else {
            item(key = "atelier-explore-empty", contentType = "atelier-empty") {
                Text(
                    text = strings.exploreEmpty,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierDesign.textSecondary,
                    modifier = Modifier.padding(horizontal = AtelierDesign.gutter).padding(top = AtelierDesign.sectionGap)
                )
            }
        }
    }
}
