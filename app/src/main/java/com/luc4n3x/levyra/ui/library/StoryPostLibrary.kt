package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.storypost.StoryPost
import com.luc4n3x.levyra.data.storypost.StoryPostKind
import com.luc4n3x.levyra.data.storypost.StoryPostSelection
import com.luc4n3x.levyra.data.storypost.StoryPostSourceFile
import com.luc4n3x.levyra.ui.i18n.LevyraStoryPostStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.StoryPostImportState
import java.util.Locale

internal data class StoryPostLibraryCallbacks(
    val onImportFolder: () -> Unit,
    val onImportZip: () -> Unit,
    val onPlay: (StoryPost) -> Unit,
    val onDelete: (StoryPost) -> Unit
)

internal fun LazyListScope.storyPostItems(
    posts: List<StoryPost>,
    reading: Boolean,
    currentTrackId: String?,
    isPlaying: Boolean,
    storyStrings: LevyraStoryPostStrings,
    callbacks: StoryPostLibraryCallbacks
) {
    item(key = "story-post-import", contentType = "story-post-import") {
        StoryPostImportCard(posts.isEmpty(), reading, storyStrings, callbacks)
    }
    items(posts, key = { "story-post-${it.id}" }, contentType = { "story-post" }) { post ->
        StoryPostRow(
            post = post,
            current = post.trackId == currentTrackId,
            playing = isPlaying && post.trackId == currentTrackId,
            storyStrings = storyStrings,
            onPlay = { callbacks.onPlay(post) },
            onDelete = { callbacks.onDelete(post) }
        )
    }
}

@Composable
private fun StoryPostImportCard(
    empty: Boolean,
    reading: Boolean,
    storyStrings: LevyraStoryPostStrings,
    callbacks: StoryPostLibraryCallbacks
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainer,
        shape = LevyraCardDesign.SurfaceShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = colors.onPrimaryContainer)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        storyStrings.tab,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    if (empty || reading) {
                        Text(
                            if (reading) storyStrings.reading else storyStrings.emptyDetail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                if (reading) {
                    CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StoryPostImportButton(
                    label = storyStrings.importFolder,
                    icon = Icons.Rounded.Folder,
                    primary = true,
                    enabled = !reading,
                    onClick = callbacks.onImportFolder
                )
                StoryPostImportButton(
                    label = storyStrings.importZip,
                    icon = Icons.Rounded.FolderZip,
                    primary = false,
                    enabled = !reading,
                    onClick = callbacks.onImportZip
                )
            }
        }
    }
}

@Composable
private fun StoryPostImportButton(
    label: String,
    icon: ImageVector,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    val content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
    if (primary) {
        Button(onClick = onClick, enabled = enabled, modifier = modifier, content = content)
    } else {
        FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier, content = content)
    }
}

