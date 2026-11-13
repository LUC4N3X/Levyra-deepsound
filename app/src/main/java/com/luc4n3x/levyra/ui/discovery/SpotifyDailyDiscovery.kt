package com.luc4n3x.levyra.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.feature.discovery.DiscoveryCard
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.SpotifyHomeCoverCard
import com.luc4n3x.levyra.ui.YouTubeMusicSectionHeader
import com.luc4n3x.levyra.ui.YouTubeMusicSongColumns
import com.luc4n3x.levyra.ui.YouTubeMusicSongRowModel
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraYouTubeMusicSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign

private val SpotifyDiscoverCover = 176.dp
private val SpotifyDiscoverShape = RoundedCornerShape(4.dp)
private const val SpotifyDiscoverMosaic = 4

private class SkinDiscoverModel(val title: String, val cards: List<DiscoveryCard>, val reasons: List<String>, val onOpen: () -> Unit)

@Composable
private fun rememberSkinDiscover(handle: DiscoveryHomeHandle): SkinDiscoverModel? {
    val ui by handle.state.collectAsStateWithLifecycle()
    LaunchedEffect(handle) { handle.onWarm() }
    val code = LocalLevyraStrings.current.code
    val copy = remember(code) { discoveryCopy(code) }
    val music = remember(code) { smartMusicCopy(code) }
    val cards = ui.teaser
    if (cards.isEmpty()) return null
    return remember(cards, copy, music) { SkinDiscoverModel(copy.title, cards, cards.map { discoveryReasonLabel(it, copy, music) }, handle.onOpen) }
}

@Composable
internal fun SpotifyDiscoverForYou(handle: DiscoveryHomeHandle) {
    val model = rememberSkinDiscover(handle)
    val state by handle.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (model != null) SpotifyDiscoverRow(model)
        DiscoveryLevelChips(
            state.request.level,
            DiscoveryLevelChipStyle(
                shape = CircleShape, textStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                selectedContainer = SpotifyDesign.green, selectedContent = Color.Black,
                container = Color(0xFF2A2A2A), content = Color.White
            ),
            handle.onOpenBubble, Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun SpotifyDiscoverRow(model: SkinDiscoverModel) {
    val lead = model.cards.first()
    val tone = remember(lead.track.accentStart) { Color(lead.track.accentStart) }
    val artists = remember(model.cards) { model.cards.map { it.track.artist }.distinct().take(3).joinToString(", ") }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            model.title, color = SpotifyDesign.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).semantics { heading() }
        )
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(SpotifyDesign.green)
                .levyraPressable(onClick = model.onOpen, role = Role.Button, onClickLabel = model.title),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(28.dp))
        }
    }
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "spotify-discover-mix", contentType = "spotify-discover-mix") {
            SpotifyDiscoverMixCard(model.cards, model.title, artists, tone, model.onOpen)
        }
        itemsIndexed(model.cards, key = { _, card -> "spotify-discover-${card.key}" }, contentType = { _, _ -> "spotify-discover-track" }) { index, card ->
            SpotifyHomeCoverCard(
                card.track.title, model.reasons.getOrElse(index) { card.track.artist },
                card.track.largeThumbnailUrl.ifBlank { card.track.thumbnailUrl },
                onClick = model.onOpen
            )
        }
    }
}

@Composable
private fun SpotifyDiscoverMixCard(cards: List<DiscoveryCard>, title: String, artists: String, tone: Color, onClick: () -> Unit) {
    Column(Modifier.width(SpotifyDiscoverCover).levyraPressable(onClick = onClick, onClickLabel = title, role = Role.Button)) {
        Box(
            Modifier.size(SpotifyDiscoverCover).clip(SpotifyDiscoverShape)
                .background(Brush.linearGradient(listOf(tone, Color(0xFF121212))))
        ) {
            Column(Modifier.fillMaxSize()) {
                cards.take(SpotifyDiscoverMosaic).chunked(2).forEach { pair ->
                    Row(Modifier.weight(1f)) {
                        pair.forEach { card -> Box(Modifier.weight(1f).fillMaxSize()) { CoverImage(card.track, Modifier.fillMaxSize()) } }
                    }
                }
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))))
            Box(Modifier.padding(10.dp).size(14.dp).clip(CircleShape).background(SpotifyDesign.green))
            Text(
                title, color = Color.White, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 10.dp, bottom = 12.dp)
            )
        }
        Text(
            artists, color = SpotifyDesign.subdued, fontSize = 14.sp, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp).heightIn(min = 18.dp)
        )
    }
}

@Composable
internal fun YouTubeMusicDiscoverForYou(handle: DiscoveryHomeHandle) {
    val model = rememberSkinDiscover(handle)
    val state by handle.state.collectAsStateWithLifecycle()
    val code = LocalLevyraStrings.current.code
    val ytm = remember(code) { LevyraYouTubeMusicSkinStrings.forCode(code) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (model != null) {
            val rows = remember(model) {
                model.cards.mapIndexed { index, card ->
                    YouTubeMusicSongRowModel("ytm-discover-${card.key}", card.track, model.reasons.getOrElse(index) { card.track.artist }, model.onOpen, model.onOpen)
                }
            }
            YouTubeMusicSectionHeader(model.title, actionLabel = ytm.playAll, onAction = model.onOpen)
            Spacer(Modifier.padding(top = 4.dp))
            YouTubeMusicSongColumns(rows, null, ytm.explicit)
        }
        DiscoveryLevelChips(
            state.request.level,
            DiscoveryLevelChipStyle(
                shape = YouTubeMusicDesign.chipShape, textStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                selectedContainer = Color.White, selectedContent = Color.Black,
                container = Color.White.copy(alpha = 0.1f), content = Color.White
            ),
            handle.onOpenBubble, Modifier.padding(horizontal = YouTubeMusicDesign.gutter)
        )
    }
}
