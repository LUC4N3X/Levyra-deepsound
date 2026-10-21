package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit

@Composable
internal fun IvoryInkTab(start: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    Box(modifier.clip(ivoryEdgeTabShape(start)).background(IvoryDesign.ink), contentAlignment = Alignment.Center, content = content)
}

@Composable
internal fun IvorySectionTitle(title: String, inset: Dp, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(end = inset).heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IvoryInkTab(start = true, Modifier.width(inset + 6.dp).height(30.dp))
        Text(
            title, color = LevyraText, style = MaterialTheme.typography.titleLarge, fontSize = 21.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).semantics { heading() }
        )
        actions()
    }
}

@Composable
internal fun IvoryCornerBadge(size: Dp, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.size(size).clip(IvoryCornerShape).background(IvoryDesign.ink)) {
        Box(Modifier.align(Alignment.BottomEnd).size(size * 0.72f), contentAlignment = Alignment.Center, content = content)
    }
}

@Composable
private fun IvoryCornerGlyph(current: Boolean, playing: Boolean, size: Dp) {
    if (current) LevyraPlayingIndicator(playing = playing, color = IvoryDesign.onInk, size = size * 0.3f)
    else Icon(Icons.Rounded.PlayArrow, null, tint = IvoryDesign.onInk, modifier = Modifier.size(size * 0.42f))
}

@Composable
internal fun IvoryHomeHeader(
    greeting: String, date: String, hero: StudioHero?, inset: Dp, onSearch: () -> Unit, onSettings: () -> Unit,
    dayPart: String = "", name: String = ""
) {
    val strings = LocalLevyraStrings.current
    Box(Modifier.fillMaxWidth()) {
        if (hero != null) IvoryHeroBackdrop(hero, Modifier.fillMaxWidth().height(IvoryHeroHeight))
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (name.isNotBlank()) SkinNameGreeting(greeting, dayPart, name, Modifier.weight(1f).padding(start = inset, end = 12.dp))
                else Column(Modifier.weight(1f).padding(start = inset, end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        greeting, color = LevyraText, style = MaterialTheme.typography.headlineLarge, fontSize = 30.sp, lineHeight = 36.sp,
                        fontWeight = FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                    )
                    if (date.isNotBlank()) Text(date, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                IvoryInkTab(start = false, Modifier.height(64.dp)) {
                    Row(Modifier.padding(start = 6.dp, end = inset - 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Search, strings.search, tint = IvoryDesign.onInk) }
                        IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Settings, strings.settings, tint = IvoryDesign.onInk) }
                    }
                }
            }
            if (hero != null) IvorySoundtrackStage(hero, strings.play, inset)
        }
    }
}

private val IvoryHeroHeight = 620.dp

private fun StudioHero.ivorySecondLine(): String =
    lead.takeIf { it.isNotBlank() && !it.trim().endsWith(track.artist.trim(), ignoreCase = true) } ?: track.title

