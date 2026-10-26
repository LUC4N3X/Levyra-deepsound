package com.luc4n3x.levyra.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import kotlin.math.absoluteValue

@Immutable
internal class AppleMusicCardModel(
    val key: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String,
    val explicit: Boolean = false,
    val circular: Boolean = false,
    val onClick: () -> Unit
)

@Immutable
internal class AppleMusicHeroModel(
    val key: String,
    val heading: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val imageUrl: String,
    val coverUrl: String,
    val backgroundColor: Color?,
    val onClick: () -> Unit
)

@Immutable
internal class AppleMusicChartCardModel(
    val key: String,
    val badge: String,
    val name: String,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
internal fun appleMusicViewportWidth(): Dp = LocalConfiguration.current.screenWidthDp.dp

@Composable
internal fun AppleMusicLargeTitle(title: String, moreLabel: String, onMore: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = AppleMusicDesign.gutter)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, style = MaterialTheme.typography.headlineLarge, color = AppleMusicDesign.label,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = moreLabel, onClick = onMore),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(AppleMusicDesign.raised)
                        .border(0.5.dp, AppleMusicDesign.chromeStroke, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MoreVert, moreLabel, tint = AppleMusicDesign.label, modifier = Modifier.size(20.dp))
                }
            }
        }
        HorizontalDivider(Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = AppleMusicDesign.opaqueSeparator)
    }
}

@Composable
internal fun AppleMusicSectionHeader(title: String, onSeeAll: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = AppleMusicDesign.gutter).heightIn(min = AppleMusicDesign.headerHeight)
            .then(if (onSeeAll != null) Modifier.clip(RoundedCornerShape(AppleMusicDesign.cornerS)).clickable(role = Role.Button, onClick = onSeeAll) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, style = MaterialTheme.typography.headlineSmall, color = AppleMusicDesign.label,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).semantics { heading() }
        )
        if (onSeeAll != null) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AppleMusicDesign.secondaryLabel,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
internal fun AppleMusicHeroCarousel(items: List<AppleMusicHeroModel>) {
    if (items.isEmpty()) return
    val width = AppleMusicDesign.heroWidth(appleMusicViewportWidth())
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
        contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
    ) {
        items(items, key = { it.key }) { hero -> AppleMusicHeroCard(hero, Modifier.width(width)) }
    }
}

@Composable
internal fun AppleMusicHeroCard(hero: AppleMusicHeroModel, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(AppleMusicDesign.cornerL)).clickable(role = Role.Button, onClick = hero.onClick)) {
        if (hero.heading.isNotBlank()) {
            Text(
                hero.heading.uppercase(), style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp, letterSpacing = 0.2.sp), color = AppleMusicDesign.secondaryLabel,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            hero.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            color = AppleMusicDesign.label, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            hero.subtitle, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            color = AppleMusicDesign.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(AppleMusicDesign.heroAspect)
                .clip(AppleMusicDesign.artworkShape)
                .background(hero.backgroundColor ?: AppleMusicDesign.placeholder)
        ) {
            if (hero.imageUrl.isNotBlank()) {
                StableRemoteArtwork(hero.imageUrl, hero.title, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true)
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))
                )
            )
            Row(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    hero.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 17.sp),
                    color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                if (hero.coverUrl.isNotBlank()) {
                    Spacer(Modifier.width(12.dp))
                    StableRemoteArtwork(
                        hero.coverUrl, null,
                        Modifier.size(58.dp).clip(RoundedCornerShape(AppleMusicDesign.cornerS)),
                        ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppleMusicSongColumns(
    tracks: List<Track>,
    rows: Int,
    currentId: String?,
    explicitLabel: String,
    onPlay: (Track) -> Unit,
    onMore: (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    val columns = remember(tracks, rows) { tracks.chunked(rows.coerceIn(1, 4)) }
    val width = AppleMusicDesign.songColumnWidth(appleMusicViewportWidth())
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
        contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
    ) {
        items(columns, key = { column -> column.first().id + ":" + column.size }) { column ->
            Column(Modifier.width(width)) {
                column.forEachIndexed { index, track ->
                    AppleMusicSongRow(track, track.id == currentId, explicitLabel, index < column.lastIndex, { onPlay(track) }, { onMore(track) })
                }
            }
        }
    }
}

@Composable
internal fun AppleMusicSongRow(
    track: Track,
    current: Boolean,
    explicitLabel: String,
    divider: Boolean,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    leading: (@Composable () -> Unit)? = null
) {
    Box(Modifier.fillMaxWidth().height(AppleMusicDesign.songRow)) {
        Row(
            Modifier.fillMaxSize().clickable(role = Role.Button, onClick = onPlay),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
            } else {
                Box(
                    Modifier.size(AppleMusicDesign.songArtwork).clip(AppleMusicDesign.thumbShape)
                        .background(AppleMusicDesign.placeholder)
                        .border(0.5.dp, AppleMusicDesign.artworkStroke, AppleMusicDesign.thumbShape)
                ) { CoverImage(track, Modifier.fillMaxSize()) }
            }
            Spacer(Modifier.width(AppleMusicDesign.itemGap))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        track.title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 20.sp),
                        color = if (current) AppleMusicDesign.accent else AppleMusicDesign.label,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                    if (track.explicit) AppleMusicExplicitBadge(explicitLabel, Modifier.padding(start = 6.dp))
                }
                Text(
                    track.artist, style = MaterialTheme.typography.bodySmall, color = AppleMusicDesign.secondaryLabel,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Box(Modifier.size(width = 38.dp, height = 48.dp).clickable(role = Role.Button, onClick = onMore), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MoreVert, null, tint = AppleMusicDesign.label, modifier = Modifier.size(20.dp))
            }
        }
        if (divider) {
            HorizontalDivider(
                Modifier.align(Alignment.BottomEnd).padding(start = AppleMusicDesign.songArtwork + AppleMusicDesign.itemGap),
                thickness = 0.5.dp, color = AppleMusicDesign.separator
            )
        }
    }
}

