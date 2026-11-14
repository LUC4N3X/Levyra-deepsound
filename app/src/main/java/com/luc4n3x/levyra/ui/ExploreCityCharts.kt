package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.lerp
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
        val monoOverline = remember(skin, code) { if (skin == CitySkin.Mono) LevyraMonoHomeStrings.forCode(code).chartsOverline else "" }
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
                val markers = coverShapes()
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
                                marker = markers[slots[index] % 4],
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
    Column(
        modifier
            .clip(RoundedCornerShape(Top100CoverCorner))
            .background(palette.canvas)
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
        Spacer(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 6.dp)
                .drawWithCache {
                    val random = Random(seed)
                    val unit = 3.dp.toPx()
                    val water = size.height * 0.12f
                    val ground = size.height - water
                    val back = Path()
                    val front = Path()
                    val windows = Path()
                    skylineRow(back, null, random, size.width, ground, 0.45f, 0.85f, unit)
                    skylineRow(front, windows, random, size.width, ground, 0.2f, 0.55f, unit)
                    val sunSide = minOf(22.dp.toPx(), size.height * 0.32f)
                    val sun = marker.createOutline(Size(sunSide, sunSide), layoutDirection, this)
                    val sunLeft = size.width * (0.55f + random.nextFloat() * 0.25f)
                    val backColor = lerp(palette.canvas, palette.accent, 0.3f)
                    val frontColor = lerp(palette.canvas, Color.Black, 0.4f)
                    val waterColor = lerp(palette.canvas, Color.Black, 0.22f)
                    val ripple = palette.accent.copy(alpha = 0.32f)
                    val rippleStroke = unit * 0.5f
                    val ripples = List(2) { row ->
                        val y = ground + water * (0.4f + row * 0.35f)
                        val start = size.width * (0.08f + random.nextFloat() * 0.5f)
                        Offset(start, y) to Offset(start + size.width * (0.15f + random.nextFloat() * 0.2f), y)
                    }
                    onDrawBehind {
                        translate(sunLeft, unit * 0.5f) { drawOutline(sun, palette.accent) }
                        drawPath(back, backColor)
                        drawRect(waterColor, Offset(0f, ground), Size(size.width, water))
                        ripples.forEach { (start, end) -> drawLine(ripple, start, end, rippleStroke, StrokeCap.Round) }
                        drawPath(front, frontColor)
                        drawPath(windows, palette.accent)
                    }
                }
        )
    }
}

private fun skylineRow(
    path: Path,
    windows: Path?,
    random: Random,
    width: Float,
    ground: Float,
    minRise: Float,
    maxRise: Float,
    unit: Float
) {
    val room = ground - unit * 5f
    val roof = CornerRadius(unit * 0.8f)
    var x = -unit * 2f * random.nextFloat()
    while (x < width) {
        val w = unit * (5 + random.nextInt(5))
        val top = ground - room * (minRise + random.nextFloat() * (maxRise - minRise))
        val bodyTop = when (random.nextInt(4)) {
            0 -> {
                path.addRoundRect(RoundRect(x, top, x + w, ground, topLeftCornerRadius = roof, topRightCornerRadius = roof))
                top
            }
            1 -> {
                val step = unit * 3f
                val inset = w * 0.22f
                path.addRoundRect(
                    RoundRect(x + inset, top, x + w - inset, top + step + roof.y, topLeftCornerRadius = roof, topRightCornerRadius = roof)
                )
                path.addRoundRect(RoundRect(x, top + step, x + w, ground, topLeftCornerRadius = roof, topRightCornerRadius = roof))
                top + step
            }
            2 -> {
                val mast = unit * 0.35f
                path.addRoundRect(RoundRect(x + w / 2f - mast, top - unit * 4f, x + w / 2f + mast, top + roof.y, CornerRadius(mast)))
                path.addRoundRect(RoundRect(x, top, x + w, ground, topLeftCornerRadius = roof, topRightCornerRadius = roof))
                top
            }
            else -> {
                val dome = CornerRadius(w / 2f)
                path.addRoundRect(RoundRect(x, top, x + w, ground, topLeftCornerRadius = dome, topRightCornerRadius = dome))
                top + w / 2f
            }
        }
        if (windows != null && random.nextFloat() < 0.7f) {
            val cell = unit * 1.4f
            val pane = unit * 0.8f
            val glow = CornerRadius(unit * 0.25f)
            var wy = bodyTop + unit * 1.5f
            while (wy + pane < ground - unit) {
                var wx = x + unit * 1.2f
                while (wx + pane < x + w - unit) {
                    if (random.nextFloat() < 0.3f) windows.addRoundRect(RoundRect(wx, wy, wx + pane, wy + pane, glow))
                    wx += cell
                }
                wy += cell
            }
        }
        x += w + unit * 0.7f
    }
}
