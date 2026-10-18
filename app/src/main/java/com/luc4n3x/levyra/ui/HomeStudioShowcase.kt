package com.luc4n3x.levyra.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import com.luc4n3x.levyra.ui.components.levyraMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.domain.ResonanceCommentSnippet
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import kotlin.math.PI
import kotlin.math.sin

private const val EDITORIAL_CHART_LEAD_COUNT = 6
private const val MAX_CHART_PODIUM_COUNT = 3
private const val MAX_CHART_TICKER_END = 14

internal class StudioChartRowState(
    val currentId: String?,
    val isPlaying: Boolean,
    val onPlay: (Track) -> Unit
)

@Composable
internal fun StudioEditorialChartFolio(
    tracks: List<Track>,
    rowState: StudioChartRowState,
    modifier: Modifier = Modifier
) {
    val remaining = tracks.drop(EDITORIAL_CHART_LEAD_COUNT)
    if (remaining.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chapters = remember(remaining) { editorialChapters(remaining) }
    Column(modifier = modifier.fillMaxWidth()) {
        EditorialFolioToggle(expanded = expanded, remaining = remaining.size, onToggle = { expanded = !expanded })
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chapters.forEach { chapter -> EditorialChapter(chapter, rowState) }
            }
        }
    }
}

private class EditorialChapterRows(val firstRank: Int, val lastRank: Int, val tracks: List<Track>)

private fun editorialChapters(remaining: List<Track>): List<EditorialChapterRows> =
    remaining
        .withIndex()
        .groupBy { (index, _) -> (index + EDITORIAL_CHART_LEAD_COUNT) / 10 }
        .map { (_, rows) ->
            val firstRank = rows.first().index + EDITORIAL_CHART_LEAD_COUNT + 1
            EditorialChapterRows(firstRank, firstRank + rows.size - 1, rows.map { it.value })
        }

@Composable
private fun EditorialFolioToggle(expanded: Boolean, remaining: Int, onToggle: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val label = if (expanded) strings.showLess else strings.showAll
    val arrowTurn by animateFloatAsState(if (expanded) 180f else 0f, label = "editorial-folio-arrow")
    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        HorizontalDivider(color = LevyraText.copy(alpha = 0.55f), thickness = 1.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .levyraPressable(onClick = onToggle, onClickLabel = label, role = Role.Button),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = LevyraText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!expanded) {
                Text(
                    text = "+$remaining",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = LevyraText,
                modifier = Modifier.graphicsLayer { rotationZ = arrowTurn }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    }
}

@Composable
private fun EditorialChapter(chapter: EditorialChapterRows, rowState: StudioChartRowState) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "${chartRank(chapter.firstRank)} — ${chartRank(chapter.lastRank)}",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
        }
        chapter.tracks.forEachIndexed { offset, track ->
            EditorialIndexRow(rank = chapter.firstRank + offset, track = track, rowState = rowState)
        }
    }
}

@Composable
private fun EditorialIndexRow(rank: Int, track: Track, rowState: StudioChartRowState) {
    val current = track.id == rowState.currentId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .levyraPressable(onClick = { rowState.onPlay(track) }, onClickLabel = track.title, role = Role.Button),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = chartRank(rank),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Serif,
            color = if (current) MaterialTheme.colorScheme.primary else LevyraMuted.copy(alpha = 0.72f),
            modifier = Modifier.width(40.dp)
        )
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(4.dp))) {
            CoverImage(track = track, modifier = Modifier.fillMaxSize())
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.titleSmall, color = LevyraText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = LevyraMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (current) {
            LevyraPlayingIndicator(playing = rowState.isPlaying, color = MaterialTheme.colorScheme.primary, size = 12.dp)
        }
    }
}

private fun chartRank(rank: Int): String = rank.toString().padStart(2, '0')

