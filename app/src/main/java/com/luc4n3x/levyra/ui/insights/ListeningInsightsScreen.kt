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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.LevyraArtworkCache
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
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
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
            if (!state.snapshot.hasSignal) {
                insightsStateItems(state, strings, accent, onRetry = viewModel::refresh)
            } else {
                insightsSignalContent(
                    snapshot = state.snapshot,
                    strings = strings,
                    locale = locale,
                    accent = accent,
                    animationsEnabled = animationsEnabled,
                    onPlayTrack = onPlayTrack,
                    onOpenArtist = onOpenArtist
                )
                insightsHistoryItems(
                    state = state,
                    historyGroups = historyGroups,
                    strings = strings,
                    locale = locale,
                    accent = accent,
                    animationsEnabled = animationsEnabled,
                    onOpenSearch = { viewModel.setSearchVisible(true) },
                    onCloseSearch = {
                        keyboard?.hide()
                        viewModel.setSearchVisible(false)
                    },
                    onQueryChange = viewModel::setQuery,
                    onPlayTrack = onPlayTrack,
                    onLoadMore = viewModel::loadMoreHistory
                )
            }
        }
    }
}

private fun LazyListScope.insightsStateItems(
    state: ListeningInsightsUiState,
    strings: LevyraStrings,
    accent: Color,
    onRetry: () -> Unit
) {
    if (state.failed && !state.snapshot.hasSignal) {
        item(key = "insights-error", contentType = "state") {
            InsightsMessage(strings.insightsError, onRetry = onRetry)
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
    }
}

private fun LazyListScope.insightsSignalContent(
    snapshot: ListeningInsightsSnapshot,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color,
    animationsEnabled: Boolean,
    onPlayTrack: (ListeningInsightsTrack) -> Unit,
    onOpenArtist: (ListeningInsightsArtist) -> Unit
) {
    item(key = "insights-hero", contentType = "hero") {
        AnimatedContent(
            targetState = snapshot,
            transitionSpec = {
                if (animationsEnabled) {
                    fadeIn(tween(260)) togetherWith fadeOut(tween(160))
                } else {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                }
            },
            label = "insights-period-content"
        ) { currentSnapshot ->
            InsightsHero(currentSnapshot, strings, locale, accent)
        }
    }
    item(key = "insights-metrics", contentType = "metrics") {
        InsightsMetricRibbon(snapshot, strings, locale, accent)
    }
    item(key = "insights-activity-title", contentType = "section-title") {
        InsightsSectionTitle(strings.insightsActivity, Icons.Rounded.BarChart, accent)
    }
    item(key = "insights-activity", contentType = "chart") {
        InsightsActivityChart(snapshot, strings, locale, accent)
    }
    item(key = "insights-rhythm-title", contentType = "section-title") {
        InsightsSectionTitle(strings.insightsRhythm, Icons.Rounded.Equalizer, LevyraViolet)
    }
    item(key = "insights-rhythm", contentType = "rhythm") {
        InsightsRhythm(snapshot, strings, locale, accent)
    }
    if (snapshot.topArtists.isNotEmpty()) {
        item(key = "insights-artists-title", contentType = "section-title") {
            InsightsSectionTitle(strings.topArtistsTitle, Icons.Rounded.AutoAwesome, LevyraPink)
        }
        item(key = "insights-artists", contentType = "artists") {
            TopArtistsRail(snapshot.topArtists, strings, locale, accent, onOpenArtist)
        }
    }
    item(key = "insights-discovery", contentType = "discovery") {
        DiscoveryStory(snapshot, strings, locale, accent)
    }
    if (snapshot.topTracks.isNotEmpty()) {
        item(key = "insights-tracks-title", contentType = "section-title") {
            InsightsSectionTitle(strings.topTracksTitle, Icons.Rounded.ArrowUpward, accent)
        }
        itemsIndexed(
            items = snapshot.topTracks,
            key = { _, track -> "top-${track.trackId.ifBlank { "${track.title}|${track.artist}" }}" },
            contentType = { _, _ -> "top-track" }
        ) { index, track ->
            TopTrackRow(index, track, strings, locale, accent, onPlayTrack)
        }
    }
}

private fun LazyListScope.insightsHistoryItems(
    state: ListeningInsightsUiState,
    historyGroups: List<HistoryGroup>,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color,
    animationsEnabled: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onPlayTrack: (ListeningInsightsTrack) -> Unit,
    onLoadMore: () -> Unit
) {
    item(key = "insights-history-title", contentType = "section-title") {
        HistoryHeading(
            strings = strings,
            searchVisible = state.searchVisible,
            onSearch = onOpenSearch
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
                onValueChange = onQueryChange,
                onClose = onCloseSearch
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
            LaunchedEffect(state.history.size) { onLoadMore() }
            Text(
                text = strings.insightsLoadMore,
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLoadMore() }
                    .padding(22.dp)
            )
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
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = LevyraText)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                strings.listeningInsights,
                color = LevyraText,
                fontSize = 23.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(23.sp),
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
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LevyraPanel.copy(alpha = 0.68f))
            .border(BorderStroke(1.dp, LevyraGlassBorder), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        InsightsPeriods.forEachIndexed { index, period ->
            val active = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) accent.copy(alpha = 0.20f) else Color.Transparent)
                    .then(
                        if (active) Modifier.border(BorderStroke(1.dp, accent.copy(alpha = 0.38f)), RoundedCornerShape(12.dp))
                        else Modifier
                    )
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
                    fontSize = 11.5.sp,
                    fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1
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
    val context = LocalContext.current
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val artwork = snapshot.topTracks.map { it.artworkUrl }.filter(String::isNotBlank).distinct().take(3)
    val fontScale = LocalDensity.current.fontScale
    val heroHeight = (244f + ((fontScale - 1f).coerceAtLeast(0f) * 72f)).dp
    val isAllTime = snapshot.period == ListeningInsightsPeriod.AllTime

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .height(heroHeight)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(accent.copy(alpha = 0.38f), LevyraViolet.copy(alpha = 0.16f), LevyraPanel)
                )
            )
            .border(1.dp, accent.copy(alpha = 0.24f), RoundedCornerShape(28.dp))
    ) {
        artwork.forEachIndexed { index, url ->
            val heroArtworkRequest = remember(context, url) {
                ImageRequest.Builder(context)
                    .data(LevyraArtworkCache.large(url))
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = heroArtworkRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (24 - index * 32).dp, y = (index * 12 - 12).dp)
                    .rotate((index - 1) * 6f)
                    .size((148 - index * 14).dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to LevyraPanel.copy(alpha = 0.98f),
                    0.56f to LevyraPanel.copy(alpha = 0.72f),
                    1f to Color.Transparent
                )
            )
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    periodLongLabel(snapshot.period, strings).uppercase(locale),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.3.sp
                )
                if (isAllTime) {
                    Surface(
                        color = accent.copy(alpha = 0.14f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
                    ) {
                        Text(
                            text = strings.insightsLifetime,
                            color = accent,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    formatHeroDuration(snapshot.metrics.listenedMs, number, strings),
                    color = LevyraText,
                    fontSize = 44.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(44.sp),
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    strings.insightsListened,
                    color = LevyraMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
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
    Column(modifier = modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(label, color = LevyraMuted, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RibbonDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(LevyraGlassBorder))
}

@Composable
private fun InsightsSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 32.dp, bottom = 12.dp).semantics { heading() },
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
    val points = snapshot.activity
    val animations = LocalAnimationsEnabled.current
    var target by remember(snapshot.period) { mutableFloatStateOf(0f) }
    LaunchedEffect(snapshot.period) { target = 1f }
    val reveal by animateFloatAsState(
        targetValue = target,
        animationSpec = if (animations) tween(600, easing = FastOutSlowInEasing) else snap(),
        label = "insights-activity-reveal"
    )
    val maxValue = remember(points) { points.maxOfOrNull { it.listenedMs }?.coerceAtLeast(1L) ?: 1L }
    val isDay = snapshot.period == ListeningInsightsPeriod.Day

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(LevyraPanel.copy(alpha = 0.58f))
            .border(BorderStroke(1.dp, LevyraGlassBorder), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActivityChartHeader(snapshot, strings, locale, accent)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
                .semantics {
                    contentDescription = strings.insightsActivity
                }
        ) {
            if (points.isEmpty()) return@Canvas
            if (isDay) {
                drawHourlyBars(points, maxValue, reveal, accent)
            } else {
                drawTrendLine(points, maxValue, reveal, accent)
            }
        }
        ActivityLabels(snapshot.period, points, locale)
    }
}

@Composable
private fun ActivityChartHeader(
    snapshot: ListeningInsightsSnapshot,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color
) {
    val isAllTime = snapshot.period == ListeningInsightsPeriod.AllTime
    val isDay = snapshot.period == ListeningInsightsPeriod.Day

    if (isAllTime && snapshot.detailedFromMs > 0L) {
        val detailedFrom = remember(snapshot.detailedFromMs, locale) {
            Instant.ofEpochMilli(snapshot.detailedFromMs).atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                strings.insightsDetailAvailable.format(detailedFrom),
                color = LevyraMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                color = LevyraViolet.copy(alpha = 0.12f),
                shape = CircleShape,
                border = BorderStroke(1.dp, LevyraViolet.copy(alpha = 0.25f))
            ) {
                Text(
                    text = strings.insightsDetailedTimeline,
                    color = LevyraViolet,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    } else if (snapshot.metrics.peakDayEpochMs > 0L) {
        val peakDate = remember(snapshot.metrics.peakDayEpochMs, locale, isDay) {
            if (isDay) {
                val time = Instant.ofEpochMilli(snapshot.metrics.peakDayEpochMs).atZone(ZoneId.systemDefault()).toLocalTime()
                time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
            } else {
                Instant.ofEpochMilli(snapshot.metrics.peakDayEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(strings.pulseProPeak, color = LevyraMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(peakDate, color = accent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun DrawScope.drawHourlyBars(
    points: List<ListeningInsightsActivityPoint>,
    maxValue: Long,
    reveal: Float,
    accent: Color
) {
    val slot = size.width / points.size
    val barWidth = (slot * 0.48f).coerceAtLeast(3f)
    points.forEachIndexed { index, point ->
        val fraction = (point.listenedMs.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
        val hasActivity = fraction > 0f
        val minHeight = 3.dp.toPx()
        val barHeight = if (hasActivity) {
            max(barWidth * 1.1f, size.height * fraction) * reveal
        } else {
            minHeight * reveal
        }
        val barX = index * slot + (slot - barWidth) / 2f
        val barY = size.height - barHeight
        if (hasActivity) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(accent, LevyraViolet.copy(alpha = 0.55f)),
                    startY = barY,
                    endY = size.height
                ),
                topLeft = Offset(barX, barY),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        } else {
            drawRoundRect(
                color = LevyraViolet.copy(alpha = 0.16f),
                topLeft = Offset(barX, barY),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

private fun DrawScope.drawTrendLine(
    points: List<ListeningInsightsActivityPoint>,
    maxValue: Long,
    reveal: Float,
    accent: Color
) {
    val step = if (points.size <= 1) size.width else size.width / (points.size - 1)
    val line = Path()
    val area = Path()
    points.forEachIndexed { index, point ->
        val x = index * step
        val fraction = point.listenedMs.toFloat() / maxValue.toFloat()
        val y = size.height - fraction * size.height * 0.86f * reveal
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

@Composable
private fun ActivityLabels(period: ListeningInsightsPeriod, points: List<ListeningInsightsActivityPoint>, locale: Locale) {
    if (points.isEmpty()) return
    val positions = listOf(0, points.lastIndex / 4, points.lastIndex / 2, points.lastIndex * 3 / 4, points.lastIndex).distinct()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        positions.forEach { index ->
            val label = if (period == ListeningInsightsPeriod.Day) {
                val hour = Instant.ofEpochMilli(points[index].epochMs).atZone(ZoneId.systemDefault()).hour
                "${hour.toString().padStart(2, '0')}:00"
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
            .border(BorderStroke(1.dp, LevyraViolet.copy(alpha = 0.18f)), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(
            strings.insightsActiveAround.format(time),
            color = LevyraText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        RhythmCanvas(snapshot.hourBuckets, peak, accent, snapshot.period)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 6, 12, 18, 23).forEach { hour ->
                Text(hour.toString().padStart(2, '0'), color = if (hour == peak) accent else LevyraMuted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun RhythmCanvas(values: List<Long>, peakHour: Int, accent: Color, period: ListeningInsightsPeriod) {
    val animations = LocalAnimationsEnabled.current
    var target by remember(period) { mutableFloatStateOf(0f) }
    LaunchedEffect(period) { target = 1f }
    val reveal by animateFloatAsState(
        targetValue = target,
        animationSpec = if (animations) tween(600, easing = FastOutSlowInEasing) else snap(),
        label = "insights-rhythm-reveal"
    )
    val maxVal = remember(values) { values.maxOrNull()?.coerceAtLeast(1L) ?: 1L }
    Canvas(modifier = Modifier.fillMaxWidth().height(68.dp)) {
        if (values.isEmpty()) return@Canvas
        val slot = size.width / values.size
        val barWidth = slot * 0.44f
        values.forEachIndexed { hour, value ->
            val fraction = value.toFloat() / maxVal.toFloat()
            val height = max(3f, size.height * fraction) * reveal
            val isPeak = hour == peakHour
            drawRoundRect(
                color = if (isPeak) accent else LevyraViolet.copy(alpha = if (fraction > 0f) 0.52f else 0.16f),
                topLeft = Offset(hour * slot + (slot - barWidth) / 2f, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

private fun artistInitials(name: String): String {
    val clean = name.trim()
    if (clean.isBlank()) return "♪"
    val words = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase(Locale.ROOT)
        clean.length >= 2 -> clean.take(2).uppercase(Locale.ROOT)
        else -> clean.uppercase(Locale.ROOT)
    }
}

@Composable
private fun ArtistAvatar(
    artist: ListeningInsightsArtist,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = CircleShape
    val imageRequest = remember(context, artist.artworkUrl) {
        if (artist.artworkUrl.isNotBlank()) {
            ImageRequest.Builder(context)
                .data(LevyraArtworkCache.large(artist.artworkUrl))
                .crossfade(true)
                .build()
        } else null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(LevyraPanelSoft)
            .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.12f)), shape),
        contentAlignment = Alignment.Center
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val palette = remember(artist.name) {
                val hash = kotlin.math.abs(artist.name.hashCode())
                val gradientPairs = listOf(
                    listOf(LevyraViolet, LevyraCyan),
                    listOf(LevyraPink, LevyraViolet),
                    listOf(LevyraCyan, LevyraPink),
                    listOf(Color(0xFF7928CA), Color(0xFFFF0080)),
                    listOf(Color(0xFF0070F3), Color(0xFF00DFD8)),
                    listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))
                )
                gradientPairs[hash % gradientPairs.size]
            }
            val initials = remember(artist.name) { artistInitials(artist.name) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(palette.map { it.copy(alpha = 0.45f) })),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = (size.value * 0.32f).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
            }
        }
    }
}

@Composable
private fun TopArtistsRail(
    artists: List<ListeningInsightsArtist>,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color,
    onOpenArtist: (ListeningInsightsArtist) -> Unit
) {
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(artists, key = { _, artist -> "artist-${artist.name.lowercase(Locale.ROOT)}" }) { index, artist ->
            Column(
                modifier = Modifier
                    .width(if (index == 0) 124.dp else 108.dp)
                    .levyraPressable(
                        onClick = { onOpenArtist(artist) },
                        pressedScale = LevyraPressScale.Tile,
                        role = Role.Button,
                        onClickLabel = artist.name
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    ArtistAvatar(
                        artist = artist,
                        size = if (index == 0) 120.dp else 104.dp
                    )
                    Surface(
                        color = if (index < 3) accent else LevyraPanel,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, LevyraGlassBorder),
                        modifier = Modifier.offset(x = 4.dp, y = 4.dp)
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (index < 3) LevyraOnAccent else LevyraText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    artist.name,
                    color = LevyraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${number.format(artist.plays)} ${strings.statPlays}",
                    color = LevyraMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(LevyraPink.copy(alpha = 0.18f), accent.copy(alpha = 0.11f), LevyraPanel)))
            .border(BorderStroke(1.dp, LevyraPink.copy(alpha = 0.2f)), RoundedCornerShape(28.dp))
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
    val context = LocalContext.current
    val imageRequest = remember(context, track.artworkUrl) {
        if (track.artworkUrl.isNotBlank()) {
            ImageRequest.Builder(context)
                .data(LevyraArtworkCache.small(track.artworkUrl))
                .crossfade(true)
                .build()
        } else null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .levyraPressable(
                onClick = { onClick(track) },
                pressedScale = LevyraPressScale.Row,
                role = Role.Button,
                onClickLabel = track.title
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            number.format(index + 1),
            color = if (index < 3) accent else LevyraMuted,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp)
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LevyraPanelSoft),
            contentAlignment = Alignment.Center
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Equalizer,
                    contentDescription = null,
                    tint = LevyraMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                track.title,
                color = LevyraText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                color = LevyraMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatCompactDuration(track.listenedMs, strings.recapUnitHours, strings.recapUnitMinutes),
                color = LevyraText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${number.format(track.plays)} ${strings.statPlays}",
                color = LevyraMuted,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun HistoryHeading(strings: LevyraStrings, searchVisible: Boolean, onSearch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 32.dp, bottom = 8.dp).semantics { heading() },
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LevyraPanel.copy(alpha = 0.78f))
            .border(BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.32f)), RoundedCornerShape(16.dp))
            .focusRequester(focus),
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
    val context = LocalContext.current
    val time = remember(item.startedAt, locale) {
        Instant.ofEpochMilli(item.startedAt).atZone(ZoneId.systemDefault()).toLocalTime()
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    }
    val imageRequest = remember(context, item.track.artworkUrl) {
        if (item.track.artworkUrl.isNotBlank()) {
            ImageRequest.Builder(context)
                .data(LevyraArtworkCache.small(item.track.artworkUrl))
                .crossfade(true)
                .build()
        } else null
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .levyraPressable(
                    onClick = onClick,
                    pressedScale = LevyraPressScale.Row,
                    role = Role.Button,
                    onClickLabel = item.track.title
                )
                .padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LevyraPanelSoft),
                contentAlignment = Alignment.Center
            ) {
                if (imageRequest != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = null,
                        tint = LevyraMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    item.track.title,
                    color = LevyraText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.track.artist,
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
        if (drawDivider) Box(Modifier.fillMaxWidth().padding(start = 58.dp).height(1.dp).background(LevyraGlassBorder))
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
        Text(
            message,
            color = LevyraText,
            fontSize = 19.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(19.sp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
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
