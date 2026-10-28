package com.luc4n3x.levyra.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify

@Immutable
internal class YouTubeMusicCardModel(
    val key: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String,
    val circular: Boolean = false,
    val wide: Boolean = false,
    val track: Track? = null,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null
)

@Immutable
internal class YouTubeMusicSongRowModel(
    val key: String,
    val track: Track,
    val subtitle: String,
    val onClick: () -> Unit,
    val onMore: () -> Unit
)

@Composable
internal fun youTubeMusicViewportWidth(): Dp = LocalConfiguration.current.screenWidthDp.dp

@Composable
internal fun YouTubeMusicBrandMark(title: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(com.luc4n3x.levyra.R.drawable.levyra_logo),
            contentDescription = null,
            modifier = Modifier.size(34.dp).graphicsLayer {
                scaleX = 1.9f
                scaleY = 1.9f
            }
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title, color = YouTubeMusicDesign.text, fontSize = 27.sp, lineHeight = 30.sp,
            fontWeight = FontWeight.Bold, letterSpacing = (-1.0).sp, maxLines = 1
        )
    }
}

@Composable
internal fun YouTubeMusicTopBar(
    title: String,
    searchLabel: String,
    settingsLabel: String,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().height(YouTubeMusicDesign.topBarHeight).padding(start = YouTubeMusicDesign.gutter, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        YouTubeMusicBrandMark(title, Modifier.weight(1f))
        trailing?.invoke(this)
        YouTubeMusicIconAction(Icons.Outlined.Search, searchLabel, onSearch)
        Box(
            Modifier.size(48.dp).clip(CircleShape).levyraPressable(onClick = onSettings, role = Role.Button, onClickLabel = settingsLabel),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(YouTubeMusicDesign.avatar).clip(CircleShape).background(YouTubeMusicDesign.surfaceHigh), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Settings, settingsLabel, tint = YouTubeMusicDesign.text, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun YouTubeMusicIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = YouTubeMusicDesign.text,
    iconSize: Dp = 24.dp
) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).levyraPressable(onClick = onClick, role = Role.Button, onClickLabel = label, pressedScale = LevyraPressScale.Control),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
internal fun YouTubeMusicChipRow(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (labels.isEmpty()) return
    LazyRow(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.chipGap)
    ) {
        items(labels.size, key = { index -> "chip-$index-${labels[index]}" }) { index ->
            YouTubeMusicChip(labels[index], index == selected) { onSelect(index) }
        }
    }
}

@Composable
internal fun YouTubeMusicChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(YouTubeMusicDesign.chipHeight).clip(YouTubeMusicDesign.chipShape)
            .background(if (selected) Color.White else YouTubeMusicDesign.chipFill)
            .border(1.dp, if (selected) Color.Transparent else Color.White.copy(alpha = 0.04f), YouTubeMusicDesign.chipShape)
            .levyraPressable(onClick = onClick, role = Role.Tab, pressedScale = LevyraPressScale.Control)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, color = if (selected) Color.Black else YouTubeMusicDesign.text,
            style = MaterialTheme.typography.labelMedium, maxLines = 1
        )
    }
}

