package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne

import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraCardCaption
import com.luc4n3x.levyra.ui.components.LevyraExpressiveIconButton
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraType
import com.luc4n3x.levyra.ui.theme.levyraTonalRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.materialkolor.hct.Hct
import kotlin.math.abs

@Composable
internal fun DiscoveryTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    onActions: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val strings = LocalLevyraStrings.current
    val interaction = remember { MutableInteractionSource() }
    val corner = if (LocalAnimationsEnabled.current) {
        levyraExpressiveCorner(
            interactionSource = interaction,
            rest = LevyraCardDesign.EditorialCorner,
            pressed = LevyraCardDesign.ArtworkCorner,
            label = "discoveryArtworkCorner"
        )
    } else {
        LevyraCardDesign.EditorialCorner
    }
    val colors = MaterialTheme.colorScheme
    val scrim = remember { Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))) }
    Column(
        modifier = modifier
            .semantics { selected = isCurrent }
            .levyraPressable(
                onClick = onPlay,
                interactionSource = interaction,
                pressedScale = LevyraPressScale.Tile,
                role = Role.Button,
                onClickLabel = strings.playNow,
                onLongClick = onActions,
                onLongClickLabel = if (onActions != null) strings.songOptions else null
            ),
        verticalArrangement = Arrangement.spacedBy(LevyraCardDesign.CaptionTopGap)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (wide) LevyraHomeDesign.EditorialArtworkRatio else 1f)
                .clip(RoundedCornerShape(corner))
                .background(colors.surfaceContainerHigh)
        ) {
            CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = wide)
            if (wide || isCurrent) {
                Box(Modifier.fillMaxSize().background(scrim))
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(LevyraHomeDesign.HorizontalInset)
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(if (isCurrent) colors.primaryContainer else colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isResolving -> DiscoveryLoadingIndicator(Modifier.size(24.dp), colors.onPrimaryContainer)
                        isCurrent -> LevyraPlayingIndicator(playing = isPlaying, color = colors.onPrimaryContainer)
                        else -> Icon(Icons.Rounded.PlayArrow, null, tint = colors.onSurface)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            LevyraCardCaption(
                title = track.title,
                subtitle = track.artist,
                titleColor = if (isCurrent) colors.primary else colors.onSurface,
                subtitleColor = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
    }
}

@Composable
internal fun DiscoveryLoadingIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    if (LocalAnimationsEnabled.current) {
        LevyraLoadingIndicator(modifier = modifier, color = color)
    } else {
        Box(modifier = modifier.progressSemantics(), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = color)
        }
    }
}

@Composable
internal fun DiscoveryTrackActions(
    trackTitle: String,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenAlbum: (() -> Unit)? = null
) {
    val strings = LocalLevyraStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        LevyraExpressiveIconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.MoreHoriz, "${strings.songOptions}, $trackTitle", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(if (isFavorite) strings.removeFromFavorites else strings.addToFavorites) },
                onClick = {
                    expanded = false
                    onFavorite()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.addToPlaylist) },
                onClick = {
                    expanded = false
                    onAddToPlaylist()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.share) },
                onClick = {
                    expanded = false
                    onShare()
                }
            )
            onOpenAlbum?.let { action ->
                DropdownMenuItem(
                    text = { Text(strings.openAlbum) },
                    onClick = {
                        expanded = false
                        action()
                    }
                )
            }
        }
    }
}

