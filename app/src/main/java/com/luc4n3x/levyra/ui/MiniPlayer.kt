package com.luc4n3x.levyra.ui

import androidx.compose.runtime.getValue
import com.luc4n3x.levyra.ui.player.*
import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.ui.components.levyraMarquee
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.key
import com.luc4n3x.levyra.ui.components.LocalDockMorph
import com.luc4n3x.levyra.ui.components.dockMorphHeight
import com.luc4n3x.levyra.ui.components.dockMorphPadding
import com.luc4n3x.levyra.ui.components.dockFoldHeight
import com.luc4n3x.levyra.ui.components.dockFoldWidth
import com.luc4n3x.levyra.ui.components.dockLerpSize
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import androidx.compose.animation.core.snap
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.ui.player.PlayerMorphAnchors
import com.luc4n3x.levyra.ui.player.PlayerMorphSlot
import com.luc4n3x.levyra.ui.player.playerAxisDragGestures
import com.luc4n3x.levyra.ui.player.playerMorphAnchor
import com.luc4n3x.levyra.ui.player.playerSwipeContentAlpha
import com.luc4n3x.levyra.ui.player.rememberPlayerSwipeMotion
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm

private val MiniPlayerCardBase = PlayerDarkSurface
internal val MiniPlayerToggleRing = 54.dp
internal val MiniPlayerToggleCore = 36.dp
internal val MiniPlayerRingStroke = 3.dp
internal const val MiniPlayerSpinMillis = 14_000
private val MiniPlayerTrayTop = 4.dp
private val MiniPlayerTrayBottom = 10.dp
private val MiniPlayerCardGutter = 12.dp
private val MiniPlayerGlowElevation = 18.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MiniPlayer(
    model: MiniPlayerModel,
    morphAnchors: PlayerMorphAnchors,
    compaction: () -> Float,
    artworkHidden: () -> Boolean,
    playbackActions: MiniPlayerPlaybackActions,
    expansionActions: MiniPlayerExpansionActions,
    fixedAccents: PlayerAccentPair? = null
) {
    val track = model.track
    val isPlaying = model.isPlaying
    val isResolving = model.isResolving
    val liveRadio = track.isLiveRadio()
    val animated = model.animated
    val microMotion = LocalLevyraVisualCapabilities.current.microMotion
    val gesturesEnabled = model.gesturesEnabled
    val strings = LocalLevyraStrings.current
    val radioStrings = com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog.forCode(strings.code)
    val miniRightToLeft = LocalLayoutDirection.current == LayoutDirection.Rtl
    val miniHaptics = LocalLevyraHaptics.current
    val fallbackPalette = remember(track.accentStart, track.accentEnd) {
        ArtworkPalette(track.accentStart, track.accentEnd)
    }
    val paletteKey = remember(track.id, track.thumbnailUrl, track.largeThumbnailUrl) {
        ArtworkPaletteCache.key(
            trackId = track.id,
            thumbnailUrl = track.thumbnailUrl,
            largeThumbnailUrl = track.largeThumbnailUrl
        )
    }
    val activePalette by rememberArtworkPalette(
        paletteKey = paletteKey,
        artworkUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
        fallback = fallbackPalette,
        retainPreviousUntilLoaded = true
    )
    val targetAccentStart = Color(activePalette.start)
    val targetAccentEnd = Color(activePalette.end)
    val artworkTargets = remember(targetAccentStart, targetAccentEnd) {
        harmonizePlayerAccents(targetAccentStart, targetAccentEnd)
    }
    val harmonizedTargets = fixedAccents ?: artworkTargets
    val accentStart by animateColorAsState(
        targetValue = harmonizedTargets.primary,
        animationSpec = if (animated) LevyraPlayerDesign.paletteTween() else snap(),
        label = "mini-accent-start"
    )
    val accentEnd by animateColorAsState(
        targetValue = harmonizedTargets.secondary,
        animationSpec = if (animated) LevyraPlayerDesign.paletteTween() else snap(),
        label = "mini-accent-end"
    )
    val miniProgressColor = accentStart.playerMix(Color.White, 0.72f)
    val miniPrimaryContent = LevyraPlayerDesign.TextPrimary
    val miniSecondaryContent = LevyraPlayerDesign.TextSecondary
    val animatedProgress = animateFloatAsState(
        targetValue = model.progress.coerceIn(0f, 1f),
        animationSpec = if (animated) tween(420, easing = LinearOutSlowInEasing) else snap(),
        label = "mini-progress"
    )
    val swipe = rememberPlayerSwipeMotion(animated)
    val cardShape = CircleShape
    val artworkShape = MaterialShapes.Cookie9Sided.toShape()
    val horizontalGesturesEnabled = miniPlayerHorizontalGesturesEnabled(
        gesturesEnabled = gesturesEnabled,
        swipeTrackChangeEnabled = model.swipeTrackChangeEnabled,
        liveRadio = liveRadio
    )

    val dockMorph = LocalDockMorph.current
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .dockMorphPadding(
                    dockMorph,
                    start = MiniPlayerCardGutter,
                    end = MiniPlayerCardGutter,
                    top = MiniPlayerTrayTop,
                    bottom = MiniPlayerTrayBottom
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .dockMorphHeight(dockMorph, compaction, LevyraPlayerDesign.MiniHeight, LevyraPlayerDesign.MiniHeightCompact)
                    .playerMorphAnchor(morphAnchors, PlayerMorphSlot.MiniCard)
                    .shadow(
                        elevation = MiniPlayerGlowElevation,
                        shape = cardShape,
                        ambientColor = accentStart.copy(alpha = 0.40f),
                        spotColor = accentStart.copy(alpha = 0.55f)
                    )
                    .clip(cardShape)
                    .drawWithCache {
                        val fill = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to MiniPlayerCardBase.playerAmbienceMix(accentStart, 0.56f),
                                0.55f to MiniPlayerCardBase.playerAmbienceMix(accentStart, 0.32f),
                                1f to MiniPlayerCardBase.playerAmbienceMix(accentEnd, 0.22f)
                            )
                        )
                        val sheen = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.07f), Color.Transparent)
                        )
                        onDrawWithContent {
                            drawRect(fill)
                            drawRect(sheen)
                            drawContent()
                        }
                    }
                    .playerAxisDragGestures(
                        key = "${track.id}:$horizontalGesturesEnabled",
                        enabled = gesturesEnabled,
                        rightToLeft = miniRightToLeft,
                        edgeZonesEnabled = false
                    ) { event ->
                        handleMiniPlayerDragEvent(
                            event = event,
                            trackId = track.id,
                            playbackActions = playbackActions,
                            expansionActions = expansionActions,
                            haptics = miniHaptics,
                            horizontalGesturesEnabled = horizontalGesturesEnabled,
                            swipe = swipe
                        )
                    }
                    .semantics { onClick(label = strings.expandPlayer, action = null) }
                    .pressable(pressedScale = LevyraPressScale.Row, onClick = playbackActions.open)
                    .padding(start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedContent(
                    targetState = track,
                    modifier = Modifier
                        .weight(1f)
                        .clipToBounds()
                        .onSizeChanged { size -> swipe.carryLimitPx = size.width * MiniPlayerCarryFraction },
                    transitionSpec = { LevyraMotion.miniTrackSwap(animated) },
                    contentKey = { it.id },
                    label = "mini-track"
                ) { shownTrack ->
                    val current = shownTrack.id == track.id
                    Row(
                        modifier = Modifier.graphicsLayer {
                            val offset = swipe.offsetFor(shownTrack.id)
                            translationX = offset
                            alpha = playerSwipeContentAlpha(offset, size.width)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .then(if (current) Modifier.playerMorphAnchor(morphAnchors, PlayerMorphSlot.Mini) else Modifier)
                                .dockLerpSize(compaction, LevyraPlayerDesign.MiniArtwork, LevyraPlayerDesign.MiniArtworkCompact)
                                .animateEnterExit(
                                    enter = LevyraMotion.miniArtworkEnter(microMotion),
                                    exit = ExitTransition.None
                                )
                                .graphicsLayer { alpha = if (current && artworkHidden()) 0f else 1f }
                        ) {
                            MiniPlayerSpinningArtwork(
                                track = shownTrack,
                                spinning = current && isPlaying && animated && microMotion,
                                shape = artworkShape
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .animateEnterExit(
                                    enter = LevyraMotion.miniMetadataEnter(microMotion, model.stepDirection),
                                    exit = ExitTransition.None
                                ),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = shownTrack.title,
                                color = miniPrimaryContent,
                                fontSize = 15.sp,
                                lineHeight = LevyraTypeRhythm.lineHeight(15.sp),
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.levyraMarquee(enabled = animated, repeatDelayMillis = 3_200)
                            )
                            Text(
                                text = if (liveRadio) {
                                    model.liveNowPlaying.ifBlank { "${radioStrings.live} / ${shownTrack.artist}" }
                                } else shownTrack.artist,
                                color = miniSecondaryContent,
                                fontSize = 13.sp,
                                lineHeight = LevyraTypeRhythm.lineHeight(13.sp),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.dockFoldHeight(compaction)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MiniPlayerToggleButton(
                        isPlaying = isPlaying,
                        isResolving = isResolving,
                        liveRadio = liveRadio,
                        progress = { animatedProgress.value },
                        accent = miniProgressColor,
                        animated = animated,
                        onToggle = playbackActions.toggle
                    )
                    Row(
                        modifier = Modifier.dockFoldWidth(compaction),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    if (!liveRadio) {
                        PlayerRoundIconButton(
                            icon = Icons.Rounded.SkipNext,
                            contentDescription = strings.next,
                            size = 42.dp,
                            iconSize = 24.dp,
                            tint = miniPrimaryContent,
                            background = Color.White.copy(alpha = 0.10f),
                            borderColor = Color.Transparent,
                            onClick = playbackActions.next
                        )
                    }
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 40.dp, minHeight = LevyraPlayerDesign.MinimumTouchTarget)
                            .pressable(onClick = playbackActions.close),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = strings.closePlayer,
                            tint = LevyraPlayerDesign.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    }
                }
            }
        }
    }
}
