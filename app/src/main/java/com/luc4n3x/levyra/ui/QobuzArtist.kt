package com.luc4n3x.levyra.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.i18n.LevyraQobuzSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.QobuzSkinCopy
import com.luc4n3x.levyra.ui.player.qobuzPlayerField
import com.luc4n3x.levyra.ui.theme.QobuzDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

@Composable
internal fun QobuzArtistScreen(state: LevyraUiState, viewModel: LevyraViewModel, onTrackActions: (Track) -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraQobuzSkinStrings.forCode(strings.code) }
    val profile = state.artistProfile
    val listState = rememberSaveable(state.artistListStateKey, saver = LazyListState.Saver) { LazyListState() }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val header = remember(profile?.accentStart) { qobuzPlayerField(Color(profile?.accentStart ?: 0xFF4A2A6A.toInt())) }
    var showAllSongs by rememberSaveable(profile?.browseId) { mutableStateOf(false) }
    val collapsed by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 420 } }
    Box(Modifier.fillMaxSize().background(QobuzDesign.surfacePrimary)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = levyraTabContentBottomPadding(state.currentTrack != null) + 24.dp)) {
            when {
                state.artistLoading && profile == null -> item(key = "q-artist-loading") {
                    Box(Modifier.fillMaxWidth().padding(top = statusTop + 200.dp), contentAlignment = Alignment.Center) {
                        LevyraLoadingIndicator(color = QobuzDesign.textAccent)
                    }
                }
                profile == null -> item(key = "q-artist-error") {
                    Text(strings.artistProfileUnavailable, style = QobuzText.baseline,
                        modifier = Modifier.padding(start = QobuzDesign.gutter, end = QobuzDesign.gutter, top = statusTop + 96.dp))
                }
                else -> {
                    item(key = "q-artist-header") { QobuzArtistHeader(profile, state, viewModel, copy, header, statusTop) }
                    qobuzArtistSections(profile, state, viewModel, copy, showAllSongs, { showAllSongs = !showAllSongs }, onTrackActions)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(if (collapsed) header else Color.Transparent).padding(top = statusTop).height(QobuzDesign.topBarHeight)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QobuzWhiteIcon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, viewModel::closeArtist)
            Text(
                if (collapsed) profile?.name.orEmpty() else "", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            profile?.topSongs?.firstOrNull()?.let { first ->
                QobuzWhiteIcon(Icons.Rounded.MoreVert, strings.more) { onTrackActions(first) }
            }
        }
    }
}

private const val ArtistPreviewSongs = 5

