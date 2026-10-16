package com.luc4n3x.levyra.ui.insights

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.domain.ListeningInsightsArtist
import com.luc4n3x.levyra.domain.ListeningInsightsRanges
import com.luc4n3x.levyra.domain.ListeningInsightsSnapshot
import com.luc4n3x.levyra.domain.ListeningInsightsTrack
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import java.text.NumberFormat
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

private const val WRAPPED_PAGE_MS = 6_500
private val WrappedInk = Color.White
private val WrappedMuted = Color.White.copy(alpha = 0.72f)
private val WrappedBackground = Color(0xFF050608)
private val WrappedOnAccent = Color(0xFF07080C)

private enum class WrappedPage { Intro, Minutes, Artist, Tracks, Rhythm, Finale }

private fun wrappedPages(snapshot: ListeningInsightsSnapshot): List<WrappedPage> = buildList {
    add(WrappedPage.Intro)
    add(WrappedPage.Minutes)
    if (snapshot.topArtists.any { it.name.isNotBlank() }) add(WrappedPage.Artist)
    if (snapshot.topTracks.isNotEmpty()) add(WrappedPage.Tracks)
    if (snapshot.hourBuckets.any { it > 0L }) add(WrappedPage.Rhythm)
    add(WrappedPage.Finale)
}

@Composable
internal fun ListeningWrappedStory(
    state: ListeningWrappedState,
    accent: Color,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val animationsEnabled = LocalAnimationsEnabled.current
    val snapshot = state.snapshot?.takeIf { it.hasSignal }
    val pages = remember(snapshot) { snapshot?.let(::wrappedPages).orEmpty() }
    var pageIndex by rememberSaveable(state.year) { mutableIntStateOf(0) }
    val lastIndex = (pages.size - 1).coerceAtLeast(0)
    val page = pages.getOrNull(pageIndex.coerceIn(0, lastIndex))
    val progress = remember { Animatable(0f) }
    val covers = remember(snapshot) {
        snapshot?.topTracks?.map { it.artworkUrl }?.filter(String::isNotBlank)?.distinct().orEmpty()
    }

    BackHandler {
        if (pageIndex > 0) pageIndex-- else onClose()
    }

    LaunchedEffect(pageIndex, pages.size, animationsEnabled) {
        progress.snapTo(0f)
        if (!animationsEnabled || pages.isEmpty() || pageIndex >= lastIndex) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.animateTo(1f, tween(WRAPPED_PAGE_MS, easing = LinearEasing))
        pageIndex = (pageIndex + 1).coerceAtMost(lastIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WrappedBackground)
    ) {
        WrappedBackdrop(covers = covers, accent = accent, intro = page == WrappedPage.Intro)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            WrappedProgress(count = pages.size, current = pageIndex, progress = { progress.value })
            WrappedTopBar(
                label = if (page == WrappedPage.Intro) "" else strings.wrappedYearInMusic,
                closeLabel = strings.close,
                onClose = onClose
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .wrappedTapNavigation(
                        enabled = page != null && page != WrappedPage.Finale,
                        pageCount = pages.size,
                        onPrevious = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                        onNext = { pageIndex = (pageIndex + 1).coerceAtMost(lastIndex) }
                    )
            ) {
                WrappedBody(
                    state = state,
                    snapshot = snapshot,
                    page = page,
                    strings = strings,
                    locale = locale,
                    accent = accent,
                    animationsEnabled = animationsEnabled
                )
            }
        }
    }
}

private fun Modifier.wrappedTapNavigation(
    enabled: Boolean,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(pageCount) {
        detectTapGestures { offset ->
            if (offset.x < size.width * 0.3f) onPrevious() else onNext()
        }
    }
}

@Composable
private fun WrappedTopBar(label: String, closeLabel: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = WrappedMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = closeLabel, tint = WrappedInk)
        }
    }
}

