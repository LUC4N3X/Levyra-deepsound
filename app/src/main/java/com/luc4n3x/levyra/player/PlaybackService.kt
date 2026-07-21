package com.luc4n3x.levyra.player

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.dash.DashMediaSource
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
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import com.luc4n3x.levyra.player.queue.PlaybackQueueSnapshot
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
import java.io.IOException

@UnstableApi
class PlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private lateinit var autoLibrary: AndroidAutoLibrary
    private lateinit var queueEngine: PersistentQueueEngine
    private lateinit var resolver: PlaybackResolver
    private lateinit var musicRepository: YoutubeMusicRepository
    private lateinit var sharedMediaSourceFactory: MediaSource.Factory
    private val adaptivePlaybackPolicy by lazy { AdaptivePlaybackPolicy(this) }
    private val playbackWarmup by lazy { PlaybackWarmup(this) }
    private var queueSkipJob: Job? = null
    private var servicePrefetchJob: Job? = null
    private var serviceRecoveryJob: Job? = null
    private var recoveryResetJob: Job? = null
    private var stickyRestoreJob: Job? = null
    private var playbackWatchdogJob: Job? = null
    private lateinit var playbackWakeLock: PowerManager.WakeLock
    private lateinit var playbackStateStore: SharedPreferences
    private var serviceRecoveryAttempts = 0
    private var serviceRecoveryExhausted = false
    private var watchdogPositionMs = C.TIME_UNSET
    private var watchdogAdvancedAtMs = 0L
    private var lastPlaybackExpected: Boolean? = null
    private var lastPlaybackHeartbeatAtMs = 0L
    private var appliedPlayerWakeMode = C.WAKE_MODE_NETWORK

    companion object {
        private const val RUNNING_LOW_LEVEL = 10
        private const val RUNNING_CRITICAL_LEVEL = 15
        const val EXTRA_VIDEO_URL = "levyra.videoUrl"
        const val EXTRA_VIDEO_CACHE_KEY = "levyra.videoCacheKey"
        const val EXTRA_VIDEO_MIME_TYPE = "levyra.videoMimeType"
        const val EXTRA_VIDEO_MODE = "levyra.videoMode"
        const val EXTRA_YOUTUBE_LOUDNESS_DB = "levyra.youtubeLoudnessDb"
        const val EXTRA_YOUTUBE_PERCEPTUAL_LOUDNESS_DB = "levyra.youtubePerceptualLoudnessDb"
        private const val ACTION_QUEUE_PREVIOUS = "levyra.queue.previous"
        private const val ACTION_QUEUE_NEXT = "levyra.queue.next"
        private const val PLAYBACK_STATE_PREFS = "levyra.playback.service.state"
        private const val KEY_PLAYBACK_EXPECTED = "playbackExpected"
        private const val KEY_PLAYBACK_HEARTBEAT_AT = "playbackHeartbeatAt"
        private const val PLAYBACK_HEARTBEAT_INTERVAL_MS = 30_000L
        private const val STICKY_RESTORE_MAX_AGE_MS = 12L * 60L * 60L * 1_000L
        private const val WATCHDOG_INTERVAL_MS = 5_000L
        private const val WATCHDOG_STALL_TIMEOUT_MS = 15_000L
        private val ONLINE_RECOVERY_DELAYS_MS = longArrayOf(500L, 2_000L, 5_000L, 10_000L)
        private val LOCAL_RECOVERY_DELAYS_MS = longArrayOf(250L, 750L, 1_500L, 3_000L, 5_000L, 10_000L)

        @Volatile
        var activePlayer: ExoPlayer? = null
            private set

        @Volatile
        private var activeService: PlaybackService? = null

        @Volatile
        private var uiRecoveryAvailable = false

        fun setUiRecoveryAvailable(available: Boolean) {
            uiRecoveryAvailable = available
        }

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

        fun prepareQueueNext(track: com.luc4n3x.levyra.domain.Track): Boolean = false

        fun clearPreparedQueueNext() = Unit

        fun consumePreparedQueueNext(trackId: String) = Unit

        val normalizationProcessor = NormalizationAudioProcessor()
        val spatialAudioProcessor = StereoSpatialAudioProcessor()
        val visualizerProcessor = VisualizerAudioProcessor()
        val premiumAudioEffects = PremiumAudioEffects()

        fun applyPremiumAudioSettings(settings: LevyraAudioSettings) {
            val normalized = settings.normalized()
            spatialAudioProcessor.strength = if (normalized.equalizerEnabled) normalized.virtualizer else 0
            premiumAudioEffects.apply(normalized)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val queuePreviousCommand by lazy { SessionCommand(ACTION_QUEUE_PREVIOUS, Bundle.EMPTY) }
    private val queueNextCommand by lazy { SessionCommand(ACTION_QUEUE_NEXT, Bundle.EMPTY) }

    override fun onCreate() {
        super.onCreate()
        playbackStateStore = getSharedPreferences(PLAYBACK_STATE_PREFS, Context.MODE_PRIVATE)
        playbackWakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:PlaybackService")
            .apply { setReferenceCounted(false) }
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
        val baseHttpFactory = PlaybackNetworkStack.playbackFactory(this)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Accept-Encoding" to "identity"
                )
            )
        val upstreamFactory = LevyraYoutubeDataSource.Factory(baseHttpFactory)
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
                    .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(normalizationProcessor, spatialAudioProcessor, visualizerProcessor))
                    .build()
            }
        }
        renderersFactory.setEnableDecoderFallback(true)

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
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updatePlayerWakeMode(player, mediaItem)
                if (serviceRecoveryJob?.isActive != true && stickyRestoreJob?.isActive != true) {
                    serviceRecoveryExhausted = false
                    serviceRecoveryAttempts = 0
                }
                val extras = mediaItem?.mediaMetadata?.extras
                val loudness = extras?.takeIf { it.containsKey(EXTRA_YOUTUBE_LOUDNESS_DB) }
                    ?.getFloat(EXTRA_YOUTUBE_LOUDNESS_DB)
                val perceptual = extras?.takeIf { it.containsKey(EXTRA_YOUTUBE_PERCEPTUAL_LOUDNESS_DB) }
                    ?.getFloat(EXTRA_YOUTUBE_PERCEPTUAL_LOUDNESS_DB)
                normalizationProcessor.setYoutubeLoudness(loudness, perceptual)
                watchdogPositionMs = C.TIME_UNSET
                watchdogAdvancedAtMs = SystemClock.elapsedRealtime()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) scheduleRecoveryReset(player)
                if (playbackState == Player.STATE_ENDED && LevyraWidgetBridge.onNext == null) {
                    skipQueue(forward = true, respectRepeatOne = true, autoAdvance = true)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                updatePlaybackProtection(player)
                scheduleServiceRecovery(error)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady && serviceRecoveryExhausted) {
                    serviceRecoveryExhausted = false
                    serviceRecoveryAttempts = 0
                    markPlaybackExpected(true, force = true)
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                updatePlaybackProtection(player)
            }
        })
        startPlaybackWatchdog(player)
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
                            val resolved = resolveQueueTrack(track)
                            queueEngine.updateTrackAt(snapshot.currentIndex, resolved)
                            if (!isLocalPlaybackTrack(resolved)) prefetchServiceQueueNext()
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null && !scheduleStickyPlaybackRestore(startId)) return START_NOT_STICKY
        return START_STICKY
    }

    private fun skipQueue(forward: Boolean, respectRepeatOne: Boolean, autoAdvance: Boolean = false) {
        queueSkipJob?.cancel()
        servicePrefetchJob?.cancel()
        queueSkipJob = serviceScope.launch {
            val player = activePlayer ?: return@launch
            if (rewindInsteadOfSkip(player, forward)) return@launch
            val target = withContext(Dispatchers.IO) {
                selectSkipTarget(forward, respectRepeatOne)
            } ?: return@launch
            val resolved = withContext(Dispatchers.IO) {
                resolveSkipTarget(target, autoAdvance)
            } ?: run {
                abandonAutoAdvance(target, autoAdvance)
                return@launch
            }
            playSkipTarget(player, resolved)
        }
    }

    private fun rewindInsteadOfSkip(player: ExoPlayer, forward: Boolean): Boolean {
        if (forward || player.currentPosition <= 5_000L) return false
        player.seekTo(0L)
        queueEngine.updatePosition(0L)
        return true
    }

    private suspend fun selectSkipTarget(forward: Boolean, respectRepeatOne: Boolean): com.luc4n3x.levyra.domain.Track? {
        if (queueEngine.state.value.tracks.isEmpty()) {
            queueEngine.restore(
                fallbackTracks = emptyList(),
                fallbackIndex = -1,
                fallbackPositionMs = 0L,
                fallbackRadioEnabled = true
            )
        }
        val selected = if (forward) queueEngine.next(respectRepeatOne) else queueEngine.previous()
        if (selected != null || !forward) return selected
        return expandRadioForSkip()
    }

    private suspend fun expandRadioForSkip(): com.luc4n3x.levyra.domain.Track? {
        if (!queueEngine.state.value.radioEnabled) return null
        val seed = queueEngine.state.value.currentTrack ?: return null
        if (isLocalPlaybackTrack(seed) || !hasInternetCapableNetwork()) return null
        val additions = runCatching {
            musicRepository.radio(seed, LevyraPreferences(this).languageCode(), 20)
        }.onFailure { Timber.w(it, "Background radio expansion failed") }
            .getOrDefault(emptyList())
        if (additions.isEmpty()) return null
        queueEngine.appendRadioTracks(additions)
        return queueEngine.next(respectRepeatOne = false)
    }

    private suspend fun resolveSkipTarget(
        target: com.luc4n3x.levyra.domain.Track,
        autoAdvance: Boolean
    ): com.luc4n3x.levyra.domain.Track? = if (autoAdvance) {
        resolveQueueTrackPersistently(target)
    } else {
        runCatching { resolveQueueTrack(target) }
            .onFailure { Timber.w(it, "Background queue resolution failed") }
            .getOrNull()
    }

    private fun abandonAutoAdvance(target: com.luc4n3x.levyra.domain.Track, autoAdvance: Boolean) {
        if (!autoAdvance) return
        Timber.e("Background queue auto-advance gave up for %s", target.title)
        markPlaybackExpected(false, force = true)
        releasePlaybackWakeLock()
    }

    private fun playSkipTarget(player: ExoPlayer, resolved: com.luc4n3x.levyra.domain.Track) {
        queueEngine.updateTrackAt(queueEngine.state.value.currentIndex, resolved)
        player.setMediaItem(LevyraMediaItemFactory.build(resolved))
        player.prepare()
        player.play()
        queueEngine.updatePosition(0L)
        LevyraWidgetCenter.update(
            this,
            resolved.title,
            resolved.artist,
            resolved.largeThumbnailUrl.ifBlank { resolved.thumbnailUrl },
            true
        )
        if (!isLocalPlaybackTrack(resolved)) prefetchServiceQueueNext()
    }

    private fun prefetchServiceQueueNext() {
        servicePrefetchJob?.cancel()
        val generation = queueEngine.state.value.generation
        servicePrefetchJob = serviceScope.launch {
            val target = withContext(Dispatchers.IO) {
                queueEngine.upcoming(1).firstOrNull()
            } ?: return@launch
            if (isLocalPlaybackTrack(target) || !hasInternetCapableNetwork()) return@launch
            val resolved = withContext(Dispatchers.IO) {
                runCatching { resolveQueueTrack(target) }
                    .onFailure { Timber.d(it, "Service queue prefetch skipped") }
                    .getOrNull()
            } ?: return@launch
            if (queueEngine.state.value.generation != generation) return@launch
            withContext(Dispatchers.IO) { runCatching { playbackWarmup.prime(resolved, 256L * 1024L) } }
        }
    }

    private suspend fun resolveQueueTrackPersistently(
        track: com.luc4n3x.levyra.domain.Track
    ): com.luc4n3x.levyra.domain.Track? {
        var attempts = 0
        while (isPlaybackRecoveryExpected() && !isStickyRestoreExpired()) {
            if (!isLocalPlaybackTrack(track) && !hasInternetCapableNetwork()) {
                releasePlaybackWakeLock()
                Timber.d("Background queue auto-advance waiting for network")
                delay(5_000L)
                attempts = 0
                continue
            }
            acquirePlaybackWakeLock()
            val resolved = runCatching { resolveQueueTrack(track) }
                .onFailure { Timber.w(it, "Background queue resolution failed") }
                .getOrNull()
            if (resolved != null) return resolved
            if (attempts >= ONLINE_RECOVERY_DELAYS_MS.lastIndex) return null
            delay(ONLINE_RECOVERY_DELAYS_MS[attempts])
            attempts++
        }
        return null
    }

    private suspend fun resolveQueueTrack(track: com.luc4n3x.levyra.domain.Track): com.luc4n3x.levyra.domain.Track {
        if (isLocalPlaybackTrack(track)) {
            if (!isLocalPlaybackUri(track.streamUrl)) {
                throw IOException("File offline non disponibile per ${track.title}")
            }
            return track.copy(videoStreamUrl = "")
        }
        val hasYoutubeIdentity = track.videoUrl.contains("youtube.com", true) ||
            track.videoUrl.contains("youtu.be", true) ||
            Regex("^[A-Za-z0-9_-]{11}$").matches(track.id)
        val candidate = if (hasYoutubeIdentity) {
            track
        } else {
            if (!hasInternetCapableNetwork()) throw IOException("Connessione Internet non disponibile")
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (shouldCancelPrefetchForMemoryPressure(level)) {
            servicePrefetchJob?.cancel()
        }
    }

    private fun shouldCancelPrefetchForMemoryPressure(level: Int): Boolean =
        level == RUNNING_LOW_LEVEL ||
            level == RUNNING_CRITICAL_LEVEL ||
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        val playbackExpected = playbackStateStore.getBoolean(KEY_PLAYBACK_EXPECTED, false)
        val keepAlive = player != null &&
            player.mediaItemCount > 0 &&
            player.playbackState != Player.STATE_ENDED &&
            (player.playWhenReady || playbackExpected)
        if (!keepAlive) pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        queueSkipJob?.cancel()
        servicePrefetchJob?.cancel()
        serviceRecoveryJob?.cancel()
        recoveryResetJob?.cancel()
        stickyRestoreJob?.cancel()
        playbackWatchdogJob?.cancel()
        mediaSession?.player?.let { queueEngine.updatePosition(it.currentPosition) }
        releasePlaybackWakeLock()
        if (activeService === this) activeService = null
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        activePlayer = null
        mediaSession = null
        premiumAudioEffects.release()
        super.onDestroy()
    }

    private fun updatePlaybackProtection(player: Player) {
        (player as? ExoPlayer)?.let { updatePlayerWakeMode(it, it.currentMediaItem) }
        val playbackExpected = !serviceRecoveryExhausted &&
            player.mediaItemCount > 0 &&
            player.playWhenReady &&
            player.playbackState != Player.STATE_ENDED
        if (playbackExpected) {
            acquirePlaybackWakeLock()
            markPlaybackExpected(true)
        } else if (!shouldPreservePlaybackExpectation(player)) {
            releasePlaybackWakeLock()
            markPlaybackExpected(false)
        }
    }

    private fun isPlaybackRecoveryInFlight(): Boolean =
        serviceRecoveryJob?.isActive == true ||
            stickyRestoreJob?.isActive == true ||
            queueSkipJob?.isActive == true

    private fun shouldPreservePlaybackExpectation(player: Player): Boolean =
        isPlaybackRecoveryInFlight() &&
            (player.mediaItemCount == 0 ||
                player.playbackState == Player.STATE_ENDED ||
                player.playbackState == Player.STATE_IDLE)

    @SuppressLint("WakelockTimeout")
    private fun acquirePlaybackWakeLock() {
        if (!playbackWakeLock.isHeld) playbackWakeLock.acquire()
    }

    private fun releasePlaybackWakeLock() {
        if (::playbackWakeLock.isInitialized && playbackWakeLock.isHeld) playbackWakeLock.release()
    }

    private fun markPlaybackExpected(expected: Boolean, force: Boolean = false) {
        val now = System.currentTimeMillis()
        val heartbeatDue = expected && now - lastPlaybackHeartbeatAtMs >= PLAYBACK_HEARTBEAT_INTERVAL_MS
        if (!force && lastPlaybackExpected == expected && !heartbeatDue) return
        lastPlaybackExpected = expected
        lastPlaybackHeartbeatAtMs = now
        playbackStateStore.edit()
            .putBoolean(KEY_PLAYBACK_EXPECTED, expected)
            .putLong(KEY_PLAYBACK_HEARTBEAT_AT, now)
            .apply()
    }

    private fun scheduleRecoveryReset(player: Player) {
        recoveryResetJob?.cancel()
        recoveryResetJob = serviceScope.launch {
            delay(10_000L)
            if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                serviceRecoveryAttempts = 0
                serviceRecoveryExhausted = false
            }
        }
    }

    private data class ServiceRecoveryPlan(
        val localPlayback: Boolean,
        val delaysMs: LongArray,
        val positionMs: Long
    )

    private fun scheduleServiceRecovery(error: PlaybackException) {
        val plan = serviceRecoveryPlan() ?: return
        serviceRecoveryJob = serviceScope.launch {
            runServiceRecovery(error, plan)
        }
    }

    private fun serviceRecoveryPlan(): ServiceRecoveryPlan? {
        if (serviceRecoveryJob?.isActive == true) return null
        if (!isPlaybackRecoveryExpected()) return null
        val localPlayback = isCurrentPlaybackLocal()
        return ServiceRecoveryPlan(
            localPlayback = localPlayback,
            delaysMs = if (localPlayback) LOCAL_RECOVERY_DELAYS_MS else ONLINE_RECOVERY_DELAYS_MS,
            positionMs = mediaSession?.player?.currentPosition?.coerceAtLeast(0L) ?: 0L
        )
    }

    private suspend fun runServiceRecovery(error: PlaybackException, plan: ServiceRecoveryPlan) {
        if (awaitUiRecovery(plan.localPlayback)) return
        var awaitedConnectivity = false
        while (isPlaybackRecoveryExpected()) {
            if (finishHealthyServiceRecovery()) return
            if (awaitRecoveryConnectivity(plan.localPlayback)) {
                awaitedConnectivity = true
                continue
            }
            if (awaitedConnectivity) {
                awaitedConnectivity = false
                serviceRecoveryAttempts = 0
            }
            if (finishExhaustedServiceRecovery(error, plan.delaysMs)) return
            if (attemptServiceRecovery(error, plan)) return
        }
        releasePlaybackWakeLock()
    }

    private fun isPlaybackRecoveryExpected(): Boolean =
        playbackStateStore.getBoolean(KEY_PLAYBACK_EXPECTED, false)

    private suspend fun awaitUiRecovery(localPlayback: Boolean): Boolean {
        if (!uiRecoveryAvailable || localPlayback) return false
        releasePlaybackWakeLock()
        repeat(8) {
            delay(750L)
            if (finishHealthyServiceRecovery()) return true
        }
        val player = mediaSession?.player
        return player != null &&
            player.playWhenReady &&
            player.playbackState == Player.STATE_BUFFERING
    }

    private fun finishHealthyServiceRecovery(): Boolean {
        if (!isPlaybackHealthy(mediaSession?.player)) return false
        serviceRecoveryAttempts = 0
        serviceRecoveryExhausted = false
        mediaSession?.player?.let(::updatePlaybackProtection)
        return true
    }

    private suspend fun awaitRecoveryConnectivity(localPlayback: Boolean): Boolean {
        if (localPlayback || hasInternetCapableNetwork()) return false
        releasePlaybackWakeLock()
        Timber.d("Background playback recovery waiting for network")
        delay(5_000L)
        return true
    }

    private fun finishExhaustedServiceRecovery(
        error: PlaybackException,
        delaysMs: LongArray
    ): Boolean {
        if (serviceRecoveryAttempts < delaysMs.size) return false
        serviceRecoveryExhausted = true
        mediaSession?.player?.pause()
        markPlaybackExpected(false, force = true)
        releasePlaybackWakeLock()
        Timber.e(error, "Background playback recovery exhausted")
        return true
    }

    private suspend fun attemptServiceRecovery(
        error: PlaybackException,
        plan: ServiceRecoveryPlan
    ): Boolean {
        val attempt = serviceRecoveryAttempts++
        acquirePlaybackWakeLock()
        delay(plan.delaysMs[attempt])
        val restored = restoreCurrentPlayback(
            positionMs = plan.positionMs,
            preferFreshResolution = !plan.localPlayback && hasInternetCapableNetwork()
        )
        if (restored) {
            Timber.i(
                "Background playback recovery restored attempt=%d local=%s",
                attempt + 1,
                plan.localPlayback
            )
            return true
        }
        Timber.w(error, "Background playback recovery attempt %d failed", attempt + 1)
        return false
    }

    private fun isPlaybackHealthy(player: Player?): Boolean = player != null &&
        player.mediaItemCount > 0 &&
        player.playWhenReady &&
        player.playbackState == Player.STATE_READY

    private fun scheduleStickyPlaybackRestore(startId: Int): Boolean {
        if (!isPlaybackRecoveryExpected()) {
            stopSelfResult(startId)
            return false
        }
        if (isStickyRestoreExpired()) {
            stopStickyRestore(startId)
            return false
        }
        acquirePlaybackWakeLock()
        stickyRestoreJob?.cancel()
        stickyRestoreJob = serviceScope.launch {
            restoreStickyPlayback(startId)
        }
        return true
    }

    private suspend fun restoreStickyPlayback(startId: Int) {
        delay(250L)
        val player = mediaSession?.player ?: run {
            stopStickyRestore(startId)
            return
        }
        if (player.mediaItemCount > 0 || player.playWhenReady) return
        val snapshot = restoreQueueSnapshot()
        val currentTrack = snapshot.currentTrack ?: run {
            stopStickyRestore(startId)
            return
        }
        if (!awaitStickyRestoreConnectivity(currentTrack)) {
            if (isStickyRestoreExpired()) stopStickyRestore(startId)
            return
        }
        val restored = restoreCurrentPlayback(
            snapshot.positionMs,
            preferFreshResolution = !isLocalPlaybackTrack(currentTrack)
        )
        if (!restored) {
            Timber.w("Sticky background playback restore failed")
            stopStickyRestore(startId)
        }
    }

    private suspend fun restoreQueueSnapshot(): PlaybackQueueSnapshot = withContext(Dispatchers.IO) {
        if (queueEngine.state.value.tracks.isEmpty()) {
            queueEngine.restore(
                fallbackTracks = emptyList(),
                fallbackIndex = -1,
                fallbackPositionMs = 0L,
                fallbackRadioEnabled = true
            )
        } else {
            queueEngine.state.value
        }
    }

    private suspend fun awaitStickyRestoreConnectivity(
        track: com.luc4n3x.levyra.domain.Track
    ): Boolean {
        if (isLocalPlaybackTrack(track)) return true
        while (isPlaybackRecoveryExpected() && !isStickyRestoreExpired()) {
            if (hasInternetCapableNetwork()) {
                acquirePlaybackWakeLock()
                return true
            }
            releasePlaybackWakeLock()
            Timber.d("Sticky playback restore waiting for network")
            delay(5_000L)
        }
        return false
    }

    private fun isStickyRestoreExpired(): Boolean {
        val heartbeatAt = playbackStateStore.getLong(KEY_PLAYBACK_HEARTBEAT_AT, 0L)
        return heartbeatAt <= 0L ||
            System.currentTimeMillis() - heartbeatAt > STICKY_RESTORE_MAX_AGE_MS
    }

    private fun stopStickyRestore(startId: Int) {
        markPlaybackExpected(false, force = true)
        releasePlaybackWakeLock()
        stopSelfResult(startId)
    }

    private suspend fun restoreCurrentPlayback(positionMs: Long, preferFreshResolution: Boolean): Boolean {
        val player = mediaSession?.player ?: return false
        if (!playbackStateStore.getBoolean(KEY_PLAYBACK_EXPECTED, false)) return false
        val currentItem = player.currentMediaItem
        val queueSnapshot = queueEngine.state.value
        val queueTrack = queueSnapshot.currentTrack
        val videoMode = currentItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) ?: false
        val mediaItem = when {
            queueTrack != null && isLocalPlaybackTrack(queueTrack) -> {
                when {
                    isLocalPlaybackUri(queueTrack.streamUrl) -> LevyraMediaItemFactory.build(queueTrack, false)
                    isLocalMediaItem(currentItem) -> currentItem
                    else -> null
                }
            }
            isLocalMediaItem(currentItem) -> currentItem
            preferFreshResolution && queueTrack != null && hasInternetCapableNetwork() -> {
                val resolved = withContext(Dispatchers.IO) {
                    runCatching { resolveQueueTrack(queueTrack) }
                        .onFailure { Timber.w(it, "Fresh background stream resolution failed") }
                        .getOrNull()
                }
                if (resolved != null) {
                    queueEngine.updateTrackAt(queueSnapshot.currentIndex, resolved)
                    LevyraMediaItemFactory.build(resolved, videoMode)
                } else {
                    currentItem
                }
            }
            else -> currentItem ?: queueTrack
                ?.takeIf { it.streamUrl.isNotBlank() }
                ?.let { LevyraMediaItemFactory.build(it, videoMode) }
        } ?: return false
        (player as? ExoPlayer)?.let { updatePlayerWakeMode(it, mediaItem) }
        acquirePlaybackWakeLock()
        player.setMediaItem(mediaItem, positionMs.coerceAtLeast(0L))
        player.prepare()
        player.play()
        updatePlaybackProtection(player)
        return true
    }

    private fun startPlaybackWatchdog(player: ExoPlayer) {
        playbackWatchdogJob?.cancel()
        watchdogAdvancedAtMs = SystemClock.elapsedRealtime()
        playbackWatchdogJob = serviceScope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                inspectPlaybackWatchdog(player)
            }
        }
    }

    private fun inspectPlaybackWatchdog(player: ExoPlayer) {
        val now = SystemClock.elapsedRealtime()
        if (!shouldPreservePlaybackExpectation(player)) markPlaybackExpected(isPlaybackExpected(player))
        if (resetWatchdogForExhaustedRecovery(now)) return
        if (resetWatchdogForInactivePlayer(player, now)) return
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        if (recordWatchdogProgress(positionMs, now)) return
        if (!isWatchdogStalled(now)) return
        scheduleWatchdogRecovery(positionMs)
        watchdogAdvancedAtMs = now
    }

    private fun isPlaybackExpected(player: ExoPlayer): Boolean = !serviceRecoveryExhausted &&
        player.mediaItemCount > 0 &&
        player.playWhenReady &&
        player.playbackState != Player.STATE_ENDED

    private fun resetWatchdogForExhaustedRecovery(now: Long): Boolean {
        if (!serviceRecoveryExhausted) return false
        resetWatchdogProgress(now)
        return true
    }

    private fun resetWatchdogForInactivePlayer(player: ExoPlayer, now: Long): Boolean {
        if (isPlayerActivelyPlaying(player)) return false
        resetWatchdogProgress(now)
        return true
    }

    private fun isPlayerActivelyPlaying(player: ExoPlayer): Boolean = player.mediaItemCount > 0 &&
        player.playWhenReady &&
        player.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE &&
        (player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY)

    private fun resetWatchdogProgress(now: Long) {
        watchdogPositionMs = C.TIME_UNSET
        watchdogAdvancedAtMs = now
    }

    private fun recordWatchdogProgress(positionMs: Long, now: Long): Boolean {
        if (!hasWatchdogPositionAdvanced(positionMs)) return false
        watchdogPositionMs = positionMs
        watchdogAdvancedAtMs = now
        return true
    }

    private fun hasWatchdogPositionAdvanced(positionMs: Long): Boolean =
        watchdogPositionMs == C.TIME_UNSET ||
            positionMs > watchdogPositionMs + 250L ||
            positionMs < watchdogPositionMs

    private fun isWatchdogStalled(now: Long): Boolean =
        now - watchdogAdvancedAtMs >= WATCHDOG_STALL_TIMEOUT_MS

    private fun scheduleWatchdogRecovery(positionMs: Long) {
        if (serviceRecoveryJob?.isActive == true) return
        Timber.w("Playback watchdog detected a stalled player at %d ms", positionMs)
        serviceRecoveryJob = serviceScope.launch {
            val restored = restoreCurrentPlayback(
                positionMs,
                preferFreshResolution = !isCurrentPlaybackLocal() && hasInternetCapableNetwork()
            )
            if (!restored) Timber.w("Playback watchdog recovery failed")
        }
    }

    private fun isCurrentPlaybackLocal(): Boolean {
        val player = mediaSession?.player
        return isLocalMediaItem(player?.currentMediaItem) ||
            queueEngine.state.value.currentTrack?.let(::isLocalPlaybackTrack) == true
    }

    private fun isLocalPlaybackTrack(track: com.luc4n3x.levyra.domain.Track): Boolean =
        track.source.equals("Offline", ignoreCase = true) || isLocalPlaybackUri(track.streamUrl)

    private fun isLocalPlaybackUri(value: String): Boolean {
        val clean = value.trim()
        return clean.startsWith("content://", ignoreCase = true) ||
            clean.startsWith("file://", ignoreCase = true)
    }

    private fun isLocalMediaItem(mediaItem: MediaItem?): Boolean {
        val scheme = mediaItem?.localConfiguration?.uri?.scheme.orEmpty()
        if (scheme.equals("content", ignoreCase = true) || scheme.equals("file", ignoreCase = true)) return true
        return mediaItem?.mediaMetadata?.extras
            ?.getString("levyra.source")
            ?.equals("Offline", ignoreCase = true) == true
    }

    private fun updatePlayerWakeMode(player: ExoPlayer, mediaItem: MediaItem?) {
        val wakeMode = if (isLocalMediaItem(mediaItem)) C.WAKE_MODE_LOCAL else C.WAKE_MODE_NETWORK
        if (wakeMode == appliedPlayerWakeMode) return
        player.setWakeMode(wakeMode)
        appliedPlayerWakeMode = wakeMode
    }

    private fun hasInternetCapableNetwork(): Boolean {
        val connectivity = getSystemService(ConnectivityManager::class.java) ?: return false
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
        val videoMimeType = mediaItem.mediaMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_MIME_TYPE)
            ?: mediaItem.requestMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_MIME_TYPE)

        val audioSource = mediaSourceFor(mediaItem)
        val videoItem = MediaItem.Builder()
            .setUri(videoUrl)
            .apply {
                if (!videoCacheKey.isNullOrBlank()) setCustomCacheKey(videoCacheKey)
                if (!videoMimeType.isNullOrBlank()) setMimeType(videoMimeType)
            }
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
        return when {
            isHlsManifestUri(uri) -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            isDashManifestUri(uri) -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            else -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
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

    private fun isDashManifestUri(uri: String): Boolean {
        val clean = uri.substringBefore('#').lowercase()
        val path = clean.substringBefore('?')
        return path.endsWith(".mpd") ||
            clean.contains("mime=application%2fdash+xml") ||
            clean.contains("mime=application/dash+xml")
    }
}
