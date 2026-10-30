package com.luc4n3x.levyra.ui.media

import androidx.compose.ui.geometry.center
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.atelierUppercaseLabel
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign

private const val AtelierGlyphDegreesPerSecond = 200f
private val AtelierGlyphVinyl = Color(0xFF0C0B0D)
private val AtelierGlyphGroove = Color.White.copy(alpha = 0.10f)
private val AtelierGlyphGlint = Color.White.copy(alpha = 0.38f)
private val AtelierSelectedFill = AtelierDesign.accent.copy(alpha = 0.12f)
private val AtelierCurrentScrim = Color.Black.copy(alpha = 0.52f)
private val AtelierNumberWidth = 36.dp

@Composable
internal fun AtelierRecordGlyph(spinning: Boolean, modifier: Modifier = Modifier) {
    val animated = LocalAnimationsEnabled.current
    val rotation = remember { mutableFloatStateOf(0f) }
    val turning = spinning && animated
    LaunchedEffect(turning) {
        if (!turning) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    rotation.floatValue = (rotation.floatValue + (now - last) / 1_000_000_000f * AtelierGlyphDegreesPerSecond) % 360f
                }
                last = now
            }
        }
    }
    Spacer(
        modifier.size(20.dp)
            .graphicsLayer { rotationZ = rotation.floatValue }
            .drawWithCache {
                val r = size.minDimension / 2f
                val ring = Stroke(0.6.dp.toPx())
                val glint = Stroke(1.dp.toPx(), cap = StrokeCap.Round)
                val arcTopLeft = Offset(size.center.x - r * 0.84f, size.center.y - r * 0.84f)
                val arcSize = Size(r * 1.68f, r * 1.68f)
                onDrawBehind {
                    drawCircle(AtelierGlyphVinyl, r)
                    drawCircle(AtelierGlyphGroove, r * 0.78f, style = ring)
                    drawCircle(AtelierGlyphGroove, r * 0.62f, style = ring)
                    drawArc(AtelierGlyphGlint, -70f, 46f, false, arcTopLeft, arcSize, style = glint)
                    drawCircle(AtelierDesign.accent, r * 0.36f)
                    drawCircle(AtelierDesign.onAccent, r * 0.08f)
                }
            }
    )
}

@Composable
internal fun AtelierExplicitMark(label: String) {
    Box(
        Modifier.border(1.dp, AtelierDesign.hairlineStrong, AtelierDesign.sleeveShape).padding(horizontal = 4.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(label.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, color = AtelierDesign.textSecondary, maxLines = 1)
    }
}

private fun Modifier.atelierRowRule(downloadProgress: Int?): Modifier = drawBehind {
    val thickness = 1.dp.toPx()
    val top = size.height - thickness
    drawRect(AtelierDesign.hairline, Offset(0f, top), Size(size.width, thickness))
    if (downloadProgress != null) {
        val width = size.width * downloadProgress.coerceIn(0, 100) / 100f
        val start = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
        drawRect(AtelierDesign.accent, Offset(start, top), Size(width, thickness))
    }
}

@Composable
internal fun AtelierAlbumTrackRow(
    index: Int,
    track: Track,
    subtitle: String,
    current: Boolean,
    playing: Boolean,
    selected: Boolean,
    downloaded: Boolean,
    downloadProgress: Int?,
    onPlay: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
    trailing: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    Column(modifier.fillMaxWidth()) {
        if (index == 0) {
            Text(
                atelierUppercaseLabel(copy.sideA), color = AtelierDesign.accent,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp), maxLines = 1,
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp).clearAndSetSemantics {}
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairlineStrong))
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = AtelierDesign.rowHeight)
                .background(if (selected) AtelierSelectedFill else Color.Transparent)
                .atelierRowRule(downloadProgress)
                .levyraPressable(onClick = onPlay, onLongClick = onLongClick, role = Role.Button, pressedScale = LevyraPressScale.Row)
                .semantics {
                    this.selected = selected
                    if (current) stateDescription = strings.playing
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(AtelierNumberWidth), contentAlignment = Alignment.CenterStart) {
                if (current) {
                    AtelierRecordGlyph(playing, Modifier.size(20.dp).clearAndSetSemantics {})
                } else {
                    Text(
                        (index + 1).toString(), color = AtelierDesign.textFaint, maxLines = 1,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Light, fontFeatureSettings = "tnum")
                    )
                }
            }
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    track.title, color = if (current) AtelierDesign.accentBright else AtelierDesign.text,
                    style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank() || downloaded || track.explicit) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (track.explicit) AtelierExplicitMark(copy.explicit)
                        if (downloaded) {
                            Icon(Icons.Rounded.DownloadDone, strings.offline, tint = AtelierDesign.accent, modifier = Modifier.size(14.dp))
                        }
                        if (subtitle.isNotBlank()) {
                            Text(
                                subtitle, color = AtelierDesign.textSecondary, style = MaterialTheme.typography.bodySmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (track.durationMs > 0L) {
                Text(
                    formatSeekbarMillis(track.durationMs), color = AtelierDesign.textFaint, maxLines = 1,
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum")
                )
            }
            trailing()
        }
    }
}

