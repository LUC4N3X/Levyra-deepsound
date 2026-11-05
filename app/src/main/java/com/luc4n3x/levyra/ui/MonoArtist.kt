package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.fullResolutionArtworkUrl
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.SpeedDial
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.i18n.LevyraMonoSearchStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.MonoSearchCopy
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

private const val MonoArtistPreviewSongs = 5
private const val MonoArtistRailLimit = 24
private const val MonoArtistParallax = 0.5f
private const val MonoArtistPortraitRatio = 1.08f
private val MonoArtistPortraitMax = 480.dp
private val MonoArtistBarHeight = 56.dp
private val MonoArtistPlaySize = 56.dp
private val MonoArtistRelease = 140.dp
private val MonoArtistVideoWidth = 224.dp
private val MonoArtistPulseBrush = Brush.horizontalGradient(listOf(MonoDesign.accent.copy(alpha = 0.85f), MonoDesign.accent.copy(alpha = 0.08f)))

@Composable
internal fun MonoArtistScreen(state: LevyraUiState, viewModel: LevyraViewModel, onTrackActions: (Track) -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoSearchStrings.forCode(strings.code) }
    val profile = state.artistProfile
    val listState = rememberSaveable(state.artistListStateKey, saver = LazyListState.Saver) { LazyListState() }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var showAllSongs by rememberSaveable(profile?.browseId) { mutableStateOf(false) }
    val portrait = remember(profile?.thumbnailUrl, profile?.bannerUrl) {
        profile?.let { fullResolutionArtworkUrl(it.thumbnailUrl.ifBlank { it.bannerUrl }) }.orEmpty()
    }
    val scrolled: () -> Float = remember(listState) {
        { if (listState.firstVisibleItemIndex > 0) Float.MAX_VALUE else listState.firstVisibleItemScrollOffset.toFloat() }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(MonoDesign.base)) {
        val portraitHeight = min(maxWidth * MonoArtistPortraitRatio, MonoArtistPortraitMax)
        val density = LocalDensity.current
        val thresholdPx = with(density) { (portraitHeight - statusTop - MonoArtistBarHeight).toPx() }
        val collapsed by remember(listState, thresholdPx) {
            derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > thresholdPx }
        }
        val barAlpha by animateFloatAsState(if (collapsed) 1f else 0f, tween(if (LocalAnimationsEnabled.current) 220 else 0), label = "mono-artist-bar")
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = levyraTabContentBottomPadding(state.currentTrack != null) + 24.dp)
        ) {
            when {
                state.artistLoading && profile == null -> item(key = "mono-artist-loading") {
                    Box(Modifier.fillMaxWidth().padding(top = statusTop + 220.dp), contentAlignment = Alignment.Center) {
                        MonoWaveLine(playing = true, color = MonoDesign.accent, modifier = Modifier.width(120.dp).height(28.dp))
                    }
                }
                profile == null -> item(key = "mono-artist-unavailable") {
                    Text(
                        strings.artistProfileUnavailable, style = MaterialTheme.typography.bodyLarge, color = MonoDesign.textSecondary,
                        modifier = Modifier.padding(start = MonoDesign.gutter, end = MonoDesign.gutter, top = statusTop + 96.dp)
                    )
                }
                else -> {
                    item(key = "mono-artist-header", contentType = "mono-artist-header") {
                        MonoArtistHeader(profile, copy, portrait, portraitHeight, scrolled)
                    }
                    item(key = "mono-artist-actions", contentType = "mono-artist-actions") {
                        MonoArtistActions(profile, state, viewModel)
                    }
                    monoArtistSections(profile, state, viewModel, copy, showAllSongs, { showAllSongs = !showAllSongs }, onTrackActions)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth()
                .drawBehind { drawRect(MonoDesign.base.copy(alpha = barAlpha)) }
                .padding(top = statusTop)
        ) {
            Row(Modifier.fillMaxWidth().height(MonoArtistBarHeight).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .drawBehind { drawCircle(MonoDesign.void.copy(alpha = 0.45f * (1f - barAlpha)), radius = 18.dp.toPx()) }
                        .clickable(role = Role.Button, onClick = viewModel::closeArtist),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = MonoDesign.text, modifier = Modifier.size(22.dp))
                }
                Text(
                    profile?.name.orEmpty(), style = MaterialTheme.typography.titleMedium, color = MonoDesign.text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp).graphicsLayer { alpha = barAlpha }
                        .then(if (collapsed) Modifier else Modifier.clearAndSetSemantics { })
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).graphicsLayer { alpha = barAlpha }.background(MonoDesign.hairline))
        }
    }
}

