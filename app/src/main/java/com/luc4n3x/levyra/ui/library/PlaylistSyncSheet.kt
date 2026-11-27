package com.luc4n3x.levyra.ui.library

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.local.PlaylistSyncEntity
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncErrorKind
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLibraryEntry
import com.luc4n3x.levyra.data.youtubemusic.youtubeMusicPlaylistUrl
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.PlaylistSyncCopy
import com.luc4n3x.levyra.ui.i18n.playlistSyncCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.PlaylistSyncController
import com.luc4n3x.levyra.viewmodel.PlaylistSyncSheetState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistSyncSheet(
    controller: PlaylistSyncController,
    state: PlaylistSyncSheetState,
    onSignIn: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { playlistSyncCopy(strings.code) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            if (state.syncing) scope.launch { sheetState.show() } else controller.dismiss()
        },
        sheetState = sheetState,
        containerColor = LevyraPanel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(copy.sheetTitle, color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                if (state.trackCount == 0) {
                    "${state.playlistName} · ${copy.emptySubtitle}"
                } else {
                    "${state.playlistName} · ${copy.subtitle(state.trackCount, state.readyCount)}"
                },
                color = LevyraMuted,
                fontSize = 14.sp
            )

            when {
                state.signedOut -> SignedOutSyncContent(
                    copy = copy,
                    onSignIn = {
                        controller.dismiss()
                        onSignIn()
                    }
                )
                state.linked != null -> LinkedSyncContent(
                    copy = copy,
                    state = state,
                    linked = state.linked,
                    controller = controller,
                    onSignIn = {
                        controller.dismiss()
                        onSignIn()
                    }
                )
                else -> UnlinkedSyncContent(
                    copy = copy,
                    state = state,
                    controller = controller,
                    onSignIn = {
                        controller.dismiss()
                        onSignIn()
                    }
                )
            }
        }
    }
}

