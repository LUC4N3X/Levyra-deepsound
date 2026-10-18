package com.luc4n3x.levyra.ui.theme

import com.luc4n3x.levyra.ui.components.LevyraBackButton
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
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
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.domain.LevyraCustomTheme
import com.luc4n3x.levyra.domain.LevyraCustomThemes
import com.luc4n3x.levyra.ui.LevyraGlassStyle
import com.luc4n3x.levyra.ui.i18n.ColorPickerText
import com.luc4n3x.levyra.ui.i18n.ThemeLibraryCopy
import com.luc4n3x.levyra.ui.i18n.ThemeLibraryText
import com.luc4n3x.levyra.ui.i18n.colorPickerCopy
import com.luc4n3x.levyra.ui.i18n.glassStudioCopy
import com.luc4n3x.levyra.ui.i18n.themeLibraryCopy
import kotlin.math.roundToInt

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
    onClose: () -> Unit,
    customThemes: List<LevyraCustomTheme> = emptyList(),
    onSaveCustomTheme: (LevyraCustomTheme, Boolean) -> Unit = { _, _ -> },
    onDuplicateCustomTheme: (String, String) -> Unit = { _, _ -> },
    onDeleteCustomTheme: (String) -> Unit = {},
    onRestoreDefault: () -> Unit = {},
    glassEnabled: Boolean = false,
    glassStyle: LevyraGlassStyle = LevyraGlassStyle(),
    onGlassStyleChange: (LevyraGlassStyle) -> Unit = {}
) {
    val strings = LocalLevyraStrings.current
    val library = strings.themeLibraryCopy
    var previewAccent by remember(selectedPresetId, accent) { mutableStateOf<Int?>(null) }
    val basePreview = rememberThemeStudioPreview(selectedPresetId, accent, pureBlack, customThemes)
    val preview = rememberThemeStudioPreview(selectedPresetId, previewAccent ?: accent, pureBlack, customThemes)
    val presetRows = remember { themeStudioRows() }
    val defaultPresetId = LevyraThemes.defaultPresetId
    val dialogs = remember { ThemeStudioDialogState() }

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
                    LevyraBackButton(onClick = onClose, contentDescription = strings.back)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        strings.themes,
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        letterSpacing = 0.4.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = LevyraPlayerDesign.SpaceXs)
                            .semantics { heading() }
                    )
                    TextButton(
                        onClick = { dialogs.confirmRestore = true },
                        enabled = selectedPresetId != defaultPresetId || accent != 0
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(library[ThemeLibraryText.RestoreDefault], maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            items(
                count = presetRows.size,
                key = { index -> "studio-row-$index" },
                contentType = { "studio-row" }
            ) { index ->
                val row = presetRows[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
                ) {
                    row.forEach { palette ->
                        ThemePresetTile(
                            palette = palette,
                            selected = palette.id == selectedPresetId,
                            onClick = { onSelectPreset(palette.id) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            defaultLabel = library[ThemeLibraryText.DefaultBadge].takeIf { palette.id == defaultPresetId },
                            selectedLabel = library[ThemeLibraryText.Selected]
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            item(contentType = "studio-library-header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        library[ThemeLibraryText.MyThemes],
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        letterSpacing = 0.4.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = LevyraPlayerDesign.SpaceXs)
                            .semantics { heading() }
                    )
                    FilledTonalButton(
                        onClick = { dialogs.editing = newCustomThemeFrom(selectedPresetId, accent, customThemes, library[ThemeLibraryText.NewThemeName]) },
                        enabled = customThemes.size < LevyraCustomThemes.MAX_THEMES
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(library[ThemeLibraryText.CreateTheme])
                    }
                }
            }

            if (customThemes.isEmpty()) {
                item(contentType = "studio-library-empty") {
                    Text(
                        library[ThemeLibraryText.MyThemesEmpty],
                        color = LevyraMuted,
                        fontSize = 13.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(13f),
                        modifier = Modifier.padding(start = LevyraPlayerDesign.SpaceXs)
                    )
                }
            } else {
                val customRows = customThemes.chunked(2)
                items(
                    count = customRows.size,
                    key = { index -> "studio-custom-" + customRows[index].joinToString { it.id } },
                    contentType = { "studio-custom-row" }
                ) { index ->
                    val row = customRows[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
                    ) {
                        row.forEach { theme ->
                            val palette = remember(theme) { theme.toPalette() }
                            Box(modifier = Modifier.weight(1f)) {
                                ThemePresetTile(
                                    palette = palette,
                                    selected = theme.id == selectedPresetId,
                                    onClick = { onSelectPreset(theme.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    selectedLabel = library[ThemeLibraryText.Selected]
                                )
                                CustomThemeMenu(
                                    copy = library,
                                    tint = palette.text,
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                    onEdit = { dialogs.editing = theme },
                                    onDuplicate = { onDuplicateCustomTheme(theme.id, library[ThemeLibraryText.CopySuffix]) },
                                    onRename = { dialogs.renaming = theme },
                                    onDelete = { dialogs.deleting = theme },
                                    canDuplicate = customThemes.size < LevyraCustomThemes.MAX_THEMES
                                )
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
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
                        accentLabels = listOf(
                            strings.themeAccentBlue,
                            strings.themeAccentGreen,
                            strings.themeAccentIndigo,
                            strings.themeAccentOrange,
                            strings.themeAccentPink,
                            strings.themeAccentCyan,
                            strings.themeAccentPurple,
                            strings.themeAccentYellow
                        ),
                        presetAccent = basePreview.accent,
                        onPreviewAccent = { previewAccent = it },
                        onSelect = {
                            previewAccent = null
                            onSelectAccent(it)
                        }
                    )
                }
            }

            item(contentType = "studio-glass") {
                ThemeStudioGlassSection(
                    copy = strings.glassStudioCopy(),
                    enabled = glassEnabled,
                    style = glassStyle,
                    onStyleChange = onGlassStyleChange
                )
            }
        }
    }

    ThemeStudioDialogs(
        dialogs = dialogs,
        previewTitle = previewTitle,
        previewArtist = previewArtist,
        onSaveCustomTheme = onSaveCustomTheme,
        onDeleteCustomTheme = onDeleteCustomTheme,
        onRestoreDefault = onRestoreDefault
    )
}

private class ThemeStudioDialogState {
    var confirmRestore by mutableStateOf(false)
    var editing by mutableStateOf<LevyraCustomTheme?>(null)
    var renaming by mutableStateOf<LevyraCustomTheme?>(null)
    var deleting by mutableStateOf<LevyraCustomTheme?>(null)
}

@Composable
private fun ThemeStudioDialogs(
    dialogs: ThemeStudioDialogState,
    previewTitle: String,
    previewArtist: String,
    onSaveCustomTheme: (LevyraCustomTheme, Boolean) -> Unit,
    onDeleteCustomTheme: (String) -> Unit,
    onRestoreDefault: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val library = strings.themeLibraryCopy
    if (dialogs.confirmRestore) {
        AlertDialog(
            onDismissRequest = { dialogs.confirmRestore = false },
            title = { Text(library[ThemeLibraryText.RestoreDefault]) },
            text = { Text(library[ThemeLibraryText.RestoreConfirm]) },
            confirmButton = {
                TextButton(onClick = {
                    onRestoreDefault()
                    dialogs.confirmRestore = false
                }) { Text(library[ThemeLibraryText.Reset]) }
            },
            dismissButton = {
                TextButton(onClick = { dialogs.confirmRestore = false }) { Text(strings.cancel) }
            }
        )
    }

    dialogs.editing?.let { draft ->
        CustomThemeEditorDialog(
            initial = draft,
            copy = library,
            previewTitle = previewTitle.ifBlank { strings.nowPlaying },
            previewArtist = previewArtist.ifBlank { strings.artistLabel },
            previewLabel = strings.themeStudioPreview,
            cancelLabel = strings.cancel,
            onSave = { theme ->
                onSaveCustomTheme(theme, true)
                dialogs.editing = null
            },
            onDismiss = { dialogs.editing = null }
        )
    }

    dialogs.renaming?.let { theme ->
        var name by remember(theme.id) { mutableStateOf(theme.name) }
        AlertDialog(
            onDismissRequest = { dialogs.renaming = null },
            title = { Text(library[ThemeLibraryText.Rename]) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(LevyraCustomTheme.MAX_NAME_LENGTH) },
                    singleLine = true,
                    label = { Text(library[ThemeLibraryText.Name]) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveCustomTheme(theme.copy(name = name), false)
                        dialogs.renaming = null
                    },
                    enabled = name.isNotBlank()
                ) { Text(library[ThemeLibraryText.Save]) }
            },
            dismissButton = {
                TextButton(onClick = { dialogs.renaming = null }) { Text(strings.cancel) }
            }
        )
    }

    dialogs.deleting?.let { theme ->
        AlertDialog(
            onDismissRequest = { dialogs.deleting = null },
            title = { Text("${library[ThemeLibraryText.Delete]} · ${theme.name}") },
            text = { Text(library[ThemeLibraryText.DeleteConfirm]) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCustomTheme(theme.id)
                    dialogs.deleting = null
                }) { Text(library[ThemeLibraryText.Delete]) }
            },
            dismissButton = {
                TextButton(onClick = { dialogs.deleting = null }) { Text(strings.cancel) }
            }
        )
    }
}

internal fun newCustomThemeFrom(
    presetId: String,
    accent: Int,
    existing: List<LevyraCustomTheme>,
    baseName: String
): LevyraCustomTheme {
    val base = LevyraThemes.byId(presetId)
    val palette = if (accent != 0) LevyraThemeController.withAccent(base, Color(accent)) else base
    val names = existing.map { it.name }.toSet()
    val name = generateSequence(1) { it + 1 }
        .map { index -> if (index == 1) baseName else "$baseName $index" }
        .first { it !in names }
    return LevyraCustomTheme(
        id = LevyraCustomTheme.newId(),
        name = name,
        isLight = palette.isLight,
        background = palette.black.toArgb(),
        surface = palette.panel.toArgb(),
        primary = palette.blue.toArgb(),
        secondary = palette.pink.toArgb(),
        accent = palette.orange.toArgb()
    ).normalized()
}

@Composable
private fun CustomThemeMenu(
    copy: ThemeLibraryCopy,
    tint: Color,
    modifier: Modifier,
    canDuplicate: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = copy[ThemeLibraryText.MoreActions], tint = tint)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(copy[ThemeLibraryText.Edit]) },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = { open = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text(copy[ThemeLibraryText.Duplicate]) },
                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                enabled = canDuplicate,
                onClick = { open = false; onDuplicate() }
            )
            DropdownMenuItem(
                text = { Text(copy[ThemeLibraryText.Rename]) },
                leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
                onClick = { open = false; onRename() }
            )
            DropdownMenuItem(
                text = { Text(copy[ThemeLibraryText.Delete]) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                onClick = { open = false; onDelete() }
            )
        }
    }
}

