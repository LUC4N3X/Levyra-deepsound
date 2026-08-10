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

    private fun candidate(url: String, mime: String, codec: String) = LevyraVideoCandidate(
        url = url,
        mimeType = mime,
        codec = codec,
        width = 1280,
        height = 720,
        fps = 30,
        bitrate = 1_500_000,
        itag = 0,
        muxed = true,
        label = "test"
    )
}