@Composable
private fun SignedOutSyncContent(
    copy: PlaylistSyncCopy,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LevyraText.copy(alpha = 0.06f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(copy.signedOutTitle, color = LevyraText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(copy.signedOutMessage, color = LevyraMuted, fontSize = 13.sp)
        Button(
            onClick = onSignIn,
            colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(copy.signInButton, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LinkedSyncContent(
    copy: PlaylistSyncCopy,
    state: PlaylistSyncSheetState,
    linked: PlaylistSyncEntity,
    controller: PlaylistSyncController,
    onSignIn: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LevyraText.copy(alpha = 0.06f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(copy.linkedHeader, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(
            linked.remoteTitle.ifBlank { state.playlistName },
            color = LevyraText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        state.accountLabel?.takeIf(String::isNotBlank)?.let { label ->
            Text(copy.accountConnected(label), color = LevyraMuted, fontSize = 13.sp)
        }
        Text(copy.lastSyncedLabel(linked.lastSyncedAt), color = LevyraMuted, fontSize = 12.sp)
    }

    if (state.syncing) {
        SyncProgressRow(copy.syncing)
    } else if (state.error != null) {
        SyncErrorBanner(
            copy = copy,
            error = state.error,
            onAction = {
                when (state.error) {
                    PlaylistSyncErrorKind.SIGN_IN,
                    PlaylistSyncErrorKind.ACCOUNT_MISMATCH -> onSignIn()
                    PlaylistSyncErrorKind.NETWORK,
                    PlaylistSyncErrorKind.SERVICE -> controller.syncNow(state.playlistId)
                    PlaylistSyncErrorKind.NOT_FOUND -> controller.unlink(state.playlistId)
                }
            },
            actionLabel = when (state.error) {
                PlaylistSyncErrorKind.SIGN_IN,
                PlaylistSyncErrorKind.ACCOUNT_MISMATCH -> copy.signInButton
                PlaylistSyncErrorKind.NETWORK,
                PlaylistSyncErrorKind.SERVICE -> copy.retry
                PlaylistSyncErrorKind.NOT_FOUND -> copy.unlinkButton
            }
        )
    } else if (state.lastSummary != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(LevyraCyan.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
            Text(copy.summaryText(state.lastSummary), color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (state.unmatchedCount > 0) {
        Text(copy.unmatchedNote(state.unmatchedCount), color = LevyraMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
    }

    AutoSyncToggleRow(
        copy = copy,
        checked = linked.autoSync,
        enabled = !state.syncing,
        onCheckedChange = { controller.setAutoSync(state.playlistId, it) }
    )

    Button(
        onClick = { controller.syncNow(state.playlistId) },
        enabled = !state.syncing,
        colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(if (state.syncing) copy.syncing else copy.syncNow, fontWeight = FontWeight.Bold)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(youtubeMusicPlaylistUrl(linked.remotePlaylistId)))
                    )
                } catch (_: ActivityNotFoundException) {
                }
            },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = LevyraText, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
            Text(copy.openRemote, color = LevyraText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TextButton(
            onClick = { controller.unlink(state.playlistId) },
            enabled = !state.syncing,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.LinkOff, contentDescription = null, tint = LevyraPink, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
            Text(copy.unlinkButton, color = LevyraPink, fontWeight = FontWeight.SemiBold)
        }
    }

    Text(copy.unlinkNote, color = LevyraMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun UnlinkedSyncContent(
    copy: PlaylistSyncCopy,
    state: PlaylistSyncSheetState,
    controller: PlaylistSyncController,
    onSignIn: () -> Unit
) {
    var autoSyncDraft by remember(state.playlistId) { mutableStateOf(true) }

    Text(copy.twoWayNote, color = LevyraMuted, fontSize = 13.sp)
    state.accountLabel?.takeIf(String::isNotBlank)?.let { label ->
        Text(copy.accountConnected(label), color = LevyraCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }

    if (state.syncing) {
        SyncProgressRow(copy.syncing)
    }

    if (state.error != null) {
        SyncErrorBanner(
            copy = copy,
            error = state.error,
            onAction = {
                if (state.error == PlaylistSyncErrorKind.SIGN_IN) onSignIn()
            },
            actionLabel = if (state.error == PlaylistSyncErrorKind.SIGN_IN) copy.signInButton else null
        )
    }

    AutoSyncToggleRow(
        copy = copy,
        checked = autoSyncDraft,
        enabled = !state.syncing,
        onCheckedChange = { autoSyncDraft = it }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LevyraText.copy(alpha = 0.06f))
            .clickable(enabled = !state.syncing) { controller.createRemoteAndSync(autoSyncDraft) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val alpha = if (state.syncing) 0.5f else 1f
        Icon(
            Icons.Rounded.Add,
            contentDescription = null,
            tint = LevyraCyan.copy(alpha = alpha),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LevyraCyan.copy(alpha = 0.14f * alpha))
                .padding(9.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(copy.createPrivateTitle, color = LevyraText.copy(alpha = alpha), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.size(2.dp))
            Text(copy.createPrivateSubtitle, color = LevyraMuted.copy(alpha = alpha), fontSize = 13.sp)
        }
    }

    Text(
        copy.existingSection,
        color = LevyraMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )

    when {
        state.loadingRemotePlaylists -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                LevyraLoadingIndicator(color = LevyraCyan, modifier = Modifier.size(24.dp))
                Text(copy.loadingRemote, color = LevyraMuted, fontSize = 13.sp)
            }
        }
        state.remotePlaylistsError != null -> {
            SyncErrorBanner(
                copy = copy,
                error = state.remotePlaylistsError,
                onAction = {
                    if (state.remotePlaylistsError == PlaylistSyncErrorKind.SIGN_IN) onSignIn()
                    else controller.refreshRemotePlaylists()
                },
                actionLabel = if (state.remotePlaylistsError == PlaylistSyncErrorKind.SIGN_IN) copy.signInButton else copy.retry
            )
        }
        state.remotePlaylists.isEmpty() -> {
            Text(copy.emptyRemote, color = LevyraMuted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                state.remotePlaylists.forEachIndexed { index, entry ->
                    RemotePlaylistPickerRow(
                        entry = entry,
                        enabled = !state.syncing,
                        shapeIndex = index,
                        shapeCount = state.remotePlaylists.size,
                        onClick = { controller.linkExistingAndSync(entry.browseId, entry.title, autoSyncDraft) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoSyncToggleRow(
    copy: PlaylistSyncCopy,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LevyraText.copy(alpha = 0.06f))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(copy.autoSyncTitle, color = LevyraText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.size(2.dp))
            Text(copy.autoSyncSubtitle, color = LevyraMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LevyraOnAccent,
                checkedTrackColor = LevyraCyan
            )
        )
    }
}

@Composable
private fun SyncProgressRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LevyraCyan.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LevyraLoadingIndicator(color = LevyraCyan, modifier = Modifier.size(22.dp))
        Text(label, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SyncErrorBanner(
    copy: PlaylistSyncCopy,
    error: PlaylistSyncErrorKind,
    onAction: () -> Unit,
    actionLabel: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LevyraOrange.copy(alpha = 0.14f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = LevyraOrange, modifier = Modifier.size(18.dp))
            Text(copy.errorTitle, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(copy.errorMessage(error), color = LevyraText.copy(alpha = 0.9f), fontSize = 13.sp)
        if (!actionLabel.isNullOrBlank()) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = LevyraCyan, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RemotePlaylistPickerRow(
    entry: YoutubeMusicLibraryEntry,
    enabled: Boolean,
    shapeIndex: Int,
    shapeCount: Int,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(levyraGroupedListShape(shapeIndex, shapeCount))
            .background(LevyraText.copy(alpha = 0.06f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(LevyraText.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(20.dp))
            if (entry.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = entry.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.title,
                color = LevyraText.copy(alpha = alpha),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.subtitle.isNotBlank()) {
                Text(
                    entry.subtitle,
                    color = LevyraMuted.copy(alpha = alpha),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
