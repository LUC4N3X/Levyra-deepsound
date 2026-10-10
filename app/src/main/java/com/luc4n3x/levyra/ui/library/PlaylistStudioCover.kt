package com.luc4n3x.levyra.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.PLAYLIST_COVER_PREVIEW_PX
import com.luc4n3x.levyra.data.PlaylistCoverArtist
import com.luc4n3x.levyra.data.PlaylistCoverPreviewCache
import com.luc4n3x.levyra.domain.PlaylistCoverMode
import com.luc4n3x.levyra.domain.PlaylistCoverStyle
import com.luc4n3x.levyra.domain.PlaylistStudioDraft
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.buildPlaylistCoverPlan
import com.luc4n3x.levyra.domain.playlistStudioArtwork
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import timber.log.Timber

private const val CoverRenderDebounceMs = 90L
private val CoverCorner = 26.dp
private const val ArtworkChoicesLimit = 40

@Composable
internal fun PlaylistStudioCoverPreview(
    draft: PlaylistStudioDraft,
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(CoverCorner)
    Box(
        modifier = modifier
            .shadow(
                elevation = 22.dp,
                shape = shape,
                clip = false,
                ambientColor = LevyraViolet.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.7f)
            )
            .clip(shape)
            .background(LevyraPanelSoft)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), shape)
    ) {
        when (draft.coverStyle) {
            PlaylistCoverStyle.Current -> PlaylistCoverArt(
                coverMode = PlaylistCoverMode.CUSTOM,
                coverUrl = draft.customCoverUrl,
                tracks = draft.tracks,
                contentDescription = draft.name,
                modifier = Modifier.fillMaxSize()
            )
            PlaylistCoverStyle.Automatic -> PlaylistCoverArt(
                coverMode = PlaylistCoverMode.AUTO,
                coverUrl = "",
                tracks = draft.tracks,
                contentDescription = draft.name,
                modifier = Modifier.fillMaxSize()
            )
            else -> RenderedCoverPreview(draft = draft, animated = animated)
        }
    }
}

@Composable
private fun RenderedCoverPreview(draft: PlaylistStudioDraft, animated: Boolean) {
    val context = LocalContext.current
    val artist = remember(context) { PlaylistCoverArtist(context) }
    val plan = remember(draft.coverStyle, draft.name, draft.tracks, draft.coverTrackId, draft.photo) {
        buildPlaylistCoverPlan(draft)
    }
    var shown by remember { mutableStateOf<RenderedCover?>(null) }
    LaunchedEffect(plan) {
        val target = plan ?: return@LaunchedEffect
        val cached = PlaylistCoverPreviewCache.get(target)
        if (cached != null) {
            shown = RenderedCover(target.style, cached.asImageBitmap())
            return@LaunchedEffect
        }
        delay(CoverRenderDebounceMs)
        try {
            val rendered = artist.render(target, PLAYLIST_COVER_PREVIEW_PX)
            PlaylistCoverPreviewCache.put(target, rendered)
            shown = RenderedCover(target.style, rendered.asImageBitmap())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Playlist cover preview failed")
        }
    }
    AnimatedContent(
        targetState = shown,
        contentKey = { it?.style },
        transitionSpec = {
            if (animated) {
                fadeIn(LevyraMotion.crossfade()) togetherWith fadeOut(LevyraMotion.fade(LevyraMotion.Durations.Quick))
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        label = "playlist-studio-cover"
    ) { cover ->
        if (cover != null) {
            Image(
                bitmap = cover.bitmap,
                contentDescription = draft.name.ifBlank { null },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LevyraPanelSoft)
            )
        }
    }
}

@Immutable
private class RenderedCover(val style: PlaylistCoverStyle, val bitmap: ImageBitmap)

@Immutable
internal data class PlaylistStudioCoverOption(
    val style: PlaylistCoverStyle,
    val label: String,
    val icon: ImageVector
)

internal fun playlistStudioCoverOptions(
    draft: PlaylistStudioDraft,
    strings: LevyraStrings
): List<PlaylistStudioCoverOption> = buildList {
    if (draft.customCoverUrl.isNotBlank()) {
        add(PlaylistStudioCoverOption(PlaylistCoverStyle.Current, strings.playlistStudioCoverCurrent, Icons.Rounded.Star))
    }
    add(PlaylistStudioCoverOption(PlaylistCoverStyle.Automatic, strings.playlistStudioCoverAutomatic, Icons.Rounded.AutoAwesome))
    add(PlaylistStudioCoverOption(PlaylistCoverStyle.Mosaic, strings.playlistStudioCoverMosaic, Icons.Rounded.GridView))
    add(PlaylistStudioCoverOption(PlaylistCoverStyle.Spotlight, strings.playlistStudioCoverSpotlight, Icons.Rounded.Album))
    add(PlaylistStudioCoverOption(PlaylistCoverStyle.Signal, strings.playlistStudioCoverSignal, Icons.Rounded.Radar))
    add(PlaylistStudioCoverOption(PlaylistCoverStyle.Artwork, strings.playlistStudioCoverArtwork, Icons.Rounded.Image))
    add(PlaylistStudioCoverOption(PlaylistCoverStyle.Photo, strings.playlistStudioCoverPhoto, Icons.Rounded.PhotoLibrary))
}

@Composable
internal fun PlaylistStudioCoverStyles(
    options: List<PlaylistStudioCoverOption>,
    selected: PlaylistCoverStyle,
    animated: Boolean,
    onSelect: (PlaylistCoverStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options, key = { it.style.name }) { option ->
            val active = option.style == selected
            val tint by animateColorAsState(
                targetValue = if (active) LevyraCyan else LevyraText,
                animationSpec = LevyraMotion.spec(animated, LevyraMotion.fade()),
                label = "studio-cover-style-tint"
            )
            Surface(
                color = if (active) LevyraCyan.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, if (active) LevyraCyan.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .width(84.dp)
                    .heightIn(min = 72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .selectable(
                        selected = active,
                        role = Role.RadioButton,
                        onClick = { onSelect(option.style) }
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(option.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    Text(
                        text = option.label,
                        color = tint,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlaylistStudioArtworkChoices(
    tracks: List<Track>,
    selectedTrackId: String?,
    label: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val choices = remember(tracks) {
        tracks.asSequence()
            .filter { playlistStudioArtwork(it).isNotBlank() }
            .distinctBy(::playlistStudioArtwork)
            .take(ArtworkChoicesLimit)
            .toList()
    }
    if (choices.isEmpty()) return
    val effectiveSelection = selectedTrackId ?: choices.first().id
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = LevyraMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            modifier = Modifier.selectableGroup(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(choices, key = { it.id }) { track ->
                val active = track.id == effectiveSelection
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            BorderStroke(if (active) 2.dp else 1.dp, if (active) LevyraCyan else Color.White.copy(alpha = 0.08f)),
                            RoundedCornerShape(14.dp)
                        )
                        .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(track.id) })
                ) {
                    LibraryArtwork(
                        playlistStudioArtwork(track),
                        track.title,
                        Modifier.fillMaxSize(),
                        RoundedCornerShape(14.dp),
                        false
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlaylistStudioEmptyTracks(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(LevyraViolet.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                tint = LevyraViolet,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = title,
            color = LevyraText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = body,
            color = LevyraMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
        Surface(
            color = LevyraPanel,
            border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(top = 6.dp)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClick = onAction)
        ) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp), contentAlignment = Alignment.Center) {
                Text(text = actionLabel, color = LevyraCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
