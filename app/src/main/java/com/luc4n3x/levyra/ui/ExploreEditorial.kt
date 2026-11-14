package com.luc4n3x.levyra.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.news.MusicNewsStory
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraFilterPill
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.LevyraSectionShowAll
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.ExploreEditorialCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.exploreEditorialCopy
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val ExploreCoverAspect = 3f / 2f
private const val ExploreSecondaryAspect = 4f / 3f
private const val ExploreReleaseGridMinimum = 6
private val ExploreEditorialSectionLead = 14.dp
private val ExploreEditorialMaxWidth = 680.dp
private val ExploreDeskThumb = 84.dp
private val ExploreReleaseColumnGap = 20.dp
private val ExploreReleaseRowGap = 18.dp
private val ExploreReleaseMenuInset = 12.dp

@Immutable
internal class ExploreEditorialContent(
    val news: List<MusicNewsStory>,
    val onOpenNews: (MusicNewsStory) -> Unit,
    val onPlayChart: (List<Track>, Track) -> Unit,
    val onTrackMenu: (Track) -> Unit
)

@Composable
internal fun ExploreEditorialRow(row: ExploreRow, content: ExploreEditorialContent) {
    when (row) {
        ExploreRow.NewsHero -> when {
            com.luc4n3x.levyra.ui.theme.LevyraIsTorque -> TorqueNewsFront(content.news, content.onOpenNews)
            com.luc4n3x.levyra.ui.theme.LevyraIsIvory -> IvoryNewsFront(content.news, content.onOpenNews)
            LevyraIsPrismOnly -> PrismNewsFront(content.news, content.onOpenNews)
            else -> ExploreNewsFrontPage(content.news, content.onOpenNews)
        }
        ExploreRow.NewsList -> when {
            com.luc4n3x.levyra.ui.theme.LevyraIsTorque -> TorqueNewsDesk(content.news, content.onOpenNews)
            LevyraIsPrismOnly -> PrismNewsDesk(content.news, content.onOpenNews)
            else -> ExploreNewsDesk(content.news, content.onOpenNews)
        }
        ExploreRow.DailyTop100 -> ExploreDailyTop100(
            onPlay = content.onPlayChart,
            onTrackActions = content.onTrackMenu,
            modifier = Modifier.padding(top = ExploreEditorialSectionLead)
        )
        ExploreRow.CityCharts -> ExploreCityCharts(
            onPlay = content.onPlayChart,
            onTrackActions = content.onTrackMenu,
            modifier = Modifier.padding(top = ExploreEditorialSectionLead)
        )
        else -> Unit
    }
}

@Composable
internal fun rememberExploreEditionDate(): String {
    val code = LocalLevyraStrings.current.code
    return remember(code) {
        val locale = Locale.forLanguageTag(code)
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale))
            .replaceFirstChar { char -> char.titlecase(locale) }
    }
}

@Composable
internal fun ExploreEditorialHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    LevyraSectionHeader(
        title = title,
        modifier = Modifier.padding(
            start = LevyraHomeDesign.HorizontalInset,
            end = LevyraHomeDesign.HorizontalInset,
            top = ExploreEditorialSectionLead
        ),
        actions = { onSeeAll?.let { action -> LevyraSectionShowAll(onClick = action) } }
    )
}

@Composable
internal fun ExploreEditorialScopeChips(
    scopes: List<ExploreFreshScope>,
    selected: ExploreFreshScope,
    localLabel: String,
    onSelect: (ExploreFreshScope) -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = LevyraHomeDesign.HorizontalInset),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        scopes.forEach { scope ->
            LevyraFilterPill(
                label = if (scope == ExploreFreshScope.Local) localLabel else strings.freshScopeWorld,
                selected = scope == selected,
                onClick = { onSelect(scope) }
            )
        }
    }
}

@Composable
private fun rememberEditorialCopy(): ExploreEditorialCopy {
    val code = LocalLevyraStrings.current.code
    return remember(code) { exploreEditorialCopy(code) }
}

@Composable
private fun rememberEditorialLocale(): Locale {
    val code = LocalLevyraStrings.current.code
    return remember(code) { Locale.forLanguageTag(code) }
}

internal fun exploreNewsAgeLabel(copy: ExploreEditorialCopy, nowMs: Long, publishedAtMs: Long): String? =
    exploreNewsAge(nowMs, publishedAtMs)?.let { (unit, value) ->
        when (unit) {
            ExploreNewsAgeUnit.Minutes -> if (value == 0) copy.justNow else copy.minutesAgo.replace("{count}", value.toString())
            ExploreNewsAgeUnit.Hours -> copy.hoursAgo.replace("{count}", value.toString())
            ExploreNewsAgeUnit.Days -> copy.daysAgo.replace("{count}", value.toString())
        }
    }

