package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraMediaTitle

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
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
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.TorqueStripShape
import com.luc4n3x.levyra.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

internal val TorqueCuratedWidth = 212.dp
internal val TorqueCuratedHeight = 276.dp
private const val TorqueDiscoverHoldMillis = 3400L
private const val TorqueDiscoverSegments = 8

@Composable
private fun TorqueTag(text: String, background: Color = LevyraBlack, content: Color = Color.White, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), color = content, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp, maxLines = 1,
        modifier = modifier.background(background).padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
internal fun TorqueCuratedPlaylists(viewModel: HomeViewModel, inset: Dp, fallback: @Composable () -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoHomeStrings.forCode(strings.code) }
    val curated = rememberCuratedPlaylistShelves(strings.code)
    val shelves = curated
    if (shelves.isEmpty()) {
        fallback()
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shelves.forEachIndexed { index, shelf ->
            val tiles = remember(shelf.items) { shelf.items.distinctBy { it.key }.filter { it.playlist != null } }
            if (tiles.isEmpty()) return@forEachIndexed
            SkinSectionTitle(if (index == 0) copy.mixedForYou else shelf.title, inset) {
                if (index == 0) LocalCuratedPlaylistsOpener.current?.let { open -> TorqueShowAll(open) }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(tiles, key = { _, tile -> tile.key }, contentType = { _, _ -> "torque-curated" }) { position, tile ->
                    TorqueCuratedCard(position + 1, tile.title, monoWithoutYoutube(tile.subtitle), tile.thumbnailUrl) {
                        tile.playlist?.let(viewModel::openPlaylistHit)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TorqueCuratedCard(
    rank: Int,
    title: String,
    subtitle: String,
    artwork: String,
    modifier: Modifier = Modifier.size(TorqueCuratedWidth, TorqueCuratedHeight),
    onOpen: () -> Unit
) {
    Box(
        modifier.background(LevyraPanel)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = listOf(title, subtitle).filter(String::isNotBlank).joinToString(", ") }
    ) {
        AsyncImage(artwork, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clearAndSetSemantics { })
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 0.7f to Color.Black.copy(alpha = 0.55f))))
        Box(Modifier.fillMaxSize().clip(TorqueStripShape).background(LevyraActivePalette.blue))
        Box(Modifier.align(Alignment.TopStart).padding(10.dp)) { TorqueRank(rank, highlight = rank == 1) }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp).clearAndSetSemantics { }) {
            Text(
                title.uppercase(), color = Color.White, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun TorqueDiscoverTeaser(modifier: Modifier = Modifier) {
    val handle = LocalHomeDiscovery.current ?: return
    val ui by handle.state.collectAsStateWithLifecycle()
    LaunchedEffect(handle) { handle.onWarm() }
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { discoveryCopy(strings.code) }
    val music = remember(strings.code) { smartMusicCopy(strings.code) }
    val overline = remember(strings.code) { LevyraMonoHomeStrings.forCode(strings.code).forYouOverline }
    val cards = remember(ui.deck) {
        val musical = ui.deck.cards.filterNot { DiscoveryFilmScores.matches(it.track) }.distinctBy { it.track.id }
        musical.drop(ui.deck.index.coerceAtMost(musical.size)).ifEmpty { musical }.take(TorqueDiscoverSegments)
    }
    val animated = LocalAnimationsEnabled.current
    var turn by remember { mutableIntStateOf(0) }
    LaunchedEffect(animated, cards.size) {
        turn = 0
        if (!animated || cards.size < 2) return@LaunchedEffect
        while (true) {
            delay(TorqueDiscoverHoldMillis)
            turn += 1
        }
    }
    val leadIndex = turn.mod(cards.size.coerceAtLeast(1))
    val lead = cards.getOrNull(leadIndex)
    val accent = LevyraActivePalette.blue
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 196.dp).height(IntrinsicSize.Min).background(LevyraPanel)
                .levyraPressable(onClick = handle.onOpen, role = Role.Button, onClickLabel = copy.start, pressedScale = LevyraPressScale.Surface)
                .semantics(mergeDescendants = true) { contentDescription = copy.title + ". " + copy.lead }
        ) {
            Box(Modifier.fillMaxHeight().width(150.dp).background(LevyraPanelSoft).clearAndSetSemantics { }) {
                Crossfade(targetState = lead?.track, animationSpec = tween(600), label = "torque-discover-lead") { track: Track? ->
                    if (track != null) CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                }
                Box(Modifier.fillMaxSize().clip(TorqueStripShape).background(accent))
                Text(
                    (leadIndex + 1).toString().padStart(2, '0') + "/" + cards.size.coerceAtLeast(1).toString().padStart(2, '0'),
                    color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 20.dp)
                )
                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(cards.size.coerceAtLeast(1)) { index ->
                        Box(Modifier.weight(1f).height(3.dp).background(if (index <= leadIndex) Color.White else Color.White.copy(alpha = 0.35f)))
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)) {
                TorqueTag(overline)
                Spacer(Modifier.height(10.dp))
                Text(
                    copy.title.uppercase(), color = LevyraText, fontSize = 20.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Crossfade(targetState = lead, animationSpec = tween(400), label = "torque-discover-reason") { card ->
                    Column {
                        Text(
                            (card?.let { discoveryReasonLabel(it, copy, music) } ?: copy.lead).uppercase(), color = accent,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        LevyraMediaTitle(
                            card?.track?.let { "${it.title} · ${it.artist}" } ?: copy.lead, color = LevyraMuted, fontSize = 13.sp,
                            explicit = card?.track?.explicit == true,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
                Row(
                    Modifier.clip(TorqueTagShape).background(accent).padding(horizontal = 18.dp, vertical = 9.dp).clearAndSetSemantics { },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        copy.start.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        HomeDiscoveryLevelChips(
            DiscoveryLevelChipStyle(
                shape = TorqueTagShape,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                selectedContainer = accent,
                selectedContent = Color.White,
                container = LevyraPanel,
                content = LevyraText,
                uppercase = true
            )
        )
    }
}

@Composable
internal fun TorqueNewReleases(viewModel: HomeViewModel, inset: Dp, showFresh: Boolean, fallback: List<com.luc4n3x.levyra.domain.AlbumHit>, fallbackTitle: String) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoHomeStrings.forCode(strings.code) }
    val labels = remember(strings.code) { com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy(strings.code) }
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
        LazyRow(contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(albums, key = { _, album -> atelierAlbumKey(album) }, contentType = { _, _ -> "torque-new-release" }) { _, album ->
                val badge = when (album.releaseType) {
                    com.luc4n3x.levyra.domain.ReleaseType.Album, com.luc4n3x.levyra.domain.ReleaseType.Compilation -> labels.album
                    com.luc4n3x.levyra.domain.ReleaseType.Single -> labels.single
                    com.luc4n3x.levyra.domain.ReleaseType.Ep -> "EP"
                    com.luc4n3x.levyra.domain.ReleaseType.Unknown -> ""
                }
                TorqueReleaseTile(album.title, album.artist, album.thumbnailUrl, badge, album.releaseType == com.luc4n3x.levyra.domain.ReleaseType.Single) { viewModel.openAlbum(album) }
            }
        }
    }
}

@Composable
internal fun TorqueReleaseTile(title: String, artist: String, artwork: String, badge: String, single: Boolean, onOpen: () -> Unit) {
    Column(
        Modifier.width(164.dp)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = listOf(title, badge, artist).filter(String::isNotBlank).joinToString(", ") }
    ) {
        Box(Modifier.size(164.dp).background(LevyraPanel)) {
            AsyncImage(artwork, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
            if (badge.isNotBlank()) Text(
                badge.uppercase(), color = if (single) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, maxLines = 1,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(TorqueTagShape)
                    .background(if (single) Color.White else LevyraActivePalette.blue).padding(horizontal = 12.dp, vertical = 4.dp).clearAndSetSemantics { }
            )
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(LevyraActivePalette.blue))
        Text(
            title.uppercase(), color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp).clearAndSetSemantics { }
        )
        Text(artist, color = LevyraMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { })
    }
}
