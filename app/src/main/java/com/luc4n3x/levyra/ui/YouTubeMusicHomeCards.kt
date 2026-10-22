package com.luc4n3x.levyra.ui

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign

private val CommunityCardShape = RoundedCornerShape(16.dp)

internal class YouTubeMusicCommunityActions(
    val requestPreview: (String) -> Unit,
    val play: (List<Track>, Track?) -> Unit,
    val radio: (List<Track>) -> Unit,
    val open: (YoutubeMusicHomeItem) -> Unit,
    val trackActions: (Track) -> Unit
)

@Composable
internal fun YouTubeMusicCommunityCarousel(
    items: List<YoutubeMusicHomeItem>,
    previews: Map<String, List<Track>>,
    explicitLabel: String,
    labels: Triple<String, String, String>,
    actions: YouTubeMusicCommunityActions
) {
    val width = (youTubeMusicViewportWidth() - YouTubeMusicDesign.gutter * 2 - 24.dp).coerceIn(280.dp, 420.dp)
    LazyRow(
        contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
    ) {
        items(items, key = { it.key }) { item ->
            val playlistId = item.playlist?.playlistId.orEmpty()
            LaunchedEffect(playlistId) { actions.requestPreview(playlistId) }
            YouTubeMusicCommunityCard(item, previews[playlistId].orEmpty(), width, explicitLabel, labels, actions)
        }
    }
}

