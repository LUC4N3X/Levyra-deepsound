package com.luc4n3x.levyra.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.fullResolutionArtworkUrl
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.SpeedDial
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.atelier.AtelierAmbientBackdrop
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.AtelierCopy
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.media.AtelierExplicitMark
import com.luc4n3x.levyra.ui.media.AtelierMediaKey
import com.luc4n3x.levyra.ui.media.AtelierQuietAction
import com.luc4n3x.levyra.ui.media.AtelierRecordGlyph
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import kotlin.math.abs

private const val AtelierArtistPreviewSongs = 5
private const val AtelierArtistRelatedLimit = 24
private val AtelierArtistBarHeight = 56.dp
private val AtelierArtistPortraitMax = 520.dp
private const val AtelierArtistPortraitRatio = 1.1f
private const val AtelierArtistPortraitParallax = 0.45f
private val AtelierArtistSleeve = 150.dp
private const val AtelierShelfTilt = 22f
private const val AtelierShelfShrink = 0.1f
private val AtelierRelatedDiameters = listOf(112.dp, 84.dp, 100.dp, 76.dp, 92.dp)
private val AtelierRelatedLabelMin = 92.dp
private val AtelierFollowShape = RoundedCornerShape(11.dp)
private val AtelierPeekVinyl = Color(0xFF0C0B0D)
private val AtelierPeekGroove = Color.White.copy(alpha = 0.07f)

private fun atelierArtistNameSize(name: String): TextUnit = when {
    name.length <= 9 -> 68.sp
    name.length <= 14 -> 56.sp
    name.length <= 22 -> 46.sp
    else -> 36.sp
}

