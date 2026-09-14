package com.luc4n3x.levyra.ui.insights

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.luc4n3x.levyra.domain.ListeningInsightsActivityPoint
import com.luc4n3x.levyra.domain.ListeningInsightsArtist
import com.luc4n3x.levyra.domain.ListeningInsightsHistoryItem
import com.luc4n3x.levyra.domain.ListeningInsightsPeriod
import com.luc4n3x.levyra.domain.ListeningInsightsSnapshot
import com.luc4n3x.levyra.domain.ListeningInsightsTrack
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.max

private val InsightsPeriods = ListeningInsightsPeriod.entries

@Composable
internal fun ListeningInsightsScreen(
    accent: Color,
    onPlayTrack: (ListeningInsightsTrack) -> Unit,
    onOpenArtist: (ListeningInsightsArtist) -> Unit,
    onOpenRecap: () -> Unit,
    onClose: () -> Unit,
    viewModel: ListeningInsightsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalLevyraStrings.current
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val animationsEnabled = LocalAnimationsEnabled.current
    val historyGroups = remember(state.history, locale) { groupHistory(state.history, locale, strings) }

    LaunchedEffect(viewModel) { viewModel.refresh() }

    BackHandler(enabled = state.searchVisible) {
        keyboard?.hide()
        viewModel.setSearchVisible(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LevyraInk, LevyraBlack)))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 144.dp)
        ) {
            item(key = "insights-header", contentType = "header") {
                InsightsHeader(
                    strings = strings,
                    onOpenRecap = onOpenRecap,
                    onClose = onClose
                )
            }
            item(key = "insights-periods", contentType = "periods") {
                InsightsPeriodSelector(
                    selected = state.period,
                    strings = strings,
                    accent = accent,
                    onSelect = viewModel::selectPeriod
                )
            }
            if (state.failed && !state.snapshot.hasSignal) {
                item(key = "insights-error", contentType = "state") {
                    InsightsMessage(strings.insightsError, onRetry = viewModel::refresh)
                }
            } else if (state.loading && !state.snapshot.hasSignal) {
                item(key = "insights-loading", contentType = "state") {
                    Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
                    }
                }
            } else if (!state.snapshot.hasSignal) {
                item(key = "insights-empty", contentType = "state") {
                    InsightsEmpty(strings.insightsEmpty, accent)
                }
            } else {
                item(key = "insights-hero", contentType = "hero") {
                    AnimatedContent(
                        targetState = state.snapshot,
                        transitionSpec = {
                            if (animationsEnabled) {
                                fadeIn(tween(280)) togetherWith fadeOut(tween(180))
                            } else {
                                fadeIn(snap()) togetherWith fadeOut(snap())
                            }
                        },
                        label = "insights-period-content"
                    ) { snapshot ->
                        InsightsHero(snapshot, strings, locale, accent)
                    }
                }
                item(key = "insights-metrics", contentType = "metrics") {
                    InsightsMetricRibbon(state.snapshot, strings, locale, accent)
                }
                item(key = "insights-activity-title", contentType = "section-title") {
                    InsightsSectionTitle(strings.insightsActivity, Icons.Rounded.BarChart, accent)
                }
                item(key = "insights-activity", contentType = "chart") {
                    InsightsActivityChart(state.snapshot, strings, locale, accent)
                }
                item(key = "insights-rhythm-title", contentType = "section-title") {
                    InsightsSectionTitle(strings.insightsRhythm, Icons.Rounded.Equalizer, LevyraViolet)
                }
                item(key = "insights-rhythm", contentType = "rhythm") {
                    InsightsRhythm(state.snapshot, strings, locale, accent)
                }
                if (state.snapshot.topArtists.isNotEmpty()) {
                    item(key = "insights-artists-title", contentType = "section-title") {
                        InsightsSectionTitle(strings.topArtistsTitle, Icons.Rounded.AutoAwesome, LevyraPink)
                    }
                    item(key = "insights-artists", contentType = "artists") {
                        TopArtistsRail(state.snapshot.topArtists, strings, locale, onOpenArtist)
                    }
                }
                item(key = "insights-discovery", contentType = "discovery") {
                    DiscoveryStory(state.snapshot, strings, locale, accent)
                }
                if (state.snapshot.topTracks.isNotEmpty()) {
                    item(key = "insights-tracks-title", contentType = "section-title") {
                        InsightsSectionTitle(strings.topTracksTitle, Icons.Rounded.ArrowUpward, accent)
                    }
                    itemsIndexed(
                        items = state.snapshot.topTracks,
                        key = { _, track -> "top-${track.trackId.ifBlank { "${track.title}|${track.artist}" }}" },
                        contentType = { _, _ -> "top-track" }
                    ) { index, track ->
                        TopTrackRow(index, track, strings, locale, accent, onPlayTrack)
                    }
                }
                item(key = "insights-history-title", contentType = "section-title") {
                    HistoryHeading(
                        strings = strings,
                        searchVisible = state.searchVisible,
                        onSearch = { viewModel.setSearchVisible(true) }
                    )
                }
                item(key = "insights-history-search", contentType = "search") {
                    AnimatedVisibility(
                        visible = state.searchVisible,
                        enter = fadeIn(if (animationsEnabled) tween() else snap()),
                        exit = fadeOut(if (animationsEnabled) tween() else snap())
                    ) {
                        HistorySearch(
                            value = state.query,
                            hint = strings.insightsSearchHistory,
                            closeLabel = strings.close,
                            onValueChange = viewModel::setQuery,
                            onClose = {
                                keyboard?.hide()
                                viewModel.setSearchVisible(false)
                            }
                        )
                    }
                }
                historyGroups.forEach { group ->
                    item(key = "history-group-${group.key}", contentType = "history-group") {
                        Text(
                            text = group.label,
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 8.dp)
                        )
                    }
                    itemsIndexed(
                        items = group.items,
                        key = { _, item -> "history-${item.id}" },
                        contentType = { _, _ -> "history-track" }
                    ) { index, item ->
                        HistoryTrackRow(
                            item = item,
                            locale = locale,
                            strings = strings,
                            accent = accent,
                            drawDivider = index < group.items.lastIndex,
                            onClick = { onPlayTrack(item.track) }
                        )
                    }
                }
                if (state.historyLoading) {
                    item(key = "history-loading", contentType = "history-loading") {
                        Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        }
                    }
                } else if (state.hasMoreHistory) {
                    item(key = "history-more", contentType = "history-more") {
                        LaunchedEffect(state.history.size) { viewModel.loadMoreHistory() }
                        Text(
                            text = strings.insightsLoadMore,
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsHeader(
    strings: LevyraStrings,
    onOpenRecap: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = LevyraText)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                strings.listeningInsights,
                color = LevyraText,
                fontSize = 24.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(24.sp),
                fontWeight = FontWeight.Black
            )
            Text(strings.listeningInsightsSubtitle, color = LevyraMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        }
        IconButton(onClick = onOpenRecap) {
            Icon(Icons.Rounded.History, contentDescription = strings.openRecap, tint = LevyraMuted)
        }
    }
}

