package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.news.MusicNewsStory
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.DailyTop100Copy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.exploreEditorialCopy
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.TorqueRiseShape
import com.luc4n3x.levyra.ui.theme.TorqueStripShape
import com.luc4n3x.levyra.viewmodel.DailyTop100ChartUiState

internal val TorqueGutter = 20.dp
internal val TorqueTop100Size = 200.dp

@Composable
internal fun TorqueSlantChip(label: String, selected: Boolean, role: Role = Role.Tab, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp).clip(TorqueTagShape)
            .background(if (selected) LevyraActivePalette.blue else LevyraPanel)
            .clickable(role = role, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label.uppercase(), color = if (selected) Color.White else LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, maxLines = 1)
    }
}

@Composable
internal fun TorqueShowAll(onClick: () -> Unit) {
    val label = LocalLevyraStrings.current.showAll
    Row(
        Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClickLabel = label, onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label.uppercase(), color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, maxLines = 1)
        Spacer(Modifier.width(6.dp))
        Box(Modifier.width(30.dp).height(22.dp).clip(TorqueTagShape).background(LevyraActivePalette.blue), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun TorqueKicker(source: String, age: String?, onDark: Boolean = false) {
    val rubric = source.trim().uppercase()
    if (rubric.isEmpty() && age == null) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (rubric.isNotEmpty()) Text(rubric, color = LevyraActivePalette.blue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, maxLines = 1)
        if (age != null) Text(
            (if (rubric.isNotEmpty()) " / " else "") + age.uppercase(), color = if (onDark) Color.White.copy(alpha = 0.8f) else LevyraMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp, maxLines = 1
        )
    }
}

@Composable
private fun TorqueStoryImage(story: MusicNewsStory, modifier: Modifier) {
    Box(modifier.background(LevyraPanelSoft).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Text(story.source.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "♪", color = LevyraActivePalette.blue, fontSize = 48.sp, fontWeight = FontWeight.Black)
        if (story.imageUrl.isNotBlank()) AsyncImage(story.imageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
internal fun TorqueNewsFront(stories: List<MusicNewsStory>, onOpen: (MusicNewsStory) -> Unit) {
    val front = remember(stories) { exploreNewsFront(stories) } ?: return
    val code = LocalLevyraStrings.current.code
    val copy = remember(code) { exploreEditorialCopy(code) }
    val nowMs = remember(stories) { System.currentTimeMillis() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkinSectionTitle(copy.newsTitle, TorqueGutter)
        val lead = front.lead
        Box(
            Modifier.padding(horizontal = TorqueGutter).fillMaxWidth().aspectRatio(0.92f).background(LevyraPanel)
                .levyraPressable(onClick = { onOpen(lead) }, onClickLabel = copy.readStory, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                .semantics(mergeDescendants = true) { }
        ) {
            TorqueStoryImage(lead, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 0.75f to Color.Black.copy(alpha = 0.7f), 1f to Color.Black.copy(alpha = 0.92f))))
            Text(
                "01", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).background(Color.Black).padding(horizontal = 10.dp, vertical = 6.dp).clearAndSetSemantics { }
            )
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(36.dp).height(4.dp).background(LevyraActivePalette.blue))
                TorqueKicker(lead.source, exploreNewsAgeLabel(copy, nowMs, lead.publishedAtMs), onDark = true)
                Text(lead.title, color = Color.White, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
        if (front.secondary.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = TorqueGutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                front.secondary.forEachIndexed { index, story ->
                    key(story.url) {
                        Column(
                            Modifier.weight(1f).background(LevyraPanel)
                                .levyraPressable(onClick = { onOpen(story) }, onClickLabel = copy.readStory, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                                .semantics(mergeDescendants = true) { }
                        ) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1.25f)) {
                                TorqueStoryImage(story, Modifier.fillMaxSize())
                                Text(
                                    (index + 2).toString().padStart(2, '0'), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.TopStart).background(Color.Black).padding(horizontal = 8.dp, vertical = 4.dp).clearAndSetSemantics { }
                                )
                            }
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                TorqueKicker(story.source, null)
                                Text(story.title, color = LevyraText, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                exploreNewsAgeLabel(copy, nowMs, story.publishedAtMs)?.let { Text(it.uppercase(), color = LevyraMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp) }
                            }
                        }
                    }
                }
                if (front.secondary.size < ExploreNewsSecondaryCount) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun TorqueNewsDesk(stories: List<MusicNewsStory>, onOpen: (MusicNewsStory) -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { exploreEditorialCopy(strings.code) }
    val sources = remember(stories) { exploreNewsSources(stories) }
    var selectedSource by rememberSaveable { mutableStateOf("") }
    val source = selectedSource.takeIf { it in sources }
    val desk = remember(stories, source) { exploreNewsDesk(stories, source) }
    if (desk.isEmpty()) return
    val nowMs = remember(stories) { System.currentTimeMillis() }
    Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SkinSectionTitle(copy.newsMore, TorqueGutter)
        if (sources.size > 1) {
            LazyRow(contentPadding = PaddingValues(horizontal = TorqueGutter), horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                item(key = "torque-news-all") { TorqueSlantChip(strings.all, source == null) { selectedSource = "" } }
                items(sources, key = { "torque-news-$it" }) { name -> TorqueSlantChip(name, name == source) { selectedSource = name } }
            }
        }
        desk.forEachIndexed { index, story ->
            key(story.url) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = TorqueGutter).heightIn(min = 96.dp).background(LevyraPanel)
                        .levyraPressable(onClick = { onOpen(story) }, onClickLabel = copy.readStory, role = Role.Button, pressedScale = LevyraPressScale.Row)
                        .semantics(mergeDescendants = true) { },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        (index + 1).toString().padStart(2, '0'), color = LevyraMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                        modifier = Modifier.width(44.dp).padding(start = 14.dp).clearAndSetSemantics { }
                    )
                    Column(Modifier.weight(1f).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TorqueKicker(story.source, exploreNewsAgeLabel(copy, nowMs, story.publishedAtMs))
                        Text(story.title, color = LevyraText, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(12.dp))
                    TorqueStoryImage(story, Modifier.size(96.dp))
                }
            }
        }
    }
}

