package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.feature.scrobbling.LastFmPlaylist
import com.luc4n3x.levyra.feature.scrobbling.LastFmPlaylistKind
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraUtilitySeed
import com.luc4n3x.levyra.ui.components.levyraUtilityColors
import com.luc4n3x.levyra.ui.i18n.LastFmCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.lastFmCopy
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

@Composable
internal fun LastFmPlaylistsSection(viewModel: LevyraViewModel) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { lastFmCopy(strings.code) }
    val state by viewModel.lastFmPlaylists.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshLastFmPlaylists() }
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
                IconButton(onClick = viewModel::refreshLastFmPlaylists) { Icon(Icons.Rounded.Refresh, copy.refresh, tint = LevyraText) }
            }
        }
        if (!state.loading && state.playlists.isEmpty()) {
            Text(copy.empty, style = MaterialTheme.typography.bodyMedium, color = LevyraMuted)
        }
        state.playlists.forEach { playlist ->
            val title = copy.titleFor(playlist)
            ListenBrainzPlaylistRow(
                title = title,
                subtitle = copy.subtitleFor(playlist, strings.formatTrackCount(playlist.trackCount)),
                icon = playlist.kind.icon(),
                importing = state.importingId == playlist.id,
                importLabel = copy.import,
                onClick = { viewModel.importLastFmPlaylist(playlist, title) }
            )
        }
    }
}

private fun LastFmPlaylistKind.icon(): ImageVector = when (this) {
    LastFmPlaylistKind.Site -> Icons.AutoMirrored.Rounded.QueueMusic
    LastFmPlaylistKind.Loved -> Icons.Rounded.Favorite
    LastFmPlaylistKind.TopWeek -> Icons.Rounded.DateRange
    LastFmPlaylistKind.TopMonth -> Icons.Rounded.CalendarMonth
    LastFmPlaylistKind.TopAllTime -> Icons.Rounded.Star
}

private fun LastFmCopy.titleFor(playlist: LastFmPlaylist): String = when (playlist.kind) {
    LastFmPlaylistKind.Site -> playlist.title
    LastFmPlaylistKind.Loved -> loved
    LastFmPlaylistKind.TopWeek -> topWeek
    LastFmPlaylistKind.TopMonth -> topMonth
    LastFmPlaylistKind.TopAllTime -> topAllTime
}

private fun LastFmCopy.subtitleFor(playlist: LastFmPlaylist, count: String): String = when (playlist.kind) {
    LastFmPlaylistKind.Site -> if (playlist.trackCount > 0) "$kindSite · $count" else kindSite
    LastFmPlaylistKind.Loved -> "$kindLoved · $count"
    else -> "$kindTop · $count"
}
