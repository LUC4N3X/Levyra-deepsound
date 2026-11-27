package com.luc4n3x.levyra.ui.youtubemusic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncError
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncOutcome
import com.luc4n3x.levyra.data.youtubemusic.canonicalYoutubeMusicPlaylistId
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import com.luc4n3x.levyra.viewmodel.youtubemusic.YoutubeMusicAccountViewModel

@Composable
internal fun YoutubeMusicAccountOverlay(
    viewModel: LevyraViewModel,
    accountViewModel: YoutubeMusicAccountViewModel,
    languageCode: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val links by viewModel.playlistSync.links.collectAsStateWithLifecycle()
    val running by viewModel.playlistSync.syncing.collectAsStateWithLifecycle()
    val syncedRemotes = remember(links, running) { links.values.associate { it.remotePlaylistId to (it.playlistId in running) } }
    var failure by remember { mutableStateOf<Pair<String, PlaylistSyncError>?>(null) }
    Box(modifier.fillMaxSize()) {
        YoutubeMusicAccountScreen(
            viewModel = accountViewModel,
            languageCode = languageCode,
            onPlay = { tracks, track -> onClose(); viewModel.playFrom(tracks, track) },
            onArtist = viewModel::openArtistFromHit,
            onImported = viewModel::refreshYoutubeMusicImports,
            onClose = onClose,
            syncedRemotes = syncedRemotes,
            syncFailure = failure,
            onKeepSynced = { entry ->
                failure = null
                viewModel.playlistSync.linkRemote(entry) { _, outcome ->
                    failure = (outcome as? PlaylistSyncOutcome.Failed)?.let { canonicalYoutubeMusicPlaylistId(entry.browseId) to it.error }
                    viewModel.refreshYoutubeMusicImports()
                }
            }
        )
    }
}