@Composable
internal fun StudioExpressiveMaxChartBody(tracks: List<Track>, rowState: StudioChartRowState) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        MaxChartPodium(tracks.take(MAX_CHART_PODIUM_COUNT), rowState)
        val ticker = tracks.drop(MAX_CHART_PODIUM_COUNT).take(MAX_CHART_TICKER_END - MAX_CHART_PODIUM_COUNT)
        if (ticker.isNotEmpty()) MaxChartTicker(ticker, rowState)
        val rest = tracks.drop(MAX_CHART_TICKER_END)
        if (rest.isNotEmpty()) MaxChartExpandableList(rest, rowState)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MaxChartPodium(podium: List<Track>, rowState: StudioChartRowState) {
    val leader = podium.firstOrNull() ?: return
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(286.dp), contentAlignment = Alignment.TopCenter) {
            podium.getOrNull(1)?.let { second ->
                MaxPodiumSatellite(
                    track = second,
                    rank = 2,
                    shape = MaterialShapes.Clover8Leaf.toShape(),
                    tilt = -9f,
                    rowState = rowState,
                    modifier = Modifier.align(Alignment.BottomStart).offset(x = 18.dp)
                )
            }
            podium.getOrNull(2)?.let { third ->
                MaxPodiumSatellite(
                    track = third,
                    rank = 3,
                    shape = MaterialShapes.Gem.toShape(),
                    tilt = 8f,
                    rowState = rowState,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-18).dp, y = (-22).dp)
                )
            }
            MaxPodiumLeader(leader, rowState)
        }
        Text(
            text = leader.title,
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = LevyraText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 32.dp).levyraMarquee()
        )
        Text(
            text = leader.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = LevyraMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MaxPodiumLeader(track: Track, rowState: StudioChartRowState) {
    val scheme = MaterialTheme.colorScheme
    val spin = rememberStudioSpin(durationMillis = 26_000)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(236.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = spin() }
                .clip(MaterialShapes.SoftBurst.toShape())
                .background(Brush.sweepGradient(listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primary)))
        )
        Box(
            modifier = Modifier
                .size(178.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .levyraPressable(onClick = { rowState.onPlay(track) }, onClickLabel = track.title, role = Role.Button)
        ) {
            CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = true)
            if (track.id == rowState.currentId) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)), contentAlignment = Alignment.Center) {
                    LevyraPlayingIndicator(playing = rowState.isPlaying, color = Color.White, size = 22.dp)
                }
            }
        }
        MaxRankBadge(rank = 1, size = 58.dp, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun MaxPodiumSatellite(
    track: Track,
    rank: Int,
    shape: Shape,
    tilt: Float,
    rowState: StudioChartRowState,
    modifier: Modifier
) {
    Box(modifier = modifier.size(122.dp).graphicsLayer { rotationZ = tilt }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .levyraPressable(onClick = { rowState.onPlay(track) }, onClickLabel = track.title, role = Role.Button)
        ) {
            CoverImage(track = track, modifier = Modifier.fillMaxSize())
            if (track.id == rowState.currentId) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)), contentAlignment = Alignment.Center) {
                    LevyraPlayingIndicator(playing = rowState.isPlaying, color = Color.White, size = 16.dp)
                }
            }
        }
        MaxRankBadge(rank = rank, size = 42.dp, modifier = Modifier.align(Alignment.TopStart).offset(x = (-6).dp, y = (-6).dp))
    }
}

@Composable
private fun MaxRankBadge(rank: Int, size: Dp, modifier: Modifier = Modifier) {
    val (container, content) = maxRankTone(rank)
    val shape = maxRankShape(rank)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container)
            .border(2.dp, MaterialTheme.colorScheme.surface, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = content
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun maxRankShape(rank: Int): Shape = when (rank % 4) {
    1 -> MaterialShapes.Sunny.toShape()
    2 -> MaterialShapes.Cookie9Sided.toShape()
    3 -> MaterialShapes.Clover4Leaf.toShape()
    else -> MaterialShapes.SoftBurst.toShape()
}

@Composable
private fun maxRankTone(rank: Int): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (rank % 3) {
        1 -> scheme.primary to scheme.onPrimary
        2 -> scheme.tertiary to scheme.onTertiary
        else -> scheme.secondary to scheme.onSecondary
    }
}

