package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.MonoOverline
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.MonoCodeFont
import com.luc4n3x.levyra.ui.theme.MonoDesign

@Composable
private fun glacierLoop(periodMillis: Int, reverse: Boolean): State<Float> {
    if (!LocalAnimationsEnabled.current) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "glacier-loop").animateFloat(
        0f, 1f,
        infiniteRepeatable(
            tween(periodMillis, easing = if (reverse) FastOutSlowInEasing else LinearEasing),
            if (reverse) RepeatMode.Reverse else RepeatMode.Restart
        ),
        label = "glacier-loop-value"
    )
}

@Composable
private fun GlacierCoverFan(tracks: List<Track>, size: Dp, sway: State<Float>, modifier: Modifier) {
    val shown = remember(tracks) { tracks.distinctBy { it.thumbnailUrl }.take(3) }
    Box(modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        if (shown.isEmpty()) {
            repeat(3) { index ->
                val offset = index - 1f
                Box(
                    Modifier.size(size).graphicsLayer {
                        rotationZ = offset * (9f + 3f * sway.value)
                        translationX = offset * size.toPx() * 0.42f
                    }.border(1.dp, MonoDesign.hairlineStrong, MonoDesign.artShape)
                )
            }
        } else {
            shown.forEachIndexed { index, track ->
                val offset = index - (shown.size - 1) / 2f
                Box(
                    Modifier.size(size).graphicsLayer {
                        rotationZ = offset * (9f + 3f * sway.value)
                        translationX = offset * size.toPx() * (0.42f + 0.06f * sway.value)
                        translationY = kotlin.math.abs(offset) * 4.dp.toPx()
                        shadowElevation = 8.dp.toPx()
                        shape = MonoDesign.artShape
                        clip = true
                    }.background(MonoDesign.raised)
                ) { CoverImage(track, Modifier.fillMaxSize()) }
            }
        }
    }
}

@Composable
internal fun GlacierFavoritesHero(favorites: List<Track>, onOpen: () -> Unit, onPlay: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val sway = glacierLoop(3600, reverse = true)
    val glow = glacierLoop(5200, reverse = true)
    Row(
        Modifier.fillMaxWidth().height(156.dp)
            .clip(MonoDesign.panelShape)
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(MonoDesign.raised, MonoDesign.surface), Offset.Zero, Offset(size.width, size.height)))
                drawRect(
                    Brush.radialGradient(
                        listOf(MonoDesign.accent.copy(alpha = 0.14f + 0.08f * glow.value), Color.Transparent),
                        center = Offset(size.width * 0.82f, size.height * 0.4f), radius = size.width * 0.7f
                    )
                )
            }
            .border(1.dp, MonoDesign.hairline, MonoDesign.panelShape)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface, onClickLabel = strings.favorites)
            .padding(start = 20.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MonoOverline(strings.favoritesPlain, color = MonoDesign.accent)
            Text(strings.favorites, style = MaterialTheme.typography.headlineMedium, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(strings.formatTrackCount(favorites.size), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoCodeFont), color = MonoDesign.textSecondary)
        }
        GlacierCoverFan(favorites, 66.dp, sway, Modifier.width(118.dp).height(100.dp))
        if (favorites.isNotEmpty()) {
            Box(
                Modifier.padding(start = 6.dp).size(48.dp)
                    .clip(rememberLevyraPlayMorphShape(playing = false, animated = LocalAnimationsEnabled.current))
                    .background(MonoDesign.accent)
                    .levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Control, onClickLabel = strings.play),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.PlayArrow, strings.play, tint = MonoDesign.onAccent, modifier = Modifier.size(22.dp)) }
        }
    }
}

@Composable
internal fun GlacierSmartRow(recent: List<Track>, mostPlayed: List<Track>, onOpenRecent: () -> Unit, onOpenMostPlayed: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlacierSlab(Icons.Rounded.History, strings.recent, strings.formatTrackCount(recent.size), recent, onOpenRecent, Modifier.weight(1f))
        GlacierSlab(Icons.Rounded.Replay, strings.pulsePlays, strings.formatTrackCount(mostPlayed.size), mostPlayed, onOpenMostPlayed, Modifier.weight(1f))
    }
}

@Composable
private fun GlacierSlab(icon: ImageVector, title: String, detail: String, tracks: List<Track>, onClick: () -> Unit, modifier: Modifier) {
    val covers = remember(tracks) { tracks.distinctBy { it.thumbnailUrl }.filter { it.thumbnailUrl.isNotBlank() }.take(4) }
    Box(
        modifier.height(GlacierSlabHeight).clip(MonoDesign.panelShape).background(MonoDesign.surface)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = title)
    ) {
        if (covers.isNotEmpty()) {
            Column(Modifier.fillMaxSize().graphicsLayer { alpha = 0.55f }) {
                covers.chunked(2).let { rows -> if (rows.size == 1) rows + listOf(rows[0].reversed()) else rows }.forEach { row ->
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        (if (row.size == 1) row + row else row).forEach { track -> CoverImage(track, Modifier.weight(1f).fillMaxSize()) }
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(Brush.verticalGradient(0f to MonoDesign.base.copy(alpha = 0.15f), 0.55f to MonoDesign.base.copy(alpha = 0.78f), 1f to MonoDesign.base.copy(alpha = 0.96f)))
                drawRect(Brush.radialGradient(listOf(MonoDesign.accent.copy(alpha = 0.22f), Color.Transparent), center = Offset(size.width, 0f), radius = size.width))
                val step = 14.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(Color.White.copy(alpha = 0.035f), Offset(x, size.height), Offset(x + size.height, 0f), 1.dp.toPx())
                    x += step
                }
                drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.16f), 1f to Color.Transparent), size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
            }.border(1.dp, MonoDesign.hairlineStrong, MonoDesign.panelShape)
        )
        Box(
            Modifier.padding(12.dp).size(34.dp).clip(CircleShape).background(MonoDesign.base.copy(alpha = 0.7f)).border(1.dp, MonoDesign.hairlineStrong, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = MonoDesign.accent, modifier = Modifier.size(18.dp)) }
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoCodeFont), color = MonoDesign.accent, maxLines = 1)
        }
    }
}

