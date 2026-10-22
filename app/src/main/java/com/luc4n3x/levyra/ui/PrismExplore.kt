package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.domain.Track

@Composable
internal fun RowScope.PrismMoodTile(label: String, portrait: String, active: Boolean, onClick: () -> Unit, onMix: (() -> Unit)?, mixLabel: String, onPortraitError: () -> Unit = {}) {
    when {
        LevyraIsTorque -> TorqueMoodTile(label, portrait, active, onClick, onMix, mixLabel, onPortraitError)
        LevyraIsSpotify -> SpotifyBrowseTile(
            label, portrait, Modifier.weight(1f).height(100.dp), active = active,
            onLongClick = onMix, onLongClickLabel = mixLabel.takeIf { onMix != null }, onArtworkError = onPortraitError, onClick = onClick
        )
        LevyraIsIvory -> IvoryMoodTile(label, portrait, active, onClick, onMix, mixLabel, onPortraitError)
        LevyraIsYouTubeMusic -> YouTubeMusicMoodTile(label, exploreCategoryPalette(label).first, onClick, Modifier.weight(1f), onMix)
        LevyraIsAppleMusic -> AppleMusicCategoryTile(label, portrait, active, onClick, onMix, mixLabel, onPortraitError, Modifier.weight(1f))
        else -> PrismFacetMoodTile(label, portrait, active, onClick, onMix, mixLabel, onPortraitError)
    }
}

@Composable
private fun RowScope.PrismFacetMoodTile(label: String, portrait: String, active: Boolean, onClick: () -> Unit, onMix: (() -> Unit)?, mixLabel: String, onPortraitError: () -> Unit) {
    val shape = PrismDesign.cut(PrismDesign.surfaceCorner)
    val hasPortrait = portrait.isNotBlank()
    Box(
        Modifier.weight(1f).aspectRatio(1.05f).clip(shape)
            .background(if (hasPortrait) LevyraPanelSoft else LevyraPanel)
            .then(if (active) Modifier.border(2.dp, LevyraCyan, shape) else Modifier)
            .semantics { selected = active }
            .levyraPressable(onClick = onClick, onClickLabel = label, role = Role.Button, onLongClick = onMix, onLongClickLabel = if (onMix != null) mixLabel else null)
    ) {
        if (hasPortrait) {
            AsyncImage(portrait, contentDescription = null, contentScale = ContentScale.Crop, onError = { onPortraitError() }, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.82f))))
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PrismAccent.Tint, Color.Transparent))))
        }
        if (active) Box(Modifier.align(Alignment.TopEnd).padding(12.dp).size(10.dp).clip(CircleShape).background(PrismAccent.gradient()))
        Text(
            label, color = if (hasPortrait) Color.White else LevyraText, style = MaterialTheme.typography.titleMedium,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
        )
    }
}

@Composable
internal fun PrismFreshRelease(track: Track, title: String, subtitle: String, label: String, enabled: Boolean, onOpen: () -> Unit, actions: @Composable () -> Unit) {
    if (LevyraIsSpotify) {
        SpotifyReleaseCard(track, title, subtitle, label, enabled, onOpen, actions)
        return
    }
    if (LevyraIsIvory) {
        IvoryFreshRelease(track, title, subtitle, label, enabled, onOpen, actions)
        return
    }
    if (LevyraIsTorque) {
        Column(Modifier.fillMaxWidth().background(LevyraPanel).levyraPressable(onClick = onOpen, enabled = enabled, onClickLabel = label, role = Role.Button)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 0.78f to Color.Black.copy(alpha = 0.3f))))
                Box(Modifier.fillMaxSize().clip(TorqueStripShape).background(LevyraActivePalette.blue))
                Text(
                    label.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp, maxLines = 1,
                    modifier = Modifier.align(Alignment.TopStart).background(Color.Black).padding(horizontal = 10.dp, vertical = 6.dp)
                )
                Text(
                    title.uppercase(), color = Color.White, fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                )
            }
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                actions()
            }
        }
        return
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(PrismDesign.artworkCorner))
            .levyraPressable(onClick = onOpen, enabled = enabled, onClickLabel = label, role = Role.Button),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(PrismDesign.artworkCorner))) { CoverImage(track, Modifier.fillMaxSize(), highRes = true) }
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = LevyraText, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}

@Composable
private fun RowScope.TorqueMoodTile(label: String, portrait: String, active: Boolean, onClick: () -> Unit, onMix: (() -> Unit)?, mixLabel: String, onPortraitError: () -> Unit) {
    Box(
        Modifier.weight(1f).aspectRatio(0.82f).background(LevyraPanel)
            .then(if (active) Modifier.border(2.dp, Color.White) else Modifier)
            .semantics { selected = active }
            .levyraPressable(onClick = onClick, onClickLabel = label, role = Role.Button, onLongClick = onMix, onLongClickLabel = if (onMix != null) mixLabel else null)
    ) {
        if (portrait.isNotBlank()) AsyncImage(portrait, contentDescription = null, contentScale = ContentScale.Crop, onError = { onPortraitError() }, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().clip(TorqueRiseShape).background(LevyraActivePalette.blue.copy(alpha = if (active) 1f else 0.9f)))
        Text(
            label.uppercase(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
        )
    }
}
