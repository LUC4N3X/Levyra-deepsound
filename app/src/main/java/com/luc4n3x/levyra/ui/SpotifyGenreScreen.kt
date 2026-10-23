package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
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
import com.luc4n3x.levyra.data.YoutubeMusicMoodSection
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.viewmodel.MoodGenrePageViewModel

@Composable
internal fun SpotifyGenreScreen(
    title: String,
    params: String,
    languageCode: String,
    bottomInset: Dp,
    backEnabled: Boolean,
    onBack: () -> Unit,
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
    BackHandler(enabled = backEnabled, onBack = onBack)
    val sections = state.sections.takeIf { state.key == pageKey }.orEmpty()
    BoxWithConstraints(Modifier.fillMaxSize().background(SpotifyDesign.base)) {
        val cardWidth = (maxWidth - SpotifyDesign.gutter - SpotifyGenreCardGap) / 2
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset + SpotifyGenreSectionGap)
        ) {
            item(key = "genre-header", contentType = "genre-header") {
                SpotifyGenreHeader(title, onBack)
            }
            if (sections.isEmpty()) {
                items(SpotifyGenrePlaceholderSections, key = { "genre-placeholder-$it" }, contentType = { "genre-section" }) {
                    SpotifyGenrePlaceholderSection(cardWidth)
                }
            } else {
                itemsIndexed(sections, key = { index, section -> "genre-section-$index-${section.title}" }, contentType = { _, _ -> "genre-section" }) { _, section ->
                    SpotifyGenreSection(section, cardWidth, onOpenPlaylist)
                }
            }
        }
    }
}

@Composable
private fun SpotifyGenreHeader(title: String, onBack: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val wash = lerp(SpotifyDesign.base, spotifyBrowseColor(title), SpotifyGenreWashAmount)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(wash, SpotifyDesign.base)))
            .statusBarsPadding()
            .padding(bottom = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = SpotifyDesign.text)
            }
        }
        Text(
            title,
            style = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
            color = SpotifyDesign.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = SpotifyDesign.gutter, end = SpotifyDesign.gutter, top = 24.dp)
                .semantics { heading() }
        )
    }
}

@Composable
private fun SpotifyGenreSection(
    section: YoutubeMusicMoodSection,
    cardWidth: Dp,
    onOpenPlaylist: (PlaylistHit) -> Unit
) {
    Column(Modifier.padding(top = SpotifyGenreSectionGap), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SpotifySectionTitle(section.title, SpotifyDesign.gutter)
        LazyRow(
            contentPadding = PaddingValues(horizontal = SpotifyDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(SpotifyGenreCardGap)
        ) {
            items(section.playlists, key = { it.playlistId.ifBlank { it.browseId } }) { playlist ->
                SpotifyGenreCard(playlist, cardWidth) {
                    onOpenPlaylist(
                        PlaylistHit(
                            title = playlist.title,
                            author = playlist.description,
                            thumbnailUrl = playlist.thumbnailUrl,
                            playlistId = playlist.playlistId,
                            browseId = playlist.browseId
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotifyGenreCard(playlist: YoutubeMusicMoodPlaylist, width: Dp, onOpen: () -> Unit) {
    Column(
        Modifier
            .width(width)
            .levyraPressable(onClick = onOpen, onClickLabel = playlist.title, role = Role.Button),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StableRemoteArtwork(
            url = playlist.thumbnailUrl,
            contentDescription = playlist.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(SpotifyDesign.coverShape)
                .background(SpotifyDesign.placeholder),
            contentScale = ContentScale.Crop,
            highRes = true
        )
        Text(
            playlist.description.ifBlank { playlist.title },
            style = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
            color = SpotifyDesign.subdued,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SpotifyGenrePlaceholderSection(cardWidth: Dp) {
    Column(Modifier.padding(top = SpotifyGenreSectionGap), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier
                .padding(start = SpotifyDesign.gutter, top = 12.dp)
                .width(200.dp)
                .height(22.dp)
                .clip(SpotifyDesign.coverShape)
                .background(SpotifyDesign.placeholder)
        )
        Row(
            Modifier.padding(start = SpotifyDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(SpotifyGenreCardGap)
        ) {
            repeat(2) {
                Box(
                    Modifier
                        .width(cardWidth)
                        .aspectRatio(1f)
                        .clip(SpotifyDesign.coverShape)
                        .background(SpotifyDesign.placeholder)
                )
            }
        }
    }
}

private val SpotifyGenreCardGap = 16.dp
private val SpotifyGenreSectionGap = 16.dp
private const val SpotifyGenreWashAmount = 0.62f
private val SpotifyGenrePlaceholderSections = listOf(0, 1)
