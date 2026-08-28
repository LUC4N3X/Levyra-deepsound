package com.luc4n3x.levyra.feature.cast

import android.content.Context
import androidx.media3.cast.CastPlayer
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Player
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

class GoogleCastBackend(private val context: Context) : RemotePlaybackBackend {
    override val id: String = "google-cast"
    override val available: Boolean = runCatching { CastContext.getSharedInstance(context) }.isSuccess

    private val mutableState = MutableStateFlow(
        RemotePlaybackState(
            availability = if (available) RemotePlaybackAvailability.Idle else RemotePlaybackAvailability.Unavailable
        )
    )
    override val state: StateFlow<RemotePlaybackState> = mutableState
    private var player: CastPlayer? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
            player?.let(::publish)
        }
    }

    override fun devices(): Flow<List<RemoteDevice>> = flowOf(emptyList())

    override suspend fun connect(device: RemoteDevice) = Unit

    override suspend fun disconnect() {
        runCatching { CastContext.getSharedInstance(context).sessionManager.endCurrentSession(true) }
    }

    override suspend fun load(handoff: CastHandoff) = Unit

    override suspend fun play() { player?.play() }
    override suspend fun pause() { player?.pause() }
    override suspend fun stop() { player?.stop() }
    override suspend fun seekTo(positionMs: Long) { player?.seekTo(positionMs.coerceAtLeast(0L)) }
    override suspend fun skipToNext() { player?.seekToNextMediaItem() }
    override suspend fun skipToPrevious() { player?.seekToPreviousMediaItem() }
    override suspend fun setShuffle(enabled: Boolean) { player?.shuffleModeEnabled = enabled }
    override suspend fun setRepeatMode(mode: com.luc4n3x.levyra.domain.RepeatMode) {
        player?.repeatMode = when (mode) {
            com.luc4n3x.levyra.domain.RepeatMode.One -> Player.REPEAT_MODE_ONE
            com.luc4n3x.levyra.domain.RepeatMode.All -> Player.REPEAT_MODE_ALL
            com.luc4n3x.levyra.domain.RepeatMode.Off -> Player.REPEAT_MODE_OFF
        }
    }

    override fun attachLocalPlayer(localPlayer: Player): Player {
        if (!available) return localPlayer
        return runCatching {
            CastPlayer.Builder(context)
                .setLocalPlayer(localPlayer)
                .build()
                .also {
                    player = it
                    it.addListener(listener)
                    publish(it)
                }
        }.getOrElse { localPlayer }
    }

    override fun release() {
        player?.removeListener(listener)
        player?.release()
        player = null
    }

    private fun publish(player: Player) {
        val connected = player.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
        val deviceName = runCatching {
            CastContext.getSharedInstance(context).sessionManager.currentCastSession?.castDevice?.friendlyName
        }.getOrNull()
        mutableState.value = RemotePlaybackState(
            availability = when {
                !available -> RemotePlaybackAvailability.Unavailable
                connected -> RemotePlaybackAvailability.Connected
                else -> RemotePlaybackAvailability.Idle
            },
            connected = connected,
            deviceName = deviceName,
            queueIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId },
            currentIndex = player.currentMediaItemIndex,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            playing = player.playWhenReady,
            shuffle = player.shuffleModeEnabled,
            repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> com.luc4n3x.levyra.domain.RepeatMode.One
                Player.REPEAT_MODE_ALL -> com.luc4n3x.levyra.domain.RepeatMode.All
                else -> com.luc4n3x.levyra.domain.RepeatMode.Off
            }
        )
    }
}
