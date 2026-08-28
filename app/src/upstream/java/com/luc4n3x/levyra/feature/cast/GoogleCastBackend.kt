package com.luc4n3x.levyra.feature.cast

import android.content.Context
import androidx.media3.cast.CastPlayer
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Player
import androidx.media3.common.PlayerTransferState
import com.google.android.gms.cast.framework.CastContext
import com.luc4n3x.levyra.domain.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

class GoogleCastBackend(private val context: Context) : RemotePlaybackBackend {
    private val appContext = context.applicationContext

    override val id: String = "google-cast"
    override val available: Boolean = runCatching { CastContext.getSharedInstance(appContext) }.isSuccess

    private val mutableState = MutableStateFlow(
        RemotePlaybackState(
            availability = if (available) RemotePlaybackAvailability.Idle else RemotePlaybackAvailability.Unavailable
        )
    )
    override val state: StateFlow<RemotePlaybackState> = mutableState

    private var player: CastPlayer? = null

    private val transferCallback = CastPlayer.TransferCallback { sourcePlayer, targetPlayer ->
        if (sourcePlayer.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE) {
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
            player?.let(::publish)
        }
    }

    override fun devices(): Flow<List<RemoteDevice>> = flowOf(emptyList())

    override suspend fun connect(device: RemoteDevice) = Unit

    override suspend fun disconnect() {
        val sessionManager = runCatching {
            CastContext.getSharedInstance(appContext).sessionManager
        }.getOrNull() ?: return
        withContext(Dispatchers.Main) {
            sessionManager.endCurrentSession(true)
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
            player?.shuffleModeEnabled = enabled
        }
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        withContext(Dispatchers.Main) {
            player?.repeatMode = when (mode) {
                RepeatMode.One -> Player.REPEAT_MODE_ONE
                RepeatMode.All -> Player.REPEAT_MODE_ALL
                RepeatMode.Off -> Player.REPEAT_MODE_OFF
            }
        }
    }

    override fun attachLocalPlayer(localPlayer: Player): Player {
        if (!available) return localPlayer
        return runCatching {
            CastPlayer.Builder(appContext)
                .setLocalPlayer(localPlayer)
                .setTransferCallback(transferCallback)
                .build()
                .also { castPlayer ->
                    player?.removeListener(listener)
                    player?.release()
                    player = castPlayer
                    castPlayer.addListener(listener)
                    publish(castPlayer)
                }
        }.getOrElse { localPlayer }
    }

    override fun release() {
        player?.removeListener(listener)
        player?.release()
        player = null
    }

    private fun publish(active: Player) {
        val connected = active.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
        val deviceName = runCatching {
            CastContext.getSharedInstance(appContext)
                .sessionManager
                .currentCastSession
                ?.castDevice
                ?.friendlyName
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
            shuffle = active.shuffleModeEnabled,
            repeatMode = when (active.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.One
                Player.REPEAT_MODE_ALL -> RepeatMode.All
                else -> RepeatMode.Off
            }
        )
    }
}
