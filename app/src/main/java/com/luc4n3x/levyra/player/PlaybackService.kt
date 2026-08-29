package com.luc4n3x.levyra.player

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.app.ActivityManager
import android.os.Debug
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.video.VideoRendererEventListener
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
import com.luc4n3x.levyra.data.FavoritesStore
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.classifyPlaybackFailureReason
import com.luc4n3x.levyra.data.isTerminalPlaybackFailure
import com.luc4n3x.levyra.data.playbackRecoveryPlanFor
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.cast.RemotePlaybackBackendProvider
import com.luc4n3x.levyra.feature.cast.CastHandoffConverter
import com.luc4n3x.levyra.feature.cast.LocalPlaybackSnapshot
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import com.luc4n3x.levyra.player.queue.PlaybackQueueSnapshot
import com.luc4n3x.levyra.player.queue.playbackQueueIdentity
import com.luc4n3x.levyra.runtime.RuntimeHooks
import com.luc4n3x.levyra.runtime.RuntimeSignal
import com.luc4n3x.levyra.widget.LevyraWidgetBridge
import com.luc4n3x.levyra.widget.LevyraWidgetCenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.IOException
import java.util.ArrayList

@UnstableApi
class PlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private lateinit var autoLibrary: AndroidAutoLibrary
    private lateinit var queueEngine: PersistentQueueEngine
    private lateinit var favoritesStore: FavoritesStore
    private lateinit var resolver: PlaybackResolver
    private lateinit var musicRepository: YoutubeMusicRepository
    private lateinit var sharedMediaSourceFactory: MediaSource.Factory
    private val adaptivePlaybackPolicy by lazy { AdaptivePlaybackPolicy(this) }
    private val playbackWarmup by lazy { PlaybackWarmup(this) }
    private var queueSkipJob: Job? = null
    private var servicePrefetchJob: Job? = null
    private var servicePrefetchTargetIdentity: String? = null
    private var servicePrefetchRequestToken = 0L
    @Volatile private var preparedQueueNext: PreparedQueueNext? = null
    private var serviceRecoveryJob: Job? = null
    private var stickyRestoreJob: Job? = null
    private var playbackWatchdogJob: Job? = null
    private var queueTransitionJob: Job? = null
    private var queueTransitionMonitorJob: Job? = null
    private var memoryGuardJob: Job? = null
    private var castHandoffJob: Job? = null
    private var memoryGuardHighSamples = 0
    private var lastMemoryRecycleElapsedMs = 0L
    private var transitionPlayer: ExoPlayer? = null
    private var currentAudioSettings = LevyraAudioSettings()
    private var currentAudioNormalization = false
    private val normalizationProcessor = NormalizationAudioProcessor()
    private val equalizerProcessor = LevyraEqualizerAudioProcessor()
    private val spatialAudioProcessor = StereoSpatialAudioProcessor()
    private val limiterProcessor = TruePeakLimiterAudioProcessor()
    private val visualizerProcessor = VisualizerAudioProcessor()
    private val pcm16OutputProcessor = Pcm16OutputAudioProcessor()
    private lateinit var playbackWakeLock: PowerManager.WakeLock
    private lateinit var playbackStateStore: SharedPreferences
    private var serviceRecoveryAttempts = 0
    private var serviceRecoveryExhausted = false
    private var watchdogPositionMs = C.TIME_UNSET
    private var watchdogAdvancedAtMs = 0L
    private var lastPlaybackExpected: Boolean? = null
    private var lastPlaybackHeartbeatAtMs = 0L
    private var appliedPlayerWakeMode = C.WAKE_MODE_NETWORK

    private data class PreparedQueueNext(
        val sourceIdentity: String,
        val targetIdentity: String,
        val targetTrackId: String,
        val resolved: Track,
        val preparedAtElapsedMs: Long
    )

    private val platformMediaAudioAttributes by lazy {
        android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refreshAudioOutputProfile()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refreshAudioOutputProfile()
    }

    companion object {
        private const val RUNNING_LOW_LEVEL = 10
        private const val RUNNING_CRITICAL_LEVEL = 15
        const val EXTRA_VIDEO_URL = "levyra.videoUrl"
        const val EXTRA_VIDEO_CACHE_KEY = "levyra.videoCacheKey"
        const val EXTRA_VIDEO_MIME_TYPE = "levyra.videoMimeType"
        const val EXTRA_VIDEO_MODE = "levyra.videoMode"
        const val EXTRA_YOUTUBE_LOUDNESS_DB = "levyra.youtubeLoudnessDb"
        const val EXTRA_YOUTUBE_PERCEPTUAL_LOUDNESS_DB = "levyra.youtubePerceptualLoudnessDb"
        const val ACTION_GET_PLATFORM_TOKEN = "levyra.media.GET_PLATFORM_TOKEN"
        const val ACTION_SET_VIDEO_SUBTITLE = "levyra.media.SET_VIDEO_SUBTITLE"
        const val KEY_PLATFORM_TOKEN = "levyra.media.PLATFORM_TOKEN"
        const val KEY_VIDEO_SUBTITLE_ID = "levyra.media.VIDEO_SUBTITLE_ID"
        private const val PLAYBACK_STATE_PREFS = "levyra.playback.service.state"
        private const val KEY_PLAYBACK_EXPECTED = "playbackExpected"
        private const val KEY_PLAYBACK_HEARTBEAT_AT = "playbackHeartbeatAt"
        private const val PLAYBACK_HEARTBEAT_INTERVAL_MS = 30_000L
        private const val STICKY_RESTORE_MAX_AGE_MS = 12L * 60L * 60L * 1_000L
        private const val WATCHDOG_INTERVAL_MS = 5_000L
        private const val WATCHDOG_STALL_TIMEOUT_MS = 15_000L
        private const val MAX_TRANSITION_LOOKAHEAD_MS = 20_000L
        private const val TRANSITION_PREPARE_TIMEOUT_MS = 8_000L
        private const val PREPARED_QUEUE_WAIT_MS = 2_000L
        private const val PREPARED_QUEUE_MAX_AGE_MS = 15L * 60L * 1_000L
        private const val PREPARED_QUEUE_PRIME_BYTES = 384L * 1024L
        private const val PRIMARY_HANDOFF_WARN_MS = 5_000L
        private const val PRIMARY_HANDOFF_SYNC_TIMEOUT_MS = 2_500L
        private const val PRIMARY_HANDOFF_FADE_MS = 240L
        private const val PRIMARY_HANDOFF_SYNC_TOLERANCE_MS = 250L
        private const val PRIMARY_HANDOFF_SYNC_LEAD_MS = 120L
        private const val TRANSITION_STEP_MS = 50L
        private const val SEEK_TOLERANCE_MS = 1_500L
        private val ONLINE_RECOVERY_DELAYS_MS = longArrayOf(500L, 2_000L, 5_000L, 10_000L)
        private val LOCAL_RECOVERY_DELAYS_MS = longArrayOf(250L, 750L, 1_500L, 3_000L, 5_000L, 10_000L)

        private val _activePlayerFlow = MutableStateFlow<ExoPlayer?>(null)
        val activePlayerFlow: StateFlow<ExoPlayer?> = _activePlayerFlow.asStateFlow()

        private val _sleepTimerStateFlow = MutableStateFlow<PlaybackSleepTimerState>(PlaybackSleepTimerState.Disabled)
        val sleepTimerStateFlow: StateFlow<PlaybackSleepTimerState> = _sleepTimerStateFlow.asStateFlow()

        @Volatile
        var activePlayer: ExoPlayer? = null
            private set(value) {
                field = value
                _activePlayerFlow.value = value
            }

        fun startSleepTimer(minutes: Int): Boolean {
            val service = activeService ?: return false
            if (minutes <= 0) {
                service.sleepTimer.cancel()
                return true
            }
            service.sleepTimer.startCountdown(minutes * 60_000L)
            return true
        }

        fun startSleepTimerEndOfTrack(): Boolean {
            val service = activeService ?: return false
            service.sleepTimer.startEndOfTrack()
            return true
        }

        fun cancelSleepTimer(): Boolean {
            val service = activeService ?: return false
            service.sleepTimer.cancel()
            return true
        }

        @Volatile
        private var activeService: PlaybackService? = null

        private val premiumAudioSettingsLock = Any()
        private var pendingAudioSettings: LevyraAudioSettings? = null
        private var pendingAudioNormalization = false

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

        fun prepareQueueNext(track: com.luc4n3x.levyra.domain.Track): Boolean =
            activeService?.prepareQueueNextInternal(track) == true

        fun clearPreparedQueueNext() {
            activeService?.clearPreparedQueueNextInternal()
        }

        fun clearPreparedQueueNextIfStale() {
            activeService?.clearPreparedQueueNextIfStaleInternal()
        }

        fun consumePreparedQueueNext(trackId: String) {
            activeService?.consumePreparedQueueNextInternal(trackId)
        }

        @Volatile
        var isQueueTransitionInProgress: Boolean = false
            private set

        fun applyPremiumAudioSettings(
            settings: LevyraAudioSettings,
            audioNormalization: Boolean
        ) {
            val normalized = settings.normalized()
            synchronized(premiumAudioSettingsLock) {
                pendingAudioSettings = normalized
                pendingAudioNormalization = audioNormalization
                activeService?.applyPremiumAudioSettingsInternal(normalized, audioNormalization)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val sleepTimer by lazy { PlaybackSleepTimer(serviceScope) { pausePlaybackForSleepTimer() } }
    private var sleepTimerStateJob: Job? = null
    private val queueShuffleCommand by lazy { SessionCommand("levyra.queue.shuffle", Bundle.EMPTY) }
    private val queueLikeCommand by lazy { SessionCommand("levyra.favorite.like", Bundle.EMPTY) }
    private val platformTokenCommand by lazy { SessionCommand(ACTION_GET_PLATFORM_TOKEN, Bundle.EMPTY) }
    private val videoSubtitleCommand by lazy { SessionCommand(ACTION_SET_VIDEO_SUBTITLE, Bundle.EMPTY) }

    private fun applyPremiumAudioSettingsInternal(
        settings: LevyraAudioSettings,
        audioNormalization: Boolean
    ) {
        val normalized = settings.normalized()
        normalizationProcessor.enabled = audioNormalization || normalized.replayGainEnabled
        equalizerProcessor.enabled = normalized.equalizerEnabled
        equalizerProcessor.setBandLevels(normalized.bandLevels)
        equalizerProcessor.bassBoost = normalized.bassBoost
        equalizerProcessor.preampDb = normalized.preampDb
        spatialAudioProcessor.strength = if (normalized.equalizerEnabled) normalized.virtualizer else 0
        limiterProcessor.enabled = normalized.limiterEnabled &&
            (normalized.equalizerEnabled || normalized.virtualizer > 0 ||
                normalized.replayGainEnabled || audioNormalization)
        updateQueueTransitionSettings(normalized, audioNormalization)
    }

    private fun activateServiceAndApplyPendingAudioSettings() {
        synchronized(premiumAudioSettingsLock) {
            activeService = this
            pendingAudioSettings?.let { settings ->
                applyPremiumAudioSettingsInternal(settings, pendingAudioNormalization)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        playbackStateStore = getSharedPreferences(PLAYBACK_STATE_PREFS, Context.MODE_PRIVATE)
        playbackWakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:PlaybackService")
            .apply { setReferenceCounted(false) }
        queueEngine = PersistentQueueEngine.get(this)
        favoritesStore = FavoritesStore(this)
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

        val mergingFactory = LevyraMediaSourceFactory(
            defaultFactory,
            cacheDataSourceFactory,
            localDataSourceFactory
        ).apply {
            setLoadErrorHandlingPolicy(LevyraPlaybackLoadErrorHandlingPolicy)
        }
        sharedMediaSourceFactory = mergingFactory

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(false)
                    .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(
                        arrayOf(
                            normalizationProcessor,
                            equalizerProcessor,
                            spatialAudioProcessor,
                            limiterProcessor,
                            visualizerProcessor,
                            pcm16OutputProcessor
                        )
                    )
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
        RuntimeHooks.attachPlayer(player)
        RuntimeHooks.player(RuntimeSignal.PLAYER_CREATED)
        RuntimeHooks.hot(RuntimeSignal.HOT_PLAYER_CREATE)
        val prefs = LevyraPreferences(this)
        val snapshot = prefs.snapshot()
        currentAudioSettings = snapshot.audioSettings.normalized()
        currentAudioNormalization = snapshot.audioNormalization
        player.skipSilenceEnabled = snapshot.skipSilence
        applyPremiumAudioSettingsInternal(snapshot.audioSettings, snapshot.audioNormalization)
        RuntimeHooks.dsp(RuntimeSignal.DSP_CREATED)
        (getSystemService(Context.AUDIO_SERVICE) as AudioManager).registerAudioDeviceCallback(audioDeviceCallback, null)
        refreshAudioOutputProfile()

        activePlayer = player
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                RuntimeHooks.player(
                    action = RuntimeSignal.PLAYER_TRANSITION,
                    mode = if (mediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true) {
                        RuntimeSignal.MODE_VIDEO
                    } else {
                        RuntimeSignal.MODE_AUDIO
                    }
                )
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT && sleepTimer.consumeEndOfTrackBoundary()) {
                    pausePlaybackForSleepTimer()
                    return
                }
                updatePlayerWakeMode(player, mediaItem)
                applyPlaybackTrackSelection(player, mediaItem)
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
                if (!isLocalMediaItem(mediaItem)) prefetchServiceQueueNext()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                RuntimeHooks.player(
                    action = RuntimeSignal.PLAYER_STATE,
                    value = playbackState,
                    mode = if (player.currentMediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true) {
                        RuntimeSignal.MODE_VIDEO
                    } else {
                        RuntimeSignal.MODE_AUDIO
                    }
                )
                if (playbackState != Player.STATE_ENDED) return
                if (sleepTimer.consumeEndOfTrackBoundary()) {
                    pausePlaybackForSleepTimer()
                    return
                }
                if (LevyraWidgetBridge.onNext == null && !isQueueTransitionInProgress) {
                    skipQueue(forward = true, respectRepeatOne = true, autoAdvance = true)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                updatePlaybackProtection(player)
                discardIncompatiblePlaybackCache(error)
                val failureKind = classifyPlaybackFailureReason(playbackFailureReasonOf(error))
                RuntimeHooks.player(
                    action = RuntimeSignal.PLAYER_ERROR,
                    mode = if (player.currentMediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true) {
                        RuntimeSignal.MODE_VIDEO
                    } else {
                        RuntimeSignal.MODE_AUDIO
                    },
                    failure = failureKind.ordinal
                )
                if (isTerminalPlaybackFailure(failureKind)) {
                    serviceRecoveryExhausted = true
                    markPlaybackExpected(false, force = true)
                    releasePlaybackWakeLock()
                } else {
                    scheduleServiceRecovery(error)
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady && queueTransitionJob?.isActive == true) {
                    cancelQueueTransition()
                }
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
        sleepTimerStateJob?.cancel()
        sleepTimerStateJob = serviceScope.launch {
            sleepTimer.state.collect { state -> _sleepTimerStateFlow.value = state }
        }
        serviceScope.launch {
            while (isActive) {
                if (player.mediaItemCount > 0) queueEngine.updatePosition(player.currentPosition)
                delay(2_000L)
            }
        }

        val queueShuffleButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName(getString(com.luc4n3x.levyra.R.string.notification_shuffle))
            .setSessionCommand(queueShuffleCommand)
            .setCustomIconResId(com.luc4n3x.levyra.R.drawable.ic_notification_shuffle)
            .build()
        val queueLikeButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName(getString(com.luc4n3x.levyra.R.string.notification_favorite))
            .setSessionCommand(queueLikeCommand)
            .setCustomIconResId(com.luc4n3x.levyra.R.drawable.ic_notification_like)
            .build()

        val callback = object : MediaLibrarySession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val commandBuilder = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                    .buildUpon()
                    .add(queueShuffleCommand)
                    .add(queueLikeCommand)
                if (controller.packageName == packageName) {
                    commandBuilder.add(platformTokenCommand)
                    commandBuilder.add(videoSubtitleCommand)
                }
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                    .setAvailableSessionCommands(commandBuilder.build())
                    .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
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
                    ACTION_GET_PLATFORM_TOKEN -> {
                        if (controller.packageName != packageName) {
                            Futures.immediateFuture(
                                SessionResult(androidx.media3.session.SessionError.ERROR_PERMISSION_DENIED)
                            )
                        } else {
                            val extras = Bundle().apply {
                                putParcelable(KEY_PLATFORM_TOKEN, session.platformToken)
                            }
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, extras))
                        }
                    }
                    ACTION_SET_VIDEO_SUBTITLE -> {
                        if (controller.packageName != packageName) {
                            Futures.immediateFuture(
                                SessionResult(androidx.media3.session.SessionError.ERROR_PERMISSION_DENIED)
                            )
                        } else {
                            applyVideoSubtitleSelection(
                                player,
                                args.getString(KEY_VIDEO_SUBTITLE_ID)?.trim().orEmpty()
                            )
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                    }
                    "levyra.queue.shuffle" -> {
                        queueEngine.setShuffle(!queueEngine.state.value.shuffleEnabled)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    "levyra.favorite.like" -> {
                        serviceScope.launch(Dispatchers.IO) {
                            queueEngine.state.value.currentTrack?.let { track ->
                                favoritesStore.toggleFavorite(track)
                            }
                        }
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
                    autoLibrary.children(parentId, page, pageSize)
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

        val sessionPlayer = RemotePlaybackBackendProvider.create(this).attachLocalPlayer(player)
        sessionPlayer.addListener(object : Player.Listener {
            override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
                if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
                    cancelQueueTransition()
                    cancelServicePrefetch()
                    clearPreparedQueueNextInternal()
                    handoffQueueToCast(sessionPlayer)
                } else {
                    castHandoffJob?.cancel()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (sessionPlayer.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE) return
                val mediaId = mediaItem?.mediaId ?: return
                val snapshot = queueEngine.state.value
                val index = snapshot.tracks.indexOfFirst { LevyraMediaItemFactory.metadataOnly(it).mediaId == mediaId }
                if (index >= 0 && index != snapshot.currentIndex) {
                    queueEngine.select(index, sessionPlayer.currentPosition, rememberCurrent = true)
                }
            }
        })
        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(sessionPlayer) {
            override fun getDuration(): Long {
                val realDuration = super.getDuration()
                if (realDuration > 0L) return realDuration
                val metadataDuration = currentMediaItem?.mediaMetadata?.extras
                    ?.getLong("levyra.durationMs", androidx.media3.common.C.TIME_UNSET)
                    ?: androidx.media3.common.C.TIME_UNSET
                return metadataDuration.takeIf { it > 0L } ?: androidx.media3.common.C.TIME_UNSET
            }

            override fun isCurrentMediaItemSeekable(): Boolean {
                return getDuration() > 0L && !isCurrentMediaItemLive
            }

            override fun isCurrentMediaItemLive(): Boolean {
                return super.isCurrentMediaItemLive()
            }

            override fun getAvailableCommands(): androidx.media3.common.Player.Commands {
                val commands = super.getAvailableCommands().buildUpon()
                if (isCurrentMediaItemSeekable) {
                    commands.add(androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                } else {
                    commands.remove(androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                }
                commands.remove(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS)
                commands.remove(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                commands.remove(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT)
                commands.remove(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                if (canSkipToPreviousTrack()) {
                    commands.addAll(
                        androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS,
                        androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
                    )
                }
                if (canSkipToNextTrack()) {
                    commands.addAll(
                        androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT,
                        androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
                    )
                }
                return commands.build()
            }

            override fun isCommandAvailable(command: Int): Boolean = availableCommands.contains(command)

            override fun hasNextMediaItem(): Boolean = canSkipToNextTrack()

            override fun hasPreviousMediaItem(): Boolean = canSkipToPreviousTrack()

            override fun seekToNext() = seekRemoteOrLocal(forward = true)

            override fun seekToNextMediaItem() = seekRemoteOrLocal(forward = true)

            override fun seekToPrevious() = seekRemoteOrLocal(forward = false)

            override fun seekToPreviousMediaItem() = seekRemoteOrLocal(forward = false, allowRewind = false)

            private fun seekRemoteOrLocal(forward: Boolean, allowRewind: Boolean = true) {
                if (sessionPlayer.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE) {
                    skipQueue(forward, respectRepeatOne = false, allowRewind = allowRewind)
                    return
                }
                if (!forward && allowRewind && sessionPlayer.currentPosition > 5_000L) {
                    sessionPlayer.seekTo(0L)
                    queueEngine.updatePosition(0L)
                    return
                }
                val canMoveInsideWindow = if (forward) {
                    sessionPlayer.currentMediaItemIndex < sessionPlayer.mediaItemCount - 1
                } else {
                    sessionPlayer.currentMediaItemIndex > 0
                }
                if (canMoveInsideWindow) {
                    if (forward) super.seekToNextMediaItem() else super.seekToPreviousMediaItem()
                } else {
                    skipCastQueue(forward, sessionPlayer)
                }
            }
        }

        mediaSession = MediaLibrarySession.Builder(this, forwardingPlayer, callback)
            .setSessionActivity(sessionActivity)
            .setMediaButtonPreferences(ImmutableList.of(queueShuffleButton, queueLikeButton))
            .build()

        val notificationProvider = DefaultMediaNotificationProvider(this)
        setMediaNotificationProvider(notificationProvider)
        activateServiceAndApplyPendingAudioSettings()
        startQueueTransitionMonitor(player)
        startMemoryGuard(player)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null && !scheduleStickyPlaybackRestore(startId)) return START_NOT_STICKY
        return START_STICKY
    }

    private fun canSkipToPreviousTrack(): Boolean = queueEngine.state.value.currentTrack != null

    private fun canSkipToNextTrack(): Boolean {
        val snapshot = queueEngine.state.value
        if (snapshot.currentTrack == null) return false
        return snapshot.currentIndex < snapshot.tracks.lastIndex ||
            snapshot.shuffleEnabled ||
            snapshot.repeatMode != com.luc4n3x.levyra.domain.RepeatMode.Off ||
            snapshot.radioEnabled
    }

    private fun skipQueue(
        forward: Boolean,
        respectRepeatOne: Boolean,
        autoAdvance: Boolean = false,
        allowRewind: Boolean = true
    ) {
        cancelQueueTransition()
        queueSkipJob?.cancel()
        cancelServicePrefetch()
        queueSkipJob = serviceScope.launch {
            val player = activePlayer ?: return@launch
            if (allowRewind && rewindInsteadOfSkip(player, forward)) return@launch
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
        val initial = queueEngine.state.value
        if (!initial.radioEnabled) return null
        val seed = initial.currentTrack ?: return null
        if (isLocalPlaybackTrack(seed) || !hasInternetCapableNetwork()) return null
        val additions = try {
            musicRepository.radio(seed, LevyraPreferences(this).languageCode(), 5)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Background radio expansion failed")
            emptyList()
        }
        if (additions.isEmpty()) return null
        val current = queueEngine.state.value
        if (!current.radioEnabled || current.generation != initial.generation ||
            current.currentTrack?.let(::playbackQueueIdentity) != playbackQueueIdentity(seed)
        ) return null
        queueEngine.appendRadioTracks(additions)
        return queueEngine.next(respectRepeatOne = false)
    }

    private suspend fun resolveSkipTarget(
        target: com.luc4n3x.levyra.domain.Track,
        autoAdvance: Boolean
    ): com.luc4n3x.levyra.domain.Track? {
        val targetIdentity = playbackQueueIdentity(target)
        preparedQueueTrackForTarget(targetIdentity)?.let { resolved ->
            consumePreparedQueueNextInternal(target.id.ifBlank { resolved.id })
            return resolved
        }
        return if (autoAdvance) {
            resolveQueueTrackPersistently(target)
        } else {
            runCatching { resolveQueueTrack(target) }
                .onFailure { Timber.w(it, "Background queue resolution failed") }
                .getOrNull()
        }
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
        RuntimeHooks.player(RuntimeSignal.PLAYER_PREPARE)
        RuntimeHooks.hot(RuntimeSignal.HOT_PLAYER_PREPARE)
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
        val target = queueEngine.upcoming(1).firstOrNull() ?: return
        prepareQueueNextInternal(target)
    }

    private fun prepareQueueNextInternal(target: Track): Boolean {
        val snapshot = queueEngine.state.value
        val current = snapshot.currentTrack ?: return false
        val expectedNext = queueEngine.upcoming(1).firstOrNull() ?: return false
        val sourceIdentity = playbackQueueIdentity(current)
        val targetIdentity = playbackQueueIdentity(target)
        if (targetIdentity != playbackQueueIdentity(expectedNext)) return false

        val now = SystemClock.elapsedRealtime()
        preparedQueueNext?.let { prepared ->
            if (queuePrecacheMatchesTransition(
                    preparedSourceIdentity = prepared.sourceIdentity,
                    preparedTargetIdentity = prepared.targetIdentity,
                    currentSourceIdentity = sourceIdentity,
                    currentTargetIdentity = targetIdentity,
                    preparedAtElapsedMs = prepared.preparedAtElapsedMs,
                    nowElapsedMs = now,
                    maxAgeMs = PREPARED_QUEUE_MAX_AGE_MS
                ) && prepared.resolved.streamUrl.isNotBlank()
            ) {
                return true
            }
            if (queuePrecacheMatchesTarget(
                    preparedTargetIdentity = prepared.targetIdentity,
                    requestedTargetIdentity = targetIdentity,
                    preparedAtElapsedMs = prepared.preparedAtElapsedMs,
                    nowElapsedMs = now,
                    maxAgeMs = PREPARED_QUEUE_MAX_AGE_MS
                ) && prepared.resolved.streamUrl.isNotBlank()
            ) {
                preparedQueueNext = prepared.copy(sourceIdentity = sourceIdentity)
                return true
            }
        }

        if (servicePrefetchJob?.isActive == true && servicePrefetchTargetIdentity == targetIdentity) return true
        if (!isLocalPlaybackTrack(target) && !hasInternetCapableNetwork()) return false

        cancelServicePrefetch()
        val requestToken = ++servicePrefetchRequestToken
        servicePrefetchTargetIdentity = targetIdentity
        servicePrefetchJob = serviceScope.launch {
            try {
                val resolved = withContext(Dispatchers.IO) {
                    runCatching { resolveQueueTrack(target) }
                        .onFailure { Timber.d(it, "Service queue prefetch skipped") }
                        .getOrNull()
                } ?: return@launch
                if (!queuePairStillCurrent(sourceIdentity, targetIdentity)) return@launch

                preparedQueueNext = PreparedQueueNext(
                    sourceIdentity = sourceIdentity,
                    targetIdentity = targetIdentity,
                    targetTrackId = target.id,
                    resolved = resolved,
                    preparedAtElapsedMs = SystemClock.elapsedRealtime()
                )
                if (!isLocalPlaybackTrack(resolved)) {
                    withContext(Dispatchers.IO) {
                        runCatching { playbackWarmup.prime(resolved, PREPARED_QUEUE_PRIME_BYTES) }
                            .onFailure { Timber.d(it, "Prepared queue warmup skipped") }
                    }
                }
            } finally {
                if (servicePrefetchRequestToken == requestToken) {
                    servicePrefetchTargetIdentity = null
                    servicePrefetchJob = null
                }
            }
        }
        return true
    }

    private fun queuePairStillCurrent(sourceIdentity: String, targetIdentity: String): Boolean {
        val current = queueEngine.state.value.currentTrack?.let(::playbackQueueIdentity) ?: return false
        val next = queueEngine.upcoming(1).firstOrNull()?.let(::playbackQueueIdentity) ?: return false
        return current == sourceIdentity && next == targetIdentity
    }

    private fun preparedQueueTrackForTarget(targetIdentity: String): Track? {
        val prepared = preparedQueueNext ?: return null
        val now = SystemClock.elapsedRealtime()
        if (!queuePrecacheMatchesTarget(
                preparedTargetIdentity = prepared.targetIdentity,
                requestedTargetIdentity = targetIdentity,
                preparedAtElapsedMs = prepared.preparedAtElapsedMs,
                nowElapsedMs = now,
                maxAgeMs = PREPARED_QUEUE_MAX_AGE_MS
            ) || prepared.resolved.streamUrl.isBlank()
        ) {
            if (!isQueuePrecacheFresh(prepared.preparedAtElapsedMs, now, PREPARED_QUEUE_MAX_AGE_MS)) {
                preparedQueueNext = null
            }
            return null
        }
        return prepared.resolved
    }

    private fun preparedQueueTrackForTransition(sourceIdentity: String, targetIdentity: String): Track? {
        val prepared = preparedQueueNext ?: return null
        val now = SystemClock.elapsedRealtime()
        if (!queuePrecacheMatchesTransition(
                preparedSourceIdentity = prepared.sourceIdentity,
                preparedTargetIdentity = prepared.targetIdentity,
                currentSourceIdentity = sourceIdentity,
                currentTargetIdentity = targetIdentity,
                preparedAtElapsedMs = prepared.preparedAtElapsedMs,
                nowElapsedMs = now,
                maxAgeMs = PREPARED_QUEUE_MAX_AGE_MS
            ) || prepared.resolved.streamUrl.isBlank()
        ) {
            if (!isQueuePrecacheFresh(prepared.preparedAtElapsedMs, now, PREPARED_QUEUE_MAX_AGE_MS)) {
                preparedQueueNext = null
            }
            return null
        }
        return prepared.resolved
    }

    private suspend fun awaitPreparedQueueTrackForTransition(
        sourceIdentity: String,
        targetIdentity: String
    ): Track? {
        preparedQueueTrackForTransition(sourceIdentity, targetIdentity)?.let { return it }
        val inFlight = servicePrefetchJob
        if (servicePrefetchTargetIdentity != targetIdentity || inFlight?.isActive != true) return null
        return withTimeoutOrNull(PREPARED_QUEUE_WAIT_MS) {
            while (inFlight.isActive) {
                preparedQueueTrackForTransition(sourceIdentity, targetIdentity)?.let { return@withTimeoutOrNull it }
                delay(25L)
            }
            preparedQueueTrackForTransition(sourceIdentity, targetIdentity)
        }
    }

    private fun clearPreparedQueueNextInternal() {
        preparedQueueNext = null
        cancelServicePrefetch()
    }

    private fun clearPreparedQueueNextIfStaleInternal() {
        val currentIdentity = queueEngine.state.value.currentTrack?.let(::playbackQueueIdentity)
        val nextIdentity = queueEngine.upcoming(1).firstOrNull()?.let(::playbackQueueIdentity)
        if (currentIdentity == null || nextIdentity == null) {
            clearPreparedQueueNextInternal()
            return
        }
        val now = SystemClock.elapsedRealtime()
        preparedQueueNext = preparedQueueNext
            ?.takeIf { it.resolved.streamUrl.isNotBlank() }
            ?.takeIf { prepared ->
                queuePrecacheMatchesTarget(
                    preparedTargetIdentity = prepared.targetIdentity,
                    requestedTargetIdentity = nextIdentity,
                    preparedAtElapsedMs = prepared.preparedAtElapsedMs,
                    nowElapsedMs = now,
                    maxAgeMs = PREPARED_QUEUE_MAX_AGE_MS
                )
            }
            ?.copy(sourceIdentity = currentIdentity)
        if (servicePrefetchTargetIdentity != nextIdentity) cancelServicePrefetch()
    }

    private fun consumePreparedQueueNextInternal(trackId: String) {
        val prepared = preparedQueueNext ?: return
        if (!queuePrecacheMatchesTrackId(prepared.targetTrackId, prepared.resolved.id, trackId)) return
        preparedQueueNext = null
        if (servicePrefetchTargetIdentity == prepared.targetIdentity) cancelServicePrefetch()
    }

    private fun cancelServicePrefetch() {
        servicePrefetchRequestToken++
        servicePrefetchJob?.cancel()
        servicePrefetchJob = null
        servicePrefetchTargetIdentity = null
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
            val match = musicRepository.searchSongMatch(track.title, track.artist, LevyraPreferences(this).languageCode())
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

    private fun updateQueueTransitionSettings(settings: LevyraAudioSettings, audioNormalization: Boolean) {
        currentAudioSettings = settings.normalized()
        currentAudioNormalization = audioNormalization
        transitionPlayer?.setPlaybackParameters(
            PlaybackParameters(currentAudioSettings.playbackSpeed, currentAudioSettings.pitch)
        )
        if (currentAudioSettings.crossfadeSeconds <= 0 || !currentAudioSettings.gaplessEnabled) {
            cancelQueueTransition()
        }
    }

    private fun startQueueTransitionMonitor(player: ExoPlayer) {
        queueTransitionMonitorJob?.cancel()
        queueTransitionMonitorJob = serviceScope.launch {
            while (isActive) {
                maybePrepareQueueTransition(player)
                delay(250L)
            }
        }
    }

    private fun maybePrepareQueueTransition(player: ExoPlayer) {
        if (sleepTimer.isEndOfTrackActive()) return
        if (queueTransitionJob?.isActive == true || !player.isPlaying || player.playbackState != Player.STATE_READY) return
        val duration = player.duration.takeIf { it > 0L && it != C.TIME_UNSET } ?: return
        val remaining = duration - player.currentPosition
        if (remaining !in 1L..MAX_TRANSITION_LOOKAHEAD_MS) return
        val snapshot = queueEngine.state.value
        val current = snapshot.currentTrack ?: return
        val next = queueEngine.upcoming(1).firstOrNull()
        val videoMode = player.currentMediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true
        val plan = planAutoMix(
            current = current.copy(durationMs = duration),
            next = next,
            settings = currentAudioSettings,
            repeatMode = snapshot.repeatMode,
            videoMode = videoMode,
            lowRam = adaptivePlaybackPolicy.current(videoMode = false).lowRam
        ) ?: return
        if (remaining > plan.preloadLeadMs) return
        queueTransitionJob = serviceScope.launch {
            runQueueTransition(player, snapshot, current, next ?: return@launch, plan)
        }
    }

    private suspend fun runQueueTransition(
        primary: ExoPlayer,
        snapshot: PlaybackQueueSnapshot,
        current: Track,
        target: Track,
        plan: AutoMixPlan
    ) {
        val currentIdentity = playbackQueueIdentity(current)
        val targetIdentity = playbackQueueIdentity(target)
        var secondary: ExoPlayer? = null
        try {
            val resolved = awaitPreparedQueueTrackForTransition(currentIdentity, targetIdentity)
                ?: withContext(Dispatchers.IO) { resolveQueueTrack(target) }
            if (!transitionStillValid(snapshot.generation, currentIdentity, targetIdentity, primary)) return
            secondary = buildTransitionPlayer(resolved).also { transitionPlayer = it }
            secondary.setPlaybackParameters(
                PlaybackParameters(currentAudioSettings.playbackSpeed, currentAudioSettings.pitch)
            )
            secondary.volume = 0f
            secondary.setMediaItem(LevyraMediaItemFactory.build(resolved))
            RuntimeHooks.player(RuntimeSignal.PLAYER_PREPARE)
            RuntimeHooks.hot(RuntimeSignal.HOT_PLAYER_PREPARE)
            secondary.prepare()
            val prepared = withTimeoutOrNull(TRANSITION_PREPARE_TIMEOUT_MS) {
                while (secondary.playbackState != Player.STATE_READY) {
                    if (!transitionStillValid(snapshot.generation, currentIdentity, targetIdentity, primary)) return@withTimeoutOrNull false
                    if (secondary.playerError != null) return@withTimeoutOrNull false
                    delay(50L)
                }
                true
            } == true
            if (!prepared) return

            while (primary.duration > 0L && primary.duration - primary.currentPosition > plan.transitionMs) {
                if (!transitionStillValid(snapshot.generation, currentIdentity, targetIdentity, primary)) return
                delay(50L)
            }
            if (!transitionStillValid(snapshot.generation, currentIdentity, targetIdentity, primary)) return

            isQueueTransitionInProgress = true
            secondary.play()
            fadePlayers(primary, secondary, plan.transitionMs) {
                transitionStillValid(snapshot.generation, currentIdentity, targetIdentity, primary) &&
                    primary.duration - primary.currentPosition <= plan.transitionMs + SEEK_TOLERANCE_MS
            }

            val handedOff = queueEngine.handoffNext(
                expectedGeneration = snapshot.generation,
                expectedCurrentIdentity = currentIdentity,
                expectedNextIdentity = targetIdentity,
                resolved = resolved
            )
            if (handedOff == null) {
                Timber.w("Crossfade queue changed before compare-and-set handoff")
                return
            }
            consumePreparedQueueNextInternal(target.id.ifBlank { resolved.id })

            primary.volume = 0f
            primary.setMediaItem(
                LevyraMediaItemFactory.build(resolved),
                secondary.currentPosition.coerceAtLeast(0L)
            )
            RuntimeHooks.player(RuntimeSignal.PLAYER_PREPARE)
            RuntimeHooks.hot(RuntimeSignal.HOT_PLAYER_PREPARE)
            primary.prepare()

            if (!awaitPrimaryHandoffReady(primary, secondary, targetIdentity, resolved.title)) return
            if (!synchronizePrimaryHandoff(primary, secondary, targetIdentity)) return

            fadePlayers(secondary, primary, PRIMARY_HANDOFF_FADE_MS) {
                handoffStillValid(primary, secondary, targetIdentity)
            }
            queueEngine.updatePosition(primary.currentPosition)
            if (!isLocalPlaybackTrack(resolved)) prefetchServiceQueueNext()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Queue crossfade failed")
        } finally {
            isQueueTransitionInProgress = false
            primary.volume = 1f
            releaseTransitionPlayer(secondary)
        }
    }

    private suspend fun awaitPrimaryHandoffReady(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        targetIdentity: String,
        title: String
    ): Boolean {
        val startedAt = SystemClock.elapsedRealtime()
        var warned = false
        while (true) {
            if (!handoffStillValid(primary, secondary, targetIdentity)) return false
            if (primary.playbackState == Player.STATE_READY && primary.playerError == null) return true
            if (secondary.playbackState == Player.STATE_ENDED || secondary.playerError != null) return false
            if (!warned && SystemClock.elapsedRealtime() - startedAt >= PRIMARY_HANDOFF_WARN_MS) {
                warned = true
                Timber.w("Crossfade handoff still preparing primary player for %s", title)
            }
            delay(40L)
        }
    }

    private suspend fun synchronizePrimaryHandoff(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        targetIdentity: String
    ): Boolean {
        repeat(3) {
            if (!handoffStillValid(primary, secondary, targetIdentity)) return false
            if (!crossfadeHandoffNeedsResync(
                    primary.currentPosition,
                    secondary.currentPosition,
                    PRIMARY_HANDOFF_SYNC_TOLERANCE_MS
                )
            ) return true

            primary.seekTo(
                crossfadeHandoffSeekPosition(
                    secondaryPositionMs = secondary.currentPosition,
                    durationMs = primary.duration,
                    leadMs = PRIMARY_HANDOFF_SYNC_LEAD_MS
                )
            )
            val synced = withTimeoutOrNull(PRIMARY_HANDOFF_SYNC_TIMEOUT_MS) {
                while (true) {
                    if (!handoffStillValid(primary, secondary, targetIdentity)) return@withTimeoutOrNull false
                    if (primary.playerError != null) return@withTimeoutOrNull false
                    if (primary.playbackState == Player.STATE_READY &&
                        !crossfadeHandoffNeedsResync(
                            primary.currentPosition,
                            secondary.currentPosition,
                            PRIMARY_HANDOFF_SYNC_TOLERANCE_MS
                        )
                    ) return@withTimeoutOrNull true
                    delay(30L)
                }
            } == true
            if (synced) return true
        }
        return primary.playbackState == Player.STATE_READY &&
            handoffStillValid(primary, secondary, targetIdentity) &&
            !crossfadeHandoffNeedsResync(
                primary.currentPosition,
                secondary.currentPosition,
                PRIMARY_HANDOFF_SYNC_TOLERANCE_MS
            )
    }

    private fun handoffStillValid(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        targetIdentity: String
    ): Boolean {
        val current = queueEngine.state.value
        return playbackQueueIdentity(current.currentTrack ?: return false) == targetIdentity &&
            current.repeatMode != com.luc4n3x.levyra.domain.RepeatMode.One &&
            primary.playWhenReady &&
            secondary.playWhenReady &&
            primary.currentMediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) != true
    }

    private fun buildTransitionPlayer(track: Track): ExoPlayer {
        val normalization = NormalizationAudioProcessor().apply {
            enabled = currentAudioNormalization || currentAudioSettings.replayGainEnabled
            setYoutubeLoudness(track.youtubeLoudnessDb, track.youtubePerceptualLoudnessDb)
        }
        val equalizer = LevyraEqualizerAudioProcessor().apply {
            enabled = currentAudioSettings.equalizerEnabled
            setBandLevels(currentAudioSettings.bandLevels)
            bassBoost = currentAudioSettings.bassBoost
            preampDb = currentAudioSettings.preampDb
            outputProfile = equalizerProcessor.outputProfile
        }
        val spatial = StereoSpatialAudioProcessor().apply {
            strength = if (currentAudioSettings.equalizerEnabled) currentAudioSettings.virtualizer else 0
        }
        val limiter = TruePeakLimiterAudioProcessor().apply {
            enabled = currentAudioSettings.limiterEnabled &&
                (currentAudioSettings.equalizerEnabled || currentAudioSettings.virtualizer > 0 ||
                    currentAudioSettings.replayGainEnabled || currentAudioNormalization)
        }
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildVideoRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: Handler,
                eventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: ArrayList<Renderer>
            ) = Unit

            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(false)
                .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                .setAudioProcessors(
                    arrayOf(
                        normalization,
                        equalizer,
                        spatial,
                        limiter,
                        Pcm16OutputAudioProcessor()
                    )
                )
                .build()
        }.apply { setEnableDecoderFallback(true) }
        return ExoPlayer.Builder(this)
            .setRenderersFactory(renderers)
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
            .also(RuntimeHooks::attachPlayer)
    }

    private suspend fun fadePlayers(
        outgoing: ExoPlayer,
        incoming: ExoPlayer,
        durationMs: Long,
        isValid: () -> Boolean = { true }
    ) {
        val steps = (durationMs / TRANSITION_STEP_MS).toInt().coerceIn(8, 120)
        val mediaStepMs = (durationMs / steps).coerceAtLeast(10L)
        repeat(steps + 1) { step ->
            if ((!outgoing.playWhenReady || !incoming.playWhenReady || !isValid()) && step < steps) {
                throw CancellationException("Playback paused during crossfade")
            }
            val gains = equalPowerCrossfade(step.toFloat() / steps.toFloat())
            outgoing.volume = gains.outgoing
            incoming.volume = gains.incoming
            if (step < steps) {
                val stepDelay = crossfadeStepWallClockMs(mediaStepMs, outgoing.playbackParameters.speed)
                delay(stepDelay)
            }
        }
    }

    private fun transitionStillValid(
        generation: Long,
        currentIdentity: String,
        targetIdentity: String,
        player: ExoPlayer
    ): Boolean {
        val current = queueEngine.state.value
        return current.generation == generation &&
            playbackQueueIdentity(current.currentTrack ?: return false) == currentIdentity &&
            current.repeatMode != com.luc4n3x.levyra.domain.RepeatMode.One &&
            !sleepTimer.isEndOfTrackActive() &&
            queueEngine.upcoming(1).firstOrNull()?.let(::playbackQueueIdentity) == targetIdentity &&
            player.playWhenReady &&
            player.currentMediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) != true
    }

    private fun applyPlaybackTrackSelection(player: ExoPlayer, mediaItem: MediaItem?) {
        val videoMode = mediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true
        val disableVideo = PlaybackTrackSelectionPolicy.disableVideoTracks(videoMode)
        runCatching {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, disableVideo)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .setPreferredTextLanguage(null)
                .build()
        }.onFailure { Timber.w(it, "Track selection update failed") }
    }

    private fun applyVideoSubtitleSelection(player: ExoPlayer, subtitleId: String) {
        val videoMode = player.currentMediaItem?.mediaMetadata?.extras
            ?.getBoolean(EXTRA_VIDEO_MODE, false) == true
        val selection = subtitleId.takeIf { videoMode && it.isNotBlank() }?.let { requestedId ->
            player.currentTracks.groups.firstNotNullOfOrNull { group ->
                if (group.type != C.TRACK_TYPE_TEXT) return@firstNotNullOfOrNull null
                val index = (0 until group.length).firstOrNull { group.getTrackFormat(it).id == requestedId }
                    ?: return@firstNotNullOfOrNull null
                TrackSelectionOverride(group.mediaTrackGroup, listOf(index))
            }
        }
        runCatching {
            val builder = player.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, selection == null)
                .setPreferredTextLanguage(null)
            if (selection != null) builder.setOverrideForType(selection)
            player.trackSelectionParameters = builder.build()
        }.onFailure { Timber.w(it, "Subtitle track selection update failed") }
    }

    private fun startMemoryGuard(player: ExoPlayer) {
        memoryGuardJob?.cancel()
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val threshold = PlaybackMemoryGuardPolicy.thresholdBytes(
            totalDeviceMemoryBytes = memoryInfo.totalMem,
            lowRamDevice = activityManager.isLowRamDevice
        )
        memoryGuardJob = serviceScope.launch {
            while (isActive) {
                delay(PlaybackMemoryGuardPolicy.SAMPLE_INTERVAL_MS)
                if (!player.isPlaying) {
                    memoryGuardHighSamples = 0
                    continue
                }
                val nativeAllocatedBytes = withContext(Dispatchers.Default) { Debug.getNativeHeapAllocatedSize() }
                if (activePlayer !== player || !player.isPlaying) {
                    memoryGuardHighSamples = 0
                    continue
                }
                memoryGuardHighSamples = PlaybackMemoryGuardPolicy.nextHighSampleCount(
                    current = memoryGuardHighSamples,
                    nativeAllocatedBytes = nativeAllocatedBytes,
                    thresholdBytes = threshold
                )
                val now = SystemClock.elapsedRealtime()
                if (
                    PlaybackMemoryGuardPolicy.shouldRecycle(
                        highSamples = memoryGuardHighSamples,
                        nowElapsedMs = now,
                        lastRecycleElapsedMs = lastMemoryRecycleElapsedMs
                    )
                ) {
                    memoryGuardHighSamples = 0
                    lastMemoryRecycleElapsedMs = now
                    recyclePlaybackPipeline(player, threshold, nativeAllocatedBytes)
                }
            }
        }
    }

    private fun recyclePlaybackPipeline(
        player: ExoPlayer,
        thresholdBytes: Long,
        nativeAllocatedBytes: Long
    ) {
        val item = player.currentMediaItem ?: return
        val position = player.currentPosition.coerceAtLeast(0L)
        val resumePlayback = player.playWhenReady
        Timber.w(
            "Native playback memory %d bytes above %d, recycling playback pipeline",
            nativeAllocatedBytes,
            thresholdBytes
        )
        cancelQueueTransition()
        clearPreparedQueueNextInternal()
        runCatching { player.stop() }.onFailure { Timber.w(it, "Memory guard stop failed") }
        runCatching { player.clearMediaItems() }.onFailure { Timber.w(it, "Memory guard clear failed") }
        runCatching {
            player.setMediaItem(item, position)
            RuntimeHooks.player(
                action = RuntimeSignal.PLAYER_PREPARE,
                mode = if (item.mediaMetadata.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true) {
                    RuntimeSignal.MODE_VIDEO
                } else {
                    RuntimeSignal.MODE_AUDIO
                }
            )
            RuntimeHooks.hot(RuntimeSignal.HOT_PLAYER_PREPARE)
            player.prepare()
            player.playWhenReady = resumePlayback
        }.onFailure { Timber.w(it, "Memory guard playback restore failed") }
    }

    private fun cancelQueueTransition() {
        queueTransitionJob?.cancel()
        queueTransitionJob = null
        isQueueTransitionInProgress = false
        activePlayer?.volume = 1f
        releaseTransitionPlayer()
    }

    private fun pausePlaybackForSleepTimer() {
        cancelQueueTransition()
        mediaSession?.player?.pause()
    }

    private fun releaseTransitionPlayer(expected: ExoPlayer? = null) {
        val player = transitionPlayer ?: return
        if (expected != null && player !== expected) return
        transitionPlayer = null
        runCatching { player.pause() }
            .onFailure { Timber.w(it, "Queue crossfade secondary pause failed") }
        runCatching { player.clearMediaItems() }
            .onFailure { Timber.w(it, "Queue crossfade secondary clear failed") }
        runCatching { player.release() }
            .onFailure { Timber.w(it, "Queue crossfade secondary release failed") }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (shouldCancelPrefetchForMemoryPressure(level)) {
            clearPreparedQueueNextInternal()
            cancelQueueTransition()
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
        RuntimeHooks.dsp(RuntimeSignal.DSP_RELEASED)
        queueSkipJob?.cancel()
        clearPreparedQueueNextInternal()
        serviceRecoveryJob?.cancel()
        stickyRestoreJob?.cancel()
        playbackWatchdogJob?.cancel()
        cancelQueueTransition()
        memoryGuardJob?.cancel()
        castHandoffJob?.cancel()
        queueTransitionMonitorJob?.cancel()
        sleepTimerStateJob?.cancel()
        sleepTimer.cancel()
        _sleepTimerStateFlow.value = PlaybackSleepTimerState.Disabled
        mediaSession?.player?.let { queueEngine.updatePosition(it.currentPosition) }
        releasePlaybackWakeLock()
        synchronized(premiumAudioSettingsLock) {
            if (activeService === this) activeService = null
        }
        if (::autoLibrary.isInitialized) autoLibrary.close()
        serviceScope.cancel()
        mediaSession?.run {
            RuntimeHooks.player(RuntimeSignal.PLAYER_RELEASED)
            player.release()
            release()
        }
        activePlayer = null
        mediaSession = null
        runCatching {
            (getSystemService(Context.AUDIO_SERVICE) as AudioManager).unregisterAudioDeviceCallback(audioDeviceCallback)
        }
        super.onDestroy()
    }

    private fun handoffQueueToCast(castPlayer: Player) {
        val snapshot = queueEngine.state.value
        val current = snapshot.currentTrack ?: return
        val handoff = CastHandoffConverter.toHandoff(
            LocalPlaybackSnapshot(
                queueIds = snapshot.tracks.map(::playbackQueueIdentity),
                currentIndex = snapshot.currentIndex,
                positionMs = castPlayer.currentPosition,
                playing = castPlayer.playWhenReady,
                shuffle = snapshot.shuffleEnabled,
                repeatMode = snapshot.repeatMode
            )
        )
        val generation = snapshot.generation
        val window = snapshot.tracks.subList(
            handoff.windowStartIndex,
            handoff.windowStartIndex + handoff.queueWindowIds.size
        )
        castHandoffJob?.cancel()
        castHandoffJob = serviceScope.launch(Dispatchers.IO) {
            val resolved = window.map { track -> resolver.resolve(track, isVideoMode = false) }
            val currentState = queueEngine.state.value
            if (currentState.generation != generation ||
                currentState.currentTrack?.let(::playbackQueueIdentity) != playbackQueueIdentity(current)
            ) return@launch
            withContext(Dispatchers.Main) {
                if (castPlayer.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE) return@withContext
                castPlayer.setMediaItems(resolved.map { LevyraMediaItemFactory.build(it) }, handoff.currentIndex, handoff.positionMs)
                castPlayer.shuffleModeEnabled = handoff.shuffle
                castPlayer.repeatMode = when (handoff.repeatMode) {
                    com.luc4n3x.levyra.domain.RepeatMode.One -> Player.REPEAT_MODE_ONE
                    com.luc4n3x.levyra.domain.RepeatMode.All -> Player.REPEAT_MODE_ALL
                    com.luc4n3x.levyra.domain.RepeatMode.Off -> Player.REPEAT_MODE_OFF
                }
                castPlayer.prepare()
                if (handoff.playing) castPlayer.play()
            }
        }
    }

    private fun skipCastQueue(forward: Boolean, castPlayer: Player) {
        queueSkipJob?.cancel()
        queueSkipJob = serviceScope.launch(Dispatchers.IO) {
            val selected = if (forward) queueEngine.next(respectRepeatOne = false) else queueEngine.previous()
            if (selected != null) withContext(Dispatchers.Main) { handoffQueueToCast(castPlayer) }
        }
    }

    private fun refreshAudioOutputProfile() {
        val manager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val types = routedOutputTypes(manager)
        equalizerProcessor.outputProfile = when {
            types.any { it == AudioDeviceInfo.TYPE_USB_DEVICE || it == AudioDeviceInfo.TYPE_USB_HEADSET || it == AudioDeviceInfo.TYPE_USB_ACCESSORY } -> LevyraEqualizerAudioProcessor.OutputProfile.USB
            types.any { it == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it == AudioDeviceInfo.TYPE_WIRED_HEADSET || it == AudioDeviceInfo.TYPE_LINE_ANALOG } -> LevyraEqualizerAudioProcessor.OutputProfile.WIRED
            types.any(::isBluetoothOutputType) -> LevyraEqualizerAudioProcessor.OutputProfile.BLUETOOTH
            else -> LevyraEqualizerAudioProcessor.OutputProfile.SPEAKER
        }
    }

    private fun isBluetoothOutputType(type: Int): Boolean =
        type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)

    @Suppress("DEPRECATION")
    private fun routedOutputTypes(manager: AudioManager): Set<Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return runCatching {
                manager.getAudioDevicesForAttributes(platformMediaAudioAttributes).map { it.type }.toSet()
            }.getOrDefault(emptySet())
        }
        return when {
            manager.isBluetoothA2dpOn -> setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            manager.isBluetoothScoOn -> setOf(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
            manager.isWiredHeadsetOn -> setOf(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
            else -> emptySet()
        }
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


    private data class ServiceRecoveryPlan(
        val localPlayback: Boolean,
        val delaysMs: LongArray,
        val positionMs: Long
    )

    private fun discardIncompatiblePlaybackCache(error: PlaybackException) {
        val reason = playbackFailureReasonOf(error)
        val plan = playbackRecoveryPlanFor(classifyPlaybackFailureReason(reason))
        if (!plan.invalidateCache) return
        val track = queueEngine.state.value.currentTrack ?: return
        if (isLocalPlaybackTrack(track)) return
        val videoMode = mediaSession?.player?.currentMediaItem
            ?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true
        val keys = playbackCacheKeysToDiscard(
            streamKey = LevyraPlaybackCacheKey.stream(track),
            videoKey = LevyraPlaybackCacheKey.video(track),
            videoMode = videoMode,
            hasSeparateVideoStream = track.videoStreamUrl.isNotBlank()
        )
        serviceScope.launch(Dispatchers.IO) {
            val cache = runCatching { LevyraMediaCache.get(this@PlaybackService) }.getOrNull() ?: return@launch
            keys.forEach { key ->
                if (removePlaybackCacheResource(cache, key)) {
                    RuntimeHooks.cache(RuntimeSignal.CACHE_EVICTION)
                    Timber.w("Discarded unplayable cache entry key=%s", key)
                }
            }
        }
    }

    private fun scheduleServiceRecovery(error: PlaybackException) {
        val plan = serviceRecoveryPlan() ?: return
        RuntimeHooks.player(RuntimeSignal.PLAYER_RECOVERY)
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

    private suspend fun hasCompletePlaybackCache(track: Track, videoMode: Boolean): Boolean {
        if (videoMode || isLocalPlaybackTrack(track) || track.streamUrl.isBlank()) return false
        return withContext(Dispatchers.IO) {
            val cache = runCatching { LevyraMediaCache.get(this@PlaybackService) }.getOrNull()
                ?: return@withContext false
            isPlaybackResourceFullyCached(cache, LevyraPlaybackCacheKey.stream(track))
        }
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
            preferFreshResolution && queueTrack != null && hasCompletePlaybackCache(queueTrack, videoMode) -> {
                Timber.d("Background recovery replayed a complete cache entry")
                LevyraMediaItemFactory.build(queueTrack, videoMode)
            }
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
        RuntimeHooks.player(
            action = RuntimeSignal.PLAYER_PREPARE,
            mode = if (mediaItem.mediaMetadata.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true) {
                RuntimeSignal.MODE_VIDEO
            } else {
                RuntimeSignal.MODE_AUDIO
            }
        )
        RuntimeHooks.hot(RuntimeSignal.HOT_PLAYER_PREPARE)
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
        refreshAudioOutputProfile()
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
private object LevyraPlaybackLoadErrorHandlingPolicy : LoadErrorHandlingPolicy {
    override fun getFallbackSelectionFor(
        fallbackOptions: LoadErrorHandlingPolicy.FallbackOptions,
        loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo
    ): LoadErrorHandlingPolicy.FallbackSelection? = null

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long =
        C.TIME_UNSET

    override fun getMinimumLoadableRetryCount(dataType: Int): Int = 0
}

@UnstableApi
private class LevyraMediaSourceFactory(
    private val delegate: DefaultMediaSourceFactory,
    private val dataSourceFactory: DataSource.Factory,
    private val localDataSourceFactory: DataSource.Factory
) : MediaSource.Factory {
    private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy = LevyraPlaybackLoadErrorHandlingPolicy

    private val subtitleDataSourceFactory: DataSource.Factory by lazy {
        OkHttpDataSource.Factory(LevyraHttpClientFactory.externalIntegrations())
    }

    override fun getSupportedTypes(): IntArray = delegate.supportedTypes

    override fun setDrmSessionManagerProvider(
        provider: androidx.media3.exoplayer.drm.DrmSessionManagerProvider
    ): MediaSource.Factory {
        delegate.setDrmSessionManagerProvider(provider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(
        policy: LoadErrorHandlingPolicy
    ): MediaSource.Factory {
        loadErrorHandlingPolicy = policy
        delegate.setLoadErrorHandlingPolicy(policy)
        return this
    }

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val videoUrl = mediaItem.mediaMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_URL)
            ?: mediaItem.requestMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_URL)

        if (videoUrl.isNullOrBlank()) {
            return mergeSubtitles(mediaItem, mediaSourceFor(mediaItem))
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

        return mergeSubtitles(mediaItem, MergingMediaSource(true, true, videoSource, audioSource))
    }

    private fun mergeSubtitles(mediaItem: MediaItem, primarySource: MediaSource): MediaSource {
        val configurations = mediaItem.localConfiguration?.subtitleConfigurations.orEmpty()
        if (configurations.isEmpty()) return primarySource
        val subtitleSources = configurations.map { configuration ->
            SingleSampleMediaSource.Factory(subtitleDataSourceFactory)
                .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                .createMediaSource(configuration, C.TIME_UNSET)
        }
        return MergingMediaSource(true, true, primarySource, *subtitleSources.toTypedArray())
    }

    private fun mediaSourceFor(mediaItem: MediaItem): MediaSource {
        val localUri = mediaItem.localConfiguration?.uri
        val scheme = localUri?.scheme.orEmpty().lowercase()
        if (scheme == "content" || scheme == "file") {
            val localItem = mediaItem.buildUpon().setCustomCacheKey(null).build()
            return ProgressiveMediaSource.Factory(localDataSourceFactory)
                .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                .createMediaSource(localItem)
        }
        val uri = localUri?.toString().orEmpty()
        // The declared MIME type wins over URL shape: a YouTube HLS audio manifest whose URL does
        // not look like a playlist would otherwise reach the progressive extractor, which fails on
        // the #EXTM3U header instead of playing.
        val mimeType = mediaItem.localConfiguration?.mimeType.orEmpty()
        return when {
            isHlsMimeType(mimeType) || isHlsManifestUri(uri) ->
                HlsMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                    .createMediaSource(mediaItem)
            isDashMimeType(mimeType) || isDashManifestUri(uri) ->
                DashMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                    .createMediaSource(mediaItem)
            else -> ProgressiveMediaSource.Factory(dataSourceFactory)
                .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                .createMediaSource(mediaItem)
        }
    }

    private fun isHlsMimeType(mimeType: String): Boolean =
        mimeType.equals(MimeTypes.APPLICATION_M3U8, ignoreCase = true) ||
            mimeType.equals("application/vnd.apple.mpegurl", ignoreCase = true)

    private fun isDashMimeType(mimeType: String): Boolean =
        mimeType.equals(MimeTypes.APPLICATION_MPD, ignoreCase = true)

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
