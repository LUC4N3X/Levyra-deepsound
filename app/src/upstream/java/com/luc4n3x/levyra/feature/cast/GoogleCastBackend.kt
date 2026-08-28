@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.luc4n3x.levyra.feature.cast

import android.content.Context
import androidx.media3.cast.Cast
import androidx.media3.cast.CastPlayer
import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlayerTransferState
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.LevyraMediaItemFactory
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class GoogleCastBackend(private val context: Context) : RemotePlaybackBackend {
    private val appContext = context.applicationContext
    private val cast = runCatching {
        Cast.getSingletonInstance(appContext).apply {
            if (needsInitialization()) initialize()
        }
    }.getOrNull()

    override val id: String = "google-cast"
    override val available: Boolean = cast != null

    private val mutableState = MutableStateFlow(
        RemotePlaybackState(
            availability = if (available) RemotePlaybackAvailability.Idle else RemotePlaybackAvailability.Unavailable
        )
    )
    override val state: StateFlow<RemotePlaybackState> = mutableState

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val queueEngine by lazy { PersistentQueueEngine.get(appContext) }
    private val resolver by lazy { PlaybackResolver.getInstance(appContext) }
    private var managedQueueJob: Job? = null
    private var player: CastPlayer? = null

    private val transferCallback = CastPlayer.TransferCallback { sourcePlayer, targetPlayer ->
        if (sourcePlayer.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE) {
            val current = sourcePlayer.currentMediaItem
            if (current == null || !isReceiverSafe(current)) {
                Timber.w("Cast transfer skipped because the local media URL is not receiver-safe")
                return@TransferCallback
            }
            CastPlayer.TransferCallback.DEFAULT.transferState(sourcePlayer, targetPlayer)
            return@TransferCallback
        }

        val current = sourcePlayer.currentMediaItem
        if (current == null) {
            CastPlayer.TransferCallback.DEFAULT.transferState(sourcePlayer, targetPlayer)
            return@TransferCallback
        }

        PlayerTransferState.fromPlayer(sourcePlayer)
            .buildUpon()
            .setMediaItems(listOf(current))
            .setCurrentMediaItemIndex(0)
            .setCurrentPosition(sourcePlayer.currentPosition.coerceAtLeast(0L))
            .setShuffleModeEnabled(false)
            .setRepeatMode(
                if (sourcePlayer.repeatMode == Player.REPEAT_MODE_ONE) {
                    Player.REPEAT_MODE_ONE
                } else {
                    Player.REPEAT_MODE_OFF
                }
            )
            .build()
            .setToPlayer(targetPlayer)
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)

        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
            player?.let { active ->
                publish(active)
                if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
                    maintainManagedQueue(active)
                } else {
                    managedQueueJob?.cancel()
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            player?.let { active ->
                publish(active)
                if (active.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
                    maintainManagedQueue(active)
                }
            }
        }
    }

    override fun devices(): Flow<List<RemoteDevice>> = flowOf(emptyList())

    override suspend fun connect(device: RemoteDevice) = Unit

    override suspend fun disconnect() {
        val castRuntime = cast ?: return
        withContext(Dispatchers.Main) {
            runCatching { castRuntime.endCurrentSession(true) }
        }
    }

    override suspend fun load(handoff: CastHandoff) = Unit

    override suspend fun play() {
        withContext(Dispatchers.Main) { player?.play() }
    }

    override suspend fun pause() {
        withContext(Dispatchers.Main) { player?.pause() }
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) { player?.stop() }
    }

    override suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.Main) {
            player?.seekTo(positionMs.coerceAtLeast(0L))
        }
    }

    override suspend fun skipToNext() {
        withContext(Dispatchers.Main) { player?.seekToNextMediaItem() }
    }

    override suspend fun skipToPrevious() {
        withContext(Dispatchers.Main) { player?.seekToPreviousMediaItem() }
    }

    override suspend fun setShuffle(enabled: Boolean) {
        withContext(Dispatchers.Main) {
            player?.shuffleModeEnabled = false
        }
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        withContext(Dispatchers.Main) {
            player?.repeatMode = remoteRepeatMode(mode)
        }
    }

    override fun attachLocalPlayer(localPlayer: Player): Player {
        if (!available) return localPlayer
        val castPlayer = runCatching {
            CastPlayer.Builder(appContext)
                .setLocalPlayer(localPlayer)
                .setTransferCallback(transferCallback)
                .build()
                .also { attached ->
                    player?.removeListener(listener)
                    player?.release()
                    player = attached
                    attached.addListener(listener)
                    publish(attached)
                }
        }.getOrElse { return localPlayer }

        return object : ForwardingPlayer(castPlayer) {
            override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
                if (mediaItems.any { !isReceiverSafe(it) }) {
                    Timber.w("Cast playlist rejected because it contains a non receiver-safe media URL")
                    return
                }
                super.setMediaItems(mediaItems, startIndex, startPositionMs)
            }

            override fun release() {
                this@GoogleCastBackend.release()
                runCatching { localPlayer.release() }
                    .onFailure { Timber.w(it, "Local player release after Cast teardown failed") }
            }
        }
    }

    override fun release() {
        managedQueueJob?.cancel()
        managedQueueJob = null
        player?.removeListener(listener)
        player?.release()
        player = null
        scope.cancel()
    }

    private fun maintainManagedQueue(active: CastPlayer) {
        val currentItem = active.currentMediaItem ?: return
        if (!isReceiverSafe(currentItem)) {
            Timber.w("Cast queue maintenance skipped for a non receiver-safe current item")
            return
        }

        val before = queueEngine.state.value
        val currentIndex = before.tracks.indexOfFirst { mediaId(it) == currentItem.mediaId }
        if (currentIndex >= 0 && currentIndex != before.currentIndex) {
            queueEngine.select(currentIndex, active.currentPosition.coerceAtLeast(0L), rememberCurrent = true)
        }

        val snapshot = queueEngine.state.value
        val currentTrack = snapshot.currentTrack ?: return
        if (mediaId(currentTrack) != currentItem.mediaId) return

        if (active.shuffleModeEnabled) active.shuffleModeEnabled = false
        val desiredRepeat = remoteRepeatMode(snapshot.repeatMode)
        if (active.repeatMode != desiredRepeat) active.repeatMode = desiredRepeat

        val expectedTracks = queueEngine.upcoming(CAST_FORWARD_ITEMS)
            .asSequence()
            .filter { mediaId(it) != currentItem.mediaId }
            .distinctBy(::mediaId)
            .take(CAST_FORWARD_ITEMS)
            .toList()
        val expectedIds = expectedTracks.map(::mediaId)
        val activeIndex = active.currentMediaItemIndex.takeIf { it >= 0 } ?: return
        val remoteForwardIds = ((activeIndex + 1) until active.mediaItemCount)
            .map { active.getMediaItemAt(it).mediaId }

        val comparable = minOf(expectedIds.size, remoteForwardIds.size)
        val prefixMatches = remoteForwardIds.take(comparable) == expectedIds.take(comparable)
        val unexpectedTail = remoteForwardIds.size > expectedIds.size
        val needsTopUp = castWindowNeedsRefresh(
            expectedUpcomingIds = expectedIds,
            remoteForwardIds = remoteForwardIds,
            minimumBufferedItems = expectedIds.size.coerceAtLeast(1)
        )

        when {
            !prefixMatches || unexpectedTail -> rebuildManagedQueue(active, currentItem, expectedTracks, desiredRepeat)
            needsTopUp -> appendManagedQueue(active, currentItem.mediaId, expectedTracks.drop(remoteForwardIds.size))
            else -> prunePlayedItems(active)
        }
    }

    private fun appendManagedQueue(active: CastPlayer, expectedCurrentId: String, tracks: List<Track>) {
        if (tracks.isEmpty()) {
            prunePlayedItems(active)
            return
        }
        managedQueueJob?.cancel()
        managedQueueJob = scope.launch {
            val resolved = resolveReceiverTracks(tracks) ?: return@launch
            if (active.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE ||
                active.currentMediaItem?.mediaId != expectedCurrentId
            ) return@launch
            active.addMediaItems(resolved.map { LevyraMediaItemFactory.build(it) })
            prunePlayedItems(active)
        }
    }

    private fun rebuildManagedQueue(
        active: CastPlayer,
        currentItem: MediaItem,
        tracks: List<Track>,
        repeatMode: Int
    ) {
        managedQueueJob?.cancel()
        val requestedPosition = active.currentPosition.coerceAtLeast(0L)
        val resumePlayback = active.playWhenReady
        val expectedCurrentId = currentItem.mediaId
        managedQueueJob = scope.launch {
            val resolved = resolveReceiverTracks(tracks) ?: return@launch
            if (active.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE ||
                active.currentMediaItem?.mediaId != expectedCurrentId
            ) return@launch
            val position = castHandoffStartPositionMs(
                sameMediaItem = true,
                requestedPositionMs = requestedPosition,
                livePositionMs = active.currentPosition,
                remotePlayback = true
            )
            active.setMediaItems(
                listOf(currentItem) + resolved.map { LevyraMediaItemFactory.build(it) },
                0,
                position
            )
            active.shuffleModeEnabled = false
            active.repeatMode = repeatMode
            active.prepare()
            if (resumePlayback) active.play()
        }
    }

    private suspend fun resolveReceiverTracks(tracks: List<Track>): List<Track>? = withContext(Dispatchers.IO) {
        try {
            tracks.map { resolver.resolve(it, isVideoMode = false) }
                .takeIf { resolved -> resolved.all { isCastReceiverSafeMediaUrl(it.streamUrl) } }
                .also { resolved ->
                    if (resolved == null) Timber.w("Cast queue refresh rejected a non receiver-safe resolved URL")
                }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Cast queue refresh failed")
            null
        }
    }

    private fun prunePlayedItems(active: CastPlayer) {
        val index = active.currentMediaItemIndex
        if (index > 0) active.removeMediaItems(0, index)
    }

    private fun isReceiverSafe(item: MediaItem): Boolean =
        item.localConfiguration?.uri?.toString()?.let(::isCastReceiverSafeMediaUrl) == true

    private fun mediaId(track: Track): String = LevyraMediaItemFactory.metadataOnly(track).mediaId

    private fun remoteRepeatMode(mode: RepeatMode): Int =
        if (mode == RepeatMode.One) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

    private fun publish(active: Player) {
        val connected = active.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
        val deviceName = runCatching {
            cast?.getCurrentCastSession()?.castDevice?.friendlyName
        }.getOrNull()
        mutableState.value = RemotePlaybackState(
            availability = when {
                !available -> RemotePlaybackAvailability.Unavailable
                connected -> RemotePlaybackAvailability.Connected
                else -> RemotePlaybackAvailability.Idle
            },
            connected = connected,
            deviceName = deviceName,
            queueIds = (0 until active.mediaItemCount).map { active.getMediaItemAt(it).mediaId },
            currentIndex = active.currentMediaItemIndex,
            positionMs = active.currentPosition.coerceAtLeast(0L),
            playing = active.playWhenReady,
            shuffle = queueEngine.state.value.shuffleEnabled,
            repeatMode = queueEngine.state.value.repeatMode
        )
    }

    private companion object {
        const val CAST_FORWARD_ITEMS = 4
    }
}