private enum class CustomThemeRole { Background, Surface, Primary, Secondary, Accent }

private fun LevyraCustomTheme.colorOf(role: CustomThemeRole): Int = when (role) {
    CustomThemeRole.Background -> background
    CustomThemeRole.Surface -> surface
    CustomThemeRole.Primary -> primary
    CustomThemeRole.Secondary -> secondary
    CustomThemeRole.Accent -> accent
}

private fun LevyraCustomTheme.withColor(role: CustomThemeRole, color: Int): LevyraCustomTheme = when (role) {
    CustomThemeRole.Background -> copy(background = color)
    CustomThemeRole.Surface -> copy(surface = color)
    CustomThemeRole.Primary -> copy(primary = color)
    CustomThemeRole.Secondary -> copy(secondary = color)
    CustomThemeRole.Accent -> copy(accent = color)
}

private fun CustomThemeRole.label(copy: ThemeLibraryCopy): String = when (this) {
    CustomThemeRole.Background -> copy[ThemeLibraryText.Background]
    CustomThemeRole.Surface -> copy[ThemeLibraryText.Surface]
    CustomThemeRole.Primary -> copy[ThemeLibraryText.Primary]
    CustomThemeRole.Secondary -> copy[ThemeLibraryText.Secondary]
    CustomThemeRole.Accent -> copy[ThemeLibraryText.Accent]
}

