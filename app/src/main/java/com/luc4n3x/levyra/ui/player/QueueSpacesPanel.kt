package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.player.queue.QueueSpaceSummary
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlass
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText

internal fun queueSpaceLabel(space: QueueSpaceSummary, strings: LevyraStrings): String =
    space.name.trim().ifEmpty { strings.queueSpaceDefaultName }

@Composable
internal fun QueueSpaceDestinationDialog(
    spaces: List<QueueSpaceSummary>,
    activeSpaceId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val orderedSpaces = remember(spaces, activeSpaceId) {
        spaces.sortedBy { it.id != activeSpaceId }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.addToQueue,
                color = LevyraText,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                orderedSpaces.forEach { space ->
                    TextButton(
                        onClick = { onSelect(space.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (space.id == activeSpaceId) {
                                    Icons.Rounded.Check
                                } else {
                                    Icons.AutoMirrored.Rounded.QueueMusic
                                },
                                contentDescription = null,
                                tint = if (space.id == activeSpaceId) LevyraCyan else LevyraMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = queueSpaceLabel(space, strings),
                                    color = LevyraText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (space.trackCount == 0) {
                                        strings.queueSpaceEmpty
                                    } else {
                                        strings.formatTrackCount(space.trackCount)
                                    },
                                    color = LevyraMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
internal fun QueueSpacesPanel(
    spaces: List<QueueSpaceSummary>,
    activeSpaceId: String,
    switching: Boolean,
    accent: Color,
    onSwitch: (String) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDuplicate: (String) -> Unit,
    onClear: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val strings = LocalLevyraStrings.current
    var managedSpaceId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorSpaceId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val managed = spaces.firstOrNull { it.id == managedSpaceId }
    val editing = spaces.firstOrNull { it.id == editorSpaceId }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.queueSpaces, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(
                    strings.queueSpacesSubtitle,
                    color = LevyraMuted,
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (switching) {
                CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(spaces, key = { it.id }, contentType = { "queue-space" }) { space ->
                QueueSpaceCard(
                    space = space,
                    label = queueSpaceLabel(space, strings),
                    active = space.id == activeSpaceId,
                    accent = accent,
                    enabled = !switching,
                    onClick = {
                        pendingDeleteId = null
                        if (space.id == activeSpaceId) {
                            managedSpaceId = if (managedSpaceId == space.id) null else space.id
                        } else {
                            onSwitch(space.id)
                        }
                    },
                    onManage = {
                        pendingDeleteId = null
                        editorSpaceId = null
                        managedSpaceId = if (managedSpaceId == space.id) null else space.id
                    }
                )
            }
            item(key = "queue-space-new", contentType = "queue-space-new") {
                QueueSpaceCreateCard(
                    label = strings.queueSpaceNew,
                    onClick = {
                        managedSpaceId = null
                        editorSpaceId = null
                        creating = true
                    }
                )
            }
        }

        if (creating) {
            QueueSpaceNameEditor(
                initialValue = "",
                placeholder = strings.queueSpaceNameHint,
                accent = accent,
                onCancel = { creating = false },
                onConfirm = { name ->
                    creating = false
                    onCreate(name)
                }
            )
        }

        if (editing != null) {
            QueueSpaceNameEditor(
                initialValue = queueSpaceLabel(editing, strings),
                placeholder = strings.queueSpaceNameHint,
                accent = accent,
                onCancel = { editorSpaceId = null },
                onConfirm = { name ->
                    editorSpaceId = null
                    onRename(editing.id, name)
                }
            )
        }

        if (managed != null) {
            QueueSpaceActions(
                space = managed,
                label = queueSpaceLabel(managed, strings),
                accent = accent,
                pendingDelete = pendingDeleteId == managed.id,
                onRename = {
                    editorSpaceId = managed.id
                    managedSpaceId = null
                },
                onDuplicate = {
                    managedSpaceId = null
                    onDuplicate(managed.id)
                },
                onClear = {
                    managedSpaceId = null
                    onClear(managed.id)
                },
                onRequestDelete = { pendingDeleteId = managed.id },
                onCancelDelete = { pendingDeleteId = null },
                onConfirmDelete = {
                    pendingDeleteId = null
                    managedSpaceId = null
                    onDelete(managed.id)
                },
                onClose = {
                    pendingDeleteId = null
                    managedSpaceId = null
                }
            )
        }
    }
}

@Composable
private fun QueueSpaceCard(
    space: QueueSpaceSummary,
    label: String,
    active: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    onManage: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = if (active) accent.copy(alpha = 0.12f) else LevyraGlass,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (active) accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.width(152.dp)
    ) {
        Column(
            modifier = Modifier
                .levyraPressable(
                    onClick = onClick,
                    enabled = enabled,
                    pressedScale = LevyraPressScale.Tile,
                    role = Role.Button,
                    onClickLabel = label,
                    haptic = LevyraHapticAction.Confirm
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QueueSpaceCollage(space.artworkUrls, accent, active)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        color = if (active) accent else LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (space.trackCount == 0) {
                            strings.queueSpaceEmpty
                        } else {
                            "${strings.formatTrackCount(space.trackCount)} · " +
                                formatQueueSpaceDuration(space.durationMs)
                        },
                        color = LevyraMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onManage, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.MoreHoriz,
                        contentDescription = strings.options,
                        tint = LevyraMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueSpaceCollage(artworkUrls: List<String>, accent: Color, active: Boolean) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(LevyraPanelSoft, shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            artworkUrls.isEmpty() -> Icon(
                Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                tint = if (active) accent else LevyraMuted,
                modifier = Modifier.size(26.dp)
            )
            artworkUrls.size == 1 -> AsyncImage(
                model = artworkUrls.first(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            else -> Column(modifier = Modifier.fillMaxSize()) {
                artworkUrls.take(4).chunked(2).forEach { rowUrls ->
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        rowUrls.forEach { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                        }
                        if (rowUrls.size == 1) Box(modifier = Modifier.weight(1f).fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSpaceCreateCard(label: String, onClick: () -> Unit) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier
                .levyraPressable(
                    onClick = onClick,
                    pressedScale = LevyraPressScale.Tile,
                    role = Role.Button,
                    onClickLabel = label,
                    haptic = LevyraHapticAction.Confirm
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(LevyraPanelSoft, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(24.dp))
            }
            Text(
                text = label,
                color = LevyraText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QueueSpaceNameEditor(
    initialValue: String,
    placeholder: String,
    accent: Color,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = LocalLevyraStrings.current
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it.take(48) },
            modifier = Modifier.weight(1f).height(56.dp),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text(placeholder, color = LevyraMuted, fontSize = 13.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = LevyraText,
                unfocusedTextColor = LevyraText,
                focusedContainerColor = LevyraGlass,
                unfocusedContainerColor = LevyraGlass,
                focusedBorderColor = accent.copy(alpha = 0.45f),
                unfocusedBorderColor = Color.Transparent,
                cursorColor = accent
            )
        )
        IconButton(onClick = { if (value.isNotBlank()) onConfirm(value.trim()) }) {
            Icon(Icons.Rounded.Check, contentDescription = strings.save, tint = accent)
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Rounded.Close, contentDescription = strings.cancel, tint = LevyraMuted)
        }
    }
}

@Composable
private fun QueueSpaceActions(
    space: QueueSpaceSummary,
    label: String,
    accent: Color,
    pendingDelete: Boolean,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onClear: () -> Unit,
    onRequestDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = LevyraGlass,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    color = LevyraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = strings.close,
                        tint = LevyraMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (pendingDelete) {
                Text(strings.queueSpaceDeleteConfirm, color = LevyraMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QueueSpaceAction(Icons.Rounded.Delete, strings.queueSpaceDelete, accent, onConfirmDelete)
                    QueueSpaceAction(Icons.Rounded.Close, strings.cancel, LevyraMuted, onCancelDelete)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QueueSpaceAction(
                        Icons.Rounded.DriveFileRenameOutline,
                        strings.queueSpaceRename,
                        LevyraText,
                        onRename
                    )
                    QueueSpaceAction(Icons.Rounded.ContentCopy, strings.queueSpaceDuplicate, LevyraText, onDuplicate)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QueueSpaceAction(
                        Icons.Rounded.LayersClear,
                        strings.queueSpaceClear,
                        LevyraText,
                        onClear,
                        enabled = space.trackCount > 0
                    )
                    QueueSpaceAction(Icons.Rounded.Delete, strings.queueSpaceDelete, LevyraMuted, onRequestDelete)
                }
            }
        }
    }
}

@Composable
private fun QueueSpaceAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        color = LevyraPanelSoft.copy(alpha = if (enabled) 1f else 0.4f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier
                .levyraPressable(
                    onClick = onClick,
                    enabled = enabled,
                    pressedScale = LevyraPressScale.Row,
                    role = Role.Button,
                    onClickLabel = label,
                    haptic = LevyraHapticAction.Confirm
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                color = if (enabled) tint else LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal fun formatQueueSpaceDuration(durationMs: Long): String {
    val totalMinutes = (durationMs.coerceAtLeast(0L) / 60_000L).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
