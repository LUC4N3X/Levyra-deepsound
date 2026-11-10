package com.luc4n3x.levyra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.homeTrendingVideoShelfTitle
import com.luc4n3x.levyra.ui.i18n.homeVideoShelfTitle
import com.luc4n3x.levyra.viewmodel.TrendingMusicVideosViewModel

private const val TrendingVideosKey = "home-trending-music-videos"

@Composable
internal fun rememberHomeVideoTracks(code: String, regionId: String, fallback: List<YoutubeMusicHomeItem>): List<Track> {
    val trending = viewModel<TrendingMusicVideosViewModel>(key = TrendingVideosKey)
    LaunchedEffect(code, regionId) { trending.load(regionId, code) }
    val videos by trending.videos.collectAsStateWithLifecycle()
    val fallbackTracks = remember(fallback) { fallback.mapNotNull { it.track } }
    return videos.ifEmpty { fallbackTracks }
}

@Composable
internal fun rememberHomeVideoShelfTitle(strings: LevyraStrings, tracks: List<Track>): String {
    val trending by viewModel<TrendingMusicVideosViewModel>(key = TrendingVideosKey).videos.collectAsStateWithLifecycle()
    val showsTrending = trending.isNotEmpty() && tracks.firstOrNull()?.id == trending.first().id
    return if (showsTrending) strings.homeTrendingVideoShelfTitle else strings.homeVideoShelfTitle
}
