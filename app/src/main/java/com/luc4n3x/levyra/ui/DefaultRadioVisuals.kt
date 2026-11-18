package com.luc4n3x.levyra.ui

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
