package com.luc4n3x.levyra.ui.library

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.playlistexport.PlaylistFileFormat
import com.luc4n3x.levyra.data.playlistexport.playlistFileName
import com.luc4n3x.levyra.ui.components.LevyraWavyProgress
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.PlaylistExportCopy
import com.luc4n3x.levyra.ui.i18n.playlistExportCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.PlaylistExportController
import com.luc4n3x.levyra.viewmodel.PlaylistExportError
import com.luc4n3x.levyra.viewmodel.PlaylistExportPhase
import com.luc4n3x.levyra.viewmodel.PlaylistExportUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistExportSheet(controller: PlaylistExportController, state: PlaylistExportUiState) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { playlistExportCopy(strings.code) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val m3uLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PlaylistFileFormat.M3U8.mimeType)) { uri ->
        if (uri != null) controller.saveFile(uri, PlaylistFileFormat.M3U8)
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PlaylistFileFormat.CSV.mimeType)) { uri ->
        if (uri != null) controller.saveFile(uri, PlaylistFileFormat.CSV)
    }
    val saveAs: (PlaylistFileFormat) -> Unit = { format ->
        val name = playlistFileName(state.title, format)
        try {
            if (format == PlaylistFileFormat.M3U8) m3uLauncher.launch(name) else csvLauncher.launch(name)
        } catch (_: ActivityNotFoundException) {
        }
    }
    ModalBottomSheet(
        onDismissRequest = {
            if (state.phase == PlaylistExportPhase.SENDING) scope.launch { sheetState.show() } else controller.dismiss()
        },
        sheetState = sheetState,
        containerColor = LevyraPanel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(copy.title, color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                if (state.tracks.isEmpty()) copy.emptyPlaylist else copy.summary(state.tracks.size, state.total),
                color = LevyraMuted,
                fontSize = 14.sp
            )
            when (state.phase) {
                PlaylistExportPhase.CHOOSE -> ExportChoices(copy, state, controller::exportToYoutubeMusic, saveAs)
                PlaylistExportPhase.SENDING -> {
                    Text(
                        copy.sending(state.added, state.total),
                        color = LevyraText,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    LevyraWavyProgress(
                        progress = if (state.total == 0) 0f else state.added.toFloat() / state.total,
                        color = LevyraCyan,
                        trackColor = LevyraText.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(onClick = controller::cancel, modifier = Modifier.align(Alignment.End)) {
                        Text(copy.cancel, color = LevyraText)
                    }
                }
                else -> ExportOutcome(
                    copy = copy,
                    state = state,
                    onOpen = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.remoteUrl)))
                        } catch (_: ActivityNotFoundException) {
                        }
                    },
                    onRetry = {
                        if (state.phase == PlaylistExportPhase.FILE_FAILED) state.savedFormat?.let(saveAs) else controller.back()
                    },
                    onDone = { scope.launch { sheetState.hide() }.invokeOnCompletion { controller.dismiss() } }
                )
            }
        }
    }
}

@Composable
private fun ExportChoices(
    copy: PlaylistExportCopy,
    state: PlaylistExportUiState,
    onYoutubeMusic: () -> Unit,
    onFile: (PlaylistFileFormat) -> Unit
) {
    val account = state.youtubeAccount
    val youtubeReady = account != null && state.total > 0
    SectionLabel(copy.services)
    ExportRow(
        icon = Icons.Rounded.LibraryMusic,
        title = copy.youtubeMusic,
        subtitle = when {
            account == null -> copy.youtubeSignedOut
            state.total == 0 -> copy.youtubeNothing
            else -> copy.youtubeConnected(account)
        },
        enabled = youtubeReady,
        shapeIndex = 0,
        shapeCount = 1,
        onClick = onYoutubeMusic
    )
    if (youtubeReady && state.plan.unmatched.isNotEmpty()) Note(copy.unmatched(state.plan.unmatched.size))
    if (youtubeReady && state.plan.duplicates > 0) Note(copy.duplicates(state.plan.duplicates))
    SectionLabel(copy.files)
    val hasTracks = state.tracks.isNotEmpty()
    ExportRow(Icons.AutoMirrored.Rounded.QueueMusic, copy.m3uLabel, copy.m3uDescription, hasTracks, 0, 2) { onFile(PlaylistFileFormat.M3U8) }
    ExportRow(Icons.Rounded.TableChart, copy.csvLabel, copy.csvDescription, hasTracks, 1, 2) { onFile(PlaylistFileFormat.CSV) }
}

@Composable
private fun ExportOutcome(
    copy: PlaylistExportCopy,
    state: PlaylistExportUiState,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit
) {
    val errorText = when (state.error) {
        PlaylistExportError.NETWORK -> copy.networkError
        PlaylistExportError.SIGN_IN -> copy.signInError
        PlaylistExportError.SERVICE -> copy.serviceError
        null -> ""
    }
    val message = when (state.phase) {
        PlaylistExportPhase.SENT -> copy.sent(state.added)
        PlaylistExportPhase.PARTIAL -> copy.partial(state.added, state.total) + "\n" + errorText
        PlaylistExportPhase.CANCELLED -> copy.cancelled(state.added)
        PlaylistExportPhase.FAILED -> errorText
        PlaylistExportPhase.FILE_SAVED -> copy.fileSaved
        else -> copy.fileFailed
    }
    val success = state.phase == PlaylistExportPhase.SENT || state.phase == PlaylistExportPhase.FILE_SAVED
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            if (success) Icons.Rounded.LibraryMusic else Icons.Rounded.Description,
            contentDescription = null,
            tint = if (success) LevyraCyan else LevyraOrange,
            modifier = Modifier.size(28.dp)
        )
        Text(
            message,
            color = LevyraText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        if (!success) TextButton(onClick = onRetry) { Text(copy.retry, color = LevyraCyan) }
        if (state.remoteUrl.isNotBlank()) {
            OutlinedButton(onClick = onOpen) { Text(copy.open, color = LevyraText) }
        }
        Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent)) {
            Text(copy.done, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun Note(text: String) {
    Text(text, color = LevyraMuted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun ExportRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    shapeIndex: Int,
    shapeCount: Int,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(levyraGroupedListShape(shapeIndex, shapeCount))
            .background(LevyraText.copy(alpha = 0.06f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = LevyraCyan.copy(alpha = alpha),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LevyraCyan.copy(alpha = 0.14f * alpha))
                .padding(9.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LevyraText.copy(alpha = alpha), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.size(2.dp))
            Text(subtitle, color = LevyraMuted.copy(alpha = alpha), fontSize = 13.sp)
        }
    }
}
