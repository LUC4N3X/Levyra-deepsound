package com.luc4n3x.levyra.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings

internal val LevyraThemeAccents: List<Int> = listOf(
    0xFF0A84FF.toInt(),
    0xFF30D158.toInt(),
    0xFF5E5CE6.toInt(),
    0xFFFF9F0A.toInt(),
    0xFFFF375F.toInt(),
    0xFF64D2FF.toInt(),
    0xFFBF5AF2.toInt(),
    0xFFFFD60A.toInt()
)

@Composable
internal fun LevyraThemeStudioOverlay(
    selectedPresetId: String,
    accent: Int,
    pureBlack: Boolean,
    previewTitle: String,
    previewArtist: String,
    onSelectPreset: (String) -> Unit,
    onSelectAccent: (Int) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val preview = rememberThemeStudioPreview(selectedPresetId, accent, pureBlack)
    val presetRows = remember { themeStudioRows() }

    Box(modifier = Modifier.fillMaxSize().background(LevyraInk)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                start = LevyraPlayerDesign.GutterCompact,
                end = LevyraPlayerDesign.GutterCompact,
                top = LevyraPlayerDesign.SpaceMd,
                bottom = 140.dp
            ),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceLg)
        ) {
            item(contentType = "studio-header") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.back,
                            tint = LevyraText
                        )
                    }
                    Column(modifier = Modifier.padding(start = LevyraPlayerDesign.SpaceXs)) {
                        Text(
                            strings.themeStudio,
                            color = LevyraText,
                            fontSize = 26.sp,
                            lineHeight = LevyraTypeRhythm.lineHeight(26f),
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            strings.themeStudioSubtitle,
                            color = LevyraMuted,
                            fontSize = 13.sp,
                            lineHeight = LevyraTypeRhythm.lineHeight(13f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item(contentType = "studio-preview") {
                ThemeStudioPreview(
                    palette = preview,
                    title = previewTitle.ifBlank { strings.nowPlaying },
                    artist = previewArtist.ifBlank { strings.artistLabel },
                    label = strings.themeStudioPreview
                )
            }

            item(contentType = "studio-presets") {
                Text(
                    strings.themes,
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    letterSpacing = 0.4.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = LevyraPlayerDesign.SpaceXs)
                )
            }

            items(
                count = presetRows.size,
                key = { index -> "studio-row-$index" },
                contentType = { "studio-row" }
            ) { index ->
                val row = presetRows[index]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
                ) {
                    row.forEach { palette ->
                        ThemePresetTile(
                            palette = palette,
                            selected = palette.id == selectedPresetId,
                            onClick = { onSelectPreset(palette.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            item(contentType = "studio-accent") {
                Column(verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)) {
                    Text(
                        strings.themeAccent,
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        letterSpacing = 0.4.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = LevyraPlayerDesign.SpaceXs)
                    )
                    ThemeAccentPicker(
                        selected = accent,
                        fromPresetLabel = strings.themeAccentFromPreset,
                        presetAccent = preview.accent,
                        onSelect = onSelectAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeStudioPreview(
    palette: ThemeStudioPalette,
    title: String,
    artist: String,
    label: String
) {
    val background by animateColorAsState(
        palette.background,
        LevyraPlayerDesign.emphasizedTween(420),
        label = "studio-bg"
    )
    val surface by animateColorAsState(
        palette.surface,
        LevyraPlayerDesign.emphasizedTween(420),
        label = "studio-surface"
    )
    val accent by animateColorAsState(
        palette.accent,
        LevyraPlayerDesign.emphasizedTween(420),
        label = "studio-accent"
    )
    val onSurface by animateColorAsState(
        palette.text,
        LevyraPlayerDesign.emphasizedTween(420),
        label = "studio-text"
    )
    val muted by animateColorAsState(
        palette.muted,
        LevyraPlayerDesign.emphasizedTween(420),
        label = "studio-muted"
    )

    Column(verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)) {
        Text(
            label,
            color = LevyraMuted,
            fontSize = 12.sp,
            letterSpacing = 0.4.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = LevyraPlayerDesign.SpaceXs)
        )
        Surface(
            color = background,
            border = BorderStroke(LevyraPlayerDesign.Hairline, muted.copy(alpha = 0.22f)),
            shape = LevyraPlayerDesign.ShapeLg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(LevyraPlayerDesign.SpaceLg),
                verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .aspectRatio(1f)
                            .clip(LevyraPlayerDesign.ShapeSm)
                            .background(
                                Brush.linearGradient(
                                    listOf(accent, palette.secondary)
                                )
                            )
                            .clearAndSetSemantics { }
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)
                    ) {
                        Text(
                            title,
                            color = onSurface,
                            fontSize = 17.sp,
                            lineHeight = LevyraTypeRhythm.lineHeight(17f),
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            artist,
                            color = muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(muted.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.42f)
                                    .height(3.dp)
                                    .background(accent)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceLg, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = null,
                        tint = muted,
                        modifier = Modifier.size(22.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = palette.onAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = null,
                        tint = muted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                repeat(2) { index ->
                    Surface(
                        color = surface,
                        shape = LevyraPlayerDesign.ShapeXs,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(LevyraPlayerDesign.SpaceMd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(LevyraPlayerDesign.ShapeXxs)
                                    .background(if (index == 0) accent.copy(alpha = 0.7f) else muted.copy(alpha = 0.35f))
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (index == 0) 0.6f else 0.45f)
                                        .height(7.dp)
                                        .clip(CircleShape)
                                        .background(onSurface.copy(alpha = 0.8f))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (index == 0) 0.35f else 0.28f)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(muted.copy(alpha = 0.55f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePresetTile(
    palette: LevyraPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border by animateColorAsState(
        if (selected) LevyraCyan else LevyraMuted.copy(alpha = 0.2f),
        LevyraPlayerDesign.standardTween(),
        label = "studio-tile-border"
    )
    Surface(
        color = palette.black,
        border = BorderStroke(if (selected) 2.dp else LevyraPlayerDesign.Hairline, border),
        shape = LevyraPlayerDesign.ShapeSm,
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 96.dp)
            .semantics {
                this.role = Role.RadioButton
                this.selected = selected
                this.contentDescription = palette.label
            }
    ) {
        Column(
            modifier = Modifier.padding(LevyraPlayerDesign.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(palette.cyan))
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(palette.violet))
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(palette.pink))
                Spacer(modifier = Modifier.weight(1f))
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = LevyraCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(palette.panel)
            )
            Text(
                palette.label,
                color = palette.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeAccentPicker(
    selected: Int,
    fromPresetLabel: String,
    presetAccent: Color,
    onSelect: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)
    ) {
        ThemeAccentDot(
            color = presetAccent,
            selected = selected == 0,
            label = fromPresetLabel,
            outlined = true,
            onClick = { onSelect(0) }
        )
        LevyraThemeAccents.forEach { value ->
            ThemeAccentDot(
                color = Color(value),
                selected = selected == value,
                label = fromPresetLabel,
                outlined = false,
                onClick = { onSelect(value) }
            )
        }
    }
}

@Composable
private fun ThemeAccentDot(
    color: Color,
    selected: Boolean,
    label: String,
    outlined: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(LevyraPlayerDesign.MinimumTouchTarget)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                this.role = Role.RadioButton
                this.selected = selected
                if (outlined) this.contentDescription = label
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = color,
            shape = CircleShape,
            border = if (outlined || selected) {
                BorderStroke(2.dp, if (selected) LevyraText else LevyraMuted.copy(alpha = 0.4f))
            } else {
                null
            },
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = LevyraOnAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

internal data class ThemeStudioPalette(
    val background: Color,
    val surface: Color,
    val accent: Color,
    val secondary: Color,
    val text: Color,
    val muted: Color,
    val onAccent: Color
)

@Composable
private fun rememberThemeStudioPreview(
    presetId: String,
    accent: Int,
    pureBlack: Boolean
): ThemeStudioPalette = remember(presetId, accent, pureBlack) {
    themeStudioPaletteFor(presetId, accent, pureBlack)
}

internal fun themeStudioPaletteFor(
    presetId: String,
    accent: Int,
    pureBlack: Boolean
): ThemeStudioPalette {
    val base = LevyraThemes.byId(presetId)
    val accented = if (accent != 0) LevyraThemeController.withAccent(base, Color(accent)) else base
    val palette = if (pureBlack && !accented.isLight) {
        LevyraThemeController.asPureBlack(accented)
    } else {
        accented
    }
    return ThemeStudioPalette(
        background = palette.black,
        surface = palette.panel,
        accent = palette.cyan,
        secondary = palette.violet,
        text = palette.text,
        muted = palette.muted,
        onAccent = if (palette.isLight) Color.White else palette.black
    )
}

private fun themeStudioRows(): List<List<LevyraPalette>> = LevyraThemes.presets.chunked(2)
