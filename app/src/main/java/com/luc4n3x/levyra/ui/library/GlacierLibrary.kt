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
        GlacierSmartCard(Icons.Rounded.History, strings.recent, strings.formatTrackCount(recent.size), recent, onOpenRecent, Modifier.weight(1f))
        GlacierSmartCard(Icons.Rounded.Replay, strings.pulsePlays, strings.formatTrackCount(mostPlayed.size), mostPlayed, onOpenMostPlayed, Modifier.weight(1f))
    }
}

@Composable
private fun GlacierSmartCard(icon: ImageVector, title: String, detail: String, tracks: List<Track>, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier.height(76.dp).clip(MonoDesign.cardShape).background(MonoDesign.surface)
            .border(1.dp, MonoDesign.hairline, MonoDesign.cardShape)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = title)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val cover = tracks.firstOrNull()
        Box(Modifier.size(48.dp).clip(MonoDesign.artShape).background(MonoDesign.raised), contentAlignment = Alignment.Center) {
            if (cover != null) CoverImage(cover, Modifier.fillMaxSize()) else Icon(icon, null, tint = MonoDesign.accent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoCodeFont), color = MonoDesign.textSecondary, maxLines = 1)
        }
    }
}

@Composable
internal fun GlacierSectionGrid(entries: List<LibrarySectionEntry>, onOpen: (LibraryCategory) -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        entries.chunked(2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { columnIndex, entry ->
                    val label = entry.category.libraryLabel(strings)
                    val wide = row.size == 1
                    Column(
                        Modifier.weight(1f).heightIn(min = 104.dp).clip(MonoDesign.cardShape).background(MonoDesign.surface)
                            .border(1.dp, MonoDesign.hairline, MonoDesign.cardShape)
                            .levyraPressable(onClick = { onOpen(entry.category) }, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = label)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(MonoDesign.cardShape).background(MonoDesign.raised),
                            contentAlignment = Alignment.Center
                        ) { Icon(entry.category.sectionIcon(), null, tint = MonoDesign.accent, modifier = Modifier.size(18.dp)) }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(label, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(entry.summary, style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoCodeFont), color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
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
