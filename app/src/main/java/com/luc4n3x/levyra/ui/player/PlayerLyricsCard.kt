package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.LyricsLatencyProfiles
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.lyrics.activeLyricIndex
import com.luc4n3x.levyra.ui.lyrics.lyricsLineFocusPositionMs
import com.luc4n3x.levyra.ui.lyrics.lyricsOffsetPosition
import com.luc4n3x.levyra.ui.lyrics.rememberLyricsAudioOutputRoute
import com.luc4n3x.levyra.ui.lyrics.rememberLyricsPlaybackClock
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import java.util.Locale
import kotlinx.coroutines.delay

private const val LyricsCardFocusFraction = 0.34f
private const val LyricsCardInactiveAlpha = 0.38f
private const val LyricsCardUnsyncedAlpha = 0.86f
private const val LyricsCardManualScrollHoldMs = 2_600L
private const val LyricsCardEdgeFade = 0.12f
private const val LyricsCardAccentShade = 0.72f
private const val LyricsCardSurfaceAlpha = 0.84f

internal fun playerLyricsCardLines(lines: List<LyricLine>): List<LyricLine> =
    lines.filter { line ->
        !line.isMetadata && line.role != LyricVocalRole.BACKGROUND && line.text.isNotBlank()
    }

@Composable
internal fun PlayerLyricsCard(
    trackId: String,
    lines: List<LyricLine>,
    synced: Boolean,
    loading: Boolean,
    positionMs: Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
    latencyProfiles: LyricsLatencyProfiles,
    interactive: Boolean,
    animated: Boolean,
    cornerRadius: Dp,
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    onSeekToMs: (Long) -> Unit,
    onShowArtwork: () -> Unit,
    onOpenFullLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    val shape = RoundedCornerShape(cornerRadius)
    val cardLines = remember(lines) { playerLyricsCardLines(lines) }
    val route = rememberLyricsAudioOutputRoute()
    val offsetMs = latencyProfiles.resolve(route?.stableKey, route?.bluetooth == true)
    val currentOffsetMs by rememberUpdatedState(offsetMs)
    val clock = rememberLyricsPlaybackClock(
        trackKey = trackId,
        reportedPositionMs = positionMs,
        isPlaying = isPlaying,
        speed = playbackSpeed,
        smoothingEnabled = synced && animated
    )
    val activeIndex by remember(cardLines, synced, clock, animated) {
        derivedStateOf {
            if (synced) {
                activeLyricIndex(
                    lyricsLineFocusPositionMs(lyricsOffsetPosition(clock.positionMs, currentOffsetMs), animated),
                    cardLines
                )
            } else {
                -1
            }
        }
    }
    val background = remember(accent) {
        Brush.verticalGradient(
            listOf(
                accent.darkened(LyricsCardAccentShade).copy(alpha = LyricsCardSurfaceAlpha),
                Color.Black.copy(alpha = LyricsCardSurfaceAlpha)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(BorderStroke(LevyraPlayerDesign.Hairline, surfaces.outline), shape)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = LevyraPlayerDesign.SpaceLg, end = LevyraPlayerDesign.SpaceXs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.lyrics.uppercase(Locale.forLanguageTag(strings.code)),
                    color = surfaces.contentMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
                PlayerGlassIconButton(
                    icon = Icons.Rounded.OpenInFull,
                    contentDescription = strings.playerLyricsOpenFull,
                    onClick = onOpenFullLyrics,
                    size = LevyraPlayerDesign.HeaderButtonCompact,
                    iconSize = 17.dp,
                    enabled = interactive
                )
                PlayerGlassIconButton(
                    icon = Icons.Rounded.Album,
                    contentDescription = strings.playerLyricsShowArtwork,
                    onClick = onShowArtwork,
                    size = LevyraPlayerDesign.HeaderButtonCompact,
                    iconSize = 18.dp,
                    enabled = interactive
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (cardLines.isEmpty()) {
                    PlayerLyricsCardMessage(
                        text = if (loading) strings.playerLyricsSearching else strings.playerLyricsUnavailable,
                        color = surfaces.contentMuted
                    )
                } else {
                    PlayerLyricsCardList(
                        trackId = trackId,
                        lines = cardLines,
                        synced = synced,
                        activeIndex = activeIndex,
                        interactive = interactive,
                        animated = animated,
                        contentColor = surfaces.content,
                        onLineClick = { line ->
                            haptics.perform(LevyraHapticAction.SeekSnap)
                            onSeekToMs((line.startMs + currentOffsetMs).coerceAtLeast(0L))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerLyricsCardList(
    trackId: String,
    lines: List<LyricLine>,
    synced: Boolean,
    activeIndex: Int,
    interactive: Boolean,
    animated: Boolean,
    contentColor: Color,
    onLineClick: (LyricLine) -> Unit
) {
    val listState = remember(trackId) { LazyListState() }
    var manualScroll by remember(trackId) { mutableStateOf(false) }
    var manualReleaseToken by remember(trackId) { mutableIntStateOf(0) }
    var positioned by remember(trackId) { mutableStateOf(false) }

    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> manualScroll = true
                is DragInteraction.Stop, is DragInteraction.Cancel -> manualReleaseToken += 1
            }
        }
    }
    LaunchedEffect(manualReleaseToken) {
        if (manualReleaseToken == 0) return@LaunchedEffect
        delay(LyricsCardManualScrollHoldMs)
        manualScroll = false
    }
    LaunchedEffect(activeIndex, manualScroll, synced, lines) {
        if (!synced || manualScroll || activeIndex !in lines.indices) return@LaunchedEffect
        if (!positioned || !animated) {
            listState.scrollToItem(activeIndex)
            positioned = true
        } else {
            listState.animateScrollToItem(activeIndex)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val focusPadding = maxHeight * LyricsCardFocusFraction
        LazyColumn(
            state = listState,
            userScrollEnabled = interactive,
            contentPadding = PaddingValues(
                top = if (synced) focusPadding else LevyraPlayerDesign.SpaceMd,
                bottom = maxHeight - focusPadding
            ),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXxs),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    val mask = Brush.verticalGradient(
                        0f to Color.Transparent,
                        LyricsCardEdgeFade to Color.Black,
                        1f - LyricsCardEdgeFade to Color.Black,
                        1f to Color.Transparent
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush = mask, blendMode = BlendMode.DstIn)
                    }
                }
        ) {
            itemsIndexed(
                items = lines,
                key = { index, line -> "$index:${line.startMs}" }
            ) { index, line ->
                val targetAlpha = when {
                    !synced -> LyricsCardUnsyncedAlpha
                    index == activeIndex -> 1f
                    else -> LyricsCardInactiveAlpha
                }
                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = if (animated) LevyraMotion.standard() else snap(),
                    label = "player-lyrics-line-alpha"
                )
                Text(
                    text = line.text,
                    color = contentColor,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LevyraPlayerDesign.SpaceSm)
                        .clip(RoundedCornerShape(LevyraPlayerDesign.SpaceMd))
                        .clickable(enabled = interactive && synced) { onLineClick(line) }
                        .graphicsLayer { this.alpha = alpha }
                        .padding(horizontal = LevyraPlayerDesign.SpaceSm, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerLyricsCardMessage(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(LevyraPlayerDesign.SpaceXl),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

private fun Color.darkened(amount: Float): Color {
    val keep = 1f - amount.coerceIn(0f, 1f)
    return Color(red = red * keep, green = green * keep, blue = blue * keep, alpha = alpha)
}
