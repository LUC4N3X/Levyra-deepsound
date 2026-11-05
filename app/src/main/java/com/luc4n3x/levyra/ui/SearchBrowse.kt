package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.material3.toShape
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SearchBrowseCopy
import com.luc4n3x.levyra.ui.i18n.searchBrowseCopy
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.SearchViewModel
import java.util.Locale

private val SearchBrowseTileHeight = 100.dp
private val SearchBrowseGap = 10.dp
private const val SearchBrowseMaxFontGrowth = 1.6f
private val SearchRecentPreferredWidth = 132.dp
private val SearchRecentCarouselHeight = 140.dp
private val SearchSampleWidth = 128.dp
private val SearchSampleHeight = 196.dp
private val SearchRecentGap = 12.dp

internal sealed interface LevyraSearchBrowseTile {
    val key: String

    class Zone(val zone: ExploreZone) : LevyraSearchBrowseTile {
        override val key: String = "zone-${zone.id}"
    }

    data object Podcasts : LevyraSearchBrowseTile {
        override val key: String = "podcasts"
    }

    class Category(val category: ExploreCategory) : LevyraSearchBrowseTile {
        override val key: String = "category-${category.params}"
    }
}

internal enum class LevyraSearchBrowseSectionKind {
    Moods,
    Genres,
    Mixed,
    Picks
}

internal class LevyraSearchBrowseSection(
    val key: String,
    val kind: LevyraSearchBrowseSectionKind,
    val providerTitle: String,
    val tiles: List<LevyraSearchBrowseTile>
)

internal fun levyraSearchBrowseSections(
    zones: List<ExploreZone>,
    categories: List<ExploreCategory>
): List<LevyraSearchBrowseSection> {
    val featuredIds = listOf(ExploreCatalog.NEW_RELEASES_ZONE_ID, ExploreCatalog.LOCAL_WAVE_ZONE_ID)
    val featured = featuredIds.mapNotNull { id -> zones.firstOrNull { zone -> zone.id == id } }
    val providerSections = buildExploreCategorySections(categories)
    val providerTitles = providerSections.asSequence()
        .flatMap { section -> section.categories.asSequence() }
        .mapTo(HashSet()) { category -> category.title.lowercase(Locale.ROOT) }
    val picks = zones.asSequence()
        .filter { zone -> zone.id !in featuredIds && zone.label.trim().lowercase(Locale.ROOT) !in providerTitles }
        .distinctBy { zone -> zone.id }
        .toList()
    val sections = buildList {
        providerSections.forEach { section ->
            val kind = when (section.presentation) {
                ExploreCategoryPresentation.Atmospheric -> LevyraSearchBrowseSectionKind.Moods
                ExploreCategoryPresentation.Structured -> LevyraSearchBrowseSectionKind.Genres
                ExploreCategoryPresentation.Mixed -> LevyraSearchBrowseSectionKind.Mixed
            }
            add(LevyraSearchBrowseSection(section.key, kind, section.providerTitle, section.categories.map(LevyraSearchBrowseTile::Category)))
        }
        if (picks.isNotEmpty()) {
            add(LevyraSearchBrowseSection("picks", LevyraSearchBrowseSectionKind.Picks, "", picks.map(LevyraSearchBrowseTile::Zone)))
        }
    }
    val leading = featured.map(LevyraSearchBrowseTile::Zone)
    val first = sections.firstOrNull()
        ?: return if (leading.isEmpty()) emptyList() else listOf(LevyraSearchBrowseSection("picks", LevyraSearchBrowseSectionKind.Picks, "", leading))
    return listOf(LevyraSearchBrowseSection(first.key, first.kind, first.providerTitle, leading + first.tiles)) + sections.drop(1)
}

internal fun searchBrowseZoneCovers(localFresh: List<Track>, worldFresh: List<Track>): Map<String, String> {
    fun covers(scope: ExploreFreshScope): List<String> = exploreFreshFeed(scope, localFresh, worldFresh).spotlight
        .map { track -> exploreCoverArtworkTrack(track).let { cover -> cover.largeThumbnailUrl.ifBlank { cover.thumbnailUrl }.trim() } }
        .filter(String::isNotBlank)
    val local = covers(ExploreFreshScope.Local)
    val world = if (worldFresh.isEmpty()) emptyList() else covers(ExploreFreshScope.World)
    val newReleases = world.firstOrNull() ?: local.firstOrNull().orEmpty()
    val localWave = local.firstOrNull { cover -> cover != newReleases }.orEmpty()
    return buildMap {
        if (newReleases.isNotBlank()) put(ExploreCatalog.NEW_RELEASES_ZONE_ID, newReleases)
        if (localWave.isNotBlank()) put(ExploreCatalog.LOCAL_WAVE_ZONE_ID, localWave)
    }
}

