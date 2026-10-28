package com.luc4n3x.levyra.data.hqaudio

import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.data.hqaudio.tidal.TidalAudioProvider
import com.luc4n3x.levyra.data.hqaudio.tidal.TidalNetwork
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class TidalLosslessLiveTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private var stage = "configuration"
    private val requestCount = AtomicInteger()

    @Test
    fun authenticatesAndPlaysLosslessAndHiResOnMedia3() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("tidalLive") == "true")
        val directory = File(context.cacheDir, "tidal-emulator-native-probe")
        val reportFile = File(context.filesDir, "tidal-emulator-result.json")
        val report = JSONObject().put("samples", JSONArray())
        val previousLogLevel = androidx.media3.common.util.Log.getLogLevel()
        androidx.media3.common.util.Log.setLogLevel(androidx.media3.common.util.Log.LOG_LEVEL_OFF)
        try {
            withTimeout(120_000) {
                val provider = TidalAudioProvider(directory)
                stage = "catalog-search"
                val search = provider.search("Daft Punk Get Lucky", 1)
                check(search is ProviderSearchOutcome.Found && search.candidates.isNotEmpty())
                for ((id, bits) in listOf("92265335" to 16, "19823990" to 24)) {
                    stage = "$bits-bit:lookup"
                    val lookup = provider.lookup(id)
                    check(lookup is ProviderLookupOutcome.Found)
                    stage = "$bits-bit:resolve"
                    val selected = if (bits == 16) lookup.candidate.copy(mediaToken = "LOSSLESS") else lookup.candidate
                    val resolution = provider.resolveStream(selected)
                    check(resolution is ProviderStreamOutcome.Resolved)
                    val stream = resolution.stream
                    stage = "$bits-bit:precision"
                    report.put("observedBitDepth", stream.bitDepth).put("observedSampleRateHz", stream.sampleRateHz)
                    assertEquals(bits, stream.bitDepth)
                    assertEquals(44_100, stream.sampleRateHz)
                    stage = "$bits-bit:media3"
                    val playback = playSample(stream)
                    playback.put("sourceBitDepth", stream.bitDepth).put("sourceSampleRateHz", stream.sampleRateHz)
                    report.getJSONArray("samples").put(playback)
                }
            }
            report.put("status", "PASS").put("mediaNetworkRequests", requestCount.get())
            reportFile.writeText(report.toString(2))
        } catch (failure: Throwable) {
            report.put("status", "FAIL").put("stage", stage).put("errorClass", failure.javaClass.simpleName)
            reportFile.writeText(report.toString(2))
            throw AssertionError("TIDAL live probe failed at $stage (${failure.javaClass.simpleName}); details withheld")
        } finally {
            directory.listFiles()?.filter { it.isFile }?.forEach(File::delete)
            directory.delete()
            androidx.media3.common.util.Log.setLogLevel(previousLogLevel)
        }
    }

    private fun client(): OkHttpClient {
        val base = LevyraHttpClientFactory.externalIntegrations()
        return base.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS)
            .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
            .dns(Dns { host ->
                base.dns.lookup(host).also { addresses ->
                    if (addresses.isEmpty() || addresses.any { !SafeImageUrlPolicy.isPublicAddress(it) }) throw IOException("Blocked destination")
                }
            })
            .addInterceptor { chain ->
                if (!TidalNetwork.allows(chain.request().url) || requestCount.incrementAndGet() > 24) throw IOException("Blocked probe request")
                chain.proceed(chain.request())
            }.build()
    }

    private fun playSample(stream: ResolvedHighQualityStream): JSONObject {
        val error = AtomicReference<String?>()
        var ownedPlayer: ExoPlayer? = null
        lateinit var player: ExoPlayer
        try {
            instrumentation.runOnMainSync {
                val dataSources = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(client()))
                val source = DashMediaSource.Factory(dataSources).createMediaSource(
                    MediaItem.Builder().setUri(Uri.parse(stream.url)).setMimeType(MimeTypes.APPLICATION_MPD).build()
                )
                player = ExoPlayer.Builder(context).setLoadControl(
                    DefaultLoadControl.Builder().setBufferDurationsMs(500, 2_500, 250, 500).build()
                ).build().also { ownedPlayer = it }.apply {
                    volume = 0f
                    addListener(object : Player.Listener {
                        override fun onPlayerError(failure: PlaybackException) { error.set(failure.errorCodeName) }
                    })
                    setMediaSource(source)
                    prepare()
                    play()
                }
            }
            val deadline = SystemClock.elapsedRealtime() + 30_000
            var position = 0L
            var rendered = 0
            var mime = ""
            var sampleRate = 0
            var playing = false
            while (SystemClock.elapsedRealtime() < deadline && position < 2_000 && error.get() == null) {
                instrumentation.runOnMainSync {
                    position = player.currentPosition
                    player.audioDecoderCounters?.let { counters -> counters.ensureUpdated(); rendered = counters.renderedOutputBufferCount }
                    mime = player.audioFormat?.sampleMimeType.orEmpty()
                    sampleRate = player.audioFormat?.sampleRate ?: 0
                    playing = player.isPlaying
                }
                SystemClock.sleep(100)
            }
            check(error.get() == null) { "Media3 ${error.get()}" }
            assertTrue("Playback did not advance", position >= 2_000 && playing)
            assertTrue("Audio decoder produced no output buffers", rendered > 0)
            assertEquals(MimeTypes.AUDIO_FLAC, mime)
            assertEquals(44_100, sampleRate)
            return JSONObject().put("playedMs", position).put("renderedOutputBuffers", rendered)
                .put("decoderMimeType", mime).put("decoderSampleRateHz", sampleRate).put("muted", true)
        } finally {
            instrumentation.runOnMainSync { ownedPlayer?.release() }
        }
    }
}