@Composable
internal fun YouTubeMusicSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    strapline: String = "",
    thumbnailUrl: String = "",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = YouTubeMusicDesign.gutter).heightIn(min = 48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (thumbnailUrl.isNotBlank()) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(YouTubeMusicDesign.placeholder)) {
                StableRemoteArtwork(thumbnailUrl, null, Modifier.fillMaxSize(), ContentScale.Crop)
            }
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            if (strapline.isNotBlank()) {
                Text(
                    strapline.uppercase(), color = YouTubeMusicDesign.textTranslucent, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.headlineMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
            )
        }
        if (actionLabel != null && onAction != null) {
            Box(
                Modifier.padding(start = 12.dp).height(36.dp).clip(YouTubeMusicDesign.pillShape)
                    .border(1.dp, YouTubeMusicDesign.outline, YouTubeMusicDesign.pillShape)
                    .levyraPressable(onClick = onAction, role = Role.Button, pressedScale = LevyraPressScale.Control)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(actionLabel, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
        if (onOpen != null) {
            YouTubeMusicIconAction(Icons.AutoMirrored.Rounded.KeyboardArrowRight, title, onOpen, iconSize = 28.dp)
        }
    }
}

@Composable
internal fun YouTubeMusicExplicitBadge(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier.size(14.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)).background(YouTubeMusicDesign.textSecondary)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text("E", color = YouTubeMusicDesign.background, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun YouTubeMusicSongRow(
    track: Track,
    subtitle: String,
    current: Boolean,
    explicitLabel: String,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    background: Color = Color.Transparent,
    onLongClick: () -> Unit = onMore,
    selectionActive: Boolean = false,
    selected: Boolean = false
) {
    val artwork = if (compact) YouTubeMusicDesign.compactArtwork else YouTubeMusicDesign.listArtwork
    Row(
        modifier.fillMaxWidth().height(if (compact) YouTubeMusicDesign.compactRow else YouTubeMusicDesign.listRow)
            .background(if (selected) YouTubeMusicDesign.surfaceHigh else background)
            .then(if (selectionActive) Modifier.semantics { this.selected = selected } else Modifier)
            .levyraPressable(onClick = onClick, onLongClick = onLongClick, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .padding(start = YouTubeMusicDesign.gutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
        } else {
            Box(Modifier.size(artwork).clip(YouTubeMusicDesign.thumbShape).background(YouTubeMusicDesign.placeholder)) {
                CoverImage(track, Modifier.fillMaxSize())
                if (current) YouTubeMusicNowPlayingBars(Modifier.align(Alignment.Center))
            }
        }
        Spacer(Modifier.width(YouTubeMusicDesign.gutter))
        Column(Modifier.weight(1f)) {
            Text(
                track.title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.explicit) YouTubeMusicExplicitBadge(explicitLabel, Modifier.padding(end = 6.dp))
                Text(
                    subtitle, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selectionActive) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = YouTubeMusicDesign.text)
            }
        } else if (trailing != null) {
            trailing()
        } else {
            YouTubeMusicIconAction(Icons.Rounded.MoreVert, explicitLabel, onMore, iconSize = 24.dp)
        }
    }
}

@Composable
internal fun YouTubeMusicNowPlayingBars(modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp)) {
        val bar = size.width / 5f
        listOf(0.55f, 1f, 0.7f).forEachIndexed { index, level ->
            val x = bar * (index * 2f) + bar * 0.5f
            drawRect(Color.White, Offset(x, size.height * (1f - level)), androidx.compose.ui.geometry.Size(bar, size.height * level))
        }
    }
}

@Composable
internal fun YouTubeMusicSongColumns(
    rows: List<YouTubeMusicSongRowModel>,
    currentId: String?,
    explicitLabel: String,
    rowsPerColumn: Int = 4
) {
    if (rows.isEmpty()) return
    val columns = remember(rows, rowsPerColumn) { rows.chunked(rowsPerColumn.coerceIn(1, 4)) }
    val width = YouTubeMusicDesign.quickPickColumnWidth(youTubeMusicViewportWidth())
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(columns, key = { column -> column.first().key + ":" + column.size }) { column ->
            Column(Modifier.width(width)) {
                column.forEach { row ->
                    YouTubeMusicSongRow(row.track, row.subtitle, row.track.id == currentId, explicitLabel, row.onClick, row.onMore)
                }
            }
        }
    }
}

@Composable
internal fun YouTubeMusicCardCarousel(cards: List<YouTubeMusicCardModel>, modifier: Modifier = Modifier) {
    if (cards.isEmpty()) return
    LazyRow(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
    ) {
        items(cards, key = { it.key }) { card -> YouTubeMusicCard(card) }
    }
}

