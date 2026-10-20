package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.*

@Composable
internal fun PrismPageHeader(title: String, subtitle: String = "", actions: @Composable RowScope.() -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, color = LevyraText, style = MaterialTheme.typography.headlineLarge, fontSize = 32.sp, lineHeight = 38.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).semantics { heading() }
            )
            actions()
        }
        if (subtitle.isNotBlank()) Text(subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun PrismPlayDisc(playing: Boolean, resolving: Boolean, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(PrismAccent.gradient()), contentAlignment = Alignment.Center) {
        if (resolving) LevyraLoadingIndicator(color = Color.White, modifier = Modifier.size(size * 0.42f))
        else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
internal fun PrismHomeHeader(greeting: String, date: String, hero: StudioHero?, inset: Dp, onSearch: () -> Unit, onSettings: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = inset)) {
        PrismPageHeader(greeting, date) {
            IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Search, strings.search, tint = LevyraText) }
            IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Settings, strings.settings, tint = LevyraText) }
        }
        if (hero != null) {
            Spacer(Modifier.height(8.dp))
            if (LevyraIsTorque) TorqueSoundtrackHero(hero, strings.play) else PrismSoundtrackHero(hero, strings.play)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun PrismCompactShelf(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    val pages = remember(items) { items.chunked(3) }
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = (inset - 8.dp).coerceAtLeast(0.dp)), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(pages, key = { index, page -> "$index:${page.first().key}" }) { _, page ->
            Column(Modifier.width(312.dp), verticalArrangement = Arrangement.spacedBy(if (LevyraIsTorque) 2.dp else 0.dp)) {
                page.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(PrismDesign.controlCorner))
                            .background(if (LevyraIsTorque) { if (item.current) LevyraPanelSoft else LevyraPanel } else if (item.current) PrismAccent.Tint else Color.Transparent)
                            .levyraPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(Modifier.size(56.dp).clip(if (item.round) CircleShape else RoundedCornerShape(PrismDesign.tileCorner))) { item.artwork(Modifier.fillMaxSize(), item.round) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.title, color = if (item.current) LevyraCyan else LevyraText, style = MaterialTheme.typography.titleSmall, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (item.subtitle.isNotBlank()) Text(item.subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (item.current) LevyraPlayingIndicator(playing = isPlaying, color = LevyraCyan, size = 12.dp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PrismCollectionCard(title: String, subtitle: String, artwork: Track?, onOpen: () -> Unit, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().clip(PrismDesign.cut(PrismDesign.surfaceCorner)).background(LevyraPanel)
            .levyraPressable(onClick = onOpen, onClickLabel = title, role = Role.Button).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = LevyraText, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(36.dp).clip(CircleShape).background(PrismAccent.Tint), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
            }
        }
        if (artwork != null) Box(Modifier.size(96.dp).clip(RoundedCornerShape(PrismDesign.controlCorner))) { CoverImage(artwork, Modifier.fillMaxSize()) }
    }
}

@Composable
internal fun PrismArtworkShelf(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(items, key = { _, item -> item.key }) { _, item ->
            Column(
                Modifier.width(if (item.round) 124.dp else 148.dp)
                    .levyraPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = if (item.round) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(if (item.round) CircleShape else RoundedCornerShape(PrismDesign.controlCorner))) {
                    item.artwork(Modifier.fillMaxSize(), false)
                    if (item.current) Box(
                        Modifier.align(Alignment.BottomEnd).padding(8.dp).size(28.dp).clip(CircleShape).background(PrismAccent.gradient()),
                        contentAlignment = Alignment.Center
                    ) { LevyraPlayingIndicator(playing = isPlaying, color = Color.White, size = 12.dp) }
                }
                Spacer(Modifier.height(2.dp))
                Text(item.title, color = if (item.current) LevyraCyan else LevyraText, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.subtitle.isNotBlank()) Text(item.subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
