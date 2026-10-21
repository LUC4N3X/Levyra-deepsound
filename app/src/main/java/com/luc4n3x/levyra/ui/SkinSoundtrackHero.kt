package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.*

private fun StudioHero.secondLine(): String =
    lead.takeIf { it.isNotBlank() && !it.trim().endsWith(track.artist.trim(), ignoreCase = true) } ?: track.title

@Composable
private fun SoundtrackArt(hero: StudioHero, modifier: Modifier) {
    if (hero.artistArtworkUrl.isNotBlank()) {
        AsyncImage(hero.artistArtworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        CoverImage(hero.track, modifier, highRes = true)
    }
}

@Composable
private fun SoundtrackCompanions(urls: List<String>, ring: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        urls.take(3).forEach { url ->
            StableRemoteArtwork(
                url, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(30.dp).clip(CircleShape).border(2.dp, ring, CircleShape)
            )
        }
    }
}

private fun prismSlice(from: Float, to: Float) = GenericShape { size, _ ->
    addRect(androidx.compose.ui.geometry.Rect(size.width * from, 0f, size.width * to, size.height))
}

private val PrismSlices = listOf(prismSlice(0f, 0.33f), prismSlice(0.335f, 0.665f), prismSlice(0.67f, 1f))

@Composable
internal fun PrismSoundtrackHero(hero: StudioHero, playLabel: String) {
    val shifts = listOf((-14).dp, 12.dp, (-4).dp)
    val tints = listOf(LevyraBlue.copy(alpha = 0.22f), Color.Transparent, LevyraViolet.copy(alpha = 0.22f))
    Box(
        Modifier.fillMaxWidth().aspectRatio(0.8f).clip(PrismDesign.cut(28.dp)).background(LevyraBlack)
            .levyraPressable(onClick = hero.onOpen, onClickLabel = playLabel, role = Role.Button)
    ) {
        PrismSlices.forEachIndexed { index, slice ->
            Box(Modifier.fillMaxSize().offset(y = shifts[index]).scale(1.08f).clip(slice)) {
                SoundtrackArt(hero, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(tints[index]))
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.42f to Color.Transparent, 0.78f to Color.Black.copy(alpha = 0.72f), 1f to Color.Black)))
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.Transparent, LevyraCyan.copy(alpha = 0.26f), Color.Transparent), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(900f, 1400f))))
        Row(
            Modifier.align(Alignment.TopStart).padding(16.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(PrismAccent.gradient(false)))
            Text(hero.label, color = Color.White, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (hero.companionArtworkUrls.isNotEmpty()) Box(Modifier.align(Alignment.TopEnd).padding(16.dp)) { SoundtrackCompanions(hero.companionArtworkUrls, Color.Black) }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            Text(
                hero.track.artist, style = androidx.compose.ui.text.TextStyle(brush = Brush.horizontalGradient(listOf(Color.White, Color.White, LevyraPink))),
                fontSize = 46.sp, lineHeight = 48.sp, fontWeight = FontWeight.Normal, letterSpacing = (-1.6).sp, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(hero.secondLine(), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                PrismPlayDisc(hero.isCurrent && hero.isPlaying, hero.isCurrent && hero.isResolving, 60.dp)
            }
        }
    }
}

@Composable
internal fun TorqueSoundtrackHero(hero: StudioHero, playLabel: String) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(0.82f).background(LevyraPanel)
            .levyraPressable(onClick = hero.onOpen, onClickLabel = playLabel, role = Role.Button)
    ) {
        SoundtrackArt(hero, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 0.75f to Color.Black.copy(alpha = 0.35f))))
        Box(Modifier.fillMaxSize().clip(TorqueStripShape).background(LevyraActivePalette.blue))
        Text(
            hero.label.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.TopStart).background(Color.Black).padding(horizontal = 12.dp, vertical = 8.dp)
        )
        if (hero.companionArtworkUrls.isNotEmpty()) Box(Modifier.align(Alignment.TopEnd).padding(14.dp)) { SoundtrackCompanions(hero.companionArtworkUrls, Color.Black) }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 20.dp, end = 104.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(hero.track.artist.uppercase(), color = Color.White, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(hero.secondLine(), color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, letterSpacing = 0.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 18.dp).size(72.dp).clip(CircleShape)
                .background(Color.Black).border(5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val playing = hero.isCurrent && hero.isPlaying
            if (hero.isCurrent && hero.isResolving) LevyraLoadingIndicator(color = Color.White, modifier = Modifier.size(28.dp))
            else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(38.dp))
        }
    }
}