@Composable
internal fun YouTubeMusicCard(card: YouTubeMusicCardModel, modifier: Modifier = Modifier, width: Dp = if (card.wide) 300.dp else YouTubeMusicDesign.cardArtwork) {
    val shape = if (card.circular) CircleShape else YouTubeMusicDesign.cardShape
    val artHeight = if (card.wide) width * 9f / 16f else width
    Column(
        modifier.width(width).levyraPressable(onClick = card.onClick, onLongClick = card.onLongClick, role = Role.Button, pressedScale = LevyraPressScale.Surface)
    ) {
        Box(Modifier.size(width, artHeight).clip(shape).background(YouTubeMusicDesign.placeholder)) {
            val track = card.track
            if (card.artworkUrl.isNotBlank()) {
                StableRemoteArtwork(card.artworkUrl, card.title, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true)
            } else if (track != null) {
                CoverImage(track, Modifier.fillMaxSize(), highRes = true)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            card.title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleMedium,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = if (card.circular) Modifier.fillMaxWidth() else Modifier
        )
        if (card.subtitle.isNotBlank()) {
            Text(
                card.subtitle, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun YouTubeMusicHomeBackdrop(seed: Color?, modifier: Modifier = Modifier, height: Dp = 420.dp) {
    val top = remember(seed) { seed?.let { YouTubeMusicDesign.sheetField(it) } ?: YouTubeMusicDesign.surface }
    Box(
        modifier.fillMaxWidth().height(height).background(
            Brush.verticalGradient(listOf(top.copy(alpha = 0.92f), top.copy(alpha = 0.45f), YouTubeMusicDesign.background))
        )
    )
}

@Composable
internal fun YouTubeMusicNotice(message: String, action: String?, onAction: (() -> Unit)?) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = YouTubeMusicDesign.gutter, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(message, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge)
        if (action != null && onAction != null) {
            Box(
                Modifier.height(36.dp).clip(YouTubeMusicDesign.pillShape).background(Color.White)
                    .levyraPressable(onClick = onAction, role = Role.Button)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) { Text(action, color = Color.Black, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
internal fun YouTubeMusicMoodTile(
    label: String,
    stripe: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        modifier.height(56.dp).clip(YouTubeMusicDesign.chipShape).background(YouTubeMusicDesign.surface)
            .levyraPressable(onClick = onClick, onLongClick = onLongClick, role = Role.Button, pressedScale = LevyraPressScale.Surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.fillMaxHeight().width(6.dp).background(stripe))
        Text(
            label, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Normal),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 18.dp)
        )
    }
}

@Composable
internal fun YouTubeMusicShortcutTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.height(YouTubeMusicDesign.tileHeight).clip(YouTubeMusicDesign.tileShape).background(YouTubeMusicDesign.surface)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(icon, null, tint = YouTubeMusicDesign.text, modifier = Modifier.size(22.dp))
        Text(
            label, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 21.sp),
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun SkinAlbumTrackRow(
    index: Int,
    last: Boolean,
    track: Track,
    subtitle: String,
    current: Boolean,
    selected: Boolean,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier
): Boolean {
    if (com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic) {
        AppleMusicAlbumTrackRow(index, last, track.title, subtitle, track.explicit, current, selected, onPlay, onMore, modifier)
        return true
    }
    if (!com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic) return false
    val detail = listOf(subtitle, com.luc4n3x.levyra.ui.components.formatSeekbarMillis(track.durationMs).takeIf { track.durationMs > 0L }.orEmpty())
        .filter(String::isNotBlank).joinToString(" • ")
    Row(
        modifier.fillMaxWidth().height(YouTubeMusicDesign.compactRow)
            .background(if (selected) YouTubeMusicDesign.surface else Color.Transparent)
            .levyraPressable(onClick = onPlay, onLongClick = onMore, role = Role.Button, pressedScale = LevyraPressScale.Row),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            if (current) YouTubeMusicNowPlayingBars()
            else Text((index + 1).toString(), color = YouTubeMusicDesign.text, style = MaterialTheme.typography.bodyLarge)
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(track.title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.explicit) YouTubeMusicExplicitBadge("E", Modifier.padding(end = 6.dp))
                    Text(detail, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        YouTubeMusicIconAction(Icons.Rounded.MoreVert, track.title, onMore)
    }
    return true
}

@Composable
internal fun SkinFollowPill(isFollowed: Boolean, followLabel: String, followingLabel: String, onClick: () -> Unit): Boolean {
    if (LevyraIsSpotify) {
        SpotifyFollowPill(isFollowed, followLabel, followingLabel, onClick)
        return true
    }
    if (!com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic) return false
    Box(
        Modifier.height(40.dp).clip(YouTubeMusicDesign.pillShape)
            .background(if (isFollowed) Color.Transparent else Color.White)
            .border(1.dp, if (isFollowed) YouTubeMusicDesign.outline else Color.Transparent, YouTubeMusicDesign.pillShape)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (isFollowed) followingLabel else followLabel,
            color = if (isFollowed) Color.White else Color.Black,
            style = MaterialTheme.typography.labelLarge, maxLines = 1
        )
    }
    return true
}
