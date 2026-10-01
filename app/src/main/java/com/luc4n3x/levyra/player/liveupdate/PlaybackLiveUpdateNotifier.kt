package com.luc4n3x.levyra.player.liveupdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import timber.log.Timber

internal class PlaybackLiveUpdateNotifier(context: Context) : Player.Listener {
    private val appContext = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(appContext)
    private var player: Player? = null
    private var channelReady = false
    private var contentIntent: PendingIntent? = null
    private var artwork: Icon? = null
    private var artworkMediaId: String? = null
    private var posted: PlaybackLiveUpdateContent? = null
    private var postedWithArtwork = false

    init {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun attach(sessionPlayer: Player) {
        if (player === sessionPlayer) return
        player?.removeListener(this)
        player = sessionPlayer
        sessionPlayer.addListener(this)
    }

    fun onMediaNotification(mediaId: String?, notification: Notification) {
        contentIntent = notification.contentIntent
        val largeIcon = notification.getLargeIcon()
        if (largeIcon != null || mediaId != artworkMediaId) {
            artwork = largeIcon
            artworkMediaId = mediaId
        }
        refresh()
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_MEDIA_METADATA_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY,
                Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                Player.EVENT_TIMELINE_CHANGED
            )
        ) {
            refresh()
        }
    }

    fun release() {
        player?.removeListener(this)
        player = null
        cancel()
        contentIntent = null
        artwork = null
        artworkMediaId = null
    }

    private fun refresh() {
        val current = player ?: return cancel()
        val content = PlaybackLiveUpdateMapper.map(snapshot(current)) ?: return cancel()
        val intent = contentIntent ?: return cancel()
        if (!canPromote()) return cancel()
        val contentArtwork = artwork.takeIf { artworkMediaId == content.mediaId }
        if (content.isEquivalentTo(posted) && postedWithArtwork == (contentArtwork != null)) return
        post(content, intent, contentArtwork)
    }

    private fun snapshot(player: Player): PlaybackLiveUpdateInput {
        val metadata = player.mediaMetadata
        val duration = player.duration
        return PlaybackLiveUpdateInput(
            mediaId = player.currentMediaItem?.mediaId,
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString().orEmpty(),
            playWhenReady = player.playWhenReady,
            buffering = player.playbackState == Player.STATE_BUFFERING,
            ready = player.playbackState == Player.STATE_READY,
            playing = player.isPlaying,
            playingAd = player.isPlayingAd,
            dynamic = player.isCurrentMediaItemDynamic,
            speed = player.playbackParameters.speed,
            positionMs = player.contentPosition,
            durationMs = if (duration == C.TIME_UNSET) 0L else duration,
            nowEpochMs = System.currentTimeMillis()
        )
    }

    private fun canPromote(): Boolean =
        notificationManager.areNotificationsEnabled() && notificationManager.canPostPromotedNotifications()

    private fun post(content: PlaybackLiveUpdateContent, intent: PendingIntent, contentArtwork: Icon?) {
        ensureChannel()
        val chronometerBase = content.chronometerBaseEpochMs
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_playing)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setLargeIcon(contentArtwork)
            .setContentIntent(intent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(chronometerBase != null)
            .setUsesChronometer(chronometerBase != null)
            .setWhen(chronometerBase ?: 0L)
            .setTimeoutAfter(content.timeoutMs)
            .setRequestPromotedOngoing(true)
            .build()
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            posted = content
            postedWithArtwork = contentArtwork != null
        } catch (error: SecurityException) {
            Timber.w(error, "Playback Live Update could not be posted")
            posted = null
        }
    }

    private fun cancel() {
        if (posted == null) return
        notificationManager.cancel(NOTIFICATION_ID)
        posted = null
        postedWithArtwork = false
    }

    private fun ensureChannel() {
        if (channelReady) return
        val strings = LevyraStrings.forCode(LevyraPreferences(appContext).languageCode())
        val channel = NotificationChannel(
            CHANNEL_ID,
            strings.liveUpdatePlaybackChannel,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
        channelReady = true
    }

    private companion object {
        const val CHANNEL_ID = "levyra_playback_live_update"
        const val NOTIFICATION_ID = 0x4c4c5550
    }
}
