package com.luc4n3x.levyra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.viewmodel.ExploreGenreChartState
import com.luc4n3x.levyra.viewmodel.ExploreViewModel
import com.luc4n3x.levyra.viewmodel.LevyraUiState

private class ExploreGenreChartLookup(
    private val chart: ExploreGenreChartState,
    private val languageCode: String,
    private val viewModel: ExploreViewModel
) {
    private fun matches(identity: String) = chart.key == "$languageCode:$identity"

    fun tracks(identity: String): List<Track> = chart.tracks.takeIf { matches(identity) }.orEmpty()

    fun loading(identity: String): Boolean = matches(identity) && chart.isLoading

    fun awaiting(identity: String): Boolean = !matches(identity) || chart.isLoading

    fun play(chartTracks: List<Track>, tracks: List<Track>, track: Track?) {
        val queue = if (track == null || chartTracks.any { it.id == track.id }) (chartTracks + tracks).distinctBy { it.id } else tracks
        (track ?: queue.firstOrNull())?.let { start -> viewModel.playFrom(queue, start) }
    }
}

private class ExploreMoodRouteContext(
    val state: LevyraUiState,
    val strings: LevyraStrings,
    val viewModel: ExploreViewModel,
    val charts: ExploreGenreChartLookup,
    val backEnabled: Boolean,
    val onBack: () -> Unit
)

@Composable
internal fun ExploreMoodCollectionDestination(
    destination: String?,
    returnDestination: String?,
    zones: List<ExploreZone>,
    state: LevyraUiState,
    strings: LevyraStrings,
    viewModel: ExploreViewModel,
    onTrackActions: (Track) -> Unit,
    backEnabled: Boolean,
    onDestinationChange: (String?) -> Unit
) {
    val genreChart by viewModel.exploreGenreChart.collectAsStateWithLifecycle()
    val context = ExploreMoodRouteContext(
        state = state,
        strings = strings,
        viewModel = viewModel,
        charts = ExploreGenreChartLookup(genreChart, state.languageCode, viewModel),
        backEnabled = backEnabled,
        onBack = { onDestinationChange(returnDestination) }
    )
    val chartZoneId = exploreGenreChartDestinationId(destination)
    val providerCategory = exploreCategoryDestinationValue(destination)
    when {
        chartZoneId != null -> {
            val zone = exploreGenreChartZones(zones, strings.code).firstOrNull { it.id == chartZoneId } ?: return
            ExploreGenreChartRoute(context, zone)
        }
        providerCategory != null -> ExploreCategoryRoute(context, providerCategory, onTrackActions)
        else -> {
            val zoneId = exploreMoodDestinationId(destination) ?: return
            val zone = zones.firstOrNull { it.id == zoneId } ?: return
            ExploreZoneRoute(context, zone)
        }
    }
}

@Composable
private fun ExploreGenreChartRoute(context: ExploreMoodRouteContext, zone: ExploreZone) {
    val identity = "zone:${zone.id}"
    ExploreZoneChartDestination(
        zone = zone,
        chart = context.charts.tracks(identity),
        loading = context.charts.awaiting(identity),
        currentTrackId = context.state.currentTrack?.id,
        isPlaying = context.state.isPlaying,
        languageCode = context.state.languageCode,
        strings = context.strings,
        backEnabled = context.backEnabled,
        bottomInset = exploreDestinationBottomInset(context.state),
        onOpenChart = context.viewModel::openExploreZoneChart,
        onPlay = context.viewModel::playFrom,
        onRequestTrackArtwork = context.viewModel::ensureExploreTrackArtwork,
        onBack = context.onBack,
        actions = genreChartActions(context.viewModel, context.strings.offline)
    )
}

