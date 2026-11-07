package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.apple.AppleDailyChart
import com.luc4n3x.levyra.data.news.MusicNewsStory
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.DailyTop100Copy
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.exploreEditorialCopy
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.viewmodel.DailyTop100ChartUiState
import java.util.Locale

internal val AtelierPaper = Color(0xFFEDE4D0)
private val AtelierPaperShade = Color(0xFFD9CCB1)
internal val AtelierInk = Color(0xFF1B1815)
internal val AtelierSerif = FontFamily.Serif
private val AtelierChartSleeveSide = 140.dp
private val AtelierMoodSleeveSide = 132.dp
private val AtelierDeskThumb = 76.dp
private const val AtelierPosterAspect = 0.74f

private fun atelierSepiaFilter(strength: Float): ColorFilter {
    val sepia = floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
    val identity = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
    return ColorFilter.colorMatrix(ColorMatrix(FloatArray(20) { identity[it] + (sepia[it] - identity[it]) * strength }))
}

internal val AtelierPosterFilter = atelierSepiaFilter(0.78f)
private val AtelierPrintFilter = atelierSepiaFilter(0.9f)
private val AtelierSleeveFilter = atelierSepiaFilter(0.45f)

internal fun Modifier.atelierAgedPrint(): Modifier = drawWithCache {
    val step = 3.dp.toPx()
    val lines = Path().apply {
        var y = 0f
        while (y < size.height) {
            moveTo(0f, y)
            lineTo(size.width, y)
            y += step
        }
    }
    val stroke = Stroke(0.6.dp.toPx())
    val vignette = Brush.radialGradient(
        0.55f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.55f),
        center = Offset(size.width / 2f, size.height / 2f),
        radius = maxOf(size.width, size.height) * 0.72f
    )
    onDrawWithContent {
        drawContent()
        drawPath(lines, Color.Black.copy(alpha = 0.07f), style = stroke)
        drawRect(vignette)
    }
}

@Composable
internal fun AtelierPrintImage(url: String, filter: ColorFilter, glyph: String, modifier: Modifier) {
    Box(modifier.background(AtelierPaperShade).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Text(glyph, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 44.sp, color = AtelierInk.copy(alpha = 0.4f))
        if (url.isNotBlank()) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, colorFilter = filter, modifier = Modifier.fillMaxSize())
        }
    }
}

private fun StudioHero.atelierSecondLine(): String =
    lead.takeIf { it.isNotBlank() && !it.trim().endsWith(track.artist.trim(), ignoreCase = true) } ?: track.title

