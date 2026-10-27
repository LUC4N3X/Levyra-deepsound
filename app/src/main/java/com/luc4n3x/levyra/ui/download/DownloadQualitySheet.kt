package com.luc4n3x.levyra.ui.download

import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.domain.DownloadQualityChoice
import com.luc4n3x.levyra.player.offline.DownloadQualityOption
import com.luc4n3x.levyra.player.offline.DownloadQualityPhase
import com.luc4n3x.levyra.player.offline.DownloadQualityTier
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraMorphShape
import com.luc4n3x.levyra.ui.i18n.DownloadQualityCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.PrismAccent
import com.luc4n3x.levyra.ui.theme.PrismDesign
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign
import com.luc4n3x.levyra.viewmodel.DownloadQualityPickerState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.launch

internal enum class DownloadQualitySkin { Expressive, Nocturne, Prism, Torque, Ivory, Spotify, Apple, YouTube }

internal fun activeDownloadQualitySkin(): DownloadQualitySkin = when {
    LevyraIsTorque -> DownloadQualitySkin.Torque
    LevyraIsIvory -> DownloadQualitySkin.Ivory
    LevyraIsSpotify -> DownloadQualitySkin.Spotify
    LevyraIsAppleMusic -> DownloadQualitySkin.Apple
    LevyraIsYouTubeMusic -> DownloadQualitySkin.YouTube
    LevyraIsPrism -> DownloadQualitySkin.Prism
    LevyraIsNocturne -> DownloadQualitySkin.Nocturne
    else -> DownloadQualitySkin.Expressive
}

private class QualitySheetStyle(
    val skin: DownloadQualitySkin,
    val container: Color,
    val content: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val cardIdle: Color,
    val cardSelected: Color,
    val onCardSelected: Color,
    val hairline: Color,
    val sheetShape: Shape,
    val cardShape: Shape,
    val coverShape: Shape,
    val buttonShape: Shape,
    val buttonBrush: Brush,
    val uppercase: Boolean,
    val numerals: FontFamily?
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val LosslessBadgeMorph: Morph by lazy {
    Morph(MaterialShapes.Circle.normalized(), MaterialShapes.SoftBurst.normalized())
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val LossyBadgeMorph: Morph by lazy {
    Morph(MaterialShapes.Circle.normalized(), MaterialShapes.Cookie9Sided.normalized())
}

@Composable
internal fun DownloadQualityPickerHost(viewModel: LevyraViewModel) {
    val picker by viewModel.downloadQualityPicker.collectAsStateWithLifecycle()
    val state = picker ?: return
    DownloadQualitySheet(
        state = state,
        onSelect = viewModel::selectDownloadQuality,
        onConfirm = viewModel::confirmDownloadQuality,
        onRetry = viewModel::retryDownloadQualityProbe,
        onDismiss = viewModel::dismissDownloadQualityPicker
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadQualitySheet(
    state: DownloadQualityPickerState,
    onSelect: (DownloadQualityChoice) -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val copy = LocalLevyraStrings.current.downloadQuality
    val downloadLabel = LocalLevyraStrings.current.download
    val style = rememberQualitySheetStyle(activeDownloadQualitySkin())
    val haptics = LocalLevyraHaptics.current
    val animate = LocalAnimationsEnabled.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var committed by remember { mutableStateOf(false) }
    val phase = state.snapshot.phase
    val canDownload = phase == DownloadQualityPhase.Ready && state.selected != null && !committed

    val confirm: () -> Unit = confirm@{
        if (!canDownload) return@confirm
        committed = true
        haptics.perform(LevyraHapticAction.Confirm)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onConfirm() }
    }
    val select: (DownloadQualityChoice) -> Unit = { choice ->
        if (choice != state.selected) haptics.perform(LevyraHapticAction.SeekSnap)
        onSelect(choice)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = style.container,
        contentColor = style.content,
        shape = style.sheetShape,
        dragHandle = { QualityDragHandle(style) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = gutterFor(style.skin))
                .padding(bottom = 20.dp)
        ) {
            QualityHeader(state, copy, style)
            Spacer(Modifier.height(20.dp))
            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    if (animate) fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                    else fadeIn(snap()) togetherWith fadeOut(snap())
                },
                contentKey = { it },
                label = "download-quality-phase"
            ) { target ->
                Column {
                    when (target) {
                        DownloadQualityPhase.Loading -> QualityLoading(copy, style)
                        DownloadQualityPhase.Ready -> QualityOptionList(state, copy, style, select)
                        DownloadQualityPhase.Unavailable -> QualityMessage(
                            icon = Icons.Rounded.ErrorOutline,
                            title = copy.unavailableTitle,
                            body = copy.unavailableBody,
                            action = copy.retry,
                            style = style,
                            onAction = onRetry
                        )
                        DownloadQualityPhase.Error -> QualityMessage(
                            icon = Icons.Rounded.CloudOff,
                            title = copy.errorTitle,
                            body = copy.errorBody,
                            action = copy.retry,
                            style = style,
                            onAction = onRetry
                        )
                    }
                }
            }
            if (phase == DownloadQualityPhase.Ready || phase == DownloadQualityPhase.Loading) {
                Spacer(Modifier.height(20.dp))
                QualityPrimaryButton(
                    text = downloadLabel,
                    enabled = canDownload,
                    style = style,
                    onClick = confirm
                )
            }
        }
    }
}

