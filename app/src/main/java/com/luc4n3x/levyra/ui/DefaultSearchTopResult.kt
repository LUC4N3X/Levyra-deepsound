package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import java.util.Locale

@Composable
internal fun DefaultSearchTopResult(
    hero: Track,
    artist: ArtistHit?,
    playing: Boolean,
    enabled: Boolean,
    onArtist: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onMix: () -> Unit,
    rows: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val scheme = MaterialTheme.colorScheme
    val name = artist?.name?.ifBlank { null } ?: hero.artist
    val officialPortrait = rememberOfficialArtistPortrait(name)
    val portrait = officialPortrait?.ifBlank { null }
        ?: artist?.thumbnailUrl?.ifBlank { null }
        ?: hero.largeThumbnailUrl.ifBlank { hero.thumbnailUrl }
    val portraitUrl = remember(portrait) { highResolutionHomeArtistArtworkUrl(portrait) }
    val fallback = remember(scheme.primary, scheme.tertiary) { ArtworkPalette(scheme.primary.toArgb(), scheme.tertiary.toArgb()) }
    val palette by rememberArtworkPalette(
        paletteKey = if (portraitUrl.isBlank()) "" else "search-top-$portraitUrl",
        artworkUrl = portraitUrl,
        fallback = fallback
    )
    val tone = Color(palette.start)
    val card = lerp(scheme.surfaceContainerLow, tone, 0.14f)
    val accent = lerp(tone, Color.White, 0.4f)
    val subtitle = artist?.subscribers?.trim()?.takeIf(String::isNotBlank) ?: strings.artistLabel
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(listOf(lerp(card, tone, 0.18f), card)))
            .padding(bottom = 10.dp)
    ) {
        SleeveAndRecord(
            portraitUrl = portraitUrl,
            labelUrl = hero.thumbnailUrl.ifBlank { portraitUrl },
            playing = playing,
            enabled = enabled,
            playLabel = strings.play,
            onPlay = onPlay,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClickLabel = strings.openArtist, role = Role.Button, onClick = onArtist)
                .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.topResult.uppercase(Locale.forLanguageTag(strings.code)),
                    color = accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    maxLines = 1
                )
                Text(
                    text = name,
                    color = LevyraText,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = subtitle,
                    color = LevyraMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(28.dp))
        }
        DefaultTopResultActions(enabled, tone, onShuffle, onMix)
        rows()
    }
}

@Composable
private fun SleeveAndRecord(
    portraitUrl: String,
    labelUrl: String,
    playing: Boolean,
    enabled: Boolean,
    playLabel: String,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animated = LocalAnimationsEnabled.current
    val peek by animateDpAsState(
        targetValue = if (playing) RecordPeekPlaying else RecordPeekIdle,
        animationSpec = if (animated) spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow) else snap(),
        label = "searchRecordPeek"
    )
    val spin = rememberRecordSpin(playing && animated && LocalLevyraVisualCapabilities.current.decorativeMotion)
    val scheme = MaterialTheme.colorScheme
    Box(modifier.height(SleeveSize)) {
        Box(
            Modifier
                .offset(x = SleeveSize - RecordSize + peek)
                .align(Alignment.CenterStart)
                .size(RecordSize)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .vinylDisc(scheme.surfaceContainerLowest, scheme.primary)
                .clickable(enabled = enabled, onClickLabel = playLabel, role = Role.Button, onClick = onPlay),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(RecordSize * 0.36f)
                    .graphicsLayer { rotationZ = spin }
                    .clip(CircleShape)
                    .border(2.dp, Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(labelUrl).crossfade(animated).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(
            Modifier
                .size(SleeveSize)
                .shadow(16.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.surfaceContainerHighest)
        ) {
            if (portraitUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(portraitUrl).crossfade(animated).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(0f, -0.4f),
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f))))
            )
        }
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = SleeveSize + (peek - RecordPlayBadge) / 2)
                .size(RecordPlayBadge)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF101114),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun rememberRecordSpin(active: Boolean): Float {
    if (!active) return 0f
    val angle by rememberInfiniteTransition(label = "searchRecordSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(RecordSpinMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "searchRecordSpinAngle"
    )
    return angle
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultTopResultActions(
    enabled: Boolean,
    tone: Color,
    onShuffle: () -> Unit,
    onMix: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val pillColor = lerp(MaterialTheme.colorScheme.surfaceContainerHighest, tone, 0.28f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onShuffle,
            enabled = enabled,
            color = pillColor,
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.weight(1f).height(52.dp)
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = LevyraText, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.shuffle, color = LevyraText, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            }
        }
        FilledTonalIconButton(
            onClick = onMix,
            enabled = enabled,
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = pillColor, contentColor = LevyraText),
            modifier = Modifier.size(52.dp)
        ) {
            Icon(Icons.Rounded.Radio, contentDescription = strings.mix, modifier = Modifier.size(22.dp))
        }
    }
}

private val SleeveSize = 196.dp
private val RecordSize = 184.dp
private val RecordPeekIdle = 58.dp
private val RecordPeekPlaying = 100.dp
private val RecordPlayBadge = 32.dp
private const val RecordSpinMillis = 3_600
