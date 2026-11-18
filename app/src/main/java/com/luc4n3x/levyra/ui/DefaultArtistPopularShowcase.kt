package com.luc4n3x.levyra.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.LevyraRowText
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.selection.TrackSelectionState
import com.luc4n3x.levyra.ui.selection.trackSelectionKey
import com.luc4n3x.levyra.ui.player.*
import com.luc4n3x.levyra.ui.theme.*

private const val PopularPageRows = 4
private val PopularPageMaxWidth = 420.dp
private val PopularPagePeek = 56.dp

@Composable
internal fun DefaultArtistPopularShowcase(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    selection: TrackSelectionState,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    if (tracks.isEmpty()) return
    val pages = remember(tracks) { tracks.chunked(PopularPageRows) }
    val activate: (Track) -> Unit = { track ->
        val key = trackSelectionKey(track)
        if (selection.isActive) selection.toggle(key) else onPlay(track)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ArtistPopularHeader(strings, onPlayAll = onPlayAll)
        }
        Spacer(modifier = Modifier.height(8.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val pageWidth = if (pages.size == 1) maxWidth - 24.dp else min(maxWidth - PopularPagePeek, PopularPageMaxWidth)
            val listState = rememberLazyListState()
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start)
            ) {
                itemsIndexed(
                    items = pages,
                    key = { index, page -> "popular-page-$index-${trackSelectionKey(page.first())}" },
                    contentType = { _, _ -> "artist-popular-page" }
                ) { pageIndex, page ->
                    Column(modifier = Modifier.width(pageWidth)) {
                        page.forEachIndexed { rowIndex, track ->
                            val isCurrent = track.id == currentId
                            val key = trackSelectionKey(track)
                            PopularPageRow(
                                rank = 1 + pageIndex * PopularPageRows + rowIndex,
                                track = track,
                                isCurrent = isCurrent,
                                isPlaying = isPlaying && isCurrent,
                                isResolving = isResolving && isCurrent,
                                selected = selection.isSelected(key),
                                selectionActive = selection.isActive,
                                onLongClick = { selection.start(key) },
                                onPlay = { activate(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PopularPageRow(
    rank: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    selected: Boolean,
    selectionActive: Boolean,
    onLongClick: () -> Unit,
    onPlay: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val subtitle = remember(track.youtubeViewCount, track.durationMs, strings.code) { artistPopularSubtitle(track, strings) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LevyraCardDesign.ArtworkShape)
            .background(
                when {
                    selected -> LevyraCyan.copy(alpha = 0.16f)
                    isCurrent -> LevyraText.copy(alpha = 0.06f)
                    else -> Color.Transparent
                }
            )
            .semantics { this.selected = selected }
            .heightIn(min = LevyraCardDesign.RowHeight)
            .combinedClickable(onClick = onPlay, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(LevyraCardDesign.RowThumb)
                .clip(LevyraCardDesign.ThumbShape)
                .background(LevyraPanelSoft)
        ) {
            CoverImage(track = track, modifier = Modifier.fillMaxSize())
        }
        Text(
            text = rank.toString(),
            color = if (isCurrent) LevyraCyan else LevyraMuted,
            style = LevyraType.metadata.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.width(36.dp)
        )
        LevyraRowText(
            title = track.title,
            subtitle = subtitle,
            titleColor = if (isCurrent) LevyraCyan else LevyraText,
            modifier = Modifier.weight(1f),
            explicit = track.explicit
        )
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            when {
                selectionActive -> Icon(
                    imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) LevyraCyan else LevyraMuted,
                    modifier = Modifier.size(24.dp)
                )
                isResolving -> LevyraLoadingIndicator(modifier = Modifier.size(18.dp), color = LevyraCyan)
                isCurrent -> LevyraPlayingIndicator(playing = isPlaying, color = LevyraCyan, size = 18.dp, contentDescription = strings.playing)
            }
        }
    }
}
