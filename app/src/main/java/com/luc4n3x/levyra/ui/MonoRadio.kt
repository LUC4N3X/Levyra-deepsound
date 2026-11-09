package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.MonoDesign
import java.util.Locale

@Composable
internal fun MonoRadioHero(live: String, subtitle: String, query: String, searchHint: String, onQueryChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(MonoDesign.cardShape).background(MonoDesign.surface)
                .border(1.dp, MonoDesign.hairline, MonoDesign.cardShape).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = MonoDesign.textSecondary, modifier = Modifier.size(20.dp))
            Box(Modifier.weight(1f).padding(start = 10.dp), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(searchHint, style = MaterialTheme.typography.bodyLarge, color = MonoDesign.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { })
                }
                BasicTextField(
                    value = query, onValueChange = onQueryChange, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MonoDesign.text),
                    cursorBrush = SolidColor(MonoDesign.accent),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = searchHint }
                )
            }
        }
    }
}

@Composable
internal fun MonoRadioAddAction(label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter - 8.dp), horizontalArrangement = Arrangement.End) {
        Row(
            Modifier.heightIn(min = 48.dp).clip(MonoDesign.pillShape)
                .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.Add, null, tint = MonoDesign.textSecondary, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MonoDesign.textSecondary)
        }
    }
}

@Composable
internal fun MonoRadioSectionTitle(title: String) {
    Text(
        title, style = MaterialTheme.typography.titleLarge, color = MonoDesign.text,
        modifier = Modifier.padding(start = MonoDesign.gutter, end = MonoDesign.gutter, top = 22.dp, bottom = 10.dp).semantics { heading() }
    )
}

