package com.luc4n3x.levyra.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.discovery.DiscoveryFilmScores
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.discovery.LocalHomeDiscovery
import com.luc4n3x.levyra.ui.discovery.discoveryReasonLabel
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.PrismAccent
import com.luc4n3x.levyra.ui.theme.PrismDesign
import kotlinx.coroutines.delay

private const val PrismDiscoverHoldMillis = 3200L
private const val PrismDiscoverThumbs = 4

@Composable
internal fun PrismDiscoverTeaser(modifier: Modifier = Modifier) {
    val handle = LocalHomeDiscovery.current ?: return
    val ui by handle.state.collectAsStateWithLifecycle()
    LaunchedEffect(handle) { handle.onWarm() }
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { discoveryCopy(strings.code) }
    val music = remember(strings.code) { smartMusicCopy(strings.code) }
    val cards = remember(ui.deck) {
        val musical = ui.deck.cards.filterNot { DiscoveryFilmScores.matches(it.track) }.distinctBy { it.track.id }
        musical.drop(ui.deck.index.coerceAtMost(musical.size)).ifEmpty { musical }.take(PrismDiscoverThumbs + 4)
    }
    val animated = LocalAnimationsEnabled.current
    var turn by remember { mutableIntStateOf(0) }
    LaunchedEffect(animated, cards.size) {
        turn = 0
        if (!animated || cards.size < 2) return@LaunchedEffect
        while (true) {
            delay(PrismDiscoverHoldMillis)
            turn += 1
        }
    }
    val lead = cards.getOrNull(turn.mod(cards.size.coerceAtLeast(1)))
    val light = LevyraActivePalette.isLight
    val surface = if (light) Color(0xFFF6F1F4) else Color(0xFF0E0C10)
    val ink = if (light) Color(0xFF17121A) else Color.White
    val shape = PrismDesign.cut(26.dp)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(shape).background(surface).border(1.dp, ink.copy(alpha = 0.08f), shape)
                .levyraPressable(onClick = handle.onOpen, role = Role.Button, onClickLabel = copy.start)
                .semantics(mergeDescendants = true) { contentDescription = copy.title + ". " + copy.lead }
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(132.dp).clip(PrismDesign.cut(18.dp)).background(ink.copy(alpha = 0.06f)).clearAndSetSemantics { }) {
                    Crossfade(targetState = lead?.track, animationSpec = tween(650), label = "prism-discover-lead") { track: Track? ->
                        if (track != null) CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Crossfade(targetState = lead, animationSpec = tween(450), label = "prism-discover-reason") { card ->
                        Text(
                            (card?.let { discoveryReasonLabel(it, copy, music) } ?: copy.lead).uppercase(),
                            color = LevyraPink, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(copy.title, color = ink, fontSize = 26.sp, lineHeight = 29.sp, letterSpacing = (-0.6).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Crossfade(targetState = lead?.track, animationSpec = tween(450), label = "prism-discover-track") { track: Track? ->
                        Text(
                            track?.let { "${it.title} · ${it.artist}" } ?: copy.lead,
                            color = ink.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy((-10).dp), modifier = Modifier.clearAndSetSemantics { }) {
                    cards.filter { it !== lead }.take(PrismDiscoverThumbs).forEach { card ->
                        Box(Modifier.size(34.dp).clip(CircleShape).border(2.dp, surface, CircleShape)) {
                            CoverImage(card.track, Modifier.fillMaxSize())
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp).clearAndSetSemantics { }) {
                    Icon(Icons.Rounded.Close, null, tint = ink.copy(alpha = 0.4f), modifier = Modifier.size(15.dp))
                    Box(Modifier.padding(horizontal = 6.dp).width(16.dp).height(1.dp).background(ink.copy(alpha = 0.22f)))
                    Icon(Icons.Rounded.Favorite, null, tint = LevyraPink.copy(alpha = 0.85f), modifier = Modifier.size(15.dp))
                }
                Row(
                    Modifier.clip(PrismDesign.cut(12.dp)).background(PrismAccent.gradient(false)).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(copy.start, color = Color.White, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