@Composable
private fun MaxChartTicker(tracks: List<Track>, rowState: StudioChartRowState) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(tracks, key = { index, track -> "max-ticker-$index-${track.id}" }, contentType = { _, _ -> "max-ticker" }) { index, track ->
            MaxTickerChip(rank = index + MAX_CHART_PODIUM_COUNT + 1, track = track, rowState = rowState)
        }
    }
}

@Composable
private fun MaxTickerChip(rank: Int, track: Track, rowState: StudioChartRowState) {
    val scheme = MaterialTheme.colorScheme
    val current = track.id == rowState.currentId
    Surface(
        shape = CircleShape,
        color = if (current) scheme.primaryContainer else scheme.surfaceContainerHigh,
        modifier = Modifier
            .width(232.dp)
            .levyraPressable(onClick = { rowState.onPlay(track) }, onClickLabel = track.title, role = Role.Button)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(52.dp)) {
                Box(Modifier.fillMaxSize().clip(CircleShape)) { CoverImage(track = track, modifier = Modifier.fillMaxSize()) }
                MaxRankBadge(rank = rank, size = 26.dp, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = LevyraText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.levyraMarquee())
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = LevyraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (current) {
                LevyraPlayingIndicator(playing = rowState.isPlaying, color = scheme.primary, size = 12.dp)
                Spacer(Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun MaxChartExpandableList(tracks: List<Track>, rowState: StudioChartRowState) {
    val strings = LocalLevyraStrings.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val arrowTurn by animateFloatAsState(if (expanded) 180f else 0f, label = "max-chart-arrow")
    val range = "${chartRank(MAX_CHART_TICKER_END + 1)}–${chartRank(MAX_CHART_TICKER_END + tracks.size)}"
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) strings.showLess else "${strings.showAll} · $range")
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.graphicsLayer { rotationZ = arrowTurn })
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tracks.forEachIndexed { index, track ->
                    MaxSegmentRow(
                        rank = MAX_CHART_TICKER_END + index + 1,
                        track = track,
                        first = index == 0,
                        last = index == tracks.lastIndex,
                        rowState = rowState
                    )
                }
            }
        }
    }
}

@Composable
private fun MaxSegmentRow(rank: Int, track: Track, first: Boolean, last: Boolean, rowState: StudioChartRowState) {
    val scheme = MaterialTheme.colorScheme
    val current = track.id == rowState.currentId
    val top = if (first) 28.dp else 8.dp
    val bottom = if (last) 28.dp else 8.dp
    Surface(
        shape = RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom),
        color = if (current) scheme.primaryContainer else scheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .levyraPressable(onClick = { rowState.onPlay(track) }, onClickLabel = track.title, role = Role.Button)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MaxRankBadge(rank = rank, size = 38.dp)
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))) { CoverImage(track = track, modifier = Modifier.fillMaxSize()) }
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = LevyraText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.levyraMarquee())
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = LevyraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (current) LevyraPlayingIndicator(playing = rowState.isPlaying, color = scheme.primary, size = 12.dp)
        }
    }
}

@Composable
private fun rememberStudioSpin(durationMillis: Int): () -> Float {
    if (!LocalAnimationsEnabled.current) return { 0f }
    val transition = rememberInfiniteTransition(label = "studio-spin")
    val angle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = "studio-spin-angle"
    )
    return { angle.value }
}

internal class StudioSpotlightPlayback(
    val playLabel: String,
    val resolving: Boolean,
    val playIcon: ImageVector,
    val onOpen: () -> Unit
)

