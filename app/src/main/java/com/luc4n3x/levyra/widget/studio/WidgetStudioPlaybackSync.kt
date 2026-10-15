package com.luc4n3x.levyra.widget.studio

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.player.SystemFavoriteState
import com.luc4n3x.levyra.player.SystemMediaActionState

internal class WidgetStudioPlaybackSync(context: Context) : Player.Listener {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var player: Player? = null
    private val progressTick = Runnable { publish() }

    fun attach(session: MediaSession) {
        detachListener()
        player = session.player.also { it.addListener(this) }
        publish()
    }

    fun detach() {
        detachListener()
        WidgetStudioStore.writeSession(appContext, false, false, "", "", "", "", 0L, 0L)
        if (WidgetStudioEngine.hasWidgets(appContext)) WidgetStudioEngine.requestRender(appContext)
    }

    fun onActionsChanged(state: SystemMediaActionState) {
        WidgetStudioStore.writeActions(
            appContext,
            favorite = when (state.favorite) {
                SystemFavoriteState.Favorite -> StudioFavoriteState.Favorite
                SystemFavoriteState.NotFavorite -> StudioFavoriteState.NotFavorite
                SystemFavoriteState.Unavailable -> StudioFavoriteState.Unavailable
            },
            shuffle = state.shuffleEnabled,
            repeat = when (state.repeatMode) {
                RepeatMode.Off -> StudioRepeatState.Off
                RepeatMode.All -> StudioRepeatState.All
                RepeatMode.One -> StudioRepeatState.One
                null -> StudioRepeatState.Unavailable
            }
        )
        if (WidgetStudioEngine.hasWidgets(appContext)) WidgetStudioEngine.requestRender(appContext)
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_MEDIA_METADATA_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY,
                Player.EVENT_TIMELINE_CHANGED
            )
        ) {
            publish()
        }
    }

    private fun detachListener() {
        handler.removeCallbacks(progressTick)
        player?.removeListener(this)
        player = null
    }

    private fun publish() {
        handler.removeCallbacks(progressTick)
        val current = player ?: return
        val metadata = current.mediaMetadata
        val playing = current.playWhenReady &&
            (current.playbackState == Player.STATE_READY || current.playbackState == Player.STATE_BUFFERING)
        val duration = current.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        val changed = WidgetStudioStore.writeSession(
            appContext,
            active = current.mediaItemCount > 0,
            playing = playing,
            title = metadata.title?.toString().orEmpty(),
            artist = (metadata.artist ?: metadata.albumArtist)?.toString().orEmpty(),
            album = metadata.albumTitle?.toString().orEmpty(),
            artworkUrl = SafeImageUrlPolicy.sanitize(metadata.artworkUri?.toString()),
            positionMs = current.currentPosition.coerceAtLeast(0L),
            durationMs = duration
        )
        if (!WidgetStudioEngine.hasWidgets(appContext)) return
        if (changed) WidgetStudioEngine.requestRender(appContext) else WidgetStudioEngine.requestProgress(appContext)
        if (playing && duration > 0L && WidgetStudioEngine.hasProgressWidgets(appContext)) {
            handler.postDelayed(progressTick, WidgetStudioStore.progressRefresh(appContext).intervalMs)
        }
    }
}
