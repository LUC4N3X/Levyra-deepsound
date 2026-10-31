package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.levyraShimmerSweep
import com.luc4n3x.levyra.ui.components.rememberLevyraShimmerProgress
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign

private const val AppleComposedSongRows = 4
private const val AppleComposedChartPreview = 20
private const val AppleComposedSkeletonCards = 4
private const val AppleComposedWideAspect = 16f / 9f
private val AppleComposedMinTarget = 48.dp

internal class AppleMusicComposedKit(
    private val explicitLabel: String,
    private val onOpenCharts: () -> Unit
) : SkinHomeKit {
    @Composable
    override fun Songs(
        sectionKey: String,
        title: String,
        tracks: List<Track>,
        ranked: Boolean,
        currentId: String?,
        onPlay: (Track) -> Unit,
        onPlayAll: () -> Unit,
        onTrackActions: (Track) -> Unit
    ) {
        val unique = remember(tracks) { tracks.distinctBy { it.id } }
        if (unique.isEmpty() || title.isBlank()) return
        Column {
            if (ranked) {
                AppleMusicSectionHeader(title, onOpenCharts, Modifier.heightIn(min = AppleComposedMinTarget))
                AppleMusicRankedColumns(unique.take(AppleComposedChartPreview), currentId, explicitLabel, onPlay, onTrackActions)
            } else {
                AppleMusicSectionHeader(title)
                AppleMusicSongColumns(unique, AppleComposedSongRows, currentId, explicitLabel, onPlay, onTrackActions)
            }
        }
    }

    @Composable
    override fun Cards(title: String, cards: List<ComposedCard>) {
        val unique = remember(cards) { cards.distinctBy { it.key } }
        if (unique.isEmpty()) return
        Column {
            AppleMusicSectionHeader(title)
            if (unique.any { it.wide }) {
                AppleMusicWideCarousel(unique, explicitLabel)
            } else {
                AppleMusicCardCarousel(remember(unique) { unique.map { it.toAppleCard() } }, explicitLabel = explicitLabel)
            }
        }
    }

    @Composable
    override fun Chips(labels: List<String>, onSelect: (Int) -> Unit) {
        if (labels.isEmpty()) return
        LazyRow(
            contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(labels, key = { index, label -> "chip-$index-$label" }) { index, label ->
                AppleMusicComposedChip(label) { onSelect(index) }
            }
        }
    }

    @Composable
    override fun Hero(hero: StudioHero) {
        AppleMusicSoundtrackSection(hero)
    }

    @Composable
    override fun Quotes(title: String, quotes: List<ComposedQuote>) {
        val unique = remember(quotes) { quotes.distinctBy { it.key } }
        if (unique.isEmpty()) return
        val width = AppleMusicDesign.songColumnWidth(appleMusicViewportWidth())
        val showMeta = remember(unique) { unique.any { it.meta.isNotBlank() } }
        val listState = rememberLazyListState()
        Column {
            AppleMusicSectionHeader(title)
            LazyRow(
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
                contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
                horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
            ) {
                items(unique, key = { it.key }) { quote ->
                    AppleMusicQuoteCard(quote, showMeta, Modifier.width(width))
                }
            }
        }
    }

    @Composable
    override fun Discovery(model: ComposedDiscovery) {
        val cards = remember(model.cards) { model.cards.distinctBy { it.key } }
        Column {
            AppleMusicSectionHeader(model.title, model.onOpen, Modifier.heightIn(min = AppleComposedMinTarget))
            AppleMusicDiscoveryLead(model, cards.firstOrNull())
            val rest = remember(cards) { cards.drop(1).map { it.toAppleCard() } }
            if (rest.isNotEmpty()) {
                Spacer(Modifier.height(AppleMusicDesign.itemGap))
                AppleMusicCardCarousel(rest, explicitLabel = explicitLabel)
            } else if (model.loading && cards.isEmpty()) {
                Spacer(Modifier.height(AppleMusicDesign.itemGap))
                AppleMusicSkeletonCarousel()
            }
        }
    }

    @Composable
    override fun Notice(message: String, actionLabel: String?, onAction: (() -> Unit)?) {
        AppleMusicNotice(message, actionLabel, onAction)
    }

    @Composable
    override fun LoadingShelf(title: String) {
        val progress = rememberLevyraShimmerProgress()
        Column {
            if (title.isNotBlank()) {
                AppleMusicSectionHeader(title)
            } else {
                Box(
                    Modifier.padding(horizontal = AppleMusicDesign.gutter).heightIn(min = AppleMusicDesign.headerHeight),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AppleMusicSkeletonLine(Modifier.width(148.dp).height(18.dp), progress)
                }
            }
            AppleMusicSkeletonCarousel(progress)
        }
    }
}

