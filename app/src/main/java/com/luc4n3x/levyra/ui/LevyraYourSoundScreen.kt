package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.luc4n3x.levyra.domain.DnaArtist
import com.luc4n3x.levyra.domain.ListeningDna
import com.luc4n3x.levyra.domain.ListeningDnaPeriod
import com.luc4n3x.levyra.domain.PulseTrack
import com.luc4n3x.levyra.ui.components.LevyraConnectedDefaults
import com.luc4n3x.levyra.ui.components.LevyraConnectedPosition
import com.luc4n3x.levyra.ui.components.LevyraConnectedStyle
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSkeletonBlock
import com.luc4n3x.levyra.ui.components.levyraConnectedRowSurface
import com.luc4n3x.levyra.ui.components.levyraConnectedSurface
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraText

private val DnaPeriods = listOf(
    ListeningDnaPeriod.Week,
    ListeningDnaPeriod.Month,
    ListeningDnaPeriod.HalfYear,
    ListeningDnaPeriod.AllTime
)

@Composable
internal fun LevyraYourSoundOverlay(
    dna: ListeningDna,
    period: ListeningDnaPeriod,
    loading: Boolean,
    accent: Color,
    onSelectPeriod: (ListeningDnaPeriod) -> Unit,
    onStartArtistMix: (DnaArtist) -> Unit,
    onDiscoverArtist: (DnaArtist) -> Unit,
    onPlayTrack: (PulseTrack) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val style = LevyraConnectedDefaults.style(accent = accent)
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val percent = remember(locale) {
        NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 0 }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LevyraInk, LevyraBlack)))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(style.gap)
        ) {
            item(contentType = "dna-header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.back,
                            tint = LevyraText
                        )
                    }
                    Column(modifier = Modifier.padding(start = 6.dp)) {
                        Text(strings.yourSound, color = LevyraText, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text(strings.yourSoundSubtitle, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item(contentType = "dna-period") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(style.gap)
                ) {
                    DnaPeriods.forEachIndexed { index, entry ->
                        DnaPeriodChip(
                            label = dnaPeriodLabel(entry, strings.dnaPeriodWeek, strings.dnaPeriodMonth, strings.dnaPeriodHalfYear, strings.dnaPeriodAll),
                            selected = entry == period,
                            position = LevyraConnectedPosition.of(index, DnaPeriods.size),
                            style = style,
                            accent = accent,
                            onClick = { onSelectPeriod(entry) }
                        )
                    }
                }
            }
            if (loading && !dna.hasSignal) {
                item(contentType = "dna-loading") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LevyraSkeletonBlock(width = 220.dp, height = 26.dp, accent = accent)
                        LevyraSkeletonBlock(width = 320.dp, height = 64.dp, accent = accent)
                        LevyraSkeletonBlock(width = 320.dp, height = 64.dp, accent = accent)
                    }
                }
            } else if (!dna.hasSignal) {
                item(contentType = "dna-empty") {
                    Text(
                        text = strings.dnaEmpty,
                        color = LevyraMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                item(contentType = "dna-stats") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(style.gap)
                    ) {
                        DnaStatTile(dna.totalMinutes.toString(), strings.pulseMinutes, LevyraConnectedPosition.Top, style, accent)
                        DnaStatTile(dna.plays.toString(), strings.statPlays, LevyraConnectedPosition.Middle, style, accent)
                        DnaStatTile(dna.distinctArtists.toString(), strings.statArtists, LevyraConnectedPosition.Middle, style, accent)
                        DnaStatTile(percent.format(dna.discoveryRate / 100.0), strings.dnaDiscovery, LevyraConnectedPosition.Bottom, style, accent)
                    }
                }
                if (dna.artists.isNotEmpty()) {
                    item(contentType = "dna-artists-title") { DnaSectionTitle(strings.pulseTopArtists) }
                    itemsIndexed(
                        dna.artists,
                        key = { index, _ -> "dna-artist-$index" },
                        contentType = { _, _ -> "dna-artist" }
                    ) { index, artist ->
                        DnaArtistRow(
                            artist = artist,
                            accent = accent,
                            position = LevyraConnectedPosition.of(index, dna.artists.size),
                            style = style,
                            mixLabel = strings.mixStartRadio,
                            discoverLabel = strings.discoverMore,
                            percent = percent,
                            onStartMix = { onStartArtistMix(artist) },
                            onDiscover = { onDiscoverArtist(artist) }
                        )
                    }
                }
                item(contentType = "dna-rhythm-title") { DnaSectionTitle(strings.dnaRhythm) }
                item(contentType = "dna-rhythm") {
                    DnaRhythmChart(
                        buckets = dna.hourBuckets,
                        accent = accent,
                        style = style,
                        peakLabel = strings.pulsePeakHour,
                        peakHour = dna.peakHour,
                        locale = locale
                    )
                }
                if (dna.tracks.isNotEmpty()) {
                    item(contentType = "dna-tracks-title") { DnaSectionTitle(strings.songs) }
                    itemsIndexed(
                        dna.tracks,
                        key = { index, _ -> "dna-track-$index" },
                        contentType = { _, _ -> "dna-track" }
                    ) { index, track ->
                        DnaTrackRow(
                            track = track,
                            position = LevyraConnectedPosition.of(index, dna.tracks.size),
                            style = style,
                            accent = accent,
                            onClick = { onPlayTrack(track) }
                        )
                    }
                }
            }
        }
    }
}