@Composable
internal fun StudioFlowSpotlight(
    track: Track,
    artistArtworkUrl: String,
    label: String,
    lead: String,
    inset: Dp,
    playback: StudioSpotlightPlayback
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(312.dp)
            .levyraPressable(onClick = playback.onOpen, onClickLabel = track.title, role = Role.Button)
    ) {
        FlowMeltingArtwork(
            track = track,
            artistArtworkUrl = artistArtworkUrl,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.74f)
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawFlowWave(scheme.primary.copy(alpha = 0.55f), baseline = 0.8f, amplitude = 0.06f, phase = 0f, stroke = 3.dp.toPx())
            drawFlowWave(scheme.tertiary.copy(alpha = 0.35f), baseline = 0.86f, amplitude = 0.05f, phase = 1.4f, stroke = 2.dp.toPx())
            drawFlowWave(scheme.secondary.copy(alpha = 0.22f), baseline = 0.92f, amplitude = 0.04f, phase = 2.6f, stroke = 1.5.dp.toPx())
        }
        Column(
            modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.66f).padding(start = inset),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(scheme.primary))
                Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = scheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(track.title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = LevyraText, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(lead.ifBlank { track.artist }, style = MaterialTheme.typography.bodyLarge, color = LevyraText.copy(alpha = 0.72f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            FlowPlayPill(playback)
        }
    }
}

@Composable
private fun FlowMeltingArtwork(track: Track, artistArtworkUrl: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.horizontalGradient(0f to Color.Transparent, 0.48f to Color.Black, 1f to Color.Black),
                    blendMode = BlendMode.DstIn
                )
                drawRect(
                    brush = Brush.verticalGradient(0f to Color.Transparent, 0.22f to Color.Black, 0.74f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        if (artistArtworkUrl.isNotBlank()) {
            StudioRemoteArtwork(artistArtworkUrl, track.artist, Modifier.fillMaxSize(), true)
        } else {
            CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = true)
        }
    }
}

@Composable
private fun FlowPlayPill(playback: StudioSpotlightPlayback) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = playback.onOpen,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 8.dp),
        color = scheme.primary,
        contentColor = scheme.onPrimary,
        modifier = Modifier.padding(top = 6.dp).heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (playback.resolving) LevyraLoadingIndicator(Modifier.size(22.dp), color = scheme.onPrimary)
            else Icon(playback.playIcon, contentDescription = null)
            Text(playback.playLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private fun DrawScope.drawFlowWave(color: Color, baseline: Float, amplitude: Float, phase: Float, stroke: Float) {
    val path = Path()
    val steps = 48
    for (step in 0..steps) {
        val x = size.width * step / steps
        val y = size.height * (baseline + amplitude * sin(phase + step.toFloat() / steps * 2f * PI.toFloat()))
        if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
}

internal class StudioResonanceActions(
    val playLabel: String,
    val currentId: String?,
    val isPlaying: Boolean,
    val onPlay: (Track) -> Unit,
    val onOpenComments: (Track) -> Unit
)

@Composable
internal fun StudioResonanceSection(
    style: HomeStyle,
    inset: Dp,
    tracks: List<Track>,
    comments: Map<String, ResonanceCommentSnippet>,
    actions: StudioResonanceActions,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) return
    val strings = LocalLevyraStrings.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.padding(horizontal = inset)) {
            Text(
                text = strings.mostCommentedTracks,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = if (style == HomeStyle.ExpressiveMax) FontWeight.Black else FontWeight.Bold),
                color = LevyraText,
                modifier = Modifier.semantics { heading() }
            )
            Text(strings.tapToOpenComments, style = MaterialTheme.typography.bodyMedium, color = LevyraMuted)
        }
        if (style == HomeStyle.ExpressiveMax) {
            MaxCommentWall(tracks, comments, actions)
        } else {
            FlowCommentRiver(tracks, comments, actions, inset)
        }
    }
}

private val FlowRiverItemWidth = 248.dp
private val FlowRiverHeight = 356.dp
private val FlowRiverBubbleHeight = 214.dp
private val FlowRiverAvatarSize = 46.dp
private const val FLOW_RIVER_MID = 0.5f
private const val FLOW_RIVER_AMPLITUDE = 0.2f

