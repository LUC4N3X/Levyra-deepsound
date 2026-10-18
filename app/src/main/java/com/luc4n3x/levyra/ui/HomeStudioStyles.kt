package com.luc4n3x.levyra.ui

import android.icu.text.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import com.luc4n3x.levyra.ui.components.LevyraMorphShape
import androidx.graphics.shapes.Morph
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxHeight
import com.luc4n3x.levyra.domain.ResonanceCommentSnippet
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.LocalContentColor
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateFloatAsState
import com.luc4n3x.levyra.ui.components.rememberLevyraArtworkMorphShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextAlign
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.State
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.HomeDensity
import com.luc4n3x.levyra.domain.HomeSectionId
import com.luc4n3x.levyra.domain.HomeSectionLayout
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.domain.LevyraHomeStudioSettings
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraCardCaption
import com.luc4n3x.levyra.ui.components.levyraMarquee
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.LevyraSectionPlayAll
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.homeStudioCopy
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import java.util.Date
import java.util.Locale

@Immutable
internal data class HomeStyleSpec(
    val inset: Dp,
    val sectionGap: Dp,
    val cardWidth: Dp,
    val largeCardWidth: Dp,
    val largeAspect: Float,
    val artworkCorner: Dp,
    val listThumb: Dp,
    val listLimit: Int,
    val gridColumns: Int,
    val compactLimit: Int
)

internal fun homeStyleSpec(style: HomeStyle, density: HomeDensity): HomeStyleSpec {
    val base = when (style) {
        HomeStyle.Original -> HomeStyleSpec(16.dp, 8.dp, 156.dp, 260.dp, 1f, 12.dp, 52.dp, 5, 3, 6)
        HomeStyle.ExpressiveMax -> HomeStyleSpec(16.dp, 20.dp, 168.dp, 304.dp, 1f, 32.dp, 56.dp, 5, 3, 6)
        HomeStyle.Flow -> HomeStyleSpec(16.dp, 12.dp, 140.dp, 228.dp, 1f, 12.dp, 48.dp, 6, 3, 8)
        HomeStyle.Editorial -> HomeStyleSpec(20.dp, 24.dp, 172.dp, 304.dp, 4f / 3f, 16.dp, 64.dp, 5, 2, 6)
        HomeStyle.Minimal -> HomeStyleSpec(20.dp, 16.dp, 132.dp, 244.dp, 1f, 8.dp, 44.dp, 6, 3, 6)
        HomeStyle.Immersive -> HomeStyleSpec(16.dp, 24.dp, 180.dp, 276.dp, 0.8f, 28.dp, 56.dp, 5, 2, 6)
        HomeStyle.MaterialExpressive -> HomeStyleSpec(16.dp, 22.dp, 160.dp, 132.dp, 1f, 28.dp, 56.dp, 5, 2, 6)
    }
    val scale = when (density) {
        HomeDensity.Compact -> 0.88f
        HomeDensity.Balanced -> 1f
        HomeDensity.Spacious -> 1.12f
    }
    return base.copy(
        sectionGap = base.sectionGap * scale,
        cardWidth = base.cardWidth * scale,
        largeCardWidth = base.largeCardWidth * scale,
        listLimit = if (density == HomeDensity.Compact) base.listLimit + 1 else base.listLimit
    )
}

internal fun resolveHomeSectionLayout(
    style: HomeStyle,
    id: HomeSectionId,
    configured: HomeSectionLayout
): HomeSectionLayout {
    if (configured != HomeSectionLayout.Auto) return configured
    if (style == HomeStyle.Original) return HomeSectionLayout.Auto
    return LevyraHomeStudioSettings.defaultLayout(style).layouts[id] ?: HomeSectionLayout.Carousel
}

@Immutable
internal class StudioShelfItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val current: Boolean,
    val round: Boolean,
    val artwork: @Composable (Modifier, Boolean) -> Unit,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)?
)

@Composable
internal fun rememberTrackShelfItems(
    tracks: List<Track>,
    currentId: String?,
    onPlay: (Track) -> Unit,
    onActions: ((Track) -> Unit)?
): List<StudioShelfItem> {
    val play by rememberUpdatedState(onPlay)
    val actions by rememberUpdatedState(onActions)
    val hasActions = onActions != null
    return remember(tracks, currentId, hasActions) {
        tracks.mapIndexed { index, track ->
            StudioShelfItem(
                key = "$index-${track.id}",
                title = track.title,
                subtitle = track.artist,
                current = track.id == currentId,
                round = false,
                artwork = { modifier, highRes -> CoverImage(track = track, modifier = modifier, highRes = highRes) },
                onClick = { play(track) },
                onLongClick = if (hasActions) { { actions?.invoke(track) } } else null
            )
        }
    }
}

@Composable
internal fun rememberAlbumShelfItems(albums: List<AlbumHit>, onOpen: (AlbumHit) -> Unit): List<StudioShelfItem> {
    val open by rememberUpdatedState(onOpen)
    return remember(albums) {
        albums.mapIndexed { index, album ->
            StudioShelfItem(
                key = "$index-${album.browseId.ifBlank { album.title + "|" + album.artist }}",
                title = album.title,
                subtitle = album.artist,
                current = false,
                round = false,
                artwork = { modifier, highRes -> StudioRemoteArtwork(album.thumbnailUrl, album.title, modifier, highRes) },
                onClick = { open(album) },
                onLongClick = null
            )
        }
    }
}

@Composable
internal fun rememberArtistShelfItems(artists: List<ArtistHit>, onOpen: (ArtistHit) -> Unit): List<StudioShelfItem> {
    val open by rememberUpdatedState(onOpen)
    return remember(artists) {
        artists.mapIndexed { index, artist ->
            StudioShelfItem(
                key = "$index-${artist.browseId.ifBlank { artist.name }}",
                title = artist.name,
                subtitle = artist.subscribers,
                current = false,
                round = true,
                artwork = { modifier, highRes -> StudioRemoteArtwork(artist.thumbnailUrl, artist.name, modifier, highRes) },
                onClick = { open(artist) },
                onLongClick = null
            )
        }
    }
}