@Composable
internal fun TorqueReleaseShelf(
    releases: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onOpenAlbum: (Track) -> Unit,
    actions: @Composable (Track) -> Unit
) {
    if (releases.isEmpty()) return
    LazyRow(contentPadding = PaddingValues(horizontal = TorqueGutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(releases, key = { _, track -> "torque-release-${track.id}" }, contentType = { _, _ -> "torque-release" }) { index, track ->
            val current = track.id == currentTrackId
            TorqueReleaseCard(index, track, current, isPlaying && current, isResolving && current, { onPlay(track) }, { onOpenAlbum(track) }) { actions(track) }
        }
    }
}

@Composable
private fun TorqueReleaseCard(
    index: Int,
    track: Track,
    current: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    onOpenAlbum: () -> Unit,
    actions: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val card = remember(track) { exploreReleaseCard(track) }
    val playing = isPlaying && !card.opensAlbum
    val eyebrow = exploreReleaseEyebrow(card.kind, strings).uppercase()
    Column(Modifier.width(176.dp).background(LevyraPanel)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .levyraPressable(
                    onClick = if (card.opensAlbum) onOpenAlbum else onPlay,
                    enabled = !(isResolving && !card.opensAlbum),
                    onClickLabel = when { card.opensAlbum -> strings.openAlbum; playing -> strings.pause; else -> strings.play },
                    role = Role.Button, pressedScale = LevyraPressScale.Tile
                )
                .semantics { contentDescription = "$eyebrow, ${card.title}, ${card.subtitle}" }
        ) {
            CoverImage(track = exploreCoverArtworkTrack(track), modifier = Modifier.fillMaxSize())
            val live = current && !card.opensAlbum
            if (live) Box(Modifier.fillMaxSize().clip(TorqueStripShape).background(LevyraActivePalette.blue))
            Box(Modifier.align(Alignment.TopStart).padding(8.dp)) { TorqueRank(index + 1, index == 0) }
            if (live) Box(Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                if (isResolving) LevyraLoadingIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                else LevyraPlayingIndicator(playing = playing, color = Color.White, size = 14.dp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(eyebrow, color = LevyraActivePalette.blue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.title.uppercase(), color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.subtitle, color = LevyraMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}

@Composable
internal fun TorqueTop100Card(label: String, date: String, global: Boolean, modifier: Modifier) {
    val band = if (global) Color.White else LevyraActivePalette.blue
    val onBand = if (global) Color.Black else Color.White
    Box(modifier.background(LevyraPanel)) {
        Text(
            "100", color = LevyraPanelSoft, fontSize = 120.sp, lineHeight = 120.sp, fontWeight = FontWeight.Black, letterSpacing = (-6).sp,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-14).dp).clearAndSetSemantics { }
        )
        Text(
            date, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.TopStart).background(Color.Black).padding(horizontal = 10.dp, vertical = 6.dp)
        )
        Box(Modifier.fillMaxSize().clip(TorqueStripShape).background(band))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp)) {
            Text(label.uppercase(), color = onBand, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun TorqueTop100ChartScreen(
    open: DailyTop100ChartUiState,
    label: String,
    date: String,
    copy: DailyTop100Copy,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    BackHandler(onBack = onBack)
    val strings = LocalLevyraStrings.current
    val tracks = open.tracks
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(Modifier.fillMaxSize().background(LevyraBlack), contentPadding = PaddingValues(bottom = bottom + 24.dp)) {
        item(key = "torque-top100-hero") {
            Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp).size(48.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = LevyraText) }
                TorqueTop100Card(label, date, open.chart.global, Modifier.padding(horizontal = TorqueGutter).fillMaxWidth().aspectRatio(1.3f))
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = TorqueGutter, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { SkinSectionTitle(copy.title, 16.dp) }
                    if (tracks.isNotEmpty()) {
                        IconButton(onClick = { tracks.shuffled().let { onPlay(it, it.first()) } }, modifier = Modifier.size(52.dp)) {
                            Icon(Icons.Rounded.Shuffle, strings.shuffle, tint = LevyraText)
                        }
                        Box(
                            Modifier.size(60.dp).clip(CircleShape).background(Color.Black).border(4.dp, Color.White, CircleShape)
                                .clickable(role = Role.Button, onClickLabel = strings.play) { onPlay(tracks, tracks.first()) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                    }
                }
            }
        }
        when {
            tracks.isNotEmpty() -> itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                TorqueChartRow(index, track, strings.options, { onPlay(tracks, track) }, { onTrackActions(track) })
            }
            open.loading -> item(key = "torque-top100-loading") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { LevyraLoadingIndicator(color = LevyraActivePalette.blue, modifier = Modifier.size(36.dp)) }
            }
            else -> item(key = "torque-top100-failed") {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(copy.failed, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onRetry) { Text(copy.retry, color = LevyraActivePalette.blue) }
                }
            }
        }
    }
}

@Composable
internal fun TorqueChartRow(index: Int, track: Track, optionsLabel: String, onPlay: () -> Unit, onActions: () -> Unit, height: Dp = 76.dp) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = TorqueGutter, vertical = 2.dp).heightIn(min = height).background(LevyraPanel)
            .levyraPressable(onClick = onPlay, onLongClick = onActions, onClickLabel = track.title, onLongClickLabel = optionsLabel, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .padding(start = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TorqueRank(index + 1, index < 3)
        Box(Modifier.size(56.dp)) { CoverImage(track, Modifier.fillMaxSize()) }
        Column(Modifier.weight(1f)) {
            Text(track.title, color = LevyraText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onActions) { Icon(Icons.Rounded.MoreVert, optionsLabel, tint = LevyraMuted) }
    }
}