@Immutable
internal class LevyraSearchBrowse(
    val strings: LevyraStrings,
    val copy: SearchBrowseCopy,
    val sections: List<LevyraSearchBrowseSection>,
    val categoryArtwork: Map<String, String>,
    val zoneCovers: Map<String, String>,
    val requestCategoryArtwork: (String) -> Unit
)

@Composable
internal fun rememberLevyraSearchBrowse(state: LevyraUiState, viewModel: SearchViewModel): LevyraSearchBrowse {
    val strings = LocalLevyraStrings.current
    LaunchedEffect(strings) {
        viewModel.ensureExplore(strings)
        viewModel.ensureExploreSamples()
    }
    val zones = remember(strings) { ExploreCatalog.getZones(strings) }
    val categories = state.exploreCategories
    val sections = remember(zones, categories) { levyraSearchBrowseSections(zones, categories) }
    val categoryArtwork = state.exploreCategoryArtwork
    val localFresh = state.exploreFreshTracks
    val worldFresh = state.exploreWorldFreshTracks
    val zoneCovers = remember(localFresh, worldFresh) { searchBrowseZoneCovers(localFresh, worldFresh) }
    return remember(strings, sections, categoryArtwork, zoneCovers) {
        LevyraSearchBrowse(
            strings = strings,
            copy = searchBrowseCopy(strings.code),
            sections = sections,
            categoryArtwork = categoryArtwork,
            zoneCovers = zoneCovers,
            requestCategoryArtwork = viewModel::ensureExploreCategoryArtwork
        )
    }
}

