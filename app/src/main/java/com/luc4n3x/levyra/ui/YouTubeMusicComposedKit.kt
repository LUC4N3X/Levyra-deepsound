package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.levyraShimmerSweep
import com.luc4n3x.levyra.ui.components.rememberLevyraShimmerProgress
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.YouTubeMusicSkinCopy
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign

private const val YouTubeComposedSkeletonCards = 4
private const val YouTubeComposedHeroAspect = 16f / 9f
private val YouTubeComposedHeroMaxWidth = 560.dp

internal class YouTubeMusicComposedKit(
    private val copy: YouTubeMusicSkinCopy,
    private val countryLabel: String,
    private val onPickCountry: () -> Unit
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
        val play by rememberUpdatedState(onPlay)
        val more by rememberUpdatedState(onTrackActions)
        val rows = remember(sectionKey, tracks, ranked) {
            tracks.distinctBy { it.id }.mapIndexed { index, track ->
                YouTubeMusicSongRowModel(
                    "$sectionKey-${track.id}", track,
                    if (ranked) "${index + 1} • ${track.artist}" else track.artist,
                    { play(track) }, { more(track) }
                )
            }
        }
        if (rows.isEmpty() || title.isBlank()) return
        val picksCountry = ranked && countryLabel.isNotBlank()
        Column {
            YouTubeMusicSectionHeader(
                title,
                actionLabel = if (picksCountry) "$countryLabel ▾" else copy.playAll,
                onAction = if (picksCountry) onPickCountry else onPlayAll
            )
            Spacer(Modifier.height(4.dp))
            YouTubeMusicSongColumns(rows, currentId, copy.explicit)
        }
    }

    @Composable
    override fun Cards(title: String, cards: List<ComposedCard>) {
        val models = remember(cards) {
            cards.distinctBy { it.key }.map { card ->
                YouTubeMusicCardModel(
                    key = card.key, title = card.title, subtitle = card.subtitle, artworkUrl = card.artworkUrl,
                    circular = card.circular, wide = card.wide, track = card.track,
                    onClick = card.onClick, onLongClick = card.onLongClick
                )
            }
        }
        if (models.isEmpty()) return
        Column {
            YouTubeMusicSectionHeader(title)
            Spacer(Modifier.height(8.dp))
            YouTubeMusicCardCarousel(models)
        }
    }

    @Composable
    override fun Chips(labels: List<String>, onSelect: (Int) -> Unit) {
        YouTubeMusicChipRow(labels, selected = -1, onSelect = onSelect)
    }

    @Composable
    override fun Hero(hero: StudioHero) {
        Column {
            YouTubeMusicSectionHeader(hero.label)
            Spacer(Modifier.height(8.dp))
            YouTubeMusicHeroCard(hero)
        }
    }

    @Composable
    override fun Quotes(title: String, quotes: List<ComposedQuote>) {
        val unique = remember(quotes) { quotes.distinctBy { it.key } }
        if (unique.isEmpty()) return
        val width = YouTubeMusicDesign.quickPickColumnWidth(youTubeMusicViewportWidth())
        val showMeta = remember(unique) { unique.any { it.meta.isNotBlank() } }
        Column {
            YouTubeMusicSectionHeader(title)
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
                horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
            ) {
                items(unique, key = { it.key }) { quote ->
                    YouTubeMusicQuoteCard(quote, showMeta, Modifier.width(width))
                }
            }
        }
    }

    @Composable
    override fun Discovery(model: ComposedDiscovery) {
        val cards = remember(model.cards) {
            model.cards.distinctBy { it.key }.map { card ->
                YouTubeMusicDiscoveryCard(
                    card.key, card.title, card.subtitle, card.artworkUrl,
                    card.track?.artist.orEmpty(), card.track
                )
            }
        }
        val latestModel by rememberUpdatedState(model)
        Column {
            YouTubeMusicSectionHeader(model.title, actionLabel = model.startLabel, onAction = model.onOpen)
            if (model.lead.isNotBlank()) {
                Text(
                    model.lead, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = YouTubeMusicDesign.gutter)
                )
            }
            if (cards.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                YouTubeMusicDiscoveryCarousel(
                    cards,
                    onOpen = { latestModel.onOpen() },
                    onLongClick = { track ->
                        latestModel.cards.firstOrNull { it.track?.id == track.id }?.onLongClick?.invoke()
                    }
                )
            } else if (model.loading) {
                Spacer(Modifier.height(12.dp))
                YouTubeMusicDiscoverySkeleton()
            }
        }
    }

    @Composable
    override fun Notice(message: String, actionLabel: String?, onAction: (() -> Unit)?) {
        YouTubeMusicNotice(message, actionLabel, onAction)
    }

    @Composable
    override fun LoadingShelf(title: String) {
        val progress = rememberLevyraShimmerProgress()
        Column {
            if (title.isNotBlank()) {
                YouTubeMusicSectionHeader(title)
            } else {
                Box(
                    Modifier.padding(horizontal = YouTubeMusicDesign.gutter).heightIn(min = 48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    YouTubeMusicSkeletonLine(Modifier.width(164.dp).height(20.dp), progress)
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(
                userScrollEnabled = false,
                contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
                horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
            ) {
                items(YouTubeComposedSkeletonCards, key = { "ytm-skeleton-$it" }) {
                    Column(Modifier.width(YouTubeMusicDesign.cardArtwork)) {
                        Box(
                            Modifier.size(YouTubeMusicDesign.cardArtwork).clip(YouTubeMusicDesign.cardShape)
                                .background(YouTubeMusicDesign.placeholder)
                                .levyraShimmerSweep(progress, Color.White)
                        )
                        Spacer(Modifier.height(10.dp))
                        YouTubeMusicSkeletonLine(Modifier.fillMaxWidth(0.86f).height(13.dp), progress)
                        Spacer(Modifier.height(6.dp))
                        YouTubeMusicSkeletonLine(Modifier.fillMaxWidth(0.58f).height(13.dp), progress)
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeMusicHeroCard(hero: StudioHero) {
    val artwork = hero.artistArtworkUrl.ifBlank { hero.track.largeThumbnailUrl.ifBlank { hero.track.thumbnailUrl } }
    Box(
        Modifier.padding(horizontal = YouTubeMusicDesign.gutter).widthIn(max = YouTubeComposedHeroMaxWidth).fillMaxWidth()
            .aspectRatio(YouTubeComposedHeroAspect).clip(RoundedCornerShape(YouTubeMusicDesign.discoveryCorner))
            .background(Color(hero.track.accentStart))
            .levyraPressable(onClick = hero.onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface)
    ) {
        if (artwork.isNotBlank()) {
            StableRemoteArtwork(artwork, hero.track.artist, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true)
        } else {
            CoverImage(hero.track, Modifier.fillMaxSize(), highRes = true)
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
            Text(hero.track.artist, color = YouTubeMusicDesign.text, fontSize = 18.sp, lineHeight = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(hero.track.title, color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp, lineHeight = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (hero.lead.isNotBlank()) {
            Text(
                hero.lead, color = Color.White, fontSize = 16.sp, lineHeight = 20.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 18.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun YouTubeMusicQuoteCard(quote: ComposedQuote, showMeta: Boolean, modifier: Modifier) {
    val playLabel = LocalLevyraStrings.current.play
    Column(
        modifier.clip(YouTubeMusicDesign.tileShape).background(YouTubeMusicDesign.surface)
            .levyraPressable(onClick = quote.onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface)
            .padding(start = YouTubeMusicDesign.gutter, end = 4.dp, top = YouTubeMusicDesign.gutter, bottom = 8.dp)
    ) {
        Text(
            quote.quote, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.bodyLarge,
            minLines = 4, maxLines = 4, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 12.dp)
        )
        if (showMeta) {
            Text(
                quote.meta, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp, end = 12.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(YouTubeMusicDesign.compactArtwork).clip(YouTubeMusicDesign.thumbShape)
                    .background(YouTubeMusicDesign.placeholder)
            ) { CoverImage(quote.track, Modifier.fillMaxSize()) }
            Spacer(Modifier.width(YouTubeMusicDesign.itemGap))
            Column(Modifier.weight(1f)) {
                Text(
                    quote.track.title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    quote.track.artist, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            YouTubeMusicIconAction(Icons.Rounded.PlayArrow, playLabel, quote.onPlay)
        }
    }
}

@Composable
private fun YouTubeMusicDiscoverySkeleton() {
    val width: Dp = (youTubeMusicViewportWidth() - YouTubeMusicDesign.gutter * 2 - YouTubeMusicDesign.itemGap - 2.dp).coerceIn(260.dp, 420.dp)
    val progress = rememberLevyraShimmerProgress()
    LazyRow(
        userScrollEnabled = false,
        contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
    ) {
        items(2, key = { "ytm-discovery-skeleton-$it" }) {
            Box(
                Modifier.size(width).clip(RoundedCornerShape(YouTubeMusicDesign.discoveryCorner))
                    .background(YouTubeMusicDesign.placeholder)
                    .levyraShimmerSweep(progress, Color.White)
            )
        }
    }
}

@Composable
private fun YouTubeMusicSkeletonLine(modifier: Modifier, progress: State<Float>?) {
    Box(
        modifier.clip(YouTubeMusicDesign.thumbShape).background(YouTubeMusicDesign.placeholder)
            .levyraShimmerSweep(progress, Color.White)
    )
}
