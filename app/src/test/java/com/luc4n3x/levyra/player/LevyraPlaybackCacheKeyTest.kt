package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraPlaybackCacheKeyTest {
    @Test
    fun sameItagKeepsStableKeyAcrossSignedUrls() {
        val first = track("https://rr1---sn.example/videoplayback?expire=100&itag=140&sig=one")
        val second = track("https://rr2---sn.example/videoplayback?expire=200&itag=140&sig=two")

        assertEquals(LevyraPlaybackCacheKey.stream(first), LevyraPlaybackCacheKey.stream(second))
    }

    @Test
    fun differentItagsUseDifferentKeys() {
        val audioMp4 = track("https://rr.example/videoplayback?itag=140")
        val audioWebm = track("https://rr.example/videoplayback?itag=251")

        assertNotEquals(LevyraPlaybackCacheKey.stream(audioMp4), LevyraPlaybackCacheKey.stream(audioWebm))
    }

    @Test
    fun refreshedHlsManifestKeepsStableKey() {
        val first = track("https://manifest.googlevideo.com/api/manifest/hls_playlist/expire/100/id/demo.m3u8")
        val second = track("https://manifest.googlevideo.com/api/manifest/hls_playlist/expire/200/id/demo.m3u8")

        assertEquals(LevyraPlaybackCacheKey.stream(first), LevyraPlaybackCacheKey.stream(second))
    }

    @Test
    fun nativeVideoUsesStrictOfficialPairingCacheNamespace() {
        val track = track("https://rr.example/videoplayback?itag=140").copy(
            videoStreamUrl = "https://rr.example/videoplayback?itag=137"
        )

        assertTrue(LevyraPlaybackCacheKey.video(track).contains(":video-v5:itag-137"))
    }

    @Test
    fun artTrackAudioAndOfficialVideoAudioNeverShareAStreamKey() {
        val songMode = track("https://rr.example/videoplayback?itag=140").copy(
            audioVideoId = "lFQdcPTTzSg",
            videoUrl = "https://www.youtube.com/watch?v=lFQdcPTTzSg"
        )
        val videoMode = songMode.copy(
            videoUrl = "https://www.youtube.com/watch?v=fcnDmrtj6Sk",
            counterpartVideoId = "fcnDmrtj6Sk",
            videoType = "MUSIC_VIDEO_TYPE_OMV",
            videoStreamUrl = "https://rr.example/videoplayback?itag=137"
        )

        assertNotEquals(
            LevyraPlaybackCacheKey.stream(songMode),
            LevyraPlaybackCacheKey.stream(videoMode)
        )
    }

    @Test
    fun sabrAudioKeysSeparateSameItagByLanguageAndContentLength() {
        val italian = track("levyra-sabr://s/demo?itag=251&mime=audio%2Fmp4&clen=4500000&xtags=lang%3Dit")
        val english = track("levyra-sabr://s/demo?itag=251&mime=audio%2Fmp4&clen=4500000&xtags=lang%3Den")
        val italianOtherLength = track("levyra-sabr://s/demo?itag=251&mime=audio%2Fmp4&clen=4600000&xtags=lang%3Dit")

        assertNotEquals(LevyraPlaybackCacheKey.stream(italian), LevyraPlaybackCacheKey.stream(english))
        assertNotEquals(LevyraPlaybackCacheKey.stream(italian), LevyraPlaybackCacheKey.stream(italianOtherLength))
    }

    @Test
    fun sameLanguageAndLengthStillSeparateDifferentAudioContentKinds() {
        val original = track(
            "https://rr.example/videoplayback?itag=251&clen=4500000&xtags=lang%3Den%3Aacont%3Doriginal"
        )
        val descriptive = track(
            "https://rr.example/videoplayback?itag=251&clen=4500000&xtags=lang%3Den%3Aacont%3Ddescriptive"
        )

        assertNotEquals(LevyraPlaybackCacheKey.stream(original), LevyraPlaybackCacheKey.stream(descriptive))
    }

    @Test
    fun encodedXtagsStillSeparateLanguageCacheKeys() {
        val italian = track(
            "https://rr.example/videoplayback?foo=1%26itag%3D251%26clen%3D4500000%26xtags%3Dlang%253Dit"
        )
        val english = track(
            "https://rr.example/videoplayback?foo=1%26itag%3D251%26clen%3D4500000%26xtags%3Dlang%253Den"
        )

        assertNotEquals(LevyraPlaybackCacheKey.stream(italian), LevyraPlaybackCacheKey.stream(english))
    }

    @Test
    fun offlineExportNeverSharesTheLiveStreamCacheKey() {
        val muxedFallback = track("https://rr.example/videoplayback?itag=18&mime=video%2Fmp4&ratebypass=yes")
        val audioMp4 = track("https://rr.example/videoplayback?itag=140&mime=audio%2Fmp4&ratebypass=yes")

        assertNotEquals(
            LevyraPlaybackCacheKey.stream(muxedFallback),
            LevyraPlaybackCacheKey.offlineStream(muxedFallback)
        )
        assertNotEquals(
            LevyraPlaybackCacheKey.stream(audioMp4),
            LevyraPlaybackCacheKey.offlineStream(audioMp4)
        )
    }

    @Test
    fun offlineExportKeyStaysStableAcrossSignedUrlRefreshAndSplitsPerItag() {
        val first = track("https://rr1---sn.example/videoplayback?expire=100&itag=18&sig=one")
        val second = track("https://rr2---sn.example/videoplayback?expire=200&itag=18&sig=two")
        val otherItag = track("https://rr2---sn.example/videoplayback?expire=200&itag=140&sig=two")

        assertEquals(
            LevyraPlaybackCacheKey.offlineStream(first),
            LevyraPlaybackCacheKey.offlineStream(second)
        )
        assertNotEquals(
            LevyraPlaybackCacheKey.offlineStream(second),
            LevyraPlaybackCacheKey.offlineStream(otherItag)
        )
    }

    private fun track(streamUrl: String): Track = Track(
        id = "video-id",
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = streamUrl,
        videoUrl = "https://www.youtube.com/watch?v=video-id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )

    @Test
    fun differentVideoItagsProduceDifferentVideoCacheKeys() {
        val rung1080 = renditionTrack(
            streamUrl = "https://cdn.test/audio?itag=251",
            videoStreamUrl = "https://cdn.test/video?itag=137"
        )
        val rung720 = renditionTrack(
            streamUrl = "https://cdn.test/audio?itag=251",
            videoStreamUrl = "https://cdn.test/video?itag=136"
        )
        assertNotEquals(LevyraPlaybackCacheKey.video(rung1080), LevyraPlaybackCacheKey.video(rung720))
    }

    @Test
    fun sameItagWithDifferentSignatureParamsSharesTheRenditionKey() {
        val first = renditionTrack(
            streamUrl = "https://cdn.test/audio?itag=251&sig=AAA",
            videoStreamUrl = "https://cdn.test/video?itag=137&sig=BBB"
        )
        val second = renditionTrack(
            streamUrl = "https://cdn.test/audio?itag=251&sig=ZZZ",
            videoStreamUrl = "https://cdn.test/video?itag=137&sig=YYY"
        )
        assertEquals(LevyraPlaybackCacheKey.video(first), LevyraPlaybackCacheKey.video(second))
    }

    @Test
    fun audioAndVideoRenditionsOfTheSameVideoNeverShareAKey() {
        val item = renditionTrack(
            streamUrl = "https://cdn.test/audio?itag=251",
            videoStreamUrl = "https://cdn.test/video?itag=137"
        )
        assertNotEquals(LevyraPlaybackCacheKey.stream(item), LevyraPlaybackCacheKey.video(item))
    }

    @Test
    fun progressiveMuxedRenditionKeyDerivesFromTheMuxedUrl() {
        val muxed360 = renditionTrack(streamUrl = "https://cdn.test/muxed?itag=18")
        val muxed720 = renditionTrack(streamUrl = "https://cdn.test/muxed?itag=22")
        assertNotEquals(LevyraPlaybackCacheKey.video(muxed360), LevyraPlaybackCacheKey.video(muxed720))
    }

    @Test
    fun videoKeyIsStableForTheSameRenditionAcrossTrackCopies() {
        val original = renditionTrack(
            streamUrl = "https://cdn.test/audio?itag=251&lang=en",
            videoStreamUrl = "https://cdn.test/video?itag=137"
        )
        val copy = original.copy(title = "Renamed")
        assertEquals(LevyraPlaybackCacheKey.video(original), LevyraPlaybackCacheKey.video(copy))
    }

    @Test
    fun differentSourceVideosNeverShareKeys() {
        val first = renditionTrack(streamUrl = "https://cdn.test/muxed?itag=18", audioVideoId = "AAAAAAAAAAA")
        val second = renditionTrack(streamUrl = "https://cdn.test/muxed?itag=18", audioVideoId = "BBBBBBBBBBB")
        assertNotEquals(LevyraPlaybackCacheKey.video(first), LevyraPlaybackCacheKey.video(second))
    }

    private fun renditionTrack(
        streamUrl: String,
        videoStreamUrl: String = "",
        audioVideoId: String = "AbCdEfGhI12"
    ): Track = track(streamUrl).copy(
        id = "catalog-id",
        videoUrl = "https://www.youtube.com/watch?v=$audioVideoId",
        videoStreamUrl = videoStreamUrl,
        audioVideoId = audioVideoId
    )
}
