package com.luc4n3x.levyra.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraMiniPlayerStudioSettings
import com.luc4n3x.levyra.domain.MiniArtworkShape
import com.luc4n3x.levyra.domain.MiniBackground
import com.luc4n3x.levyra.domain.MiniButtonStyle
import com.luc4n3x.levyra.domain.MiniColorMode
import com.luc4n3x.levyra.domain.MiniControlSet
import com.luc4n3x.levyra.domain.MiniMotion
import com.luc4n3x.levyra.domain.MiniPlayerSpec
import com.luc4n3x.levyra.domain.MiniPlayerStyle
import com.luc4n3x.levyra.domain.MiniPlayerTuning
import com.luc4n3x.levyra.domain.MiniProgressStyle
import com.luc4n3x.levyra.domain.MiniTuningOption
import com.luc4n3x.levyra.domain.MiniTypography
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.MiniPlayerStudioCopy
import com.luc4n3x.levyra.ui.i18n.MiniStudioText
import com.luc4n3x.levyra.ui.i18n.homeStudioCopy
import com.luc4n3x.levyra.ui.i18n.miniPlayerStudioCopy
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import kotlin.math.roundToInt

private val StudioCardShape = RoundedCornerShape(24.dp)
private val StudioStageShape = RoundedCornerShape(32.dp)
private const val StudioTwoPaneMinWidthDp = 720

private val MiniCustomSwatches = listOf(
    0xFF7C8CFF.toInt(),
    0xFF5BD6C0.toInt(),
    0xFFFF8A65.toInt(),
    0xFFF06292.toInt(),
    0xFFFFC857.toInt(),
    0xFF9CCC65.toInt(),
    0xFFB388FF.toInt(),
    0xFF4FC3F7.toInt()
)

internal fun miniPlayerStyleName(style: MiniPlayerStyle): String = when (style) {
    MiniPlayerStyle.Original -> "Levyra Original"
    MiniPlayerStyle.SuperExpressive -> "Super Expressive"
    MiniPlayerStyle.Floating -> "Floating"
    MiniPlayerStyle.LiquidGlass -> "Liquid Glass"
    MiniPlayerStyle.UltraCompact -> "Ultra Compact"
    MiniPlayerStyle.ArtworkFocus -> "Artwork Focus"
    MiniPlayerStyle.YtMusic -> "YT Music"
    MiniPlayerStyle.Connected -> "Connected"
}

private fun miniPlayerStyleDescription(style: MiniPlayerStyle, copy: MiniPlayerStudioCopy): String = when (style) {
    MiniPlayerStyle.Original -> copy[MiniStudioText.DescOriginal]
    MiniPlayerStyle.SuperExpressive -> copy[MiniStudioText.DescSuperExpressive]
    MiniPlayerStyle.Floating -> copy[MiniStudioText.DescFloating]
    MiniPlayerStyle.LiquidGlass -> copy[MiniStudioText.DescLiquidGlass]
    MiniPlayerStyle.UltraCompact -> copy[MiniStudioText.DescUltraCompact]
    MiniPlayerStyle.ArtworkFocus -> copy[MiniStudioText.DescArtworkFocus]
    MiniPlayerStyle.YtMusic -> copy[MiniStudioText.DescYtMusic]
    MiniPlayerStyle.Connected -> copy[MiniStudioText.DescConnected]
}

internal fun parseMiniHexColor(raw: String): Int? {
    val digits = raw.trim().removePrefix("#")
    if (digits.length != 6 || digits.any { it !in "0123456789abcdefABCDEF" }) return null
    return (0xFF000000 or digits.toLong(16)).toInt()
}

internal fun formatMiniHexColor(color: Int): String = "%06X".format(color and 0xFFFFFF)

