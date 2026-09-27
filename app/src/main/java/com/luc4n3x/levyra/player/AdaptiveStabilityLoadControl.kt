package com.luc4n3x.levyra.player

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
) : LoadControl {

    private val lock = Any()
    private val periodProfiles = object : LinkedHashMap<Any, LoadControl>(5, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Any, LoadControl>): Boolean = size > 5
    }

    @Volatile
    private var lastRequestedProfile: LoadControl = normal

    private fun loadControlFor(periodUid: Any?): LoadControl = synchronized(lock) {
        if (periodUid == null) return lastRequestedProfile
        return periodProfiles[periodUid] ?: lastRequestedProfile
    }

    val activeProfile: PlaybackStabilityProfile
        get() = if (lastRequestedProfile === stable) PlaybackStabilityProfile.Stable else PlaybackStabilityProfile.Normal

    override fun onPrepared(playerId: PlayerId) {
        normal.onPrepared(playerId)
        stable.onPrepared(playerId)
    }

    override fun onTracksSelected(
        parameters: LoadControl.Parameters,
        trackGroups: TrackGroupArray,
        trackSelections: Array<ExoTrackSelection?>
    ) {
        val uid = parameters.mediaPeriodId.periodUid
        synchronized(lock) {
            if (!periodProfiles.containsKey(uid)) {
                val profile = if (signals.requestedProfile() == PlaybackStabilityProfile.Stable) stable else normal
                periodProfiles[uid] = profile
                lastRequestedProfile = profile
            }
        }

        normal.onTracksSelected(parameters, trackGroups, trackSelections)
        stable.onTracksSelected(parameters, trackGroups, trackSelections)
    }

    override fun onStopped(playerId: PlayerId) {
        synchronized(lock) { periodProfiles.clear() }
        normal.onStopped(playerId)
        stable.onStopped(playerId)
    }

    override fun onReleased(playerId: PlayerId) {
        synchronized(lock) { periodProfiles.clear() }
        normal.onReleased(playerId)
        stable.onReleased(playerId)
    }

    override fun getAllocator(playerId: PlayerId): Allocator = normal.getAllocator(playerId)

    override fun getBackBufferDurationUs(playerId: PlayerId): Long = lastRequestedProfile.getBackBufferDurationUs(playerId)

    override fun retainBackBufferFromKeyframe(playerId: PlayerId): Boolean = lastRequestedProfile.retainBackBufferFromKeyframe(playerId)

    override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean {
        return loadControlFor(parameters.mediaPeriodId.periodUid).shouldContinueLoading(parameters)
    }

    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean {
        return loadControlFor(parameters.mediaPeriodId.periodUid).shouldStartPlayback(parameters)
    }

    override fun shouldContinuePreloading(
        playerId: PlayerId,
        timeline: Timeline,
        mediaPeriodId: MediaSource.MediaPeriodId,
        bufferedDurationUs: Long
    ): Boolean {
        return loadControlFor(mediaPeriodId.periodUid).shouldContinuePreloading(playerId, timeline, mediaPeriodId, bufferedDurationUs)
    }

    companion object {
        fun stableProfileOf(normalProfile: PlaybackBufferProfile): PlaybackBufferProfile = PlaybackBufferProfile(
            minBufferMs = normalProfile.minBufferMs + StableExtraMinBufferMs,
            maxBufferMs = normalProfile.maxBufferMs + StableExtraMaxBufferMs,
            playbackBufferMs = normalProfile.playbackBufferMs,
            rebufferMs = normalProfile.rebufferMs + StableExtraRebufferMs,
            backBufferMs = normalProfile.backBufferMs
        )

        private const val StableExtraMinBufferMs = 3_000
        private const val StableExtraMaxBufferMs = 16_000
        private const val StableExtraRebufferMs = 1_500
    }
}
