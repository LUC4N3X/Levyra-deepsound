package com.luc4n3x.levyra.data.hqaudio.lossless

import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackQuery
import com.luc4n3x.levyra.data.hqaudio.HighQualityAudioProvider
import com.luc4n3x.levyra.data.hqaudio.HighQualityAudioResolver
import com.luc4n3x.levyra.data.hqaudio.HighQualityMappingStorage
import com.luc4n3x.levyra.data.hqaudio.HighQualityMappingStore
import com.luc4n3x.levyra.data.hqaudio.HighQualityProviderHttpClient
import com.luc4n3x.levyra.data.hqaudio.HighQualityResolution
import com.luc4n3x.levyra.data.hqaudio.OkHttpProviderExchange
import com.luc4n3x.levyra.data.hqaudio.tidal.TidalAudioProvider
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import java.io.File
import com.luc4n3x.levyra.domain.HighQualityAudioMode
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class LosslessFallbackLiveTest {
    private val getLucky = AlternativeTrackQuery("Get Lucky", "Daft Punk", "Random Access Memories", 369_000L, false)

    @Test
    fun tidalSiteStreamsTheExactAlbumCutToTheEnd() = verifyPlayback(getLucky, "tidal")

    @Test
    fun kuwoSiteStreamsTheExactAlbumCutToTheEnd() = verifyPlayback(getLucky, "kuwo")

    @Test
    fun tidalSiteMatchesBillieJean() = verifyPlayback(
        AlternativeTrackQuery("Billie Jean", "Michael Jackson", "Thriller", 294_000L, false), "tidal"
    )

    @Test
    fun kuwoSiteMatchesBlindingLights() = verifyPlayback(
        AlternativeTrackQuery("Blinding Lights", "The Weeknd", "After Hours", 200_000L, false), "kuwo"
    )

    @Test
    fun availableLosslessProviderPlaysAfterOtherProvidersFail() = verifyPlayback(
        AlternativeTrackQuery("One More Time", "Daft Punk", "Discovery", 320_000L, false)
    )

    @Test
    fun billieJeanPlaysVerifiedFlac() = verifyPlayback(
        AlternativeTrackQuery("Billie Jean", "Michael Jackson", "Thriller", 294_000L, false)
    )

    @Test
    fun blindingLightsPlaysVerifiedFlac() = verifyPlayback(
        AlternativeTrackQuery("Blinding Lights", "The Weeknd", "After Hours", 200_000L, false)
    )

    @Test
    fun getLuckyPlaysVerifiedFlac() = verifyPlayback(
        AlternativeTrackQuery("Get Lucky", "Daft Punk", "Random Access Memories", 369_000L, false)
    )

    @Test
    fun russianKinoTrackPlaysVerifiedFlac() = verifyPlayback(
        AlternativeTrackQuery("Группа крови", "КИНО", "Последний герой", 235_000L, false)
    )

    @Test
    fun russianMolchatDomaTrackPlaysVerifiedFlac() = verifyPlayback(
        AlternativeTrackQuery("Звезды", "Молчат дома", "Звезды", 230_000L, false)
    )

    private companion object {
        private var providers: List<HighQualityAudioProvider>? = null

        @Synchronized
        fun sharedProviders(cacheDir: File): List<HighQualityAudioProvider> = providers ?: listOf(
            TidalAudioProvider(File(cacheDir, "tidal-manifests")),
            KuwoAudioProvider(OkHttpProviderExchange(HighQualityProviderHttpClient::client))
        ).also { providers = it }
    }

    private fun verifyPlayback(query: AlternativeTrackQuery, only: String? = null): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val storage = object : HighQualityMappingStorage {
            override fun read(key: String): String? = null
            override fun write(key: String, value: String) = Unit
            override fun remove(key: String) = Unit
            override fun keys(): Set<String> = emptySet()
        }
        val resolver = HighQualityAudioResolver(
            providers = sharedProviders(instrumentation.targetContext.cacheDir).filter { only == null || it.id == only },
            mappingStore = HighQualityMappingStore(storage),
            scope = scope
        ).apply { mode = HighQualityAudioMode.PREFER_LOSSLESS }
        var ownedPlayer: ExoPlayer? = null
        val error = AtomicReference<String?>()
        try {
            val started = SystemClock.elapsedRealtime()
            val selected = withTimeout(20_000L) {
                resolver.begin("live-${query.artist}-${query.title}", query).await()
            }
            assertTrue("Lossless selection required: $selected", selected is HighQualityResolution.Selected)
            val stream = (selected as HighQualityResolution.Selected).stream
            assertTrue(stream.tier.lossless)
            assertEquals("flac", stream.codec)
            only?.let { assertEquals(it, stream.providerId) }
            val evaluation = selected.evaluation
            assertTrue("Matched cut must be within 5 s", kotlin.math.abs(evaluation.candidate.durationSeconds * 1_000L - query.durationMs) <= 5_000L)
            Log.i("LosslessFallbackLiveTest", "title=${query.title} candidate=${evaluation.candidate.providerTrackId} '${evaluation.candidate.title}' album='${evaluation.candidate.album}' verdict=${evaluation.verdict} bits=${stream.bitDepth} rate=${stream.sampleRateHz}")
            Log.i("LosslessFallbackLiveTest", "title=${query.title} selected=${stream.providerId} resolveMs=${SystemClock.elapsedRealtime() - started}")
            instrumentation.runOnMainSync {
                val dataSource = DefaultHttpDataSource.Factory().setConnectTimeoutMs(6_000).setReadTimeoutMs(15_000)
                ownedPlayer = ExoPlayer.Builder(instrumentation.targetContext).build().apply {
                    volume = 0.05f
                    addListener(object : Player.Listener {
                        override fun onPlayerError(failure: PlaybackException) {
                            error.set(failure.errorCodeName)
                        }
                    })
                    val item = MediaItem.Builder().setUri(stream.url).setMimeType(stream.mimeType).build()
                    setMediaSource(
                        if (stream.deliveryMethod == PlaybackDeliveryMethod.DASH) {
                            DashMediaSource.Factory(DefaultDataSource.Factory(instrumentation.targetContext, dataSource)).createMediaSource(item)
                        } else {
                            ProgressiveMediaSource.Factory(dataSource).createMediaSource(item)
                        }
                    )
                    prepare()
                    play()
                }
            }
            val deadline = SystemClock.elapsedRealtime() + 30_000L
            var playing = false
            var position = 0L
            var mime: String? = null
            while (SystemClock.elapsedRealtime() < deadline && position < 2_000L && error.get() == null) {
                instrumentation.runOnMainSync {
                    playing = ownedPlayer!!.isPlaying
                    position = ownedPlayer!!.currentPosition
                    mime = ownedPlayer!!.audioFormat?.sampleMimeType
                }
                SystemClock.sleep(100L)
            }
            assertEquals("Playback error", null, error.get())
            assertTrue("Playback must advance, position=$position playing=$playing", playing && position >= 2_000L)
            assertEquals(MimeTypes.AUDIO_FLAC, mime)
            var duration = 0L
            instrumentation.runOnMainSync { duration = ownedPlayer!!.duration }
            assertTrue("Full-length stream required, duration=$duration", kotlin.math.abs(duration - query.durationMs) <= 5_000L)
            instrumentation.runOnMainSync { ownedPlayer!!.seekTo(duration - 8_000L) }
            val tailDeadline = SystemClock.elapsedRealtime() + 30_000L
            while (SystemClock.elapsedRealtime() < tailDeadline && position < duration - 6_000L && error.get() == null) {
                instrumentation.runOnMainSync { position = ownedPlayer!!.currentPosition }
                SystemClock.sleep(100L)
            }
            assertEquals("Tail playback error", null, error.get())
            assertTrue("Tail must play, position=$position duration=$duration", position >= duration - 6_000L)
            Log.i("LosslessFallbackLiveTest", "title=${query.title} playing=true positionMs=$position mime=$mime provider=${stream.providerId}")
        } finally {
            instrumentation.runOnMainSync { ownedPlayer?.release() }
            scope.cancel()
        }
    }
}
