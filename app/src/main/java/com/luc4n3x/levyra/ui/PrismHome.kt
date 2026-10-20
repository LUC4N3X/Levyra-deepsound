package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = LevyraText, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).semantics { heading() })
            actions()
        }
        if (subtitle.isNotBlank()) Text(subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(44.dp).height(3.dp).background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)), RoundedCornerShape(2.dp)))
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
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(
                    Brush.linearGradient(listOf(LevyraCyan.copy(alpha = 0.14f), LevyraPanel, LevyraViolet.copy(alpha = 0.10f)))
                ).border(1.dp, LevyraGlassBorder, RoundedCornerShape(24.dp))
                    .levyraPressable(onClick = hero.onOpen, onClickLabel = strings.play, role = Role.Button).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(hero.label, color = LevyraCyan, style = MaterialTheme.typography.labelMedium)
                    Text(hero.track.title, color = LevyraText, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(hero.track.artist, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            if (hero.isResolving && hero.isCurrent) LevyraLoadingIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                            else Icon(if (hero.isCurrent && hero.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                Box(Modifier.widthIn(max = 144.dp).weight(0.8f).aspectRatio(1f).clip(RoundedCornerShape(18.dp))) { CoverImage(hero.track, Modifier.fillMaxSize()) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
internal fun PrismCompactShelf(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    val pages = remember(items) { items.chunked(3) }
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(pages, key = { index, page -> "$index:${page.first().key}" }) { _, page ->
            Column(Modifier.width(304.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                page.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 76.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (item.current) LevyraPanelSoft else LevyraPanel.copy(alpha = 0.45f))
                            .levyraPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))) { item.artwork(Modifier.fillMaxSize(), item.round) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title, color = if (item.current) LevyraCyan else LevyraText, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(item.subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(LevyraPanel)
            .levyraPressable(onClick = onOpen, onClickLabel = title, role = Role.Button).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = LevyraText, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = LevyraCyan)
        }
        if (artwork != null) Box(Modifier.size(100.dp).clip(RoundedCornerShape(16.dp))) { CoverImage(artwork, Modifier.fillMaxSize()) }
    }
}

@Composable
internal fun PrismArtworkShelf(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(items, key = { _, item -> item.key }) { _, item ->
            Column(
                Modifier.width(if (item.round) 120.dp else 148.dp)
                    .levyraPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val shape = if (item.round) RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp, bottomStart = 18.dp, bottomEnd = 18.dp) else RoundedCornerShape(18.dp)
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape)) {
                    item.artwork(Modifier.fillMaxSize(), false)
                    if (item.current) Surface(Modifier.align(Alignment.BottomEnd).padding(8.dp), shape = RoundedCornerShape(10.dp), color = LevyraPanel) {
                        Box(Modifier.padding(8.dp)) { LevyraPlayingIndicator(playing = isPlaying, color = LevyraCyan, size = 14.dp) }
                    }
                }
                Text(item.title, color = if (item.current) LevyraCyan else LevyraText, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.subtitle.isNotBlank()) Text(item.subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
