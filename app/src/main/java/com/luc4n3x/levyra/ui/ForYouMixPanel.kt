package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraMixKind
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings

private val ForYouCoverSize = 148.dp
private val ForYouCoverShape = RoundedCornerShape(20.dp)
private const val FamiliarPreset = 0.85f
private const val BalancedPreset = 0.50f
private const val DiscoveryPreset = 0.15f

private class ForYouMix(
    val kind: LevyraMixKind,
    val title: String,
    val icon: ImageVector,
    val hueShift: Double,
    val tracks: List<Track>
)

@Composable
internal fun ForYouMixPanel(
    familiarity: Float,
    loading: Boolean,
    accent: Color,
    coverSources: LevyraMixCoverSources,
    modifier: Modifier = Modifier,
    onFamiliarityChange: (Float) -> Unit,
    onStartMix: (LevyraMixKind) -> Unit,
    onOpenYourSound: () -> Unit,
    onOpenMixLab: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    var pending by rememberSaveable { mutableStateOf<LevyraMixKind?>(null) }
    LaunchedEffect(loading) { if (!loading) pending = null }
    val mixes = remember(coverSources, strings) {
        listOf(
            ForYouMix(LevyraMixKind.Personalized, strings.mixForYou, Icons.Rounded.AutoAwesome, 0.0, coverSources.personalized),
            ForYouMix(LevyraMixKind.CurrentRotation, strings.collectionRepeat, Icons.Rounded.Repeat, 55.0, coverSources.rotation),
            ForYouMix(LevyraMixKind.Rediscover, strings.collectionThrowback, Icons.Rounded.History, -60.0, coverSources.throwback),
            ForYouMix(LevyraMixKind.SurpriseMe, strings.surpriseMe, Icons.Rounded.Casino, 150.0, emptyList())
        )
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            mixes.forEachIndexed { index, mix ->
                ForYouMixCard(
                    mix = mix,
                    index = index,
                    accent = accent,
                    loading = loading && pending == mix.kind,
                    enabled = !loading,
                    onClick = {
                        pending = mix.kind
                        onStartMix(mix.kind)
                    }
                )
            }
        }
        FamiliaritySelector(
            familiarity = familiarity,
            enabled = !loading,
            labels = listOf(strings.mixFamiliarLabel, strings.mix, strings.mixDiscoveryLabel),
            onFamiliarityChange = onFamiliarityChange
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ForYouTool(Icons.Rounded.GraphicEq, strings.yourSound, onOpenYourSound)
            ForYouTool(Icons.Rounded.Tune, strings.mixLab, onOpenMixLab)
        }
    }
}

@Composable
private fun ForYouMixCard(
    mix: ForYouMix,
    index: Int,
    accent: Color,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val tint = remember(accent, mix.hueShift, dark) { shiftedTone(accent, mix.hueShift, if (dark) 68.0 else 46.0) }
    val deep = remember(accent, mix.hueShift, dark) { shiftedTone(accent, mix.hueShift, if (dark) 22.0 else 88.0) }
    val onTint = if (tint.luminance() > 0.45f) Color.Black else Color.White
    val shapes = coverShapes()
    val artworks = remember(mix.tracks) {
        mix.tracks.asSequence()
            .map { it.largeThumbnailUrl.ifBlank { it.thumbnailUrl } }
            .filter { it.isNotBlank() }
            .distinct()
            .take(4)
            .toList()
    }
    val artists = remember(mix.tracks) {
        mix.tracks.asSequence().map { it.artist.trim() }.filter { it.isNotEmpty() }.distinct().take(3).joinToString(", ")
    }
    Column(
        modifier = Modifier
            .width(ForYouCoverSize)
            .semantics(mergeDescendants = true) { contentDescription = mix.title }
            .levyraPressable(onClick = onClick, enabled = enabled, role = Role.Button, pressedScale = LevyraPressScale.Tile),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(ForYouCoverSize).clip(ForYouCoverShape).background(deep)) {
            if (artworks.size >= 4) {
                ArtworkMosaic(artworks)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.35f))))
            } else {
                GenerativeCover(index, shapes, tint, deep, mix.icon, onTint)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(10.dp).size(40.dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    DiscoveryLoadingIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                } else {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                }
            }
        }
        Text(
            mix.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (artists.isNotEmpty()) {
            Text(
                artists,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FamiliaritySelector(
    familiarity: Float,
    enabled: Boolean,
    labels: List<String>,
    onFamiliarityChange: (Float) -> Unit
) {
    val presets = listOf(FamiliarPreset, BalancedPreset, DiscoveryPreset)
    val selected = when {
        familiarity >= 0.70f -> 0
        familiarity >= 0.35f -> 1
        else -> 2
    }
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            val colorSpec: AnimationSpec<Color> = if (LocalAnimationsEnabled.current) spring() else snap()
            val container by animateColorAsState(if (isSelected) scheme.onSurface else Color.Transparent, colorSpec, label = "for-you-segment")
            val content by animateColorAsState(if (isSelected) scheme.surface else scheme.onSurfaceVariant, colorSpec, label = "for-you-segment-text")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(CircleShape)
                    .background(container)
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onFamiliarityChange(presets[index]) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = content,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ForYouTool(icon: ImageVector, label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh)
            .levyraPressable(onClick = onClick, onClickLabel = label, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(18.dp))
        Text(label, color = scheme.onSurface, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
