@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.luc4n3x.levyra.ui.rewind

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.domain.RewindChapter
import com.luc4n3x.levyra.domain.chapters
import com.luc4n3x.levyra.ui.experiences.LocalMusicExperiences
import com.luc4n3x.levyra.ui.experiences.MusicExperienceStyle
import com.luc4n3x.levyra.ui.experiences.musicExperienceStyle
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.MusicExperienceText
import com.luc4n3x.levyra.ui.i18n.musicExperience
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import com.luc4n3x.levyra.ui.theme.LevyraText
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
internal fun RewindEntry(modifier: Modifier = Modifier, tint: androidx.compose.ui.graphics.Color = LevyraText) {
    val navigation = LocalMusicExperiences.current
    IconButton(onClick = navigation.rewind, modifier = modifier.testTag("open-rewind")) {
        Icon(Icons.Rounded.Replay, "Levyra Rewind", tint = tint)
    }
}

@Composable
internal fun RewindScreen(onClose: () -> Unit, viewModel: RewindViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.load(state.year.takeIf { state.summary != null }) }
    RewindPresentation(state, onClose, viewModel::load, onRetry = { viewModel.load(state.year) })
}

@Composable
internal fun RewindPresentation(state: RewindState, onClose: () -> Unit, onYear: (Int) -> Unit, onRetry: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val summary = state.summary
    val style = musicExperienceStyle(summary?.snapshot?.topTracks?.firstOrNull()?.artworkUrl.orEmpty())
    var yearsVisible by rememberSaveable { mutableStateOf(false) }
    var chaptersVisible by rememberSaveable { mutableStateOf(false) }
    val chapters = remember(summary) { summary?.takeIf { it.snapshot.hasSignal }?.chapters().orEmpty() }
    val pager = rememberPagerState { chapters.size }
    val scope = rememberCoroutineScope()
    val animated = LocalAnimationsEnabled.current && LocalLevyraVisualCapabilities.current.depthTransitions
    val enter = remember { Animatable(if (animated) 0f else 1f) }
    val motion = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(animated) { enter.animateTo(1f, if (animated) motion else snap()) }
    LaunchedEffect(state.year) { pager.requestScrollToPage(0) }
    BackHandler {
        if (chaptersVisible) chaptersVisible = false
        else if (pager.currentPage > 0) scope.launch { pager.scrollToPage(pager.currentPage - 1) }
        else onClose()
    }

    Surface(color = style.background, contentColor = style.text, modifier = Modifier.fillMaxSize().testTag("rewind-screen")) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("LEVYRA / REWIND", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f).padding(start = 12.dp))
                Box {
                    TextButton(onClick = { yearsVisible = true }, enabled = state.years.isNotEmpty()) {
                        Text(state.year.toString(), color = style.text)
                        Icon(Icons.Rounded.ExpandMore, null, tint = style.text)
                    }
                    DropdownMenu(expanded = yearsVisible, onDismissRequest = { yearsVisible = false }) {
                        state.years.forEach { year ->
                            DropdownMenuItem(text = { Text(year.toString()) }, onClick = { yearsVisible = false; onYear(year) })
                        }
                    }
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, strings.close) }
            }
            if (summary == null || !summary.snapshot.hasSignal) {
                RewindPlaceholder(state, style, strings, onRetry, Modifier.weight(1f))
            } else {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    key = { chapters[it] }
                ) { index ->
                    val offset = (pager.currentPage - index + pager.currentPageOffsetFraction).absoluteValue
                    Box(Modifier.fillMaxSize().graphicsLayer {
                        if (animated) {
                            val scale = 1f - 0.06f * offset.coerceIn(0f, 1f)
                            scaleX = scale * (0.94f + enter.value * 0.06f)
                            scaleY = scaleX
                            alpha = (1f - offset.coerceIn(0f, 1f) * 0.35f) * enter.value
                        }
                    }, contentAlignment = Alignment.TopCenter) {
                        RewindChapterContent(chapters[index], summary, style, active = pager.settledPage == index)
                    }
                }
                RewindControls(pager, chapters.size, animated, style, strings, onChapters = { chaptersVisible = true })
            }
        }
    }
    if (chaptersVisible) {
        RewindChapterSheet(chapters, pager, style, strings, onDismiss = { chaptersVisible = false })
    }
}

@Composable
private fun RewindPlaceholder(state: RewindState, style: MusicExperienceStyle, strings: LevyraStrings, onRetry: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Levyra\nRewind", style = MaterialTheme.typography.displayLargeEmphasized, color = style.accent)
        Spacer(Modifier.height(24.dp))
        when {
            state.loading -> LoadingIndicator(color = style.accent, modifier = Modifier.size(72.dp))
            state.failed -> {
                Text(strings.insightsError, style = MaterialTheme.typography.titleLarge)
                Button(onClick = onRetry) { Text(strings.updateRetry) }
            }
            else -> {
                Text(strings.insightsEmpty, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Text(strings.musicExperience(MusicExperienceText.Private), color = style.muted)
            }
        }
    }
}

@Composable
private fun RewindControls(
    pager: PagerState, count: Int, animated: Boolean, style: MusicExperienceStyle, strings: LevyraStrings, onChapters: () -> Unit
) {
    val scope = rememberCoroutineScope()
    fun go(page: Int) = scope.launch { if (animated) pager.animateScrollToPage(page) else pager.scrollToPage(page) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { go(pager.currentPage - 1) }, enabled = pager.currentPage > 0) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back)
        }
        TextButton(onClick = onChapters, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.ViewCarousel, null, tint = style.accent)
            Spacer(Modifier.width(8.dp))
            Text("${pager.currentPage + 1} / $count", color = style.text)
        }
        FilledIconButton(
            onClick = { go(pager.currentPage + 1) },
            enabled = pager.currentPage < count - 1,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = style.accent, contentColor = style.onAccent),
            shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(52.dp).testTag("rewind-next")
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, strings.next) }
    }
}

@Composable
private fun RewindChapterSheet(chapters: List<RewindChapter>, pager: PagerState, style: MusicExperienceStyle, strings: LevyraStrings, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = style.background) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp)) {
            Text(strings.musicExperience(MusicExperienceText.Chapters), style = MaterialTheme.typography.headlineMedium, color = style.text, modifier = Modifier.semantics { heading() })
            chapters.forEachIndexed { index, chapter ->
                TextButton(onClick = { onDismiss(); scope.launch { pager.scrollToPage(index) } }, modifier = Modifier.fillMaxWidth()) {
                    Text("${index + 1} / ${rewindChapterLabel(chapter, strings)}", color = if (index == pager.currentPage) style.accent else style.text, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

internal fun rewindChapterLabel(chapter: RewindChapter, strings: LevyraStrings): String = when (chapter) {
    RewindChapter.Overture -> "Levyra Rewind"
    RewindChapter.Time -> strings.receiptMinutesListened
    RewindChapter.Artists -> strings.topArtistsTitle
    RewindChapter.Tracks -> strings.topTracksTitle
    RewindChapter.Albums -> strings.topAlbumsTitle
    RewindChapter.Rhythm -> strings.insightsRhythm
    RewindChapter.Timeline -> strings.insightsActivity
    RewindChapter.Poster -> strings.share
}
