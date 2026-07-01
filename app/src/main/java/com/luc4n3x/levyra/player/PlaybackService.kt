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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    companion object {
        /** Chiave extra nel MediaItem per lo stream video-only da mergiare con l'audio. */
        const val EXTRA_VIDEO_URL = "levyra.videoUrl"

        /**
         * ExoPlayer reale della sessione attiva. Serve alla UI per collegare la video
         * surface (PlayerView) direttamente al player che decodifica i frame.
         */
        @Volatile
        var activePlayer: ExoPlayer? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2_500, 50_000, 150, 350)
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
            .setFragmentSize(2L * 1024L * 1024L)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(cacheSinkFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Factory di default: gestisce progressive (audio-only, muxed) e HLS.
        val defaultFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        // Factory custom: se il MediaItem porta un extra video, costruisce un
        // MergingMediaSource(video-only + audio-only). Altrimenti delega al default.
        val mergingFactory = LevyraMediaSourceFactory(defaultFactory, cacheDataSourceFactory)

        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
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
        player.skipSilenceEnabled = LevyraPreferences(this).skipSilence()
        activePlayer = player
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        activePlayer = null
        mediaSession = null
        LevyraMediaCache.release()
        super.onDestroy()
    }
}

/**
 * MediaSource.Factory che intercetta i MediaItem con extra [PlaybackService.EXTRA_VIDEO_URL].
 * Quando presente, costruisce un [MergingMediaSource] combinando la traccia video-only
 * (uri video) con la traccia audio-only (uri principale del MediaItem). È il pattern usato
 * da NewPipe/Metrolist per riprodurre i music video di YouTube, dove YouTube fornisce
 * audio e video come stream adaptive separati.
 */
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
            // Audio-only, muxed o HLS: percorso standard.
            return delegate.createMediaSource(mediaItem)
        }

        // MediaItem principale = traccia audio (uri già impostato).
        val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        val videoItem = MediaItem.fromUri(videoUrl)
        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(videoItem)

        // video prima, audio dopo: il timeline segue la traccia più lunga.
        return MergingMediaSource(
            /* adjustPeriodTimeOffsets = */ true,
            /* clipDurations = */ false,
            videoSource,
            audioSource
        )
    }
}
