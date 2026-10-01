package com.luc4n3x.levyra.player.liveupdate

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

@UnstableApi
internal class LiveUpdateMediaNotificationProvider(
    private val delegate: MediaNotification.Provider,
    private val liveUpdate: PlaybackLiveUpdateNotifier
) : MediaNotification.Provider {
    override fun createNotification(
        mediaSession: MediaSession,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val mediaId = mediaSession.player.currentMediaItem?.mediaId
        val mediaNotification = delegate.createNotification(
            mediaSession,
            mediaButtonPreferences,
            actionFactory
        ) { updated ->
            onNotificationChangedCallback.onNotificationChanged(updated)
            liveUpdate.onMediaNotification(mediaId, updated.notification)
        }
        liveUpdate.onMediaNotification(mediaId, mediaNotification.notification)
        return mediaNotification
    }

    override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean =
        delegate.handleCustomCommand(session, action, extras)

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo =
        delegate.notificationChannelInfo
}
