package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.YoutubeMusicMoodPlaylist
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign
import com.luc4n3x.levyra.viewmodel.MoodGenrePageViewModel

@Composable
internal fun YouTubeMusicGenreScreen(
    title: String,
    params: String,
    languageCode: String,
    bottomInset: Dp,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpenPlaylist: (PlaylistHit) -> Unit,
    fallback: @Composable () -> Unit
) {
    val pageViewModel = viewModel<MoodGenrePageViewModel>()
    val state by pageViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(params, languageCode) { pageViewModel.load(params, languageCode) }
    val pageKey = "$languageCode:$params"
    if (state.key == pageKey && state.failed) {
        fallback()
        return
    }
    val sections = state.sections.takeIf { state.key == pageKey }.orEmpty()
    var openSection by rememberSaveable(pageKey) { mutableStateOf<Int?>(null) }
    val expanded = openSection?.let(sections::getOrNull)
    BackHandler(enabled = backEnabled || expanded != null) {
        if (expanded != null) openSection = null else onBack()
    }
    Column(Modifier.fillMaxSize().background(YouTubeMusicDesign.background)) {
        YouTubeMusicGenreTopBar(
            title = expanded?.title ?: title,
            onBack = { if (expanded != null) openSection = null else onBack() },
            onSearch = onSearch
        )
        if (expanded != null) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(YouTubeMusicDesign.cardArtwork - 16.dp),
                contentPadding = PaddingValues(
                    start = YouTubeMusicDesign.gutter,
                    end = YouTubeMusicDesign.gutter,
                    top = 8.dp,
                    bottom = bottomInset + YouTubeMusicDesign.sectionGap
                ),
                horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap),
                verticalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.sectionGap),
                modifier = Modifier.fillMaxSize()
            ) {
                items(expanded.playlists, key = { it.playlistId.ifBlank { it.browseId } }) { playlist ->
                    YouTubeMusicGenreCard(playlist, Modifier.fillMaxWidth()) { onOpenPlaylist(playlist.toHit()) }
                }
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = bottomInset + YouTubeMusicDesign.sectionGap),
            verticalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.sectionGap + 8.dp)
        ) {
            if (sections.isEmpty()) {
                items(YouTubeMusicGenrePlaceholders, key = { "ytm-genre-placeholder-$it" }) {
                    YouTubeMusicGenrePlaceholderRow()
                }
            } else {
                itemsIndexed(sections, key = { index, section -> "ytm-genre-section-$index-${section.title}" }) { index, section ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        YouTubeMusicSectionHeader(section.title, onOpen = { openSection = index })
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
                            horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
                        ) {
                            items(section.playlists, key = { it.playlistId.ifBlank { it.browseId } }) { playlist ->
                                YouTubeMusicGenreCard(playlist, Modifier.width(YouTubeMusicDesign.cardArtwork)) {
                                    onOpenPlaylist(playlist.toHit())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeMusicGenreTopBar(title: String, onBack: () -> Unit, onSearch: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(YouTubeMusicDesign.topBarHeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = YouTubeMusicDesign.text)
        }
        Text(
            title,
            style = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
            color = YouTubeMusicDesign.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 8.dp).semantics { heading() }
        )
        IconButton(onClick = onSearch) {
            Icon(Icons.Rounded.Search, strings.search, tint = YouTubeMusicDesign.text)
        }
    }
}

@Composable
private fun YouTubeMusicGenreCard(playlist: YoutubeMusicMoodPlaylist, modifier: Modifier, onOpen: () -> Unit) {
    Column(
        modifier.levyraPressable(onClick = onOpen, onClickLabel = playlist.title, role = Role.Button),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        StableRemoteArtwork(
            url = playlist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(YouTubeMusicDesign.cardShape)
                .background(YouTubeMusicDesign.placeholder),
            contentScale = ContentScale.Crop,
            highRes = true
        )
        Text(
            playlist.title,
            style = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
            color = YouTubeMusicDesign.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (playlist.description.isNotBlank()) {
            Text(
                playlist.description,
                style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
                color = YouTubeMusicDesign.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun YouTubeMusicGenrePlaceholderRow() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier
                .padding(start = YouTubeMusicDesign.gutter, top = 12.dp)
                .width(220.dp)
                .height(24.dp)
                .clip(YouTubeMusicDesign.cardShape)
                .background(YouTubeMusicDesign.placeholder)
        )
        Row(
            Modifier.padding(start = YouTubeMusicDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
        ) {
            repeat(3) {
                Box(
                    Modifier
                        .width(YouTubeMusicDesign.cardArtwork)
                        .aspectRatio(1f)
                        .clip(YouTubeMusicDesign.cardShape)
                        .background(YouTubeMusicDesign.placeholder)
                )
            }
        }
    }
}

private fun YoutubeMusicMoodPlaylist.toHit() = PlaylistHit(
    title = title,
    author = description,
    thumbnailUrl = thumbnailUrl,
    playlistId = playlistId,
    browseId = browseId
)

private val YouTubeMusicGenrePlaceholders = listOf(0, 1, 2)
