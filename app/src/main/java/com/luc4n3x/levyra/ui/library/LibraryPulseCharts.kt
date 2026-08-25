package com.luc4n3x.levyra.ui.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import com.luc4n3x.levyra.domain.ListeningChartProjection
import com.luc4n3x.levyra.domain.PulseArtistShare
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm

private val RHYTHM_LABEL_HOURS = intArrayOf(0, 6, 12, 18, 23)

@Composable
internal fun LibraryRhythmChart(
    hourBuckets: List<Long>,
    accent: Color,
    peakAccent: Color,
    mutedColor: Color,
    label: String,
    peakHourDescription: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val fractions = remember(hourBuckets) { ListeningChartProjection.hourFractions(hourBuckets) }
    val peakHour = remember(hourBuckets) { ListeningChartProjection.peakHourIndex(hourBuckets) }
    val reveal by animateFloatAsState(
        targetValue = if (fractions.any { it > 0f }) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "library-rhythm-reveal"
    )
    val description = if (peakHour >= 0) peakHourDescription(peakHour) else label

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .semantics { contentDescription = description }
        ) {
            val count = ListeningChartProjection.HOURS_PER_DAY
            val gap = size.width * 0.006f
            val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1f)
            val radius = barWidth / 2f
            for (hour in 0 until count) {
                val fraction = fractions[hour] * reveal
                val barHeight = (size.height * fraction).coerceAtLeast(if (fraction > 0f) radius * 2f else 2f)
                val left = hour * (barWidth + gap)
                val top = size.height - barHeight
                val color = when {
                    fraction <= 0f -> mutedColor
                    hour == peakHour -> peakAccent
                    else -> accent
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (hour in RHYTHM_LABEL_HOURS) {
                Text(
                    text = hour.toString().padStart(2, '0'),
                    color = mutedColor,
                    fontSize = 9.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(9.sp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
internal fun LibraryArtistRing(
    shares: List<PulseArtistShare>,
    palette: List<Color>,
    trackColor: Color,
    centerLabel: String,
    centerValue: String,
    textColor: Color,
    mutedColor: Color,
    percentLabel: (Float) -> String,
    shareDescription: (String, String) -> String,
    modifier: Modifier = Modifier
) {
    if (shares.isEmpty()) return
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
        label = "library-artist-ring-reveal"
    )
    val description = remember(shares) {
        shares.joinToString(", ") { share ->
            shareDescription(share.name, percentLabel(share.fraction))
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(92.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(92.dp)
                    .semantics { contentDescription = description }
            ) {
                val stroke = size.minDimension * 0.13f
                val inset = stroke / 2f
                val diameter = size.minDimension - stroke
                val gapDegrees = 3f
                var startAngle = -90f
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke)
                )
                shares.forEachIndexed { index, share ->
                    val sweep = (share.fraction * 360f * reveal - gapDegrees).coerceAtLeast(0f)
                    if (sweep > 0f) {
                        drawArc(
                            color = palette[index % palette.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(diameter, diameter),
                            style = Stroke(width = stroke)
                        )
                    }
                    startAngle += share.fraction * 360f * reveal
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerValue,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(15.sp),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = centerLabel,
                    color = mutedColor,
                    fontSize = 9.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(9.sp),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            shares.forEachIndexed { index, share ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(palette[index % palette.size])
                    )
                    Text(
                        text = share.name,
                        color = textColor,
                        fontSize = 12.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(12.sp),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = percentLabel(share.fraction),
                        color = mutedColor,
                        fontSize = 11.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(11.sp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
