package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackWarmupPolicyTest {
    @Test
    fun splitVideoCachesOnlyTheSeparateAudioPrimary() {
        val track = track("https://media.example/audio?itag=140").copy(
            videoStreamUrl = "https://media.example/video?itag=137"
        )

        val plan = videoWarmupPlan(track)

        assertTrue(plan.cachePrimaryAsAudio)
        assertEquals(track.videoStreamUrl, plan.probeUrl)
    }

    @Test
    fun muxedVideoNeverWritesPartialVideoIntoPlaybackCache() {
        val track = track("https://media.example/muxed?itag=22")

        val plan = videoWarmupPlan(track)

        assertFalse(plan.cachePrimaryAsAudio)
        assertEquals(track.streamUrl, plan.probeUrl)
    }

    @Test
    fun sabrStreamsAreNeverWarmedThroughThePlainHttpStack() {
        val sabrUrl = "levyra-sabr://s/ZW5kcG9pbnQ?itag=140&mime=audio%2Fmp4"
        val audioOnly = track(sabrUrl)
        val splitVideo = track(sabrUrl).copy(videoStreamUrl = sabrUrl)

        assertEquals("", videoWarmupPlan(audioOnly).probeUrl)
        assertFalse(videoWarmupPlan(audioOnly).cachePrimaryAsAudio)
        assertEquals("", videoWarmupPlan(splitVideo).probeUrl)
        assertFalse(videoWarmupPlan(splitVideo).cachePrimaryAsAudio)
        assertFalse(isWarmableMediaUrl(sabrUrl))
        assertTrue(isWarmableMediaUrl("https://media.example/audio?itag=140"))
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
}
