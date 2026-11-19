package com.luc4n3x.levyra.ui

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.feature.radio.RadioCategory
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

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
internal fun DefaultRadioDial(stations: List<RadioStation>, liveId: String?, onTune: (RadioStation) -> Unit) {
    if (stations.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val accent = com.luc4n3x.levyra.ui.components.levyraUtilityColors(com.luc4n3x.levyra.ui.components.LevyraUtilitySeed).accent
    val state = androidx.compose.foundation.lazy.rememberLazyListState()
    val fling = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(state)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val stepPx = with(density) { (RadioDialItem + RadioDialGap).toPx() }
    val tickPx = with(density) { RadioDialTick.toPx() }
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 20.dp).height(RadioDialHeight)) {
        val side = (maxWidth - RadioDialItem) / 2
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .drawBehind {
                    val scrolled = state.firstVisibleItemIndex * stepPx + state.firstVisibleItemScrollOffset
                    val phase = scrolled % tickPx
                    val first = (scrolled / tickPx).toInt()
                    var k = 0
                    var x = -phase
                    while (x <= size.width) {
                        val major = (first + k) % 5 == 0
                        val h = if (major) size.height else size.height * 0.45f
                        drawLine(
                            colors.onSurfaceVariant.copy(alpha = if (major) 0.55f else 0.25f),
                            Offset(x, size.height - h),
                            Offset(x, size.height),
                            strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx()
                        )
                        x += tickPx
                        k++
                    }
                }
        )
        androidx.compose.foundation.lazy.LazyRow(
            state = state,
            flingBehavior = fling,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = side, end = side, top = 38.dp),
            horizontalArrangement = Arrangement.spacedBy(RadioDialGap),
            modifier = Modifier.fillMaxSize()
        ) {
            items(stations.size, key = { stations[it].uuid }) { index ->
                val station = stations[index]
                val live = station.uuid == liveId
                Column(
                    Modifier
                        .width(RadioDialItem)
                        .levyraPressable(onClick = { onTune(station) }, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = station.name),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier.size(64.dp).border(if (live) 3.dp else 0.dp, if (live) accent else Color.Transparent, CircleShape).padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) { RadioArtwork(station, 56.dp, CircleShape) }
                    Text(
                        station.name,
                        color = if (live) accent else colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .width(4.dp)
                .height(34.dp)
                .background(accent, CircleShape)
        )
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            0f to com.luc4n3x.levyra.ui.theme.LevyraBlack,
                            0.14f to Color.Transparent,
                            0.86f to Color.Transparent,
                            1f to com.luc4n3x.levyra.ui.theme.LevyraBlack
                        )
                    )
                }
        )
    }
}

private val RadioDialItem = 76.dp
private val RadioDialGap = 12.dp
private val RadioDialTick = 11.dp
private val RadioDialHeight = 140.dp

@Composable
internal fun DefaultRadioOnAirLamp(onAir: Boolean, label: String) {
    val colors = MaterialTheme.colorScheme
    val glow = if (onAir && LocalAnimationsEnabled.current) {
        rememberInfiniteTransition(label = "default-radio-lamp").animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), androidx.compose.animation.core.RepeatMode.Reverse),
            label = "default-radio-lamp-glow"
        )
    } else null
    val lamp = if (onAir) RadioLiveRed else colors.onSurfaceVariant
    androidx.compose.foundation.layout.Row(
        Modifier
            .clip(CircleShape)
            .background(if (onAir) RadioLiveRed.copy(alpha = 0.16f) else colors.surfaceContainerHighest)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .drawBehind {
                    val strength = glow?.value ?: 1f
                    if (onAir) drawCircle(lamp.copy(alpha = 0.35f * strength), radius = size.minDimension * 1.1f)
                    drawCircle(lamp.copy(alpha = if (onAir) strength else 1f))
                }
        )
        Text(
            label.uppercase(androidx.compose.ui.platform.LocalConfiguration.current.locales[0]),
            color = if (onAir) RadioLiveRed else colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun DefaultRadioTransmitter(station: RadioStation, onAir: Boolean, accent: Color) {
    val moving = onAir && LocalAnimationsEnabled.current
    val phase = if (moving) {
        rememberInfiniteTransition(label = "default-radio-transmitter").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
            label = "default-radio-transmitter-phase"
        )
    } else null
    Box(
        Modifier
            .size(RadioTransmitterSize)
            .drawBehind {
                val inner = RadioTransmitterLogo.toPx() / 2f
                val outer = size.minDimension / 2f
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                val shift = phase?.value
                repeat(RadioTransmitterRings) { ring ->
                    val progress = if (shift == null) (ring + 1f) / (RadioTransmitterRings + 1f) else (shift + ring.toFloat() / RadioTransmitterRings) % 1f
                    val radius = inner + (outer - inner) * progress
                    val alpha = if (shift == null) 0.18f else (1f - progress) * 0.7f
                    drawCircle(accent.copy(alpha = alpha), radius = radius, style = stroke)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(RadioTransmitterLogo)
                .border(3.dp, if (onAir) accent else Color.White.copy(alpha = 0.18f), CircleShape)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) { RadioArtwork(station, RadioTransmitterLogo - 6.dp, CircleShape) }
    }
}

private const val RadioTransmitterRings = 3
private val RadioTransmitterSize = 200.dp
private val RadioTransmitterLogo = 116.dp
