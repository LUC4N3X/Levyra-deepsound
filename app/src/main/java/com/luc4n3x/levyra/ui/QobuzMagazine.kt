package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.qobuz.QobuzStore
import com.luc4n3x.levyra.data.qobuz.QobuzStory
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraQobuzSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.QobuzSkinCopy
import com.luc4n3x.levyra.ui.theme.QobuzDesign
import com.luc4n3x.levyra.viewmodel.QobuzMagazineViewModel

internal class QobuzMagazineInputs(
    val chartCountry: String,
    val categories: List<ExploreCategory>,
    val freshTracks: List<Track>,
    val currentId: String?,
    val onOpenSearch: () -> Unit,
    val onOpenCategory: (ExploreCategory) -> Unit,
    val onOpenNewReleases: () -> Unit,
    val onOpenSamples: () -> Unit,
    val onPlay: (List<Track>, Track) -> Unit,
    val onTrackActions: (Track) -> Unit
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun QobuzMagazineScreen(inputs: QobuzMagazineInputs, listState: LazyListState, bottomInset: Dp) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraQobuzSkinStrings.forCode(strings.code) }
    val magazine = viewModel<QobuzMagazineViewModel>()
    val state by magazine.state.collectAsStateWithLifecycle()
    val locale = remember(strings.code, inputs.chartCountry) { QobuzStore.locale(strings.code, inputs.chartCountry) }
    LaunchedEffect(locale) { magazine.load(locale) }
    val uriHandler = LocalUriHandler.current
    val openStory: (QobuzStory) -> Unit = { story -> runCatching { uriHandler.openUri(story.url) } }
    val page = state.page
    Column(Modifier.fillMaxSize().background(QobuzDesign.surfacePrimary).statusBarsPadding()) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = bottomInset + 24.dp)) {
            item(key = "mag-title") {
                QobuzPageHeader(copy.magazine) { QobuzIconAction(Icons.Rounded.Search, strings.search, inputs.onOpenSearch) }
            }
            stickyHeader(key = "mag-tabs") {
                Column(Modifier.fillMaxWidth().background(QobuzDesign.surfacePrimary)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = QobuzDesign.gutter, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(QobuzDesign.chipGap)
                    ) {
                        item(key = "home") { QobuzChip(copy.home, state.section.isBlank(), { magazine.selectSection("") }, QobuzHomeIcon) }
                        items(state.rubrics, key = { it.slug }) { rubric ->
                            QobuzChip(rubric.label, state.section == rubric.slug, { magazine.selectSection(rubric.slug) })
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(QobuzDesign.separator))
                }
            }
            val featured = page?.featured.orEmpty()
            if (featured.isNotEmpty()) {
                item(key = "mag-carousel") {
                    Column(Modifier.padding(top = 16.dp)) { QobuzStoryCarousel(featured, copy, openStory) }
                }
            }
            val stories = page?.stories.orEmpty()
            if (stories.isNotEmpty()) {
                item(key = "mag-picked") {
                    Text(copy.pickedForYou, style = QobuzText.sectionTitle,
                        modifier = Modifier.padding(start = QobuzDesign.gutter, end = QobuzDesign.gutter, top = 32.dp, bottom = 8.dp))
                }
                items(stories, key = { "story-" + it.url }) { story ->
                    QobuzStoryCard(story, copy, Modifier.padding(horizontal = QobuzDesign.gutter, vertical = 6.dp)) { openStory(story) }
                }
            } else if (state.loading && featured.isEmpty()) {
                item(key = "mag-loading") {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = QobuzDesign.textAccent)
                    }
                }
            } else if (state.failed) {
                item(key = "mag-failed") {
                    Text(copy.magazineUnavailable, style = QobuzText.baseline, modifier = Modifier.padding(QobuzDesign.gutter))
                }
            }
            if (state.section.isBlank()) {
                item(key = "levyra-explore") {
                    QobuzLevyraRubrics(inputs, copy)
                }
            }
        }
    }
}

@Composable
private fun QobuzStoryCarousel(stories: List<QobuzStory>, copy: QobuzSkinCopy, onOpen: (QobuzStory) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = (maxWidth - QobuzDesign.gutter * 2 - 24.dp).coerceAtMost(440.dp)
        LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(stories, key = { it.url }) { story ->
                QobuzStoryGradientCard(story, copy, width) { onOpen(story) }
            }
        }
    }
}