@Composable
internal fun StudioRemoteArtwork(url: String, description: String, modifier: Modifier, highRes: Boolean) {
    Box(modifier = modifier.background(LevyraPanel)) {
        if (url.isNotBlank()) {
            StableRemoteArtwork(
                url = url,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                highRes = highRes
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun expressiveShapeFor(index: Int): Shape = when (index % 4) {
    0 -> RoundedCornerShape(28.dp)
    1 -> MaterialShapes.Cookie9Sided.toShape()
    2 -> RoundedCornerShape(topStart = 48.dp, topEnd = 14.dp, bottomEnd = 48.dp, bottomStart = 14.dp)
    else -> MaterialShapes.Arch.toShape()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun expressiveArtistShapeFor(index: Int): Shape = when (index % 3) {
    0 -> MaterialShapes.Cookie12Sided.toShape()
    1 -> CircleShape
    else -> MaterialShapes.Clover8Leaf.toShape()
}

@Composable
private fun studioArtworkShape(style: HomeStyle, spec: HomeStyleSpec, index: Int, round: Boolean): Shape = when {
    round && style == HomeStyle.Minimal -> RoundedCornerShape(spec.artworkCorner)
    round && style == HomeStyle.ExpressiveMax -> expressiveArtistShapeFor(index)
    round -> CircleShape
    style == HomeStyle.ExpressiveMax -> expressiveShapeFor(index)
    else -> RoundedCornerShape(spec.artworkCorner)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun studioOrbitShape(style: HomeStyle, index: Int, fallback: Shape): Shape = when (style) {
    HomeStyle.ExpressiveMax -> when (index % 6) {
        0 -> MaterialShapes.SoftBurst.toShape()
        1 -> MaterialShapes.Cookie9Sided.toShape()
        2 -> MaterialShapes.Clover8Leaf.toShape()
        3 -> MaterialShapes.Arch.toShape()
        4 -> MaterialShapes.Gem.toShape()
        else -> MaterialShapes.Cookie12Sided.toShape()
    }
    HomeStyle.Flow -> RoundedCornerShape(topStart = 28.dp, topEnd = 8.dp, bottomEnd = 28.dp, bottomStart = 8.dp)
    HomeStyle.Editorial -> if (index % 2 == 0) {
        RoundedCornerShape(topStart = 4.dp, topEnd = 32.dp, bottomEnd = 4.dp, bottomStart = 4.dp)
    } else {
        RoundedCornerShape(topStart = 32.dp, topEnd = 4.dp, bottomEnd = 4.dp, bottomStart = 4.dp)
    }
    HomeStyle.Minimal -> RoundedCornerShape(8.dp)
    HomeStyle.Immersive -> RoundedCornerShape(32.dp)
    HomeStyle.Original, HomeStyle.MaterialExpressive -> fallback
}

@Composable
private fun studioTitleColor(item: StudioShelfItem): Color =
    if (item.current) MaterialTheme.colorScheme.primary else LevyraText

@Composable
internal fun StudioSectionHeader(
    style: HomeStyle,
    spec: HomeStyleSpec,
    title: String,
    featured: Boolean,
    onPlayAll: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.homeStudioCopy
    val headerModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = spec.inset)
    when (style) {
        HomeStyle.MaterialExpressive -> MxSectionHeader(spec, title, featured, copy.featured, strings.playAll, onPlayAll, modifier)
        HomeStyle.Original -> LevyraSectionHeader(
            title = title,
            subtitle = if (featured) copy.featured else null,
            modifier = headerModifier
        ) {
            onPlayAll?.let { action -> LevyraSectionPlayAll(onClick = action) }
        }
        HomeStyle.Editorial -> Column(modifier = headerModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).semantics { heading() }) {
                    if (featured) {
                        Text(
                            text = copy.featured.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                        color = LevyraText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                onPlayAll?.let { action ->
                    TextButton(onClick = action) { Text(strings.playAll) }
                }
            }
        }
        else -> Row(
            modifier = headerModifier.heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (style == HomeStyle.Flow) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .width(4.dp)
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Column(modifier = Modifier.weight(1f).semantics { heading() }) {
                if (featured) {
                    Text(
                        text = copy.featured,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = title,
                    style = when (style) {
                        HomeStyle.ExpressiveMax -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                        HomeStyle.Minimal -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                        HomeStyle.Immersive -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                        else -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    },
                    color = LevyraText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            onPlayAll?.let { action -> StudioPlayAllButton(style, strings.playAll, action) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StudioPlayAllButton(style: HomeStyle, label: String, onClick: () -> Unit) {
    when (style) {
        HomeStyle.ExpressiveMax -> FilledTonalIconButton(
            onClick = onClick,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = label)
        }
        HomeStyle.Flow -> TextButton(onClick = onClick) { Text(label) }
        HomeStyle.Immersive -> FilledIconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = label)
        }
        else -> IconButton(onClick = onClick) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = label, tint = LevyraText)
        }
    }
}

@Composable
internal fun StudioShelf(
    style: HomeStyle,
    spec: HomeStyleSpec,
    layout: HomeSectionLayout,
    items: List<StudioShelfItem>,
    isPlaying: Boolean,
    actionsLabel: String,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val roundItems = items.first().round
    if (style == HomeStyle.MaterialExpressive) {
        when (layout) {
            HomeSectionLayout.Grid -> MxPagedGrid(spec, items, isPlaying, modifier)
            HomeSectionLayout.List -> MxSegmentedList(spec, items, isPlaying, actionsLabel, modifier)
            HomeSectionLayout.Compact -> StudioCompact(style, spec, items, isPlaying, modifier)
            HomeSectionLayout.Large -> if (roundItems) {
                MxShapeGallery(spec, items, isPlaying, modifier)
            } else {
                MxOrganic(spec, items, isPlaying, modifier)
            }
            HomeSectionLayout.Carousel, HomeSectionLayout.Auto -> if (roundItems) {
                MxShapeGallery(spec, items, isPlaying, modifier)
            } else {
                MxMorphCarousel(spec, items, isPlaying, modifier)
            }
        }
        return
    }
    when (layout) {
        HomeSectionLayout.Grid -> StudioGrid(style, spec, items, isPlaying, modifier)
        HomeSectionLayout.List -> StudioList(style, spec, items, isPlaying, actionsLabel, modifier)
        HomeSectionLayout.Compact -> StudioCompact(style, spec, items, isPlaying, modifier)
        HomeSectionLayout.Large -> if (style == HomeStyle.ExpressiveMax && !roundItems) {
            StudioExpressiveMosaic(spec, items, isPlaying, modifier)
        } else {
            StudioLargeRow(style, spec, items, isPlaying, modifier)
        }
        HomeSectionLayout.Carousel, HomeSectionLayout.Auto -> if (style == HomeStyle.ExpressiveMax && !roundItems) {
            StudioExpressiveCarousel(spec, items, isPlaying, modifier)
        } else {
            StudioCarousel(style, spec, items, isPlaying, modifier)
        }
    }
}

@Composable
private fun Modifier.studioPress(item: StudioShelfItem, scale: Float = LevyraPressScale.Tile): Modifier =
    levyraPressable(
        onClick = item.onClick,
        onLongClick = item.onLongClick,
        onClickLabel = item.title,
        role = Role.Button,
        pressedScale = scale
    )

@Composable
private fun StudioCurrentBadge(item: StudioShelfItem, isPlaying: Boolean, modifier: Modifier = Modifier) {
    if (!item.current) return
    Box(
        modifier = modifier
            .padding(8.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.52f))
            .padding(6.dp)
    ) {
        LevyraPlayingIndicator(playing = isPlaying, color = Color.White, size = 12.dp)
    }
}

@Composable
private fun StudioCarousel(
    style: HomeStyle,
    spec: HomeStyleSpec,
    items: List<StudioShelfItem>,
    isPlaying: Boolean,
    modifier: Modifier
) {
    val roundItems = items.first().round
    val width = if (roundItems) spec.cardWidth * 0.82f else spec.cardWidth
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (style == HomeStyle.Flow) 10.dp else 14.dp),
        contentPadding = PaddingValues(horizontal = spec.inset)
    ) {
        itemsIndexed(items, key = { _, item -> item.key }, contentType = { _, _ -> "studio-carousel-card" }) { index, item ->
            Column(
                modifier = Modifier
                    .width(width)
                    .studioPress(item),
                horizontalAlignment = if (item.round) Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(studioArtworkShape(style, spec, index, item.round))
                ) {
                    item.artwork(Modifier.fillMaxSize(), false)
                    StudioCurrentBadge(item, isPlaying, Modifier.align(Alignment.TopEnd))
                }
                LevyraCardCaption(
                    title = item.title,
                    subtitle = if (item.round) "" else item.subtitle,
                    titleColor = studioTitleColor(item),
                    titleLines = if (style == HomeStyle.Flow || item.round) 1 else 2,
                    reserveTitleLines = !item.round,
                    marqueeTitle = style == HomeStyle.ExpressiveMax
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioExpressiveCarousel(
    spec: HomeStyleSpec,
    items: List<StudioShelfItem>,
    isPlaying: Boolean,
    modifier: Modifier
) {
    val carouselState = rememberCarouselState { items.size }
    val shape = MaterialTheme.shapes.extraLarge
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = spec.cardWidth * 1.3f,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = spec.inset),
        modifier = modifier
            .fillMaxWidth()
            .height(spec.cardWidth * 1.42f)
    ) { index ->
        val item = items[index]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .maskClip(shape)
                .studioPress(item)
        ) {
            item.artwork(Modifier.fillMaxSize(), true)
            Box(modifier = Modifier.fillMaxSize().background(StudioCaptionScrim))
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val info = carouselItemDrawInfo
                        translationX = info.maskRect.left
                        alpha = ((info.size - info.minSize) / (info.maxSize - info.minSize).coerceAtLeast(1f))
                            .coerceIn(0f, 1f)
                    }
                    .padding(16.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.levyraMarquee()
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            StudioCurrentBadge(item, isPlaying, Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun StudioExpressiveMosaic(
    spec: HomeStyleSpec,
    items: List<StudioShelfItem>,
    isPlaying: Boolean,
    modifier: Modifier
) {
    val hero = items.first()
    val side = items.drop(1).take(2)
    val bottom = items.drop(3).take(3)
    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(horizontal = spec.inset)) {
        val gap = 10.dp
        val heroSize = (maxWidth - gap * 0.5f) / 1.5f
        val sideSize = (heroSize - gap) / 2
        val tileSize = (maxWidth - gap * 2) / 3
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Box(
                    modifier = Modifier
                        .size(heroSize)
                        .clip(RoundedCornerShape(topStart = 56.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp))
                        .studioPress(hero)
                ) {
                    hero.artwork(Modifier.fillMaxSize(), true)
                    Box(modifier = Modifier.fillMaxSize().background(StudioCaptionScrim))
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                        Text(
                            text = hero.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.levyraMarquee()
                        )
                        Text(
                            text = hero.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    StudioCurrentBadge(hero, isPlaying, Modifier.align(Alignment.TopEnd))
                }
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    side.forEachIndexed { index, item ->
                        StudioMosaicTile(item, index + 1, sideSize, isPlaying)
                    }
                }
            }
            if (bottom.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    bottom.forEachIndexed { index, item ->
                        StudioMosaicTile(item, index + 3, tileSize, isPlaying)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioMosaicTile(item: StudioShelfItem, index: Int, size: Dp, isPlaying: Boolean) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(expressiveShapeFor(index))
            .studioPress(item)
            .clearAndSetSemantics { contentDescription = "${item.title}, ${item.subtitle}" }
    ) {
        item.artwork(Modifier.fillMaxSize(), false)
        StudioCurrentBadge(item, isPlaying, Modifier.align(Alignment.Center))
    }
}

@Composable
private fun StudioLargeRow(
    style: HomeStyle,
    spec: HomeStyleSpec,
    items: List<StudioShelfItem>,
    isPlaying: Boolean,
    modifier: Modifier
) {
    val roundItems = items.first().round
    val aspect = if (roundItems) 0.78f else spec.largeAspect
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = spec.inset)
    ) {
        itemsIndexed(items, key = { _, item -> item.key }, contentType = { _, _ -> "studio-large-card" }) { _, item ->
            StudioLargeCard(
                item = item,
                style = style,
                spec = spec,
                aspect = aspect,
                roundItems = roundItems,
                isPlaying = isPlaying
            )
        }
    }
}

@Composable
private fun StudioLargeCard(
    item: StudioShelfItem,
    style: HomeStyle,
    spec: HomeStyleSpec,
    aspect: Float,
    roundItems: Boolean,
    isPlaying: Boolean
) {
    val overlay = style == HomeStyle.Immersive || roundItems
    val marquee = style == HomeStyle.ExpressiveMax
    Column(
        modifier = Modifier
            .width(spec.largeCardWidth)
            .studioPress(item, LevyraPressScale.Surface),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(spec.artworkCorner))
        ) {
            item.artwork(Modifier.fillMaxSize(), true)
            if (overlay) {
                StudioLargeOverlayCaption(item, marquee, Modifier.align(Alignment.BottomStart))
            }
            StudioCurrentBadge(item, isPlaying, Modifier.align(Alignment.TopEnd))
        }
        if (!overlay) {
            StudioLargeExternalCaption(item, style, marquee)
        }
    }
}

@Composable
private fun StudioLargeOverlayCaption(
    item: StudioShelfItem,
    marquee: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize().background(StudioCaptionScrim))
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = if (marquee) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.levyraMarquee(enabled = marquee)
        )
        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudioLargeExternalCaption(
    item: StudioShelfItem,
    style: HomeStyle,
    marquee: Boolean
) {
    val titleStyle = if (style == HomeStyle.Editorial) {
        MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)
    } else {
        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = item.title,
            style = titleStyle,
            color = studioTitleColor(item),
            maxLines = if (marquee) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.levyraMarquee(enabled = marquee)
        )
        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LevyraMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudioGrid(
    style: HomeStyle,
    spec: HomeStyleSpec,
    items: List<StudioShelfItem>,
    isPlaying: Boolean,
    modifier: Modifier
) {
    val columns = spec.gridColumns
    val rows = remember(items, columns) { items.take(columns * 2).chunked(columns) }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = spec.inset),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { columnIndex, item ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .studioPress(item),
                        horizontalAlignment = if (item.round) Alignment.CenterHorizontally else Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(studioArtworkShape(style, spec, rowIndex * columns + columnIndex, item.round))
                        ) {
                            item.artwork(Modifier.fillMaxSize(), false)
                            StudioCurrentBadge(item, isPlaying, Modifier.align(Alignment.TopEnd))
                        }
                        LevyraCardCaption(
                            title = item.title,
                            subtitle = if (item.round) "" else item.subtitle,
                            titleColor = studioTitleColor(item),
                            titleLines = 1,
                            reserveTitleLines = false,
                            marqueeTitle = style == HomeStyle.ExpressiveMax
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StudioList(
    style: HomeStyle,
    spec: HomeStyleSpec,
    items: List<StudioShelfItem>,
    isPlaying: Boolean,
    actionsLabel: String,
    modifier: Modifier
) {
    val visible = remember(items, spec.listLimit) { items.take(spec.listLimit) }
    val marquee = style == HomeStyle.ExpressiveMax
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = spec.inset),
        verticalArrangement = Arrangement.spacedBy(if (style == HomeStyle.Minimal) 2.dp else 6.dp)
    ) {
        visible.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = spec.listThumb + 12.dp)
                    .clip(RoundedCornerShape(spec.artworkCorner))
                    .studioPress(item, LevyraPressScale.Row)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (style == HomeStyle.Editorial) {
                    Text(
                        text = (index + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(32.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(spec.listThumb)
                        .clip(if (item.round) CircleShape else RoundedCornerShape(spec.artworkCorner.coerceAtMost(12.dp)))
                ) {
                    item.artwork(Modifier.fillMaxSize(), false)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = studioTitleColor(item),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.levyraMarquee(enabled = marquee)
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = LevyraMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (item.current) {
                    LevyraPlayingIndicator(playing = isPlaying, color = MaterialTheme.colorScheme.primary, size = 14.dp)
                }
                item.onLongClick?.let { actions ->
                    IconButton(onClick = actions) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = actionsLabel, tint = LevyraMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioCompact(
    style: HomeStyle,
    spec: HomeStyleSpec,
    items: List<StudioShelfItem>,
    isPlaying: Boolean,
    modifier: Modifier
) {
    if (items.first().round) {
        StudioChipRow(spec, items, modifier)
        return
    }
    val rows = remember(items, spec.compactLimit) { items.take(spec.compactLimit).chunked(2) }
    val tileShape = RoundedCornerShape(if (style == HomeStyle.Flow) 6.dp else spec.artworkCorner.coerceAtMost(12.dp))
    val marquee = style == HomeStyle.ExpressiveMax
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = spec.inset),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(tileShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
                            .studioPress(item, LevyraPressScale.Row),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(56.dp)) {
                            item.artwork(Modifier.fillMaxSize(), false)
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = studioTitleColor(item),
                            maxLines = if (marquee) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                                .levyraMarquee(enabled = marquee)
                        )
                        if (item.current) {
                            Box(modifier = Modifier.padding(end = 10.dp)) {
                                LevyraPlayingIndicator(playing = isPlaying, color = MaterialTheme.colorScheme.primary, size = 12.dp)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StudioChipRow(spec: HomeStyleSpec, items: List<StudioShelfItem>, modifier: Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = spec.inset)
    ) {
        itemsIndexed(items, key = { _, item -> item.key }, contentType = { _, _ -> "studio-chip" }) { _, item ->
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f))
                    .studioPress(item, LevyraPressScale.Row)
                    .padding(start = 6.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape)) {
                    item.artwork(Modifier.fillMaxSize(), false)
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = LevyraText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.levyraMarquee()
                )
            }
        }
    }
}

private val StudioCaptionScrim = Brush.verticalGradient(
    0.0f to Color.Transparent,
    0.45f to Color.Transparent,
    1.0f to Color.Black.copy(alpha = 0.78f)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun StudioHomeHeader(
    style: HomeStyle,
    spec: HomeStyleSpec,
    userName: String,
    isResolving: Boolean,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    hero: StudioHero? = null,
    topBleed: Dp = 0.dp
) {
    val strings = LocalLevyraStrings.current
    val greetingHour by rememberCurrentGreetingHour()
    val dayPart = remember(strings, greetingHour) { strings.greetingDayPart(greetingHour) }
    val name = remember(strings, userName) { strings.greetingName(userName) }
    val greeting = remember(strings, userName, greetingHour) { strings.formatGreeting(userName, greetingHour) }
    val today = remember(greetingHour, strings.code) {
        val locale = Locale.forLanguageTag(strings.code)
        DateFormat.getInstanceForSkeleton("EEEEMMMMd", locale).format(Date())
            .replaceFirstChar { it.titlecase(locale) }
    }
    val topKicker = if (name.isNotBlank()) dayPart else today
    val mainHeading = if (name.isNotBlank()) name else dayPart
    if (style == HomeStyle.MaterialExpressive && hero != null) {
        MxBleedHero(
            hero = hero,
            kicker = topKicker,
            headline = mainHeading,
            greeting = greeting,
            searchLabel = strings.search,
            settingsLabel = strings.settings,
            playLabel = strings.play,
            isResolving = isResolving,
            topBleed = topBleed,
            onSearch = onSearch,
            onSettings = onSettings
        )
        return
    }
    val actions: @Composable () -> Unit = {
        if (style == HomeStyle.ExpressiveMax) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(onClick = onSearch, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Rounded.Search, contentDescription = strings.search)
                }
                FilledTonalIconButton(onClick = onSettings, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(52.dp)) {
                    if (isResolving) {
                        LevyraLoadingIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Rounded.Settings, contentDescription = strings.settings)
                    }
                }
            }
        } else {
            Row {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Rounded.Search, contentDescription = strings.search, tint = LevyraText)
                }
                IconButton(onClick = onSettings) {
                    if (isResolving) {
                        LevyraLoadingIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Rounded.Settings, contentDescription = strings.settings, tint = LevyraText)
                    }
                }
            }
        }
    }
    val headingModifier = Modifier.clearAndSetSemantics {
        contentDescription = greeting
        heading()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spec.inset)
    ) {
        when (style) {
            HomeStyle.MaterialExpressive -> MxHeader(
                topKicker, mainHeading, greeting, strings.search, strings.settings, isResolving, onSearch, onSettings
            )
            HomeStyle.ExpressiveMax -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HomeLogoBadge()
                    Spacer(modifier = Modifier.weight(1f))
                    actions()
                }
                Column(modifier = headingModifier.padding(top = 18.dp)) {
                    Text(
                        text = topKicker,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mainHeading,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                        color = LevyraText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.levyraMarquee()
                    )
                }
            }
            HomeStyle.Editorial -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = today.uppercase(Locale.forLanguageTag(strings.code)),
                        style = MaterialTheme.typography.labelLarge,
                        color = LevyraMuted,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    actions()
                }
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif),
                    color = LevyraText,
                    modifier = headingModifier.padding(top = 6.dp, bottom = 4.dp)
                )
            }
            HomeStyle.Minimal, HomeStyle.Flow -> Row(
                modifier = Modifier.heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = greeting,
                    style = if (style == HomeStyle.Flow) {
                        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                    },
                    color = LevyraText,
                    modifier = headingModifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                actions()
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.weight(1f))
                    actions()
                }
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = LevyraText,
                    modifier = headingModifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
