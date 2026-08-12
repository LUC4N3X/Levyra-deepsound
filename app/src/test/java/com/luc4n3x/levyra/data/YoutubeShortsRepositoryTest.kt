package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs

class YoutubeShortsRepositoryTest {
    private companion object {
        const val MAX_TEST_QUERY_BOUND = 4
    }

    @Test
    fun extractorShortFlagIsAccepted() {
        assertTrue(
            isYoutubeShortCandidate(
                isShortFormContent = true,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 42L
            )
        )
    }

    @Test
    fun verifiedShortWithUnknownDurationIsAccepted() {
        assertTrue(
            isYoutubeShortCandidate(
                isShortFormContent = true,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 0L
            )
        )
    }

    @Test
    fun canonicalShortsUrlsAreAcceptedAsFallback() {
        assertTrue(
            isYoutubeShortCandidate(
                isShortFormContent = false,
                url = "https://www.youtube.com/shorts/abcdefghijk",
                durationSeconds = 58L
            )
        )
    }

    @Test
    fun threeMinuteShortsAreAcceptedButLongerVideosAreRejected() {
        assertTrue(
            isYoutubeShortCandidate(
                isShortFormContent = true,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 180L
            )
        )
        assertFalse(
            isYoutubeShortCandidate(
                isShortFormContent = true,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 181L
            )
        )
    }

    @Test
    fun shortsTabOriginAcceptsUnknownOrBoundedDuration() {
        assertTrue(isYoutubeShortsTabCandidate(0L))
        assertTrue(isYoutubeShortsTabCandidate(180L))
        assertFalse(isYoutubeShortsTabCandidate(181L))
    }

    @Test
    fun longOrOrdinaryVideosAreRejected() {
        assertFalse(
            isYoutubeShortCandidate(
                isShortFormContent = true,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 480L
            )
        )
        assertFalse(
            isYoutubeShortCandidate(
                isShortFormContent = false,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 90L
            )
        )
    }

    @Test
    fun localTimeoutFenceCancelsAStalledSearch() = runBlocking {
        var timedOut = false
        try {
            withShortsSearchTimeout(timeoutMs = 20L) {
                delay(250L)
            }
        } catch (_: TimeoutCancellationException) {
            timedOut = true
        }

        assertTrue(timedOut)
    }

    @Test
    fun retryBackoffIsBounded() {
        assertEquals(30_000L, youtubeShortsRetryDelayMs(1))
        assertEquals(60_000L, youtubeShortsRetryDelayMs(2))
        assertEquals(600_000L, youtubeShortsRetryDelayMs(5))
        assertEquals(600_000L, youtubeShortsRetryDelayMs(100))
    }

    @Test
    fun shortTracksAreRecognizedBySourceUrlOrType() {
        assertTrue(isYoutubeShortTrack(track(source = YOUTUBE_SHORTS_SOURCE)))
        assertTrue(isYoutubeShortTrack(track(videoUrl = "https://www.youtube.com/shorts/abcdefghijk")))
        assertTrue(isYoutubeShortTrack(track(videoType = "SHORTS")))
        assertFalse(isYoutubeShortTrack(track()))
    }

    @Test
    fun fullMusicVideosNeverQualifyAsShorts() {
        assertFalse(
            isYoutubeShortTrack(
                track(
                    source = "YouTube Music Samples",
                    videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
                    videoType = "MUSIC_VIDEO_TYPE_OMV"
                )
            )
        )
    }

    @Test
    fun followedArtistsAndLanguageDriveQueriesBeforeGenericFallbacks() {
        val queries = youtubeShortQueries(
            seeds = listOf(track(title = "Brano", artist = "Artista ascoltato")),
            preferredArtists = listOf("Artista seguito"),
            languageCode = "it-IT"
        )

        assertEquals("Artista seguito #shorts", queries.first())
        assertTrue(queries.contains("Artista ascoltato #shorts"))
        assertTrue(queries.contains("shorts musica italiana"))
        assertTrue(queries.indexOf("shorts musica italiana") < MAX_TEST_QUERY_BOUND)
        assertFalse(queries.contains("music shorts"))
    }

    @Test
    fun localizedFallbacksExistWithoutListeningHistory() {
        val queries = youtubeShortQueries(
            seeds = emptyList(),
            preferredArtists = emptyList(),
            languageCode = "it"
        )

        assertTrue(queries.isNotEmpty())
        assertEquals("shorts musica italiana", queries.first())
        assertTrue(queries.contains("musica virale #shorts"))
    }

    @Test
    fun channelLookupQueriesPreferFollowedArtistsAndStayBounded() {
        val queries = youtubeShortChannelLookupQueries(
            seeds = listOf(
                track(artist = "Artista ascoltato"),
                track(artist = "Secondo artista ascoltato")
            ),
            preferredArtists = listOf("Artista seguito", "Artista seguito", "Secondo seguito")
        )

        assertEquals(listOf("Artista seguito", "Secondo seguito"), queries)
    }