@Composable
private fun YouTubeMusicCommunityCard(
    item: YoutubeMusicHomeItem,
    tracks: List<Track>,
    width: Dp,
    explicitLabel: String,
    labels: Triple<String, String, String>,
    actions: YouTubeMusicCommunityActions
) {
    val seed = tracks.firstOrNull()?.accentStart?.let(::Color)
    val fill = remember(seed) { seed?.let { YouTubeMusicDesign.playerField(it) } ?: YouTubeMusicDesign.surface }
    Column(
        Modifier.width(width).clip(CommunityCardShape)
            .background(Brush.verticalGradient(listOf(fill, YouTubeMusicDesign.playerFieldDeep(fill))))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().levyraPressable(onClick = { actions.open(item) }, role = Role.Button, pressedScale = LevyraPressScale.Surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            YouTubeMusicMosaic(item.thumbnailUrl, tracks.take(4), 100.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.subtitle, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Column {
            tracks.take(3).forEach { track ->
                YouTubeMusicSongRow(
                    track = track, subtitle = track.artist, current = false, explicitLabel = explicitLabel,
                    onClick = { actions.play(tracks, track) }, onMore = { actions.trackActions(track) },
                    compact = true, modifier = Modifier.offset(x = (-16).dp).fillMaxWidth()
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            YouTubeMusicRoundAction(Icons.Rounded.PlayArrow, labels.first, filled = true, enabled = tracks.isNotEmpty()) { actions.play(tracks, null) }
            YouTubeMusicRoundAction(Icons.Rounded.Podcasts, labels.second, filled = false, enabled = tracks.isNotEmpty()) { actions.radio(tracks) }
            YouTubeMusicRoundAction(Icons.Outlined.BookmarkBorder, labels.third, filled = false, enabled = true) { actions.open(item) }
        }
    }
}

@Composable
private fun YouTubeMusicMosaic(fallbackUrl: String, tracks: List<Track>, size: Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(4.dp)).background(YouTubeMusicDesign.placeholder)) {
        if (tracks.size >= 4) {
            Column {
                tracks.chunked(2).take(2).forEach { pair ->
                    Row {
                        pair.forEach { track -> Box(Modifier.size(size / 2)) { CoverImage(track, Modifier.fillMaxSize()) } }
                    }
                }
            }
        } else if (fallbackUrl.isNotBlank()) {
            StableRemoteArtwork(fallbackUrl, null, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true)
        }
    }
}

@Composable
internal fun YouTubeMusicRoundAction(icon: ImageVector, label: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape)
            .background(if (filled) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .border(1.dp, if (filled) Color.Transparent else Color.White.copy(alpha = 0.32f), CircleShape)
            .levyraPressable(onClick = onClick, enabled = enabled, role = Role.Button, onClickLabel = label, pressedScale = LevyraPressScale.Control),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@androidx.compose.runtime.Immutable
internal class YouTubeMusicDiscoveryCard(
    val key: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String,
    val caption: String,
    val track: Track?
)

internal fun YoutubeMusicHomeItem.toDiscoveryCard(): YouTubeMusicDiscoveryCard = YouTubeMusicDiscoveryCard(
    key = key, title = title, subtitle = subtitle, artworkUrl = thumbnailUrl,
    caption = track?.album?.takeIf { it.isNotBlank() && it != "YouTube Music" }.orEmpty(), track = track
)

@Composable
internal fun YouTubeMusicDiscoveryCarousel(cards: List<YouTubeMusicDiscoveryCard>, onOpen: (Track) -> Unit, onLongClick: (Track) -> Unit) {
    val width = (youTubeMusicViewportWidth() - YouTubeMusicDesign.gutter * 2 - YouTubeMusicDesign.itemGap - 2.dp).coerceIn(260.dp, 420.dp)
    val shape = RoundedCornerShape(YouTubeMusicDesign.discoveryCorner)
    LazyRow(
        contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
    ) {
        items(cards, key = { it.key }) { card ->
            val track = card.track
            Box(
                Modifier.size(width).clip(shape).background(YouTubeMusicDesign.placeholder)
                    .levyraPressable(
                        onClick = { track?.let(onOpen) }, onLongClick = { track?.let(onLongClick) },
                        role = Role.Button, pressedScale = 1f
                    )
            ) {
                if (card.artworkUrl.isNotBlank()) {
                    StableRemoteArtwork(card.artworkUrl, card.title, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true)
                } else if (track != null) {
                    CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.6f), 0.3f to Color.Black.copy(alpha = 0.34f),
                            0.7f to Color.Black.copy(alpha = 0.3f), 1f to Color.Black.copy(alpha = 0.58f)
                        )
                    )
                )
                Column(Modifier.align(Alignment.TopStart).padding(start = 18.dp, end = 18.dp, top = 16.dp)) {
                    Text(card.title, color = YouTubeMusicDesign.text, fontSize = 18.sp, lineHeight = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (card.subtitle.isNotBlank()) {
                        Text(card.subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp, lineHeight = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (card.caption.isNotBlank()) {
                    Text(
                        card.caption, color = Color.White, fontSize = 16.sp, lineHeight = 20.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 18.dp, bottom = 16.dp)
                    )
                }
            }
        }
    }
}

private const val SpeedGridColumns = 3
private const val SpeedGridRows = 3
private val SpeedGridGap = 5.dp
private val SpeedGridCorner = 4.dp
private val SpeedGridShape = RoundedCornerShape(SpeedGridCorner)
private val SpeedGridEndInset = 22.dp

@Composable
internal fun YouTubeMusicSpeedGrid(
    title: String,
    listener: HomeListener,
    tracks: List<Track>,
    currentId: String?,
    onPlay: (Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val pages = remember(tracks) { tracks.chunked(SpeedGridColumns * SpeedGridRows) }
    if (pages.isEmpty()) return
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pages.size })
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = YouTubeMusicDesign.gutter).padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val name = listener.name.trim()
            val initial = remember(name) { homePersonalOrbitInitial(name) }
            if (initial != null || listener.avatarUrl.isNotBlank()) {
                HomeOrbitAvatar(initial.orEmpty(), listener.photoPath, listener.photoVersion, listener.avatarUrl)
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                if (name.isNotBlank()) {
                    Text(
                        name.uppercase(), color = YouTubeMusicDesign.textSecondary, fontSize = 16.sp, lineHeight = 20.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(start = YouTubeMusicDesign.gutter, end = SpeedGridEndInset),
            pageSpacing = YouTubeMusicDesign.gutter + SpeedGridEndInset,
            key = { index -> pages[index].first().id + ":" + index }
        ) { index ->
            Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(SpeedGridGap)) {
                pages[index].chunked(SpeedGridColumns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(SpeedGridGap)) {
                        row.forEach { track ->
                            YouTubeMusicSpeedTile(track, track.id == currentId, { onPlay(track) }, { onTrackActions(track) }, Modifier.weight(1f))
                        }
                        repeat(SpeedGridColumns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (pages.size > 1) {
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                repeat(pages.size) { page ->
                    Box(
                        Modifier.size(8.dp).clip(CircleShape)
                            .background(if (page == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

@Composable
private fun YouTubeMusicSpeedTile(track: Track, current: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier.aspectRatio(1f)
            .then(if (current) Modifier.drawBehind {
                val inset = 4.dp.toPx()
                val stroke = 3.dp.toPx()
                val corner = SpeedGridCorner.toPx() + inset
                drawRoundRect(
                    Color.White, topLeft = Offset(-inset, -inset), size = Size(size.width + inset * 2, size.height + inset * 2),
                    cornerRadius = CornerRadius(corner, corner), style = Stroke(stroke)
                )
            } else Modifier)
            .clip(SpeedGridShape).background(YouTubeMusicDesign.placeholder)
            .levyraPressable(onClick = onClick, onLongClick = onLongClick, role = Role.Button, pressedScale = 1f)
    ) {
        CoverImage(track, Modifier.fillMaxSize(), highRes = true)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.62f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f))
            )
        )
        if (current) YouTubeMusicNowPlayingBars(Modifier.align(Alignment.Center))
        Text(
            track.title, color = Color.White, fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 10.dp, bottom = 8.dp)
        )
    }
}
