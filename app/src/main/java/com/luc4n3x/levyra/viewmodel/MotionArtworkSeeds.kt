package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.AlbumDetail
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.feature.motion.MotionTrackIdentity
import com.luc4n3x.levyra.feature.motion.normalizeMotionText
import com.luc4n3x.levyra.feature.motion.primaryMotionArtistMatches
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

internal fun shouldContinueMotionPrefetch(
    activeKey: String,
    currentKey: String,
    nextKey: String,
    queueChanged: Boolean
): Boolean = when (activeKey) {
    nextKey -> true
    currentKey -> !queueChanged
    else -> false
}

internal fun shouldRefreshMotionArtworkOwnership(
    previous: LevyraInterfaceSettings,
    next: LevyraInterfaceSettings
): Boolean = previous.canvasSource != next.canvasSource ||
    previous.motionArtworkWifiOnly != next.motionArtworkWifiOnly

internal fun selectArtistMotionSeed(
    profileName: String,
    tracks: List<Track>,
    isLocal: (Track) -> Boolean
): Track? = selectArtistMotionSeeds(profileName, tracks, isLocal).firstOrNull()

internal fun selectArtistMotionSeeds(
    profileName: String,
    tracks: List<Track>,
    isLocal: (Track) -> Boolean,
    limit: Int = MAX_ARTIST_MOTION_SEEDS
): List<Track> = tracks.filter { candidate ->
    !isLocal(candidate) &&
        primaryMotionArtistMatches(
            MotionTrackIdentity.from(candidate).artists,
            listOf(profileName)
        )
}.take(limit)

private const val MAX_ARTIST_MOTION_SEEDS = 4

internal suspend fun firstAvailableMotionArtwork(
    attempts: List<suspend () -> MotionArtwork?>
): MotionArtwork? = channelFlow {
    attempts.forEach { attempt -> launch { send(attempt()) } }
}.firstOrNull { it != null }

internal fun selectAlbumMotionSeed(
    detail: AlbumDetail,
    isLocal: (Track) -> Boolean
): Track? = detail.tracks.firstOrNull { candidate ->
    !isLocal(candidate) && albumMotionSeedMatches(
        track = candidate,
        albumTitle = detail.album.title,
        albumArtist = detail.album.artist
    )
}

internal fun albumMotionSeedMatches(track: Track, albumTitle: String, albumArtist: String): Boolean {
    val identity = MotionTrackIdentity.from(track)
    if (albumArtist.isNotBlank() && !primaryMotionArtistMatches(identity.artists, listOf(albumArtist))) return false
    if (albumTitle.isBlank() || identity.album.isBlank()) return true
    return normalizeMotionText(identity.album) == normalizeMotionText(albumTitle)
}

internal fun canPublishAlbumMotionArtwork(
    visible: AlbumDetail?,
    expected: AlbumDetail,
    albumVisible: Boolean,
    animationsEnabled: Boolean,
    motionArtworkEnabled: Boolean
): Boolean {
    if (!albumVisible || !animationsEnabled || !motionArtworkEnabled || visible == null) return false
    val expectedBrowseId = expected.album.browseId
    if (expectedBrowseId.isNotBlank() && visible.album.browseId.isNotBlank()) {
        return visible.album.browseId.equals(expectedBrowseId, ignoreCase = true)
    }
    return visible.album.title.equals(expected.album.title, ignoreCase = true) &&
        visible.album.artist.equals(expected.album.artist, ignoreCase = true)
}
