package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.QobuzSkinCopy
import com.luc4n3x.levyra.ui.theme.QobuzDesign
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class QobuzMix(val id: String, val title: String, val description: String, val tracks: List<Track>, val variant: Int)

private val QobuzMixPalettes = listOf(
    listOf(Color(0xFF15102A), Color(0xFFFF3FD0), Color(0xFFB9F3FF), Color(0xFFFFE680)),
    listOf(Color(0xFF2A0F3A), Color(0xFFB04BFF), Color(0xFFFF7AD9), Color(0xFF6E3BFF)),
    listOf(Color(0xFF0E1E2A), Color(0xFF2FD4C5), Color(0xFF7AA8FF), Color(0xFFFFB86B))
)

@Composable
internal fun QobuzMixArt(variant: Int, modifier: Modifier) {
    val colors = QobuzMixPalettes[variant % QobuzMixPalettes.size]
    val animated = LocalAnimationsEnabled.current
    val phase = if (animated) {
        rememberInfiniteTransition(label = "qobuz-mix-art").animateFloat(
            initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(tween<Float>(16_000, easing = LinearEasing)), label = "qobuz-mix-phase"
        )
    } else null
    Canvas(modifier) {
        val t = phase?.value ?: 0f
        fun at(x: Float, y: Float, shift: Float) = Offset(
            size.width * (x + 0.1f * kotlin.math.cos(t + shift)), size.height * (y + 0.08f * kotlin.math.sin(t + shift))
        )
        drawRect(colors[0])
        drawRect(Brush.radialGradient(listOf(colors[1], Color.Transparent), center = at(0.9f, 0.6f, 0f), radius = size.width * 0.8f))
        drawRect(Brush.radialGradient(listOf(colors[2], Color.Transparent), center = at(0.3f, 0.4f, 2.1f), radius = size.width * 0.55f))
        drawRect(Brush.radialGradient(listOf(colors[3], Color.Transparent), center = at(0.62f, 0.02f, 4.2f), radius = size.width * 0.55f))
        drawRect(Brush.verticalGradient(0.55f to Color.Transparent, 1f to colors[0].copy(alpha = 0.85f)))
    }
}

@Composable
internal fun QobuzMixCard(mix: QobuzMix, width: Dp, onClick: () -> Unit) {
    Column(Modifier.width(width).clickable(role = Role.Button, onClickLabel = mix.title, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(6.dp))) {
            QobuzMixArt(mix.variant, Modifier.fillMaxSize())
            Column(Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 22.dp, end = 12.dp)) {
                Text("levyra", color = Color.White, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium))
                Text(mix.title, color = Color.White, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(mix.description, style = QobuzText.baseline, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun QobuzMixRow(mixes: List<QobuzMix>, copy: QobuzSkinCopy, onOpen: (QobuzMix) -> Unit) {
    Column {
        QobuzSectionHeader(copy.mixes, copy.mixesBaseline, copy.seeAll, null)
        Spacer(Modifier.height(QobuzDesign.headerToContent + 4.dp))
        LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
            items(mixes, key = { it.id }) { mix -> QobuzMixCard(mix, 252.dp) { onOpen(mix) } }
        }
    }
}

@Composable
internal fun QobuzSimilarHeader(seed: Track, copy: QobuzSkinCopy) {
    Row(Modifier.padding(horizontal = QobuzDesign.gutter), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(QobuzDesign.artworkShape).background(QobuzDesign.placeholder)) { CoverImage(seed, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(copy.similarTo, style = QobuzText.baseline)
            Text(seed.album.ifBlank { seed.title }, style = QobuzText.sectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() })
        }
    }
}

@Composable
internal fun QobuzMixScreen(
    mix: QobuzMix,
    copy: QobuzSkinCopy,
    currentId: String?,
    bottomInset: Dp,
    onPlay: (List<Track>, Track) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onQueue: (List<Track>) -> Unit,
    onTrackActions: (Track) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }
    LazyColumn(Modifier.fillMaxSize().background(QobuzDesign.surfacePrimary).clickable(enabled = false) {}, contentPadding = PaddingValues(bottom = bottomInset + 24.dp)) {
        item(key = "mix-header") {
            Box(Modifier.fillMaxWidth()) {
                QobuzMixArt(mix.variant, Modifier.matchParentSize())
                Column(Modifier.fillMaxWidth().padding(top = 200.dp, bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(date, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp))
                    Spacer(Modifier.height(8.dp))
                    Text(mix.title, color = Color.White, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() })
                    Spacer(Modifier.height(12.dp))
                    Text(mix.description, color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                        modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                        QobuzMixIcon(Icons.Rounded.Shuffle, strings.shuffle) { onPlayAll(mix.tracks.shuffled()) }
                        Box(
                            Modifier.size(64.dp).clip(CircleShape).background(Color.White)
                                .clickable(role = Role.Button, onClickLabel = strings.play, enabled = mix.tracks.isNotEmpty()) { onPlayAll(mix.tracks) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.PlayArrow, strings.play, tint = Color.Black, modifier = Modifier.size(32.dp)) }
                        QobuzMixIcon(Icons.Rounded.LibraryAdd, strings.addToQueue) { onQueue(mix.tracks) }
                    }
                }
                Box(Modifier.padding(top = 36.dp, start = 4.dp)) { QobuzMixIcon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, onBack) }
            }
        }
        items(mix.tracks, key = { "mix-${mix.id}-${it.id}" }) { track ->
            QobuzTrackRow(track, track.id == currentId, copy, { onPlay(mix.tracks, track) }, { onTrackActions(track) })
        }
    }
}

@Composable
private fun QobuzMixIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(30.dp))
    }
}
