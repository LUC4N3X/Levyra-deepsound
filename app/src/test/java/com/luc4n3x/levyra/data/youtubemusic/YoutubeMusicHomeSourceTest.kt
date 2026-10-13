package com.luc4n3x.levyra.data.youtubemusic

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicHomeSourceTest {
    @Test
    fun unresolvedSessionIsPendingNotLoggedOut() {
        assertEquals(YoutubeMusicHomeSource.Pending, youtubeMusicHomeSource(restoring = true, activeAccountId = null, home = null))
    }

    @Test
    fun cachedHomeIsShownWhileSessionRestores() {
        val home = home("a")
        assertEquals(YoutubeMusicHomeSource.Ready(home), youtubeMusicHomeSource(restoring = true, activeAccountId = null, home = home))
    }

    @Test
    fun cachedHomeIsDroppedWhenRestoredAccountDiffers() {
        assertEquals(YoutubeMusicHomeSource.Standard, youtubeMusicHomeSource(restoring = false, activeAccountId = null, home = home("a")))
        assertEquals(YoutubeMusicHomeSource.Pending, youtubeMusicHomeSource(restoring = false, activeAccountId = "b", home = home("a")))
    }

    @Test
    fun restoredSessionWithoutAccountUsesStandardHome() {
        assertEquals(YoutubeMusicHomeSource.Standard, youtubeMusicHomeSource(restoring = false, activeAccountId = null, home = null))
    }

    @Test
    fun restoredSessionWithoutHomeStaysPending() {
        assertEquals(YoutubeMusicHomeSource.Pending, youtubeMusicHomeSource(restoring = false, activeAccountId = "a", home = null))
    }

    @Test
    fun failedLoadWithoutDataNeverSpinsForever() {
        assertEquals(YoutubeMusicHomeSource.Standard, youtubeMusicHomeSource(restoring = false, activeAccountId = "a", home = null, homeFailed = true))
    }

    @Test
    fun failedRefreshKeepsLastValidHome() {
        val home = home("a")
        assertEquals(YoutubeMusicHomeSource.Ready(home), youtubeMusicHomeSource(restoring = false, activeAccountId = "a", home = home, homeFailed = true))
    }

    @Test
    fun homeOfAnotherAccountIsNeverShown() {
        assertEquals(YoutubeMusicHomeSource.Pending, youtubeMusicHomeSource(restoring = false, activeAccountId = "b", home = home("a")))
    }

    @Test
    fun homeOfActiveAccountIsReady() {
        val home = home("a")
        assertEquals(YoutubeMusicHomeSource.Ready(home), youtubeMusicHomeSource(restoring = false, activeAccountId = "a", home = home))
    }

    @Test
    fun navigationDoesNotRefetchAnAlreadyLoadedHome() {
        val key = youtubeMusicHomeKey("a", "it")
        assertFalse(youtubeMusicHomeNeedsFetch("a", "it", fetchedKey = key, inFlightKey = null))
        assertFalse(youtubeMusicHomeNeedsFetch("a", "it", fetchedKey = null, inFlightKey = key))
    }

    @Test
    fun cachedHomeStillRefreshesOncePerProcess() {
        assertTrue(youtubeMusicHomeNeedsFetch("a", "it", fetchedKey = null, inFlightKey = null))
    }

    @Test
    fun languageOrAccountChangeRefetches() {
        val key = youtubeMusicHomeKey("a", "it")
        assertTrue(youtubeMusicHomeNeedsFetch("a", "en", fetchedKey = key, inFlightKey = null))
        assertTrue(youtubeMusicHomeNeedsFetch("b", "it", fetchedKey = key, inFlightKey = null))
        assertFalse(youtubeMusicHomeNeedsFetch(null, "it", fetchedKey = null, inFlightKey = null))
    }

    @Test
    fun cacheRoundTripsForTheSameAccount() {
        val original = home("a")
        val decoded = YoutubeMusicHomeCodec.decode(YoutubeMusicHomeCodec.encode(original), "a")
        assertEquals(original.orbit.map { it.id }, decoded?.orbit?.map { it.id })
        assertEquals(original.sections.map { it.title }, decoded?.sections?.map { it.title })
        assertEquals(original.albums.map { it.browseId }, decoded?.albums?.map { it.browseId })
        assertEquals("it", decoded?.languageCode)
    }

    @Test
    fun cacheOfAnotherAccountIsRejected() {
        assertNull(YoutubeMusicHomeCodec.decode(YoutubeMusicHomeCodec.encode(home("a")), "b"))
    }

    @Test
    fun corruptedCacheIsIgnored() {
        assertNull(YoutubeMusicHomeCodec.decode("{not json", "a"))
        assertNull(YoutubeMusicHomeCodec.decode("{\"version\":99,\"accountId\":\"a\"}", "a"))
    }

    private fun home(accountId: String) = YoutubeMusicHome(
        accountId = accountId,
        languageCode = "it",
        orbit = (1..9).map { track("orbit$it") },
        sections = listOf(HomeSection("Mixed for you", listOf(track("mix1"), track("mix2")))),
        albums = listOf(AlbumHit("Album", "Artist", "2024", "https://example.com/a.jpg", "Artist Album", browseId = "MPREb_1"))
    )

    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist $id",
        album = "Album $id",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://music.youtube.com/watch?v=$id",
        thumbnailUrl = "https://example.com/$id.jpg",
        largeThumbnailUrl = "https://example.com/$id.jpg",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
