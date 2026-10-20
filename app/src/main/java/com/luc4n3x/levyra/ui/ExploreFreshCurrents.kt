package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import kotlin.math.abs
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.LevyraSectionShowAll
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.Nocturne78Palette
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraType
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape

private val FreshArtworkCorner = 36.dp
private val FreshArtworkCornerResting = 20.dp
private const val FreshArtworkRestingScale = 0.92f
private val FreshArtworkMaxWidth = 360.dp
private val FreshArtworkMinWidth = 180.dp
private const val FreshArtworkScreenHeightRatio = 0.32f
private val FreshSpotlightPeek = 52.dp
private val FreshMomentThumb = 52.dp
private const val FreshCardAspect = 0.84f
private const val FreshCardTiltDegrees = -14f
private const val FreshCardRollDegrees = 2.5f
private const val FreshCardCameraDistance = 14f
private const val FreshParallaxScale = 1.22f
private const val FreshParallaxShift = 0.16f
private val FreshTextDrift = 56.dp
private val FreshPlaySize = 56.dp
private val FreshScopeHeight = 44.dp
private val FreshScopeInnerCorner = 8.dp
private val FreshScopeGap = 4.dp
private val FreshMomentRowHeight = 68.dp

@Composable
internal fun ExploreFreshScopeRail(
    scopes: List<ExploreFreshScope>,
    selected: ExploreFreshScope,
    localLabel: String,
    modifier: Modifier = Modifier,
    onSelect: (ExploreFreshScope) -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LevyraHomeDesign.HorizontalInset),
        horizontalArrangement = Arrangement.spacedBy(FreshScopeGap)
    ) {
        scopes.forEachIndexed { index, scope ->
            ExploreFreshScopeSegment(
                label = when (scope) {
                    ExploreFreshScope.Local -> localLabel
                    ExploreFreshScope.World -> strings.freshScopeWorld
                },
                isSelected = scope == selected,
                first = index == 0,
                last = index == scopes.lastIndex,
                onClick = { onSelect(scope) }
            )
        }
    }
}

@Composable
private fun RowScope.ExploreFreshScopeSegment(
    label: String,
    isSelected: Boolean,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val outer = FreshScopeHeight / 2
    val inner = levyraExpressiveToggleCorner(
        checked = isSelected,
        unchecked = FreshScopeInnerCorner,
        checkedCorner = outer,
        label = "fresh-scope-corner"
    )
    val shape = RoundedCornerShape(
        topStart = if (first) outer else inner,
        bottomStart = if (first) outer else inner,
        topEnd = if (last) outer else inner,
        bottomEnd = if (last) outer else inner
    )
    val container by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.surfaceContainerHigh,
        label = "fresh-scope-container"
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
        label = "fresh-scope-content"
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .levyraPressable(
                onClick = onClick,
                role = Role.Tab,
                pressedScale = LevyraPressScale.Control
            )
            .semantics { selected = isSelected },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FreshScopeHeight)
                .clip(shape)
                .background(container),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = content,
                style = LevyraType.cardTitle.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}