@Composable
private fun MonoArtistHeader(
    profile: ArtistProfile,
    copy: MonoSearchCopy,
    portrait: String,
    portraitHeight: Dp,
    scrolled: () -> Float
) {
    val animated = LocalAnimationsEnabled.current
    val overline = profile.monthlyListeners.ifBlank { profile.subscribers }.ifBlank { copy.artistOverline }
    Box(Modifier.fillMaxWidth().height(portraitHeight).clipToBounds()) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                if (animated) {
                    val travel = minOf(scrolled(), size.height)
                    translationY = travel * MonoArtistParallax
                    alpha = 1f - travel / size.height.coerceAtLeast(1f) * 0.7f
                }
            }
        ) {
            if (portrait.isNotBlank()) {
                AsyncImage(portrait, null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(MonoDesign.surface), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, null, tint = MonoDesign.textFaint, modifier = Modifier.size(72.dp))
                }
            }
        }
        Spacer(
            Modifier.matchParentSize().drawWithCache {
                val shade = Brush.verticalGradient(
                    0f to MonoDesign.base.copy(alpha = 0.55f),
                    0.22f to Color.Transparent,
                    0.5f to Color.Transparent,
                    0.82f to MonoDesign.base.copy(alpha = 0.78f),
                    1f to MonoDesign.base
                )
                onDrawBehind { drawRect(shade) }
            }
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = MonoDesign.gutter),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MonoOverline(overline, color = MonoDesign.accent)
            Text(
                profile.name, style = MaterialTheme.typography.displaySmall, color = MonoDesign.text,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
            )
        }
    }
}

@Composable
private fun MonoArtistActions(profile: ArtistProfile, state: LevyraUiState, viewModel: LevyraViewModel) {
    val strings = LocalLevyraStrings.current
    val songs = profile.topSongs
    val followed = (profile.browseId.isNotBlank() && profile.browseId in state.followedArtistKeys) || profile.name.trim().lowercase() in state.followedArtistKeys
    val excluded = state.artistExclusions.excludesBrowseId(profile.browseId) || state.artistExclusions.excludesArtistName(profile.name)
    val pinned = SpeedDial.artistKey(profile.name, profile.browseId)?.let { key -> state.speedDialPins.any { it.key == key } } == true
    var menuOpen by rememberSaveable(profile.browseId) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = MonoDesign.gutter, end = 8.dp, top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val playEnabled = songs.isNotEmpty()
        Box(
            Modifier.size(MonoArtistPlaySize).clip(CircleShape)
                .background(if (playEnabled) MonoDesign.accent else MonoDesign.raised)
                .clickable(enabled = playEnabled, role = Role.Button, onClickLabel = strings.play) { viewModel.playAll(songs) }
                .semantics { contentDescription = strings.play },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = if (playEnabled) MonoDesign.onAccent else MonoDesign.textFaint, modifier = Modifier.size(30.dp))
        }
        MonoArtistPill(
            if (followed) Icons.Rounded.HowToReg else Icons.Rounded.PersonAdd,
            if (followed) strings.followingArtist else strings.followArtist,
            selected = followed,
            enabled = true
        ) { viewModel.toggleFollowArtist() }
        MonoArtistPill(Icons.Rounded.Shuffle, strings.shuffle, selected = false, enabled = songs.size > 1) {
            val shuffled = songs.shuffled()
            shuffled.firstOrNull()?.let { first -> viewModel.playFrom(shuffled, first) }
        }
        Spacer(Modifier.weight(1f))
        Box {
            MonoSearchIconTarget(Icons.Rounded.MoreVert, strings.more, MonoDesign.textSecondary) { menuOpen = true }
            val speedDial = remember(strings) { strings.speedDialCopy() }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = MonoDesign.raised) {
                MonoArtistMenuItem(strings.continuousRadio, Icons.Rounded.Sensors, songs.isNotEmpty()) {
                    menuOpen = false
                    songs.firstOrNull()?.let(viewModel::startSongRadioFrom)
                }
                MonoArtistMenuItem(strings.addToQueue, Icons.AutoMirrored.Rounded.QueueMusic, songs.isNotEmpty()) {
                    menuOpen = false
                    viewModel.addTracksToQueue(songs)
                }
                MonoArtistMenuItem(if (excluded) strings.includeArtist else strings.excludeArtist, if (excluded) Icons.Rounded.Recommend else Icons.Rounded.DoNotDisturbOn, true) {
                    menuOpen = false
                    viewModel.toggleExcludeArtist(profile.browseId, profile.name)
                }
                MonoArtistMenuItem(if (pinned) speedDial.removeFromHome else speedDial.addToHome, Icons.Rounded.PushPin, true) {
                    menuOpen = false
                    viewModel.toggleSpeedDialArtist(profile.name, profile.browseId, profile.thumbnailUrl)
                }
            }
        }
    }
}