@Composable
private fun QobuzWhiteIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun QobuzArtistHeader(profile: ArtistProfile, state: LevyraUiState, viewModel: LevyraViewModel, copy: QobuzSkinCopy, background: Color, statusTop: Dp) {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    var expanded by rememberSaveable(profile.browseId) { mutableStateOf(false) }
    val followed = (profile.browseId.isNotBlank() && profile.browseId in state.followedArtistKeys) || profile.name.trim().lowercase() in state.followedArtistKeys
    val bio = profile.biography?.text?.takeIf { it.isNotBlank() } ?: profile.bio
    Column(
        Modifier.fillMaxWidth().background(background).padding(top = statusTop + QobuzDesign.topBarHeight - 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val portrait = remember(profile.thumbnailUrl, profile.bannerUrl) {
            com.luc4n3x.levyra.data.fullResolutionArtworkUrl(profile.thumbnailUrl.ifBlank { profile.bannerUrl })
        }
        QobuzArtworkBox(portrait, Modifier.size(200.dp), CircleShape)
        Spacer(Modifier.height(24.dp))
        Text(profile.name, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
            color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Text(copy.artist, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium), color = Color.White)
        if (bio.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                bio, style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center,
                maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                (if (expanded) strings.showLess else copy.readMore).uppercase(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White, modifier = Modifier.clip(QobuzDesign.buttonShape).clickable(role = Role.Button) { expanded = !expanded }.padding(12.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            QobuzWhiteIcon(Icons.Rounded.Share, strings.share) {
                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, profile.name) }
                runCatching { context.startActivity(Intent.createChooser(intent, strings.shareVia)) }
            }
            profile.topSongs.firstOrNull()?.let { seed -> QobuzWhiteIcon(Icons.Rounded.Sensors, strings.continuousRadio) { viewModel.startSongRadioFrom(seed) } }
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Color.White)
                    .clickable(role = Role.Button, onClickLabel = strings.play, enabled = profile.topSongs.isNotEmpty()) { viewModel.playAll(profile.topSongs) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.PlayArrow, strings.play, tint = Color.Black, modifier = Modifier.size(30.dp)) }
            QobuzWhiteIcon(if (followed) Icons.Rounded.HowToReg else Icons.Rounded.PersonAdd, if (followed) strings.followingArtist else strings.followArtist, viewModel::toggleFollowArtist)
            if (profile.topSongs.isNotEmpty()) {
                QobuzWhiteIcon(Icons.AutoMirrored.Rounded.QueueMusic, strings.addToQueue) { profile.topSongs.forEach(viewModel::addToQueue) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.qobuzArtistSections(
    profile: ArtistProfile,
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    copy: QobuzSkinCopy,
    showAllSongs: Boolean,
    onToggleSongs: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val currentId = state.currentTrack?.id
    if (profile.topSongs.isNotEmpty()) {
        item(key = "q-artist-popular-title") {
            Column(Modifier.padding(top = 32.dp, bottom = 8.dp)) {
                QobuzSectionHeader(
                    LocalLevyraStrings.current.popularTracks, null,
                    if (showAllSongs) LocalLevyraStrings.current.showLess else copy.seeAll,
                    if (profile.topSongs.size > ArtistPreviewSongs) onToggleSongs else null
                )
            }
        }
        items(if (showAllSongs) profile.topSongs else profile.topSongs.take(ArtistPreviewSongs), key = { "q-top-${it.id}" }) { track ->
            QobuzTrackRow(track, track.id == currentId, copy, { viewModel.playFrom(profile.topSongs, track) }, { onTrackActions(track) })
        }
    }
    qobuzReleaseShelf("albums", profile.albums + profile.compilations, profile, viewModel, copy) { LocalLevyraStrings.current.albumsPlain }
    qobuzReleaseShelf("singles", profile.singles, profile, viewModel, copy) { LocalLevyraStrings.current.singlesAndEps }
    if (profile.relatedArtists.isNotEmpty()) {
        item(key = "q-artist-related") {
            Column(Modifier.padding(top = QobuzDesign.sectionGap)) {
                QobuzSectionHeader(LocalLevyraStrings.current.similarArtists, null, copy.seeAll, null)
                Spacer(Modifier.height(QobuzDesign.headerToContent))
                LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
                    items(profile.relatedArtists, key = { "q-rel-${it.browseId.ifBlank { it.name }}" }) { hit ->
                        val followed = (hit.browseId.isNotBlank() && hit.browseId in state.followedArtistKeys) || hit.name.trim().lowercase() in state.followedArtistKeys
                        QobuzArtistCard(hit, copy, followed, { viewModel.openArtistFromHit(hit) }, { viewModel.toggleFollowArtist(hit) })
                    }
                }
            }
        }
    }
    if (profile.featuredPlaylists.isNotEmpty()) {
        item(key = "q-artist-featured") {
            Column(Modifier.padding(top = QobuzDesign.sectionGap)) {
                QobuzSectionHeader(com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy(LocalLevyraStrings.current.code).featuredOn, null, copy.seeAll, null)
                Spacer(Modifier.height(QobuzDesign.headerToContent))
                LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
                    items(profile.featuredPlaylists, key = { "q-feat-${it.playlistId.ifBlank { it.browseId.ifBlank { it.title } }}" }) { playlist ->
                        Column(Modifier.width(QobuzDesign.gridCard).clickable(role = Role.Button) { viewModel.openPlaylistHit(playlist) }) {
                            QobuzArtworkBox(playlist.thumbnailUrl, Modifier.fillMaxWidth().aspectRatio(1f))
                            Spacer(Modifier.height(8.dp))
                            Text(playlist.title, style = QobuzText.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(playlist.author, style = QobuzText.cardSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.qobuzReleaseShelf(
    key: String,
    releases: List<ArtistRelease>,
    profile: ArtistProfile,
    viewModel: LevyraViewModel,
    copy: QobuzSkinCopy,
    title: @Composable () -> String
) {
    if (releases.isEmpty()) return
    item(key = "q-artist-$key") {
        Column(Modifier.padding(top = QobuzDesign.sectionGap)) {
            QobuzSectionHeader(title(), null, copy.seeAll, null)
            Spacer(Modifier.height(QobuzDesign.headerToContent))
            LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
                items(releases.distinctBy { it.browseId.ifBlank { it.title } }, key = { "q-$key-${it.browseId.ifBlank { it.title }}" }) { release ->
                    Column(Modifier.width(192.dp).clickable(role = Role.Button) { viewModel.openArtistRelease(release, profile.name) }) {
                        QobuzArtworkBox(release.thumbnailUrl, Modifier.fillMaxWidth().aspectRatio(1f))
                        Spacer(Modifier.height(8.dp))
                        Text(release.title, style = QobuzText.cardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(release.year, style = QobuzText.cardSubtitle, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
internal fun QobuzTrackRow(track: Track, current: Boolean, copy: QobuzSkinCopy, onClick: () -> Unit, onMore: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(start = QobuzDesign.gutter, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(QobuzDesign.listArtwork).clip(QobuzDesign.artworkShape).background(QobuzDesign.placeholder)) { CoverImage(track, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = if (current) QobuzDesign.textAccent else QobuzDesign.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.explicit) {
                    QobuzExplicitBadge(copy.explicit)
                    Spacer(Modifier.width(6.dp))
                }
                Text(listOf(track.artist, track.album).filter(String::isNotBlank).joinToString(" • "), style = QobuzText.cardSubtitle,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = LocalLevyraStrings.current.more, onClick = onMore), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MoreVert, LocalLevyraStrings.current.more, tint = QobuzDesign.iconPrimary, modifier = Modifier.size(22.dp))
        }
    }
}
