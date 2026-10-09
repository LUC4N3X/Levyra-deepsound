package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

private val FreshScopeHeight = 44.dp
private val FreshSpotlightCorner = 28.dp
private val FreshSpotlightThumb = 76.dp
private val FreshMomentThumb = 52.dp
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
    val animated = LocalAnimationsEnabled.current
    val container by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.surfaceContainerHigh,
        label = "freshScopeContainer"
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
        label = "freshScopeContent"
    )
    val corner by animateDpAsState(
        targetValue = if (isSelected && animated) 14.dp else FreshScopeHeight / 2,
        label = "freshScopeCorner"
    )
    Row(
        modifier = Modifier
            .heightIn(min = FreshScopeHeight)
            .clip(RoundedCornerShape(corner))
            .background(container)
            .semantics { selected = isSelected }
            .levyraPressable(
                onClick = onClick,
                onClickLabel = label,
                role = Role.Tab,
                pressedScale = LevyraPressScale.Control
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (global) {
            Icon(
                imageVector = Icons.Rounded.Public,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = label,
            style = LevyraType.cardTitle,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun ExploreFreshSpotlight(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    modifier: Modifier = Modifier,
    onOpenReleases: () -> Unit,
    onPlay: (Track) -> Unit,
    actions: @Composable (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    val feedKey = tracks.first().id
    val pagerState = key(feedKey) { rememberPagerState(pageCount = { tracks.size }) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(
                start = LevyraHomeDesign.HorizontalInset,
                end = if (tracks.size > 1) LevyraHomeDesign.HorizontalInset + 24.dp else LevyraHomeDesign.HorizontalInset
            ),
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
                onOpenReleases = onOpenReleases,
                onPlay = { onPlay(track) },
                actions = { actions(track) }
            )
        }
        if (tracks.size > 1) {
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
    onOpenReleases: () -> Unit,
    onPlay: () -> Unit,
    actions: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    val scrim = remember {
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.10f),
            0.42f to Color.Transparent,
            0.72f to Color.Black.copy(alpha = 0.55f),
            1f to Color.Black.copy(alpha = 0.92f)
        )
    }
    val eyebrow = exploreReleaseEyebrow(track, strings)
    val caption = exploreReleaseCaption(track, strings)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = eyebrow,
                    style = LevyraType.overline,
                    color = colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.title,
                    style = LevyraType.screenTitle,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = LevyraType.artist,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            actions()
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val aspectRatio = if (maxWidth > 600.dp) 1.9f else 1.34f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(FreshSpotlightCorner))
                    .background(colors.surfaceContainerHigh)
                    .levyraPressable(
                        onClick = onOpenReleases,
                        onClickLabel = strings.exploreNewReleases,
                        role = Role.Button,
                        pressedScale = LevyraPressScale.Tile
                    )
            ) {
                CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = true, zoom = 1.18f)
                Box(Modifier.fillMaxSize().background(scrim))
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = caption,
                        style = LevyraType.contentTitle,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(FreshSpotlightThumb)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceContainerHighest)
                    ) {
                        CoverImage(track = track, modifier = Modifier.fillMaxSize())
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer)
                        .levyraPressable(
                            onClick = onPlay,
                            enabled = !isResolving,
                            onClickLabel = if (isPlaying) strings.pause else strings.play,
                            role = Role.Button,
                            pressedScale = LevyraPressScale.Control
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isResolving -> DiscoveryLoadingIndicator(Modifier.size(24.dp), colors.onPrimaryContainer)
                        isCurrent -> LevyraPlayingIndicator(playing = isPlaying, color = colors.onPrimaryContainer)
                        else -> Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(27.dp)
                        )
                    }
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
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val isSelected = index == selected
            val width by animateDpAsState(
                targetValue = if (isSelected) 18.dp else 6.dp,
                label = "freshDotWidth"
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.primary else colors.outlineVariant)
            )
        }
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
            end = LevyraHomeDesign.HorizontalInset + LevyraHomeDesign.TrackColumnPeek
        ),
        pageSpacing = LevyraHomeDesign.TrackColumnGap,
        beyondViewportPageCount = 1
    ) { page ->
        Column(modifier = Modifier.fillMaxWidth()) {
            pages[page].forEachIndexed { index, track ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = FreshMomentThumb + 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FreshMomentRowHeight)
            .clip(LevyraCardDesign.ThumbShape)
            .semantics { selected = isCurrent }
            .levyraPressable(
                onClick = onPlay,
                onClickLabel = strings.playNow,
                role = Role.Button,
                pressedScale = LevyraPressScale.Row
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(FreshMomentThumb)
                .clip(LevyraCardDesign.ThumbShape)
                .background(colors.surfaceContainerHigh)
        ) {
            CoverImage(track = track, modifier = Modifier.fillMaxSize())
            if (isCurrent || isResolving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
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
                style = LevyraType.contentTitle,
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

internal fun exploreReleaseEyebrow(track: Track, strings: LevyraStrings): String =
    when (exploreReleaseKind(track)) {
        ExploreReleaseKind.Album -> strings.newAlbum
        ExploreReleaseKind.Single -> strings.newSingle
        ExploreReleaseKind.Release -> strings.newRelease
    }

internal fun exploreReleaseCaption(track: Track, strings: LevyraStrings): String =
    when (exploreReleaseKind(track)) {
        ExploreReleaseKind.Album -> track.album.trim()
        else -> strings.newReleaseSubtitle
    }