@Composable
private fun rememberQualitySheetStyle(skin: DownloadQualitySkin): QualitySheetStyle {
    val scheme = MaterialTheme.colorScheme
    val palette = LevyraActivePalette
    return remember(skin, scheme, palette) {
        val light = palette.isLight
        when (skin) {
            DownloadQualitySkin.Expressive -> QualitySheetStyle(
                skin = skin,
                container = scheme.surfaceContainerLow,
                content = scheme.onSurface,
                muted = scheme.onSurfaceVariant,
                accent = scheme.primary,
                onAccent = scheme.onPrimary,
                cardIdle = scheme.surfaceContainerHigh,
                cardSelected = scheme.primaryContainer,
                onCardSelected = scheme.onPrimaryContainer,
                hairline = scheme.outlineVariant,
                sheetShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                cardShape = RoundedCornerShape(20.dp),
                coverShape = RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomEnd = 28.dp, bottomStart = 12.dp),
                buttonShape = CircleShape,
                buttonBrush = SolidColor(scheme.primary),
                uppercase = false,
                numerals = null
            )
            DownloadQualitySkin.Nocturne -> QualitySheetStyle(
                skin = skin,
                container = palette.panel,
                content = palette.text,
                muted = palette.muted,
                accent = palette.cyan,
                onAccent = palette.black,
                cardIdle = palette.panelSoft,
                cardSelected = Nocturne78Design.variant,
                onCardSelected = palette.text,
                hairline = palette.text.copy(alpha = 0.10f),
                sheetShape = RoundedCornerShape(topStart = Nocturne78Design.panelCorner, topEnd = Nocturne78Design.panelCorner),
                cardShape = RoundedCornerShape(Nocturne78Design.cardCorner),
                coverShape = RoundedCornerShape(Nocturne78Design.artworkCorner),
                buttonShape = RoundedCornerShape(Nocturne78Design.controlCorner),
                buttonBrush = SolidColor(palette.cyan),
                uppercase = true,
                numerals = FontFamily.Monospace
            )
            DownloadQualitySkin.Prism -> QualitySheetStyle(
                skin = skin,
                container = palette.black,
                content = palette.text,
                muted = palette.muted,
                accent = palette.cyan,
                onAccent = palette.black,
                cardIdle = palette.panel,
                cardSelected = palette.cyan.copy(alpha = if (light) 0.10f else 0.16f),
                onCardSelected = palette.text,
                hairline = palette.text.copy(alpha = if (light) 0.10f else 0.08f),
                sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                cardShape = PrismDesign.cut(14.dp),
                coverShape = PrismDesign.cut(12.dp),
                buttonShape = PrismDesign.cut(14.dp),
                buttonBrush = PrismAccent.gradient(light),
                uppercase = false,
                numerals = null
            )
            DownloadQualitySkin.Torque -> QualitySheetStyle(
                skin = skin,
                container = palette.panel,
                content = palette.text,
                muted = palette.muted,
                accent = palette.blue,
                onAccent = Color.White,
                cardIdle = palette.panelSoft,
                cardSelected = palette.blue.copy(alpha = 0.14f),
                onCardSelected = palette.text,
                hairline = palette.text.copy(alpha = 0.12f),
                sheetShape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
                cardShape = RoundedCornerShape(2.dp),
                coverShape = RoundedCornerShape(0.dp),
                buttonShape = RoundedCornerShape(2.dp),
                buttonBrush = SolidColor(palette.blue),
                uppercase = true,
                numerals = FontFamily.Monospace
            )
            DownloadQualitySkin.Ivory -> QualitySheetStyle(
                skin = skin,
                container = palette.black,
                content = palette.text,
                muted = palette.muted,
                accent = IvoryDesign.ink,
                onAccent = IvoryDesign.onInk,
                cardIdle = IvoryDesign.well,
                cardSelected = IvoryDesign.ink,
                onCardSelected = IvoryDesign.onInk,
                hairline = palette.text.copy(alpha = 0.08f),
                sheetShape = RoundedCornerShape(topStart = IvoryDesign.surfaceCorner, topEnd = IvoryDesign.surfaceCorner),
                cardShape = RoundedCornerShape(IvoryDesign.surfaceCorner),
                coverShape = IvoryDesign.coverShape,
                buttonShape = CircleShape,
                buttonBrush = SolidColor(IvoryDesign.ink),
                uppercase = false,
                numerals = null
            )
            DownloadQualitySkin.Spotify -> QualitySheetStyle(
                skin = skin,
                container = SpotifyDesign.sheet,
                content = SpotifyDesign.text,
                muted = SpotifyDesign.subdued,
                accent = SpotifyDesign.green,
                onAccent = Color.Black,
                cardIdle = Color.Transparent,
                cardSelected = SpotifyDesign.tinted,
                onCardSelected = SpotifyDesign.text,
                hairline = SpotifyDesign.decorative,
                sheetShape = RoundedCornerShape(topStart = SpotifyDesign.cornerLarge, topEnd = SpotifyDesign.cornerLarge),
                cardShape = RoundedCornerShape(SpotifyDesign.corner),
                coverShape = RoundedCornerShape(SpotifyDesign.cornerSmall),
                buttonShape = CircleShape,
                buttonBrush = SolidColor(SpotifyDesign.green),
                uppercase = false,
                numerals = null
            )
            DownloadQualitySkin.Apple -> QualitySheetStyle(
                skin = skin,
                container = AppleMusicDesign.groupedBackground,
                content = AppleMusicDesign.label,
                muted = AppleMusicDesign.secondaryLabel,
                accent = AppleMusicDesign.accent,
                onAccent = Color.White,
                cardIdle = if (light) AppleMusicDesign.background else AppleMusicDesign.raised,
                cardSelected = if (light) AppleMusicDesign.background else AppleMusicDesign.raised,
                onCardSelected = AppleMusicDesign.label,
                hairline = AppleMusicDesign.separator,
                sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                cardShape = RoundedCornerShape(AppleMusicDesign.cornerL),
                coverShape = RoundedCornerShape(AppleMusicDesign.cornerM),
                buttonShape = RoundedCornerShape(AppleMusicDesign.cornerL),
                buttonBrush = SolidColor(AppleMusicDesign.accent),
                uppercase = false,
                numerals = null
            )
            DownloadQualitySkin.YouTube -> QualitySheetStyle(
                skin = skin,
                container = YouTubeMusicDesign.surface,
                content = YouTubeMusicDesign.text,
                muted = YouTubeMusicDesign.textSecondary,
                accent = Color.White,
                onAccent = Color.Black,
                cardIdle = Color.Transparent,
                cardSelected = YouTubeMusicDesign.buttonFill,
                onCardSelected = YouTubeMusicDesign.text,
                hairline = YouTubeMusicDesign.divider,
                sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                cardShape = RoundedCornerShape(YouTubeMusicDesign.chipCorner),
                coverShape = RoundedCornerShape(YouTubeMusicDesign.artworkCorner),
                buttonShape = CircleShape,
                buttonBrush = SolidColor(Color.White),
                uppercase = false,
                numerals = null
            )
        }
    }
}

