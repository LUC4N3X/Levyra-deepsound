package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.*
import kotlin.math.abs

private val SpotifyBrowseColors = listOf(
    Color(0xFFE13300), Color(0xFF1E3264), Color(0xFF8D67AB), Color(0xFFE8115B),
    Color(0xFF148A08), Color(0xFFBA5D07), Color(0xFF27856A), Color(0xFF509BF5),
    Color(0xFFDC148C), Color(0xFF477D95), Color(0xFFAF2896), Color(0xFF0D73EC),
    Color(0xFF8C1932), Color(0xFF503750), Color(0xFF7D4B32), Color(0xFFE91429)
)

internal fun spotifyBrowseColor(seed: String): Color = SpotifyBrowseColors[abs(seed.hashCode()) % SpotifyBrowseColors.size]

@Composable
internal fun SpotifySectionTitle(title: String, inset: Dp, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = inset, end = (inset - 8.dp).coerceAtLeast(0.dp)).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, color = SpotifyDesign.text, style = MaterialTheme.typography.headlineSmall,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).semantics { heading() }
        )
        actions()
    }
}

@Composable
internal fun SpotifyPlayCircle(playing: Boolean, resolving: Boolean, size: Dp, modifier: Modifier = Modifier, fill: Color = SpotifyDesign.green) {
    Box(modifier.size(size).clip(CircleShape).background(fill), contentAlignment = Alignment.Center) {
        if (resolving) LevyraLoadingIndicator(color = Color.Black, modifier = Modifier.size(size * 0.42f))
        else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(size * 0.56f))
    }
}

@Composable
internal fun SpotifyAvatar(name: String, size: Dp, modifier: Modifier = Modifier) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "L"
    Box(modifier.size(size).clip(CircleShape).background(spotifyBrowseColor(name.ifBlank { "levyra" })), contentAlignment = Alignment.Center) {
        Text(initial, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.44f).sp, maxLines = 1)
    }
}

@Composable
internal fun SpotifyHomeHeader(hero: StudioHero?, inset: Dp) {
    val strings = LocalLevyraStrings.current
    if (hero == null) return
    Column(Modifier.fillMaxWidth()) {
        SpotifySectionTitle(hero.label, inset)
        SpotifyPickedCard(hero, strings.play, inset)
        Spacer(Modifier.height(8.dp))
    }
}

internal const val SpotifyOrbitTitle = "Your Orbit"

