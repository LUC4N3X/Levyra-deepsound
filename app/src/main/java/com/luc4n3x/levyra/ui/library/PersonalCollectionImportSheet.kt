package com.luc4n3x.levyra.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.data.collections.CollectionIssueKind
import com.luc4n3x.levyra.data.collections.CollectionSyncError
import com.luc4n3x.levyra.data.collections.PersonalCollectionImportSource
import com.luc4n3x.levyra.data.collections.PersonalCollectionPreview
import com.luc4n3x.levyra.data.collections.PersonalCollectionSourceKind
import com.luc4n3x.levyra.data.collections.PersonalCollectionUrlPolicy
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.i18n.LevyraPersonalCollectionStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.viewmodel.PersonalCollectionImportState

private enum class ImportStep { Source, Link }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PersonalCollectionImportSheet(
    state: PersonalCollectionImportState,
    strings: LevyraStrings,
    collectionStrings: LevyraPersonalCollectionStrings,
    onPreview: (PersonalCollectionImportSource, Boolean) -> Unit,
    onConfirm: (name: String, autoRefresh: Boolean, allowCleartext: Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded))
    var stepName by rememberSaveable { mutableStateOf(ImportStep.Source.name) }
    var url by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var credentialSecret by remember { mutableStateOf("") }
    var allowCleartext by rememberSaveable { mutableStateOf(false) }
    val step = ImportStep.valueOf(stepName)
    val localNetwork = rememberLocalNetworkAccess()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) onPreview(PersonalCollectionImportSource.LocalFile(uri), false)
    }

    ModalBottomSheet(
        onDismissRequest = { if (state !is PersonalCollectionImportState.Saving) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                collectionStrings.importTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            AnimatedContent(
                targetState = importContentKey(state, step),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "personal-collection-import"
            ) { key ->
                when (key) {
                    ImportContent.Source -> ImportSourceStep(
                        collectionStrings = collectionStrings,
                        onFile = { picker.launch(arrayOf("*/*")) },
                        onLink = { stepName = ImportStep.Link.name }
                    )
                    ImportContent.Link -> ImportLinkStep(
                        strings = strings,
                        collectionStrings = collectionStrings,
                        url = url,
                        username = username,
                        credentialSecret = credentialSecret,
                        allowCleartext = allowCleartext,
                        onUrl = { url = it },
                        onUsername = { username = it },
                        onCredentialSecret = { credentialSecret = it },
                        onAllowCleartext = { allowCleartext = it },
                        onBack = { stepName = ImportStep.Source.name },
                        onContinue = {
                            val source = PersonalCollectionImportSource.Remote(url.trim(), username.trim(), credentialSecret)
                            val host = url.trim().toHttpUrlOrNull()?.host.orEmpty()
                            if (PersonalCollectionUrlPolicy.isLocalNetworkLiteral(host)) {
                                localNetwork.ensure { granted ->
                                    if (granted) onPreview(source, allowCleartext)
                                }
                            } else {
                                onPreview(source, allowCleartext)
                            }
                        }
                    )
                    ImportContent.Loading -> ImportProgress(collectionStrings.reading, strings.cancel, onCancel = onReset)
                    ImportContent.Saving -> ImportProgress(collectionStrings.reading, null, onCancel = {})
                    ImportContent.Preview -> (state as? PersonalCollectionImportState.Preview)?.let { previewState ->
                        ImportPreviewStep(
                            preview = previewState.preview,
                            strings = strings,
                            collectionStrings = collectionStrings,
                            initialCleartext = allowCleartext,
                            localNetworkGranted = localNetwork.granted,
                            onBack = onReset,
                            onConfirm = { name, autoRefresh, cleartext ->
                                if (previewState.preview.usesLocalNetwork) {
                                    localNetwork.ensure { granted ->
                                        if (granted) onConfirm(name, autoRefresh, cleartext)
                                    }
                                } else {
                                    onConfirm(name, autoRefresh, cleartext)
                                }
                            }
                        )
                    }
                    ImportContent.Failed -> ImportFailure(
                        error = (state as? PersonalCollectionImportState.Failed)?.error ?: CollectionSyncError.Unknown,
                        strings = strings,
                        collectionStrings = collectionStrings,
                        onBack = onReset
                    )
                }
            }
        }
    }
}