@Composable
private fun FlowCommentRiver(
    tracks: List<Track>,
    comments: Map<String, ResonanceCommentSnippet>,
    actions: StudioResonanceActions,
    inset: Dp
) {
    val scheme = MaterialTheme.colorScheme
    val waveBrush = remember(scheme.primary, scheme.tertiary) { Brush.horizontalGradient(listOf(scheme.primary, scheme.tertiary)) }
    LazyRow(contentPadding = PaddingValues(horizontal = inset)) {
        itemsIndexed(tracks, key = { index, track -> "flow-river-$index-${track.id}" }, contentType = { _, _ -> "flow-river" }) { index, track ->
            Box(
                modifier = Modifier
                    .width(FlowRiverItemWidth)
                    .height(FlowRiverHeight)
                    .drawBehind { drawRiverSegment(index, waveBrush) }
            ) {
                FlowRiverStop(index = index, track = track, snippet = comments[track.id], actions = actions)
            }
        }
    }
}

private fun DrawScope.drawRiverSegment(index: Int, brush: Brush) {
    val path = Path()
    val steps = 32
    for (step in 0..steps) {
        val x = size.width * step / steps
        val y = riverY(index, step.toFloat() / steps) * size.height
        if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, brush = brush, alpha = 0.18f, style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
    drawPath(path, brush = brush, alpha = 0.8f, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
}

private fun riverY(index: Int, fraction: Float): Float =
    FLOW_RIVER_MID - FLOW_RIVER_AMPLITUDE * sin(index * PI.toFloat() + fraction * PI.toFloat())

@Composable
private fun FlowRiverStop(index: Int, track: Track, snippet: ResonanceCommentSnippet?, actions: StudioResonanceActions) {
    val crest = index % 2 == 0
    val avatarTop = FlowRiverHeight * riverY(index, 0.5f) - FlowRiverAvatarSize / 2
    val bubbleTop = if (crest) avatarTop + FlowRiverAvatarSize + 8.dp else avatarTop - FlowRiverBubbleHeight - 8.dp
    Box(modifier = Modifier.fillMaxSize()) {
        FlowRiverBubble(
            track = track,
            snippet = snippet,
            tailOnTop = crest,
            actions = actions,
            modifier = Modifier.padding(horizontal = 8.dp).offset(y = bubbleTop)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = avatarTop)
                .size(FlowRiverAvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            ResonanceAvatar(snippet = snippet, track = track)
        }
    }
}

@Composable
private fun ResonanceAvatar(snippet: ResonanceCommentSnippet?, track: Track) {
    val avatar = snippet?.authorAvatarUrl.orEmpty()
    if (avatar.isNotBlank()) {
        StudioRemoteArtwork(avatar, snippet?.author.orEmpty(), Modifier.fillMaxSize(), false)
    } else {
        CoverImage(track = track, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun FlowRiverBubble(
    track: Track,
    snippet: ResonanceCommentSnippet?,
    tailOnTop: Boolean,
    actions: StudioResonanceActions,
    modifier: Modifier
) {
    val round = 28.dp
    val tail = 6.dp
    Surface(
        shape = if (tailOnTop) RoundedCornerShape(tail, round, round, round) else RoundedCornerShape(round, round, round, tail),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        modifier = modifier
            .fillMaxWidth()
            .height(FlowRiverBubbleHeight)
            .levyraPressable(onClick = { actions.onOpenComments(track) }, onClickLabel = track.title, role = Role.Button)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            ResonanceSnippetBody(snippet = snippet, color = LevyraText, modifier = Modifier.weight(1f))
            ResonanceTrackLine(track = track, actions = actions, coverShape = CircleShape)
        }
    }
}

@Composable
private fun ResonanceSnippetBody(snippet: ResonanceCommentSnippet?, color: Color, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (snippet != null && snippet.author.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(snippet.author, style = MaterialTheme.typography.labelLarge, color = color.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (snippet.countText.isNotBlank()) {
                    Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    Text(snippet.countText, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.6f))
                }
            }
        }
        when {
            snippet == null || snippet.isLoading -> LevyraLoadingIndicator(Modifier.size(32.dp), color = color)
            snippet.text.isNotBlank() -> Text(snippet.text, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 4, overflow = TextOverflow.Ellipsis)
            else -> Spacer(Modifier.height(1.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResonanceTrackLine(track: Track, actions: StudioResonanceActions, coverShape: Shape) {
    val current = track.id == actions.currentId
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(40.dp).clip(coverShape)) { CoverImage(track = track, modifier = Modifier.fillMaxSize()) }
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (current) MaterialTheme.colorScheme.primary else LevyraText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.levyraMarquee())
            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = LevyraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledIconButton(onClick = { actions.onPlay(track) }, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(40.dp)) {
            Icon(if (current && actions.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = actions.playLabel)
        }
    }
}

@Composable
private fun MaxCommentWall(
    tracks: List<Track>,
    comments: Map<String, ResonanceCommentSnippet>,
    actions: StudioResonanceActions
) {
    LazyRow(
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        itemsIndexed(tracks, key = { index, track -> "max-wall-$index-${track.id}" }, contentType = { _, _ -> "max-wall" }) { index, track ->
            MaxCommentSticker(index = index, track = track, snippet = comments[track.id], actions = actions)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MaxCommentSticker(index: Int, track: Track, snippet: ResonanceCommentSnippet?, actions: StudioResonanceActions) {
    val (container, content) = maxStickerTone(index)
    val tilt = if (index % 2 == 0) -2.5f else 2f
    Box(modifier = Modifier.width(264.dp).height(318.dp).graphicsLayer { rotationZ = tilt }) {
        Surface(
            shape = if (index % 2 == 0) RoundedCornerShape(40.dp, 40.dp, 40.dp, 10.dp) else RoundedCornerShape(10.dp, 40.dp, 40.dp, 40.dp),
            color = container,
            contentColor = content,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 22.dp)
                .levyraPressable(onClick = { actions.onOpenComments(track) }, onClickLabel = track.title, role = Role.Button)
        ) {
            Box {
                Text(
                    text = "“",
                    style = MaterialTheme.typography.displayLargeEmphasized.copy(fontSize = 140.sp),
                    color = content.copy(alpha = 0.16f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp).offset(y = 46.dp)
                )
                Column(
                    modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 44.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    MaxStickerQuote(snippet = snippet, content = content, modifier = Modifier.weight(1f))
                    ResonanceTrackLine(track = track, actions = actions, coverShape = MaterialShapes.Cookie9Sided.toShape())
                }
            }
        }
        MaxStickerAvatar(index = index, snippet = snippet, track = track, modifier = Modifier.padding(start = 18.dp))
        if (snippet != null && snippet.countText.isNotBlank()) {
            MaxStickerCount(countText = snippet.countText, modifier = Modifier.align(Alignment.TopEnd).padding(end = 14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MaxStickerQuote(snippet: ResonanceCommentSnippet?, content: Color, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (snippet != null && snippet.author.isNotBlank()) {
            Text(snippet.author, style = MaterialTheme.typography.labelLargeEmphasized, color = content.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        when {
            snippet == null || snippet.isLoading -> LevyraLoadingIndicator(Modifier.size(36.dp), color = content)
            snippet.text.isNotBlank() -> Text(snippet.text, style = MaterialTheme.typography.titleMediumEmphasized, color = content, maxLines = 5, overflow = TextOverflow.Ellipsis)
            else -> Spacer(Modifier.height(1.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MaxStickerAvatar(index: Int, snippet: ResonanceCommentSnippet?, track: Track, modifier: Modifier) {
    val shape = when (index % 3) {
        0 -> MaterialShapes.Sunny.toShape()
        1 -> MaterialShapes.Clover4Leaf.toShape()
        else -> MaterialShapes.Cookie9Sided.toShape()
    }
    Box(
        modifier = modifier
            .size(58.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(3.dp)
            .clip(shape)
    ) {
        ResonanceAvatar(snippet = snippet, track = track)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MaxStickerCount(countText: String, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(60.dp)
            .clip(MaterialShapes.SoftBurst.toShape())
            .background(scheme.inverseSurface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
            color = scheme.inverseOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun maxStickerTone(index: Int): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (index % 3) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
}
