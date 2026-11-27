package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.desktop.core.account.DesktopOwnedPlaylist
import com.luc4n3x.levyra.desktop.core.account.DesktopPlaylistSync
import com.luc4n3x.levyra.desktop.core.account.DesktopPlaylistSyncError
import com.luc4n3x.levyra.desktop.core.account.DesktopPlaylistSyncOutcome
import com.luc4n3x.levyra.desktop.core.account.DesktopPlaylistSyncSummary
import com.luc4n3x.levyra.desktop.core.storage.LocalPlaylist
import com.luc4n3x.levyra.desktop.core.storage.PlaylistSyncLink
import com.luc4n3x.levyra.ui.i18n.PlaylistSyncCopy
import com.luc4n3x.levyra.ui.i18n.PlaylistSyncText
import com.luc4n3x.levyra.ui.i18n.playlistSyncCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun PlaylistSyncDialog(
    sync: DesktopPlaylistSync,
    playlist: LocalPlaylist,
    link: PlaylistSyncLink?,
    syncing: Boolean,
    accountName: String?,
    languageCode: String,
    onSignIn: () -> Unit,
    onDismiss: () -> Unit
) {
    val copy = remember(languageCode) { SharedStrings.forCode(languageCode).playlistSyncCopy }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<DesktopPlaylistSyncError?>(null) }
    var summary by remember { mutableStateOf<DesktopPlaylistSyncSummary?>(null) }
    var owned by remember { mutableStateOf<List<DesktopOwnedPlaylist>?>(null) }
    var ownedError by remember { mutableStateOf<DesktopPlaylistSyncError?>(null) }
    var autoSync by remember { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }
    val run: (suspend () -> DesktopPlaylistSyncOutcome) -> Unit = { block ->
        if (!busy) {
            busy = true
            error = null
            summary = null
            scope.launch {
                when (val outcome = block()) {
                    is DesktopPlaylistSyncOutcome.Synced -> summary = outcome.summary
                    is DesktopPlaylistSyncOutcome.Failed -> error = outcome.error
                }
                busy = false
            }
        }
    }
    LaunchedEffect(link == null, accountName, reload) {
        if (link != null || accountName == null) return@LaunchedEffect
        owned = null
        ownedError = null
        try {
            owned = sync.ownedPlaylists()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            ownedError = sync.errorOf(failure)
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 6.dp, shadowElevation = 24.dp) {
            Column(
                Modifier.width(520.dp).heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(copy[PlaylistSyncText.Title], style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                when {
                    link != null -> LinkedPanel(copy, languageCode, link, sync.remoteUrl(link), busy || syncing, error, summary, onSignIn,
                        onSync = { run { sync.sync(playlist.id) } },
                        onAutoSync = { sync.setAutoSync(playlist.id, it) },
                        onUnlink = { scope.launch { sync.unlink(playlist.id) } })
                    accountName == null -> {
                        Text(copy[PlaylistSyncText.SignedOutTitle], style = MaterialTheme.typography.titleMedium)
                        Text(copy[PlaylistSyncText.SignedOutBody], color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = onSignIn) { Text(copy[PlaylistSyncText.SignIn]) }
                        }
                    }
                    busy -> BusyRow(copy[PlaylistSyncText.Syncing])
                    else -> {
                        Text(copy[PlaylistSyncText.Intro], color = MaterialTheme.colorScheme.onSurfaceVariant)
                        error?.let { ErrorRow(copy, it) { reload++ } }
                        AutoSyncToggle(copy, autoSync) { autoSync = it }
                        ChoiceRow(copy[PlaylistSyncText.CreateNew], copy.format(PlaylistSyncText.CreateNewBody, name = accountName)) {
                            run { sync.createAndLink(playlist.id, autoSync) }
                        }
                        Text(copy[PlaylistSyncText.LinkExisting], style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val list = owned
                        when {
                            ownedError != null -> ErrorRow(copy, ownedError!!) { reload++ }
                            list == null -> BusyRow(copy[PlaylistSyncText.Loading])
                            list.isEmpty() -> Text(copy[PlaylistSyncText.NoPlaylists], color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> list.forEach { remote ->
                                ChoiceRow(remote.title, remote.subtitle) { run { sync.linkExisting(playlist.id, remote, autoSync) } }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(SharedStrings.forCode(languageCode).close) }
                }
            }
        }
    }
}

@Composable
private fun LinkedPanel(
    copy: PlaylistSyncCopy,
    languageCode: String,
    link: PlaylistSyncLink,
    remoteUrl: String,
    busy: Boolean,
    error: DesktopPlaylistSyncError?,
    summary: DesktopPlaylistSyncSummary?,
    onSignIn: () -> Unit,
    onSync: () -> Unit,
    onAutoSync: (Boolean) -> Unit,
    onUnlink: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val shown = link.lastError.takeIf(String::isNotBlank)?.let { stored -> DesktopPlaylistSyncError.entries.firstOrNull { it.name == stored } }
        ?: error?.takeIf { it == DesktopPlaylistSyncError.ALREADY_LINKED }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(copy[PlaylistSyncText.Linked], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            when {
                busy -> copy[PlaylistSyncText.Syncing]
                link.lastSyncedAt > 0L -> copy.format(PlaylistSyncText.LastSynced, time = desktopSyncTime(link.lastSyncedAt, languageCode))
                else -> copy[PlaylistSyncText.NotSynced]
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (!busy && summary != null) {
        val changes = listOfNotNull(
            "Levyra +${summary.addedLocally} −${summary.removedLocally}".takeIf { summary.addedLocally + summary.removedLocally > 0 },
            "YouTube Music +${summary.addedRemotely} −${summary.removedRemotely}".takeIf { summary.addedRemotely + summary.removedRemotely > 0 }
        )
        Text(if (changes.isEmpty() && !summary.renamed) copy[PlaylistSyncText.UpToDate] else copy[PlaylistSyncText.Complete], fontWeight = FontWeight.SemiBold)
        if (changes.isNotEmpty()) Text(changes.joinToString("  ·  "), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (summary.waiting > 0) Text(copy[PlaylistSyncText.Waiting], color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (summary.rejected > 0) Text(copy.format(PlaylistSyncText.Rejected, count = summary.rejected), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (!busy && shown != null) ErrorRow(copy, shown, if (shown == DesktopPlaylistSyncError.SIGN_IN) onSignIn else onSync)
    if (link.unmatched > 0) Text(copy.format(PlaylistSyncText.Unmatched, count = link.unmatched), color = MaterialTheme.colorScheme.onSurfaceVariant)
    AutoSyncToggle(copy, link.autoSync, onAutoSync)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onUnlink, enabled = !busy) { Text(copy[PlaylistSyncText.Unlink], color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = { uriHandler.openUri(remoteUrl) }) { Text(copy[PlaylistSyncText.Open]) }
        Button(onClick = onSync, enabled = !busy) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
            }
            Text(copy[PlaylistSyncText.SyncNow])
        }
    }
    Text(copy[PlaylistSyncText.UnlinkBody], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun AutoSyncToggle(copy: PlaylistSyncCopy, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(copy[PlaylistSyncText.AutoSync], style = MaterialTheme.typography.titleSmall)
            Text(copy[PlaylistSyncText.AutoSyncBody], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ChoiceRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BusyRow(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorRow(copy: PlaylistSyncCopy, error: DesktopPlaylistSyncError, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(desktopSyncErrorText(copy, error), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
        if (error != DesktopPlaylistSyncError.ACCOUNT && error != DesktopPlaylistSyncError.MISSING && error != DesktopPlaylistSyncError.ALREADY_LINKED) {
            TextButton(onClick = onAction) {
                Text(
                    if (error == DesktopPlaylistSyncError.SIGN_IN) copy[PlaylistSyncText.SignIn] else copy[PlaylistSyncText.Retry],
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

internal fun desktopSyncErrorText(copy: PlaylistSyncCopy, error: DesktopPlaylistSyncError): String = when (error) {
    DesktopPlaylistSyncError.SIGN_IN -> copy[PlaylistSyncText.ErrorSignIn]
    DesktopPlaylistSyncError.ACCOUNT -> copy[PlaylistSyncText.ErrorAccount]
    DesktopPlaylistSyncError.MISSING -> copy[PlaylistSyncText.ErrorMissing]
    DesktopPlaylistSyncError.ALREADY_LINKED -> copy[PlaylistSyncText.ErrorAlreadyLinked]
    DesktopPlaylistSyncError.NETWORK -> copy[PlaylistSyncText.ErrorNetwork]
    DesktopPlaylistSyncError.SERVICE -> copy[PlaylistSyncText.ErrorService]
}

internal fun desktopSyncTime(timestamp: Long, languageCode: String): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.forLanguageTag(languageCode.ifBlank { "en" })).format(Date(timestamp))