private fun gutterFor(skin: DownloadQualitySkin): Dp = when (skin) {
    DownloadQualitySkin.Apple -> AppleMusicDesign.gutter
    DownloadQualitySkin.Spotify -> SpotifyDesign.gutter
    DownloadQualitySkin.YouTube -> YouTubeMusicDesign.gutter
    else -> 20.dp
}

@Composable
private fun QualityDragHandle(style: QualitySheetStyle) {
    val (width, height, shape) = when (style.skin) {
        DownloadQualitySkin.Torque -> Triple(40.dp, 3.dp, RoundedCornerShape(0.dp))
        DownloadQualitySkin.Nocturne -> Triple(28.dp, 3.dp, RoundedCornerShape(2.dp))
        DownloadQualitySkin.Apple -> Triple(36.dp, 5.dp, CircleShape)
        else -> Triple(32.dp, 4.dp, CircleShape)
    }
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width, height)
            .clip(shape)
            .background(style.muted.copy(alpha = 0.4f))
    )
}

@Composable
private fun QualityHeader(state: DownloadQualityPickerState, copy: DownloadQualityCopy, style: QualitySheetStyle) {
    val track = state.track
    val expressive = style.skin == DownloadQualitySkin.Expressive
    val glow = if (expressive) {
        val fallback = remember(style.accent) { ArtworkPalette(style.accent.toArgb(), style.accent.toArgb()) }
        val palette by rememberArtworkPalette(
            paletteKey = "download-quality:${track.id}",
            artworkUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
            fallback = fallback
        )
        Color(palette.start)
    } else {
        Color.Transparent
    }
    val locale = Locale.getDefault()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            if (expressive) {
                Canvas(Modifier.size(104.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent)),
                        radius = size.minDimension / 2f
                    )
                }
            }
            CoverImage(
                track = track,
                modifier = Modifier
                    .size(if (expressive) 76.dp else 64.dp)
                    .clip(style.coverShape)
            )
        }
        Spacer(Modifier.width(if (expressive) 8.dp else 16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (style.uppercase) copy.title.uppercase(locale) else copy.title,
                color = if (style.skin == DownloadQualitySkin.Spotify || style.skin == DownloadQualitySkin.YouTube) style.muted else style.accent,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = if (style.uppercase) 1.4.sp else 0.2.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = track.title,
                color = style.content,
                style = if (expressive) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                fontWeight = if (expressive) FontWeight.ExtraBold else FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = style.muted,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QualityLoading(copy: DownloadQualityCopy, style: QualitySheetStyle) {
    val animate = LocalAnimationsEnabled.current
    val pulse = if (animate) {
        val transition = rememberInfiniteTransition(label = "download-quality-skeleton")
        transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "download-quality-skeleton-alpha"
        ).value
    } else {
        0.7f
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (style.skin == DownloadQualitySkin.Expressive) {
                LevyraLoadingIndicator(color = style.accent, modifier = Modifier.size(36.dp))
            } else {
                CircularProgressIndicator(color = style.accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = label(copy.checking, style),
                color = style.muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .alpha(pulse * (1f - index * 0.18f))
                    .clip(style.cardShape)
                    .background(skeletonColor(style))
            )
        }
    }
}

private fun skeletonColor(style: QualitySheetStyle): Color = when (style.skin) {
    DownloadQualitySkin.Spotify -> SpotifyDesign.highlight
    DownloadQualitySkin.YouTube -> YouTubeMusicDesign.surfaceRaised
    else -> style.cardIdle
}

@Composable
private fun QualityOptionList(
    state: DownloadQualityPickerState,
    copy: DownloadQualityCopy,
    style: QualitySheetStyle,
    onSelect: (DownloadQualityChoice) -> Unit
) {
    val options = state.snapshot.options
    val grouped = style.skin == DownloadQualitySkin.Apple
    Column(
        verticalArrangement = Arrangement.spacedBy(if (grouped) 0.dp else listGap(style.skin)),
        modifier = Modifier
            .selectableGroup()
            .then(if (grouped) Modifier.clip(style.cardShape).background(style.cardIdle) else Modifier)
    ) {
        options.forEachIndexed { index, option ->
            val selected = option.choice == state.selected
            val select = { onSelect(option.choice) }
            when (style.skin) {
                DownloadQualitySkin.Expressive -> ExpressiveOption(option, index == 0, selected, copy, style, select)
                DownloadQualitySkin.Nocturne -> NocturneOption(option, selected, copy, style, select)
                DownloadQualitySkin.Prism -> PrismOption(option, index == 0, selected, copy, style, select)
                DownloadQualitySkin.Torque -> TorqueOption(option, index, selected, copy, style, select)
                DownloadQualitySkin.Ivory -> IvoryOption(option, index == 0, selected, copy, style, select)
                DownloadQualitySkin.Spotify -> SpotifyOption(option, selected, copy, style, select)
                DownloadQualitySkin.Apple -> AppleOption(option, selected, index < options.lastIndex, copy, style, select)
                DownloadQualitySkin.YouTube -> YouTubeOption(option, selected, copy, style, select)
            }
        }
    }
    if (state.snapshot.checkingMore) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 14.dp)
        ) {
            CircularProgressIndicator(color = style.muted, strokeWidth = 1.5.dp, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(10.dp))
            Text(label(copy.checkingMore, style), color = style.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun listGap(skin: DownloadQualitySkin): Dp = when (skin) {
    DownloadQualitySkin.Spotify, DownloadQualitySkin.YouTube -> 2.dp
    DownloadQualitySkin.Torque -> 6.dp
    else -> 10.dp
}

private fun Modifier.optionSelectable(selected: Boolean, onClick: () -> Unit): Modifier =
    selectable(selected = selected, role = Role.RadioButton, onClick = onClick)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun animatedProgress(selected: Boolean): Float {
    val animate = LocalAnimationsEnabled.current
    val spec: FiniteAnimationSpec<Float> = if (animate) MaterialTheme.motionScheme.defaultSpatialSpec() else snap()
    return animateFloatAsState(if (selected) 1f else 0f, spec, label = "download-quality-selection").value
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun animatedColor(target: Color): Color {
    val animate = LocalAnimationsEnabled.current
    val spec: FiniteAnimationSpec<Color> = if (animate) MaterialTheme.motionScheme.defaultEffectsSpec() else snap()
    return animateColorAsState(target, spec, label = "download-quality-color").value
}

@Composable
private fun ExpressiveOption(
    option: DownloadQualityOption,
    best: Boolean,
    selected: Boolean,
    copy: DownloadQualityCopy,
    style: QualitySheetStyle,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val progress = animatedProgress(selected)
    val lossless = option.lossless
    val container = animatedColor(
        when {
            selected && lossless -> scheme.tertiaryContainer
            selected -> style.cardSelected
            else -> style.cardIdle
        }
    )
    val onContainer = animatedColor(
        when {
            selected && lossless -> scheme.onTertiaryContainer
            selected -> style.onCardSelected
            else -> style.content
        }
    )
    val large = 20.dp + 12.dp * progress
    val small = 20.dp - 8.dp * progress
    val shape = RoundedCornerShape(topStart = large, topEnd = small, bottomEnd = large, bottomStart = small)
    val badgeColor = if (lossless) scheme.tertiary else scheme.primary
    val onBadge = if (lossless) scheme.onTertiary else scheme.onPrimary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .graphicsLayer {
                val scale = 1f + 0.015f * progress
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                if (selected && lossless) {
                    Brush.linearGradient(listOf(container, lerp(container, scheme.primaryContainer, 0.55f)))
                } else {
                    SolidColor(container)
                }
            )
            .optionSelectable(selected, onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(LevyraMorphShape(if (lossless) LosslessBadgeMorph else LossyBadgeMorph, progress, 30f * progress))
                .background(lerp(badgeColor.copy(alpha = 0.16f), badgeColor, progress))
        ) {
            Icon(
                imageVector = tierIcon(option.tier),
                contentDescription = null,
                tint = lerp(badgeColor, onBadge, progress),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (best) {
                Text(
                    text = copy.best,
                    color = if (selected) onContainer else badgeColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tierTitle(option.tier, copy),
                    color = onContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (lossless) {
                    Spacer(Modifier.width(8.dp))
                    SpectrumGlyph(active = selected, color = onContainer)
                }
            }
            Text(
                text = technicalLine(option, copy),
                color = onContainer.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
            Box(
                Modifier
                    .size(22.dp)
                    .border(2.dp, style.muted.copy(alpha = 1f - progress), CircleShape)
            )
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = if (lossless) scheme.tertiary else scheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = progress
                        scaleY = progress
                        alpha = progress
                    }
            )
        }
    }
}

private val StaticSpectrum = floatArrayOf(0.5f, 0.85f, 1f, 0.7f, 0.45f)

@Composable
private fun SpectrumGlyph(active: Boolean, color: Color) {
    val animate = LocalAnimationsEnabled.current && active
    val phase = if (animate) {
        val transition = rememberInfiniteTransition(label = "download-quality-spectrum")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1_400, easing = LinearEasing)),
            label = "download-quality-spectrum-phase"
        ).value
    } else {
        0.2f
    }
    Canvas(Modifier.size(width = 22.dp, height = 14.dp)) {
        val bars = 5
        val gap = size.width / (bars * 2 - 1)
        for (bar in 0 until bars) {
            val wave = (sin((phase + bar * 0.17f) * 2f * PI.toFloat()) + 1f) / 2f
            val fraction = 0.3f + 0.7f * if (animate) wave else StaticSpectrum[bar]
            val height = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(bar * gap * 2, size.height - height),
                size = Size(gap, height),
                cornerRadius = CornerRadius(gap / 2f)
            )
        }
    }
}

