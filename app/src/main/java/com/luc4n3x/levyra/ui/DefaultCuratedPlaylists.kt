package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign

private val CuratedCardShape = RoundedCornerShape(28.dp)
private val CuratedArtShape = RoundedCornerShape(20.dp)
private val CuratedCardWidth = 164.dp

internal fun defaultHomeOwnsCuratedPlaylists(): Boolean = defaultHomeUsesNewReleases(true)

@Composable
internal fun DefaultCuratedPlaylistShelves(onOpen: (PlaylistHit) -> Unit, fallback: @Composable () -> Unit) {
    if (!defaultHomeOwnsCuratedPlaylists()) {
        fallback()
        return
    }
    val code = LocalLevyraStrings.current.code
    val shelves = rememberCuratedPlaylistShelves(code)
    if (shelves.isEmpty()) {
        fallback()
        return
    }
    val copy = remember(code) { LevyraMonoHomeStrings.forCode(code) }
    val railMotion = defaultRailMotionEnabled()
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        shelves.forEachIndexed { index, shelf ->
            val items = remember(shelf.items) { shelf.items.filter { it.playlist != null }.distinctBy { it.key } }
            if (items.isEmpty()) return@forEachIndexed
            Column {
                LevyraSectionHeader(
                    title = if (index == 0) copy.mixedForYou else shelf.title,
                    subtitle = if (index == 0) copy.rotationOverline else shelf.strapline.ifBlank { null },
                    modifier = Modifier.padding(horizontal = LevyraHomeDesign.HorizontalInset).padding(bottom = 12.dp)
                )
                val rowState = rememberLazyListState()
                LazyRow(
                    state = rowState,
                    contentPadding = PaddingValues(horizontal = LevyraHomeDesign.HorizontalInset),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.key }, contentType = { "default-curated-playlist" }) { item ->
                        Box(Modifier.defaultRailItemMotion(railMotion, rowState, item.key)) {
                            DefaultCuratedPlaylistCard(item, Modifier.defaultRailParallax(railMotion, rowState, item.key)) { item.playlist?.let(onOpen) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultCuratedPlaylistCard(item: YoutubeMusicHomeItem, artworkMotion: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val subtitle = remember(item.subtitle) { monoWithoutYoutube(item.subtitle) }
    var failed by remember(item.thumbnailUrl) { mutableStateOf(false) }
    Column(
        Modifier.width(CuratedCardWidth).clip(CuratedCardShape).background(scheme.surfaceContainer)
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Tile, role = Role.Button)
            .semantics(mergeDescendants = true) { contentDescription = listOf(item.title, subtitle).filter(String::isNotBlank).joinToString(", ") }
            .padding(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(CuratedArtShape).background(scheme.surfaceContainerHighest)) {
            if (item.thumbnailUrl.isNotBlank() && !failed) {
                AsyncImage(item.thumbnailUrl, null, contentScale = ContentScale.Crop, onError = { failed = true }, modifier = Modifier.fillMaxSize().then(artworkMotion))
            }
        }
        Column(Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 6.dp).clearAndSetSemantics { }) {
            Text(
                item.title, color = scheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