private fun ComposedCard.appleArtworkUrl(): String =
    artworkUrl.ifBlank { track?.let { it.largeThumbnailUrl.ifBlank { it.thumbnailUrl } }.orEmpty() }

private fun ComposedCard.toAppleCard(): AppleMusicCardModel =
    AppleMusicCardModel(key, title, subtitle, appleArtworkUrl(), explicit, circular, onClick)

@Composable
private fun AppleMusicRankedColumns(
    tracks: List<Track>,
    currentId: String?,
    explicitLabel: String,
    onPlay: (Track) -> Unit,
    onMore: (Track) -> Unit
) {
    val columns = remember(tracks) { tracks.chunked(AppleComposedSongRows) }
    val width = AppleMusicDesign.songColumnWidth(appleMusicViewportWidth())
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
        contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
    ) {
        itemsIndexed(columns, key = { index, _ -> "rank-column-$index" }) { columnIndex, column ->
            Column(Modifier.width(width)) {
                column.forEachIndexed { index, track ->
                    val rank = columnIndex * AppleComposedSongRows + index + 1
                    AppleMusicSongRow(
                        track, track.id == currentId, explicitLabel, index < column.lastIndex,
                        { onPlay(track) }, { onMore(track) },
                        leading = { AppleMusicRankedLeading(track, rank) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleMusicRankedLeading(track: Track, rank: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(AppleMusicDesign.songArtwork).clip(AppleMusicDesign.thumbShape)
                .background(AppleMusicDesign.placeholder)
                .border(0.5.dp, AppleMusicDesign.artworkStroke, AppleMusicDesign.thumbShape)
        ) { CoverImage(track, Modifier.fillMaxSize()) }
        Text(
            "$rank", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = AppleMusicDesign.label, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp)
        )
    }
}

@Composable
private fun AppleMusicWideCarousel(cards: List<ComposedCard>, explicitLabel: String) {
    val width = AppleMusicDesign.heroWidth(appleMusicViewportWidth())
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
        contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
    ) {
        items(cards, key = { it.key }) { card -> AppleMusicWideCard(card, explicitLabel, width) }
    }
}

@Composable
private fun AppleMusicWideCard(card: ComposedCard, explicitLabel: String, width: Dp) {
    val artwork = card.appleArtworkUrl()
    Column(
        Modifier.width(width).clip(RoundedCornerShape(AppleMusicDesign.cornerS))
            .levyraPressable(onClick = card.onClick, onLongClick = card.onLongClick, role = Role.Button, pressedScale = 1f)
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(AppleComposedWideAspect).clip(AppleMusicDesign.artworkShape)
                .background(AppleMusicDesign.placeholder)
                .border(0.5.dp, AppleMusicDesign.artworkStroke, AppleMusicDesign.artworkShape)
        ) {
            if (artwork.isNotBlank()) {
                StableRemoteArtwork(artwork, card.title, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true)
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
private fun AppleMusicComposedChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = AppleComposedMinTarget).clip(AppleMusicDesign.pillShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.clip(AppleMusicDesign.pillShape).background(AppleMusicDesign.fill)
                .heightIn(min = 34.dp).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = AppleMusicDesign.label, maxLines = 1)
        }
    }
}

@Composable
private fun AppleMusicQuoteCard(quote: ComposedQuote, showMeta: Boolean, modifier: Modifier) {
    val playLabel = LocalLevyraStrings.current.play
    Column(
        modifier.clip(RoundedCornerShape(AppleMusicDesign.cornerL)).background(AppleMusicDesign.groupedBackground)
            .clickable(role = Role.Button, onClick = quote.onOpen).padding(16.dp)
    ) {
        Text(
            quote.quote, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
            color = AppleMusicDesign.label, minLines = 4, maxLines = 4, overflow = TextOverflow.Ellipsis
        )
        if (showMeta) {
            Text(
                quote.meta, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 16.sp),
                color = AppleMusicDesign.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(Modifier.height(AppleMusicDesign.itemGap))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(AppleMusicDesign.songArtwork).clip(AppleMusicDesign.thumbShape)
                    .background(AppleMusicDesign.placeholder)
                    .border(0.5.dp, AppleMusicDesign.artworkStroke, AppleMusicDesign.thumbShape)
            ) { CoverImage(quote.track, Modifier.fillMaxSize()) }
            Spacer(Modifier.width(AppleMusicDesign.itemGap))
            Column(Modifier.weight(1f)) {
                Text(
                    quote.track.title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 20.sp),
                    color = AppleMusicDesign.label, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    quote.track.artist, style = MaterialTheme.typography.bodySmall, color = AppleMusicDesign.secondaryLabel,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                Modifier.size(AppleComposedMinTarget).clip(CircleShape).clickable(role = Role.Button, onClickLabel = playLabel, onClick = quote.onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, playLabel, tint = AppleMusicDesign.accent, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun AppleMusicDiscoveryLead(model: ComposedDiscovery, lead: ComposedCard?) {
    val artwork = lead?.appleArtworkUrl().orEmpty()
    val hasArtwork = artwork.isNotBlank()
    val progress = rememberLevyraShimmerProgress(active = model.loading && lead == null)
    Box(
        Modifier.padding(horizontal = AppleMusicDesign.gutter).fillMaxWidth().aspectRatio(AppleMusicDesign.heroAspect)
            .clip(AppleMusicDesign.artworkShape).background(AppleMusicDesign.placeholder)
            .border(0.5.dp, AppleMusicDesign.artworkStroke, AppleMusicDesign.artworkShape)
            .clickable(role = Role.Button, onClick = model.onOpen)
            .levyraShimmerSweep(progress, AppleMusicDesign.label)
    ) {
        if (hasArtwork) {
            StableRemoteArtwork(artwork, lead?.title, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Transparent, 0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.68f))
                )
            )
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp)) {
            Text(
                model.lead, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 17.sp),
                color = if (hasArtwork) Color.White else AppleMusicDesign.label,
                maxLines = 3, overflow = TextOverflow.Ellipsis
            )
            AppleMusicDiscoveryStart(model.startLabel, hasArtwork, model.onOpen)
        }
    }
}

