package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Nightlife
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.feature.radio.RadioCategory
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import kotlin.math.PI
import kotlin.math.sin

private const val AirwaveTwoPi = (2 * PI).toFloat()

@Composable
internal fun DefaultRadioAirwave(color: Color, live: Boolean, modifier: Modifier) {
    val moving = live && LocalAnimationsEnabled.current
    val phase = if (moving) {
        rememberInfiniteTransition(label = "default-radio-airwave").animateFloat(
            initialValue = 0f,
            targetValue = AirwaveTwoPi,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
            label = "default-radio-airwave-phase"
        )
    } else null
    Spacer(
        modifier.drawWithCache {
            val path = Path()
            val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            val layerColors = AirwaveLayers.map { color.copy(alpha = it.alpha) }
            val amplitude = size.height * if (live) 0.38f else 0.12f
            onDrawBehind {
                val shift = phase?.value ?: 0f
                AirwaveLayers.forEachIndexed { layer, spec ->
                    path.reset()
                    var x = 0f
                    while (x <= size.width) {
                        val t = x / size.width
                        val envelope = sin(PI.toFloat() * t)
                        val y = size.height / 2f + sin(t * spec.cycles * AirwaveTwoPi + shift * spec.speed + layer) * amplitude * spec.scale * envelope
                        if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                        x += 4f
                    }
                    drawPath(path, layerColors[layer], style = stroke)
                }
            }
        }
    )
}

private class AirwaveLayer(val cycles: Float, val speed: Float, val scale: Float, val alpha: Float)

private val AirwaveLayers = listOf(
    AirwaveLayer(cycles = 3f, speed = 1f, scale = 1f, alpha = 0.95f),
    AirwaveLayer(cycles = 4.5f, speed = -1f, scale = 0.7f, alpha = 0.45f),
    AirwaveLayer(cycles = 6f, speed = 2f, scale = 0.45f, alpha = 0.22f)
)

@Composable
internal fun DefaultRadioBroadcastRings(color: Color, live: Boolean, modifier: Modifier) {
    val moving = live && LocalAnimationsEnabled.current
    val progress = if (moving) {
        rememberInfiniteTransition(label = "default-radio-rings").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
            label = "default-radio-rings-progress"
        )
    } else null
    Spacer(
        modifier.drawWithCache {
            val maxRadius = size.minDimension / 2f
            val stroke = Stroke(width = 2.dp.toPx())
            onDrawBehind {
                val base = progress?.value
                if (base == null) {
                    drawCircle(color.copy(alpha = 0.18f), radius = maxRadius * 0.82f, style = stroke)
                } else {
                    repeat(RingCount) { ring ->
                        val f = (base + ring / RingCount.toFloat()) % 1f
                        drawCircle(color.copy(alpha = (1f - f) * 0.55f), radius = maxRadius * (0.6f + 0.4f * f), style = stroke)
                    }
                }
            }
        }
    )
}

private const val RingCount = 3

