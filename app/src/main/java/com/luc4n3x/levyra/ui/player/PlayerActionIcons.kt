package com.luc4n3x.levyra.ui.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Airplay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowCircleDown
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
internal data class PlayerActionIcons(
    val favoriteOn: ImageVector,
    val favoriteOff: ImageVector,
    val previous: ImageVector,
    val next: ImageVector,
    val more: ImageVector,
    val lyrics: ImageVector,
    val queue: ImageVector,
    val playerStyle: ImageVector,
    val downloadPending: ImageVector,
    val downloaded: ImageVector,
    val output: ImageVector,
    val share: ImageVector,
    val shareMoment: ImageVector,
    val addToPlaylist: ImageVector,
    val startRadio: ImageVector,
    val dislike: ImageVector,
    val sleepTimer: ImageVector,
    val speed: ImageVector,
    val sectionLoop: ImageVector,
    val normalization: ImageVector,
    val nightListening: ImageVector,
    val audioQuality: ImageVector,
    val technicalAudio: ImageVector,
    val ambient: ImageVector,
    val refreshArtwork: ImageVector,
    val mixChapters: ImageVector,
    val credits: ImageVector,
    val album: ImageVector,
    val artist: ImageVector
) {
    fun favorite(isFavorite: Boolean): ImageVector = if (isFavorite) favoriteOn else favoriteOff

    fun download(isDownloaded: Boolean): ImageVector = if (isDownloaded) downloaded else downloadPending
}

internal val LevyraPlayerActionIcons = PlayerActionIcons(
    favoriteOn = Icons.Rounded.Favorite,
    favoriteOff = Icons.Rounded.FavoriteBorder,
    previous = Icons.Rounded.SkipPrevious,
    next = Icons.Rounded.SkipNext,
    more = Icons.Rounded.MoreVert,
    lyrics = Icons.AutoMirrored.Rounded.Subject,
    queue = Icons.AutoMirrored.Rounded.QueueMusic,
    playerStyle = Icons.Rounded.Style,
    downloadPending = Icons.Rounded.Download,
    downloaded = Icons.Rounded.DownloadDone,
    output = Icons.Rounded.Speaker,
    share = Icons.Rounded.Share,
    shareMoment = Icons.Rounded.AccessTime,
    addToPlaylist = Icons.AutoMirrored.Rounded.PlaylistAdd,
    startRadio = Icons.Rounded.Radio,
    dislike = Icons.Rounded.ThumbDown,
    sleepTimer = Icons.Rounded.Bedtime,
    speed = Icons.Rounded.Speed,
    sectionLoop = Icons.Rounded.Loop,
    normalization = Icons.AutoMirrored.Rounded.VolumeUp,
    nightListening = Icons.Rounded.NightsStay,
    audioQuality = Icons.Rounded.Tune,
    technicalAudio = Icons.Rounded.Info,
    ambient = Icons.Rounded.Nightlight,
    refreshArtwork = Icons.Rounded.Refresh,
    mixChapters = Icons.AutoMirrored.Rounded.ListAlt,
    credits = Icons.Rounded.Groups,
    album = Icons.Rounded.Album,
    artist = Icons.Rounded.Person
)

internal val AppleMusicPlayerActionIcons = LevyraPlayerActionIcons.copy(
    favoriteOn = Icons.Rounded.Star,
    favoriteOff = Icons.Rounded.StarBorder,
    previous = Icons.Rounded.FastRewind,
    next = Icons.Rounded.FastForward,
    more = Icons.Rounded.MoreHoriz,
    lyrics = Icons.Rounded.FormatQuote,
    queue = Icons.AutoMirrored.Rounded.FormatListBulleted,
    downloadPending = Icons.Rounded.ArrowCircleDown,
    downloaded = Icons.Rounded.DownloadForOffline,
    output = Icons.Rounded.Airplay,
    share = Icons.Rounded.IosShare
)

internal val PlayerIcons: PlayerActionIcons
    @Composable @ReadOnlyComposable get() =
        if (PlayerAppleMusicStyle) AppleMusicPlayerActionIcons else LevyraPlayerActionIcons