@Composable
internal fun AtelierArtistScreen(state: LevyraUiState, viewModel: LevyraViewModel, onTrackActions: (Track) -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
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
    BoxWithConstraints(Modifier.fillMaxSize().background(AtelierDesign.room)) {
        val portraitHeight = min(maxWidth * AtelierArtistPortraitRatio, AtelierArtistPortraitMax)
        val density = LocalDensity.current
        val portraitPx = with(density) { portraitHeight.toPx() }
        val barPx = with(density) { (statusTop + AtelierArtistBarHeight).toPx() }
        val collapsed by remember(listState, portraitPx, barPx) {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > portraitPx - barPx
            }
        }
        if (profile != null && portrait.isNotBlank()) {
            AtelierAmbientBackdrop(
                coverUrl = portrait,
                modifier = Modifier.fillMaxWidth().height(portraitHeight * 1.5f).graphicsLayer {
                    val travel = minOf(scrolled(), portraitPx)
                    alpha = 1f - travel / portraitPx.coerceAtLeast(1f)
                    translationY = -travel * 0.4f
                },
                strength = 1.15f
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = levyraTabContentBottomPadding(state.currentTrack != null) + 24.dp)
        ) {
            when {
                state.artistLoading && profile == null -> item(key = "atelier-artist-loading") {
                    Box(Modifier.fillMaxWidth().padding(top = statusTop + 200.dp), contentAlignment = Alignment.Center) {
                        LevyraLoadingIndicator(color = AtelierDesign.accent)
                    }
                }
                profile == null -> item(key = "atelier-artist-unavailable") {
                    Text(
                        strings.artistProfileUnavailable, style = MaterialTheme.typography.bodyLarge, color = AtelierDesign.textSecondary,
                        modifier = Modifier.padding(start = AtelierDesign.gutter, end = AtelierDesign.gutter, top = statusTop + 96.dp)
                    )
                }
                else -> {
                    item(key = "atelier-artist-header", contentType = "atelier-artist-header") {
                        AtelierArtistHeader(profile, state, viewModel, copy, portrait, portraitHeight, scrolled)
                    }
                    atelierArtistSections(profile, state, viewModel, copy, showAllSongs, { showAllSongs = !showAllSongs }, onTrackActions)
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(if (collapsed) AtelierDesign.cabinet else Color.Transparent).padding(top = statusTop)) {
            Row(Modifier.fillMaxWidth().height(AtelierArtistBarHeight).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                AtelierIconTarget(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, AtelierDesign.text, viewModel::closeArtist)
                Text(
                    if (collapsed) profile?.name.orEmpty() else "", style = MaterialTheme.typography.titleMedium, color = AtelierDesign.text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
            }
            if (collapsed) HorizontalDivider(thickness = 1.dp, color = AtelierDesign.hairline)
        }
    }
}

@Composable
private fun AtelierArtistHeader(
    profile: ArtistProfile,
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    copy: AtelierCopy,
    portrait: String,
    portraitHeight: Dp,
    scrolled: () -> Float
) {
    val strings = LocalLevyraStrings.current
    val animated = LocalAnimationsEnabled.current
    var expanded by rememberSaveable(profile.browseId) { mutableStateOf(false) }
    val bio = profile.biography?.text?.takeIf { it.isNotBlank() } ?: profile.bio
    val audience = profile.monthlyListeners.ifBlank { profile.subscribers }
    val nameSize = remember(profile.name) { atelierArtistNameSize(profile.name) }
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(portraitHeight)) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        if (animated) {
                            val travel = minOf(scrolled(), size.height)
                            val zoom = 1f + travel / size.height.coerceAtLeast(1f) * 0.06f
                            translationY = travel * AtelierArtistPortraitParallax
                            scaleX = zoom
                            scaleY = zoom
                        }
                    }
                    .drawWithCache {
                        val topShade = Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.5f), 0.22f to Color.Transparent)
                        val fade = Brush.verticalGradient(0f to Color.Black, 0.5f to Color.Black, 1f to Color.Transparent)
                        onDrawWithContent {
                            drawContent()
                            drawRect(topShade)
                            drawRect(fade, blendMode = BlendMode.DstIn)
                        }
                    }
            ) {
                if (portrait.isNotBlank()) {
                    AsyncImage(portrait, null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().background(AtelierDesign.panel), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Person, null, tint = AtelierDesign.textFaint, modifier = Modifier.size(72.dp))
                    }
                }
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = AtelierDesign.gutter)) {
                Text(
                    atelierUppercaseLabel(copy.artistLabel), color = AtelierDesign.accent,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp), maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    profile.name, color = AtelierDesign.text,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = nameSize, lineHeight = nameSize * 1.02f, fontWeight = FontWeight.Light, letterSpacing = (-1.2).sp
                    ),
                    maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter)) {
            if (audience.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    audience, style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.6.sp), color = AtelierDesign.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (bio.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    bio, style = MaterialTheme.typography.bodyLarge, color = AtelierDesign.textSecondary,
                    maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis
                )
                Box(
                    Modifier.heightIn(min = 48.dp).clip(AtelierDesign.cardShape).clickable(role = Role.Button) { expanded = !expanded }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(atelierUppercaseLabel(if (expanded) strings.showLess else strings.showMore), style = MaterialTheme.typography.labelMedium, color = AtelierDesign.accent)
                }
            }
            Spacer(Modifier.height(18.dp))
            AtelierArtistActions(profile, state, viewModel)
        }
    }
}

