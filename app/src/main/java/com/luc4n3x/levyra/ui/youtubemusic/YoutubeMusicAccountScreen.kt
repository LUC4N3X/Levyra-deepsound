package com.luc4n3x.levyra.ui.youtubemusic

import com.luc4n3x.levyra.ui.components.LevyraBackButton
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.youtubemusic.*
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.*
import com.luc4n3x.levyra.viewmodel.youtubemusic.*

@Composable
internal fun YoutubeMusicLibraryLink(account: YoutubeMusicAccount?, onClick: () -> Unit) {
    val copy = youtubeMusicCopy(LocalLevyraStrings.current.code)
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = MaterialTheme.shapes.extraLarge) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountAvatar(account, Modifier.size(48.dp))
            Column(Modifier.weight(1f)) {
                Text("YouTube Music", style = MaterialTheme.typography.titleMedium)
                Text(account?.name ?: copy.introduction, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YoutubeMusicAccountScreen(
    viewModel: YoutubeMusicAccountViewModel,
    languageCode: String,
    onPlay: (List<Track>, Track) -> Unit,
    onArtist: (ArtistHit) -> Unit,
    onImported: () -> Unit,
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val copy = youtubeMusicCopy(languageCode)
    var showAccounts by rememberSaveable { mutableStateOf(false) }
    var disconnectId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(languageCode, state.restoring, state.active?.id) { viewModel.open(languageCode) }
    val error = state.error?.message(copy)
    val actions = YoutubeMusicScreenActions(onPlay, onArtist, onImported, onClose) { showAccounts = true }
    val back: () -> Unit = { if (state.detail != null) viewModel.closeDetail() else onClose() }
    if (state.showLogin) {
        YoutubeMusicLoginScreen(copy, state.checkingLogin, error, viewModel::acceptCookie, viewModel::cancelLogin)
        return
    }
    BackHandler(onBack = back)
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopAppBar(
                title = { Text(state.detail?.title ?: "YouTube Music", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { LevyraBackButton(onClick = back, contentDescription = copy.back) },
                actions = {
                    if (state.active != null) IconButton(onClick = viewModel::refresh, enabled = !state.busy) { Icon(Icons.Rounded.Refresh, copy.refresh) }
                    IconButton(onClick = { showAccounts = true }, enabled = !state.restoring) { Icon(Icons.Rounded.AccountCircle, copy.accounts) }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
            AnimatedVisibility(state.busy || state.restoring) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            YoutubeMusicFeedback(state, copy, viewModel)
            if (state.active == null) {
                YoutubeMusicDisconnected(state, copy, viewModel, Modifier.weight(1f))
            } else {
                YoutubeMusicConnected(state, copy, viewModel, actions)
            }
        }
    }
    if (showAccounts) YoutubeMusicAccountsSheet(state, copy, viewModel, { showAccounts = false }) { disconnectId = it }
    disconnectId?.let { id ->
        AlertDialog(onDismissRequest = { disconnectId = null }, title = { Text(copy.disconnectTitle) }, text = { Text(copy.disconnectMessage) },
            confirmButton = { TextButton(onClick = { disconnectId = null; showAccounts = false; if (id == "all") viewModel.logoutAll() else viewModel.logout(id) }) { Text(copy.logout) } },
            dismissButton = { TextButton(onClick = { disconnectId = null }) { Text(copy.cancel) } })
    }
}

private class YoutubeMusicScreenActions(
    val play: (List<Track>, Track) -> Unit,
    val artist: (ArtistHit) -> Unit,
    val imported: () -> Unit,
    val close: () -> Unit,
    val accounts: () -> Unit
)

private fun YoutubeMusicAccountNotice.message(copy: YoutubeMusicCopy): String = when (this) {
    YoutubeMusicAccountNotice.Imported -> copy.imported
    YoutubeMusicAccountNotice.Empty -> copy.empty
    YoutubeMusicAccountNotice.UpToDate -> copy.upToDate
    YoutubeMusicAccountNotice.PlaylistOnly -> copy.playlistOnly
}

private fun YoutubeMusicAccountError.message(copy: YoutubeMusicCopy): String = when (this) {
    YoutubeMusicAccountError.Network -> copy.networkError
    YoutubeMusicAccountError.SignIn -> copy.signInError
    YoutubeMusicAccountError.Protocol -> copy.protocolError
    YoutubeMusicAccountError.Storage -> copy.storageError
    YoutubeMusicAccountError.Limit -> copy.limitError
}

@Composable
private fun YoutubeMusicFeedback(state: YoutubeMusicAccountUiState, copy: YoutubeMusicCopy, viewModel: YoutubeMusicAccountViewModel) {
    val error = state.error?.message(copy)
    if (error != null) {
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text(error, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { if (state.error == YoutubeMusicAccountError.SignIn) viewModel.beginLogin() else viewModel.retry() }, enabled = !state.busy) {
                    Text(if (state.error == YoutubeMusicAccountError.SignIn) copy.reconnect else copy.retry)
                }
            }
        }
    }
    if (state.notice != null) Text(
        state.notice.message(copy),
        Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun YoutubeMusicDisconnected(state: YoutubeMusicAccountUiState, copy: YoutubeMusicCopy, viewModel: YoutubeMusicAccountViewModel, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(Icons.Rounded.LibraryMusic, null, Modifier.padding(28.dp).size(56.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(copy.library, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(copy.introduction, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = viewModel::beginLogin, enabled = !state.restoring && !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(copy.connect) }
        Spacer(Modifier.height(12.dp))
        Text(copy.optional, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ColumnScope.YoutubeMusicConnected(state: YoutubeMusicAccountUiState, copy: YoutubeMusicCopy, viewModel: YoutubeMusicAccountViewModel, actions: YoutubeMusicScreenActions) {
    if (state.detail == null) {
        AccountHeader(state.active!!, copy, actions.accounts)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            YoutubeMusicCollection.entries.forEach { collection ->
                FilterChip(selected = state.collection == collection, onClick = { viewModel.selectCollection(collection) }, label = { Text(collection.label(copy)) })
            }
        }
    }
    if (state.detail != null || state.collection == YoutubeMusicCollection.Likes) {
        YoutubeMusicTracks(state, copy, viewModel, actions)
    } else {
        YoutubeMusicGrid(state, copy, viewModel, actions)
    }
}

@Composable
private fun ColumnScope.YoutubeMusicTracks(state: YoutubeMusicAccountUiState, copy: YoutubeMusicCopy, viewModel: YoutubeMusicAccountViewModel, actions: YoutubeMusicScreenActions) {
    val page = state.currentPage
    val error = state.error
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!page?.tracks.isNullOrEmpty()) item(key = "import") {
            Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.importCurrent(actions.imported) }, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Download, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(copy.import)
                    }
                    FilledTonalIconButton(onClick = { page?.tracks?.firstOrNull()?.let { actions.play(page.tracks, it) } }) { Icon(Icons.Rounded.PlayArrow, copy.play) }
                }
                Text(copy.importDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(page?.tracks.orEmpty(), key = { it.id }) { track ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.large) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).clickable { actions.play(page!!.tracks, track) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(track.thumbnailUrl, null, Modifier.size(52.dp).clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
                        Column {
                            Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { viewModel.importSong(track, actions.imported) }, enabled = !state.busy) { Icon(Icons.Rounded.Add, copy.importSong) }
                }
            }
        }
        if (page != null && page.tracks.isEmpty() && !state.busy && error == null) item { Text(copy.empty, Modifier.padding(16.dp)) }
        if (page?.continuation?.isNotBlank() == true) item { TextButton(onClick = viewModel::loadMore, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(copy.more) } }
    }
}

@Composable
private fun ColumnScope.YoutubeMusicGrid(state: YoutubeMusicAccountUiState, copy: YoutubeMusicCopy, viewModel: YoutubeMusicAccountViewModel, actions: YoutubeMusicScreenActions) {
    val page = state.currentPage
    val error = state.error
    LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(page?.entries.orEmpty(), key = { it.browseId }) { entry ->
            Card(onClick = {
                if (entry.collection == YoutubeMusicCollection.Artists) {
                    actions.close()
                    actions.artist(ArtistHit(entry.title, entry.subtitle, entry.thumbnailUrl, 0, 0, entry.browseId))
                } else viewModel.openDetail(entry)
            }, shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                    Icon(if (entry.collection == YoutubeMusicCollection.Artists) Icons.Rounded.Person else Icons.Rounded.LibraryMusic, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    AsyncImage(entry.thumbnailUrl, null, Modifier.fillMaxSize().then(if (entry.collection == YoutubeMusicCollection.Artists) Modifier.padding(12.dp).clip(CircleShape) else Modifier), contentScale = ContentScale.Crop)
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (page != null && page.entries.isEmpty() && !state.busy && error == null) item(span = { GridItemSpan(maxLineSpan) }) { Text(copy.empty, Modifier.padding(16.dp)) }
        if (page?.continuation?.isNotBlank() == true) item(span = { GridItemSpan(maxLineSpan) }) { TextButton(onClick = viewModel::loadMore, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(copy.more) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YoutubeMusicAccountsSheet(state: YoutubeMusicAccountUiState, copy: YoutubeMusicCopy, viewModel: YoutubeMusicAccountViewModel, onDismiss: () -> Unit, onDisconnect: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(copy.accounts, Modifier.padding(8.dp).semantics { heading() }, style = MaterialTheme.typography.headlineSmall) }
            items(state.accounts, key = { it.id }) { account ->
                Card(colors = CardDefaults.cardColors(containerColor = if (account.id == state.active?.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.large) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).clickable(enabled = !state.busy) { onDismiss(); viewModel.switchAccount(account.id) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AccountAvatar(account, Modifier.size(48.dp))
                            Column(Modifier.weight(1f)) {
                                Text(account.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                Text(if (account.id == state.active?.id) copy.selected else account.handle, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { onDisconnect(account.id) }, enabled = !state.busy) { Icon(Icons.AutoMirrored.Rounded.Logout, copy.logout) }
                    }
                }
            }
            item { FilledTonalButton(onClick = { onDismiss(); viewModel.beginLogin() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(copy.addAccount) } }
            if (state.accounts.isNotEmpty()) item { TextButton(onClick = { onDisconnect("all") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(copy.logoutAll) } }
        }
    }

}

@Composable
private fun AccountHeader(account: YoutubeMusicAccount, copy: YoutubeMusicCopy, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.extraLarge) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountAvatar(account, Modifier.size(64.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.headlineSmall, maxLines = 2)
                Text(account.handle.ifBlank { copy.library }, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Rounded.UnfoldMore, copy.accounts)
        }
    }
}

@Composable
private fun AccountAvatar(account: YoutubeMusicAccount?, modifier: Modifier) {
    Surface(modifier.clip(CircleShape), color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.AccountCircle, null, Modifier.fillMaxSize().padding(8.dp))
            if (!account?.avatarUrl.isNullOrBlank()) AsyncImage(account?.avatarUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

private fun YoutubeMusicCollection.label(copy: YoutubeMusicCopy): String = when (this) {
    YoutubeMusicCollection.Playlists -> copy.playlists
    YoutubeMusicCollection.Likes -> copy.likes
    YoutubeMusicCollection.Albums -> copy.albums
    YoutubeMusicCollection.Artists -> copy.artists
}
