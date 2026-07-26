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

class LibraryStoreTest {

    private lateinit var directory: Path

    private val track = Track(
        id = "abc",
        title = "Titolo",
        artist = "Artista",
        videoUrl = "https://www.youtube.com/watch?v=abc"
    )

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("levyra-library")
    }

    @After
    fun tearDown() {
        directory.toFile().deleteRecursively()
    }

    private fun newStore(clock: () -> Long = { 1_000L }): LibraryStore = LibraryStore(
        store = JsonFileStore(
            file = directory.resolve("library.json"),
            serializer = LibraryData.serializer(),
            defaultValue = { LibraryData() }
        ),
        nowMillis = clock
    )

    @Test
    fun `favorites toggle and persist`() {
        val store = newStore()
        store.toggleFavorite(track)
        assertTrue(store.isFavorite("abc"))

        val reloaded = newStore()
        assertTrue(reloaded.isFavorite("abc"))

        reloaded.toggleFavorite(track)
        assertFalse(reloaded.isFavorite("abc"))
        assertFalse(newStore().isFavorite("abc"))
    }

    @Test
    fun `playlists accept unique tracks only`() {
        val store = newStore()
        val playlistId = store.createPlaylist("  Preferiti estate ")
        store.addToPlaylist(playlistId, listOf(track, track))
        store.addToPlaylist(playlistId, listOf(track))

        val playlist = store.current.playlists.single()
        assertEquals("Preferiti estate", playlist.name)
        assertEquals(1, playlist.tracks.size)

        store.removeFromPlaylist(playlistId, "abc")
        assertTrue(store.current.playlists.single().tracks.isEmpty())

        store.deletePlaylist(playlistId)
        assertTrue(store.current.playlists.isEmpty())
    }

    @Test
    fun `history keeps the latest entry first without duplicates`() {
        var now = 10L
        val store = newStore { now }
        val other = track.copy(id = "def", title = "Altro")

        store.recordPlayback(track)
        now = 20L
        store.recordPlayback(other)
        now = 30L
        store.recordPlayback(track)

        val history = store.current.history
        assertEquals(2, history.size)
        assertEquals("abc", history.first().track.id)
        assertEquals(30L, history.first().playedAt)

        store.clearHistory()
        assertTrue(store.current.history.isEmpty())
    }

    @Test
    fun `recent searches are capped and normalised`() {
        val store = newStore()
        store.recordSearch("a")
        store.recordSearch("rock")
        store.recordSearch("ROCK")

        assertEquals(listOf("ROCK"), store.current.recentSearches)

        repeat(LibraryStore.MAX_RECENT_SEARCHES + 4) { index -> store.recordSearch("query$index") }

        val searches = store.current.recentSearches
        assertEquals(LibraryStore.MAX_RECENT_SEARCHES, searches.size)
        assertEquals("query${LibraryStore.MAX_RECENT_SEARCHES + 3}", searches.first())
        assertFalse(searches.any { it.equals("rock", ignoreCase = true) })
    }
}
