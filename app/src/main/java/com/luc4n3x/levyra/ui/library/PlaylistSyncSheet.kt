package com.luc4n3x.levyra.ui.library

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.local.PlaylistSyncEntity
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncError
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncSummary
import com.luc4n3x.levyra.data.youtubemusic.youtubeMusicPlaylistUrl
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.PlaylistSyncCopy
import com.luc4n3x.levyra.ui.i18n.PlaylistSyncText
import com.luc4n3x.levyra.ui.i18n.playlistSyncCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.PlaylistSyncController
import com.luc4n3x.levyra.viewmodel.PlaylistSyncSheetState
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistSyncSheet(
    controller: PlaylistSyncController,
    state: PlaylistSyncSheetState,
    link: PlaylistSyncEntity?,
    syncing: Boolean,
    onSignIn: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.playlistSyncCopy
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    ModalBottomSheet(onDismissRequest = controller::dismiss, sheetState = sheetState, containerColor = LevyraPanel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(copy[PlaylistSyncText.Title], color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
            when {
                link != null -> LinkedContent(copy, strings.code, state, link, syncing, controller, onSignIn)
                !state.accountLoaded -> LevyraLoadingIndicator(color = LevyraCyan, modifier = Modifier.align(Alignment.CenterHorizontally).size(36.dp))
                state.account == null -> SignedOutContent(copy, onSignIn)
                else -> LinkChoices(copy, state, controller)
            }
        }
    }
}