@Composable
internal fun MiniPlayerStudioOverlay(
    settings: LevyraMiniPlayerStudioSettings,
    track: Track?,
    isPlaying: Boolean,
    progress: Float,
    animated: Boolean,
    onUpdate: (LevyraMiniPlayerStudioSettings) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.miniPlayerStudioCopy
    val haptics = LocalLevyraHaptics.current
    val latestSettings by rememberUpdatedState(settings)
    var draft by remember { mutableStateOf<MiniPlayerTuning?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }
    val style = settings.style
    val tuning = draft ?: settings.activeTuning()
    val previewTrack = track ?: rememberMiniDemoTrack(copy)
    val previewModel = MiniPlayerModel(
        track = previewTrack,
        isPlaying = if (track == null) true else isPlaying,
        isResolving = false,
        progress = if (track == null) 0.42f else progress,
        bufferedProgress = 0f,
        liveNowPlaying = "",
        animated = animated,
        gesturesEnabled = false,
        swipeTrackChangeEnabled = false,
        stepDirection = 0
    )
    val thumbnailModel = remember(previewTrack, previewModel.isPlaying) {
        previewModel.copy(animated = false, progress = 0.42f)
    }
    val previewTuning: (MiniPlayerTuning) -> Unit = { next -> draft = next.normalized(style) }
    val commitTuning: (MiniPlayerTuning) -> Unit = { next ->
        val current = latestSettings
        onUpdate(current.updateTuning(current.style) { next })
        draft = null
    }
    val selectStyle: (MiniPlayerStyle) -> Unit = { target ->
        if (target != latestSettings.style) {
            draft = null
            onUpdate(latestSettings.withStyle(target))
            haptics.perform(LevyraHapticAction.Confirm)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraInk)
            .statusBarsPadding()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val twoPane = maxWidth.value >= StudioTwoPaneMinWidthDp
            val controls: LazyListScope.() -> Unit = {
                miniStudioItems(
                    settings = settings,
                    tuning = tuning,
                    copy = copy,
                    thumbnailModel = thumbnailModel,
                    onSelectStyle = selectStyle,
                    onPreview = previewTuning,
                    onCommit = commitTuning,
                    onResetStyle = {
                        draft = null
                        onUpdate(latestSettings.resetStyle(latestSettings.style))
                    }
                )
            }
            Column(modifier = Modifier.fillMaxSize()) {
                MiniStudioHeader(
                    copy = copy,
                    backLabel = strings.back,
                    restoreEnabled = settings != LevyraMiniPlayerStudioSettings(),
                    onBack = onClose,
                    onRestore = { confirmRestore = true }
                )
                if (twoPane) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        MiniStudioStage(
                            style = style,
                            tuning = tuning,
                            model = previewModel,
                            modifier = Modifier
                                .weight(0.46f)
                                .navigationBarsPadding()
                                .padding(bottom = 16.dp)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .weight(0.54f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            content = controls
                        )
                    }
                } else {
                    MiniStudioStage(
                        style = style,
                        tuning = tuning,
                        model = previewModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = controls
                    )
                }
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(copy[MiniStudioText.RestoreDefault]) },
            text = { Text(copy[MiniStudioText.RestoreConfirm]) },
            confirmButton = {
                TextButton(onClick = {
                    draft = null
                    onUpdate(LevyraMiniPlayerStudioSettings())
                    haptics.perform(LevyraHapticAction.Confirm)
                    confirmRestore = false
                }) { Text(strings.homeStudioCopy.reset) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun rememberMiniDemoTrack(copy: MiniPlayerStudioCopy): Track {
    val title = copy[MiniStudioText.DemoTitle]
    val artist = copy[MiniStudioText.DemoArtist]
    return remember(title, artist) {
        Track(
            id = "mini-player-studio-demo",
            title = title,
            artist = artist,
            album = "",
            durationMs = 0L,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "",
            moodTags = emptySet(),
            energy = 0,
            vocal = 0,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0xFF7C8CFF.toInt(),
            accentEnd = 0xFF5BD6C0.toInt()
        )
    }
}

@Composable
private fun MiniStudioHeader(
    copy: MiniPlayerStudioCopy,
    backLabel: String,
    restoreEnabled: Boolean,
    onBack: () -> Unit,
    onRestore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LevyraBackButton(onClick = onBack, contentDescription = backLabel)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        ) {
            Text(
                text = copy[MiniStudioText.Title],
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = LevyraText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = copy[MiniStudioText.Subtitle],
                style = MaterialTheme.typography.bodyMedium,
                color = LevyraMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = onRestore, enabled = restoreEnabled) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = copy[MiniStudioText.RestoreDefault],
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniStudioStage(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    model: MiniPlayerModel,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val palette = rememberMiniPlayerPalette(style, tuning, model.track, model.animated)
    Box(
        modifier = modifier
            .clip(StudioStageShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.surfaceContainerHigh.playerMix(palette.accent, 0.10f),
                        scheme.surfaceContainerLow
                    )
                )
            )
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.35f), StudioStageShape)
            .semantics { contentDescription = miniPlayerStyleName(style) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MiniStudioStageContent(accent = palette.accent)
            AnimatedContent(
                targetState = style,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(120)))
                },
                label = "mini-studio-stage"
            ) { shownStyle ->
                MiniPlayerStylePreview(
                    style = shownStyle,
                    tuning = if (shownStyle == style) tuning else LevyraMiniPlayerStudioSettings.spec(shownStyle).defaults,
                    model = model,
                    glass = null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            MiniStudioStageDock()
        }
    }
}