@Composable
internal fun AppleMusicExplicitBadge(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier.size(14.dp).clip(RoundedCornerShape(2.dp)).background(AppleMusicDesign.secondaryLabel)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text("E", color = AppleMusicDesign.background, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun AppleMusicCardCarousel(cards: List<AppleMusicCardModel>, rows: Int = 1, explicitLabel: String = "") {
    if (cards.isEmpty()) return
    val width = AppleMusicDesign.cardWidth(appleMusicViewportWidth())
    val columns = remember(cards, rows) { cards.chunked(rows.coerceIn(1, 2)) }
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
        contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
    ) {
        items(columns, key = { column -> column.first().key }) { column ->
            Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)) {
                column.forEach { card -> AppleMusicSquareCard(card, explicitLabel) }
            }
        }
    }
}

@Composable
internal fun AppleMusicSquareCard(card: AppleMusicCardModel, explicitLabel: String, modifier: Modifier = Modifier) {
    val shape = if (card.circular) CircleShape else AppleMusicDesign.artworkShape
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(AppleMusicDesign.cornerS)).clickable(role = Role.Button, onClick = card.onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(AppleMusicDesign.placeholder)
                .border(0.5.dp, AppleMusicDesign.artworkStroke, shape)
        ) {
            if (card.artworkUrl.isNotBlank()) {
                StableRemoteArtwork(card.artworkUrl, card.title, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true)
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                card.title, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 16.sp),
                color = AppleMusicDesign.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (card.explicit) AppleMusicExplicitBadge(explicitLabel, Modifier.padding(start = 4.dp))
        }
        if (card.subtitle.isNotBlank()) {
            Text(
                card.subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 16.sp),
                color = AppleMusicDesign.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun AppleMusicChartCarousel(cards: List<AppleMusicChartCardModel>) {
    if (cards.isEmpty()) return
    val width = AppleMusicDesign.cardWidth(appleMusicViewportWidth())
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
        contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
    ) {
        items(cards, key = { it.key }) { card -> AppleMusicChartCard(card, Modifier.width(width)) }
    }
}

