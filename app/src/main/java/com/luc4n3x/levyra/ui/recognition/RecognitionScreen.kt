package com.luc4n3x.levyra.ui.recognition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.recognition.RecognitionErrorKind
import com.luc4n3x.levyra.feature.recognition.RecognitionHistoryEntry
import com.luc4n3x.levyra.feature.recognition.RecognitionResult
import com.luc4n3x.levyra.feature.recognition.RecognitionState
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText

@Composable
internal fun LevyraRecognitionOverlay(
    state: RecognitionState,
    history: List<RecognitionHistoryEntry>,
    match: Track?,
    matching: Boolean,
    deviceCaptureSupported: Boolean,
    onListenMicrophone: () -> Unit,
    onListenDevice: () -> Unit,
    onCancel: () -> Unit,
    onPlayMatch: () -> Unit,
    onSearchResult: (RecognitionResult) -> Unit,
    onTrackActions: (Track) -> Unit,
    onOpenHistoryEntry: (RecognitionHistoryEntry) -> Unit,
    onDeleteHistoryEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val busy = state is RecognitionState.Listening || state is RecognitionState.Identifying

    Box(modifier = Modifier.fillMaxSize().background(LevyraInk)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(contentType = "recognition-header") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.back,
                            tint = LevyraText
                        )
                    }
                    Column(modifier = Modifier.padding(start = 6.dp)) {
                        Text(
                            strings.recognitionTitle,
                            color = LevyraText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            strings.recognitionSubtitle,
                            color = LevyraMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item(contentType = "recognition-state") {
                RecognitionStateCard(
                    state = state,
                    match = match,
                    matching = matching,
                    strings = strings,
                    onPlayMatch = onPlayMatch,
                    onSearchResult = onSearchResult,
                    onTrackActions = onTrackActions
                )
            }

            item(contentType = "recognition-actions") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecognitionActionRow(
                        label = if (busy) strings.recognitionCancelAction else strings.recognitionListenMicrophone,
                        icon = if (busy) Icons.Rounded.Tune else Icons.Rounded.Mic,
                        enabled = true,
                        onClick = { if (busy) onCancel() else onListenMicrophone() }
                    )
                    if (deviceCaptureSupported) {
                        RecognitionActionRow(
                            label = strings.recognitionListenDevice,
                            icon = Icons.Rounded.PhoneAndroid,
                            enabled = !busy,
                            onClick = onListenDevice
                        )
                    }
                }
            }

            item(contentType = "recognition-history-header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        strings.historyLabel,
                        color = LevyraText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (history.isNotEmpty()) {
                        TextButton(onClick = onClearHistory) {
                            Text(strings.recognitionClearHistory, color = LevyraCyan, fontSize = 13.sp)
                        }
                    }
                }
            }

            if (history.isEmpty()) {
                item(contentType = "recognition-history-empty") {
                    Text(
                        strings.recognitionHistoryEmpty,
                        color = LevyraMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                items(history, key = { it.id }, contentType = { "recognition-history-entry" }) { entry ->
                    RecognitionHistoryRow(
                        entry = entry,
                        strings = strings,
                        onOpen = { onOpenHistoryEntry(entry) },
                        onDelete = { onDeleteHistoryEntry(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecognitionStateCard(
    state: RecognitionState,
    match: Track?,
    matching: Boolean,
    strings: LevyraStrings,
    onPlayMatch: () -> Unit,
    onSearchResult: (RecognitionResult) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(LevyraPanel)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            RecognitionState.Idle -> Text(
                strings.recognitionTapToListen,
                color = LevyraMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            RecognitionState.Listening -> RecognitionProgress(strings.recognitionListening)
            RecognitionState.Identifying -> RecognitionProgress(strings.recognitionProcessing)
            RecognitionState.NoMatch -> Text(
                strings.recognitionNoMatch,
                color = LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            is RecognitionState.Error -> Text(
                recognitionErrorText(state.kind, strings),
                color = LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            is RecognitionState.Result -> RecognitionResultBody(
                result = state.result,
                match = match,
                matching = matching,
                strings = strings,
                onPlayMatch = onPlayMatch,
                onSearchResult = onSearchResult,
                onTrackActions = onTrackActions
            )
        }
    }
}

@Composable
private fun RecognitionResultBody(
    result: RecognitionResult,
    match: Track?,
    matching: Boolean,
    strings: LevyraStrings,
    onPlayMatch: () -> Unit,
    onSearchResult: (RecognitionResult) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    RecognitionArtwork(url = result.artworkUrl, description = result.title)
    Text(
        result.title,
        color = LevyraText,
        fontSize = 22.sp,
        fontWeight = FontWeight.Black,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    Text(
        result.artist,
        color = LevyraMuted,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    val details = listOf(result.album, result.year, result.label).filter { it.isNotBlank() }
    if (details.isNotEmpty()) {
        Text(
            details.joinToString(" · "),
            color = LevyraMuted,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    Text(
        when {
            matching -> strings.searchingYouTubeMusic
            match != null -> strings.recognitionCatalogMatch
            else -> strings.recognitionCatalogMissing
        },
        color = if (match != null) LevyraCyan else LevyraMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (match != null) {
            RecognitionActionRow(
                label = strings.play,
                icon = Icons.Rounded.PlayArrow,
                enabled = true,
                onClick = onPlayMatch
            )
            RecognitionActionRow(
                label = strings.actions,
                icon = Icons.Rounded.Tune,
                enabled = true,
                onClick = { onTrackActions(match) }
            )
        }
        RecognitionActionRow(
            label = strings.search,
            icon = Icons.Rounded.Search,
            enabled = true,
            onClick = { onSearchResult(result) }
        )
    }
}

@Composable
private fun RecognitionArtwork(url: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(LevyraInk),
        contentAlignment = Alignment.Center
    ) {
        if (url.isBlank()) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = LevyraMuted,
                modifier = Modifier.size(56.dp)
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun RecognitionProgress(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = LevyraCyan, strokeWidth = 2.dp)
        Text(label, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecognitionActionRow(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tint = if (enabled) LevyraText else LevyraMuted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LevyraPanel),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, contentDescription = label, tint = tint)
        }
        TextButton(onClick = onClick, enabled = enabled) {
            Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecognitionHistoryRow(
    entry: RecognitionHistoryEntry,
    strings: LevyraStrings,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LevyraPanel)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LevyraInk),
            contentAlignment = Alignment.Center
        ) {
            if (entry.result.artworkUrl.isBlank()) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = LevyraMuted)
            } else {
                AsyncImage(
                    model = entry.result.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.result.title,
                color = LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                entry.result.artist,
                color = LevyraMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onOpen) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = strings.open, tint = LevyraText)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = strings.remove, tint = LevyraMuted)
        }
    }
}

private fun recognitionErrorText(kind: RecognitionErrorKind, strings: LevyraStrings): String = when (kind) {
    RecognitionErrorKind.PermissionDenied -> strings.recognitionPermissionRequired
    RecognitionErrorKind.Unavailable -> strings.recognitionUnavailable
    RecognitionErrorKind.Cancelled -> strings.recognitionTapToListen
    RecognitionErrorKind.Timeout,
    RecognitionErrorKind.Network,
    RecognitionErrorKind.Fingerprint -> strings.recognitionFailed
}