@Composable
internal fun MonoRadioDirectoryButton(modifier: Modifier, label: String, icon: (@Composable () -> Unit)?, onClick: () -> Unit) {
    Row(
        modifier.heightIn(min = 48.dp).clip(MonoDesign.pillShape).border(1.dp, MonoDesign.hairlineStrong, MonoDesign.pillShape)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon?.invoke()
        Text(label, style = MaterialTheme.typography.labelLarge, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MonoDesign.textSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun monoRadioDetail(station: RadioStation): String {
    val languageCode = LocalLevyraStrings.current.code
    return remember(station.uuid, languageCode) {
        val place = station.countryCode.takeIf(String::isNotBlank)?.let { localizedCountryName(it, languageCode) }?.takeIf(String::isNotBlank) ?: station.country
        listOfNotNull(place.takeIf(String::isNotBlank), station.tags.firstOrNull(), station.qualityLabel.takeIf(String::isNotBlank)).joinToString(" · ")
    }
}

@Composable
private fun MonoRadioStar(favorite: Boolean, addLabel: String, removeLabel: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarOutline, if (favorite) removeLabel else addLabel,
            tint = if (favorite) MonoDesign.accent else MonoDesign.textSecondary, modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
internal fun MonoRadioFeatured(
    station: RadioStation, favorite: Boolean, playing: Boolean, live: String, playLabel: String,
    addFavoriteLabel: String, removeFavoriteLabel: String, onPlay: () -> Unit, onFavorite: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter, vertical = 6.dp).monoPanel(MonoDesign.panelShape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RadioArtwork(station, 88.dp, MonoDesign.artShape)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MonoOverline(live, color = MonoDesign.accent)
                Text(station.name, style = MaterialTheme.typography.titleLarge, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val detail = monoRadioDetail(station)
                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MonoWaveLine(playing, Modifier.weight(1f).height(22.dp).clearAndSetSemantics { }, color = MonoDesign.accent, cycles = 10f, strokeWidth = 1.5.dp)
            MonoRadioStar(favorite, addFavoriteLabel, removeFavoriteLabel, onFavorite)
            Box(
                Modifier.size(56.dp).clip(rememberLevyraPlayMorphShape(playing = playing, animated = LocalAnimationsEnabled.current))
                    .background(MonoDesign.accent)
                    .levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Control, onClickLabel = playLabel),
                contentAlignment = Alignment.Center
            ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, playLabel, tint = MonoDesign.onAccent, modifier = Modifier.size(26.dp)) }
        }
    }
}

@Composable
internal fun MonoRadioRow(
    station: RadioStation, favorite: Boolean, playing: Boolean, live: String, playLabel: String,
    addFavoriteLabel: String, removeFavoriteLabel: String, onPlay: () -> Unit, onFavorite: () -> Unit, actions: (@Composable () -> Unit)?
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp)
            .drawBehind {
                if (playing) {
                    drawRect(Brush.horizontalGradient(listOf(MonoDesign.accent.copy(alpha = 0.14f), Color.Transparent)))
                    drawRect(MonoDesign.accent, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
                }
            }
            .levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Row, onClickLabel = playLabel)
            .padding(start = MonoDesign.gutter, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp)) {
            RadioArtwork(station, 52.dp, MonoDesign.artShape)
            if (playing) {
                Box(Modifier.size(52.dp).clip(MonoDesign.artShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    MonoWaveLine(true, Modifier.width(30.dp).height(14.dp), color = MonoDesign.accent, cycles = 3f, strokeWidth = 1.5.dp)
                }
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            RadioStationName(
                name = station.name,
                playing = playing,
                color = if (playing) MonoDesign.accent else MonoDesign.text,
                fontSize = MaterialTheme.typography.titleSmall.fontSize,
                style = MaterialTheme.typography.titleSmall,
                minLines = 1
            )
            val detail = if (playing) live else monoRadioDetail(station)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        MonoRadioStar(favorite, addFavoriteLabel, removeFavoriteLabel, onFavorite)
        actions?.invoke()
    }
}

@Composable
internal fun MonoRadioRail(
    stations: List<RadioStation>, favoriteIds: Set<String>, currentStationId: String?, isPlaying: Boolean,
    playLabel: String, onPlay: (RadioStation) -> Unit
) {
    LazyRow(contentPadding = PaddingValues(horizontal = MonoDesign.gutter), horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
        items(stations, key = { "mono-rail-${it.uuid}" }) { station ->
            val playing = currentStationId == station.uuid && isPlaying
            val favorite = station.uuid.lowercase(Locale.ROOT) in favoriteIds
            Column(
                Modifier.width(MonoDesign.card + 16.dp)
                    .levyraPressable(onClick = { onPlay(station) }, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = playLabel)
            ) {
                Box {
                    RadioArtwork(station, MonoDesign.card + 16.dp, MonoDesign.artShape)
                    if (favorite) {
                        Icon(Icons.Rounded.Star, null, tint = MonoDesign.accent, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp))
                    }
                    if (playing) {
                        MonoWaveLine(true, Modifier.align(Alignment.BottomCenter).padding(8.dp).fillMaxWidth().height(14.dp), color = MonoDesign.accent, cycles = 5f, strokeWidth = 1.5.dp)
                    }
                }
                RadioStationName(
                    name = station.name,
                    playing = playing,
                    color = MonoDesign.text,
                    fontSize = MaterialTheme.typography.titleSmall.fontSize,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                val detail = monoRadioDetail(station)
                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun monoFrequency(index: Int): String = String.format(Locale.ROOT, "%.1f", 88.1f + index * 0.4f)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun MonoRadioTuner(
    stations: List<RadioStation>, favoriteIds: Set<String>, currentStationId: String?, isPlaying: Boolean,
    live: String, playLabel: String, addFavoriteLabel: String, removeFavoriteLabel: String,
    onPlay: (RadioStation) -> Unit, onFavorite: (RadioStation) -> Unit
) {
    if (stations.isEmpty()) return
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val haptics = com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics.current
    val tunedIndex by androidx.compose.runtime.remember(listState) {
        androidx.compose.runtime.derivedStateOf {
            val layout = listState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - center) }?.index ?: 0
        }
    }
    androidx.compose.runtime.LaunchedEffect(tunedIndex) {
        if (listState.isScrollInProgress) haptics.perform(com.luc4n3x.levyra.ui.theme.LevyraHapticAction.SeekSnap)
    }
    val station = stations[tunedIndex.coerceIn(0, stations.lastIndex)]
    val playing = currentStationId == station.uuid && isPlaying
    val favorite = station.uuid.lowercase(Locale.ROOT) in favoriteIds
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.padding(10.dp)) {
                GlacierSignal(playing) { RadioArtwork(station, 84.dp, CircleShape) }
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(monoFrequency(tunedIndex), style = MaterialTheme.typography.headlineMedium.copy(fontFamily = com.luc4n3x.levyra.ui.theme.MonoCodeFont), color = MonoDesign.accent, maxLines = 1)
                    MonoOverline("FM", Modifier.padding(bottom = 4.dp), color = MonoDesign.textSecondary)
                }
                Text(station.name, style = MaterialTheme.typography.titleLarge, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val detail = monoRadioDetail(station)
                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MonoRadioStar(favorite, addFavoriteLabel, removeFavoriteLabel) { onFavorite(station) }
            Box(
                Modifier.size(52.dp).clip(rememberLevyraPlayMorphShape(playing = playing, animated = LocalAnimationsEnabled.current))
                    .background(MonoDesign.accent)
                    .levyraPressable(onClick = { onPlay(station) }, role = Role.Button, pressedScale = LevyraPressScale.Control, onClickLabel = playLabel),
                contentAlignment = Alignment.Center
            ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, playLabel, tint = MonoDesign.onAccent, modifier = Modifier.size(24.dp)) }
        }
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 18.dp).height(74.dp)) {
            val side = (maxWidth - MonoTunerSlot) / 2
            androidx.compose.foundation.lazy.LazyRow(
                state = listState,
                flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState),
                contentPadding = PaddingValues(horizontal = side),
                modifier = Modifier.fillMaxSize().semantics { contentDescription = station.name }
            ) {
                items(stations.size, key = { stations[it].uuid }) { index ->
                    val marked = stations[index].uuid.lowercase(Locale.ROOT) in favoriteIds || stations[index].uuid == currentStationId
                    Box(Modifier.width(MonoTunerSlot).fillMaxHeight()) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                            val ticks = 8
                            val step = size.width / ticks
                            for (t in 0 until ticks) {
                                val x = step * t + step / 2f
                                val major = t == ticks / 2
                                val h = if (major) size.height * 0.42f else size.height * 0.2f
                                drawLine(
                                    if (major) MonoDesign.textSecondary else MonoDesign.hairlineStrong,
                                    Offset(x, size.height * 0.62f - h), Offset(x, size.height * 0.62f), strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx()
                                )
                            }
                            if (marked) drawCircle(MonoDesign.accent, 3.dp.toPx(), Offset(size.width / 2f + step / 2f, size.height * 0.08f))
                        }
                        Text(
                            monoFrequency(index), style = MaterialTheme.typography.labelSmall, color = MonoDesign.textFaint,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(start = MonoTunerSlot / 8).clearAndSetSemantics { }
                        )
                    }
                }
            }
            Box(
                Modifier.align(Alignment.TopCenter).padding(start = MonoTunerSlot / 8).width(2.dp).height(52.dp)
                    .clip(MonoDesign.pillShape).background(MonoDesign.accent)
            )
            Box(Modifier.matchParentSize().drawBehind {
                drawRect(Brush.horizontalGradient(0f to MonoDesign.base, 0.2f to Color.Transparent, 0.8f to Color.Transparent, 1f to MonoDesign.base))
            })
        }
    }
}

