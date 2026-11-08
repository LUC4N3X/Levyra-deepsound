package com.luc4n3x.levyra.ui.library

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.domain.FollowedArtist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.theme.LevyraTheme
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryArtistsScrollTest {
    @Test
    fun artistAliasesScrollInBothDirectionsWithoutDuplicateKeys() {
        val tracks = (0 until 80).flatMap { index ->
            listOf(track(index, "Artist $index"), track(index, "Alias $index"))
        }
        val catalog = buildLibraryCatalog(
            favorites = tracks,
            playlists = emptyList(),
            downloads = emptyList(),
            recentListens = emptyList(),
            followedArtists = (0 until 80).map { index ->
                FollowedArtist("UC_$index", "Followed $index", "", index.toLong())
            }
        )
        val completed = CountDownLatch(1)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    LevyraTheme {
                        val listState = rememberLazyListState()
                        LazyColumn(Modifier.fillMaxSize(), state = listState) {
                            items(catalog.artists, key = { "artist-${it.key}" }) { artist ->
                                LibraryArtistRow(artist, false, false, {}, {}, {})
                            }
                        }
                        LaunchedEffect(Unit) {
                            repeat(2) {
                                for (index in catalog.artists.indices step 3) {
                                    listState.scrollToItem(index)
                                    withFrameNanos { }
                                }
                                for (index in catalog.artists.indices.reversed() step 3) {
                                    listState.scrollToItem(index)
                                    withFrameNanos { }
                                }
                            }
                            completed.countDown()
                        }
                    }
                }
            }
            assertTrue("Artist scrolling did not complete", completed.await(40, TimeUnit.SECONDS))
            assertEquals(80, catalog.artists.size)
            assertEquals(80, catalog.artists.map { it.key }.toSet().size)
            assertTrue(catalog.artists.all { it.tracks.size == 2 })
        }
    }

    private fun track(index: Int, artist: String) = Track(
        id = "$index-$artist",
        title = "Song $index",
        artist = artist,
        album = "Album",
        durationMs = 180_000L,
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
        accentEnd = 0,
        artistBrowseIds = listOf("UC_$index")
    )
}
