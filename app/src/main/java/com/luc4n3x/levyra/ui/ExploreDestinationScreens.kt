package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText

internal const val ExploreNewReleasesDestination = "explore-destination-new-releases"
internal const val ExploreMoodsDestination = "explore-destination-moods"
private const val ExploreMoodDestinationPrefix = "explore-destination-mood:"
private val ExploreDestinationHeaderHeight = 66.dp

internal fun exploreMoodDestination(zoneId: String): String = "$ExploreMoodDestinationPrefix$zoneId"
internal fun exploreMoodDestinationId(destination: String?): String? =
    destination?.takeIf { it.startsWith(ExploreMoodDestinationPrefix) }
        ?.removePrefix(ExploreMoodDestinationPrefix)
        ?.takeIf(String::isNotBlank)

@Composable
internal fun ExploreCollectionDestinationScreen(
    title: String,
    subtitle: String?,
    tracks: List<Track>,
    isLoading: Boolean,
    currentTrackId: String?,
    isPlaying: Boolean,
    strings: LevyraStrings,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayTrack: (Track) -> Unit
) {
    BackHandler(onBack = onBack)
    ExploreDestinationSurface(
        title = title,
        subtitle = subtitle,
        strings = strings,
        onBack = onBack,
        trailing = if (tracks.isNotEmpty()) {
            {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(LevyraCyan, CircleShape)
                        .clickable(onClick = onPlayAll),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = strings.play,
                        tint = LevyraBlack,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        } else {
            null
        }
    ) { contentPadding ->
        when {
            isLoading && tracks.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LevyraCyan, strokeWidth = 3.dp)
            }

            tracks.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.exploreEmpty,
                    color = LevyraMuted,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = contentPadding.calculateTopPadding() + 10.dp,
                    bottom = 130.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tracks, key = { track -> "explore-destination-track-${track.id}" }) { track ->
                    ExploreDestinationTrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        isPlaying = isPlaying && track.id == currentTrackId,
                        onClick = { onPlayTrack(track) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ExploreNewReleasesDestinationScreen(
    releases: List<AlbumHit>,
    isLoading: Boolean,
    strings: LevyraStrings,
    onBack: () -> Unit,
    onOpenRelease: (AlbumHit) -> Unit
) {
    BackHandler(onBack = onBack)
    ExploreDestinationSurface(
        title = strings.exploreNewReleases,
        subtitle = null,
        strings = strings,
        onBack = onBack
    ) { contentPadding ->
        when {
            isLoading && releases.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LevyraCyan, strokeWidth = 3.dp)
            }
            releases.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.exploreEmpty,
                    color = LevyraMuted,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = contentPadding.calculateTopPadding() + 10.dp,
                    bottom = 130.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = releases,
                    key = { release -> "ytm-release-${release.browseId.ifBlank { release.title + release.artist }}" }
                ) { release ->
                    ExploreDestinationReleaseRow(
                        release = release,
                        onClick = { onOpenRelease(release) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreDestinationReleaseRow(
    release: AlbumHit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp)
            .clip(shape)
            .background(LevyraPanel.copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = release.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(62.dp).clip(RoundedCornerShape(9.dp))
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = release.title,
                color = LevyraText,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(release.artist, release.year)
                    .filter(String::isNotBlank)
                    .joinToString(" • "),
                color = LevyraMuted,
                fontSize = 12.5.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = LevyraText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
internal fun ExploreMoodsDestinationScreen(
    zones: List<ExploreZone>,
    strings: LevyraStrings,
    onBack: () -> Unit,
    onOpenZone: (ExploreZone) -> Unit
) {
    BackHandler(onBack = onBack)
    ExploreDestinationSurface(
        title = strings.exploreMoods,
        subtitle = strings.exploreSubtitle,
        strings = strings,
        onBack = onBack
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = 130.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(zones.chunked(2), key = { pair -> pair.joinToString("|") { it.id } }) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExploreDestinationMoodCard(
                        zone = pair.first(),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenZone(pair.first()) }
                    )
                    val trailing = pair.getOrNull(1)
                    if (trailing == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        ExploreDestinationMoodCard(
                            zone = trailing,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenZone(trailing) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreDestinationSurface(
    title: String,
    subtitle: String?,
    strings: LevyraStrings,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraBlack)
            .zIndex(24f)
    ) {
        content(PaddingValues(top = statusBarTop + ExploreDestinationHeaderHeight))
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .background(LevyraBlack.copy(alpha = 0.96f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(LevyraPanel, CircleShape)
                    .semantics { role = Role.Button }
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.back,
                    tint = LevyraText,
                    modifier = Modifier.size(21.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = LevyraText,
                    fontSize = 21.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.takeIf(String::isNotBlank)?.let { detail ->
                    Text(
                        text = detail,
                        color = LevyraMuted,
                        fontSize = 12.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun ExploreDestinationTrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(if (isCurrent) LevyraCyan.copy(alpha = 0.11f) else LevyraPanel.copy(alpha = 0.72f))
            .border(
                BorderStroke(
                    1.dp,
                    if (isCurrent) LevyraCyan.copy(alpha = 0.44f) else Color.White.copy(alpha = 0.08f)
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(9.dp))
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = track.title,
                color = LevyraText,
                fontSize = 14.5.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = LevyraMuted,
                fontSize = 12.5.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier.size(38.dp).background(
                if (isCurrent) LevyraCyan else Color.White.copy(alpha = 0.08f),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (isCurrent) LevyraBlack else LevyraText,
                modifier = Modifier.size(if (isPlaying) 20.dp else 21.dp)
            )
        }
    }
}

@Composable
private fun ExploreDestinationMoodCard(
    zone: ExploreZone,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val start = Color(zone.accentStart)
    val end = Color(zone.accentEnd)
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .heightIn(min = 68.dp)
            .clip(shape)
            .background(LevyraPanel.copy(alpha = 0.80f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)), shape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(7.dp)
                .background(Brush.verticalGradient(listOf(start, end)))
        )
        Text(
            text = zone.label,
            color = LevyraText,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)
        )
    }
}
