package com.luc4n3x.levyra.ui

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
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

private val FreshChipHeight = 42.dp
private val FreshArtworkCorner = 28.dp
private val FreshArtworkMaxWidth = 360.dp
private val FreshArtworkMinWidth = 180.dp
private const val FreshArtworkScreenHeightRatio = 0.32f
private val FreshSpotlightPeek = 52.dp
private val FreshMomentThumb = 52.dp
private val FreshMomentRowHeight = 68.dp
private val FreshMomentDividerInset = FreshMomentThumb + 14.dp

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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        scopes.forEach { scope ->
            ExploreFreshScopeChip(
                label = when (scope) {
                    ExploreFreshScope.Local -> localLabel
                    ExploreFreshScope.World -> strings.freshScopeWorld
                },
                global = scope == ExploreFreshScope.World,
                isSelected = scope == selected,
                onClick = { onSelect(scope) }
            )
        }
    }
}

@Composable
private fun ExploreFreshScopeChip(
    label: String,
    global: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (isSelected) colors.primaryContainer else colors.surfaceContainerHigh.copy(alpha = 0.45f),
        label = "freshScopeContainer"
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        label = "freshScopeContent"
    )
    val outline by animateColorAsState(
        targetValue = if (isSelected) colors.primary.copy(alpha = 0.6f) else colors.outlineVariant.copy(alpha = 0.35f),
        label = "freshScopeOutline"
    )
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics { selected = isSelected }
            .levyraPressable(
                onClick = onClick,
                onClickLabel = label,
                role = Role.Tab,
                pressedScale = LevyraPressScale.Control
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(FreshChipHeight)
                .clip(CircleShape)
                .background(container)
                .border(1.dp, outline, CircleShape)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (global) {
                Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = LevyraType.cardTitle.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    onOpenAlbum: () -> Unit,
    actions: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    val card = exploreReleaseCard(track)
    val titleStyle = LevyraType.sectionTitle.copy(
        fontSize = 22.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold
    )
    val subtitleStyle = titleStyle.copy(
        fontSize = 15.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Normal
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primaryContainer.copy(alpha = 0.55f))
                    .border(0.75.dp, colors.primary.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    )
                    Text(
                        text = exploreReleaseEyebrow(card.kind, strings).uppercase(Locale.ROOT),
                        style = LevyraType.overline.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 9.5.sp
                        ),
                        color = colors.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            actions()
        }

        val textBlockHeight = with(LocalDensity.current) {
            (titleStyle.lineHeight.toPx() * 2 + subtitleStyle.lineHeight.toPx()).toDp()
        }
        Column(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .height(textBlockHeight),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = card.title,
                style = titleStyle,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = card.subtitle,
                style = subtitleStyle,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        ExploreFreshSpotlightArtwork(
            track = track,
            opensAlbum = card.opensAlbum,
            isCurrent = isCurrent && !card.opensAlbum,
            isPlaying = isPlaying,
            isResolving = isResolving && !card.opensAlbum,
            onOpen = if (card.opensAlbum) onOpenAlbum else onPlay
        )
    }
}

@Composable
private fun ExploreFreshSpotlightArtwork(
    track: Track,
    opensAlbum: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onOpen: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    val glowStart = remember(track.accentStart) { Color(track.accentStart) }
    val glowEnd = remember(track.accentEnd) { Color(track.accentEnd) }
    val borderGradient = remember(glowStart, glowEnd) {
        Brush.linearGradient(
            listOf(
                glowStart.copy(alpha = 0.55f),
                glowEnd.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.15f)
            )
        )
    }
    val scrim = remember {
        Brush.verticalGradient(
            0.50f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.65f)
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(FreshArtworkCorner),
                clip = false,
                ambientColor = glowStart.copy(alpha = 0.32f),
                spotColor = glowEnd.copy(alpha = 0.42f)
            )
            .clip(RoundedCornerShape(FreshArtworkCorner))
            .background(colors.surfaceContainerHigh)
            .border(1.dp, borderGradient, RoundedCornerShape(FreshArtworkCorner))
            .levyraPressable(
                onClick = onOpen,
                enabled = !isResolving,
                onClickLabel = when {
                    opensAlbum -> strings.openAlbum
                    isPlaying -> strings.pause
                    else -> strings.play
                },
                role = Role.Button,
                pressedScale = LevyraPressScale.Tile
            )
    ) {
        CoverImage(
            track = exploreCoverArtworkTrack(track),
            modifier = Modifier.fillMaxSize(),
            highRes = true
        )
        if (isCurrent || isResolving) {
            Box(Modifier.fillMaxSize().background(scrim))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .size(46.dp)
                    .shadow(8.dp, CircleShape, ambientColor = colors.primary.copy(alpha = 0.4f))
                    .clip(CircleShape)
                    .background(colors.primaryContainer)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isResolving) {
                    DiscoveryLoadingIndicator(Modifier.size(20.dp), colors.onPrimaryContainer)
                } else {
                    LevyraPlayingIndicator(playing = isPlaying, color = colors.onPrimaryContainer)
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
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LevyraHomeDesign.HorizontalInset)
            .heightIn(min = 48.dp)
            .levyraPressable(
                onClick = onShowAll,
                onClickLabel = title,
                role = Role.Button,
                pressedScale = LevyraPressScale.Row
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = LevyraType.sectionTitle,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).semantics { heading() }
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(26.dp)
        )
    }
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
            pages[page].forEachIndexed { index, track ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = FreshMomentDividerInset),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                }
                val isCurrent = track.id == currentTrackId
                ExploreFreshMomentRow(
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
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    actions: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val thumbShape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FreshMomentRowHeight)
            .clip(shape)
            .background(if (isCurrent) colors.primary.copy(alpha = 0.08f) else Color.Transparent)
            .semantics { selected = isCurrent }
            .levyraPressable(
                onClick = onPlay,
                onClickLabel = strings.playNow,
                role = Role.Button,
                pressedScale = LevyraPressScale.Row
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
        Column(
            modifier = Modifier.weight(1f),
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
        actions()
    }
}

internal fun exploreReleaseEyebrow(kind: ExploreReleaseKind, strings: LevyraStrings): String =
    when (kind) {
        ExploreReleaseKind.Album -> strings.newAlbum
        ExploreReleaseKind.Single -> strings.newSingle
        ExploreReleaseKind.Release -> strings.newRelease
    }
