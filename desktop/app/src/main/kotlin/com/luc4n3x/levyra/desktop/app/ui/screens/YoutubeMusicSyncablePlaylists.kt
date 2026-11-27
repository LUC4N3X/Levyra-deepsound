package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.SectionTints
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.core.account.DesktopOwnedPlaylist
import com.luc4n3x.levyra.desktop.core.account.DesktopPlaylistSync
import com.luc4n3x.levyra.desktop.core.account.DesktopPlaylistSyncError
import com.luc4n3x.levyra.desktop.core.account.DesktopPlaylistSyncOutcome
import com.luc4n3x.levyra.ui.i18n.PlaylistSyncCopy
import com.luc4n3x.levyra.ui.i18n.PlaylistSyncText
import com.luc4n3x.levyra.ui.i18n.playlistSyncCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun YoutubeMusicSyncablePlaylists(sync: DesktopPlaylistSync, accountId: String, languageCode: String) {
    val strings = remember(languageCode) { SharedStrings.forCode(languageCode) }
    val copy = remember(strings) { strings.playlistSyncCopy }
    val scope = rememberCoroutineScope()
    var owned by remember(accountId) { mutableStateOf<List<DesktopOwnedPlaylist>?>(null) }
    var loadError by remember(accountId) { mutableStateOf<DesktopPlaylistSyncError?>(null) }
    var working by remember(accountId) { mutableStateOf<String?>(null) }
    var failure by remember(accountId) { mutableStateOf<Pair<String, DesktopPlaylistSyncError>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(accountId, reload) {
        loadError = null
        owned = try {
            sync.ownedPlaylists()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            loadError = sync.errorOf(error)
            emptyList()
        }
    }
    val keep: (DesktopOwnedPlaylist) -> Unit = { remote ->
        working = remote.playlistId
        failure = null
        scope.launch {
            when (val outcome = sync.importAndLink(remote)) {
                is DesktopPlaylistSyncOutcome.Failed -> failure = remote.playlistId to outcome.error
                is DesktopPlaylistSyncOutcome.Synced -> reload++
            }
            working = null
        }
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.74f))
            .padding(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SectionHeader(strings.playlists, copy[PlaylistSyncText.Intro], owned?.size?.takeIf { it > 0 && loadError == null })
        Spacer(Modifier.height(8.dp))
        val list = owned
        val error = loadError
        when {
            list == null -> repeat(3) { PlaceholderRow() }
            error != null -> StatusLine(desktopSyncErrorText(copy, error), MaterialTheme.colorScheme.error) {
                TextButton(onClick = { reload++ }) { Text(copy[PlaylistSyncText.Retry]) }
            }
            list.isEmpty() -> StatusLine(copy[PlaylistSyncText.NoPlaylists], MaterialTheme.colorScheme.onSurfaceVariant)
            else -> list.forEachIndexed { index, remote ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = 92.dp, end = 22.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                SyncableRow(
                    remote = remote,
                    copy = copy,
                    busy = working == remote.playlistId,
                    enabled = working == null,
                    error = failure?.takeIf { it.first == remote.playlistId }?.second,
                    onKeep = { keep(remote) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, body: String, count: Int?) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(SectionTints.Rose.copy(alpha = 0.32f), SectionTints.Periwinkle.copy(alpha = 0.22f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(LevyraIcons.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                if (count != null) {
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SyncableRow(
    remote: DesktopOwnedPlaylist,
    copy: PlaylistSyncCopy,
    busy: Boolean,
    enabled: Boolean,
    error: DesktopPlaylistSyncError?,
    onKeep: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val surface by animateColorAsState(if (hovered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f) else Color.Transparent)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp).clip(RoundedCornerShape(16.dp)).background(surface)
            .hoverable(interaction).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Artwork(remote.thumbnailUrl, Modifier.size(56.dp), cornerRadius = 12.dp, iconSize = 22.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(remote.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                error?.let { desktopSyncErrorText(copy, it) } ?: remote.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        FilledTonalButton(
            onClick = onKeep,
            enabled = enabled,
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            modifier = Modifier.heightIn(min = 40.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(copy[PlaylistSyncText.Syncing])
            } else {
                Icon(LevyraIcons.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(copy[PlaylistSyncText.KeepSynced])
            }
        }
    }
}

@Composable
private fun PlaceholderRow() {
    val tone = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(tone))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(180.dp).height(12.dp).clip(CircleShape).background(tone))
            Box(Modifier.width(110.dp).height(10.dp).clip(CircleShape).background(tone))
        }
        Box(Modifier.width(132.dp).height(40.dp).clip(CircleShape).background(tone))
    }
}

@Composable
private fun StatusLine(text: String, color: Color, action: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
        action()
    }
}
