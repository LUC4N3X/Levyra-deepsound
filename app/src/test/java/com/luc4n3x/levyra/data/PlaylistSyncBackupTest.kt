package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.local.PlaylistSyncEntity
import com.luc4n3x.levyra.data.youtubemusic.KeyedMutex
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlaylistSyncBackupTest {
    @Test
    fun roundTripKeepsBaselineAndDropsLinksWithoutPlaylist() {
        val kept = link("pl-1", "PLremote1")
        val orphan = link("pl-gone", "PLremote2")
        val array = JSONArray().put(playlistSyncToJson(kept)).put(playlistSyncToJson(orphan))

        val restored = parsePlaylistSyncs(array, setOf("pl-1"))

        assertEquals(1, restored.size)
        assertEquals("PLremote1", restored.single().remotePlaylistId)
        assertEquals("AAAAAAAAAAA,BBBBBBBBBBB", restored.single().videoIds)
        assertEquals(0L, restored.single().localUpdatedAt)
    }

    @Test
    fun duplicateRemoteLinksKeepOnlyTheFirst() {
        val array = JSONArray()
            .put(playlistSyncToJson(link("pl-1", "PLsame")))
            .put(playlistSyncToJson(link("pl-2", "VLPLsame")))

        val restored = parsePlaylistSyncs(array, setOf("pl-1", "pl-2"))

        assertEquals(listOf("pl-1"), restored.map { it.playlistId })
    }

    @Test
    fun keyedMutexForgetsKeysOnceReleased() = runBlocking {
        val locks = KeyedMutex()
        val order = mutableListOf<Int>()
        listOf(1, 2).map { value ->
            async {
                locks.withLock("pl-1") {
                    order += value
                    delay(10)
                    order += value
                }
            }
        }.awaitAll()

        assertEquals(0, locks.size())
        assertFalse(order == listOf(1, 2, 1, 2))
    }

    private fun link(playlistId: String, remoteId: String) = PlaylistSyncEntity(
        playlistId = playlistId,
        accountId = "account",
        remotePlaylistId = remoteId,
        localTitle = "Mix",
        remoteTitle = "Mix",
        videoIds = "AAAAAAAAAAA,BBBBBBBBBBB,bad",
        autoSync = true,
        synced = true,
        localUpdatedAt = 99L,
        lastSyncedAt = 50L,
        lastError = "NETWORK",
        unmatched = 1,
        keptLocal = 0
    )
}
