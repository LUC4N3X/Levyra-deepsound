package com.luc4n3x.levyra.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import java.io.IOException

enum class PlaybackStabilityProfile { Normal, Stable }

interface PlaybackStabilityProfileSource {
    fun requestedProfile(): PlaybackStabilityProfile
}

class PlaybackStabilitySignals(
    private val nowMs: () -> Long = System::currentTimeMillis
) : Player.Listener, AnalyticsListener, PlaybackStabilityProfileSource {

    private val lock = Any()
    private var profile = PlaybackStabilityProfile.Normal
    private var stableSinceMs: Long? = null
    private var lastState = Player.STATE_IDLE
    private var lastPlayWhenReady = false
    private var suppressNextBuffering = true
    private var consecutiveLoadFailures = 0
    private val rebufferTimestamps = ArrayDeque<Long>()

    override fun requestedProfile(): PlaybackStabilityProfile = synchronized(lock) {
        pruneRebufferWindowLocked()
        if (profile == PlaybackStabilityProfile.Stable && canRecoverLocked()) {
            profile = PlaybackStabilityProfile.Normal
            stableSinceMs = null
        }
        profile
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        synchronized(lock) { lastPlayWhenReady = playWhenReady }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        synchronized(lock) {
            if (playbackState == Player.STATE_BUFFERING) {
                if (!suppressNextBuffering && lastState == Player.STATE_READY && lastPlayWhenReady) {
                    registerRebufferLocked()
                }
                suppressNextBuffering = false
            } else if (playbackState == Player.STATE_READY) {
                suppressNextBuffering = false
            }
            lastState = playbackState
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        onDiscontinuity()
    }

    override fun onLoadError(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean
    ) {
        onLoadOutcome(failed = true, wasCanceled = wasCanceled)
    }

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        onLoadOutcome(failed = false, wasCanceled = false)
    }

    internal fun onDiscontinuity() {
        synchronized(lock) { suppressNextBuffering = true }
    }

    internal fun onLoadOutcome(failed: Boolean, wasCanceled: Boolean) {
        synchronized(lock) {
            if (wasCanceled) return
            if (!failed) {
                consecutiveLoadFailures = 0
                return
            }
            consecutiveLoadFailures += 1
            if (consecutiveLoadFailures >= LoadFailureThreshold) escalateLocked()
        }
    }

    private fun registerRebufferLocked() {
        val now = nowMs()
        rebufferTimestamps.addLast(now)
        pruneRebufferWindowLocked()
        if (rebufferTimestamps.size >= RebufferThreshold) escalateLocked()
    }

    private fun pruneRebufferWindowLocked() {
        val now = nowMs()
        while (rebufferTimestamps.isNotEmpty() && now - rebufferTimestamps.first() > RebufferWindowMs) {
            rebufferTimestamps.removeFirst()
        }
    }

    private fun escalateLocked() {
        profile = PlaybackStabilityProfile.Stable
        stableSinceMs = nowMs()
    }

    private fun canRecoverLocked(): Boolean {
        val since = stableSinceMs ?: return true
        val dwellElapsed = nowMs() - since >= MinimumDwellMs
        return dwellElapsed && rebufferTimestamps.isEmpty() && consecutiveLoadFailures == 0
    }

    private companion object {
        const val RebufferWindowMs = 90_000L
        const val RebufferThreshold = 2
        const val LoadFailureThreshold = 3
        const val MinimumDwellMs = 120_000L
    }
}
