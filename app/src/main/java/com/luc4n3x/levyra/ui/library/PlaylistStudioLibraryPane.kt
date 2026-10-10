package com.luc4n3x.levyra.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.StudioSearchIndex
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.playlistStudioArtwork
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.playlistProCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

@Composable
internal fun PlaylistStudioLibraryPane(
    candidates: StudioSearchIndex?,
    selectedIds: Set<String>,
    downloadedTrackIds: Set<String>,
    animated: Boolean,
    onToggle: (Track) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(candidates, query) { candidates?.filter(query) }
    val listState = rememberLazyListState()
    BackHandler(onBack = onClose)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevyraInk)
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = LevyraText)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.playlistStudioAddSongs,
                    color = LevyraText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = strings.formatTrackCount(selectedIds.size),
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(strings.done, color = LevyraCyan, fontWeight = FontWeight.Bold)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            placeholder = { Text(strings.playlistStudioSearchLibrary) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, contentDescription = strings.clear) } }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )
        AnimatedContent(
            targetState = results == null,
            transitionSpec = { LevyraMotion.contentSwap(animated) },
            label = "playlist-studio-library",
            modifier = Modifier.weight(1f)
        ) { loading ->
            when {
                loading -> StudioPaneMessage(text = strings.playlistStudioLoading, busy = true)
                results.isNullOrEmpty() && candidates?.tracks.isNullOrEmpty() ->
                    StudioPaneMessage(text = strings.playlistStudioLibraryEmpty, busy = false)
                results.isNullOrEmpty() -> StudioPaneMessage(text = strings.playlistProCopy().noSearchResults, busy = false)
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(results.orEmpty(), key = { it.id }, contentType = { "studio-candidate" }) { track ->
                        val added = track.id in selectedIds
                        StudioCandidateRow(
                            track = track,
                            added = added,
                            offline = track.id in downloadedTrackIds,
                            addedLabel = strings.playlistStudioInPlaylist,
                            animated = animated,
                            onToggle = {
                                haptics.perform(LevyraHapticAction.Confirm)
                                onToggle(track)
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun StudioPaneMessage(text: String, busy: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (busy) {
            CircularProgressIndicator(color = LevyraCyan, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(14.dp))
        }
        Text(text = text, color = LevyraMuted, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun StudioCandidateRow(
    track: Track,
    added: Boolean,
    offline: Boolean,
    addedLabel: String,
    animated: Boolean,
    onToggle: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (added) LevyraCyan.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = LevyraMotion.spec(animated, LevyraMotion.fade()),
        label = "studio-candidate-background"
    )
    val iconTint by animateColorAsState(
        targetValue = if (added) LevyraCyan else LevyraMuted,
        animationSpec = LevyraMotion.spec(animated, LevyraMotion.fade()),
        label = "studio-candidate-icon"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(background, RoundedCornerShape(16.dp))
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics { if (added) stateDescription = addedLabel }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibraryArtwork(
            playlistStudioArtwork(track),
            track.title,
            Modifier.size(48.dp),
            RoundedCornerShape(12.dp),
            false
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = LevyraText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (offline) {
                    Icon(
                        Icons.Rounded.OfflinePin,
                        contentDescription = null,
                        tint = LevyraCyan.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = track.artist,
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (added) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(26.dp)
                    .background(if (added) LevyraCyan.copy(alpha = 0.12f) else Color.Transparent, CircleShape)
            )
        }
    }
}
