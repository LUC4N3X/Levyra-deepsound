package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.collections.PersonalCollectionSourceKind
import com.luc4n3x.levyra.data.collections.PersonalCollectionSummary
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.i18n.LevyraPersonalCollectionStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings

internal fun LazyListScope.personalCollectionsSection(
    collections: List<PersonalCollectionSummary>,
    syncingIds: Set<String>,
    query: String,
    strings: LevyraStrings,
    collectionStrings: LevyraPersonalCollectionStrings,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onPlay: (String) -> Unit
) {
    if (collections.isEmpty()) {
        item(key = "personal-collections-empty") {
            PersonalCollectionsEmptyState(collectionStrings, onAdd)
        }
        return
    }
    val visible = if (query.isBlank()) {
        collections
    } else {
        collections.filter { it.name.contains(query, ignoreCase = true) || it.sourceLabel.contains(query, ignoreCase = true) }
    }
    item(key = "personal-collections-add") {
        PersonalCollectionAddButton(collectionStrings.add, onAdd)
    }
    if (visible.isEmpty()) {
        item(key = "personal-collections-no-match") {
            LibraryEmpty(androidx.compose.material.icons.Icons.Rounded.LibraryMusic, strings.emptySearchPrompt)
        }
        return
    }
    item(key = "personal-collections-group") {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            visible.forEachIndexed { index, collection ->
                key(collection.id) {
                    PersonalCollectionRow(
                        collection = collection,
                        syncing = collection.id in syncingIds,
                        strings = strings,
                        collectionStrings = collectionStrings,
                        shapeIndex = index,
                        shapeCount = visible.size,
                        onOpen = { onOpen(collection.id) },
                        onPlay = { onPlay(collection.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PersonalCollectionsEmptyState(
    collectionStrings: LevyraPersonalCollectionStrings,
    onAdd: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(colors.surfaceContainerLow)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.LibraryMusic,
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.size(4.dp))
        Text(
            collectionStrings.emptyTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            collectionStrings.emptyDetail,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(4.dp))
        Button(onClick = onAdd, shapes = ButtonDefaults.shapes(), modifier = Modifier.heightIn(min = 52.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(collectionStrings.add)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PersonalCollectionAddButton(label: String, onAdd: () -> Unit) {
    FilledTonalButton(
        onClick = onAdd,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PersonalCollectionRow(
    collection: PersonalCollectionSummary,
    syncing: Boolean,
    strings: LevyraStrings,
    collectionStrings: LevyraPersonalCollectionStrings,
    shapeIndex: Int,
    shapeCount: Int,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainer,
        shape = levyraGroupedListShape(shapeIndex, shapeCount),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PersonalCollectionArtwork(collection.artworkModel, size = 64.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOf(strings.formatTrackCount(collection.trackCount), collection.sourceDescription(collectionStrings))
                        .filter(String::isNotBlank)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                PersonalCollectionStatusLine(collection, syncing, collectionStrings)
            }
            FilledTonalIconButton(
                onClick = onPlay,
                enabled = collection.trackCount > 0,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = strings.play)
            }
        }
    }
}

@Composable
internal fun PersonalCollectionStatusLine(
    collection: PersonalCollectionSummary,
    syncing: Boolean,
    collectionStrings: LevyraPersonalCollectionStrings,
    showError: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val error = collection.lastSyncError?.takeIf { showError }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        when {
            syncing -> {
                LevyraLoadingIndicator(modifier = Modifier.size(16.dp))
                Text(collectionStrings.refreshing, style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
            error != null -> {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp))
                Text(
                    collectionStrings.savedCopy,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            collection.lastSyncedAt > 0L -> {
                if (collection.autoRefresh) {
                    Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                Text(
                    collectionStrings.updated(collection.lastSyncedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun PersonalCollectionArtwork(model: String, size: Dp, large: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val shape = if (large) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(colors.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.LibraryMusic,
            contentDescription = null,
            tint = colors.onSecondaryContainer,
            modifier = Modifier.size(size * 0.42f)
        )
        if (model.isNotBlank()) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

internal fun PersonalCollectionSummary.sourceDescription(collectionStrings: LevyraPersonalCollectionStrings): String =
    if (sourceKind == PersonalCollectionSourceKind.URL) sourceLabel else collectionStrings.sourceFile
