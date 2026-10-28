package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.domain.LibrarySort
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryScrollbarLabelsTest {

    @Test
    fun `songs are labelled by the active alphabetical sort`() {
        val songs = listOf(track("1", "Zeta", "Adele", "Hello"), track("2", "alpha", "Mina", "Ritorno"))
        val byTitle = labels(LibraryCategory.Songs, LibrarySort.Title, tracks = songs)
        assertEquals("Z", byTitle["song-${libraryTrackKey(songs[0])}"])
        assertEquals("A", byTitle["song-${libraryTrackKey(songs[1])}"])
        val byArtist = labels(LibraryCategory.Songs, LibrarySort.Artist, tracks = songs)
        assertEquals("A", byArtist["song-${libraryTrackKey(songs[0])}"])
        assertEquals("M", byArtist["song-${libraryTrackKey(songs[1])}"])
        val byAlbum = labels(LibraryCategory.Songs, LibrarySort.Album, tracks = songs)
        assertEquals("R", byAlbum["song-${libraryTrackKey(songs[1])}"])
    }

    @Test
    fun `time based sorts show no misleading letter`() {
        val songs = listOf(track("1", "Zeta", "Adele", "Hello"))
        assertTrue(labels(LibraryCategory.Songs, LibrarySort.Recent, tracks = songs).isEmpty())
        assertTrue(labels(LibraryCategory.Songs, LibrarySort.Duration, tracks = songs).isEmpty())
    }

    @Test
    fun `grid rows reuse the lazy keys of the library screen`() {
        val artists = listOf(artist("a", "Björk"), artist("b", "Cher"), artist("c", "2Cellos"))
        val result = labels(LibraryCategory.Artists, LibrarySort.Title, artists = artists)
        assertEquals("B", result["artist-a"])
        assertEquals("B", result["artist-grid-row-a-b"])
        assertEquals("#", result["artist-grid-row-c"])
        val playlists = listOf(playlist("p1", "Road trip"), playlist("p2", "chill"))
        val playlistLabels = labels(LibraryCategory.Playlists, LibrarySort.Title, playlists = playlists)
        assertEquals("R", playlistLabels["playlist-p1"])
        assertEquals("R", playlistLabels["playlist-grid-row-p1-p2"])
    }

    @Test
    fun `albums follow title or artist sort`() {
        val albums = listOf(album("k1", "Thriller", "Michael Jackson"))
        assertEquals("T", labels(LibraryCategory.Albums, LibrarySort.Title, albums = albums)["album-k1"])
        assertEquals("M", labels(LibraryCategory.Albums, LibrarySort.Artist, albums = albums)["album-k1"])
    }

    @Test
    fun `device library keeps its own alphabet rail`() {
        assertFalse(LibraryCategory.Device in LibraryScrollbarCategories)
        assertFalse(LibraryCategory.Overview in LibraryScrollbarCategories)
        assertTrue(labels(LibraryCategory.Device, LibrarySort.Title, tracks = listOf(track("1", "A", "B", "C"))).isEmpty())
    }

    private fun labels(
        category: LibraryCategory,
        sort: LibrarySort,
        tracks: List<Track> = emptyList(),
        albums: List<LibraryAlbum> = emptyList(),
        artists: List<LibraryArtist> = emptyList(),
        playlists: List<Playlist> = emptyList()
    ) = libraryScrollbarLabels(category, sort, tracks, albums, artists, playlists, emptyList())

    private fun artist(key: String, name: String) = LibraryArtist(key, name, "", "", 0L, emptyList())

    private fun album(key: String, title: String, artist: String) =
        LibraryAlbum(key, title, artist, "", "", "", false, emptyList())

    private fun playlist(id: String, name: String) = Playlist(id, name, "", emptyList(), 0L, 0L)

    private fun track(id: String, title: String, artist: String, album: String) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 1_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