@Composable
internal fun DiscoveryEditorialCard(
    title: String,
    subtitle: String,
    artwork: Track?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    accentStart: Color = Color(0xFF7C3AED),
    accentEnd: Color = Color(0xFF4C1D95),
    artworkShape: Shape? = null
) {
    val colors = MaterialTheme.colorScheme
    val isLight = LevyraIsLight
    if (LevyraIsNocturne) {
        NocturneCollectionCard(title, subtitle, artwork, onOpen, modifier)
        return
    }
    val tonal = remember(accentStart, isLight) {
        levyraTonalRole(accentStart, isLight, maxChroma = 46.0)
    }
    val backgroundColor = lerp(colors.surfaceContainerHigh, tonal.container, 0.94f)
    val contrastPalette = remember(backgroundColor) {
        editorialContrastPalette(backgroundColor, backgroundColor)
    }
    val interaction = remember { MutableInteractionSource() }
    val corner = if (LocalAnimationsEnabled.current) {
        levyraExpressiveCorner(
            interactionSource = interaction,
            rest = EditorialCardRestCorner,
            pressed = EditorialCardPressedCorner,
            label = "editorialCollectionCorner"
        )
    } else {
        EditorialCardRestCorner
    }
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(contrastPalette.start)
            .levyraPressable(
                onClick = onOpen,
                interactionSource = interaction,
                role = Role.Button,
                pressedScale = LevyraPressScale.Tile
            )
    ) {
        val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.4f
        if (stacked) {
            Column(
                modifier = Modifier.padding(LevyraHomeDesign.EditorialPadding),
                verticalArrangement = Arrangement.spacedBy(LevyraHomeDesign.EditorialPadding)
            ) {
                DiscoveryEditorialText(title, subtitle, contrastPalette.text)
                if (artwork != null) {
                    CoverImage(
                        track = artwork,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(LevyraHomeDesign.EditorialArtworkRatio)
                            .clip(LevyraCardDesign.ArtworkShape)
                    )
                }
            }
        } else {
            val artworkSize = LevyraHomeDesign.EditorialThumb + 22.dp
            val textWidth = if (artwork == null) {
                maxWidth - LevyraHomeDesign.EditorialPadding * 2
            } else {
                (maxWidth - artworkSize - LevyraHomeDesign.EditorialPadding - 16.dp)
                    .coerceAtLeast(112.dp)
            }
            DiscoveryEditorialText(
                title = title,
                subtitle = subtitle,
                textColor = contrastPalette.text,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = LevyraHomeDesign.EditorialPadding,
                        top = LevyraHomeDesign.EditorialPadding
                    )
                    .width(textWidth)
            )
            if (artwork != null) {
                CoverImage(
                    track = artwork,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 16.dp, y = 18.dp)
                        .size(artworkSize)
                        .rotate(12f)
                        .clip(artworkShape ?: LevyraCardDesign.ArtworkShape)
                )
            }
        }
    }
}

@Composable
private fun DiscoveryEditorialText(
    title: String,
    subtitle: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
        Text(
            title,
            style = LevyraType.sectionTitle.copy(fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.7).sp),
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = LevyraType.metadata,
                color = textColor.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal fun distinctEditorialAccents(seeds: List<Color>): List<Pair<Color, Color>> {
    val usedHues = mutableListOf<Double>()
    return seeds.map { seed ->
        val sourceHue = Hct.fromInt(seed.toArgb()).hue
        val unused = EditorialCollectionHues.filterNot { it in usedHues }
        val candidates = unused.ifEmpty {
            EditorialCollectionHues.filterNot { it == usedHues.lastOrNull() }
        }
        val hue = candidates.minBy { editorialHueDistance(it, sourceHue) }
        usedHues += hue
        Color(Hct.from(hue, EditorialAccentChroma, EditorialAccentTone).toInt()) to
            Color(Hct.from(hue, EditorialAccentChroma, EditorialAccentDeepTone).toInt())
    }
}

private fun editorialHueDistance(first: Double, second: Double): Double {
    val delta = abs(first - second) % 360.0
    return minOf(delta, 360.0 - delta)
}

private val EditorialCollectionHues = listOf(52.0, 290.0, 175.0, 345.0, 255.0, 140.0, 20.0, 215.0)
private const val EditorialAccentChroma = 48.0
private const val EditorialAccentTone = 50.0
private const val EditorialAccentDeepTone = 30.0

private val EditorialCardRestCorner = 32.dp
private val EditorialCardPressedCorner = 24.dp

private data class EditorialContrastPalette(
    val start: Color,
    val end: Color,
    val text: Color
)

private fun editorialContrastPalette(start: Color, end: Color): EditorialContrastPalette {
    var safeStart = start.copy(alpha = 1f)
    var safeEnd = end.copy(alpha = 1f)
    repeat(10) {
        editorialTextColor(safeStart, safeEnd)?.let { textColor ->
            return EditorialContrastPalette(safeStart, safeEnd, textColor)
        }
        safeStart = Color.Black.copy(alpha = 0.10f).compositeOver(safeStart)
        safeEnd = Color.Black.copy(alpha = 0.10f).compositeOver(safeEnd)
    }
    return EditorialContrastPalette(safeStart, safeEnd, Color.White)
}

private fun editorialTextColor(start: Color, end: Color): Color? {
    val whiteContrast = minOf(
        contrastRatio(Color.White, start),
        contrastRatio(Color.White, end)
    )
    if (whiteContrast >= EditorialMinimumContrast) return Color.White

    val effectiveStart = Color.Black.copy(alpha = 0.20f).compositeOver(start)
    val effectiveEnd = Color.Black.copy(alpha = 0.12f).compositeOver(end)
    val blackContrast = minOf(
        contrastRatio(Color.Black, effectiveStart),
        contrastRatio(Color.Black, effectiveEnd)
    )
    return Color.Black.takeIf { blackContrast >= EditorialMinimumContrast }
}

private fun contrastRatio(foreground: Color, background: Color): Float {
    val foregroundLuminance = foreground.luminance()
    val backgroundLuminance = background.luminance()
    val lighter = maxOf(foregroundLuminance, backgroundLuminance)
    val darker = minOf(foregroundLuminance, backgroundLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private const val EditorialMinimumContrast = 4.5f
