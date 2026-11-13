package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifyChartStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import java.time.LocalDate

private val SpotifyYearCoverSize = 244.dp
private const val SpotifyYearSuggestionColumns = 2

internal object SpotifyYearChartNavigator {
    var open by mutableStateOf(false)
}

@Composable
internal fun SpotifyYearChartOverlay(
    state: LevyraUiState,
    bottomInset: Dp,
    onPlay: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit,
    onOpenGenre: (ExploreZone) -> Unit
) {
    if (!SpotifyYearChartNavigator.open) return
    val tracks = state.charts
    if (tracks.isEmpty()) {
        SpotifyYearChartNavigator.open = false
        return
    }
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifyChartStrings.forCode(strings.code) }
    val region = remember(state.selectedChartId) { ChartsCatalog.region(state.selectedChartId).label }
    val year = remember { LocalDate.now().year }
    val title = "${com.luc4n3x.levyra.ui.i18n.spotifyYearCopy(strings.code).topTracks} $region $year"
    val cover = tracks.first().largeThumbnailUrl.ifBlank { tracks.first().thumbnailUrl }
    val suggestions = rememberProviderGenreZones()
    val coverPx = with(LocalDensity.current) { SpotifyYearCoverSize.toPx() }
    val close = { SpotifyYearChartNavigator.open = false }
    SpotifyPlaylistPage(
        title = title,
        toneTrack = tracks.first(),
        fallbackTone = Color(0xFFD9822B),
        heroCollapsePx = coverPx,
        showFindBar = false,
        description = copy.chartDescription.format(region),
        ownerLabel = "Levyra",
        meta = strings.formatTrackCount(tracks.size),
        thumbUrl = cover,
        tracks = tracks,
        currentId = state.currentTrack?.id,
        isPlaying = state.isPlaying,
        bottomInset = bottomInset,
        onPlay = onPlay,
        onTrackActions = onTrackActions,
        onDismiss = close,
        footer = { spotifyYearSuggestions(strings.youMightAlsoLike, suggestions) { zone -> close(); onOpenGenre(zone) } }
    ) { headerScroll, progress ->
        Box(Modifier.fillMaxWidth().padding(top = 36.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(SpotifyYearCoverSize)
                    .graphicsLayer {
                        val scale = 1f - progress * 0.3f
                        scaleX = scale
                        scaleY = scale
                        translationY = headerScroll.coerceAtMost(coverPx) * 0.55f
                        alpha = 1f - progress
                    }
                    .shadow(24.dp, RoundedCornerShape(2.dp))
                    .background(SpotifyDesign.placeholder)
            ) {
                StableRemoteArtwork(cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Text(
                    title.replace(" $region", "\n$region"), color = Color.White, fontSize = 32.sp, lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold, maxLines = 3,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 12.dp, bottom = 18.dp)
                )
            }
        }
    }
}

private fun LazyListScope.spotifyYearSuggestions(title: String, charts: List<ExploreZone>, onOpen: (ExploreZone) -> Unit) {
    if (charts.isEmpty()) return
    item(key = "spotify-year-suggestions-title", contentType = "spotify-year-suggestions-title") {
        Text(
            title, color = SpotifyDesign.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp).semantics { heading() }
        )
    }
    charts.chunked(SpotifyYearSuggestionColumns).forEachIndexed { index, row ->
        item(key = "spotify-year-suggestions-$index", contentType = "spotify-year-suggestions-row") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { zone -> SpotifyYearSuggestionCard(rememberProviderGenreChart(zone), Modifier.weight(1f)) { onOpen(zone) } }
                repeat(SpotifyYearSuggestionColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SpotifyYearSuggestionCard(chart: ProviderGenreChart, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Column(modifier.levyraPressable(onClick = onClick, onClickLabel = chart.title, role = Role.Button)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(SpotifyDesign.placeholder)) { chart.cover(shape) }
        Text(
            chart.title, color = SpotifyDesign.text, fontSize = 16.sp, lineHeight = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp)
        )
        if (chart.subtitle.isNotBlank()) {
            Text(
                chart.subtitle, color = SpotifyDesign.subdued, fontSize = 14.sp, lineHeight = 18.sp, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
