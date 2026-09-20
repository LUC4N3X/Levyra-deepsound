package com.luc4n3x.levyra.player

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

class PlaybackTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sessionJob: Job? = null
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingToggle = false
    private var listening = false
    private var strings: LevyraStrings? = null
    private var lastProjection: PlaybackTileProjection? = null

    private val playbackListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            renderTile(player)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        listening = true
        strings = localizedStrings()
        if (hasActivePlaybackSession()) {
            connect()
        } else {
            renderTile(null)
        }
        observeSession()
    }

    override fun onStopListening() {
        sessionJob?.cancel()
        sessionJob = null
        listening = false
        releaseController(force = false)
        super.onStopListening()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        strings = localizedStrings()
        if (hasActivePlaybackSession()) {
            connect()
        } else {
            renderTile(null)
        }
    }

    override fun onTileRemoved() {
        sessionJob?.cancel()
        sessionJob = null
        releaseController(force = true)
        super.onTileRemoved()
    }

    override fun onDestroy() {
        listening = false
        sessionJob?.cancel()
        sessionJob = null
        scope.cancel()
        releaseController(force = true)
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        val connected = controller
        if (connected != null) {
            performAction(connected)
            return
        }
        if (!hasActivePlaybackSession()) {
            openApp()
            return
        }
        pendingToggle = true
        connect()
    }

    private fun hasActivePlaybackSession(): Boolean =
        PlaybackService.activePlayer != null

    private fun observeSession() {
        sessionJob?.cancel()
        sessionJob = scope.launch {
            PlaybackService.activePlayerFlow.collect { player ->
                if (player != null && controller == null && controllerFuture == null) {
                    connect()
                } else if (player == null && controller != null && !pendingToggle) {
                    releaseController(force = false)
                    renderTile(null)
                }
            }
        }
    }

    private fun connect() {
        if (controller != null || controllerFuture != null) return
        val appContext = applicationContext
        val future = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        ).buildAsync()
        controllerFuture = future
        future.addListener({
            if (controllerFuture === future) controllerFuture = null
            val connected = runCatching { future.get() }.getOrNull()
            if (connected == null) {
                Timber.w("Levyra playback tile could not attach to the playback session")
                if (pendingToggle) {
                    pendingToggle = false
                    openApp()
                }
                if (!listening) {
                    releaseController(force = true)
                }
                return@addListener
            }
            if (!listening && !pendingToggle) {
                connected.release()
                return@addListener
            }
            controller = connected
            connected.addListener(playbackListener)
            renderTile(connected)
            if (pendingToggle) {
                pendingToggle = false
                performAction(connected)
                if (!listening) {
                    releaseController(force = true)
                }
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun releaseController(force: Boolean = false) {
        if (!force && pendingToggle) return
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller?.removeListener(playbackListener)
        controller?.release()
        controller = null
        lastProjection = null
        pendingToggle = false
    }

    private fun performAction(player: MediaController) {
        when (playbackTileProjection(
            isPlaying = player.isPlaying,
            playWhenReady = player.playWhenReady,
            playbackState = player.playbackState,
            mediaItemCount = player.mediaItemCount
        ).action) {
            PlaybackTileAction.Pause -> player.pause()
            PlaybackTileAction.Resume -> player.play()
            PlaybackTileAction.OpenApp -> openApp()
        }
    }

    private fun renderTile(player: Player?) {
        val tile = qsTile ?: return
        val projection = player?.let {
            playbackTileProjection(
                isPlaying = it.isPlaying,
                playWhenReady = it.playWhenReady,
                playbackState = it.playbackState,
                mediaItemCount = it.mediaItemCount
            )
        } ?: playbackTileProjection(
            isPlaying = false,
            playWhenReady = false,
            playbackState = Player.STATE_IDLE,
            mediaItemCount = 0
        )
        if (projection == lastProjection) return
        lastProjection = projection
        val localized = strings ?: localizedStrings().also { strings = it }
        tile.label = localized.playbackTileLabel
        tile.state = if (projection.kind == PlaybackTileProjectionKind.Active) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = when (projection.description) {
                PlaybackTileDescription.Playing -> localized.playing
                PlaybackTileDescription.Paused -> localized.playbackPaused
                PlaybackTileDescription.Idle -> localized.playbackTileIdle
            }
        }
        tile.updateTile()
    }

    private fun openApp() {
        TileServiceCompat.startActivityAndCollapse(
            this,
            PendingIntentActivityWrapper(
                this,
                REQUEST_TILE_LAUNCH,
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT,
                false
            )
        )
    }

    private fun localizedStrings(): LevyraStrings =
        LevyraStrings.forCode(LevyraPreferences(this).snapshot().languageCode)

    private companion object {
        const val REQUEST_TILE_LAUNCH = 5422
    }
}
