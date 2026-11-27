package com.luc4n3x.levyra.player.visualizer

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class SpectrumClockPublisher(
    private val player: Player,
    private val scope: CoroutineScope,
    private val history: PcmHistory = SpectrumEngine.history
) : Player.Listener {
    private var enabled = false
    private var refreshJob: Job? = null
    private var chainRootUid: Any? = null
    private var chainOffsetUs = 0L
    private var lastPeriodUid: Any? = null
    private var lastDurationUs = 0L

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        history.setEnabled(value)
        if (value) {
            player.addListener(this)
            publish()
            refreshJob = scope.launch {
                SpectrumEngine.active.collectLatest { observed ->
                    while (observed && isActive) {
                        publish()
                        delay(REFRESH_MS)
                    }
                }
            }
        } else {
            player.removeListener(this)
            refreshJob?.cancel()
            refreshJob = null
            resetChain()
            SpectrumEngine.clock = null
        }
    }

    fun release() = setEnabled(false)

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val continuous = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
        val previous = lastPeriodUid
        if (!continuous || previous == null || lastDurationUs <= 0L) {
            resetChain()
            return
        }
        if (chainRootUid == null || history.hasSegment(previous)) {
            chainRootUid = previous
            chainOffsetUs = lastDurationUs
        } else {
            chainOffsetUs += lastDurationUs
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
            resetChain()
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(*PUBLISH_EVENTS)) publish()
    }

    private fun resetChain() {
        chainRootUid = null
        chainOffsetUs = 0L
    }

    private fun publish() {
        if (!enabled) return
        val timeline = player.currentTimeline
        val periodUid = if (timeline.isEmpty) null else timeline.getUidOfPeriod(player.currentPeriodIndex)
        val durationMs = player.duration
        lastPeriodUid = periodUid
        lastDurationUs = if (durationMs == C.TIME_UNSET || durationMs <= 0L) 0L else durationMs * 1_000L
        SpectrumEngine.clock = PlaybackClockAnchor(
            periodUid = periodUid,
            chainRootUid = chainRootUid,
            chainOffsetUs = chainOffsetUs,
            positionUs = player.currentPosition * 1_000L,
            elapsedRealtimeUs = SystemClock.elapsedRealtimeNanos() / 1_000L,
            speed = player.playbackParameters.speed,
            playing = player.isPlaying,
            durationUs = lastDurationUs
        )
    }

    private companion object {
        const val REFRESH_MS = 500L
        val PUBLISH_EVENTS = intArrayOf(
            Player.EVENT_IS_PLAYING_CHANGED,
            Player.EVENT_PLAYBACK_STATE_CHANGED,
            Player.EVENT_POSITION_DISCONTINUITY,
            Player.EVENT_MEDIA_ITEM_TRANSITION,
            Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
            Player.EVENT_TIMELINE_CHANGED
        )
    }
}