@Composable
private fun MonoArtistMenuItem(label: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = if (enabled) MonoDesign.text else MonoDesign.textFaint) },
        leadingIcon = { Icon(icon, null, tint = MonoDesign.textSecondary) },
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
private fun MonoArtistPill(icon: ImageVector, label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val tint = when {
        !enabled -> MonoDesign.textFaint
        selected -> MonoDesign.accent
        else -> MonoDesign.text
    }
    Row(
        Modifier.heightIn(min = 48.dp).clip(MonoDesign.pillShape)
            .border(1.dp, if (selected) MonoDesign.accent else MonoDesign.hairlineStrong, MonoDesign.pillShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun LazyListScope.monoArtistSections(
    profile: ArtistProfile,
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    copy: MonoSearchCopy,
    showAllSongs: Boolean,
    onToggleSongs: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val currentId = state.currentTrack?.id
    val topSongs = profile.topSongs.distinctBy(Track::id)
    if (topSongs.isNotEmpty()) {
        item(key = "mono-artist-popular-title", contentType = "mono-artist-header") {
            val strings = LocalLevyraStrings.current
            MonoSectionHeader(
                copy.popular, Modifier.padding(top = MonoDesign.sectionGap, bottom = 6.dp), overline = copy.popularOverline,
                action = if (showAllSongs) strings.showLess else strings.showMore,
                onAction = onToggleSongs.takeIf { topSongs.size > MonoArtistPreviewSongs }
            )
        }
        itemsIndexed(
            if (showAllSongs) topSongs else topSongs.take(MonoArtistPreviewSongs),
            key = { _, track -> "mono-top-${track.id}" },
            contentType = { _, _ -> "mono-artist-track" }
        ) { index, track ->
            val current = track.id == currentId
            MonoArtistTrackRow(
                index + 1, track, current, current && state.isPlaying, copy,
                { viewModel.playFrom(profile.topSongs, track) }, { onTrackActions(track) }
            )
        }
    }
    monoReleaseRail("albums", profile.albums + profile.compilations, profile, viewModel, copy.discographyOverline) { LocalLevyraStrings.current.albumsPlain }
    monoReleaseRail("singles", profile.singles, profile, viewModel, null) { LocalLevyraStrings.current.singlesAndEps }
    if (profile.videos.isNotEmpty()) {
        item(key = "mono-artist-videos", contentType = "mono-artist-rail") {
            val videos = remember(profile.videos) { profile.videos.distinctBy { it.id }.take(MonoArtistRailLimit) }
            Column {
                MonoArtistRailHeader(LocalLevyraStrings.current.video, copy.videosOverline)
                LazyRow(contentPadding = PaddingValues(horizontal = MonoDesign.gutter), horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
                    items(videos, key = { "mono-video-${it.id}" }) { video ->
                        MonoArtistVideoCard(video, video.id == currentId, { viewModel.playFrom(videos, video) }) { onTrackActions(video) }
                    }
                }
            }
        }
    }
    if (profile.relatedArtists.isNotEmpty()) {
        item(key = "mono-artist-related", contentType = "mono-artist-rail") {
            val related = remember(profile.relatedArtists) { profile.relatedArtists.distinctBy { it.browseId.ifBlank { it.name } }.take(MonoArtistRailLimit) }
            Column {
                MonoArtistRailHeader(copy.fansAlsoLike, copy.fansOverline)
                LazyRow(contentPadding = PaddingValues(horizontal = MonoDesign.gutter), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(related, key = { "mono-related-${it.browseId.ifBlank { it.name }}" }) { hit ->
                        MonoArtistAvatar(hit.name, hit.thumbnailUrl) { viewModel.openArtistFromHit(hit) }
                    }
                }
            }
        }
    }
    if (profile.featuredPlaylists.isNotEmpty()) {
        item(key = "mono-artist-featured", contentType = "mono-artist-rail") {
            val strings = LocalLevyraStrings.current
            val title = remember(strings.code) { searchDiscographyCopy(strings.code).featuredOn }
            val playlists = remember(profile.featuredPlaylists) { profile.featuredPlaylists.distinctBy { it.playlistId.ifBlank { it.browseId.ifBlank { it.title } } } }
            Column {
                MonoArtistRailHeader(title, null)
                LazyRow(contentPadding = PaddingValues(horizontal = MonoDesign.gutter), horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
                    items(playlists, key = { "mono-featured-${it.playlistId.ifBlank { it.browseId.ifBlank { it.title } }}" }) { playlist ->
                        MonoArtistCard(playlist.title, playlist.author, playlist.thumbnailUrl, MonoDesign.artShape) { viewModel.openPlaylistHit(playlist) }
                    }
                }
            }
        }
    }
    val bio = profile.biography?.text?.takeIf { it.isNotBlank() } ?: profile.bio
    val audience = listOf(profile.monthlyListeners, profile.subscribers).filter(String::isNotBlank).distinct()
    if (bio.isNotBlank() || audience.isNotEmpty()) {
        item(key = "mono-artist-about", contentType = "mono-artist-about") {
            Column {
                MonoArtistRailHeader(copy.about, copy.aboutOverline)
                MonoArtistAbout(profile.browseId, audience, bio)
            }
        }
    }
}

private fun LazyListScope.monoReleaseRail(
    key: String,
    releases: List<ArtistRelease>,
    profile: ArtistProfile,
    viewModel: LevyraViewModel,
    overline: String?,
    title: @Composable () -> String
) {
    if (releases.isEmpty()) return
    item(key = "mono-artist-$key", contentType = "mono-artist-rail") {
        val unique = remember(releases) { releases.distinctBy { it.browseId.ifBlank { it.title } } }
        Column {
            MonoArtistRailHeader(title(), overline)
            LazyRow(contentPadding = PaddingValues(horizontal = MonoDesign.gutter), horizontalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
                items(unique, key = { "mono-$key-${it.browseId.ifBlank { it.title }}" }) { release ->
                    MonoArtistCard(release.title, release.year, release.thumbnailUrl, MonoDesign.artShape) { viewModel.openArtistRelease(release, profile.name) }
                }
            }
        }
    }
}

@Composable
private fun MonoArtistRailHeader(title: String, overline: String?) {
    MonoSectionHeader(title, Modifier.padding(top = MonoDesign.sectionGap, bottom = MonoDesign.headerToContent), overline = overline)
}

@Composable
private fun MonoArtistArtwork(url: String, modifier: Modifier, shape: Shape) {
    Box(modifier.clip(shape).background(MonoDesign.surface).border(1.dp, MonoDesign.hairline, shape), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.MusicNote, null, tint = MonoDesign.textFaint, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun MonoArtistCard(title: String, subtitle: String, artworkUrl: String, shape: Shape, onClick: () -> Unit) {
    Column(Modifier.width(MonoArtistRelease).clip(MonoDesign.artShape).clickable(role = Role.Button, onClick = onClick)) {
        MonoArtistArtwork(artworkUrl, Modifier.size(MonoArtistRelease), shape)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonoArtistVideoCard(video: Track, current: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        Modifier.width(MonoArtistVideoWidth).clip(MonoDesign.artShape)
            .semantics { selected = current }
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MonoDesign.artShape).background(MonoDesign.surface)
                .border(1.dp, MonoDesign.hairline, MonoDesign.artShape)
        ) {
            CoverImage(video, Modifier.fillMaxSize(), highRes = true)
            Box(
                Modifier.align(Alignment.BottomStart).padding(8.dp).size(28.dp).clip(CircleShape).background(MonoDesign.void.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = MonoDesign.text, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(video.title, style = MaterialTheme.typography.titleSmall, color = if (current) MonoDesign.accent else MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (video.durationMs > 0L) {
            Text(formatSeekbarMillis(video.durationMs), style = MaterialTheme.typography.labelSmall, color = MonoDesign.textSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun MonoArtistAvatar(name: String, thumbnailUrl: String, onClick: () -> Unit) {
    Column(
        Modifier.width(MonoDesign.artistCircle).clip(MonoDesign.cardShape).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(MonoDesign.artistCircle).border(1.dp, MonoDesign.hairlineStrong, CircleShape).padding(3.dp)) {
            MonoArtistArtwork(thumbnailUrl, Modifier.fillMaxSize(), CircleShape)
        }
        Spacer(Modifier.height(8.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MonoArtistAbout(browseId: String, audience: List<String>, bio: String) {
    val strings = LocalLevyraStrings.current
    var expanded by rememberSaveable(browseId) { mutableStateOf(false) }
    Column(
        Modifier.padding(horizontal = MonoDesign.gutter).fillMaxWidth()
            .monoPanel(MonoDesign.panelShape)
            .then(if (bio.isNotBlank()) Modifier.clickable(role = Role.Button, onClickLabel = if (expanded) strings.showLess else strings.showMore) { expanded = !expanded } else Modifier)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        audience.forEach { line ->
            Text(line, style = MaterialTheme.typography.titleMedium, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (audience.isNotEmpty() && bio.isNotBlank()) {
            MonoWaveLine(playing = false, color = MonoDesign.hairlineStrong, strokeWidth = 1.dp, modifier = Modifier.fillMaxWidth().height(6.dp))
        }
        if (bio.isNotBlank()) {
            Text(bio, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary, maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
            MonoOverline(if (expanded) strings.showLess else strings.showMore, color = MonoDesign.accent)
        }
    }
}

@Composable
private fun MonoArtistTrackRow(
    rank: Int,
    track: Track,
    current: Boolean,
    playing: Boolean,
    copy: MonoSearchCopy,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val pulse = (1f - (rank - 1) * 0.085f).coerceIn(0.18f, 1f)
    Row(
        Modifier.fillMaxWidth().heightIn(min = MonoDesign.rowHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                selected = current
                if (current) stateDescription = strings.playing
            }
            .padding(start = MonoDesign.gutter, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(34.dp), contentAlignment = Alignment.CenterStart) {
            if (current) {
                MonoWaveLine(playing, Modifier.size(width = 22.dp, height = 14.dp).clearAndSetSemantics { }, MonoDesign.accent, cycles = 2.5f, strokeWidth = 1.5.dp)
            } else {
                Text(
                    rank.toString().padStart(2, '0'), color = MonoDesign.textFaint, maxLines = 1,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp, fontSize = 13.sp)
                )
            }
        }
        Box(Modifier.size(MonoDesign.rowArtwork).clip(MonoDesign.artShape).background(MonoDesign.surface)) {
            CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics { })
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title, style = MaterialTheme.typography.titleSmall, color = if (current) MonoDesign.accent else MonoDesign.text,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (track.explicit) MonoSearchExplicitMark(copy.explicit)
                Text(
                    track.album.ifBlank { track.artist }, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(
                Modifier.padding(top = 6.dp).fillMaxWidth(pulse * 0.6f).height(1.5.dp)
                    .clip(MonoDesign.pillShape).background(MonoArtistPulseBrush)
            )
        }
        if (track.durationMs > 0L) {
            Text(
                formatSeekbarMillis(track.durationMs), color = MonoDesign.textFaint, maxLines = 1,
                style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        MonoSearchIconTarget(Icons.Rounded.MoreVert, strings.more, MonoDesign.textSecondary, onMore)
    }
}