@Composable
private fun BoxScope.WrappedBody(
    state: ListeningWrappedState,
    snapshot: ListeningInsightsSnapshot?,
    page: WrappedPage?,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color,
    animationsEnabled: Boolean
) {
    when {
        state.loading -> CircularProgressIndicator(
            color = accent,
            strokeWidth = 2.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp)
        )
        snapshot == null || page == null -> Text(
            text = if (state.failed) strings.insightsError else strings.wrappedEmpty.replace("%s", state.year.toString()),
            color = WrappedMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp)
        )
        else -> AnimatedContent(
            targetState = page,
            transitionSpec = { wrappedPageTransition(animationsEnabled) },
            label = "wrapped-page"
        ) { target ->
            WrappedPageContent(
                page = target,
                year = state.year,
                snapshot = snapshot,
                strings = strings,
                locale = locale,
                accent = accent,
                animationsEnabled = animationsEnabled
            )
        }
    }
}

private fun wrappedPageTransition(animationsEnabled: Boolean): ContentTransform = if (animationsEnabled) {
    val enter = fadeIn(tween(320)) + scaleIn(tween(420, easing = FastOutSlowInEasing), initialScale = 0.96f)
    enter togetherWith fadeOut(tween(180))
} else {
    fadeIn(snap()) togetherWith fadeOut(snap())
}

@Composable
private fun WrappedProgress(count: Int, current: Int, progress: () -> Float) {
    if (count <= 0) return
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(4.dp)
    ) {
        val gap = 4.dp.toPx()
        val width = (size.width - gap * (count - 1)) / count
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        repeat(count) { index ->
            val left = index * (width + gap)
            drawRoundRect(Color.White.copy(alpha = 0.24f), Offset(left, 0f), Size(width, size.height), radius)
            val fill = when {
                index < current -> 1f
                index == current -> progress().coerceIn(0f, 1f)
                else -> 0f
            }
            if (fill > 0f) {
                drawRoundRect(Color.White, Offset(left, 0f), Size(width * fill, size.height), radius)
            }
        }
    }
}

@Composable
private fun WrappedBackdrop(covers: List<String>, accent: Color, intro: Boolean) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        if (covers.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                repeat(5) { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(3) { column ->
                            val url = covers[(row * 3 + column + row) % covers.size]
                            val request = remember(context, url) {
                                ImageRequest.Builder(context).data(LevyraArtworkCache.large(url)).crossfade(true).build()
                            }
                            AsyncImage(
                                model = request,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alpha = if (intro) 0.55f else 0.16f,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to WrappedBackground.copy(alpha = if (intro) 0.15f else 0.55f),
                        0.45f to WrappedBackground.copy(alpha = if (intro) 0.55f else 0.8f),
                        1f to WrappedBackground
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to accent.copy(alpha = 0.22f)
                    )
                )
        )
    }
}

@Composable
private fun WrappedPageContent(
    page: WrappedPage,
    year: Int,
    snapshot: ListeningInsightsSnapshot,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color,
    animationsEnabled: Boolean
) {
    when (page) {
        WrappedPage.Intro -> WrappedIntro(year, strings)
        WrappedPage.Minutes -> WrappedMinutes(snapshot, strings, locale, accent, animationsEnabled)
        WrappedPage.Artist -> WrappedArtist(snapshot.topArtists, strings, locale, accent)
        WrappedPage.Tracks -> WrappedTracks(snapshot.topTracks, strings, accent)
        WrappedPage.Rhythm -> WrappedRhythm(snapshot, strings, locale, accent)
        WrappedPage.Finale -> WrappedFinale(year, snapshot, strings, locale, accent)
    }
}

