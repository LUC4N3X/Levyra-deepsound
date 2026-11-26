package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.desktop.app.state.OpenChart
import com.luc4n3x.levyra.desktop.app.ui.components.CookieShape
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.SectionPlayButton
import com.luc4n3x.levyra.desktop.app.ui.components.SunnyShape
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

internal val ChartCardSize: Dp = 184.dp
private val ChartCorner = 24.dp
private const val HUE_SLOTS = 12
private const val SEED_HUE = 197f
private val TopBars = floatArrayOf(0.34f, 0.52f, 0.42f, 0.7f, 0.58f, 0.86f, 0.66f, 1f)

@Immutable
internal data class ChartPalette(val canvas: Color, val ink: Color, val accent: Color, val light: Color, val deep: Color)

internal fun chartHueSlots(ids: List<String>): List<Int> {
    val taken = HashSet<Int>()
    return ids.map { id ->
        var slot = Math.floorMod(id.hashCode(), HUE_SLOTS)
        if (taken.size < HUE_SLOTS) while (!taken.add(slot)) slot = (slot + 1) % HUE_SLOTS
        slot
    }
}

private fun hsb(hue: Float, saturation: Float, brightness: Float): Color =
    Color(java.awt.Color.HSBtoRGB(((hue % 360f) + 360f) % 360f / 360f, saturation, brightness) or 0xFF000000.toInt())

internal fun chartPalette(global: Boolean, slot: Int): ChartPalette {
    val hue = if (global) SEED_HUE else SEED_HUE + (slot + 1) * (360f / HUE_SLOTS)
    return ChartPalette(
        canvas = hsb(hue, 0.62f, 0.36f),
        ink = hsb(hue, 0.05f, 0.98f),
        accent = hsb(hue, 0.34f, 0.95f),
        light = hsb(hue, 0.58f, 0.66f),
        deep = hsb(hue, 0.70f, 0.26f)
    )
}

internal fun chartDate(locale: Locale): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM", locale)).uppercase(locale)

private val markers: List<Shape> = listOf(CookieShape, SunnyShape, CircleShape, RoundedCornerShape(30))

@Composable
internal fun Top100Card(label: String, date: String, global: Boolean, slot: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = remember(global, slot) { chartPalette(global, slot) }
    val bars = remember(slot) { FloatArray(TopBars.size) { TopBars[(it + slot) % TopBars.size] } }
    Box(
        modifier
            .size(ChartCardSize)
            .clip(RoundedCornerShape(ChartCorner))
            .clickable(role = Role.Button, onClick = onClick)
            .drawWithCache {
                val fill = Brush.linearGradient(listOf(palette.light, palette.deep), start = Offset.Zero, end = Offset(size.width, size.height))
                val barWidth = 7.dp.toPx()
                val gap = 5.dp.toPx()
                val maxHeight = size.height * 0.46f
                val radius = CornerRadius(barWidth / 2f)
                val right = size.width - 14.dp.toPx()
                onDrawBehind {
                    drawRect(fill)
                    bars.forEachIndexed { index, level ->
                        val height = maxHeight * level
                        val left = right - (bars.size - index) * (barWidth + gap) + gap
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.10f + 0.06f * level),
                            topLeft = Offset(left, size.height - height),
                            size = Size(barWidth, height + radius.x),
                            cornerRadius = radius
                        )
                    }
                }
            }
    ) {
        Text(
            date,
            color = Color.White.copy(alpha = 0.86f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 17.dp)
        )
        Box(
            Modifier.align(Alignment.TopEnd).padding(12.dp).size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(LevyraIcons.Chart, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 15.dp)) {
            Text("100", color = Color.White, fontSize = 60.sp, lineHeight = 60.sp, fontWeight = FontWeight.Black, letterSpacing = (-2.5).sp, maxLines = 1)
            Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun CityChartCard(badge: String, name: String, slot: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = remember(slot) { chartPalette(false, slot) }
    val marker = markers[slot % markers.size]
    Column(
        modifier
            .size(ChartCardSize)
            .clip(RoundedCornerShape(ChartCorner))
            .background(palette.canvas)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 14.dp)) {
            Text(badge, color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                name,
                color = palette.ink,
                fontSize = 24.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp).citySkyline(name.hashCode(), palette, marker))
    }
}

private fun Modifier.citySkyline(seed: Int, palette: ChartPalette, marker: Shape): Modifier =
    drawWithCache {
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

private fun skylineRow(path: Path, windows: Path?, random: Random, width: Float, ground: Float, minRise: Float, maxRise: Float, unit: Float) {
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
                path.addRoundRect(RoundRect(x + inset, top, x + w - inset, top + step + roof.y, topLeftCornerRadius = roof, topRightCornerRadius = roof))
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

@Composable
internal fun ChartDetailPage(
    chart: OpenChart,
    date: String,
    playLabel: String,
    errorLabel: String,
    retryLabel: String,
    backLabel: String,
    actions: TrackActions,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(key = "chart-header") {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 20.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBack)
                        .align(Alignment.Top),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(LevyraIcons.ChevronLeft, contentDescription = backLabel, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
                if (chart.city) {
                    CityChartCard(chart.badge, chart.title, chart.slot, onClick = {}, modifier = Modifier.size(220.dp))
                } else {
                    Top100Card(chart.title, date, chart.global, chart.slot, onClick = {}, modifier = Modifier.size(220.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (chart.city) chart.badge else "Top 100",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(chart.title, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (chart.tracks.isNotEmpty()) SectionPlayButton(playLabel) { actions.onPlay(chart.tracks, 0) }
                }
            }
        }
        when {
            chart.loading -> item(key = "chart-loading") {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                }
            }
            chart.tracks.isEmpty() -> item(key = "chart-error") {
                Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onRetry) { Text(retryLabel) }
                }
            }
            else -> items(chart.tracks.size, key = { "chart-track-${chart.tracks[it].id}-$it" }) { index ->
                val track = chart.tracks[index]
                TrackRow(
                    track = track,
                    isCurrent = track.id == actions.currentTrackId,
                    isFavorite = actions.isFavorite(track),
                    onPlay = { actions.onPlay(chart.tracks, index) },
                    onPlayNext = { actions.onPlayNext(track) },
                    onEnqueue = { actions.onEnqueue(track) },
                    onToggleFavorite = { actions.onToggleFavorite(track) },
                    onAddToPlaylist = { actions.onAddToPlaylist(track) },
                    position = index + 1
                )
            }
        }
    }
}
