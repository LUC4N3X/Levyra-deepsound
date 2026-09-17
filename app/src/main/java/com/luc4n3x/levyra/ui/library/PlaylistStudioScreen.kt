package com.luc4n3x.levyra.ui.library

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.domain.PlaylistCoverStyle
import com.luc4n3x.levyra.domain.PlaylistStudioPhoto
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.playlistStudioArtwork
import com.luc4n3x.levyra.domain.playlistStudioStats
import com.luc4n3x.levyra.ui.components.rememberLastNonNull
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.viewmodel.PlaylistStudioController
import com.luc4n3x.levyra.viewmodel.PlaylistStudioSaveState
import com.luc4n3x.levyra.viewmodel.PlaylistStudioSession
import com.luc4n3x.levyra.viewmodel.PlaylistStudioUndoKind
import kotlinx.coroutines.delay

private const val UndoVisibleMs = 5_000L
private val StudioGutter = 16.dp
private val StudioCoverMax = 300.dp
private val StudioMaxContentWidth = 840.dp
private const val StudioWideBreakpointDp = 640

@Composable
internal fun PlaylistStudioScreen(
    session: PlaylistStudioSession,
    controller: PlaylistStudioController,
    downloadedTrackIds: Set<String>,
    animated: Boolean,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    val draft = session.draft
    var libraryOpen by rememberSaveable(session.generation) { mutableStateOf(false) }
    var discardPrompt by rememberSaveable(session.generation) { mutableStateOf(false) }
    var photoSource by remember(session.generation) { mutableStateOf<android.net.Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoSource = uri
    }
    val listState = rememberLazyListState()
    var draggedTrackId by remember(session.generation) { mutableStateOf<String?>(null) }
    var dragOffsetY by remember(session.generation) { mutableFloatStateOf(0f) }
    val stats = remember(draft.tracks, downloadedTrackIds) { playlistStudioStats(draft.tracks, downloadedTrackIds) }
    val coverOptions = remember(draft.customCoverUrl, strings) { playlistStudioCoverOptions(draft, strings) }
    val untouchedNew = draft.isNew && draft.name.isBlank() && draft.tracks.isEmpty()

    val requestClose: () -> Unit = {
        if (session.saving || untouchedNew || session.saveState == PlaylistStudioSaveState.Clean ||
            session.saveState == PlaylistStudioSaveState.Saved
        ) {
            onClose()
        } else {
            discardPrompt = true
        }
    }
    BackHandler(enabled = !libraryOpen, onBack = requestClose)

    PlaylistDragFrameLoop(
        activeEntryKey = draggedTrackId,
        listState = listState,
        snapshotProvider = {
            draggedTrackId?.let { PlaylistDragSnapshot(it, controller.session.value?.draft?.tracks.orEmpty(), dragOffsetY) }
        },
        actions = PlaylistDragFrameActions(
            onMove = { fromIndex, toIndex, offsetAdjustment ->
                controller.move(fromIndex, toIndex)
                dragOffsetY += offsetAdjustment
                haptics.perform(LevyraHapticAction.Reorder)
            },
            onScrollConsumed = { consumed -> dragOffsetY += consumed }
        )
    )

    val lastUndo = session.lastUndo
    val displayedUndo = rememberLastNonNull(lastUndo, session.generation)
    LaunchedEffect(lastUndo) {
        if (lastUndo != null) {
            delay(UndoVisibleMs)
            controller.dismissUndo()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraInk)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wide = maxWidth.value >= StudioWideBreakpointDp
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = StudioMaxContentWidth)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
                contentPadding = PaddingValues(bottom = 132.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item(key = "studio-top", contentType = "studio-top") {
                    StudioTopBar(
                        session = session,
                        strings = strings,
                        animated = animated,
                        onClose = requestClose,
                        onSave = {
                            haptics.perform(LevyraHapticAction.Confirm)
                            controller.save()
                        }
                    )
                }
                item(key = "studio-hero", contentType = "studio-hero") {
                    StudioHero(
                        session = session,
                        strings = strings,
                        animated = animated,
                        wide = wide,
                        statsLine = studioStatsLine(strings, stats.trackCount, stats.durationMs, stats.artistCount, stats.offlineCount),
                        onRename = controller::rename,
                        onRetry = controller::retry
                    )
                }
                item(key = "studio-cover-label", contentType = "studio-label") {
                    StudioSectionLabel(strings.playlistStudioCover)
                }
                item(key = "studio-cover-styles", contentType = "studio-cover-styles") {
                    PlaylistStudioCoverStyles(
                        options = coverOptions,
                        selected = draft.coverStyle,
                        animated = animated,
                        onSelect = { style ->
                            if (style == PlaylistCoverStyle.Photo) {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else {
                                controller.setCoverStyle(style)
                            }
                        }
                    )
                }
                if (draft.coverStyle == PlaylistCoverStyle.Artwork) {
                    item(key = "studio-cover-artworks", contentType = "studio-cover-artworks") {
                        PlaylistStudioArtworkChoices(
                            tracks = draft.tracks,
                            selectedTrackId = draft.coverTrackId,
                            label = strings.playlistStudioChooseArtwork,
                            onSelect = controller::setCoverTrack,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                item(key = "studio-tracks-header", contentType = "studio-tracks-header") {
                    StudioTracksHeader(
                        title = strings.songsPlain,
                        count = draft.tracks.size,
                        hint = if (draft.tracks.size > 1) strings.dragToReorder else null,
                        addLabel = strings.playlistStudioAddSongs,
                        onAdd = { libraryOpen = true }
                    )
                }
                if (draft.tracks.isEmpty()) {
                    item(key = "studio-empty", contentType = "studio-empty") {
                        PlaylistStudioEmptyTracks(
                            title = strings.playlistStudioEmptyTitle,
                            body = strings.playlistStudioEmptyBody,
                            actionLabel = strings.playlistStudioAddSongs,
                            onAction = { libraryOpen = true }
                        )
                    }
                } else {
                    itemsIndexed(
                        items = draft.tracks,
                        key = { _, track -> "reorder-${track.id}" },
                        contentType = { _, _ -> "studio-track" }
                    ) { index, track ->
                        val dragging = draggedTrackId == track.id
                        StudioTrackRow(
                            track = track,
                            index = index,
                            count = draft.tracks.size,
                            offline = track.id in downloadedTrackIds,
                            dragging = dragging,
                            dragOffsetY = if (dragging) dragOffsetY else 0f,
                            strings = strings,
                            actions = PlaylistReorderRowActions(
                                onMoveUp = {
                                    controller.beginMove(track.id)
                                    controller.move(index, index - 1)
                                },
                                onMoveDown = {
                                    controller.beginMove(track.id)
                                    controller.move(index, index + 1)
                                },
                                onDragStart = {
                                    controller.beginMove(track.id)
                                    draggedTrackId = track.id
                                    dragOffsetY = 0f
                                },
                                onDrag = { delta -> if (draggedTrackId == track.id) dragOffsetY += delta },
                                onDragEnd = {
                                    draggedTrackId = null
                                    dragOffsetY = 0f
                                }
                            ),
                            onRemove = {
                                haptics.perform(LevyraHapticAction.Confirm)
                                controller.remove(track.id)
                            },
                            animated = animated
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = lastUndo != null && !libraryOpen,
            enter = LevyraMotion.sheetEnter(animated),
            exit = LevyraMotion.sheetExit(animated),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            val undo = displayedUndo ?: return@AnimatedVisibility
            StudioUndoBar(
                message = when (undo.kind) {
                    PlaylistStudioUndoKind.Removed -> strings.playlistStudioRemoved(undo.trackTitle)
                    PlaylistStudioUndoKind.Moved -> strings.playlistStudioMoved(undo.trackTitle)
                },
                actionLabel = strings.playlistStudioUndo,
                onUndo = controller::undo
            )
        }

        AnimatedVisibility(
            visible = libraryOpen,
            enter = LevyraMotion.sheetEnter(animated),
            exit = LevyraMotion.sheetExit(animated),
            modifier = Modifier.zIndex(4f)
        ) {
            PlaylistStudioLibraryPane(
                candidates = session.candidates,
                selectedIds = draft.trackIds,
                downloadedTrackIds = downloadedTrackIds,
                animated = animated,
                onToggle = controller::toggle,
                onClose = { libraryOpen = false }
            )
        }
    }

    if (discardPrompt) {
        AlertDialog(
            onDismissRequest = { discardPrompt = false },
            title = { Text(strings.playlistStudioDiscardTitle) },
            text = { Text(strings.playlistStudioDiscardBody) },
            confirmButton = {
                TextButton(onClick = {
                    discardPrompt = false
                    onClose()
                }) { Text(strings.playlistStudioDiscard, color = LevyraPink) }
            },
            dismissButton = {
                TextButton(onClick = { discardPrompt = false }) { Text(strings.playlistStudioKeepEditing) }
            }
        )
    }

    photoSource?.let { source ->
        PlaylistCoverCropDialog(
            source = source,
            onDismiss = { photoSource = null },
            onConfirm = { crop ->
                controller.setPhoto(
                    PlaylistStudioPhoto(
                        uri = source.toString(),
                        viewportSizePx = crop.viewportSizePx,
                        zoom = crop.zoom,
                        offsetX = crop.offsetX,
                        offsetY = crop.offsetY
                    )
                )
                photoSource = null
            }
        )
    }
}

internal fun studioStatsLine(
    strings: LevyraStrings,
    trackCount: Int,
    durationMs: Long,
    artistCount: Int,
    offlineCount: Int
): String = buildList {
    add(strings.formatTrackCount(trackCount))
    if (durationMs > 0L) add(strings.formatLibraryDuration(durationMs))
    if (artistCount > 0) add("$artistCount ${strings.statArtists}")
    if (offlineCount > 0) add("$offlineCount ${strings.offline}")
}.joinToString("  ·  ")

@Composable
private fun StudioTopBar(
    session: PlaylistStudioSession,
    strings: LevyraStrings,
    animated: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = StudioGutter, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = strings.close, tint = LevyraText)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.playlistStudio,
                color = LevyraText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = if (session.draft.isNew) strings.playlistStudioNew else strings.playlistStudioEdit,
                color = LevyraMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val enabled = session.canSave
        val container by animateColorAsState(
            targetValue = if (enabled) LevyraCyan else Color.White.copy(alpha = 0.08f),
            animationSpec = LevyraMotion.spec(animated, LevyraMotion.fade()),
            label = "studio-save-container"
        )
        Surface(
            color = container,
            shape = CircleShape,
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onSave)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnimatedContent(
                    targetState = session.saving,
                    transitionSpec = { LevyraMotion.contentSwap(animated) },
                    label = "studio-save-glyph"
                ) { saving ->
                    if (saving) {
                        CircularProgressIndicator(
                            color = LevyraBlack,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (enabled) LevyraBlack else LevyraMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = strings.save,
                    color = if (enabled) LevyraBlack else LevyraMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StudioHero(
    session: PlaylistStudioSession,
    strings: LevyraStrings,
    animated: Boolean,
    wide: Boolean,
    statsLine: String,
    onRename: (String) -> Unit,
    onRetry: () -> Unit
) {
    val draft = session.draft
    val cover: @Composable (Modifier) -> Unit = { modifier ->
        PlaylistStudioCoverPreview(draft = draft, animated = animated, modifier = modifier)
    }
    val details: @Composable (Modifier, TextAlign) -> Unit = { modifier, align ->
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StudioNameField(
                value = draft.name,
                placeholder = strings.playlistStudioNameHint,
                align = align,
                onValueChange = onRename
            )
            Text(
                text = statsLine,
                color = LevyraMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )
            StudioSaveStatus(
                session = session,
                strings = strings,
                align = align,
                onRetry = onRetry
            )
        }
    }
    if (wide) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = StudioGutter, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            cover(Modifier.width(240.dp).aspectRatio(1f))
            details(Modifier.weight(1f), TextAlign.Start)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = StudioGutter, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            cover(
                Modifier
                    .fillMaxWidth(0.78f)
                    .widthIn(max = StudioCoverMax)
                    .aspectRatio(1f)
            )
            details(Modifier.fillMaxWidth(), TextAlign.Center)
        }
    }
}

@Composable
private fun StudioNameField(
    value: String,
    placeholder: String,
    align: TextAlign,
    onValueChange: (String) -> Unit
) {
    val style = TextStyle(
        color = LevyraText,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.8).sp,
        textAlign = align
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        cursorBrush = SolidColor(LevyraCyan),
        maxLines = 3,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        decorationBox = { inner ->
            Box(contentAlignment = if (align == TextAlign.Center) Alignment.Center else Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = style.copy(color = LevyraMuted.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun StudioSaveStatus(
    session: PlaylistStudioSession,
    strings: LevyraStrings,
    align: TextAlign,
    onRetry: () -> Unit
) {
    val state = session.saveState
    val missingName = session.draft.name.isBlank()
    val (label, dot) = when {
        state == PlaylistStudioSaveState.Failed -> strings.playlistStudioStateFailed to LevyraPink
        state == PlaylistStudioSaveState.Saving -> strings.playlistStudioStateSaving to LevyraCyan
        state == PlaylistStudioSaveState.Dirty && missingName -> strings.playlistStudioNameRequired to LevyraMuted
        state == PlaylistStudioSaveState.Dirty -> strings.playlistStudioStateUnsaved to LevyraViolet
        state == PlaylistStudioSaveState.Saved -> strings.playlistStudioStateSaved to LevyraCyan
        else -> strings.playlistStudioStateSaved to LevyraMuted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = if (align == TextAlign.Center) {
            Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        } else {
            Arrangement.spacedBy(8.dp)
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(dot, CircleShape)
        )
        Text(
            text = label,
            color = if (state == PlaylistStudioSaveState.Failed) LevyraText else LevyraMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        if (state == PlaylistStudioSaveState.Failed) {
            TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 40.dp)) {
                Text(strings.playlistStudioRetry, color = LevyraCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StudioSectionLabel(text: String) {
    Text(
        text = text,
        color = LevyraText,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(start = StudioGutter, end = StudioGutter, top = 12.dp, bottom = 4.dp)
            .semantics { heading() }
    )
}

@Composable
private fun StudioTracksHeader(
    title: String,
    count: Int,
    hint: String?,
    addLabel: String,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = StudioGutter, end = StudioGutter, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )
                if (count > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = count.toString(), color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (hint != null) {
                Text(text = hint, color = LevyraMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Surface(
            color = LevyraPanel,
            border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.35f)),
            shape = CircleShape,
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onAdd)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
                Text(text = addLabel, color = LevyraCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LazyItemScope.StudioTrackRow(
    track: Track,
    index: Int,
    count: Int,
    offline: Boolean,
    dragging: Boolean,
    dragOffsetY: Float,
    strings: LevyraStrings,
    actions: PlaylistReorderRowActions,
    onRemove: () -> Unit,
    animated: Boolean
) {
    val haptics = LocalLevyraHaptics.current
    val rowState = PlaylistReorderRowState(index = index, count = count, isDragging = dragging, dragOffsetY = dragOffsetY)
    val moveUpLabel = "${strings.dragToReorder} ↑"
    val moveDownLabel = "${strings.dragToReorder} ↓"
    val container by animateColorAsState(
        targetValue = if (dragging) LevyraPanel else Color.Transparent,
        animationSpec = LevyraMotion.spec(animated, LevyraMotion.fade(LevyraMotion.Durations.Quick)),
        label = "studio-row-container"
    )
    Surface(
        color = container,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = if (dragging) 12.dp else 0.dp,
        modifier = Modifier
            .then(if (dragging || !animated) Modifier else Modifier.animateItem())
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .playlistReorderDrag(track.id, rowState, actions) { haptics.perform(LevyraHapticAction.Reorder) }
            .semantics {
                customActions = buildList {
                    if (index > 0) add(CustomAccessibilityAction(moveUpLabel) { actions.onMoveUp(); true })
                    if (index < count - 1) add(CustomAccessibilityAction(moveDownLabel) { actions.onMoveDown(); true })
                }
            }
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}",
                color = LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(fontFeatureSettings = "tnum"),
                modifier = Modifier.width(30.dp)
            )
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
                            contentDescription = strings.offline,
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
            Icon(
                Icons.Rounded.DragHandle,
                contentDescription = null,
                tint = if (dragging) LevyraCyan else LevyraMuted,
                modifier = Modifier.size(22.dp)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Rounded.RemoveCircleOutline,
                    contentDescription = "${strings.remove}: ${track.title}",
                    tint = LevyraMuted
                )
            }
        }
    }
}

@Composable
private fun StudioUndoBar(
    message: String,
    actionLabel: String,
    onUndo: () -> Unit
) {
    Surface(
        color = LevyraPanel,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 10.dp,
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 6.dp)
                .heightIn(min = 52.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = LevyraText,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onUndo, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(actionLabel, color = LevyraCyan, fontWeight = FontWeight.Bold)
            }
        }
    }
}
