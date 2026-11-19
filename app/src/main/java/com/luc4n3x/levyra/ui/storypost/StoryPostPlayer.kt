package com.luc4n3x.levyra.ui.storypost

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.luc4n3x.levyra.ui.components.LevyraSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.storypost.StoryPost
import com.luc4n3x.levyra.data.storypost.StoryPostKind
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStoryPostCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraStoryPostStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.player.formatMixChapterTime
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText

private const val STORY_POST_DETAILS_MAX_FRACTION = 0.45f

@Composable
internal fun StoryPostPlayer(
    post: StoryPost?,
    track: Track,
    isPlaying: Boolean,
    isVideoMode: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCollapse: () -> Unit,
    onShowVideo: () -> Unit,
    videoSurface: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val postStrings = remember(strings.code) { LevyraStoryPostCatalog.forCode(strings.code) }
    val isVideoPost = post?.kind == StoryPostKind.Video

    LaunchedEffect(track.id, post?.kind, isVideoMode) {
        if (isVideoPost && !isVideoMode) onShowVideo()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevyraBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        IconButton(onClick = onCollapse, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = strings.close, tint = LevyraText)
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            when {
                isVideoPost -> videoSurface(Modifier.fillMaxSize())
                post != null && post.images.isNotEmpty() -> key(post.id) {
                    StoryPostPhotoPager(post = post, postStrings = postStrings)
                }
                else -> AsyncImage(
                    model = track.largeThumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        val detailsMaxHeight = (LocalConfiguration.current.screenHeightDp * STORY_POST_DETAILS_MAX_FRACTION).dp
        Column(
            modifier = Modifier
                .heightIn(max = detailsMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            val title = post?.title ?: track.title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = LevyraText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = (post?.artist ?: track.artist).ifBlank { post?.sourceName ?: track.album }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LevyraMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val caption = post?.caption.orEmpty().trim()
            if (caption.isNotBlank() && caption != title.trim()) {
                Spacer(Modifier.size(12.dp))
                StoryPostCaption(caption = caption, postId = post?.id.orEmpty())
            }
            StoryPostSeekBar(positionMs = positionMs, durationMs = durationMs, trackId = track.id, onSeek = onSeek)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = strings.previous, tint = LevyraText)
                }
                FilledIconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = LevyraText,
                        contentColor = LevyraBlack
                    )
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) strings.pause else strings.play,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = strings.next, tint = LevyraText)
                }
            }
        }
    }
}

@Composable
private fun StoryPostPhotoPager(post: StoryPost, postStrings: LevyraStoryPostStrings) {
    val total = post.images.size
    val pagerState = rememberPagerState { total }
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = post.images[page],
                contentDescription = postStrings.photoPosition(page + 1, total),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (total > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .semantics { contentDescription = postStrings.photoPosition(pagerState.currentPage + 1, total) },
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(total) { index ->
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryPostCaption(caption: String, postId: String) {
    var expanded by rememberSaveable(postId) { mutableStateOf(false) }
    Text(
        text = caption,
        style = MaterialTheme.typography.bodyMedium,
        color = LevyraText,
        maxLines = if (expanded) Int.MAX_VALUE else 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState(), enabled = expanded)
            .clickable { expanded = !expanded }
    )
}

@Composable
private fun StoryPostSeekBar(positionMs: Long, durationMs: Long, trackId: String, onSeek: (Float) -> Unit) {
    var dragFraction by remember(trackId) { mutableStateOf<Float?>(null) }
    val fraction = dragFraction
        ?: if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    LevyraSlider(
        value = fraction,
        onValueChange = { dragFraction = it },
        onValueChangeFinished = {
            dragFraction?.let(onSeek)
            dragFraction = null
        },
        enabled = durationMs > 0L,
        colors = SliderDefaults.colors(
            thumbColor = LevyraCyan,
            activeTrackColor = LevyraCyan,
            inactiveTrackColor = LevyraMuted.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        val shownMs = dragFraction?.let { (it * durationMs).toLong() } ?: positionMs
        Text(formatMixChapterTime(shownMs), style = MaterialTheme.typography.labelMedium, color = LevyraMuted)
        Text(formatMixChapterTime(durationMs), style = MaterialTheme.typography.labelMedium, color = LevyraMuted)
    }
}