@Composable
internal fun ExploreFreshSpotlight(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    modifier: Modifier = Modifier,
    onPlay: (Track) -> Unit,
    onOpenAlbum: (Track) -> Unit,
    actions: @Composable (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    val feedKey = tracks.first().id
    val pagerState = key(feedKey) { rememberPagerState(pageCount = { tracks.size }) }
    val multiPage = tracks.size > 1
    val configuration = LocalConfiguration.current
    val cardWidth = remember(configuration.screenWidthDp, configuration.screenHeightDp, multiPage) {
        val available = configuration.screenWidthDp.dp - LevyraHomeDesign.HorizontalInset * 2 -
            if (multiPage) FreshSpotlightPeek else 0.dp
        minOf(available, configuration.screenHeightDp.dp * FreshArtworkScreenHeightRatio, FreshArtworkMaxWidth)
            .coerceAtLeast(FreshArtworkMinWidth)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(cardWidth),
            contentPadding = PaddingValues(horizontal = LevyraHomeDesign.HorizontalInset),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1
        ) { page ->
            val track = tracks[page]
            val isCurrent = track.id == currentTrackId
            ExploreFreshSpotlightPage(
                pageOffset = {
                    (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                },
                track = track,
                isCurrent = isCurrent,
                isPlaying = isPlaying && isCurrent,
                isResolving = isResolving && isCurrent,
                onPlay = { onPlay(track) },
                onOpenAlbum = { onOpenAlbum(track) },
                actions = { actions(track) }
            )
        }
        if (multiPage) {
            ExploreFreshPageDots(
                count = tracks.size,
                selected = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ExploreFreshSpotlightPage(
    pageOffset: () -> Float,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    onOpenAlbum: () -> Unit,
    actions: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val nocturne = LevyraIsNocturne
    val animationsEnabled = LocalAnimationsEnabled.current && !nocturne
    val card = exploreReleaseCard(track)
    val palette = rememberFreshSpotlightPalette(track)
    val accent = palette.accent
    val glowEnd = palette.glowEnd
    val ink = palette.ink
    val scrim = remember(ink) {
        Brush.verticalGradient(
            0.00f to Color.Black.copy(alpha = 0.34f),
            0.22f to Color.Transparent,
            0.48f to Color.Transparent,
            0.78f to ink.copy(alpha = 0.78f),
            1.00f to ink.copy(alpha = 0.96f)
        )
    }
    val buttonFill = palette.buttonFill
    val onOpen = if (card.opensAlbum) onOpenAlbum else onPlay
    val playing = isCurrent && isPlaying && !card.opensAlbum

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(FreshCardAspect)
            .graphicsLayer {
                val offset = if (animationsEnabled) pageOffset() else 0f
                val focus = 1f - abs(offset)
                val corner = FreshArtworkCornerResting + (FreshArtworkCorner - FreshArtworkCornerResting) * focus
                val scale = FreshArtworkRestingScale + (1f - FreshArtworkRestingScale) * focus
                scaleX = scale
                scaleY = scale
                rotationY = offset * FreshCardTiltDegrees
                rotationZ = offset * FreshCardRollDegrees
                cameraDistance = FreshCardCameraDistance * density
                shape = RoundedCornerShape(if (nocturne) Nocturne78Design.cardCorner.toPx() else corner.toPx())
                clip = true
                shadowElevation = if (nocturne) 0f else (20.dp * focus).toPx()
                ambientShadowColor = accent.copy(alpha = 0.32f)
                spotShadowColor = glowEnd.copy(alpha = 0.42f)
            }
            .background(ink)
            .levyraPressable(
                onClick = onOpen,
                enabled = !(isResolving && !card.opensAlbum),
                onClickLabel = freshSpotlightActionLabel(card.opensAlbum, playing, strings),
                role = Role.Button,
                pressedScale = LevyraPressScale.Tile
            )
    ) {
        CoverImage(
            track = exploreCoverArtworkTrack(track),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val offset = if (animationsEnabled) pageOffset() else 0f
                    scaleX = FreshParallaxScale
                    scaleY = FreshParallaxScale
                    translationX = offset * size.width * FreshParallaxShift
                },
            highRes = true
        )
        Box(Modifier.fillMaxSize().background(scrim))
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exploreReleaseEyebrow(card.kind, strings).uppercase(Locale.ROOT),
                style = LevyraType.overline.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.0.sp,
                    fontSize = 10.5.sp
                ),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(buttonFill)
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            )
            Spacer(Modifier.weight(1f))
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                actions()
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .graphicsLayer {
                    val offset = if (animationsEnabled) pageOffset() else 0f
                    alpha = 1f - abs(offset) * 0.7f
                    translationX = offset * FreshTextDrift.toPx()
                }
                .padding(start = 18.dp, end = 14.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.title,
                    style = LevyraType.sectionTitle.copy(
                        fontSize = 24.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = card.subtitle,
                    style = LevyraType.metadata.copy(fontWeight = FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(FreshPlaySize)
                    .clip(
                        rememberLevyraPlayMorphShape(
                            playing = playing,
                            animated = animationsEnabled,
                            spinWhilePlaying = !isResolving
                        )
                    )
                    .background(buttonFill),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isResolving && !card.opensAlbum -> DiscoveryLoadingIndicator(Modifier.size(22.dp), ink)
                    card.opensAlbum -> Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = ink,
                        modifier = Modifier.size(24.dp)
                    )
                    playing -> LevyraPlayingIndicator(playing = true, color = ink)
                    else -> Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = ink,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreFreshPageDots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val isSelected = index == selected
            val width by animateDpAsState(
                targetValue = if (isSelected) 22.dp else 6.dp,
                label = "freshDotWidth"
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) colors.primary
                        else colors.onSurface.copy(alpha = 0.22f)
                    )
            )
        }
    }
}

@Composable
internal fun ExploreFreshMomentHeader(
    title: String,
    modifier: Modifier = Modifier,
    onShowAll: () -> Unit
) {
    LevyraSectionHeader(
        title = title,
        modifier = modifier.padding(horizontal = LevyraHomeDesign.HorizontalInset),
        actions = { LevyraSectionShowAll(onClick = onShowAll) }
    )
}

