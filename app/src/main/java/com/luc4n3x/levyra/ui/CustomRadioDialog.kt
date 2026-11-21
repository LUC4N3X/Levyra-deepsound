package com.luc4n3x.levyra.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.feature.radio.CustomRadioError
import com.luc4n3x.levyra.feature.radio.RadioImportResult
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraUtilityBackdrop
import com.luc4n3x.levyra.ui.components.LevyraUtilitySeed
import com.luc4n3x.levyra.ui.components.levyraUtilityColors
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.RadioLibraryCopy
import com.luc4n3x.levyra.ui.i18n.radioLibraryCopy
import com.luc4n3x.levyra.ui.theme.LevyraSkinManagesLayout
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomRadioDialog(
    station: RadioStation?, saving: Boolean, error: CustomRadioError?,
    onDismiss: () -> Unit, onChanged: () -> Unit, onSave: (String, String, String) -> Unit,
    imported: RadioImportResult? = null, onImport: (Uri) -> Unit = {}
) {
    val strings = LocalLevyraStrings.current
    val copy = radioLibraryCopy(strings.code)
    var name by rememberSaveable(station?.uuid) { mutableStateOf(station?.name.orEmpty()) }
    var stream by rememberSaveable(station?.uuid) { mutableStateOf(station?.streamUrl.orEmpty()) }
    var image by rememberSaveable(station?.uuid) { mutableStateOf(station?.faviconUrl.orEmpty()) }
    var importing by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importing = true
            onImport(uri)
        }
    }
    val busy by rememberUpdatedState(saving)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value -> value != SheetValue.Hidden || !busy }
    )
    val importFailed = error == CustomRadioError.Playlist
    val importDone = station == null && imported != null && name.isBlank() && stream.isBlank()
    val fieldShape = if (LevyraSkinManagesLayout) MaterialTheme.shapes.medium else RoundedCornerShape(20.dp)
    val submit = { importing = false; onSave(name, stream, image) }
    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Box {
            LevyraUtilityBackdrop(Modifier.matchParentSize(), accent = LevyraUtilitySeed, strength = CustomRadioHaloStrength)
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BottomSheetDefaults.DragHandle(Modifier.align(Alignment.CenterHorizontally))
                CustomRadioPreview(
                    title = if (station == null) copy.add else copy.edit,
                    name = name,
                    stream = stream,
                    image = image
                )
                if (station == null) {
                    RadioPlaylistImportCard(
                        title = copy.importPlaylist,
                        supporting = radioImportSupporting(copy, importFailed, imported),
                        busy = saving && importing,
                        done = imported != null && !importFailed,
                        failed = importFailed,
                        enabled = !saving,
                        shape = fieldShape,
                        onClick = { onChanged(); picker.launch(RadioPlaylistMimeTypes) }
                    )
                }
                CustomRadioFields(
                    values = CustomRadioValues(name, stream, image),
                    error = error,
                    saving = saving,
                    shape = fieldShape,
                    onName = { name = it.take(160); onChanged() },
                    onStream = { stream = it.take(4096); onChanged() },
                    onImage = { image = it.take(4096); onChanged() }
                )
                if (error != null && !importFailed) {
                    Text(
                        copy.error(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                CustomRadioFooter(
                    importDone = importDone,
                    saving = saving,
                    savingStation = saving && !importing,
                    onDismiss = onDismiss,
                    onSubmit = submit
                )
            }
        }
    }
}

private const val CustomRadioHaloStrength = 0.6f

private data class CustomRadioValues(val name: String, val stream: String, val image: String)

private fun radioImportSupporting(copy: RadioLibraryCopy, failed: Boolean, imported: RadioImportResult?): String = when {
    failed -> copy.error(CustomRadioError.Playlist)
    imported != null -> copy.imported(imported.added, imported.skipped)
    else -> RadioPlaylistFormats
}

