package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.luc4n3x.levyra.domain.ExploreCategory
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
private const val AtelierShufflePoolMax = 60
private const val AtelierGenreLinesMax = 12
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
private fun AtelierExploreHero(
    album: AlbumHit?,
    track: Track?,
    overline: String,
    statusTop: Dp,
    minHeight: Dp,
    listState: LazyListState,
    onOpen: () -> Unit,
    onShuffle: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val title = album?.title ?: track?.title.orEmpty()
    val subtitle = album?.artist ?: track?.artist.orEmpty()
    val albumUrl = remember(album?.thumbnailUrl) { album?.thumbnailUrl?.let(::fullResolutionArtworkUrl).orEmpty() }
    Box(Modifier.fillMaxWidth().heightIn(min = minHeight).clipToBounds()) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer {
                    if (listState.firstVisibleItemIndex == 0) {
                        translationY = listState.firstVisibleItemScrollOffset * 0.45f
                    }
                }
                .levyraPressable(
                    onClick = onOpen,
                    role = Role.Button,
                    onClickLabel = if (album != null) strings.openAlbum else strings.play,
                    pressedScale = 1f
                )
        ) {
            if (album != null) {
                AsyncImage(albumUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics {})
            } else if (track != null) {
                CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {}, highRes = true)
            }
        }
        Box(
            Modifier.matchParentSize().drawWithCache {
                val fade = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.6f),
                    0.2f to Color.Transparent,
                    0.42f to Color.Transparent,
                    0.78f to AtelierDesign.room.copy(alpha = 0.9f),
                    1f to AtelierDesign.room
                )
                onDrawBehind { drawRect(fade) }
            }
        )
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(top = statusTop).padding(start = AtelierDesign.gutter, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = atelierUppercase(strings.exploreTitle),
                style = MaterialTheme.typography.labelMedium,
                color = AtelierDesign.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).clearAndSetSemantics {
                    heading()
                    contentDescription = strings.exploreTitle
                }
            )
            AtelierIconAction(Icons.Rounded.Search, strings.search, onOpenSearch, AtelierDesign.text)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(top = statusTop + 64.dp)
                .padding(horizontal = AtelierDesign.gutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = atelierUppercase(overline),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
                color = AtelierDesign.accentBright,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Light),
                color = AtelierDesign.text,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp)
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierDesign.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            AtelierHardwareKey(
                label = strings.shuffle,
                lit = false,
                onClick = onShuffle,
                modifier = Modifier.heightIn(min = 54.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Rounded.Shuffle, null, tint = AtelierDesign.accentBright, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = strings.shuffle,
                    style = MaterialTheme.typography.labelLarge,
                    color = AtelierDesign.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics {}
                )
            }
            Spacer(Modifier.height(8.dp))
        }
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
private fun AtelierCabinetKnob(angle: Float, modifier: Modifier) {
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
private fun AtelierCabinetCell(label: String, angle: Float, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.heightIn(min = 76.dp)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AtelierCabinetKnob(angle, Modifier.size(26.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = AtelierDesign.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun AtelierCabinet(artTrack: Track?, artAlbum: AlbumHit?, onShortcut: (ExploreShortcut) -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(
        Modifier.fillMaxWidth().padding(top = AtelierDesign.sectionGap).padding(horizontal = AtelierDesign.gutter)
            .clip(AtelierDesign.panelShape)
            .background(AtelierDesign.cabinet)
            .border(1.dp, AtelierDesign.hairlineStrong, AtelierDesign.panelShape)
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.9f)
                .levyraPressable(onClick = { onShortcut(ExploreShortcut.NewReleases) }, role = Role.Button, pressedScale = LevyraPressScale.Surface)
        ) {
            when {
                artAlbum != null -> AtelierArtwork(artAlbum.thumbnailUrl, Modifier.clearAndSetSemantics {})
                artTrack != null -> CoverImage(artTrack, Modifier.fillMaxSize().clearAndSetSemantics {})
            }
            Box(
                Modifier.matchParentSize().drawWithCache {
                    val solid = AtelierDesign.cabinet.copy(alpha = 0.94f)
                    val colors = listOf(solid, solid.copy(alpha = 0.55f), Color.Transparent)
                    val shade = Brush.horizontalGradient(if (layoutDirection == LayoutDirection.Rtl) colors.reversed() else colors)
                    onDrawBehind { drawRect(shade) }
                }
            )
            Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.66f).padding(20.dp)) {
                Text(
                    text = strings.exploreNewReleases,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
                    color = AtelierDesign.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    null,
                    tint = AtelierDesign.accent,
                    modifier = Modifier.padding(top = 6.dp).size(28.dp)
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairlineStrong))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            AtelierCabinetCell(strings.exploreMoods, -140f, Modifier.weight(1f)) { onShortcut(ExploreShortcut.Moods) }
            Box(Modifier.width(1.dp).fillMaxHeight().background(AtelierDesign.hairlineStrong))
            AtelierCabinetCell(strings.exploreSamples, -40f, Modifier.weight(1f)) { onShortcut(ExploreShortcut.Samples) }
        }
    }
}

