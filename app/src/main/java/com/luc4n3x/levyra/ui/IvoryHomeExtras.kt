package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import com.luc4n3x.levyra.ui.components.LevyraMediaTitle

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.discovery.DiscoveryLevelChipStyle
import com.luc4n3x.levyra.ui.discovery.HomeDiscoveryLevelChips
import com.luc4n3x.levyra.feature.discovery.DiscoveryFilmScores
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.discovery.LocalHomeDiscovery
import com.luc4n3x.levyra.ui.discovery.discoveryReasonLabel
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@Composable
internal fun IvoryCuratedPlaylists(viewModel: HomeViewModel, inset: Dp, fallback: @Composable () -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoHomeStrings.forCode(strings.code) }
    val curated = rememberCuratedPlaylistShelves(strings.code)
    val collections = rememberDefaultCuratedCollectionShelves(strings.code)
    val shelves = remember(curated, collections) { if (curated.isEmpty()) curated else curated + collections }
    if (shelves.isEmpty()) {
        fallback()
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shelves.forEachIndexed { index, shelf ->
            val tiles = remember(shelf.items) { shelf.items.distinctBy { it.key }.filter { it.playlist != null } }
            if (tiles.isEmpty()) return@forEachIndexed
            SkinSectionTitle(if (index == 0) copy.mixedForYou else shelf.title, inset) {
                if (index == 0) LocalCuratedPlaylistsOpener.current?.let { open -> IvoryShowAll(open) }
            }
            val rowState = rememberLazyListState()
            val motion = ivoryRailMotionEnabled()
            LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(tiles, key = { _, tile -> tile.key }, contentType = { _, _ -> "ivory-curated" }) { _, tile ->
                    Box(Modifier.ivoryRailItemMotion(motion, rowState, tile.key)) {
                        IvoryArtworkCard(tile.title, monoWithoutYoutube(tile.subtitle), tile.thumbnailUrl, 188.dp) { tile.playlist?.let(viewModel::openPlaylistHit) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun IvoryShowAll(onClick: () -> Unit) {
    val label = LocalLevyraStrings.current.showAll
    Box(
        Modifier.heightIn(min = 48.dp).clip(CircleShape).background(IvoryDesign.ink)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = IvoryDesign.onInk, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
}

@Composable
internal fun IvoryArtworkCard(title: String, subtitle: String, artwork: String, width: Dp, badge: String = "", onOpen: () -> Unit) {
    Column(
        Modifier.width(width).levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = listOf(title, badge, subtitle).filter(String::isNotBlank).joinToString(", ") },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(IvoryDesign.coverShape).background(LevyraPanel)) {
            AsyncImage(artwork, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
            if (badge.isNotBlank()) Text(
                badge, color = IvoryDesign.onInk, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(IvoryDesign.ink).padding(horizontal = 10.dp, vertical = 4.dp).clearAndSetSemantics { }
            )
            IvoryCornerBadge(52.dp, Modifier.align(Alignment.BottomEnd)) {
                Icon(Icons.Rounded.PlayArrow, null, tint = IvoryDesign.onInk, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(title, color = LevyraText, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { })
        if (subtitle.isNotBlank()) Text(subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { })
    }
}

@Composable
internal fun IvoryDiscoverTeaser(modifier: Modifier = Modifier) {
    val handle = LocalHomeDiscovery.current ?: return
    val ui by handle.state.collectAsStateWithLifecycle()
    LaunchedEffect(handle) { handle.onWarm() }
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { discoveryCopy(strings.code) }
    val music = remember(strings.code) { smartMusicCopy(strings.code) }
    val cards = remember(ui.deck) {
        val musical = ui.deck.cards.filterNot { DiscoveryFilmScores.matches(it.track) }.distinctBy { it.track.id }
        musical.drop(ui.deck.index.coerceAtMost(musical.size)).ifEmpty { musical }.take(6)
    }
    val animated = LocalAnimationsEnabled.current
    var turn by remember { mutableIntStateOf(0) }
    LaunchedEffect(animated, cards.size) {
        turn = 0
        if (!animated || cards.size < 2) return@LaunchedEffect
        while (true) {
            delay(3600L)
            turn += 1
        }
    }
    val lead = cards.getOrNull(turn.mod(cards.size.coerceAtLeast(1)))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(IvoryDesign.surfaceCorner)).background(LevyraPanel)
                .levyraPressable(onClick = handle.onOpen, role = Role.Button, onClickLabel = copy.start, pressedScale = LevyraPressScale.Surface)
                .semantics(mergeDescendants = true) { contentDescription = copy.title + ". " + copy.lead }
        ) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(128.dp).clip(IvoryDesign.coverShape).background(IvoryDesign.well).clearAndSetSemantics { }) {
                    Crossfade(targetState = lead?.track, animationSpec = tween(600), label = "ivory-discover-lead") { track: Track? ->
                        if (track != null) CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(copy.title, color = LevyraText, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Crossfade(targetState = lead, animationSpec = tween(400), label = "ivory-discover-reason") { card ->
                        Column {
                            Text(card?.let { discoveryReasonLabel(it, copy, music) } ?: copy.lead, color = IvoryDesign.ink, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            LevyraMediaTitle(card?.track?.let { "${it.title} · ${it.artist}" } ?: copy.lead, explicit = card?.track?.explicit == true, color = LevyraMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            IvoryInkTab(start = false, Modifier.align(Alignment.BottomEnd).padding(bottom = 14.dp).height(44.dp)) {
                Row(Modifier.padding(start = 18.dp, end = 20.dp).clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = IvoryDesign.onInk, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(copy.start, color = IvoryDesign.onInk, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
        HomeDiscoveryLevelChips(
            DiscoveryLevelChipStyle(
                shape = CircleShape,
                textStyle = MaterialTheme.typography.labelLarge,
                selectedContainer = IvoryDesign.ink,
                selectedContent = IvoryDesign.onInk,
                container = LevyraPanel,
                content = LevyraText
            )
        )
    }
}

@Composable
internal fun IvoryNewReleases(viewModel: HomeViewModel, inset: Dp, showFresh: Boolean, fallback: List<AlbumHit>, fallbackTitle: String) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoHomeStrings.forCode(strings.code) }
    val labels = remember(strings.code) { searchDiscographyCopy(strings.code) }
    val newThisWeek by viewModel.appleNewThisWeek.collectAsStateWithLifecycle()
    androidx.lifecycle.compose.LifecycleResumeEffect(strings.code, showFresh) {
        if (showFresh) viewModel.ensureAppleNewThisWeek()
        onPauseOrDispose { }
    }
    val fresh = remember(newThisWeek.albums, showFresh) { if (showFresh) newThisWeek.albums.distinctBy(::atelierAlbumKey) else emptyList() }
    val albums = remember(fresh, fallback) { fresh.ifEmpty { fallback.distinctBy(::atelierAlbumKey) } }
    if (albums.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SkinSectionTitle(if (fresh.isNotEmpty()) copy.newAlbums else fallbackTitle, inset)
        val rowState = rememberLazyListState()
        val motion = ivoryRailMotionEnabled()
        LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(albums, key = { _, album -> atelierAlbumKey(album) }, contentType = { _, _ -> "ivory-new-release" }) { _, album ->
                val badge = when (album.releaseType) {
                    ReleaseType.Album, ReleaseType.Compilation -> labels.album
                    ReleaseType.Single -> labels.single
                    ReleaseType.Ep -> "EP"
                    ReleaseType.Unknown -> ""
                }
                Box(Modifier.ivoryRailItemMotion(motion, rowState, atelierAlbumKey(album))) {
                    IvoryArtworkCard(album.title, album.artist, album.thumbnailUrl, 164.dp, badge) { viewModel.openAlbum(album) }
                }
            }
        }
    }
}