@Composable
private fun NocturneOption(
    option: DownloadQualityOption,
    selected: Boolean,
    copy: DownloadQualityCopy,
    style: QualitySheetStyle,
    onClick: () -> Unit
) {
    val container = animatedColor(if (selected) style.cardSelected else style.cardIdle)
    val led = animatedColor(if (selected) style.accent else style.muted.copy(alpha = 0.3f))
    val lit = when (option.tier) {
        DownloadQualityTier.HiResLossless -> 5
        DownloadQualityTier.Lossless -> 4
        DownloadQualityTier.High -> 3
        DownloadQualityTier.Standard -> 2
        DownloadQualityTier.Compact -> 1
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(style.cardShape)
            .background(container)
            .border(1.dp, if (selected) style.accent.copy(alpha = 0.5f) else style.hairline, style.cardShape)
            .optionSelectable(selected, onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(led)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label(tierTitle(option.tier, copy), style),
                color = style.content,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = technicalLine(option, copy),
                color = style.muted,
                fontFamily = style.numerals,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
            repeat(5) { segment ->
                Box(
                    Modifier
                        .size(width = 4.dp, height = (8 + segment * 3).dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (segment < lit) led else style.hairline)
                )
            }
        }
    }
}

@Composable
private fun PrismOption(
    option: DownloadQualityOption,
    best: Boolean,
    selected: Boolean,
    copy: DownloadQualityCopy,
    style: QualitySheetStyle,
    onClick: () -> Unit
) {
    val container = animatedColor(if (selected) style.cardSelected else style.cardIdle)
    val light = LevyraActivePalette.isLight
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(style.cardShape)
            .background(container)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                brush = if (selected) PrismAccent.gradient(light) else SolidColor(style.hairline),
                shape = style.cardShape
            )
            .optionSelectable(selected, onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tierTitle(option.tier, copy),
                    color = style.content,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (best) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = copy.best,
                        color = style.accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(technicalLine(option, copy), color = style.muted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(PrismDesign.cut(6.dp))
                .background(if (selected) PrismAccent.gradient(light) else SolidColor(Color.Transparent))
                .border(1.dp, if (selected) Color.Transparent else style.muted.copy(alpha = 0.5f), PrismDesign.cut(6.dp))
        ) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = style.onAccent, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TorqueOption(
    option: DownloadQualityOption,
    index: Int,
    selected: Boolean,
    copy: DownloadQualityCopy,
    style: QualitySheetStyle,
    onClick: () -> Unit
) {
    val container = animatedColor(if (selected) style.cardSelected else style.cardIdle)
    val strip = animatedColor(if (selected) style.accent else style.hairline)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 68.dp)
            .clip(style.cardShape)
            .background(container)
            .optionSelectable(selected, onClick)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(strip)
        )
        Text(
            text = String.format(Locale.ROOT, "%02d", index + 1),
            color = if (selected) style.accent else style.muted,
            fontFamily = style.numerals,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp)
        )
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = label(tierTitle(option.tier, copy), style),
                color = style.content,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = label(technicalLine(option, copy), style),
                color = style.muted,
                fontFamily = style.numerals,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = style.accent,
                modifier = Modifier.padding(end = 14.dp).size(22.dp)
            )
        }
    }
}