internal fun StudioFlowShortcuts(
    spec: HomeStyleSpec,
    title: String,
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    onPlay: (Track) -> Unit,
    onActions: (Track) -> Unit
) {
    val items = rememberTrackShelfItems(tracks, currentId, onPlay, onActions)
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = LevyraText,
            modifier = Modifier
                .padding(horizontal = spec.inset)
                .semantics { heading() }
        )
        StudioCompact(HomeStyle.Flow, spec, items, isPlaying, Modifier)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun StudioSpotlight(
    style: HomeStyle,
    spec: HomeStyleSpec,
    track: Track,
    artistArtworkUrl: String,
    label: String,
    lead: String,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    animationsEnabled: Boolean,
    onOpen: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val playLabel = strings.play
    val playing = isCurrent && isPlaying
    val resolving = isCurrent && isResolving
    val playIcon = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
    when (style) {
        HomeStyle.ExpressiveMax -> ExpressiveSpotlight(spec, track, artistArtworkUrl, label, lead, playLabel, playing, resolving, playIcon, animationsEnabled, onOpen)
        HomeStyle.Editorial -> EditorialSpotlight(spec, track, artistArtworkUrl, label, lead, playLabel, resolving, playIcon, onOpen)
        HomeStyle.Immersive -> CinematicSpotlight(style, spec, track, artistArtworkUrl, label, lead, playLabel, playing, resolving, playIcon, animationsEnabled, onOpen)
        HomeStyle.Flow -> StudioFlowSpotlight(track, artistArtworkUrl, label, lead, spec.inset, StudioSpotlightPlayback(playLabel, resolving, playIcon, onOpen))
        else -> CompactSpotlight(style, spec, track, artistArtworkUrl, label, lead, playLabel, playing, resolving, playIcon, animationsEnabled, onOpen)
    }
}

