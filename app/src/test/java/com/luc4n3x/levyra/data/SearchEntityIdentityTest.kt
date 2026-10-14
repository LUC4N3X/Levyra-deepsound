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
    fun sameRecordingSpotifyDifferentAlbumKeepsDistinctSearchCards() {
        val original = track(id = "spotify:original-album", title = "Song", artist = "Artist",
            album = "Original album", durationMs = 180_000L).copy(
            source = "spotify", metadataProvider = "spotify", isrc = "USABC2600001"
        )
        val compilation = original.copy(id = "spotify:compilation-album", album = "Compilation album")

        val merged = mergeSearchSongs(listOf(original), listOf(compilation))

        assertEquals(listOf(original.id, compilation.id), merged.map { it.id })
        assertEquals(listOf(original.album, compilation.album), merged.map { it.album })
        assertEquals(listOf(compilation), filterSearchSongsExcludingTopResult(merged, listOf(original)))
    }

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
    fun `local recording keeps local identity when merged with remote duplicate`() {
        val local = track(
            id = "local:yellow",
            title = "Yellow",
            artist = "Coldplay",
            durationMs = 240_000L
        ).copy(
            streamUrl = "content://media/external/audio/42",
            source = "Offline"
        )
        val remote = track(
            id = "AbCdEf12345",
            title = "Yellow",
            artist = "Coldplay",
            durationMs = 240_000L
        ).copy(
            videoUrl = "https://www.youtube.com/watch?v=AbCdEf12345",
            source = "YouTube Music"
        )

        val localFirst = mergeSearchSongs(listOf(local), listOf(remote)).single()
        val localSecond = mergeSearchSongs(listOf(remote), listOf(local)).single()

        listOf(localFirst, localSecond).forEach { merged ->
            assertEquals("local:yellow", merged.id)
            assertEquals("Offline", merged.source)
            assertEquals("content://media/external/audio/42", merged.streamUrl)
        }
    }



    @Test
    fun `local recording keeps offline source when merged with spotify metadata`() {
        val local = track(
            id = "local:spotify-yellow",
            title = "Yellow",
            artist = "Coldplay",
            durationMs = 240_000L
        ).copy(
            streamUrl = "content://media/external/audio/99",
            source = "Offline"
        )
        val spotify = track(
            id = "spotify:yellow",
            title = "Yellow",
            artist = "Coldplay",
            durationMs = 240_000L
        ).copy(
            videoUrl = "",
            source = "spotify",
            metadataProvider = "spotify"
        )

        val spotifyFirst = mergeSearchSongs(listOf(spotify), listOf(local)).single()
        val localFirst = mergeSearchSongs(listOf(local), listOf(spotify)).single()

        listOf(spotifyFirst, localFirst).forEach { merged ->
            assertEquals("local:spotify-yellow", merged.id)
            assertEquals("Offline", merged.source)
            assertEquals("content://media/external/audio/99", merged.streamUrl)
            assertEquals(local.metadataProvider, merged.metadataProvider)
        }
    }


    @Test
    fun `one recording shows once with its official video as counterpart`() {
        val cover = "https://i.scdn.co/image/ab67616d00001e02e2f039481babe23658fc719a"
        val spotify = track(
            id = "spotify:in-the-end",
            title = "In the End",
            artist = "Linkin Park",
            album = "Hybrid Theory (Bonus Edition)",
            durationMs = 216_000L
        ).copy(source = "spotify", metadataProvider = "spotify", thumbnailUrl = cover, largeThumbnailUrl = cover)
        val song = track(id = "BLZWkjBXfN8", title = "In the End", artist = "Linkin Park", album = "", durationMs = 216_000L,
            youtubeViewCount = 3_300_000_000L)
            .copy(videoType = "MUSIC_VIDEO_TYPE_ATV", thumbnailUrl = "https://yt3.googleusercontent.com/song")
        fun video(id: String, title: String, artist: String = "Linkin Park", durationMs: Long = 0L, type: String = "MUSIC_VIDEO_TYPE_OMV") =
            track(id = id, title = title, artist = artist, album = "", durationMs = durationMs)
                .copy(videoType = type, thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg")
        val official = video("eVTXPUF4Oz4", "In the End")
        val reupload = video("w3xqsMv6T_k", "In the End", durationMs = 215_000L, type = "MUSIC_VIDEO_TYPE_UGC")
        val live = video("pQto8npSUC0", "In the End (Live In Texas)", durationMs = 234_000L)
        val remix = video("WNeLUngb-Xg", "Linkin Park - In The End (Mellen Gi & Tommee Profitt Remix)",
            artist = "TrapMusicHDTV", durationMs = 219_000L, type = "MUSIC_VIDEO_TYPE_UGC")
        val other = video("dbHi7vyRXbo", "In The End", artist = "Twin", type = "MUSIC_VIDEO_TYPE_ATV")

        listOf(
            mergeSearchSongs(listOf(spotify), listOf(official, reupload, song, live, remix, other)),
            mergeSearchSongs(listOf(official, reupload, song, live, remix, other), listOf(spotify))
        ).forEach { merged ->
            val recording = merged.filter { it.title.equals("In the End", ignoreCase = true) && it.artist == "Linkin Park" }
            assertEquals(1, recording.size)
            val entry = recording.single()
            assertEquals("BLZWkjBXfN8", entry.id)
            assertEquals("BLZWkjBXfN8", entry.audioVideoId)
            assertEquals("eVTXPUF4Oz4", entry.counterpartVideoId)
            assertEquals(cover, entry.thumbnailUrl)
            assertEquals(3_300_000_000L, entry.youtubeViewCount)
            assertEquals(0, merged.indexOf(entry))
            assertTrue(merged.any { it.id == "pQto8npSUC0" })
            assertTrue(merged.any { it.id == "WNeLUngb-Xg" })
            assertTrue(merged.any { it.id == "dbHi7vyRXbo" })
            assertEquals(4, merged.size)
        }
    }

    @Test
    fun `youtube only results collapse the official video into the song`() {
        val song = track(id = "BLZWkjBXfN8", title = "In the End", artist = "Linkin Park", album = "Hybrid Theory", durationMs = 216_000L)
            .copy(videoType = "MUSIC_VIDEO_TYPE_ATV", thumbnailUrl = "https://yt3.googleusercontent.com/song")
        val official = track(id = "eVTXPUF4Oz4", title = "In the End", artist = "Linkin Park", album = "")
            .copy(videoType = "MUSIC_VIDEO_TYPE_OMV", thumbnailUrl = "https://i.ytimg.com/vi/eVTXPUF4Oz4/hqdefault.jpg")

        val merged = mergeSearchSongs(listOf(official), listOf(song)).single()

        assertEquals("BLZWkjBXfN8", merged.id)
        assertEquals("eVTXPUF4Oz4", merged.counterpartVideoId)
        assertEquals(song.thumbnailUrl, merged.thumbnailUrl)
    }

    @Test
    fun `seed top result resolves to the playable recording and lists its versions next`() {
        val seed = track(id = "chart-seed-76b15b01f1fd9ee9", title = "In The End", artist = "Linkin Park", album = "Hybrid Theory")
            .copy(source = "Levyra Start", thumbnailUrl = "https://yt3.googleusercontent.com/seed")
        val unified = track(id = "levyra-08735e6eba50b46580da", title = "In the End", artist = "Linkin Park",
            album = "Hybrid Theory (Bonus Edition)", durationMs = 216_880L)
            .copy(source = "spotify", metadataProvider = "spotify", videoType = "MUSIC_VIDEO_TYPE_ATV",
                audioVideoId = "BLZWkjBXfN8", thumbnailUrl = "https://i.scdn.co/image/hybrid")
        val live = track(id = "bnecjgMM8IE", title = "In the End (Live)", artist = "Linkin Park", album = "One More Light (Live)")
            .copy(videoType = "MUSIC_VIDEO_TYPE_ATV")
        val givenUp = track(id = "D93vYwuMeUw", title = "Given Up", artist = "Linkin Park", album = "")
            .copy(videoType = "MUSIC_VIDEO_TYPE_ATV")
        val songs = listOf(unified, live, givenUp)

        val top = selectSearchTopResultTracks(seed, songs)

        assertEquals(listOf("levyra-08735e6eba50b46580da", "bnecjgMM8IE", "D93vYwuMeUw"), top.map { it.id })
        assertTrue(filterSearchSongsExcludingTopResult(songs, top).isEmpty())
    }

    @Test
    fun `seed top result keeps the youtube play count of the unified recording`() {
        val seed = track(id = "chart-seed-76b15b01f1fd9ee9", title = "In The End", artist = "Linkin Park", album = "Hybrid Theory")
            .copy(source = "Levyra Start", videoUrl = "https://www.youtube.com/watch?v=eVTXPUF4Oz4",
                thumbnailUrl = "https://i.ytimg.com/vi/eVTXPUF4Oz4/hqdefault.jpg")
        val editorial = track(id = "levyra-08735e6eba50b46580da", title = "In the End", artist = "Linkin Park",
            album = "Hybrid Theory (Bonus Edition)", durationMs = 216_880L)
            .copy(source = "spotify", metadataProvider = "spotify", videoType = "MUSIC_VIDEO_TYPE_ATV",
                audioVideoId = "BLZWkjBXfN8", thumbnailUrl = "https://i.scdn.co/image/hybrid")
        val youtube = track(id = "BLZWkjBXfN8", title = "In the End", artist = "Linkin Park",
            album = "Hybrid Theory (Bonus Edition)", youtubeViewCount = 3_300_000_000L)
            .copy(videoType = "MUSIC_VIDEO_TYPE_ATV")
        val live = track(id = "bnecjgMM8IE", title = "In the End (Live)", artist = "Linkin Park", album = "One More Light (Live)")
            .copy(videoType = "MUSIC_VIDEO_TYPE_ATV")

        listOf(
            mergeSearchSongs(listOf(editorial), listOf(youtube, live)),
            mergeSearchSongs(listOf(youtube, live), listOf(editorial))
        ).forEach { songs ->
            val hero = selectSearchTopResultTracks(seed, songs).first()
            assertEquals(3_300_000_000L, hero.youtubeViewCount)
            assertEquals("BLZWkjBXfN8", hero.audioVideoId.ifBlank { hero.id })
            assertEquals("https://i.scdn.co/image/hybrid", hero.thumbnailUrl)
            assertEquals(1, songs.count { it.title.equals("In the End", ignoreCase = true) })
        }
    }

    @Test
    fun `seed top result never resolves to another album recording`() {
        val seed = track(id = "chart-seed-1", title = "Song", artist = "Artist", album = "Original Album")
            .copy(source = "Levyra Start")
        val compilation = track(id = "ytCompil001", title = "Song", artist = "Artist", album = "Summer Hits 2001")
            .copy(videoType = "MUSIC_VIDEO_TYPE_ATV")

        assertEquals("chart-seed-1", selectSearchTopResultTracks(seed, listOf(compilation)).first().id)
    }

    @Test
    fun `top result video resolves to the single song entry`() {
        val cover = "https://i.scdn.co/image/ab67616d00001e02e2f039481babe23658fc719a"
        val spotify = track(id = "spotify:in-the-end", title = "In the End", artist = "Linkin Park",
            album = "Hybrid Theory (Bonus Edition)", durationMs = 216_000L)
            .copy(source = "spotify", metadataProvider = "spotify", thumbnailUrl = cover, largeThumbnailUrl = cover)
        val song = track(id = "BLZWkjBXfN8", title = "In the End", artist = "Linkin Park", album = "", durationMs = 216_000L)
            .copy(videoType = "MUSIC_VIDEO_TYPE_ATV")
        val official = track(id = "eVTXPUF4Oz4", title = "In the End", artist = "Linkin Park", album = "")
            .copy(videoType = "MUSIC_VIDEO_TYPE_OMV", thumbnailUrl = "https://i.ytimg.com/vi/eVTXPUF4Oz4/hqdefault.jpg")
        val songs = mergeSearchSongs(listOf(spotify), listOf(song))

        val top = selectSearchTopResultTracks(official, songs)

        assertEquals(1, top.count { it.title.equals("In the End", ignoreCase = true) })
        assertEquals(cover, top.first().thumbnailUrl)
        assertEquals("eVTXPUF4Oz4", top.first().counterpartVideoId)
        assertTrue(filterSearchSongsExcludingTopResult(songs, top).none { it.title.equals("In the End", ignoreCase = true) })
    }

    @Test
    fun `spotify records stay spotify until a youtube match exists`() {
        val first = track(
            id = "spotify:yellow",
            title = "Yellow",
            artist = "Coldplay",
            durationMs = 240_000L
        ).copy(
            source = "spotify",
            metadataProvider = "spotify"
        )
        val second = first.copy(thumbnailUrl = "https://img/spotify-yellow")

        val merged = mergeSearchSongs(listOf(first), listOf(second)).single()

        assertEquals("spotify", merged.source)
        assertTrue(merged.counterpartVideoId.isBlank())
        assertTrue(merged.audioVideoId.isBlank())
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
    fun `top result collapses the same visible recording across providers even when durations disagree`() {
        val spotify = track(
            id = "spotify:da-dio",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 173_000L
        ).copy(
            source = "spotify",
            metadataProvider = "spotify",
            counterpartVideoId = "AbCdEf12345"
        )
        val youtube = track(
            id = "AbCdEf12345",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 250_000L,
            youtubeViewCount = 934_000L
        )
        val alternate = track(
            id = "LiveAlt1234",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 310_000L,
            youtubeViewCount = 120_000L
        )
        val other = track(
            id = "ZyXwVu98765",
            title = "Introvabile",
            artist = "Bresh",
            durationMs = 190_000L,
            youtubeViewCount = 4_300_000L
        )

        val selected = selectSearchTopResultTracks(
            topTrack = spotify,
            songs = listOf(spotify, youtube, alternate, other)
        )

        assertEquals(listOf("AbCdEf12345", "ZyXwVu98765"), selected.map { it.id })
        assertEquals(934_000L, selected.first().youtubeViewCount)
    }

    @Test
    fun `songs shelf excludes same visible recording already used by top result despite duration drift`() {
        val hero = track(
            id = "spotify:da-dio",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 173_000L
        ).copy(
            source = "spotify",
            metadataProvider = "spotify",
            counterpartVideoId = "DupDaDio123"
        )
        val duplicate = track(
            id = "DupDaDio123",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 250_000L
        )
        val alternate = track(
            id = "AltDaDio123",
            title = "Da Dio",
            artist = "Bresh",
            durationMs = 310_000L
        )
        val other = track(
            id = "other-song",
            title = "Introvabile",
            artist = "Bresh",
            durationMs = 190_000L
        )

        val filtered = filterSearchSongsExcludingTopResult(
            songs = listOf(duplicate, alternate, other),
            topResultTracks = listOf(hero)
        )

        assertEquals(listOf("AltDaDio123", "other-song"), filtered.map { it.id })
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