@Composable
private fun StoryPostRow(
    post: StoryPost,
    current: Boolean,
    playing: Boolean,
    storyStrings: LevyraStoryPostStrings,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    var confirmDelete by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onPlay)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LevyraPanelSoft),
            contentAlignment = Alignment.Center
        ) {
            val cover = post.cover ?: post.images.firstOrNull()
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp)
                )
            } else {
                Icon(Icons.Rounded.Movie, contentDescription = null, tint = LevyraMuted)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = post.title,
                color = if (current) LevyraCyan else LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (playing) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(14.dp))
                }
                Text(
                    text = listOfNotNull(
                        if (post.kind == StoryPostKind.Video) storyStrings.video else storyStrings.photos(post.images.size),
                        post.durationMs.takeIf { it > 0L }?.let(::storyPostDuration)
                    ).joinToString(" · "),
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = { confirmDelete = true }, enabled = !current) {
            Icon(Icons.Rounded.Delete, contentDescription = strings.delete, tint = if (current) LevyraMuted.copy(alpha = 0.38f) else LevyraMuted)
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(storyStrings.deleteTitle) },
            text = { Text(storyStrings.deleteBody) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
internal fun StoryPostImportHost(
    state: StoryPostImportState,
    storyStrings: LevyraStoryPostStrings,
    onSelect: (StoryPostSelection) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state) {
        is StoryPostImportState.Preview -> StoryPostPreviewSheet(state, storyStrings, onSelect, onSave, onDismiss)
        is StoryPostImportState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(storyStrings.errorTitle) },
            text = { Text(storyStrings.error(state.error.ordinal)) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(LocalLevyraStrings.current.close) } }
        )
        StoryPostImportState.Idle, StoryPostImportState.Reading -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoryPostPreviewSheet(
    state: StoryPostImportState.Preview,
    storyStrings: LevyraStoryPostStrings,
    onSelect: (StoryPostSelection) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val draft = state.draft
    val plan = draft.plan
    val selection = state.selection
    val video = plan.kind == StoryPostKind.Video
    ModalBottomSheet(
        onDismissRequest = { if (!state.saving) onDismiss() },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = STORY_POST_SHEET_GUTTER, end = STORY_POST_SHEET_GUTTER, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    storyStrings.previewTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = listOf(
                        draft.sourceName,
                        if (video) storyStrings.video else storyStrings.photos(plan.images.size)
                    ).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!video && plan.images.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.libraryCategoriesBleed(STORY_POST_SHEET_GUTTER),
                    contentPadding = PaddingValues(horizontal = STORY_POST_SHEET_GUTTER),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(plan.images, key = { _, image -> image.path }) { index, image ->
                        StoryPostPreviewImage(index, plan.images.size, image, storyStrings)
                    }
                }
            }
            if (video) {
                StoryPostChoice(
                    title = storyStrings.videoFile,
                    icon = Icons.Rounded.Movie,
                    options = plan.videoCandidates,
                    selected = selection.videoPath,
                    onSelect = { onSelect(selection.copy(videoPath = it)) }
                )
            } else {
                StoryPostChoice(
                    title = storyStrings.music,
                    icon = Icons.Rounded.GraphicEq,
                    options = plan.audioCandidates,
                    selected = selection.audioPath,
                    onSelect = { onSelect(selection.copy(audioPath = it)) }
                )
            }
            if (plan.captionCandidates.isNotEmpty()) {
                StoryPostChoice(
                    title = storyStrings.caption,
                    icon = null,
                    options = plan.captionCandidates,
                    selected = selection.captionPath,
                    onSelect = { onSelect(selection.copy(captionPath = it)) },
                    noneLabel = storyStrings.noCaption.takeIf { plan.needsCaptionChoice },
                    onNone = { onSelect(selection.copy(captionPath = null)) }
                )
                selection.captionPath?.let(draft.captions::get)?.takeIf(String::isNotBlank)?.let { caption ->
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(14.dp)) {
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss, enabled = !state.saving) { Text(strings.cancel) }
                Button(onClick = onSave, enabled = !state.saving) {
                    if (state.saving) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Text(storyStrings.saving, modifier = Modifier.padding(start = 8.dp))
                    } else {
                        Text(strings.save)
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryPostPreviewImage(
    index: Int,
    total: Int,
    image: StoryPostSourceFile,
    storyStrings: LevyraStoryPostStrings
) {
    Box(
        modifier = Modifier
            .height(132.dp)
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        AsyncImage(
            model = image.previewModel,
            contentDescription = storyStrings.photoPosition(index + 1, total),
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
        ) {
            Text(
                text = (index + 1).toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun StoryPostChoice(
    title: String,
    icon: ImageVector?,
    options: List<StoryPostSourceFile>,
    selected: String?,
    onSelect: (String) -> Unit,
    noneLabel: String? = null,
    onNone: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        if (options.size == 1 && noneLabel == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Text(
                    options.first().name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            return@Column
        }
        Column(modifier = Modifier.selectableGroup()) {
            options.forEach { option ->
                StoryPostChoiceRow(option.name, option.path == selected) { onSelect(option.path) }
            }
            if (noneLabel != null) StoryPostChoiceRow(noneLabel, selected == null, onNone)
        }
    }
}

@Composable
private fun StoryPostChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private val STORY_POST_SHEET_GUTTER = 24.dp

private fun storyPostDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60L, totalSeconds % 60L)
}
