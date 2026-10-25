package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.Nocturne78Palette

import com.luc4n3x.levyra.ui.components.LevyraBackButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.Shape
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun collectionMosaicShape(): Shape = if (LevyraIsNocturne) RoundedCornerShape(Nocturne78Design.artworkCorner) else MaterialShapes.Cookie4Sided.toShape()
private val CollectionActionHeight = 56.dp
private val CollectionActionGroupGap = 4.dp
private val CollectionActionOuterCorner get() = if (LevyraIsNocturne) Nocturne78Design.controlCorner else 28.dp
private val CollectionActionInnerCorner = 10.dp
private val CollectionActionChrome = 16.dp * 2 + 22.dp + 8.dp
private val CollectionActionLabelStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = LevyraType.contentTitle.copy(fontWeight = FontWeight.Bold)

@Composable
internal fun HomeCollectionScreen(
    title: String,
    tracks: List<Track>,
    accentStart: Color,
    accentEnd: Color,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onDismiss: () -> Unit,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onTrackActions: (Track) -> Unit,
    bottomInset: Dp
) {
    if (com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic) {
        AppleMusicCollectionScreen(
            title = title, curator = "Levyra", caption = "", description = "",
            artworkUrl = "", artworkTrack = tracks.firstOrNull(), tracks = tracks, currentId = currentId,
            loading = false, failed = false, bottomInset = bottomInset, onDismiss = onDismiss, onPlay = onPlay,
            onPlayAll = onPlayAll, onShuffle = onShuffle, onTrackActions = onTrackActions
        )
        return
    }
    if (LevyraIsSpotify) {
        SpotifyRadioPage(
            title, tracks, accentStart, currentId, isPlaying, bottomInset,
            onPlay = { list, track -> if (list == tracks) onPlay(track) else onShuffle() },
            onTrackActions = onTrackActions,
            onDismiss = onDismiss
        )
        return
    }
    BackHandler(onBack = onDismiss)
    val strings = LocalLevyraStrings.current
    val animationsEnabled = LocalAnimationsEnabled.current
    val visibility = remember { MutableTransitionState(!animationsEnabled).apply { targetState = true } }
    val mosaic = remember(tracks) {
        tracks.distinctBy { it.thumbnailUrl.ifBlank { it.id } }.take(4)
    }
    val artistLine = remember(tracks) {
        tracks.asSequence()
            .map { it.artist.trim() }
            .filter(String::isNotBlank)
            .distinct()
            .take(3)
            .joinToString(" · ")
    }
    val nocturne = LevyraIsNocturne
    val spotify = LevyraIsSpotify
    val backdrop = remember(accentStart, accentEnd, nocturne, spotify) {
        if (spotify) Brush.verticalGradient(0f to spotifyRadioPastel(accentStart), 0.5f to spotifyRadioPastel(accentStart).copy(alpha = 0.55f), 1f to Color.Transparent) else
        if (nocturne) Brush.verticalGradient(listOf(Nocturne78Palette.black, Nocturne78Palette.black)) else
        Brush.verticalGradient(
            0f to accentStart.copy(alpha = 0.55f),
            0.45f to accentEnd.copy(alpha = 0.18f),
            1f to Color.Transparent
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(homeCanvasColor(LevyraIsLight))
            .pointerInput(Unit) {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .background(backdrop)
        )
        AnimatedVisibility(
            visibleState = visibility,
            enter = fadeIn() + slideInVertically { height -> height / 14 }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = bottomInset + 16.dp)
            ) {
                item(key = "collection-top") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LevyraBackButton(onClick = onDismiss, contentDescription = strings.back)
                    }
                }
                if (spotify) item(key = "collection-hero-spotify") {
                    SpotifyCollectionHero(
                        title = title,
                        subtitle = listOf(strings.formatTrackCount(tracks.size), artistLine).filter(String::isNotBlank).joinToString(" • "),
                        tracks = mosaic,
                        accent = accentStart,
                        playLabel = strings.playAll,
                        shuffleLabel = strings.shuffle,
                        onPlayAll = onPlayAll,
                        onShuffle = onShuffle
                    )
                } else item(key = "collection-hero") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CollectionMosaic(
                            tracks = mosaic,
                            accentStart = accentStart,
                            modifier = Modifier
                                .fillMaxWidth(if (nocturne) 0.66f else 0.78f)
                                .aspectRatio(1f)
                        )
                        Text(
                            text = strings.collectionsTitle.uppercase(),
                            color = LevyraText.copy(alpha = 0.72f),
                            style = LevyraType.overline,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                        Text(
                            text = title,
                            color = LevyraText,
                            style = LevyraType.heroTitle,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .semantics { heading() }
                        )
                        Text(
                            text = listOf(strings.formatTrackCount(tracks.size), artistLine)
                                .filter(String::isNotBlank)
                                .joinToString(" · "),
                            color = LevyraMuted,
                            style = LevyraType.metadata,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        CollectionActionGroup(
                            playLabel = strings.playAll,
                            shuffleLabel = strings.shuffle,
                            accent = accentStart,
                            onPlayAll = onPlayAll,
                            onShuffle = onShuffle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 22.dp, bottom = 18.dp)
                        )
                    }
                }
                itemsIndexed(
                    items = tracks,
                    key = { index, track -> "collection-track-$index-${track.id}" }
                ) { index, track ->
                    val active = track.id == currentId
                    CollectionTrackRow(
                        rank = index + 1,
                        count = tracks.size,
                        track = track,
                        active = active,
                        isPlaying = isPlaying && active,
                        isResolving = isResolving && active,
                        accent = accentStart,
                        onPlay = { onPlay(track) },
                        onActions = { onTrackActions(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionMosaic(
    tracks: List<Track>,
    accentStart: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(collectionMosaicShape())
            .background(accentStart.copy(alpha = 0.35f))
    ) {
        if (tracks.size >= 4) {
            Column {
                tracks.chunked(2).forEach { pair ->
                    Row(modifier = Modifier.weight(1f)) {
                        pair.forEach { track ->
                            CoverImage(
                                track = track,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                                highRes = false
                            )
                        }
                    }
                }
            }
        } else {
            tracks.firstOrNull()?.let { track ->
                CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = true)
            }
        }
    }
}

@Composable
private fun CollectionActionGroup(
    playLabel: String,
    shuffleLabel: String,
    accent: Color,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = CollectionActionLabelStyle
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val stacked = remember(playLabel, shuffleLabel, maxWidth, density, labelStyle) {
            with(density) {
                collectionActionsStacked(
                    availableWidth = maxWidth.toPx(),
                    playLabelWidth = textMeasurer.measure(playLabel, labelStyle, maxLines = 1).size.width.toFloat(),
                    shuffleLabelWidth = textMeasurer.measure(shuffleLabel, labelStyle, maxLines = 1).size.width.toFloat(),
                    buttonChrome = CollectionActionChrome.toPx(),
                    gap = CollectionActionGroupGap.toPx()
                )
            }
        }
        val play: @Composable (Modifier, Shape) -> Unit = { buttonModifier, shape ->
            CollectionAction(
                label = playLabel,
                icon = Icons.Rounded.PlayArrow,
                container = accent,
                content = Color.White,
                shape = shape,
                onClick = onPlayAll,
                modifier = buttonModifier
            )
        }
        val shuffle: @Composable (Modifier, Shape) -> Unit = { buttonModifier, shape ->
            CollectionAction(
                label = shuffleLabel,
                icon = Icons.Rounded.Shuffle,
                container = LevyraText.copy(alpha = 0.10f),
                content = LevyraText,
                shape = shape,
                onClick = onShuffle,
                modifier = buttonModifier
            )
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(CollectionActionGroupGap)) {
                play(
                    Modifier.fillMaxWidth(),
                    RoundedCornerShape(
                        topStart = CollectionActionOuterCorner,
                        topEnd = CollectionActionOuterCorner,
                        bottomStart = CollectionActionInnerCorner,
                        bottomEnd = CollectionActionInnerCorner
                    )
                )
                shuffle(
                    Modifier.fillMaxWidth(),
                    RoundedCornerShape(
                        topStart = CollectionActionInnerCorner,
                        topEnd = CollectionActionInnerCorner,
                        bottomStart = CollectionActionOuterCorner,
                        bottomEnd = CollectionActionOuterCorner
                    )
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CollectionActionGroupGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                play(
                    Modifier.weight(1f),
                    RoundedCornerShape(
                        topStart = CollectionActionOuterCorner,
                        bottomStart = CollectionActionOuterCorner,
                        topEnd = CollectionActionInnerCorner,
                        bottomEnd = CollectionActionInnerCorner
                    )
                )
                shuffle(
                    Modifier.weight(1f),
                    RoundedCornerShape(
                        topStart = CollectionActionInnerCorner,
                        bottomStart = CollectionActionInnerCorner,
                        topEnd = CollectionActionOuterCorner,
                        bottomEnd = CollectionActionOuterCorner
                    )
                )
            }
        }
    }
}

internal fun collectionActionsStacked(
    availableWidth: Float,
    playLabelWidth: Float,
    shuffleLabelWidth: Float,
    buttonChrome: Float,
    gap: Float
): Boolean {
    val buttonWidth = (availableWidth - gap) / 2f
    return maxOf(playLabelWidth, shuffleLabelWidth) + buttonChrome > buttonWidth
}

@Composable
private fun CollectionAction(
    label: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(CollectionActionHeight)
            .clip(shape)
            .background(container)
            .levyraPressable(
                onClick = onClick,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            color = content,
            style = CollectionActionLabelStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun collectionRowBackground(active: Boolean, accent: Color, spotify: Boolean): Color = when {
    spotify -> Color.Transparent
    active -> accent.copy(alpha = 0.18f)
    else -> LevyraText.copy(alpha = 0.05f)
}

@Composable
private fun CollectionTrackRow(
    rank: Int,
    count: Int,
    track: Track,
    active: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    accent: Color,
    onPlay: () -> Unit,
    onActions: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val spotify = LevyraIsSpotify
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 1.dp)
            .heightIn(min = LevyraCardDesign.RowHeight)
            .clip(levyraGroupedListShape(rank - 1, count))
            .background(collectionRowBackground(active, accent, spotify))
            .levyraPressable(onClick = onPlay, pressedScale = LevyraPressScale.Row, role = Role.Button)
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LevyraCardDesign.RowTextGap)
    ) {
        if (!spotify) Text(
            text = rank.toString(),
            color = if (active) accent else LevyraText.copy(alpha = 0.72f),
            style = LevyraType.contentTitle.copy(fontWeight = FontWeight.Black, fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.width(28.dp)
        )
        Box(
            modifier = Modifier
                .size(LevyraCardDesign.RowThumb)
                .clip(LevyraHomeDesign.ThumbShape),
            contentAlignment = Alignment.Center
        ) {
            CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = false)
            if (active) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.42f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isResolving) {
                        LevyraLoadingIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        LevyraPlayingIndicator(
                            playing = isPlaying,
                            color = Color.White,
                            size = 18.dp,
                            contentDescription = strings.playing
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LevyraCardDesign.RowLineGap)
        ) {
            Text(
                text = track.title,
                color = LevyraText,
                style = LevyraType.contentTitle.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = LevyraMuted,
                style = LevyraType.metadata,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onActions) {
            Icon(Icons.Rounded.MoreVert, contentDescription = strings.songOptions, tint = LevyraMuted)
        }
    }
}
