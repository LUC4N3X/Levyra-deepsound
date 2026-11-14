package com.luc4n3x.levyra.desktop.player

import com.luc4n3x.levyra.desktop.core.model.*
import com.luc4n3x.levyra.desktop.core.stream.*
import com.luc4n3x.levyra.domain.HighQualityAudioMode
import java.nio.file.Files
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import okhttp3.Request
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.localmusic.resolveLocalFile
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod

class HighQualityNativePlaybackTest {
    @Test fun losslessDownloadRetainsFlacAndReopensOffline() = runBlocking {
        assumeTrue(System.getenv("LEVYRA_LOSSLESS_NATIVE_SMOKE") == "1")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cache = Files.createTempDirectory("levyra-flac-native")
        val file = cache.resolve("FLAC città हिन्दी.flac")
        try {
            val original = object : StreamResolver {
                override suspend fun resolve(track: Track, quality: AudioQuality, codec: PreferredCodec): ResolvedAudio {
                    check(track.offlinePath.isNotBlank()) { "No original source in lossless provider verification" }
                    return YoutubeStreamResolver().resolve(track, quality, codec)
                }
                override fun invalidate(track: Track) = Unit
            }
            val resolver = DesktopHighQualityResolver.create(scope, original, cache,
                { HighQualityAudioMode.PREFER_LOSSLESS }, forDownload = true)
            val track = Track("lossless-recording", "Blinding Lights", "The Weeknd", "After Hours", "", durationMs = 200_000)
            val audio = resolver.resolve(track, AudioQuality.HIGH, PreferredCodec.AUTO)
            println("Lossless download source: ${audio.label}; codec=${audio.codec}; bytes=${audio.contentLength}")
            assertEquals("flac", audio.codec.lowercase())
            VlcAudioPlayer.create("C:/Program Files/VideoLAN/VLC", null).use { player ->
                player.setMuted(true)
                player.play(audio.url)
                delay(3_000)
                assertTrue("FLAC streaming decode", player.isPlaying())
                player.seekTo(90_000)
                delay(2_000)
                assertTrue("FLAC streaming seek", player.positionMs() >= 89_000)
            }
            withContext(Dispatchers.IO) {
                if (audio.deliveryMethod == PlaybackDeliveryMethod.DASH) {
                    DashFlacDownload.write(resolveLocalFile(audio.url)!!, file) {}
                } else {
                val client = ExtractorHttp.client.newBuilder().dns(YoutubeStreamNetworkPolicy.validatingDns())
                    .callTimeout(90, java.util.concurrent.TimeUnit.SECONDS).build()
                client.newCall(Request.Builder().url(audio.url).build()).execute().use { response ->
                    assertEquals(200, response.code)
                    assertEquals(audio.contentLength, response.body.contentLength())
                    Files.newOutputStream(file).use { target -> response.body.byteStream().use { it.copyTo(target) } }
                }
                assertEquals(audio.contentLength, Files.size(file))
                }
                DownloadedAudioValidation.requireFlac(file, track.durationMs)
            }
            val offline = resolver.resolve(track.copy(offlinePath = file.toString(), offlineMediaLabel = audio.label), AudioQuality.HIGH, PreferredCodec.AUTO)
            assertTrue(offline.url.startsWith("file:"))
            VlcAudioPlayer.create("C:/Program Files/VideoLAN/VLC", null).use { player ->
                player.setMuted(true)
                player.play(offline.url)
                delay(2_000)
                assertTrue("FLAC decoding offline", player.isPlaying())
                player.seekTo(90_000)
                delay(1_000)
                assertTrue("FLAC seek offline", player.positionMs() >= 89_000)
            }
            println("PASS: real FLAC bytes, complete download, Unicode file path, offline decode and seek")
        } finally {
            scope.cancel()
            Files.deleteIfExists(file)
        }
    }

    @Test fun default320IndianRecordingPlaysBeyondFirstMinute() = runBlocking {
        assumeTrue(System.getenv("LEVYRA_HQ_NATIVE_SMOKE") == "1")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cache = Files.createTempDirectory("levyra-hq-native")
        val original = object : StreamResolver {
            override suspend fun resolve(track: Track, quality: AudioQuality, codec: PreferredCodec): ResolvedAudio =
                throw StreamResolutionException("Original unavailable in provider verification")
            override fun invalidate(track: Track) = Unit
        }
        try {
            val resolver = DesktopHighQualityResolver.create(scope, original, cache, { HighQualityAudioMode.DEFAULT })
            val track = Track("indian-recording", "Tum Hi Ho", "Arijit Singh", "Aashiqui 2",
                "https://www.youtube.com/watch?v=Umqb9KENgmk", durationMs = 262_000)
            val resolved = resolver.resolve(track, AudioQuality.HIGH, PreferredCodec.AUTO)
            println("HQ source: ${resolved.label}; codec=${resolved.codec}; bytes=${resolved.contentLength}")
            assertTrue(resolved.label.contains("320 kbps"))
            assertEquals(track.title, resolved.title)
            VlcAudioPlayer.create("C:/Program Files/VideoLAN/VLC", null).use { player ->
                player.setMuted(true)
                player.play(resolved.url)
                val deadline = System.nanoTime() + 90_000_000_000L
                while (player.positionMs() < 70_000 && System.nanoTime() < deadline) delay(100)
                assertTrue("320 kbps playback beyond 70 seconds", player.positionMs() >= 70_000)
                player.seekTo(130_000)
                delay(2_000)
                assertTrue("320 kbps seek after minute", player.positionMs() >= 129_000)
                println("PASS: Indian recording, default 320 kbps, >70 s, seek 130 s")
            }
        } finally { scope.cancel() }
    }
}
