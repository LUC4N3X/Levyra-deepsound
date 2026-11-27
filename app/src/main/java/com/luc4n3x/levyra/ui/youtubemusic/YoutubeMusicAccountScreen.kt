@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.luc4n3x.levyra.ui.youtubemusic

import com.luc4n3x.levyra.ui.components.LevyraMediaTitle
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.LevyraExpressiveIconButton
import com.luc4n3x.levyra.ui.components.LevyraFilterPill
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraUtilityBackdrop
import com.luc4n3x.levyra.ui.components.LevyraUtilityColors
import com.luc4n3x.levyra.ui.components.LevyraUtilityStyleActive
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.levyraUtilityColors
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.youtubemusic.*
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.*
import com.luc4n3x.levyra.ui.library.playlistSyncErrorText
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
    onClose: () -> Unit,
    syncedRemotes: Map<String, Boolean> = emptyMap(),
    syncFailure: Pair<String, PlaylistSyncError>? = null,
    onKeepSynced: (YoutubeMusicLibraryEntry) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val computerLink by viewModel.computerLink.collectAsStateWithLifecycle()
    val copy = youtubeMusicCopy(languageCode)
    val linkCopy = LocalLevyraStrings.current.youtubeMusicLinkCopy
    val scanComputer = rememberComputerLinkScanner(linkCopy[YoutubeMusicLinkText.ScanPrompt], viewModel::linkComputer, viewModel::computerScanUnavailable)
    var showAccounts by rememberSaveable { mutableStateOf(false) }
    var disconnectId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(languageCode, state.restoring, state.active?.id) { viewModel.open(languageCode) }
    val error = state.error?.message(copy)
    val tone = levyraUtilityColors()
    val actions = YoutubeMusicScreenActions(onPlay, onArtist, onImported, onClose, { showAccounts = true }, syncedRemotes, syncFailure, onKeepSynced)
    val back: () -> Unit = { if (state.detail != null) viewModel.closeDetail() else onClose() }
    if (state.showLogin) {
        YoutubeMusicLoginScreen(copy, state.checkingLogin, error, viewModel::acceptCookie, viewModel::cancelLogin)
        return
    }
    BackHandler(onBack = back)
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            if (LevyraUtilityStyleActive) LevyraUtilityBackdrop(strength = YoutubeMusicBackdropStrength)
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                YoutubeMusicTopRow(state.detail?.title, copy.back, back) {
                    if (state.active != null) {
                        LevyraExpressiveIconButton(onClick = scanComputer, enabled = !state.busy) { Icon(Icons.Rounded.Computer, linkCopy[YoutubeMusicLinkText.SignInComputer]) }
                        LevyraExpressiveIconButton(onClick = viewModel::refresh, enabled = !state.busy) { Icon(Icons.Rounded.Refresh, copy.refresh) }
                    }
                    if (state.active == null) {
                        LevyraExpressiveIconButton(onClick = { showAccounts = true }, enabled = !state.restoring) { Icon(Icons.Rounded.ManageAccounts, copy.accounts) }
                    }
                }
                AnimatedVisibility(state.busy || state.restoring) {
                    LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), color = tone.accent, trackColor = tone.softContainer)
                }
                YoutubeMusicFeedback(state, copy, tone, viewModel)
                if (state.active == null) {
                    YoutubeMusicDisconnected(state, copy, tone, viewModel, Modifier.weight(1f))
                } else {
                    YoutubeMusicConnected(state, copy, tone, viewModel, actions)
                }
            }
        }
    }
    if (showAccounts) YoutubeMusicAccountsSheet(state, copy, linkCopy, tone, viewModel, { showAccounts = false }, scanComputer) { disconnectId = it }
    ComputerLinkSheet(computerLink, linkCopy, viewModel::confirmComputerLink, { viewModel.dismissComputerLink(); scanComputer() }, viewModel::dismissComputerLink)
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
    val accounts: () -> Unit,
    val syncedRemotes: Map<String, Boolean>,
    val syncFailure: Pair<String, PlaylistSyncError>?,
    val keepSynced: (YoutubeMusicLibraryEntry) -> Unit
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
private fun YoutubeMusicTopRow(title: String?, backLabel: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LevyraBackButton(
            onClick = onBack,
            contentDescription = backLabel,
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface)
        )
        Box(Modifier.weight(1f)) {
            if (title != null) Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
        }
        actions()
    }
}