@Composable
private fun rememberSpotlightMotion(
    active: Boolean,
    from: Float,
    to: Float,
    durationMillis: Int,
    reverse: Boolean
): State<Float> {
    if (!active) return remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "spotlight-motion")
    return transition.animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = if (reverse) FastOutSlowInEasing else LinearEasing),
            repeatMode = if (reverse) RepeatMode.Reverse else RepeatMode.Restart
        ),
        label = "spotlight-motion-value"
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
private fun ExpressiveSpotlight(
    spec: HomeStyleSpec,
    track: Track,
    artistArtworkUrl: String,
    label: String,
    lead: String,
    playLabel: String,
    playing: Boolean,
    resolving: Boolean,
    playIcon: ImageVector,
    animationsEnabled: Boolean,
    onOpen: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val scheme = MaterialTheme.colorScheme
    val isLight = LevyraIsLight
    val canvas = homeCanvasColor(isLight)
    val seed = if (track.accentStart != 0) Color(track.accentStart) else scheme.primary
    val secondarySeed = if (track.accentEnd != 0) Color(track.accentEnd) else scheme.tertiary
    val morph = rememberLevyraPlayMorphShape(playing = playing, animated = animationsEnabled, spinWhilePlaying = !resolving)
    val spin = rememberSpotlightMotion(animationsEnabled && playing, 0f, 360f, 44_000, reverse = false)
    val breathe = rememberSpotlightMotion(animationsEnabled, 0.18f, 0.72f, 6_800, reverse = true)
    val sleeveShape = RoundedCornerShape(topStart = 96.dp, topEnd = 26.dp, bottomEnd = 92.dp, bottomStart = 28.dp)
    val backingShape = RoundedCornerShape(topStart = 76.dp, topEnd = 46.dp, bottomEnd = 104.dp, bottomStart = 34.dp)
    val stampShape = MaterialShapes.Cookie6Sided.toShape()
    val radioShape = MaterialShapes.Sunny.toShape()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .levyraPressable(
                onClick = onOpen,
                onClickLabel = track.title,
                role = Role.Button,
                pressedScale = 1f
            )
    ) {
        val coverSize = (maxWidth * 0.90f).coerceAtMost(380.dp)
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(coverSize + 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val focus = Offset(size.width * 0.78f, size.height * 0.44f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        seed.copy(alpha = if (isLight) 0.24f else 0.34f),
                                        secondarySeed.copy(alpha = if (isLight) 0.10f else 0.16f),
                                        Color.Transparent
                                    ),
                                    center = focus,
                                    radius = size.width * 0.82f
                                ),
                                center = focus,
                                radius = size.width * 0.82f
                            )
                        }
                )
                // A single off-axis sleeve peeks out behind the artwork, rather than spinning decorative bursts.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = 14.dp)
                        .size(coverSize)
                        .graphicsLayer { rotationZ = -5f + spin.value * 0.012f }
                        .clip(backingShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    scheme.primaryContainer.copy(alpha = 0.70f),
                                    secondarySeed.copy(alpha = if (isLight) 0.24f else 0.34f),
                                    scheme.tertiaryContainer.copy(alpha = 0.46f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 16.dp, y = 4.dp)
                        .size(coverSize)
                        .clip(sleeveShape)
                ) {
                    if (artistArtworkUrl.isNotBlank()) {
                        StudioRemoteArtwork(artistArtworkUrl, track.artist, Modifier.fillMaxSize(), true)
                    } else {
                        CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = true)
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.56f to Color.Transparent,
                                    0.80f to canvas.copy(alpha = if (isLight) 0.30f else 0.40f),
                                    1.0f to canvas.copy(alpha = if (isLight) 0.76f else 0.84f)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                1.dp,
                                scheme.primary.copy(alpha = if (isLight) 0.25f else 0.38f),
                                sleeveShape
                            )
                    )
                }
                // The copy sits over a soft tonal fade, but the cover stays crisp and fully readable.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(154.dp)
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.44f to canvas.copy(alpha = if (isLight) 0.46f else 0.56f),
                                1.0f to canvas.copy(alpha = if (isLight) 0.94f else 0.96f)
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = spec.inset, end = spec.inset, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Surface(
                        color = scheme.tertiaryContainer.copy(alpha = 0.94f),
                        contentColor = scheme.onTertiaryContainer,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(radioShape)
                                    .background(scheme.tertiary)
                            )
                            Text(
                                text = strings.homeSoundtrackRadio.uppercase(Locale.forLanguageTag(strings.code)),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.displaySmallEmphasized.copy(fontWeight = FontWeight.Black),
                        color = if (isLight) LevyraText else Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = spec.inset, end = spec.inset, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer { rotationZ = -6f + 12f * breathe.value }
                        .clip(stampShape)
                        .background(scheme.primaryContainer)
                        .padding(2.5.dp)
                        .clip(stampShape)
                ) {
                    CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = false)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMediumEmphasized.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isLight) LevyraText else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.levyraMarquee()
                    )
                    Text(
                        text = lead.ifBlank { track.artist },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (isLight) LevyraMuted else Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.levyraMarquee()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(morph)
                        .background(
                            Brush.linearGradient(
                                listOf(scheme.primary, scheme.tertiary)
                            )
                        )
                        .levyraPressable(onClick = onOpen, onClickLabel = playLabel, role = Role.Button),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(morph)
                            .background(scheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (resolving) {
                            LevyraLoadingIndicator(modifier = Modifier.size(30.dp), color = scheme.onPrimary)
                        } else {
                            Icon(
                                imageVector = playIcon,
                                contentDescription = playLabel,
                                tint = scheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialSpotlight(
    spec: HomeStyleSpec,
    track: Track,
    artistArtworkUrl: String,
    label: String,
    lead: String,
    playLabel: String,
    resolving: Boolean,
    playIcon: ImageVector,
    onOpen: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val hasPortrait = artistArtworkUrl.isNotBlank()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spec.inset)
            .aspectRatio(0.84f)
            .clip(RoundedCornerShape(spec.artworkCorner))
            .background(LevyraPanel)
            .levyraPressable(
                onClick = onOpen,
                onClickLabel = track.title,
                role = Role.Button,
                pressedScale = LevyraPressScale.Surface
            )
    ) {
        if (hasPortrait) {
            StudioRemoteArtwork(artistArtworkUrl, track.artist, Modifier.fillMaxSize(), true)
        } else {
            CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = true)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.46f),
                        0.26f to Color.Transparent,
                        0.5f to Color.Transparent,
                        0.78f to Color.Black.copy(alpha = 0.62f),
                        1f to Color.Black.copy(alpha = 0.9f)
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 18.dp, top = 18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(2.dp)
                        .background(scheme.primary)
                )
            }
            if (hasPortrait) {
                CoverImage(
                    track = track,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(72.dp)
                        .graphicsLayer { rotationZ = 5f }
                        .background(Color.White)
                        .padding(3.dp),
                    highRes = false
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 18.dp, bottom = 20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lead.ifBlank { track.artist }.uppercase(Locale.forLanguageTag(LocalLevyraStrings.current.code)),
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.6.sp),
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledIconButton(
                onClick = onOpen,
                modifier = Modifier.size(60.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary
                )
            ) {
                if (resolving) {
                    LevyraLoadingIndicator(modifier = Modifier.size(26.dp), color = scheme.onPrimary)
                } else {
                    Icon(playIcon, contentDescription = playLabel, modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CinematicSpotlight(
    style: HomeStyle,
    spec: HomeStyleSpec,
    track: Track,
    artistArtworkUrl: String,
    label: String,
    lead: String,
    playLabel: String,
    playing: Boolean,
    resolving: Boolean,
    playIcon: ImageVector,
    animationsEnabled: Boolean,
    onOpen: () -> Unit
) {
    val immersive = style == HomeStyle.Immersive
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spec.inset)
            .aspectRatio(if (immersive) 0.82f else 16f / 11f)
            .clip(RoundedCornerShape(if (immersive) 32.dp else 20.dp))
            .levyraPressable(
                onClick = onOpen,
                onClickLabel = track.title,
                role = Role.Button,
                pressedScale = LevyraPressScale.Surface
            )
    ) {
        if (artistArtworkUrl.isNotBlank()) {
            StudioRemoteArtwork(artistArtworkUrl, track.artist, Modifier.fillMaxSize(), true)
        } else {
            CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = true)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (immersive) StudioImmersiveHeroScrim else StudioCaptionScrim)
        )
        if (immersive) {
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.width(38.dp).height(2.dp).background(Color.White.copy(alpha = 0.78f)))
                Text("IMMERSIVE CUT", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.88f), fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.36f))
                    .padding(5.dp)
                    .clip(CircleShape)
            ) {
                CoverImage(track = track, modifier = Modifier.fillMaxSize())
                Box(
                    Modifier.align(Alignment.Center).size(16.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.82f))
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.title,
                style = if (immersive) {
                    MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                } else {
                    MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif)
                },
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = lead.ifBlank { track.artist },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (resolving) {
                    LevyraLoadingIndicator(modifier = Modifier.size(22.dp), color = Color.Black)
                } else {
                    Icon(playIcon, contentDescription = null, tint = Color.Black)
                }
                Text(
                    text = playLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
            }
        }
    }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
private fun CompactSpotlight(
    style: HomeStyle,
    spec: HomeStyleSpec,
    track: Track,
    artistArtworkUrl: String,
    label: String,
    lead: String,
    playLabel: String,
    playing: Boolean,
    resolving: Boolean,
    playIcon: ImageVector,
    animationsEnabled: Boolean,
    onOpen: () -> Unit
) {
    val minimal = style == HomeStyle.Minimal
    val flow = style == HomeStyle.Flow
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spec.inset)
            .clip(RoundedCornerShape(if (minimal) 4.dp else if (flow) 18.dp else 8.dp))
            .then(
                when {
                    minimal -> Modifier.drawBehind {
                        drawLine(
                            color = LevyraMuted.copy(alpha = 0.24f),
                            start = Offset(0f, size.height - 1.dp.toPx()),
                            end = Offset(size.width, size.height - 1.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    flow -> Modifier.background(
                        Brush.horizontalGradient(
                            0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                            1f to MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.22f)
                        )
                    )
                    else -> Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
                }
            )
            .levyraPressable(
                onClick = onOpen,
                onClickLabel = track.title,
                role = Role.Button,
                pressedScale = LevyraPressScale.Row
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(if (flow) 8.dp else 0.dp)
                .size(if (minimal) 64.dp else if (flow) 80.dp else 88.dp)
                .clip(RoundedCornerShape(if (minimal) 4.dp else if (flow) 14.dp else 0.dp))
        ) {
            CoverImage(track = track, modifier = Modifier.fillMaxSize())
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (minimal) 0.dp else 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (minimal) LevyraMuted else MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (minimal) FontWeight.Medium else FontWeight.Bold
                ),
                color = LevyraText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = LevyraMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onOpen, modifier = Modifier.padding(end = if (flow) 8.dp else 4.dp)) {
            if (resolving) {
                LevyraLoadingIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.primary)
            } else {
                Icon(playIcon, contentDescription = playLabel, tint = LevyraText)
            }
        }
    }

}


private val StudioImmersiveHeroScrim = Brush.verticalGradient(
    0.0f to Color.Black.copy(alpha = 0.10f),
    0.40f to Color.Transparent,
    1.0f to Color.Black.copy(alpha = 0.86f)
)

@Composable
internal fun StudioHomeBackdrop(
    style: HomeStyle,
    accent: Color,
    heroTrack: Track?,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val isLight = LevyraIsLight
    val canvas = homeCanvasColor(isLight)
    val scheme = MaterialTheme.colorScheme
    when (style) {
        HomeStyle.Original -> LevyraHomeAtmosphere(isLight = isLight, accent = accent, modifier = modifier)
        HomeStyle.ExpressiveMax -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(canvas)
                .drawBehind {
                    val primaryCenter = Offset(size.width * 0.12f, size.height * 0.08f)
                    val secondaryCenter = Offset(size.width * 0.92f, size.height * 0.26f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.24f), Color.Transparent),
                            center = primaryCenter,
                            radius = size.width * 0.92f
                        ),
                        center = primaryCenter,
                        radius = size.width * 0.92f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                scheme.tertiary.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = secondaryCenter,
                            radius = size.width * 0.68f
                        ),
                        center = secondaryCenter,
                        radius = size.width * 0.68f
                    )
                }
        )
        HomeStyle.Minimal -> Box(modifier = modifier.fillMaxSize().background(canvas))
        HomeStyle.MaterialExpressive -> {
            val tint = if (accent == LevyraCyan) MaterialTheme.colorScheme.primaryContainer else accent
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(canvas)
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to tint.copy(alpha = 0.34f),
                                1f to Color.Transparent,
                                endY = size.height * 0.5f
                            )
                        )
                    }
            )
        }
        HomeStyle.Flow, HomeStyle.Editorial -> {
            val strength = if (style == HomeStyle.Flow) 0.18f else 0.12f
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(canvas)
                    .drawBehind {
                        drawRect(
                            brush = if (style == HomeStyle.Flow) {
                                Brush.linearGradient(
                                    0f to accent.copy(alpha = strength),
                                    0.45f to accent.copy(alpha = 0.035f),
                                    1f to Color.Transparent,
                                    start = Offset.Zero,
                                    end = Offset(size.width, size.height * 0.38f)
                                )
                            } else {
                                Brush.radialGradient(
                                    colors = listOf(accent.copy(alpha = strength), Color.Transparent),
                                    center = Offset(size.width * 0.82f, size.height * 0.08f),
                                    radius = maxOf(size.width, size.height * 0.34f)
                                )
                            }
                        )
                        if (style == HomeStyle.Editorial) {
                            drawLine(
                                color = accent.copy(alpha = 0.16f),
                                start = Offset(size.width * 0.05f, size.height * 0.16f),
                                end = Offset(size.width * 0.95f, size.height * 0.04f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
            )
        }
        HomeStyle.Immersive -> Box(modifier = modifier.fillMaxSize().background(canvas)) {
            val heroHeight = LevyraHomeDesign.HeroHeight * 1.6f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .graphicsLayer {
                        val scrolled = if (listState.firstVisibleItemIndex > 0) {
                            size.height
                        } else {
                            listState.firstVisibleItemScrollOffset.toFloat()
                        }
                        alpha = (1f - scrolled / size.height.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    }
            ) {
                if (heroTrack != null) {
                    CoverImage(
                        track = heroTrack,
                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.62f },
                        highRes = true
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to accent.copy(alpha = 0.30f),
                                0.55f to canvas.copy(alpha = 0.72f),
                                1f to canvas
                            )
                        )
                )
            }
        }
    }
}