@Composable
internal fun SpotifyOrbitHeader(onPlayAll: (() -> Unit)?) {
    val strings = LocalLevyraStrings.current
    SpotifySectionTitle(SpotifyOrbitTitle, SpotifyDesign.gutter) {
        if (onPlayAll != null) Box(
            Modifier.heightIn(min = 48.dp).levyraPressable(onClick = onPlayAll, onClickLabel = strings.playAll, role = Role.Button).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(strings.playAll, color = SpotifyDesign.subdued, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
    }
}

@Composable
private fun SpotifyPickedCard(hero: StudioHero, playLabel: String, inset: Dp) {
    val playing = hero.isCurrent && hero.isPlaying
    Row(
        Modifier.padding(horizontal = inset).fillMaxWidth().height(152.dp).clip(RoundedCornerShape(SpotifyDesign.cornerLarge))
            .background(SpotifyDesign.highlight)
            .levyraPressable(onClick = hero.onOpen, onClickLabel = playLabel, role = Role.Button)
    ) {
        Box(Modifier.fillMaxHeight().aspectRatio(1f).background(SpotifyDesign.placeholder)) {
            if (hero.artistArtworkUrl.isNotBlank()) {
                AsyncImage(hero.artistArtworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                CoverImage(hero.track, Modifier.fillMaxSize(), highRes = true)
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(12.dp)) {
            Text(hero.track.artist, color = SpotifyDesign.text, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                hero.lead.ifBlank { hero.track.title }, color = SpotifyDesign.subdued, style = MaterialTheme.typography.bodySmall,
                maxLines = 3, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (hero.companionArtworkUrls.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    hero.companionArtworkUrls.take(3).forEach { url ->
                        StableRemoteArtwork(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(22.dp).clip(CircleShape).border(1.5.dp, SpotifyDesign.highlight, CircleShape))
                    }
                }
                Spacer(Modifier.weight(1f))
                SpotifyPlayCircle(playing, hero.isCurrent && hero.isResolving, 36.dp, fill = Color.White)
            }
        }
    }
}

@Composable
internal fun SpotifyShortcutGrid(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    val pages = remember(items) { items.chunked(8) }
    if (pages.size == 1) {
        SpotifyShortcutPage(pages.first(), isPlaying, modifier.padding(horizontal = inset))
        return
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val pageWidth = maxWidth - inset * 2
        LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(pages, key = { index, page -> "$index:${page.first().key}" }) { _, page ->
                SpotifyShortcutPage(page, isPlaying, Modifier.width(pageWidth))
            }
        }
    }
}

@Composable
private fun SpotifyShortcutPage(page: List<StudioShelfItem>, isPlaying: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        page.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item -> SpotifyShortcutTile(item, isPlaying, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpotifyShortcutTile(item: StudioShelfItem, isPlaying: Boolean, modifier: Modifier) {
    Row(
        modifier.height(SpotifyDesign.shortcutHeight).clip(SpotifyDesign.coverShape).background(SpotifyDesign.tinted)
            .levyraPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(SpotifyDesign.shortcutHeight).background(SpotifyDesign.placeholder)) {
            if (item.round) Box(Modifier.padding(4.dp).fillMaxSize().clip(CircleShape)) { item.artwork(Modifier.fillMaxSize(), true) }
            else item.artwork(Modifier.fillMaxSize(), false)
        }
        Text(
            item.title, color = if (item.current) SpotifyDesign.greenText else SpotifyDesign.text,
            style = MaterialTheme.typography.labelLarge, fontSize = 13.sp, lineHeight = 16.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        if (item.current) LevyraPlayingIndicator(playing = isPlaying, color = SpotifyDesign.green, size = 12.dp, modifier = Modifier.padding(end = 10.dp))
    }
}

@Composable
internal fun SpotifyCarousel(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(SpotifyDesign.gutter)) {
        itemsIndexed(items, key = { _, item -> item.key }) { _, item -> SpotifyCard(item, isPlaying) }
    }
}

@Composable
internal fun SpotifyCard(item: StudioShelfItem, isPlaying: Boolean, width: Dp = SpotifyDesign.cardSize) {
    Column(
        Modifier.width(width).levyraPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button),
        horizontalAlignment = if (item.round) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Box(Modifier.size(width).clip(if (item.round) CircleShape else SpotifyDesign.coverShape).background(SpotifyDesign.placeholder)) {
            item.artwork(Modifier.fillMaxSize(), item.round)
            if (item.current) Box(
                Modifier.align(if (item.round) Alignment.BottomCenter else Alignment.BottomEnd).padding(8.dp).size(32.dp).clip(CircleShape).background(SpotifyDesign.green),
                contentAlignment = Alignment.Center
            ) { LevyraPlayingIndicator(playing = isPlaying, color = Color.Black, size = 12.dp) }
        }
        Spacer(Modifier.height(if (item.round) 12.dp else 10.dp))
        Text(
            item.title, color = if (item.current) SpotifyDesign.greenText else SpotifyDesign.text,
            style = MaterialTheme.typography.titleSmall, textAlign = if (item.round) TextAlign.Center else TextAlign.Start,
            maxLines = if (item.round) 1 else 2, overflow = TextOverflow.Ellipsis,
            modifier = if (item.round) Modifier.fillMaxWidth() else Modifier
        )
        if (item.subtitle.isNotBlank() && !item.round) {
            Spacer(Modifier.height(4.dp))
            Text(item.subtitle, color = SpotifyDesign.subdued, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun SpotifyTrackCarousel(
    tracks: List<Track>, currentId: String?, playing: Boolean, resolving: Boolean,
    onPlay: (Track) -> Unit, onActions: (Track) -> Unit
) {
    val strings = LocalLevyraStrings.current
    LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = SpotifyDesign.gutter), horizontalArrangement = Arrangement.spacedBy(SpotifyDesign.gutter)) {
        itemsIndexed(tracks, key = { index, track -> "$index-${LevyraPersonalOrbit.identityKey(track)}" }) { _, track ->
            val current = track.id == currentId
            Column(
                Modifier.width(SpotifyDesign.cardSize).levyraPressable(
                    onClick = { onPlay(track) }, onLongClick = { onActions(track) },
                    onClickLabel = track.title, onLongClickLabel = strings.songOptions, role = Role.Button
                )
            ) {
                Box(Modifier.size(SpotifyDesign.cardSize).clip(SpotifyDesign.coverShape).background(SpotifyDesign.placeholder)) {
                    CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                    if (current) Box(
                        Modifier.align(Alignment.BottomEnd).padding(8.dp).size(32.dp).clip(CircleShape).background(SpotifyDesign.green),
                        contentAlignment = Alignment.Center
                    ) {
                        if (resolving) LevyraLoadingIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                        else LevyraPlayingIndicator(playing = playing, color = Color.Black, size = 12.dp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(track.title, color = if (current) SpotifyDesign.greenText else SpotifyDesign.text, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(track.artist, color = SpotifyDesign.subdued, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun SpotifyChartShelf(ranked: List<IndexedValue<Track>>, currentId: String?, playing: Boolean, inset: Dp, onPlay: (Track) -> Unit) {
    val pages = remember(ranked) { ranked.chunked(4) }
    LazyRow(contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(SpotifyDesign.gutter)) {
        itemsIndexed(pages, key = { index, _ -> "chart-page-$index" }) { _, page ->
            Column(Modifier.width(304.dp)) {
                page.forEach { (index, track) -> SpotifyRankedRow(index + 1, track, track.id == currentId, playing) { onPlay(track) } }
            }
        }
    }
}

@Composable
internal fun SpotifyRankedRow(rank: Int, track: Track, current: Boolean, playing: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).levyraPressable(onClick = onClick, onClickLabel = track.title, role = Role.Button),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
            if (current) LevyraPlayingIndicator(playing = playing, color = SpotifyDesign.green, size = 12.dp)
            else Text(rank.toString(), color = SpotifyDesign.text, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        }
        Box(Modifier.size(SpotifyDesign.rowCover).clip(RoundedCornerShape(2.dp)).background(SpotifyDesign.placeholder)) { CoverImage(track, Modifier.fillMaxSize()) }
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, color = if (current) SpotifyDesign.greenText else SpotifyDesign.text, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = SpotifyDesign.subdued, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun SpotifyMixCard(collection: StudioCollection, tracks: List<Track>, onOpen: (String) -> Unit) {
    SpotifyRadioCard(collection.title, collection.subtitle, collection.accent, tracks) { onOpen(collection.id) }
}

@Composable
internal fun SpotifyBrowseTile(
    title: String, artworkUrl: String, modifier: Modifier, active: Boolean? = null,
    onLongClick: (() -> Unit)? = null, onLongClickLabel: String? = null, onArtworkError: () -> Unit = {}, onClick: () -> Unit
) {
    Box(
        modifier.clip(SpotifyDesign.tileShape).background(spotifyBrowseColor(title))
            .then(if (active == true) Modifier.border(2.dp, Color.White, SpotifyDesign.tileShape) else Modifier)
            .then(if (active != null) Modifier.semantics { selected = active } else Modifier)
            .levyraPressable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = onLongClickLabel, onClickLabel = title, role = Role.Button)
    ) {
        if (artworkUrl.isNotBlank()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val art = (maxHeight * 0.66f).coerceAtMost(maxWidth * 0.5f)
                AsyncImage(
                    artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, onError = { onArtworkError() },
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = art * 0.18f, y = art * 0.06f).size(art)
                        .graphicsLayer { rotationZ = 25f; shadowElevation = 8f }.clip(RoundedCornerShape(SpotifyDesign.cornerSmall))
                )
            }
        }
        Text(
            title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(0.72f).padding(12.dp)
        )
    }
}

@Composable
internal fun SpotifyNowPlayingBar(
    track: Track, isPlaying: Boolean, isResolving: Boolean, progress: Float,
    onToggle: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier,
    onNext: (() -> Unit)? = null, nextEnabled: Boolean = true, nextLabel: String = ""
) {
    val strings = LocalLevyraStrings.current
    val surface by rememberSpotifyCoverTone(track, SpotifyDesign::miniSurface)
    Box(
        modifier.fillMaxWidth().height(SpotifyDesign.miniHeight).clip(RoundedCornerShape(SpotifyDesign.cornerLarge)).background(surface)
            .levyraPressable(onClick = onOpen, onClickLabel = strings.expandPlayer, role = Role.Button)
    ) {
        Row(Modifier.fillMaxSize().padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(SpotifyDesign.coverShape).background(SpotifyDesign.placeholder)) { CoverImage(track, Modifier.fillMaxSize()) }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(track.title, color = SpotifyDesign.text, style = MaterialTheme.typography.labelLarge, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = SpotifyDesign.subdued, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onToggle, enabled = !isResolving) {
                if (isResolving) LevyraLoadingIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isPlaying) strings.pause else strings.play, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            if (onNext != null) IconButton(onClick = onNext, enabled = nextEnabled) {
                Icon(Icons.Rounded.SkipNext, nextLabel.ifBlank { strings.next }, tint = if (nextEnabled) Color.White else SpotifyDesign.faint)
            }
        }
        if (progress >= 0f) Box(Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp).fillMaxWidth().height(2.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(Color.White))
        }
    }
}

@Composable
internal fun SpotifyMiniControls(playing: Boolean, resolving: Boolean, favorite: Boolean, onFavorite: () -> Unit, onToggle: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(Modifier.padding(end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .levyraPressable(onClick = onFavorite, role = Role.Switch, haptic = LevyraHapticAction.Confirm)
                .semantics {
                    contentDescription = if (favorite) strings.removeFromFavorites else strings.addToFavorites
                    toggleableState = ToggleableState(favorite)
                    role = Role.Switch
                },
            contentAlignment = Alignment.Center
        ) {
            if (favorite) Box(Modifier.size(24.dp).clip(CircleShape).background(SpotifyDesign.green), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            } else Icon(Icons.Outlined.AddCircleOutline, null, tint = SpotifyDesign.text, modifier = Modifier.size(28.dp))
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .levyraPressable(onClick = onToggle, enabled = !resolving, onClickLabel = if (playing) strings.pause else strings.play, role = Role.Button),
            contentAlignment = Alignment.Center
        ) {
            if (resolving) LevyraLoadingIndicator(color = Color.White, modifier = Modifier.size(22.dp))
            else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) strings.pause else strings.play, tint = SpotifyDesign.text, modifier = Modifier.size(32.dp))
        }
    }
}

internal val SpotifyLyricsPending = Color.Black.copy(alpha = 0.86f)

@Composable
internal fun SpotifyLyricsBackdrop(track: Track?) {
    if (track == null) {
        Box(Modifier.fillMaxSize().background(SpotifyDesign.highlight))
        return
    }
    val surface by rememberSpotifyCoverTone(track, SpotifyDesign::lyricsSurface)
    Box(Modifier.fillMaxSize().background(surface))
}

internal fun com.luc4n3x.levyra.domain.LevyraLyricsStudioSettings.spotifyLyricsApplies(): Boolean =
    LevyraIsSpotify && fontPreset == com.luc4n3x.levyra.domain.LevyraFontPreset.Outfit &&
        activeColorMode == com.luc4n3x.levyra.domain.LyricsActiveColorMode.WHITE &&
        backgroundMode == com.luc4n3x.levyra.domain.LyricsBackgroundMode.ORIGINAL

@Composable
internal fun rememberSpotifyCoverTone(track: Track, tone: (Color) -> Color): androidx.compose.runtime.State<Color> {
    val key = remember(track.id, track.thumbnailUrl, track.largeThumbnailUrl) {
        com.luc4n3x.levyra.data.ArtworkPaletteCache.key(track.id, track.thumbnailUrl, track.largeThumbnailUrl)
    }
    val fallback = remember(track.accentStart, track.accentEnd) { com.luc4n3x.levyra.data.ArtworkPalette(track.accentStart, track.accentEnd) }
    val palette by com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette(key, preferredPlayerArtworkUrl(track), fallback, retainPreviousUntilLoaded = true)
    val target = remember(palette, tone) { tone(Color(palette.start)) }
    return androidx.compose.animation.animateColorAsState(
        target, LevyraPlayerDesign.motion(LocalAnimationsEnabled.current, LevyraPlayerDesign.paletteTween()), label = "spotify-cover-tone"
    )
}

@Composable
internal fun SpotifyFollowPill(isFollowed: Boolean, followLabel: String, followingLabel: String, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp).levyraPressable(onClick = onClick, role = Role.Switch, haptic = LevyraHapticAction.Confirm)
            .clearAndSetSemantics {
                contentDescription = followLabel
                toggleableState = ToggleableState(isFollowed)
                role = Role.Switch
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (isFollowed) followingLabel else followLabel, color = SpotifyDesign.text,
            style = MaterialTheme.typography.labelLarge, maxLines = 1,
            modifier = Modifier.border(1.dp, if (isFollowed) SpotifyDesign.text else SpotifyDesign.faint, CircleShape).padding(horizontal = 16.dp, vertical = 7.dp)
        )
    }
}

@Composable
internal fun SpotifySettingsHeading(title: String) {
    Text(
        title, color = SpotifyDesign.text, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp).semantics { heading() }
    )
}

@Composable
internal fun SpotifyExploreTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(96.dp).clip(SpotifyDesign.tileShape).background(spotifyBrowseColor(label))
            .levyraPressable(onClick = onClick, onClickLabel = label, role = Role.Button)
    ) {
        Text(
            label, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(0.78f).padding(12.dp)
        )
        Icon(
            icon, null, tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 6.dp).size(64.dp).graphicsLayer { rotationZ = 25f }
        )
    }
}

@Composable
internal fun SpotifyReleaseCard(track: Track, title: String, subtitle: String, label: String, enabled: Boolean, onOpen: () -> Unit, actions: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(SpotifyDesign.cornerLarge)).background(SpotifyDesign.elevated)
            .levyraPressable(onClick = onOpen, enabled = enabled, onClickLabel = label, role = Role.Button).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(112.dp).clip(SpotifyDesign.coverShape).background(SpotifyDesign.placeholder)) { CoverImage(track, Modifier.fillMaxSize(), highRes = true) }
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, color = SpotifyDesign.subdued, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(title, color = SpotifyDesign.text, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = SpotifyDesign.subdued, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}