@Composable
private fun AtelierArtistActions(profile: ArtistProfile, state: LevyraUiState, viewModel: LevyraViewModel) {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    var menuOpen by rememberSaveable(profile.browseId) { mutableStateOf(false) }
    val followed = (profile.browseId.isNotBlank() && profile.browseId in state.followedArtistKeys) || profile.name.trim().lowercase() in state.followedArtistKeys
    val excluded = state.artistExclusions.excludesBrowseId(profile.browseId) || state.artistExclusions.excludesArtistName(profile.name)
    val pinned = SpeedDial.artistKey(profile.name, profile.browseId)?.let { key -> state.speedDialPins.any { it.key == key } } == true
    val songs = profile.topSongs
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        AtelierMediaKey(
            label = strings.play, icon = Icons.Rounded.PlayArrow, enabled = songs.isNotEmpty(), loading = false,
            onClick = { viewModel.playAll(songs) }, modifier = Modifier.weight(1f)
        )
        AtelierFollowKey(followed, Modifier.weight(1f)) { viewModel.toggleFollowArtist() }
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        AtelierQuietAction(Icons.Rounded.Shuffle, strings.shuffle, songs.size > 1, {
            val shuffled = songs.shuffled()
            shuffled.firstOrNull()?.let { first -> viewModel.playFrom(shuffled, first) }
        })
        AtelierQuietAction(Icons.Rounded.Sensors, strings.continuousRadio, songs.isNotEmpty(), {
            songs.firstOrNull()?.let(viewModel::startSongRadioFrom)
        })
        AtelierQuietAction(Icons.AutoMirrored.Rounded.QueueMusic, strings.addToQueue, songs.isNotEmpty(), { viewModel.addTracksToQueue(songs) })
        AtelierQuietAction(Icons.Rounded.Share, strings.share, true, {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, profile.name)
            }
            runCatching { context.startActivity(Intent.createChooser(intent, strings.shareVia)) }
        })
        Box {
            AtelierQuietAction(Icons.Rounded.MoreVert, strings.more, true, { menuOpen = true })
            val speedDialCopy = remember(strings) { strings.speedDialCopy() }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = AtelierDesign.panelRaised) {
                DropdownMenuItem(
                    text = { Text(if (excluded) strings.includeArtist else strings.excludeArtist) },
                    leadingIcon = { Icon(if (excluded) Icons.Rounded.Recommend else Icons.Rounded.DoNotDisturbOn, null) },
                    onClick = {
                        menuOpen = false
                        viewModel.toggleExcludeArtist(profile.browseId, profile.name)
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (pinned) speedDialCopy.removeFromHome else speedDialCopy.addToHome) },
                    leadingIcon = { Icon(Icons.Rounded.PushPin, null, tint = if (pinned) AtelierDesign.accentBright else AtelierDesign.text) },
                    onClick = {
                        menuOpen = false
                        viewModel.toggleSpeedDialArtist(profile.name, profile.browseId, profile.thumbnailUrl)
                    }
                )
            }
        }
    }
}