private val MonoTunerSlot = 88.dp

@Composable
private fun GlacierSignal(playing: Boolean, content: @Composable () -> Unit) {
    val active = playing && LocalAnimationsEnabled.current
    val wave = if (active) {
        androidx.compose.animation.core.rememberInfiniteTransition(label = "glacier-signal").animateFloat(
            0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2400, easing = androidx.compose.animation.core.LinearEasing)),
            label = "glacier-signal-wave"
        )
    } else null
    Box(
        Modifier.drawBehind {
            val base = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            if (wave != null) {
                for (ring in 0 until 3) {
                    val t = (wave.value + ring / 3f) % 1f
                    drawCircle(MonoDesign.accent.copy(alpha = 0.45f * (1f - t)), base * (1.05f + 0.55f * t), center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                }
            } else {
                drawCircle(MonoDesign.hairlineStrong, base * 1.12f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
        },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
internal fun MonoRadioGlow() {
    Box(
        Modifier.fillMaxWidth().height(MonoRadioGlowHeight)
            .drawBehind {
                drawRect(Brush.verticalGradient(0f to MonoDesign.accent.copy(alpha = 0.24f), 0.45f to MonoDesign.accent.copy(alpha = 0.08f), 1f to Color.Transparent))
            }
    )
}

private val MonoRadioGlowHeight = 520.dp
