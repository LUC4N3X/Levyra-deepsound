package com.luc4n3x.levyra.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AppleMusicAudioBadgeTest {

    @Test
    fun atmosOnlyForJointObjectCodingStreams() {
        assertEquals("Dolby Atmos", appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "E-AC-3", mimeType = "audio/eac3-joc", channels = 6)))
        assertEquals("E-AC-3 · 640 kbps", appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "E-AC-3", bitrateKbps = 640, mimeType = "audio/eac3")))
    }

    @Test
    fun losslessTiersFollowTheSampleRate() {
        assertEquals("Lossless", appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "FLAC", sampleRateHz = 44_100, mimeType = "audio/flac")))
        assertEquals("Hi-Res Lossless", appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "FLAC", sampleRateHz = 96_000, mimeType = "audio/flac")))
    }

    @Test
    fun lossyStreamsShowTheRealCodecAndBitrate() {
        assertEquals("AAC · 256 kbps", appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "AAC", bitrateKbps = 256, mimeType = "audio/mp4a-latm")))
        assertEquals("", appleMusicAudioBadgeLabel(PlayerAudioSpec()))
    }
}
