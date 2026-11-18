package com.luc4n3x.levyra.ui.library

import coil3.compose.AsyncImage
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.FilledIconButton
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.foundation.layout.offset
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.LevyraPastelRose
import com.luc4n3x.levyra.ui.components.LevyraPastelAmber
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.DownloadOwnership
import com.luc4n3x.levyra.domain.OfflineDownloadTask
import com.luc4n3x.levyra.domain.OfflineLaneProgress
import com.luc4n3x.levyra.domain.OfflineTransferState
import com.luc4n3x.levyra.domain.PreloadQueueItem
import com.luc4n3x.levyra.domain.preloadLaneProgress
import com.luc4n3x.levyra.player.offline.work.OfflineTransferLane
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.OfflineQueueCopy
import com.luc4n3x.levyra.ui.components.LevyraPastelLavender
import com.luc4n3x.levyra.ui.components.LevyraPastelPeriwinkle
import com.luc4n3x.levyra.ui.components.LevyraUtilitySeed
import com.luc4n3x.levyra.ui.components.levyraUtilityColors
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText

private const val COLLAPSED_LANE_ROWS = 5

internal fun offlineLaneTitle(lane: OfflineTransferLane, strings: LevyraStrings, copy: OfflineQueueCopy): String = when (lane) {
    OfflineTransferLane.Preload -> copy.preloading
    OfflineTransferLane.Manual -> strings.downloads
    OfflineTransferLane.SmartOffline -> strings.smartOffline.title
}

internal fun offlineMixTitle(mix: OfflineMix, strings: LevyraStrings, copy: OfflineQueueCopy): String = when (mix) {
    OfflineMix.Recent -> copy.recentlyDownloaded
    OfflineMix.Favorites -> strings.favoritesPlain
    OfflineMix.MostPlayed -> copy.mostPlayed
    OfflineMix.SmartOffline -> strings.smartOffline.title
}

internal fun LazyListScope.offlineTransferLanes(
    transfers: StateFlow<OfflineTransferState>,
    tasks: List<OfflineDownloadTask>,
    expandedLanes: Set<OfflineTransferLane>,
    strings: LevyraStrings,
    copy: OfflineQueueCopy,
    onToggleLane: (OfflineTransferLane) -> Unit,
    taskRow: @Composable (OfflineDownloadTask) -> Unit,
    leadingManualContent: LazyListScope.() -> Unit = {}
) {
    item(key = "offline-lane-${OfflineTransferLane.Preload.name}", contentType = "offline-preload-lane") {
        val preload = transfers.collectAsStateWithLifecycle().value.preload
        if (preload.isNotEmpty()) {
            val expanded = OfflineTransferLane.Preload in expandedLanes
            Column {
                OfflineLaneHeader(Icons.Rounded.Speed, LevyraPastelPeriwinkle, copy.preloading, preload.preloadLaneProgress())
                (if (expanded) preload else preload.take(COLLAPSED_LANE_ROWS)).forEach { item ->
                    key(item.key) { OfflinePreloadRow(item, LevyraPastelPeriwinkle) }
                }
                if (preload.size > COLLAPSED_LANE_ROWS && !expanded) {
                    LaneShowAll(strings.showAll, preload.size) { onToggleLane(OfflineTransferLane.Preload) }
                }
            }
        }
    }
    listOf(
        Triple(OfflineTransferLane.Manual, DownloadOwnership.MANUAL, Icons.Rounded.Download),
        Triple(OfflineTransferLane.SmartOffline, DownloadOwnership.SMART_OFFLINE, Icons.Rounded.AutoAwesome)
    ).forEach { (lane, ownership, icon) ->
        val laneTasks = tasks.filter { it.ownership == ownership }
        if (laneTasks.isEmpty()) {
            if (lane == OfflineTransferLane.Manual) leadingManualContent()
            return@forEach
        }
        val accent = if (lane == OfflineTransferLane.Manual) LevyraUtilitySeed else LevyraPastelLavender
        item(key = "offline-lane-${lane.name}", contentType = "offline-lane-header") {
            val progress = transfers.collectAsStateWithLifecycle().value.lanes[ownership]
                ?: OfflineLaneProgress.of(laneTasks.size, 0, laneTasks.sumOf { it.progress.coerceIn(0, 100) })
            OfflineLaneHeader(icon, accent, offlineLaneTitle(lane, strings, copy), progress)
        }
        if (lane == OfflineTransferLane.Manual) leadingManualContent()
        val expanded = lane in expandedLanes
        items(
            if (expanded) laneTasks else laneTasks.take(COLLAPSED_LANE_ROWS),
            key = { "task-${it.taskKey}" },
            contentType = { "download-task" }
        ) { taskRow(it) }
        if (laneTasks.size > COLLAPSED_LANE_ROWS && !expanded) {
            item(key = "offline-lane-more-${lane.name}", contentType = "offline-lane-more") {
                LaneShowAll(strings.showAll, laneTasks.size) { onToggleLane(lane) }
            }
        }
    }
}