@Composable
private fun AppleMusicDiscoveryStart(label: String, onArtwork: Boolean, onClick: () -> Unit) {
    val container = if (onArtwork) Color.White else AppleMusicDesign.accent
    val content = if (onArtwork) Color.Black else Color.White
    Box(
        Modifier.heightIn(min = AppleComposedMinTarget).clip(AppleMusicDesign.pillShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier.clip(AppleMusicDesign.pillShape).background(container).heightIn(min = 34.dp)
                .padding(start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = content, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AppleMusicSkeletonCarousel(progress: State<Float>? = rememberLevyraShimmerProgress()) {
    val width = AppleMusicDesign.cardWidth(appleMusicViewportWidth())
    LazyRow(
        userScrollEnabled = false,
        contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
    ) {
        items(AppleComposedSkeletonCards, key = { "apple-skeleton-$it" }) {
            Column(Modifier.width(width)) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(AppleMusicDesign.artworkShape)
                        .background(AppleMusicDesign.placeholder)
                        .levyraShimmerSweep(progress, AppleMusicDesign.label)
                )
                Spacer(Modifier.height(7.dp))
                AppleMusicSkeletonLine(Modifier.fillMaxWidth(0.82f).height(11.dp), progress)
                Spacer(Modifier.height(5.dp))
                AppleMusicSkeletonLine(Modifier.fillMaxWidth(0.54f).height(11.dp), progress)
            }
        }
    }
}

@Composable
private fun AppleMusicSkeletonLine(modifier: Modifier, progress: State<Float>?) {
    Box(
        modifier.clip(AppleMusicDesign.thumbShape).background(AppleMusicDesign.placeholder)
            .levyraShimmerSweep(progress, AppleMusicDesign.label)
    )
}
