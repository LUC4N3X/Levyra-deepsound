package com.luc4n3x.levyra.ui.lyrics

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.OutlinedTextFieldDefaults
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.data.CustomLyricsParse
import com.luc4n3x.levyra.data.CustomLyricsParser
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.viewmodel.CustomLyricsDraft
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CustomLyricsEditorSheet(
    draft: CustomLyricsDraft,
    onSave: (String) -> Unit,
    onRestoreOriginal: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by rememberSaveable(draft.track.id) { mutableStateOf(draft.source) }
    var confirmRestore by rememberSaveable(draft.track.id) { mutableStateOf(false) }
    val preview by produceState<Pair<String, CustomLyricsParse>?>(initialValue = null, source) {
        if (value != null) delay(CUSTOM_LYRICS_PREVIEW_DEBOUNCE_MS)
        value = source to withContext(Dispatchers.Default) { CustomLyricsParser.parse(source) }
    }
    val parse = preview?.takeIf { it.first == source }?.second
    val canSave = parse is CustomLyricsParse.Valid && !draft.saving

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null)
                    }
                }
                Text(
                    text = strings.customLyrics,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            CustomLyricsTrackCard(draft)
            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                enabled = !draft.saving,
                placeholder = { Text(CUSTOM_LYRICS_PLACEHOLDER, fontFamily = FontFamily.Monospace) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedBorderColor = Color.Transparent
                ),
                minLines = 6,
                maxLines = 12,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
            )
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val clip = clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 } ?: return@launch
                        val pasted = withContext(Dispatchers.IO) { clip.getItemAt(0)?.coerceToText(context)?.toString() }
                        if (!pasted.isNullOrBlank()) source = pasted
                    }
                },
                enabled = !draft.saving,
                shapes = ButtonDefaults.shapes()
            ) {
                Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(strings.customLyricsPaste)
            }
            CustomLyricsPreview(parse = parse, failed = draft.failed, strings = strings)
            Button(
                onClick = { onSave(source) },
                enabled = canSave,
                shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (draft.saving) {
                    LevyraLoadingIndicator(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                }
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(strings.save)
            }
            if (draft.existing) {
                TextButton(
                    onClick = { confirmRestore = true },
                    enabled = !draft.saving,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(strings.customLyricsRestore, textAlign = TextAlign.Center)
                }
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            icon = { Icon(Icons.Rounded.Restore, contentDescription = null) },
            title = { Text(strings.customLyricsRestore) },
            text = { Text(strings.customLyricsRestoreBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRestore = false
                        onRestoreOriginal()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun CustomLyricsTrackCard(draft: CustomLyricsDraft) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(draft.track.thumbnailUrl).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceContainerHighest)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = draft.track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                if (draft.track.artist.isNotBlank()) {
                    Text(
                        text = draft.track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomLyricsPreview(parse: CustomLyricsParse?, failed: Boolean, strings: LevyraStrings) {
    val colors = MaterialTheme.colorScheme
    val error = failed || parse == CustomLyricsParse.Invalid || parse == CustomLyricsParse.TooLong
    Surface(
        color = if (error) colors.errorContainer else colors.surfaceContainerHigh,
        contentColor = if (error) colors.onErrorContainer else colors.onSurface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                failed -> CustomLyricsMessage(strings.customLyricsFailed, error = true)
                parse == null -> LevyraLoadingIndicator(modifier = Modifier.size(24.dp))
                parse == CustomLyricsParse.Empty -> Text(strings.customLyricsHint, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                parse == CustomLyricsParse.TooLong -> CustomLyricsMessage(strings.customLyricsTooLong, error = true)
                parse == CustomLyricsParse.Invalid -> CustomLyricsMessage(strings.customLyricsInvalid, error = true)
                parse is CustomLyricsParse.Valid -> CustomLyricsValidPreview(parse, strings)
            }
        }
    }
}

@Composable
private fun CustomLyricsMessage(text: String, error: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (error) Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CustomLyricsValidPreview(parse: CustomLyricsParse.Valid, strings: LevyraStrings) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = "${if (parse.synced) strings.synced else strings.customLyricsUnsynced} · ${parse.lines.size} ${strings.linesLabel}",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        color = if (parse.synced) colors.primary else colors.onSurfaceVariant
    )
    if (parse.ignoredLines > 0) {
        Text(
            text = "${strings.customLyricsSkipped}: ${parse.ignoredLines}",
            style = MaterialTheme.typography.labelMedium,
            color = colors.tertiary
        )
    }
    parse.lines.take(CUSTOM_LYRICS_PREVIEW_LINES).forEach { line ->
        CustomLyricsPreviewLine(line, parse.synced)
    }
}

@Composable
private fun CustomLyricsPreviewLine(line: LyricLine, synced: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        if (synced) {
            Text(
                text = customLyricsTimestamp(line.startMs),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AddCustomLyricsButton(onClick: () -> Unit, modifier: Modifier = Modifier, label: String? = null) {
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color.White.copy(alpha = 0.14f),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(label ?: LocalLevyraStrings.current.addCustomLyrics, fontWeight = FontWeight.Bold)
    }
}

internal fun customLyricsTimestamp(startMs: Long): String {
    val clamped = startMs.coerceAtLeast(0L)
    return String.format(Locale.ROOT, "%02d:%02d.%02d", clamped / 60_000L, clamped / 1_000L % 60L, clamped / 10L % 100L)
}

private const val CUSTOM_LYRICS_PREVIEW_DEBOUNCE_MS = 180L
private const val CUSTOM_LYRICS_PREVIEW_LINES = 4
private const val CUSTOM_LYRICS_PLACEHOLDER = "[00:12.50] …\n[00:16.20] …"
