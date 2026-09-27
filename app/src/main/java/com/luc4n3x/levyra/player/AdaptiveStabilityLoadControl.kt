package com.luc4n3x.levyra.player

import androidx.media3.common.Timeline
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator

class AdaptiveStabilityLoadControl(
    private val normal: LoadControl,
    private val stable: LoadControl,
    private val signals: PlaybackStabilityProfileSource
) : LoadControl {

    @Volatile
    private var active: LoadControl = normal

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
        val nextActive = if (signals.requestedProfile() == PlaybackStabilityProfile.Stable) stable else normal
        val inactive = if (nextActive === stable) normal else stable
        inactive.onTracksSelected(parameters, trackGroups, trackSelections)
        nextActive.onTracksSelected(parameters, trackGroups, trackSelections)
        active = nextActive
    }

    override fun onStopped(playerId: PlayerId) {
        normal.onStopped(playerId)
        stable.onStopped(playerId)
    }

    override fun onReleased(playerId: PlayerId) {
        normal.onReleased(playerId)
        stable.onReleased(playerId)
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