@Composable
private fun CustomRadioFields(
    values: CustomRadioValues,
    error: CustomRadioError?,
    saving: Boolean,
    shape: Shape,
    onName: (String) -> Unit,
    onStream: (String) -> Unit,
    onImage: (String) -> Unit
) {
    val copy = radioLibraryCopy(LocalLevyraStrings.current.code)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CustomRadioField(
            value = values.name, label = copy.name, icon = Icons.Rounded.Radio, shape = shape,
            enabled = !saving, isError = error == CustomRadioError.Name,
            keyboardType = KeyboardType.Text, imeAction = ImeAction.Next, onValueChange = onName
        )
        CustomRadioField(
            value = values.stream, label = copy.stream, icon = Icons.Rounded.Link, shape = shape,
            enabled = !saving, isError = error == CustomRadioError.StreamUrl || error == CustomRadioError.Duplicate,
            keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next, onValueChange = onStream
        )
        CustomRadioField(
            value = values.image, label = copy.image, icon = Icons.Rounded.Image, shape = shape,
            enabled = !saving, isError = error == CustomRadioError.ImageUrl,
            keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done, onValueChange = onImage
        )
    }
}

@Composable
private fun CustomRadioFooter(
    importDone: Boolean,
    saving: Boolean,
    savingStation: Boolean,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!importDone) {
            TextButton(
                onClick = onDismiss,
                enabled = !saving,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp)
            ) { Text(strings.cancel, style = MaterialTheme.typography.titleSmall) }
        }
        Button(
            onClick = if (importDone) onDismiss else onSubmit,
            enabled = importDone || !saving,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
        ) {
            if (savingStation) {
                LevyraLoadingIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (importDone) strings.done else strings.save, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CustomRadioPreview(title: String, name: String, stream: String, image: String) {
    val scheme = MaterialTheme.colorScheme
    val artwork = image.trim().takeIf { it.startsWith("https://", ignoreCase = true) && it.length > 12 }
    val host = remember(stream) { stream.trim().toHttpUrlOrNull()?.host.orEmpty() }
    val badgeShape = MaterialShapes.Cookie9Sided.toShape()
    val utility = levyraUtilityColors(LevyraUtilitySeed)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(64.dp).clip(badgeShape).background(utility.container),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Podcasts, null, Modifier.size(32.dp), tint = utility.onContainer)
            if (artwork != null) {
                StableRemoteArtwork(url = artwork, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val typed = name.trim()
            if (typed.isNotEmpty()) Text(title, style = MaterialTheme.typography.labelLarge, color = scheme.primary)
            AnimatedContent(
                targetState = typed.ifEmpty { title },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "customRadioName"
            ) { headline ->
                Text(
                    headline,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (host.isNotBlank()) {
                Text(host, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CustomRadioField(
    value: String,
    label: String,
    icon: ImageVector,
    shape: Shape,
    enabled: Boolean,
    isError: Boolean,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        enabled = enabled,
        singleLine = true,
        isError = isError,
        shape = shape,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = scheme.surfaceContainerHigh,
            focusedContainerColor = scheme.surfaceContainerHigh,
            disabledContainerColor = scheme.surfaceContainerHigh,
            unfocusedBorderColor = scheme.surfaceContainerHigh,
            unfocusedLeadingIconColor = scheme.onSurfaceVariant,
            focusedLeadingIconColor = scheme.primary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RadioPlaylistImportCard(
    title: String,
    supporting: String,
    busy: Boolean,
    done: Boolean,
    failed: Boolean,
    enabled: Boolean,
    shape: Shape,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val utility = levyraUtilityColors(LevyraUtilitySeed)
    val container = when {
        failed -> scheme.errorContainer
        done -> utility.softContainer
        else -> scheme.surfaceContainerHighest
    }
    val content = if (failed) scheme.onErrorContainer else scheme.onSurface
    val badge = when {
        failed -> scheme.error
        done -> utility.accent
        else -> utility.container
    }
    val badgeContent = when {
        failed -> scheme.onError
        done -> utility.onAccent
        else -> utility.onContainer
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = container,
        contentColor = content,
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(badge), contentAlignment = Alignment.Center) {
                when {
                    busy -> LevyraLoadingIndicator(Modifier.size(30.dp), color = badgeContent)
                    failed -> Icon(Icons.Rounded.ErrorOutline, null, tint = badgeContent)
                    done -> Icon(Icons.Rounded.CheckCircle, null, tint = badgeContent)
                    else -> Icon(Icons.Rounded.FileOpen, null, tint = badgeContent)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.78f))
            }
        }
    }
}

private const val RadioPlaylistFormats = "M3U · M3U8"

private val RadioPlaylistMimeTypes = arrayOf(
    "audio/x-mpegurl", "audio/mpegurl", "application/x-mpegurl", "application/vnd.apple.mpegurl",
    "text/plain", "application/octet-stream"
)
