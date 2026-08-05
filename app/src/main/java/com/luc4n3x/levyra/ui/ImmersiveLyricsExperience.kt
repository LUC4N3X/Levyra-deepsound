package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricSection
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.PremiumSeekbar
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import kotlin.math.abs

internal fun immersiveActiveLyricIndex(positionMs: Long, lyrics: List<LyricLine>): Int {
    if (lyrics.isEmpty()) return -1
    val exact = lyrics.indexOfFirst { positionMs in it.startMs..it.endMs }
    return if (exact >= 0) exact else lyrics.indexOfLast { it.startMs <= positionMs }
}

@Composable
internal fun ImmersiveLyricsExperience(
    state: LevyraUiState,
    viewModel: LevyraViewModel
) {
    val strings = LevyraStrings.forCode(state.languageCode)
    val track = state.currentTrack
    val lyrics = remember(state.lyrics) { state.lyrics.filterNot(LyricLine::isMetadata) }
    val activeIndex = if (state.lyricsSynced) immersiveActiveLyricIndex(state.positionMs, lyrics) else -1
    val listState = rememberLazyListState()
    var largeText by remember(track?.id) { mutableStateOf(false) }
    var followPlayback by remember(track?.id) { mutableStateOf(true) }

    BackHandler(onBack = viewModel::closeLyrics)
    LaunchedEffect(activeIndex, followPlayback, lyrics.size) {
        if (followPlayback && activeIndex >= 0) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(130f)
            .background(LevyraBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val expanded = maxWidth >= 720.dp || (maxWidth >= 600.dp && maxWidth > maxHeight * 1.08f)
        val primary = track?.let { Color(it.accentStart) } ?: LevyraCyan
        val secondary = track?.let { Color(it.accentEnd) } ?: LevyraViolet

        LyricsBackdrop(track, primary, secondary, Modifier.fillMaxSize())

        if (expanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                LyricsNowPlayingPane(
                    state = state,
                    track = track,
                    strings = strings,
                    primary = primary,
                    secondary = secondary,
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(0.88f)
                        .widthIn(max = 480.dp)
                )
                Surface(
                    color = LevyraInk.copy(alpha = 0.76f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(34.dp),
                    modifier = Modifier
                        .weight(1.12f)
                        .fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LyricsToolbar(
                            state = state,
                            strings = strings,
                            primary = primary,
                            secondary = secondary,
                            largeText = largeText,
                            followPlayback = followPlayback,
                            onTranslation = {
                                viewModel.setLyricsTranslationEnabled(!state.lyricsTranslationEnabled)
                            },
                            onLargeText = { largeText = !largeText },
                            onFollow = { followPlayback = !followPlayback },
                            onClose = viewModel::closeLyrics
                        )
                        LyricsReader(
                            state = state,
                            lyrics = lyrics,
                            activeIndex = activeIndex,
                            sections = state.lyricsSections,
                            listState = listState,
                            primary = primary,
                            secondary = secondary,
                            largeText = largeText,
                            onUserScroll = { followPlayback = false },
                            onSeekLine = { line ->
                                seekToLyricLine(state, line, viewModel)
                                followPlayback = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                LyricsToolbar(
                    state = state,
                    strings = strings,
                    primary = primary,
                    secondary = secondary,
                    largeText = largeText,
                    followPlayback = followPlayback,
                    onTranslation = {
                        viewModel.setLyricsTranslationEnabled(!state.lyricsTranslationEnabled)
                    },
                    onLargeText = { largeText = !largeText },
                    onFollow = { followPlayback = !followPlayback },
                    onClose = viewModel::closeLyrics
                )
                LyricsReader(
                    state = state,
                    lyrics = lyrics,
                    activeIndex = activeIndex,
                    sections = state.lyricsSections,
                    listState = listState,
                    primary = primary,
                    secondary = secondary,
                    largeText = largeText,
                    onUserScroll = { followPlayback = false },
                    onSeekLine = { line ->
                        seekToLyricLine(state, line, viewModel)
                        followPlayback = true
                    },
                    modifier = Modifier.weight(1f)
                )
                LyricsTransport(
                    state = state,
                    strings = strings,
                    primary = primary,
                    secondary = secondary,
                    viewModel = viewModel,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

private fun seekToLyricLine(state: LevyraUiState, line: LyricLine, viewModel: LevyraViewModel) {
    if (state.durationMs <= 0L) return
    viewModel.seekTo((line.startMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f))
}

@Composable
private fun LyricsBackdrop(
    track: Track?,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val artwork = track?.largeThumbnailUrl?.ifBlank { track.thumbnailUrl }.orEmpty()
    Box(modifier = modifier) {
        if (artwork.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artwork)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.20f
                        scaleY = 1.20f
                        alpha = 0.30f
                    }
                    .blur(88.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(primary.copy(alpha = 0.30f), Color.Transparent, secondary.copy(alpha = 0.18f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun LyricsToolbar(
    state: LevyraUiState,
    strings: LevyraStrings,
    primary: Color,
    secondary: Color,
    largeText: Boolean,
    followPlayback: Boolean,
    onTranslation: () -> Unit,
    onLargeText: () -> Unit,
    onFollow: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.currentTrack?.title ?: strings.lyrics,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(state.currentTrack?.artist.orEmpty())
                    if (state.lyricsSynced) append(" · ").append(strings.synced)
                    if (state.lyricsConfidence > 0) append(" · ").append(state.lyricsConfidence).append('%')
                },
                color = Color.White.copy(alpha = 0.52f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        LyricsToolButton(Icons.Rounded.Translate, state.lyricsTranslationEnabled, primary, onTranslation)
        LyricsToolButton(Icons.Rounded.TextFields, largeText, secondary, onLargeText)
        LyricsToolButton(Icons.Rounded.Refresh, followPlayback, primary, onFollow)
        LyricsRoundButton(Icons.Rounded.Close, strings.closeLyrics, onClose)
    }
}

@Composable
private fun LyricsNowPlayingPane(
    state: LevyraUiState,
    track: Track?,
    strings: LevyraStrings,
    primary: Color,
    secondary: Color,
    viewModel: LevyraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val artwork = track?.largeThumbnailUrl?.ifBlank { track.thumbnailUrl }.orEmpty()
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = strings.lyrics,
                color = primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp
            )
            Text(
                text = track?.title.orEmpty(),
                color = Color.White,
                fontSize = 25.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track?.artist.orEmpty(),
                color = Color.White.copy(alpha = 0.56f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally)
                .graphicsLayer {
                    shadowElevation = 28f
                    shape = RoundedCornerShape(34.dp)
                    clip = true
                }
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(34.dp))
                .background(Brush.linearGradient(listOf(primary.copy(alpha = 0.42f), secondary.copy(alpha = 0.34f)))),
            contentAlignment = Alignment.Center
        ) {
            if (artwork.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artwork)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(160)
                        .build(),
                    contentDescription = track?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Subject,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
        LyricsTransport(state, strings, primary, secondary, viewModel, Modifier.fillMaxWidth())
    }
}

@Composable
private fun LyricsReader(
    state: LevyraUiState,
    lyrics: List<LyricLine>,
    activeIndex: Int,
    sections: List<LyricSection>,
    listState: LazyListState,
    primary: Color,
    secondary: Color,
    largeText: Boolean,
    onUserScroll: () -> Unit,
    onSeekLine: (LyricLine) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) onUserScroll()
    }
    val sectionStarts = remember(sections, lyrics) {
        sections.mapNotNull { section ->
            val index = lyrics.indexOfFirst { it.startMs >= section.startMs }
            if (index >= 0) index to section else null
        }.toMap()
    }

    when {
        state.lyricsLoading && lyrics.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primary, strokeWidth = 3.dp)
            }
        }
        lyrics.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = LevyraStrings.forCode(state.languageCode).lyricsUnavailable,
                    color = Color.White.copy(alpha = 0.56f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        else -> {
            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 42.dp, bottom = 170.dp),
                verticalArrangement = Arrangement.spacedBy(if (largeText) 18.dp else 12.dp)
            ) {
                itemsIndexed(
                    items = lyrics,
                    key = { index, line -> "immersive-${line.startMs}-${line.endMs}-$index" }
                ) { index, line ->
                    sectionStarts[index]?.let { section ->
                        Text(
                            text = section.type.name.replace('_', ' '),
                            color = secondary.copy(alpha = 0.82f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.3.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                        )
                    }
                    ImmersiveLyricLine(
                        line = line,
                        positionMs = state.positionMs,
                        active = index == activeIndex,
                        distance = if (activeIndex >= 0) abs(index - activeIndex) else 0,
                        synced = state.lyricsSynced,
                        primary = primary,
                        secondary = secondary,
                        largeText = largeText,
                        onClick = { onSeekLine(line) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImmersiveLyricLine(
    line: LyricLine,
    positionMs: Long,
    active: Boolean,
    distance: Int,
    synced: Boolean,
    primary: Color,
    secondary: Color,
    largeText: Boolean,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = when {
            active -> 1f
            !synced -> 0.76f
            distance == 1 -> 0.54f
            distance == 2 -> 0.30f
            else -> 0.17f
        },
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 430f),
        label = "immersive-line-alpha"
    )
    val background by animateColorAsState(
        targetValue = if (active) primary.copy(alpha = 0.11f) else Color.Transparent,
        label = "immersive-line-background"
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.015f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
        label = "immersive-line-scale"
    )
    val text = remember(line, positionMs, active) {
        if (line.isInstrumental && line.text.isBlank()) AnnotatedString("♪")
        else immersiveLyricText(line, positionMs, active)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .border(
                if (active) 1.dp else 0.dp,
                if (active) primary.copy(alpha = 0.20f) else Color.Transparent,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = if (largeText) 13.dp else 9.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = when {
                active && largeText -> 31.sp
                active -> 27.sp
                largeText -> 25.sp
                else -> 21.sp
            },
            lineHeight = when {
                active && largeText -> 37.sp
                active -> 33.sp
                largeText -> 31.sp
                else -> 27.sp
            },
            fontWeight = if (active) FontWeight.Black else FontWeight.Bold
        )
        if (line.translated.isNotBlank()) {
            Text(
                text = line.translated,
                color = secondary.copy(alpha = if (active) 0.82f else 0.58f),
                fontSize = if (largeText) 15.sp else 13.sp,
                lineHeight = if (largeText) 21.sp else 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        if (line.romanized.isNotBlank() && active) {
            Text(
                text = line.romanized,
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun immersiveLyricText(line: LyricLine, positionMs: Long, active: Boolean): AnnotatedString {
    if (!active || line.words.isEmpty()) return AnnotatedString(line.text)
    return buildAnnotatedString {
        line.words.forEach { word ->
            val completed = positionMs >= word.endMs
            val inProgress = positionMs in word.startMs..word.endMs
            withStyle(
                SpanStyle(
                    color = when {
                        completed -> Color.White
                        inProgress -> Color.White.copy(alpha = 0.92f)
                        else -> Color.White.copy(alpha = 0.40f)
                    },
                    fontWeight = if (completed || inProgress) FontWeight.Black else FontWeight.Bold
                )
            ) {
                append(word.text)
            }
        }
    }
}

@Composable
private fun LyricsTransport(
    state: LevyraUiState,
    strings: LevyraStrings,
    primary: Color,
    secondary: Color,
    viewModel: LevyraViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        color = LevyraInk.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.11f)),
        shape = RoundedCornerShape(26.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PremiumSeekbar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                bufferedPositionMs = state.bufferedPositionMs,
                onSeekTo = { seekMs ->
                    if (state.durationMs > 0L) {
                        viewModel.seekTo((seekMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f))
                    }
                },
                activeColor = primary,
                trailingColor = secondary,
                inactiveColor = Color.White.copy(alpha = 0.14f),
                isPlaying = state.isPlaying,
                animated = state.animationsEnabled
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LyricsSeekButton(Icons.Rounded.Remove, "15") { viewModel.seekBy(-15_000L) }
                LyricsRoundButton(Icons.Rounded.SkipPrevious, strings.previous, viewModel::previous)
                Surface(
                    color = Color.Transparent,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(54.dp)
                        .clickable(onClick = viewModel::togglePlay)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(primary, secondary)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isResolving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.3.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (state.isPlaying) strings.pause else strings.play,
                                tint = Color.White,
                                modifier = Modifier.size(29.dp)
                            )
                        }
                    }
                }
                LyricsRoundButton(Icons.Rounded.SkipNext, strings.next, viewModel::next)
                LyricsSeekButton(Icons.Rounded.Add, "15") { viewModel.seekBy(15_000L) }
            }
        }
    }
}

@Composable
private fun LyricsSeekButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        shape = CircleShape,
        modifier = Modifier
            .size(42.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(18.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun LyricsToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.09f)
        ),
        shape = CircleShape,
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else Color.White.copy(alpha = 0.76f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LyricsRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        shape = CircleShape,
        modifier = Modifier
            .size(42.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