@Composable
internal fun AtelierSoundtrackPoster(hero: StudioHero) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    val active = hero.isCurrent && hero.isPlaying
    val artwork = hero.artistArtworkUrl.ifBlank { atelierCoverUrl(hero.track) }
    val spin = if (active && hero.animationsEnabled) {
        rememberInfiniteTransition(label = "atelier-poster-vinyl")
            .animateFloat(0f, 360f, infiniteRepeatable(tween(5400, easing = LinearEasing), RepeatMode.Restart), label = "atelier-poster-spin")
    } else {
        null
    }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(top = 10.dp, bottom = 4.dp)) {
        val posterWidth = maxWidth * 0.84f
        val posterHeight = posterWidth / AtelierPosterAspect
        val disc = maxWidth * 0.66f
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(posterHeight)) {
                AtelierVinyl(
                    hero.track,
                    Modifier.align(Alignment.CenterEnd).offset(x = disc * 0.12f).size(disc)
                        .graphicsLayer { rotationZ = spin?.value ?: 18f }
                )
                Box(
                    Modifier.align(Alignment.CenterStart).width(posterWidth).fillMaxHeight()
                        .graphicsLayer {
                            rotationZ = -1.4f
                            shadowElevation = 26.dp.toPx()
                            shape = RectangleShape
                            clip = true
                        }
                        .background(AtelierPaper)
                        .levyraPressable(
                            onClick = hero.onOpen,
                            role = Role.Button,
                            onClickLabel = if (active) strings.pause else strings.play,
                            pressedScale = LevyraPressScale.Surface
                        )
                        .semantics(mergeDescendants = true) { contentDescription = "${hero.label}, ${hero.track.artist}" }
                        .padding(9.dp)
                ) {
                    Box(Modifier.fillMaxSize().border(1.dp, AtelierInk.copy(alpha = 0.5f))) {
                        AtelierPrintImage(artwork, AtelierPosterFilter, hero.track.artist.take(1), Modifier.fillMaxSize().atelierAgedPrint())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.48f to Color.Transparent, 1f to AtelierInk.copy(alpha = 0.92f))))
                        AtelierPosterStamp(hero.label, Modifier.align(Alignment.TopStart).padding(14.dp))
                        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp)) {
                            Text(
                                "N° 01 · ${copy.sideA}",
                                fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 13.sp,
                                color = AtelierPaper.copy(alpha = 0.78f), maxLines = 1
                            )
                            Text(
                                hero.track.artist,
                                fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 40.sp,
                                color = AtelierPaper, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                hero.atelierSecondLine(),
                                style = MaterialTheme.typography.bodyMedium, color = AtelierPaper.copy(alpha = 0.82f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                AtelierHardwareKey(
                    label = if (active) strings.pause else strings.play,
                    lit = active,
                    busy = hero.isCurrent && hero.isResolving,
                    onClick = hero.onOpen,
                    modifier = Modifier.width(92.dp).height(56.dp)
                ) { AtelierPlayGlyph(active, 28.dp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        atelierUppercase(hero.label), style = MaterialTheme.typography.labelMedium,
                        color = AtelierDesign.accentBright, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        hero.lead.ifBlank { hero.track.title }, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 14.sp,
                        color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (hero.companionArtworkUrls.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp), modifier = Modifier.clearAndSetSemantics { }) {
                        hero.companionArtworkUrls.take(3).forEach { url ->
                            AsyncImage(
                                url, null, contentScale = ContentScale.Crop, colorFilter = AtelierSleeveFilter,
                                modifier = Modifier.size(32.dp).clip(CircleShape).border(2.dp, AtelierDesign.room, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AtelierPosterStamp(label: String, modifier: Modifier) {
    Text(
        atelierUppercase(label),
        fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 3.sp,
        color = AtelierDesign.accentBright, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = modifier.graphicsLayer { rotationZ = -6f }
            .background(AtelierInk.copy(alpha = 0.72f))
            .border(2.dp, AtelierDesign.accentBright)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

private fun atelierNewsGlyph(source: String): String = source.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "♪"

@Composable
private fun AtelierNewsKicker(source: String, age: String?) {
    val rubric = source.trim().takeIf(String::isNotEmpty)?.let { atelierUppercase(it) }
    val text = listOfNotNull(rubric, age).joinToString("  ·  ")
    if (text.isEmpty()) return
    Text(
        text, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
        color = AtelierDesign.accent, maxLines = 1, overflow = TextOverflow.Ellipsis
    )
}

private val AtelierCoverFilter = atelierSepiaFilter(0.55f)

@Composable
internal fun AtelierGazette(stories: List<MusicNewsStory>, statusTop: Dp, onOpenSearch: () -> Unit, onOpen: (MusicNewsStory) -> Unit) {
    val front = remember(stories) { exploreNewsFront(stories) } ?: return
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { exploreEditorialCopy(strings.code) }
    val edition = rememberExploreEditionDate()
    val nowMs = remember(stories) { System.currentTimeMillis() }
    val lead = front.lead
    Column(Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            Modifier.fillMaxWidth()
                .levyraPressable(onClick = { onOpen(lead) }, onClickLabel = copy.readStory, role = Role.Button, pressedScale = LevyraPressScale.Surface)
        ) {
            val coverHeight = (maxWidth * 1.3f).coerceIn(480.dp, 680.dp) + statusTop
            Box(Modifier.fillMaxWidth().height(coverHeight)) {
                AtelierPrintImage(lead.imageUrl, AtelierCoverFilter, atelierNewsGlyph(lead.source), Modifier.fillMaxSize().atelierAgedPrint())
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to AtelierDesign.room.copy(alpha = 0.85f),
                            0.28f to Color.Transparent,
                            0.5f to Color.Transparent,
                            0.82f to AtelierDesign.room.copy(alpha = 0.88f),
                            1f to AtelierDesign.room
                        )
                    )
                )
                Column(Modifier.fillMaxWidth().padding(top = statusTop)) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = 8.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            atelierUppercase(strings.explore), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.4.sp),
                            color = AtelierPaper.copy(alpha = 0.85f), modifier = Modifier.weight(1f)
                        )
                        AtelierIconAction(Icons.Rounded.Search, strings.search, onOpenSearch, AtelierPaper)
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(top = 6.dp)) {
                        Box(Modifier.fillMaxWidth().height(2.dp).background(AtelierPaper))
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierPaper.copy(alpha = 0.7f)))
                        Text(
                            atelierUppercase(copy.newsTitle),
                            fontFamily = AtelierSerif, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = 1.sp,
                            color = AtelierPaper, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics { heading() }
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierPaper.copy(alpha = 0.7f)))
                        Text(
                            edition, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 13.sp,
                            color = AtelierPaper.copy(alpha = 0.85f), textAlign = TextAlign.Center, maxLines = 1,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierPaper.copy(alpha = 0.7f)))
                    }
                }
                Column(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(bottom = 18.dp)
                        .semantics(mergeDescendants = true) { }
                ) {
                    AtelierNewsKicker(lead.source, exploreNewsAgeLabel(copy, nowMs, lead.publishedAtMs))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        lead.title, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 35.sp,
                        color = AtelierPaper, maxLines = 5, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(28.dp).height(2.dp).background(AtelierDesign.accentBright))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            atelierUppercase(copy.readStory), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.8.sp),
                            color = AtelierDesign.accentBright, maxLines = 1
                        )
                    }
                }
            }
        }
        if (front.secondary.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = AtelierDesign.gutter).padding(top = 18.dp)) {
                front.secondary.forEachIndexed { index, story ->
                    if (index > 0) {
                        Box(Modifier.padding(horizontal = 12.dp).width(1.dp).fillMaxHeight().background(AtelierDesign.hairline))
                    }
                    AtelierColumnStory(story, exploreNewsAgeLabel(copy, nowMs, story.publishedAtMs), copy.readStory, Modifier.weight(1f)) { onOpen(story) }
                }
                if (front.secondary.size < ExploreNewsSecondaryCount) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AtelierColumnStory(story: MusicNewsStory, age: String?, actionLabel: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.levyraPressable(onClick = onClick, onClickLabel = actionLabel, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { }
    ) {
        AtelierPrintImage(
            story.imageUrl, AtelierPrintFilter, atelierNewsGlyph(story.source),
            Modifier.fillMaxWidth().aspectRatio(4f / 3f).atelierAgedPrint()
        )
        Spacer(Modifier.height(10.dp))
        AtelierNewsKicker(story.source, null)
        Spacer(Modifier.height(4.dp))
        Text(
            story.title, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp,
            color = AtelierDesign.text, maxLines = 4, overflow = TextOverflow.Ellipsis
        )
        age?.let {
            Text(
                it, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 12.sp,
                color = AtelierDesign.textFaint, maxLines = 1, modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
internal fun AtelierNewsDesk(stories: List<MusicNewsStory>, onOpen: (MusicNewsStory) -> Unit) {
    val desk = remember(stories) { exploreNewsDesk(stories, null) }
    if (desk.isEmpty()) return
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { exploreEditorialCopy(strings.code) }
    val nowMs = remember(stories) { System.currentTimeMillis() }
    Column(Modifier.fillMaxWidth()) {
        AtelierSectionLabel(copy.newsMore)
        desk.forEachIndexed { index, story ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = AtelierDeskThumb + 24.dp).atelierBottomRule()
                    .levyraPressable(onClick = { onOpen(story) }, onClickLabel = copy.readStory, role = Role.Button, pressedScale = LevyraPressScale.Row)
                    .semantics(mergeDescendants = true) { }
                    .padding(horizontal = AtelierDesign.gutter, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "%02d".format(Locale.ROOT, index + 1), fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 26.sp,
                    color = AtelierDesign.accent, modifier = Modifier.width(44.dp).clearAndSetSemantics { }
                )
                Column(Modifier.weight(1f).padding(end = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AtelierNewsKicker(story.source, exploreNewsAgeLabel(copy, nowMs, story.publishedAtMs))
                    Text(
                        story.title, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp,
                        color = AtelierDesign.text, maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                }
                AtelierPrintImage(
                    story.imageUrl, AtelierPrintFilter, atelierNewsGlyph(story.source),
                    Modifier.size(AtelierDeskThumb).clip(AtelierDesign.sleeveShape)
                )
            }
        }
    }
}

internal fun atelierChartHue(slot: Int, global: Boolean, accent: Color): Color =
    if (global) accent else Color.hsv((slot * 30f + 18f) % 360f, 0.38f, 0.72f)

@Composable
internal fun AtelierTop100Shelf(
    charts: List<AppleDailyChart>,
    slots: List<Int>,
    copy: DailyTop100Copy,
    date: String,
    onOpen: (AppleDailyChart) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        AtelierSectionLabel(copy.title)
        Text(
            copy.subtitle, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 14.sp,
            color = AtelierDesign.textSecondary,
            modifier = Modifier.padding(horizontal = AtelierDesign.gutter).padding(bottom = 14.dp)
        )
        LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
            itemsIndexed(charts, key = { _, chart -> chart.playlistId }, contentType = { _, _ -> "atelier-top100" }) { index, chart ->
                val label = if (chart.global) copy.global else chart.label
                AtelierChartSleeve(
                    label, date, atelierChartHue(slots[index], chart.global, AtelierDesign.accent),
                    Modifier.semantics(mergeDescendants = true) { contentDescription = "${copy.title}, $label" }
                        .levyraPressable(onClick = { onOpen(chart) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                )
            }
        }
    }
}

@Composable
private fun AtelierChartSleeve(label: String, date: String, hue: Color, modifier: Modifier, side: Dp = AtelierChartSleeveSide) {
    Box(modifier.width(side * 1.2f).height(side)) {
        AtelierVinyl(null, Modifier.align(Alignment.CenterEnd).size(side * 0.94f).graphicsLayer { rotationZ = 24f })
        Box(
            Modifier.size(side)
                .graphicsLayer {
                    shadowElevation = 14.dp.toPx()
                    shape = AtelierDesign.sleeveShape
                    clip = true
                }
                .background(AtelierPaper)
                .drawWithCache {
                    val band = Brush.verticalGradient(listOf(hue, hue.copy(alpha = 0.82f)))
                    val ring = Stroke(1.dp.toPx())
                    onDrawBehind {
                        drawRect(band, topLeft = Offset(0f, size.height * 0.56f), size = Size(size.width, size.height * 0.44f))
                        drawCircle(AtelierInk, size.width * 0.12f, Offset(size.width * 0.84f, size.height * 0.17f), style = ring)
                        drawCircle(AtelierInk, size.width * 0.03f, Offset(size.width * 0.84f, size.height * 0.17f))
                    }
                }
                .padding(12.dp)
        ) {
            Text("TOP", fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = AtelierInk, modifier = Modifier.align(Alignment.TopStart))
            Text(
                "100", fontFamily = AtelierSerif, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 40.sp,
                color = AtelierInk, modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp)
            )
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(date, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp), color = AtelierInk.copy(alpha = 0.7f), maxLines = 1)
                Text(
                    atelierUppercase(label), fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = AtelierInk, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun AtelierTop100ChartScreen(
    open: DailyTop100ChartUiState,
    label: String,
    date: String,
    slot: Int,
    copy: DailyTop100Copy,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    BackHandler(onBack = onBack)
    val strings = LocalLevyraStrings.current
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
    val tracks = open.tracks
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hue = atelierChartHue(slot, open.chart.global, AtelierDesign.accent)
    LazyColumn(
        Modifier.fillMaxSize().background(AtelierDesign.room).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottom + 24.dp)
    ) {
        item(key = "atelier-top100-head", contentType = "atelier-top100-head") {
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.padding(start = 4.dp)) {
                    AtelierIconAction(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, onBack, AtelierDesign.text)
                }
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    AtelierChartSleeve(label, date, hue, Modifier.clearAndSetSemantics { }, side = 200.dp)
                }
                Text(
                    "${copy.title} · $label", fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp,
                    color = AtelierDesign.text, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter, vertical = 12.dp).semantics { heading() }
                )
                if (tracks.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AtelierHardwareKey(strings.play, lit = false, onClick = { onPlay(tracks, tracks.first()) }, modifier = Modifier.width(92.dp).height(54.dp)) {
                            AtelierPlayGlyph(false, 26.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        AtelierHardwareKey(
                            strings.shuffle,
                            lit = false,
                            onClick = {
                                val shuffled = tracks.shuffled()
                                onPlay(shuffled, shuffled.first())
                            },
                            modifier = Modifier.width(92.dp).height(54.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, null, tint = AtelierDesign.text, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
        when {
            tracks.isNotEmpty() -> itemsIndexed(tracks, key = { _, track -> track.id }, contentType = { _, _ -> "atelier-top100-row" }) { index, track ->
                AtelierTrackLine(
                    track, index + 1, false, Icons.Rounded.MoreVert, strings.options,
                    onPlay = { onPlay(tracks, track) },
                    onAction = { onTrackActions(track) }
                )
            }
            open.loading -> item(key = "atelier-top100-loading", contentType = "atelier-top100-status") {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    LevyraLoadingIndicator(color = AtelierDesign.accentBright, modifier = Modifier.size(36.dp))
                }
            }
            else -> item(key = "atelier-top100-failed", contentType = "atelier-top100-status") {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(copy.failed, style = MaterialTheme.typography.bodyLarge, color = AtelierDesign.textSecondary, textAlign = TextAlign.Center)
                    AtelierTextAction(copy.retry, onRetry)
                }
            }
        }
    }
}

private fun atelierBrowseOrder(kind: LevyraSearchBrowseSectionKind): Int = when (kind) {
    LevyraSearchBrowseSectionKind.Moods -> 0
    LevyraSearchBrowseSectionKind.Genres -> 1
    LevyraSearchBrowseSectionKind.Mixed -> 2
    LevyraSearchBrowseSectionKind.Picks -> 3
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
internal fun LazyListScope.atelierSearchBrowseSections(
    browse: LevyraSearchBrowse,
    samples: List<Track>,
    samplesTitle: String,
    samplePreview: Boolean,
    onOpenSample: (Int) -> Unit,
    onOpenZone: (ExploreZone) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit
) {
    val leadZones = browse.sections.firstOrNull()?.tiles.orEmpty()
        .takeWhile { it is LevyraSearchBrowseTile.Zone }
    if (samples.isNotEmpty()) {
        item(key = "atelier-browse-samples", contentType = "atelier-browse-samples") {
            Column {
                AtelierSectionLabel(samplesTitle)
                LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                    itemsIndexed(samples, key = { _, track -> "sample-${track.id}" }, contentType = { _, _ -> "atelier-sample" }) { index, track ->
                        AtelierSampleCard(track, samplePreview && index == 0) { onOpenSample(index) }
                    }
                }
            }
        }
    }
    var leadPlaced = false
    browse.sections.sortedBy { atelierBrowseOrder(it.kind) }.forEach { section ->
        val categories = section.tiles.filterIsInstance<LevyraSearchBrowseTile.Category>()
        val zones = if (section.kind == LevyraSearchBrowseSectionKind.Picks) {
            section.tiles.filterIsInstance<LevyraSearchBrowseTile.Zone>().filterNot { it in leadZones }
        } else {
            emptyList()
        }
        val grid = section.kind != LevyraSearchBrowseSectionKind.Moods && section.kind != LevyraSearchBrowseSectionKind.Picks
        val gridTiles: List<LevyraSearchBrowseTile> = if (grid && !leadPlaced) {
            leadPlaced = true
            leadZones + categories
        } else {
            categories
        }
        if (gridTiles.isEmpty() && categories.isEmpty() && zones.isEmpty()) return@forEach
        val title = section.providerTitle.ifBlank {
            if (section.kind == LevyraSearchBrowseSectionKind.Picks) browse.copy.levyraPicks else browse.copy.moodsAndGenres
        }
        item(key = "atelier-browse-head-${section.key}", contentType = "atelier-browse-head") { AtelierSectionLabel(title) }
        if (!grid) {
            item(key = "atelier-browse-rack-${section.key}", contentType = "atelier-browse-rack") {
                LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                    items(categories, key = { it.key }, contentType = { "atelier-mood-sleeve" }) { tile ->
                        val category = tile.category
                        LaunchedEffect(category.params) { browse.requestCategoryArtwork(category.params) }
                        AtelierMoodSleeve(category.title, browse.categoryArtwork[category.params].orEmpty()) { onOpenCategory(category) }
                    }
                    items(zones, key = { it.key }, contentType = { "atelier-mood-sleeve" }) { tile ->
                        AtelierMoodSleeve(tile.zone.label, rememberExploreMoodArtworkUrl(tile.zone)) { onOpenZone(tile.zone) }
                    }
                }
            }
        } else {
            gridTiles.chunked(2).forEach { row ->
                item(key = "atelier-browse-row-${section.key}-${row.first().key}", contentType = "atelier-browse-row") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { tile -> AtelierBrowseDivider(tile, browse, Modifier.weight(1f), onOpenZone, onOpenCategory) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
    if (!leadPlaced && leadZones.isNotEmpty()) {
        leadZones.chunked(2).forEach { row ->
            item(key = "atelier-browse-lead-${row.first().key}", contentType = "atelier-browse-row") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { tile -> AtelierBrowseDivider(tile, browse, Modifier.weight(1f), onOpenZone, onOpenCategory) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AtelierBrowseDivider(
    tile: LevyraSearchBrowseTile,
    browse: LevyraSearchBrowse,
    modifier: Modifier,
    onOpenZone: (ExploreZone) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit
) {
    when (tile) {
        is LevyraSearchBrowseTile.Category -> {
            val category = tile.category
            LaunchedEffect(category.params) { browse.requestCategoryArtwork(category.params) }
            AtelierCrateDivider(category.title, browse.categoryArtwork[category.params].orEmpty(), modifier) { onOpenCategory(category) }
        }
        is LevyraSearchBrowseTile.Zone -> {
            val zone = tile.zone
            val cover = browse.zoneCovers[zone.id].orEmpty().ifBlank { rememberExploreMoodArtworkUrl(zone) }
            AtelierCrateDivider(zone.label, cover, modifier) { onOpenZone(zone) }
        }
        LevyraSearchBrowseTile.Podcasts -> Unit
    }
}

@Composable
private fun AtelierSampleCard(track: Track, preview: Boolean, onOpen: () -> Unit) {
    Box(
        Modifier.width(132.dp).height(208.dp)
            .graphicsLayer {
                shadowElevation = 12.dp.toPx()
                shape = AtelierDesign.sleeveShape
                clip = true
            }
            .background(AtelierPaper)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = "${track.title}, ${track.artist}" }
            .padding(5.dp)
    ) {
        AtelierPrintImage(atelierCoverUrl(track), AtelierSleeveFilter, track.title.take(1).uppercase(), Modifier.fillMaxSize().atelierAgedPrint())
        if (preview) SpotifySamplePreview(track)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to AtelierInk.copy(alpha = 0.92f))))
        Box(
            Modifier.align(Alignment.TopEnd).padding(8.dp).size(30.dp).clip(CircleShape)
                .background(AtelierInk.copy(alpha = 0.6f)).border(1.dp, AtelierPaper.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) { AtelierPlayGlyph(false, 18.dp) }
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(
                track.title, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp,
                color = AtelierPaper, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = AtelierPaper.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AtelierMoodSleeve(title: String, artwork: String, onClick: () -> Unit) {
    Column(
        Modifier.width(AtelierMoodSleeveSide)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = title }
    ) {
        Box(Modifier.size(AtelierMoodSleeveSide)) {
            AtelierVinyl(null, Modifier.align(Alignment.TopCenter).offset(y = (-6).dp).size(AtelierMoodSleeveSide * 0.84f))
            Box(
                Modifier.align(Alignment.BottomCenter).size(AtelierMoodSleeveSide * 0.9f)
                    .graphicsLayer {
                        shadowElevation = 12.dp.toPx()
                        shape = AtelierDesign.sleeveShape
                        clip = true
                    }
                    .background(AtelierPaper)
                    .padding(5.dp)
            ) {
                AtelierPrintImage(artwork, AtelierSleeveFilter, title.take(1).uppercase(), Modifier.fillMaxSize().atelierAgedPrint())
            }
        }
        Text(
            title, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AtelierDesign.text,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp).clearAndSetSemantics { }
        )
    }
}

@Composable
private fun AtelierCrateDivider(title: String, artwork: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(96.dp)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = title }
    ) {
        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().height(80.dp).clip(AtelierDesign.cardShape)
                .background(AtelierDesign.cabinet).border(1.dp, AtelierDesign.hairline, AtelierDesign.cardShape)
        ) {
            AtelierPrintImage(
                artwork, AtelierSleeveFilter, title.take(1).uppercase(),
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().aspectRatio(1f)
            )
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(0.35f to AtelierDesign.cabinet, 0.8f to Color.Transparent)))
            Text(
                title, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp, color = AtelierDesign.text,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 14.dp, end = 64.dp).clearAndSetSemantics { }
            )
        }
        Box(
            Modifier.padding(start = 12.dp).width(46.dp).height(20.dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(AtelierPaper)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Text(title.take(1).uppercase(), fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AtelierInk)
        }
    }
}

private val AtelierCuratedSleeveSide = 168.dp

internal fun LazyListScope.atelierHomeCurated(shelves: List<YoutubeMusicHomeShelf>, leadTitle: String, onOpen: (PlaylistHit) -> Unit) {
    shelves.forEachIndexed { index, shelf ->
        val tiles = shelf.items.distinctBy { it.key }.filter { it.playlist != null }
        if (tiles.isEmpty()) return@forEachIndexed
        item(key = "atelier-curated-${shelf.key}", contentType = "atelier-curated") {
            Column {
                AtelierSectionLabel(if (index == 0) leadTitle else shelf.title)
                LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                    items(tiles, key = { it.key }, contentType = { "atelier-curated-sleeve" }) { tile ->
                        Column(
                            Modifier.width(AtelierCuratedSleeveSide)
                                .levyraPressable(onClick = { tile.playlist?.let(onOpen) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                                .semantics(mergeDescendants = true) { }
                        ) {
                            Box(
                                Modifier.size(AtelierCuratedSleeveSide)
                                    .graphicsLayer {
                                        shadowElevation = 12.dp.toPx()
                                        shape = AtelierDesign.sleeveShape
                                        clip = true
                                    }
                                    .background(AtelierPaper)
                                    .padding(5.dp)
                            ) {
                                AtelierArtwork(tile.thumbnailUrl, Modifier.clearAndSetSemantics { })
                            }
                            Text(
                                tile.title, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 19.sp,
                                color = AtelierDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp)
                            )
                            val subtitle = monoWithoutYoutube(tile.subtitle)
                            if (subtitle.isNotBlank()) {
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val AtelierFilmFrameWidth = 248.dp

internal fun LazyListScope.atelierHomeVideos(title: String, videos: List<YoutubeMusicHomeItem>, currentId: String?, onPlay: (Track) -> Unit) {
    if (videos.isEmpty()) return
    item(key = "atelier-new-videos", contentType = "atelier-film-strip") {
        Column {
            AtelierSectionLabel(title)
            LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(videos, key = { it.key }, contentType = { "atelier-film-frame" }) { item ->
                    val track = item.track ?: return@items
                    AtelierFilmFrame(item.thumbnailUrl, track, track.id == currentId) { onPlay(track) }
                }
            }
        }
    }
}

private fun Modifier.atelierFilmPerforation(): Modifier = drawWithCache {
    val hole = Size(9.dp.toPx(), 6.dp.toPx())
    val gap = 16.dp.toPx()
    val inset = 5.dp.toPx()
    val radius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
    val holeColor = AtelierPaper.copy(alpha = 0.82f)
    onDrawBehind {
        drawRect(Color(0xFF0B0B0D))
        var x = gap / 2f
        while (x < size.width) {
            drawRoundRect(holeColor, Offset(x, inset), hole, radius)
            drawRoundRect(holeColor, Offset(x, size.height - inset - hole.height), hole, radius)
            x += gap
        }
    }
}

@Composable
private fun AtelierFilmFrame(thumbnail: String, track: Track, current: Boolean, onPlay: () -> Unit) {
    Column(
        Modifier.width(AtelierFilmFrameWidth)
            .levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = "${track.title}, ${track.artist}" }
    ) {
        Box(Modifier.fillMaxWidth().atelierFilmPerforation().padding(vertical = 17.dp, horizontal = 3.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(3.dp)).background(AtelierDesign.panel)) {
                AsyncImage(thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().atelierAgedPrint())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))))
                Box(
                    Modifier.align(Alignment.Center).size(46.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
                        .border(1.5.dp, AtelierPaper.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { AtelierPlayGlyph(current, 24.dp) }
            }
        }
        Text(
            track.title, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 19.sp,
            color = if (current) AtelierDesign.accentBright else AtelierDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp)
        )
        Text(
            track.artist, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
