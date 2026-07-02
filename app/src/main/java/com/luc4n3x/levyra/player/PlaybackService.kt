package com.luc4n3x.levyra.player

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import android.app.PendingIntent
import com.luc4n3x.levyra.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null

    companion object {
        const val EXTRA_VIDEO_URL = "levyra.videoUrl"
        const val EXTRA_VIDEO_CACHE_KEY = "levyra.videoCacheKey"

        @Volatile
        var activePlayer: ExoPlayer? = null
            private set
            
        val normalizationProcessor = NormalizationAudioProcessor()
        val visualizerProcessor = VisualizerAudioProcessor()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var autoPlayManager: AutoPlayManager? = null

    override fun onCreate() {
        super.onCreate()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(32_000, 32_000, 0, 0)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        val okHttpClient = LevyraHttpClientFactory.media(this)
        val upstreamFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("LevyraPlayer/1.13 Android Music")
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Accept-Encoding" to "identity",
                    "Connection" to "keep-alive"
                )
            )
        val cache = LevyraMediaCache.get(this)
        val cacheSinkFactory = CacheDataSink.Factory()
            .setCache(cache)
            .setFragmentSize(512L * 1024L)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(cacheSinkFactory)
            .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val defaultFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        val mergingFactory = LevyraMediaSourceFactory(defaultFactory, cacheDataSourceFactory)

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(normalizationProcessor, visualizerProcessor))
                    .build()
            }
        }

        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(mergingFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        val prefs = LevyraPreferences(this)
        player.skipSilenceEnabled = prefs.snapshot().skipSilence
        normalizationProcessor.enabled = prefs.snapshot().audioNormalization
        
        activePlayer = player
        
        val callback = object : MediaLibrarySession.Callback {
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                return Futures.immediateFuture(LibraryResult.ofItem(
                    MediaItem.Builder().setMediaId("root").build(), params
                ))
            }
        }
        
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        mediaSession = MediaLibrarySession.Builder(this, player, callback)
            .setSessionActivity(sessionActivity)
            .build()
            
        val notificationProvider = DefaultMediaNotificationProvider(this)
        setMediaNotificationProvider(notificationProvider)
        
        autoPlayManager = AutoPlayManager(this, player, serviceScope)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        activePlayer = null
        mediaSession = null
        autoPlayManager = null
        LevyraMediaCache.release()
        super.onDestroy()
    }
}

@UnstableApi
private class LevyraMediaSourceFactory(
    private val delegate: DefaultMediaSourceFactory,
    private val dataSourceFactory: DataSource.Factory
) : MediaSource.Factory {

    override fun getSupportedTypes(): IntArray = delegate.supportedTypes

    override fun setDrmSessionManagerProvider(
        provider: androidx.media3.exoplayer.drm.DrmSessionManagerProvider
    ): MediaSource.Factory {
        delegate.setDrmSessionManagerProvider(provider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(
        policy: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
    ): MediaSource.Factory {
        delegate.setLoadErrorHandlingPolicy(policy)
        return this
    }

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val videoUrl = mediaItem.mediaMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_URL)
            ?: mediaItem.requestMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_URL)

        if (videoUrl.isNullOrBlank()) {
            return mediaSourceFor(mediaItem)
        }

        val videoCacheKey = mediaItem.mediaMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_CACHE_KEY)
            ?: mediaItem.requestMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_CACHE_KEY)

        val audioSource = mediaSourceFor(mediaItem)
        val videoItem = MediaItem.Builder()
            .setUri(videoUrl)
            .apply { if (!videoCacheKey.isNullOrBlank()) setCustomCacheKey(videoCacheKey) }
            .build()
        val videoSource = mediaSourceFor(videoItem)

        return MergingMediaSource(true, false, videoSource, audioSource)
    }

    private fun mediaSourceFor(mediaItem: MediaItem): MediaSource {
        val uri = mediaItem.localConfiguration?.uri?.toString().orEmpty()
        return if (uri.contains(".m3u8", true) || uri.contains("hls", true)) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }
    }
}