@Composable
private fun AtelierGenreLine(category: ExploreCategory, artworkUrl: String, onRequestArtwork: (String) -> Unit, onOpen: () -> Unit) {
    LaunchedEffect(category.params) { onRequestArtwork(category.params) }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 76.dp)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Row)
    ) {
        if (artworkUrl.isNotBlank()) {
            Box(Modifier.matchParentSize()) {
                Box(
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.58f)
                        .drawWithCache {
                            val colors = listOf(AtelierDesign.room, AtelierDesign.room.copy(alpha = 0.4f), Color.Transparent)
                            val veil = Brush.horizontalGradient(if (layoutDirection == LayoutDirection.Rtl) colors.reversed() else colors)
                            onDrawWithContent {
                                drawContent()
                                drawRect(veil)
                            }
                        }
                ) {
                    AsyncImage(
                        artworkUrl,
                        null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.42f,
                        modifier = Modifier.fillMaxSize().clearAndSetSemantics {}
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().atelierBottomRule()
                .padding(start = AtelierDesign.gutter, end = 12.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light),
                color = AtelierDesign.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                null,
                tint = AtelierDesign.accent,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
internal fun AtelierExploreEntry(
    state: LevyraUiState,
    viewModel: ExploreViewModel,
    freshTracks: List<Track>,
    listState: LazyListState,
    bottomInset: Dp,
    onShortcut: (ExploreShortcut) -> Unit,
    onTrackActions: (Track) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val currentId = state.currentTrack?.id
    val releases = remember(state.exploreNewReleases) { state.exploreNewReleases.distinctBy(::atelierAlbumKey).take(20) }
    val fresh = remember(freshTracks) { freshTracks.distinctBy(Track::id).take(20) }
    val showCharts = state.interfaceSettings.showCharts
    val charts = remember(state.charts, showCharts) { atelierExploreChartTracks(state.charts, showCharts) }
    val chartsTitle = remember(strings, charts.size, state.chartRegions, state.selectedChartId, state.languageCode) {
        atelierExploreChartsTitle(strings, charts.size, state)
    }
    val categorySections = remember(state.exploreCategories) { buildExploreCategorySections(state.exploreCategories) }
    val heroAlbum = releases.firstOrNull()
    val heroTrack = atelierExploreHeroTrack(heroAlbum, fresh, charts)
    val shufflePool = remember(fresh, charts) { (fresh + charts).distinctBy(Track::id).take(AtelierShufflePoolMax) }
    val cratePool = remember(releases, fresh, charts) { atelierExploreCratePool(releases, fresh, charts) }
    val crateTracks = remember(cratePool) { cratePool.mapNotNull { it.track } }
    var crateRoll by rememberSaveable { mutableIntStateOf(0) }
    val crate = remember(cratePool, crateRoll) { atelierExploreCrateSelection(cratePool, crateRoll) }
    val cabinetAlbum = releases.getOrNull(1) ?: heroAlbum
    val cabinetTrack = atelierExploreCabinetTrack(cabinetAlbum, fresh)
    val nothingLoaded = releases.isEmpty() && fresh.isEmpty() && charts.isEmpty() && categorySections.isEmpty()
    val stillLoading = state.isExploreCategoriesLoading || state.isFreshCurrentsLoading || state.isNewReleasesLoading
    var chartsExpanded by rememberSaveable { mutableStateOf(false) }
    val trackAction = strings.addToPlaylist
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val play: (List<Track>, Track) -> Unit = { list, track -> atelierExplorePlay(list, track, currentId, state, viewModel) }
    val shuffle: () -> Unit = { atelierExploreShuffle(shufflePool, heroAlbum, viewModel) }
    val openCrate: (AtelierCrateItem) -> Unit = { item -> atelierExploreOpenCrate(item, crateTracks, viewModel, play) }
    BoxWithConstraints(Modifier.fillMaxSize().background(AtelierDesign.room)) {
        val heroHeight = (maxWidth * 1.12f).coerceIn(380.dp, 580.dp)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset + AtelierDesign.sectionGap)
        ) {
            atelierExploreLead(heroAlbum, heroTrack, fresh, charts, strings, statusTop, heroHeight, listState, viewModel, play, shuffle)
            atelierExploreCrateAndCabinet(crate, crateRoll, { crateRoll += 1 }, openCrate, cabinetTrack, cabinetAlbum, onShortcut)
            atelierExploreReleases(releases, strings, viewModel, onShortcut)
            atelierExploreFresh(fresh, currentId, trackAction, strings, play, onTrackActions)
            atelierExploreChartRows(charts, chartsTitle, chartsExpanded, currentId, trackAction, play, onTrackActions) {
                chartsExpanded = !chartsExpanded
            }
            atelierExploreCategories(categorySections, strings, state, viewModel, onShortcut, onOpenCategory)
            atelierExploreFallback(nothingLoaded, stillLoading, strings)
        }
    }
}

private fun atelierExploreChartTracks(charts: List<Track>, showCharts: Boolean): List<Track> =
    if (showCharts) charts.distinctBy(Track::id).take(50) else emptyList()

private fun atelierExploreChartsTitle(strings: LevyraStrings, chartCount: Int, state: LevyraUiState): String {
    val market = state.chartRegions.firstOrNull { it.id == state.selectedChartId }
        ?.let { ChartMarketDirectory.displayName(it, state.languageCode) }.orEmpty()
    return strings.chartMarketTitle(chartCount, market)
}

private fun atelierExploreHeroTrack(heroAlbum: AlbumHit?, fresh: List<Track>, charts: List<Track>): Track? =
    if (heroAlbum == null) fresh.firstOrNull() ?: charts.firstOrNull() else null

private fun atelierExploreCabinetTrack(cabinetAlbum: AlbumHit?, fresh: List<Track>): Track? =
    if (cabinetAlbum == null) fresh.getOrNull(1) ?: fresh.firstOrNull() else null

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

private fun atelierExploreShuffle(shufflePool: List<Track>, heroAlbum: AlbumHit?, viewModel: ExploreViewModel) {
    if (shufflePool.isNotEmpty()) {
        val order = shufflePool.shuffled()
        viewModel.playFrom(order, order.first())
    } else {
        heroAlbum?.let(viewModel::openAlbum)
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

private fun LazyListScope.atelierExploreLead(
    heroAlbum: AlbumHit?,
    heroTrack: Track?,
    fresh: List<Track>,
    charts: List<Track>,
    strings: LevyraStrings,
    statusTop: Dp,
    heroHeight: Dp,
    listState: LazyListState,
    viewModel: ExploreViewModel,
    play: (List<Track>, Track) -> Unit,
    shuffle: () -> Unit
) {
    if (heroAlbum != null || heroTrack != null) {
        item(key = "atelier-explore-hero", contentType = "atelier-explore-hero") {
            AtelierExploreHero(
                album = heroAlbum,
                track = heroTrack,
                overline = if (heroAlbum != null) strings.exploreNewReleases else strings.exploreFresh,
                statusTop = statusTop,
                minHeight = heroHeight,
                listState = listState,
                onOpen = {
                    when {
                        heroAlbum != null -> viewModel.openAlbum(heroAlbum)
                        heroTrack != null -> play(if (fresh.isNotEmpty()) fresh else charts, heroTrack)
                    }
                },
                onShuffle = shuffle,
                onOpenSearch = viewModel::openSearch
            )
        }
    } else {
        item(key = "atelier-explore-masthead", contentType = "atelier-masthead") {
            AtelierExploreMasthead(statusTop, viewModel::openSearch)
        }
    }
}

private fun LazyListScope.atelierExploreCrateAndCabinet(
    crate: List<AtelierCrateItem>,
    crateRoll: Int,
    onRollCrate: () -> Unit,
    openCrate: (AtelierCrateItem) -> Unit,
    cabinetTrack: Track?,
    cabinetAlbum: AlbumHit?,
    onShortcut: (ExploreShortcut) -> Unit
) {
    if (crate.isNotEmpty()) {
        item(key = "atelier-explore-crate", contentType = "atelier-crate") {
            AtelierCrate(crate, crateRoll, onRollCrate, openCrate)
        }
    }
    item(key = "atelier-explore-cabinet", contentType = "atelier-cabinet") {
        AtelierCabinet(cabinetTrack, cabinetAlbum, onShortcut)
    }
}

private fun LazyListScope.atelierExploreReleases(
    releases: List<AlbumHit>,
    strings: LevyraStrings,
    viewModel: ExploreViewModel,
    onShortcut: (ExploreShortcut) -> Unit
) {
    if (releases.isNotEmpty()) {
        item(key = "atelier-explore-releases", contentType = "atelier-album-strip") {
            Column {
                AtelierSectionLabel(strings.exploreNewReleases) {
                    AtelierTextAction(strings.showAll, { onShortcut(ExploreShortcut.NewReleases) })
                }
                LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                    items(releases, key = { atelierAlbumKey(it) }, contentType = { "atelier-album-tile" }) { album ->
                        AtelierAlbumTile(album, Modifier.width(AtelierDesign.albumCard)) { viewModel.openAlbum(album) }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.atelierExploreFresh(
    fresh: List<Track>,
    currentId: String?,
    trackAction: String,
    strings: LevyraStrings,
    play: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    if (fresh.isNotEmpty()) {
        item(key = "atelier-explore-fresh", contentType = "atelier-strip") {
            Column {
                AtelierSectionLabel(strings.exploreFresh)
                LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                    items(fresh, key = { it.id }, contentType = { "atelier-track-tile" }) { track ->
                        AtelierTrackTile(
                            track = track,
                            width = AtelierDesign.albumCard,
                            isCurrent = track.id == currentId,
                            actionsLabel = trackAction,
                            onPlay = { play(fresh, track) },
                            onActions = { onTrackActions(track) }
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.atelierExploreChartRows(
    charts: List<Track>,
    chartsTitle: String,
    chartsExpanded: Boolean,
    currentId: String?,
    trackAction: String,
    play: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit,
    onToggleCharts: () -> Unit
) {
    if (charts.isNotEmpty()) {
        item(key = "atelier-explore-charts-label", contentType = "atelier-label") {
            AtelierSectionLabel(chartsTitle)
        }
        atelierTrackRows(
            key = "atelier-explore-charts",
            tracks = charts,
            visible = if (chartsExpanded) charts.size else AtelierExploreCollapsedCharts,
            ranked = true
        ) { track, rank ->
            AtelierTrackLine(
                track = track,
                rank = rank,
                isCurrent = track.id == currentId,
                actionIcon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                actionLabel = trackAction,
                onPlay = { play(charts, track) },
                onAction = { onTrackActions(track) }
            )
        }
        atelierExpandToggle("atelier-explore-charts", charts.size, AtelierExploreCollapsedCharts, chartsExpanded, onToggleCharts)
    }
}

private fun LazyListScope.atelierExploreCategories(
    categorySections: List<ExploreCategorySection>,
    strings: LevyraStrings,
    state: LevyraUiState,
    viewModel: ExploreViewModel,
    onShortcut: (ExploreShortcut) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit
) {
    categorySections.forEach { section ->
        val structured = section.presentation == ExploreCategoryPresentation.Structured
        val title = section.providerTitle.ifBlank { if (structured) strings.genres else strings.exploreMoods }
        val categories = section.categories.filter { it.params.isNotBlank() }.distinctBy { it.params }
        val lines = categories.take(AtelierGenreLinesMax)
        val overflow = categories.size > lines.size
        item(key = "${section.key}-label", contentType = "atelier-label") {
            AtelierSectionLabel(title) {
                if (!structured || overflow) {
                    AtelierTextAction(strings.showAll, { onShortcut(ExploreShortcut.Moods) })
                }
            }
        }
        items(lines, key = { "${section.key}-${it.params}" }, contentType = { "atelier-genre-line" }) { category ->
            AtelierGenreLine(
                category = category,
                artworkUrl = state.exploreCategoryArtwork[category.params].orEmpty(),
                onRequestArtwork = { params -> viewModel.ensureExploreCategoryArtwork(params, true) },
                onOpen = { onOpenCategory(category) }
            )
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