internal fun parseThemeHex(raw: String): Int? =
    com.luc4n3x.levyra.ui.components.parseLevyraHexColor(raw)

internal fun formatThemeHex(color: Int): String =
    com.luc4n3x.levyra.ui.components.formatLevyraHexColor(color)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CustomThemeEditorDialog(
    initial: LevyraCustomTheme,
    copy: ThemeLibraryCopy,
    previewTitle: String,
    previewArtist: String,
    previewLabel: String,
    cancelLabel: String,
    onSave: (LevyraCustomTheme) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(initial.id) { mutableStateOf(initial) }
    var role by remember { mutableStateOf(CustomThemeRole.Primary) }
    val palette = remember(draft) { themeStudioPaletteOf(draft.normalized().toPalette()) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LevyraInk)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LevyraPlayerDesign.SpaceSm, vertical = LevyraPlayerDesign.SpaceSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = cancelLabel, tint = LevyraText)
                }
                Text(
                    copy[ThemeLibraryText.EditorTitle],
                    color = LevyraText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                )
                Button(onClick = { onSave(draft.normalized()) }, enabled = draft.name.isNotBlank()) {
                    Text(copy[ThemeLibraryText.Save])
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = LevyraPlayerDesign.GutterCompact,
                    end = LevyraPlayerDesign.GutterCompact,
                    bottom = 48.dp
                ),
                verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceLg)
            ) {
                item(contentType = "editor-preview") {
                    ThemeStudioPreview(
                        palette = palette,
                        title = previewTitle,
                        artist = previewArtist,
                        label = previewLabel,
                        cornerScale = draft.cornerScale / 100f
                    )
                }
                item(contentType = "editor-name") {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it.take(LevyraCustomTheme.MAX_NAME_LENGTH)) },
                        singleLine = true,
                        label = { Text(copy[ThemeLibraryText.Name]) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item(contentType = "editor-mode") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf(false, true).forEachIndexed { index, light ->
                            SegmentedButton(
                                selected = draft.isLight == light,
                                onClick = { draft = themeWithMode(draft, light) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                            ) {
                                Text(if (light) copy[ThemeLibraryText.Light] else copy[ThemeLibraryText.Dark])
                            }
                        }
                    }
                }
                item(contentType = "editor-roles") {
                    ThemeRoleEditor(
                        draft = draft,
                        role = role,
                        copy = copy,
                        onRole = { role = it },
                        onColor = { color -> draft = draft.withColor(role, color) }
                    )
                }
                item(contentType = "editor-corners") {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                copy[ThemeLibraryText.Corners],
                                color = LevyraText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${draft.cornerScale}%", color = LevyraCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        val cornerRange = LevyraCustomTheme.CORNER_SCALE_RANGE.first.toFloat()..LevyraCustomTheme.CORNER_SCALE_RANGE.last.toFloat()
                        val cornerState = remember(cornerRange) { SliderState(draft.cornerScale.toFloat(), 0, cornerRange) }
                        cornerState.value = draft.cornerScale.toFloat()
                        Slider(
                            state = cornerState,
                            onValueChange = { draft = draft.copy(cornerScale = it.roundToInt()) },
                            modifier = Modifier.semantics { contentDescription = copy[ThemeLibraryText.Corners] }
                        )
                    }
                }
            }
        }
    }
}