@Composable
internal fun SpotifyNameGreeting(greeting: String, modifier: Modifier = Modifier) {
    Text(
        greeting, color = SpotifyDesign.text, style = MaterialTheme.typography.headlineSmall,
        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier.semantics { heading() }
    )
}

@Composable
internal fun SpotifyBottomTabs(
    entries: List<LevyraTabEntry>, selected: com.luc4n3x.levyra.domain.LevyraTab, applySystemInset: Boolean, modifier: Modifier,
    onSelect: (LevyraTabEntry, Boolean) -> Unit
) {
    Column(
        modifier.fillMaxWidth().drawBehind {
            val reach = SpotifyDesign.miniHeight.toPx() + SpotifyDesign.dockGap.toPx() * 2
            drawRect(
                Brush.verticalGradient(
                    0f to Color.Transparent, 0.35f to SpotifyDesign.navScrim.copy(alpha = 0.45f),
                    0.7f to SpotifyDesign.navScrim, 1f to SpotifyDesign.navScrim,
                    startY = -reach, endY = size.height
                ),
                topLeft = Offset(0f, -reach), size = Size(size.width, size.height + reach)
            )
        }
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp).height(SpotifyDesign.navHeight - 6.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
            entries.forEach { entry ->
                val active = entry.tab == selected
                Column(
                    Modifier.weight(1f).fillMaxHeight().selectable(active, role = Role.Tab, onClick = { onSelect(entry, active) }),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (active) entry.selectedIcon else entry.unselectedIcon, null,
                        tint = if (active) SpotifyDesign.text else SpotifyDesign.subdued, modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        entry.label, color = if (active) SpotifyDesign.text else SpotifyDesign.subdued,
                        fontSize = 11.sp, lineHeight = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
        if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
    }
}
