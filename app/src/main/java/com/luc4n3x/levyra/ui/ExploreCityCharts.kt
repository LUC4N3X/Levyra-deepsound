package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsMono
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.viewmodel.DailyChartRef
import com.luc4n3x.levyra.viewmodel.DailyTop100ViewModel
import kotlin.random.Random

private enum class CitySkin { Mono, Ivory, Torque, Prism, Spotify, YouTubeMusic, Atelier, Default }

internal val AppleCityChart.numeral: String get() = badge.filter(Char::isDigit).ifEmpty { badge }

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
        val skin = when {
            LevyraIsMono -> CitySkin.Mono
            LevyraIsIvory -> CitySkin.Ivory
            LevyraIsTorque -> CitySkin.Torque
            LevyraIsPrismOnly -> CitySkin.Prism
            LevyraIsSpotify -> CitySkin.Spotify
            LevyraIsYouTubeMusic -> CitySkin.YouTubeMusic
            LevyraIsAtelier -> CitySkin.Atelier
            else -> CitySkin.Default
        }
        val monoOverline = if (skin == CitySkin.Mono) LevyraMonoHomeStrings.forCode(code).chartsOverline else ""
        val openCity = { chart: AppleCityChart -> viewModel.open(DailyChartRef.City(chart)) }
        when (skin) {
            CitySkin.YouTubeMusic -> YouTubeMusicCityShelf(cities, slots, copy, rememberTop100Date(code), modifier, openCity)
            CitySkin.Atelier -> AtelierCityShelf(cities, slots, copy, rememberTop100Date(code), openCity)
            else -> Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            .levyraPressable(onClick = { openCity(chart) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                        when (skin) {
                            CitySkin.Mono -> MonoCityCard(chart, slots[index], Modifier.size(Top100CoverSize).then(tap))
                            CitySkin.Ivory -> IvoryCityCard(chart, Modifier.size(TorqueTop100Size).then(tap))
                            CitySkin.Torque -> TorqueCityCard(chart, Modifier.size(TorqueTop100Size).then(tap))
                            CitySkin.Prism -> PrismCityCard(chart, slots[index], Modifier.size(Top100CoverSize).then(tap))
                            CitySkin.Spotify -> SpotifyCityCard(chart, slots[index], tap)
                            else -> CityChartCover(
                                chart = chart,
                                palette = rememberTop100Palette(global = false, slot = slots[index]),
                                marker = coverShapes()[slots[index] % 4],
                                modifier = Modifier.size(Top100CoverSize).then(tap)
                            )
                        }
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
    marker: Shape,
    modifier: Modifier = Modifier
) {
    val seed = chart.name.hashCode()
    Box(
        modifier
            .clip(RoundedCornerShape(Top100CoverCorner))
            .background(palette.canvas)
            .drawWithCache {
                val random = Random(seed)
                val count = 6 + random.nextInt(4)
                val gap = 3.dp.toPx()
                val radius = CornerRadius(6.dp.toPx())
                val shades = arrayOf(palette.glyph, palette.accent.copy(alpha = 0.3f), palette.ink.copy(alpha = 0.14f))
                val low = count / 2 + random.nextInt(count - count / 2)
                val weights = FloatArray(count) { if (it == low) 1.5f else 0.7f + random.nextFloat() * 0.6f }
                val unit = (size.width - gap * (count + 1)) / weights.sum()
                val widths = FloatArray(count) { weights[it] * unit }
                val lefts = FloatArray(count)
                var x = gap
                for (i in 0 until count) {
                    lefts[i] = x
                    x += widths[i] + gap
                }
                val tops = FloatArray(count) {
                    size.height * (1f - if (it == low) 0.16f + random.nextFloat() * 0.05f else 0.2f + random.nextFloat() * 0.2f)
                }
                val tones = IntArray(count) { random.nextInt(shades.size) }
                val markerSide = minOf(22.dp.toPx(), widths[low])
                val markerOutline = marker.createOutline(Size(markerSide, markerSide), layoutDirection, this)
                val markerLeft = lefts[low] + (widths[low] - markerSide) / 2f
                val markerTop = tops[low] - gap * 2 - markerSide
                onDrawBehind {
                    for (i in 0 until count) {
                        drawRoundRect(shades[tones[i]], Offset(lefts[i], tops[i]), Size(widths[i], size.height - tops[i] + radius.y), radius)
                    }
                    translate(markerLeft, markerTop) { drawOutline(markerOutline, palette.accent) }
                }
            }
    ) {
        Column(Modifier.padding(start = 14.dp, top = 14.dp, end = 12.dp)) {
            Text(
                chart.badge,
                color = palette.accent,
                fontSize = fixedSp(12f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                chart.name,
                color = palette.ink,
                style = MaterialTheme.typography.headlineSmall,
                fontSize = fixedSp(22f),
                lineHeight = fixedSp(25f),
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
