package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraVideoStreamSelectorTest {
    @Test
    fun incompleteCodecDiscoveryPrefersConservativeMp4Fallback() {
        val av1 = candidate(url = "https://media.example/av1", mime = "video/webm", codec = "av01")
        val h264 = candidate(url = "https://media.example/h264", mime = "video/mp4", codec = "avc1.64001f")

        val fallback = conservativeVideoFallbackCandidates(listOf(av1, h264))

        assertEquals(listOf(h264), fallback)
    }

    @Test
    fun incompleteMetadataStillLeavesAPlayableCandidate() {
        val unknown = candidate(url = "https://media.example/unknown", mime = "", codec = "")

        assertEquals(listOf(unknown), conservativeVideoFallbackCandidates(listOf(unknown)))
    }

    @Test
    fun stableAndroidSelectionPrefersAdequateH264OverModernCodec() {
        val av1 = candidate(
            url = "https://media.example/av1-1080",
            mime = "video/webm",
            codec = "av01.0.08M.08",
            height = 1080
        )
        val h264 = candidate(
            url = "https://media.example/h264-1080",
            mime = "video/mp4",
            codec = "avc1.640028",
            height = 1080
        )

        assertEquals(
            listOf(h264),
            stableAndroidVideoCandidates(listOf(av1, h264), targetHeight = 1080)
        )
    }

    @Test
    fun stableAndroidSelectionDoesNotForceLowResolutionH264() {
        val av1 = candidate(
            url = "https://media.example/av1-1080",
            mime = "video/webm",
            codec = "av01.0.08M.08",
            height = 1080
        )
        val h264 = candidate(
            url = "https://media.example/h264-360",
            mime = "video/mp4",
            codec = "avc1.42001e",
            height = 360
        )

        assertEquals(
            listOf(av1, h264),
            stableAndroidVideoCandidates(listOf(av1, h264), targetHeight = 1080)
        )
    }

    @Test
    fun reliableSelectionUsesMuxedBeforeSplitFallback() {
        val muxed = candidate(
            url = "https://media.example/muxed-720",
            mime = "video/mp4",
            codec = "avc1.64001f",
            height = 720,
            muxed = true
        )
        val split = candidate(
            url = "https://media.example/video-only-1080",
            mime = "video/mp4",
            codec = "avc1.640028",
            height = 1080,
            muxed = false
        )

        assertEquals(muxed, reliableVideoCandidate(muxed, split))
        assertEquals(split, reliableVideoCandidate(null, split))
    }

    private fun candidate(
        url: String,
        mime: String,
        codec: String,
        height: Int = 720,
        muxed: Boolean = true
    ) = LevyraVideoCandidate(
        url = url,
        mimeType = mime,
        codec = codec,
        width = if (height >= 1080) 1920 else 1280,
        height = height,
        fps = 30,
        bitrate = 1_500_000,
        itag = 0,
        muxed = muxed,
        label = "test"
    )
}
