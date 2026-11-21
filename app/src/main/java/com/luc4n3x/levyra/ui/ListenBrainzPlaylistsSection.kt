package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.feature.scrobbling.ListenBrainzPlaylist
import com.luc4n3x.levyra.feature.scrobbling.ListenBrainzPlaylistKind
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraUtilitySeed
import com.luc4n3x.levyra.ui.components.levyraUtilityColors
import com.luc4n3x.levyra.ui.i18n.ListenBrainzCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.listenBrainzCopy
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

@Composable
internal fun ListenBrainzPlaylistsSection(viewModel: LevyraViewModel) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { listenBrainzCopy(strings.code) }
    val state by viewModel.listenBrainzPlaylists.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshListenBrainzPlaylists() }
    val utility = levyraUtilityColors(LevyraUtilitySeed)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(copy.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LevyraText)
                Text(copy.subtitle, style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
            }
            if (state.loading) {
                LevyraLoadingIndicator(Modifier.size(32.dp), color = utility.accent)
            } else {
                IconButton(onClick = viewModel::refreshListenBrainzPlaylists) { Icon(Icons.Rounded.Refresh, copy.refresh, tint = LevyraText) }
            }
        }
        if (!state.loading && state.playlists.isEmpty()) {
            Text(copy.empty, style = MaterialTheme.typography.bodyMedium, color = LevyraMuted)
        }
        state.playlists.forEach { playlist ->
            val title = copy.titleFor(playlist)
            ListenBrainzPlaylistRow(
                title = title,
                subtitle = copy.kind(playlist.kind),
                icon = playlist.kind.icon(),
                importing = state.importingId == playlist.id,
                importLabel = copy.import,
                onClick = { viewModel.importListenBrainzPlaylist(playlist, title) }
            )
        }
    }
}

@Composable
private fun ListenBrainzPlaylistRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    importing: Boolean,
    importLabel: String,
    onClick: () -> Unit
) {
    val utility = levyraUtilityColors(LevyraUtilitySeed)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = !importing, role = Role.Button, onClickLabel = importLabel, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(utility.container), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = utility.onContainer)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = LevyraText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LevyraMuted, maxLines = 1)
        }
        if (importing) {
            LevyraLoadingIndicator(Modifier.size(28.dp), color = utility.accent)
        } else {
            Icon(Icons.Rounded.Download, importLabel, tint = utility.accent)
        }
    }
}

private fun ListenBrainzPlaylistKind.icon(): ImageVector = when (this) {
    ListenBrainzPlaylistKind.TopWeek -> Icons.Rounded.DateRange
    ListenBrainzPlaylistKind.TopMonth -> Icons.Rounded.CalendarMonth
    ListenBrainzPlaylistKind.CreatedFor -> Icons.Rounded.AutoAwesome
    ListenBrainzPlaylistKind.Own -> Icons.AutoMirrored.Rounded.QueueMusic
}

private fun ListenBrainzCopy.titleFor(playlist: ListenBrainzPlaylist): String = when (playlist.kind) {
    ListenBrainzPlaylistKind.TopWeek -> topWeek
    ListenBrainzPlaylistKind.TopMonth -> topMonth
    else -> playlist.title
}