internal fun LazyListScope.levyraSearchBrowseItems(
    browse: LevyraSearchBrowse,
    onOpenZone: (ExploreZone) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit,
    podcastsTitle: String,
    podcastsArtwork: String,
    onOpenPodcasts: () -> Unit
) {
    val tiles = levyraSearchBrowseTiles(browse.sections).let { all ->
        val featured = all.takeWhile { it is LevyraSearchBrowseTile.Zone }
        featured + LevyraSearchBrowseTile.Podcasts + all.drop(featured.size)
    }
    if (tiles.isEmpty()) return
    item(key = "search-browse-header", contentType = "search-browse-header") {
        LevyraSectionHeader(
            title = browse.copy.browseTitle,
            modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
        )
    }
    tiles.chunked(2).forEach { row ->
        item(key = "search-browse-row-${row.first().key}", contentType = "search-browse-row") {
            val fontScale = LocalDensity.current.fontScale.coerceIn(1f, SearchBrowseMaxFontGrowth)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = SearchBrowseGap - 3.dp),
                horizontalArrangement = Arrangement.spacedBy(SearchBrowseGap)
            ) {
                val tileModifier = Modifier.weight(1f).height(SearchBrowseTileHeight * fontScale)
                row.forEach { tile ->
                    if (tile is LevyraSearchBrowseTile.Podcasts) {
                        ExploreDiscoveryCategoryCard(title = podcastsTitle, identity = "podcasts", artworkUrl = podcastsArtwork, modifier = tileModifier, onClick = onOpenPodcasts)
                    } else {
                        SearchBrowseTileCard(tile, browse, tileModifier, onOpenZone, onOpenCategory)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

internal fun levyraSearchBrowseTiles(sections: List<LevyraSearchBrowseSection>): List<LevyraSearchBrowseTile> {
    val leading = sections.firstOrNull()?.tiles.orEmpty().takeWhile { it is LevyraSearchBrowseTile.Zone }
    val rest = sections.mapIndexed { index, section -> if (index == 0) section.copyTiles(section.tiles.drop(leading.size)) else section }
        .sortedBy { section -> searchBrowseKindOrder(section.kind) }
        .flatMap { section -> section.tiles }
    return (leading + rest).distinctBy { tile -> tile.key }
}

private fun LevyraSearchBrowseSection.copyTiles(tiles: List<LevyraSearchBrowseTile>): LevyraSearchBrowseSection =
    LevyraSearchBrowseSection(key, kind, providerTitle, tiles)

private fun searchBrowseKindOrder(kind: LevyraSearchBrowseSectionKind): Int = when (kind) {
    LevyraSearchBrowseSectionKind.Moods -> 0
    LevyraSearchBrowseSectionKind.Mixed -> 1
    LevyraSearchBrowseSectionKind.Genres -> 2
    LevyraSearchBrowseSectionKind.Picks -> 3
}

@Composable
private fun SearchBrowseTileCard(
    tile: LevyraSearchBrowseTile,
    browse: LevyraSearchBrowse,
    modifier: Modifier,
    onOpenZone: (ExploreZone) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit
) {
    when (tile) {
        is LevyraSearchBrowseTile.Category -> {
            val category = tile.category
            LaunchedEffect(category.params) { browse.requestCategoryArtwork(category.params) }
            ExploreDiscoveryCategoryCard(
                title = category.title,
                identity = category.params,
                artworkUrl = browse.categoryArtwork[category.params].orEmpty(),
                modifier = modifier,
                onClick = { onOpenCategory(category) }
            )
        }
        LevyraSearchBrowseTile.Podcasts -> Unit
        is LevyraSearchBrowseTile.Zone -> {
            val zone = tile.zone
            val cover = browse.zoneCovers[zone.id].orEmpty()
            ExploreDiscoveryCategoryCard(
                title = zone.label,
                identity = if (zone.id == ExploreCatalog.NEW_RELEASES_ZONE_ID) "new-releases" else zone.id,
                artworkUrl = cover.ifBlank { rememberExploreMoodArtworkUrl(zone) },
                modifier = modifier,
                onClick = { onOpenZone(zone) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LevyraRecentSearchesRail(
    title: String,
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    menu: @Composable (track: Track, expanded: Boolean, onDismiss: () -> Unit) -> Unit
) {
    val actionsLabel = LocalLevyraStrings.current.actions
    val carouselState = rememberCarouselState { tracks.size }
    Column {
        LevyraSectionHeader(title = title, modifier = Modifier.padding(bottom = 6.dp))
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = SearchRecentPreferredWidth,
            itemSpacing = SearchRecentGap,
            contentPadding = PaddingValues(horizontal = RecentSearchGutter),
            modifier = Modifier.recentSearchesBleed(RecentSearchGutter).fillMaxWidth().height(SearchRecentCarouselHeight)
        ) { index ->
            val track = tracks[index]
            var menuExpanded by remember(track.id) { mutableStateOf(false) }
            val reveal = carouselItemDrawInfo.let { info ->
                if (info.maxSize <= info.minSize) 1f else ((info.size - info.minSize) / (info.maxSize - info.minSize)).coerceIn(0f, 1f)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .levyraPressable(
                        onClick = { onTrackClick(track) },
                        onLongClick = { menuExpanded = true },
                        onLongClickLabel = actionsLabel,
                        pressedScale = LevyraPressScale.Tile,
                        role = Role.Button
                    )
                    .semantics(mergeDescendants = true) { contentDescription = "${track.title}, ${track.artist}" }
            ) {
                CoverImage(
                    track = track,
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = reveal }
                        .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f)))
                )
                Column(
                    Modifier.align(Alignment.BottomStart).padding(10.dp).graphicsLayer { alpha = reveal },
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(track.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                menu(track, menuExpanded) { menuExpanded = false }
            }
        }
    }
}

@Composable
internal fun LevyraSearchSamplesRail(
    title: String,
    samples: List<Track>,
    previewEnabled: Boolean,
    onOpen: (Int) -> Unit
) {
    val code = LocalLevyraStrings.current.code
    val musicLabel = remember(code) { LevyraSpotifySkinStrings.forCode(code).music }
    Column {
        LevyraSectionHeader(title = title, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        LazyRow(
            modifier = Modifier.recentSearchesBleed(RecentSearchGutter),
            horizontalArrangement = Arrangement.spacedBy(SearchRecentGap),
            contentPadding = PaddingValues(horizontal = RecentSearchGutter)
        ) {
            itemsIndexed(samples, key = { _, track -> "sample-${track.id}" }, contentType = { _, _ -> "search-sample" }) { index, track ->
                val shape = MaterialTheme.shapes.extraLarge
                Box(
                    Modifier
                        .size(width = SearchSampleWidth, height = SearchSampleHeight)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .levyraPressable(onClick = { onOpen(index) }, pressedScale = LevyraPressScale.Tile, role = Role.Button)
                        .semantics(mergeDescendants = true) { contentDescription = "${track.title}, ${track.artist}" }
                ) {
                    CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                    if (previewEnabled && index == 0) SpotifySamplePreview(track)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.62f))))
                    Text(
                        spotifyShortTag(track, musicLabel),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun rememberSearchPodcastsArtwork(languageCode: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val factory = remember(context.applicationContext) { com.luc4n3x.levyra.feature.podcast.PodcastLibraryViewModel.factory(context.applicationContext) }
    val podcasts: com.luc4n3x.levyra.feature.podcast.PodcastLibraryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key = "levyra-podcasts", factory = factory)
    LaunchedEffect(languageCode) { podcasts.activate(languageCode) }
    val state by podcasts.state.collectAsStateWithLifecycle()
    return remember(state.discover) {
        state.discover.asSequence().flatMap { it.episodes.asSequence() }
            .map { it.largeThumbnailUrl.ifBlank { it.thumbnailUrl } }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
    }
}