@Composable
private fun ExploreCategoryRoute(context: ExploreMoodRouteContext, providerCategory: ExploreCategory, onTrackActions: (Track) -> Unit) {
    val state = context.state
    val activeCategory = state.exploreCategories.firstOrNull { it.params == providerCategory.params } ?: providerCategory
    val categoryTracks: @Composable () -> Unit = { ExploreCategoryTracks(context, providerCategory, activeCategory) }
    val bottomInset = exploreDestinationBottomInset(state)
    when {
        LevyraIsAppleMusic -> AppleMusicGenreScreen(
            title = activeCategory.title,
            categoryParams = providerCategory.params,
            languageCode = state.languageCode,
            storefront = ChartsCatalog.region(state.selectedChartId).country,
            currentId = state.currentTrack?.id,
            bottomInset = bottomInset,
            exploreViewModel = context.viewModel,
            onTrackActions = onTrackActions,
            onOpenPlaylist = context.viewModel::openPlaylistHit,
            backEnabled = context.backEnabled,
            onBack = context.onBack,
            fallback = categoryTracks
        )
        LevyraIsSpotify -> SpotifyGenreScreen(
            title = activeCategory.title,
            params = providerCategory.params,
            languageCode = state.languageCode,
            bottomInset = bottomInset,
            backEnabled = context.backEnabled,
            onBack = context.onBack,
            onOpenPlaylist = context.viewModel::openPlaylistHit,
            fallback = categoryTracks
        )
        LevyraIsYouTubeMusic -> YouTubeMusicGenreScreen(
            title = activeCategory.title,
            params = providerCategory.params,
            languageCode = state.languageCode,
            bottomInset = bottomInset,
            backEnabled = context.backEnabled,
            onBack = context.onBack,
            onSearch = context.viewModel::openSearch,
            onOpenPlaylist = context.viewModel::openPlaylistHit,
            fallback = categoryTracks
        )
        else -> categoryTracks()
    }
}

@Composable
private fun ExploreCategoryTracks(context: ExploreMoodRouteContext, providerCategory: ExploreCategory, activeCategory: ExploreCategory) {
    val state = context.state
    val viewModel = context.viewModel
    val identity = "category:${providerCategory.params}"
    val isActive = state.exploreCategoryParams == providerCategory.params
    val tracks = if (isActive) state.exploreTracks else emptyList()
    LaunchedEffect(providerCategory.params, state.languageCode) { viewModel.openExploreCategoryChart(activeCategory) }
    LaunchedEffect(providerCategory.params, state.languageCode, isActive) {
        if (!isActive) viewModel.selectExploreCategory(activeCategory)
    }
    val chart = context.charts.tracks(identity)
    val discovery = rememberExploreGenreDiscovery(viewModel, identity, state.languageCode, state.selectedChartId) {
        viewModel.loadExploreCategoryDiscovery(activeCategory)
    }
    ExploreCollectionDestinationScreen(
        identity = providerCategory.params,
        title = activeCategory.title,
        subtitle = activeCategory.section.ifBlank { context.strings.exploreMoods },
        zone = null,
        tracks = tracks,
        isLoading = state.isExploreLoading || !isActive,
        currentTrackId = state.currentTrack?.id,
        isPlaying = state.isPlaying,
        strings = context.strings,
        backEnabled = context.backEnabled,
        onBack = context.onBack,
        onPlayAll = { context.charts.play(chart, tracks, null) },
        onPlayTrack = { track -> context.charts.play(chart, tracks, track) },
        onRequestTrackArtwork = viewModel::ensureExploreTrackArtwork,
        bottomInset = exploreDestinationBottomInset(state),
        chart = chart,
        chartLoading = context.charts.loading(identity),
        discovery = discovery.state,
        discoveryActions = discovery.actions,
        chartActions = genreChartActions(viewModel, context.strings.offline)
    )
}

@Composable
private fun ExploreZoneRoute(context: ExploreMoodRouteContext, zone: ExploreZone) {
    val state = context.state
    val viewModel = context.viewModel
    val identity = "zone:${zone.id}"
    val isActive = state.exploreZoneId == zone.id
    val tracks = if (isActive) state.exploreTracks else emptyList()
    LaunchedEffect(zone.id, state.languageCode) { viewModel.openExploreZoneChart(zone) }
    LaunchedEffect(zone.id, state.languageCode, isActive) {
        if (!isActive) viewModel.selectExploreZone(zone)
    }
    val chart = context.charts.tracks(identity)
    val discovery = rememberExploreGenreDiscovery(viewModel, identity, state.languageCode, state.selectedChartId) {
        viewModel.loadExploreZoneDiscovery(zone)
    }
    ExploreCollectionDestinationScreen(
        identity = zone.id,
        title = zone.label,
        subtitle = context.strings.exploreMoods,
        zone = zone,
        tracks = tracks,
        isLoading = state.isExploreLoading || !isActive,
        currentTrackId = state.currentTrack?.id,
        isPlaying = state.isPlaying,
        strings = context.strings,
        backEnabled = context.backEnabled,
        onBack = context.onBack,
        onPlayAll = { context.charts.play(chart, tracks, null) },
        onPlayTrack = { track -> context.charts.play(chart, tracks, track) },
        onRequestTrackArtwork = viewModel::ensureExploreTrackArtwork,
        bottomInset = exploreDestinationBottomInset(state),
        chart = chart,
        chartLoading = context.charts.loading(identity),
        discovery = discovery.state,
        discoveryActions = discovery.actions,
        chartActions = genreChartActions(viewModel, context.strings.offline)
    )
}