@Composable
internal fun ExploreFreshMomentRail(
    pages: List<List<Track>>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    modifier: Modifier = Modifier,
    onPlay: (Track) -> Unit,
    actions: @Composable (Track) -> Unit
) {
    if (pages.isEmpty()) return
    val feedKey = pages.first().first().id
    val pagerState = key(feedKey) { rememberPagerState(pageCount = { pages.size }) }
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = LevyraHomeDesign.HorizontalInset,
            end = LevyraHomeDesign.HorizontalInset +
                if (pages.size > 1) LevyraHomeDesign.TrackColumnPeek else 0.dp
        ),
        pageSpacing = LevyraHomeDesign.TrackColumnGap,
        beyondViewportPageCount = 1
    ) { page ->
        Column(modifier = Modifier.fillMaxWidth()) {
            val pageTracks = pages[page]
            pageTracks.forEachIndexed { index, track ->
                if (index > 0) Spacer(modifier = Modifier.height(3.dp))
                val isCurrent = track.id == currentTrackId
                ExploreFreshMomentRow(
                    groupShape = levyraGroupedListShape(index, pageTracks.size),
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying && isCurrent,
                    isResolving = isResolving && isCurrent,
                    onPlay = { onPlay(track) },
                    actions = { actions(track) }
                )
            }
        }
    }
}

@Composable
private fun ExploreFreshMomentRow(
    groupShape: Shape,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    actions: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FreshMomentRowHeight)
            .clip(groupShape)
            .background(colors.surfaceContainerHigh.copy(alpha = if (isCurrent) 0.95f else 0.62f))
            .semantics { selected = isCurrent }
            .levyraPressable(
                onClick = onPlay,
                onClickLabel = strings.playNow,
                role = Role.Button,
                pressedScale = LevyraPressScale.Row
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ExploreFreshMomentArtwork(track, isCurrent, isPlaying, isResolving)
        ExploreFreshMomentDetails(track, isCurrent, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
private fun ExploreFreshMomentArtwork(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val thumbShape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(FreshMomentThumb)
            .shadow(
                elevation = if (isCurrent) 8.dp else 4.dp,
                shape = thumbShape,
                ambientColor = colors.primary.copy(alpha = if (isCurrent) 0.35f else 0.12f),
                spotColor = colors.primary.copy(alpha = if (isCurrent) 0.45f else 0.15f)
            )
            .clip(thumbShape)
            .background(colors.surfaceContainerHigh)
    ) {
        CoverImage(track = track, modifier = Modifier.fillMaxSize())
        if (isCurrent || isResolving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f)),
                contentAlignment = Alignment.Center
            ) {
                if (isResolving) {
                    DiscoveryLoadingIndicator(Modifier.size(20.dp), Color.White)
                } else {
                    LevyraPlayingIndicator(playing = isPlaying, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ExploreFreshMomentDetails(
    track: Track,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LevyraCardDesign.CaptionLineGap)
    ) {
        Text(
            text = track.title,
            style = LevyraType.contentTitle.copy(
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = if (isCurrent) colors.primary else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = LevyraType.caption,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun exploreReleaseEyebrow(kind: ExploreReleaseKind, strings: LevyraStrings): String =
    when (kind) {
        ExploreReleaseKind.Album -> strings.newAlbum
        ExploreReleaseKind.Single -> strings.newSingle
        ExploreReleaseKind.Release -> strings.newRelease
    }

private data class FreshSpotlightPalette(val accent: Color, val glowEnd: Color, val ink: Color, val buttonFill: Color)

@Composable
private fun rememberFreshSpotlightPalette(track: Track): FreshSpotlightPalette {
    val nocturne = LevyraIsNocturne
    return remember(track.accentStart, track.accentEnd, nocturne) {
        if (nocturne) FreshSpotlightPalette(Nocturne78Palette.cyan, Nocturne78Palette.panel, Nocturne78Palette.ink, Nocturne78Palette.cyan)
        else {
            val accent = Color(track.accentStart)
            FreshSpotlightPalette(accent, Color(track.accentEnd), lerp(accent, Color.Black, 0.72f), lerp(accent, Color.White, 0.72f))
        }
    }
}

private fun freshSpotlightActionLabel(opensAlbum: Boolean, playing: Boolean, strings: LevyraStrings): String = when {
    opensAlbum -> strings.openAlbum
    playing -> strings.pause
    else -> strings.play
}