    @Test
    fun discoveredChannelsRemainAvailableWhenDirectBudgetIsFull() {
        val directChannels = listOf("direct-1", "direct-2", "direct-3", "direct-4")
        val fallbackChannel = "https://www.youtube.com/channel/UCfallback12345678901234"

        val discovered = youtubeShortDiscoveredChannelUrls(
            directChannelUrls = directChannels,
            discoveredChannelUrls = listOf(directChannels.first(), fallbackChannel, fallbackChannel),
            limit = 4
        )

        assertEquals(listOf(fallbackChannel), discovered)
    }

    @Test
    fun artistBrowseIdsAndFollowedChannelIdsBecomeDirectChannelUrls() {
        val track = track(artistBrowseIds = listOf("UC1234567890123456789012"))

        val urls = youtubeShortChannelUrls(
            seeds = listOf(track),
            preferredChannelIds = listOf("UCabcdefghijklmnopqrstuv")
        )

        assertEquals(
            listOf(
                "https://www.youtube.com/channel/UCabcdefghijklmnopqrstuv",
                "https://www.youtube.com/channel/UC1234567890123456789012"
            ),
            urls
        )
    }

    @Test
    fun channelUrlsAreNormalizedWithoutAcceptingUnrelatedBrowseIds() {
        assertEquals(
            "https://www.youtube.com/@artist",
            canonicalYoutubeChannelUrl("/@artist")
        )
        assertEquals(
            "https://www.youtube.com/channel/UCabcdefghijklmnopqrstuv",
            canonicalYoutubeChannelUrl("UCabcdefghijklmnopqrstuv")
        )
        assertEquals(null, canonicalYoutubeChannelUrl("MPLA-not-a-channel"))
    }

    @Test
    fun shortsChannelUrlsBuildShortsTabHandlers() {
        listOf(
            "https://www.youtube.com/@artist",
            "https://www.youtube.com/channel/UCabcdefghijklmnopqrstuv"
        ).forEach { channelUrl ->
            val handler = ServiceList.YouTube.channelTabLHFactory.fromUrl("$channelUrl/shorts")
            assertEquals(ChannelTabs.SHORTS, handler.contentFilters.single().name)
        }
    }

    @Test
    fun lookalikeChannelHostsAreRejected() {
        listOf(
            "https://youtube.com.attacker.example/channel/UCabcdefghijklmnopqrstuv",
            "https://notyoutube.com/channel/UCabcdefghijklmnopqrstuv",
            "https://attacker.example/?next=https://www.youtube.com/channel/UCabcdefghijklmnopqrstuv",
            "https://www.youtube.com:8443/channel/UCabcdefghijklmnopqrstuv",
            "https://user:token@www.youtube.com/channel/UCabcdefghijklmnopqrstuv",
            "http://www.youtube.com/channel/UCabcdefghijklmnopqrstuv"
        ).forEach { candidate ->
            assertNull(candidate, canonicalYoutubeChannelUrl(candidate))
        }
    }

    @Test
    fun officialChannelHostsAreCanonicalizedAndStripped() {
        assertEquals(
            "https://www.youtube.com/channel/UCabcdefghijklmnopqrstuv",
            canonicalYoutubeChannelUrl("https://m.youtube.com/channel/UCabcdefghijklmnopqrstuv?si=track#top")
        )
        assertEquals(
            "https://www.youtube.com/@artist",
            canonicalYoutubeChannelUrl("https://music.youtube.com/@artist/videos")
        )
        assertEquals(
            "https://www.youtube.com/user/legacy",
            canonicalYoutubeChannelUrl("https://youtube.com/user/legacy/")
        )
        assertNull(canonicalYoutubeChannelUrl("https://www.youtube.com/channel"))
        assertNull(canonicalYoutubeChannelUrl("https://www.youtube.com/watch?v=abcdefghijk"))
    }

    private fun track(
        source: String = "YouTube Music",
        videoUrl: String = "https://www.youtube.com/watch?v=abcdefghijk",
        videoType: String = "",
        title: String = "Title",
        artist: String = "Artist",
        artistBrowseIds: List<String> = emptyList()
    ): Track = Track(
        id = "abcdefghijk",
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 60_000L,
        streamUrl = "",
        videoUrl = videoUrl,
        thumbnailUrl = "https://levyra.test/short.jpg",
        largeThumbnailUrl = "https://levyra.test/short-large.jpg",
        source = source,
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0xFF00E5FF.toInt(),
        accentEnd = 0xFF2979FF.toInt(),
        artistBrowseIds = artistBrowseIds,
        videoType = videoType
    )
}
