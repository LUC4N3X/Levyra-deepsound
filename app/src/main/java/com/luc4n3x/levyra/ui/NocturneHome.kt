package com.luc4n3x.levyra.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
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
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.Nocturne78Icons
import com.luc4n3x.levyra.ui.theme.Nocturne78Palette

@Composable
internal fun NocturneHomeHeader(
    greeting: String,
    date: String,
    hero: StudioHero?,
    inset: Dp,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    dayPart: String = "",
    name: String = ""
) {
    val strings = LocalLevyraStrings.current
    val p = Nocturne78Palette
    Box(Modifier.fillMaxWidth().background(p.black)) {
        if (hero != null) {
            Box(Modifier.matchParentSize()) {
                Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.82f).fillMaxHeight()) {
                    if (hero.artistArtworkUrl.isNotBlank()) {
                        StudioRemoteArtwork(hero.artistArtworkUrl, hero.track.artist, Modifier.fillMaxSize(), true)
                    } else {
                        CoverImage(hero.track, Modifier.fillMaxSize())
                    }
                }
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                    0f to p.black, 0.35f to p.black.copy(alpha = 0.8f), 1f to Color.Transparent
                )))
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                    0f to p.black.copy(alpha = 0.88f), 0.38f to Color.Transparent,
                    0.72f to p.black.copy(alpha = 0.5f), 1f to p.black
                )))
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = inset, vertical = 12.dp)) {
            if (date.isNotBlank()) {
                Text(date, color = p.muted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (name.isNotBlank()) SkinNameGreeting(greeting, dayPart, name, Modifier.weight(1f))
                else Text(
                    greeting, color = p.text, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal,
                    fontSize = 27.sp, lineHeight = 32.sp, modifier = Modifier.weight(1f).semantics { heading() }
                )
                IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Search, contentDescription = strings.search, tint = p.text)
                }
                IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Settings, contentDescription = strings.settings, tint = p.text)
                }
            }
            if (hero != null) {
                Spacer(Modifier.height(104.dp))
                Text(hero.label, color = p.text, fontFamily = FontFamily.Serif, fontSize = 26.sp, lineHeight = 32.sp)
                Text(hero.lead, color = p.muted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Nocturne78Design.cardCorner))
                        .background(p.panel).border(1.dp, p.outline, RoundedCornerShape(Nocturne78Design.cardCorner))
                        .levyraPressable(onClick = hero.onOpen, onClickLabel = hero.track.title, role = Role.Button)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(Nocturne78Design.artworkCorner))) {
                        CoverImage(hero.track, Modifier.fillMaxSize())
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(hero.track.title, color = p.text, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(hero.track.artist, color = p.muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        Modifier.size(48.dp).border(BorderStroke(1.dp, p.outline), RoundedCornerShape(Nocturne78Design.controlCorner)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hero.isResolving && hero.isCurrent) {
                            LevyraLoadingIndicator(color = p.cyan, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(if (hero.isCurrent && hero.isPlaying) Nocturne78Icons.Pause else Nocturne78Icons.Play, null, tint = p.cyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NocturneCollectionCard(title: String, subtitle: String, artwork: Track?, onOpen: () -> Unit, modifier: Modifier) {
    val p = Nocturne78Palette
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Nocturne78Design.cardCorner)).background(p.panel)
            .border(1.dp, p.outline, RoundedCornerShape(Nocturne78Design.cardCorner))
            .levyraPressable(onClick = onOpen, onClickLabel = title, role = Role.Button).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (artwork != null) Box(Modifier.size(76.dp).clip(RoundedCornerShape(Nocturne78Design.artworkCorner))) {
            CoverImage(artwork, Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = p.text, fontFamily = FontFamily.Serif, fontSize = 20.sp, lineHeight = 25.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = p.muted, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = p.muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun NocturneCompactShelf(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    val pages = remember(items) { items.chunked(3) }
    val p = Nocturne78Palette
    LazyRow(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = inset),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        itemsIndexed(pages, key = { index, page -> "$index:${page.first().key}" }) { _, page ->
            Column(Modifier.width(300.dp)) {
                page.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 68.dp)
                            .levyraPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(Nocturne78Design.artworkCorner))) {
                            item.artwork(Modifier.fillMaxSize(), false)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(item.title, color = if (item.current) p.cyan else p.text, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(item.subtitle, color = p.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (item.current) LevyraPlayingIndicator(playing = isPlaying, color = p.cyan, size = 12.dp)
                    }
                    HorizontalDivider(color = p.outline.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
internal fun NocturneChartShelf(tracks: List<Track>, currentId: String?, isPlaying: Boolean, onPlay: (Track) -> Unit, inset: Dp) {
    val pages = remember(tracks) { tracks.chunked(5) }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = inset),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        itemsIndexed(pages, key = { index, page -> "$index:${page.first().id}" }) { pageIndex, page ->
            Column(Modifier.width(300.dp)) {
                page.forEachIndexed { rowIndex, track ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 68.dp)
                            .levyraPressable(onClick = { onPlay(track) }, onClickLabel = track.title, role = Role.Button)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text((pageIndex * 5 + rowIndex + 1).toString(), color = Nocturne78Palette.muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.width(24.dp))
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(Nocturne78Design.artworkCorner))) { CoverImage(track, Modifier.fillMaxSize()) }
                        Column(Modifier.weight(1f)) {
                            Text(track.title, color = Nocturne78Palette.text, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = Nocturne78Palette.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (track.id == currentId) LevyraPlayingIndicator(playing = isPlaying, color = Nocturne78Palette.cyan, size = 12.dp)
                    }
                    HorizontalDivider(color = Nocturne78Palette.outline.copy(alpha = 0.5f))
                }
            }
        }
    }
}
