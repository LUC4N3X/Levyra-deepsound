package com.luc4n3x.levyra.desktop.player

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorRuntime
import com.luc4n3x.levyra.desktop.core.model.AudioQuality
import com.luc4n3x.levyra.desktop.core.model.AppLanguage
import com.luc4n3x.levyra.desktop.core.model.PreferredCodec
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.stream.YoutubeStreamResolver
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.atomic.AtomicReference

class YoutubeNativePlaybackTest {
    @Test fun englishIndiaPlaybackCrossesFirstMinuteAndSeeks() = runBlocking {
        assumeTrue(System.getenv("LEVYRA_ONLINE_NATIVE_SMOKE") == "1")
        ExtractorRuntime.ensureInitialized(AppLanguage.ENGLISH, "IN")
        val track = Track("JGwWNGJdvx8", "Shape of You", "Ed Sheeran", videoUrl = Track.watchUrlOf("JGwWNGJdvx8"))
        val audio = YoutubeStreamResolver().resolve(track, AudioQuality.HIGH, PreferredCodec.AUTO)
        val source = audio.url.toHttpUrl()
        println("YouTube client=${source.queryParameter("c")} itag=${source.queryParameter("itag")} bytes=${source.queryParameter("clen")}")
        VlcAudioPlayer.create("C:/Program Files/VideoLAN/VLC", null).use { player ->
            val terminalFailure = AtomicReference<String?>()
            val eventJob = CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
                player.events.collect { event ->
                    if (event is PlayerEvent.Failed) terminalFailure.set(event.reason)
                    if (event !is PlayerEvent.TimeChanged && event !is PlayerEvent.Buffering) println("VLC: $event")
                }
            }
            player.setMuted(true)
            val started = System.nanoTime()
            player.play(audio.url)
            await(15_000, "first audio") { player.isPlaying() && player.positionMs() > 0 }
            println("First audio: ${(System.nanoTime() - started) / 1_000_000} ms; source=${audio.label}")
            try {
                await(90_000, "stream beyond first minute") {
                    check(terminalFailure.get() == null) { "Bridge failure: ${terminalFailure.get()}" }
                    player.positionMs() >= 70_000
                }
            } finally {
                println("Position at stream check: ${player.positionMs()} ms; playing=${player.isPlaying()}")
                eventJob.cancel()
            }
            assertTrue(player.isPlaying())
            player.seekTo(130_000)
            await(15_000, "seek after first minute") { player.positionMs() >= 129_000 }
            player.pause()
            await(5_000, "pause") { !player.isPlaying() }
            player.resume()
            await(5_000, "resume") { player.isPlaying() }
            println("PASS: Windows libVLC, English/India, >70 s, seek 130 s, pause/resume")
        }
    }

    private fun await(timeoutMs: Long, scenario: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + timeoutMs * 1_000_000
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(100)
        assertTrue(scenario, condition())
    }
}
