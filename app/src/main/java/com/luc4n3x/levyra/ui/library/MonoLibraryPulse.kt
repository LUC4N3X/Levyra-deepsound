package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.ui.MonoArtwork
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.i18n.tasteCardCopy
import com.luc4n3x.levyra.ui.theme.MonoCodeFont
import com.luc4n3x.levyra.ui.theme.MonoDesign

private val PulseCover = 52.dp
private const val PulseCoverStep = 34

@Composable
internal fun MonoPulseCard(pulse: ListeningPulse, onOpen: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val taste = remember(strings.code) { tasteCardCopy(strings.code) }
    val period = pulse.last7Days
    val persona = tastePersona(period.peakHour)
    val covers = remember(period.topTracks) { period.topTracks.map { it.thumbnailUrl }.filter(String::isNotBlank).distinct().take(3) }
    val artists = remember(period.topArtists) { period.topArtists.map { it.name }.filter(String::isNotBlank).take(3) }
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .clip(MonoDesign.panelShape)
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(MonoDesign.raised, MonoDesign.surface), Offset.Zero, Offset(size.width, size.height)))
                drawRect(Brush.radialGradient(listOf(MonoDesign.accent.copy(alpha = 0.14f), Color.Transparent), center = Offset(size.width, 0f), radius = size.width * 0.9f))
                drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.10f), 1f to Color.Transparent), size = Size(size.width, 1.dp.toPx()))
            }
            .border(1.dp, MonoDesign.hairline, MonoDesign.panelShape)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface, onClickLabel = strings.listeningInsights)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.height(PulseCover).width(PulseCover + (PulseCoverStep * (covers.size - 1).coerceAtLeast(0)).dp)) {
                covers.forEachIndexed { i, url ->
                    Box(
                        Modifier.padding(start = (i * PulseCoverStep).dp).size(PulseCover).clip(CircleShape)
                            .border(2.dp, MonoDesign.raised, CircleShape)
                    ) { MonoArtwork(url, Modifier.size(PulseCover)) }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(52.dp).clip(CircleShape).background(MonoDesign.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MonoDesign.onAccent, modifier = Modifier.size(24.dp))
            }
        }
        Text(
            "${taste.yourSound} · ${strings.pulseWeek}".uppercase(), style = MaterialTheme.typography.labelLarge, color = MonoDesign.accent,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (persona == null || period.plays == 0) {
            Text(strings.pulseEmpty, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary)
            return@Column
        }
        Text(
            taste.title(persona), style = MaterialTheme.typography.headlineMedium, color = MonoDesign.text, maxLines = 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
        )
        Text(if (isExplorer(period)) taste.explorer else taste.loyal, style = MaterialTheme.typography.titleMedium, color = MonoDesign.textSecondary)
        if (artists.isNotEmpty()) {
            Text(artists.joinToString(" · "), style = MaterialTheme.typography.titleLarge, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlacierPulsePill(strings.formatLibraryDuration(period.totalListenMs))
            GlacierPulsePill(strings.formatTrackCount(period.distinctTracks))
        }
    }
}

@Composable
private fun GlacierPulsePill(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoCodeFont), color = MonoDesign.text, maxLines = 1,
        modifier = Modifier.clip(MonoDesign.pillShape).background(MonoDesign.raisedHigh)
            .border(1.dp, MonoDesign.hairline, MonoDesign.pillShape).padding(horizontal = 16.dp, vertical = 10.dp)
    )
}