private enum class ImportContent { Source, Link, Loading, Preview, Failed, Saving }

private fun importContentKey(state: PersonalCollectionImportState, step: ImportStep): ImportContent = when (state) {
    PersonalCollectionImportState.Idle -> if (step == ImportStep.Link) ImportContent.Link else ImportContent.Source
    PersonalCollectionImportState.Loading -> ImportContent.Loading
    PersonalCollectionImportState.Saving -> ImportContent.Saving
    is PersonalCollectionImportState.Preview -> ImportContent.Preview
    is PersonalCollectionImportState.Failed -> ImportContent.Failed
}

@Composable
private fun ImportSourceStep(
    collectionStrings: LevyraPersonalCollectionStrings,
    onFile: () -> Unit,
    onLink: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ImportSourceOption(
            icon = Icons.Rounded.FileOpen,
            title = collectionStrings.fromFile,
            detail = collectionStrings.fromFileDetail,
            shapeIndex = 0,
            onClick = onFile
        )
        ImportSourceOption(
            icon = Icons.Rounded.Link,
            title = collectionStrings.fromUrl,
            detail = collectionStrings.fromUrlDetail,
            shapeIndex = 1,
            onClick = onLink
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportSourceOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    shapeIndex: Int,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainerHigh, shape = levyraGroupedListShape(shapeIndex, 2), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(if (shapeIndex == 0) MaterialShapes.Cookie4Sided.toShape() else MaterialShapes.Clover4Leaf.toShape())
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportLinkStep(
    strings: LevyraStrings,
    collectionStrings: LevyraPersonalCollectionStrings,
    url: String,
    username: String,
    credentialSecret: String,
    allowCleartext: Boolean,
    onUrl: (String) -> Unit,
    onUsername: (String) -> Unit,
    onCredentialSecret: (String) -> Unit,
    onAllowCleartext: (Boolean) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val trimmed = url.trim()
    val cleartext = trimmed.startsWith("http://", ignoreCase = true)
    val valid = trimmed.startsWith("https://", ignoreCase = true) || cleartext
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrl,
            label = { Text(collectionStrings.urlLabel) },
            placeholder = { Text("https://") },
            leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = username,
            onValueChange = onUsername,
            label = { Text(collectionStrings.usernameLabel) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = credentialSecret,
            onValueChange = onCredentialSecret,
            label = { Text(collectionStrings.passwordLabel) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )
        if (cleartext) {
            CleartextConsentRow(collectionStrings, allowCleartext, onAllowCleartext)
        }
        PrivacyNote(collectionStrings.privacyNote)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                Text(strings.back)
            }
            Button(
                onClick = onContinue,
                enabled = valid && (!cleartext || allowCleartext),
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp)
            ) {
                Text(collectionStrings.continueAction)
            }
        }
    }
}