@Composable
internal fun AppleMusicChartCard(card: AppleMusicChartCardModel, modifier: Modifier = Modifier) {
    val seed = remember(card.name) { card.name.hashCode().absoluteValue }
    val accent = AppleMusicDesign.accent
    Column(modifier.clip(RoundedCornerShape(AppleMusicDesign.cornerS)).clickable(role = Role.Button, onClick = card.onClick)) {
        Column(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(AppleMusicDesign.artworkShape).background(AppleMusicDesign.groupedBackground)
                .border(0.5.dp, AppleMusicDesign.artworkStroke, AppleMusicDesign.artworkShape)
                .padding(start = 9.dp, end = 9.dp, top = 6.dp, bottom = 9.dp)
        ) {
            Text(card.badge, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AppleMusicDesign.label, maxLines = 1)
            Text(
                card.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 19.sp),
                color = AppleMusicDesign.label, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Canvas(Modifier.fillMaxWidth().weight(1f)) {
                val columns = 6
                val rows = 4
                val cell = Size(size.width / columns, size.height / rows)
                for (row in 0 until rows) {
                    for (column in 0 until columns) {
                        val noise = ((seed shr (row * columns + column) % 24) and 0x7) / 7f
                        val fade = column.toFloat() / (columns - 1) * 0.7f
                        val alpha = (1f - fade - noise * 0.18f + row * 0.03f).coerceIn(0.12f, 1f)
                        drawRect(
                            accent.copy(alpha = alpha),
                            topLeft = Offset(column * cell.width, row * cell.height),
                            size = cell
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            card.title, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 16.sp),
            color = AppleMusicDesign.label, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            card.subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 16.sp),
            color = AppleMusicDesign.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun AppleMusicNotice(message: String, action: String?, onAction: (() -> Unit)?) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = AppleMusicDesign.gutter).clip(RoundedCornerShape(AppleMusicDesign.cornerL))
            .background(AppleMusicDesign.groupedBackground).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = AppleMusicDesign.secondaryLabel)
        if (action != null && onAction != null) {
            Text(
                action, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = AppleMusicDesign.accent,
                modifier = Modifier.clip(RoundedCornerShape(AppleMusicDesign.cornerS)).clickable(role = Role.Button, onClick = onAction).padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
internal fun AppleMusicPillButtons(
    playLabel: String,
    shuffleLabel: String,
    enabled: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AppleMusicPill(playLabel, Icons.Rounded.PlayArrow, enabled, onPlay, Modifier.weight(1f))
        AppleMusicPill(shuffleLabel, Icons.Rounded.Shuffle, enabled, onShuffle, Modifier.weight(1f))
    }
}

@Composable
private fun AppleMusicPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier.height(AppleMusicDesign.pillHeight).clip(AppleMusicDesign.pillShape).background(AppleMusicDesign.fill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = AppleMusicDesign.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium), color = AppleMusicDesign.accent, maxLines = 1)
    }
}

@Composable
internal fun AppleMusicAlbumTrackRow(
    index: Int,
    last: Boolean,
    title: String,
    subtitle: String,
    explicit: Boolean,
    current: Boolean,
    selected: Boolean,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.fillMaxWidth().heightIn(min = AppleMusicDesign.listRow)
            .background(if (selected) AppleMusicDesign.fill else Color.Transparent)
            .clickable(role = Role.Button, onClick = onPlay)
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = AppleMusicDesign.listRow).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${index + 1}", style = MaterialTheme.typography.bodyLarge, color = if (current) AppleMusicDesign.accent else AppleMusicDesign.secondaryLabel,
                maxLines = 1, modifier = Modifier.width(28.dp)
            )
            Column(Modifier.weight(1f).padding(start = 8.dp, top = 6.dp, bottom = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title, style = MaterialTheme.typography.bodyLarge, color = if (current) AppleMusicDesign.accent else AppleMusicDesign.label,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                    if (explicit) AppleMusicExplicitBadge(com.luc4n3x.levyra.ui.i18n.LevyraAppleMusicStrings.forCode(com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings.current.code).explicit, Modifier.padding(start = 6.dp))
                }
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = AppleMusicDesign.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(Modifier.size(width = 44.dp, height = AppleMusicDesign.listRow).clickable(role = Role.Button, onClick = onMore), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MoreVert, null, tint = AppleMusicDesign.label, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(6.dp))
        }
        if (!last) {
            HorizontalDivider(Modifier.align(Alignment.BottomEnd).padding(start = 52.dp, end = 16.dp), thickness = 0.5.dp, color = AppleMusicDesign.separator)
        }
    }
}

@Composable
internal fun AppleMusicCategoryTile(
    label: String,
    portrait: String,
    active: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    longClickLabel: String,
    onPortraitError: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    toned: Boolean = false
) {
    val seed = remember(label) { label.hashCode().absoluteValue }
    val fill = tint ?: remember(seed) { Color.hsv((seed % 360).toFloat(), 0.62f, 0.86f) }
    val grayscale = remember { androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }) }
    Box(
        modifier.aspectRatio(AppleMusicDesign.categoryTileAspect).clip(AppleMusicDesign.categoryTileShape).background(fill)
            .then(if (active) Modifier.border(2.dp, AppleMusicDesign.accent, AppleMusicDesign.categoryTileShape) else Modifier)
            .levyraPressable(onClick = onClick, onClickLabel = label, role = Role.Button, onLongClick = onLongClick, onLongClickLabel = longClickLabel.takeIf { onLongClick != null })
    ) {
        if (portrait.isNotBlank() && toned) {
            coil3.compose.AsyncImage(
                portrait, null, contentScale = ContentScale.Crop,
                onError = { onPortraitError() }, modifier = Modifier.fillMaxSize()
            )
        } else if (portrait.isNotBlank()) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(fill, blendMode = androidx.compose.ui.graphics.BlendMode.Multiply)
                    }
            ) {
                coil3.compose.AsyncImage(
                    portrait, null, contentScale = ContentScale.Crop, colorFilter = grayscale,
                    onError = { onPortraitError() }, modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            label, color = Color.White,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
        )
    }
}

@Composable
internal fun AppleMusicLinkRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = AppleMusicDesign.accent, modifier = Modifier.size(26.dp))
            Text(
                label, color = AppleMusicDesign.accent, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 14.dp)
            )
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AppleMusicDesign.tertiaryLabel, modifier = Modifier.size(24.dp))
        }
        HorizontalDivider(Modifier.align(Alignment.BottomEnd).padding(start = 40.dp), thickness = 0.5.dp, color = AppleMusicDesign.separator)
    }
}

@Composable
internal fun AppleMusicCountryChip(label: String, onClick: () -> Unit) {
    Row(
        Modifier.padding(start = AppleMusicDesign.gutter, top = 10.dp).clip(AppleMusicDesign.pillShape)
            .background(AppleMusicDesign.fill).clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 32.dp).padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = AppleMusicDesign.label, maxLines = 1)
        Icon(androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown, null, tint = AppleMusicDesign.secondaryLabel, modifier = Modifier.size(20.dp))
    }
}
