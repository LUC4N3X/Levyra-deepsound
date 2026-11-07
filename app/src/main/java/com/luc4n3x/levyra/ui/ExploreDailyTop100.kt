package com.luc4n3x.levyra.ui

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.apple.AppleDailyChart
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.DailyTop100Copy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.dailyTop100Copy
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.viewmodel.DailyTop100ChartUiState
import com.luc4n3x.levyra.viewmodel.DailyTop100ViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Top100CoverSize = 164.dp
private val Top100CoverCorner = 24.dp
private const val HUE_SLOTS = 12

@Immutable
private data class Top100Palette(val canvas: Color, val ink: Color, val accent: Color, val glyph: Color)

internal fun dailyTop100HueSlots(ids: List<String>): List<Int> {
    val taken = HashSet<Int>()
    return ids.map { id ->
        var slot = Math.floorMod(id.hashCode(), HUE_SLOTS)
        if (taken.size < HUE_SLOTS) while (!taken.add(slot)) slot = (slot + 1) % HUE_SLOTS
        slot
    }
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
internal fun ExploreDailyTop100(
    onPlay: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel<DailyTop100ViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { dailyTop100Copy(strings.code) }
    LaunchedEffect(strings.code) {
        viewModel.load(ChartsCatalog.defaultRegionForLanguage(strings.code).country, strings.code)
    }
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val date = remember(locale) {
        LocalDate.now().format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale)).uppercase(locale)
    }
    val charts = state.charts
    val slots = remember(charts) { dailyTop100HueSlots(charts.map { it.playlistId }) }
    if (charts.isNotEmpty() && com.luc4n3x.levyra.ui.theme.LevyraIsAtelier) {
        AtelierTop100Shelf(charts, slots, copy, date, viewModel::open)
    } else if (charts.isNotEmpty()) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (com.luc4n3x.levyra.ui.theme.LevyraIsMono) {
                MonoSectionHeader(copy.title, overline = com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings.forCode(strings.code).chartsOverline)
            } else {
                LevyraSectionHeader(
                    title = copy.title,
                    subtitle = copy.subtitle,
                    modifier = Modifier.padding(horizontal = LevyraHomeDesign.HorizontalInset)
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = LevyraHomeDesign.HorizontalInset),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(charts, key = { _, chart -> chart.playlistId }, contentType = { _, _ -> "daily-top-100" }) { index, chart ->
                    val label = chartLabel(chart, copy)
                    if (com.luc4n3x.levyra.ui.theme.LevyraIsMono) {
                        MonoTop100Cover(
                            label, date, slots[index], chart.global, Top100CoverSize,
                            Modifier.semantics(mergeDescendants = true) { contentDescription = "${copy.title}, $label" }
                                .levyraPressable(onClick = { viewModel.open(chart) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                        )
                        return@itemsIndexed
                    }
                    if (LevyraIsPrismOnly) {
                        PrismTop100Card(
                            label, date, slots[index], chart.global,
                            Modifier.size(Top100CoverSize)
                                .semantics(mergeDescendants = true) { contentDescription = "${copy.title}, $label" }
                                .levyraPressable(onClick = { viewModel.open(chart) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                        )
                        return@itemsIndexed
                    }
                    if (com.luc4n3x.levyra.ui.theme.LevyraIsSpotify) {
                        SpotifyTop100Card(label, copy.title, copy.subtitle, slots[index], chart.global) { viewModel.open(chart) }
                        return@itemsIndexed
                    }
                    Top100Cover(
                        label = label,
                        date = date,
                        palette = rememberTop100Palette(chart.global, slots[index]),
                        glyph = coverShapes()[slots[index] % 4],
                        rotation = slots[index] * 15f,
                        modifier = Modifier
                            .size(Top100CoverSize)
                            .semantics(mergeDescendants = true) { contentDescription = "${copy.title}, $label" }
                            .levyraPressable(onClick = { viewModel.open(chart) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                    )
                }
            }
        }
    }
    state.open?.let { open ->
        val index = charts.indexOfFirst { it.playlistId == open.chart.playlistId }
        val slot = slots.getOrNull(index) ?: dailyTop100HueSlots(listOf(open.chart.playlistId)).first()
        Dialog(
            onDismissRequest = viewModel::close,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            if (com.luc4n3x.levyra.ui.theme.LevyraIsMono) {
                MonoTop100ChartScreen(open, chartLabel(open.chart, copy), date, slot, copy.failed, copy.retry, viewModel::close, viewModel::retry, onPlay) { track ->
                    viewModel.close()
                    onTrackActions(track)
                }
                return@Dialog
            }
            if (LevyraIsPrismOnly) {
                PrismTop100ChartScreen(open, chartLabel(open.chart, copy), date, slot, copy, viewModel::close, viewModel::retry, onPlay) { track ->
                    viewModel.close()
                    onTrackActions(track)
                }
                return@Dialog
            }
            if (com.luc4n3x.levyra.ui.theme.LevyraIsAtelier) {
                AtelierTop100ChartScreen(open, chartLabel(open.chart, copy), date, slot, copy, viewModel::close, viewModel::retry, onPlay) { track ->
                    viewModel.close()
                    onTrackActions(track)
                }
                return@Dialog
            }
            Top100ChartScreen(
                open = open,
                label = chartLabel(open.chart, copy),
                date = date,
                copy = copy,
                palette = rememberTop100Palette(open.chart.global, slot),
                glyph = coverShapes()[slot % 4],
                rotation = slot * 15f,
                onBack = viewModel::close,
                onRetry = viewModel::retry,
                onPlay = onPlay,
                onTrackActions = { track ->
                    viewModel.close()
                    onTrackActions(track)
                }
            )
        }
    }
}

private fun chartLabel(chart: AppleDailyChart, copy: DailyTop100Copy): String = if (chart.global) copy.global else chart.label

@Composable
private fun rememberTop100Palette(global: Boolean, slot: Int): Top100Palette {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    return remember(global, slot, scheme.primary, scheme.inverseSurface, scheme.inverseOnSurface, scheme.inversePrimary, dark) {
        if (global) {
            Top100Palette(scheme.inverseSurface, scheme.inverseOnSurface, scheme.inversePrimary, scheme.inverseOnSurface.copy(alpha = 0.08f))
        } else {
            val shift = slot * (360.0 / HUE_SLOTS)
            Top100Palette(
                canvas = shiftedTone(scheme.primary, shift, if (dark) 30.0 else 38.0),
                ink = shiftedTone(scheme.primary, shift, 97.0),
                accent = shiftedTone(scheme.primary, shift, 84.0),
                glyph = shiftedTone(scheme.primary, shift, if (dark) 38.0 else 46.0)
            )
        }
    }
}

@Composable
private fun fixedSp(value: Float): TextUnit = (value / LocalDensity.current.fontScale).sp

@Composable
private fun Top100Numeral(palette: Top100Palette, size: TextUnit) {
    Text(
        "100",
        color = palette.ink,
        fontSize = size,
        lineHeight = size,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.04f * size.value).sp,
        style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
        maxLines = 1
    )
}

@Composable
private fun Top100Glyph(glyph: Shape, rotation: Float, color: Color, size: Dp, modifier: Modifier) {
    Box(modifier.size(size).rotate(rotation).clip(glyph).background(color))
}

@Composable
private fun Top100Cover(
    label: String,
    date: String,
    palette: Top100Palette,
    glyph: Shape,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    Box(modifier.clip(RoundedCornerShape(Top100CoverCorner)).background(palette.canvas)) {
        Top100Glyph(glyph, rotation, palette.glyph, 132.dp, Modifier.align(Alignment.TopEnd).offset(x = 44.dp, y = (-40).dp))
        Text(
            date,
            color = palette.accent,
            fontSize = fixedSp(11f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 14.dp)
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            Top100Numeral(palette, fixedSp(60f))
            Text(
                label,
                color = palette.ink,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = if (largeText) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

@Composable
private fun Top100ChartScreen(
    open: DailyTop100ChartUiState,
    label: String,
    date: String,
    copy: DailyTop100Copy,
    palette: Top100Palette,
    glyph: Shape,
    rotation: Float,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    BackHandler(onBack = onBack)
    val strings = LocalLevyraStrings.current
    val scheme = MaterialTheme.colorScheme
    val view = LocalView.current
    val lightStatus = palette.canvas.luminance() > 0.5f
    val lightNavigation = scheme.background.luminance() > 0.5f
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightStatus
            isAppearanceLightNavigationBars = lightNavigation
        }
    }
    val tracks = open.tracks
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize().background(scheme.background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottom + 24.dp)) {
            item(key = "top100-hero", contentType = "top100-hero") {
                Top100Hero(label, date, copy.title, palette, glyph, rotation, tracks.isNotEmpty(), onBack,
                    onPlayAll = { onPlay(tracks, tracks.first()) },
                    onShuffle = {
                        val shuffled = tracks.shuffled()
                        onPlay(shuffled, shuffled.first())
                    }
                )
            }
            when {
                tracks.isNotEmpty() -> itemsIndexed(tracks, key = { _, track -> track.id }, contentType = { _, _ -> "top100-row" }) { index, track ->
                    Top100Row(index + 1, track, strings.options, onClick = { onPlay(tracks, track) }, onActions = { onTrackActions(track) })
                }
                open.loading -> item(key = "top100-loading", contentType = "top100-status") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        DiscoveryLoadingIndicator(Modifier.size(40.dp))
                    }
                }
                else -> item(key = "top100-failed", contentType = "top100-status") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(copy.failed, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text(copy.retry) }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(palette.canvas))
    }
}

@Composable
private fun Top100Hero(
    label: String,
    date: String,
    overline: String,
    palette: Top100Palette,
    glyph: Shape,
    rotation: Float,
    canPlay: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(palette.canvas)
    ) {
        Top100Glyph(glyph, rotation, palette.glyph, 260.dp, Modifier.align(Alignment.TopEnd).offset(x = 72.dp, y = 8.dp))
        Column(Modifier.statusBarsPadding().padding(start = 8.dp, end = 20.dp, bottom = 20.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = palette.ink)
            }
            Column(Modifier.padding(start = 12.dp, top = 12.dp)) {
                Text(
                    overline.uppercase(Locale.forLanguageTag(strings.code)),
                    color = palette.accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Top100Numeral(palette, fixedSp(128f))
                Text(
                    label,
                    color = palette.ink,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    date,
                    color = palette.accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.size(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onPlayAll,
                        enabled = canPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.ink, contentColor = palette.canvas),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.playAll, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalButton(
                        onClick = onShuffle,
                        enabled = canPlay,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = palette.ink.copy(alpha = 0.16f), contentColor = palette.ink),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.shuffle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun Top100Row(rank: Int, track: Track, optionsLabel: String, onClick: () -> Unit, onActions: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Row, onLongClick = onActions, onLongClickLabel = optionsLabel)
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            rank.toString(),
            color = if (rank <= 3) scheme.primary else scheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.width(44.dp)
        )
        StableRemoteArtwork(
            url = track.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(scheme.surfaceContainerHigh)
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(track.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onActions) {
            Icon(Icons.Rounded.MoreVert, contentDescription = optionsLabel, tint = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SpotifyTop100Card(label: String, title: String, subtitle: String, slot: Int, global: Boolean, onOpen: () -> Unit) {
    val colors = SpotifyTop100Gradients[(if (global) 0 else slot + 1) % SpotifyTop100Gradients.size]
    val badge = title.substringBefore('·').substringBefore(" - ").trim()
    Column(
        Modifier.width(SpotifyTop100Size)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $label" }
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)
    ) {
        Box(
            Modifier.size(SpotifyTop100Size).clip(RoundedCornerShape(4.dp))
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(colors.map(::Color)))
        ) {
            Box(Modifier.padding(8.dp).size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.White.copy(alpha = 0.7f)))
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    badge, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 10.dp)
                )
                Box(Modifier.padding(vertical = 10.dp).width(84.dp).size(width = 84.dp, height = 1.dp).background(Color.White.copy(alpha = 0.45f)))
                Text(
                    label.uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }
        Text(
            "$badge $label", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)
        )
        Text("$label: $subtitle", color = com.luc4n3x.levyra.ui.theme.SpotifyDesign.subdued, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private val SpotifyTop100Size = 150.dp
private val SpotifyTop100Gradients = listOf(
    listOf(0xFF0E7C7B, 0xFF1A2F5C),
    listOf(0xFF1E5A8A, 0xFF1C2A55),
    listOf(0xFF8C1932, 0xFF3A0C1A),
    listOf(0xFF148A08, 0xFF0B3D10),
    listOf(0xFF7358FF, 0xFF2A1F66),
    listOf(0xFFBA5D07, 0xFF4A2504)
)
