package com.luc4n3x.levyra.ui.lyrics.studio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.LyricsFontEntry
import com.luc4n3x.levyra.data.LyricsFontError
import com.luc4n3x.levyra.data.LyricsFontException
import com.luc4n3x.levyra.data.LyricsFontLibrary
import com.luc4n3x.levyra.domain.LevyraLyricsStudioSettings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.lyrics.lyricsFontFamily
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val FONT_MIME_TYPES = arrayOf(
    "font/ttf", "font/otf", "font/sfnt", "application/x-font-ttf", "application/x-font-otf",
    "application/x-font-opentype", "application/vnd.ms-opentype", "application/font-sfnt", "application/octet-stream"
)

private const val FONT_PREVIEW_SAMPLE = "Aa Lyrics · 歌詞 · Текст · كلمات"

@Composable
internal fun LyricsFontLibrarySection(
    settings: LevyraLyricsStudioSettings,
    onUpdateSettings: (LevyraLyricsStudioSettings) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    val library = remember(context) { LyricsFontLibrary.get(context) }
    val fonts by library.fonts.collectAsState()
    val latestSettings by rememberUpdatedState(settings)
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var replaceTarget by remember { mutableStateOf<LyricsFontEntry?>(null) }
    var renameTarget by remember { mutableStateOf<LyricsFontEntry?>(null) }
    var deleteTarget by remember { mutableStateOf<LyricsFontEntry?>(null) }

    LaunchedEffect(library) {
        runCatching { library.load() }
        loaded = true
    }

    fun runFontTask(successMessage: String, task: suspend () -> LyricsFontEntry, onDone: (LyricsFontEntry) -> Unit) {
        if (busy) return
        busy = true
        status = strings.lyricsFontsImporting
        scope.launch {
            status = try {
                onDone(task())
                successMessage
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: LyricsFontException) {
                lyricsFontErrorMessage(strings, error.error)
            } catch (error: Exception) {
                strings.lyricsFontsFailed
            } finally {
                busy = false
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runFontTask(
            strings.lyricsFontsImported,
            {
                val fallbackName = withContext(Dispatchers.IO) { displayNameOf(context, uri).substringBeforeLast('.') }
                library.import({ context.contentResolver.openInputStream(uri) }, fallbackName)
            }
        ) { entry -> onUpdateSettings(latestSettings.copy(customFontId = entry.id)) }
    }
    val replaceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val target = replaceTarget
        replaceTarget = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
        runFontTask(
            strings.lyricsFontsReplaced,
            { library.replace(target.id) { context.contentResolver.openInputStream(uri) } }
        ) { }
    }

    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF16161A))
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape)
            .padding(16.dp)
    ) {
        LyricsFontLibraryHeader(busy = busy, onImport = { importLauncher.launch(FONT_MIME_TYPES) })
        val selectedMissing = loaded && settings.customFontId.isNotBlank() && fonts.none { it.id == settings.customFontId }
        val notice = status.ifBlank { if (selectedMissing) strings.lyricsFontsUnavailable else "" }
        if (notice.isNotBlank()) {
            Text(
                notice,
                color = LevyraMuted,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        fonts.forEach { entry ->
            LyricsFontRow(
                entry = entry,
                library = library,
                selected = entry.id == settings.customFontId,
                enabled = !busy,
                onSelect = { onUpdateSettings(latestSettings.copy(customFontId = entry.id)) },
                onRename = { renameTarget = entry },
                onReplace = {
                    replaceTarget = entry
                    replaceLauncher.launch(FONT_MIME_TYPES)
                },
                onDelete = { deleteTarget = entry }
            )
        }
    }

    renameTarget?.let { entry ->
        LyricsFontRenameDialog(entry, onDismiss = { renameTarget = null }) { name ->
            renameTarget = null
            scope.launch { runCatching { library.rename(entry.id, name) } }
        }
    }

    deleteTarget?.let { entry ->
        LyricsFontDeleteDialog(onDismiss = { deleteTarget = null }) {
            deleteTarget = null
            status = ""
            if (latestSettings.customFontId == entry.id) onUpdateSettings(latestSettings.copy(customFontId = ""))
            scope.launch { runCatching { library.delete(entry.id) } }
        }
    }
}

@Composable
private fun LyricsFontLibraryHeader(busy: Boolean, onImport: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(strings.lyricsFontsTitle, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(strings.lyricsFontsHint, color = LevyraMuted, fontSize = 12.sp, lineHeight = 16.sp)
        OutlinedButton(
            onClick = onImport,
            enabled = !busy,
            border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.6f)),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LevyraCyan)
            } else {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
            }
            Text(
                strings.lyricsFontsImport,
                color = LevyraCyan,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun LyricsFontRenameDialog(entry: LyricsFontEntry, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val strings = LocalLevyraStrings.current
    var name by remember(entry.id) { mutableStateOf(entry.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.lyricsFontsRename) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(64) },
                label = { Text(strings.lyricsFontsName) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun LyricsFontDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val strings = LocalLevyraStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.lyricsFontsDeleteTitle) },
        text = { Text(strings.lyricsFontsDeleteBody) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(strings.delete) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun LyricsFontRow(
    entry: LyricsFontEntry,
    library: LyricsFontLibrary,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val previewFamily by produceState<FontFamily?>(null, entry.fileName) {
        value = library.fontFile(entry.id)?.let { (resolved, file) -> lyricsFontFamily(resolved, file) }
    }
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) LevyraCyan.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, if (selected) LevyraCyan.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.06f), shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.name,
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (entry.variable) {
                    Text(
                        strings.lyricsFontsVariable,
                        color = LevyraCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LevyraCyan.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                FONT_PREVIEW_SAMPLE,
                color = LevyraText.copy(alpha = 0.86f),
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontFamily = previewFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "${entry.format.extension.uppercase()} · ${formatFontSize(entry.sizeBytes)}",
                color = LevyraMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
        }
        Box {
            IconButton(
                onClick = { menuOpen = true },
                enabled = enabled,
                modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget)
            ) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "${strings.lyricsFontsOptions}: ${entry.name}", tint = LevyraMuted)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(strings.lyricsFontsRename) }, onClick = { menuOpen = false; onRename() })
                DropdownMenuItem(text = { Text(strings.lyricsFontsReplace) }, onClick = { menuOpen = false; onReplace() })
                DropdownMenuItem(text = { Text(strings.delete) }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }
}

private fun lyricsFontErrorMessage(strings: LevyraStrings, error: LyricsFontError): String = when (error) {
    LyricsFontError.TOO_LARGE -> strings.lyricsFontsTooLarge
    LyricsFontError.INVALID -> strings.lyricsFontsInvalid
    LyricsFontError.LIBRARY_FULL -> strings.lyricsFontsLibraryFull
    LyricsFontError.UNREADABLE -> strings.lyricsFontsFailed
}

private fun formatFontSize(bytes: Long): String =
    if (bytes >= 1024L * 1024L) "%.1f MB".format(java.util.Locale.ROOT, bytes / (1024.0 * 1024.0))
    else "${(bytes / 1024L).coerceAtLeast(1L)} KB"

private fun displayNameOf(context: Context, uri: Uri): String = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull().orEmpty()
