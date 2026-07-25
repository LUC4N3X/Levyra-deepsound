package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.desktop.core.model.AudioQuality
import com.luc4n3x.levyra.desktop.core.model.PreferredCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioStreamSelectorTest {

    private val opusLow = AudioCandidate(
        url = "https://rr1.googlevideo.com/videoplayback?itag=249",
        mimeType = "audio/webm",
        suffix = "webm",
        itag = 249,
        averageBitrate = 50
    )

    private val opusHigh = AudioCandidate(
        url = "https://rr1.googlevideo.com/videoplayback?itag=251",
        mimeType = "audio/webm",
        suffix = "webm",
        itag = 251,
        averageBitrate = 160
    )

    private val aacMedium = AudioCandidate(
        url = "https://rr1.googlevideo.com/videoplayback?itag=140",
        mimeType = "audio/mp4",
        suffix = "m4a",
        itag = 140,
        averageBitrate = 128
    )

    @Test
    fun `high quality prefers the highest bitrate`() {
        val selected = AudioStreamSelector.select(
            listOf(opusLow, aacMedium, opusHigh),
            AudioQuality.HIGH,
            PreferredCodec.AUTO
        )
        assertEquals(opusHigh, selected)
    }

    @Test
    fun `low quality prefers the smallest stream`() {
        val selected = AudioStreamSelector.select(
            listOf(opusLow, aacMedium, opusHigh),
            AudioQuality.LOW,
            PreferredCodec.AUTO
        )
        assertEquals(opusLow, selected)
    }

    @Test
    fun `codec preference wins over bitrate distance`() {
        val selected = AudioStreamSelector.select(
            listOf(opusHigh, aacMedium),
            AudioQuality.HIGH,
            PreferredCodec.AAC
        )
        assertEquals(aacMedium, selected)
    }

    @Test
    fun `balanced quality targets mid bitrate`() {
        val selected = AudioStreamSelector.select(
            listOf(opusLow, aacMedium, opusHigh),
            AudioQuality.BALANCED,
            PreferredCodec.AUTO
        )
        assertEquals(aacMedium, selected)
    }

    @Test
    fun `manifest and empty urls are rejected`() {
        assertFalse(AudioStreamSelector.isPlayable(opusHigh.copy(url = "")))
        assertFalse(AudioStreamSelector.isPlayable(opusHigh.copy(url = "https://host/api/manifest/dash/id/x")))
        assertFalse(AudioStreamSelector.isPlayable(opusHigh.copy(url = "ftp://host/file")))
        assertTrue(AudioStreamSelector.isPlayable(opusHigh))
    }

    @Test
    fun `no playable candidate returns null`() {
        val selected = AudioStreamSelector.select(
            listOf(opusHigh.copy(url = "")),
            AudioQuality.HIGH,
            PreferredCodec.AUTO
        )
        assertNull(selected)
    }

    @Test
    fun `codec detection covers itag only candidates`() {
        val opusByItag = AudioCandidate(url = "https://host/a", itag = 251, averageBitrate = 160)
        val aacByItag = AudioCandidate(url = "https://host/b", itag = 140, averageBitrate = 128)
        assertTrue(opusByItag.isOpus)
        assertTrue(aacByItag.isAac)
    }
}
