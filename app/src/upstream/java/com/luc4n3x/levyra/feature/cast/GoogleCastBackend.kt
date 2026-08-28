package com.luc4n3x.levyra.feature.cast

import android.content.Context
import androidx.media3.cast.CastPlayer
import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlayerTransferState
import com.google.android.gms.cast.framework.CastContext
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.LevyraMediaItemFactory
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import com.luc4n3x.levyra.player.queue.playbackQueueIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class GoogleCastBackend(private val context: Context) : RemotePlaybackBackend {
    private val appContext = context.applicationContext
    private val castContext by lazy { CastContext.getSharedInstance(appContext) }
    private val queueEngine = PersistentQueueEngine.get(appContext)
    private val resolver = PlaybackResolver.getInstance(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stateFlow = MutableStateFlow(RemotePlaybackState())
    private var castPlayer: CastPlayer? = null
    private var player: ManagedCastPlayer? = null
    private var queueJob: Job? = null
    private var refreshJob: Job? = null
    private var lastQueueFingerprint = ""
    private var applyingManagedQueue = false

    private val transferCallback = CastPlayer.TransferCallback { sourcePlayer, targetPlayer ->
        if (sourcePlayer.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
            val current = sourcePlayer.currentMediaItem
            if (current == null) {
                CastPlayer.TransferCallback.DEFAULT.transferState(sourcePlayer, targetPlayer)
            } else {
                val queue = queueEngine.state.value
                PlayerTransferState.fromPlayer(sourcePlayer)
                    .buildUpon()
                    .setMediaItems(listOf(current))
                    .setCurrentMediaItemIndex(0)
                    .setCurrentPosition(sourcePlayer.currentPosition.coerceAtLeast(0L))
                    .setShuffleModeEnabled(queue.shuffleEnabled)
                    .setRepeatMode(if (queue.repeatMode == RepeatMode.One) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF)
                    .build()
                    .setToPlayer(targetPlayer)
            }
        } else {
            CastPlayer.TransferCallback.DEFAULT.transferState(sourcePlayer, targetPlayer)
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)

        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
            player?.let(::publish)
            if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
                scheduleManagedQueueRefresh(force = true)
            } else {
                refreshJob?.cancel()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (player?.deviceInfo?.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
                scheduleManagedQueueRefresh(force = false)
            }
        }
    }

    override fun devices(): Flow<List<RemoteDevice>> = flowOf(emptyList())

    override fun state(): Flow<RemotePlaybackState> = stateFlow.asStateFlow()

    override fun attachLocalPlayer(localPlayer: Player): Player {
        player?.removeListener(listener)
        castPlayer?.release()
        queueJob?.cancel()
        refreshJob?.cancel()

        val created = CastPlayer.Builder(appContext)
            .setLocalPlayer(localPlayer)
            .setTransferCallback(transferCallback)
            .build()
        val managed = ManagedCastPlayer(created)
        castPlayer = created
        player = managed
        managed.addListener(listener)
        publish(managed)

        queueJob = scope.launch {
            queueEngine.state.collect { snapshot ->
                val fingerprint = buildString {
                    append(snapshot.generation)
                    append('|').append(snapshot.currentIndex)
                    append('|').append(snapshot.tracks.size)
                    append('|').append(snapshot.shuffleEnabled)
                    append('|').append(snapshot.repeatMode.name)
                }
                if (fingerprint == lastQueueFingerprint) return@collect
                lastQueueFingerprint = fingerprint
                if (managed.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
                    scheduleManagedQueueRefresh(force = false)
                }
            }
        }
        return managed
    }

    override suspend fun connect(deviceId: String) = Unit

    override suspend fun disconnect() {
        val sessionManager = runCatching { castContext.sessionManager }.getOrNull() ?: return
        withContext(Dispatchers.Main) { sessionManager.endCurrentSession(true) }
    }

    override suspend fun load(queue: List<MediaItem>, startIndex: Int, positionMs: Long, playWhenReady: Boolean) {
        val active = player ?: return
        withContext(Dispatchers.Main) {
            if (queue.isEmpty()) {
                active.clearMediaItems()
                return@withContext
            }
            val safeIndex = startIndex.coerceIn(0, queue.lastIndex)
            active.setMediaItems(queue, safeIndex, positionMs.coerceAtLeast(0L))
            active.prepare()
            active.playWhenReady = playWhenReady
            if (playWhenReady) active.play()
        }
    }

    override suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.Main) { player?.seekTo(positionMs.coerceAtLeast(0L)) }
    }

    override suspend fun play() {
        withContext(Dispatchers.Main) { player?.play() }
    }

    override suspend fun pause() {
        withContext(Dispatchers.Main) { player?.pause() }
    }

    override suspend fun next() {
        withContext(Dispatchers.Main) { player?.seekToNextMediaItem() }
    }

    override suspend fun previous() {
        withContext(Dispatchers.Main) { player?.seekToPreviousMediaItem() }
    }

    override fun release() {
        refreshJob?.cancel()
        queueJob?.cancel()
        player?.removeListener(listener)
        castPlayer?.release()
        castPlayer = null
        player = null
        scope.cancel()
    }

    private fun scheduleManagedQueueRefresh(force: Boolean) {
        val active = player ?: return
        if (active.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE) return
        val snapshot = queueEngine.state.value
        val current = snapshot.currentTrack ?: return
        val currentId = LevyraMediaItemFactory.metadataOnly(current).mediaId
        if (!force && active.currentMediaItem?.mediaId != currentId) return
        val upcoming = queueEngine.upcoming(MANAGED_QUEUE_LOOKAHEAD)
        val expectedUpcomingIds = upcoming.map { LevyraMediaItemFactory.metadataOnly(it).mediaId }
        val remoteForwardIds = buildList {
            val start = active.currentMediaItemIndex + 1
            for (index in start until active.mediaItemCount) add(active.getMediaItemAt(index).mediaId)
        }
        if (!force && !castWindowNeedsRefresh(expectedUpcomingIds, remoteForwardIds, MIN_REMOTE_BUFFERED_ITEMS)) return

        refreshJob?.cancel()
        val expectedGeneration = snapshot.generation
        val expectedCurrentIdentity = playbackQueueIdentity(current)
        refreshJob = scope.launch {
            val currentItem = active.currentMediaItem?.takeIf { it.mediaId == currentId }
                ?: resolveTrack(current)?.let(LevyraMediaItemFactory::build)
                ?: return@launch
            val resolvedUpcoming = coroutineScope {
                upcoming.map { track -> async(Dispatchers.IO) { resolveTrack(track) } }.awaitAll()
            }
            val items = buildList {
                add(currentItem)
                for (track in resolvedUpcoming) {
                    if (track == null) break
                    add(LevyraMediaItemFactory.build(track))
                }
            }
            val latest = queueEngine.state.value
            if (latest.generation != expectedGeneration ||
                latest.currentTrack?.let(::playbackQueueIdentity) != expectedCurrentIdentity ||
                active.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE
            ) return@launch

            val playing = active.playWhenReady
            val livePosition = castHandoffStartPositionMs(
                sameMediaItem = active.currentMediaItem?.mediaId == currentId,
                requestedPositionMs = latest.positionMs,
                livePositionMs = active.currentPosition,
                remotePlayback = true
            )
            applyingManagedQueue = true
            try {
                active.applyManagedModes(latest.repeatMode, latest.shuffleEnabled)
                active.setMediaItems(items, 0, livePosition)
                active.prepare()
                active.playWhenReady = playing
                if (playing) active.play()
            } finally {
                applyingManagedQueue = false
            }
        }
    }

    private suspend fun resolveTrack(track: Track): Track? = try {
        resolver.resolve(track, isVideoMode = false)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Timber.w(error, "Cast queue resolve failed for %s", track.title)
        null
    }

    private fun publish(active: Player) {
        val remote = active.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
        val session = runCatching { castContext.sessionManager.currentCastSession }.getOrNull()
        val device = session?.castDevice
        stateFlow.value = RemotePlaybackState(
            connected = remote,
            device = device?.let { RemoteDevice(it.deviceId.orEmpty(), it.friendlyName.orEmpty().ifBlank { "Google Cast" }) },
            queue = (0 until active.mediaItemCount).map(active::getMediaItemAt),
            currentIndex = active.currentMediaItemIndex,
            positionMs = active.currentPosition.coerceAtLeast(0L),
            playing = active.playWhenReady,
            shuffle = active.shuffleModeEnabled,
            repeatMode = when (active.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.One
                Player.REPEAT_MODE_ALL -> RepeatMode.All
                else -> RepeatMode.Off
            }
        )
    }

    private inner class ManagedCastPlayer(player: CastPlayer) : ForwardingPlayer(player) {
        private var logicalShuffle = player.shuffleModeEnabled
        private var logicalRepeat = player.repeatMode

        override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
            val sameItem = mediaItems.getOrNull(startIndex)?.mediaId == currentMediaItem?.mediaId
            val safePosition = castHandoffStartPositionMs(
                sameMediaItem = sameItem,
                requestedPositionMs = startPositionMs,
                livePositionMs = currentPosition,
                remotePlayback = deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
            )
            super.setMediaItems(mediaItems, startIndex, safePosition)
            if (!applyingManagedQueue && deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
                scope.launch { scheduleManagedQueueRefresh(force = true) }
            }
        }

        override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
            logicalShuffle = shuffleModeEnabled
            super.setShuffleModeEnabled(
                if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) false else shuffleModeEnabled
            )
        }

        override fun getShuffleModeEnabled(): Boolean =
            if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) logicalShuffle else super.getShuffleModeEnabled()

        override fun setRepeatMode(repeatMode: Int) {
            logicalRepeat = repeatMode
            super.setRepeatMode(
                if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE && repeatMode == Player.REPEAT_MODE_ALL) {
                    Player.REPEAT_MODE_OFF
                } else {
                    repeatMode
                }
            )
        }

        override fun getRepeatMode(): Int =
            if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) logicalRepeat else super.getRepeatMode()

        fun applyManagedModes(repeatMode: RepeatMode, shuffleEnabled: Boolean) {
            logicalShuffle = shuffleEnabled
            logicalRepeat = when (repeatMode) {
                RepeatMode.One -> Player.REPEAT_MODE_ONE
                RepeatMode.All -> Player.REPEAT_MODE_ALL
                RepeatMode.Off -> Player.REPEAT_MODE_OFF
            }
            super.setShuffleModeEnabled(false)
            super.setRepeatMode(if (repeatMode == RepeatMode.One) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF)
        }
    }

    private companion object {
        const val MANAGED_QUEUE_LOOKAHEAD = 7
        const val MIN_REMOTE_BUFFERED_ITEMS = 3
    }
}