@Composable
private fun LaneShowAll(label: String, count: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("$label · $count")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OfflineLaneHeader(icon: ImageVector, seed: Color, title: String, progress: OfflineLaneProgress) {
    val tone = levyraUtilityColors(seed)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(tone.container)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(tone.accent, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tone.onAccent, modifier = Modifier.size(22.dp))
            }
            Text(
                title,
                color = tone.onContainer,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            Text(
                "${progress.completed}/${progress.total} · ${progress.percent}%",
                color = tone.onContainer,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        LinearProgressIndicator(
            progress = { progress.percent / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = tone.accent,
            trackColor = tone.onContainer.copy(alpha = 0.14f)
        )
    }
}

@Composable
private fun OfflinePreloadRow(item: PreloadQueueItem, seed: Color) {
    val tone = levyraUtilityColors(seed)
    val percent = if (item.complete) 100 else item.progress.coerceIn(0, 100)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = LevyraText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artist, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("$percent%", color = if (item.complete) tone.accent else LevyraMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = tone.accent,
            trackColor = tone.softContainer
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun OfflineHero(
    badge: String,
    title: String,
    subtitle: String,
    covers: List<String>,
    playLabel: String,
    shuffleLabel: String,
    clearFilterLabel: String?,
    enabled: Boolean,
    onClearFilter: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    val tone = levyraUtilityColors(LevyraUtilitySeed)
    val blobShape = MaterialShapes.Cookie12Sided.toShape()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(tone.container)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 72.dp, y = (-64).dp)
                .size(200.dp)
                .graphicsLayer { rotationZ = 18f }
                .background(tone.accent.copy(alpha = 0.16f), blobShape)
        )
        OfflineCoverCluster(
            covers = covers,
            tone = tone,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 14.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tone.accent.copy(alpha = 0.22f))
                        .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.OfflinePin, contentDescription = null, tint = tone.onContainer, modifier = Modifier.size(16.dp))
                    Text(badge, color = tone.onContainer, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                if (clearFilterLabel != null) {
                    FilledTonalIconButton(
                        onClick = onClearFilter,
                        shape = MaterialShapes.Cookie4Sided.toShape(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = tone.onContainer.copy(alpha = 0.14f),
                            contentColor = tone.onContainer
                        )
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = clearFilterLabel)
                    }
                }
            }
            Spacer(Modifier.height(OfflineHeroTitleGap))
            Text(
                title,
                color = tone.onContainer,
                style = MaterialTheme.typography.headlineMediumEmphasized,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.52f).semantics { heading() }
            )
            Text(
                subtitle,
                color = tone.onContainer.copy(alpha = 0.74f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledIconButton(
                    onClick = onPlay,
                    enabled = enabled,
                    shapes = IconButtonDefaults.shapes(
                        shape = MaterialShapes.Cookie9Sided.toShape(),
                        pressedShape = MaterialShapes.Cookie4Sided.toShape()
                    ),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = tone.accent,
                        contentColor = tone.onAccent,
                        disabledContainerColor = tone.accent.copy(alpha = 0.38f),
                        disabledContentColor = tone.onAccent.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = playLabel, modifier = Modifier.size(34.dp))
                }
                FilledTonalButton(
                    onClick = onShuffle,
                    enabled = enabled,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.height(52.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = tone.onContainer.copy(alpha = 0.12f),
                        contentColor = tone.onContainer
                    )
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(shuffleLabel, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private val OfflineHeroTitleGap = 18.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OfflineCoverCluster(covers: List<String>, tone: com.luc4n3x.levyra.ui.components.LevyraUtilityColors, modifier: Modifier) {
    val slots = listOf(
        OfflineCoverSlot(MaterialShapes.Cookie9Sided, 92.dp, 40.dp, 0.dp, -8f),
        OfflineCoverSlot(MaterialShapes.Clover4Leaf, 70.dp, 0.dp, 62.dp, 10f),
        OfflineCoverSlot(MaterialShapes.Sunny, 60.dp, 86.dp, 80.dp, -14f)
    )
    Box(modifier = modifier.size(width = 150.dp, height = 148.dp).clearAndSetSemantics { }) {
        if (covers.isEmpty()) {
            Box(
                modifier = Modifier
                    .offset(x = 40.dp)
                    .size(92.dp)
                    .background(tone.accent, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.OfflinePin, contentDescription = null, tint = tone.onAccent, modifier = Modifier.size(36.dp))
            }
        } else {
            covers.take(slots.size).forEachIndexed { index, url ->
                val slot = slots[index]
                val shape = slot.shape.toShape()
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .offset(x = slot.x, y = slot.y)
                        .graphicsLayer { rotationZ = slot.rotation }
                        .size(slot.size)
                        .clip(shape)
                        .background(tone.accent.copy(alpha = 0.3f))
                        .border(3.dp, tone.container, shape)
                )
            }
        }
    }
}

private data class OfflineCoverSlot(
    val shape: androidx.graphics.shapes.RoundedPolygon,
    val size: androidx.compose.ui.unit.Dp,
    val x: androidx.compose.ui.unit.Dp,
    val y: androidx.compose.ui.unit.Dp,
    val rotation: Float
)

internal enum class OfflineRailStyle { Mix, Artist, Playlist }

private val OfflineMixTileSize = 148.dp
private val OfflineArtistSize = 96.dp
private val OfflinePlaylistSize = 128.dp
private val OfflineTileShape = RoundedCornerShape(28.dp)

internal fun offlineMixSeed(mix: OfflineMix): Color = when (mix) {
    OfflineMix.Recent -> LevyraUtilitySeed
    OfflineMix.Favorites -> LevyraPastelRose
    OfflineMix.MostPlayed -> LevyraPastelAmber
    OfflineMix.SmartOffline -> LevyraPastelLavender
}

private fun offlineMixIcon(mix: OfflineMix): ImageVector = when (mix) {
    OfflineMix.Recent -> Icons.Rounded.History
    OfflineMix.Favorites -> Icons.Rounded.Favorite
    OfflineMix.MostPlayed -> Icons.Rounded.Replay
    OfflineMix.SmartOffline -> Icons.Rounded.AutoAwesome
}

@Composable
internal fun OfflineCollectionRail(
    title: String,
    icon: ImageVector,
    seed: Color,
    style: OfflineRailStyle,
    collections: List<OfflineCollection>,
    selectedKey: String?,
    label: (OfflineCollection) -> String,
    countLabel: (Int) -> String,
    onSelect: (OfflineCollection) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LevyraSectionHeader(
            title = title,
            modifier = Modifier.padding(top = 12.dp),
            titleColor = MaterialTheme.colorScheme.onSurface,
            subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            leadingIcon = icon,
            leadingSeed = seed
        )
        LazyRow(
            modifier = Modifier.libraryCategoriesBleed(LibraryCategoriesGutter),
            contentPadding = PaddingValues(horizontal = LibraryCategoriesGutter),
            horizontalArrangement = Arrangement.spacedBy(if (style == OfflineRailStyle.Artist) 16.dp else 12.dp)
        ) {
            items(collections, key = { it.key }, contentType = { "offline-${style.name}" }) { collection ->
                val selected = collection.key == selectedKey
                val name = label(collection)
                val count = countLabel(collection.itemKeys.size)
                when (style) {
                    OfflineRailStyle.Mix -> OfflineMixTile(collection, name, count, selected) { onSelect(collection) }
                    OfflineRailStyle.Artist -> OfflineArtistCircle(collection, name, count, selected) { onSelect(collection) }
                    OfflineRailStyle.Playlist -> OfflinePlaylistCard(collection, name, count, selected) { onSelect(collection) }
                }
            }
        }
    }
}

@Composable
private fun OfflineArtworkImage(collection: OfflineCollection, title: String, shape: Shape, modifier: Modifier) {
    val track = collection.artwork
    LibraryArtwork(
        url = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
        fallbackUrl = com.luc4n3x.levyra.domain.LevyraPersonalOrbit.youtubeFallbackArtwork(track).orEmpty(),
        title = title,
        modifier = modifier,
        shape = shape,
        selected = false
    )
}

@Composable
private fun Modifier.offlineSelectable(selected: Boolean, label: String, onClick: () -> Unit): Modifier =
    this
        .semantics(mergeDescendants = true) { this.selected = selected }
        .levyraPressable(onClick = onClick, role = Role.Tab, onClickLabel = label, pressedScale = LevyraPressScale.Tile)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OfflineMixTile(collection: OfflineCollection, title: String, count: String, selected: Boolean, onClick: () -> Unit) {
    val mix = collection.mix ?: OfflineMix.Recent
    val tone = levyraUtilityColors(offlineMixSeed(mix))
    Box(
        modifier = Modifier
            .size(OfflineMixTileSize)
            .clip(OfflineTileShape)
            .background(tone.container)
            .then(if (selected) Modifier.border(3.dp, tone.accent, OfflineTileShape) else Modifier)
            .offlineSelectable(selected, title, onClick)
    ) {
        OfflineArtworkImage(collection, title, OfflineTileShape, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.08f),
                        1f to Color.Black.copy(alpha = 0.78f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .padding(10.dp)
                .size(36.dp)
                .background(tone.accent, MaterialShapes.Cookie4Sided.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(offlineMixIcon(mix), contentDescription = null, tint = tone.onAccent, modifier = Modifier.size(20.dp))
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleSmallEmphasized, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(count, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun OfflineArtistCircle(collection: OfflineCollection, title: String, count: String, selected: Boolean, onClick: () -> Unit) {
    val tone = levyraUtilityColors(LevyraUtilitySeed)
    Column(
        modifier = Modifier.width(OfflineArtistSize + 8.dp).offlineSelectable(selected, title, onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(OfflineArtistSize + 8.dp)
                .then(if (selected) Modifier.border(3.dp, tone.accent, CircleShape) else Modifier)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            OfflineArtworkImage(collection, title, CircleShape, Modifier.size(OfflineArtistSize))
        }
        Text(
            title,
            color = if (selected) tone.accent else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(count, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun OfflinePlaylistCard(collection: OfflineCollection, title: String, count: String, selected: Boolean, onClick: () -> Unit) {
    val tone = levyraUtilityColors(LevyraPastelLavender)
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier.width(OfflinePlaylistSize).offlineSelectable(selected, title, onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OfflineArtworkImage(
            collection,
            title,
            shape,
            Modifier
                .size(OfflinePlaylistSize)
                .then(if (selected) Modifier.border(3.dp, tone.accent, shape) else Modifier)
        )
        Text(
            title,
            color = if (selected) tone.accent else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(count, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