@Composable
private fun InsightsPeriodSelector(
    selected: ListeningInsightsPeriod,
    strings: LevyraStrings,
    accent: Color,
    onSelect: (ListeningInsightsPeriod) -> Unit
) {
    val labels = remember(strings.code) {
        listOf(
            strings.insightsPeriod24h,
            strings.insightsPeriod7d,
            strings.insightsPeriod30d,
            strings.insightsPeriod6m,
            strings.insightsPeriodAll
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LevyraPanel.copy(alpha = 0.72f))
            .border(1.dp, LevyraGlassBorder, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        InsightsPeriods.forEachIndexed { index, period ->
            val active = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) accent.copy(alpha = 0.18f) else Color.Transparent)
                    .levyraPressable(
                        onClick = { onSelect(period) },
                        pressedScale = LevyraPressScale.Control,
                        role = Role.Tab,
                        onClickLabel = labels[index]
                    )
                    .semantics {
                        this.selected = active
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    labels[index],
                    color = if (active) accent else LevyraMuted,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

@Composable
private fun InsightsHero(
    snapshot: ListeningInsightsSnapshot,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color
) {
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val artwork = snapshot.topTracks.map { it.artworkUrl }.filter(String::isNotBlank).distinct().take(3)
    val fontScale = LocalDensity.current.fontScale
    val heroHeight = (250f + ((fontScale - 1f).coerceAtLeast(0f) * 76f)).dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .height(heroHeight)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(accent.copy(alpha = 0.42f), LevyraViolet.copy(alpha = 0.18f), LevyraPanel)
                )
            )
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(30.dp))
    ) {
        artwork.forEachIndexed { index, url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (26 - index * 34).dp, y = (index * 14 - 14).dp)
                    .rotate((index - 1) * 7f)
                    .size((154 - index * 14).dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to LevyraPanel.copy(alpha = 0.98f),
                    0.55f to LevyraPanel.copy(alpha = 0.68f),
                    1f to Color.Transparent
                )
            )
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 23.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                periodLongLabel(snapshot.period, strings),
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    formatHeroDuration(snapshot.metrics.listenedMs, number, strings),
                    color = LevyraText,
                    fontSize = 45.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(45.sp),
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.6).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    strings.insightsListened,
                    color = LevyraMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                snapshot.metrics.trendPercent?.let { trend ->
                    Surface(color = trendColor(trend).copy(alpha = 0.16f), shape = CircleShape) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (trend >= 0) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = trendColor(trend),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                (if (trend >= 0) "+" else "") + number.format(trend) + "%",
                                color = trendColor(trend),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Text(strings.insightsPrevious, color = LevyraMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun InsightsMetricRibbon(
    snapshot: ListeningInsightsSnapshot,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color
) {
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RibbonMetric(number.format(snapshot.metrics.countedPlays), strings.statPlays, accent, Modifier.weight(1f))
        RibbonDivider()
        RibbonMetric(number.format(snapshot.metrics.distinctTracks), strings.songs, LevyraViolet, Modifier.weight(1f))
        RibbonDivider()
        RibbonMetric(number.format(snapshot.metrics.distinctArtists), strings.artists, LevyraPink, Modifier.weight(1f))
        RibbonDivider()
        RibbonMetric("${number.format(snapshot.metrics.completionRate)}%", strings.pulseCompletion, accent, Modifier.weight(1f))
    }
}

@Composable
private fun RibbonMetric(value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier = modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(label, color = LevyraMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RibbonDivider() {
    Box(Modifier.width(1.dp).height(30.dp).background(LevyraGlassBorder))
}

@Composable
private fun InsightsSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 34.dp, bottom = 13.dp).semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        Text(title, color = LevyraText, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun InsightsActivityChart(
    snapshot: ListeningInsightsSnapshot,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color
) {
    val points = if (snapshot.period == ListeningInsightsPeriod.Day) {
        snapshot.hourBuckets.mapIndexed { hour, value -> ListeningInsightsActivityPoint(hour.toLong(), value, 0) }
    } else {
        snapshot.activity
    }
    val animations = LocalAnimationsEnabled.current
    var target by remember(snapshot.period, points) { mutableFloatStateOf(0f) }
    LaunchedEffect(snapshot.period, points) { target = 1f }
    val reveal by animateFloatAsState(
        targetValue = target,
        animationSpec = if (animations) tween(650, easing = FastOutSlowInEasing) else snap(),
        label = "insights-activity-reveal"
    )
    val maxValue = remember(points) { points.maxOfOrNull { it.listenedMs }?.coerceAtLeast(1L) ?: 1L }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(24.dp))
            .background(LevyraPanel.copy(alpha = 0.58f)).border(1.dp, LevyraGlassBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (snapshot.metrics.peakDayEpochMs > 0L) {
            val peakDate = remember(snapshot.metrics.peakDayEpochMs, locale) {
                Instant.ofEpochMilli(snapshot.metrics.peakDayEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings.pulseProPeak, color = LevyraMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(peakDate, color = accent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }
        Canvas(
            modifier = Modifier.fillMaxWidth().height(142.dp).semantics {
                contentDescription = strings.insightsActivity
            }
        ) {
            if (points.isEmpty()) return@Canvas
            if (snapshot.period == ListeningInsightsPeriod.Day) {
                val slot = size.width / points.size
                val barWidth = slot * 0.48f
                points.forEachIndexed { index, point ->
                    val fraction = point.listenedMs.toFloat() / maxValue.toFloat()
                    val height = max(barWidth, size.height * fraction) * reveal
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(accent, LevyraViolet.copy(alpha = 0.42f))),
                        topLeft = Offset(index * slot + (slot - barWidth) / 2f, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 2f)
                    )
                }
            } else {
                val step = if (points.size <= 1) size.width else size.width / (points.size - 1)
                val line = Path()
                val area = Path()
                points.forEachIndexed { index, point ->
                    val x = index * step
                    val y = size.height - (point.listenedMs.toFloat() / maxValue.toFloat()) * size.height * 0.86f * reveal
                    if (index == 0) {
                        line.moveTo(x, y)
                        area.moveTo(x, size.height)
                        area.lineTo(x, y)
                    } else {
                        line.lineTo(x, y)
                        area.lineTo(x, y)
                    }
                }
                area.lineTo(size.width, size.height)
                area.close()
                drawPath(area, Brush.verticalGradient(listOf(accent.copy(alpha = 0.28f), Color.Transparent)))
                drawPath(line, color = accent, style = Stroke(width = 3.2f, cap = StrokeCap.Round))
            }
        }
        ActivityLabels(snapshot.period, points, locale)
        if (snapshot.period == ListeningInsightsPeriod.AllTime && snapshot.detailedFromMs > 0L) {
            val detailedFrom = remember(snapshot.detailedFromMs, locale) {
                Instant.ofEpochMilli(snapshot.detailedFromMs).atZone(ZoneId.systemDefault()).toLocalDate()
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
            }
            Text(
                strings.insightsDetailAvailable.format(detailedFrom),
                color = LevyraMuted,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActivityLabels(period: ListeningInsightsPeriod, points: List<ListeningInsightsActivityPoint>, locale: Locale) {
    if (points.isEmpty()) return
    val positions = listOf(0, points.lastIndex / 3, points.lastIndex * 2 / 3, points.lastIndex).distinct()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        positions.forEach { index ->
            val label = if (period == ListeningInsightsPeriod.Day) {
                index.toString().padStart(2, '0')
            } else {
                val date = Instant.ofEpochMilli(points[index].epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
                date.format(DateTimeFormatter.ofPattern(if (period == ListeningInsightsPeriod.AllTime) "MMM yy" else "d MMM", locale))
            }
            Text(label, color = LevyraMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun InsightsRhythm(snapshot: ListeningInsightsSnapshot, strings: LevyraStrings, locale: Locale, accent: Color) {
    val peak = snapshot.metrics.peakHour
    val time = if (peak in 0..23) {
        LocalTime.of(peak, 0).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    } else {
        "--"
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(LevyraViolet.copy(alpha = 0.13f), LevyraPanel.copy(alpha = 0.72f))))
            .border(1.dp, LevyraViolet.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(
            strings.insightsActiveAround.format(time),
            color = LevyraText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        RhythmCanvas(snapshot.hourBuckets, peak, accent)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 6, 12, 18, 23).forEach { hour ->
                Text(hour.toString().padStart(2, '0'), color = if (hour == peak) accent else LevyraMuted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun RhythmCanvas(values: List<Long>, peakHour: Int, accent: Color) {
    val animations = LocalAnimationsEnabled.current
    var target by remember(values) { mutableFloatStateOf(0f) }
    LaunchedEffect(values) { target = 1f }
    val reveal by animateFloatAsState(target, if (animations) tween(520) else snap(), label = "insights-rhythm-reveal")
    val peak = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Canvas(Modifier.fillMaxWidth().height(72.dp)) {
        val slot = size.width / 24f
        val bar = slot * 0.42f
        repeat(24) { hour ->
            val fraction = ((values.getOrNull(hour) ?: 0L).toFloat() / peak.toFloat()).coerceAtLeast(0.08f)
            val height = size.height * fraction * reveal
            drawRoundRect(
                color = if (hour == peakHour) accent else accent.copy(alpha = 0.25f + fraction * 0.28f),
                topLeft = Offset(hour * slot + (slot - bar) / 2f, (size.height - height) / 2f),
                size = Size(bar, height),
                cornerRadius = CornerRadius(bar / 2f)
            )
        }
    }
}

@Composable
private fun TopArtistsRail(
    artists: List<ListeningInsightsArtist>,
    strings: LevyraStrings,
    locale: Locale,
    onOpenArtist: (ListeningInsightsArtist) -> Unit
) {
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(artists, key = { _, artist -> "artist-${artist.name.lowercase(Locale.ROOT)}" }) { index, artist ->
            Column(
                modifier = Modifier.width(if (index == 0) 174.dp else 146.dp)
                    .levyraPressable(
                        onClick = { onOpenArtist(artist) },
                        pressedScale = LevyraPressScale.Tile,
                        role = Role.Button,
                        onClickLabel = artist.name
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(if (index == 0) 28.dp else 24.dp))
                        .background(LevyraPanelSoft)
                ) {
                    AsyncImage(
                        model = artist.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)))))
                    Text(
                        text = number.format(index + 1),
                        color = Color.White,
                        fontSize = if (index == 0) 34.sp else 27.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.BottomStart).padding(13.dp)
                    )
                }
                Text(artist.name, color = LevyraText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${number.format(artist.plays)} ${strings.statPlays}", color = LevyraMuted, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DiscoveryStory(snapshot: ListeningInsightsSnapshot, strings: LevyraStrings, locale: Locale, accent: Color) {
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val count = snapshot.metrics.discoveryCount
    val discoveryDetail = strings.insightsNewTracks.format(number.format(count)) +
        if (snapshot.period == ListeningInsightsPeriod.AllTime) " - ${strings.recapActivityLast30Days}" else ""
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 34.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(LevyraPink.copy(alpha = 0.18f), accent.copy(alpha = 0.11f), LevyraPanel)))
            .border(1.dp, LevyraPink.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
            .padding(horizontal = 21.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(strings.insightsDiscovery.uppercase(locale), color = LevyraPink, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${number.format(snapshot.metrics.discoveryRate)}%", color = LevyraText, fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            Text(strings.dnaDiscovery, color = LevyraMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 7.dp))
        }
        Text(discoveryDetail, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TopTrackRow(
    index: Int,
    track: ListeningInsightsTrack,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color,
    onClick: (ListeningInsightsTrack) -> Unit
) {
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp).clip(RoundedCornerShape(17.dp))
            .levyraPressable(
                onClick = { onClick(track) },
                pressedScale = LevyraPressScale.Row,
                role = Role.Button,
                onClickLabel = track.title
            ).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            number.format(index + 1),
            color = if (index < 3) accent else LevyraMuted,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(32.dp)
        )
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(13.dp)).background(LevyraPanelSoft)
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, color = LevyraText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = LevyraMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatCompactDuration(track.listenedMs, strings.recapUnitHours, strings.recapUnitMinutes),
                color = LevyraText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text("${number.format(track.plays)} ${strings.statPlays}", color = LevyraMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun HistoryHeading(strings: LevyraStrings, searchVisible: Boolean, onSearch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 34.dp, bottom = 8.dp).semantics { heading() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(strings.insightsHistory, color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        if (!searchVisible) {
            IconButton(onClick = onSearch) {
                Icon(Icons.Rounded.Search, contentDescription = strings.insightsSearchHistory, tint = LevyraMuted)
            }
        }
    }
}

@Composable
private fun HistorySearch(
    value: String,
    hint: String,
    closeLabel: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Medium),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).height(50.dp)
            .clip(RoundedCornerShape(16.dp)).background(LevyraPanel.copy(alpha = 0.78f))
            .border(1.dp, LevyraCyan.copy(alpha = 0.32f), RoundedCornerShape(16.dp)).focusRequester(focus),
        decorationBox = { inner ->
            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    if (value.isEmpty()) Text(hint, color = LevyraMuted, fontSize = 13.5.sp)
                    inner()
                }
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = closeLabel, tint = LevyraMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    )
}

@Composable
private fun HistoryTrackRow(
    item: ListeningInsightsHistoryItem,
    locale: Locale,
    strings: LevyraStrings,
    accent: Color,
    drawDivider: Boolean,
    onClick: () -> Unit
) {
    val time = remember(item.startedAt, locale) {
        Instant.ofEpochMilli(item.startedAt).atZone(ZoneId.systemDefault()).toLocalTime()
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).levyraPressable(
                onClick = onClick,
                pressedScale = LevyraPressScale.Row,
                role = Role.Button,
                onClickLabel = item.track.title
            ).padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.track.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(LevyraPanelSoft)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.track.title, color = LevyraText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.track.artist, color = LevyraMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(time, color = LevyraMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text(
                    formatCompactDuration(item.listenedMs, strings.recapUnitHours, strings.recapUnitMinutes),
                    color = if (item.completed) accent else LevyraMuted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (drawDivider) Box(Modifier.fillMaxWidth().padding(start = 60.dp).height(1.dp).background(LevyraGlassBorder))
    }
}

@Composable
private fun InsightsMessage(message: String, onRetry: () -> Unit) {
    Text(
        text = message,
        color = LevyraMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(48.dp).levyraPressable(onClick = onRetry, role = Role.Button)
    )
}

@Composable
private fun InsightsEmpty(message: String, accent: Color) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(Modifier.size(86.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = accent, modifier = Modifier.size(34.dp))
        }
        Text(message, color = LevyraText, fontSize = 19.sp, lineHeight = LevyraTypeRhythm.lineHeight(19.sp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

private data class HistoryGroup(val key: String, val label: String, val items: List<ListeningInsightsHistoryItem>)

private fun groupHistory(items: List<ListeningInsightsHistoryItem>, locale: Locale, strings: LevyraStrings): List<HistoryGroup> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val byDay = items.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
    val longDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val weekday = DateTimeFormatter.ofPattern("EEEE", locale)
    return byDay.map { (date, entries) ->
        val label = when (date) {
            today -> strings.insightsToday
            today.minusDays(1) -> strings.insightsYesterday
            else -> if (date >= today.minusDays(6)) date.format(weekday).replaceFirstChar { it.titlecase(locale) } else date.format(longDate)
        }
        HistoryGroup(date.toString(), label, entries)
    }
}

private fun periodLongLabel(period: ListeningInsightsPeriod, strings: LevyraStrings): String = when (period) {
    ListeningInsightsPeriod.Day -> strings.insightsPeriod24h
    ListeningInsightsPeriod.Week -> strings.insightsPeriod7d
    ListeningInsightsPeriod.Month -> strings.insightsPeriod30d
    ListeningInsightsPeriod.HalfYear -> strings.insightsPeriod6m
    ListeningInsightsPeriod.AllTime -> strings.insightsPeriodAll
}

private fun formatHeroDuration(value: Long, number: NumberFormat, strings: LevyraStrings): String {
    val totalMinutes = value.coerceAtLeast(0L) / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) {
        "${number.format(hours)}${strings.recapUnitHours} ${number.format(minutes)}${strings.recapUnitMinutes}"
    } else {
        "${number.format(minutes)}${strings.recapUnitMinutes}"
    }
}

private fun formatCompactDuration(value: Long, hourUnit: String, minuteUnit: String): String {
    val minutes = value.coerceAtLeast(0L) / 60_000L
    return if (minutes >= 60L) "${minutes / 60L}$hourUnit ${minutes % 60L}$minuteUnit" else "${minutes}$minuteUnit"
}

private fun trendColor(trend: Int): Color = if (trend >= 0) LevyraCyan else Color(0xFFFF6B7A)