@Composable
private fun AtelierFollowKey(followed: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val label = if (followed) strings.followingArtist else strings.followArtist
    val tint = if (followed) AtelierDesign.accentBright else AtelierDesign.text
    Row(
        modifier.heightIn(min = 56.dp).clip(AtelierFollowShape)
            .border(1.dp, if (followed) AtelierDesign.accent.copy(alpha = 0.7f) else AtelierDesign.hairlineStrong, AtelierFollowShape)
            .levyraPressable(onClick = onToggle, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { selected = followed }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (followed) Icons.Rounded.HowToReg else Icons.Rounded.PersonAdd, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun LazyListScope.atelierArtistSections(
    profile: ArtistProfile,
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    copy: AtelierCopy,
    showAllSongs: Boolean,
    onToggleSongs: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val currentId = state.currentTrack?.id
    if (profile.topSongs.isNotEmpty()) {
        item(key = "atelier-artist-popular-title") {
            val strings = LocalLevyraStrings.current
            AtelierArtistSectionHeader(
                strings.popularTracks,
                if (showAllSongs) strings.showLess else strings.showMore,
                if (profile.topSongs.size > AtelierArtistPreviewSongs) onToggleSongs else null
            )
        }
        itemsIndexed(
            if (showAllSongs) profile.topSongs.distinctBy(Track::id) else profile.topSongs.distinctBy(Track::id).take(AtelierArtistPreviewSongs),
            key = { _, track -> "atelier-top-${track.id}" },
            contentType = { _, _ -> "atelier-artist-track" }
        ) { index, track ->
            val current = track.id == currentId
            AtelierArtistTrackRow(
                index + 1, track, current, current && state.isPlaying, copy,
                { viewModel.playFrom(profile.topSongs, track) }, { onTrackActions(track) }
            )
        }
    }
    atelierReleaseShelf("albums", profile.albums + profile.compilations, profile, viewModel) { LocalLevyraStrings.current.albumsPlain }
    atelierReleaseShelf("singles", profile.singles, profile, viewModel) { LocalLevyraStrings.current.singlesAndEps }
    if (profile.relatedArtists.isNotEmpty()) {
        item(key = "atelier-artist-related", contentType = "atelier-artist-related") {
            val related = remember(profile.relatedArtists) { profile.relatedArtists.distinctBy { it.browseId.ifBlank { it.name } }.take(AtelierArtistRelatedLimit) }
            Column {
                AtelierArtistSectionHeader(LocalLevyraStrings.current.similarArtists, null, null)
                LazyRow(
                    contentPadding = AtelierArtistShelfPadding,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(related, key = { _, hit -> "atelier-related-${hit.browseId.ifBlank { hit.name }}" }) { index, hit ->
                        val followed = (hit.browseId.isNotBlank() && hit.browseId in state.followedArtistKeys) || hit.name.trim().lowercase() in state.followedArtistKeys
                        AtelierRelatedArtistBubble(
                            hit.name, hit.thumbnailUrl, AtelierRelatedDiameters[index % AtelierRelatedDiameters.size], followed,
                            { viewModel.openArtistFromHit(hit) }, { viewModel.toggleFollowArtist(hit) }
                        )
                    }
                }
            }
        }
    }
    if (profile.featuredPlaylists.isNotEmpty()) {
        item(key = "atelier-artist-featured", contentType = "atelier-artist-shelf") {
            val strings = LocalLevyraStrings.current
            val title = remember(strings.code) { searchDiscographyCopy(strings.code).featuredOn }
            Column {
                AtelierArtistSectionHeader(title, null, null)
                LazyRow(contentPadding = AtelierArtistShelfPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                    items(profile.featuredPlaylists.distinctBy { it.playlistId.ifBlank { it.browseId.ifBlank { it.title } } }, key = { "atelier-featured-${it.playlistId.ifBlank { it.browseId.ifBlank { it.title } }}" }) { playlist ->
                        AtelierArtistShelfCard(playlist.title, playlist.author, playlist.thumbnailUrl) { viewModel.openPlaylistHit(playlist) }
                    }
                }
            }
        }
    }
}

private val AtelierArtistShelfPadding = PaddingValues(horizontal = AtelierDesign.gutter)

private fun LazyListScope.atelierReleaseShelf(
    key: String,
    releases: List<ArtistRelease>,
    profile: ArtistProfile,
    viewModel: LevyraViewModel,
    title: @Composable () -> String
) {
    if (releases.isEmpty()) return
    item(key = "atelier-artist-$key", contentType = "atelier-artist-shelf") {
        val unique = remember(releases) { releases.distinctBy { it.browseId.ifBlank { it.title } } }
        val rowState = rememberLazyListState()
        val animated = LocalAnimationsEnabled.current
        val towardEnd = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
        Column {
            AtelierArtistSectionHeader(title(), null, null)
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(start = AtelierDesign.gutter, end = AtelierDesign.gutter, top = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(unique, key = { _, release -> "atelier-$key-${release.browseId.ifBlank { release.title }}" }) { index, release ->
                    AtelierReleaseSleeve(
                        release.title, release.year, release.thumbnailUrl, towardEnd,
                        Modifier.atelierShelfPerspective(rowState, index, animated)
                    ) { viewModel.openArtistRelease(release, profile.name) }
                }
            }
        }
    }
}

private fun Modifier.atelierShelfPerspective(rowState: LazyListState, index: Int, enabled: Boolean): Modifier =
    if (!enabled) this else graphicsLayer {
        val info = rowState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return@graphicsLayer
        val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat().coerceAtLeast(1f)
        val center = info.viewportStartOffset + viewport / 2f
        val fraction = ((item.offset + item.size / 2f - center) / viewport).coerceIn(-1f, 1f)
        val shrink = 1f - abs(fraction) * AtelierShelfShrink
        cameraDistance = 10f * density
        rotationY = -fraction * AtelierShelfTilt
        scaleX = shrink
        scaleY = shrink
    }

@Composable
private fun AtelierReleaseSleeve(
    title: String,
    subtitle: String,
    artworkUrl: String,
    towardEnd: Float,
    perspective: Modifier,
    onClick: () -> Unit
) {
    Column(Modifier.width(AtelierArtistSleeve).levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)) {
        Box(perspective.fillMaxWidth().aspectRatio(1f)) {
            Spacer(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        translationX = towardEnd * size.width * 0.12f
                        scaleX = 0.94f
                        scaleY = 0.94f
                    }
                    .drawWithCache {
                        val r = size.minDimension / 2f
                        val groove = Stroke(0.6.dp.toPx())
                        onDrawBehind {
                            drawCircle(AtelierPeekVinyl, r)
                            drawCircle(AtelierPeekGroove, r * 0.82f, style = groove)
                            drawCircle(AtelierPeekGroove, r * 0.64f, style = groove)
                            drawCircle(AtelierDesign.accent, r * 0.3f)
                        }
                    }
            )
            AtelierArtistArtwork(
                artworkUrl,
                Modifier.fillMaxSize().shadow(10.dp, AtelierDesign.sleeveShape, clip = false, ambientColor = Color.Black, spotColor = Color.Black),
                AtelierDesign.sleeveShape
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = AtelierDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AtelierArtistSectionHeader(title: String, actionLabel: String?, onAction: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = 8.dp, top = AtelierDesign.sectionGap, bottom = 8.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            atelierUppercaseLabel(title), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp), color = AtelierDesign.accent,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        if (actionLabel != null && onAction != null) {
            Box(
                Modifier.heightIn(min = 48.dp).clip(AtelierDesign.cardShape).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text(atelierUppercaseLabel(actionLabel), style = MaterialTheme.typography.labelMedium, color = AtelierDesign.textSecondary) }
        }
    }
}

@Composable
private fun AtelierArtistArtwork(url: String, modifier: Modifier, shape: Shape) {
    Box(modifier.clip(shape).background(AtelierDesign.panel).border(1.dp, AtelierDesign.hairline, shape), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.MusicNote, null, tint = AtelierDesign.textFaint, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun AtelierArtistShelfCard(title: String, subtitle: String, artworkUrl: String, onClick: () -> Unit) {
    Column(Modifier.width(AtelierDesign.albumCard).clip(AtelierDesign.cardShape).clickable(role = Role.Button, onClick = onClick)) {
        AtelierArtistArtwork(artworkUrl, Modifier.fillMaxWidth().aspectRatio(1f), AtelierDesign.sleeveShape)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = AtelierDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AtelierRelatedArtistBubble(
    name: String,
    thumbnailUrl: String,
    diameter: Dp,
    followed: Boolean,
    onOpen: () -> Unit,
    onToggleFollow: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.width(max(diameter, AtelierRelatedLabelMin)), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.fillMaxWidth().clip(AtelierDesign.cardShape)
                .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(diameter).border(1.dp, if (followed) AtelierDesign.accent.copy(alpha = 0.7f) else AtelierDesign.hairlineStrong, CircleShape)
                    .padding(3.dp)
            ) {
                AtelierArtistArtwork(thumbnailUrl, Modifier.fillMaxSize(), CircleShape)
            }
            Spacer(Modifier.height(10.dp))
            Text(name, style = MaterialTheme.typography.titleSmall, color = AtelierDesign.text, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        AtelierIconTarget(
            if (followed) Icons.Rounded.HowToReg else Icons.Rounded.PersonAdd,
            if (followed) strings.followingArtist else strings.followArtist,
            if (followed) AtelierDesign.accent else AtelierDesign.textFaint,
            onToggleFollow
        )
    }
}

@Composable
private fun AtelierArtistTrackRow(
    rank: Int,
    track: Track,
    current: Boolean,
    playing: Boolean,
    copy: AtelierCopy,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).atelierBottomRule()
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .semantics {
                selected = current
                if (current) stateDescription = strings.playing
            }
            .padding(start = AtelierDesign.gutter, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) {
            if (current) {
                AtelierRecordGlyph(playing, Modifier.size(18.dp).clearAndSetSemantics {})
            } else {
                Text(
                    rank.toString(), color = AtelierDesign.textFaint, maxLines = 1,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Light, fontFeatureSettings = "tnum")
                )
            }
        }
        Box(Modifier.size(40.dp).clip(AtelierDesign.sleeveShape).background(AtelierDesign.panel)) {
            CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title, style = MaterialTheme.typography.titleSmall, color = if (current) AtelierDesign.accentBright else AtelierDesign.text,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (track.explicit) AtelierExplicitMark(copy.explicit)
                Text(
                    track.album.ifBlank { track.artist }, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (track.durationMs > 0L) {
            Text(
                formatSeekbarMillis(track.durationMs), color = AtelierDesign.textFaint, maxLines = 1,
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        AtelierIconTarget(Icons.Rounded.MoreVert, strings.more, AtelierDesign.textSecondary, onMore)
    }
}