@Composable
private fun WrappedIntro(year: Int, strings: LevyraStrings) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.levyra_logo),
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "LEVYRA",
                color = WrappedInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = strings.wrappedYearInMusic,
            color = WrappedMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            text = year.toString(),
            color = WrappedInk,
            fontSize = 128.sp,
            lineHeight = 128.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-5).sp,
            maxLines = 1
        )
        Text(
            text = strings.wrappedEntrySubtitle,
            color = WrappedMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = strings.wrappedTapToBegin, color = WrappedInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = WrappedInk)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WrappedMinutes(
    snapshot: ListeningInsightsSnapshot,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color,
    animationsEnabled: Boolean
) {
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val decimal = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 } }
    val minutes = snapshot.metrics.listenedMs.coerceAtLeast(0L) / 60_000L
    val hours = snapshot.metrics.listenedMs.coerceAtLeast(0L) / 3_600_000.0
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = strings.wrappedMinutesTitle, color = WrappedInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(270.dp)
                    .rotate(12f)
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.55f), LevyraViolet.copy(alpha = 0.35f))))
            )
            CountUpText(
                target = minutes,
                format = number,
                animate = animationsEnabled
            )
        }
        Text(
            text = strings.wrappedMinutesUnit.uppercase(locale),
            color = accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = strings.wrappedHours.replace("%s", decimal.format(hours)),
            color = WrappedMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        snapshot.metrics.trendPercent?.let { trend ->
            Spacer(Modifier.height(16.dp))
            Surface(color = WrappedInk.copy(alpha = 0.12f), shape = CircleShape) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (trend >= 0) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    val template = if (trend >= 0) strings.wrappedTrendUp else strings.wrappedTrendDown
                    Text(
                        text = template.replace("%s", number.format(abs(trend))),
                        color = WrappedInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CountUpText(target: Long, format: NumberFormat, animate: Boolean, modifier: Modifier = Modifier) {
    val value = remember(target) { Animatable(if (animate) 0f else target.toFloat()) }
    LaunchedEffect(target, animate) {
        if (animate) value.animateTo(target.toFloat(), tween(1_400, easing = FastOutSlowInEasing)) else value.snapTo(target.toFloat())
    }
    Text(
        text = format.format(value.value.toLong()),
        color = WrappedInk,
        fontSize = 88.sp,
        lineHeight = 92.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-3).sp,
        maxLines = 1,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WrappedArtist(
    artists: List<ListeningInsightsArtist>,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color
) {
    val context = LocalContext.current
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val named = artists.filter { it.name.isNotBlank() }
    val top = named.firstOrNull() ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = strings.wrappedArtistTitle, color = WrappedInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(Brush.linearGradient(listOf(accent, LevyraViolet))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ListeningReceiptModel.initial(top.name),
                color = WrappedOnAccent,
                fontSize = 84.sp,
                fontWeight = FontWeight.Black
            )
            if (top.artworkUrl.isNotBlank()) {
                val request = remember(context, top.artworkUrl) {
                    ImageRequest.Builder(context).data(LevyraArtworkCache.large(top.artworkUrl)).crossfade(true).build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = top.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = top.name,
            color = WrappedInk,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (top.plays > 0) {
            Text(
                text = "${number.format(top.plays)} ${strings.statPlays}",
                color = accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(16.dp))
        named.drop(1).take(2).forEachIndexed { index, artist ->
            Text(
                text = "${index + 2}  ${artist.name}",
                color = WrappedMuted,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WrappedTracks(tracks: List<ListeningInsightsTrack>, strings: LevyraStrings, accent: Color) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = strings.wrappedTracksTitle, color = WrappedInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        tracks.take(ListeningReceiptModel.MAX_TRACKS).forEachIndexed { index, track ->
            val first = index == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(if (first) 28.dp else 18.dp))
                    .background(if (first) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = if (first) accent else WrappedMuted,
                    fontSize = if (first) 28.sp else 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.width(34.dp)
                )
                Box(
                    modifier = Modifier
                        .size(if (first) 64.dp else 52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(accent, LevyraViolet))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ListeningReceiptModel.initial(track.title),
                        color = WrappedOnAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (track.artworkUrl.isNotBlank()) {
                        val request = remember(context, track.artworkUrl) {
                            ImageRequest.Builder(context).data(LevyraArtworkCache.large(track.artworkUrl)).crossfade(true).build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text(
                        text = track.title,
                        color = WrappedInk,
                        fontSize = if (first) 19.sp else 16.sp,
                        fontWeight = if (first) FontWeight.Black else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (track.artist.isNotBlank()) {
                        Text(
                            text = track.artist,
                            color = WrappedMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WrappedRhythm(
    snapshot: ListeningInsightsSnapshot,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color
) {
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val buckets = snapshot.hourBuckets
    val peak = snapshot.metrics.peakHour.takeIf { it in 0..23 } ?: buckets.indices.maxByOrNull { buckets[it] } ?: -1
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = strings.wrappedRhythmTitle, color = WrappedInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (peak >= 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.wrappedPeakHour.replace("%s", String.format(locale, "%02d:00", peak)),
                color = WrappedMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(28.dp))
        WrappedHourChart(buckets = buckets, peak = peak, accent = accent)
        Spacer(Modifier.height(28.dp))
        WrappedStatTiles(
            stats = listOf(
                number.format(snapshot.metrics.countedPlays) to strings.statPlays,
                number.format(snapshot.metrics.distinctTracks) to strings.statTracks,
                number.format(snapshot.metrics.distinctArtists) to strings.statArtists
            ),
            accent = accent
        )
    }
}

@Composable
private fun WrappedHourChart(buckets: List<Long>, peak: Int, accent: Color) {
    val maxValue = buckets.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        val count = buckets.size.coerceAtLeast(1)
        val step = size.width / count
        val barWidth = step * 0.62f
        buckets.forEachIndexed { index, value ->
            val fraction = (value.toFloat() / maxValue).coerceIn(0f, 1f)
            val height = (size.height * fraction).coerceAtLeast(barWidth)
            drawRoundRect(
                color = if (index == peak) accent else Color.White.copy(alpha = if (value > 0L) 0.42f else 0.14f),
                topLeft = Offset(index * step + (step - barWidth) / 2f, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("00", "06", "12", "18", "23").forEach {
            Text(text = it, color = WrappedMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WrappedStatTiles(stats: List<Pair<String, String>>, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        stats.forEachIndexed { index, (value, label) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(wrappedTileShape(first = index == 0, last = index == stats.lastIndex))
                    .background(accent.copy(alpha = 0.2f))
                    .padding(horizontal = 14.dp, vertical = 16.dp)
            ) {
                Text(text = value, color = WrappedInk, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(text = label, color = WrappedMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

private fun wrappedTileShape(first: Boolean, last: Boolean): RoundedCornerShape {
    val outer = 26.dp
    val inner = 8.dp
    return RoundedCornerShape(
        topStart = if (first) outer else inner,
        bottomStart = if (first) outer else inner,
        topEnd = if (last) outer else inner,
        bottomEnd = if (last) outer else inner
    )
}

@Composable
private fun WrappedFinale(
    year: Int,
    snapshot: ListeningInsightsSnapshot,
    strings: LevyraStrings,
    locale: Locale,
    accent: Color
) {
    val content = remember(snapshot, strings, locale, year) {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        ListeningReceiptModel.content(
            snapshot = snapshot,
            labels = receiptLabels(strings, strings.wrappedReceiptTitle, year.toString(), locale),
            locale = locale,
            nowMs = now,
            zone = zone,
            range = ListeningInsightsRanges.year(year, now, zone)
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = strings.wrappedFinaleTitle,
            color = WrappedInk,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
        ListeningReceiptComposer(
            content = content,
            loading = false,
            accent = accent,
            textColor = WrappedInk,
            mutedColor = WrappedMuted,
            previewMaxHeight = 460.dp
        )
    }
}