@Composable
private fun IvoryHeroBackdrop(hero: StudioHero, modifier: Modifier) {
    val fade = remember {
        Brush.verticalGradient(0.04f to Color.Transparent, 0.3f to Color.Black, 0.52f to Color.Black, 0.76f to Color.Transparent)
    }
    Box(
        modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent { drawContent(); drawRect(fade, blendMode = BlendMode.DstIn) }
    ) {
        if (hero.artistArtworkUrl.isNotBlank()) {
            AsyncImage(hero.artistArtworkUrl, contentDescription = null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
        } else {
            CoverImage(hero.track, Modifier.fillMaxSize(), highRes = true)
        }
    }
}

@Composable
private fun IvorySoundtrackStage(hero: StudioHero, playLabel: String, inset: Dp) {
    val playing = hero.isCurrent && hero.isPlaying
    val ink = IvoryDesign.ink
    val onInk = IvoryDesign.onInk
    Column(
        Modifier.fillMaxWidth().height(IvoryHeroHeight - 112.dp)
            .levyraPressable(onClick = hero.onOpen, onClickLabel = playLabel, role = Role.Button)
    ) {
        Spacer(Modifier.height(10.dp))
        IvoryInkTab(start = true, Modifier.height(54.dp)) {
            Row(Modifier.padding(start = inset, end = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(IvoryDesign.rose))
                Text(hero.label, color = onInk, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.4.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (hero.companionArtworkUrls.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    hero.companionArtworkUrls.take(3).forEach { url ->
                        StableRemoteArtwork(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, ink, CircleShape))
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        IvorySignalCurve(active = playing && hero.animationsEnabled, color = IvoryDesign.rose, guide = LevyraText.copy(alpha = 0.18f), modifier = Modifier.fillMaxWidth().padding(horizontal = inset).height(46.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f).padding(start = inset, end = 16.dp)) {
                Text(
                    hero.track.artist, color = LevyraText, fontSize = 50.sp, lineHeight = 52.sp, fontWeight = FontWeight.Light,
                    letterSpacing = (-1.2).sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(hero.ivorySecondLine(), color = LevyraMuted, style = MaterialTheme.typography.bodyLarge, letterSpacing = 0.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IvoryInkTab(start = false, Modifier.padding(bottom = 2.dp).width(inset + 82.dp).height(92.dp)) {
                Box(Modifier.padding(end = inset - 8.dp), contentAlignment = Alignment.Center) {
                    if (hero.isCurrent && hero.isResolving) LevyraLoadingIndicator(color = onInk, modifier = Modifier.size(28.dp))
                    else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = onInk, modifier = Modifier.size(38.dp))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
internal fun IvorySignalCurve(active: Boolean, color: Color, guide: Color, modifier: Modifier = Modifier) {
    val rose = color
    val phase = if (active) {
        val transition = rememberInfiniteTransition(label = "ivory-signal")
        val value by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart), label = "ivory-signal-phase")
        value
    } else 0.9f
    Canvas(modifier) {
        val mid = size.height * 0.58f
        drawLine(guide, Offset(0f, mid + size.height * 0.22f), Offset(size.width, mid + size.height * 0.22f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
        val path = Path()
        val steps = 64
        for (step in 0..steps) {
            val t = step / steps.toFloat()
            val envelope = sin(t * PI).toFloat()
            val wave = sin(t * 3.4f * PI.toFloat() + phase) * 0.7f + sin(t * 7.1f * PI.toFloat() - phase * 1.6f) * 0.3f
            val y = mid - wave * envelope * size.height * 0.42f
            if (step == 0) path.moveTo(0f, y) else path.lineTo(t * size.width, y)
        }
        drawPath(path, rose, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
internal fun IvoryAlbumCard(item: StudioShelfItem, isPlaying: Boolean) {
    Column(
        Modifier.width(156.dp).levyraPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(IvoryDesign.coverShape).background(LevyraPanel)) {
            item.artwork(Modifier.fillMaxSize(), false)
            IvoryCornerBadge(52.dp, Modifier.align(Alignment.BottomEnd)) { IvoryCornerGlyph(item.current, isPlaying, 52.dp) }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.title, color = if (item.current) IvoryDesign.rose else LevyraText, style = MaterialTheme.typography.titleSmall, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (item.subtitle.isNotBlank()) Text(item.subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun IvoryArtistCard(item: StudioShelfItem) {
    Column(
        Modifier.width(128.dp).levyraPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.size(124.dp).clip(CircleShape).border(2.dp, if (item.current) IvoryDesign.rose else IvoryDesign.ink, CircleShape).padding(5.dp).clip(CircleShape)) {
                item.artwork(Modifier.fillMaxSize(), true)
            }
            Box(Modifier.width(52.dp).height(14.dp).clip(IvoryNotchShape).background(IvoryDesign.ink))
        }
        Text(item.title, color = if (item.current) IvoryDesign.rose else LevyraText, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun IvoryShowcaseShelf(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(items, key = { _, item -> item.key }) { _, item ->
            if (item.round) IvoryArtistCard(item) else IvoryAlbumCard(item, isPlaying)
        }
    }
}

@Composable
internal fun IvoryTrackRow(
    title: String, subtitle: String, current: Boolean, playing: Boolean, leadInset: Dp,
    onClick: () -> Unit, onLongClick: (() -> Unit)?, modifier: Modifier = Modifier,
    artwork: @Composable (Modifier) -> Unit
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 70.dp)
            .levyraPressable(onClick = onClick, onLongClick = onLongClick, onClickLabel = title, role = Role.Button),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (current) {
            IvoryInkTab(start = true, Modifier.width(leadInset + 54.dp).height(70.dp)) {
                LevyraPlayingIndicator(playing = playing, color = IvoryDesign.onInk, size = 14.dp)
            }
        } else {
            Spacer(Modifier.width(leadInset))
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(IvoryDesign.tileCorner))) { artwork(Modifier.fillMaxSize()) }
        }
        Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = LevyraText, style = MaterialTheme.typography.titleSmall, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun IvoryCompactShelf(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    val pages = remember(items) { items.chunked(3) }
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(end = inset), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        itemsIndexed(pages, key = { index, page -> "$index:${page.first().key}" }) { index, page ->
            Column(Modifier.width(320.dp)) {
                page.forEach { item ->
                    IvoryTrackRow(item.title, item.subtitle, item.current, isPlaying, if (index == 0) inset else 12.dp, item.onClick, item.onLongClick) {
                        item.artwork(it, item.round)
                    }
                }
            }
        }
    }
}

@Composable
internal fun IvoryChartPage(page: List<IndexedValue<Track>>, currentId: String?, playing: Boolean, onPlay: (Track) -> Unit) {
    Column(Modifier.width(326.dp).clip(RoundedCornerShape(IvoryDesign.surfaceCorner)).background(LevyraActivePalette.ink).padding(vertical = 8.dp)) {
        page.forEach { (index, track) ->
            val current = track.id == currentId
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(IvoryDesign.tileCorner))
                    .background(if (current) IvoryDesign.ink else Color.Transparent)
                    .levyraPressable(onClick = { onPlay(track) }, onClickLabel = track.title, role = Role.Button)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val content = if (current) IvoryDesign.onInk else LevyraText
                Text(
                    (index + 1).toString(), color = if (current) IvoryDesign.onInk else if (index < 3) IvoryDesign.rose else LevyraMuted,
                    fontSize = 24.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center, modifier = Modifier.width(32.dp)
                )
                Box(Modifier.size(50.dp).clip(RoundedCornerShape(IvoryDesign.tileCorner))) { CoverImage(track, Modifier.fillMaxSize()) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(track.title, color = content, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = content.copy(alpha = 0.66f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (current) LevyraPlayingIndicator(playing = playing, color = IvoryDesign.onInk, size = 12.dp)
            }
        }
    }
}

@Composable
internal fun IvoryCollectionCard(collection: StudioCollection, tracks: List<Track>, onOpen: (String) -> Unit) {
    Column(
        Modifier.width(196.dp).levyraPressable(onClick = { onOpen(collection.id) }, onClickLabel = collection.title, role = Role.Button),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(IvoryDesign.coverShape).background(LevyraPanel).padding(6.dp)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tracks.take(4).chunked(2).forEach { row ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { track -> Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(IvoryDesign.tileCorner))) { CoverImage(track, Modifier.fillMaxSize()) } }
                    }
                }
            }
            IvoryCornerBadge(56.dp, Modifier.align(Alignment.BottomEnd).offset(6.dp, 6.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = IvoryDesign.onInk, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(collection.title, color = LevyraText, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(collection.subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun IvoryCategoryCard(title: String, artworkUrl: String, modifier: Modifier, onLongClick: (() -> Unit)?, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(IvoryDesign.surfaceCorner)).background(LevyraPanel)
            .levyraPressable(onClick = onClick, onLongClick = onLongClick, onClickLabel = title, role = Role.Button)
    ) {
        if (artworkUrl.isNotBlank()) AsyncImage(artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        IvoryInkTab(start = true, Modifier.align(Alignment.BottomStart).padding(bottom = 10.dp).fillMaxWidth(0.82f).height(48.dp)) {
            Text(
                title, color = IvoryDesign.onInk, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 18.dp)
            )
        }
    }
}

@Composable
internal fun RowScope.IvoryMoodTile(label: String, portrait: String, active: Boolean, onClick: () -> Unit, onMix: (() -> Unit)?, mixLabel: String, onPortraitError: () -> Unit) {
    Box(
        Modifier.weight(1f).aspectRatio(1f).clip(IvoryDesign.coverShape).background(LevyraPanel)
            .semantics { selected = active }
            .levyraPressable(onClick = onClick, onClickLabel = label, role = Role.Button, onLongClick = onMix, onLongClickLabel = if (onMix != null) mixLabel else null)
    ) {
        if (portrait.isNotBlank()) AsyncImage(portrait, contentDescription = null, contentScale = ContentScale.Crop, onError = { onPortraitError() }, modifier = Modifier.fillMaxSize())
        IvoryInkTab(start = true, Modifier.align(Alignment.TopStart).padding(top = 12.dp).fillMaxWidth(0.86f).height(42.dp)) {
            Text(label, color = IvoryDesign.onInk, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 14.dp))
        }
        if (active) IvoryCornerBadge(52.dp, Modifier.align(Alignment.BottomEnd)) {
            Icon(Icons.Rounded.Check, null, tint = IvoryDesign.rose, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun IvoryFreshRelease(track: Track, title: String, subtitle: String, label: String, enabled: Boolean, onOpen: () -> Unit, actions: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().levyraPressable(onClick = onOpen, enabled = enabled, onClickLabel = label, role = Role.Button)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(36.dp))) {
            CoverImage(track, Modifier.fillMaxSize(), highRes = true)
            Text(
                label, color = IvoryDesign.onInk, style = MaterialTheme.typography.labelLarge, maxLines = 1,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp).clip(CircleShape).background(IvoryDesign.ink).padding(horizontal = 14.dp, vertical = 7.dp)
            )
            IvoryCornerBadge(84.dp, Modifier.align(Alignment.BottomEnd)) {
                Icon(Icons.Rounded.PlayArrow, null, tint = IvoryDesign.onInk, modifier = Modifier.size(30.dp))
            }
        }
        Row(Modifier.padding(start = 6.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = LevyraText, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}

@Composable
internal fun IvoryOrbitShelf(
    tracks: List<Track>, currentId: String?, playing: Boolean, resolving: Boolean,
    onPlay: (Track) -> Unit, onActions: (Track) -> Unit
) {
    val animate = LocalAnimationsEnabled.current
    LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(tracks, key = { index, track -> "$index-${LevyraPersonalOrbit.identityKey(track)}" }) { _, track ->
            val current = track.id == currentId
            IvorySleeveCard(track, current, playing, current && playing && animate, current && resolving, onPlay, onActions)
        }
    }
}

@Composable
private fun IvorySleeveCard(
    track: Track, current: Boolean, playing: Boolean, spinning: Boolean, resolving: Boolean,
    onPlay: (Track) -> Unit, onActions: (Track) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val slide by androidx.compose.animation.core.animateDpAsState(if (current) 36.dp else 18.dp, label = "ivory-sleeve-record")
    Column(
        Modifier.width(186.dp).levyraPressable(
            onClick = { onPlay(track) }, onLongClick = { onActions(track) },
            onClickLabel = track.title, onLongClickLabel = strings.songOptions, role = Role.Button
        ),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            IvoryRecord(track, spinning, Modifier.align(Alignment.CenterStart).offset(x = slide).size(140.dp))
            Box(Modifier.align(Alignment.CenterStart).size(150.dp).clip(RoundedCornerShape(26.dp)).background(LevyraPanel)) {
                CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                if (current) Box(
                    Modifier.align(Alignment.BottomStart).padding(10.dp).size(32.dp).clip(CircleShape).background(IvoryDesign.ink),
                    contentAlignment = Alignment.Center
                ) {
                    if (resolving) LevyraLoadingIndicator(color = IvoryDesign.onInk, modifier = Modifier.size(16.dp))
                    else LevyraPlayingIndicator(playing = playing, color = IvoryDesign.onInk, size = 12.dp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(track.title, color = if (current) IvoryDesign.rose else LevyraText, style = MaterialTheme.typography.titleSmall, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(150.dp))
        Text(track.artist, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(150.dp))
    }
}

@Composable
private fun IvoryRecord(track: Track, spinning: Boolean, modifier: Modifier) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        while (true) {
            rotation.snapTo(rotation.value % 360f)
            rotation.animateTo(rotation.value + 360f, tween(12_000, easing = LinearEasing))
        }
    }
    val groove = Color(0xFF141016)
    Box(modifier.clip(CircleShape).background(groove), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            listOf(0.47f, 0.42f, 0.37f, 0.32f).forEach { drawCircle(Color.White.copy(alpha = 0.06f), size.minDimension * it, style = Stroke(1.dp.toPx())) }
        }
        Box(Modifier.fillMaxSize(0.42f).graphicsLayer { rotationZ = rotation.value }.clip(CircleShape)) {
            CoverImage(track, Modifier.fillMaxSize())
        }
        Box(Modifier.size(7.dp).clip(CircleShape).background(groove))
    }
}

@Composable
internal fun IvoryChartShelf(ranked: List<IndexedValue<Track>>, currentId: String?, playing: Boolean, inset: Dp, onPlay: (Track) -> Unit) {
    val pages = remember(ranked) { ranked.chunked(6) }
    val total = ranked.size
    LazyRow(contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        itemsIndexed(pages, key = { index, _ -> "chart-page-$index" }) { _, page -> IvoryChartBands(page, total, currentId, playing, onPlay) }
    }
}

@Composable
private fun IvoryChartBands(page: List<IndexedValue<Track>>, total: Int, currentId: String?, playing: Boolean, onPlay: (Track) -> Unit) {
    val ink = IvoryDesign.ink
    val onInk = IvoryDesign.onInk
    Column(Modifier.width(318.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        page.forEachIndexed { position, (index, track) ->
            val current = track.id == currentId
            val fraction = 1f - position * 0.07f - index / total.coerceAtLeast(1).toFloat() * 0.06f
            Row(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
                    .levyraPressable(onClick = { onPlay(track) }, onClickLabel = track.title, role = Role.Button),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    (index + 1).toString().padStart(2, '0'), color = if (index < 3) IvoryDesign.rose else LevyraMuted,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(34.dp)
                )
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(LevyraPanel))
                    Row(
                        Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(26.dp)).background(if (current) IvoryDesign.rose else ink)
                            .padding(start = 5.dp, end = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(42.dp).clip(CircleShape)) { CoverImage(track, Modifier.fillMaxSize()) }
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(track.title, color = onInk, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = onInk.copy(alpha = 0.62f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (current) LevyraPlayingIndicator(playing = playing, color = onInk, size = 12.dp)
                    }
                }
            }
        }
    }
}
