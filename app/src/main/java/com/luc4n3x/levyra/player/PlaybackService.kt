package com.luc4n3x.levyra.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import com.luc4n3x.levyra.widget.LevyraWidgetBridge
import com.luc4n3x.levyra.widget.LevyraWidgetCenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private lateinit var autoLibrary: AndroidAutoLibrary
    private lateinit var queueEngine: PersistentQueueEngine
    private lateinit var resolver: PlaybackResolver
    private lateinit var musicRepository: YoutubeMusicRepository
    private lateinit var sharedMediaSourceFactory: MediaSource.Factory
    private val adaptivePlaybackPolicy by lazy { AdaptivePlaybackPolicy(this) }
    private var preparedQueuePlayer: ExoPlayer? = null
    private var preparedQueueTrackId: String = ""
    private var queueSkipJob: Job? = null
    private var servicePrefetchJob: Job? = null

    companion object {
        const val EXTRA_VIDEO_URL = "levyra.videoUrl"
        const val EXTRA_VIDEO_CACHE_KEY = "levyra.videoCacheKey"
        private const val ACTION_QUEUE_PREVIOUS = "levyra.queue.previous"
        private const val ACTION_QUEUE_NEXT = "levyra.queue.next"

        @Volatile
        var activePlayer: ExoPlayer? = null
            private set

        @Volatile
        private var activeService: PlaybackService? = null

        fun requestQueueNext(): Boolean {
            val service = activeService ?: return false
            service.skipQueue(forward = true, respectRepeatOne = false)
            return true
        }

        fun requestQueuePrevious(): Boolean {
            val service = activeService ?: return false
            service.skipQueue(forward = false, respectRepeatOne = false)
            return true
        }

        fun prepareQueueNext(track: com.luc4n3x.levyra.domain.Track): Boolean {
            val service = activeService ?: return false
            service.prepareSecondaryQueuePlayer(track)
            return true
        }

        fun clearPreparedQueueNext() {
            activeService?.releasePreparedQueuePlayer()
        }

        fun consumePreparedQueueNext(trackId: String) {
            activeService?.consumePreparedQueuePlayer(trackId)
        }

        val normalizationProcessor = NormalizationAudioProcessor()
        val visualizerProcessor = VisualizerAudioProcessor()
        val premiumAudioEffects = PremiumAudioEffects()

        fun applyPremiumAudioSettings(settings: LevyraAudioSettings) {
            premiumAudioEffects.apply(settings)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val queuePreviousCommand by lazy { SessionCommand(ACTION_QUEUE_PREVIOUS, Bundle.EMPTY) }
    private val queueNextCommand by lazy { SessionCommand(ACTION_QUEUE_NEXT, Bundle.EMPTY) }

    override fun onCreate() {
        super.onCreate()
        queueEngine = PersistentQueueEngine.get(this)
        resolver = PlaybackResolver.getInstance(this)
        musicRepository = YoutubeMusicRepository(this)
        autoLibrary = AndroidAutoLibrary(this)
        val bufferProfile = AdaptivePlaybackPolicy(this).serviceBuffers()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferProfile.minBufferMs,
                bufferProfile.maxBufferMs,
                bufferProfile.playbackBufferMs,
                bufferProfile.rebufferMs
            )
            .setBackBuffer(bufferProfile.backBufferMs, false)
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
            .setFragmentSize(256L * 1024L)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(cacheSinkFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val defaultFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        val localDataSourceFactory = DefaultDataSource.Factory(this)

        val mergingFactory = LevyraMediaSourceFactory(defaultFactory, cacheDataSourceFactory, localDataSourceFactory)
        sharedMediaSourceFactory = mergingFactory

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
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        val audioSessionId = runCatching {
            val manager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            manager.generateAudioSessionId().takeIf { it > 0 } ?: 0
        }.getOrDefault(0)
        val attachedAudioSessionId = if (audioSessionId > 0) {
            runCatching {
                player.javaClass.getMethod("setAudioSessionId", Int::class.javaPrimitiveType).invoke(player, audioSessionId)
                audioSessionId
            }.getOrDefault(0)
        } else {
            0
        }
        val prefs = LevyraPreferences(this)
        val snapshot = prefs.snapshot()
        player.skipSilenceEnabled = snapshot.skipSilence
        normalizationProcessor.enabled = snapshot.audioNormalization || snapshot.audioSettings.replayGainEnabled
        val resolvedAudioSessionId = attachedAudioSessionId.takeIf { it > 0 } ?: runCatching {
            player.javaClass.getMethod("getAudioSessionId").invoke(player) as? Int ?: 0
        }.getOrDefault(0)
        premiumAudioEffects.bind(resolvedAudioSessionId)
        applyPremiumAudioSettings(snapshot.audioSettings)

        activePlayer = player
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && LevyraWidgetBridge.onNext == null) {
                    skipQueue(forward = true, respectRepeatOne = true)
                }
            }
        })
        serviceScope.launch {
            while (isActive) {
                if (player.mediaItemCount > 0) queueEngine.updatePosition(player.currentPosition)
                delay(2_000L)
            }
        }

        val queuePreviousButton = CommandButton.Builder(CommandButton.ICON_PREVIOUS)
            .setDisplayName("Precedente")
            .setSessionCommand(queuePreviousCommand)
            .build()
        val queueNextButton = CommandButton.Builder(CommandButton.ICON_NEXT)
            .setDisplayName("Successivo")
            .setSessionCommand(queueNextCommand)
            .build()

        val callback = object : MediaLibrarySession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                    .buildUpon()
                    .add(queuePreviousCommand)
                    .add(queueNextCommand)
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .build()
            }

            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                isForPlayback: Boolean
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                serviceScope.launch(Dispatchers.IO) {
                    runCatching {
                        if (queueEngine.state.value.tracks.isEmpty()) {
                            queueEngine.restore(
                                fallbackTracks = emptyList(),
                                fallbackIndex = -1,
                                fallbackPositionMs = 0L,
                                fallbackRadioEnabled = true
                            )
                        }
                        val snapshot = queueEngine.state.value
                        val track = snapshot.currentTrack ?: error("Nessun brano da ripristinare")
                        val item = if (isForPlayback) {
                            val resolved = if (track.streamUrl.startsWith("content://") || track.streamUrl.startsWith("file://")) {
                                track
                            } else {
                                resolveQueueTrack(track)
                            }
                            queueEngine.updateTrackAt(snapshot.currentIndex, resolved)
                            prefetchServiceQueueNext()
                            LevyraMediaItemFactory.build(resolved)
                        } else {
                            LevyraMediaItemFactory.metadataOnly(track)
                        }
                        MediaSession.MediaItemsWithStartPosition(
                            listOf(item),
                            0,
                            snapshot.positionMs.coerceAtLeast(0L)
                        )
                    }.onSuccess(future::set).onFailure(future::setException)
                }
                return future
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                return when (customCommand.customAction) {
                    ACTION_QUEUE_PREVIOUS -> {
                        skipQueue(forward = false, respectRepeatOne = false)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_QUEUE_NEXT -> {
                        skipQueue(forward = true, respectRepeatOne = false)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    else -> super.onCustomCommand(session, controller, customCommand, args)
                }
            }
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                return Futures.immediateFuture(LibraryResult.ofItem(autoLibrary.root(), params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                return libraryListFuture(params) {
                    paginate(autoLibrary.children(parentId), page, pageSize)
                }
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<MediaItem>> {
                return libraryItemFuture(null) { autoLibrary.item(mediaId) }
            }

            override fun onSearch(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<Void>> {
                autoLibrary.preloadSearch(query)
                return Futures.immediateFuture(LibraryResult.ofVoid())
            }

            override fun onGetSearchResult(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                return libraryListFuture(params) {
                    paginate(autoLibrary.search(query), page, pageSize)
                }
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: List<MediaItem>
            ): ListenableFuture<List<MediaItem>> {
                return mediaItemsFuture { autoLibrary.playableItems(mediaItems) }
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
            .setMediaButtonPreferences(ImmutableList.of(queuePreviousButton, queueNextButton))
            .build()

        val notificationProvider = DefaultMediaNotificationProvider(this)
        setMediaNotificationProvider(notificationProvider)
        activeService = this
    }

    private fun skipQueue(forward: Boolean, respectRepeatOne: Boolean) {
        queueSkipJob?.cancel()
        servicePrefetchJob?.cancel()
        releasePreparedQueuePlayer()
        queueSkipJob = serviceScope.launch {
            val player = activePlayer ?: return@launch
            if (!forward && player.currentPosition > 5_000L) {
                player.seekTo(0L)
                queueEngine.updatePosition(0L)
                return@launch
            }
            val target = withContext(Dispatchers.IO) {
                if (queueEngine.state.value.tracks.isEmpty()) {
                    queueEngine.restore(
                        fallbackTracks = emptyList(),
                        fallbackIndex = -1,
                        fallbackPositionMs = 0L,
                        fallbackRadioEnabled = true
                    )
                }
                var selected = if (forward) queueEngine.next(respectRepeatOne) else queueEngine.previous()
                if (selected == null && forward && queueEngine.state.value.radioEnabled) {
                    val seed = queueEngine.state.value.currentTrack
                    if (seed != null) {
                        val additions = runCatching {
                            musicRepository.radio(seed, LevyraPreferences(this@PlaybackService).languageCode(), 20)
                        }.onFailure { Timber.w(it, "Background radio expansion failed") }
                            .getOrDefault(emptyList())
                        if (additions.isNotEmpty()) {
                            queueEngine.appendRadioTracks(additions)
                            selected = queueEngine.next(respectRepeatOne = false)
                        }
                    }
                }
                selected
            } ?: return@launch
            val resolved = withContext(Dispatchers.IO) {
                runCatching { resolveQueueTrack(target) }
                    .onFailure { Timber.w(it, "Background queue resolution failed") }
                    .getOrNull()
            } ?: return@launch
            queueEngine.updateTrackAt(queueEngine.state.value.currentIndex, resolved)
            player.setMediaItem(LevyraMediaItemFactory.build(resolved))
            player.prepare()
            player.play()
            queueEngine.updatePosition(0L)
            LevyraWidgetCenter.update(
                this@PlaybackService,
                resolved.title,
                resolved.artist,
                resolved.largeThumbnailUrl.ifBlank { resolved.thumbnailUrl },
                true
            )
            prefetchServiceQueueNext()
        }
    }

    private fun prefetchServiceQueueNext() {
        servicePrefetchJob?.cancel()
        val generation = queueEngine.state.value.generation
        servicePrefetchJob = serviceScope.launch {
            val target = withContext(Dispatchers.IO) {
                queueEngine.upcoming(1).firstOrNull()
            } ?: return@launch
            val resolved = withContext(Dispatchers.IO) {
                runCatching { resolveQueueTrack(target) }
                    .onFailure { Timber.d(it, "Service queue prefetch skipped") }
                    .getOrNull()
            } ?: return@launch
            if (queueEngine.state.value.generation != generation) return@launch
            prepareSecondaryQueuePlayer(resolved)
        }
    }

    private suspend fun resolveQueueTrack(track: com.luc4n3x.levyra.domain.Track): com.luc4n3x.levyra.domain.Track {
        if (track.streamUrl.startsWith("content://", true) || track.streamUrl.startsWith("file://", true)) return track
        val hasYoutubeIdentity = track.videoUrl.contains("youtube.com", true) ||
            track.videoUrl.contains("youtu.be", true) ||
            Regex("^[A-Za-z0-9_-]{11}$").matches(track.id)
        val candidate = if (hasYoutubeIdentity) {
            track
        } else {
            val query = listOf(track.title, track.artist).filter { it.isNotBlank() }.joinToString(" ")
            val match = musicRepository.searchOne(query, LevyraPreferences(this).languageCode())
            match?.copy(
                title = track.title.ifBlank { match.title },
                artist = track.artist.ifBlank { match.artist },
                album = track.album.ifBlank { match.album },
                thumbnailUrl = track.thumbnailUrl.ifBlank { match.thumbnailUrl },
                largeThumbnailUrl = track.largeThumbnailUrl.ifBlank { match.largeThumbnailUrl },
                accentStart = track.accentStart,
                accentEnd = track.accentEnd
            ) ?: track
        }
        return resolver.resolve(candidate)
    }

    private fun prepareSecondaryQueuePlayer(track: com.luc4n3x.levyra.domain.Track) {
        if (track.streamUrl.isBlank()) return
        val plan = adaptivePlaybackPolicy.current(videoMode = false)
        if (plan.lowRam || plan.powerConstrained) {
            releasePreparedQueuePlayer()
            return
        }
        if (preparedQueueTrackId == track.id && preparedQueuePlayer != null) return
        releasePreparedQueuePlayer()
        val preloadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(250, 7_000, 100, 180)
            .setBackBuffer(0, false)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        preparedQueuePlayer = ExoPlayer.Builder(this)
            .setLoadControl(preloadControl)
            .setMediaSourceFactory(sharedMediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false
            )
            .setHandleAudioBecomingNoisy(false)
            .build()
            .apply {
                volume = 0f
                playWhenReady = false
                setMediaItem(LevyraMediaItemFactory.build(track))
                prepare()
            }
        preparedQueueTrackId = track.id
    }

    private fun consumePreparedQueuePlayer(trackId: String) {
        if (preparedQueueTrackId == trackId) releasePreparedQueuePlayer()
    }

    private fun releasePreparedQueuePlayer() {
        preparedQueuePlayer?.release()
        preparedQueuePlayer = null
        preparedQueueTrackId = ""
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            releasePreparedQueuePlayer()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        queueSkipJob?.cancel()
        servicePrefetchJob?.cancel()
        releasePreparedQueuePlayer()
        queueEngine.flushBlocking()
        if (activeService === this) activeService = null
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        activePlayer = null
        mediaSession = null
        premiumAudioEffects.release()
        LevyraMediaCache.release()
        super.onDestroy()
    }

    private fun libraryItemFuture(
        params: LibraryParams?,
        block: suspend () -> MediaItem
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val future = SettableFuture.create<LibraryResult<MediaItem>>()
        serviceScope.launch(Dispatchers.IO) {
            val result = runCatching { LibraryResult.ofItem(block(), params) }
                .getOrElse { error ->
                    Timber.w(error, "Android Auto item load failed")
                    LibraryResult.ofItem(autoLibrary.root(), params)
                }
            future.set(result)
        }
        return future
    }

    private fun libraryListFuture(
        params: LibraryParams?,
        block: suspend () -> List<MediaItem>
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        serviceScope.launch(Dispatchers.IO) {
            val items = runCatching { block() }
                .getOrElse { error ->
                    Timber.w(error, "Android Auto children load failed")
                    emptyList()
                }
            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }
        return future
    }

    private fun mediaItemsFuture(block: suspend () -> List<MediaItem>): ListenableFuture<List<MediaItem>> {
        val future = SettableFuture.create<List<MediaItem>>()
        serviceScope.launch(Dispatchers.IO) {
            val items = runCatching { block() }
                .getOrElse { error ->
                    Timber.w(error, "Android Auto media item resolve failed")
                    emptyList()
                }
            future.set(items)
        }
        return future
    }

    private fun paginate(items: List<MediaItem>, page: Int, pageSize: Int): List<MediaItem> {
        if (pageSize <= 0) return items
        val safePage = page.coerceAtLeast(0)
        val from = safePage.toLong() * pageSize.toLong()
        if (from >= items.size) return emptyList()
        val start = from.toInt()
        val end = (start + pageSize).coerceAtMost(items.size)
        return items.subList(start, end)
    }
}

@UnstableApi
private class LevyraMediaSourceFactory(
    private val delegate: DefaultMediaSourceFactory,
    private val dataSourceFactory: DataSource.Factory,
    private val localDataSourceFactory: DataSource.Factory
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
        val localUri = mediaItem.localConfiguration?.uri
        val scheme = localUri?.scheme.orEmpty().lowercase()
        if (scheme == "content" || scheme == "file") {
            val localItem = mediaItem.buildUpon().setCustomCacheKey(null).build()
            return ProgressiveMediaSource.Factory(localDataSourceFactory).createMediaSource(localItem)
        }
        val uri = localUri?.toString().orEmpty()
        return if (isHlsManifestUri(uri)) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }
    }

    private fun isHlsManifestUri(uri: String): Boolean {
        val clean = uri.substringBefore('#').lowercase()
        val path = clean.substringBefore('?')
        return path.endsWith(".m3u8") ||
            path.contains("/hls_playlist") ||
            path.contains("/manifest/hls") ||
            clean.contains("mime=application%2fx-mpegurl") ||
            clean.contains("mime=application/vnd.apple.mpegurl") ||
            clean.contains("type=application%2fx-mpegurl")
    }
}
