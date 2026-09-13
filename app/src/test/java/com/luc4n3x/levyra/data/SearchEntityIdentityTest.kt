package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEntityIdentityTest {

    @Test
    fun `same album from different endpoints collapses on canonical browse id`() {
        val merged = deduplicateSearchAlbums(
            listOf(
                album(title = "Ghost Stories", artist = "Coldplay", browseId = "MPREb_ghost"),
                album(title = "Ghost Stories Deluxe", artist = "Coldplay", browseId = "MPREb_ghost", thumbnailUrl = "https://img/2")
            )
        )

        assertEquals(1, merged.size)
        assertEquals("Ghost Stories", merged.single().title)
    }

    @Test
    fun `same album with different browse ids still collapses on metadata`() {
        val merged = deduplicateSearchAlbums(
            listOf(
                album(title = "Ghost Stories", artist = "Coldplay", browseId = "MPREb_one", thumbnailUrl = ""),
                album(title = "Ghost Stories", artist = "Coldplay", browseId = "MPREb_two", thumbnailUrl = "https://img/2")
            )
        )

        assertEquals(1, merged.size)
        assertEquals("MPREb_one", merged.single().browseId)
        assertEquals("https://img/2", merged.single().thumbnailUrl)
    }

    @Test
    fun `distinct albums by the same artist are preserved`() {
        val merged = deduplicateSearchAlbums(
            listOf(
                album(title = "Parachutes", artist = "Coldplay", browseId = "MPREb_par"),
                album(title = "X and Y", artist = "Coldplay", browseId = "MPREb_xy")
            )
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun `album merge fills missing canonical identifiers from the duplicate`() {
        val merged = deduplicateSearchAlbums(
            listOf(
                album(title = "Viva la Vida", artist = "Coldplay", browseId = ""),
                album(title = "Viva la Vida", artist = "Coldplay", browseId = "MPREb_viva", audioPlaylistId = "OLAK5uy_viva")
            )
        )

        assertEquals(1, merged.size)
        assertEquals("MPREb_viva", merged.single().browseId)
        assertEquals("OLAK5uy_viva", merged.single().audioPlaylistId)
    }

    @Test
    fun `artists collapse on canonical browse id despite different display names`() {
        val merged = deduplicateSearchArtists(
            listOf(
                artist(name = "Coldplay", browseId = "UCchannel"),
                artist(name = "COLDPLAY ", browseId = "UCchannel", thumbnailUrl = "https://img/artist")
            )
        )

        assertEquals(1, merged.size)
        assertEquals("https://img/artist", merged.single().thumbnailUrl)
    }

    @Test
    fun `different artists with different browse ids are preserved`() {
        val merged = deduplicateSearchArtists(
            listOf(
                artist(name = "Coldplay", browseId = "UCone"),
                artist(name = "Radiohead", browseId = "UCtwo")
            )
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun `playlists collapse on playlist id`() {
        val merged = deduplicateSearchPlaylists(
            listOf(
                playlist(title = "Chill Hits", playlistId = "PL123"),
                playlist(title = "Chill Hits Official", playlistId = "PL123", thumbnailUrl = "https://img/pl")
            )
        )

        assertEquals(1, merged.size)
        assertEquals("https://img/pl", merged.single().thumbnailUrl)
    }

    @Test
    fun `songs collapse on video id and keep the richer record`() {
        val merged = deduplicateSearchSongs(
            listOf(
                track(id = "abc", title = "Yellow", album = ""),
                track(id = "abc", title = "Yellow", album = "Parachutes", durationMs = 267_000L)
            )
        )

        assertEquals(1, merged.size)
        assertEquals("Parachutes", merged.single().album)
        assertEquals(267_000L, merged.single().durationMs)
    }

    @Test
    fun `continuation merge appends new items without duplicating the first page`() {
        val firstPage = listOf(track(id = "a"), track(id = "b"))
        val secondPage = listOf(track(id = "b"), track(id = "c"))

        val merged = mergeSearchSongs(firstPage, secondPage)

        assertEquals(listOf("a", "b", "c"), merged.map { it.id })
    }

    @Test
    fun `same recording with different video ids collapses on song metadata`() {
        val merged = mergeSearchSongs(
            listOf(
                track(
                    id = "video-a",
                    title = "Blinding Lights",
                    artist = "The Weeknd",
                    durationMs = 200_000L,
                    youtubeViewCount = 3_000_000_000L
                )
            ),
            listOf(
                track(
                    id = "video-b",
                    title = "Blinding Lights",
                    artist = "The Weeknd",
                    durationMs = 200_000L,
                    youtubeViewCount = 3_600_000_000L
                )
            )
        )

        assertEquals(1, merged.size)
        assertEquals("video-a", merged.single().id)
        assertEquals(3_600_000_000L, merged.single().youtubeViewCount)
    }

    @Test
    fun `different song variants remain separate`() {
        val merged = deduplicateSearchSongs(
            listOf(
                track(id = "studio", title = "Blinding Lights", artist = "The Weeknd"),
                track(id = "live", title = "Blinding Lights Live", artist = "The Weeknd")
            )
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun `top result selects three tracks from the hero artist only`() {
        val hero = track(id = "hero", title = "After Hours", artist = "The Weeknd")
        val selected = selectSearchTopResultTracks(
            topTrack = hero,
            songs = listOf(
                hero,
                track(id = "one", title = "Blinding Lights", artist = "The Weeknd"),
                track(id = "wrong", title = "Creepin", artist = "Metro Boomin"),
                track(id = "two", title = "Save Your Tears", artist = "The Weeknd"),
                track(id = "three", title = "The Hills", artist = "The Weeknd")
            )
        )

        assertEquals(listOf("hero", "one", "two"), selected.map { it.id })
    }

    @Test
    fun `songs shelf excludes metadata duplicate of top result`() {
        val hero = track(
            id = "hero-video",
            title = "Blinding Lights",
            artist = "The Weeknd",
            durationMs = 200_000L
        )
        val alternateVideo = track(
            id = "alternate-video",
            title = "Blinding Lights",
            artist = "The Weeknd",
            durationMs = 200_000L
        )
        val other = track(
            id = "other",
            title = "Save Your Tears",
            artist = "The Weeknd",
            durationMs = 215_000L
        )

        val filtered = filterSearchSongsExcludingTopResult(
            songs = listOf(alternateVideo, other),
            topResultTracks = listOf(hero)
        )

        assertEquals(listOf("other"), filtered.map { it.id })
    }

    @Test
    fun `songs shelf keeps alternate id when recording metadata is incomplete`() {
        val hero = track(id = "hero-video", title = "Unknown", artist = "Artist", durationMs = 0L)
        val alternateVideo = track(id = "alternate-video", title = "Unknown", artist = "Artist", durationMs = 0L)

        val filtered = filterSearchSongsExcludingTopResult(
            songs = listOf(alternateVideo),
            topResultTracks = listOf(hero)
        )

        assertEquals(listOf("alternate-video"), filtered.map { it.id })
    }

    @Test
    fun `top result does not invent related tracks when hero artist is missing`() {
        val hero = track(id = "hero", title = "Unknown", artist = "")
        val selected = selectSearchTopResultTracks(
            topTrack = hero,
            songs = listOf(hero, track(id = "other", title = "Other", artist = "Someone"))
        )

        assertEquals(listOf("hero"), selected.map { it.id })
    }

    @Test
    fun `music video type classification separates songs from videos`() {
        assertFalse(isMusicVideoResult("MUSIC_VIDEO_TYPE_ATV"))
        assertFalse(isMusicVideoResult(""))
        assertTrue(isMusicVideoResult("MUSIC_VIDEO_TYPE_OMV"))
        assertTrue(isMusicVideoResult("MUSIC_VIDEO_TYPE_UGC"))
    }

    @Test
    fun `albums without canonical ids stay separate when metadata differs`() {
        val merged = deduplicateSearchAlbums(
            listOf(
                album(title = "Parachutes", artist = "Coldplay", browseId = ""),
                album(title = "Kid A", artist = "Radiohead", browseId = "")
            )
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun `indistinguishable malformed entries collapse instead of duplicating the shelf`() {
        val merged = deduplicateSearchAlbums(
            listOf(
                album(title = "", artist = "", browseId = ""),
                album(title = "", artist = "", browseId = "")
            )
        )

        assertEquals(1, merged.size)
    }

    @Test
    fun `an echoed continuation token ends pagination instead of refetching page one`() {
        assertEquals("", nextSearchContinuation(requested = "TOKEN_A", returned = "TOKEN_A"))
        assertEquals("", nextSearchContinuation(requested = "TOKEN_A", returned = " TOKEN_A "))
        assertEquals("TOKEN_B", nextSearchContinuation(requested = "TOKEN_A", returned = "TOKEN_B"))
    }

    @Test
    fun `a missing continuation token ends pagination`() {
        assertEquals("", nextSearchContinuation(requested = "TOKEN_A", returned = ""))
        assertEquals("", nextSearchContinuation(requested = "", returned = "   "))
    }

    @Test
    fun `the first page accepts the token it did not request`() {
        assertEquals("TOKEN_A", nextSearchContinuation(requested = "", returned = "TOKEN_A"))
    }

    @Test
    fun `empty input stays empty`() {
        assertTrue(deduplicateSearchAlbums(emptyList()).isEmpty())
        assertTrue(deduplicateSearchArtists(emptyList()).isEmpty())
        assertTrue(deduplicateSearchPlaylists(emptyList()).isEmpty())
        assertTrue(deduplicateSearchSongs(emptyList()).isEmpty())
    }

    @Test
    fun `findVerifiedTopResultArtist matches exact artist name`() {
        val hero = track(id = "s1", title = "Tran Tran", artist = "Sfera Ebbasta")
        val candidates = listOf(
            artist(name = "Sfera Ebbasta", browseId = "UC_sfera"),
            artist(name = "Baby Gang", browseId = "UC_baby")
        )

        val result = findVerifiedTopResultArtist(candidates, hero, query = "Sfera Ebbasta")

        assertNotNull(result)
        assertEquals("UC_sfera", result?.browseId)
        assertEquals("Sfera Ebbasta", result?.name)
    }

    @Test
    fun `findVerifiedTopResultArtist matches when query is prefix and candidate matches hero track`() {
        val hero = track(id = "s1", title = "Tran Tran", artist = "Sfera Ebbasta")
        val candidates = listOf(
            artist(name = "Sfera Ebbasta", browseId = "UC_sfera")
        )

        val result = findVerifiedTopResultArtist(candidates, hero, query = "sfera")

        assertNotNull(result)
        assertEquals("Sfera Ebbasta", result?.name)
    }

    @Test
    fun `findVerifiedTopResultArtist matches featured artist when query explicitly targets candidate`() {
        val hero = track(id = "s1", title = "Calcolatrici", artist = "Geolier feat. Sfera Ebbasta & Guè")
        val candidates = listOf(
            artist(name = "Geolier", browseId = "UC_geolier"),
            artist(name = "Sfera Ebbasta", browseId = "UC_sfera")
        )

        val result = findVerifiedTopResultArtist(candidates, hero, query = "sfera")

        assertNotNull(result)
        assertEquals("UC_sfera", result?.browseId)
        assertEquals("Sfera Ebbasta", result?.name)
    }

    @Test
    fun `findVerifiedTopResultArtist returns null when query is song title and candidate is not connected to hero track`() {
        val hero = track(id = "s1", title = "Tran Tran", artist = "Sfera Ebbasta")
        val candidates = listOf(
            artist(name = "Tran Tran", browseId = "UC_other_band")
        )

        val result = findVerifiedTopResultArtist(candidates, hero, query = "Tran Tran")

        assertNull(result)
    }

    @Test
    fun `findVerifiedTopResultArtist returns null when no candidate matches hero track and never falls back to first`() {
        val hero = track(id = "s1", title = "Tran Tran", artist = "Sfera Ebbasta")
        val candidates = listOf(
            artist(name = "Baby Gang", browseId = "UC_baby"),
            artist(name = "Tedua", browseId = "UC_tedua")
        )

        val result = findVerifiedTopResultArtist(candidates, hero, query = "sfera")

        assertNull(result)
    }

    @Test
    fun `findVerifiedTopResultArtist matches on browse id even if artist name has alias`() {
        val hero = track(
            id = "s1",
            title = "Song",
            artist = "Gionata Boschetti",
            artistBrowseIds = listOf("UC_sfera")
        )
        val candidates = listOf(
            artist(name = "Sfera Ebbasta", browseId = "UC_sfera")
        )

        val result = findVerifiedTopResultArtist(candidates, hero, query = "sfera")

        assertNotNull(result)
        assertEquals("UC_sfera", result?.browseId)
    }

    @Test
    fun `deduplicateSearchSongs guarantees hero track is present in playback context for mix`() {
        val hero = track(id = "hero_1", title = "Hero Song", artist = "Artist A")
        val otherSongs = listOf(
            track(id = "song_2", title = "Song Two", artist = "Artist B"),
            track(id = "song_3", title = "Song Three", artist = "Artist C")
        )

        val playbackContext = deduplicateSearchSongs(listOf(hero) + otherSongs)

        assertEquals(3, playbackContext.size)
        assertEquals("hero_1", playbackContext.first().id)
        val heroIndex = playbackContext.indexOfFirst { it.id == hero.id }
        assertEquals(0, heroIndex)
    }

    @Test
    fun `deduplicateSearchSongs collapses duplicates when hero is already in song list`() {
        val hero = track(id = "hero_1", title = "Hero Song", artist = "Artist A", durationMs = 210_000L)
        val otherSongs = listOf(
            track(id = "hero_1", title = "Hero Song", artist = "Artist A", durationMs = 0L),
            track(id = "song_2", title = "Song Two", artist = "Artist B")
        )

        val playbackContext = deduplicateSearchSongs(listOf(hero) + otherSongs)

        assertEquals(2, playbackContext.size)
        assertEquals("hero_1", playbackContext.first().id)
        assertEquals(210_000L, playbackContext.first().durationMs)
    }

    private fun album(
        title: String,
        artist: String,
        browseId: String,
        audioPlaylistId: String = "",
        thumbnailUrl: String = "https://img/1"
    ) = AlbumHit(
        title = title,
        artist = artist,
        year = "",
        thumbnailUrl = thumbnailUrl,
        query = "$title $artist",
        browseId = browseId,
        audioPlaylistId = audioPlaylistId,
        releaseType = ReleaseType.Album
    )

    private fun artist(
        name: String,
        browseId: String,
        thumbnailUrl: String = ""
    ) = ArtistHit(
        name = name,
        subscribers = "",
        thumbnailUrl = thumbnailUrl,
        accentStart = 0,
        accentEnd = 0,
        browseId = browseId
    )

    private fun playlist(
        title: String,
        playlistId: String,
        thumbnailUrl: String = ""
    ) = PlaylistHit(
        title = title,
        author = "YouTube Music",
        thumbnailUrl = thumbnailUrl,
        playlistId = playlistId
    )

    private fun track(
        id: String,
        title: String = "Song",
        album: String = "Album",
        durationMs: Long = 0L,
        artist: String = "Coldplay",
        youtubeViewCount: Long = -1L,
        artistBrowseIds: List<String> = emptyList()
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        youtubeViewCount = youtubeViewCount,
        artistBrowseIds = artistBrowseIds
    )
}
