package com.luc4n3x.levyra.data.qobuz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QobuzCatalogParserTest {
    @Test
    fun editorialNewReleasesKeepOnlyHomepageItems() {
        val html = """
            <li class="album-container filter-item homepage smr">
            <a href="/it-it/album/first-album-artist-one/abcd1234" class="album-cover"><img src="https://static.qobuz.com/images/covers/34/12/abcd1234_150.jpg" /></a>
            <a href="/it-it/album/first-album-artist-one/abcd1234" class="album-title overflow">First &amp; Album</a>
            <a href="/it-it/interpreter/artist-one/1" class="artist-name overflow">Artist One</a>
            </li>
            <li class="album-container filter-item genre-112">
            <a href="/it-it/album/other/zzzz9999" class="album-cover"></a>
            <a href="/it-it/album/other/zzzz9999" class="album-title overflow">Other</a>
            </li>
        """.trimIndent()
        val albums = QobuzCatalogParser.editorialNewReleases(html)
        assertEquals(1, albums.size)
        assertEquals("First & Album", albums.first().title)
        assertEquals("Artist One", albums.first().artist)
        assertTrue(albums.first().hiRes)
        assertEquals("https://static.qobuz.com/images/covers/34/12/abcd1234_600.jpg", albums.first().coverUrl)
    }

    @Test
    fun playlistDetailIgnoresStreamSourcesAndDerivesCovers() {
        val html = """
            <h1 class="album-meta__title" title="Mix">Playlist: Mix</h1>
            <h2 class="album-meta__artist" title="Editors">di Editors</h2>
            <div class="track track--playlist" data-track="1" data-source="https://example.invalid/file?app_id=1&amp;hmac=secret">
            <div class="track__item track__item--name" title="Song"><span>Song</span></div>
            <span class="track__item track__item--artist" title="Singer"><a class="link" href="/it-it/interpreter/singer/2">Singer</a></span>
            <span class="track__item track__item--album" title="Record"><a class="link" href="/it-it/album/record-singer/wxyz5678">Record</a></span>
            <span class="track__item track__item--duration">00:03:05</span>
            </div>
            <div class="track__infos"></div>
        """.trimIndent()
        val detail = QobuzCatalogParser.playlistDetail("9", html, 0L)
        val track = detail!!.tracks.single()
        assertEquals("Song", track.title)
        assertEquals("Singer", track.artist)
        assertEquals(185_000L, track.durationMs)
        assertEquals("", track.streamUrl)
        assertEquals("https://static.qobuz.com/images/covers/78/56/wxyz5678_600.jpg", track.thumbnailUrl)
        assertEquals("Editors", detail.curator)
    }

    @Test
    fun emptyPlaylistPageReturnsNull() {
        assertNull(QobuzCatalogParser.playlistDetail("1", "<html></html>", 0L))
    }

    @Test
    fun storeLocaleFollowsAppLanguage() {
        assertEquals("it-it", QobuzStore.locale("it", "US"))
        assertEquals("gb-en", QobuzStore.locale("en", "gb"))
        assertEquals("us-en", QobuzStore.locale("ru", "ru"))
        assertEquals("be-fr", QobuzStore.locale("fr", "be"))
    }

    @Test
    fun storeUrlRejectsOtherHosts() {
        assertTrue(QobuzStore.isStoreUrl("https://www.qobuz.com/it-it/shop"))
        assertEquals(false, QobuzStore.isStoreUrl("https://play.qobuz.com/album/1"))
        assertEquals(false, QobuzStore.isStoreUrl("http://www.qobuz.com/it-it/shop"))
    }
}