@Composable
private fun MiniStudioStageContent(accent: Color) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = 0.34f - index * 0.08f),
                                scheme.surfaceContainerHighest.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun MiniStudioStageDock() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainer.copy(alpha = 0.9f))
            .padding(horizontal = 28.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(width = if (index == 0) 40.dp else 18.dp, height = 18.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == 0) scheme.secondaryContainer else scheme.onSurfaceVariant.copy(alpha = 0.30f)
                    )
            )
        }
    }
}

private fun LazyListScope.miniStudioItems(
    settings: LevyraMiniPlayerStudioSettings,
    tuning: MiniPlayerTuning,
    copy: MiniPlayerStudioCopy,
    thumbnailModel: MiniPlayerModel,
    onSelectStyle: (MiniPlayerStyle) -> Unit,
    onPreview: (MiniPlayerTuning) -> Unit,
    onCommit: (MiniPlayerTuning) -> Unit,
    onResetStyle: () -> Unit
) {
    val style = settings.style
    val spec = LevyraMiniPlayerStudioSettings.spec(style)
    val options = spec.options
    item(key = "mini-style-label", contentType = "label") {
        MiniStudioLabel(copy[MiniStudioText.StyleSection])
    }
    item(key = "mini-styles", contentType = "styles") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(MiniPlayerStyle.entries, key = { it.storageId }) { option ->
                MiniStyleCard(
                    style = option,
                    selected = option == style,
                    copy = copy,
                    model = thumbnailModel,
                    tuning = settings.tuningFor(option),
                    onClick = { onSelectStyle(option) }
                )
            }
        }
    }
    item(key = "mini-style-description", contentType = "text") {
        Text(
            text = "${miniPlayerStyleName(style)} · ${miniPlayerStyleDescription(style, copy)}",
            style = MaterialTheme.typography.bodyMedium,
            color = LevyraMuted
        )
    }
    if (options.isEmpty()) {
        item(key = "mini-original-locked", contentType = "info") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StudioCardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = copy[MiniStudioText.OriginalLocked],
                    style = MaterialTheme.typography.bodyMedium,
                    color = LevyraText
                )
            }
        }
        return
    }
    miniStudioSizeItems(spec, tuning, copy, onPreview, onCommit)
    miniStudioDepthItems(spec, tuning, copy, onPreview, onCommit)
    miniStudioProgressItems(spec, tuning, copy, onPreview, onCommit)
    miniStudioControlsItems(spec, tuning, copy, onPreview, onCommit)
    miniStudioAppearanceItems(spec, tuning, copy, onPreview, onCommit)
    item(key = "mini-reset-style", contentType = "actions") {
        OutlinedButton(
            onClick = onResetStyle,
            enabled = style in settings.normalized().tunings,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(copy[MiniStudioText.ResetStyle])
        }
    }
}

