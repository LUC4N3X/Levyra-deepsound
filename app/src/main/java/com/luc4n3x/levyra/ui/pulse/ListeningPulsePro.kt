package com.luc4n3x.levyra.ui.pulse

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.domain.PulseDay
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as DayTextStyle
import java.util.Locale

@Composable
fun ListeningPulseProCard(
    pulse: ListeningPulse,
    modifier: Modifier = Modifier,
    strings: LevyraStrings = LocalLevyraStrings.current,
    onOpenRecap: (() -> Unit)? = null
) {
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }

    val cardShape = RoundedCornerShape(26.dp)
    val isDark = MaterialTheme.colorScheme.background.run { (red * 0.299 + green * 0.587 + blue * 0.114) < 0.5 }
    val borderColor = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.08f)
    val panelBg = if (isDark) LevyraPanel.copy(alpha = 0.94f) else MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        color = panelBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LevyraCyan.copy(alpha = if (isDark) 0.08f else 0.05f),
                            LevyraViolet.copy(alpha = if (isDark) 0.04f else 0.02f),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
                .padding(20.dp)
        ) {
            if (!pulse.hasSignal) {
                PulseEmptyState(strings = strings, onOpenRecap = onOpenRecap, isDark = isDark)
            } else {
                PulseActiveContent(
                    pulse = pulse,
                    strings = strings,
                    number = number,
                    locale = locale,
                    isDark = isDark,
                    onOpenRecap = onOpenRecap
                )
            }
        }
    }
}