@Composable
internal fun StudioTrackSection(
    style: HomeStyle,
    spec: HomeStyleSpec,
    layout: HomeSectionLayout,
    title: String,
    featured: Boolean,
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    actionsLabel: String,
    onPlay: (Track) -> Unit,
    onPlayAll: (() -> Unit)?,
    onActions: ((Track) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val items = rememberTrackShelfItems(tracks, currentId, onPlay, onActions)
    if (items.isEmpty()) return
    StudioSectionFrame(style, modifier) {
        StudioSectionHeader(style, spec, title, featured, onPlayAll)
        StudioShelf(style, spec, layout, items, isPlaying, actionsLabel)
    }
}

@Composable
internal fun StudioAlbumSection(
    style: HomeStyle,
    spec: HomeStyleSpec,
    layout: HomeSectionLayout,
    title: String,
    featured: Boolean,
    albums: List<AlbumHit>,
    onOpen: (AlbumHit) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = rememberAlbumShelfItems(albums, onOpen)
    if (items.isEmpty()) return
    StudioSectionFrame(style, modifier) {
        StudioSectionHeader(style, spec, title, featured, null)
        if (style == HomeStyle.MaterialExpressive && (layout == HomeSectionLayout.Carousel || layout == HomeSectionLayout.Auto)) {
            MxVinylShelf(spec, items)
        } else {
            StudioShelf(style, spec, layout, items, isPlaying = false, actionsLabel = title)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxVinylShelf(spec: HomeStyleSpec, items: List<StudioShelfItem>) {
    val listState = rememberLazyListState()
    val coverSize = 150.dp
    val discSize = 138.dp
    val grooveColor = Color.White.copy(alpha = 0.07f)
    val discColor = Color(0xFF0E0E10)
    val ring = MaterialTheme.colorScheme.tertiary
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = spec.inset)
    ) {
        itemsIndexed(items, key = { _, item -> item.key }, contentType = { _, _ -> "mx-vinyl" }) { index, item ->
            fun focus(): Float {
                val info = listState.layoutInfo
                val entry = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return 0f
                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                val distance = kotlin.math.abs(viewportCenter - (entry.offset + entry.size / 2f)) /
                    (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(1)
                return (1f - distance * 1.8f).coerceIn(0f, 1f)
            }
            Column(
                modifier = Modifier
                    .width(coverSize + 64.dp)
                    .studioPress(item),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(coverSize), contentAlignment = Alignment.CenterStart) {
                    Box(
                        modifier = Modifier
                            .size(discSize)
                            .graphicsLayer {
                                val f = focus()
                                translationX = (coverSize - discSize * 0.55f).toPx() + 46.dp.toPx() * f
                                rotationZ = 160f * f
                            }
                            .clip(CircleShape)
                            .background(discColor)
                            .drawBehind {
                                val radius = size.minDimension / 2f
                                for (step in 1..7) {
                                    drawCircle(
                                        color = grooveColor,
                                        radius = radius * (0.42f + step * 0.075f),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                                drawCircle(color = ring, radius = radius * 0.4f, style = Stroke(width = 3.dp.toPx()))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(discSize * 0.36f).clip(CircleShape)) {
                            item.artwork(Modifier.fillMaxSize(), false)
                        }
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(discColor))
                    }
                    Box(
                        modifier = Modifier
                            .size(coverSize)
                            .graphicsLayer {
                                val f = focus()
                                rotationZ = -3f * (1f - f)
                                val scale = 0.92f + 0.08f * f
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(MaterialTheme.shapes.large)
                    ) {
                        item.artwork(Modifier.fillMaxSize(), false)
                    }
                }
                Column(modifier = Modifier.padding(end = 40.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = LevyraText,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LevyraMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun StudioArtistSection(
    style: HomeStyle,
    spec: HomeStyleSpec,
    layout: HomeSectionLayout,
    title: String,
    featured: Boolean,
    artists: List<ArtistHit>,
    onOpen: (ArtistHit) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = rememberArtistShelfItems(artists, onOpen)
    if (items.isEmpty()) return
    StudioSectionFrame(style, modifier) {
        StudioSectionHeader(style, spec, title, featured, null)
        StudioShelf(style, spec, layout, items, isPlaying = false, actionsLabel = title)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val MxGalleryShapes = listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Sunny,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Gem,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.SoftBurst,
    MaterialShapes.Arch,
    MaterialShapes.Pentagon
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxHeader(
    kicker: String,
    headline: String,
    greeting: String,
    searchLabel: String,
    settingsLabel: String,
    isResolving: Boolean,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer,
        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp, bottomEnd = 48.dp, bottomStart = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            MxHeroDecor(Modifier.matchParentSize())
            Column(modifier = Modifier.padding(start = 22.dp, end = 14.dp, top = 18.dp, bottom = 20.dp)) {
                MxGreetingRow(kicker, headline, greeting, searchLabel, settingsLabel, isResolving, onSearch, onSettings)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxHeroDecor(modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val burst = MaterialShapes.SoftBurst.toShape()
    val clover = MaterialShapes.Clover4Leaf.toShape()
    val sunny = MaterialShapes.Sunny.toShape()
    val pill = MaterialShapes.Pill.toShape()
    Box(modifier = modifier.clip(RectangleShape)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-56).dp)
                .size(230.dp)
                .graphicsLayer { rotationZ = 18f }
                .clip(burst)
                .background(scheme.tertiary.copy(alpha = 0.22f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-36).dp, y = 40.dp)
                .size(150.dp)
                .graphicsLayer { rotationZ = -12f }
                .clip(clover)
                .background(scheme.secondary.copy(alpha = 0.20f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-24).dp, y = 70.dp)
                .size(44.dp)
                .clip(sunny)
                .background(scheme.primary.copy(alpha = 0.35f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 20.dp, y = 120.dp)
                .size(width = 64.dp, height = 26.dp)
                .graphicsLayer { rotationZ = -24f }
                .clip(pill)
                .background(scheme.tertiary.copy(alpha = 0.28f))
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun StudioChartSection(
    style: HomeStyle,
    spec: HomeStyleSpec,
    title: String,
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    playAllLabel: String,
    changeMarketLabel: String,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onChangeMarket: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) return
    if (style == HomeStyle.ExpressiveMax) {
        StudioExpressiveMaxChart(
            title = title,
            tracks = tracks,
            currentId = currentId,
            isPlaying = isPlaying,
            playAllLabel = playAllLabel,
            changeMarketLabel = changeMarketLabel,
            onPlay = onPlay,
            onPlayAll = onPlayAll,
            onChangeMarket = onChangeMarket,
            modifier = modifier
        )
        return
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StudioChartHeader(
            style = style,
            inset = spec.inset,
            title = title,
            playAllLabel = playAllLabel,
            changeMarketLabel = changeMarketLabel,
            onPlayAll = onPlayAll,
            onChangeMarket = onChangeMarket
        )
        when (style) {
            HomeStyle.Flow -> StudioFlowChart(tracks, currentId, isPlaying, spec, onPlay)
            HomeStyle.Editorial -> StudioEditorialChart(tracks, currentId, isPlaying, spec, onPlay)
            HomeStyle.Minimal -> StudioMinimalChart(tracks, currentId, isPlaying, spec, onPlay)
            HomeStyle.Immersive -> StudioImmersiveChart(tracks, currentId, isPlaying, spec, onPlay)
            HomeStyle.Original, HomeStyle.MaterialExpressive, HomeStyle.ExpressiveMax -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
private fun StudioExpressiveMaxChart(
    title: String,
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    playAllLabel: String,
    changeMarketLabel: String,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onChangeMarket: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(MaterialShapes.SoftBurst.toShape())
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("50", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = scheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmallEmphasized, color = LevyraText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
                Text("THE SHAPE OF NOW", style = MaterialTheme.typography.labelSmall, color = scheme.primary, letterSpacing = 1.8.sp)
            }
            FilledTonalIconButton(onClick = onChangeMarket, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Public, contentDescription = changeMarketLabel)
            }
            FilledIconButton(onClick = onPlayAll, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = playAllLabel)
            }
        }
        StudioExpressiveMaxChartBody(tracks, StudioChartRowState(currentId, isPlaying, onPlay))
    }
}

@Composable
private fun StudioChartHeader(
    style: HomeStyle,
    inset: Dp,
    title: String,
    playAllLabel: String,
    changeMarketLabel: String,
    onPlayAll: () -> Unit,
    onChangeMarket: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = inset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = when (style) {
                HomeStyle.Editorial -> MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif)
                HomeStyle.Immersive -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                HomeStyle.Minimal -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
                else -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            },
            color = LevyraText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        IconButton(onClick = onChangeMarket) {
            Icon(Icons.Rounded.Public, contentDescription = changeMarketLabel, tint = LevyraText)
        }
        FilledIconButton(onClick = onPlayAll, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = playAllLabel)
        }
    }
}

@Composable
private fun StudioFlowChart(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    spec: HomeStyleSpec,
    onPlay: (Track) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), Color.Transparent, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                )
            )
            .padding(vertical = 18.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = spec.inset),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(tracks.take(12), key = { index, track -> "flow-chart-$index-${track.id}" }) { index, track ->
                Column(
                    modifier = Modifier
                        .width(if (index == 0) 188.dp else 148.dp)
                        .padding(top = if (index % 2 == 0) 0.dp else 30.dp, bottom = if (index % 2 == 0) 26.dp else 0.dp)
                        .graphicsLayer { rotationZ = if (index % 2 == 0) -2f else 2f }
                        .levyraPressable(onClick = { onPlay(track) }, onClickLabel = track.title, role = Role.Button),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(if (index % 2 == 0) RoundedCornerShape(topStart = 46.dp, topEnd = 10.dp, bottomEnd = 46.dp, bottomStart = 10.dp) else RoundedCornerShape(topStart = 10.dp, topEnd = 46.dp, bottomEnd = 10.dp, bottomStart = 46.dp))
                ) {
                    CoverImage(track = track, modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp))
                            .background(if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("#${index + 1}", style = MaterialTheme.typography.labelLarge, color = if (index == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    if (track.id == currentId) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.52f))
                                .padding(6.dp)
                        ) {
                            LevyraPlayingIndicator(playing = isPlaying, color = Color.White, size = 12.dp)
                        }
                    }
                }
                LevyraCardCaption(track.title, track.artist, titleLines = 1, reserveTitleLines = false)
            }
        }
        }
    }
}

@Composable
private fun StudioEditorialChart(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    spec: HomeStyleSpec,
    onPlay: (Track) -> Unit
) {
    val visible = tracks.take(6)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spec.inset),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        visible.forEachIndexed { index, track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (index == 0) 112.dp else 76.dp)
                    .levyraPressable(onClick = { onPlay(track) }, onClickLabel = track.title, role = Role.Button),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = (index + 1).toString().padStart(2, '0'),
                    style = if (index == 0) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
                    color = if (track.id == currentId) MaterialTheme.colorScheme.primary else LevyraMuted,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.width(52.dp)
                )
                Box(
                    modifier = Modifier
                        .size(if (index == 0) 96.dp else 60.dp)
                        .clip(if (index == 0) RoundedCornerShape(topEnd = 32.dp) else RoundedCornerShape(8.dp))
                ) {
                    CoverImage(track = track, modifier = Modifier.fillMaxSize())
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = if (index == 0) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium, color = LevyraText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = LevyraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (track.id == currentId) {
                    LevyraPlayingIndicator(
                        playing = isPlaying,
                        color = MaterialTheme.colorScheme.primary,
                        size = 14.dp
                    )
                }
            }
            if (index < visible.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }
        StudioEditorialChartFolio(tracks, StudioChartRowState(currentId, isPlaying, onPlay))
    }
}

