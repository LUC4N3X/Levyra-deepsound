package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleTrendingChartsTest {

    @Test
    fun parseAlbumsFiltersOutSinglesAndUpgradesArtwork() {
        val json = """
            {
              "feed": {
                "results": [
                  {
                    "id": "1",
                    "name": "The Dark Side of the Moon",
                    "artistName": "Pink Floyd",
                    "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/01/02/03/100x100bb.jpg",
                    "url": "https://music.apple.com/it/album/the-dark-side-of-the-moon/1",
                    "genres": [{"name": "Rock"}]
                  },
                  {
                    "id": "2",
                    "name": "Espresso - Single",
                    "artistName": "Sabrina Carpenter",
                    "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/04/05/06/100x100bb.jpg",
                    "url": "https://music.apple.com/it/album/espresso-single/2",
                    "genres": [{"name": "Pop"}]
                  },
                  {
                    "id": "3",
                    "name": "Single Girl [Single]",
                    "artistName": "Indie Artist",
                    "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/07/08/09/100x100bb.jpg",
                    "url": "https://music.apple.com/it/album/single-girl/3",
                    "genres": [{"name": "Alternative"}]
                  },
                  {
                    "id": "4",
                    "name": "Summer Track",
                    "artistName": "DJ Sample",
                    "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/10/11/12/100x100bb.jpg",
                    "url": "https://music.apple.com/it/album/summer-track/4",
                    "genres": [{"name": "Single"}]
                  },
                  {
                    "id": "5",
                    "name": "Short n' Sweet",
                    "artistName": "Sabrina Carpenter",
                    "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/13/14/15/100x100bb.jpg",
                    "url": "https://music.apple.com/it/album/short-n-sweet/5",
                    "genres": [{"name": "Pop"}]
                  },
                  {
                    "id": "6",
                    "name": "The Dark Side of the Moon",
                    "artistName": "Pink Floyd",
                    "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/01/02/03/100x100bb.jpg",
                    "url": "https://music.apple.com/it/album/the-dark-side-of-the-moon/6",
                    "genres": [{"name": "Rock"}]
                  }
                ]
              }
            }
        """.trimIndent()

        val albums = AppleTrendingParser.parseAlbums(json)

        // 6 items in json:
        // - id 1: The Dark Side of the Moon (kept)
        // - id 2: Espresso - Single (filtered - Single title)
        // - id 3: Single Girl [Single] (filtered - [Single] title)
        // - id 4: Summer Track (filtered - Single genre)
        // - id 5: Short n' Sweet (kept)
        // - id 6: Duplicate Pink Floyd (deduplicated)
        assertEquals(2, albums.size)

        assertEquals("The Dark Side of the Moon", albums[0].title)
        assertEquals("Pink Floyd", albums[0].artist)
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/01/02/03/1400x1400bb.jpg",
            albums[0].thumbnailUrl
        )

        assertEquals("Short n' Sweet", albums[1].title)
        assertEquals("Sabrina Carpenter", albums[1].artist)
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/13/14/15/1400x1400bb.jpg",
            albums[1].thumbnailUrl
        )
    }

    @Test
    fun extractTrendingArtistCandidatesSplitsCollaborationsAndRanksByFrequencyAndPeak() {
        val json = """
            {
              "feed": {
                "results": [
                  {
                    "id": "s1",
                    "name": "Fortnight",
                    "artistName": "Taylor Swift feat. Post Malone"
                  },
                  {
                    "id": "s2",
                    "name": "Birds of a Feather",
                    "artistName": "Billie Eilish"
                  },
                  {
                    "id": "s3",
                    "name": "I Had Some Help",
                    "artistName": "Post Malone & Morgan Wallen"
                  },
                  {
                    "id": "s4",
                    "name": "Cruel Summer",
                    "artistName": "Taylor Swift"
                  },
                  {
                    "id": "s5",
                    "name": "Not Like Us",
                    "artistName": "Kendrick Lamar"
                  },
                  {
                    "id": "s6",
                    "name": "Pour Me a Drink",
                    "artistName": "Post Malone feat. Blake Shelton"
                  }
                ]
              }
            }
        """.trimIndent()

        val candidates = AppleTrendingParser.extractTrendingArtistCandidates(json, limit = 15)

        // Post Malone appears 3 times (songs 1, 3, 6) -> 1st
        // Taylor Swift appears 2 times (songs 1, 4) -> 2nd
        // Billie Eilish appears 1 time (song 2, peak 2) -> 3rd
        // Morgan Wallen appears 1 time (song 3, peak 3) -> 4th
        // Kendrick Lamar appears 1 time (song 5, peak 5) -> 5th
        // Blake Shelton appears 1 time (song 6, peak 6) -> 6th

        assertEquals(6, candidates.size)
        assertEquals("Post Malone", candidates[0].name)
        assertEquals(3, candidates[0].appearances)
        assertEquals(1, candidates[0].peakRank)

        assertEquals("Taylor Swift", candidates[1].name)
        assertEquals(2, candidates[1].appearances)
        assertEquals(1, candidates[1].peakRank)

        assertEquals("Billie Eilish", candidates[2].name)
        assertEquals(1, candidates[2].appearances)
        assertEquals(2, candidates[2].peakRank)

        assertEquals("Morgan Wallen", candidates[3].name)
        assertEquals(1, candidates[3].appearances)
        assertEquals(3, candidates[3].peakRank)

        assertEquals("Kendrick Lamar", candidates[4].name)
        assertEquals(1, candidates[4].appearances)
        assertEquals(5, candidates[4].peakRank)

        assertEquals("Blake Shelton", candidates[5].name)
        assertEquals(1, candidates[5].appearances)
        assertEquals(6, candidates[5].peakRank)
    }

    @Test
    fun testArtworkUpgraderReplacesDimensions() {
        val standard = "https://is1-ssl.mzstatic.com/image/thumb/Music/100x100bb.jpg"
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/Music/1400x1400bb.jpg", AppleTrendingParser.upgradeArtwork(standard))

        val customSize = "https://is1-ssl.mzstatic.com/image/thumb/Music/200x200w.png"
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/Music/1400x1400bb.png", AppleTrendingParser.upgradeArtwork(customSize))

        val nonMatching = "https://example.com/artwork.jpg"
        assertEquals("https://example.com/artwork.jpg", AppleTrendingParser.upgradeArtwork(nonMatching))
    }

    @Test
    fun testIsSingleDetection() {
        assertTrue(AppleTrendingParser.isSingle("Song Title (Single)"))
        assertTrue(AppleTrendingParser.isSingle("Song Title [Single]"))
        assertTrue(AppleTrendingParser.isSingle("Song Title - Single"))
        assertTrue(AppleTrendingParser.isSingle("Normal Song", listOf("Pop", "Single")))
        assertFalse(AppleTrendingParser.isSingle("Single Life (Live)", listOf("Rock", "Album")))
        assertFalse(AppleTrendingParser.isSingle("Greatest Hits", listOf("Pop")))
    }

    @Test
    fun parseDeezerAlbumsExtractsAlbumsCorrectly() {
        val json = """
            {
              "data": [
                {
                  "id": 12345,
                  "title": "Hit Me Hard and Soft",
                  "artist": {
                    "name": "Billie Eilish"
                  },
                  "cover_xl": "https://api.deezer.com/album/12345/image_xl.jpg"
                },
                {
                  "id": 12346,
                  "title": "Single Release - Single",
                  "artist": {
                    "name": "Someone"
                  },
                  "cover_xl": "https://api.deezer.com/album/12346/image_xl.jpg"
                }
              ]
            }
        """.trimIndent()

        val albums = AppleTrendingParser.parseDeezerAlbums(json)
        assertEquals(1, albums.size)
        assertEquals("Hit Me Hard and Soft", albums[0].title)
        assertEquals("Billie Eilish", albums[0].artist)
        assertEquals("https://api.deezer.com/album/12345/image_xl.jpg", albums[0].thumbnailUrl)
    }

    @Test
    fun extractDeezerArtistCandidatesExtractsAndRanks() {
        val json = """
            {
              "data": [
                {
                  "title": "Song 1",
                  "artist": { "name": "Artist A" }
                },
                {
                  "title": "Song 2",
                  "artist": { "name": "Artist B" }
                },
                {
                  "title": "Song 3",
                  "artist": { "name": "Artist A" }
                }
              ]
            }
        """.trimIndent()

        val candidates = AppleTrendingParser.extractDeezerArtistCandidates(json, limit = 5)
        assertEquals(2, candidates.size)
        assertEquals("Artist A", candidates[0].name) // Count 2
        assertEquals(2, candidates[0].appearances)
        assertEquals("Artist B", candidates[1].name) // Count 1
        assertEquals(1, candidates[1].appearances)
    }

    @Test
    fun extractTrendingArtistCandidatesLimitsToFifteen() {
        val resultsArray = (1..25).joinToString(",") { i ->
            """{"id": "$i", "name": "Song $i", "artistName": "Artist Number $i"}"""
        }
        val json = """{"feed": {"results": [$resultsArray]}}"""
        val candidates = AppleTrendingParser.extractTrendingArtistCandidates(json, limit = 15)
        assertEquals(15, candidates.size)
    }

    @Test
    fun extractTrendingArtistCandidatesHandlesVariousCollabSeparators() {
        val json = """
            {
              "feed": {
                "results": [
                  {
                    "id": "1",
                    "name": "Track 1",
                    "artistName": "Artist One with Artist Two"
                  },
                  {
                    "id": "2",
                    "name": "Track 2",
                    "artistName": "Artist Three con Artist Four"
                  },
                  {
                    "id": "3",
                    "name": "Track 3",
                    "artistName": "Artist Five vs. Artist Six"
                  },
                  {
                    "id": "4",
                    "name": "Track 4",
                    "artistName": "Artist Seven, Artist Eight & Artist Nine"
                  }
                ]
              }
            }
        """.trimIndent()

        val candidates = AppleTrendingParser.extractTrendingArtistCandidates(json, limit = 15)
        val names = candidates.map { it.name }.toSet()
        assertTrue("Artist One" in names)
        assertTrue("Artist Two" in names)
        assertTrue("Artist Three" in names)
        assertTrue("Artist Four" in names)
        assertTrue("Artist Five" in names)
        assertTrue("Artist Six" in names)
        assertTrue("Artist Seven" in names)
        assertTrue("Artist Eight" in names)
        assertTrue("Artist Nine" in names)
        assertFalse(names.any { it.contains("with") || it.contains("con") || it.contains("vs") })
    }
}