@Composable
private fun PulseActiveContent(
    pulse: ListeningPulse,
    strings: LevyraStrings,
    number: NumberFormat,
    locale: Locale,
    isDark: Boolean,
    onOpenRecap: (() -> Unit)?
) {
    val week = pulse.week.takeLast(7)
    val weekMinutes = week.sumOf { it.listenedMs } / 60_000L
    val peakMs = pulse.weekPeakMs
    val peakDay = week.maxByOrNull { it.listenedMs }
    val avgMinutesPerDay = if (week.isNotEmpty()) weekMinutes / week.size else 0L

    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(LevyraCyan.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = LevyraCyan,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Column {
                    Text(
                        text = strings.pulseTitle,
                        color = LevyraText,
                        fontSize = 17.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(17.sp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.pulseWeek,
                        color = LevyraMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (onOpenRecap != null) {
                Surface(
                    onClick = onOpenRecap,
                    shape = RoundedCornerShape(999.dp),
                    color = LevyraViolet.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, LevyraViolet.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = LevyraViolet,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = strings.listeningRecap,
                            color = LevyraText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = LevyraMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = formatListeningDuration(pulse.totalListenMs, strings, number),
                        color = LevyraText,
                        fontSize = 28.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(28.sp),
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }
                Text(
                    text = strings.pulseMinutes,
                    color = LevyraMuted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PulseMiniBadge(
                    icon = Icons.Rounded.PlayArrow,
                    value = number.format(pulse.plays),
                    label = strings.pulsePlays,
                    accent = LevyraCyan
                )
                PulseMiniBadge(
                    icon = Icons.Rounded.LocalFireDepartment,
                    value = "${number.format(pulse.streakDays)}${strings.recapUnitDays}",
                    label = strings.pulseStreak,
                    accent = LevyraOrange
                )
                PulseMiniBadge(
                    icon = Icons.Rounded.Equalizer,
                    value = "${number.format(avgMinutesPerDay)}${strings.recapUnitMinutes}",
                    label = strings.pulseProAverage,
                    accent = LevyraViolet
                )
            }
        }

        PulseDailyActivityVisualizer(
            week = week,
            peakMs = peakMs,
            selectedDayIndex = selectedDayIndex,
            onSelectDay = { index ->
                selectedDayIndex = if (selectedDayIndex == index) null else index
            },
            locale = locale,
            strings = strings
        )

        PulseFooterInsights(
            pulse = pulse,
            peakDay = peakDay,
            strings = strings,
            locale = locale,
            isDark = isDark
        )
    }
}

@Composable
private fun PulseDailyActivityVisualizer(
    week: List<PulseDay>,
    peakMs: Long,
    selectedDayIndex: Int?,
    onSelectDay: (Int) -> Unit,
    locale: Locale,
    strings: LevyraStrings
) {
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "pulse-pro-reveal"
    )

    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (selectedDayIndex != null && selectedDayIndex in week.indices) {
            val selectedDay = week[selectedDayIndex]
            val dayName = selectedDay.date.dayOfWeek.getDisplayName(DayTextStyle.FULL, locale)
            val minutes = selectedDay.listenedMs / 60_000L
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LevyraCyan.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayName,
                    color = LevyraCyan,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${number.format(minutes)} ${strings.pulseMinuteShort}",
                    color = LevyraText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            week.forEachIndexed { index, day ->
                val isPeak = peakMs > 0L && day.listenedMs == peakMs
                val isSelected = selectedDayIndex == index
                val isToday = index == week.lastIndex

                val fraction = if (peakMs > 0L) {
                    ((day.listenedMs.toFloat() / peakMs.toFloat()) * reveal).coerceIn(0.08f, 1f)
                } else {
                    0.08f
                }

                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                    label = "bar-$index"
                )

                val barBrush = when {
                    isPeak -> Brush.verticalGradient(
                        listOf(LevyraCyan, LevyraViolet.copy(alpha = 0.85f))
                    )
                    day.listenedMs > 0L -> Brush.verticalGradient(
                        listOf(
                            LevyraCyan.copy(alpha = if (isSelected) 1f else 0.70f),
                            LevyraViolet.copy(alpha = if (isSelected) 0.85f else 0.45f)
                        )
                    )
                    else -> Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.04f))
                    )
                }

                val dayLabel = day.date.dayOfWeek
                    .getDisplayName(DayTextStyle.SHORT_STANDALONE, locale)
                    .replace(".", "")
                val dayName = day.date.dayOfWeek.getDisplayName(DayTextStyle.FULL_STANDALONE, locale)
                val minutes = (day.listenedMs / 60_000L).coerceAtLeast(0L)
                val durationText = "${number.format(minutes)} ${strings.pulseMinuteShort}"
                val barDescription = "$dayName, $durationText"

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            role = Role.Button
                            contentDescription = barDescription
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = { onSelectDay(index) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (isPeak && day.listenedMs > 0L) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(LevyraCyan)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedFraction)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(barBrush)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = dayLabel,
                        color = when {
                            isSelected -> LevyraCyan
                            isToday -> LevyraText
                            else -> LevyraMuted.copy(alpha = 0.75f)
                        },
                        fontSize = 10.sp,
                        fontWeight = if (isToday || isSelected) FontWeight.Black else FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PulseFooterInsights(
    pulse: ListeningPulse,
    peakDay: PulseDay?,
    strings: LevyraStrings,
    locale: Locale,
    isDark: Boolean
) {
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }
    val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.06f)
    val bgCol = if (isDark) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.03f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgCol)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (peakDay != null && peakDay.listenedMs > 0L) {
            val dayName = peakDay.date.dayOfWeek.getDisplayName(DayTextStyle.SHORT, locale)
            val minutes = peakDay.listenedMs / 60_000L
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = LevyraCyan,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "${strings.pulseProPeak}: $dayName (${number.format(minutes)} ${strings.recapUnitMinutes})",
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (pulse.peakHour in 0..23) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = LevyraMuted,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = LocalTime.of(pulse.peakHour, 0).format(timeFormatter),
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PulseMiniBadge(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(11.dp))
            Text(text = value, color = LevyraText, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        Text(text = label, color = LevyraMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PulseEmptyState(
    strings: LevyraStrings,
    onOpenRecap: (() -> Unit)?,
    isDark: Boolean
) {
    val ghostBarColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LevyraCyan.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = LevyraCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = strings.pulseEmpty,
                color = LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.emptyRecapSubtitle,
                color = LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            listOf(0.2f, 0.45f, 0.3f, 0.6f, 0.35f, 0.7f, 0.5f).forEach { heightFraction ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(heightFraction)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(ghostBarColor)
                )
            }
        }

        if (onOpenRecap != null) {
            Surface(
                onClick = onOpenRecap,
                shape = RoundedCornerShape(999.dp),
                color = LevyraCyan.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.22f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = LevyraCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = strings.openRecap,
                        color = LevyraText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatListeningDuration(listenedMs: Long, strings: LevyraStrings, number: NumberFormat): String {
    val totalMinutes = listenedMs / 60_000L
    val hours = totalMinutes / 60L
    val remainingMinutes = totalMinutes % 60L
    return when {
        hours > 0 && remainingMinutes > 0 -> "${number.format(hours)} ${strings.recapUnitHours} ${number.format(remainingMinutes)} ${strings.recapUnitMinutes}"
        hours > 0 -> "${number.format(hours)} ${strings.recapUnitHours}"
        else -> "${number.format(totalMinutes)} ${strings.pulseMinuteShort}"
    }
}