@Composable
internal fun DefaultRadioSignalBars(bitrateKbps: Int, color: Color, modifier: Modifier = Modifier) {
    val level = radioSignalLevel(bitrateKbps)
    val idle = color.copy(alpha = 0.25f)
    Spacer(
        modifier.size(width = 14.dp, height = 11.dp).drawBehind {
            val gap = 1.5.dp.toPx()
            val barWidth = (size.width - gap * 3f) / 4f
            repeat(4) { bar ->
                val height = size.height * (0.4f + 0.2f * bar)
                drawRoundRect(
                    color = if (bar < level) color else idle,
                    topLeft = Offset(bar * (barWidth + gap), size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                )
            }
        }
    )
}

internal fun radioSignalLevel(bitrateKbps: Int): Int = when {
    bitrateKbps <= 64 -> 1
    bitrateKbps <= 128 -> 2
    bitrateKbps <= 192 -> 3
    else -> 4
}

internal fun radioCategoryIcon(category: RadioCategory): ImageVector = when (category) {
    RadioCategory.Popular -> Icons.Rounded.Whatshot
    RadioCategory.Music -> Icons.Rounded.MusicNote
    RadioCategory.Pop -> Icons.Rounded.Star
    RadioCategory.Rock -> Icons.Rounded.ElectricBolt
    RadioCategory.HipHop -> Icons.Rounded.Album
    RadioCategory.Electronic -> Icons.Rounded.GraphicEq
    RadioCategory.Dance -> Icons.Rounded.Nightlife
    RadioCategory.Chill -> Icons.Rounded.Spa
    RadioCategory.Jazz -> Icons.Rounded.TheaterComedy
    RadioCategory.Classical -> Icons.Rounded.Piano
    RadioCategory.News -> Icons.Rounded.Newspaper
    RadioCategory.Talk -> Icons.Rounded.RecordVoiceOver
    RadioCategory.Sport -> Icons.Rounded.SportsSoccer
    RadioCategory.Local -> Icons.Rounded.Place
    RadioCategory.Worldwide -> Icons.Rounded.Public
}

@Composable
internal fun DefaultRadioGenreKey(category: RadioCategory, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val rest = levyraExpressiveToggleCorner(checked = selected, unchecked = 16.dp, checkedCorner = 28.dp, label = "default-radio-genre-corner")
    val corner = levyraExpressiveCorner(interactionSource = interaction, rest = rest, pressed = 10.dp, label = "default-radio-genre-press")
    val content = if (selected) colors.onPrimary else colors.onSurface
    Row(
        Modifier
            .height(52.dp)
            .widthIn(min = 128.dp)
            .clip(RoundedCornerShape(corner))
            .background(if (selected) colors.primary else colors.surfaceContainerHigh)
            .levyraPressable(onClick = onClick, interactionSource = interaction, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { this.selected = selected }
            .padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(radioCategoryIcon(category), null, tint = if (selected) content else colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(label, color = content, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun DefaultRadioPreset(
    number: Int,
    station: RadioStation,
    playing: Boolean,
    removeFavoriteLabel: String,
    onRemoveFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .width(88.dp)
            .levyraPressable(
                onClick = onClick,
                onLongClick = onRemoveFavorite,
                onLongClickLabel = removeFavoriteLabel,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control,
                onClickLabel = station.name
            )
            .semantics { selected = playing },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            Box(
                Modifier
                    .size(80.dp)
                    .border(3.dp, if (playing) colors.primary else colors.outlineVariant, CircleShape)
                    .padding(5.dp)
            ) { RadioArtwork(station, 70.dp, CircleShape) }
            Box(
                Modifier.align(Alignment.TopStart).size(26.dp).background(if (playing) colors.primary else colors.inverseSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    number.toString(),
                    color = if (playing) colors.onPrimary else colors.inverseOnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Text(
            station.name,
            color = if (playing) colors.primary else colors.onSurface,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun DefaultRadioPodiumCard(
    rank: Int,
    station: RadioStation,
    playing: Boolean,
    favorite: DefaultRadioFavoriteAction,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val fallback = remember(colors.primary, colors.tertiary) { ArtworkPalette(colors.primary.toArgb(), colors.tertiary.toArgb()) }
    val palette by rememberArtworkPalette(paletteKey = "radio-${station.uuid}", artworkUrl = station.safeFaviconUrl, fallback = fallback)
    val tone = Color(palette.start)
    val wash = remember(tone, colors.surfaceContainer) {
        Brush.verticalGradient(listOf(lerp(colors.surfaceContainer, tone, 0.38f), colors.surfaceContainer))
    }
    val outline = with(LocalDensity.current) { 1.5.dp.toPx() }
    Column(
        Modifier
            .width(172.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(wash)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = station.name)
            .semantics { selected = playing }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(116.dp)) {
            Text(
                rank.toString(),
                color = if (playing) colors.primary else colors.onSurface.copy(alpha = 0.42f),
                style = TextStyle(fontSize = 108.sp, lineHeight = 108.sp, fontWeight = FontWeight.Black, drawStyle = Stroke(width = outline)),
                modifier = Modifier.align(Alignment.BottomStart).offset(x = (-6).dp, y = 14.dp)
            )
            Box(Modifier.align(Alignment.TopEnd)) { RadioArtwork(station, 88.dp, RoundedCornerShape(22.dp)) }
            IconButton(onClick = favorite.onToggle, modifier = Modifier.align(Alignment.TopStart).offset(x = (-12).dp, y = (-12).dp).size(48.dp)) {
                Icon(
                    if (favorite.active) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = favorite.label,
                    tint = if (favorite.active) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (playing) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(32.dp).background(colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) { RadioEqualizer(colors.onPrimary) }
            }
        }
        Text(
            station.name,
            color = colors.onSurface,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DefaultRadioSignalBars(station.bitrateKbps, tone)
            RadioStationDetail(station)
        }
    }
}

internal class DefaultRadioFavoriteAction(val active: Boolean, val label: String, val onToggle: () -> Unit)
