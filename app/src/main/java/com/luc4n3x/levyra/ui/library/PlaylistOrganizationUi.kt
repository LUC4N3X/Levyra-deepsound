package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.PLAYLIST_TAG_MAX_PER_PLAYLIST
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.PlaylistTag
import com.luc4n3x.levyra.domain.isValidPlaylistTagName
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted

@Composable
internal fun LibraryPlaylistFilterRow(
    tags: List<PlaylistTag>,
    selectedTagIds: Set<String>,
    hiddenVisible: Boolean,
    hiddenCount: Int,
    onClearTags: () -> Unit,
    onToggleTag: (String) -> Unit,
    onToggleHidden: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    if (tags.isEmpty() && hiddenCount == 0) return
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (tags.isNotEmpty()) {
            LibraryCategoryChip(
                label = strings.all,
                selected = selectedTagIds.isEmpty(),
                onClick = onClearTags
            )
            tags.forEach { tag ->
                LibraryCategoryChip(
                    label = tag.name,
                    selected = tag.id in selectedTagIds,
                    onClick = { onToggleTag(tag.id) }
                )
            }
        }
        if (hiddenCount > 0) {
            LibraryCategoryChip(
                label = "${strings.hiddenPlaylists} · $hiddenCount",
                selected = hiddenVisible,
                onClick = onToggleHidden
            )
        }
    }
}

@Composable
internal fun PlaylistTagsLine(
    playlist: Playlist,
    contentColor: Color,
    onEditTags: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (playlist.tags.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        playlist.tags.forEach { tag ->
            PlaylistTagChip(label = tag.name, contentColor = contentColor, onClick = onEditTags)
        }
    }
}

@Composable
private fun PlaylistTagChip(label: String, contentColor: Color, onClick: () -> Unit) {
    Surface(
        color = contentColor.copy(alpha = 0.10f),
        shape = CircleShape,
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = contentColor.copy(alpha = 0.88f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
internal fun PlaylistTagEditorDialog(
    assignedTagIds: Set<String>,
    tags: List<PlaylistTag>,
    onDismiss: () -> Unit,
    onToggleTag: (String) -> Unit,
    onCreateTag: (String) -> Unit,
    onRenameTag: (String, String) -> Unit,
    onDeleteTag: (String) -> Unit
) {
    val strings = LocalLevyraStrings.current
    var tagName by remember { mutableStateOf("") }
    var renamingTagId by remember { mutableStateOf<String?>(null) }
    val limitReached = assignedTagIds.size >= PLAYLIST_TAG_MAX_PER_PLAYLIST

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.playlistTags) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (limitReached) {
                    Text(
                        text = strings.playlistTagLimitReached,
                        color = LevyraMuted,
                        fontSize = 12.sp
                    )
                }
                tags.forEach { tag ->
                    val selected = tag.id in assignedTagIds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LibraryCategoryChip(
                            label = tag.name,
                            selected = selected,
                            onClick = {
                                if (selected || !limitReached) onToggleTag(tag.id)
                            }
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = {
                                    renamingTagId = tag.id
                                    tagName = tag.name
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = strings.editPlaylistTags,
                                    tint = if (renamingTagId == tag.id) LevyraCyan else LevyraMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (renamingTagId == tag.id) {
                                        renamingTagId = null
                                        tagName = ""
                                    }
                                    onDeleteTag(tag.id)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = strings.delete,
                                    tint = LevyraMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                val editingTagId = renamingTagId
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.playlistTagName) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (!isValidPlaylistTagName(tagName)) return@IconButton
                                if (editingTagId != null) {
                                    onRenameTag(editingTagId, tagName)
                                    renamingTagId = null
                                } else {
                                    onCreateTag(tagName)
                                }
                                tagName = ""
                            },
                            enabled = isValidPlaylistTagName(tagName) &&
                                (editingTagId != null || !limitReached)
                        ) {
                            Icon(
                                imageVector = if (editingTagId != null) Icons.Rounded.Check else Icons.Rounded.Add,
                                contentDescription = if (editingTagId != null) {
                                    strings.editPlaylistTags
                                } else {
                                    strings.newPlaylistTag
                                }
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.done) }
        }
    )
}
