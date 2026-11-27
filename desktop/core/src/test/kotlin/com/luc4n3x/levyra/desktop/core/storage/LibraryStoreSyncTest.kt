package com.luc4n3x.levyra.desktop.core.storage

import com.luc4n3x.levyra.desktop.core.model.Track
import java.nio.file.Files
import java.nio.file.Path
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LibraryStoreSyncTest {
    private lateinit var directory: Path
    private var now = 1_000L

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("levyra-library-sync")
    }

    @After
    fun tearDown() {
        directory.toFile().deleteRecursively()
    }

    private fun newStore(): LibraryStore = LibraryStore(
        store = JsonFileStore(directory.resolve("library.json"), LibraryData.serializer(), defaultValue = { LibraryData() }),
        nowMillis = { now }
    )

    @Test
    fun editsBumpUpdatedAtOnlyForTheChangedPlaylist() {
        val store = newStore()
        val first = store.createPlaylist("First")
        val second = store.createPlaylist("Second")
        now = 5_000L
        store.addToPlaylist(first, listOf(track("AAAAAAAAAAA")))

        assertEquals(5_000L, store.current.playlists.first { it.id == first }.updatedAt)
        assertEquals(1_000L, store.current.playlists.first { it.id == second }.updatedAt)
        now = 9_000L
        store.removeTracks(first, setOf("AAAAAAAAAAA"))
        assertEquals(9_000L, store.current.playlists.first { it.id == first }.updatedAt)
    }

    @Test
    fun syncLinksRejectSecondPlaylistForSameRemoteAndFollowDeletion() {
        val store = newStore()
        val first = store.createPlaylist("First")
        val second = store.createPlaylist("Second")

        assertTrue(store.saveSyncLink(PlaylistSyncLink(first, "acc", "PLremote")))
        assertFalse(store.saveSyncLink(PlaylistSyncLink(second, "acc", "PLremote")))
        assertFalse(store.saveSyncLink(PlaylistSyncLink("missing", "acc", "PLother")))

        store.deletePlaylist(first)
        assertTrue(store.current.playlistSyncs.isEmpty())
        assertTrue(newStore().current.playlistSyncs.isEmpty())
    }

    @Test
    fun syncLinksSurviveReload() {
        val store = newStore()
        val id = store.createPlaylist("Road trip")
        store.saveSyncLink(PlaylistSyncLink(id, "acc", "PLremote", videoIds = listOf("AAAAAAAAAAA"), synced = true))

        val reloaded = newStore().current.playlistSyncs.single()
        assertEquals(listOf("AAAAAAAAAAA"), reloaded.videoIds)
        assertTrue(reloaded.synced)
    }

    private fun track(id: String) = Track(id = id, title = "Song $id", videoUrl = Track.watchUrlOf(id))
}