@Composable
private fun QobuzStoryGradientCard(story: QobuzStory, copy: QobuzSkinCopy, width: Dp, onClick: () -> Unit) {
    val base = Color(0xFF1E1E1E)
    Column(
        Modifier.width(width).clip(RoundedCornerShape(QobuzDesign.cardCorner)).background(base)
            .clickable(role = Role.Button, onClickLabel = copy.openOnQobuz, onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            if (story.imageUrl.isNotBlank()) {
                coil3.compose.AsyncImage(story.imageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to base)))
        }
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (story.rubric.isNotBlank()) {
                Text(story.rubric, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                Spacer(Modifier.height(8.dp))
            }
            Text(story.title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
                color = Color.White, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (story.author.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(story.author, style = MaterialTheme.typography.bodyLarge, color = Color.White)
            }
        }
    }
}

@Composable
private fun QobuzStoryCard(story: QobuzStory, copy: QobuzSkinCopy, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = copy.openOnQobuz, onClick = onClick).padding(vertical = 10.dp)) {
        QobuzArtworkBox(story.imageUrl, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Spacer(Modifier.height(12.dp))
        if (story.rubric.isNotBlank()) Text(story.rubric, style = QobuzText.category.copy(fontSize = 12.sp))
        Text(story.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium), color = QobuzDesign.textPrimary,
            maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (story.author.isNotBlank()) Text(story.author, style = QobuzText.baseline)
    }
}

@Composable
private fun QobuzLevyraRubrics(inputs: QobuzMagazineInputs, copy: QobuzSkinCopy) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(QobuzDesign.sectionGap)) {
        val shortcuts = listOf(
            strings.exploreNewReleases to inputs.onOpenNewReleases,
            strings.exploreSamples to inputs.onOpenSamples
        )
        Column {
            QobuzSectionHeader(copy.levyraExplore, null, copy.seeAll, null)
            Spacer(Modifier.height(QobuzDesign.headerToContent))
            LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shortcuts, key = { it.first }) { (label, action) ->
                    QobuzRubricCard(label, qobuzTagPalette(label), action)
                }
            }
        }
        if (inputs.categories.isNotEmpty()) {
            Column {
                QobuzSectionHeader(strings.exploreMoods, null, copy.seeAll, null)
                Spacer(Modifier.height(QobuzDesign.headerToContent))
                LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(inputs.categories.distinctBy { it.title }, key = { it.title + it.params }) { category ->
                        QobuzRubricCard(category.title, qobuzTagPalette(category.title)) { inputs.onOpenCategory(category) }
                    }
                }
            }
        }
        if (inputs.freshTracks.isNotEmpty()) {
            Column {
                QobuzSectionHeader(strings.exploreFresh, null, copy.seeAll, null)
                Spacer(Modifier.height(QobuzDesign.headerToContent))
                LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
                    items(inputs.freshTracks.take(20), key = { it.id }) { track ->
                        QobuzTrackCard(track, QobuzDesign.gridCard, { inputs.onPlay(inputs.freshTracks, track) }, { inputs.onTrackActions(track) })
                    }
                }
            }
        }
    }
}

@Composable
private fun QobuzRubricCard(title: String, colors: Pair<Color, Color>, onClick: () -> Unit) {
    Box(
        Modifier.width(216.dp).aspectRatio(16f / 10f).clip(RoundedCornerShape(4.dp))
            .background(Brush.linearGradient(listOf(colors.first, colors.second)))
            .clickable(role = Role.Button, onClickLabel = title, onClick = onClick)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Normal), color = Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
    }
}

@Composable
internal fun QobuzMagazineEntry(
    state: com.luc4n3x.levyra.viewmodel.LevyraUiState,
    viewModel: com.luc4n3x.levyra.viewmodel.ExploreViewModel,
    freshTracks: List<Track>,
    listState: LazyListState,
    bottomInset: Dp,
    onShortcut: (ExploreShortcut) -> Unit,
    onTrackActions: (Track) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit
) {
    QobuzMagazineScreen(
        QobuzMagazineInputs(
            chartCountry = state.selectedChartId,
            categories = state.exploreCategories,
            freshTracks = freshTracks,
            currentId = state.currentTrack?.id,
            onOpenSearch = viewModel::openSearch,
            onOpenCategory = onOpenCategory,
            onOpenNewReleases = { onShortcut(ExploreShortcut.NewReleases) },
            onOpenSamples = { onShortcut(ExploreShortcut.Samples) },
            onPlay = { list, track -> viewModel.playFrom(list, track) },
            onTrackActions = onTrackActions
        ),
        listState, bottomInset
    )
}
