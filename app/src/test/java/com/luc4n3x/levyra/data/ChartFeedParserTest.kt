package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartFeedParserTest {

    @Test
    fun modernFeedKeepsChartOrderAndUpgradesArtwork() {
        val tracks = ChartFeedParser.modern(modernFeed, limit = 50)

        assertEquals(2, tracks.size)
        assertEquals("Prima Canzone", tracks[0].title)
        assertEquals("Artista Uno", tracks[0].artist)
        assertEquals("https://cdn.example/a/600x600bb.jpg", tracks[0].thumbnailUrl)
        assertEquals("https://cdn.example/a/600x600bb.jpg", tracks[0].largeThumbnailUrl)
        assertEquals("Seconda Canzone", tracks[1].title)
    }

    @Test
    fun classicFeedUsesTheLargestImageVariant() {
        val tracks = ChartFeedParser.classic(classicFeed, limit = 50)

        assertEquals(1, tracks.size)
        assertEquals("Classica", tracks[0].title)
        assertEquals("Artista Due", tracks[0].artist)
        assertEquals("https://cdn.example/c/600x600bb.jpg", tracks[0].thumbnailUrl)
    }

    @Test
    fun entriesWithoutATitleAreSkipped() {
        val tracks = ChartFeedParser.modern(modernFeedWithBlankTitle, limit = 50)

        assertEquals(1, tracks.size)
        assertEquals("Solo Questa", tracks[0].title)
    }

    @Test
    fun limitCapsTheParsedEntries() {
        val tracks = ChartFeedParser.modern(modernFeed, limit = 1)

        assertEquals(1, tracks.size)
        assertEquals("Prima Canzone", tracks[0].title)
    }

    @Test
    fun malformedPayloadsParseAsEmptyInsteadOfThrowing() {
        assertTrue(ChartFeedParser.modern("not json", limit = 50).isEmpty())
        assertTrue(ChartFeedParser.classic("", limit = 50).isEmpty())
        assertTrue(ChartFeedParser.modern("""{"feed":{}}""", limit = 50).isEmpty())
    }

    @Test
    fun identityStaysStableAcrossFeedsAndVariesByTrack() {
        val fromModern = ChartFeedParser.modern(modernFeed, limit = 50)
        val fromClassic = ChartFeedParser.classic(sameTrackAsClassicFeed, limit = 50)

        assertEquals(fromModern[0].id, fromClassic[0].id)
        assertNotEquals(fromModern[0].id, fromModern[1].id)
        assertTrue(fromModern[0].id.startsWith("chart-"))
    }

    @Test
    fun identityMatchesThePersistedChartIdFormat() {
        // Chart ids are stored in favourites, playlists and home snapshots, so the derivation is
        // pinned here: SHA-256("title|artist") truncated to the first eight bytes.
        val tracks = ChartFeedParser.modern(modernFeed, limit = 1)

        assertEquals("chart-${sha256Prefix("Prima Canzone|Artista Uno")}", tracks[0].id)
    }

    @Test
    fun missingArtistFallsBackToTheGenericLabel() {
        val tracks = ChartFeedParser.modern(modernFeedWithoutArtist, limit = 50)

        assertEquals("Vari artisti", tracks[0].artist)
    }

    @Test
    fun normalizeMusicTextDropsDecorations() {
        assertEquals("titolo", ChartFeedParser.normalizeMusicText("Titolo (Official Video)"))
        assertEquals("titolo canzone", ChartFeedParser.normalizeMusicText("Titolo   Canzone!"))
        assertEquals("titolo qualcuno", ChartFeedParser.normalizeMusicText("Titolo feat. Qualcuno"))
    }

    private fun sha256Prefix(value: String): String {
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }

    private val modernFeed = """
        {
          "feed": {
            "results": [
              {"name": "Prima Canzone", "artistName": "Artista Uno", "artworkUrl100": "https://cdn.example/a/100x100bb.jpg"},
              {"name": "Seconda Canzone", "artistName": "Artista Due", "artworkUrl100": "https://cdn.example/b/100x100bb.jpg"}
            ]
          }
        }
    """.trimIndent()

    private val modernFeedWithBlankTitle = """
        {
          "feed": {
            "results": [
              {"name": "   ", "artistName": "Nessuno", "artworkUrl100": "https://cdn.example/x/100x100bb.jpg"},
              {"name": "Solo Questa", "artistName": "Artista", "artworkUrl100": "https://cdn.example/y/100x100bb.jpg"}
            ]
          }
        }
    """.trimIndent()

    private val modernFeedWithoutArtist = """
        {
          "feed": {
            "results": [
              {"name": "Senza Artista", "artistName": "", "artworkUrl100": "https://cdn.example/z/100x100bb.jpg"}
            ]
          }
        }
    """.trimIndent()

    private val classicFeed = """
        {
          "feed": {
            "entry": [
              {
                "im:name": {"label": "Classica"},
                "im:artist": {"label": "Artista Due"},
                "im:image": [
                  {"label": "https://cdn.example/c/55x55bb.jpg"},
                  {"label": "https://cdn.example/c/170x170bb.jpg"}
                ]
              }
            ]
          }
        }
    """.trimIndent()

    private val sameTrackAsClassicFeed = """
        {
          "feed": {
            "entry": [
              {
                "im:name": {"label": "Prima Canzone"},
                "im:artist": {"label": "Artista Uno"},
                "im:image": [{"label": "https://cdn.example/a/170x170bb.jpg"}]
              }
            ]
          }
        }
    """.trimIndent()
}
