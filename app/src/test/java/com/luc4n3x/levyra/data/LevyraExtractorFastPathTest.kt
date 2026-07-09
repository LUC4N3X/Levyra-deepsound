package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.downloader.CancellableCall
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.levyra.LevyraSabrPreflight
import org.schabi.newpipe.extractor.levyra.LevyraYoutubeResolver
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrClientProfile

class LevyraExtractorFastPathTest {
    @Test
    fun resolvesSabrAudioPreflightWithoutTreatingBootstrapAsPlayerUrl() {
        val result = LevyraExtractorFastPath.preflight(
            track = sampleTrack(),
            isVideoMode = false,
            preferMp4Audio = false,
            resolver = resolverWithSamplePreflight()
        )

        assertNotNull(result)
        requireNotNull(result)
        assertEquals(251, result.audioItag)
        assertEquals(-1, result.videoItag)
        assertTrue(result.source.contains("SABR", ignoreCase = true))
    }

    @Test
    fun resolvesSabrVideoPreflightAsWarmupMetadata() {
        val result = LevyraExtractorFastPath.preflight(
            track = sampleTrack(),
            isVideoMode = true,
            preferMp4Audio = false,
            resolver = resolverWithSamplePreflight()
        )

        assertNotNull(result)
        requireNotNull(result)
        assertEquals(251, result.audioItag)
        assertEquals(22, result.videoItag)
        assertEquals(720, result.videoHeight)
        assertTrue(result.source.contains("720p", ignoreCase = true))
    }

    @Test
    fun okhttpDownloaderAdvertisesTrueStreamingResponses() {
        assertTrue(OkHttpNewPipeDownloader().supportsStreamingResponses())
    }

    private fun resolverWithSamplePreflight(): LevyraYoutubeResolver {
        return LevyraYoutubeResolver(
            TestStreamingDownloader(),
            { request ->
                LevyraSabrPreflight.createForTests(
                    request.videoId,
                    YoutubeSabrClientProfile.ANDROID,
                    "audio/webm; codecs=\"opus\"",
                    251,
                    0,
                    160_000,
                    "video/mp4; codecs=\"avc1\"",
                    22,
                    720,
                    2_000_000
                )
            },
            null,
            60_000L
        )
    }

    private fun sampleTrack(): Track {
        return Track(
            id = "abc12345678",
            title = "Song",
            artist = "Artist",
            album = "",
            durationMs = 123_000L,
            streamUrl = "",
            videoUrl = "https://www.youtube.com/watch?v=abc12345678",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "test",
            moodTags = emptySet(),
            energy = 50,
            vocal = 50,
            replayScore = 50,
            cacheScore = 50,
            accentStart = 0,
            accentEnd = 0
        )
    }

    private class TestStreamingDownloader : Downloader() {
        override fun supportsStreamingResponses(): Boolean = true

        override fun execute(request: Request): Response {
            throw UnsupportedOperationException("not used")
        }

        override fun executeAsync(request: Request, callback: AsyncCallback): CancellableCall {
            throw UnsupportedOperationException("not used")
        }
    }
}
