package com.luc4n3x.levyra.player

import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList
import com.luc4n3x.levyra.data.FavoriteMembership
import com.luc4n3x.levyra.player.queue.PlaybackQueueSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber

internal class SystemMediaActionController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val queueState: StateFlow<PlaybackQueueSnapshot>,
    private val favoriteMembership: Flow<FavoriteMembership>
) {
    private val sessionItem = MutableStateFlow<SystemMediaSessionItem?>(null)
    private var attachedSession: MediaSession? = null
    private var publishJob: Job? = null

    private val sessionItemListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_DEVICE_INFO_CHANGED
                )
            ) {
                sessionItem.value = systemMediaSessionItem(player.currentMediaItem)
            }
        }
    }

    fun initialButtons(): ImmutableList<CommandButton> =
        commandButtons(systemMediaActionState(queueState.value, sessionItem = null, favorites = null))

    fun attach(session: MediaSession) {
        detach()
        attachedSession = session
        session.player.addListener(sessionItemListener)
        sessionItem.value = systemMediaSessionItem(session.player.currentMediaItem)
        publishJob = scope.launch {
            combine(
                queueState,
                sessionItem,
                favoriteMembership
                    .onStart<FavoriteMembership?> { emit(null) }
                    .catch { error ->
                        Timber.w(error, "Favorite state unavailable for system media actions")
                        emit(null)
                    }
            ) { queue, item, favorites ->
                systemMediaActionState(queue, item, favorites)
            }
                .distinctUntilChanged()
                .collect { state -> session.setMediaButtonPreferences(commandButtons(state)) }
        }
    }

    fun detach() {
        publishJob?.cancel()
        publishJob = null
        attachedSession?.player?.removeListener(sessionItemListener)
        attachedSession = null
        sessionItem.value = null
    }

    private fun commandButtons(state: SystemMediaActionState): ImmutableList<CommandButton> {
        val buttons = ImmutableList.builder<CommandButton>()
        systemMediaButtonSpecs(state).forEach { spec ->
            buttons.add(
                CommandButton.Builder(spec.icon)
                    .setDisplayName(context.getString(spec.labelRes))
                    .setSessionCommand(spec.command.sessionCommand())
                    .setCustomIconResId(spec.iconRes)
                    .build()
            )
        }
        return buttons.build()
    }

    companion object {
        val sessionCommands: List<SessionCommand> = SystemMediaCommand.entries.map { it.sessionCommand() }
    }
}

internal fun systemMediaSessionItem(mediaItem: MediaItem?): SystemMediaSessionItem? =
    mediaItem?.let { SystemMediaSessionItem(it.mediaId, isLiveRadioMediaItem(it)) }

private fun SystemMediaCommand.sessionCommand(): SessionCommand = SessionCommand(customAction, Bundle.EMPTY)
