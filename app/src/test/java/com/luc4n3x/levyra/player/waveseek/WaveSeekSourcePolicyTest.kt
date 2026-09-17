package com.luc4n3x.levyra.player.waveseek

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveSeekSourcePolicyTest {

    @Test
    fun `local media can always be analysed when duration is known`() {
        assertTrue(WaveSeekSourcePolicy.canAnalyze("content://media/audio/42", 180_000L))
        assertTrue(WaveSeekSourcePolicy.canAnalyze("file:///music/song.m4a", 180_000L))
    }

    @Test
    fun `direct http audio is allowed but adaptive manifests are not`() {
        assertTrue(WaveSeekSourcePolicy.canAnalyze("https://media.example/song.m4a?token=x", 180_000L))
        assertTrue(WaveSeekSourcePolicy.canAnalyze("https://media.example/videoplayback?mime=audio%2Fmp4", 180_000L))
        assertFalse(WaveSeekSourcePolicy.canAnalyze("https://media.example/master.m3u8", 180_000L))
        assertFalse(WaveSeekSourcePolicy.canAnalyze("https://media.example/manifest.mpd", 180_000L))
        assertFalse(
            WaveSeekSourcePolicy.canAnalyze(
                "https://media.example/videoplayback?mime=application%2Fx-mpegURL",
                180_000L
            )
        )
        assertFalse(
            WaveSeekSourcePolicy.canAnalyze(
                "https://media.example/videoplayback?type=application%2Fx-mpegURL",
                180_000L
            )
        )
        assertFalse(
            WaveSeekSourcePolicy.canAnalyze(
                "https://media.example/videoplayback?mime=application%2Fdash+xml",
                180_000L
            )
        )
    }

    @Test
    fun `levyra cache and sabr sources can be measured passively`() {
        assertTrue(WaveSeekSourcePolicy.canAnalyze("levyra-cache://media?key=track", 180_000L))
        assertTrue(WaveSeekSourcePolicy.canAnalyze("levyra-sabr://s/abc?itag=251", 180_000L))
    }

    @Test
    fun `unknown duration and unsupported schemes fall back to the existing seekbar`() {
        assertFalse(WaveSeekSourcePolicy.canAnalyze("https://media.example/song.m4a", 0L))
        assertFalse(WaveSeekSourcePolicy.canAnalyze("rtsp://media.example/song", 180_000L))
        assertFalse(WaveSeekSourcePolicy.canAnalyze("", 180_000L))
    }
}