private fun dnaPeriodLabel(
    period: ListeningDnaPeriod,
    week: String,
    month: String,
    halfYear: String,
    allTime: String
): String = when (period) {
    ListeningDnaPeriod.Week -> week
    ListeningDnaPeriod.Month -> month
    ListeningDnaPeriod.HalfYear -> halfYear
    ListeningDnaPeriod.AllTime -> allTime
}

@Composable
private fun RowScope.DnaPeriodChip(
    label: String,
    selected: Boolean,
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .levyraConnectedRowSurface(position, style, selected = selected)
            .levyraPressable(
                onClick = onClick,
                pressedScale = LevyraPressScale.Row,
                role = Role.Tab,
                onClickLabel = label
            )
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) accent else LevyraMuted,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.DnaStatTile(
    value: String,
    label: String,
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    accent: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .levyraConnectedRowSurface(position, style)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, color = accent, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(
            text = label,
            color = LevyraMuted,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DnaSectionTitle(title: String) {
    Text(
        text = title,
        color = LevyraText,
        fontSize = 15.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun DnaArtistRow(
    artist: DnaArtist,
    accent: Color,
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    mixLabel: String,
    discoverLabel: String,
    percent: NumberFormat,
    onStartMix: () -> Unit,
    onDiscover: () -> Unit
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val weight by animateFloatAsState(
        targetValue = artist.weight.coerceIn(0f, 1f),
        animationSpec = if (animationsEnabled) tween(620) else snap(),
        label = "dna-artist-weight"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .levyraConnectedSurface(position, style)
            .levyraPressable(
                onClick = onStartMix,
                pressedScale = LevyraPressScale.Row,
                role = Role.Button,
                onClickLabel = mixLabel,
                onLongClick = onDiscover,
                onLongClickLabel = discoverLabel
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = artist.name,
                color = LevyraText,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = percent.format(artist.weight.toDouble()),
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(5.dp)) {
            val trackHeight = size.height
            val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            drawRoundRect(
                color = LevyraPlayerDesign.TrackInactive,
                topLeft = Offset.Zero,
                size = Size(size.width, trackHeight),
                cornerRadius = radius
            )
            val filled = size.width * weight
            if (filled > 0f) {
                drawRoundRect(
                    color = accent,
                    topLeft = Offset.Zero,
                    size = Size(filled, trackHeight),
                    cornerRadius = radius
                )
            }
        }
    }
}

@Composable
private fun DnaRhythmChart(
    buckets: List<Long>,
    accent: Color,
    style: LevyraConnectedStyle,
    peakLabel: String,
    peakHour: Int,
    locale: Locale
) {
    if (buckets.isEmpty()) return
    val peak = buckets.maxOrNull() ?: 0L
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .levyraConnectedSurface(LevyraConnectedPosition.Single, style)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            if (peak <= 0L) return@Canvas
            val count = buckets.size
            val slot = size.width / count
            val barWidth = (slot * 0.6f).coerceAtLeast(1f)
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
            for (index in 0 until count) {
                val ratio = (buckets[index].toFloat() / peak.toFloat()).coerceIn(0f, 1f)
                val barHeight = (size.height * ratio).coerceAtLeast(barWidth)
                drawRoundRect(
                    color = if (index == peakHour) accent else accent.copy(alpha = 0.32f),
                    topLeft = Offset(index * slot + (slot - barWidth) / 2f, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius
                )
            }
        }
        if (peakHour in 0..23) {
            Text(
                text = peakLabel + " " + localizedHour(peakHour, locale),
                color = LevyraMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DnaTrackRow(
    track: PulseTrack,
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .levyraConnectedSurface(position, style)
            .levyraPressable(
                onClick = onClick,
                pressedScale = LevyraPressScale.Row,
                role = Role.Button,
                onClickLabel = track.title
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = track.title,
                color = LevyraText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = track.plays.toString(),
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
        )
    }
}

private fun localizedHour(hour: Int, locale: Locale): String {
    val safeHour = hour.coerceIn(0, 23)
    return LocalTime.of(safeHour, 0)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
}
