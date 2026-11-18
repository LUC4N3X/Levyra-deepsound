package com.luc4n3x.levyra.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.components.levyraUtilityColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.luc4n3x.levyra.ui.components.LevyraPastelAmber
import com.luc4n3x.levyra.ui.components.LevyraPastelLavender
import com.luc4n3x.levyra.ui.components.LevyraPastelPeriwinkle
import com.luc4n3x.levyra.ui.components.LevyraPastelRose
import com.luc4n3x.levyra.ui.components.LevyraUtilitySeed
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_ALBUMS
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_CHART_MARKET
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_COLLECTIONS
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_CURATED_PLAYLISTS
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_DAILY_DISCOVER
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_FAVORITE_RADIO
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_FORGOTTEN_FAVORITES
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_MORE_MUSIC
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_NEW_RELEASES
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_OFFLINE_DOWNLOADS
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_OFFLINE_FAVORITES
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_OFFLINE_PLAYLISTS
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_OFFLINE_RECENT
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_ORBIT
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_QUICK_PICKS
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_RELEASE_RADAR
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_SIMILAR_ARTISTS
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_TRENDING_ARTISTS
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_VIDEOS
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_YOUR_MUSIC
import com.luc4n3x.levyra.ui.theme.LevyraIsDefaultSkin

@Immutable
internal data class HomeSectionMedallion(val icon: ImageVector, val seed: Color)

internal const val HOME_MEDALLION_NONE = -1
internal const val HOME_MEDALLION_GENRE_HITS = 100
internal const val HOME_MEDALLION_MOST_DISCUSSED = 101
internal const val HOME_MEDALLION_TRENDING_VIDEOS = 102

internal fun homeSectionMedallion(key: Int): HomeSectionMedallion? {
    if (!LevyraIsDefaultSkin) return null
    return when (key) {
        HOME_SUBTITLE_OFFLINE_DOWNLOADS -> HomeSectionMedallion(Icons.Rounded.DownloadDone, LevyraUtilitySeed)
        HOME_SUBTITLE_OFFLINE_PLAYLISTS -> HomeSectionMedallion(Icons.AutoMirrored.Rounded.QueueMusic, LevyraPastelLavender)
        HOME_SUBTITLE_OFFLINE_FAVORITES -> HomeSectionMedallion(Icons.Rounded.Favorite, LevyraPastelRose)
        HOME_SUBTITLE_OFFLINE_RECENT -> HomeSectionMedallion(Icons.Rounded.History, LevyraPastelPeriwinkle)
        HOME_SUBTITLE_ORBIT -> HomeSectionMedallion(Icons.Rounded.TrackChanges, LevyraPastelLavender)
        HOME_SUBTITLE_QUICK_PICKS -> HomeSectionMedallion(Icons.Rounded.Bolt, LevyraPastelAmber)
        HOME_SUBTITLE_FORGOTTEN_FAVORITES -> HomeSectionMedallion(Icons.Rounded.Restore, LevyraPastelRose)
        HOME_SUBTITLE_ALBUMS -> HomeSectionMedallion(Icons.Rounded.Album, LevyraPastelPeriwinkle)
        HOME_SUBTITLE_TRENDING_ARTISTS -> HomeSectionMedallion(Icons.AutoMirrored.Rounded.TrendingUp, LevyraPastelRose)
        HOME_SUBTITLE_NEW_RELEASES -> HomeSectionMedallion(Icons.Rounded.NewReleases, LevyraPastelAmber)
        HOME_SUBTITLE_VIDEOS -> HomeSectionMedallion(Icons.Rounded.SmartDisplay, LevyraUtilitySeed)
        HOME_SUBTITLE_RELEASE_RADAR -> HomeSectionMedallion(Icons.Rounded.Radar, LevyraPastelLavender)
        HOME_SUBTITLE_SIMILAR_ARTISTS -> HomeSectionMedallion(Icons.Rounded.Group, LevyraPastelPeriwinkle)
        HOME_SUBTITLE_MORE_MUSIC -> HomeSectionMedallion(Icons.Rounded.LibraryMusic, LevyraUtilitySeed)
        HOME_SUBTITLE_CURATED_PLAYLISTS -> HomeSectionMedallion(Icons.Rounded.AutoAwesome, LevyraPastelLavender)
        HOME_SUBTITLE_COLLECTIONS -> HomeSectionMedallion(Icons.Rounded.CollectionsBookmark, LevyraPastelAmber)
        HOME_SUBTITLE_DAILY_DISCOVER -> HomeSectionMedallion(Icons.Rounded.Explore, LevyraUtilitySeed)
        HOME_SUBTITLE_FAVORITE_RADIO -> HomeSectionMedallion(Icons.Rounded.Radio, LevyraPastelRose)
        HOME_SUBTITLE_CHART_MARKET -> HomeSectionMedallion(Icons.Rounded.Leaderboard, LevyraPastelAmber)
        HOME_SUBTITLE_YOUR_MUSIC -> HomeSectionMedallion(Icons.Rounded.LibraryMusic, LevyraPastelPeriwinkle)
        HOME_MEDALLION_GENRE_HITS -> HomeSectionMedallion(Icons.Rounded.Leaderboard, LevyraPastelPeriwinkle)
        HOME_MEDALLION_MOST_DISCUSSED -> HomeSectionMedallion(Icons.Rounded.Forum, LevyraPastelRose)
        HOME_MEDALLION_TRENDING_VIDEOS -> HomeSectionMedallion(Icons.Rounded.Theaters, LevyraUtilitySeed)
        else -> null
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeSectionMedallionBadge(medallion: HomeSectionMedallion, modifier: Modifier = Modifier) {
    val tone = levyraUtilityColors(medallion.seed)
    Box(
        modifier.size(40.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(tone.accent),
        contentAlignment = Alignment.Center
    ) {
        Icon(medallion.icon, contentDescription = null, tint = tone.onAccent, modifier = Modifier.size(20.dp))
    }
}