private fun LazyListScope.miniStudioSizeItems(
    spec: MiniPlayerSpec,
    tuning: MiniPlayerTuning,
    copy: MiniPlayerStudioCopy,
    onPreview: (MiniPlayerTuning) -> Unit,
    onCommit: (MiniPlayerTuning) -> Unit
) {
    val options = spec.options
    val sizeOptions = listOf(
        MiniTuningOption.Height,
        MiniTuningOption.Corner,
        MiniTuningOption.Inset,
        MiniTuningOption.ArtworkSize,
        MiniTuningOption.ArtworkShape
    )
    if (sizeOptions.any { it in options }) {
        item(key = "mini-size", contentType = "group") {
            MiniStudioGroup(copy[MiniStudioText.Size]) {
                if (MiniTuningOption.Height in options) {
                    MiniStudioSlider(
                        title = copy[MiniStudioText.Height],
                        value = tuning.heightDp,
                        range = spec.height,
                        unit = "dp",
                        onPreview = { onPreview(tuning.copy(heightDp = it)) },
                        onCommit = { onCommit(tuning.copy(heightDp = it)) }
                    )
                }
                if (MiniTuningOption.Corner in options) {
                    MiniStudioSlider(
                        title = copy[MiniStudioText.Corner],
                        value = tuning.cornerDp,
                        range = spec.corner.first..minOf(spec.corner.last, tuning.heightDp / 2),
                        unit = "dp",
                        onPreview = { onPreview(tuning.copy(cornerDp = it)) },
                        onCommit = { onCommit(tuning.copy(cornerDp = it)) }
                    )
                }
                if (MiniTuningOption.Inset in options) {
                    MiniStudioSlider(
                        title = copy[MiniStudioText.Margins],
                        value = tuning.insetDp,
                        range = spec.inset,
                        unit = "dp",
                        onPreview = { onPreview(tuning.copy(insetDp = it)) },
                        onCommit = { onCommit(tuning.copy(insetDp = it)) }
                    )
                }
                if (MiniTuningOption.ArtworkSize in options) {
                    MiniStudioSlider(
                        title = copy[MiniStudioText.ArtworkSize],
                        value = tuning.artworkDp,
                        range = spec.artwork.first..minOf(spec.artwork.last, tuning.heightDp - 12),
                        unit = "dp",
                        onPreview = { onPreview(tuning.copy(artworkDp = it)) },
                        onCommit = { onCommit(tuning.copy(artworkDp = it)) }
                    )
                }
                if (MiniTuningOption.ArtworkShape in options) {
                    MiniShapePicker(
                        title = copy[MiniStudioText.ArtworkShape],
                        selected = tuning.artworkShape,
                        copy = copy,
                        onSelect = { onCommit(tuning.copy(artworkShape = it)) }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.miniStudioDepthItems(
    spec: MiniPlayerSpec,
    tuning: MiniPlayerTuning,
    copy: MiniPlayerStudioCopy,
    onPreview: (MiniPlayerTuning) -> Unit,
    onCommit: (MiniPlayerTuning) -> Unit
) {
    val options = spec.options
    val depthOptions = listOf(MiniTuningOption.Shadow, MiniTuningOption.Glow, MiniTuningOption.Opacity)
    if (depthOptions.any { it in options }) {
        item(key = "mini-depth", contentType = "group") {
            MiniStudioGroup(copy[MiniStudioText.Depth]) {
                if (MiniTuningOption.Shadow in options) {
                    MiniStudioSlider(
                        title = copy[MiniStudioText.Shadow],
                        value = tuning.shadow,
                        range = 0..100,
                        unit = "%",
                        onPreview = { onPreview(tuning.copy(shadow = it)) },
                        onCommit = { onCommit(tuning.copy(shadow = it)) }
                    )
                }
                if (MiniTuningOption.Glow in options) {
                    MiniStudioSlider(
                        title = copy[MiniStudioText.Glow],
                        value = tuning.glow,
                        range = 0..100,
                        unit = "%",
                        onPreview = { onPreview(tuning.copy(glow = it)) },
                        onCommit = { onCommit(tuning.copy(glow = it)) }
                    )
                }
                if (MiniTuningOption.Opacity in options) {
                    MiniStudioSlider(
                        title = copy[MiniStudioText.Opacity],
                        value = tuning.opacity,
                        range = spec.opacity,
                        unit = "%",
                        onPreview = { onPreview(tuning.copy(opacity = it)) },
                        onCommit = { onCommit(tuning.copy(opacity = it)) }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.miniStudioProgressItems(
    spec: MiniPlayerSpec,
    tuning: MiniPlayerTuning,
    copy: MiniPlayerStudioCopy,
    onPreview: (MiniPlayerTuning) -> Unit,
    onCommit: (MiniPlayerTuning) -> Unit
) {
    val options = spec.options
    if (MiniTuningOption.Progress in options && spec.progressStyles.size > 1) {
        item(key = "mini-progress", contentType = "group") {
            MiniStudioGroup(copy[MiniStudioText.Progress]) {
                MiniStudioChoice(
                    options = spec.progressStyles,
                    selected = tuning.progress,
                    label = { miniProgressLabel(it, copy) },
                    onSelect = { onCommit(tuning.copy(progress = it)) }
                )
            }
        }
    }
}

private fun LazyListScope.miniStudioControlsItems(
    spec: MiniPlayerSpec,
    tuning: MiniPlayerTuning,
    copy: MiniPlayerStudioCopy,
    onPreview: (MiniPlayerTuning) -> Unit,
    onCommit: (MiniPlayerTuning) -> Unit
) {
    val options = spec.options
    val controlOptions = listOf(
        MiniTuningOption.Buttons,
        MiniTuningOption.ControlScale,
        MiniTuningOption.Controls,
        MiniTuningOption.ShowClose
    )
    if (controlOptions.any { it in options }) {
        item(key = "mini-controls", contentType = "group") {
            MiniStudioGroup(copy[MiniStudioText.Controls]) {
                if (MiniTuningOption.Buttons in options && spec.buttonStyles.size > 1) {
                    MiniStudioCaption(copy[MiniStudioText.ButtonStyle])
                    MiniStudioChoice(
                        options = spec.buttonStyles,
                        selected = tuning.buttons,
                        label = { miniButtonLabel(it, copy) },
                        onSelect = { onCommit(tuning.copy(buttons = it)) }
                    )
                }
                if (MiniTuningOption.ControlScale in options) {
                    MiniStudioSlider(
                        title = copy[MiniStudioText.ControlSize],
                        value = tuning.controlScale,
                        range = MiniPlayerTuning.CONTROL_SCALE_RANGE,
                        unit = "%",
                        onPreview = { onPreview(tuning.copy(controlScale = it)) },
                        onCommit = { onCommit(tuning.copy(controlScale = it)) }
                    )
                }
                if (MiniTuningOption.Controls in options) {
                    MiniStudioCaption(copy[MiniStudioText.ControlSet])
                    MiniStudioChoice(
                        options = MiniControlSet.entries,
                        selected = tuning.controls,
                        label = { miniControlSetLabel(it, copy) },
                        onSelect = { onCommit(tuning.copy(controls = it)) }
                    )
                }
                if (MiniTuningOption.ShowClose in options) {
                    MiniStudioSwitch(
                        title = copy[MiniStudioText.ShowClose],
                        checked = tuning.showClose,
                        onCheckedChange = { onCommit(tuning.copy(showClose = it)) }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.miniStudioAppearanceItems(
    spec: MiniPlayerSpec,
    tuning: MiniPlayerTuning,
    copy: MiniPlayerStudioCopy,
    onPreview: (MiniPlayerTuning) -> Unit,
    onCommit: (MiniPlayerTuning) -> Unit
) {
    val options = spec.options
    val appearanceOptions = listOf(
        MiniTuningOption.Color,
        MiniTuningOption.Typography,
        MiniTuningOption.Motion,
        MiniTuningOption.Background
    )
    if (appearanceOptions.any { it in options }) {
        item(key = "mini-appearance", contentType = "group") {
            MiniStudioGroup(copy[MiniStudioText.Appearance]) {
                if (MiniTuningOption.Color in options) {
                    MiniStudioCaption(copy[MiniStudioText.Color])
                    MiniStudioChoice(
                        options = MiniColorMode.entries,
                        selected = tuning.colorMode,
                        label = { miniColorModeLabel(it, copy) },
                        onSelect = { onCommit(tuning.copy(colorMode = it)) }
                    )
                    if (tuning.colorMode == MiniColorMode.Custom) {
                        MiniColorEditor(
                            color = tuning.customColor,
                            copy = copy,
                            onColor = { onCommit(tuning.copy(customColor = it)) }
                        )
                    }
                }
                if (MiniTuningOption.Typography in options) {
                    MiniStudioCaption(copy[MiniStudioText.Typography])
                    MiniStudioChoice(
                        options = MiniTypography.entries,
                        selected = tuning.typography,
                        label = { miniTypographyLabel(it, copy) },
                        onSelect = { onCommit(tuning.copy(typography = it)) }
                    )
                }
                if (MiniTuningOption.Motion in options) {
                    MiniStudioMotionChoice(
                        selected = tuning.motion,
                        onSelect = { onCommit(tuning.copy(motion = it)) }
                    )
                }
                if (MiniTuningOption.Background in options && spec.backgrounds.size > 1) {
                    MiniStudioCaption(copy[MiniStudioText.Background])
                    MiniStudioChoice(
                        options = spec.backgrounds,
                        selected = tuning.background,
                        label = { miniBackgroundLabel(it, copy) },
                        onSelect = { onCommit(tuning.copy(background = it)) }
                    )
                }
            }
        }
    }
}

private fun miniProgressLabel(style: MiniProgressStyle, copy: MiniPlayerStudioCopy): String = when (style) {
    MiniProgressStyle.Ring -> copy[MiniStudioText.ProgressRing]
    MiniProgressStyle.Line -> copy[MiniStudioText.ProgressLine]
    MiniProgressStyle.Wavy -> copy[MiniStudioText.ProgressWavy]
    MiniProgressStyle.Fill -> copy[MiniStudioText.ProgressFill]
    MiniProgressStyle.Hidden -> copy[MiniStudioText.ProgressHidden]
}

private fun miniButtonLabel(style: MiniButtonStyle, copy: MiniPlayerStudioCopy): String = when (style) {
    MiniButtonStyle.Filled -> copy[MiniStudioText.ButtonFilled]
    MiniButtonStyle.Tonal -> copy[MiniStudioText.ButtonTonal]
    MiniButtonStyle.Outlined -> copy[MiniStudioText.ButtonOutlined]
    MiniButtonStyle.Plain -> copy[MiniStudioText.ButtonPlain]
}

private fun miniControlSetLabel(set: MiniControlSet, copy: MiniPlayerStudioCopy): String = when (set) {
    MiniControlSet.Essential -> copy[MiniStudioText.SetEssential]
    MiniControlSet.Standard -> copy[MiniStudioText.SetStandard]
    MiniControlSet.Full -> copy[MiniStudioText.SetFull]
}

private fun miniColorModeLabel(mode: MiniColorMode, copy: MiniPlayerStudioCopy): String = when (mode) {
    MiniColorMode.Artwork -> copy[MiniStudioText.ColorArtwork]
    MiniColorMode.Theme -> copy[MiniStudioText.ColorTheme]
    MiniColorMode.Custom -> copy[MiniStudioText.ColorCustom]
}

private fun miniTypographyLabel(typography: MiniTypography, copy: MiniPlayerStudioCopy): String = when (typography) {
    MiniTypography.Standard -> copy[MiniStudioText.TypoStandard]
    MiniTypography.Bold -> copy[MiniStudioText.TypoBold]
    MiniTypography.Editorial -> copy[MiniStudioText.TypoEditorial]
}

private fun miniBackgroundLabel(background: MiniBackground, copy: MiniPlayerStudioCopy): String = when (background) {
    MiniBackground.Gradient -> copy[MiniStudioText.BgGradient]
    MiniBackground.Solid -> copy[MiniStudioText.BgSolid]
    MiniBackground.Artwork -> copy[MiniStudioText.BgArtwork]
}

private fun miniShapeLabel(shape: MiniArtworkShape, copy: MiniPlayerStudioCopy): String = when (shape) {
    MiniArtworkShape.Cookie -> copy[MiniStudioText.ShapeCookie]
    MiniArtworkShape.Circle -> copy[MiniStudioText.ShapeCircle]
    MiniArtworkShape.Rounded -> copy[MiniStudioText.ShapeRounded]
    MiniArtworkShape.Clover -> copy[MiniStudioText.ShapeClover]
    MiniArtworkShape.Square -> copy[MiniStudioText.ShapeSquare]
}

@Composable
internal fun MiniStudioLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = LevyraText,
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { heading() }
    )
}

@Composable
internal fun MiniStudioCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = LevyraMuted,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
internal fun MiniStudioGroup(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StudioCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = LevyraText,
            modifier = Modifier.semantics { heading() }
        )
        content()
    }
}

@Composable
private fun MiniStyleCard(
    style: MiniPlayerStyle,
    selected: Boolean,
    copy: MiniPlayerStudioCopy,
    model: MiniPlayerModel,
    tuning: MiniPlayerTuning,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val name = miniPlayerStyleName(style)
    val isDefault = style == MiniPlayerStyle.Original
    val selectedLabel = copy[MiniStudioText.Selected]
    val defaultLabel = copy[MiniStudioText.DefaultBadge]
    Column(
        modifier = Modifier
            .width(168.dp)
            .clip(StudioCardShape)
            .background(if (selected) scheme.secondaryContainer else scheme.surfaceContainerHigh)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.5f),
                shape = StudioCardShape
            )
            .levyraPressable(onClick = onClick, onClickLabel = name, role = Role.RadioButton)
            .semantics {
                this.selected = selected
                stateDescription = listOfNotNull(
                    selectedLabel.takeIf { selected },
                    defaultLabel.takeIf { isDefault }
                ).joinToString(", ")
            }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center
        ) {
            MiniPlayerStylePreview(
                style = style,
                tuning = tuning,
                model = model,
                glass = null,
                modifier = Modifier
                    .requiredWidth(320.dp)
                    .graphicsLayer {
                        scaleX = 0.46f
                        scaleY = 0.46f
                    }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (selected) scheme.onSecondaryContainer else LevyraText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        Box(modifier = Modifier.height(22.dp), contentAlignment = Alignment.CenterStart) {
            if (isDefault) {
                MiniDefaultBadge(defaultLabel)
            }
        }
    }
}

@Composable
internal fun MiniDefaultBadge(label: String) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = scheme.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, scheme.tertiary.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
internal fun MiniStudioSlider(
    title: String,
    value: Int,
    range: IntRange,
    unit: String,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit
) {
    var dragValue by remember { mutableStateOf<Int?>(null) }
    val shown = dragValue ?: value
    val adjustable = range.last > range.first
    val safeRange = if (adjustable) range else range.first..(range.first + 1)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = LevyraText,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$shown $unit",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        val sliderRange = safeRange.first.toFloat()..safeRange.last.toFloat()
        val sliderState = remember(sliderRange) { SliderState(shown.toFloat(), 0, sliderRange) }
        sliderState.value = shown.toFloat()
        Slider(
            state = sliderState,
            onValueChange = { raw ->
                val next = raw.roundToInt().coerceIn(safeRange)
                if (next != dragValue) {
                    dragValue = next
                    onPreview(next)
                }
            },
            onValueChangeFinished = {
                dragValue?.let(onCommit)
                dragValue = null
            },
            enabled = adjustable,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = title }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun <T> MiniStudioChoice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MiniStudioMotionChoice(selected: MiniMotion, onSelect: (MiniMotion) -> Unit) {
    val home = LocalLevyraStrings.current.homeStudioCopy
    MiniStudioCaption(home.motion)
    MiniStudioChoice(
        options = MiniMotion.entries,
        selected = selected,
        label = { motion ->
            when (motion) {
                MiniMotion.Reduced -> home.motionReduced
                MiniMotion.Normal -> home.motionNormal
                MiniMotion.Expressive -> home.motionExpressive
            }
        },
        onSelect = onSelect
    )
}

@Composable
internal fun MiniStudioSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = LevyraText,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = title }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MiniShapePicker(
    title: String,
    selected: MiniArtworkShape,
    copy: MiniPlayerStudioCopy,
    onSelect: (MiniArtworkShape) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    MiniStudioCaption(title)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniArtworkShape.entries.forEach { shape ->
            val active = shape == selected
            val label = miniShapeLabel(shape, copy)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (active) scheme.secondaryContainer else scheme.surfaceContainerHighest)
                    .border(
                        width = if (active) 2.dp else 0.dp,
                        color = if (active) scheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .levyraPressable(onClick = { onSelect(shape) }, onClickLabel = label, role = Role.RadioButton)
                    .semantics {
                        this.selected = active
                        contentDescription = label
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(miniArtworkShape(shape))
                        .background(if (active) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.6f))
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MiniColorEditor(color: Int, copy: MiniPlayerStudioCopy, onColor: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var text by remember(color) { mutableStateOf(formatMiniHexColor(color)) }
    val parsed = parseMiniHexColor(text)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniCustomSwatches.forEach { swatch ->
                val active = swatch == color
                val hex = "#" + formatMiniHexColor(swatch)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .levyraPressable(onClick = { onColor(swatch) }, onClickLabel = hex, role = Role.RadioButton)
                        .semantics {
                            this.selected = active
                            contentDescription = hex
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (active) 40.dp else 34.dp)
                            .clip(CircleShape)
                            .background(Color(swatch))
                            .border(
                                width = if (active) 3.dp else 0.dp,
                                color = if (active) scheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(parsed ?: color))
            )
            OutlinedTextField(
                value = text,
                onValueChange = { raw ->
                    val cleaned = raw.removePrefix("#").filter { it in "0123456789abcdefABCDEF" }.take(6).uppercase()
                    text = cleaned
                    parseMiniHexColor(cleaned)?.let(onColor)
                },
                singleLine = true,
                isError = parsed == null,
                prefix = { Text("#") },
                label = { Text(copy[MiniStudioText.Hex]) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { parsed?.let(onColor) }),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