@Composable
internal fun CleartextConsentRow(
    collectionStrings: LevyraPersonalCollectionStrings,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.errorContainer.copy(alpha = 0.55f), shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Switch) { onChecked(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(collectionStrings.allowHttp, style = MaterialTheme.typography.titleSmall, color = colors.onErrorContainer)
                Text(collectionStrings.allowHttpDetail, style = MaterialTheme.typography.bodySmall, color = colors.onErrorContainer)
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun PrivacyNote(text: String) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportProgress(label: String, cancelLabel: String?, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LevyraLoadingIndicator(modifier = Modifier.size(56.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (cancelLabel != null) {
            TextButton(onClick = onCancel) { Text(cancelLabel) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportPreviewStep(
    preview: PersonalCollectionPreview,
    strings: LevyraStrings,
    collectionStrings: LevyraPersonalCollectionStrings,
    initialCleartext: Boolean,
    localNetworkGranted: Boolean,
    onBack: () -> Unit,
    onConfirm: (String, Boolean, Boolean) -> Unit
) {
    var name by rememberSaveable(preview.sourceLabel) { mutableStateOf(preview.parsed.name) }
    var autoRefresh by rememberSaveable(preview.sourceLabel) { mutableStateOf(false) }
    val sourceCleartext = preview.sourceKind == PersonalCollectionSourceKind.URL &&
        preview.sourceUrl.startsWith("http://", ignoreCase = true)
    var allowCleartext by rememberSaveable(preview.sourceLabel) { mutableStateOf(initialCleartext || sourceCleartext) }
    LaunchedEffect(preview) { if (name.isBlank()) name = preview.parsed.name }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ImportPreviewHeader(preview, strings, collectionStrings)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(collectionStrings.nameLabel) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )
        ImportPreviewSample(preview)
        ImportPreviewNotes(preview, collectionStrings)
        ImportPreviewOptions(
            preview = preview,
            collectionStrings = collectionStrings,
            sourceCleartext = sourceCleartext,
            localNetworkGranted = localNetworkGranted,
            allowCleartext = allowCleartext,
            autoRefresh = autoRefresh,
            onAllowCleartext = { allowCleartext = it },
            onAutoRefresh = { autoRefresh = it }
        )
        ImportPreviewActions(strings, collectionStrings, name.isNotBlank(), onBack) {
            onConfirm(name, autoRefresh, allowCleartext)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportPreviewHeader(
    preview: PersonalCollectionPreview,
    strings: LevyraStrings,
    collectionStrings: LevyraPersonalCollectionStrings
) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier.size(56.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(colors.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colors.onTertiaryContainer)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                strings.formatTrackCount(preview.parsed.entries.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Text(
                preview.sourceLabel.ifBlank { collectionStrings.sourceFile },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ImportPreviewSample(preview: PersonalCollectionPreview) {
    val colors = MaterialTheme.colorScheme
    val sample = remember(preview) { preview.parsed.entries.take(PREVIEW_SAMPLE) }
    Surface(color = colors.surfaceContainerHigh, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            sample.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.End
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (entry.artist.isNotBlank()) {
                            Text(entry.artist, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (preview.parsed.entries.size > sample.size) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = colors.outlineVariant)
                Text(
                    "+${preview.parsed.entries.size - sample.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ImportPreviewNotes(preview: PersonalCollectionPreview, strings: LevyraPersonalCollectionStrings) {
    val duplicates = preview.parsed.issueCount(CollectionIssueKind.Duplicate)
    val skipped = preview.parsed.skippedCount - duplicates
    val notes = buildList {
        if (skipped > 0) add(strings.skipped(skipped))
        if (duplicates > 0) add(strings.duplicates(duplicates))
        if (preview.cleartextEntries > 0) add(strings.httpTracks(preview.cleartextEntries))
    }
    if (notes.isNotEmpty()) {
        Text(notes.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ImportPreviewOptions(
    preview: PersonalCollectionPreview,
    collectionStrings: LevyraPersonalCollectionStrings,
    sourceCleartext: Boolean,
    localNetworkGranted: Boolean,
    allowCleartext: Boolean,
    autoRefresh: Boolean,
    onAllowCleartext: (Boolean) -> Unit,
    onAutoRefresh: (Boolean) -> Unit
) {
    if (preview.usesLocalNetwork && !localNetworkGranted) PrivacyNote(collectionStrings.localNetworkDetail)
    if (preview.requiresCleartextConsent && !sourceCleartext) {
        CleartextConsentRow(collectionStrings, allowCleartext, onAllowCleartext)
    }
    if (preview.sourceKind == PersonalCollectionSourceKind.URL) {
        SettingSwitchRow(collectionStrings.autoRefresh, collectionStrings.autoRefreshDetail, autoRefresh, onAutoRefresh)
    }
}

@Composable
private fun ImportPreviewActions(
    strings: LevyraStrings,
    collectionStrings: LevyraPersonalCollectionStrings,
    enabled: Boolean,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onBack, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
            Text(strings.back)
        }
        Button(
            onClick = onConfirm,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.weight(1f).heightIn(min = 52.dp)
        ) {
            Text(collectionStrings.importAction)
        }
    }
}

@Composable
internal fun SettingSwitchRow(title: String, detail: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainerHigh, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Switch) { onChecked(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportFailure(
    error: CollectionSyncError,
    strings: LevyraStrings,
    collectionStrings: LevyraPersonalCollectionStrings,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(colors.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = colors.onErrorContainer)
        }
        Text(
            collectionStrings.error(error),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(2.dp))
        Button(onClick = onBack, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(strings.back)
        }
    }
}

private const val PREVIEW_SAMPLE = 5