private fun exploreNewsGlyph(source: String): String =
    source.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "♪"

@Composable
private fun ExploreNewsFrontPage(stories: List<MusicNewsStory>, onOpen: (MusicNewsStory) -> Unit) {
    val front = remember(stories) { exploreNewsFront(stories) } ?: return
    val copy = rememberEditorialCopy()
    val nowMs = remember(stories) { System.currentTimeMillis() }
    Column(
        modifier = Modifier
            .widthIn(max = ExploreEditorialMaxWidth)
            .fillMaxWidth()
            .padding(horizontal = LevyraHomeDesign.HorizontalInset),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LevyraSectionHeader(title = copy.newsTitle)
        ExploreCoverStory(
            story = front.lead,
            age = exploreNewsAgeLabel(copy, nowMs, front.lead.publishedAtMs),
            actionLabel = copy.readStory,
            onClick = { onOpen(front.lead) }
        )
        if (front.secondary.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                front.secondary.forEach { story ->
                    key(story.url) {
                        ExploreSecondaryStory(
                            story = story,
                            age = exploreNewsAgeLabel(copy, nowMs, story.publishedAtMs),
                            actionLabel = copy.readStory,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpen(story) }
                        )
                    }
                }
                if (front.secondary.size < ExploreNewsSecondaryCount) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExploreCoverStory(story: MusicNewsStory, age: String?, actionLabel: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .levyraPressable(onClick = onClick, onClickLabel = actionLabel, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ExploreStoryArtwork(
            story = story,
            shape = LevyraCardDesign.EditorialShape,
            glyphStyle = MaterialTheme.typography.displayLarge,
            modifier = Modifier.fillMaxWidth().aspectRatio(ExploreCoverAspect)
        )
        Spacer(Modifier.height(6.dp))
        ExploreStoryKicker(story.source, age)
        Text(
            text = story.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExploreSecondaryStory(
    story: MusicNewsStory,
    age: String?,
    actionLabel: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .levyraPressable(onClick = onClick, onClickLabel = actionLabel, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(LevyraCardDesign.CaptionLineGap)
    ) {
        ExploreStoryArtwork(
            story = story,
            shape = LevyraCardDesign.ArtworkShape,
            glyphStyle = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.fillMaxWidth().aspectRatio(ExploreSecondaryAspect)
        )
        Spacer(Modifier.height(LevyraCardDesign.CaptionTopGap - LevyraCardDesign.CaptionLineGap))
        ExploreStoryKicker(story.source, age = null)
        Text(
            text = story.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        age?.let { label ->
            Text(
                text = label,
                style = LevyraType.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExploreStoryKicker(source: String, age: String?) {
    val colors = MaterialTheme.colorScheme
    val locale = rememberEditorialLocale()
    val rubric = source.trim().uppercase(locale)
    if (rubric.isEmpty() && age == null) return
    val text = remember(rubric, age, colors.primary, colors.onSurfaceVariant) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold)) { append(rubric) }
            if (age != null) {
                withStyle(SpanStyle(color = colors.onSurfaceVariant)) {
                    if (rubric.isNotEmpty()) append("  ·  ")
                    append(age)
                }
            }
        }
    }
    Text(text = text, style = LevyraType.overline, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun ExploreStoryArtwork(story: MusicNewsStory, shape: Shape, glyphStyle: TextStyle, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = exploreNewsGlyph(story.source),
            style = glyphStyle.copy(fontWeight = FontWeight.Black),
            color = colors.primary.copy(alpha = 0.55f)
        )
        if (story.imageUrl.isNotBlank()) ExploreNewsImage(story.imageUrl, Modifier.fillMaxSize())
    }
}

@Composable
private fun ExploreNewsImage(url: String, modifier: Modifier) {
    val context = LocalContext.current
    val crossfadeMs = if (LocalAnimationsEnabled.current) 220 else 0
    val request = remember(context, url, crossfadeMs) {
        ImageRequest.Builder(context).data(url).crossfade(crossfadeMs).build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
private fun ExploreNewsDesk(stories: List<MusicNewsStory>, onOpen: (MusicNewsStory) -> Unit) {
    val copy = rememberEditorialCopy()
    val strings = LocalLevyraStrings.current
    val sources = remember(stories) { exploreNewsSources(stories) }
    var selectedSource by rememberSaveable { mutableStateOf("") }
    val source = selectedSource.takeIf { it in sources }
    val desk = remember(stories, source) { exploreNewsDesk(stories, source) }
    if (desk.isEmpty()) return
    val nowMs = remember(stories) { System.currentTimeMillis() }
    val divider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Column(modifier = Modifier.widthIn(max = ExploreEditorialMaxWidth).fillMaxWidth()) {
        ExploreEditorialHeader(title = copy.newsMore)
        if (sources.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = LevyraHomeDesign.HorizontalInset),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "explore-news-source-all") {
                    LevyraFilterPill(label = strings.all, selected = source == null, onClick = { selectedSource = "" })
                }
                items(sources, key = { name -> "explore-news-source-$name" }) { name ->
                    LevyraFilterPill(label = name, selected = name == source, onClick = { selectedSource = name })
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        desk.forEachIndexed { index, story ->
            key(story.url) {
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = LevyraHomeDesign.HorizontalInset),
                        thickness = 1.dp,
                        color = divider
                    )
                }
                ExploreDeskRow(
                    story = story,
                    age = exploreNewsAgeLabel(copy, nowMs, story.publishedAtMs),
                    actionLabel = copy.readStory,
                    onClick = { onOpen(story) }
                )
            }
        }
    }
}

@Composable
private fun ExploreDeskRow(story: MusicNewsStory, age: String?, actionLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ExploreDeskThumb + 28.dp)
            .levyraPressable(onClick = onClick, onClickLabel = actionLabel, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = LevyraHomeDesign.HorizontalInset, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ExploreStoryKicker(story.source, age)
            Text(
                text = story.title,
                style = LevyraType.cardTitle.copy(fontSize = LevyraCardDesign.RowTitleSize),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        ExploreStoryArtwork(
            story = story,
            shape = LevyraCardDesign.ThumbShape,
            glyphStyle = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.size(ExploreDeskThumb)
        )
    }
}

@Composable
internal fun ExploreReleaseShelf(
    releases: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onOpenAlbum: (Track) -> Unit,
    actions: @Composable (Track) -> Unit
) {
    if (releases.isEmpty()) return
    val columns = remember(releases) {
        if (releases.size >= ExploreReleaseGridMinimum) releases.chunked(2) else releases.map(::listOf)
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = LevyraHomeDesign.HorizontalInset),
        horizontalArrangement = Arrangement.spacedBy(ExploreReleaseColumnGap)
    ) {
        items(columns, key = { column -> "explore-release-${column.first().id}" }, contentType = { "explore-release-column" }) { column ->
            Column(
                modifier = Modifier.width(LevyraHomeDesign.ArtworkCardWidth),
                verticalArrangement = Arrangement.spacedBy(ExploreReleaseRowGap)
            ) {
                column.forEach { track ->
                    key(track.id) {
                        val isCurrent = track.id == currentTrackId
                        ExploreReleaseCard(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            isResolving = isResolving && isCurrent,
                            onPlay = { onPlay(track) },
                            onOpenAlbum = { onOpenAlbum(track) },
                            actions = { actions(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreReleaseCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    onOpenAlbum: () -> Unit,
    actions: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    val locale = rememberEditorialLocale()
    val card = remember(track) { exploreReleaseCard(track) }
    val playing = isPlaying && !card.opensAlbum
    val actionLabel = when {
        card.opensAlbum -> strings.openAlbum
        playing -> strings.pause
        else -> strings.play
    }
    val eyebrow = exploreReleaseEyebrow(card.kind, strings).uppercase(locale)
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(LevyraCardDesign.ArtworkShape)
                .background(colors.surfaceContainerHigh)
                .levyraPressable(
                    onClick = if (card.opensAlbum) onOpenAlbum else onPlay,
                    enabled = !(isResolving && !card.opensAlbum),
                    onClickLabel = actionLabel,
                    role = Role.Button,
                    pressedScale = LevyraPressScale.Tile
                )
                .semantics { contentDescription = "$eyebrow, ${card.title}, ${card.subtitle}" }
        ) {
            CoverImage(track = exploreCoverArtworkTrack(track), modifier = Modifier.fillMaxSize())
            if (isCurrent && !card.opensAlbum) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (isResolving) {
                        DiscoveryLoadingIndicator(Modifier.size(18.dp), colors.onPrimaryContainer)
                    } else {
                        LevyraPlayingIndicator(playing = playing, color = colors.onPrimaryContainer, size = 14.dp)
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier.weight(1f).padding(top = 6.dp).clearAndSetSemantics { },
                verticalArrangement = Arrangement.spacedBy(LevyraCardDesign.CaptionLineGap)
            ) {
                Text(eyebrow, style = LevyraType.overline, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.title, style = LevyraType.cardTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.subtitle, style = LevyraType.caption, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.offset(x = ExploreReleaseMenuInset)) { actions() }
        }
    }
}
