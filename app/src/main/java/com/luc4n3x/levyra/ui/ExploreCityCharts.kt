package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.apple.AppleCityChart
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.cityChartsCopy
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsMono
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.viewmodel.DailyChartRef
import com.luc4n3x.levyra.viewmodel.DailyTop100ViewModel

private enum class CitySkin { Mono, Ivory, Torque, Prism, Spotify, Default }

internal val AppleCityChart.numeral: String get() = badge.filter(Char::isDigit).ifEmpty { badge }

internal fun cityChartPalette(scheme: ColorScheme): Top100Palette =
    Top100Palette(scheme.tertiaryContainer, scheme.onTertiaryContainer, scheme.tertiary, scheme.tertiary.copy(alpha = 0.16f))

@Composable
internal fun ExploreCityCharts(
    onPlay: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel<DailyTop100ViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val code = LocalLevyraStrings.current.code
    LaunchedEffect(code) {
        viewModel.load(ChartsCatalog.defaultRegionForLanguage(code).country, code)
    }
    val cities = state.cities
    if (cities.isNotEmpty()) {
        val copy = remember(code) { cityChartsCopy(code) }
        val slots = remember(cities) { dailyTop100HueSlots(cities.map { it.playlistId }) }
        val palette = cityChartPalette(MaterialTheme.colorScheme)
        val shapes = coverShapes()
        val largeText = LocalDensity.current.fontScale > 1.3f
        val skin = when {
            LevyraIsMono -> CitySkin.Mono
            LevyraIsIvory -> CitySkin.Ivory
            LevyraIsTorque -> CitySkin.Torque
            LevyraIsPrismOnly -> CitySkin.Prism
            LevyraIsSpotify -> CitySkin.Spotify
            else -> CitySkin.Default
        }
        val monoOverline = if (skin == CitySkin.Mono) LevyraMonoHomeStrings.forCode(code).chartsOverline else ""
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (skin) {
                CitySkin.Mono -> MonoSectionHeader(copy.title, overline = monoOverline)
                CitySkin.Torque -> SkinSectionTitle(copy.title, TorqueGutter)
                else -> LevyraSectionHeader(
                    title = copy.title,
                    subtitle = copy.subtitle,
                    modifier = Modifier.padding(horizontal = LevyraHomeDesign.HorizontalInset)
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = LevyraHomeDesign.HorizontalInset),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(cities, key = { _, chart -> chart.playlistId }, contentType = { _, _ -> "city-chart" }) { index, chart ->
                    val tap = Modifier
                        .semantics(mergeDescendants = true) { contentDescription = "${copy.title}, ${chart.name}" }
                        .levyraPressable(onClick = { viewModel.open(DailyChartRef.City(chart)) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                    when (skin) {
                        CitySkin.Mono -> MonoCityCard(chart, slots[index], Modifier.size(Top100CoverSize).then(tap))
                        CitySkin.Ivory -> IvoryCityCard(chart, Modifier.size(TorqueTop100Size).then(tap))
                        CitySkin.Torque -> TorqueCityCard(chart, Modifier.size(TorqueTop100Size).then(tap))
                        CitySkin.Prism -> PrismCityCard(chart, slots[index], Modifier.size(Top100CoverSize).then(tap))
                        CitySkin.Spotify -> SpotifyCityCard(chart, slots[index], tap)
                        CitySkin.Default -> CityChartCover(
                            chart = chart,
                            palette = palette,
                            glyph = shapes[slots[index] % 4],
                            rotation = slots[index] * 15f,
                            largeText = largeText,
                            modifier = Modifier.size(Top100CoverSize).then(tap)
                        )
                    }
                }
            }
        }
    }
    DailyChartDialog(cities = true, onPlay = onPlay, onTrackActions = onTrackActions)
}

@Composable
private fun CityChartCover(
    chart: AppleCityChart,
    palette: Top100Palette,
    glyph: Shape,
    rotation: Float,
    largeText: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier.clip(RoundedCornerShape(Top100CoverCorner)).background(palette.canvas)) {
        Top100Glyph(glyph, rotation, palette.glyph, 128.dp, Modifier.align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp))
        Text(
            chart.badge,
            color = palette.canvas,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
                .clip(CircleShape)
                .background(palette.ink)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        Text(
            chart.name,
            color = palette.ink,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            maxLines = if (largeText) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 12.dp, bottom = 14.dp)
        )
    }
}
