package com.luc4n3x.levyra.ui.playlistimport

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.domain.PlaylistImportFailureKind
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportCompleteness
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportSource
import com.luc4n3x.levyra.nexus.playlistimport.ResolutionPreference
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.PlaylistImportHubCopy
import com.luc4n3x.levyra.ui.i18n.playlistImportFailureMessage
import com.luc4n3x.levyra.ui.i18n.playlistImportHubCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.viewmodel.PlaylistImportActivity
import com.luc4n3x.levyra.viewmodel.PlaylistImportController
import com.luc4n3x.levyra.viewmodel.PlaylistImportStep
import com.luc4n3x.levyra.viewmodel.PlaylistImportUiState

internal val ImportShape = RoundedCornerShape(20.dp)
internal val ImportRowShape = RoundedCornerShape(14.dp)

internal fun PlaylistImportSource.displayName(): String = when (this) {
    PlaylistImportSource.YOUTUBE_MUSIC -> "YouTube Music"
    PlaylistImportSource.YOUTUBE -> "YouTube"
    PlaylistImportSource.SPOTIFY -> "Spotify"
    PlaylistImportSource.APPLE_MUSIC -> "Apple Music"
    PlaylistImportSource.DEEZER -> "Deezer"
    PlaylistImportSource.TIDAL -> "TIDAL"
    PlaylistImportSource.SOUNDCLOUD -> "SoundCloud"
    PlaylistImportSource.JIOSAAVN -> "JioSaavn"
    PlaylistImportSource.AMAZON_MUSIC -> "Amazon Music"
    PlaylistImportSource.BANDCAMP -> "Bandcamp"
    PlaylistImportSource.M3U -> "M3U"
    PlaylistImportSource.PLS -> "PLS"
    PlaylistImportSource.XSPF -> "XSPF"
    PlaylistImportSource.CSV -> "CSV"
    PlaylistImportSource.TSV -> "TSV"
    PlaylistImportSource.EXPORTIFY -> "Exportify CSV"
    PlaylistImportSource.TUNEMYMUSIC -> "TuneMyMusic CSV"
    PlaylistImportSource.KREATE -> "Kreate CSV"
    PlaylistImportSource.JSON -> "JSON"
    PlaylistImportSource.TEXT -> "TXT"
    PlaylistImportSource.LEVYRA -> "Levyra"
}

private const val SUPPORTED_SERVICES = "YouTube Music · YouTube · Spotify · Apple Music · Deezer · JioSaavn · Bandcamp"
private const val SUPPORTED_FILES = "M3U · M3U8 · PLS · XSPF · CSV · TSV · JSON · TXT · Exportify · TuneMyMusic"
private val PLAYLIST_FILE_TYPES = arrayOf(
    "audio/x-mpegurl", "audio/mpegurl", "application/vnd.apple.mpegurl", "application/x-mpegurl", "audio/x-scpls",
    "application/xspf+xml", "text/csv", "text/comma-separated-values", "text/tab-separated-values", "application/json",
    "text/plain", "application/octet-stream"
)