@Composable
private fun IvoryOption(
    option: DownloadQualityOption,
    best: Boolean,
    selected: Boolean,
    copy: DownloadQualityCopy,
    style: QualitySheetStyle,
    onClick: () -> Unit
) {
    val container = animatedColor(if (selected) style.cardSelected else style.cardIdle)
    val content = animatedColor(if (selected) style.onCardSelected else style.content)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(style.cardShape)
            .background(container)
            .optionSelectable(selected, onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = tierTitle(option.tier, copy),
                color = content,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (best) "${copy.best} · ${technicalLine(option, copy)}" else technicalLine(option, copy),
                color = content.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (selected) content else Color.Transparent)
                .border(1.5.dp, content.copy(alpha = if (selected) 0f else 0.4f), CircleShape)
        )
    }
}

@Composable
private fun SpotifyOption(
    option: DownloadQualityOption,
    selected: Boolean,
    copy: DownloadQualityCopy,
    style: QualitySheetStyle,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SpotifyDesign.rowCover + 16.dp)
            .clip(style.cardShape)
            .optionSelectable(selected, onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(SpotifyDesign.rowCover)
                .clip(RoundedCornerShape(SpotifyDesign.cornerSmall))
                .background(SpotifyDesign.highlight)
        ) {
            Icon(tierIcon(option.tier), null, tint = if (selected) style.accent else style.muted, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = tierTitle(option.tier, copy),
                color = if (selected) style.accent else style.content,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(technicalLine(option, copy), color = style.muted, fontSize = 13.sp)
        }
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, null, tint = style.accent, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun AppleOption(
    option: DownloadQualityOption,
    selected: Boolean,
    showSeparator: Boolean,
    copy: DownloadQualityCopy,
    style: QualitySheetStyle,
    onClick: () -> Unit
) {
    Column(Modifier.optionSelectable(selected, onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppleMusicDesign.songRow)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(tierTitle(option.tier, copy), color = style.content, fontSize = 17.sp)
                Text(technicalLine(option, copy), color = style.muted, fontSize = 13.sp)
            }
            if (selected) {
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Rounded.Check, null, tint = style.accent, modifier = Modifier.size(22.dp))
            }
        }
        if (showSeparator) {
            HorizontalDivider(thickness = 0.5.dp, color = style.hairline, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun YouTubeOption(
    option: DownloadQualityOption,
    selected: Boolean,
    copy: DownloadQualityCopy,
    style: QualitySheetStyle,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = YouTubeMusicDesign.compactRow)
            .clip(style.cardShape)
            .background(if (selected) style.cardSelected else style.cardIdle)
            .optionSelectable(selected, onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) style.content else YouTubeMusicDesign.textTertiary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(tierTitle(option.tier, copy), color = style.content, style = MaterialTheme.typography.titleMedium)
            Text(technicalLine(option, copy), color = style.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QualityMessage(
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    style: QualitySheetStyle,
    onAction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(if (style.skin == DownloadQualitySkin.Expressive) MaterialShapes.Cookie9Sided.toShape() else style.cardShape)
                .background(style.cardIdle.takeUnless { it == Color.Transparent } ?: skeletonColor(style))
        ) {
            Icon(icon, null, tint = style.muted, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(label(title, style), color = style.content, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(body, color = style.muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(18.dp))
        QualityPrimaryButton(text = action, enabled = true, style = style, onClick = onAction, icon = null)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QualityPrimaryButton(
    text: String,
    enabled: Boolean,
    style: QualitySheetStyle,
    onClick: () -> Unit,
    icon: ImageVector? = Icons.Rounded.Download
) {
    if (style.skin == DownloadQualitySkin.Expressive) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(containerColor = style.accent, contentColor = style.onAccent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        return
    }
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(style.buttonShape)
            .background(style.buttonBrush)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = style.onAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label(text, style),
            color = style.onAccent,
            fontWeight = if (style.uppercase) FontWeight.Black else FontWeight.Bold,
            letterSpacing = if (style.uppercase) 1.2.sp else 0.sp,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

private fun tierTitle(tier: DownloadQualityTier, copy: DownloadQualityCopy): String = when (tier) {
    DownloadQualityTier.HiResLossless -> copy.hiResLossless
    DownloadQualityTier.Lossless -> copy.lossless
    DownloadQualityTier.High -> copy.high
    DownloadQualityTier.Standard -> copy.standard
    DownloadQualityTier.Compact -> copy.compact
}

private fun tierIcon(tier: DownloadQualityTier): ImageVector = when (tier) {
    DownloadQualityTier.HiResLossless, DownloadQualityTier.Lossless -> Icons.Rounded.GraphicEq
    DownloadQualityTier.High -> Icons.Rounded.HighQuality
    DownloadQualityTier.Standard, DownloadQualityTier.Compact -> Icons.Rounded.MusicNote
}

private fun label(text: String, style: QualitySheetStyle): String =
    if (style.uppercase) text.uppercase(Locale.getDefault()) else text

@Composable
private fun technicalLine(option: DownloadQualityOption, copy: DownloadQualityCopy): String {
    val context = LocalContext.current
    return remember(option, copy, context) {
        buildList {
            add(option.codecLabel)
            if (option.lossless) {
                if (option.bitDepth > 0) add("${option.bitDepth}-bit")
                if (option.sampleRateHz > 0) add(kilohertz(option.sampleRateHz))
            } else {
                if (option.kbps > 0) add("${option.kbps} kbps")
                add(copy.compressed)
            }
            if (option.estimatedBytes > 0L) add("≈ ${Formatter.formatShortFileSize(context, option.estimatedBytes)}")
        }.joinToString(" · ")
    }
}

private fun kilohertz(sampleRateHz: Int): String =
    if (sampleRateHz % 1_000 == 0) "${sampleRateHz / 1_000} kHz"
    else String.format(Locale.getDefault(), "%.1f kHz", sampleRateHz / 1_000.0)
