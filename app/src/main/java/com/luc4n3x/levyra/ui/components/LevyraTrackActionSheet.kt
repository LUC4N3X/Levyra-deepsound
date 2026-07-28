package com.luc4n3x.levyra.ui.components

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.StableRemoteArtwork
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TrackActionSheetExitMs = 210
private const val TrackActionSheetEnterMs = 260
private val TrackActionSheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
private val TrackActionSheetDismissDistance = 108.dp

private val trackActionSheetSurface: Color
    get() = if (LevyraActivePalette.isLight) Color(0xFFF6F5FB) else LevyraPanel

private val trackActionSheetBorder: Color
    get() = if (LevyraActivePalette.isLight) Color(0x1A101322) else Color.White.copy(alpha = 0.11f)

private val trackActionSheetTile: Color
    get() = if (LevyraActivePalette.isLight) Color(0x0F101322) else Color.White.copy(alpha = 0.07f)

private val trackActionSheetTileBorder: Color
    get() = if (LevyraActivePalette.isLight) Color(0x14101322) else Color.White.copy(alpha = 0.09f)

@Composable
internal fun LevyraTrackActionSheet(
    track: Track,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    canRemoveFromHistory: Boolean,
    animationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onRemoveFromHistory: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val dismissDistancePx = remember(density) { with(density) { TrackActionSheetDismissDistance.toPx() } }
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(closing) {
        if (closing) {
            delay(TrackActionSheetExitMs.toLong())
            onDismiss()
        }
    }

    val dismiss: () -> Unit = {
        if (!closing) {
            visible = false
            closing = true
        }
    }
    val perform: (() -> Unit) -> Unit = { action ->
        if (!closing) {
            action()
            dismiss()
        }
    }

    BackHandler(enabled = !closing, onBack = dismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(60f)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(TrackActionSheetEnterMs)),
            exit = fadeOut(animationSpec = tween(TrackActionSheetExitMs))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = dismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                animationSpec = tween(TrackActionSheetEnterMs),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = tween(TrackActionSheetEnterMs)),
            exit = slideOutVertically(
                animationSpec = tween(TrackActionSheetExitMs),
                targetOffsetY = { it }
            ) + fadeOut(animationSpec = tween(TrackActionSheetExitMs))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                color = trackActionSheetSurface,
                border = BorderStroke(1.dp, trackActionSheetBorder),
                shape = TrackActionSheetShape,
                tonalElevation = 0.dp,
                shadowElevation = 26.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (dragOffset.value > dismissDistancePx) {
                                            dismiss()
                                        } else {
                                            scope.launch {
                                                dragOffset.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onDragCancel = { scope.launch { dragOffset.animateTo(0f) } }
                                ) { change, delta ->
                                    change.consume()
                                    scope.launch {
                                        dragOffset.snapTo((dragOffset.value + delta).coerceAtLeast(0f))
                                    }
                                }
                            }
                            .padding(top = 11.dp, bottom = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(LevyraMuted.copy(alpha = 0.34f))
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        TrackActionHeader(
                            track = track,
                            isFavorite = isFavorite,
                            favoriteLabel = if (isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                            onToggleFavorite = { onToggleFavorite() }
                        )

                        TrackActionDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TrackActionTile(
                                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                label = strings.playNext,
                                animationsEnabled = animationsEnabled,
                                modifier = Modifier.weight(1f),
                                onClick = { perform(onPlayNext) }
                            )
                            TrackActionTile(
                                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                label = strings.addToQueue,
                                animationsEnabled = animationsEnabled,
                                modifier = Modifier.weight(1f),
                                onClick = { perform(onAddToQueue) }
                            )
                            TrackActionTile(
                                icon = Icons.Rounded.Share,
                                label = strings.share,
                                animationsEnabled = animationsEnabled,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    perform {
                                        val shareText = buildString {
                                            append(track.title)
                                            if (track.artist.isNotBlank()) append(" - ").append(track.artist)
                                            val link = track.videoUrl.ifBlank { track.streamUrl }
                                            if (link.isNotBlank()) append("\n").append(link)
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, strings.shareSong))
                                    }
                                }
                            )
                        }

                        TrackActionDivider()

                        Column(modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)) {
                            TrackActionRow(
                                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                label = strings.addToPlaylist,
                                onClick = { perform(onAddToPlaylist) }
                            )
                            when {
                                isDownloading -> TrackActionRow(
                                    icon = Icons.Rounded.Download,
                                    label = strings.downloadInProgress,
                                    enabled = false,
                                    trailing = {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(17.dp),
                                            strokeWidth = 2.dp,
                                            color = LevyraCyan
                                        )
                                    },
                                    onClick = {}
                                )
                                isDownloaded -> TrackActionRow(
                                    icon = Icons.Rounded.Delete,
                                    label = strings.deleteDownload,
                                    tint = LevyraPink,
                                    trailing = {
                                        Icon(
                                            imageVector = Icons.Rounded.DownloadDone,
                                            contentDescription = strings.downloaded,
                                            tint = LevyraCyan.copy(alpha = 0.85f),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    },
                                    onClick = { perform(onDeleteDownload) }
                                )
                                else -> TrackActionRow(
                                    icon = Icons.Rounded.Download,
                                    label = strings.download,
                                    onClick = { perform(onDownload) }
                                )
                            }
                            if (track.album.isNotBlank()) {
                                TrackActionRow(
                                    icon = Icons.Rounded.Album,
                                    label = strings.openAlbum,
                                    onClick = { perform(onOpenAlbum) }
                                )
                            }
                            if (track.artist.isNotBlank()) {
                                TrackActionRow(
                                    icon = Icons.Rounded.Person,
                                    label = strings.openArtist,
                                    onClick = { perform(onOpenArtist) }
                                )
                            }
                            if (canRemoveFromHistory) {
                                TrackActionRow(
                                    icon = Icons.Rounded.History,
                                    label = strings.removeFromRecentSearches,
                                    tint = LevyraPink,
                                    onClick = { perform(onRemoveFromHistory) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackActionHeader(
    track: Track,
    isFavorite: Boolean,
    favoriteLabel: String,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val artworkShape = RoundedCornerShape(14.dp)
        val artworkUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(artworkShape)
                .background(trackActionSheetTile),
            contentAlignment = Alignment.Center
        ) {
            if (artworkUrl.isNotBlank()) {
                StableRemoteArtwork(
                    url = artworkUrl,
                    contentDescription = track.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = LevyraMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = track.title,
                color = LevyraText,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (track.artist.isNotBlank()) {
                Text(
                    text = track.artist,
                    color = LevyraMuted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleFavorite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = favoriteLabel,
                tint = if (isFavorite) LevyraPink else LevyraMuted,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
private fun TrackActionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        LevyraCyan.copy(alpha = 0.26f),
                        LevyraViolet.copy(alpha = 0.22f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun TrackActionTile(
    icon: ImageVector,
    label: String,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .trackActionPressable(animationsEnabled = animationsEnabled, onClick = onClick),
            color = trackActionSheetTile,
            border = BorderStroke(1.dp, trackActionSheetTileBorder),
            shape = RoundedCornerShape(18.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LevyraText,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
        Text(
            text = label,
            color = LevyraText.copy(alpha = 0.88f),
            fontSize = 11.5.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrackActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = LevyraText,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint.copy(alpha = 0.92f) else LevyraMuted.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (enabled) tint else LevyraMuted.copy(alpha = 0.7f),
            fontSize = 14.5.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
private fun Modifier.trackActionPressable(
    animationsEnabled: Boolean,
    onClick: () -> Unit
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && animationsEnabled) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "track-action-tile"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interaction,
            indication = if (animationsEnabled) null else LocalIndication.current,
            onClick = onClick
        )
}