@Composable
private fun YoutubeMusicFeedback(state: YoutubeMusicAccountUiState, copy: YoutubeMusicCopy, tone: LevyraUtilityColors, viewModel: YoutubeMusicAccountViewModel) {
    val error = state.error?.message(copy)
    if (error != null) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.errorContainer).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(error, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = { if (state.error == YoutubeMusicAccountError.SignIn) viewModel.beginLogin() else viewModel.retry() }, enabled = !state.busy) {
                Text(if (state.error == YoutubeMusicAccountError.SignIn) copy.reconnect else copy.retry, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
    if (state.notice != null) Row(
        Modifier.padding(horizontal = 20.dp, vertical = 6.dp).clip(CircleShape).background(tone.softContainer)
            .padding(horizontal = 14.dp, vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp), tint = tone.accent)
        Text(state.notice.message(copy), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun YoutubeMusicDisconnected(state: YoutubeMusicAccountUiState, copy: YoutubeMusicCopy, tone: LevyraUtilityColors, viewModel: YoutubeMusicAccountViewModel, modifier: Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(120.dp).background(tone.softContainer, MaterialShapes.Cookie9Sided.toShape()).clearAndSetSemantics { },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LibraryMusic, null, Modifier.size(52.dp), tint = tone.accent)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                copy.library,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(12.dp))
            Text(copy.introduction, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = viewModel::beginLogin,
                enabled = !state.restoring && !state.busy,
                colors = ButtonDefaults.buttonColors(containerColor = tone.accent, contentColor = tone.onAccent),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) {
                Text(copy.connect, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(14.dp))
            Text(copy.optional, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ColumnScope.YoutubeMusicConnected(state: YoutubeMusicAccountUiState, copy: YoutubeMusicCopy, tone: LevyraUtilityColors, viewModel: YoutubeMusicAccountViewModel, actions: YoutubeMusicScreenActions) {
    if (state.detail != null || state.collection == YoutubeMusicCollection.Likes) {
        YoutubeMusicTracks(state, copy, tone, viewModel, actions)
    } else {
        YoutubeMusicGrid(state, copy, tone, viewModel, actions)
    }
}

@Composable
private fun YoutubeMusicOverview(state: YoutubeMusicAccountUiState, copy: YoutubeMusicCopy, tone: LevyraUtilityColors, viewModel: YoutubeMusicAccountViewModel, onAccounts: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            "YouTube Music",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() }
        )
        state.active?.let { AccountHero(it, copy, tone, onAccounts) }
        Row(
            Modifier.fillMaxWidth().bleed(YoutubeMusicGutter).horizontalScroll(rememberScrollState()).padding(horizontal = YoutubeMusicGutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            YoutubeMusicCollection.entries.forEach { collection ->
                LevyraFilterPill(collection.label(copy), state.collection == collection, { viewModel.selectCollection(collection) })
            }
        }
    }
}

@Composable
private fun YoutubeMusicDetailHeader(entry: YoutubeMusicLibraryEntry) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.size(124.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.LibraryMusic, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            AsyncImage(entry.thumbnailUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(entry.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            if (entry.subtitle.isNotBlank()) Text(entry.subtitle, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ColumnScope.YoutubeMusicTracks(state: YoutubeMusicAccountUiState, copy: YoutubeMusicCopy, tone: LevyraUtilityColors, viewModel: YoutubeMusicAccountViewModel, actions: YoutubeMusicScreenActions) {
    val page = state.currentPage
    val error = state.error
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = YoutubeMusicGutter, end = YoutubeMusicGutter, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val detail = state.detail
        if (detail == null) item(key = "overview") { YoutubeMusicOverview(state, copy, tone, viewModel, actions.accounts) }
        else item(key = "detail") { YoutubeMusicDetailHeader(detail) }
        if (!page?.tracks.isNullOrEmpty()) item(key = "import") {
            Column(Modifier.padding(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { viewModel.importCurrent(actions.imported) },
                        enabled = !state.busy,
                        colors = ButtonDefaults.buttonColors(containerColor = tone.accent, contentColor = tone.onAccent),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                    ) {
                        Icon(Icons.Rounded.Download, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(copy.import)
                    }
                    FilledTonalIconButton(
                        onClick = { page.tracks.firstOrNull()?.let { actions.play(page.tracks, it) } },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = tone.container, contentColor = tone.onContainer),
                        modifier = Modifier.size(52.dp)
                    ) { Icon(Icons.Rounded.PlayArrow, copy.play) }
                }
                Text(copy.importDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (detail != null && detail.owned && detail.collection == YoutubeMusicCollection.Playlists) {
                    YoutubeMusicKeepSynced(detail, tone, actions, enabled = !state.busy)
                }
            }
        }
        items(page?.tracks.orEmpty(), key = { it.id }, contentType = { "YoutubeMusicAccountScreen-page" }) { track ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .levyraPressable(onClick = { actions.play(page!!.tracks, track) }, pressedScale = LevyraPressScale.Row, role = Role.Button, onClickLabel = copy.play)
                    .padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AsyncImage(track.thumbnailUrl, null, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LevyraMediaTitle(track.title, explicit = track.explicit, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { viewModel.importSong(track, actions.imported) }, enabled = !state.busy) { Icon(Icons.Rounded.AddCircleOutline, copy.importSong, tint = tone.accent) }
            }
        }
        if (page != null && page.tracks.isEmpty() && !state.busy && error == null) item(key = "empty") { YoutubeMusicEmpty(copy, tone) }
        if (page?.continuation?.isNotBlank() == true) item(key = "more") { YoutubeMusicMore(copy, state.busy, viewModel::loadMore) }
    }
}

@Composable
private fun ColumnScope.YoutubeMusicGrid(state: YoutubeMusicAccountUiState, copy: YoutubeMusicCopy, tone: LevyraUtilityColors, viewModel: YoutubeMusicAccountViewModel, actions: YoutubeMusicScreenActions) {
    val page = state.currentPage
    val error = state.error
    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(start = YoutubeMusicGutter, end = YoutubeMusicGutter, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "overview", span = { GridItemSpan(maxLineSpan) }) { YoutubeMusicOverview(state, copy, tone, viewModel, actions.accounts) }
        items(page?.entries.orEmpty(), key = { it.browseId }, contentType = { "YoutubeMusicAccountScreen-page" }) { entry ->
            YoutubeMusicEntryTile(entry, tone) {
                if (entry.collection == YoutubeMusicCollection.Artists) {
                    actions.close()
                    actions.artist(ArtistHit(entry.title, entry.subtitle, entry.thumbnailUrl, 0, 0, entry.browseId))
                } else viewModel.openDetail(entry)
            }
        }
        if (page != null && page.entries.isEmpty() && !state.busy && error == null) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { YoutubeMusicEmpty(copy, tone) }
        if (page?.continuation?.isNotBlank() == true) item(key = "more", span = { GridItemSpan(maxLineSpan) }) { YoutubeMusicMore(copy, state.busy, viewModel::loadMore) }
    }
}

@Composable
private fun YoutubeMusicEntryTile(entry: YoutubeMusicLibraryEntry, tone: LevyraUtilityColors, onOpen: () -> Unit) {
    val artist = entry.collection == YoutubeMusicCollection.Artists
    Column(
        Modifier.levyraPressable(
            onClick = onOpen,
            pressedScale = LevyraPressScale.Tile,
            role = Role.Button,
            onClickLabel = entry.title
        ),
        horizontalAlignment = if (artist) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(if (artist) CircleShape else RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(tone.softContainer, MaterialTheme.colorScheme.surfaceContainerHigh))),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (artist) Icons.Rounded.Person else Icons.Rounded.LibraryMusic, null, Modifier.size(44.dp), tint = tone.accent)
            AsyncImage(entry.thumbnailUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Column(Modifier.padding(horizontal = 4.dp), horizontalAlignment = if (artist) Alignment.CenterHorizontally else Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = if (artist) TextAlign.Center else TextAlign.Start, color = MaterialTheme.colorScheme.onSurface)
            if (entry.subtitle.isNotBlank()) Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun YoutubeMusicEmpty(copy: YoutubeMusicCopy, tone: LevyraUtilityColors) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(72.dp).background(tone.softContainer, MaterialShapes.Cookie9Sided.toShape()).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.LibraryMusic, null, Modifier.size(32.dp), tint = tone.accent)
        }
        Text(copy.empty, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun YoutubeMusicMore(copy: YoutubeMusicCopy, busy: Boolean, onMore: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        FilledTonalButton(onClick = onMore, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(copy.more) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YoutubeMusicAccountsSheet(
    state: YoutubeMusicAccountUiState,
    copy: YoutubeMusicCopy,
    linkCopy: YoutubeMusicLinkCopy,
    tone: LevyraUtilityColors,
    viewModel: YoutubeMusicAccountViewModel,
    onDismiss: () -> Unit,
    onSignInComputer: () -> Unit,
    onDisconnect: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(copy.accounts, Modifier.padding(start = 4.dp, bottom = 8.dp).semantics { heading() }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            items(state.accounts, key = { it.id }, contentType = { "YoutubeMusicAccountScreen-accounts" }) { account ->
                val active = account.id == state.active?.id
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(if (active) tone.softContainer else MaterialTheme.colorScheme.surfaceContainer),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier.weight(1f).clickable(enabled = !state.busy, role = Role.Button) { onDismiss(); viewModel.switchAccount(account.id) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AccountAvatar(account, Modifier.size(48.dp))
                        Column(Modifier.weight(1f)) {
                            Text(account.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, color = MaterialTheme.colorScheme.onSurface)
                            Text(if (active) copy.selected else account.handle, style = MaterialTheme.typography.bodySmall, color = if (active) tone.accent else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (active) Icon(Icons.Rounded.CheckCircle, null, tint = tone.accent)
                    }
                    IconButton(onClick = { onDisconnect(account.id) }, enabled = !state.busy) { Icon(Icons.AutoMirrored.Rounded.Logout, copy.logout) }
                }
            }
            item {
                Button(
                    onClick = { onDismiss(); viewModel.beginLogin() },
                    enabled = !state.busy,
                    colors = ButtonDefaults.buttonColors(containerColor = tone.accent, contentColor = tone.onAccent),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 52.dp)
                ) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(copy.addAccount) }
            }
            if (state.active != null) item {
                OutlinedButton(onClick = { onDismiss(); onSignInComputer() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(Icons.Rounded.Computer, null); Spacer(Modifier.width(8.dp)); Text(linkCopy[YoutubeMusicLinkText.SignInComputer])
                }
            }
            if (state.accounts.isNotEmpty()) item { TextButton(onClick = { onDisconnect("all") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(copy.logoutAll) } }
        }
    }
}

@Composable
private fun AccountHero(account: YoutubeMusicAccount, copy: YoutubeMusicCopy, tone: LevyraUtilityColors, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(tone.softContainer, MaterialTheme.colorScheme.surfaceContainer)))
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Surface, role = Role.Button, onClickLabel = copy.accounts)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(68.dp).border(2.dp, tone.accent, CircleShape).padding(5.dp)) {
            AccountAvatar(account, Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(account.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            Text(account.handle.ifBlank { copy.library }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.size(44.dp).background(tone.container, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.UnfoldMore, null, tint = tone.onContainer)
        }
    }
}

@Composable
private fun AccountAvatar(account: YoutubeMusicAccount?, modifier: Modifier) {
    Surface(modifier.clip(CircleShape), color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.AccountCircle, null, Modifier.fillMaxSize().padding(8.dp))
            if (!account?.avatarUrl.isNullOrBlank()) AsyncImage(account.avatarUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

private fun YoutubeMusicCollection.label(copy: YoutubeMusicCopy): String = when (this) {
    YoutubeMusicCollection.Playlists -> copy.playlists
    YoutubeMusicCollection.Likes -> copy.likes
    YoutubeMusicCollection.Albums -> copy.albums
    YoutubeMusicCollection.Artists -> copy.artists
}

private const val YoutubeMusicBackdropStrength = 1.3f
private val YoutubeMusicGutter = 20.dp

private fun Modifier.bleed(gutter: Dp): Modifier = layout { measurable, constraints ->
    val extra = gutter.roundToPx()
    val placeable = measurable.measure(constraints.copy(minWidth = constraints.maxWidth + extra * 2, maxWidth = constraints.maxWidth + extra * 2))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
}

@Composable
private fun YoutubeMusicKeepSynced(entry: YoutubeMusicLibraryEntry, tone: LevyraUtilityColors, actions: YoutubeMusicScreenActions, enabled: Boolean) {
    val copy = LocalLevyraStrings.current.playlistSyncCopy
    val remoteId = canonicalYoutubeMusicPlaylistId(entry.browseId)
    val syncing = actions.syncedRemotes[remoteId]
    val failure = actions.syncFailure?.takeIf { it.first == remoteId }?.second
    FilledTonalButton(
        onClick = { actions.keepSynced(entry) },
        enabled = enabled && syncing != true,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = tone.container, contentColor = tone.onContainer),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) {
        if (syncing == true) {
            CircularWavyProgressIndicator(Modifier.size(18.dp), color = tone.onContainer)
        } else {
            Icon(if (syncing == false) Icons.Rounded.CloudDone else Icons.Rounded.CloudSync, null, Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            when (syncing) {
                true -> copy[PlaylistSyncText.Syncing]
                false -> copy[PlaylistSyncText.SyncNow]
                null -> copy[PlaylistSyncText.KeepSynced]
            }
        )
    }
    if (failure != null && syncing != true) {
        Text(playlistSyncErrorText(copy, failure), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}
