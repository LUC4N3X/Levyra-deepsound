package com.luc4n3x.levyra.player

import android.content.ComponentName
import android.os.Bundle
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.data.TrackPayloadCodec
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class AndroidAutoBrowserTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val component = ComponentName(context, PlaybackService::class.java)
    private val notifications = LinkedBlockingQueue<Pair<String, Int>>()

    private fun <T> main(block: () -> T): T {
        var value: T? = null
        instrumentation.runOnMainSync { value = block() }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private fun connect(): MediaBrowser = main {
        MediaBrowser.Builder(context, SessionToken(context, component))
            .setListener(object : MediaBrowser.Listener {
                override fun onSearchResultChanged(browser: MediaBrowser, query: String, itemCount: Int, params: LibraryParams?) {
                    notifications.offer(query to itemCount)
                }
            }).buildAsync()
    }.get(15, TimeUnit.SECONDS)

    private fun search(browser: MediaBrowser, query: String): List<MediaItem> {
        notifications.clear()
        val started = System.nanoTime()
        val result = main { browser.search(query, null) }.get(18, TimeUnit.SECONDS)
        assertEquals("Search must succeed", LibraryResult.RESULT_SUCCESS, result.resultCode)
        val notification = notifications.poll(3, TimeUnit.SECONDS)
        assertNotNull("Search availability callback missing", notification)
        assertEquals(query, notification!!.first)
        val items = main { browser.getSearchResult(query, 0, 80, null) }.get(4, TimeUnit.SECONDS)
        assertEquals(LibraryResult.RESULT_SUCCESS, items.resultCode)
        val found = items.value!!.toList()
        assertEquals(notification.second, found.size)
        assertTrue(found.all { it.mediaMetadata.isBrowsable != null && it.mediaMetadata.isPlayable != null })
        Log.i("AutoBrowserTest", "search length=${query.length} items=${found.size} elapsedMs=${(System.nanoTime() - started) / 1_000_000}")
        return found
    }

    @Test
    fun coldServiceConnectionTokenBrowseAndEmptySearch() {
        val browser = connect()
        try {
            val root = main { browser.getLibraryRoot(null) }.get(5, TimeUnit.SECONDS)
            assertEquals(AndroidAutoLibrary.ID_ROOT, root.value!!.mediaId)
            val token = main {
                browser.sendCustomCommand(SessionCommand(PlaybackService.ACTION_GET_PLATFORM_TOKEN, Bundle.EMPTY), Bundle.EMPTY)
            }.get(5, TimeUnit.SECONDS)
            assertEquals(SessionResult.RESULT_SUCCESS, token.resultCode)
            assertTrue(token.extras.containsKey(PlaybackService.KEY_PLATFORM_TOKEN))
            val roots = main { browser.getChildren(AndroidAutoLibrary.ID_ROOT, 0, 80, null) }.get(5, TimeUnit.SECONDS)
            assertEquals(LibraryResult.RESULT_SUCCESS, roots.resultCode)
            assertTrue(roots.value!!.map { it.mediaId }.containsAll(listOf(AndroidAutoLibrary.ID_HOME,
                AndroidAutoLibrary.ID_FAVORITES, AndroidAutoLibrary.ID_DOWNLOADS, AndroidAutoLibrary.ID_PLAYLISTS)))
            listOf(AndroidAutoLibrary.ID_HOME, AndroidAutoLibrary.ID_FAVORITES, AndroidAutoLibrary.ID_DOWNLOADS,
                AndroidAutoLibrary.ID_PLAYLISTS).forEach { id ->
                val children = main { browser.getChildren(id, 0, 20, null) }.get(18, TimeUnit.SECONDS)
                assertEquals("Browse category must succeed", LibraryResult.RESULT_SUCCESS, children.resultCode)
            }
            assertTrue(search(browser, "x").isEmpty())
        } finally { main { browser.release() } }
    }

    @Test
    fun reconnectAndRapidSearchesDoNotPublishObsoleteResults() {
        var browser = connect()
        main { browser.release() }
        browser = connect()
        try {
            notifications.clear()
            val previous = main { browser.search("AndroidAutoSupersededSearch1044", null) }
            val latest = main { browser.search("x", null) }
            assertEquals(LibraryResult.RESULT_SUCCESS, latest.get(18, TimeUnit.SECONDS).resultCode)
            val notification = notifications.poll(3, TimeUnit.SECONDS)
            assertEquals("x", notification!!.first)
            assertEquals(0, notification.second)
            try { previous.get(5, TimeUnit.SECONDS) } catch (_: java.util.concurrent.CancellationException) { }
            val old = main { browser.getSearchResult("AndroidAutoSupersededSearch1044", 0, 10, null) }.get(5, TimeUnit.SECONDS)
            assertNotEquals(LibraryResult.RESULT_SUCCESS, old.resultCode)
            assertTrue(search(browser, "x").isEmpty())
        } finally { main { browser.release() } }
    }

    @Test
    fun liveSongsArtistsAlbumsSpecialCharactersAndPagination() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("autoLive") == "true")
        val browser = connect()
        try {
            listOf("Daft Punk Get Lucky", "Radiohead", "Random Access Memories Daft Punk", "Björk Jóga").forEach { query ->
                val items = search(browser, query)
                assertTrue("Known catalog query must return content", items.isNotEmpty())
                val first = main { browser.getSearchResult(query, 0, 2, null) }.get(4, TimeUnit.SECONDS).value!!
                val next = main { browser.getSearchResult(query, 1, 2, null) }.get(4, TimeUnit.SECONDS).value!!
                assertEquals(items.take(4).map { it.mediaId }, (first + next).map { it.mediaId })
                val beyond = main { browser.getSearchResult(query, Int.MAX_VALUE, 80, null) }.get(4, TimeUnit.SECONDS)
                assertTrue(beyond.value!!.isEmpty())
                val repeat = search(browser, query)
                assertEquals(items.map { it.mediaId }, repeat.map { it.mediaId })
                val playable = items.firstOrNull { it.mediaMetadata.isPlayable == true }
                assertNotNull(playable)
                assertNotNull(playable!!.mediaMetadata.artworkUri)
                val item = main { browser.getItem(playable.mediaId) }.get(4, TimeUnit.SECONDS)
                assertEquals(playable.mediaId, item.value!!.mediaId)
                if (query == "Daft Punk Get Lucky" && InstrumentationRegistry.getArguments().getString("autoPlayback") == "true") {
                    verifyPlayback(browser, items)
                }
            }
            val suggestions = search(browser, "zxqv1044nonexistentzzqv9831726450")
            assertTrue(suggestions.all { it.mediaMetadata.durationMs == null || it.mediaMetadata.durationMs!! >= 0L })
        } finally { main { browser.release() } }
    }

    @Test
    fun legacyMediaBrowserReceivesSearchCompletion() {
        val connected = LinkedBlockingQueue<Boolean>()
        val results = LinkedBlockingQueue<List<MediaBrowserCompat.MediaItem>>()
        val browser = main {
            MediaBrowserCompat(context, component, object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() { connected.offer(true) }
                override fun onConnectionFailed() { connected.offer(false) }
            }, null).also { it.connect() }
        }
        try {
            assertEquals(true, connected.poll(15, TimeUnit.SECONDS))
            main {
                browser.search("x", null, object : MediaBrowserCompat.SearchCallback() {
                    override fun onSearchResult(query: String, extras: Bundle?, items: MutableList<MediaBrowserCompat.MediaItem>) {
                        results.offer(items)
                    }
                    override fun onError(query: String, extras: Bundle?) { connected.offer(false) }
                })
            }
            assertNotNull("Legacy search must terminate through its callback", results.poll(18, TimeUnit.SECONDS))
        } finally { main { browser.disconnect() } }
    }

    @Test
    fun unavailableProviderTerminatesWithAnError() {
        val expected = InstrumentationRegistry.getArguments().getString("autoExpectedFailure")
        assumeTrue(expected != null)
        val browser = connect()
        try {
            val started = System.nanoTime()
            val result = main { browser.search("AutoFailure1044${System.nanoTime()}", null) }.get(18, TimeUnit.SECONDS)
            assertNotEquals("Unavailable provider must not look like an empty success", LibraryResult.RESULT_SUCCESS, result.resultCode)
            val timedOut = result.sessionError?.extras?.getString(AndroidAutoLibrary.EXTRA_SEARCH_FAILURE) == "timeout"
            if (expected == "timeout") assertTrue("Provider deadline must be reported", timedOut)
            Log.i("AutoBrowserTest", "provider-failure timeout=$timedOut elapsedMs=${(System.nanoTime() - started) / 1_000_000}")
        } finally { main { browser.release() } }
    }

    @Test
    fun selectedResultKeepsTheWholeQueueAndSupportsNextPreviousAndNormalPlayback() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("autoFixture") == "true")
        val file = File(context.cacheDir, "share/lyrics/auto-protocol-test.m4a")
        assertTrue("Seed the AAC fixture before enabling autoFixture", file.isFile)
        val tracks = (0..2).map { index -> Track(
            id = "auto-fixture-$index", title = "Auto fixture $index", artist = "Protocol test", album = "Test",
            durationMs = 30_000, streamUrl = Uri.fromFile(file).toString(), videoUrl = "",
            thumbnailUrl = "", largeThumbnailUrl = "", source = "Download", moodTags = emptySet(),
            energy = 50, vocal = 50, replayScore = 0, cacheScore = 100, accentStart = 0, accentEnd = 0
        ) }
        val entries = listOf(tracks[0], tracks[0].copy(title = "Duplicate fixture"), tracks[1], tracks[2])
        val items = entries.map { track -> MediaItem.Builder()
            .setMediaId(AndroidAutoLibrary.ID_TRACK_PREFIX + track.id)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setIsPlayable(true).setIsBrowsable(false)
                .setExtras(Bundle().apply { putString(AndroidAutoLibrary.EXTRA_TRACK_PAYLOAD, TrackPayloadCodec.encode(track)) }).build())
            .build()
        }
        val queue = PersistentQueueEngine.get(context)
        val browser = connect()
        val playing = LinkedBlockingQueue<Boolean>()
        try {
            main {
                browser.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) { if (isPlaying) playing.offer(true) }
                    override fun onEvents(player: Player, events: Player.Events) {
                        if (player.isPlaying && player.mediaMetadata.title == tracks[1].title) playing.offer(true)
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) { playing.offer(false) }
                })
                browser.setMediaItems(items, 2, 0)
                browser.prepare()
                browser.play()
            }
            val started = playing.poll(15, TimeUnit.SECONDS)
            val actual = main { PlaybackService.activePlayer?.let {
                "state=${it.playbackState} playing=${it.isPlaying} intent=${it.playWhenReady} suppression=${it.playbackSuppressionReason} buffered=${it.bufferedPosition} error=${it.playerError?.errorCode}"
            } }
            assertEquals("Fixture actual=$actual browser=${main { browser.playbackState }}", true, started)
            assertEquals(tracks.map { it.id }, queue.state.value.tracks.map { it.id })
            assertEquals(1, queue.state.value.currentIndex)
            Log.i("AutoBrowserTest", "before-next available=${main { browser.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) }}")
            main { browser.seekToNextMediaItem() }
            runBlocking { withTimeout(8_000) { queue.state.first { it.currentIndex == 2 } } }
            Log.i("AutoBrowserTest", "after-next index=${queue.state.value.currentIndex} previous=${main { browser.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) }}")
            main { browser.seekToPreviousMediaItem() }
            runBlocking { withTimeout(8_000) { queue.state.first { it.currentIndex == 1 } } }
            main { browser.setMediaItem(items.first()); browser.prepare(); browser.play() }
            runBlocking { withTimeout(8_000) { queue.state.first { it.currentIndex == 0 } } }
            assertEquals(tracks.map { it.id }, queue.state.value.tracks.map { it.id })
            main { browser.pause() }
            instrumentation.waitForIdleSync()
            assertFalse(main { browser.playWhenReady })
            playing.clear()
            main {
                browser.setMediaItem(LevyraMediaItemFactory.build(tracks[1]))
                browser.prepare()
                browser.play()
            }
            assertEquals(true, playing.poll(15, TimeUnit.SECONDS))
            Log.i("AutoBrowserTest", "queue-selection-next-previous-normal-playback=passed")
        } finally {
            main { browser.pause(); browser.clearMediaItems(); browser.release() }
            file.delete()
        }
    }

    private fun verifyPlayback(browser: MediaBrowser, results: List<MediaItem>) {
        try {
            val items = results.filter { it.mediaMetadata.isPlayable == true }.take(1)
            assertTrue(items.isNotEmpty())
            val ready = LinkedBlockingQueue<Boolean>()
            main {
                browser.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) { if (isPlaying) ready.offer(true) }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) { ready.offer(false) }
                })
                browser.setMediaItems(items)
                browser.prepare()
                browser.play()
            }
            assertEquals("Search result must reach audible playback", true, ready.poll(60, TimeUnit.SECONDS))
            main { browser.pause() }
            instrumentation.waitForIdleSync()
            assertFalse(main { browser.playWhenReady })
            main { browser.play() }
            assertEquals(true, ready.poll(15, TimeUnit.SECONDS))
            Log.i("AutoBrowserTest", "playback-controls=passed")
        } finally { main { browser.pause() } }
    }
}