private fun themeWithMode(theme: LevyraCustomTheme, light: Boolean): LevyraCustomTheme {
    if (theme.isLight == light) return theme
    val source = if (light) LevyraThemes.frost else LevyraThemes.levyraAura
    return theme.copy(
        isLight = light,
        background = source.black.toArgb(),
        surface = source.panel.toArgb()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeRoleEditor(
    draft: LevyraCustomTheme,
    role: CustomThemeRole,
    copy: ThemeLibraryCopy,
    onRole: (CustomThemeRole) -> Unit,
    onColor: (Int) -> Unit
) {
    val current = draft.colorOf(role)
    var hex by remember(role) { mutableStateOf(formatThemeHex(current)) }
    var showPicker by remember(role) { mutableStateOf(false) }
    var savedBeforePicker by remember(role) { mutableStateOf(current) }
    LaunchedEffect(role, current, showPicker) {
        if (!showPicker) {
            hex = formatThemeHex(current)
        }
    }
    val parsed = parseThemeHex(hex)
    val swatches = themeRoleSwatches(role, draft.isLight)
    Column(verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
        ) {
            CustomThemeRole.entries.forEach { option ->
                val active = option == role
                Row(
                    modifier = Modifier
                        .heightIn(min = LevyraPlayerDesign.MinimumTouchTarget)
                        .clip(CircleShape)
                        .background(if (active) LevyraPanelSoft else LevyraPanel)
                        .border(
                            width = if (active) 2.dp else 0.dp,
                            color = if (active) LevyraCyan else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onRole(option) }
                        .semantics {
                            this.role = Role.RadioButton
                            this.selected = active
                        }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(draft.colorOf(option)))
                            .border(1.dp, LevyraMuted.copy(alpha = 0.4f), CircleShape)
                    )
                    Text(option.label(copy), color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)
        ) {
            swatches.forEach { swatch ->
                ThemeAccentDot(
                    color = Color(swatch),
                    selected = swatch == current,
                    label = "#" + formatThemeHex(swatch),
                    outlined = true,
                    onClick = {
                        hex = formatThemeHex(swatch)
                        onColor(swatch)
                    }
                )
            }
        }
        com.luc4n3x.levyra.ui.components.LevyraColorInputAndPickerRow(
            color = parsed ?: current,
            hexText = hex,
            hexLabel = "${role.label(copy)} · ${copy[ThemeLibraryText.Hex]}",
            onHexTextChange = { raw ->
                val cleaned = com.luc4n3x.levyra.ui.components.sanitizeHexInput(raw)
                hex = cleaned
                parseThemeHex(cleaned)?.let(onColor)
            },
            onOpenPicker = {
                savedBeforePicker = current
                showPicker = true
            }
        )
    }
    if (showPicker) {
        com.luc4n3x.levyra.ui.components.LevyraAdvancedColorPickerDialog(
            initialColor = savedBeforePicker,
            presets = swatches,
            title = role.label(copy),
            onPreviewColor = onColor,
            onConfirmColor = { chosen ->
                showPicker = false
                hex = formatThemeHex(chosen)
                onColor(chosen)
            },
            onDismiss = {
                showPicker = false
                hex = formatThemeHex(savedBeforePicker)
                onColor(savedBeforePicker)
            }
        )
    }
}

private fun themeRoleSwatches(role: CustomThemeRole, light: Boolean): List<Int> = when (role) {
    CustomThemeRole.Background -> if (light) {
        listOf(0xFFFFFFFF, 0xFFF6F2EA, 0xFFF3F6FB, 0xFFF5F0FA, 0xFFEFF6F2, 0xFFFAF3EE)
    } else {
        listOf(0xFF000000, 0xFF0A0A0A, 0xFF05070F, 0xFF040B0A, 0xFF0E0508, 0xFF141218)
    }
    CustomThemeRole.Surface -> if (light) {
        listOf(0xFFEFF3FA, 0xFFE6DED0, 0xFFE4EBF4, 0xFFEDE4F5, 0xFFE0EDE6, 0xFFF1E4DA)
    } else {
        listOf(0xFF1D1D1F, 0xFF131A2B, 0xFF10211E, 0xFF26111A, 0xFF211F26, 0xFF22120D)
    }
    else -> LevyraThemeAccents.map { it.toLong() and 0xFFFFFFFFL } + listOf(0xFFD0BCFF, 0xFFA63C26)
}.map { it.toInt() }

@Composable
private fun ThemeStudioPreview(
    palette: ThemeStudioPalette,
    title: String,
    artist: String,
    label: String,
    cornerScale: Float = 1f
) {
    val outerShape = RoundedCornerShape(LevyraPlayerDesign.CornerLg * cornerScale)
    val tileShape = RoundedCornerShape(LevyraPlayerDesign.CornerXs * cornerScale)
    val artShape = RoundedCornerShape(LevyraPlayerDesign.CornerSm * cornerScale)
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
            shape = outerShape,
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
                            .clip(artShape)
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
                        shape = tileShape,
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp * cornerScale))
                        .background(surface)
                        .padding(horizontal = LevyraPlayerDesign.SpaceSm, vertical = LevyraPlayerDesign.SpaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp * cornerScale))
                            .background(Brush.linearGradient(listOf(accent, palette.secondary)))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(onSurface.copy(alpha = 0.75f))
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(16.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LevyraPlayerDesign.SpaceMd),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(width = if (index == 0) 44.dp else 20.dp, height = 20.dp)
                                .clip(CircleShape)
                                .background(if (index == 0) accent.copy(alpha = 0.32f) else muted.copy(alpha = 0.35f))
                        )
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
    modifier: Modifier = Modifier,
    defaultLabel: String? = null,
    selectedLabel: String? = null
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
                val states = listOfNotNull(selectedLabel.takeIf { selected }, defaultLabel)
                if (states.isNotEmpty()) stateDescription = states.joinToString(", ")
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
            if (defaultLabel != null) {
                Text(
                    defaultLabel,
                    color = palette.cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .border(1.dp, palette.cyan.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeAccentPicker(
    selected: Int,
    fromPresetLabel: String,
    accentLabels: List<String>,
    presetAccent: Color,
    onPreviewAccent: (Int?) -> Unit = {},
    onSelect: (Int) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val pickerCopy = strings.colorPickerCopy
    var showCustomPicker by remember { mutableStateOf(false) }
    val isCustomSelected = selected != 0 && selected !in LevyraThemeAccents
    val activeArgb = if (selected != 0) selected else presetAccent.toArgb()
    val activeColor = Color(activeArgb)
    var pickerInitialColor by remember { mutableStateOf(activeArgb) }

    Column(verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)) {
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
            LevyraThemeAccents.forEachIndexed { index, value ->
                ThemeAccentDot(
                    color = Color(value),
                    selected = selected == value,
                    label = accentLabels[index],
                    outlined = false,
                    onClick = { onSelect(value) }
                )
            }
            if (isCustomSelected) {
                ThemeAccentDot(
                    color = Color(selected),
                    selected = true,
                    label = "#" + formatThemeHex(selected),
                    outlined = true,
                    onClick = {
                        pickerInitialColor = activeArgb
                        showCustomPicker = true
                    }
                )
            }
        }

        Surface(
            onClick = {
                pickerInitialColor = activeArgb
                showCustomPicker = true
            },
            shape = RoundedCornerShape(22.dp),
            color = LevyraPanel,
            border = BorderStroke(
                width = if (isCustomSelected) 1.5.dp else 1.dp,
                color = if (isCustomSelected) activeColor.copy(alpha = 0.72f) else LevyraMuted.copy(alpha = 0.28f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(activeColor)
                        .border(1.dp, LevyraMuted.copy(alpha = 0.45f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = LevyraOnAccent,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = pickerCopy[ColorPickerText.OpenPicker],
                            color = LevyraText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(activeColor.copy(alpha = 0.20f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#${formatThemeHex(activeArgb)}",
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LevyraText
                            )
                        }
                    }
                    Text(
                        text = pickerCopy[ColorPickerText.Subtitle],
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    if (showCustomPicker) {
        com.luc4n3x.levyra.ui.components.LevyraAdvancedColorPickerDialog(
            initialColor = pickerInitialColor,
            presets = LevyraThemeAccents,
            title = strings.themeAccent,
            onPreviewColor = { preview -> onPreviewAccent(preview) },
            onConfirmColor = { chosen ->
                showCustomPicker = false
                onPreviewAccent(null)
                onSelect(chosen)
            },
            onDismiss = {
                showCustomPicker = false
                onPreviewAccent(null)
            }
        )
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
                this.contentDescription = label
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
    pureBlack: Boolean,
    customThemes: List<LevyraCustomTheme>
): ThemeStudioPalette = remember(presetId, accent, pureBlack, customThemes) {
    themeStudioPaletteFor(presetId, accent, pureBlack)
}

internal fun themeStudioPaletteOf(palette: LevyraPalette): ThemeStudioPalette = ThemeStudioPalette(
    background = palette.black,
    surface = palette.panel,
    accent = palette.cyan,
    secondary = palette.violet,
    text = palette.text,
    muted = palette.muted,
    onAccent = if (palette.isLight) Color.White else palette.black
)

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
