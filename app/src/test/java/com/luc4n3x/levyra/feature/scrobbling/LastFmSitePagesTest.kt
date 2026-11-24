package com.luc4n3x.levyra.feature.scrobbling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LastFmSitePagesTest {
    @Test
    fun parsesPlaylistsFromProfilePage() {
        val html = """
            <h3 class="playlisting-playlists-item-name" itemprop="name"><a
                href="/user/RJ/playlists/12088860"
                    title="Sounds Like The Signal"
                    class="link-block-target"
            >Sounds Like The Signal</a></h3>
            <p class="playlisting-playlists-item-aux-text playlisting-playlists-item-entry-count">
                30 tracks
            </p>
            <span class="playlist-image playlisting-playlists-item-image">
                <img src="https://lastfm.freetls.fastly.net/i/u/300x300/8e9c.jpg"
            </span>
            <a href="/user/RJ/playlists/12088860/export">Export</a>
            <a href="/user/RJ/playlists/12088860">Open</a>
            <h3 class="playlisting-playlists-item-name"><a
                href="/user/RJ/playlists/2615079"
                    title="Dust &amp; Gold &#39;70s"
            >x</a></h3>
            <p class="playlisting-playlists-item-entry-count">1,204 tracks</p>
        """.trimIndent()

        val playlists = LastFmSitePages.playlists(html)

        assertEquals(listOf("12088860", "2615079"), playlists.map { it.id })
        assertEquals("Sounds Like The Signal", playlists[0].title)
        assertEquals(30, playlists[0].trackCount)
        assertEquals("https://lastfm.freetls.fastly.net/i/u/300x300/8e9c.jpg", playlists[0].coverUrl)
        assertEquals("Dust & Gold '70s", playlists[1].title)
        assertEquals(1204, playlists[1].trackCount)
        assertTrue(playlists.all { it.kind == LastFmPlaylistKind.Site })
    }

    @Test
    fun parsesTracksFromPlaylistPage() {
        val html = """
            <tr data-playlist-row data-ajax-form-state>
                <a data-youtube-id="bWXazVhlyxQ"
                   data-track-name="First Light"
                   data-artist-name="The Signal">
            </tr>
            <tr data-playlist-row>
                <a data-track-name="Salt &amp; Iron" data-artist-name="North, Jr.">
            </tr>
            <tr data-playlist-row><td>no data</td></tr>
        """.trimIndent()

        val tracks = LastFmSitePages.tracks(html)

        assertEquals(
            listOf(
                ListenBrainzTrack("First Light", "The Signal"),
                ListenBrainzTrack("Salt & Iron", "North, Jr.")
            ),
            tracks
        )
    }

    @Test
    fun detectsClientChallengePage() {
        assertTrue(LastFmSitePages.isChallenge("<html><head><title>Client Challenge</title>"))
        assertFalse(LastFmSitePages.isChallenge("<html><head><title>RJ's playlists | Last.fm</title>"))
    }
}