@Composable
private fun StudioMinimalChart(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    spec: HomeStyleSpec,
    onPlay: (Track) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = spec.inset)) {
        tracks.take(8).forEachIndexed { index, track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .levyraPressable(onClick = { onPlay(track) }, onClickLabel = track.title, role = Role.Button),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text((index + 1).toString(), style = MaterialTheme.typography.labelLarge, color = LevyraMuted, modifier = Modifier.width(20.dp))
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))) { CoverImage(track, Modifier.fillMaxSize()) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleSmall, color = LevyraText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = LevyraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (track.id == currentId) {
                    LevyraPlayingIndicator(
                        playing = isPlaying,
                        color = MaterialTheme.colorScheme.primary,
                        size = 12.dp
                    )
                }
            }
        }
    }
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
private fun StudioImmersiveChart(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    spec: HomeStyleSpec,
    onPlay: (Track) -> Unit
) {
    LazyRow(contentPadding = PaddingValues(horizontal = spec.inset), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(tracks.take(10), key = { index, track -> "immersive-chart-$index-${track.id}" }) { index, track ->
            val leader = index == 0
            Box(
                modifier = Modifier
                    .width(if (leader) 300.dp else 204.dp)
                    .height(if (leader) 382.dp else 318.dp)
                    .clip(if (leader) RoundedCornerShape(topStart = 72.dp, topEnd = 22.dp, bottomEnd = 72.dp, bottomStart = 22.dp) else RoundedCornerShape(34.dp))
                    .levyraPressable(onClick = { onPlay(track) }, onClickLabel = track.title, role = Role.Button)
            ) {
                CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                Box(Modifier.fillMaxSize().background(StudioImmersiveHeroScrim))
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(18.dp)
                        .size(if (leader) 82.dp else 58.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.32f))
                        .drawBehind {
                            drawCircle(Color.White.copy(alpha = 0.20f), style = Stroke(width = 1.dp.toPx()))
                            drawCircle(Color.White.copy(alpha = 0.10f), radius = size.minDimension * 0.28f, style = Stroke(width = 1.dp.toPx()))
                        }
                )
                Text(
                    text = if (leader) "NO. 01" else (index + 1).toString().padStart(2, '0'),
                    style = if (leader) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp) else MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.align(Alignment.TopStart).padding(if (leader) 24.dp else 18.dp)
                )
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(if (leader) 26.dp else 18.dp)) {
                    if (leader) Text("CHART PREMIERE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.64f), letterSpacing = 1.6.sp)
                    Text(track.title, style = if (leader) MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black) else MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.76f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (track.id == currentId) {
                        LevyraPlayingIndicator(playing = isPlaying, color = Color.White, size = 14.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MxChartSection(
    title: String,
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    playAllLabel: String,
    changeMarketLabel: String,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onChangeMarket: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val wave = remember(tracks) { tracks.take(5) }
    val tail = remember(tracks) { tracks.drop(5).take(45) }
    val shapes = listOf(
        MaterialShapes.Cookie12Sided.toShape(),
        MaterialShapes.Sunny.toShape(),
        MaterialShapes.Clover4Leaf.toShape(),
        MaterialShapes.Gem.toShape(),
        MaterialShapes.Cookie6Sided.toShape(),
        MaterialShapes.SoftBurst.toShape(),
        MaterialShapes.Arch.toShape(),
        MaterialShapes.Pentagon.toShape()
    )
    val waveColor = scheme.primary.copy(alpha = 0.16f)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = LevyraText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            FilledTonalIconButton(onClick = onChangeMarket, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Public, contentDescription = changeMarketLabel)
            }
            FilledIconButton(onClick = onPlayAll, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = playAllLabel)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val path = Path()
                val amplitude = size.width * 0.22f
                val step = size.height / wave.size.coerceAtLeast(1)
                path.moveTo(size.width / 2f - amplitude, 0f)
                for (index in wave.indices) {
                    val top = step * index
                    val fromX = if (index % 2 == 0) size.width / 2f - amplitude else size.width / 2f + amplitude
                    val toX = if (index % 2 == 0) size.width / 2f + amplitude else size.width / 2f - amplitude
                    path.cubicTo(fromX, top + step * 0.5f, toX, top + step * 0.5f, toX, top + step)
                }
                drawPath(path, waveColor, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                wave.forEachIndexed { index, track ->
                    MxWaveRow(
                        rank = index + 1,
                        track = track,
                        shape = if (index < 3) shapes[index] else RoundedCornerShape(20.dp),
                        alignEnd = index % 2 == 1,
                        current = track.id == currentId,
                        isPlaying = isPlaying,
                        onPlay = { onPlay(track) }
                    )
                }
            }
        }
        if (tail.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                itemsIndexed(tail, key = { index, track -> "mx-chart-tail-$index-${track.id}" }, contentType = { _, _ -> "mx-chart-tail" }) { index, track ->
                    val rank = index + 6
                    Column(
                        modifier = Modifier
                            .width(84.dp)
                            .levyraPressable(onClick = { onPlay(track) }, onClickLabel = track.title, role = Role.Button),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(84.dp)) {
                            Box(modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.large)) {
                                CoverImage(track = track, modifier = Modifier.fillMaxSize())
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(scheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rank.toString(),
                                    style = MaterialTheme.typography.labelMediumEmphasized,
                                    color = scheme.onSecondaryContainer
                                )
                            }
                        }
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            color = if (track.id == currentId) scheme.primary else LevyraText,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxWaveRow(
    rank: Int,
    track: Track,
    shape: Shape,
    alignEnd: Boolean,
    current: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    val podium = rank <= 3
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .levyraPressable(onClick = onPlay, onClickLabel = track.title, role = Role.Button, pressedScale = LevyraPressScale.Row),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (alignEnd) {
            MxWaveText(track, alignEnd, current, podium)
            MxWaveArt(rank, track, shape, alignEnd, current, isPlaying, podium)
        } else {
            MxWaveArt(rank, track, shape, alignEnd, current, isPlaying, podium)
            MxWaveText(track, alignEnd, current, podium)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxWaveArt(
    rank: Int,
    track: Track,
    shape: Shape,
    alignEnd: Boolean,
    current: Boolean,
    isPlaying: Boolean,
    podium: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val artSize = mxWaveArtSize(rank)
    val (badgeTone, badgeContent) = mxWaveBadgeColors(rank)
    Box(modifier = Modifier.size(artSize)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(if (podium) badgeTone else scheme.surfaceContainerHigh)
                .padding(if (podium) 5.dp else 0.dp)
                .clip(shape)
        ) {
            CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = rank == 1)
        }
        Box(
            modifier = Modifier
                .align(if (alignEnd) Alignment.TopEnd else Alignment.TopStart)
                .size(if (podium) 44.dp else 34.dp)
                .clip(MaterialShapes.Cookie6Sided.toShape())
                .background(badgeTone),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                style = if (podium) MaterialTheme.typography.titleLargeEmphasized else MaterialTheme.typography.labelLargeEmphasized,
                color = badgeContent
            )
        }
        if (current) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                LevyraPlayingIndicator(playing = isPlaying, color = Color.White, size = 16.dp)
            }
        }
    }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.MxWaveText(track: Track, alignEnd: Boolean, current: Boolean, podium: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = track.title,
            style = if (podium) MaterialTheme.typography.titleLargeEmphasized else MaterialTheme.typography.titleMediumEmphasized,
            color = if (current) scheme.primary else LevyraText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = LevyraMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

}

private fun mxWaveArtSize(rank: Int): Dp = when (rank) {
    1 -> 112.dp
    2, 3 -> 92.dp
    else -> 72.dp
}

@Composable
private fun mxWaveBadgeColors(rank: Int): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (rank) {
        1 -> scheme.tertiary to scheme.onTertiary
        2 -> scheme.primary to scheme.onPrimary
        3 -> scheme.secondary to scheme.onSecondary
        else -> scheme.surfaceContainerHighest to LevyraText
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MxCommentBubbles(
    title: String,
    tracks: List<Track>,
    comments: Map<String, ResonanceCommentSnippet>,
    currentId: String?,
    isPlaying: Boolean,
    playLabel: String,
    onPlay: (Track) -> Unit,
    onOpenComments: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val tones = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer
    )
    val avatarShapes = listOf(
        MaterialShapes.Clover4Leaf.toShape(),
        MaterialShapes.Cookie9Sided.toShape(),
        MaterialShapes.Sunny.toShape()
    )
    val decor = MaterialShapes.SoftBurst.toShape()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = LevyraText,
            modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() }
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(tracks, key = { index, track -> "mx-comment-$index-${track.id}" }, contentType = { _, _ -> "mx-comment" }) { index, track ->
                val (container, content) = tones[index % tones.size]
                val snippet = comments[track.id]
                val bubbleShape = if (index % 2 == 0) {
                    RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomEnd = 32.dp, bottomStart = 6.dp)
                } else {
                    RoundedCornerShape(topStart = 6.dp, topEnd = 32.dp, bottomEnd = 32.dp, bottomStart = 32.dp)
                }
                Column(modifier = Modifier.width(272.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = container,
                        contentColor = content,
                        shape = bubbleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(196.dp)
                            .levyraPressable(onClick = { onOpenComments(track) }, onClickLabel = track.title, role = Role.Button)
                    ) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 34.dp, y = 34.dp)
                                    .size(120.dp)
                                    .graphicsLayer { rotationZ = index * 23f }
                                    .clip(decor)
                                    .background(content.copy(alpha = 0.10f))
                            )
                            Text(
                                text = "\u201C",
                                style = MaterialTheme.typography.displayLargeEmphasized,
                                color = content.copy(alpha = 0.28f),
                                modifier = Modifier.padding(start = 14.dp).offset(y = (-14).dp)
                            )
                            Column(
                                modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                when {
                                    snippet == null || snippet.isLoading -> LevyraLoadingIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = content
                                    )
                                    snippet.text.isNotBlank() -> Text(
                                        text = snippet.text,
                                        style = MaterialTheme.typography.titleMediumEmphasized,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    else -> Spacer(modifier = Modifier.height(1.dp))
                                }
                                if (snippet != null && snippet.author.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(avatarShapes[index % avatarShapes.size])
                                                .background(content.copy(alpha = 0.2f))
                                        ) {
                                            if (snippet.authorAvatarUrl.isNotBlank()) {
                                                StudioRemoteArtwork(snippet.authorAvatarUrl, snippet.author, Modifier.fillMaxSize(), false)
                                            }
                                        }
                                        Text(
                                            text = snippet.author,
                                            style = MaterialTheme.typography.labelLargeEmphasized,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (snippet.countText.isNotBlank()) {
                                            Text(text = snippet.countText, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.size(44.dp).clip(MaterialShapes.Cookie9Sided.toShape())) {
                            CoverImage(track = track, modifier = Modifier.fillMaxSize())
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmallEmphasized,
                                color = if (track.id == currentId) scheme.primary else LevyraText,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = LevyraMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalIconButton(onClick = { onPlay(track) }, shapes = IconButtonDefaults.shapes()) {
                            Icon(
                                imageVector = if (track.id == currentId && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = playLabel
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxOrganic(spec: HomeStyleSpec, items: List<StudioShelfItem>, isPlaying: Boolean, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val lead = items.first()
    val side = items.drop(1).take(2)
    val rest = items.drop(3)
    val sideShapes = listOf(CircleShape, MaterialShapes.Cookie9Sided.toShape())
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .padding(horizontal = spec.inset),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(scheme.secondaryContainer)
                    .studioPress(lead)
            ) {
                lead.artwork(Modifier.fillMaxSize(), true)
                Box(modifier = Modifier.fillMaxSize().background(StudioCaptionScrim))
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 18.dp, end = 18.dp, bottom = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = lead.title,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = lead.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StudioCurrentBadge(lead, isPlaying, Modifier.align(Alignment.TopCenter).padding(top = 22.dp))
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                side.forEachIndexed { index, item ->
                    Column(
                        modifier = Modifier.fillMaxWidth().studioPress(item),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(1f)
                                .clip(sideShapes[index])
                                .background(if (index == 0) scheme.tertiaryContainer else scheme.primaryContainer)
                        ) {
                            item.artwork(Modifier.fillMaxSize(), false)
                            StudioCurrentBadge(item, isPlaying, Modifier.align(Alignment.Center))
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = studioTitleColor(item),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        if (rest.isNotEmpty()) {
            MxTonalTiles(spec, rest, isPlaying)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxMorphCarousel(spec: HomeStyleSpec, items: List<StudioShelfItem>, isPlaying: Boolean, modifier: Modifier) {
    val listState = rememberLazyListState()
    val morph = remember { Morph(MaterialShapes.Cookie9Sided.normalized(), MaterialShapes.Square.normalized()) }
    val itemWidth = 168.dp
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = spec.inset)
    ) {
        itemsIndexed(items, key = { _, item -> item.key }, contentType = { _, _ -> "mx-morph" }) { index, item ->
            fun focus(): Float {
                val info = listState.layoutInfo
                val entry = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return 0f
                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                val itemCenter = entry.offset + entry.size / 2f
                val distance = kotlin.math.abs(viewportCenter - itemCenter) / (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(1)
                return (1f - distance * 1.6f).coerceIn(0f, 1f)
            }
            Column(
                modifier = Modifier
                    .width(itemWidth)
                    .studioPress(item),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .graphicsLayer {
                            val f = focus()
                            val scale = 0.8f + 0.2f * f
                            scaleX = scale
                            scaleY = scale
                            shape = LevyraMorphShape(morph, 0.25f + 0.75f * f)
                            clip = true
                        }
                ) {
                    item.artwork(Modifier.fillMaxSize(), false)
                    StudioCurrentBadge(item, isPlaying, Modifier.align(Alignment.Center))
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = studioTitleColor(item),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .graphicsLayer { alpha = 0.45f + 0.55f * focus() }
                        .basicMarquee()
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LevyraMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer { alpha = focus() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxTonalTiles(spec: HomeStyleSpec, items: List<StudioShelfItem>, isPlaying: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val rows = remember(items) { items.take(6).chunked(2) }
    val glyph = MaterialShapes.Cookie4Sided.toShape()
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spec.inset),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    Surface(
                        color = if (item.current) scheme.secondaryContainer else scheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .studioPress(item, LevyraPressScale.Row)
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.size(52.dp).clip(glyph)) {
                                item.artwork(Modifier.fillMaxSize(), false)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                    color = studioTitleColor(item),
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LevyraMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (item.current) {
                                LevyraPlayingIndicator(playing = isPlaying, color = scheme.primary, size = 12.dp)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
private fun MxBleedHero(
    hero: StudioHero,
    kicker: String,
    headline: String,
    greeting: String,
    searchLabel: String,
    settingsLabel: String,
    playLabel: String,
    isResolving: Boolean,
    topBleed: Dp,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val scheme = MaterialTheme.colorScheme
    val isLight = LevyraIsLight
    val canvas = homeCanvasColor(isLight)
    val playing = hero.isCurrent && hero.isPlaying
    val seed = if (hero.track.accentStart != 0) Color(hero.track.accentStart) else scheme.primary
    val secondarySeed = if (hero.track.accentEnd != 0) Color(hero.track.accentEnd) else scheme.tertiary
    val playShape = rememberLevyraPlayMorphShape(
        playing = playing,
        animated = hero.animationsEnabled,
        spinWhilePlaying = !hero.isResolving
    )
    val spin = rememberSpotlightMotion(hero.animationsEnabled && playing, 0f, 360f, 46_000, reverse = false)
    val breathe = rememberSpotlightMotion(hero.animationsEnabled, 0.16f, 0.84f, 7_200, reverse = true)
    val heroHeight = 540.dp
    val portraitShape = MaterialShapes.Cookie9Sided.toShape()
    val orbitShape = MaterialShapes.Cookie12Sided.toShape()
    val bloomShape = MaterialShapes.Clover8Leaf.toShape()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .levyraPressable(
                onClick = hero.onOpen,
                onClickLabel = hero.track.title,
                role = Role.Button,
                pressedScale = 1f
            )
    ) {
        val totalHeight = heroHeight + topBleed
        val portraitSize = maxWidth * 1.28f
        val orbitSize = portraitSize * 1.10f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .offset(y = -topBleed)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Black,
                            0.68f to Color.Black,
                            0.88f to Color.Black.copy(alpha = 0.48f),
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to seed.copy(alpha = if (isLight) 0.16f else 0.26f),
                            0.52f to secondarySeed.copy(alpha = if (isLight) 0.09f else 0.14f),
                            1.0f to Color.Transparent
                        )
                    )
                    .drawBehind {
                        val center = Offset(size.width * 0.74f, size.height * 0.38f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    seed.copy(alpha = if (isLight) 0.26f else 0.34f),
                                    scheme.tertiary.copy(alpha = if (isLight) 0.10f else 0.16f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.width * 0.86f
                            ),
                            center = center,
                            radius = size.width * 0.86f
                        )
                    }
            )
            // Soft expressive layers frame the cover without hiding the artwork.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = portraitSize * 0.10f, y = topBleed + 32.dp)
                    .size(portraitSize * 1.04f)
                    .graphicsLayer { rotationZ = -3f + breathe.value * 2f }
                    .clip(bloomShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                seed.copy(alpha = if (isLight) 0.16f else 0.22f),
                                scheme.tertiary.copy(alpha = if (isLight) 0.08f else 0.14f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = orbitSize * 0.12f, y = topBleed + 20.dp)
                    .size(orbitSize)
                    .graphicsLayer { rotationZ = spin.value * 0.035f }
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                scheme.primary.copy(alpha = if (isLight) 0.32f else 0.40f),
                                scheme.tertiary.copy(alpha = 0.22f),
                                scheme.primary.copy(alpha = 0.08f)
                            )
                        ),
                        shape = orbitShape
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = portraitSize * 0.14f, y = topBleed + 38.dp)
                    .size(portraitSize)
                    .clip(portraitShape)
                    .border(
                        width = 1.dp,
                        color = scheme.primary.copy(alpha = if (isLight) 0.20f else 0.28f),
                        shape = portraitShape
                    )
            ) {
                if (hero.artistArtworkUrl.isNotBlank()) {
                    StudioRemoteArtwork(
                        url = hero.artistArtworkUrl,
                        description = hero.track.artist,
                        modifier = Modifier.fillMaxSize(),
                        highRes = true
                    )
                } else {
                    CoverImage(
                        track = hero.track,
                        modifier = Modifier.fillMaxSize(),
                        highRes = true
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = if (isLight) 0.05f else 0.18f),
                            0.26f to Color.Transparent,
                            0.52f to canvas.copy(alpha = if (isLight) 0.12f else 0.18f),
                            0.78f to canvas.copy(alpha = if (isLight) 0.78f else 0.84f),
                            1.0f to canvas
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to canvas.copy(alpha = if (isLight) 0.52f else 0.60f),
                            0.40f to canvas.copy(alpha = if (isLight) 0.10f else 0.16f),
                            0.76f to Color.Transparent
                        )
                    )
            )
        }
        MxBleedGreeting(
            modifier = Modifier.align(Alignment.TopStart),
            kicker = kicker,
            headline = headline,
            greeting = greeting,
            searchLabel = searchLabel,
            settingsLabel = settingsLabel,
            isResolving = isResolving,
            onSearch = onSearch,
            onSettings = onSettings
        )
        MxBleedTitle(
            modifier = Modifier.align(Alignment.BottomStart),
            hero = hero,
            radioLabel = strings.homeSoundtrackRadio,
            playLabel = playLabel,
            playing = playing,
            spinAngle = { spin.value },
            breathe = { breathe.value },
            playShape = playShape
        )
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxBleedGreeting(
    modifier: Modifier,
    kicker: String,
    headline: String,
    greeting: String,
    searchLabel: String,
    settingsLabel: String,
    isResolving: Boolean,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    val isLight = LevyraIsLight
    val textColor = if (isLight) LevyraText else Color.White
    val subColor = if (isLight) LevyraText.copy(alpha = 0.76f) else Color.White.copy(alpha = 0.82f)
    val buttonContainer = if (isLight) Color.White.copy(alpha = 0.68f) else Color.Black.copy(alpha = 0.38f)
    val buttonContent = if (isLight) LevyraText else Color.White
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, start = 18.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeLogoBadge()
        Column(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics {
                    contentDescription = greeting
                    heading()
                }
        ) {
            Text(
                text = kicker,
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = subColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMediumEmphasized.copy(
                    fontWeight = FontWeight.Black
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.levyraMarquee()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(
                onClick = onSearch,
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = buttonContainer,
                    contentColor = buttonContent
                ),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Rounded.Search, contentDescription = searchLabel)
            }
            FilledTonalIconButton(
                onClick = onSettings,
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = buttonContainer,
                    contentColor = buttonContent
                ),
                modifier = Modifier.size(46.dp)
            ) {
                if (isResolving) {
                    LevyraLoadingIndicator(modifier = Modifier.size(22.dp), color = buttonContent)
                } else {
                    Icon(Icons.Rounded.Settings, contentDescription = settingsLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxBleedTitle(
    modifier: Modifier,
    hero: StudioHero,
    radioLabel: String,
    playLabel: String,
    playing: Boolean,
    spinAngle: () -> Float,
    breathe: () -> Float,
    playShape: Shape
) {
    val scheme = MaterialTheme.colorScheme
    val isLight = LevyraIsLight
    val canvas = homeCanvasColor(isLight)
    val badgeShape = MaterialShapes.Sunny.toShape()
    val coverStampShape = MaterialShapes.Cookie9Sided.toShape()
    val companionShape1 = MaterialShapes.Cookie9Sided.toShape()
    val companionShape2 = MaterialShapes.Clover4Leaf.toShape()
    val companionShape3 = MaterialShapes.Sunny.toShape()
    val companionShapes = listOf(companionShape1, companionShape2, companionShape3)
    val companions = hero.companionArtworkUrls.take(3)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Surface(
                color = scheme.tertiaryContainer.copy(alpha = 0.92f),
                contentColor = scheme.onTertiaryContainer,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(start = 7.dp, end = 13.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .graphicsLayer { rotationZ = spinAngle() * 2f }
                            .clip(badgeShape)
                            .background(scheme.tertiary)
                    )
                    Text(
                        text = radioLabel.uppercase(Locale.forLanguageTag(LocalLevyraStrings.current.code)),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (companions.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-7).dp)
                ) {
                    companions.forEachIndexed { index, url ->
                        val shape = companionShapes[index % companionShapes.size]
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(shape)
                                .background(canvas)
                                .padding(2.dp)
                                .clip(shape)
                        ) {
                            StudioRemoteArtwork(url, "", Modifier.fillMaxSize(), false)
                        }
                    }
                }
            }
        }
        Text(
            text = hero.label,
            style = MaterialTheme.typography.displaySmallEmphasized.copy(
                fontWeight = FontWeight.Black
            ),
            color = if (isLight) LevyraText else Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            color = scheme.surfaceContainerHigh.copy(alpha = if (isLight) 0.92f else 0.84f),
            contentColor = scheme.onSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 20.dp, bottomEnd = 32.dp, bottomStart = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .graphicsLayer { rotationZ = -6f + 12f * breathe() }
                        .clip(coverStampShape)
                        .background(scheme.tertiary)
                        .padding(2.5.dp)
                        .clip(coverStampShape)
                ) {
                    CoverImage(track = hero.track, modifier = Modifier.fillMaxSize(), highRes = false)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = hero.track.title,
                        style = MaterialTheme.typography.titleMediumEmphasized.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.levyraMarquee()
                    )
                    Text(
                        text = hero.lead.ifBlank { hero.track.artist },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = scheme.onSurfaceVariant.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.levyraMarquee()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(playShape)
                        .background(scheme.tertiary)
                        .levyraPressable(onClick = hero.onOpen, onClickLabel = playLabel, role = Role.Button),
                    contentAlignment = Alignment.Center
                ) {
                    if (hero.isCurrent && hero.isResolving) {
                        LevyraLoadingIndicator(modifier = Modifier.size(28.dp), color = scheme.onTertiary)
                    } else {
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = playLabel,
                            tint = scheme.onTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Immutable
internal class StudioHero(
    val track: Track,
    val artistArtworkUrl: String,
    val companionArtworkUrls: List<String>,
    val label: String,
    val lead: String,
    val isCurrent: Boolean,
    val isPlaying: Boolean,
    val isResolving: Boolean,
    val animationsEnabled: Boolean,
    val onOpen: () -> Unit
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxGreetingRow(
    kicker: String,
    headline: String,
    greeting: String,
    searchLabel: String,
    settingsLabel: String,
    isResolving: Boolean,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics {
                    contentDescription = greeting
                    heading()
                }
        ) {
            Text(
                text = kicker,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = LocalContentColor.current.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineLargeEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.levyraMarquee()
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledIconButton(onClick = onSearch, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.Search, contentDescription = searchLabel)
            }
            FilledTonalIconButton(
                onClick = onSettings,
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = scheme.surfaceContainerLowest.copy(alpha = 0.6f)
                ),
                modifier = Modifier.size(52.dp)
            ) {
                if (isResolving) {
                    LevyraLoadingIndicator(modifier = Modifier.size(24.dp), color = scheme.primary)
                } else {
                    Icon(Icons.Rounded.Settings, contentDescription = settingsLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxSectionHeader(
    spec: HomeStyleSpec,
    title: String,
    featured: Boolean,
    featuredLabel: String,
    playAllLabel: String,
    onPlayAll: (() -> Unit)?,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spec.inset)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(MaterialShapes.Sunny.toShape())
                .background(if (featured) scheme.tertiary else scheme.secondaryContainer)
        )
        Column(modifier = Modifier.weight(1f).semantics { heading() }) {
            if (featured) {
                Text(text = featuredLabel, style = MaterialTheme.typography.labelLargeEmphasized, color = scheme.tertiary)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = LevyraText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        onPlayAll?.let { action ->
            FilledTonalButton(
                onClick = action,
                shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(playAllLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxShapeGallery(spec: HomeStyleSpec, items: List<StudioShelfItem>, isPlaying: Boolean, modifier: Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = spec.inset)
    ) {
        itemsIndexed(items, key = { _, item -> item.key }, contentType = { _, _ -> "mx-gallery" }) { index, item ->
            val shape = MxGalleryShapes[index % MxGalleryShapes.size].toShape()
            Column(
                modifier = Modifier
                    .width(spec.largeCardWidth)
                    .studioPress(item),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    item.artwork(Modifier.fillMaxSize(), false)
                    StudioCurrentBadge(item, isPlaying, Modifier.align(Alignment.Center))
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = studioTitleColor(item),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxSegmentedList(
    spec: HomeStyleSpec,
    items: List<StudioShelfItem>,
    isPlaying: Boolean,
    actionsLabel: String,
    modifier: Modifier
) {
    val visible = remember(items, spec.listLimit) { items.take(spec.listLimit) }
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = spec.inset),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        visible.forEachIndexed { index, item ->
            val outer = 28.dp
            val inner = 6.dp
            val shape = RoundedCornerShape(
                topStart = if (index == 0) outer else inner,
                topEnd = if (index == 0) outer else inner,
                bottomStart = if (index == visible.lastIndex) outer else inner,
                bottomEnd = if (index == visible.lastIndex) outer else inner
            )
            Surface(
                color = if (item.current) scheme.secondaryContainer else scheme.surfaceContainer,
                shape = shape,
                modifier = Modifier
                    .fillMaxWidth()
                    .studioPress(item, LevyraPressScale.Row)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(spec.listThumb)
                            .clip(if (item.current) MaterialShapes.Cookie9Sided.toShape() else MaterialTheme.shapes.large)
                    ) {
                        item.artwork(Modifier.fillMaxSize(), false)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = LevyraText,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LevyraMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.current) {
                        LevyraPlayingIndicator(playing = isPlaying, color = scheme.primary, size = 14.dp)
                    }
                    item.onLongClick?.let { actions ->
                        IconButton(onClick = actions, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = actionsLabel, tint = LevyraMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioSectionFrame(style: HomeStyle, modifier: Modifier, content: @Composable () -> Unit) {
    val gap = if (style == HomeStyle.MaterialExpressive) 12.dp else 8.dp
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(gap)) { content() }
}

private const val MxGridColumns = 3
private const val MxGridRows = 2

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MxPagedGrid(spec: HomeStyleSpec, items: List<StudioShelfItem>, isPlaying: Boolean, modifier: Modifier) {
    val pages = remember(items) { items.take(MxGridColumns * MxGridRows * 3).chunked(MxGridColumns * MxGridRows) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val spacing = 8.dp
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = spacing,
            contentPadding = PaddingValues(horizontal = spec.inset),
            key = { page -> pages.getOrNull(page)?.firstOrNull()?.key ?: "mx-grid-$page" },
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val pageItems = pages.getOrNull(page).orEmpty()
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                pageItems.chunked(MxGridColumns).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        row.forEachIndexed { columnIndex, item ->
                            val shapeIndex = rowIndex * MxGridColumns + columnIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(if (shapeIndex == 0 && page == 0) MaterialShapes.Cookie9Sided.toShape() else MaterialTheme.shapes.large)
                                    .studioPress(item)
                                    .clearAndSetSemantics { contentDescription = "${item.title}, ${item.subtitle}" }
                            ) {
                                item.artwork(Modifier.fillMaxSize(), false)
                                Box(modifier = Modifier.fillMaxSize().background(StudioCaptionScrim))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                                )
                                StudioCurrentBadge(item, isPlaying, Modifier.align(Alignment.TopEnd))
                            }
                        }
                        repeat(MxGridColumns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (pages.size > 1) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (selected) 20.dp else 8.dp,
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                        label = "mx-grid-dot"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (selected) scheme.primary else scheme.outlineVariant,
                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                        label = "mx-grid-dot-color"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MxGenreCloud(zones: List<ExploreZone>, onSelect: (ExploreZone) -> Unit) {
    val dot = MaterialShapes.Cookie6Sided.toShape()
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(zones, key = { _, zone -> zone.id }, contentType = { _, _ -> "mx-genre" }) { _, zone ->
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .levyraPressable(onClick = { onSelect(zone) }, onClickLabel = zone.label, role = Role.Button)
                    .padding(start = 14.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(dot)
                        .background(Color(zone.accentStart))
                )
                Text(
                    text = zone.label,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = LevyraText,
                    maxLines = 1
                )
            }
        }
    }
}

@Immutable
internal class StudioCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val tracks: List<Track>,
    val accent: Color
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MxCollectionFan(title: String, collections: List<StudioCollection>, onOpen: (String) -> Unit) {
    if (collections.isEmpty()) return
    val fanShapes = listOf(
        MaterialShapes.Cookie9Sided.toShape(),
        CircleShape,
        RoundedCornerShape(24.dp)
    )
    val blob = MaterialShapes.SoftBurst.toShape()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = LevyraText,
            modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() }
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(collections, key = { _, item -> item.id }, contentType = { _, _ -> "mx-collection" }) { index, collection ->
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .levyraPressable(onClick = { onOpen(collection.id) }, onClickLabel = collection.title, role = Role.Button),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(196.dp)
                                .graphicsLayer { rotationZ = index * 27f }
                                .clip(blob)
                                .background(collection.accent.copy(alpha = 0.42f))
                        )
                        collection.tracks.forEachIndexed { trackIndex, track ->
                            val offset = (trackIndex - 1) * 46
                            Box(
                                modifier = Modifier
                                    .offset(x = offset.dp, y = (if (trackIndex == 1) -10 else 10).dp)
                                    .size(if (trackIndex == 1) 112.dp else 92.dp)
                                    .graphicsLayer { rotationZ = (trackIndex - 1) * 12f }
                                    .clip(fanShapes[trackIndex % fanShapes.size])
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                CoverImage(track = track, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                    Text(
                        text = collection.title,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = LevyraText,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    if (collection.subtitle.isNotBlank()) {
                        Text(
                            text = collection.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LevyraMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
