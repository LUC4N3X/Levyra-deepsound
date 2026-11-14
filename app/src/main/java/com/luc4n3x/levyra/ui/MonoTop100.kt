package com.luc4n3x.levyra.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.apple.AppleCityChart
import com.luc4n3x.levyra.ui.theme.MonoCodeFont
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.ui.theme.MonoFont

internal val MonoTop100Hues = listOf(
    Color(0xFF7C4DFF), Color(0xFFFF4D6D), Color(0xFFFF8A00), Color(0xFF00B894),
    Color(0xFF2E7DFF), Color(0xFFE84393), Color(0xFFFFC300), Color(0xFF00BFB3)
)

internal fun monoTop100Hue(slot: Int, global: Boolean): Color = if (global) MonoDesign.accent else MonoTop100Hues[slot % MonoTop100Hues.size]

@Composable
internal fun MonoTop100Cover(label: String, date: String, slot: Int, global: Boolean, size: Dp, modifier: Modifier = Modifier, numeral: String = "100") {
    val hue = monoTop100Hue(slot, global)
    val ink = if (hue.luminance() > 0.5f) Color(0xFF0B0F12) else Color.White
    val scale = 1f / LocalDensity.current.fontScale
    Box(
        modifier.size(size).clip(MonoDesign.cardShape)
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(lerp(hue, Color.White, 0.1f), lerp(hue, Color.Black, 0.28f)), Offset.Zero, Offset(this.size.width, this.size.height)))
            }
            .clearAndSetSemantics { }
    ) {
        Text(date, color = ink.copy(alpha = 0.75f), fontFamily = MonoCodeFont, fontSize = (9.5f * scale).sp, letterSpacing = 1.2.sp, maxLines = 1,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 14.dp))
        Column(Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 10.dp)) {
            Text("TOP", color = ink, fontFamily = MonoFont, fontWeight = FontWeight.Bold, fontSize = (20f * scale).sp, lineHeight = (22f * scale).sp, letterSpacing = 1.sp)
            Text(numeral, color = ink, fontFamily = MonoFont, fontWeight = FontWeight.Bold, fontSize = (64f * scale).sp, lineHeight = (62f * scale).sp, letterSpacing = (-3).sp, maxLines = 1, softWrap = false)
        }
        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(10.dp).clip(RoundedCornerShape(8.dp)).background(ink)
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(label.uppercase(), color = hue.let { if (ink == Color.White) lerp(it, Color.Black, 0.35f) else it }, fontFamily = MonoFont, fontWeight = FontWeight.Bold,
                fontSize = (13f * scale).sp, letterSpacing = 0.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun MonoTop100ChartScreen(
    open: com.luc4n3x.levyra.viewmodel.DailyTop100ChartUiState,
    label: String,
    date: String,
    slot: Int,
    numeral: String,
    failedLabel: String,
    retryLabel: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (List<com.luc4n3x.levyra.domain.Track>, com.luc4n3x.levyra.domain.Track) -> Unit,
    onTrackActions: (com.luc4n3x.levyra.domain.Track) -> Unit
) {
    val strings = com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings.current
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window ?: return@SideEffect
        androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
    val tracks = open.tracks
    val bottom = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    GlacierCollectionScreen(
        title = label, tracks = tracks, currentId = null, isPlaying = false,
        onDismiss = onBack, onPlay = { track -> onPlay(tracks, track) },
        onPlayAll = { tracks.firstOrNull()?.let { onPlay(tracks, it) } },
        onShuffle = { tracks.shuffled().let { shuffled -> shuffled.firstOrNull()?.let { onPlay(shuffled, it) } } },
        onTrackActions = onTrackActions, bottomInset = bottom,
        byline = "Top $numeral · $date", loading = open.loading, glow = monoTop100Hue(slot, open.chart.global), ranked = true,
        failedLabel = failedLabel.takeIf { open.failed }, retryLabel = retryLabel, onRetry = onRetry,
        cover = { MonoTop100Cover(label, date, slot, open.chart.global, 248.dp, numeral = numeral) }
    )
}

@Composable
internal fun MonoCityCard(chart: AppleCityChart, slot: Int, modifier: Modifier = Modifier) {
    val hue = monoTop100Hue(slot, false)
    val ink = if (hue.luminance() > 0.5f) Color(0xFF0B0F12) else Color.White
    val scale = 1f / LocalDensity.current.fontScale
    Box(
        modifier.clip(MonoDesign.cardShape)
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(lerp(hue, Color.White, 0.1f), lerp(hue, Color.Black, 0.28f)), Offset.Zero, Offset(this.size.width, this.size.height)))
            }
            .clearAndSetSemantics { }
    ) {
        Text(chart.name.uppercase(), color = ink, fontFamily = MonoFont, fontWeight = FontWeight.Bold, fontSize = (22f * scale).sp, lineHeight = (24f * scale).sp,
            letterSpacing = 0.4.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 14.dp, end = 14.dp))
        Box(
            Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(8.dp)).background(ink)
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(chart.badge.uppercase(), color = hue.let { if (ink == Color.White) lerp(it, Color.Black, 0.35f) else it }, fontFamily = MonoCodeFont, fontWeight = FontWeight.Bold,
                fontSize = (12f * scale).sp, letterSpacing = 0.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
