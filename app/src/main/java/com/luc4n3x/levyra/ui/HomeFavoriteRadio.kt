package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import com.luc4n3x.levyra.ui.i18n.radioLibraryCopy
import com.luc4n3x.levyra.ui.theme.*

internal data class HomeRadioState(
    val stations: List<RadioStation> = emptyList(),
    val currentId: String? = null,
    val playing: Boolean = false,
    val visible: Boolean = true,
    val onPlay: (RadioStation) -> Unit = {}
)

internal val LocalHomeRadio = staticCompositionLocalOf { HomeRadioState() }

internal fun LazyListScope.favoriteRadioSection(radio: HomeRadioState) {
    if (radio.visible && radio.stations.isNotEmpty()) {
        item(key = "home-favorite-radio", contentType = "home-favorite-radio") { HomeFavoriteRadio(radio) }
    }
}

@Composable
private fun HomeFavoriteRadio(radio: HomeRadioState) {
    val strings = LocalLevyraStrings.current
    val title = radioLibraryCopy(strings.code).favorites
    val inset = when { LevyraIsAppleMusic -> AppleMusicDesign.gutter; LevyraIsYouTubeMusic -> YouTubeMusicDesign.gutter; else -> 24.dp }
    Column(verticalArrangement = Arrangement.spacedBy(if (LevyraSkinManagesLayout) 8.dp else 12.dp)) {
        if (LevyraSkinManagesLayout && !LevyraIsAppleMusic && !LevyraIsYouTubeMusic) SkinSectionTitle(title, inset)
        else Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LevyraText,
            modifier = Modifier.padding(horizontal = inset).semantics { heading() })
        when {
            LevyraIsNocturne || LevyraIsTorque || LevyraIsIvory -> LazyRow(
                contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(radio.stations.chunked(3), key = { it.first().uuid }) { group ->
                    Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        group.forEach { station -> CompactHomeRadio(station, radio) }
                    }
                }
            }
            else -> LazyRow(contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(if (LevyraIsYouTubeMusic) 16.dp else 12.dp)) {
                items(radio.stations, key = RadioStation::uuid) { station ->
                    if (LevyraSkinManagesLayout) SkinHomeRadio(station, radio) else ExpressiveHomeRadio(station, radio)
                }
            }
        }
    }
}

@Composable
private fun ExpressiveHomeRadio(station: RadioStation, radio: HomeRadioState) {
    val active = radio.currentId == station.uuid
    val playing = active && radio.playing
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(topStart = 36.dp, topEnd = 20.dp, bottomEnd = 36.dp, bottomStart = 20.dp)
    Row(
        Modifier.width(308.dp).heightIn(min = 176.dp).clip(shape)
            .background(lerp(colors.surfaceContainer, colors.primaryContainer, if (active) 0.32f else 0.08f))
            .border(1.dp, if (active) colors.primary.copy(alpha = 0.45f) else colors.outlineVariant.copy(alpha = 0.45f), shape)
            .semantics { selected = active }.levyraPressable(onClick = { radio.onPlay(station) }, role = Role.Button)
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(112.dp).clip(CircleShape).background(colors.surfaceContainerLowest)
            .border(1.dp, colors.outlineVariant.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
            RadioArtwork(station, 96.dp, CircleShape)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Radio, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                Text(LevyraLiveRadioCatalog.forCode(LocalLevyraStrings.current.code).live, color = colors.primary, style = MaterialTheme.typography.labelMedium)
            }
            Text(station.name, color = colors.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioTuningScale(Modifier.weight(1f), active)
                RadioHomePlay(playing)
            }
        }
    }
}

@Composable
private fun RadioTuningScale(modifier: Modifier, active: Boolean) {
    val ticks = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val marker = MaterialTheme.colorScheme.primary
    Canvas(modifier.height(24.dp)) {
        val stroke = 1.5.dp.toPx()
        repeat(9) { index ->
            val x = stroke + (size.width - stroke * 2) * index / 8
            val height = if (index % 4 == 0) size.height * 0.6f else size.height * 0.3f
            drawLine(ticks, Offset(x, (size.height - height) / 2), Offset(x, (size.height + height) / 2), stroke, StrokeCap.Round)
        }
        if (active) drawLine(marker, Offset(size.width * 0.65f, 0f), Offset(size.width * 0.65f, size.height), 3.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun SkinHomeRadio(station: RadioStation, radio: HomeRadioState) {
    val size = if (LevyraIsAppleMusic) 168.dp else if (LevyraIsYouTubeMusic) 148.dp else 156.dp
    val shape: Shape = if (LevyraIsSpotify) RoundedCornerShape(SpotifyDesign.cornerSmall) else PrismDesign.cut(PrismDesign.artworkCorner)
    val active = radio.currentId == station.uuid
    Column(Modifier.width(size).semantics { selected = active }.levyraPressable(onClick = { radio.onPlay(station) }, role = Role.Button), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            RadioArtwork(station, size, shape)
            RadioHomePlay(active && radio.playing, Modifier.align(Alignment.BottomEnd).padding(8.dp))
        }
        Text(station.name, color = LevyraText, style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (LevyraIsYouTubeMusic || LevyraIsSpotify) FontWeight.Bold else FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CompactHomeRadio(station: RadioStation, radio: HomeRadioState) {
    val active = radio.currentId == station.uuid
    val shape = when { LevyraIsNocturne -> CircleShape; LevyraIsTorque -> RoundedCornerShape(2.dp); else -> RoundedCornerShape(IvoryDesign.coverCorner) }
    Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).clip(shape).background(LevyraPanel)
        .semantics { selected = active }.levyraPressable(onClick = { radio.onPlay(station) }, role = Role.Button).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RadioArtwork(station, 56.dp, shape)
        Text(station.name, color = if (active) LevyraCyan else LevyraText, style = MaterialTheme.typography.titleSmall,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        RadioHomePlay(active && radio.playing)
    }
}

@Composable
private fun RadioHomePlay(playing: Boolean, modifier: Modifier = Modifier) {
    val shape = if (LevyraSkinManagesLayout) CircleShape else rememberLevyraPlayMorphShape(playing, animated = LocalAnimationsEnabled.current)
    Box(modifier.size(48.dp).clip(shape).background(LevyraCyan), contentAlignment = Alignment.Center) {
        Icon(if (playing) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary)
    }
}
