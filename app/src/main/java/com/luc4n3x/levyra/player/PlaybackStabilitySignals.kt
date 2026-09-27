package com.luc4n3x.levyra.player

import androidx.media3.common.C
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
    private val nowMs: () -> Long = android.os.SystemClock::elapsedRealtime
) : Player.Listener, AnalyticsListener, PlaybackStabilityProfileSource {

    private val lock = Any()
    private var profile = PlaybackStabilityProfile.Normal
    private var stableSinceMs: Long? = null
    private var lastState = Player.STATE_IDLE
    private var lastPlayWhenReady = false
    private var suppressNextBuffering = true
    private var consecutiveLoadFailures = 0
    private val rebufferTimestamps = ArrayDeque<Long>()
    private var playingPeriodUid: Any? = null

    override fun requestedProfile(): PlaybackStabilityProfile = synchronized(lock) {
        pruneRebufferWindowLocked()
        if (profile == PlaybackStabilityProfile.Stable && canRecoverLocked()) {
            profile = PlaybackStabilityProfile.Normal
            stableSinceMs = null
            consecutiveLoadFailures = 0
        }
        profile
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        synchronized(lock) { lastPlayWhenReady = playWhenReady }
    }

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
        synchronized(lock) {
            eventTime.mediaPeriodId?.periodUid?.let { playingPeriodUid = it }
        }
    }

    override fun onMediaItemTransition(eventTime: AnalyticsListener.EventTime, mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        synchronized(lock) {
            eventTime.mediaPeriodId?.periodUid?.let { playingPeriodUid = it }
        }
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
        if (!isRelevantLoad(eventTime, mediaLoadData)) return
        onLoadOutcome(failed = true, wasCanceled = wasCanceled)
    }

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        if (!isRelevantLoad(eventTime, mediaLoadData)) return
        onLoadOutcome(failed = false, wasCanceled = false)
    }

    private fun isRelevantLoad(eventTime: AnalyticsListener.EventTime, mediaLoadData: MediaLoadData): Boolean {
        synchronized(lock) {
            if (playingPeriodUid != null && eventTime.mediaPeriodId?.periodUid != playingPeriodUid) {
                return false
            }
        }
        val isMedia = mediaLoadData.dataType == C.DATA_TYPE_MEDIA ||
                      mediaLoadData.dataType == C.DATA_TYPE_MEDIA_INITIALIZATION
        val isMainTrack = mediaLoadData.trackType == C.TRACK_TYPE_AUDIO ||
                          mediaLoadData.trackType == C.TRACK_TYPE_VIDEO ||
                          mediaLoadData.trackType == C.TRACK_TYPE_DEFAULT ||
                          mediaLoadData.trackType == C.TRACK_TYPE_UNKNOWN
        return isMedia && isMainTrack
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
        return dwellElapsed && rebufferTimestamps.isEmpty()
    }

    private companion object {
        const val RebufferWindowMs = 90_000L
        const val RebufferThreshold = 2
        const val LoadFailureThreshold = 3
        const val MinimumDwellMs = 120_000L
    }
}
