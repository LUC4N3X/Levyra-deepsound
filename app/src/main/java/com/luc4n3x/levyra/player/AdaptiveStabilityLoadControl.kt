package com.luc4n3x.levyra.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator
import java.util.LinkedHashMap

class AdaptiveStabilityLoadControl(
    private val normal: LoadControl,
    private val stable: LoadControl,
    private val signals: PlaybackStabilityProfileSource
) : LoadControl, Player.Listener {

    @Volatile
    private var active: LoadControl = normal

    private val mapLock = Any()
    private val profileByMediaItem = LinkedHashMap<Any, PlaybackStabilityProfile>()
    private val window = Timeline.Window()
    private val period = Timeline.Period()
    private var currentMediaItem: MediaItem? = null
    private var currentProfile: PlaybackStabilityProfile? = null

    val activeProfile: PlaybackStabilityProfile
        get() = if (active === stable) PlaybackStabilityProfile.Stable else PlaybackStabilityProfile.Normal

    override fun onPrepared(playerId: PlayerId) {
        normal.onPrepared(playerId)
        stable.onPrepared(playerId)
    }

    override fun onTracksSelected(
        parameters: LoadControl.Parameters,
        trackGroups: TrackGroupArray,
        trackSelections: Array<ExoTrackSelection?>
    ) {
        normal.onTracksSelected(parameters, trackGroups, trackSelections)
        stable.onTracksSelected(parameters, trackGroups, trackSelections)

        val key = mediaItemKeyOf(parameters)
        val item = mediaItemOf(parameters)
        synchronized(mapLock) {
            val isCurrent = currentMediaItem == null || (item != null && item == currentMediaItem)
            if (isCurrent) {
                val profile = currentProfile ?: profileFor(key)
                profileByMediaItem[key] = profile
                active = if (profile == PlaybackStabilityProfile.Stable) stable else normal
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        synchronized(mapLock) {
            currentMediaItem = mediaItem
            if (mediaItem != null) {
                val profile = signals.requestedProfile()
                currentProfile = profile
                active = if (profile == PlaybackStabilityProfile.Stable) stable else normal
            } else {
                currentProfile = null
            }
        }
    }

    override fun onStopped(playerId: PlayerId) {
        normal.onStopped(playerId)
        stable.onStopped(playerId)
        clearProfiles()
    }

    override fun onReleased(playerId: PlayerId) {
        normal.onReleased(playerId)
        stable.onReleased(playerId)
        clearProfiles()
    }

    override fun getAllocator(playerId: PlayerId): Allocator = normal.getAllocator(playerId)

    override fun getBackBufferDurationUs(playerId: PlayerId): Long = active.getBackBufferDurationUs(playerId)

    override fun retainBackBufferFromKeyframe(playerId: PlayerId): Boolean = active.retainBackBufferFromKeyframe(playerId)

    override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean = active.shouldContinueLoading(parameters)

    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean = active.shouldStartPlayback(parameters)

    override fun shouldContinuePreloading(
        playerId: PlayerId,
        timeline: Timeline,
        mediaPeriodId: MediaSource.MediaPeriodId,
        bufferedDurationUs: Long
    ): Boolean = active.shouldContinuePreloading(playerId, timeline, mediaPeriodId, bufferedDurationUs)

    private fun mediaItemOf(parameters: LoadControl.Parameters): MediaItem? {
        val periodUid = parameters.mediaPeriodId.periodUid
        val periodIndex = parameters.timeline.getIndexOfPeriod(periodUid)
        if (periodIndex < 0) return null
        synchronized(mapLock) {
            parameters.timeline.getPeriod(periodIndex, period)
            val windowIndex = period.windowIndex
            if (windowIndex < 0 || windowIndex >= parameters.timeline.windowCount) return null
            parameters.timeline.getWindow(windowIndex, window)
            return window.mediaItem
        }
    }

    private fun mediaItemKeyOf(parameters: LoadControl.Parameters): Any {
        val periodUid = parameters.mediaPeriodId.periodUid
        val periodIndex = parameters.timeline.getIndexOfPeriod(periodUid)
        if (periodIndex < 0) return periodUid
        synchronized(mapLock) {
            parameters.timeline.getPeriod(periodIndex, period)
            val windowIndex = period.windowIndex
            if (windowIndex < 0 || windowIndex >= parameters.timeline.windowCount) return periodUid
            parameters.timeline.getWindow(windowIndex, window)
            return window.uid
        }
    }

    private fun profileFor(key: Any): PlaybackStabilityProfile = synchronized(mapLock) {
        profileByMediaItem[key] ?: run {
            val resolved = signals.requestedProfile()
            profileByMediaItem[key] = resolved
            if (profileByMediaItem.size > MaxProfileEntries) {
                val eldest = profileByMediaItem.entries.iterator().next().key
                profileByMediaItem.remove(eldest)
            }
            resolved
        }
    }

    private fun clearProfiles() {
        synchronized(mapLock) {
            profileByMediaItem.clear()
            currentMediaItem = null
            currentProfile = null
        }
    }

    internal fun trackedMediaItemCount(): Int = synchronized(mapLock) { profileByMediaItem.size }

    companion object {
        fun stableProfileOf(normalProfile: PlaybackBufferProfile): PlaybackBufferProfile = PlaybackBufferProfile(
            minBufferMs = normalProfile.minBufferMs + StableExtraMinBufferMs,
            maxBufferMs = normalProfile.maxBufferMs + StableExtraMaxBufferMs,
            playbackBufferMs = normalProfile.playbackBufferMs,
            rebufferMs = normalProfile.rebufferMs + StableExtraRebufferMs,
            backBufferMs = normalProfile.backBufferMs
        )

        private const val MaxProfileEntries = 8
        private const val StableExtraMinBufferMs = 3_000
        private const val StableExtraMaxBufferMs = 16_000
        private const val StableExtraRebufferMs = 1_500
    }
}
