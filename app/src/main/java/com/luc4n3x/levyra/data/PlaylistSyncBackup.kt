package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.local.PlaylistSyncEntity
import com.luc4n3x.levyra.data.youtubemusic.canonicalYoutubeMusicPlaylistId
import com.luc4n3x.levyra.data.youtubemusic.decodePlaylistSyncVideoIds
import com.luc4n3x.levyra.data.youtubemusic.encodePlaylistSyncVideoIds
import org.json.JSONArray
import org.json.JSONObject

internal fun playlistSyncToJson(link: PlaylistSyncEntity): JSONObject = JSONObject()
    .put("playlistId", link.playlistId)
    .put("accountId", link.accountId)
    .put("remotePlaylistId", link.remotePlaylistId)
    .put("localTitle", link.localTitle)
    .put("remoteTitle", link.remoteTitle)
    .put("videoIds", link.videoIds)
    .put("autoSync", link.autoSync)
    .put("synced", link.synced)
    .put("lastSyncedAt", link.lastSyncedAt)

internal fun parsePlaylistSyncs(array: JSONArray?, playlistIds: Set<String>): List<PlaylistSyncEntity> {
    if (array == null) return emptyList()
    val seenRemotes = hashSetOf<String>()
    return buildList {
        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            val playlistId = json.optString("playlistId").trim()
            val accountId = json.optString("accountId").trim()
            val remoteId = canonicalYoutubeMusicPlaylistId(json.optString("remotePlaylistId"))
            if (playlistId !in playlistIds || accountId.isEmpty() || remoteId.isEmpty()) continue
            if (!seenRemotes.add("$accountId:$remoteId")) continue
            add(
                PlaylistSyncEntity(
                    playlistId = playlistId,
                    accountId = accountId,
                    remotePlaylistId = remoteId,
                    localTitle = json.optString("localTitle"),
                    remoteTitle = json.optString("remoteTitle"),
                    videoIds = encodePlaylistSyncVideoIds(decodePlaylistSyncVideoIds(json.optString("videoIds"))),
                    autoSync = json.optBoolean("autoSync", true),
                    synced = json.optBoolean("synced"),
                    localUpdatedAt = 0L,
                    lastSyncedAt = json.optLong("lastSyncedAt").coerceAtLeast(0L),
                    lastError = "",
                    unmatched = 0,
                    keptLocal = 0
                )
            )
        }
    }
}
