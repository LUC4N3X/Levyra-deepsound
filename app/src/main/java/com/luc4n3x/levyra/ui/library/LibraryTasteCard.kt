package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.domain.PulsePeriod
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.TasteCardCopy
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.i18n.tasteCardCopy
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraType
import java.util.Locale

internal enum class TastePersona { NIGHT, MORNING, DAY, EVENING }

internal fun tastePersona(peakHour: Int): TastePersona? = when (peakHour) {
    in 0..4, in 22..23 -> TastePersona.NIGHT
    in 5..10 -> TastePersona.MORNING
    in 11..17 -> TastePersona.DAY
    in 18..21 -> TastePersona.EVENING
    else -> null
}

internal fun isExplorer(period: PulsePeriod): Boolean =
    period.plays > 0 && period.distinctArtists * 10 >= period.plays * 4

private fun TasteCardCopy.title(persona: TastePersona): String = when (persona) {
    TastePersona.NIGHT -> nightOwl
    TastePersona.MORNING -> earlyRiser
    TastePersona.DAY -> daytime
    TastePersona.EVENING -> evening
}

@Composable
internal fun LibraryTasteCard(
    pulse: ListeningPulse,
    onOpenInsights: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    val copy = remember(strings.code) { tasteCardCopy(strings.code) }
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val period = pulse.last7Days
    val persona = tastePersona(period.peakHour)
    val covers = remember(period.topTracks) {
        period.topTracks.map { it.thumbnailUrl }.filter(String::isNotBlank).distinct().take(3)
    }
    val artists = remember(period.topArtists) {
        period.topArtists.map { it.name }.filter(String::isNotBlank).take(3)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .levyraPressable(
                onClick = onOpenInsights,
                pressedScale = LevyraPressScale.Surface,
                role = Role.Button,
                onClickLabel = strings.listeningInsights
            ),
        color = colors.surfaceContainer,
        shape = LevyraCardDesign.SurfaceShape
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LibraryCoverStack(covers = covers, ringColor = colors.surfaceContainer, round = true, size = 40.dp, step = 26.dp)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = "${copy.yourSound} · ${strings.pulseWeek}".uppercase(locale),
                color = colors.primary,
                style = LevyraType.overline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (persona == null || period.plays == 0) {
                Text(strings.pulseEmpty, color = colors.onSurfaceVariant, style = LevyraType.metadata)
                return@Column
            }
            Text(
                text = copy.title(persona),
                color = colors.onSurface,
                style = LevyraType.sectionTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = if (isExplorer(period)) copy.explorer else copy.loyal,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            if (artists.isNotEmpty()) {
                Text(
                    text = artists.joinToString(" · "),
                    color = colors.onSurface,
                    style = LevyraType.cardTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TasteStatPill(strings.formatLibraryDuration(period.totalListenMs))
                TasteStatPill(strings.formatTrackCount(period.distinctTracks))
            }
        }
    }
}

@Composable
private fun TasteStatPill(text: String) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text,
        color = colors.onSurface,
        style = LevyraType.caption,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
