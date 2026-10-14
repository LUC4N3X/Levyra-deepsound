@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.luc4n3x.levyra.ui.lyrics.studio

import com.luc4n3x.levyra.ui.components.LevyraBackButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.domain.DemoLyrics
import com.luc4n3x.levyra.domain.LevyraFontPreset
import com.luc4n3x.levyra.domain.LevyraLyricsStudioSettings
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricsActiveColorMode
import com.luc4n3x.levyra.domain.LyricsAlignment
import com.luc4n3x.levyra.domain.LyricsBackgroundMode
import com.luc4n3x.levyra.domain.LyricsFontWeightPreset
import com.luc4n3x.levyra.domain.LyricsGlowEffect
import com.luc4n3x.levyra.domain.LyricsLineSpacing
import com.luc4n3x.levyra.domain.LyricsStylePreset
import com.luc4n3x.levyra.domain.LyricsTextSize
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.KaraokeWordTimedText
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraThemeAccents
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.ui.theme.levyraFontFamilyFor
import kotlinx.coroutines.delay

@Composable
internal fun LyricsStudioOverlay(
    settings: LevyraLyricsStudioSettings,
    currentTrack: Track?,
    liveLyrics: List<LyricLine>,
    lyricsSynced: Boolean,
    positionMs: Long,
    isPlaying: Boolean,
    onUpdateSettings: (LevyraLyricsStudioSettings) -> Unit,
    onResetSettings: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    var showResetDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraInk)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = LevyraPlayerDesign.GutterCompact,
                end = LevyraPlayerDesign.GutterCompact,
                top = LevyraPlayerDesign.SpaceMd,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceLg)
        ) {
            // Header
            item(contentType = "studio-header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LevyraBackButton(onClick = onClose, contentDescription = strings.back)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = LevyraPlayerDesign.SpaceXs)
                    ) {
                        Text(
                            text = strings.lyricsStudio,
                            color = LevyraText,
                            fontSize = 26.sp,
                            lineHeight = LevyraTypeRhythm.lineHeight(26f),
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = strings.lyricsStudioSubtitle,
                            color = LevyraMuted,
                            fontSize = 13.sp,
                            lineHeight = LevyraTypeRhythm.lineHeight(13f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = strings.lyricsStudioReset,
                            tint = LevyraMuted
                        )
                    }
                }
            }

            // Live Preview Card
            item(contentType = "studio-preview") {
                LyricsStudioLivePreview(
                    settings = settings,
                    currentTrack = currentTrack,
                    liveLyrics = liveLyrics,
                    lyricsSynced = lyricsSynced,
                    playbackPositionMs = positionMs,
                    isPlaying = isPlaying
                )
            }

            // Presets Selector (5 Stili di Testi)
            item(contentType = "studio-presets") {
                StudioSectionHeader(
                    title = strings.lyricsStudioPresets,
                    icon = Icons.Rounded.AutoAwesome
                )
            }

            item(contentType = "studio-presets-row") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LyricsStylePreset.entries.forEach { preset ->
                        val (title, description) = when (preset) {
                            LyricsStylePreset.LEVYRA_ORIGINAL -> strings.lyricsStyleOriginal to strings.lyricsStyleOriginalDesc
                            LyricsStylePreset.EXPRESSIVE_KARAOKE -> strings.lyricsStyleExpressive to strings.lyricsStyleExpressiveDesc
                            LyricsStylePreset.MINIMAL -> strings.lyricsStyleMinimal to strings.lyricsStyleMinimalDesc
                            LyricsStylePreset.FOCUS -> strings.lyricsStyleFocus to strings.lyricsStyleFocusDesc
                            LyricsStylePreset.CINEMATIC -> strings.lyricsStyleCinematic to strings.lyricsStyleCinematicDesc
                        }
                        PresetCard(
                            preset = preset,
                            title = title,
                            description = description,
                            selected = settings.stylePreset == preset,
                            onClick = { onUpdateSettings(settings.applyPreset(preset)) }
                        )
                    }
                }
            }

            // Typography Section
            item(contentType = "studio-typography-header") {
                StudioSectionHeader(
                    title = strings.lyricsStudioTypography,
                    icon = Icons.Rounded.TextFields
                )
            }

            item(contentType = "studio-typography-font") {
                StudioCard {
                    Text(
                        text = strings.lyricsStudioFont,
                        color = LevyraText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(LevyraFontPreset.entries) { fontPreset ->
                            val selected = settings.fontPreset == fontPreset
                            StudioChoiceChip(
                                label = fontPreset.displayName,
                                selected = selected,
                                onClick = { onUpdateSettings(settings.copy(fontPreset = fontPreset)) }
                            )
                        }
                    }
                }
            }

            item(contentType = "studio-typography-options") {
                StudioCard {
                    // Text Size
                    Text(
                        text = strings.lyricsStudioTextSize,
                        color = LevyraText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LyricsTextSize.entries.forEach { size ->
                            val label = when (size) {
                                LyricsTextSize.COMPACT -> strings.lyricsStudioSizeCompact
                                LyricsTextSize.STANDARD -> strings.lyricsStudioSizeStandard
                                LyricsTextSize.LARGE -> strings.lyricsStudioSizeLarge
                                LyricsTextSize.EXTRA_LARGE -> strings.lyricsStudioSizeExtraLarge
                            }
                            StudioChoiceChip(
                                label = label,
                                selected = settings.textSize == size,
                                onClick = { onUpdateSettings(settings.copy(textSize = size)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Font Weight
                    Text(
                        text = strings.lyricsStudioFontWeight,
                        color = LevyraText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LyricsFontWeightPreset.entries.forEach { weight ->
                            val label = when (weight) {
                                LyricsFontWeightPreset.MEDIUM -> strings.lyricsStudioWeightMedium
                                LyricsFontWeightPreset.SEMI_BOLD -> strings.lyricsStudioWeightSemiBold
                                LyricsFontWeightPreset.BOLD -> strings.lyricsStudioWeightBold
                                LyricsFontWeightPreset.EXTRA_BOLD -> strings.lyricsStudioWeightExtraBold
                                LyricsFontWeightPreset.BLACK -> strings.lyricsStudioWeightBlack
                            }
                            StudioChoiceChip(
                                label = label,
                                selected = settings.fontWeight == weight,
                                onClick = { onUpdateSettings(settings.copy(fontWeight = weight)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Alignment
                    Text(
                        text = strings.lyricsStudioAlignment,
                        color = LevyraText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LyricsAlignment.entries.forEach { alignment ->
                            val (label, icon) = when (alignment) {
                                LyricsAlignment.START -> strings.lyricsStudioAlignStart to Icons.AutoMirrored.Rounded.FormatAlignLeft
                                LyricsAlignment.CENTER -> strings.lyricsStudioAlignCenter to Icons.Rounded.FormatAlignCenter
                                LyricsAlignment.END -> strings.lyricsStudioAlignEnd to Icons.AutoMirrored.Rounded.FormatAlignRight
                            }
                            StudioIconChoiceChip(
                                label = label,
                                icon = icon,
                                selected = settings.alignment == alignment,
                                onClick = { onUpdateSettings(settings.copy(alignment = alignment)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Line Spacing
                    Text(
                        text = strings.lyricsStudioLineSpacing,
                        color = LevyraText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LyricsLineSpacing.entries.forEach { spacing ->
                            val label = when (spacing) {
                                LyricsLineSpacing.COMPACT -> strings.lyricsStudioSpacingCompact
                                LyricsLineSpacing.STANDARD -> strings.lyricsStudioSpacingStandard
                                LyricsLineSpacing.RELAXED -> strings.lyricsStudioSpacingRelaxed
                                LyricsLineSpacing.SPACIOUS -> strings.lyricsStudioSpacingSpacious
                            }
                            StudioChoiceChip(
                                label = label,
                                selected = settings.lineSpacing == spacing,
                                onClick = { onUpdateSettings(settings.copy(lineSpacing = spacing)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Inactive Scale
                    Text(
                        text = strings.lyricsStudioInactiveScale,
                        color = LevyraText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.80f to "80%", 0.90f to "90%", 1.0f to "100%").forEach { (scale, label) ->
                            StudioChoiceChip(
                                label = label,
                                selected = kotlin.math.abs(settings.inactiveScale - scale) < 0.05f,
                                onClick = { onUpdateSettings(settings.copy(inactiveScale = scale)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Colors Section
            item(contentType = "studio-colors-header") {
                StudioSectionHeader(
                    title = strings.lyricsStudioColors,
                    icon = Icons.Rounded.Palette
                )
            }

            item(contentType = "studio-colors-options") {
                StudioCard {
                    Text(
                        text = strings.lyricsStudioActiveColor,
                        color = LevyraText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LyricsActiveColorMode.entries.forEach { mode ->
                            val label = when (mode) {
                                LyricsActiveColorMode.WHITE -> strings.lyricsStudioColorWhite
                                LyricsActiveColorMode.AUTOMATIC -> strings.lyricsStudioColorAuto
                                LyricsActiveColorMode.CUSTOM -> strings.lyricsStudioColorCustom
                            }
                            StudioChoiceChip(
                                label = label,
                                selected = settings.activeColorMode == mode,
                                onClick = { onUpdateSettings(settings.copy(activeColorMode = mode)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    AnimatedVisibility(visible = settings.activeColorMode == LyricsActiveColorMode.CUSTOM) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = strings.themeAccent,
                                color = LevyraMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val accentLabels = listOf(
                                    strings.themeAccentBlue, strings.themeAccentGreen,
                                    strings.themeAccentIndigo, strings.themeAccentOrange,
                                    strings.themeAccentPink, strings.themeAccentCyan,
                                    strings.themeAccentPurple, strings.themeAccentYellow
                                )
                                LevyraThemeAccents.forEachIndexed { index, accent ->
                                    val isSelected = settings.customAccentColor == accent
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(accent))
                                            .selectable(
                                                selected = isSelected,
                                                role = Role.RadioButton,
                                                onClick = { onUpdateSettings(settings.copy(customAccentColor = accent)) }
                                            )
                                            .semantics { contentDescription = accentLabels[index] },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Inactive Opacity
                    Text(
                        text = strings.lyricsStudioInactiveOpacity,
                        color = LevyraText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.25f to "25%", 0.40f to "40%", 0.55f to "55%", 0.70f to "70%").forEach { (opacity, label) ->
                            StudioChoiceChip(
                                label = label,
                                selected = kotlin.math.abs(settings.inactiveOpacity - opacity) < 0.08f,
                                onClick = { onUpdateSettings(settings.copy(inactiveOpacity = opacity)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Effects Section
            item(contentType = "studio-effects-header") {
                StudioSectionHeader(
                    title = strings.lyricsStudioEffects,
                    icon = Icons.Rounded.GraphicEq
                )
            }

            item(contentType = "studio-effects-options") {
                StudioCard {
                    // Glow
                    Text(
                        text = strings.lyricsStudioGlow,
                        color = LevyraText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LyricsGlowEffect.entries.forEach { glow ->
                            val label = when (glow) {
                                LyricsGlowEffect.OFF -> strings.lyricsStudioGlowOff
                                LyricsGlowEffect.SUBTLE -> strings.lyricsStudioGlowSubtle
                                LyricsGlowEffect.VIBRANT -> strings.lyricsStudioGlowVibrant
                            }
                            StudioChoiceChip(
                                label = label,
                                selected = settings.glowEffect == glow,
                                onClick = { onUpdateSettings(settings.copy(glowEffect = glow)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progressive Karaoke Switch
                    StudioToggleRow(
                        title = strings.lyricsStudioWordKaraoke,
                        subtitle = strings.lyricsStudioWordKaraokeSubtitle,
                        checked = settings.progressiveKaraokeEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(progressiveKaraokeEnabled = it)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Depth Blur Switch
                    StudioToggleRow(
                        title = strings.lyricsStudioDepthBlur,
                        subtitle = strings.lyricsStudioDepthBlurSubtitle,
                        checked = settings.blurEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(blurEnabled = it)) }
                    )
                }
            }

            // Background Section
            item(contentType = "studio-bg-header") {
                StudioSectionHeader(
                    title = strings.lyricsStudioBackground,
                    icon = Icons.Rounded.Wallpaper
                )
            }

            item(contentType = "studio-bg-options") {
                StudioCard {
                    val bgRows = listOf(
                        listOf(LyricsBackgroundMode.ORIGINAL, LyricsBackgroundMode.SOLID),
                        listOf(LyricsBackgroundMode.AMOLED, LyricsBackgroundMode.GRADIENT),
                        listOf(LyricsBackgroundMode.BLURRED_ARTWORK, LyricsBackgroundMode.CINEMATIC)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bgRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { mode ->
                                    val label = when (mode) {
                                        LyricsBackgroundMode.ORIGINAL -> strings.lyricsStudioBgOriginal
                                        LyricsBackgroundMode.SOLID -> strings.lyricsStudioBgSolid
                                        LyricsBackgroundMode.AMOLED -> strings.lyricsStudioBgAmoled
                                        LyricsBackgroundMode.GRADIENT -> strings.lyricsStudioBgGradient
                                        LyricsBackgroundMode.BLURRED_ARTWORK -> strings.lyricsStudioBgBlurredArtwork
                                        LyricsBackgroundMode.CINEMATIC -> strings.lyricsStudioBgCinematic
                                    }
                                    StudioChoiceChip(
                                        label = label,
                                        selected = settings.backgroundMode == mode,
                                        onClick = { onUpdateSettings(settings.copy(backgroundMode = mode)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reset Button
            item(contentType = "studio-reset-action") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = LevyraCyan
                        ),
                        border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 6.dp)
                        )
                        Text(
                            text = strings.lyricsStudioReset,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = strings.lyricsStudioReset,
                    fontWeight = FontWeight.Black,
                    color = LevyraText
                )
            },
            text = {
                Text(
                    text = strings.lyricsStudioResetConfirm,
                    color = LevyraMuted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetSettings()
                        showResetDialog = false
                    }
                ) {
                    Text(
                        text = strings.lyricsStudioReset,
                        color = LevyraCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(
                        text = strings.cancel,
                        color = LevyraMuted
                    )
                }
            },
            containerColor = Color(0xFF1E1E24),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun StudioSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = LevyraPlayerDesign.SpaceXs, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LevyraCyan,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            color = LevyraText,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
private fun PresetCard(
    preset: LyricsStylePreset,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val borderColor by animateColorAsState(
        targetValue = if (selected) LevyraCyan else Color.White.copy(alpha = 0.08f),
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(200)),
        label = "preset-border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) LevyraCyan.copy(alpha = 0.12f) else Color(0xFF16161A),
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(200)),
        label = "preset-bg"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .semantics {
                this.role = Role.RadioButton
                this.selected = selected
            },
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (selected) LevyraCyan else LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(LevyraCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = LevyraInk,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF16161A),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun StudioChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val bgColor by animateColorAsState(
        targetValue = if (selected) LevyraCyan else Color.White.copy(alpha = 0.06f),
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(180)),
        label = "chip-bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) LevyraInk else LevyraText,
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(180)),
        label = "chip-text"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StudioIconChoiceChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val bgColor by animateColorAsState(
        targetValue = if (selected) LevyraCyan else Color.White.copy(alpha = 0.06f),
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(180)),
        label = "chip-icon-bg"
    )
    val tintColor by animateColorAsState(
        targetValue = if (selected) LevyraInk else LevyraText,
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(180)),
        label = "chip-icon-tint"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = tintColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun StudioToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = LevyraText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = LevyraMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LevyraInk,
                checkedTrackColor = LevyraCyan,
                uncheckedThumbColor = LevyraMuted,
                uncheckedTrackColor = Color.White.copy(alpha = 0.10f)
            )
        )
    }
}

@Composable
private fun LyricsStudioLivePreview(
    settings: LevyraLyricsStudioSettings,
    currentTrack: Track?,
    liveLyrics: List<LyricLine>,
    lyricsSynced: Boolean,
    playbackPositionMs: Long,
    isPlaying: Boolean
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val useLive = currentTrack != null && liveLyrics.isNotEmpty() && lyricsSynced
    val previewLines = if (useLive) liveLyrics else DemoLyrics

    val simPositionMs = rememberSimulatedPlaybackPosition(useLive = useLive, isPlaying = isPlaying)
    val activePos = if (useLive) playbackPositionMs else simPositionMs

    val activeIndex = remember(previewLines, activePos) {
        val index = previewLines.indexOfLast { it.startMs <= activePos }
        if (index >= 0) index else 0
    }

    val accentColor = resolveStudioAccentColor(settings, currentTrack)
    val fontFamily = remember(settings.fontPreset) {
        levyraFontFamilyFor(settings.fontPreset)
    }
    val baseFontSize = 21f * settings.textSize.scale
    val lineHeight = LevyraTypeRhythm.lineHeight(baseFontSize)
    val glowShadow = resolveStudioGlowShadow(settings, accentColor)

    Column(modifier = Modifier.fillMaxWidth()) {
        StudioPreviewHeader(useLive = useLive, currentTrack = currentTrack)

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
        ) {
            StudioPreviewBackground(
                mode = settings.backgroundMode,
                track = currentTrack,
                accent = accentColor
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = when (settings.alignment) {
                    LyricsAlignment.START -> Alignment.Start
                    LyricsAlignment.CENTER -> Alignment.CenterHorizontally
                    LyricsAlignment.END -> Alignment.End
                }
            ) {
                val displayIndices = (activeIndex - 1..activeIndex + 1)
                    .map { it.coerceIn(0, previewLines.lastIndex) }
                    .distinct()

                displayIndices.forEach { idx ->
                    StudioPreviewLyricsLine(
                        line = previewLines[idx],
                        playbackPositionMs = activePos,
                        isLineActive = idx == activeIndex,
                        distance = kotlin.math.abs(idx - activeIndex),
                        settings = settings,
                        accentColor = accentColor,
                        fontFamily = fontFamily,
                        baseFontSize = baseFontSize,
                        lineHeight = lineHeight,
                        glowShadow = glowShadow,
                        animationsEnabled = animationsEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberSimulatedPlaybackPosition(useLive: Boolean, isPlaying: Boolean): Long {
    var simPositionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying, useLive) {
        if (!useLive) {
            val totalDuration = 14000L
            while (true) {
                delay(60L)
                simPositionMs = (simPositionMs + 60L) % totalDuration
            }
        }
    }
    return simPositionMs
}

private fun resolveStudioAccentColor(settings: LevyraLyricsStudioSettings, currentTrack: Track?): Color =
    when (settings.activeColorMode) {
        LyricsActiveColorMode.WHITE -> Color.White
        LyricsActiveColorMode.AUTOMATIC -> if (currentTrack != null) Color(currentTrack.accentEnd) else LevyraCyan
        LyricsActiveColorMode.CUSTOM -> Color(settings.customAccentColor)
    }

private fun resolveStudioGlowShadow(settings: LevyraLyricsStudioSettings, accentColor: Color): Shadow? =
    when (settings.glowEffect) {
        LyricsGlowEffect.OFF -> null
        LyricsGlowEffect.SUBTLE -> Shadow(
            color = accentColor.copy(alpha = 0.55f),
            blurRadius = 14f,
            offset = Offset.Zero
        )
        LyricsGlowEffect.VIBRANT -> Shadow(
            color = accentColor.copy(alpha = 0.85f),
            blurRadius = 26f,
            offset = Offset.Zero
        )
    }

@Composable
private fun StudioPreviewHeader(
    useLive: Boolean,
    currentTrack: Track?
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = strings.lyricsStudioPreview,
            color = LevyraMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (useLive) LevyraCyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (useLive && currentTrack != null) currentTrack.title.ifBlank { strings.nowPlaying } else "Demo Timeline",
                color = if (useLive) LevyraCyan else LevyraMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudioPreviewLyricsLine(
    line: LyricLine,
    playbackPositionMs: Long,
    isLineActive: Boolean,
    distance: Int,
    settings: LevyraLyricsStudioSettings,
    accentColor: Color,
    fontFamily: FontFamily,
    baseFontSize: Float,
    lineHeight: TextUnit,
    glowShadow: Shadow?,
    animationsEnabled: Boolean
) {
    val lineAlpha by animateFloatAsState(
        targetValue = if (isLineActive) 1f else settings.inactiveOpacity,
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(180)),
        label = "preview-alpha"
    )
    val lineScale by animateFloatAsState(
        targetValue = if (isLineActive) 1.03f else settings.inactiveScale,
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(200)),
        label = "preview-scale"
    )
    val lineBlur = previewLineBlur(isLineActive, settings.blurEnabled, animationsEnabled, distance)
    val textColor = if (isLineActive) accentColor else Color.White
    val resolvedWeight = if (isLineActive) settings.fontWeight.weight else FontWeight.Medium
    val activeShadow = if (isLineActive) glowShadow else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = settings.lineSpacing.spacingDp / 2)
            .graphicsLayer {
                scaleX = lineScale
                scaleY = lineScale
                alpha = lineAlpha
            }
            .then(if (lineBlur > 0.dp) Modifier.blur(lineBlur) else Modifier),
        contentAlignment = previewAlignment(settings.alignment)
    ) {
        StudioPreviewLineText(
            line = line,
            playbackPositionMs = playbackPositionMs,
            progressiveKaraokeEnabled = settings.progressiveKaraokeEnabled,
            isLineActive = isLineActive,
            textColor = textColor,
            fontFamily = fontFamily,
            baseFontSize = baseFontSize,
            lineHeight = lineHeight,
            fontWeight = resolvedWeight,
            textAlign = settings.alignment.textAlign,
            glowShadow = activeShadow
        )
    }
}

@Composable
private fun StudioPreviewLineText(
    line: LyricLine,
    playbackPositionMs: Long,
    progressiveKaraokeEnabled: Boolean,
    isLineActive: Boolean,
    textColor: Color,
    fontFamily: FontFamily,
    baseFontSize: Float,
    lineHeight: TextUnit,
    fontWeight: FontWeight,
    textAlign: TextAlign,
    glowShadow: Shadow?
) {
    if (progressiveKaraokeEnabled && line.words.isNotEmpty()) {
        KaraokeWordTimedText(
            lineText = line.text,
            words = line.words,
            positionProvider = { playbackPositionMs },
            isActive = isLineActive,
            fontSize = baseFontSize.sp,
            lineHeight = lineHeight,
            textAlign = textAlign,
            inactiveColor = textColor,
            completedColor = textColor,
            activeColor = textColor,
            pendingColor = textColor.copy(alpha = 0.35f),
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            shadow = glowShadow
        )
        return
    }
    Text(
        text = line.text,
        color = textColor,
        fontFamily = fontFamily,
        fontSize = baseFontSize.sp,
        lineHeight = lineHeight,
        fontWeight = fontWeight,
        textAlign = textAlign,
        style = TextStyle(shadow = glowShadow)
    )
}

private fun previewAlignment(alignment: LyricsAlignment): Alignment = when (alignment) {
    LyricsAlignment.START -> Alignment.CenterStart
    LyricsAlignment.CENTER -> Alignment.Center
    LyricsAlignment.END -> Alignment.CenterEnd
}

private fun previewLineBlur(
    isLineActive: Boolean,
    blurEnabled: Boolean,
    animationsEnabled: Boolean,
    distance: Int
): Dp {
    if (isLineActive || !blurEnabled || !animationsEnabled) return 0.dp
    return (distance * 1.5f).dp
}

@Composable
private fun StudioPreviewBackground(
    mode: LyricsBackgroundMode,
    track: Track?,
    accent: Color
) {
    val context = LocalContext.current
    when (mode) {
        LyricsBackgroundMode.ORIGINAL -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF141418))
            )
        }
        LyricsBackgroundMode.SOLID -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16161A))
            )
        }
        LyricsBackgroundMode.AMOLED -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
        LyricsBackgroundMode.GRADIENT -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = 0.28f),
                                Color(0xFF0F0F12),
                                Color(0xFF0A0A0C)
                            )
                        )
                    )
            )
        }
        LyricsBackgroundMode.BLURRED_ARTWORK -> {
            val artworkUrl = track?.largeThumbnailUrl?.ifBlank { track.thumbnailUrl }.orEmpty()
            if (artworkUrl.isNotBlank()) {
                val request = remember(artworkUrl) {
                    ImageRequest.Builder(context)
                        .data(artworkUrl)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(LevyraCyan.copy(alpha = 0.22f), LevyraViolet.copy(alpha = 0.22f))
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
            )
        }
        LyricsBackgroundMode.CINEMATIC -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.24f), Color(0xFF08080A)),
                            radius = 600f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.70f))
                        )
                    )
            )
        }
    }
}
