package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.local.SavedPlaylistEntity
import com.luc4n3x.levyra.data.local.SavedPlaylistsDao
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.PlaylistHitPreview
import com.luc4n3x.levyra.domain.SavedPlaylist
import com.luc4n3x.levyra.domain.canBeSaved
import com.luc4n3x.levyra.domain.savedPlaylistKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedPlaylistsStoreTest {
    private val hit = PlaylistHit(
        title = "Radiohead Essentials",
        author = "YouTube Music",
        thumbnailUrl = "https://example.org/cover.jpg",
        playlistId = "RDCLAK5uy_radiohead",
        browseId = "VLRDCLAK5uy_radiohead"
    )

    @Test
    fun savingKeepsTheRemoteIdentityWithoutCopyingTracks() = runBlocking {
        val dao = FakeSavedPlaylistsDao()
        val store = SavedPlaylistsStore(dao) { 42L }

        assertTrue(store.toggle(SavedPlaylist(hit)))

        val row = dao.rows.value.single()
        assertEquals("youtube:RDCLAK5uy_radiohead", row.playlistKey)
        assertEquals("RDCLAK5uy_radiohead", row.playlistId)
        assertEquals("VLRDCLAK5uy_radiohead", row.browseId)
        assertEquals(42L, row.savedAt)
        assertEquals(listOf(hit.copy()), store.observe().first().map { it.hit })
    }

    @Test
    fun toggleRemovesAnAlreadySavedPlaylist() = runBlocking {
        val store = SavedPlaylistsStore(FakeSavedPlaylistsDao())
        store.toggle(SavedPlaylist(hit))

        assertFalse(store.toggle(SavedPlaylist(hit)))
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun savedPlaylistsSurviveANewStoreInstance() = runBlocking {
        val dao = FakeSavedPlaylistsDao()
        SavedPlaylistsStore(dao).toggle(SavedPlaylist(hit))

        val reopened = SavedPlaylistsStore(dao)

        assertEquals(listOf("youtube:RDCLAK5uy_radiohead"), reopened.load().map { it.key })
    }

    @Test
    fun repeatedRestoreNeverDuplicatesAPlaylist() = runBlocking {
        val store = SavedPlaylistsStore(FakeSavedPlaylistsDao())

        store.replaceAll(listOf(SavedPlaylist(hit, savedAt = 1L), SavedPlaylist(hit.copy(title = "Renamed"), savedAt = 2L)))
        store.replaceAll(store.load())

        assertEquals(1, store.load().size)
    }

    @Test
    fun blankIdentityIsNeverStored() = runBlocking {
        val store = SavedPlaylistsStore(FakeSavedPlaylistsDao())

        assertFalse(store.toggle(SavedPlaylist(hit.copy(playlistId = "  "))))
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun appleAndYoutubePlaylistsWithTheSameIdStayDistinct() {
        assertNotEquals(savedPlaylistKey("pl.123", ""), savedPlaylistKey("pl.123", "it"))
        assertEquals("apple:it:pl.123", SavedPlaylist(hit.copy(playlistId = "pl.123"), appleStorefront = "IT").key)
    }

    @Test
    fun onlyLoadedNonEmptyPlaylistsCanBeSaved() {
        val track = com.luc4n3x.levyra.feature.discovery.track("Creep", "Radiohead")
        assertFalse(PlaylistHitPreview(hit).canBeSaved)
        assertFalse(PlaylistHitPreview(hit, loading = false, failed = true).canBeSaved)
        assertFalse(PlaylistHitPreview(hit, loading = false, tracks = emptyList()).canBeSaved)
        assertFalse(PlaylistHitPreview(hit.copy(playlistId = ""), tracks = listOf(track), loading = false).canBeSaved)
        assertTrue(PlaylistHitPreview(hit, tracks = listOf(track), loading = false).canBeSaved)
    }

    @Test
    fun backupJsonRoundTripsAndSkipsInvalidRows() {
        val array = JSONArray()
            .put(
                JSONObject()
                    .put("playlistId", "pl.123")
                    .put("appleStorefront", "us")
                    .put("title", "Today's Hits")
                    .put("savedAt", 7L)
            )
            .put(JSONObject().put("playlistId", "").put("title", "broken"))
            .put(JSONObject().put("playlistId", "pl.123").put("appleStorefront", "us").put("title", "duplicate"))

        val parsed = parseSavedPlaylists(array)

        assertEquals(1, parsed.size)
        assertEquals("apple:us:pl.123", parsed.single().key)
        assertEquals("Today's Hits", parsed.single().hit.title)
        assertEquals(7L, parsed.single().savedAt)
    }
}

private class FakeSavedPlaylistsDao : SavedPlaylistsDao() {
    val rows = MutableStateFlow<List<SavedPlaylistEntity>>(emptyList())

    override fun observeAll(): Flow<List<SavedPlaylistEntity>> = rows

    override suspend fun all(): List<SavedPlaylistEntity> = rows.value.sortedByDescending { it.savedAt }

    override suspend fun contains(playlistKey: String): Boolean = rows.value.any { it.playlistKey == playlistKey }

    override suspend fun upsert(row: SavedPlaylistEntity) {
        rows.value = rows.value.filterNot { it.playlistKey == row.playlistKey } + row
    }

    override suspend fun upsertAll(rows: List<SavedPlaylistEntity>) = rows.forEach { upsert(it) }

    override suspend fun delete(playlistKey: String) {
        rows.value = rows.value.filterNot { it.playlistKey == playlistKey }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }
}