@Composable
internal fun AtelierLibraryTrackRow(
    track: Track,
    selected: Boolean,
    selectionActive: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Int?,
    metadata: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onQueue: (() -> Unit)?,
    onAddToPlaylist: (() -> Unit)?,
    onEditTags: (() -> Unit)?,
    onKeepOffline: (() -> Unit)?,
    onDeleteDownload: (() -> Unit)?,
    onRemoveFromPlaylist: (() -> Unit)?,
    onChangeMatch: (() -> Unit)?,
    modifier: Modifier
) {
    val strings = LocalLevyraStrings.current
    val subtitle = remember(track.artist, track.album, metadata) {
        listOf(track.artist, track.album, metadata?.trim().orEmpty()).filter(String::isNotBlank).joinToString(" · ")
    }
    Row(
        modifier.fillMaxWidth().heightIn(min = AtelierDesign.rowHeight)
            .background(if (selected) AtelierSelectedFill else Color.Transparent)
            .atelierRowRule(downloadProgress)
            .levyraPressable(onClick = onClick, onLongClick = onLongClick, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .semantics {
                this.selected = selected
                if (isCurrent) stateDescription = strings.playing
            }
            .padding(start = LevyraCardDesign.RowHorizontalPadding, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(AtelierDesign.rowArtwork).clip(AtelierDesign.sleeveShape).background(AtelierDesign.panel)
                .border(1.dp, AtelierDesign.hairline, AtelierDesign.sleeveShape),
            contentAlignment = Alignment.Center
        ) {
            CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
            if (isCurrent) {
                Box(Modifier.fillMaxSize().background(AtelierCurrentScrim), contentAlignment = Alignment.Center) {
                    AtelierRecordGlyph(isPlaying, Modifier.size(24.dp).clearAndSetSemantics {})
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                track.title, color = if (isCurrent) AtelierDesign.accentBright else AtelierDesign.text,
                style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isFavorite) Icon(Icons.Rounded.Favorite, strings.favoritesPlain, tint = AtelierDesign.accent, modifier = Modifier.size(12.dp))
                if (isDownloaded) Icon(Icons.Rounded.DownloadDone, strings.downloaded, tint = AtelierDesign.accent, modifier = Modifier.size(14.dp))
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle, color = AtelierDesign.textSecondary, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (track.durationMs > 0L) {
            Text(
                formatSeekbarMillis(track.durationMs), color = AtelierDesign.textFaint, maxLines = 1,
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum")
            )
        }
        if (selectionActive) {
            Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
                tint = if (selected) AtelierDesign.accentBright else AtelierDesign.textFaint,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        } else {
            AtelierLibraryTrackMenu(
                isFavorite, isDownloaded, downloadProgress, onClick, onFavorite, onDownload, onQueue, onAddToPlaylist,
                onEditTags, onKeepOffline, onDeleteDownload, onRemoveFromPlaylist, onChangeMatch
            )
        }
    }
}

@Composable
private fun AtelierLibraryTrackMenu(
    isFavorite: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Int?,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onQueue: (() -> Unit)?,
    onAddToPlaylist: (() -> Unit)?,
    onEditTags: (() -> Unit)?,
    onKeepOffline: (() -> Unit)?,
    onDeleteDownload: (() -> Unit)?,
    onRemoveFromPlaylist: (() -> Unit)?,
    onChangeMatch: (() -> Unit)?
) {
    val strings = LocalLevyraStrings.current
    var expanded by remember { mutableStateOf(false) }
    val pick: (() -> Unit) -> Unit = { action ->
        expanded = false
        action()
    }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, strings.songOptions, tint = AtelierDesign.textSecondary, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = AtelierDesign.panelRaised) {
            DropdownMenuItem(
                text = { Text(strings.play) },
                leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                onClick = { pick(onPlay) }
            )
            DropdownMenuItem(
                text = { Text(if (isFavorite) strings.removeFromFavorites else strings.addToFavorites) },
                leadingIcon = {
                    Icon(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null,
                        tint = if (isFavorite) AtelierDesign.accentBright else AtelierDesign.text
                    )
                },
                onClick = { pick(onFavorite) }
            )
            onQueue?.let { action ->
                DropdownMenuItem(
                    text = { Text(strings.addToQueue) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
                    onClick = { pick(action) }
                )
            }
            onAddToPlaylist?.let { action ->
                DropdownMenuItem(
                    text = { Text(strings.addToPlaylist) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                    onClick = { pick(action) }
                )
            }
            onEditTags?.let { action ->
                DropdownMenuItem(
                    text = { Text(strings.localEditTags) },
                    leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                    onClick = { pick(action) }
                )
            }
            onKeepOffline?.let { action ->
                DropdownMenuItem(
                    text = { Text(strings.saveOffline) },
                    leadingIcon = { Icon(Icons.Rounded.DownloadDone, null) },
                    onClick = { pick(action) }
                )
            }
            if (onDeleteDownload != null) {
                DropdownMenuItem(
                    text = { Text(strings.deleteDownload, color = AtelierDesign.error) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = AtelierDesign.error) },
                    onClick = { pick(onDeleteDownload) }
                )
            } else if (!isDownloaded && downloadProgress == null) {
                DropdownMenuItem(
                    text = { Text(strings.download) },
                    leadingIcon = { Icon(Icons.Rounded.Download, null) },
                    onClick = { pick(onDownload) }
                )
            }
            onChangeMatch?.let { action ->
                DropdownMenuItem(
                    text = { Text(com.luc4n3x.levyra.ui.i18n.playlistImportHubCopy(strings.code)["changeMatch"]) },
                    leadingIcon = { Icon(Icons.Rounded.SwapHoriz, null, tint = AtelierDesign.accent) },
                    onClick = { pick(action) }
                )
            }
            onRemoveFromPlaylist?.let { action ->
                DropdownMenuItem(
                    text = { Text(strings.removeFromPlaylist, color = AtelierDesign.error) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = AtelierDesign.error) },
                    onClick = { pick(action) }
                )
            }
        }
    }
}