@Composable
fun PlaylistImportHub(
    state: PlaylistImportUiState,
    controller: PlaylistImportController,
    onPickFile: (Uri) -> Unit
) {
    if (!state.visible) return
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { playlistImportHubCopy(strings.code) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPickFile(uri)
    }
    Dialog(
        onDismissRequest = controller::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BackHandler {
            when {
                state.focusedPosition != null -> controller.closeEntry()
                else -> controller.close()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LevyraInk)
                .background(
                    Brush.verticalGradient(
                        0f to LevyraViolet.copy(alpha = 0.14f),
                        0.45f to Color.Transparent
                    )
                )
        ) {
            AnimatedContent(
                targetState = state.step.screen(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "playlist-import-step"
            ) { screen ->
                when (screen) {
                    ImportScreen.INPUT -> ImportInputScreen(state, copy, controller) {
                        filePicker.launch(PLAYLIST_FILE_TYPES)
                    }
                    ImportScreen.WORKING -> ImportProgressScreen(state, copy, controller)
                    ImportScreen.INCOMPLETE -> ImportIncompleteScreen(state, copy, controller) {
                        filePicker.launch(PLAYLIST_FILE_TYPES)
                    }
                    ImportScreen.REVIEW -> PlaylistImportReviewScreen(state, copy, controller)
                    ImportScreen.SUMMARY -> ImportSummaryScreen(state, copy, controller)
                    ImportScreen.FAILED -> ImportFailureScreen(state, copy, controller) {
                        filePicker.launch(PLAYLIST_FILE_TYPES)
                    }
                }
            }
        }
    }
}

private enum class ImportScreen { INPUT, WORKING, INCOMPLETE, REVIEW, SUMMARY, FAILED }

private fun PlaylistImportStep.screen(): ImportScreen = when (this) {
    PlaylistImportStep.INPUT -> ImportScreen.INPUT
    PlaylistImportStep.READING, PlaylistImportStep.MATCHING -> ImportScreen.WORKING
    PlaylistImportStep.INCOMPLETE -> ImportScreen.INCOMPLETE
    PlaylistImportStep.REVIEW, PlaylistImportStep.SAVING -> ImportScreen.REVIEW
    PlaylistImportStep.SUMMARY -> ImportScreen.SUMMARY
    PlaylistImportStep.FAILED -> ImportScreen.FAILED
}

@Composable
internal fun ImportTopBar(title: String, onBack: () -> Unit, backDescription: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backDescription, tint = LevyraText)
        }
        Text(
            text = title,
            color = LevyraText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun ImportInputScreen(
    state: PlaylistImportUiState,
    copy: PlaylistImportHubCopy,
    controller: PlaylistImportController,
    onChooseFile: () -> Unit
) {
    val context = LocalContext.current
    CenteredColumn {
        ImportTopBar(copy["title"], controller::close, copy["back"])
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = copy["heroTitle"],
                color = LevyraText,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Text(copy["heroSubtitle"], color = LevyraMuted, fontSize = 14.sp, lineHeight = 20.sp)
            OutlinedTextField(
                value = state.input,
                onValueChange = controller::updateInput,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 6,
                shape = ImportShape,
                placeholder = { Text(copy["inputPlaceholder"], color = LevyraMuted.copy(alpha = 0.7f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LevyraText,
                    unfocusedTextColor = LevyraText,
                    focusedBorderColor = LevyraCyan.copy(alpha = 0.7f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = LevyraPanel.copy(alpha = 0.6f),
                    unfocusedContainerColor = LevyraPanel.copy(alpha = 0.45f),
                    cursorColor = LevyraCyan
                )
            )
            DetectionLine(state, copy)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { readClipboard(context)?.let(controller::updateInput) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(copy["paste"])
                }
                OutlinedButton(onClick = onChooseFile, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(copy["chooseFile"])
                }
            }
            PreferenceSelector(state.preference, copy, controller::setPreference)
            Button(
                onClick = controller::start,
                enabled = state.input.isNotBlank() && state.inputIssue == null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent),
                shape = ImportShape
            ) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(copy["start"], fontWeight = FontWeight.SemiBold)
            }
            if (state.resumable.isNotEmpty()) ResumableSessions(state, copy, controller)
            SupportedSources(copy)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetectionLine(state: PlaylistImportUiState, copy: PlaylistImportHubCopy) {
    val strings = LocalLevyraStrings.current
    val issue = state.inputIssue
    val source = state.detectedSource
    val text = when {
        issue != null -> issueMessage(issue, source, copy, strings.code)
        source != null && state.input.isNotBlank() -> copy.format("detected", "source" to source.displayName())
        else -> return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Icon(
            imageVector = if (issue == null) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = if (issue == null) LevyraCyan else LevyraPink,
            modifier = Modifier.size(18.dp)
        )
        Text(text, color = if (issue == null) LevyraText else LevyraMuted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

internal fun issueMessage(kind: PlaylistImportFailureKind, source: PlaylistImportSource?, copy: PlaylistImportHubCopy, code: String): String =
    when (kind) {
        PlaylistImportFailureKind.AUTH_REQUIRED -> copy.format("fAuth", "source" to (source?.displayName() ?: ""))
        PlaylistImportFailureKind.UNSUPPORTED_SOURCE -> copy["fUnsupported"]
        else -> playlistImportFailureMessage(code, kind, null)
    }

@Composable
private fun PreferenceSelector(
    preference: ResolutionPreference,
    copy: PlaylistImportHubCopy,
    onSelect: (ResolutionPreference) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(copy["preferenceTitle"], color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ResolutionPreference.SMART to copy["prefSmart"],
                ResolutionPreference.PREFER_LOCAL to copy["prefLocal"],
                ResolutionPreference.PREFER_ONLINE to copy["prefOnline"]
            ).forEach { (value, label) ->
                FilterChip(
                    selected = preference == value,
                    onClick = { onSelect(value) },
                    label = { Text(label, maxLines = 1) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LevyraCyan.copy(alpha = 0.18f),
                        selectedLabelColor = LevyraText,
                        labelColor = LevyraMuted
                    )
                )
            }
        }
        if (preference == ResolutionPreference.SMART) {
            Text(copy["prefSmartHint"], color = LevyraMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ResumableSessions(state: PlaylistImportUiState, copy: PlaylistImportHubCopy, controller: PlaylistImportController) {
    Surface(color = LevyraPanel.copy(alpha = 0.7f), shape = ImportShape) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
                Text(copy["resumeTitle"], color = LevyraText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            state.resumable.forEach { session ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            session.title.ifBlank { session.source.displayName() },
                            color = LevyraText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp
                        )
                        Text(
                            "${session.source.displayName()} · " +
                                copy.format("resumeProgress", "done" to session.resolved, "total" to session.total),
                            color = LevyraMuted,
                            fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = { controller.discard(session.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(copy["discard"], color = LevyraMuted)
                    }
                    TextButton(onClick = { controller.resume(session.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(copy["resume"], color = LevyraCyan, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportedSources(copy: PlaylistImportHubCopy) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(copy["supportedTitle"], color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(SUPPORTED_SERVICES, color = LevyraText.copy(alpha = 0.86f), fontSize = 13.sp, lineHeight = 19.sp)
        Text(SUPPORTED_FILES, color = LevyraText.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 18.sp)
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Share, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(16.dp).padding(top = 2.dp))
            Text(copy["shareHint"], color = LevyraMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Text(copy["unsupportedNote"], color = LevyraMuted.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun ImportProgressScreen(state: PlaylistImportUiState, copy: PlaylistImportHubCopy, controller: PlaylistImportController) {
    val sourceName = (state.descriptor?.source ?: state.detectedSource)?.displayName().orEmpty()
    val total = state.progressTotal
    val line = when (state.activity) {
        PlaylistImportActivity.READING -> if (total != null) {
            copy.format("fetched", "done" to state.progressDone, "total" to total)
        } else {
            copy.format("fetchedUnknown", "done" to state.progressDone)
        }
        PlaylistImportActivity.MATCHING -> copy.format("matching", "done" to state.progressDone, "total" to (total ?: state.progressDone))
        PlaylistImportActivity.CHECKING -> copy["checking"]
        PlaylistImportActivity.PREPARING -> copy["preparing"]
    }
    val headline = if (state.step == PlaylistImportStep.READING) copy.format("reading", "source" to sourceName) else line
    CenteredColumn {
        ImportTopBar(copy["title"], controller::close, copy["back"])
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            state.descriptor?.let { descriptor ->
                PlaylistImportHero(descriptor, state.entries, state.progressTotal ?: descriptor.declaredTrackCount, copy)
            }
            Text(
                headline,
                color = LevyraText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            val determinate = total != null && total > 0 &&
                (state.activity == PlaylistImportActivity.MATCHING || state.activity == PlaylistImportActivity.READING)
            if (determinate) {
                LinearProgressIndicator(
                    progress = { (state.progressDone.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = LevyraCyan,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp), color = LevyraCyan, trackColor = Color.White.copy(alpha = 0.08f))
            }
            if (state.step == PlaylistImportStep.READING) Text(line, color = LevyraMuted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = controller::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(copy["cancel"]) }
                TextButton(onClick = controller::close, modifier = Modifier.heightIn(min = 48.dp)) { Text(copy["hide"], color = LevyraCyan) }
            }
        }
    }
}

@Composable
private fun ImportIncompleteScreen(
    state: PlaylistImportUiState,
    copy: PlaylistImportHubCopy,
    controller: PlaylistImportController,
    onChooseFile: () -> Unit
) {
    val incomplete = state.completeness as? PlaylistImportCompleteness.Incomplete
    val sourceName = state.descriptor?.source?.displayName().orEmpty()
    val read = incomplete?.retrieved ?: state.entries.size
    val body = if (incomplete?.declared != null) {
        copy.format("incompleteBody", "read" to read, "total" to incomplete.declared!!, "source" to sourceName)
    } else {
        copy.format("incompleteBodyUnknown", "read" to read, "source" to sourceName)
    }
    CenteredColumn {
        ImportTopBar(copy["title"], controller::close, copy["back"])
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.descriptor?.let { PlaylistImportHero(it, emptyList(), incomplete?.declared, copy) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = LevyraPink, modifier = Modifier.size(22.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(copy["incompleteTitle"], color = LevyraText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
                    Text(body, color = LevyraText.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 20.sp)
                    Text(copy["incompleteHint"], color = LevyraMuted, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
            Button(
                onClick = onChooseFile,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent),
                shape = ImportShape
            ) { Text(copy["chooseFile"], fontWeight = FontWeight.SemiBold) }
            OutlinedButton(onClick = controller::continueIncomplete, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = ImportShape) {
                Text(copy.format("importPartial", "read" to read))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = controller::retry, modifier = Modifier.heightIn(min = 48.dp)) { Text(copy["retry"], color = LevyraCyan) }
                TextButton(onClick = controller::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(copy["cancel"], color = LevyraMuted) }
            }
        }
    }
}

@Composable
private fun ImportFailureScreen(
    state: PlaylistImportUiState,
    copy: PlaylistImportHubCopy,
    controller: PlaylistImportController,
    onChooseFile: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val kind = state.failure ?: PlaylistImportFailureKind.INVALID_INPUT
    val offerFile = kind == PlaylistImportFailureKind.AUTH_REQUIRED || kind == PlaylistImportFailureKind.UNSUPPORTED_SOURCE ||
        kind == PlaylistImportFailureKind.PROVIDER_CHANGED || kind == PlaylistImportFailureKind.NOT_AVAILABLE
    val canRetry = kind == PlaylistImportFailureKind.NETWORK || kind == PlaylistImportFailureKind.RATE_LIMITED ||
        kind == PlaylistImportFailureKind.PROVIDER_CHANGED || kind == PlaylistImportFailureKind.NOT_AVAILABLE
    CenteredColumn {
        ImportTopBar(copy["title"], controller::finish, copy["back"])
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = LevyraPink, modifier = Modifier.size(40.dp))
            Text(copy["failureTitle"], color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
            Text(
                issueMessage(kind, state.failureSource, copy, strings.code),
                color = LevyraText.copy(alpha = 0.88f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            if (canRetry) {
                Button(
                    onClick = controller::retry,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent),
                    shape = ImportShape
                ) { Text(copy["retry"], fontWeight = FontWeight.SemiBold) }
            }
            if (offerFile) {
                OutlinedButton(onClick = onChooseFile, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = ImportShape) {
                    Text(copy["chooseFile"])
                }
            }
            TextButton(onClick = controller::finish, modifier = Modifier.heightIn(min = 48.dp)) { Text(copy["back"], color = LevyraMuted) }
        }
    }
}

@Composable
private fun ImportSummaryScreen(state: PlaylistImportUiState, copy: PlaylistImportHubCopy, controller: PlaylistImportController) {
    val summary = state.summary ?: return
    val context = LocalContext.current
    CenteredColumn {
        ImportTopBar(copy["title"], controller::finish, copy["back"])
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            state.descriptor?.let { PlaylistImportHero(it, state.entries, summary.imported, copy) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(26.dp))
                Text(
                    if (summary.appended) copy["summaryUpdated"] else copy["summaryTitle"],
                    color = LevyraText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )
            }
            Text(summary.playlistName, color = LevyraMuted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Surface(color = LevyraPanel.copy(alpha = 0.7f), shape = ImportShape) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryLine(copy.format("summaryImported", "count" to summary.imported), LevyraText)
                    if (summary.mergedRepeats > 0) SummaryLine(copy.format("mergedNote", "count" to summary.mergedRepeats), LevyraMuted)
                    if (summary.skipped > 0) SummaryLine(copy.format("summarySkipped", "count" to summary.skipped), LevyraMuted)
                    if (summary.unresolved > 0) SummaryLine(copy.format("summaryUnresolved", "count" to summary.unresolved), LevyraMuted)
                    if (summary.needsReview > 0) SummaryLine(copy.format("summaryReview", "count" to summary.needsReview), LevyraMuted)
                    if (summary.manual > 0) SummaryLine(copy.format("summaryManual", "count" to summary.manual), LevyraMuted)
                }
            }
            Button(
                onClick = controller::viewPlaylist,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent),
                shape = ImportShape
            ) { Text(copy["viewPlaylist"], fontWeight = FontWeight.SemiBold) }
            val unresolved = summary.unresolved + summary.needsReview + summary.skipped
            if (unresolved > 0) {
                OutlinedButton(onClick = controller::fixMissing, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = ImportShape) {
                    Text(copy["fixMissing"])
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = controller::retryUnresolved, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(copy["retryUnresolved"], color = LevyraCyan)
                    }
                    TextButton(
                        onClick = { copyToClipboard(context, summary.playlistName, controller.unresolvedReport()) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(copy["copyReport"], color = LevyraCyan) }
                }
            }
            TextButton(onClick = controller::importAnother, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(copy["importAnother"], color = LevyraCyan)
            }
            TextButton(onClick = controller::finish, modifier = Modifier.heightIn(min = 48.dp)) { Text(copy["done"], color = LevyraMuted) }
        }
    }
}

@Composable
private fun SummaryLine(text: String, color: Color) {
    Text(text, color = color, fontSize = 14.sp, lineHeight = 20.sp)
}

private fun readClipboard(context: Context): String? {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return null
    return clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    if (text.isBlank()) return
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
}

@Composable
internal fun ArtworkBox(url: String, modifier: Modifier) {
    Box(modifier = modifier.background(LevyraPanel)) {
        if (url.isNotBlank()) {
            coil3.compose.AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun MosaicArtwork(urls: List<String>, modifier: Modifier) {
    val tiles = urls.filter(String::isNotBlank).distinct().take(4)
    if (tiles.size < 4) {
        ArtworkBox(tiles.firstOrNull().orEmpty(), modifier)
        return
    }
    Column(modifier = modifier) {
        Row(Modifier.weight(1f)) {
            ArtworkBox(tiles[0], Modifier.weight(1f).fillMaxSize())
            ArtworkBox(tiles[1], Modifier.weight(1f).fillMaxSize())
        }
        Row(Modifier.weight(1f)) {
            ArtworkBox(tiles[2], Modifier.weight(1f).fillMaxSize())
            ArtworkBox(tiles[3], Modifier.weight(1f).fillMaxSize())
        }
    }
}

@Composable
internal fun PlaylistImportHero(
    descriptor: com.luc4n3x.levyra.nexus.playlistimport.ImportedPlaylistDescriptor,
    entries: List<com.luc4n3x.levyra.nexus.playlistimport.ImportEntry>,
    trackCount: Int?,
    copy: PlaylistImportHubCopy,
    trailing: @Composable () -> Unit = {}
) {
    val artwork = remember(descriptor.artworkUrl, entries.size) {
        if (descriptor.artworkUrl.isNotBlank()) listOf(descriptor.artworkUrl)
        else entries.asSequence()
            .mapNotNull { entry -> entry.selected?.candidate?.artworkUrl?.takeIf(String::isNotBlank) ?: entry.identity.artworkUrl.takeIf(String::isNotBlank) }
            .distinct()
            .take(4)
            .toList()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        MosaicArtwork(
            artwork,
            Modifier
                .size(112.dp)
                .aspectRatio(1f)
                .clip(ImportShape)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                descriptor.source.displayName().uppercase(),
                color = LevyraCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Text(
                descriptor.title.ifBlank { copy["title"] },
                color = LevyraText,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = listOfNotNull(
                descriptor.owner.takeIf(String::isNotBlank),
                trackCount?.takeIf { it > 0 }?.let { "$it ♪" }
            ).joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, color = LevyraMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            trailing()
        }
    }
}