@Composable
internal fun PlaylistSyncStatusChip(
    link: PlaylistSyncEntity,
    syncing: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.playlistSyncCopy
    val failed = !syncing && link.lastError.isNotBlank()
    val detail = when {
        syncing -> copy[PlaylistSyncText.Syncing]
        failed -> null
        link.lastSyncedAt > 0L -> copy.format(PlaylistSyncText.LastSynced, time = syncTime(link.lastSyncedAt, strings.code))
        else -> copy[PlaylistSyncText.NotSynced]
    }
    Row(modifier = modifier.fillMaxWidth()) {
        Surface(color = contentColor.copy(alpha = 0.10f), shape = CircleShape, onClick = onClick) {
            Row(
                modifier = Modifier.heightIn(min = 36.dp).padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (syncing) {
                    LevyraLoadingIndicator(color = contentColor, modifier = Modifier.size(18.dp))
                } else {
                    Icon(
                        if (failed) Icons.Rounded.ErrorOutline else Icons.Rounded.CloudSync,
                        contentDescription = null,
                        tint = if (failed) LevyraOrange else contentColor.copy(alpha = 0.88f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = listOfNotNull(copy[PlaylistSyncText.Linked], detail).joinToString(" · "),
                    color = contentColor.copy(alpha = 0.88f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.LinkedContent(
    copy: PlaylistSyncCopy,
    languageCode: String,
    state: PlaylistSyncSheetState,
    link: PlaylistSyncEntity,
    syncing: Boolean,
    controller: PlaylistSyncController,
    onSignIn: () -> Unit
) {
    val context = LocalContext.current
    val busy = syncing || state.busy
    val error = link.lastError.takeIf(String::isNotBlank)?.let { stored -> PlaylistSyncError.entries.firstOrNull { it.name == stored } }
    StatusCard(
        icon = Icons.Rounded.CloudSync,
        title = copy[PlaylistSyncText.Linked],
        subtitle = when {
            busy -> copy[PlaylistSyncText.Syncing]
            link.lastSyncedAt > 0L -> copy.format(PlaylistSyncText.LastSynced, time = syncTime(link.lastSyncedAt, languageCode))
            else -> copy[PlaylistSyncText.NotSynced]
        },
        busy = busy
    )
    if (!busy) state.summary?.let { SummaryLine(copy, it) }
    if (error != null && !busy) {
        ErrorLine(copy, error, if (error == PlaylistSyncError.SIGN_IN) onSignIn else controller::syncNow)
    }
    if (link.unmatched > 0) Note(copy.format(PlaylistSyncText.Unmatched, count = link.unmatched))
    if (link.keptLocal > 0) Note(copy.format(PlaylistSyncText.KeptAlbums, count = link.keptLocal))
    AutoSyncRow(copy, link.autoSync, controller::setAutoSync)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        OutlinedButton(onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(youtubeMusicPlaylistUrl(link.remotePlaylistId))))
            } catch (_: ActivityNotFoundException) {
            }
        }) { Text(copy[PlaylistSyncText.Open], color = LevyraText) }
        Button(
            onClick = controller::syncNow,
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent)
        ) {
            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(copy[PlaylistSyncText.SyncNow], fontWeight = FontWeight.Bold)
        }
    }
    Column(modifier = Modifier.align(Alignment.End), horizontalAlignment = Alignment.End) {
        TextButton(onClick = controller::unlink, enabled = !busy) { Text(copy[PlaylistSyncText.Unlink], color = LevyraText) }
        Text(copy[PlaylistSyncText.UnlinkBody], color = LevyraMuted, fontSize = 12.sp, modifier = Modifier.padding(end = 12.dp))
    }
}

@Composable
private fun SignedOutContent(copy: PlaylistSyncCopy, onSignIn: () -> Unit) {
    StatusCard(Icons.Rounded.AccountCircle, copy[PlaylistSyncText.SignedOutTitle], copy[PlaylistSyncText.SignedOutBody], busy = false)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(onClick = onSignIn, colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent)) {
            Text(copy[PlaylistSyncText.SignIn], fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ColumnScope.LinkChoices(copy: PlaylistSyncCopy, state: PlaylistSyncSheetState, controller: PlaylistSyncController) {
    var autoSync by rememberSaveable(state.playlistId) { mutableStateOf(true) }
    Text(copy[PlaylistSyncText.Intro], color = LevyraMuted, fontSize = 14.sp)
    if (state.ready > 0) Note(copy.format(PlaylistSyncText.Ready, count = state.ready))
    if (state.unmatched > 0) Note(copy.format(PlaylistSyncText.Unmatched, count = state.unmatched))
    if (state.busy) {
        Text(
            copy[PlaylistSyncText.Syncing],
            color = LevyraText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        LevyraLoadingIndicator(color = LevyraCyan, modifier = Modifier.align(Alignment.CenterHorizontally).size(36.dp))
        return
    }
    state.error?.let { ErrorLine(copy, it, controller::retryRemote) }
    AutoSyncRow(copy, autoSync) { autoSync = it }
    val remoteCount = state.remote.size
    ChoiceRow(
        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
        title = copy[PlaylistSyncText.CreateNew],
        subtitle = copy.format(PlaylistSyncText.CreateNewBody, name = state.account.orEmpty()),
        index = 0,
        count = 1
    ) { controller.createAndLink(autoSync) }
    SectionLabel(copy[PlaylistSyncText.LinkExisting])
    when {
        state.remoteLoading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LevyraLoadingIndicator(color = LevyraCyan, modifier = Modifier.size(24.dp))
            Text(copy[PlaylistSyncText.Loading], color = LevyraMuted, fontSize = 14.sp)
        }
        state.remoteError != null -> ErrorLine(copy, state.remoteError, controller::retryRemote)
        remoteCount == 0 -> Note(copy[PlaylistSyncText.NoPlaylists])
        else -> state.remote.forEachIndexed { index, entry ->
            ChoiceRow(Icons.Rounded.LibraryMusic, entry.title, entry.subtitle, index, remoteCount) { controller.linkExisting(entry, autoSync) }
        }
    }
}

@Composable
private fun StatusCard(icon: ImageVector, title: String, subtitle: String, busy: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(levyraGroupedListShape(0, 1))
            .background(LevyraCyan.copy(alpha = 0.10f))
            .padding(16.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (busy) {
            LevyraLoadingIndicator(color = LevyraCyan, modifier = Modifier.size(40.dp))
        } else {
            Icon(
                icon,
                contentDescription = null,
                tint = LevyraCyan,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(LevyraCyan.copy(alpha = 0.16f)).padding(9.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LevyraText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.size(2.dp))
            Text(subtitle, color = LevyraMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SummaryLine(copy: PlaylistSyncCopy, summary: PlaylistSyncSummary) {
    val changes = listOfNotNull(
        "Levyra +${summary.addedLocally} −${summary.removedLocally}".takeIf { summary.addedLocally + summary.removedLocally > 0 },
        "YouTube Music +${summary.addedRemotely} −${summary.removedRemotely}".takeIf { summary.addedRemotely + summary.removedRemotely > 0 }
    )
    val headline = if (changes.isEmpty() && !summary.renamed) copy[PlaylistSyncText.UpToDate] else copy[PlaylistSyncText.Complete]
    Column(modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Text(headline, color = LevyraText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        if (changes.isNotEmpty()) Text(changes.joinToString("  ·  "), color = LevyraMuted, fontSize = 13.sp)
    }
    if (summary.waiting > 0) Note(copy[PlaylistSyncText.Waiting])
    if (summary.rejected > 0) Note(copy.format(PlaylistSyncText.Rejected, count = summary.rejected))
}

@Composable
private fun ErrorLine(copy: PlaylistSyncCopy, error: PlaylistSyncError, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(levyraGroupedListShape(0, 1))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
        Text(playlistSyncErrorText(copy, error), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (error != PlaylistSyncError.ACCOUNT && error != PlaylistSyncError.MISSING && error != PlaylistSyncError.ALREADY_LINKED) {
            TextButton(onClick = onAction) {
                Text(
                    if (error == PlaylistSyncError.SIGN_IN) copy[PlaylistSyncText.SignIn] else copy[PlaylistSyncText.Retry],
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AutoSyncRow(copy: PlaylistSyncCopy, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(levyraGroupedListShape(0, 1))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(copy[PlaylistSyncText.AutoSync], color = LevyraText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(copy[PlaylistSyncText.AutoSyncBody], color = LevyraMuted, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = LevyraCyan, checkedThumbColor = LevyraOnAccent)
        )
    }
}

@Composable
private fun ChoiceRow(icon: ImageVector, title: String, subtitle: String, index: Int, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(levyraGroupedListShape(index, count))
            .background(LevyraText.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = LevyraCyan,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(LevyraCyan.copy(alpha = 0.14f)).padding(9.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LevyraText, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.size(2.dp))
                Text(subtitle, color = LevyraMuted, fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun Note(text: String) {
    Text(text, color = LevyraMuted, fontSize = 13.sp)
}

internal fun playlistSyncErrorText(copy: PlaylistSyncCopy, error: PlaylistSyncError): String = when (error) {
    PlaylistSyncError.SIGN_IN -> copy[PlaylistSyncText.ErrorSignIn]
    PlaylistSyncError.ACCOUNT -> copy[PlaylistSyncText.ErrorAccount]
    PlaylistSyncError.MISSING -> copy[PlaylistSyncText.ErrorMissing]
    PlaylistSyncError.ALREADY_LINKED -> copy[PlaylistSyncText.ErrorAlreadyLinked]
    PlaylistSyncError.NETWORK -> copy[PlaylistSyncText.ErrorNetwork]
    PlaylistSyncError.SERVICE -> copy[PlaylistSyncText.ErrorService]
}

private fun syncTime(timestamp: Long, languageCode: String): String {
    val locale = Locale.forLanguageTag(languageCode.ifBlank { "en" })
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = Calendar.getInstance()
    val sameDay = then.get(Calendar.YEAR) == now.get(Calendar.YEAR) && then.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    val format = if (sameDay) DateFormat.getTimeInstance(DateFormat.SHORT, locale) else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
    return format.format(Date(timestamp))
}