private val GlacierSlabHeight = 132.dp
private val GlacierLeadingCount = Regex("""^\s*([0-9][0-9.,   ]*)\s*(.*)$""")

private class GlacierStratum(val entry: LibrarySectionEntry, val count: String, val unit: String, val value: Long)

private fun glacierStratum(entry: LibrarySectionEntry): GlacierStratum {
    val match = GlacierLeadingCount.find(entry.summary) ?: return GlacierStratum(entry, "", entry.summary, -1L)
    val number = match.groupValues[1].trim()
    return GlacierStratum(entry, number, match.groupValues[2].trim(), number.filter(Char::isDigit).toLongOrNull() ?: 0L)
}

@Composable
internal fun GlacierSectionGrid(entries: List<LibrarySectionEntry>, onOpen: (LibraryCategory) -> Unit) {
    val strings = LocalLevyraStrings.current
    val strata = remember(entries) { entries.map(::glacierStratum) }
    val peak = remember(strata) { strata.maxOfOrNull { it.value }?.coerceAtLeast(1L) ?: 1L }
    val animated = LocalAnimationsEnabled.current
    val grow = remember { androidx.compose.animation.core.Animatable(if (animated) 0f else 1f) }
    androidx.compose.runtime.LaunchedEffect(animated) { if (animated) grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) else grow.snapTo(1f) }
    Column(
        Modifier.fillMaxWidth().padding(top = 18.dp).clip(MonoDesign.panelShape)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(MonoDesign.raised, MonoDesign.surface)))
                drawRect(Brush.radialGradient(listOf(MonoDesign.accent.copy(alpha = 0.10f), Color.Transparent), center = Offset(0f, 0f), radius = size.width))
            }
            .border(1.dp, MonoDesign.hairline, MonoDesign.panelShape)
            .padding(vertical = 6.dp)
    ) {
        strata.forEachIndexed { index, stratum ->
            val label = stratum.entry.category.libraryLabel(strings)
            if (index > 0) Box(Modifier.padding(start = 70.dp).fillMaxWidth().height(1.dp).background(MonoDesign.hairline))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 68.dp)
                    .levyraPressable(onClick = { onOpen(stratum.entry.category) }, role = Role.Button, pressedScale = LevyraPressScale.Row, onClickLabel = label)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(MonoDesign.accent.copy(alpha = 0.22f), MonoDesign.raisedHigh)))
                        .border(1.dp, MonoDesign.accent.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(stratum.entry.category.sectionIcon(), null, tint = MonoDesign.accent, modifier = Modifier.size(20.dp)) }
                Column(Modifier.weight(1f).padding(start = 14.dp, end = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(label, style = MaterialTheme.typography.titleMedium, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (stratum.value >= 0) {
                        val share = (stratum.value.toFloat() / peak).coerceIn(0f, 1f)
                        Box(Modifier.fillMaxWidth().height(3.dp).clip(MonoDesign.pillShape).background(MonoDesign.hairline)) {
                            Box(
                                Modifier.fillMaxWidth((0.04f + 0.96f * share) * grow.value).height(3.dp).clip(MonoDesign.pillShape)
                                    .background(Brush.horizontalGradient(listOf(MonoDesign.accentDim, MonoDesign.accent, Color.White.copy(alpha = 0.9f))))
                            )
                        }
                    } else {
                        Text(stratum.unit, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (stratum.count.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stratum.count, style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoCodeFont), color = if (stratum.value > 0) MonoDesign.text else MonoDesign.textFaint, maxLines = 1)
                        Text(stratum.unit, style = MaterialTheme.typography.labelSmall, color = MonoDesign.textFaint, maxLines = 1)
                    }
                } else {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MonoDesign.textSecondary, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
internal fun GlacierLibraryOverview(
    favorites: List<Track>,
    recent: List<Track>,
    mostPlayed: List<Track>,
    entries: List<LibrarySectionEntry>,
    pulse: com.luc4n3x.levyra.domain.ListeningPulse,
    onOpenCollection: (String) -> Unit,
    onPlayTracks: (List<Track>) -> Unit,
    onOpenCategory: (LibraryCategory) -> Unit,
    onOpenInsights: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        GlacierFavoritesHero(favorites, { onOpenCollection(SMART_COLLECTION_FAVORITES) }) { onPlayTracks(favorites) }
        GlacierSmartRow(recent, mostPlayed, { onOpenCollection(SMART_COLLECTION_RECENT) }) { onOpenCollection(SMART_COLLECTION_MOST_PLAYED) }
        GlacierSectionGrid(entries, onOpenCategory)
        Spacer(Modifier.height(8.dp))
    }
}
