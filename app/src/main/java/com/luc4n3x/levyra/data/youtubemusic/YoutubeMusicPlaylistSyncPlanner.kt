package com.luc4n3x.levyra.data.youtubemusic

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.playlistexport.youtubeVideoIdOrNull
import com.luc4n3x.levyra.domain.Track

internal data class PlaylistSyncBaseline(
    val remoteTitle: String,
    val videoIds: List<String>
)

internal data class PlaylistSyncPlan(
    val videoIdsToAddRemotely: List<String>,
    val remoteItemsToRemove: List<RemotePlaylistRemoval>,
    val tracksToAddLocally: List<Track>,
    val localTrackIdsToRemove: Set<String>,
    val renameRemoteTitle: String?,
    val renameLocalTitle: String?,
    val newBaselineTitle: String,
    val newBaselineVideoIds: List<String>,
    val unmatchedLocalCount: Int
)

internal fun canonicalYoutubePlaylistId(raw: String): String =
    raw.trim().substringAfter("list=").removePrefix("VL").trim()

internal fun youtubeMusicPlaylistUrl(remotePlaylistId: String): String =
    "${YoutubeMusicAuth.ORIGIN}/playlist?list=${canonicalYoutubePlaylistId(remotePlaylistId)}"

internal fun encodeSyncedVideoIds(videoIds: List<String>): String =
    videoIds.asSequence().map(String::trim).filter(String::isNotBlank).distinct().joinToString(",")

internal fun decodeSyncedVideoIds(raw: String): List<String> =
    raw.split(',').asSequence().map(String::trim).filter(String::isNotBlank).distinct().toList()

internal fun planYoutubeMusicTwoWaySync(
    localName: String,
    localTracks: List<Track>,
    remoteTitle: String,
    remoteItems: List<YoutubeMusicRepository.RemotePlaylistItem>,
    baseline: PlaylistSyncBaseline?
): PlaylistSyncPlan {
    val localByVideoId = LinkedHashMap<String, MutableList<Track>>()
    var unmatchedLocalCount = 0
    localTracks.forEach { track ->
        val videoId = track.youtubeVideoIdOrNull()
        if (videoId == null) {
            unmatchedLocalCount++
        } else {
            localByVideoId.getOrPut(videoId) { mutableListOf() }.add(track)
        }
    }

    val remoteByVideoId = LinkedHashMap<String, MutableList<YoutubeMusicRepository.RemotePlaylistItem>>()
    remoteItems.forEach { item ->
        val videoId = item.videoId.trim()
        if (videoId.isNotBlank()) {
            remoteByVideoId.getOrPut(videoId) { mutableListOf() }.add(item)
        }
    }

    val localVideoIds = localByVideoId.keys
    val remoteVideoIds = remoteByVideoId.keys
    val cleanLocalName = localName.trim()
    val cleanRemoteTitle = remoteTitle.trim()

    if (baseline == null) {
        val toAddRemotely = localVideoIds.filterNot { it in remoteVideoIds }
        val toAddLocally = remoteByVideoId.entries
            .filter { (videoId, _) -> videoId !in localVideoIds }
            .map { (_, items) -> items.first().track.copy(streamUrl = "", videoStreamUrl = "") }
        val newBaselineTitle = cleanRemoteTitle.ifBlank { cleanLocalName }
        val newBaselineIds = (remoteVideoIds + toAddRemotely).distinct()
        return PlaylistSyncPlan(
            videoIdsToAddRemotely = toAddRemotely,
            remoteItemsToRemove = emptyList(),
            tracksToAddLocally = toAddLocally,
            localTrackIdsToRemove = emptySet(),
            renameRemoteTitle = null,
            renameLocalTitle = null,
            newBaselineTitle = newBaselineTitle,
            newBaselineVideoIds = newBaselineIds,
            unmatchedLocalCount = unmatchedLocalCount
        )
    }

    val baselineIds = baseline.videoIds.toSet()
    val toAddRemotely = localVideoIds.filter { it !in remoteVideoIds && it !in baselineIds }
    val tracksToAddLocally = remoteByVideoId.entries
        .filter { (videoId, _) -> videoId !in localVideoIds && videoId !in baselineIds }
        .map { (_, items) -> items.first().track.copy(streamUrl = "", videoStreamUrl = "") }

    val localTrackIdsToRemove = LinkedHashSet<String>()
    localByVideoId.forEach { (videoId, tracks) ->
        if (videoId !in remoteVideoIds && videoId in baselineIds) {
            tracks.forEach { track ->
                if (track.id.isNotBlank()) localTrackIdsToRemove += track.id
            }
        }
    }

    val remoteItemsToRemove = mutableListOf<RemotePlaylistRemoval>()
    val unremovableRemoteIds = hashSetOf<String>()
    remoteByVideoId.forEach { (videoId, items) ->
        if (videoId !in localVideoIds && videoId in baselineIds) {
            val removals = items
                .filter { it.setVideoId.isNotBlank() }
                .map { RemotePlaylistRemoval(videoId = it.videoId, setVideoId = it.setVideoId) }
            if (removals.isNotEmpty()) {
                remoteItemsToRemove += removals
            } else {
                unremovableRemoteIds += videoId
            }
        }
    }

    val cleanBaseTitle = baseline.remoteTitle.trim()
    val localTitleChanged = cleanBaseTitle.isNotBlank() && cleanLocalName.isNotBlank() && cleanLocalName != cleanBaseTitle
    val remoteTitleChanged = cleanBaseTitle.isNotBlank() && cleanRemoteTitle.isNotBlank() && cleanRemoteTitle != cleanBaseTitle

    val renameRemoteTitle: String?
    val renameLocalTitle: String?
    val newBaselineTitle: String
    when {
        cleanLocalName.isNotBlank() && cleanRemoteTitle.isNotBlank() && cleanLocalName == cleanRemoteTitle -> {
            renameRemoteTitle = null
            renameLocalTitle = null
            newBaselineTitle = cleanLocalName
        }
        localTitleChanged && !remoteTitleChanged -> {
            renameRemoteTitle = cleanLocalName
            renameLocalTitle = null
            newBaselineTitle = cleanLocalName
        }
        !localTitleChanged && remoteTitleChanged -> {
            renameRemoteTitle = null
            renameLocalTitle = cleanRemoteTitle
            newBaselineTitle = cleanRemoteTitle
        }
        localTitleChanged && remoteTitleChanged -> {
            renameRemoteTitle = cleanLocalName
            renameLocalTitle = null
            newBaselineTitle = cleanLocalName
        }
        else -> {
            renameRemoteTitle = null
            renameLocalTitle = null
            newBaselineTitle = cleanRemoteTitle.ifBlank { cleanLocalName }
        }
    }

    val removedFromRemoteIds = remoteItemsToRemove.mapTo(hashSetOf()) { it.videoId }
    val newBaselineIds = buildList {
        remoteVideoIds.forEach { id ->
            if (id !in removedFromRemoteIds || id in unremovableRemoteIds) add(id)
        }
        toAddRemotely.forEach { add(it) }
    }.distinct()

    return PlaylistSyncPlan(
        videoIdsToAddRemotely = toAddRemotely,
        remoteItemsToRemove = remoteItemsToRemove,
        tracksToAddLocally = tracksToAddLocally,
        localTrackIdsToRemove = localTrackIdsToRemove,
        renameRemoteTitle = renameRemoteTitle,
        renameLocalTitle = renameLocalTitle,
        newBaselineTitle = newBaselineTitle,
        newBaselineVideoIds = newBaselineIds,
        unmatchedLocalCount = unmatchedLocalCount
    )
}
